package store

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
	"sync"
	"time"
)

// Backend selects persistence.
// file (default) or mysql via DSN env NEXA_IAM_DSN / config.
type Backend string

const (
	BackendFile  Backend = "file"
	BackendMySQL Backend = "mysql"
)

// User is IAM user row.
type User struct {
	ID          int64    `json:"id"`
	Username    string   `json:"username"`
	Nickname    string   `json:"nickname"`
	Password    string   `json:"password,omitempty"`
	TenantID    int64    `json:"tenantId"`
	Roles       []string `json:"roles"`
	Permissions []string `json:"permissions"`
}

type Tenant struct {
	ID        int64  `json:"id"`
	Name      string `json:"name"`
	Code      string `json:"code"`
	Status    string `json:"status"`
	CreatedAt string `json:"createdAt"`
}

type Invite struct {
	Code      string `json:"code"`
	TenantID  int64  `json:"tenantId"`
	Role      string `json:"role"`
	CreatedBy string `json:"createdBy"`
	CreatedAt string `json:"createdAt"`
	ExpiresAt string `json:"expiresAt"`
	UsedBy    string `json:"usedBy,omitempty"`
}

type TokenRecord struct {
	Username  string    `json:"username"`
	ExpiresAt time.Time `json:"expiresAt"`
}

// Data is the full IAM snapshot used by file backend and as DTO.
type Data struct {
	Users     map[string]User        `json:"users"`
	Tokens    map[string]TokenRecord `json:"tokens"`
	Tenants   map[int64]Tenant       `json:"tenants"`
	Invites   map[string]Invite      `json:"invites"`
	Seq       int64                  `json:"seq"`
	TenantSeq int64                  `json:"tenantSeq"`
}

// Store is concurrency-safe IAM persistence.
type Store struct {
	mu   sync.Mutex
	path string
	data Data
}

func NewFileStore(dir string) *Store {
	_ = os.MkdirAll(dir, 0o755)
	return &Store{path: filepath.Join(dir, "iam.json"), data: emptyData()}
}

func emptyData() Data {
	return Data{
		Users:   map[string]User{},
		Tokens:  map[string]TokenRecord{},
		Tenants: map[int64]Tenant{},
		Invites: map[string]Invite{},
	}
}

func (s *Store) Load() error {
	s.mu.Lock()
	defer s.mu.Unlock()
	raw, err := os.ReadFile(s.path)
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			s.data = emptyData()
			return os.ErrNotExist
		}
		return err
	}
	var d Data
	if err := json.Unmarshal(raw, &d); err != nil {
		return err
	}
	if d.Users == nil {
		d.Users = map[string]User{}
	}
	if d.Tokens == nil {
		d.Tokens = map[string]TokenRecord{}
	}
	if d.Tenants == nil {
		d.Tenants = map[int64]Tenant{}
	}
	if d.Invites == nil {
		d.Invites = map[string]Invite{}
	}
	s.data = d
	return nil
}

func (s *Store) Save() error {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.saveLocked()
}

func (s *Store) saveLocked() error {
	raw, err := json.MarshalIndent(s.data, "", "  ")
	if err != nil {
		return err
	}
	tmp := s.path + ".tmp"
	if err := os.WriteFile(tmp, raw, 0o644); err != nil {
		return err
	}
	return os.Rename(tmp, s.path)
}

func (s *Store) WithLock(fn func(d *Data) error) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if err := fn(&s.data); err != nil {
		return err
	}
	return s.saveLocked()
}

func (s *Store) Snapshot() Data {
	s.mu.Lock()
	defer s.mu.Unlock()
	// shallow copy maps
	out := emptyData()
	out.Seq = s.data.Seq
	out.TenantSeq = s.data.TenantSeq
	for k, v := range s.data.Users {
		out.Users[k] = v
	}
	for k, v := range s.data.Tokens {
		out.Tokens[k] = v
	}
	for k, v := range s.data.Tenants {
		out.Tenants[k] = v
	}
	for k, v := range s.data.Invites {
		out.Invites[k] = v
	}
	return out
}

// MySQLOpen tries to open mysql DSN; returns error if driver not linked.
// Optional: build with -tags mysql and import go-sql-driver.
func MySQLOpen(ctx context.Context, dsn string) (*sql.DB, error) {
	return sql.Open("mysql", dsn)
}
