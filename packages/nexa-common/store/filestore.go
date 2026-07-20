package store

import (
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
	"sync"
)

// FileStore is a tiny JSON document store for local/dev persistence.
type FileStore[T any] struct {
	path string
	mu   sync.Mutex
}

func NewFileStore[T any](path string) *FileStore[T] {
	return &FileStore[T]{path: path}
}

func (s *FileStore[T]) Load(dst *T) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	raw, err := os.ReadFile(s.path)
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return os.ErrNotExist
		}
		return err
	}
	return json.Unmarshal(raw, dst)
}

func (s *FileStore[T]) Save(v T) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if err := os.MkdirAll(filepath.Dir(s.path), 0o755); err != nil {
		return err
	}
	raw, err := json.MarshalIndent(v, "", "  ")
	if err != nil {
		return err
	}
	tmp := s.path + ".tmp"
	if err := os.WriteFile(tmp, raw, 0o644); err != nil {
		return err
	}
	return os.Rename(tmp, s.path)
}
