// nexa-ai — AI (Go-only enterprise platform service).
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

var version = "0.1.0-skeleton"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	Log struct {
		Level string `json:"level"`
	} `json:"log"`
	MySQL struct {
		DSN string `json:"dsn"`
	} `json:"mysql"`
}

func loadConfig(path string) config {
	cfg := config{Name: "nexa-ai"}
	cfg.HTTP.Addr = ":48089"
	cfg.Log.Level = "info"
	raw, err := os.ReadFile(path)
	if err != nil {
		if !os.IsNotExist(err) {
			log.Printf("config read: %v (using defaults)", err)
		}
		return cfg
	}
	if err := json.Unmarshal(raw, &cfg); err != nil {
		log.Fatalf("config parse: %v", err)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48089"
	}
	if cfg.Name == "" {
		cfg.Name = "nexa-ai"
	}
	return cfg
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path (JSON)")
	flag.Parse()
	cfg := loadConfig(*cfgPath)

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{
			"service":  cfg.Name,
			"title":    "AI",
			"version":  version,
			"status":   "skeleton",
			"runtime":  "go",
			"note":     "Go rewrite only; domain APIs module-by-module",
		})
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
