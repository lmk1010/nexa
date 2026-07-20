// nexa-cdc-mysql — MySQL binlog CDC (Go rewrite of canal-style pipeline).
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

	"github.com/lmk1010/nexa-cdc-mysql/internal/binlog"
	"github.com/lmk1010/nexa-cdc-mysql/internal/config"
	"github.com/lmk1010/nexa-cdc-mysql/internal/filter"
	"github.com/lmk1010/nexa-cdc-mysql/internal/metrics"
	"github.com/lmk1010/nexa-cdc-mysql/internal/sink"
	"github.com/lmk1010/nexa-cdc-mysql/internal/store"
)

var version = "0.1.0-skeleton"

func main() {
	cfgPath := flag.String("config", "./configs/config.yaml", "path to config yaml")
	flag.Parse()

	cfg, err := config.Load(*cfgPath)
	if err != nil {
		log.Fatalf("[boot] config: %v", err)
	}

	reg := metrics.New()
	tf := filter.New(cfg.IncludeTables)
	st := store.NewStore(cfg.Store.Driver)

	// Prefer log sink until MySQL upsert is implemented.
	var writer sink.Writer = sink.NewLogWriter()
	if cfg.Sink.DSN != "" {
		log.Printf("[boot] sink.dsn set — MySQLWriter still TODO, using LogWriter")
	}

	dumper := binlog.NewDumper(cfg, tf, st, writer)

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", reg.Handler())
	mux.HandleFunc("/metrics", reg.Handler())
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"service":"nexa-cdc-mysql","version":"` + version + `"}`))
	})

	srv := &http.Server{Addr: cfg.HTTP.Addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	go func() {
		log.Printf("[boot] nexa-cdc-mysql %s listening on %s", version, cfg.HTTP.Addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("[boot] http: %v", err)
		}
	}()

	errCh := make(chan error, 1)
	go func() { errCh <- dumper.Run(ctx) }()

	select {
	case <-ctx.Done():
		log.Printf("[boot] signal received, shutting down")
	case err := <-errCh:
		if err != nil {
			log.Printf("[boot] dumper exited: %v", err)
		}
	}

	shutdownCtx, c := context.WithTimeout(context.Background(), 10*time.Second)
	defer c()
	_ = srv.Shutdown(shutdownCtx)
	_ = writer.Close()
	log.Printf("[boot] bye")
}
