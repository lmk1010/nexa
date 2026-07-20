package tenant

import (
	"context"
	"net/http"
	"strconv"
	"strings"
)

type ctxKey string

const ContextKey ctxKey = "nexaTenant"

// Info is the tenant/user context extracted from gateway headers or claims.
type Info struct {
	TenantID int64
	UserID   int64
	Username string
}

func WithInfo(ctx context.Context, info Info) context.Context {
	return context.WithValue(ctx, ContextKey, info)
}

func FromContext(ctx context.Context) (Info, bool) {
	v, ok := ctx.Value(ContextKey).(Info)
	return v, ok
}

// FromRequest reads common nexa/gateway headers.
// Supported:
//   - X-Tenant-Id / tenant-id
//   - X-User-Id / login-user-id
//   - X-Username
func FromRequest(r *http.Request) Info {
	info := Info{}
	info.TenantID = parseInt64(first(r.Header.Get("X-Tenant-Id"), r.Header.Get("tenant-id")))
	info.UserID = parseInt64(first(r.Header.Get("X-User-Id"), r.Header.Get("login-user-id")))
	info.Username = first(r.Header.Get("X-Username"), r.Header.Get("username"))
	return info
}

// Middleware injects tenant info into request context.
func Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		info := FromRequest(r)
		next.ServeHTTP(w, r.WithContext(WithInfo(r.Context(), info)))
	})
}

func first(vals ...string) string {
	for _, v := range vals {
		if strings.TrimSpace(v) != "" {
			return strings.TrimSpace(v)
		}
	}
	return ""
}

func parseInt64(s string) int64 {
	if s == "" {
		return 0
	}
	n, _ := strconv.ParseInt(s, 10, 64)
	return n
}
