// 权限校验：查 kyx_oa 里当前用户是否属于 dashboard 白名单角色，
// 或者已被赋 app:data-center:use 权限点。带 per-user 60s 缓存。
//
// 数据看板 / 运维监控 / 数据中心 这三块要求 admin 或老板才能用，前端隐藏 + 后端校验双保险。
// 数据中心特别做成 permission point，方便未来在 OA 后台把权限点分给具体人。

package auth

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

const (
	// 数据中心权限点 —— OA 后台 "APP 权限管理 → 数据中心导出" 那条。
	// 检查权限点而非角色，OA admin 可在后台任意分配给某个人/角色，不用改代码。
	DataCenterPermission = "app:data-center:use"

	cacheTTL = 60 * time.Second
)

var (
	dbOnce sync.Once
	oaDB   *sql.DB
	dbErr  error

	cacheMu sync.RWMutex
	cache   = map[int64]*cachedAccess{}
)

type cachedAccess struct {
	at          time.Time
	roles       map[string]struct{}
	permissions map[string]struct{}
}

func openOaDB() (*sql.DB, error) {
	dbOnce.Do(func() {
		dsn := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?parseTime=true&timeout=3s",
			envOr("OA_DB_USER", "kyx_user"),
			envOr("OA_DB_PASS", "kyx123456"),
			envOr("OA_DB_HOST", "kyx-mysql-master"),
			envOr("OA_DB_PORT", "3306"),
			envOr("OA_DB_NAME", "kyx_oa"),
		)
		db, err := sql.Open("mysql", dsn)
		if err != nil {
			dbErr = err
			return
		}
		db.SetMaxOpenConns(3)
		db.SetConnMaxLifetime(30 * time.Minute)
		oaDB = db
	})
	return oaDB, dbErr
}

func envOr(k, fallback string) string {
	v := strings.TrimSpace(os.Getenv(k))
	if v == "" {
		return fallback
	}
	return v
}

// LoadUserAccess 查（或读缓存）某用户的角色 + 权限集合。
func LoadUserAccess(ctx context.Context, userID int64) (roles map[string]struct{}, perms map[string]struct{}, err error) {
	if userID == 0 {
		return map[string]struct{}{}, map[string]struct{}{}, nil
	}
	cacheMu.RLock()
	c := cache[userID]
	cacheMu.RUnlock()
	if c != nil && time.Since(c.at) < cacheTTL {
		return c.roles, c.permissions, nil
	}

	db, err := openOaDB()
	if err != nil {
		return nil, nil, err
	}
	q := `SELECT DISTINCT r.code, COALESCE(m.permission, '')
	      FROM system_user_role ur
	      JOIN system_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 0
	      LEFT JOIN system_role_menu rm ON rm.role_id = r.id AND rm.deleted = 0
	      LEFT JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = 0 AND m.status = 0
	      WHERE ur.user_id = ? AND ur.deleted = 0`
	ctx2, cancel := context.WithTimeout(ctx, 3*time.Second)
	defer cancel()
	rows, err := db.QueryContext(ctx2, q, userID)
	if err != nil {
		return nil, nil, err
	}
	defer rows.Close()
	roles = map[string]struct{}{}
	perms = map[string]struct{}{}
	for rows.Next() {
		var code, perm string
		if err := rows.Scan(&code, &perm); err != nil {
			return nil, nil, err
		}
		if code != "" {
			roles[code] = struct{}{}
		}
		if perm != "" {
			perms[perm] = struct{}{}
		}
	}
	cacheMu.Lock()
	cache[userID] = &cachedAccess{at: time.Now(), roles: roles, permissions: perms}
	cacheMu.Unlock()
	return roles, perms, nil
}

// CanUseDataCenter 只检查 app:data-center:use 权限点。
// OA 后台"角色权限管理"里勾谁 = 谁能用，不用改代码/发版。
func CanUseDataCenter(ctx context.Context, userID int64) (bool, error) {
	_, perms, err := LoadUserAccess(ctx, userID)
	if err != nil {
		return false, err
	}
	_, ok := perms[DataCenterPermission]
	return ok, nil
}

// RequireDataCenterAccess 中间件：必须已 Middleware 解析 Principal，
// 校验用户拥有 app:data-center:use 权限（或白名单角色）才放行。
func RequireDataCenterAccess(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		p, ok := FromContext(r.Context())
		if !ok || p == nil {
			http.Error(w, `{"code":401,"msg":"账号未登录"}`, http.StatusUnauthorized)
			return
		}
		ok2, err := CanUseDataCenter(r.Context(), p.UserID)
		if err != nil {
			log.Printf("[auth] permission check failed uid=%d: %v", p.UserID, err)
			http.Error(w, `{"code":500,"msg":"权限校验失败"}`, http.StatusInternalServerError)
			return
		}
		if !ok2 {
			http.Error(w, `{"code":403,"msg":"无权访问数据中心，请联系管理员授予权限"}`, http.StatusForbidden)
			return
		}
		next.ServeHTTP(w, r)
	})
}

// InvalidateCache 手动清缓存（未来加"重置权限"端点用）
func InvalidateCache(userID int64) {
	cacheMu.Lock()
	if userID == 0 {
		cache = map[int64]*cachedAccess{}
	} else {
		delete(cache, userID)
	}
	cacheMu.Unlock()
}

// asInt64 helper (compat)
var _ = strconv.ParseInt
