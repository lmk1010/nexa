// nexa-core — single business process: gateway edge + all product domains.
// IAM stays separate (auth). Agent stays Node. CDC optional separate.
package main

import (
	"bytes"
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"sync"
	"syscall"
	"time"
)

var version = "1.0.0-core"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	DataDir  string `json:"dataDir"`
	IAMURL   string `json:"iamUrl"`
	AgentURL string `json:"agentUrl"`
	Auth     struct {
		Enabled bool `json:"enabled"`
	} `json:"auth"`
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-core", IAMURL: "http://127.0.0.1:48081", AgentURL: "http://127.0.0.1:48091"}
	cfg.HTTP.Addr = ":48080"
	cfg.DataDir = "./data"
	cfg.Auth.Enabled = true
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if v := os.Getenv("NEXA_DATA_DIR"); v != "" {
		cfg.DataDir = v
	}
	if v := os.Getenv("NEXA_IAM_URL"); v != "" {
		cfg.IAMURL = v
	}
	if v := os.Getenv("NEXA_AGENT_URL"); v != "" {
		cfg.AgentURL = v
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48080"
	}
	_ = os.MkdirAll(cfg.DataDir, 0o755)

	store := newStore(cfg.DataDir)
	store.loadOrSeed()

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{"status": "UP", "service": "nexa-core", "version": version, "mode": "monolith-business"})
	})
	mux.HandleFunc("/v1/platform/services", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{
			"service": "nexa-core",
			"version": version,
			"processes": []map[string]any{
				{"name": "core", "port": 48080, "hosts": []string{"gateway", "hr", "bpm", "business", "erp", "finance", "im", "op", "ai", "data-center"}},
				{"name": "iam", "port": 48081, "hosts": []string{"auth", "tenant"}},
				{"name": "agent", "port": 48091, "hosts": []string{"neox"}},
			},
			"note": "business domains merged into nexa-core to save resources",
		})
	})

	// Proxy IAM + Agent
	registerProxy(mux, "/v1/iam", cfg.IAMURL, false)
	registerProxy(mux, "/app-api/system", cfg.IAMURL, false)
	registerProxy(mux, "/admin-api/system", cfg.IAMURL, false)
	registerProxy(mux, "/agent", cfg.AgentURL, true)
	registerProxy(mux, "/app-api/agent", cfg.AgentURL, true)
	registerProxy(mux, "/admin-api/agent", cfg.AgentURL, true)

	// Domain routes (in-process)
	registerHR(mux, store)
	registerBPM(mux, store)
	registerBusiness(mux, store)
	registerERP(mux, store)
	registerFinance(mux, store)
	registerIM(mux, store)
	registerOP(mux, store)
	registerAI(mux, store)
	registerDataCenter(mux, store)

	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/" {
			writeJSON(w, 200, map[string]any{
				"product": "nexa",
				"tagline": "joinable enterprise DingTalk",
				"service": "nexa-core",
				"version": version,
				"docs":    []string{"README.md", "docs/PRODUCT.md"},
			})
			return
		}
		http.NotFound(w, r)
	})

	handler := http.Handler(mux)
	if cfg.Auth.Enabled {
		handler = authMiddleware(cfg.IAMURL, mux)
	}

	srv := &http.Server{Addr: cfg.HTTP.Addr, Handler: withLog(handler), ReadHeaderTimeout: 10 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] nexa-core %s on %s data=%s iam=%s", version, cfg.HTTP.Addr, cfg.DataDir, cfg.IAMURL)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
	}()
	<-ctx.Done()
	sh, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(sh)
}

func registerProxy(mux *http.ServeMux, prefix, target string, strip bool) {
	u, err := url.Parse(target)
	if err != nil {
		log.Printf("bad proxy target %s: %v", target, err)
		return
	}
	p := httputil.NewSingleHostReverseProxy(u)
	base := p.Director
	p.Director = func(req *http.Request) {
		base(req)
		req.Host = u.Host
		if strip {
			rest := strings.TrimPrefix(req.URL.Path, prefix)
			if rest == "" {
				rest = "/"
			}
			if !strings.HasPrefix(rest, "/") {
				rest = "/" + rest
			}
			req.URL.Path = rest
		}
	}
	p.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		writeJSON(w, 502, map[string]any{"code": 502, "msg": "upstream unavailable", "path": r.URL.Path, "error": err.Error()})
	}
	mux.HandleFunc(prefix+"/", p.ServeHTTP)
	mux.HandleFunc(prefix, p.ServeHTTP)
}

