from pathlib import Path

p = Path("E:/code/nexa/services/iam/cmd/nexa-iam/main.go")
t = p.read_text(encoding="utf-8")
if "handleRegisterTenant" in t:
    print("already has tenant APIs")
    raise SystemExit(0)

t = t.replace('var version = "0.3.0-m2"', 'var version = "0.4.0-tenant"')
t = t.replace('var version = "0.3.0-m2"', 'var version = "0.4.0-tenant"')

if "type tenant struct" not in t:
    t = t.replace(
        "type user struct {",
        """type tenant struct {
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

type user struct {""",
    )

t = t.replace(
    """type db struct {
	Users  map[string]user            `json:"users"`
	Tokens map[string]tokenRecord     `json:"tokens"`
	Seq    int64                      `json:"seq"`
}""",
    """type db struct {
	Users     map[string]user        `json:"users"`
	Tokens    map[string]tokenRecord `json:"tokens"`
	Tenants   map[int64]tenant       `json:"tenants"`
	Invites   map[string]invite      `json:"invites"`
	Seq       int64                  `json:"seq"`
	TenantSeq int64                  `json:"tenantSeq"`
}""",
)

# seed
old_seed = """	s.db = db{
		Users: map[string]user{
			"admin": {ID: 1, Username: "admin", Nickname: "管理员", Password: "admin123", TenantID: 1, Roles: []string{"super_admin"}, Permissions: []string{"*"}},
			"boss":  {ID: 2, Username: "boss", Nickname: "老板", Password: "boss123", TenantID: 1, Roles: []string{"boss"}, Permissions: []string{"app:data-center:use", "app:ops:view", "app:cockpit:view", "hr:read", "bpm:approve"}},
		},
		Tokens: map[string]tokenRecord{},
		Seq:    2,
	}"""
new_seed = """	s.db = db{
		Users: map[string]user{
			"admin": {ID: 1, Username: "admin", Nickname: "管理员", Password: "admin123", TenantID: 1, Roles: []string{"super_admin"}, Permissions: []string{"*"}},
			"boss":  {ID: 2, Username: "boss", Nickname: "老板", Password: "boss123", TenantID: 1, Roles: []string{"boss"}, Permissions: []string{"app:data-center:use", "app:ops:view", "app:cockpit:view", "hr:read", "bpm:approve"}},
		},
		Tokens:    map[string]tokenRecord{},
		Tenants:   map[int64]tenant{1: {ID: 1, Name: "Demo Corp", Code: "demo", Status: "active", CreatedAt: time.Now().Format(time.RFC3339)}},
		Invites:   map[string]invite{},
		Seq:       2,
		TenantSeq: 1,
	}"""
if old_seed in t:
    t = t.replace(old_seed, new_seed)
else:
    print("seed block not exact; trying partial")

t = t.replace(
    """	if s.db.Tokens == nil {
		s.db.Tokens = map[string]tokenRecord{}
	}
	return nil""",
    """	if s.db.Tokens == nil {
		s.db.Tokens = map[string]tokenRecord{}
	}
	if s.db.Tenants == nil {
		s.db.Tenants = map[int64]tenant{}
	}
	if s.db.Invites == nil {
		s.db.Invites = map[string]invite{}
	}
	return nil""",
)

t = t.replace(
    'mux.HandleFunc("/v1/iam/token/introspect", s.handleIntrospect)',
    """mux.HandleFunc("/v1/iam/token/introspect", s.handleIntrospect)
	mux.HandleFunc("/v1/iam/tenants/register", s.handleRegisterTenant)
	mux.HandleFunc("/v1/iam/tenants", s.handleListTenants)
	mux.HandleFunc("/v1/iam/invites", s.handleCreateInvite)
	mux.HandleFunc("/v1/iam/invites/accept", s.handleAcceptInvite)
	mux.HandleFunc("/v1/iam/onboarding/status", s.handleOnboardingStatus)""",
)

handlers = r'''
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
	u := user{ID: s.db.Seq, Username: body.Admin, Nickname: body.Nickname, Password: body.Password, TenantID: tid, Roles: []string{"tenant_admin"}, Permissions: []string{"*", "tenant:admin", "app:data-center:use", "hr:read", "bpm:approve"}}
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
	u := user{ID: s.db.Seq, Username: body.Username, Nickname: body.Nickname, Password: body.Password, TenantID: inv.TenantID, Roles: []string{role}, Permissions: []string{"hr:read", "bpm:approve", "app:data-center:use"}}
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

'''

if "func writeJSON(w http.ResponseWriter, status int, v any)" in t:
    t = t.replace(
        "func writeJSON(w http.ResponseWriter, status int, v any) {",
        handlers + "\nfunc writeJSON(w http.ResponseWriter, status int, v any) {",
        1,
    )
else:
    print("writeJSON not found")

# ensure strings import
if '"strings"' not in t:
    t = t.replace('\t"sync"\n', '\t"strings"\n\t"sync"\n')

p.write_text(t, encoding="utf-8")
print("iam tenant written")
