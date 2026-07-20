package httpserver

import (
	"context"
	"encoding/json"
	"net/http"
	"time"

	"github.com/lmk1010/nexa/services/agent/internal/config"
)

// Server is the HTTP entry for nexa-agent.
type Server struct {
	httpServer *http.Server
	cfg        *config.Config
	version    string
}

func New(cfg *config.Config, version string) *Server {
	s := &Server{cfg: cfg, version: version}
	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", s.handleHealth)
	mux.HandleFunc("/v1/chat", s.handleChat)
	mux.HandleFunc("/", s.handleRoot)
	s.httpServer = &http.Server{
		Addr:              cfg.HTTP.Addr,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
	}
	return s
}

func (s *Server) ListenAndServe() error { return s.httpServer.ListenAndServe() }

func (s *Server) Shutdown(ctx context.Context) error {
	return s.httpServer.Shutdown(ctx)
}

func (s *Server) handleRoot(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, map[string]any{
		"service": "nexa-agent",
		"version": s.version,
		"role":    "dingtalk/enterprise agent",
	})
}

func (s *Server) handleHealth(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, map[string]any{"status": "UP", "service": "nexa-agent", "version": s.version})
}

// handleChat is a placeholder for the future tool-using agent loop.
func (s *Server) handleChat(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"error":"POST only"}`, http.StatusMethodNotAllowed)
		return
	}
	writeJSON(w, map[string]any{
		"ok":      true,
		"message": "skeleton: wire tools/session/dingtalk next (see legacy/agent-node)",
	})
}

func writeJSON(w http.ResponseWriter, v any) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(v)
}