func authMiddleware(iamURL string, next http.Handler) http.Handler {
	publicExact := map[string]bool{
		"/healthz": true, "/v1/platform/services": true, "/": true,
		"/v1/iam/login": true, "/v1/iam/tenants/register": true, "/v1/iam/invites/accept": true,
		"/app-api/system/auth/login": true, "/admin-api/system/auth/login": true,
		"/v1/ai/skills": true, "/v1/ai/intent/route": true, "/v1/ai/assistant/bootstrap": true,
		"/v1/ai/connectors": true, "/v1/ai/sense": true,
	}
	publicPrefix := []string{"/agent", "/app-api/agent", "/admin-api/agent", "/v1/iam/login", "/v1/iam/tenants/register", "/v1/iam/invites/accept"}
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		path := r.URL.Path
		if publicExact[path] {
			next.ServeHTTP(w, r)
			return
		}
		for _, p := range publicPrefix {
			if path == p || strings.HasPrefix(path, p+"/") {
				next.ServeHTTP(w, r)
				return
			}
		}
		// IAM proxied paths that need auth still go through - login/register already public
		if strings.HasPrefix(path, "/v1/iam/") && (path == "/v1/iam/login" || path == "/v1/iam/tenants/register" || path == "/v1/iam/invites/accept") {
			next.ServeHTTP(w, r)
			return
		}
		tok := strings.TrimSpace(strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer"))
		tok = strings.TrimSpace(tok)
		if tok == "" {
			tok = r.Header.Get("token")
		}
		if tok == "" {
			writeJSON(w, 401, map[string]any{"code": 401, "msg": "unauthorized"})
			return
		}
		user, ok := introspect(iamURL, tok)
		if !ok {
			writeJSON(w, 401, map[string]any{"code": 401, "msg": "unauthorized"})
			return
		}
		if tid, ok := user["tenantId"].(float64); ok {
			r.Header.Set("X-Tenant-Id", fmt.Sprintf("%d", int64(tid)))
		}
		if uid, ok := user["id"].(float64); ok {
			r.Header.Set("X-User-Id", fmt.Sprintf("%d", int64(uid)))
		}
		if un, ok := user["username"].(string); ok {
			r.Header.Set("X-Username", un)
		}
		next.ServeHTTP(w, r)
	})
}

func introspect(iamURL, tok string) (map[string]any, bool) {
	body, _ := json.Marshal(map[string]string{"token": tok})
	req, err := http.NewRequest(http.MethodPost, strings.TrimRight(iamURL, "/")+"/v1/iam/token/introspect", bytes.NewReader(body))
	if err != nil {
		return nil, false
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, false
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	var out struct {
		Code int `json:"code"`
		Data struct {
			Active bool           `json:"active"`
			User   map[string]any `json:"user"`
		} `json:"data"`
	}
	if json.Unmarshal(raw, &out) != nil || out.Code != 0 || !out.Data.Active {
		return nil, false
	}
	return out.Data.User, true
}

func withLog(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		next.ServeHTTP(w, r)
		log.Printf("%s %s %s", r.Method, r.URL.Path, time.Since(start).Truncate(time.Millisecond))
	})
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func tenantID(r *http.Request) int64 {
	for _, k := range []string{"X-Tenant-Id", "tenant-id"} {
		if v := strings.TrimSpace(r.Header.Get(k)); v != "" {
			var n int64
			fmt.Sscan(v, &n)
			return n
		}
	}
	return 0
}

// ---------- store ----------

type store struct {
	dir string
	mu  sync.Mutex
	db  coreDB
}

