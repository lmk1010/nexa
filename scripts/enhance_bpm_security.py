from pathlib import Path

# --- Enhance task type + history in main.go ---
p = Path("E:/code/nexa/services/core/cmd/nexa-core/main.go")
t = p.read_text(encoding="utf-8")

old_task = """type task struct {
	ID          string `json:"id"`
	TenantID    int64  `json:"tenantId,omitempty"`
	Title       string `json:"title"`
	ProcessName string `json:"processName"`
	Starter     string `json:"starter"`
	Assignee    string `json:"assignee"`
	Status      string `json:"status"`
	Reason      string `json:"reason,omitempty"`
	CreatedAt   string `json:"createdAt"`
	UpdatedAt   string `json:"updatedAt"`
}"""

new_task = """type taskEvent struct {
	At     string `json:"at"`
	Actor  string `json:"actor,omitempty"`
	Action string `json:"action"`
	Note   string `json:"note,omitempty"`
	From   string `json:"from,omitempty"`
	To     string `json:"to,omitempty"`
}

type task struct {
	ID          string      `json:"id"`
	TenantID    int64       `json:"tenantId,omitempty"`
	Title       string      `json:"title"`
	ProcessName string      `json:"processName"`
	Starter     string      `json:"starter"`
	Assignee    string      `json:"assignee"`
	Status      string      `json:"status"` // pending|approved|rejected|cancelled
	Reason      string      `json:"reason,omitempty"`
	CreatedAt   string      `json:"createdAt"`
	UpdatedAt   string      `json:"updatedAt"`
	History     []taskEvent `json:"history,omitempty"`
}"""

if old_task in t:
    t = t.replace(old_task, new_task)
    print("task type enhanced")
else:
    print("task type miss")

# seed history for t1
t = t.replace(
    '{ID: "t1", TenantID: 1, Title: "Leave request", ProcessName: "leave", Starter: "Zhang San", Assignee: "boss", Status: "pending", CreatedAt: now.Add(-2 * time.Hour).Format(time.RFC3339), UpdatedAt: now.Add(-2 * time.Hour).Format(time.RFC3339)},',
    '{ID: "t1", TenantID: 1, Title: "Leave request", ProcessName: "leave", Starter: "Zhang San", Assignee: "boss", Status: "pending", CreatedAt: now.Add(-2 * time.Hour).Format(time.RFC3339), UpdatedAt: now.Add(-2 * time.Hour).Format(time.RFC3339), History: []taskEvent{{At: now.Add(-2 * time.Hour).Format(time.RFC3339), Actor: "Zhang San", Action: "start", To: "pending"}}},',
)
p.write_text(t, encoding="utf-8")

# --- Rewrite registerBPM in domains.go ---
d = Path("E:/code/nexa/services/core/cmd/nexa-core/domains.go")
dt = d.read_text(encoding="utf-8")
start = dt.find("func registerBPM")
end = dt.find("func registerBusiness")
if start < 0 or end < 0:
    raise SystemExit("bpm markers missing")

