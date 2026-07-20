// kyx-data-center 数据平台中心
// 定位: 明细导出 (xlsx 流式) + 队列 + 预约, 直查 warehouse
package main

import (
	"context"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"

	"github.com/kyx/kyx-data-center/internal/api"
	"github.com/kyx/kyx-data-center/internal/auth"
	"github.com/kyx/kyx-data-center/internal/config"
	"github.com/kyx/kyx-data-center/internal/queue"
	"github.com/kyx/kyx-data-center/internal/store"
	"github.com/kyx/kyx-data-center/internal/template"
	"github.com/kyx/kyx-data-center/internal/warehouse"
	"github.com/kyx/kyx-data-center/internal/worker"
)

var version = "0.1.0"

func main() {
	cfgPath := flag.String("config", "/etc/kyx-data-center/config.yaml", "config yaml path")
	flag.Parse()

	cfg, err := config.Load(*cfgPath)
	if err != nil {
		log.Fatalf("[boot] load config: %v", err)
	}

	// warehouse 连接池
	db, err := warehouse.New(cfg.Warehouse)
	if err != nil {
		log.Fatalf("[boot] warehouse: %v", err)
	}
	defer db.Close()
	log.Printf("[boot] warehouse connected: %s:%d/%s", cfg.Warehouse.Host, cfg.Warehouse.Port, cfg.Warehouse.Database)

	// 建 export_* 表 (幂等)
	if err := store.EnsureSchema(context.Background(), db); err != nil {
		log.Fatalf("[boot] ensure schema: %v", err)
	}
	log.Printf("[boot] schema ready: export_jobs / export_schedule")

	// 输出目录
	if err := os.MkdirAll(cfg.Storage.Dir, 0o755); err != nil {
		log.Fatalf("[boot] storage dir: %v", err)
	}

	// 模板
	reg, err := template.Load(cfg.Templates.Dir)
	if err != nil {
		log.Fatalf("[boot] load templates: %v", err)
	}
	log.Printf("[boot] templates loaded: %d", len(reg.All()))

	// store
	st := store.New(db)

	// 队列 + worker
	q := queue.New(cfg.Queue.MaxSize)
	pool := worker.New(cfg.Queue.WorkerPoolSize, q, st, db, reg, cfg.Storage.Dir)
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()
	pool.Start(ctx)
	// 恢复重启前的 pending/queued/running 任务
	_ = pool.RecoverPending(ctx)

	// GC old files (每小时扫)
	go api.GCLoop(ctx, cfg.Storage.Dir, time.Duration(cfg.Storage.TTLHours)*time.Hour)

	// HTTP
	r := chi.NewRouter()
	r.Use(middleware.Recoverer, middleware.RealIP)

	// /actuator/health 不走 auth
	r.Get("/actuator/health", func(w http.ResponseWriter, r *http.Request) {
		if err := db.PingContext(r.Context()); err != nil {
			http.Error(w, `{"status":"DOWN"}`, http.StatusServiceUnavailable)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte(`{"status":"UP","service":"kyx-data-center","version":"` + version + `"}`))
	})

	// 业务
	r.Group(func(r chi.Router) {
		r.Use(middleware.Logger, auth.Middleware)
		api.Mount(r, &api.Deps{
			DB:       db,
			Store:    st,
			Queue:    q,
			Pool:     pool,
			Registry: reg,
			Storage:  cfg.Storage.Dir,
			Quota:    api.QuotaConfig{PerUserActive: cfg.Quota.PerUserActive, PerUserDaily: cfg.Quota.PerUserDaily},
		})
	})

	srv := &http.Server{
		Addr:              cfg.HTTP.Addr,
		Handler:           r,
		ReadHeaderTimeout: 5 * time.Second,
	}

	go func() {
		log.Printf("[boot] kyx-data-center %s listening on %s", version, cfg.HTTP.Addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("[http] listen: %v", err)
		}
	}()

	<-ctx.Done()
	log.Println("[shutdown] draining...")
	pool.Stop()

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()
	if err := srv.Shutdown(shutdownCtx); err != nil {
		log.Printf("[shutdown] http: %v", err)
	}
	log.Println("[shutdown] bye")
}
