package binlog

import (
	"context"
	"log"
	"time"

	"github.com/lmk1010/nexa-cdc-mysql/internal/config"
	"github.com/lmk1010/nexa-cdc-mysql/internal/filter"
	"github.com/lmk1010/nexa-cdc-mysql/internal/parser"
	"github.com/lmk1010/nexa-cdc-mysql/internal/sink"
	"github.com/lmk1010/nexa-cdc-mysql/internal/store"
)

// Dumper streams ROW binlog events from the source MySQL.
//
// Implementation plan (next phase):
//  1. go-mysql replication.BinlogSyncer
//  2. Decode RowsEvent → parser.RowEvent
//  3. filter.Allow → sink.Write
//  4. store.Save position periodically
//
// Current build is a lifecycle skeleton so deploy/config can land first.
type Dumper struct {
	cfg    *config.Config
	filter *filter.TableFilter
	store  store.PositionStore
	writer sink.Writer
}

func NewDumper(cfg *config.Config, f *filter.TableFilter, st store.PositionStore, w sink.Writer) *Dumper {
	return &Dumper{cfg: cfg, filter: f, store: st, writer: w}
}

// Run blocks until ctx is cancelled. Skeleton: heartbeat only.
func (d *Dumper) Run(ctx context.Context) error {
	log.Printf("[binlog] skeleton dumper start source=%s server_id=%d tables=%d",
		d.cfg.SourceAddr(), d.cfg.ServerID, d.filter.Size())
	log.Printf("[binlog] TODO: implement dump with go-mysql-org/go-mysql (ROW events)")

	// Demonstrate store load path.
	if pos, ok, err := d.store.Load(ctx, d.cfg.Store.Channel); err != nil {
		return err
	} else if ok {
		log.Printf("[binlog] resume from %s:%d gtid=%q", pos.File, pos.Pos, pos.GTID)
	} else {
		log.Printf("[binlog] no stored position; will start from latest when dump is implemented")
	}

	t := time.NewTicker(30 * time.Second)
	defer t.Stop()
	for {
		select {
		case <-ctx.Done():
			log.Printf("[binlog] stopping")
			return ctx.Err()
		case <-t.C:
			log.Printf("[binlog] heartbeat (skeleton) allowed_tables=%d", d.filter.Size())
		}
	}
}

// Handle is a placeholder for the future event loop body.
func (d *Dumper) Handle(ctx context.Context, ev *parser.RowEvent) error {
	if !d.filter.Allow(ev.Schema, ev.Table) {
		return nil
	}
	return d.writer.Write(ctx, ev)
}
