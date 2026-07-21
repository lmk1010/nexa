package main

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"time"

	bolt "go.etcd.io/bbolt"
)

var bktCore = []byte("core")

func openCoreBolt(path string) (*bolt.DB, error) {
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return nil, err
	}
	db, err := bolt.Open(path, 0o600, &bolt.Options{Timeout: 3 * time.Second})
	if err != nil {
		return nil, err
	}
	if err := db.Update(func(tx *bolt.Tx) error {
		_, err := tx.CreateBucketIfNotExists(bktCore)
		return err
	}); err != nil {
		_ = db.Close()
		return nil, err
	}
	return db, nil
}

func (s *store) loadFromBolt() error {
	if s.bolt == nil {
		return fmt.Errorf("bolt nil")
	}
	return s.bolt.View(func(tx *bolt.Tx) error {
		b := tx.Bucket(bktCore)
		raw := b.Get([]byte("db"))
		if len(raw) == 0 {
			return nil
		}
		return json.Unmarshal(raw, &s.db)
	})
}

func (s *store) saveToBolt() error {
	if s.bolt == nil {
		return fmt.Errorf("bolt nil")
	}
	raw, err := json.Marshal(s.db)
	if err != nil {
		return err
	}
	return s.bolt.Update(func(tx *bolt.Tx) error {
		return tx.Bucket(bktCore).Put([]byte("db"), raw)
	})
}

func resolveCoreBackend() string {
	if v := strings.TrimSpace(os.Getenv("NEXA_CORE_BACKEND")); v != "" {
		return strings.ToLower(v)
	}
	if v := strings.TrimSpace(os.Getenv("NEXA_DB_BACKEND")); v != "" {
		return strings.ToLower(v)
	}
	return "file"
}

func resolveCoreBoltPath(dataDir string) string {
	if v := os.Getenv("NEXA_CORE_BOLT"); v != "" {
		return v
	}
	return filepath.Join(dataDir, "core.bolt")
}

func (s *store) close() {
	if s.bolt != nil {
		_ = s.bolt.Close()
	}
}

// migrateJSONToBolt copies existing core.json into bolt if bolt empty.
func (s *store) migrateJSONToBolt(jsonPath string) {
	if s.bolt == nil {
		return
	}
	empty := true
	_ = s.bolt.View(func(tx *bolt.Tx) error {
		if len(tx.Bucket(bktCore).Get([]byte("db"))) > 0 {
			empty = false
		}
		return nil
	})
	if !empty {
		return
	}
	raw, err := os.ReadFile(jsonPath)
	if err != nil || len(raw) == 0 {
		return
	}
	if err := json.Unmarshal(raw, &s.db); err != nil {
		log.Printf("[store] migrate json unmarshal: %v", err)
		return
	}
	if err := s.saveToBolt(); err != nil {
		log.Printf("[store] migrate save: %v", err)
		return
	}
	log.Printf("[store] migrated %s -> bolt", jsonPath)
}
