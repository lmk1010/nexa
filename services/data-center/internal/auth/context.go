package auth

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"net/http"
	"net/url"
	"strconv"
	"strings"
)

// Principal 是从 gateway 透传 login-user header 解析出的当前用户
type Principal struct {
	UserID    int64    `json:"userId"`
	UserName  string   `json:"username"`
	DeptID    int64    `json:"deptId,omitempty"`
	TenantID  int64    `json:"tenantId,omitempty"`
	Roles     []string `json:"roles,omitempty"`
	IsAdmin   bool     `json:"-"`
}

type ctxKey struct{}

// FromContext 拿当前 principal
func FromContext(ctx context.Context) (*Principal, bool) {
	p, ok := ctx.Value(ctxKey{}).(*Principal)
	return p, ok
}

// MustFromContext 拿不到就 panic（handler 已经过中间件必定有）
func MustFromContext(ctx context.Context) *Principal {
	p, ok := FromContext(ctx)
	if !ok {
		panic("no principal in context")
	}
	return p
}

// Middleware 解析 login-user header 塞进 context
// login-user 由 kyx-gateway 生成, JSON base64 编码
func Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		p, err := parseLoginUser(r)
		if err != nil {
			// 401 时 dump 请求头方便排查
			var keys []string
			for k := range r.Header {
				keys = append(keys, k)
			}
			log.Printf("[auth] 401 %s %s | err=%v | headers=%v | login-user=%q",
				r.Method, r.URL.Path, err, keys, r.Header.Get("login-user"))
			http.Error(w, `{"code":401,"msg":"账号未登录"}`, http.StatusUnauthorized)
			return
		}
		ctx := context.WithValue(r.Context(), ctxKey{}, p)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// gateway 实际透传的 login-user (URL-encoded JSON) 结构:
//
//	{"id":1111,"userType":2,"info":{"nickname":"刘明康","deptId":"412604253"},
//	 "tenantId":171,"scopes":null,"expiresTime":1783675775928}
//
// 其中 id = userId, nickname/deptId 埋在 info 里, deptId 还是 string.
type rawLoginUser struct {
	ID       any    `json:"id"`
	UserID   any    `json:"userId"` // 老结构兜底
	UserType int    `json:"userType"`
	TenantID any    `json:"tenantId"`
	Scopes   []any  `json:"scopes"`
	Info     struct {
		Nickname string `json:"nickname"`
		Username string `json:"username"`
		UserName string `json:"userName"`
		DeptID   any    `json:"deptId"`
	} `json:"info"`
	// 平铺兜底 (旧接口)
	Nickname string `json:"nickname"`
	Username string `json:"username"`
	DeptID   any    `json:"deptId"`
	Roles    []any  `json:"roles"`
}

func parseLoginUser(r *http.Request) (*Principal, error) {
	h := r.Header.Get("login-user")
	if h == "" {
		// dev 兜底: 简单 header
		uid, _ := strconv.ParseInt(r.Header.Get("user-id"), 10, 64)
		if uid == 0 {
			return nil, errors.New("no login-user header")
		}
		return &Principal{
			UserID:   uid,
			UserName: r.Header.Get("user-name"),
			IsAdmin:  strings.EqualFold(r.Header.Get("role"), "admin"),
		}, nil
	}
	// 先尝试 URL decode (gateway 会 %XX 编码整个 JSON)
	decoded := h
	if strings.Contains(h, "%") {
		if u, err := url.QueryUnescape(h); err == nil {
			decoded = u
		}
	}
	var raw rawLoginUser
	if err := json.Unmarshal([]byte(decoded), &raw); err != nil {
		return nil, err
	}
	p := &Principal{
		UserID:   asInt64(raw.ID, raw.UserID),
		TenantID: asInt64(raw.TenantID),
		UserName: firstNonEmpty(raw.Info.Nickname, raw.Info.Username, raw.Info.UserName, raw.Nickname, raw.Username),
		DeptID:   asInt64(raw.Info.DeptID, raw.DeptID),
	}
	for _, r := range raw.Roles {
		if s, ok := r.(string); ok {
			p.Roles = append(p.Roles, s)
			if strings.Contains(strings.ToUpper(s), "ADMIN") {
				p.IsAdmin = true
			}
		}
	}
	// userType==1 通常代表 admin (kyx 网关约定); 具体以 roles 优先
	if !p.IsAdmin && raw.UserType == 1 {
		p.IsAdmin = true
	}
	if p.UserID == 0 {
		return nil, errors.New("login-user 缺 id")
	}
	return p, nil
}

// asInt64 从 any (可能是 float64 / string / int) 转 int64, 从多个候选里挑第一个非零
func asInt64(candidates ...any) int64 {
	for _, v := range candidates {
		switch x := v.(type) {
		case float64:
			if int64(x) != 0 {
				return int64(x)
			}
		case int:
			if x != 0 {
				return int64(x)
			}
		case int64:
			if x != 0 {
				return x
			}
		case string:
			if n, err := strconv.ParseInt(x, 10, 64); err == nil && n != 0 {
				return n
			}
		}
	}
	return 0
}

func firstNonEmpty(ss ...string) string {
	for _, s := range ss {
		if s != "" {
			return s
		}
	}
	return ""
}
