package api

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"

	"github.com/kyx/kyx-data-center/internal/auth"
	"github.com/kyx/kyx-data-center/internal/lookup"
	"github.com/kyx/kyx-data-center/internal/queue"
	"github.com/kyx/kyx-data-center/internal/store"
	"github.com/kyx/kyx-data-center/internal/template"
	"github.com/kyx/kyx-data-center/internal/worker"
)

// Deps 注入 dependencies
type Deps struct {
	DB       *sql.DB
	Store    *store.Store
	Queue    *queue.Queue
	Pool     *worker.Pool
	Registry *template.Registry
	Storage  string
	Quota    QuotaConfig
}

type QuotaConfig struct {
	PerUserActive int
	PerUserDaily  int
}

// Mount 挂路由。所有业务路由都要求用户具备 app:data-center:use 权限点
// 或者是老板 / 租户管理员角色。permission check 走 kyx_oa，per-user 60s 缓存。
func Mount(r chi.Router, d *Deps) {
	r.Group(func(r chi.Router) {
		r.Use(auth.RequireDataCenterAccess)
		r.Get("/templates", d.handleListTemplates)
		r.Post("/jobs", d.handleCreateJob)
		r.Get("/jobs", d.handleListMyJobs)
		r.Get("/jobs/{id}", d.handleGetJob)
		r.Delete("/jobs/{id}", d.handleCancelJob)
		r.Get("/jobs/{id}/xlsx", d.handleDownload)
		r.Get("/hall", d.handleHall)
		// 可搜索下拉数据源（filter.type = "lookup" 用）
		r.Get("/lookups/{name}", d.handleLookupSearch)
	})

	// admin
	r.Group(func(r chi.Router) {
		r.Use(auth.RequireDataCenterAccess)
		r.Use(requireAdmin)
		r.Get("/admin/jobs", d.handleAdminListJobs)
	})
}

// GET /templates: 我能看到的模板
func (d *Deps) handleListTemplates(w http.ResponseWriter, r *http.Request) {
	p := auth.MustFromContext(r.Context())
	list := d.Registry.ListVisible(p.DeptID, p.Roles)
	// 精简: 不返 sql, 只返 UI 需要
	type tplLite struct {
		ID          string            `json:"id"`
		Label       string            `json:"label"`
		Category    string            `json:"category"`
		Description string            `json:"description"`
		Icon        string            `json:"icon"`
		Color       string            `json:"color"`
		MaxRows     int               `json:"max_rows"`
		Filters     map[string]template.Filter `json:"filters"`
		Enums       map[string]template.Enum   `json:"enums"`
	}
	out := make([]tplLite, 0, len(list))
	for _, t := range list {
		out = append(out, tplLite{
			ID: t.ID, Label: t.Label, Category: t.Category, Description: t.Description,
			Icon: t.Icon, Color: t.Color, MaxRows: t.MaxRows,
			Filters: t.Filters, Enums: t.Enums,
		})
	}
	writeJSON(w, http.StatusOK, out)
}

