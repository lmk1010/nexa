// nexa-bpm — approvals (Go rewrite, thin state model first).
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

var version = "0.2.0-m4-partial"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
}

type task struct {
	ID          string `json:"id"`
	Title       string `json:"title"`
	ProcessName string `json:"processName"`
	Starter     string `json:"starter"`
	Status      string `json:"status"`
	CreatedAt   string `json:"createdAt"`
}

var demoTasks = []task{
	{ID: "t1", Title: "Leave request 3 days", ProcessName: "leave", Starter: "Zhang San", Status: "pending", CreatedAt: time.Now().Add(-2 * time.Hour).Format(time.RFC3339)},
	{ID: "t2", Title: "Purchase office supplies", ProcessName: "purchase", Starter: "Li Si", Status: "pending", CreatedAt: time.Now().Add(-5 * time.Hour).Format(time.RFC3339)},
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-bpm"}
	cfg.HTTP.Addr = ":48082"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48082"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/bpm/tasks/todo", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": demoTasks, "total": len(demoTasks)})
	})
	mux.HandleFunc("/v1/bpm/tasks/approve", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, `{"code":405}`, http.StatusMethodNotAllowed)
			return
		}
		var body struct {
			TaskID string `json:"taskId"`
			Action string `json:"action"`
			Reason string `json:"reason"`
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		writeJSON(w, map[string]any{"code": 0, "data": map[string]any{"taskId": body.TaskID, "action": body.Action, "status": "done"}})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "apis": []string{"/v1/bpm/tasks/todo", "/v1/bpm/tasks/approve"}})
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
