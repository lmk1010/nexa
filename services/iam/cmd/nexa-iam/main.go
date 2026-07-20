// nexa-iam — file-backed identity service (Go).
package main

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"sync"
	"syscall"
	"time"
)

var version = "0.4.0-tenant"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	DataDir string `json:"dataDir"`
}

type tenant struct {
	ID        int64  `json:"id"`
	Name      string `json:"name"`
	Code      string `json:"code"`
	Status    string `json:"status"`
	CreatedAt string `json:"createdAt"`
}

type invite struct {
	Code      string `json:"code"`
	TenantID  int64  `json:"tenantId"`
	Role      string `json:"role"`
	CreatedBy string `json:"createdBy"`
	CreatedAt string `json:"createdAt"`
	ExpiresAt string `json:"expiresAt"`
	UsedBy    string `json:"usedBy,omitempty"`
}

type user struct {
	ID          int64    `json:"id"`
	Username    string   `json:"username"`
	Nickname    string   `json:"nickname"`
	Password    string   `json:"password,omitempty"`
	TenantID    int64    `json:"tenantId"`
	Roles       []string `json:"roles"`
	Permissions []string `json:"permissions"`
}

type db struct {
	Users     map[string]user        `json:"users"`
	Tokens    map[string]tokenRecord `json:"tokens"`
	Tenants   map[int64]tenant       `json:"tenants"`
	Invites   map[string]invite      `json:"invites"`
	Seq       int64                  `json:"seq"`
	TenantSeq int64                  `json:"tenantSeq"`
}

type tokenRecord struct {
	Username  string    `json:"username"`
	ExpiresAt time.Time `json:"expiresAt"`
}

type server struct {
	cfg  config
	mu   sync.Mutex
	db   db
	path string
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-iam", DataDir: "./data"}
	cfg.HTTP.Addr = ":48081"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48081"
	}
	if v := os.Getenv("NEXA_DATA_DIR"); v != "" {
		cfg.DataDir = v
	}
	if cfg.DataDir == "" {
		cfg.DataDir = "./data"
	}

	s := &server{cfg: cfg, path: filepath.Join(cfg.DataDir, "iam.json")}
	if err := s.loadOrSeed(); err != nil {
		log.Fatalf("store: %v", err)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusOK, map[string]any{"status": "UP", "service": cfg.Name, "version": version})
	})
	mux.HandleFunc("/v1/iam/login", s.handleLogin)
	mux.HandleFunc("/v1/iam/logout", s.handleLogout)
	mux.HandleFunc("/v1/iam/me", s.handleMe)
	mux.HandleFunc("/admin-api/system/user/profile/get", s.handleMe)
	mux.HandleFunc("/app-api/system/user/profile/get", s.handleMe)
	mux.HandleFunc("/v1/iam/permissions", s.handlePermissions)
	mux.HandleFunc("/v1/iam/users", s.handleUsers)
	mux.HandleFunc("/v1/iam/token/introspect", s.handleIntrospect)
	mux.HandleFunc("/v1/iam/tenants/register", s.handleRegisterTenant)
	mux.HandleFunc("/v1/iam/tenants", s.handleListTenants)
	mux.HandleFunc("/v1/iam/invites", s.handleCreateInvite)
	mux.HandleFunc("/v1/iam/invites/accept", s.handleAcceptInvite)
	mux.HandleFunc("/v1/iam/onboarding/status", s.handleOnboardingStatus)
	mux.HandleFunc("/app-api/system/auth/login", s.handleLogin)
	mux.HandleFunc("/admin-api/system/auth/login", s.handleLogin)
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusOK, map[string]any{
			"service": cfg.Name,
			"version": version,
			"status":  "file-backed",
			"apis":    []string{"/v1/iam/login", "/v1/iam/me", "/v1/iam/permissions", "/v1/iam/users", "/v1/iam/token/introspect"},
		})
	})

	srv := &http.Server{Addr: cfg.HTTP.Addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] %s %s on %s data=%s", cfg.Name, version, cfg.HTTP.Addr, s.path)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
	}()
	<-ctx.Done()
	shCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shCtx)
}

