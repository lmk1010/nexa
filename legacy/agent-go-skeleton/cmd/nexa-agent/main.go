// nexa-agent — enterprise / DingTalk agent entry (Go rewrite target).
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

	"github.com/lmk1010/nexa/services/agent/internal/config"
	httpserver "github.com/lmk1010/nexa/services/agent/internal/http"
)

var version = "0.1.0-skeleton"

func main() {
	cfgPath := flag.String("config", "./configs/config.yaml", "path to config yaml")
	flag.Parse()

	cfg, err := config.Load(*cfgPath)
	if err != nil {
		log.Fatalf("[boot] config: %v", err)
	}

	srv := httpserver.New(cfg, version)

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	go func() {
		log.Printf("[boot] nexa-agent %s listening on %s", version, cfg.HTTP.Addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("[boot] http: %v", err)
		}
	}()

	<-ctx.Done()
	log.Printf("[boot] shutting down")
	shutdownCtx, c := context.WithTimeout(context.Background(), 10*time.Second)
	defer c()
	_ = srv.Shutdown(shutdownCtx)
}
