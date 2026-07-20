from pathlib import Path

Path("E:/code/nexa/services/gateway/cmd/nexa-gateway/main.go").write_text(
    r'''// nexa-gateway — reverse proxy + optional IAM token check (Go only).
package main

import (
	"bytes"
	"context"
	"encoding/json"
	"flag"
	"io"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"
)

var version = "0.3.0"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	Auth struct {
		Enabled    bool   `json:"enabled"`
		IAMBaseURL string `json:"iamBaseUrl"`
	} `json:"auth"`
	Upstreams map[string]string `json:"upstreams"`
	Routes    []route           `json:"routes"`
	Public    []string          `json:"publicPrefixes"`
}

type route struct {
	Prefix    string `json:"prefix"`
	Upstream  string `json:"upstream"`
	Strip     bool   `json:"strip_prefix"`
	RewriteTo string `json:"rewrite_to"`
}

func defaultConfig() config {
	cfg := config{Name: "nexa-gateway"}
	cfg.HTTP.Addr = ":48080"
	cfg.Auth.Enabled = true
	cfg.Auth.IAMBaseURL = "http://127.0.0.1:48081"
	cfg.Upstreams = map[string]string{
		"agent": "http://127.0.0.1:48091", "iam": "http://127.0.0.1:48081", "bpm": "http://127.0.0.1:48082",
		"hr": "http://127.0.0.1:48083", "business": "http://127.0.0.1:48084", "erp": "http://127.0.0.1:48085",
		"finance": "http://127.0.0.1:48086", "im": "http://127.0.0.1:48087", "op": "http://127.0.0.1:48088",
		"ai": "http://127.0.0.1:48089", "data-center": "http://127.0.0.1:48092", "cdc": "http://127.0.0.1:6060",
	}
	cfg.Routes = []route{
		{Prefix: "/agent", Upstream: "agent", Strip: true},
		{Prefix: "/app-api/agent", Upstream: "agent", Strip: true},
		{Prefix: "/admin-api/agent", Upstream: "agent", Strip: true},
		{Prefix: "/v1/iam", Upstream: "iam"}, {Prefix: "/v1/bpm", Upstream: "bpm"}, {Prefix: "/v1/hr", Upstream: "hr"},
		{Prefix: "/v1/business", Upstream: "business"}, {Prefix: "/v1/erp", Upstream: "erp"}, {Prefix: "/v1/finance", Upstream: "finance"},
		{Prefix: "/v1/im", Upstream: "im"}, {Prefix: "/v1/op", Upstream: "op"}, {Prefix: "/v1/ai", Upstream: "ai"},
		{Prefix: "/v1/data-center", Upstream: "data-center", Strip: true}, {Prefix: "/v1/cdc", Upstream: "cdc", Strip: true},
		{Prefix: "/app-api/system", Upstream: "iam"}, {Prefix: "/admin-api/system", Upstream: "iam"},
		{Prefix: "/app-api/hr", Upstream: "hr"}, {Prefix: "/admin-api/hr", Upstream: "hr"},
		{Prefix: "/app-api/bpm", Upstream: "bpm"}, {Prefix: "/admin-api/bpm", Upstream: "bpm"},
	}
	cfg.Public = []string{
		"/healthz",
		"/v1/platform/services",
		"/v1/iam/login",
		"/app-api/system/auth/login",
		"/admin-api/system/auth/login",
		"/agent",
		"/app-api/agent",
		"/admin-api/agent",
	}
	return cfg
}

func loadConfig(path string) config {
	cfg := defaultConfig()
	raw, err := os.ReadFile(path)
	if err != nil {
		return cfg
	}
	_ = json.Unmarshal(raw, &cfg)
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48080"
	}
	if len(cfg.Upstreams) == 0 {
		return defaultConfig()
	}
	return cfg
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := loadConfig(*cfgPath)

	proxies := map[string]*httputil.ReverseProxy{}
	for name, rawURL := range cfg.Upstreams {
		u, err := url.Parse(rawURL)
		if err != nil {
			log.Fatalf("upstream %s: %v", name, err)
		}
		p := httputil.NewSingleHostReverseProxy(u)
		base := p.Director
		target := u
		p.Director = func(req *http.Request) {
			base(req)
			req.Host = target.Host
		}
		p.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusBadGateway)
			_ = json.NewEncoder(w).Encode(map[string]any{"code": 502, "msg": "upstream unavailable", "path": r.URL.Path, "error": err.Error()})
		}
		proxies[name] = p
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version, "auth": cfg.Auth.Enabled})
	})
	mux.HandleFunc("/v1/platform/services", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "upstreams": cfg.Upstreams, "routes": cfg.Routes, "authEnabled": cfg.Auth.Enabled})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if cfg.Auth.Enabled && !isPublic(cfg, r.URL.Path) {
			if !authorize(cfg, r) {
				writeJSONStatus(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "unauthorized"})
				return
			}
		}
		var matched *route
		for i := range cfg.Routes {
			rt := &cfg.Routes[i]
			if strings.HasPrefix(r.URL.Path, rt.Prefix) {
				if matched == nil || len(rt.Prefix) > len(matched.Prefix) {
					matched = rt
				}
			}
		}
		if matched == nil {
			writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "msg": "nexa gateway"})
			return
		}
		p := proxies[matched.Upstream]
		if p == nil {
			writeJSONStatus(w, 500, map[string]any{"code": 500, "msg": "upstream not configured"})
			return
		}
		if matched.Strip {
			rest := strings.TrimPrefix(r.URL.Path, matched.Prefix)
			if rest == "" {
				rest = "/"
			}
			if !strings.HasPrefix(rest, "/") {
				rest = "/" + rest
			}
			if matched.RewriteTo != "" {
				rest = strings.TrimRight(matched.RewriteTo, "/") + rest
			}
			r.URL.Path = rest
		}
		p.ServeHTTP(w, r)
	})

	srv := &http.Server{Addr: cfg.HTTP.Addr, Handler: withAccessLog(mux), ReadHeaderTimeout: 10 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] %s %s on %s auth=%v", cfg.Name, version, cfg.HTTP.Addr, cfg.Auth.Enabled)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
	}()
	<-ctx.Done()
	shCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shCtx)
}

func isPublic(cfg config, path string) bool {
	if path == "/healthz" || path == "/v1/platform/services" {
		return true
	}
	exact := map[string]bool{
		"/v1/iam/login":                 true,
		"/app-api/system/auth/login":    true,
		"/admin-api/system/auth/login":  true,
	}
	if exact[path] {
		return true
	}
	prefixes := []string{"/agent", "/app-api/agent", "/admin-api/agent"}
	for _, p := range prefixes {
		if path == p || strings.HasPrefix(path, p+"/") {
			return true
		}
	}
	for _, p := range cfg.Public {
		if path == p || strings.HasPrefix(path, p+"/") {
			return true
		}
	}
	return false
}

func authorize(cfg config, r *http.Request) bool {
	tok := strings.TrimSpace(strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer"))
	tok = strings.TrimSpace(tok)
	if tok == "" {
		tok = r.Header.Get("token")
	}
	if tok == "" {
		return false
	}
	body, _ := json.Marshal(map[string]string{"token": tok})
	req, err := http.NewRequest(http.MethodPost, strings.TrimRight(cfg.Auth.IAMBaseURL, "/")+"/v1/iam/token/introspect", bytes.NewReader(body))
	if err != nil {
		return false
	}
	req.Header.Set("Content-Type", "application/json")
	client := &http.Client{Timeout: 2 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("auth introspect error: %v", err)
		return false
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	var out struct {
		Code int `json:"code"`
		Data struct {
			Active bool `json:"active"`
		} `json:"data"`
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		return false
	}
	return out.Code == 0 && out.Data.Active
}

func withAccessLog(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		next.ServeHTTP(w, r)
		log.Printf("%s %s %s", r.Method, r.URL.Path, time.Since(start).Truncate(time.Millisecond))
	})
}

func writeJSON(w http.ResponseWriter, v any) { writeJSONStatus(w, 200, v) }

func writeJSONStatus(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
''',
    encoding="utf-8",
)
print("gateway written")
