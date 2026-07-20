from pathlib import Path


def w(path: str, content: str) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8")
    print("wrote", path)


def service_main(name: str, port: str, version: str, seed_and_routes: str) -> str:
    return f'''// nexa-{name} — file-backed domain service (Go).
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

var version = "{version}"

type config struct {{
	Name string `json:"name"`
	HTTP struct {{
		Addr string `json:"addr"`
	}} `json:"http"`
	DataDir string `json:"dataDir"`
}}

{seed_and_routes}
'''


BUSINESS_BODY = r'''
type todo struct {
	ID        string `json:"id"`
	Title     string `json:"title"`
	Assignee  string `json:"assignee"`
	Status    string `json:"status"`
	DueAt     string `json:"dueAt"`
	CreatedAt string `json:"createdAt"`
}
type workReq struct {
	ID        string `json:"id"`
	Title     string `json:"title"`
	Dept      string `json:"dept"`
	Priority  string `json:"priority"`
	Status    string `json:"status"`
	CreatedAt string `json:"createdAt"`
}
type calEvent struct {
	ID    string `json:"id"`
	Title string `json:"title"`
	Start string `json:"start"`
	End   string `json:"end"`
	Type  string `json:"type"`
}
type reception struct {
	ID      string `json:"id"`
	Visitor string `json:"visitor"`
	Purpose string `json:"purpose"`
	At      string `json:"at"`
}
type db struct {
	Todos      []todo      `json:"todos"`
	Reqs       []workReq   `json:"workRequirements"`
	Events     []calEvent  `json:"events"`
	Receptions []reception `json:"receptions"`
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
	cfg := config{Name: "nexa-business", DataDir: "./data"}
	cfg.HTTP.Addr = ":48084"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if v := os.Getenv("NEXA_DATA_DIR"); v != "" {
		cfg.DataDir = v
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48084"
	}
	if cfg.DataDir == "" {
		cfg.DataDir = "./data"
	}
	s := &server{cfg: cfg, path: filepath.Join(cfg.DataDir, "business.json")}
	if err := s.loadOrSeed(); err != nil {
		log.Fatalf("store: %v", err)
	}
	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/business/todos", s.handleTodos)
	mux.HandleFunc("/v1/business/work-requirements", s.handleReqs)
	mux.HandleFunc("/v1/business/calendar/events", s.handleEvents)
	mux.HandleFunc("/v1/business/reception/latest", s.handleReception)
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{"service": cfg.Name, "version": version, "status": "file-backed"})
	})
	runHTTP(cfg.HTTP.Addr, mux, cfg.Name, s.path)
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
	now := time.Now()
	s.db = db{
		Todos: []todo{{ID: "td1", Title: "Review weekly ops report", Assignee: "boss", Status: "open", DueAt: now.Add(24 * time.Hour).Format(time.RFC3339), CreatedAt: now.Add(-3 * time.Hour).Format(time.RFC3339)}},
		Reqs:  []workReq{{ID: "wr1", Title: "Q3 customer revisit campaign", Dept: "Ops", Priority: "high", Status: "active", CreatedAt: now.Add(-24 * time.Hour).Format(time.RFC3339)}},
		Events: []calEvent{{ID: "ev1", Title: "All-hands meeting", Start: now.Add(2 * time.Hour).Format(time.RFC3339), End: now.Add(3 * time.Hour).Format(time.RFC3339), Type: "meeting"}},
		Receptions: []reception{{ID: "rc1", Visitor: "Guest A", Purpose: "interview", At: now.Add(-40 * time.Minute).Format(time.RFC3339)}},
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
func (s *server) handleTodos(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodGet:
		s.mu.Lock(); defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Todos, "total": len(s.db.Todos)})
	case http.MethodPost:
		var body todo
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			writeJSON(w, 400, map[string]any{"code": 400}); return
		}
		s.mu.Lock(); defer s.mu.Unlock()
		if body.ID == "" { body.ID = "td" + time.Now().Format("150405") }
		if body.Status == "" { body.Status = "open" }
		if body.CreatedAt == "" { body.CreatedAt = time.Now().Format(time.RFC3339) }
		s.db.Todos = append(s.db.Todos, body)
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": body})
	default:
		writeJSON(w, 405, map[string]any{"code": 405})
	}
}
func (s *server) handleReqs(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock(); defer s.mu.Unlock()
	writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Reqs, "total": len(s.db.Reqs)})
}
func (s *server) handleEvents(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock(); defer s.mu.Unlock()
	writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Events, "total": len(s.db.Events)})
}
func (s *server) handleReception(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock(); defer s.mu.Unlock()
	writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Receptions})
}
func runHTTP(addr string, mux *http.ServeMux, name, dataPath string) {
	srv := &http.Server{Addr: addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] %s %s on %s data=%s", name, version, addr, dataPath)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
	}()
	<-ctx.Done()
	shCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shCtx)
}
func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
'''

