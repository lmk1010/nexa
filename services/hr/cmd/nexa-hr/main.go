// nexa-hr — employees, departments, dingtalk sync skeleton (Go).
package main

import (
	"context"
	"encoding/json"
	"errors"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"sync"
	"syscall"
	"time"
)

var version = "0.3.0-m3"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	DataDir string `json:"dataDir"`
}

type employee struct {
	ID         int64  `json:"id"`
	Name       string `json:"name"`
	Mobile     string `json:"mobile"`
	DeptID     int64  `json:"deptId"`
	DeptName   string `json:"deptName"`
	JobNo      string `json:"jobNo"`
	Status     string `json:"status"`
	DingTalkID string `json:"dingTalkId,omitempty"`
	UpdatedAt  string `json:"updatedAt"`
}

type department struct {
	ID       int64  `json:"id"`
	Name     string `json:"name"`
	ParentID int64  `json:"parentId"`
}

type syncJob struct {
	ID        string `json:"id"`
	Type      string `json:"type"`
	Status    string `json:"status"`
	Message   string `json:"message"`
	StartedAt string `json:"startedAt"`
	FinishedAt string `json:"finishedAt,omitempty"`
	Stats     map[string]int `json:"stats,omitempty"`
}

type db struct {
	Employees   []employee   `json:"employees"`
	Departments []department `json:"departments"`
	SyncJobs    []syncJob    `json:"syncJobs"`
	SeqEmp      int64        `json:"seqEmp"`
	SeqDept     int64        `json:"seqDept"`
}

type server struct {
	cfg  config
	mu   sync.Mutex
	db   db
	path string
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-hr", DataDir: "./data"}
	cfg.HTTP.Addr = ":48083"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48083"
	}
	if v := os.Getenv("NEXA_DATA_DIR"); v != "" {
		cfg.DataDir = v
	}
	if cfg.DataDir == "" {
		cfg.DataDir = "./data"
	}
	s := &server{cfg: cfg, path: filepath.Join(cfg.DataDir, "hr.json")}
	if err := s.loadOrSeed(); err != nil {
		log.Fatalf("store: %v", err)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusOK, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/hr/employees", s.handleEmployees)
	mux.HandleFunc("/v1/hr/departments/tree", s.handleDeptTree)
	mux.HandleFunc("/v1/hr/departments", s.handleDepartments)
	mux.HandleFunc("/v1/hr/dingtalk/sync", s.handleDingSync)
	mux.HandleFunc("/v1/hr/dingtalk/jobs", s.handleDingJobs)
	mux.HandleFunc("/v1/hr/dingtalk/status", s.handleDingStatus)
	mux.HandleFunc("/app-api/hr/employees", s.handleEmployees)
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusOK, map[string]any{"service": cfg.Name, "version": version, "status": "file-backed", "apis": []string{
			"/v1/hr/employees", "/v1/hr/departments/tree", "/v1/hr/dingtalk/sync", "/v1/hr/dingtalk/jobs", "/v1/hr/dingtalk/status",
		}})
	})

	srv := &http.Server{Addr: cfg.HTTP.Addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] %s %s on %s data=%s", cfg.Name, version, cfg.HTTP.Addr, s.path)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
	}()
	<-ctx.Done()
	shCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shCtx)
}

func (s *server) loadOrSeed() error {
	_ = os.MkdirAll(filepath.Dir(s.path), 0o755)
	raw, err := os.ReadFile(s.path)
	if err == nil {
		return json.Unmarshal(raw, &s.db)
	}
	if !errors.Is(err, os.ErrNotExist) {
		return err
	}
	now := time.Now().Format(time.RFC3339)
	s.db = db{
		Departments: []department{
			{ID: 1, Name: "HQ", ParentID: 0},
			{ID: 10, Name: "R&D", ParentID: 1},
			{ID: 20, Name: "Ops", ParentID: 1},
			{ID: 30, Name: "HR", ParentID: 1},
		},
		Employees: []employee{
			{ID: 1001, Name: "Zhang San", Mobile: "13800000001", DeptID: 10, DeptName: "R&D", JobNo: "KYX001", Status: "active", UpdatedAt: now},
			{ID: 1002, Name: "Li Si", Mobile: "13800000002", DeptID: 20, DeptName: "Ops", JobNo: "KYX002", Status: "active", UpdatedAt: now},
			{ID: 1003, Name: "Wang Wu", Mobile: "13800000003", DeptID: 30, DeptName: "HR", JobNo: "KYX003", Status: "active", UpdatedAt: now},
		},
		SyncJobs: []syncJob{},
		SeqEmp:   1003,
		SeqDept:  30,
	}
	return s.save()
}

func (s *server) save() error {
	raw, err := json.MarshalIndent(s.db, "", "  ")
	if err != nil {
		return err
	}
	tmp := s.path + ".tmp"
	if err := os.WriteFile(tmp, raw, 0o644); err != nil {
		return err
	}
	return os.Rename(tmp, s.path)
}