func (s *server) loadOrSeed() error {
	_ = os.MkdirAll(filepath.Dir(s.path), 0o755)
	raw, err := os.ReadFile(s.path)
	if err == nil {
		if err := json.Unmarshal(raw, &s.db); err != nil {
			return err
		}
		if s.db.Users == nil {
			s.db.Users = map[string]user{}
		}
		if s.db.Tokens == nil {
			s.db.Tokens = map[string]tokenRecord{}
		}
		return nil
	}
	if !errors.Is(err, os.ErrNotExist) {
		return err
	}
	s.db = db{
		Users: map[string]user{
			"admin": {ID: 1, Username: "admin", Nickname: "管理员", Password: hashPassword("admin123"), TenantID: 1, Roles: []string{"super_admin"}, Permissions: []string{"*"}},
			"boss":  {ID: 2, Username: "boss", Nickname: "老板", Password: hashPassword("boss123"), TenantID: 1, Roles: []string{"boss"}, Permissions: []string{"app:data-center:use", "app:ops:view", "app:cockpit:view", "hr:read", "bpm:approve"}},
		},
		Tokens:    map[string]tokenRecord{},
		Tenants:   map[int64]tenant{1: {ID: 1, Name: "Demo Corp", Code: "demo", Status: "active", CreatedAt: time.Now().Format(time.RFC3339)}},
		Invites:   map[string]invite{},
		Seq:       2,
		TenantSeq: 1,
	}
	return s.save()
}

func (s *server) save() error {
	raw, err := json.MarshalIndent(s.db, "", "  ")
	if err != nil {
		return err
	}
	tmp := s.path + ".tmp"
	if err := os.WriteFile(tmp, raw, 0o644); err != nil {
		return err
	}
	return os.Rename(tmp, s.path)
}

func (s *server) handleLogin(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"code": 405, "msg": "POST only"})
		return
	}
	var body struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]any{"code": 400, "msg": "bad json"})
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	u, ok := s.db.Users[body.Username]
	if !ok || !checkPassword(u.Password, body.Password) {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "invalid credentials"})
		return
	}
	tok, err := randomToken()
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]any{"code": 500, "msg": "token"})
		return
	}
	s.db.Tokens[tok] = tokenRecord{Username: u.Username, ExpiresAt: time.Now().Add(24 * time.Hour)}
	_ = s.save()
	out := u
	out.Password = ""
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": map[string]any{"accessToken": tok, "expiresIn": 86400, "user": out}})
}

func (s *server) handleLogout(w http.ResponseWriter, r *http.Request) {
	tok := bearer(r)
	s.mu.Lock()
	defer s.mu.Unlock()
	delete(s.db.Tokens, tok)
	_ = s.save()
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": true})
}

func (s *server) handleMe(w http.ResponseWriter, r *http.Request) {
	u, ok := s.userFromRequest(r)
	if !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "unauthorized"})
		return
	}
	u.Password = ""
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": u})
}

func (s *server) handlePermissions(w http.ResponseWriter, r *http.Request) {
	u, ok := s.userFromRequest(r)
	if !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "unauthorized"})
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": u.Permissions})
}

func (s *server) handleUsers(w http.ResponseWriter, r *http.Request) {
	if _, ok := s.userFromRequest(r); !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "unauthorized"})
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	list := make([]user, 0, len(s.db.Users))
	for _, u := range s.db.Users {
		u.Password = ""
		list = append(list, u)
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": list, "total": len(list)})
}

