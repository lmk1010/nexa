from pathlib import Path


def w(path: str, content: str) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8")
    print("wrote", path)


BUSINESS = r'''// nexa-business — todos, work requirements, calendar, reception (Go).
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
'''

OP = r'''// nexa-op — ops monitor / release / audit (Go).
package main

import (
	"context"
	"encoding/json"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"runtime"
	"syscall"
	"time"
)

var version = "0.2.0-m6-partial"
var started = time.Now()

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-op"}
	cfg.HTTP.Addr = ":48088"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48088"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/op/status", func(w http.ResponseWriter, r *http.Request) {
		var ms runtime.MemStats
		runtime.ReadMemStats(&ms)
		writeJSON(w, map[string]any{"code": 0, "data": map[string]any{
			"uptimeSec": int(time.Since(started).Seconds()),
			"goVersion": runtime.Version(),
			"goroutines": runtime.NumGoroutine(),
			"memAllocMB": float64(ms.Alloc) / 1024 / 1024,
			"services": []map[string]any{
				{"name": "gateway", "port": 48080, "expected": true},
				{"name": "iam", "port": 48081, "expected": true},
				{"name": "bpm", "port": 48082, "expected": true},
				{"name": "hr", "port": 48083, "expected": true},
				{"name": "business", "port": 48084, "expected": true},
				{"name": "agent", "port": 48091, "expected": true},
				{"name": "data-center", "port": 48092, "expected": true},
				{"name": "cdc-mysql", "port": 6060, "expected": true},
			},
		}})
	})
	mux.HandleFunc("/v1/op/audit/recent", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"id": "a1", "actor": "boss", "action": "login", "at": time.Now().Add(-10 * time.Minute).Format(time.RFC3339)},
			{"id": "a2", "actor": "admin", "action": "view_employees", "at": time.Now().Add(-5 * time.Minute).Format(time.RFC3339)},
		}})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "apis": []string{"/v1/op/status", "/v1/op/audit/recent"}})
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
'''

AI = r'''// nexa-ai — reception / ASR / vision config surface (Go).
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

var version = "0.2.0-m6-partial"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-ai"}
	cfg.HTTP.Addr = ":48089"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48089"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/ai/models", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"id": "deepseek-chat", "provider": "deepseek", "kind": "chat"},
			{"id": "whisper-1", "provider": "openai", "kind": "asr"},
		}})
	})
	mux.HandleFunc("/v1/ai/reception/config", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": map[string]any{
			"enabled": true,
			"notifyEnabled": true,
			"keywords": []string{"面试", "快递", "访客"},
			"updatedAt": time.Now().Format(time.RFC3339),
		}})
	})
	mux.HandleFunc("/v1/ai/reception/records", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"id": "rr1", "text": "访客来访面试", "status": "done", "at": time.Now().Add(-1 * time.Hour).Format(time.RFC3339)},
		}, "total": 1})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "apis": []string{"/v1/ai/models", "/v1/ai/reception/config", "/v1/ai/reception/records"}})
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
'''

ERP = r'''// nexa-erp — stock / purchase query surface (Go).
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

var version = "0.2.0-m6-partial"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-erp"}
	cfg.HTTP.Addr = ":48085"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48085"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/erp/stock/summary", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"sku": "SKU-001", "name": "Widget A", "qty": 120, "warehouse": "WH-East"},
			{"sku": "SKU-002", "name": "Widget B", "qty": 45, "warehouse": "WH-East"},
		}})
	})
	mux.HandleFunc("/v1/erp/purchase/orders", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"id": "PO-1001", "vendor": "Supplier X", "amount": 3200.5, "status": "open", "createdAt": time.Now().Add(-72 * time.Hour).Format(time.RFC3339)},
		}, "total": 1})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "apis": []string{"/v1/erp/stock/summary", "/v1/erp/purchase/orders"}})
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
'''

FINANCE = r'''// nexa-finance — ledger / report query surface (Go).
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

var version = "0.2.0-m6-partial"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-finance"}
	cfg.HTTP.Addr = ":48086"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48086"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/finance/summary/monthly", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": map[string]any{
			"month": time.Now().Format("2006-01"),
			"income": 128000.0,
			"expense": 76450.5,
			"profit": 51549.5,
		}})
	})
	mux.HandleFunc("/v1/finance/ledger/recent", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"id": "L1", "title": "Office rent", "amount": -12000, "at": time.Now().Add(-48 * time.Hour).Format(time.RFC3339)},
			{"id": "L2", "title": "Service revenue", "amount": 35000, "at": time.Now().Add(-24 * time.Hour).Format(time.RFC3339)},
		}})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "apis": []string{"/v1/finance/summary/monthly", "/v1/finance/ledger/recent"}})
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
'''

IM = r'''// nexa-im — conversation / contacts surface (Go).
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

var version = "0.2.0-m6-partial"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-im"}
	cfg.HTTP.Addr = ":48087"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48087"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/im/conversations", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"id": "c1", "title": "Ops group", "unread": 2, "updatedAt": time.Now().Add(-15 * time.Minute).Format(time.RFC3339)},
			{"id": "c2", "title": "HR notice", "unread": 0, "updatedAt": time.Now().Add(-2 * time.Hour).Format(time.RFC3339)},
		}})
	})
	mux.HandleFunc("/v1/im/contacts", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, map[string]any{"code": 0, "data": []map[string]any{
			{"id": 1001, "name": "Zhang San", "dept": "R&D"},
			{"id": 1002, "name": "Li Si", "dept": "Ops"},
		}})
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, map[string]any{"service": cfg.Name, "version": version, "apis": []string{"/v1/im/conversations", "/v1/im/contacts"}})
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
'''

files = {
    "E:/code/nexa/services/business/cmd/nexa-business/main.go": BUSINESS,
    "E:/code/nexa/services/op/cmd/nexa-op/main.go": OP,
    "E:/code/nexa/services/ai/cmd/nexa-ai/main.go": AI,
    "E:/code/nexa/services/erp/cmd/nexa-erp/main.go": ERP,
    "E:/code/nexa/services/finance/cmd/nexa-finance/main.go": FINANCE,
    "E:/code/nexa/services/im/cmd/nexa-im/main.go": IM,
}
for path, content in files.items():
    w(path, content)
print("all domain mains written")
