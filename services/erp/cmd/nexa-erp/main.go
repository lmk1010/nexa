// nexa-erp — file-backed stock/purchase (Go).
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
