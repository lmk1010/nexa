// nexa-business — todos, work requirements, calendar, reception (Go).
package main

import (
	"context"
	"encoding/json"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"sync"
	"syscall"
	"time"
)

var version = "0.2.0-m5-partial"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
}

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

var (
	mu    sync.RWMutex
	todos = []todo{
		{ID: "td1", Title: "Review weekly ops report", Assignee: "boss", Status: "open", DueAt: time.Now().Add(24 * time.Hour).Format(time.RFC3339), CreatedAt: time.Now().Add(-3 * time.Hour).Format(time.RFC3339)},
		{ID: "td2", Title: "Confirm onboarding checklist", Assignee: "hr", Status: "open", DueAt: time.Now().Add(48 * time.Hour).Format(time.RFC3339), CreatedAt: time.Now().Add(-6 * time.Hour).Format(time.RFC3339)},
	}
	reqs = []workReq{
		{ID: "wr1", Title: "Q3 customer revisit campaign", Dept: "Ops", Priority: "high", Status: "active", CreatedAt: time.Now().Add(-24 * time.Hour).Format(time.RFC3339)},
	}
	events = []calEvent{
		{ID: "ev1", Title: "All-hands meeting", Start: time.Now().Add(2 * time.Hour).Format(time.RFC3339), End: time.Now().Add(3 * time.Hour).Format(time.RFC3339), Type: "meeting"},
	}
)

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-business"}
	cfg.HTTP.Addr = ":48084"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48084"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/business/todos", handleTodos)
	mux.HandleFunc("/v1/business/work-requirements", handleReqs)
	mux.HandleFunc("/v1/business/calendar/events", handleEvents)
	mux.HandleFunc("/v1/business/reception/latest", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"id": "rc1", "visitor": "Guest A", "purpose": "interview", "at": time.Now().Add(-40 * time.Minute).Format(time.RFC3339)},
		}})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "apis": []string{
			"/v1/business/todos", "/v1/business/work-requirements", "/v1/business/calendar/events", "/v1/business/reception/latest",
		}})
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

func handleTodos(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodGet:
		mu.RLock()
		defer mu.RUnlock()
		writeJSON(w, map[string]any{"code": 0, "data": todos, "total": len(todos)})
	case http.MethodPost:
		var body todo
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			http.Error(w, `{"code":400}`, http.StatusBadRequest)
			return
		}
		if body.ID == "" {
			body.ID = "td" + time.Now().Format("150405")
		}
		if body.Status == "" {
			body.Status = "open"
		}
		if body.CreatedAt == "" {
			body.CreatedAt = time.Now().Format(time.RFC3339)
		}
		mu.Lock()
		todos = append(todos, body)
		mu.Unlock()
		writeJSON(w, map[string]any{"code": 0, "data": body})
	default:
		http.Error(w, `{"code":405}`, http.StatusMethodNotAllowed)
	}
}

func handleReqs(w http.ResponseWriter, r *http.Request) {
	mu.RLock()
	defer mu.RUnlock()
	writeJSON(w, map[string]any{"code": 0, "data": reqs, "total": len(reqs)})
}

func handleEvents(w http.ResponseWriter, r *http.Request) {
	mu.RLock()
	defer mu.RUnlock()
	writeJSON(w, map[string]any{"code": 0, "data": events, "total": len(events)})
}

func writeJSON(w http.ResponseWriter, v any) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(v)
}
