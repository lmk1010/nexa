// nexa-op — ops monitor / release / audit (Go).
package main

import (
	"context"
	"encoding/json"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"runtime"
	"syscall"
	"time"
)

var version = "0.2.0-m6-partial"
var started = time.Now()

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-op"}
	cfg.HTTP.Addr = ":48088"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48088"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/op/status", func(w http.ResponseWriter, r *http.Request) {
		var ms runtime.MemStats
		runtime.ReadMemStats(&ms)
		writeJSON(w, map[string]any{"code": 0, "data": map[string]any{
			"uptimeSec": int(time.Since(started).Seconds()),
			"goVersion": runtime.Version(),
			"goroutines": runtime.NumGoroutine(),
			"memAllocMB": float64(ms.Alloc) / 1024 / 1024,
			"services": []map[string]any{
				{"name": "gateway", "port": 48080, "expected": true},
				{"name": "iam", "port": 48081, "expected": true},
				{"name": "bpm", "port": 48082, "expected": true},
				{"name": "hr", "port": 48083, "expected": true},
				{"name": "business", "port": 48084, "expected": true},
				{"name": "agent", "port": 48091, "expected": true},
				{"name": "data-center", "port": 48092, "expected": true},
				{"name": "cdc-mysql", "port": 6060, "expected": true},
			},
		}})
	})
	mux.HandleFunc("/v1/op/audit/recent", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"id": "a1", "actor": "boss", "action": "login", "at": time.Now().Add(-10 * time.Minute).Format(time.RFC3339)},
			{"id": "a2", "actor": "admin", "action": "view_employees", "at": time.Now().Add(-5 * time.Minute).Format(time.RFC3339)},
		}})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "apis": []string{"/v1/op/status", "/v1/op/audit/recent"}})
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