ERP_BODY = r'''
type stockItem struct {
	SKU       string `json:"sku"`
	Name      string `json:"name"`
	Qty       int    `json:"qty"`
	Warehouse string `json:"warehouse"`
}
type purchaseOrder struct {
	ID        string  `json:"id"`
	Vendor    string  `json:"vendor"`
	Amount    float64 `json:"amount"`
	Status    string  `json:"status"`
	CreatedAt string  `json:"createdAt"`
}
type db struct {
	Stock     []stockItem     `json:"stock"`
	Purchases []purchaseOrder `json:"purchases"`
}
type server struct {
	cfg config
	mu sync.Mutex
	db db
	path string
}
func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-erp", DataDir: "./data"}
	cfg.HTTP.Addr = ":48085"
	if raw, err := os.ReadFile(*cfgPath); err == nil { _ = json.Unmarshal(raw, &cfg) }
	if v := os.Getenv("NEXA_DATA_DIR"); v != "" { cfg.DataDir = v }
	if cfg.HTTP.Addr == "" { cfg.HTTP.Addr = ":48085" }
	if cfg.DataDir == "" { cfg.DataDir = "./data" }
	s := &server{cfg: cfg, path: filepath.Join(cfg.DataDir, "erp.json")}
	if err := s.loadOrSeed(); err != nil { log.Fatalf("store: %v", err) }
	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) { writeJSON(w, 200, map[string]any{"status":"UP","service":cfg.Name,"version":version}) })
	mux.HandleFunc("/v1/erp/stock/summary", func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock(); defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code":0,"data":s.db.Stock,"total":len(s.db.Stock)})
	})
	mux.HandleFunc("/v1/erp/purchase/orders", func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock(); defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code":0,"data":s.db.Purchases,"total":len(s.db.Purchases)})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) { writeJSON(w, 200, map[string]any{"service":cfg.Name,"version":version,"status":"file-backed"}) })
	runHTTP(cfg.HTTP.Addr, mux, cfg.Name, s.path)
}
func (s *server) loadOrSeed() error {
	_ = os.MkdirAll(filepath.Dir(s.path), 0o755)
	raw, err := os.ReadFile(s.path)
	if err == nil { return json.Unmarshal(raw, &s.db) }
	if !errors.Is(err, os.ErrNotExist) { return err }
	s.db = db{
		Stock: []stockItem{{SKU:"SKU-001",Name:"Widget A",Qty:120,Warehouse:"WH-East"},{SKU:"SKU-002",Name:"Widget B",Qty:45,Warehouse:"WH-East"}},
		Purchases: []purchaseOrder{{ID:"PO-1001",Vendor:"Supplier X",Amount:3200.5,Status:"open",CreatedAt:time.Now().Add(-72*time.Hour).Format(time.RFC3339)}},
	}
	return s.save()
}
func (s *server) save() error {
	raw, err := json.MarshalIndent(s.db, "", "  ")
	if err != nil { return err }
	tmp := s.path + ".tmp"
	if err := os.WriteFile(tmp, raw, 0o644); err != nil { return err }
	return os.Rename(tmp, s.path)
}
func runHTTP(addr string, mux *http.ServeMux, name, dataPath string) {
	srv := &http.Server{Addr: addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] %s %s on %s data=%s", name, version, addr, dataPath)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed { log.Fatalf("http: %v", err) }
	}()
	<-ctx.Done()
	shCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shCtx)
}
func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
'''