type coreDB struct {
	Employees     []employee              `json:"employees"`
	Departments   []department            `json:"departments"`
	Tasks         []task                  `json:"tasks"`
	Todos         []todo                  `json:"todos"`
	Stock         []stockItem             `json:"stock"`
	Purchases     []purchase              `json:"purchases"`
	Ledger        []ledger                `json:"ledger"`
	Conversations []conv                  `json:"conversations"`
	Messages      []msg                   `json:"messages"`
	Sense         []senseEv               `json:"sense"`
	Rules         []autoRule              `json:"rules"`
	ExportJobs    []exportJob             `json:"exportJobs"`
	SyncJobs      []syncJob               `json:"syncJobs"`
	Connectors    map[string]connectorCfg `json:"connectors"`
	Seq           int64                   `json:"seq"`
}

func newStore(dir string) *store { return &store{dir: dir} }

func (s *store) path() string { return filepath.Join(s.dir, "core.json") }

func (s *store) loadOrSeed() {
	_ = os.MkdirAll(s.dir, 0o755)
	if raw, err := os.ReadFile(s.path()); err == nil {
		_ = json.Unmarshal(raw, &s.db)
		if s.db.Connectors == nil {
			s.db.Connectors = map[string]connectorCfg{}
		}
		return
	}
	now := time.Now()
	s.db = coreDB{
		Departments: []department{{ID: 1, TenantID: 1, Name: "HQ", ParentID: 0}, {ID: 10, TenantID: 1, Name: "R&D", ParentID: 1}, {ID: 20, TenantID: 1, Name: "Ops", ParentID: 1}, {ID: 30, TenantID: 1, Name: "HR", ParentID: 1}},
		Employees: []employee{
			{ID: 1001, TenantID: 1, Name: "Zhang San", Mobile: "13800000001", DeptID: 10, DeptName: "R&D", JobNo: "NEXA001", Status: "active", UpdatedAt: now.Format(time.RFC3339)},
			{ID: 1002, TenantID: 1, Name: "Li Si", Mobile: "13800000002", DeptID: 20, DeptName: "Ops", JobNo: "NEXA002", Status: "active", UpdatedAt: now.Format(time.RFC3339)},
		},
		Tasks: []task{
			{ID: "t1", TenantID: 1, Title: "Leave request", ProcessName: "leave", Starter: "Zhang San", Assignee: "boss", Status: "pending", CreatedAt: now.Add(-2 * time.Hour).Format(time.RFC3339), UpdatedAt: now.Add(-2 * time.Hour).Format(time.RFC3339)},
		},
		Todos:         []todo{{ID: "td1", TenantID: 1, Title: "Review weekly report", Assignee: "boss", Status: "open", DueAt: now.Add(24 * time.Hour).Format(time.RFC3339), CreatedAt: now.Format(time.RFC3339)}},
		Stock:         []stockItem{{TenantID: 1, SKU: "SKU-001", Name: "Widget A", Qty: 120, Warehouse: "WH-East"}, {TenantID: 1, SKU: "SKU-002", Name: "Widget B", Qty: 45, Warehouse: "WH-East"}},
		Purchases:     []purchase{{ID: "PO-1", TenantID: 1, Vendor: "Supplier X", Amount: 3200.5, Status: "open", CreatedAt: now.Add(-48 * time.Hour).Format(time.RFC3339)}},
		Ledger:        []ledger{{ID: "L1", TenantID: 1, Title: "Service revenue", Amount: 35000, At: now.Add(-24 * time.Hour).Format(time.RFC3339)}},
		Conversations: []conv{{ID: "c1", TenantID: 1, Title: "Ops group", Unread: 0, UpdatedAt: now.Format(time.RFC3339)}},
		Messages:      []msg{},
		Sense:         []senseEv{},
		Rules: []autoRule{
			{ID: "rule_bpm", Name: "审批超时", Enabled: true, SenseType: "bpm.task.overdue", Actions: []string{"notify:assignee"}},
			{ID: "rule_join", Name: "入职待办", Enabled: true, SenseType: "hr.employee.joined", Actions: []string{"biz.todo.create"}},
		},
		Connectors: map[string]connectorCfg{},
		Seq:        1002,
	}
	_ = s.save()
}

func (s *store) save() error {
	raw, err := json.MarshalIndent(s.db, "", "  ")
	if err != nil {
		return err
	}
	tmp := s.path() + ".tmp"
	if err := os.WriteFile(tmp, raw, 0o644); err != nil {
		return err
	}
	return os.Rename(tmp, s.path())
}

