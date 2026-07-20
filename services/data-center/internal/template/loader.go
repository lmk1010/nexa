package template

import (
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"
)

// Registry 内存里的模板注册表, 启动加载, 全生命周期不变
type Registry struct {
	mu     sync.RWMutex
	byID   map[string]*Template
	sorted []string // 稳定顺序 (按 ID)
}

// Load 扫目录, 加载 *.json 模板
func Load(dir string) (*Registry, error) {
	entries, err := os.ReadDir(dir)
	if err != nil {
		return nil, fmt.Errorf("read templates dir %s: %w", dir, err)
	}
	r := &Registry{byID: map[string]*Template{}}
	for _, e := range entries {
		if e.IsDir() {
			continue
		}
		if !strings.HasSuffix(strings.ToLower(e.Name()), ".json") {
			continue
		}
		path := filepath.Join(dir, e.Name())
		b, err := os.ReadFile(path)
		if err != nil {
			return nil, fmt.Errorf("read %s: %w", path, err)
		}
		t, err := Parse(b)
		if err != nil {
			return nil, fmt.Errorf("parse %s: %w", e.Name(), err)
		}
		if _, exists := r.byID[t.ID]; exists {
			return nil, fmt.Errorf("duplicate template id: %s (in %s)", t.ID, e.Name())
		}
		r.byID[t.ID] = t
		r.sorted = append(r.sorted, t.ID)
	}
	sort.Strings(r.sorted)
	return r, nil
}

// Get 按 id 拿模板
func (r *Registry) Get(id string) (*Template, bool) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	t, ok := r.byID[id]
	return t, ok
}

// ListVisible 返回该用户能看到的所有模板
func (r *Registry) ListVisible(userDeptID int64, roles []string) []*Template {
	r.mu.RLock()
	defer r.mu.RUnlock()
	out := make([]*Template, 0, len(r.sorted))
	for _, id := range r.sorted {
		t := r.byID[id]
		if t.AllowFor(userDeptID, roles) {
			out = append(out, t)
		}
	}
	return out
}

// All 返回全部模板（管理员用）
func (r *Registry) All() []*Template {
	r.mu.RLock()
	defer r.mu.RUnlock()
	out := make([]*Template, 0, len(r.sorted))
	for _, id := range r.sorted {
		out = append(out, r.byID[id])
	}
	return out
}