FINANCE_BODY = r'''
type ledgerItem struct {
	ID     string  `json:"id"`
	Title  string  `json:"title"`
	Amount float64 `json:"amount"`
	At     string  `json:"at"`
}
type monthSummary struct {
	Month   string  `json:"month"`
	Income  float64 `json:"income"`
	Expense float64 `json:"expense"`
	Profit  float64 `json:"profit"`
}
type db struct {
	Summary monthSummary `json:"summary"`
	Ledger  []ledgerItem `json:"ledger"`
}
type server struct {
	cfg config
	mu sync.Mutex
	db db
	path string
}
func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-finance", DataDir: "./data"}
	cfg.HTTP.Addr = ":48086"
	if raw, err := os.ReadFile(*cfgPath); err == nil { _ = json.Unmarshal(raw, &cfg) }
	if v := os.Getenv("NEXA_DATA_DIR"); v != "" { cfg.DataDir = v }
	if cfg.HTTP.Addr == "" { cfg.HTTP.Addr = ":48086" }
	if cfg.DataDir == "" { cfg.DataDir = "./data" }
	s := &server{cfg: cfg, path: filepath.Join(cfg.DataDir, "finance.json")}
	if err := s.loadOrSeed(); err != nil { log.Fatalf("store: %v", err) }
	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) { writeJSON(w, 200, map[string]any{"status":"UP","service":cfg.Name,"version":version}) })
	mux.HandleFunc("/v1/finance/summary/monthly", func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock(); defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code":0,"data":s.db.Summary})
	})
	mux.HandleFunc("/v1/finance/ledger/recent", func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock(); defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code":0,"data":s.db.Ledger,"total":len(s.db.Ledger)})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) { writeJSON(w, 200, map[string]any{"service":cfg.Name,"version":version,"status":"file-backed"}) })
	runHTTP(cfg.HTTP.Addr, mux, cfg.Name, s.path)
}
func (s *server) loadOrSeed() error {
	_ = os.MkdirAll(filepath.Dir(s.path), 0o755)
	raw, err := os.ReadFile(s.path)
	if err == nil { return json.Unmarshal(raw, &s.db) }
	if !errors.Is(err, os.ErrNotExist) { return err }
	s.db = db{
		Summary: monthSummary{Month: time.Now().Format("2006-01"), Income: 128000, Expense: 76450.5, Profit: 51549.5},
		Ledger: []ledgerItem{
			{ID:"L1", Title:"Office rent", Amount:-12000, At:time.Now().Add(-48*time.Hour).Format(time.RFC3339)},
			{ID:"L2", Title:"Service revenue", Amount:35000, At:time.Now().Add(-24*time.Hour).Format(time.RFC3339)},
		},
	}
	return s.save()
}
func (s *server) save() error {
	raw, err := json.MarshalIndent(s.db, "", "  ")
	if err != nil { return err }
	tmp := s.path + ".tmp"
	if err := os.WriteFile(tmp, raw, 0o644); err != nil { return err }
	return os.Rename(tmp, s.path)
}
func runHTTP(addr string, mux *http.ServeMux, name, dataPath string) {
	srv := &http.Server{Addr: addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] %s %s on %s data=%s", name, version, addr, dataPath)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed { log.Fatalf("http: %v", err) }
	}()
	<-ctx.Done()
	shCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shCtx)
}
func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
'''

# Write full files with package preamble
preamble = '''package main

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

'''

w("E:/code/nexa/services/business/cmd/nexa-business/main.go",
  "// nexa-business — file-backed todos/work/calendar/reception (Go).\n" + preamble + 'var version = "0.3.0-m5"\n\ntype config struct {\n\tName string `json:"name"`\n\tHTTP struct {\n\t\tAddr string `json:"addr"`\n\t} `json:"http"`\n\tDataDir string `json:"dataDir"`\n}\n' + BUSINESS_BODY)

w("E:/code/nexa/services/erp/cmd/nexa-erp/main.go",
  "// nexa-erp — file-backed stock/purchase (Go).\n" + preamble + 'var version = "0.3.0-m6"\n\ntype config struct {\n\tName string `json:"name"`\n\tHTTP struct {\n\t\tAddr string `json:"addr"`\n\t} `json:"http"`\n\tDataDir string `json:"dataDir"`\n}\n' + ERP_BODY)

w("E:/code/nexa/services/finance/cmd/nexa-finance/main.go",
  "// nexa-finance — file-backed summary/ledger (Go).\n" + preamble + 'var version = "0.3.0-m6"\n\ntype config struct {\n\tName string `json:"name"`\n\tHTTP struct {\n\t\tAddr string `json:"addr"`\n\t} `json:"http"`\n\tDataDir string `json:"dataDir"`\n}\n' + FINANCE_BODY)

print("done")
