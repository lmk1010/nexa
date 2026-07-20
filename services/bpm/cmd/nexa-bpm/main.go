// nexa-bpm — file-backed approval tasks (Go).
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

var version = "0.3.0-m4"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	DataDir string `json:"dataDir"`
}

type task struct {
	ID          string `json:"id"`
	Title       string `json:"title"`
	ProcessName string `json:"processName"`
	Starter     string `json:"starter"`
	Assignee    string `json:"assignee"`
	Status      string `json:"status"` // pending|approved|rejected
	Reason      string `json:"reason,omitempty"`
	CreatedAt   string `json:"createdAt"`
	UpdatedAt   string `json:"updatedAt"`
}

type db struct {
	Tasks []task `json:"tasks"`
	Seq   int    `json:"seq"`
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
	cfg := config{Name: "nexa-bpm", DataDir: "./data"}
	cfg.HTTP.Addr = ":48082"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48082"
	}
	if v := os.Getenv("NEXA_DATA_DIR"); v != "" {
		cfg.DataDir = v
	}
	if cfg.DataDir == "" {
		cfg.DataDir = "./data"
	}
	s := &server{cfg: cfg, path: filepath.Join(cfg.DataDir, "bpm.json")}
	if err := s.loadOrSeed(); err != nil {
		log.Fatalf("store: %v", err)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusOK, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/bpm/tasks/todo", s.handleTodo)
	mux.HandleFunc("/v1/bpm/tasks/done", s.handleDone)
	mux.HandleFunc("/v1/bpm/tasks/approve", s.handleApprove)
	mux.HandleFunc("/v1/bpm/tasks/start", s.handleStart)
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusOK, map[string]any{"service": cfg.Name, "version": version, "status": "file-backed", "apis": []string{
			"/v1/bpm/tasks/todo", "/v1/bpm/tasks/done", "/v1/bpm/tasks/approve", "/v1/bpm/tasks/start",
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
	now := time.Now()
	s.db = db{
		Tasks: []task{
			{ID: "t1", Title: "Leave request 3 days", ProcessName: "leave", Starter: "Zhang San", Assignee: "boss", Status: "pending", CreatedAt: now.Add(-2 * time.Hour).Format(time.RFC3339), UpdatedAt: now.Add(-2 * time.Hour).Format(time.RFC3339)},
			{ID: "t2", Title: "Purchase office supplies", ProcessName: "purchase", Starter: "Li Si", Assignee: "boss", Status: "pending", CreatedAt: now.Add(-5 * time.Hour).Format(time.RFC3339), UpdatedAt: now.Add(-5 * time.Hour).Format(time.RFC3339)},
		},
		Seq: 2,
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

func (s *server) handleTodo(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	out := make([]task, 0)
	for _, t := range s.db.Tasks {
		if t.Status == "pending" {
			out = append(out, t)
		}
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": out, "total": len(out)})
}

func (s *server) handleDone(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	out := make([]task, 0)
	for _, t := range s.db.Tasks {
		if t.Status != "pending" {
			out = append(out, t)
		}
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": out, "total": len(out)})
}

func (s *server) handleApprove(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"code": 405})
		return
	}
	var body struct {
		TaskID string `json:"taskId"`
		Action string `json:"action"` // approve|reject
		Reason string `json:"reason"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]any{"code": 400})
		return
	}
	if body.Action != "approve" && body.Action != "reject" {
		writeJSON(w, http.StatusBadRequest, map[string]any{"code": 400, "msg": "action must be approve|reject"})
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	for i := range s.db.Tasks {
		if s.db.Tasks[i].ID == body.TaskID {
			if body.Action == "approve" {
				s.db.Tasks[i].Status = "approved"
			} else {
				s.db.Tasks[i].Status = "rejected"
			}
			s.db.Tasks[i].Reason = body.Reason
			s.db.Tasks[i].UpdatedAt = time.Now().Format(time.RFC3339)
			_ = s.save()
			writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": s.db.Tasks[i]})
			return
		}
	}
	writeJSON(w, http.StatusNotFound, map[string]any{"code": 404, "msg": "task not found"})
}

func (s *server) handleStart(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"code": 405})
		return
	}
	var body struct {
		Title       string `json:"title"`
		ProcessName string `json:"processName"`
		Starter     string `json:"starter"`
		Assignee    string `json:"assignee"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]any{"code": 400})
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	s.db.Seq++
	now := time.Now().Format(time.RFC3339)
	t := task{
		ID:          "t" + time.Now().Format("150405"),
		Title:       body.Title,
		ProcessName: body.ProcessName,
		Starter:     body.Starter,
		Assignee:    body.Assignee,
		Status:      "pending",
		CreatedAt:   now,
		UpdatedAt:   now,
	}
	if t.Assignee == "" {
		t.Assignee = "boss"
	}
	s.db.Tasks = append(s.db.Tasks, t)
	_ = s.save()
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": t})
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