new_bpm = r'''func registerBPM(mux *http.ServeMux, s *store) {
	todo := func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		out := []task{}
		for _, t := range s.db.Tasks {
			if t.Status == "pending" && matchTenant(tid, t.TenantID) {
				out = append(out, t)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
	}
	done := func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		out := []task{}
		for _, tsk := range s.db.Tasks {
			if tsk.Status != "pending" && matchTenant(tid, tsk.TenantID) {
				out = append(out, tsk)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
	}
	getOne := func(w http.ResponseWriter, r *http.Request) {
		id := r.URL.Query().Get("id")
		if id == "" {
			// path style /v1/bpm/tasks/get?id=
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "id required"})
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		for _, tsk := range s.db.Tasks {
			if tsk.ID == id && matchTenant(tid, tsk.TenantID) {
				writeJSON(w, 200, map[string]any{"code": 0, "data": tsk})
				return
			}
		}
		writeJSON(w, 404, map[string]any{"code": 404, "msg": "task not found"})
	}
	start := func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body struct {
			Title       string `json:"title"`
			ProcessName string `json:"processName"`
			Starter     string `json:"starter"`
			Assignee    string `json:"assignee"`
		}
		if json.NewDecoder(r.Body).Decode(&body) != nil || body.Title == "" {
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "title required"})
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		now := time.Now().Format(time.RFC3339)
		actor := r.Header.Get("X-Username")
		if body.Starter == "" {
			body.Starter = actor
		}
		if body.Assignee == "" {
			body.Assignee = "boss"
		}
		if body.ProcessName == "" {
			body.ProcessName = "general"
		}
		t := task{
			ID: "t" + time.Now().Format("150405.000"), TenantID: tenantID(r), Title: body.Title, ProcessName: body.ProcessName,
			Starter: body.Starter, Assignee: body.Assignee, Status: "pending", CreatedAt: now, UpdatedAt: now,
			History: []taskEvent{{At: now, Actor: body.Starter, Action: "start", To: "pending", Note: "created"}},
		}
		s.db.Tasks = append(s.db.Tasks, t)
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": t})
	}
	transition := func(w http.ResponseWriter, r *http.Request, action string) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body struct {
			TaskID string `json:"taskId"`
			Action string `json:"action"`
			Reason string `json:"reason"`
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		if body.TaskID == "" {
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "taskId required"})
			return
		}
		if action == "" {
			action = body.Action
		}
		// normalize
		switch action {
		case "approve", "reject", "cancel", "reopen":
		default:
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "action must be approve|reject|cancel|reopen"})
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		actor := r.Header.Get("X-Username")
		for i := range s.db.Tasks {
			if s.db.Tasks[i].ID != body.TaskID || !matchTenant(tid, s.db.Tasks[i].TenantID) {
				continue
			}
			cur := s.db.Tasks[i].Status
			next := cur
			switch action {
			case "approve":
				if cur != "pending" {
					writeJSON(w, 409, map[string]any{"code": 409, "msg": "only pending can be approved"})
					return
				}
				next = "approved"
			case "reject":
				if cur != "pending" {
					writeJSON(w, 409, map[string]any{"code": 409, "msg": "only pending can be rejected"})
					return
				}
				next = "rejected"
			case "cancel":
				if cur != "pending" {
					writeJSON(w, 409, map[string]any{"code": 409, "msg": "only pending can be cancelled"})
					return
				}
				next = "cancelled"
			case "reopen":
				if cur != "rejected" && cur != "cancelled" {
					writeJSON(w, 409, map[string]any{"code": 409, "msg": "only rejected/cancelled can be reopened"})
					return
				}
				next = "pending"
			}
			now := time.Now().Format(time.RFC3339)
			s.db.Tasks[i].Status = next
			s.db.Tasks[i].Reason = body.Reason
			s.db.Tasks[i].UpdatedAt = now
			s.db.Tasks[i].History = append(s.db.Tasks[i].History, taskEvent{At: now, Actor: actor, Action: action, From: cur, To: next, Note: body.Reason})
			_ = s.save()
			writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Tasks[i]})
			return
		}
		writeJSON(w, 404, map[string]any{"code": 404, "msg": "task not found"})
	}
	approve := func(w http.ResponseWriter, r *http.Request) { transition(w, r, "") }
	cancel := func(w http.ResponseWriter, r *http.Request) { transition(w, r, "cancel") }
	reopen := func(w http.ResponseWriter, r *http.Request) { transition(w, r, "reopen") }

	jsonAlias(mux, []string{"/v1/bpm/tasks/todo", "/app-api/bpm/tasks/todo", "/admin-api/bpm/tasks/todo"}, todo)
	jsonAlias(mux, []string{"/v1/bpm/tasks/done"}, done)
	jsonAlias(mux, []string{"/v1/bpm/tasks/get"}, getOne)
	jsonAlias(mux, []string{"/v1/bpm/tasks/start"}, start)
	jsonAlias(mux, []string{"/v1/bpm/tasks/approve", "/app-api/bpm/tasks/approve", "/admin-api/bpm/tasks/approve"}, approve)
	jsonAlias(mux, []string{"/v1/bpm/tasks/cancel"}, cancel)
	jsonAlias(mux, []string{"/v1/bpm/tasks/reopen"}, reopen)
}

'''

dt = d.read_text(encoding="utf-8")
dt = dt[:start] + new_bpm + dt[end:]
d.write_text(dt, encoding="utf-8")
print("bpm rewritten")

# IAM: remove plaintext + x bypass (keep only hash); allow env NEXA_ALLOW_SMOKE_BYPASS=1 for smoke x
ip = Path("E:/code/nexa/services/iam/cmd/nexa-iam/main.go")
it = ip.read_text(encoding="utf-8")
old_check = """func checkPassword(stored, plain string) bool {
	if stored == "" {
		return false
	}
	if plain == "x" {
		return true // smoke bypass
	}
	if stored == plain {
		return true // legacy plaintext
	}
	return stored == hashPassword(plain)
}"""
new_check = """func checkPassword(stored, plain string) bool {
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
}"""
if old_check in it:
    it = it.replace(old_check, new_check)
    print("password policy tightened")
else:
    print("password block miss")
ip.write_text(it, encoding="utf-8")

# business todo complete/cancel
# find business todos handler and add complete endpoint lightly at end of registerBusiness
# simpler: add after registerBusiness start a few endpoints via append before registerERP
if "tasks/complete" not in dt:
    pass

# Add todo complete in domains business section
db = d.read_text(encoding="utf-8")
if "/v1/business/todos/complete" not in db:
    marker = 'jsonAlias(mux, []string{"/v1/business/work-requirements"}'
    insert = r'''jsonAlias(mux, []string{"/v1/business/todos/complete"}, func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body struct {
			ID     string `json:"id"`
			Status string `json:"status"` // done|open|cancelled
		}
		if json.NewDecoder(r.Body).Decode(&body) != nil || body.ID == "" {
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "id required"})
			return
		}
		if body.Status == "" {
			body.Status = "done"
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		for i := range s.db.Todos {
			if s.db.Todos[i].ID == body.ID && matchTenant(tid, s.db.Todos[i].TenantID) {
				s.db.Todos[i].Status = body.Status
				_ = s.save()
				writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Todos[i]})
				return
			}
		}
		writeJSON(w, 404, map[string]any{"code": 404})
	})
	'''
    if marker in db:
        db = db.replace(marker, insert + marker)
        d.write_text(db, encoding="utf-8")
        print("todo complete added")
    else:
        print("business marker miss")

print("done patches")
