package sink

import (
	"context"
	"log"

	"github.com/lmk1010/nexa-cdc-mysql/internal/parser"
)

// Writer applies row events to a destination (warehouse, MQ, log).
type Writer interface {
	Write(ctx context.Context, ev *parser.RowEvent) error
	Close() error
}

// LogWriter prints events — useful for dry-run / skeleton.
type LogWriter struct{}

func NewLogWriter() *LogWriter { return &LogWriter{} }

func (w *LogWriter) Write(_ context.Context, ev *parser.RowEvent) error {
	log.Printf("[sink] %s %s.%s rows=%d pos=%s:%d",
		ev.Action, ev.Schema, ev.Table, len(ev.Rows), ev.Pos.File, ev.Pos.Pos)
	return nil
}

func (w *LogWriter) Close() error { return nil }

// MySQLWriter upserts into warehouse (TODO).
//
// Expected behaviour:
//   - INSERT/UPDATE → INSERT ... ON DUPLICATE KEY UPDATE
//   - DELETE → soft-delete or hard DELETE based on table policy
//   - batch by table, flush on size/time
type MySQLWriter struct {
	dsn       string
	batchSize int
}

func NewMySQLWriter(dsn string, batchSize int) *MySQLWriter {
	return &MySQLWriter{dsn: dsn, batchSize: batchSize}
}

func (w *MySQLWriter) Write(ctx context.Context, ev *parser.RowEvent) error {
	// TODO: open pool, map columns, upsert
	_ = ctx
	_ = ev
	return nil
}

func (w *MySQLWriter) Close() error { return nil }
