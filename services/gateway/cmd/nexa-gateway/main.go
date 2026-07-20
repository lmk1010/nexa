// nexa-gateway — unified entry for the enterprise assistant platform (Go only).
package main

import (
	"context"
	"encoding/json"
	"flag"
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

var version = "0.2.0"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	Upstreams map[string]string `json:"upstreams"`
	Routes    []route           `json:"routes"`
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
	cfg.Upstreams = map[string]string{
		"agent":       "http://127.0.0.1:48091",
		"iam":         "http://127.0.0.1:48081",
		"bpm":         "http://127.0.0.1:48082",
		"hr":          "http://127.0.0.1:48083",
		"business":    "http://127.0.0.1:48084",
		"erp":         "http://127.0.0.1:48085",
		"finance":     "http://127.0.0.1:48086",
		"im":          "http://127.0.0.1:48087",
		"op":          "http://127.0.0.1:48088",
		"ai":          "http://127.0.0.1:48089",
		"data-center": "http://127.0.0.1:48092",
		"cdc":         "http://127.0.0.1:6060",
	}
	cfg.Routes = []route{
		{Prefix: "/agent", Upstream: "agent", Strip: true},
		{Prefix: "/app-api/agent", Upstream: "agent", Strip: true},
		{Prefix: "/admin-api/agent", Upstream: "agent", Strip: true},
		{Prefix: "/v1/iam", Upstream: "iam"},
		{Prefix: "/v1/bpm", Upstream: "bpm"},
		{Prefix: "/v1/hr", Upstream: "hr"},
		{Prefix: "/v1/business", Upstream: "business"},
		{Prefix: "/v1/erp", Upstream: "erp"},
		{Prefix: "/v1/finance", Upstream: "finance"},
		{Prefix: "/v1/im", Upstream: "im"},
		{Prefix: "/v1/op", Upstream: "op"},
		{Prefix: "/v1/ai", Upstream: "ai"},
		{Prefix: "/v1/data-center", Upstream: "data-center", Strip: true},
		{Prefix: "/v1/cdc", Upstream: "cdc", Strip: true},
		{Prefix: "/app-api/system", Upstream: "iam"},
		{Prefix: "/admin-api/system", Upstream: "iam"},
		{Prefix: "/app-api/hr", Upstream: "hr"},
		{Prefix: "/admin-api/hr", Upstream: "hr"},
		{Prefix: "/app-api/bpm", Upstream: "bpm"},
		{Prefix: "/admin-api/bpm", Upstream: "bpm"},
	}
	return cfg
}

func loadConfig(path string) config {
	cfg := defaultConfig()
	raw, err := os.ReadFile(path)
	if err != nil {
		if !os.IsNotExist(err) {
			log.Printf("config read: %v (defaults)", err)
		}
		return cfg
	}
	if err := json.Unmarshal(raw, &cfg); err != nil {
		log.Fatalf("config parse: %v", err)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48080"
	}
	if cfg.Name == "" {
		cfg.Name = "nexa-gateway"
	}
	if len(cfg.Upstreams) == 0 {
		return defaultConfig()
	}
	return cfg
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path (JSON)")
	flag.Parse()
	cfg := loadConfig(*cfgPath)

	proxies := map[string]*httputil.ReverseProxy{}
	for name, rawURL := range cfg.Upstreams {
		u, err := url.Parse(rawURL)
		if err != nil {
			log.Fatalf("upstream %s: %v", name, err)
		}
		p := httputil.NewSingleHostReverseProxy(u)
		baseDirector := p.Director
		target := u
		p.Director = func(req *http.Request) {
			baseDirector(req)
			req.Host = target.Host
		}
		p.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
			log.Printf("proxy error %s %s: %v", r.Method, r.URL.Path, err)
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusBadGateway)
			_ = json.NewEncoder(w).Encode(map[string]any{
				"code":  502,
				"msg":   "upstream unavailable",
				"path":  r.URL.Path,
				"error": err.Error(),
			})
		}
		proxies[name] = p
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/platform/services", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{
			"service":   cfg.Name,
			"version":   version,
			"upstreams": cfg.Upstreams,
			"routes":    cfg.Routes,
		})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
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
			writeJSON(w, map[string]any{
				"service": cfg.Name,
				"version": version,
				"msg":     "nexa gateway — use /v1/<domain>/... or /agent/...",
			})
			return
		}
		p := proxies[matched.Upstream]
		if p == nil {
			http.Error(w, `{"code":500,"msg":"upstream not configured"}`, http.StatusInternalServerError)
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
		log.Printf("[boot] %s %s on %s (%d routes)", cfg.Name, version, cfg.HTTP.Addr, len(cfg.Routes))
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
	}()
	<-ctx.Done()
	shCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shCtx)
}

func withAccessLog(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		next.ServeHTTP(w, r)
		log.Printf("%s %s %s", r.Method, r.URL.Path, time.Since(start).Truncate(time.Millisecond))
	})
}

func writeJSON(w http.ResponseWriter, v any) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(v)
}