func (s *server) handleEmployees(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodGet:
		s.mu.Lock()
		defer s.mu.Unlock()
		writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": s.db.Employees, "total": len(s.db.Employees)})
	case http.MethodPost:
		var body employee
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]any{"code": 400, "msg": "bad json"})
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		s.db.SeqEmp++
		body.ID = s.db.SeqEmp
		if body.Status == "" {
			body.Status = "active"
		}
		body.UpdatedAt = time.Now().Format(time.RFC3339)
		s.db.Employees = append(s.db.Employees, body)
		_ = s.save()
		writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": body})
	default:
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"code": 405})
	}
}

func (s *server) handleDepartments(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": s.db.Departments, "total": len(s.db.Departments)})
}

func (s *server) handleDeptTree(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	type node struct {
		ID       int64  `json:"id"`
		Name     string `json:"name"`
		Children []node `json:"children,omitempty"`
	}
	byParent := map[int64][]department{}
	for _, d := range s.db.Departments {
		byParent[d.ParentID] = append(byParent[d.ParentID], d)
	}
	var build func(parent int64) []node
	build = func(parent int64) []node {
		out := []node{}
		for _, d := range byParent[parent] {
			out = append(out, node{ID: d.ID, Name: d.Name, Children: build(d.ID)})
		}
		return out
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": build(0)})
}

// handleDingSync simulates a dingtalk directory/roster pull and upserts local store.
// Real OpenAPI credentials can be wired later via config/env without changing routes.
func (s *server) handleDingSync(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"code": 405})
		return
	}
	var body struct {
		Mode string `json:"mode"` // directory | roster | full
	}
	_ = json.NewDecoder(r.Body).Decode(&body)
	if body.Mode == "" {
		body.Mode = "full"
	}
	jobID := "ding_" + time.Now().Format("20060102_150405")
	started := time.Now().Format(time.RFC3339)
	s.mu.Lock()
	job := syncJob{ID: jobID, Type: body.Mode, Status: "running", Message: "sync started", StartedAt: started}
	s.db.SyncJobs = append([]syncJob{job}, s.db.SyncJobs...)
	_ = s.save()
	s.mu.Unlock()

	// Simulated remote payload (replace with DingTalk OpenAPI client).
	remoteDepts := []department{
		{ID: 1, Name: "HQ", ParentID: 0},
		{ID: 10, Name: "R&D", ParentID: 1},
		{ID: 20, Name: "Ops", ParentID: 1},
		{ID: 30, Name: "HR", ParentID: 1},
		{ID: 40, Name: "Finance", ParentID: 1},
	}
	remoteEmps := []employee{
		{ID: 1001, Name: "Zhang San", Mobile: "13800000001", DeptID: 10, DeptName: "R&D", JobNo: "KYX001", Status: "active", DingTalkID: "dt_zhangsan"},
		{ID: 1002, Name: "Li Si", Mobile: "13800000002", DeptID: 20, DeptName: "Ops", JobNo: "KYX002", Status: "active", DingTalkID: "dt_lisi"},
		{ID: 1003, Name: "Wang Wu", Mobile: "13800000003", DeptID: 30, DeptName: "HR", JobNo: "KYX003", Status: "active", DingTalkID: "dt_wangwu"},
		{ID: 1004, Name: "Zhao Liu", Mobile: "13800000004", DeptID: 40, DeptName: "Finance", JobNo: "KYX004", Status: "active", DingTalkID: "dt_zhaoliu"},
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	if body.Mode == "directory" || body.Mode == "full" {
		s.db.Departments = remoteDepts
		s.db.SeqDept = 40
	}
	upserted := 0
	if body.Mode == "roster" || body.Mode == "full" {
		byJob := map[string]int{}
		for i, e := range s.db.Employees {
			byJob[e.JobNo] = i
		}
		now := time.Now().Format(time.RFC3339)
		for _, re := range remoteEmps {
			re.UpdatedAt = now
			if idx, ok := byJob[re.JobNo]; ok {
				s.db.Employees[idx] = re
			} else {
				s.db.Employees = append(s.db.Employees, re)
			}
			upserted++
			if re.ID > s.db.SeqEmp {
				s.db.SeqEmp = re.ID
			}
		}
	}
	finished := time.Now().Format(time.RFC3339)
	if len(s.db.SyncJobs) > 0 && s.db.SyncJobs[0].ID == jobID {
		s.db.SyncJobs[0].Status = "success"
		s.db.SyncJobs[0].Message = "sync completed (simulated OpenAPI)"
		s.db.SyncJobs[0].FinishedAt = finished
		s.db.SyncJobs[0].Stats = map[string]int{"departments": len(s.db.Departments), "employeesUpserted": upserted}
	}
	_ = s.save()
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": s.db.SyncJobs[0]})
}

func (s *server) handleDingJobs(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": s.db.SyncJobs, "total": len(s.db.SyncJobs)})
}

func (s *server) handleDingStatus(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	var last any
	if len(s.db.SyncJobs) > 0 {
		last = s.db.SyncJobs[0]
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": map[string]any{
		"provider": "dingtalk",
		"mode":     "skeleton-simulated",
		"employees": len(s.db.Employees),
		"departments": len(s.db.Departments),
		"lastJob": last,
	}})
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
