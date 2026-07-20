// nexa-dc-lite — stdlib data-center surface for local/dev without warehouse deps.
// Production path remains cmd/kyx-data-center (full Go export engine).
package main

import (
	"context"
	"encoding/json"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"sync"
	"syscall"
	"time"
)

var version = "0.1.0-lite"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	TemplatesDir string `json:"templatesDir"`
	DataDir      string `json:"dataDir"`
}

type templateMeta struct {
	ID          string `json:"id"`
	Label       string `json:"label"`
	Category    string `json:"category"`
	Description string `json:"description"`
}

type job struct {
	ID         string         `json:"id"`
	TemplateID string         `json:"templateId"`
	State      string         `json:"state"`
	Filters    map[string]any `json:"filters,omitempty"`
	CreatedAt  string         `json:"createdAt"`
	UpdatedAt  string         `json:"updatedAt"`
	Message    string         `json:"message,omitempty"`
	Download   string         `json:"download,omitempty"`
}

type server struct {
	cfg   config
	mu    sync.Mutex
	tpls  []templateMeta
	jobs  []job
	seq   int
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-data-center", TemplatesDir: "./templates", DataDir: "./data"}
	cfg.HTTP.Addr = ":48092"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if v := os.Getenv("NEXA_DATA_DIR"); v != "" {
		cfg.DataDir = v
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48092"
	}
	if cfg.TemplatesDir == "" {
		cfg.TemplatesDir = "./templates"
	}

	s := &server{cfg: cfg}
	s.tpls = loadTemplates(cfg.TemplatesDir)
	_ = os.MkdirAll(cfg.DataDir, 0o755)

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{"status": "UP", "service": cfg.Name, "version": version, "mode": "lite", "templates": len(s.tpls)})
	})
	// lite paths (gateway strip /v1/data-center -> /...)
	mux.HandleFunc("/templates", s.handleTemplates)
	mux.HandleFunc("/jobs", s.handleJobs)
	mux.HandleFunc("/hall", s.handleHall)
	// also accept /v1/data-center/* if not stripped
	mux.HandleFunc("/v1/data-center/templates", s.handleTemplates)
	mux.HandleFunc("/v1/data-center/jobs", s.handleJobs)
	mux.HandleFunc("/v1/data-center/hall", s.handleHall)
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{
			"service": cfg.Name, "version": version, "mode": "lite",
			"apis": []string{"/templates", "/jobs", "/hall"},
			"note": "lite export simulator; full engine is cmd/kyx-data-center",
		})
	})

	srv := &http.Server{Addr: cfg.HTTP.Addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] %s %s on %s templates=%d", cfg.Name, version, cfg.HTTP.Addr, len(s.tpls))
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
	}()
	<-ctx.Done()
	shCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shCtx)
}

func loadTemplates(dir string) []templateMeta {
	entries, err := os.ReadDir(dir)
	if err != nil {
		return []templateMeta{
			{ID: "order", Label: "订单明细", Category: "order", Description: "订单导出模板"},
			{ID: "pf_record", Label: "赔付明细", Category: "pf", Description: "赔付导出模板"},
			{ID: "work_ticket", Label: "工单明细", Category: "work", Description: "工单导出模板"},
		}
	}
	var out []templateMeta
	for _, e := range entries {
		if e.IsDir() || !strings.HasSuffix(e.Name(), ".json") {
			continue
		}
		raw, err := os.ReadFile(filepath.Join(dir, e.Name()))
		if err != nil {
			continue
		}
		var m map[string]any
		if json.Unmarshal(raw, &m) != nil {
			continue
		}
		id, _ := m["id"].(string)
		if id == "" {
			id = strings.TrimSuffix(e.Name(), ".json")
		}
		label, _ := m["label"].(string)
		if label == "" {
			label = id
		}
		cat, _ := m["category"].(string)
		desc, _ := m["description"].(string)
		out = append(out, templateMeta{ID: id, Label: label, Category: cat, Description: desc})
	}
	if len(out) == 0 {
		return loadTemplates("")
	}
	return out
}

func (s *server) handleTemplates(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, 200, map[string]any{"code": 0, "data": s.tpls, "total": len(s.tpls)})
}

func (s *server) handleHall(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{
		"templates": len(s.tpls),
		"jobs":      len(s.jobs),
		"mode":      "lite",
	}})
}

func (s *server) handleJobs(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodGet:
		s.mu.Lock()
		defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code": 0, "data": s.jobs, "total": len(s.jobs)})
	case http.MethodPost:
		var body struct {
			TemplateID string         `json:"template_id"`
			TemplateId string         `json:"templateId"`
			Filters    map[string]any `json:"filters"`
		}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			writeJSON(w, 400, map[string]any{"code": 400})
			return
		}
		tid := body.TemplateID
		if tid == "" {
			tid = body.TemplateId
		}
		if tid == "" {
			tid = "order"
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		s.seq++
		now := time.Now().Format(time.RFC3339)
		id := "job_" + time.Now().Format("150405")
		j := job{
			ID:         id,
			TemplateID: tid,
			State:      "done",
			Filters:    body.Filters,
			CreatedAt:  now,
			UpdatedAt:  now,
			Message:    "lite mode simulated export (no warehouse)",
			Download:   "/jobs/" + id + "/xlsx",
		}
		s.jobs = append([]job{j}, s.jobs...)
		writeJSON(w, 200, map[string]any{"code": 0, "data": j})
	default:
		writeJSON(w, 405, map[string]any{"code": 405})
	}
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
