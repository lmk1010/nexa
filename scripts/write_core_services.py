from pathlib import Path

def w(path: str, content: str) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8")
    print("wrote", path, "bytes", len(content))


w(
    "E:/code/nexa/services/iam/cmd/nexa-iam/main.go",
    r"""// nexa-iam — identity, tenant, roles (Go rewrite of enterprise IAM).
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
""",
)

w(
    "E:/code/nexa/services/hr/cmd/nexa-hr/main.go",
    r"""// nexa-hr — org and employee (Go rewrite).
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
""",
)

w(
    "E:/code/nexa/services/bpm/cmd/nexa-bpm/main.go",
    r"""// nexa-bpm — approvals (Go rewrite, thin state model first).
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
""",
)

print("done")
