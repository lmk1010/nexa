from pathlib import Path

# --- Enhance IM in domains.go ---
p = Path("E:/code/nexa/services/core/cmd/nexa-core/domains.go")
t = p.read_text(encoding="utf-8")
start = t.find("func registerIM")
end = t.find("func registerOP")
if start < 0 or end < 0:
    raise SystemExit("im markers missing")

new_im = r'''func registerIM(mux *http.ServeMux, s *store) {
	listConv := func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		out := make([]conv, 0)
		for _, x := range s.db.Conversations {
			if matchTenant(tid, x.TenantID) {
				out = append(out, x)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
	}
	createConv := func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body struct {
			Title string `json:"title"`
		}
		if json.NewDecoder(r.Body).Decode(&body) != nil || body.Title == "" {
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "title required"})
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		now := time.Now().Format(time.RFC3339)
		c := conv{ID: "c" + time.Now().Format("150405"), TenantID: tenantID(r), Title: body.Title, Unread: 0, UpdatedAt: now}
		s.db.Conversations = append(s.db.Conversations, c)
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": c})
	}
	listMsg := func(w http.ResponseWriter, r *http.Request) {
		cid := r.URL.Query().Get("conversationId")
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		out := make([]msg, 0)
		for _, m := range s.db.Messages {
			if !matchTenant(tid, m.TenantID) {
				continue
			}
			if cid == "" || m.ConversationID == cid {
				out = append(out, m)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
	}
	sendMsg := func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body struct {
			ConversationID string `json:"conversationId"`
			From           string `json:"from"`
			Text           string `json:"text"`
		}
		if json.NewDecoder(r.Body).Decode(&body) != nil || body.Text == "" {
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "text required"})
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		if body.From == "" {
			body.From = r.Header.Get("X-Username")
		}
		if body.ConversationID == "" {
			// auto create conversation
			c := conv{ID: "c" + time.Now().Format("150405"), TenantID: tid, Title: "Chat", Unread: 0, UpdatedAt: time.Now().Format(time.RFC3339)}
			s.db.Conversations = append(s.db.Conversations, c)
			body.ConversationID = c.ID
		} else {
			// ensure conversation belongs to tenant or create shadow
			found := false
			for _, c := range s.db.Conversations {
				if c.ID == body.ConversationID && matchTenant(tid, c.TenantID) {
					found = true
					break
				}
			}
			if !found {
				writeJSON(w, 404, map[string]any{"code": 404, "msg": "conversation not found"})
				return
			}
		}
		now := time.Now().Format(time.RFC3339)
		m := msg{ID: "m" + time.Now().Format("150405.000"), TenantID: tid, ConversationID: body.ConversationID, From: body.From, Text: body.Text, At: now}
		s.db.Messages = append(s.db.Messages, m)
		for i := range s.db.Conversations {
			if s.db.Conversations[i].ID == body.ConversationID {
				s.db.Conversations[i].UpdatedAt = now
				s.db.Conversations[i].Unread++
			}
		}
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": m})
	}
	readConv := func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body struct {
			ConversationID string `json:"conversationId"`
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		for i := range s.db.Conversations {
			if s.db.Conversations[i].ID == body.ConversationID && matchTenant(tid, s.db.Conversations[i].TenantID) {
				s.db.Conversations[i].Unread = 0
				_ = s.save()
				writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Conversations[i]})
				return
			}
		}
		writeJSON(w, 404, map[string]any{"code": 404})
	}
	// contacts from tenant employees
	contacts := func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		out := make([]map[string]any, 0)
		for _, e := range s.db.Employees {
			if matchTenant(tid, e.TenantID) {
				out = append(out, map[string]any{"id": e.ID, "name": e.Name, "dept": e.DeptName, "jobNo": e.JobNo})
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
	}

	jsonAlias(mux, []string{"/v1/im/conversations"}, func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodPost {
			createConv(w, r)
			return
		}
		listConv(w, r)
	})
	jsonAlias(mux, []string{"/v1/im/contacts"}, contacts)
	jsonAlias(mux, []string{"/v1/im/messages"}, listMsg)
	jsonAlias(mux, []string{"/v1/im/messages/send"}, sendMsg)
	jsonAlias(mux, []string{"/v1/im/conversations/read"}, readConv)
}

'''