// POST /jobs: 创建导出任务
func (d *Deps) handleCreateJob(w http.ResponseWriter, r *http.Request) {
	p := auth.MustFromContext(r.Context())
	var body struct {
		TemplateID string         `json:"template_id"`
		Filters    map[string]any `json:"filters"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeErr(w, http.StatusBadRequest, "bad json")
		return
	}
	tpl, ok := d.Registry.Get(body.TemplateID)
	if !ok {
		writeErr(w, http.StatusNotFound, "template not found")
		return
	}
	if !tpl.AllowFor(p.DeptID, p.Roles) {
		writeErr(w, http.StatusForbidden, "无权使用此模板")
		return
	}
	// 试拼一次 SQL, 校验 filters
	if _, _, err := tpl.BuildSQL(body.Filters, tpl.MaxRows); err != nil {
		writeErr(w, http.StatusBadRequest, err.Error())
		return
	}
	// quota: 单用户同时最多 N 个
	active, err := d.Store.CountActiveByOwner(r.Context(), p.UserID)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "check quota: "+err.Error())
		return
	}
	if active >= d.Quota.PerUserActive {
		writeErr(w, http.StatusTooManyRequests, fmt.Sprintf("你已有 %d 个导出在进行中，请等完成后再发起", active))
		return
	}

	// 建 job
	paramsBytes, _ := json.Marshal(body.Filters)
	job := &store.Job{
		ID:         uuid.NewString(),
		OwnerID:    p.UserID,
		OwnerName:  p.UserName,
		TemplateID: body.TemplateID,
		ParamsJSON: string(paramsBytes),
		State:      store.StatePending,
	}
	if err := d.Store.Insert(r.Context(), job); err != nil {
		writeErr(w, http.StatusInternalServerError, "insert job: "+err.Error())
		return
	}
	// 入队
	pos, err := d.Queue.Push(job.ID)
	if err != nil {
		_ = d.Store.MarkFailed(r.Context(), job.ID, "queue full")
		writeErr(w, http.StatusServiceUnavailable, "队列已满")
		return
	}
	_ = d.Store.MarkQueued(r.Context(), job.ID, pos)

	writeJSON(w, http.StatusAccepted, map[string]any{
		"job_id":    job.ID,
		"queue_pos": pos,
		"queue_len": d.Queue.Len(),
	})
}

// fillTemplateLabel 从当前 Registry 补 TemplateLabel 字段。
// 模板被重命名/删除的历史任务也能看到当前名，找不到就用 ID 兜底。
func (d *Deps) fillTemplateLabel(j *store.Job) {
	if j == nil {
		return
	}
	if tpl, ok := d.Registry.Get(j.TemplateID); ok {
		j.TemplateLabel = tpl.Label
	} else {
		j.TemplateLabel = j.TemplateID
	}
}

// GET /jobs/:id: 状态
func (d *Deps) handleGetJob(w http.ResponseWriter, r *http.Request) {
	p := auth.MustFromContext(r.Context())
	id := chi.URLParam(r, "id")
	j, err := d.Store.Get(r.Context(), id)
	if err != nil {
		if errors.Is(err, store.ErrNotFound) {
			writeErr(w, http.StatusNotFound, "job not found")
			return
		}
		writeErr(w, http.StatusInternalServerError, err.Error())
		return
	}
	if j.OwnerID != p.UserID && !p.IsAdmin {
		writeErr(w, http.StatusForbidden, "非本人任务")
		return
	}
	// 实时位次覆盖 (数据库里的 pos 可能过时)
	if j.State == store.StateQueued {
		pos := d.Queue.Position(j.ID)
		if pos > 0 {
			j.QueuePos = &pos
		}
	}
	d.fillTemplateLabel(j)
	writeJSON(w, http.StatusOK, j)
}

// GET /jobs?limit=20: 我的
func (d *Deps) handleListMyJobs(w http.ResponseWriter, r *http.Request) {
	p := auth.MustFromContext(r.Context())
	limit := 20
	if s := r.URL.Query().Get("limit"); s != "" {
		if n, err := strconv.Atoi(s); err == nil && n > 0 && n <= 100 {
			limit = n
		}
	}
	list, err := d.Store.ListByOwner(r.Context(), p.UserID, limit)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, err.Error())
		return
	}
	for _, j := range list {
		d.fillTemplateLabel(j)
	}
	writeJSON(w, http.StatusOK, list)
}

// DELETE /jobs/:id: 取消 (只能取自己的, pending/queued/running)
func (d *Deps) handleCancelJob(w http.ResponseWriter, r *http.Request) {
	p := auth.MustFromContext(r.Context())
	id := chi.URLParam(r, "id")
	_ = d.Queue.Remove(id)
	if err := d.Store.MarkCancelled(r.Context(), id, p.UserID); err != nil {
		writeErr(w, http.StatusInternalServerError, err.Error())
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"cancelled": id})
}

// GET /jobs/:id/xlsx: 下载
func (d *Deps) handleDownload(w http.ResponseWriter, r *http.Request) {
	p := auth.MustFromContext(r.Context())
	id := chi.URLParam(r, "id")
	j, err := d.Store.Get(r.Context(), id)
	if err != nil {
		writeErr(w, http.StatusNotFound, "job not found")
		return
	}
	if j.OwnerID != p.UserID && !p.IsAdmin {
		writeErr(w, http.StatusForbidden, "非本人任务")
		return
	}
	if j.State != store.StateDone || j.FilePath == "" {
		writeErr(w, http.StatusBadRequest, "任务未完成")
		return
	}
	f, err := os.Open(j.FilePath)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "open file: "+err.Error())
		return
	}
	defer f.Close()
	tpl, _ := d.Registry.Get(j.TemplateID)
	label := j.TemplateID
	if tpl != nil {
		label = tpl.Label
	}
	fileName := fmt.Sprintf("%s_%s.xlsx", label, j.CreatedAt.Format("20060102_150405"))
	w.Header().Set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	w.Header().Set("Content-Disposition", fmt.Sprintf(`attachment; filename*=UTF-8''%s`, urlEncode(fileName)))
	if j.FileSize != nil {
		w.Header().Set("Content-Length", strconv.FormatInt(*j.FileSize, 10))
	}
	_, _ = copyFile(w, f)
}

// GET /lookups/{name}?q=xxx&limit=30&offset=0 —— 搜索 + 分页
func (d *Deps) handleLookupSearch(w http.ResponseWriter, r *http.Request) {
	name := chi.URLParam(r, "name")
	spec, ok := lookup.Get(name)
	if !ok {
		writeErr(w, http.StatusNotFound, "unknown lookup: "+name)
		return
	}
	q := strings.TrimSpace(r.URL.Query().Get("q"))
	limit := 30
	if s := r.URL.Query().Get("limit"); s != "" {
		if n, err := strconv.Atoi(s); err == nil && n > 0 && n <= 100 {
			limit = n
		}
	}
	offset := 0
	if s := r.URL.Query().Get("offset"); s != "" {
		if n, err := strconv.Atoi(s); err == nil && n >= 0 && n <= 100000 {
			offset = n
		}
	}
	items, err := spec.Search(r.Context(), d.DB, q, limit, offset)
	if err != nil {
		writeErr(w, http.StatusInternalServerError, err.Error())
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"name":     spec.Name,
		"label":    spec.Label,
		"q":        q,
		"offset":   offset,
		"limit":    limit,
		"items":    items,
		"has_more": len(items) == limit, // 拿满 = 可能还有下一页
	})
}

// GET /hall: 大厅快照 (谁在导, 队列多长)
func (d *Deps) handleHall(w http.ResponseWriter, r *http.Request) {
	list, err := d.Store.ListActive(r.Context())
	if err != nil {
		writeErr(w, http.StatusInternalServerError, err.Error())
		return
	}
	for _, j := range list {
		d.fillTemplateLabel(j)
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"queue_len":   d.Queue.Len(),
		"worker_cap":  d.Pool.Info().WorkerCap,
		"active_jobs": list,
	})
}

// admin: 全部任务
func (d *Deps) handleAdminListJobs(w http.ResponseWriter, r *http.Request) {
	// TODO: 支持 state / template_id / owner_id 过滤
	list, err := d.Store.ListActive(r.Context())
	if err != nil {
		writeErr(w, http.StatusInternalServerError, err.Error())
		return
	}
	for _, j := range list {
		d.fillTemplateLabel(j)
	}
	writeJSON(w, http.StatusOK, list)
}

// --- helpers ---

func requireAdmin(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		p := auth.MustFromContext(r.Context())
		if !p.IsAdmin {
			writeErr(w, http.StatusForbidden, "管理员专属")
			return
		}
		next.ServeHTTP(w, r)
	})
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func writeErr(w http.ResponseWriter, status int, msg string) {
	writeJSON(w, status, map[string]any{"code": status, "msg": msg})
}

// URL 编码文件名 (中文)
func urlEncode(s string) string {
	buf := make([]byte, 0, len(s)*3)
	for i := 0; i < len(s); i++ {
		c := s[i]
		if 'A' <= c && c <= 'Z' || 'a' <= c && c <= 'z' || '0' <= c && c <= '9' || c == '.' || c == '-' || c == '_' {
			buf = append(buf, c)
		} else {
			buf = append(buf, '%', hexChar(c>>4), hexChar(c&0x0F))
		}
	}
	return string(buf)
}

func hexChar(b byte) byte {
	if b < 10 {
		return '0' + b
	}
	return 'A' + b - 10
}

// copyFile 简单 io.Copy 包装, 避免 import io
func copyFile(dst http.ResponseWriter, src *os.File) (int64, error) {
	buf := make([]byte, 32*1024)
	var total int64
	for {
		n, err := src.Read(buf)
		if n > 0 {
			if _, werr := dst.Write(buf[:n]); werr != nil {
				return total, werr
			}
			total += int64(n)
		}
		if err != nil {
			if err.Error() == "EOF" {
				return total, nil
			}
			return total, err
		}
	}
}

// GCLoop 每小时扫 storage 清老于 TTL 的文件 (main 启动一个 goroutine)
func GCLoop(ctx context.Context, storageDir string, ttl time.Duration) {
	ticker := time.NewTicker(time.Hour)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			cutoff := time.Now().Add(-ttl)
			entries, _ := os.ReadDir(storageDir)
			for _, e := range entries {
				if e.IsDir() || filepath.Ext(e.Name()) != ".xlsx" {
					continue
				}
				info, err := e.Info()
				if err != nil {
					continue
				}
				if info.ModTime().Before(cutoff) {
					_ = os.Remove(filepath.Join(storageDir, e.Name()))
				}
			}
		}
	}
}
