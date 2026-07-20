// nexa-iam — identity, tenant, roles (Go rewrite of enterprise IAM).
package main

import (
	"context"
	"encoding/json"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"sync"
	"syscall"
	"time"
)

var version = "0.2.0-m2"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
}

type user struct {
	ID       int64    `json:"id"`
	Username string   `json:"username"`
	Nickname string   `json:"nickname"`
	TenantID int64    `json:"tenantId"`
	Roles    []string `json:"roles"`
	Perms    []string `json:"permissions"`
}

var (
	mu    sync.RWMutex
	users = map[string]user{
		"admin": {ID: 1, Username: "admin", Nickname: "管理员", TenantID: 1, Roles: []string{"super_admin"}, Perms: []string{"*"}},
		"boss":  {ID: 2, Username: "boss", Nickname: "老板", TenantID: 1, Roles: []string{"boss"}, Perms: []string{"app:data-center:use", "app:ops:view", "app:cockpit:view"}},
	}
	tokens = map[string]string{}
)

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-iam"}
	cfg.HTTP.Addr = ":48081"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48081"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/iam/login", handleLogin)
	mux.HandleFunc("/v1/iam/me", handleMe)
	mux.HandleFunc("/v1/iam/permissions", handlePermissions)
	mux.HandleFunc("/app-api/system/auth/login", handleLogin)
	mux.HandleFunc("/admin-api/system/auth/login", handleLogin)
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "status": "partial", "apis": []string{"/v1/iam/login", "/v1/iam/me", "/v1/iam/permissions"}})
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

func handleLogin(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"code":405,"msg":"POST only"}`, http.StatusMethodNotAllowed)
		return
	}
	var body struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		http.Error(w, `{"code":400,"msg":"bad json"}`, http.StatusBadRequest)
		return
	}
	mu.RLock()
	u, ok := users[body.Username]
	mu.RUnlock()
	if !ok {
		http.Error(w, `{"code":401,"msg":"invalid credentials (demo users: admin, boss)"}`, http.StatusUnauthorized)
		return
	}
	tok := "nexa_" + body.Username + "_" + time.Now().Format("150405")
	mu.Lock()
	tokens[tok] = body.Username
	mu.Unlock()
	writeJSON(w, map[string]any{"code": 0, "data": map[string]any{"accessToken": tok, "user": u}})
}

func handleMe(w http.ResponseWriter, r *http.Request) {
	u, ok := userFromRequest(r)
	if !ok {
		http.Error(w, `{"code":401,"msg":"unauthorized"}`, http.StatusUnauthorized)
		return
	}
	writeJSON(w, map[string]any{"code": 0, "data": u})
}

func handlePermissions(w http.ResponseWriter, r *http.Request) {
	u, ok := userFromRequest(r)
	if !ok {
		http.Error(w, `{"code":401,"msg":"unauthorized"}`, http.StatusUnauthorized)
		return
	}
	writeJSON(w, map[string]any{"code": 0, "data": u.Perms})
}

func userFromRequest(r *http.Request) (user, bool) {
	auth := r.Header.Get("Authorization")
	tok := strings.TrimSpace(strings.TrimPrefix(auth, "Bearer"))
	tok = strings.TrimSpace(tok)
	if tok == "" {
		tok = r.Header.Get("token")
	}
	mu.RLock()
	defer mu.RUnlock()
	if uname, ok := tokens[tok]; ok {
		return users[uname], true
	}
	return user{}, false
}

func writeJSON(w http.ResponseWriter, v any) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(v)
}