t = Path("E:/code/nexa/services/core/cmd/nexa-core/domains.go").read_text(encoding="utf-8")
start = t.find("func registerIM")
end = t.find("func registerOP")
t = t[:start] + new_im + t[end:]
Path("E:/code/nexa/services/core/cmd/nexa-core/domains.go").write_text(t, encoding="utf-8")
print("im enhanced")

# --- IAM password change + tenant user list filter ---
ip = Path("E:/code/nexa/services/iam/cmd/nexa-iam/main.go")
it = ip.read_text(encoding="utf-8")
if "handleChangePassword" not in it:
    it = it.replace(
        'mux.HandleFunc("/v1/iam/onboarding/status", s.handleOnboardingStatus)',
        '''mux.HandleFunc("/v1/iam/onboarding/status", s.handleOnboardingStatus)
	mux.HandleFunc("/v1/iam/password/change", s.handleChangePassword)''',
    )
    # filter users by tenant
    old_users = '''s.mu.Lock()
	defer s.mu.Unlock()
	list := make([]user, 0, len(s.db.Users))
	for _, u := range s.db.Users {
		u.Password = ""
		list = append(list, u)
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": list, "total": len(list)})'''
    new_users = '''me, ok := s.userFromRequest(r)
	if !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "unauthorized"})
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	list := make([]user, 0)
	for _, u := range s.db.Users {
		if u.TenantID != me.TenantID && me.TenantID != 0 {
			continue
		}
		u.Password = ""
		list = append(list, u)
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": list, "total": len(list)})'''
    # handleUsers currently double-checks auth - replace body after first auth check
    if "for _, u := range s.db.Users" in it and "u.TenantID != me.TenantID" not in it:
        it = it.replace(
            '''func (s *server) handleUsers(w http.ResponseWriter, r *http.Request) {
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
}''',
            '''func (s *server) handleUsers(w http.ResponseWriter, r *http.Request) {
	me, ok := s.userFromRequest(r)
	if !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "unauthorized"})
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	list := make([]user, 0)
	for _, u := range s.db.Users {
		if me.TenantID != 0 && u.TenantID != me.TenantID {
			continue
		}
		u.Password = ""
		list = append(list, u)
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": list, "total": len(list)})
}''',
        )
        print("users tenant filter")

    handler = r'''
func (s *server) handleChangePassword(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"code": 405})
		return
	}
	me, ok := s.userFromRequest(r)
	if !ok {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "unauthorized"})
		return
	}
	var body struct {
		OldPassword string `json:"oldPassword"`
		NewPassword string `json:"newPassword"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.NewPassword == "" {
		writeJSON(w, http.StatusBadRequest, map[string]any{"code": 400, "msg": "newPassword required"})
		return
	}
	if len(body.NewPassword) < 6 {
		writeJSON(w, http.StatusBadRequest, map[string]any{"code": 400, "msg": "password min 6 chars"})
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	u, ok := s.db.Users[me.Username]
	if !ok || !checkPassword(u.Password, body.OldPassword) {
		writeJSON(w, http.StatusUnauthorized, map[string]any{"code": 401, "msg": "old password incorrect"})
		return
	}
	u.Password = hashPassword(body.NewPassword)
	s.db.Users[me.Username] = u
	_ = s.save()
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": true})
}

'''
    if "handleChangePassword" not in it:
        it = it.replace(
            "func hashPassword(pw string) string {",
            handler + "\nfunc hashPassword(pw string) string {",
        )
        print("password change added")
    ip.write_text(it, encoding="utf-8")
else:
    print("password change exists")

# OP audit append on login? skip for now - enhance op audit write in core
# add simple audit list with tenant optional - skip

print("done")
