package filter

import (
	"strings"
	"sync"
)

// TableFilter is an exact schema.table allow-list (deny by default).
type TableFilter struct {
	mu   sync.RWMutex
	set  map[string]struct{}
}

// New builds a filter from "schema.table" entries (case-insensitive).
func New(tables []string) *TableFilter {
	f := &TableFilter{set: make(map[string]struct{}, len(tables))}
	for _, t := range tables {
		k := normalize(t)
		if k == "" {
			continue
		}
		f.set[k] = struct{}{}
	}
	return f
}

// Allow reports whether schema.table is included.
func (f *TableFilter) Allow(schema, table string) bool {
	f.mu.RLock()
	defer f.mu.RUnlock()
	_, ok := f.set[normalize(schema+"."+table)]
	return ok
}

// Size returns the number of allowed tables.
func (f *TableFilter) Size() int {
	f.mu.RLock()
	defer f.mu.RUnlock()
	return len(f.set)
}

func normalize(s string) string {
	return strings.ToLower(strings.TrimSpace(s))
}
