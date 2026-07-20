// nexa-im — file-backed conversations/contacts/messages (Go).
package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
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


var version = "0.3.0-m6"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	DataDir string `json:"dataDir"`
}

type conversation struct {
	ID        string `json:"id"`
	Title     string `json:"title"`
	Unread    int    `json:"unread"`
	UpdatedAt string `json:"updatedAt"`
}

type contact struct {
	ID   int64  `json:"id"`
	Name string `json:"name"`
	Dept string `json:"dept"`
}

type message struct {
	ID             string `json:"id"`
	ConversationID string `json:"conversationId"`
	From           string `json:"from"`
	Text           string `json:"text"`
	At             string `json:"at"`
}

type db struct {
	Conversations []conversation `json:"conversations"`
	Contacts      []contact      `json:"contacts"`
	Messages      []message      `json:"messages"`
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
	cfg := config{Name: "nexa-im", DataDir: "./data"}
	cfg.HTTP.Addr = ":48087"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if v := os.Getenv("NEXA_DATA_DIR"); v != "" {
		cfg.DataDir = v
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48087"
	}
	if cfg.DataDir == "" {
		cfg.DataDir = "./data"
	}
	s := &server{cfg: cfg, path: filepath.Join(cfg.DataDir, "im.json")}
	if err := s.loadOrSeed(); err != nil {
		log.Fatalf("store: %v", err)
	}
	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/im/conversations", s.handleConversations)
	mux.HandleFunc("/v1/im/contacts", s.handleContacts)
	mux.HandleFunc("/v1/im/messages", s.handleMessages)
	mux.HandleFunc("/v1/im/messages/send", s.handleSend)
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{"service": cfg.Name, "version": version, "status": "file-backed"})
	})
	run(cfg.HTTP.Addr, mux, cfg.Name, s.path)
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
		Conversations: []conversation{
			{ID: "c1", Title: "Ops group", Unread: 2, UpdatedAt: now.Add(-15 * time.Minute).Format(time.RFC3339)},
			{ID: "c2", Title: "HR notice", Unread: 0, UpdatedAt: now.Add(-2 * time.Hour).Format(time.RFC3339)},
		},
		Contacts: []contact{{ID: 1001, Name: "Zhang San", Dept: "R&D"}, {ID: 1002, Name: "Li Si", Dept: "Ops"}},
		Messages: []message{{ID: "m1", ConversationID: "c1", From: "system", Text: "Welcome to Nexa IM", At: now.Add(-1 * time.Hour).Format(time.RFC3339)}},
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

func (s *server) handleConversations(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Conversations, "total": len(s.db.Conversations)})
}

func (s *server) handleContacts(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Contacts, "total": len(s.db.Contacts)})
}

func (s *server) handleMessages(w http.ResponseWriter, r *http.Request) {
	cid := r.URL.Query().Get("conversationId")
	s.mu.Lock()
	defer s.mu.Unlock()
	out := make([]message, 0)
	for _, m := range s.db.Messages {
		if cid == "" || m.ConversationID == cid {
			out = append(out, m)
		}
	}
	writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
}

func (s *server) handleSend(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, 405, map[string]any{"code": 405})
		return
	}
	var body struct {
		ConversationID string `json:"conversationId"`
		From           string `json:"from"`
		Text           string `json:"text"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.Text == "" {
		writeJSON(w, 400, map[string]any{"code": 400, "msg": "text required"})
		return
	}
	if body.ConversationID == "" {
		body.ConversationID = "c1"
	}
	if body.From == "" {
		body.From = "user"
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	msg := message{ID: "m" + time.Now().Format("150405"), ConversationID: body.ConversationID, From: body.From, Text: body.Text, At: time.Now().Format(time.RFC3339)}
	s.db.Messages = append(s.db.Messages, msg)
	for i := range s.db.Conversations {
		if s.db.Conversations[i].ID == body.ConversationID {
			s.db.Conversations[i].UpdatedAt = msg.At
			s.db.Conversations[i].Unread++
		}
	}
	_ = s.save()
	go emitSense("im.message.sent", "im", "info", "message sent", map[string]any{"conversationId": body.ConversationID, "id": msg.ID})
	writeJSON(w, 200, map[string]any{"code": 0, "data": msg})
}

func emitSense(senseType, source, severity, message string, payload map[string]any) {
	body, _ := json.Marshal(map[string]any{"type": senseType, "source": source, "severity": severity, "message": message, "payload": payload})
	bases := []string{}
	if v := os.Getenv("NEXA_AI_URL"); v != "" {
		bases = append(bases, strings.TrimRight(v, "/"))
	}
	bases = append(bases, "http://127.0.0.1:48089")
	client := &http.Client{Timeout: 1500 * time.Millisecond}
	for _, base := range bases {
		req, err := http.NewRequest(http.MethodPost, base+"/v1/ai/sense", bytes.NewReader(body))
		if err != nil {
			continue
		}
		req.Header.Set("Content-Type", "application/json")
		resp, err := client.Do(req)
		if err != nil {
			continue
		}
		resp.Body.Close()
		return
	}
}

func run(addr string, mux *http.ServeMux, name, dataPath string) {
	srv := &http.Server{Addr: addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] %s %s on %s data=%s", name, version, addr, dataPath)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
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