// domain types
type employee struct {
	ID         int64  `json:"id"`
	TenantID   int64  `json:"tenantId,omitempty"`
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
	TenantID int64  `json:"tenantId,omitempty"`
	Name     string `json:"name"`
	ParentID int64  `json:"parentId"`
}
type task struct {
	ID          string `json:"id"`
	TenantID    int64  `json:"tenantId,omitempty"`
	Title       string `json:"title"`
	ProcessName string `json:"processName"`
	Starter     string `json:"starter"`
	Assignee    string `json:"assignee"`
	Status      string `json:"status"`
	Reason      string `json:"reason,omitempty"`
	CreatedAt   string `json:"createdAt"`
	UpdatedAt   string `json:"updatedAt"`
}
type todo struct {
	ID                                        string `json:"id"`
	TenantID                                  int64  `json:"tenantId,omitempty"`
	Title, Assignee, Status, DueAt, CreatedAt string
}
type stockItem struct {
	TenantID  int64  `json:"tenantId,omitempty"`
	SKU       string `json:"sku"`
	Name      string `json:"name"`
	Qty       int    `json:"qty"`
	Warehouse string `json:"warehouse"`
}
type purchase struct {
	ID        string  `json:"id"`
	TenantID  int64   `json:"tenantId,omitempty"`
	Vendor    string  `json:"vendor"`
	Amount    float64 `json:"amount"`
	Status    string  `json:"status"`
	CreatedAt string  `json:"createdAt"`
}
type ledger struct {
	ID       string  `json:"id"`
	TenantID int64   `json:"tenantId,omitempty"`
	Title    string  `json:"title"`
	Amount   float64 `json:"amount"`
	At       string  `json:"at"`
}
type conv struct {
	ID        string `json:"id"`
	TenantID  int64  `json:"tenantId,omitempty"`
	Title     string `json:"title"`
	Unread    int    `json:"unread"`
	UpdatedAt string `json:"updatedAt"`
}
type msg struct {
	ID             string `json:"id"`
	TenantID       int64  `json:"tenantId,omitempty"`
	ConversationID string `json:"conversationId"`
	From           string `json:"from"`
	Text           string `json:"text"`
	At             string `json:"at"`
}
type senseEv struct {
	ID       string         `json:"id"`
	TenantID int64          `json:"tenantId,omitempty"`
	Type     string         `json:"type"`
	Source   string         `json:"source"`
	Severity string         `json:"severity"`
	Message  string         `json:"message"`
	At       string         `json:"at"`
	Handled  bool           `json:"handled"`
	Actions  []string       `json:"actions,omitempty"`
	Payload  map[string]any `json:"payload,omitempty"`
}
type autoRule struct {
	ID        string
	Name      string
	Enabled   bool
	SenseType string
	Actions   []string
}
type exportJob struct {
	ID         string `json:"id"`
	TenantID   int64  `json:"tenantId,omitempty"`
	TemplateID string `json:"templateId"`
	State      string `json:"state"`
	CreatedAt  string `json:"createdAt"`
	Message    string `json:"message,omitempty"`
	Download   string `json:"download,omitempty"`
}
type syncJob struct {
	ID         string         `json:"id"`
	TenantID   int64          `json:"tenantId,omitempty"`
	Type       string         `json:"type"`
	Status     string         `json:"status"`
	Message    string         `json:"message"`
	StartedAt  string         `json:"startedAt"`
	FinishedAt string         `json:"finishedAt,omitempty"`
	Stats      map[string]int `json:"stats,omitempty"`
}

type connectorCfg struct {
	ID        string         `json:"id"`
	TenantID  int64          `json:"tenantId"`
	Enabled   bool           `json:"enabled"`
	Config    map[string]any `json:"config,omitempty"`
	UpdatedAt string         `json:"updatedAt"`
}

func matchTenant(tid, row int64) bool {
	if tid == 0 {
		return true
	}
	return row == 0 || row == tid
}

func jsonAlias(mux *http.ServeMux, paths []string, h http.HandlerFunc) {
	for _, p := range paths {
		mux.HandleFunc(p, h)
	}
}
