// nexa-hr — org and employee (Go rewrite).
package main

import (
	"context"
	"encoding/json"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"
)

var version = "0.2.0-m3-partial"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
}

type employee struct {
	ID       int64  `json:"id"`
	Name     string `json:"name"`
	Mobile   string `json:"mobile"`
	DeptName string `json:"deptName"`
	JobNo    string `json:"jobNo"`
	Status   string `json:"status"`
}

var demoEmployees = []employee{
	{ID: 1001, Name: "Zhang San", Mobile: "13800000001", DeptName: "R&D", JobNo: "KYX001", Status: "active"},
	{ID: 1002, Name: "Li Si", Mobile: "13800000002", DeptName: "Ops", JobNo: "KYX002", Status: "active"},
	{ID: 1003, Name: "Wang Wu", Mobile: "13800000003", DeptName: "HR", JobNo: "KYX003", Status: "active"},
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-hr"}
	cfg.HTTP.Addr = ":48083"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48083"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/hr/employees", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, `{"code":405}`, http.StatusMethodNotAllowed)
			return
		}
		writeJSON(w, map[string]any{"code": 0, "data": demoEmployees, "total": len(demoEmployees)})
	})
	mux.HandleFunc("/v1/hr/departments/tree", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"id": 1, "name": "HQ", "children": []map[string]any{
				{"id": 10, "name": "R&D"},
				{"id": 20, "name": "Ops"},
				{"id": 30, "name": "HR"},
			}},
		}})
	})
	mux.HandleFunc("/app-api/hr/employees", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": demoEmployees})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "apis": []string{"/v1/hr/employees", "/v1/hr/departments/tree"}})
	})

	srv := &http.Server{Addr: cfg.HTTP.Addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] %s %s on %s", cfg.Name, version, cfg.HTTP.Addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
	}()
	<-ctx.Done()
	shCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shCtx)
}

func writeJSON(w http.ResponseWriter, v any) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(v)
}
