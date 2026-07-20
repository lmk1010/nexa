package store

import (
	"context"
	"sync"

	"github.com/lmk1010/nexa-cdc-mysql/internal/parser"
)

// PositionStore persists CDC progress per channel.
type PositionStore interface {
	Load(ctx context.Context, channel string) (parser.Position, bool, error)
	Save(ctx context.Context, channel string, pos parser.Position) error
}

// NewStore selects a driver. "mysql" falls back to memory until implemented.
func NewStore(driver string) PositionStore {
	switch driver {
	case "mysql":
		// TODO: return NewMySQLStore(dsn)
		return NewMemoryStore()
	default:
		return NewMemoryStore()
	}
}

// MemoryStore is process-local (lost on restart).
type MemoryStore struct {
	mu   sync.Mutex
	data map[string]parser.Position
}

func NewMemoryStore() *MemoryStore {
	return &MemoryStore{data: make(map[string]parser.Position)}
}

func (s *MemoryStore) Load(_ context.Context, channel string) (parser.Position, bool, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	p, ok := s.data[channel]
	return p, ok, nil
}

func (s *MemoryStore) Save(_ context.Context, channel string, pos parser.Position) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.data[channel] = pos
	return nil
}

// MySQLPositionDDL creates the position table on warehouse.
const MySQLPositionDDL = `
CREATE TABLE IF NOT EXISTS nexa_cdc_position (
  channel   VARCHAR(64)  NOT NULL PRIMARY KEY,
  file      VARCHAR(255) NOT NULL DEFAULT '',
  pos       BIGINT       NOT NULL DEFAULT 0,
  gtid      TEXT,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
`
