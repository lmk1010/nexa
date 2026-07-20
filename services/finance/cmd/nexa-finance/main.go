// nexa-finance — file-backed summary/ledger (Go).
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

var version = "0.3.0-m6"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	DataDir string `json:"dataDir"`
}

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