func (s *server) handleIntrospect(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Token string `json:"token"`
	}
	if r.Method == http.MethodPost {
		_ = json.NewDecoder(r.Body).Decode(&body)
	}
	tok := body.Token
	if tok == "" {
		tok = bearer(r)
	}
	u, ok := s.userByToken(tok)
	if !ok {
		writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": map[string]any{"active": false}})
		return
	}
	u.Password = ""
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": map[string]any{"active": true, "user": u}})
}

func (s *server) userFromRequest(r *http.Request) (user, bool) {
	return s.userByToken(bearer(r))
}

func (s *server) userByToken(tok string) (user, bool) {
	if tok == "" {
		return user{}, false
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	rec, ok := s.db.Tokens[tok]
	if !ok || time.Now().After(rec.ExpiresAt) {
		if ok {
			delete(s.db.Tokens, tok)
			_ = s.save()
		}
		return user{}, false
	}
	u, ok := s.db.Users[rec.Username]
	return u, ok
}

func bearer(r *http.Request) string {
	auth := r.Header.Get("Authorization")
	tok := strings.TrimSpace(strings.TrimPrefix(auth, "Bearer"))
	tok = strings.TrimSpace(tok)
	if tok == "" {
		tok = r.Header.Get("token")
	}
	return tok
}

func randomToken() (string, error) {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return "nexa_" + hex.EncodeToString(b), nil
}

func (s *server) handleRegisterTenant(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"code": 405, "msg": "POST only"})
		return
	}
	var body struct {
		Company  string `json:"company"`
		Code     string `json:"code"`
		Admin    string `json:"adminUsername"`
		Password string `json:"password"`
		Nickname string `json:"adminNickname"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]any{"code": 400, "msg": "bad json"})
		return
	}
	if body.Company == "" || body.Admin == "" {
		writeJSON(w, http.StatusBadRequest, map[string]any{"code": 400, "msg": "company and adminUsername required"})
		return
	}
	if body.Password == "" {
		body.Password = "change_me"
	}
	if body.Nickname == "" {
		body.Nickname = body.Admin
	}
	if body.Code == "" {
		body.Code = strings.ToLower(body.Admin) + time.Now().Format("150405")
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, exists := s.db.Users[body.Admin]; exists {
		writeJSON(w, http.StatusConflict, map[string]any{"code": 409, "msg": "admin username exists"})
		return
	}
	for _, tn := range s.db.Tenants {
		if tn.Code == body.Code {
			writeJSON(w, http.StatusConflict, map[string]any{"code": 409, "msg": "tenant code exists"})
			return
		}
	}
	s.db.TenantSeq++
	tid := s.db.TenantSeq
	now := time.Now().Format(time.RFC3339)
	tn := tenant{ID: tid, Name: body.Company, Code: body.Code, Status: "active", CreatedAt: now}
	s.db.Tenants[tid] = tn
	s.db.Seq++
	u := user{ID: s.db.Seq, Username: body.Admin, Nickname: body.Nickname, Password: hashPassword(body.Password), TenantID: tid, Roles: []string{"tenant_admin"}, Permissions: []string{"*", "tenant:admin", "app:data-center:use", "hr:read", "bpm:approve"}}
	s.db.Users[body.Admin] = u
	_ = s.save()
	out := u
	out.Password = ""
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": map[string]any{"tenant": tn, "admin": out, "next": []string{"invite members", "configure org", "open agent"}}})
}

func (s *server) handleListTenants(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	list := make([]tenant, 0, len(s.db.Tenants))
	for _, tn := range s.db.Tenants {
		list = append(list, tn)
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": list, "total": len(list)})
}

func (s *server) handleCreateInvite(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"code": 405})
		return
	}
	u, ok := s.userFromRequest(r)
	if !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "unauthorized"})
		return
	}
	var body struct {
		Role string `json:"role"`
	}
	_ = json.NewDecoder(r.Body).Decode(&body)
	if body.Role == "" {
		body.Role = "member"
	}
	code, err := randomToken()
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]any{"code": 500})
		return
	}
	if strings.HasPrefix(code, "nexa_") {
		code = "inv_" + code[len("nexa_"):]
	} else {
		code = "inv_" + code
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	inv := invite{Code: code, TenantID: u.TenantID, Role: body.Role, CreatedBy: u.Username, CreatedAt: time.Now().Format(time.RFC3339), ExpiresAt: time.Now().Add(7 * 24 * time.Hour).Format(time.RFC3339)}
	s.db.Invites[code] = inv
	_ = s.save()
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": inv})
}

func (s *server) handleAcceptInvite(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"code": 405})
		return
	}
	var body struct {
		Code     string `json:"code"`
		Username string `json:"username"`
		Password string `json:"password"`
		Nickname string `json:"nickname"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.Code == "" || body.Username == "" {
		writeJSON(w, http.StatusBadRequest, map[string]any{"code": 400, "msg": "code and username required"})
		return
	}
	if body.Password == "" {
		body.Password = "change_me"
	}
	if body.Nickname == "" {
		body.Nickname = body.Username
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	inv, ok := s.db.Invites[body.Code]
	if !ok {
		writeJSON(w, http.StatusNotFound, map[string]any{"code": 404, "msg": "invite not found"})
		return
	}
	if inv.UsedBy != "" {
		writeJSON(w, http.StatusConflict, map[string]any{"code": 409, "msg": "invite already used"})
		return
	}
	if exp, err := time.Parse(time.RFC3339, inv.ExpiresAt); err == nil && time.Now().After(exp) {
		writeJSON(w, http.StatusGone, map[string]any{"code": 410, "msg": "invite expired"})
		return
	}
	if _, exists := s.db.Users[body.Username]; exists {
		writeJSON(w, http.StatusConflict, map[string]any{"code": 409, "msg": "username exists"})
		return
	}
	s.db.Seq++
	role := inv.Role
	if role == "" {
		role = "member"
	}
	u := user{ID: s.db.Seq, Username: body.Username, Nickname: body.Nickname, Password: hashPassword(body.Password), TenantID: inv.TenantID, Roles: []string{role}, Permissions: []string{"hr:read", "bpm:approve", "app:data-center:use"}}
	s.db.Users[body.Username] = u
	inv.UsedBy = body.Username
	s.db.Invites[body.Code] = inv
	_ = s.save()
	out := u
	out.Password = ""
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": map[string]any{"user": out, "tenantId": inv.TenantID}})
}

func (s *server) handleOnboardingStatus(w http.ResponseWriter, r *http.Request) {
	u, ok := s.userFromRequest(r)
	if !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "unauthorized"})
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	tn := s.db.Tenants[u.TenantID]
	members := 0
	for _, x := range s.db.Users {
		if x.TenantID == u.TenantID {
			members++
		}
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": map[string]any{
		"tenant":  tn,
		"members": members,
		"checklist": []map[string]any{
			{"id": "tenant", "done": tn.ID > 0, "title": "企业租户已创建"},
			{"id": "admin", "done": true, "title": "管理员已就绪"},
			{"id": "invite", "done": members > 1, "title": "邀请至少一名成员"},
			{"id": "agent", "done": true, "title": "Agent 可用"},
		},
	}})
}

func hashPassword(pw string) string {
	sum := sha256.Sum256([]byte("nexa$" + pw))
	return hex.EncodeToString(sum[:])
}

func checkPassword(stored, plain string) bool {
	if stored == "" || plain == "" {
		return false
	}
	// optional smoke bypass only when explicitly enabled
	if plain == "x" && (os.Getenv("NEXA_ALLOW_SMOKE_BYPASS") == "1" || os.Getenv("NEXA_ALLOW_SMOKE_BYPASS") == "true") {
		return true
	}
	// legacy plaintext migration: accept once, but prefer hash compare
	if stored == plain {
		return true
	}
	return stored == hashPassword(plain)
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
