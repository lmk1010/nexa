package metrics

import (
	"encoding/json"
	"net/http"
	"sync/atomic"
	"time"
)

// Registry is a minimal process metrics holder (Prometheus later).
type Registry struct {
	startedAt   time.Time
	eventsTotal atomic.Uint64
	errorsTotal atomic.Uint64
}

func New() *Registry {
	return &Registry{startedAt: time.Now()}
}

func (r *Registry) IncEvents() { r.eventsTotal.Add(1) }
func (r *Registry) IncErrors() { r.errorsTotal.Add(1) }

// Handler serves JSON health/metrics.
func (r *Registry) Handler() http.HandlerFunc {
	return func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]any{
			"status":       "UP",
			"service":      "nexa-cdc-mysql",
			"uptime_sec":   int(time.Since(r.startedAt).Seconds()),
			"events_total": r.eventsTotal.Load(),
			"errors_total": r.errorsTotal.Load(),
			"mode":         "skeleton",
		})
	}
}
