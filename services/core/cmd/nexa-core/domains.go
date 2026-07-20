package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"
)

func registerHR(mux *http.ServeMux, s *store) {

	ensureTenantOrg := func(tid int64) {
		if tid == 0 {
			return
		}
		for _, d := range s.db.Departments {
			if d.TenantID == tid {
				return
			}
		}
		base := tid * 1000
		s.db.Departments = append(s.db.Departments,
			department{ID: base + 1, TenantID: tid, Name: "HQ", ParentID: 0},
			department{ID: base + 10, TenantID: tid, Name: "General", ParentID: base + 1},
		)
	}

	jsonAlias(mux, []string{"/v1/hr/bootstrap-tenant"}, func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		if tid == 0 {
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "tenant required"})
			return
		}
		ensureTenantOrg(tid)
		_ = s.save()
		depts := []department{}
		for _, d := range s.db.Departments {
			if d.TenantID == tid {
				depts = append(depts, d)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{"tenantId": tid, "departments": depts}})
	})

	hEmp := func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		if r.Method == http.MethodGet {
			tid := tenantID(r)
			ensureTenantOrg(tid)
			out := make([]employee, 0)
			for _, e := range s.db.Employees {
				if matchTenant(tid, e.TenantID) {
					out = append(out, e)
				}
			}
			writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
			return
		}
		if r.Method == http.MethodPost {
			var body employee
			if json.NewDecoder(r.Body).Decode(&body) != nil {
				writeJSON(w, 400, map[string]any{"code": 400})
				return
			}
			s.db.Seq++
			body.ID = s.db.Seq
			if body.TenantID == 0 {
				body.TenantID = tenantID(r)
			}
			if body.Status == "" {
				body.Status = "active"
			}
			body.UpdatedAt = time.Now().Format(time.RFC3339)
			s.db.Employees = append(s.db.Employees, body)
			_ = s.save()
			writeJSON(w, 200, map[string]any{"code": 0, "data": body})
			return
		}
		writeJSON(w, 405, map[string]any{"code": 405})
	}
	hTree := func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		ensureTenantOrg(tid)
		type node struct {
			ID       int64  `json:"id"`
			Name     string `json:"name"`
			Children []node `json:"children,omitempty"`
		}
		by := map[int64][]department{}
		for _, d := range s.db.Departments {
			if !matchTenant(tid, d.TenantID) {
				continue
			}
			by[d.ParentID] = append(by[d.ParentID], d)
		}
		var build func(int64) []node
		build = func(p int64) []node {
			out := []node{}
			for _, d := range by[p] {
				out = append(out, node{ID: d.ID, Name: d.Name, Children: build(d.ID)})
			}
			return out
		}
		// prefer roots whose parent is missing in this tenant
		roots := build(0)
		if len(roots) == 0 {
			// any department with parent not in tenant set
			ids := map[int64]bool{}
			for _, d := range s.db.Departments {
				if matchTenant(tid, d.TenantID) {
					ids[d.ID] = true
				}
			}
			for pid, list := range by {
				if pid != 0 && !ids[pid] {
					for _, d := range list {
						roots = append(roots, node{ID: d.ID, Name: d.Name, Children: build(d.ID)})
					}
				}
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": roots})
	}
	hSync := func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		job := syncJob{ID: "ding_" + time.Now().Format("150405"), TenantID: tenantID(r), Type: "full", Status: "success", Message: "sync completed (simulated; OpenAPI connector optional)", StartedAt: time.Now().Format(time.RFC3339), FinishedAt: time.Now().Format(time.RFC3339), Stats: map[string]int{"employeesUpserted": len(s.db.Employees), "departments": len(s.db.Departments)}}
		s.db.SyncJobs = append([]syncJob{job}, s.db.SyncJobs...)
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": job})
	}
		jsonAlias(mux, []string{"/v1/hr/departments"}, func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body department
		if json.NewDecoder(r.Body).Decode(&body) != nil || body.Name == "" {
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "name required"})
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		if body.TenantID == 0 {
			body.TenantID = tid
		}
		s.db.Seq++
		body.ID = s.db.Seq
		s.db.Departments = append(s.db.Departments, body)
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": body})
	})
	jsonAlias(mux, []string{"/v1/hr/employees", "/app-api/hr/employees", "/admin-api/hr/employees"}, hEmp)
	jsonAlias(mux, []string{"/v1/hr/departments/tree", "/app-api/hr/departments/tree", "/admin-api/hr/departments/tree"}, hTree)
	jsonAlias(mux, []string{"/v1/hr/dingtalk/sync"}, hSync)
	jsonAlias(mux, []string{"/v1/hr/dingtalk/status"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		var last any
		if len(s.db.SyncJobs) > 0 {
			last = s.db.SyncJobs[0]
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{"provider": "dingtalk-import-connector", "employees": len(s.db.Employees), "lastJob": last}})
	})
}


func processNodes(processName string) []string {
	switch processName {
	case "leave":
		return []string{"start", "manager_approve", "end"}
	case "expense":
		return []string{"start", "finance_approve", "end"}
	case "purchase":
		return []string{"start", "manager_approve", "finance_approve", "end"}
	case "onboard":
		return []string{"start", "hr_approve", "end"}
	default:
		return []string{"start", "manager_approve", "end"}
	}
}

func nextApprovalNode(nodes []string, current string) (string, bool) {
	// returns next node after current; done=true when next is end or past end
	if len(nodes) == 0 {
		return "end", true
	}
	idx := -1
	for i, n := range nodes {
		if n == current {
			idx = i
			break
		}
	}
	if idx < 0 {
		// if current empty, jump to first non-start
		for _, n := range nodes {
			if n != "start" && n != "end" {
				return n, false
			}
		}
		return "end", true
	}
	for j := idx + 1; j < len(nodes); j++ {
		n := nodes[j]
		if n == "end" {
			return "end", true
		}
		if n != "start" {
			return n, false
		}
	}
	return "end", true
}

func registerBPM(mux *http.ServeMux, s *store) {
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
		nodes := processNodes(body.ProcessName)
		cur, _ := nextApprovalNode(nodes, "start")
		t := task{
			ID: "t" + time.Now().Format("150405.000"), TenantID: tenantID(r), Title: body.Title, ProcessName: body.ProcessName,
			Starter: body.Starter, Assignee: body.Assignee, Status: "pending", CurrentNode: cur, Nodes: nodes,
			CreatedAt: now, UpdatedAt: now,
			History: []taskEvent{{At: now, Actor: body.Starter, Action: "start", To: "pending", Note: "node=" + cur}},
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
				nodes := s.db.Tasks[i].Nodes
				if len(nodes) == 0 {
					nodes = processNodes(s.db.Tasks[i].ProcessName)
					s.db.Tasks[i].Nodes = nodes
				}
				currNode := s.db.Tasks[i].CurrentNode
				if currNode == "" {
					currNode = "start"
				}
				nn, done := nextApprovalNode(nodes, currNode)
				if done {
					next = "approved"
					s.db.Tasks[i].CurrentNode = "end"
				} else {
					next = "pending"
					s.db.Tasks[i].CurrentNode = nn
				}
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
				nodes := s.db.Tasks[i].Nodes
				if len(nodes) == 0 {
					nodes = processNodes(s.db.Tasks[i].ProcessName)
					s.db.Tasks[i].Nodes = nodes
				}
				nn, _ := nextApprovalNode(nodes, "start")
				s.db.Tasks[i].CurrentNode = nn
			}
			now := time.Now().Format(time.RFC3339)
			s.db.Tasks[i].Status = next
			s.db.Tasks[i].Reason = body.Reason
			s.db.Tasks[i].UpdatedAt = now
			note := body.Reason
			if s.db.Tasks[i].CurrentNode != "" {
				if note != "" {
					note = note + "; node=" + s.db.Tasks[i].CurrentNode
				} else {
					note = "node=" + s.db.Tasks[i].CurrentNode
				}
			}
			s.db.Tasks[i].History = append(s.db.Tasks[i].History, taskEvent{At: now, Actor: actor, Action: action, From: cur, To: next, Note: note})
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

func registerBusiness(mux *http.ServeMux, s *store) {
	jsonAlias(mux, []string{"/v1/business/todos"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		if r.Method == http.MethodGet {
			tid := tenantID(r)
			out := make([]todo, 0)
			for _, x := range s.db.Todos {
				if matchTenant(tid, x.TenantID) {
					out = append(out, x)
				}
			}
			writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
			return
		}
		if r.Method == http.MethodPost {
			var body todo
			_ = json.NewDecoder(r.Body).Decode(&body)
			if body.ID == "" {
				body.ID = "td" + time.Now().Format("150405")
			}
			if body.Status == "" {
				body.Status = "open"
			}
			body.TenantID = tenantID(r)
			body.CreatedAt = time.Now().Format(time.RFC3339)
			s.db.Todos = append(s.db.Todos, body)
			_ = s.save()
			writeJSON(w, 200, map[string]any{"code": 0, "data": body})
			return
		}
		writeJSON(w, 405, map[string]any{"code": 405})
	})
	jsonAlias(mux, []string{"/v1/business/todos/complete"}, func(w http.ResponseWriter, r *http.Request) {
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
	jsonAlias(mux, []string{"/v1/business/work-requirements"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": []map[string]any{{"id": "wr1", "title": "Q3 campaign", "status": "active"}}, "total": 1})
	})
	jsonAlias(mux, []string{"/v1/business/calendar/events"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		if r.Method == http.MethodGet {
			out := make([]calEvent, 0)
			for _, e := range s.db.Events {
				if matchTenant(tid, e.TenantID) {
					out = append(out, e)
				}
			}
			if len(out) == 0 && tid <= 1 {
				out = append(out, calEvent{ID: "ev1", TenantID: 1, Title: "All-hands", Start: time.Now().Add(2 * time.Hour).Format(time.RFC3339)})
			}
			writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
			return
		}
		if r.Method == http.MethodPost {
			var body calEvent
			if json.NewDecoder(r.Body).Decode(&body) != nil || body.Title == "" {
				writeJSON(w, 400, map[string]any{"code": 400})
				return
			}
			if body.TenantID == 0 {
				body.TenantID = tid
			}
			if body.ID == "" {
				body.ID = "ev" + time.Now().Format("150405")
			}
			if body.Start == "" {
				body.Start = time.Now().Format(time.RFC3339)
			}
			s.db.Events = append(s.db.Events, body)
			_ = s.save()
			writeJSON(w, 200, map[string]any{"code": 0, "data": body})
			return
		}
		writeJSON(w, 405, map[string]any{"code": 405})
	})
	jsonAlias(mux, []string{"/v1/business/reception/latest"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": []map[string]any{{"id": "rc1", "visitor": "Guest", "purpose": "meeting"}}})
	})
}

func registerERP(mux *http.ServeMux, s *store) {
	jsonAlias(mux, []string{"/v1/erp/stock/summary"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		out := make([]stockItem, 0)
		for _, x := range s.db.Stock {
			if matchTenant(tid, x.TenantID) {
				out = append(out, x)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
	})
	jsonAlias(mux, []string{"/v1/erp/purchase/orders"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		out := make([]purchase, 0)
		for _, x := range s.db.Purchases {
			if matchTenant(tid, x.TenantID) {
				out = append(out, x)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
	})
}

func registerFinance(mux *http.ServeMux, s *store) {
	jsonAlias(mux, []string{"/v1/finance/summary/monthly"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{"month": time.Now().Format("2006-01"), "income": 128000.0, "expense": 76450.5, "profit": 51549.5}})
	})
	jsonAlias(mux, []string{"/v1/finance/ledger/recent"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		out := make([]ledger, 0)
		for _, x := range s.db.Ledger {
			if matchTenant(tid, x.TenantID) {
				out = append(out, x)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
	})
}

func registerIM(mux *http.ServeMux, s *store) {
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
		if len(out) == 0 {
			if un := r.Header.Get("X-Username"); un != "" {
				out = append(out, map[string]any{"id": 0, "name": un, "dept": "HQ", "jobNo": ""})
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

func registerOP(mux *http.ServeMux, s *store) {
	jsonAlias(mux, []string{"/v1/op/status"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{
			"mode": "nexa-core",
			"services": []map[string]any{
				{"name": "core", "ok": true},
				{"name": "iam", "url": "http://127.0.0.1:48081/healthz"},
				{"name": "agent", "url": "http://127.0.0.1:48091/health"},
			},
		}})
	})
	jsonAlias(mux, []string{"/v1/op/audit/recent"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": []map[string]any{{"actor": "system", "action": "core-boot", "at": time.Now().Format(time.RFC3339)}}})
	})
}

func registerAI(mux *http.ServeMux, s *store) {
	skills := []map[string]any{
		{"id": "iam.whoami", "domain": "iam", "title": "当前用户", "method": "GET", "path": "/v1/iam/me", "keywords": []string{"我是谁", "whoami"}},
		{"id": "hr.employees.search", "domain": "hr", "title": "员工列表", "method": "GET", "path": "/v1/hr/employees", "keywords": []string{"员工", "employee", "roster"}},
		{"id": "bpm.todo.list", "domain": "bpm", "title": "审批待办", "method": "GET", "path": "/v1/bpm/tasks/todo", "keywords": []string{"审批", "todo", "approval"}},
		{"id": "biz.todo.list", "domain": "business", "title": "业务待办", "method": "GET", "path": "/v1/business/todos", "keywords": []string{"待办", "todo"}},
		{"id": "erp.stock.summary", "domain": "erp", "title": "库存", "method": "GET", "path": "/v1/erp/stock/summary", "keywords": []string{"库存", "stock"}},
		{"id": "finance.month.summary", "domain": "finance", "title": "月度财务", "method": "GET", "path": "/v1/finance/summary/monthly", "keywords": []string{"财务", "finance"}},
		{"id": "ai.connectors", "domain": "ai", "title": "连接器", "method": "GET", "path": "/v1/ai/connectors", "keywords": []string{"连接器", "connector"}},
		{"id": "iam.tenant.register", "domain": "iam", "title": "注册企业", "method": "POST", "path": "/v1/iam/tenants/register", "keywords": []string{"注册企业", "开通", "register"}},
	}
	jsonAlias(mux, []string{"/v1/ai/skills"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": skills, "total": len(skills)})
	})
	jsonAlias(mux, []string{"/v1/ai/intent/route"}, func(w http.ResponseWriter, r *http.Request) {
		var body struct {
			Text string `json:"text"`
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		matches := []map[string]any{}
		low := strings.ToLower(body.Text)
		for _, sk := range skills {
			score := 0
			for _, kw := range sk["keywords"].([]string) {
				if strings.Contains(body.Text, kw) || strings.Contains(low, strings.ToLower(kw)) {
					score += 2
				}
			}
			if score > 0 {
				matches = append(matches, map[string]any{"skill": sk, "score": score})
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{"text": body.Text, "matches": matches}})
	})
	jsonAlias(mux, []string{"/v1/ai/connectors"}, func(w http.ResponseWriter, r *http.Request) {
		catalog := []map[string]any{
			{"id": "dingtalk-import", "kind": "dingtalk_import", "name": "钉钉通讯录导入", "description": "可选导入，非主数据权威"},
			{"id": "cdc-mysql", "kind": "cdc_mysql", "name": "MySQL CDC", "description": "业务库 binlog -> warehouse"},
			{"id": "data-center", "kind": "warehouse_ro", "name": "数据中心", "description": "模板导出"},
			{"id": "business-http", "kind": "business_api", "name": "HTTP 业务连接器", "description": "外部业务 API"},
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		out := make([]map[string]any, 0, len(catalog))
		for _, c := range catalog {
			item := map[string]any{}
			for k, v := range c {
				item[k] = v
			}
			key := fmt.Sprintf("%d:%s", tid, c["id"])
			if cfg, ok := s.db.Connectors[key]; ok {
				item["enabled"] = cfg.Enabled
				item["config"] = cfg.Config
				item["updatedAt"] = cfg.UpdatedAt
			} else {
				item["enabled"] = false
			}
			out = append(out, item)
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out), "note": "per-tenant config via PUT /v1/ai/connectors/config"})
	})
	jsonAlias(mux, []string{"/v1/ai/connectors/config"}, func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPut && r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body struct {
			ID      string         `json:"id"`
			Enabled bool           `json:"enabled"`
			Config  map[string]any `json:"config"`
		}
		if json.NewDecoder(r.Body).Decode(&body) != nil || body.ID == "" {
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "id required"})
			return
		}
		tid := tenantID(r)
		if tid == 0 {
			writeJSON(w, 400, map[string]any{"code": 400, "msg": "tenant required"})
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		if s.db.Connectors == nil {
			s.db.Connectors = map[string]connectorCfg{}
		}
		key := fmt.Sprintf("%d:%s", tid, body.ID)
		s.db.Connectors[key] = connectorCfg{ID: body.ID, TenantID: tid, Enabled: body.Enabled, Config: body.Config, UpdatedAt: time.Now().Format(time.RFC3339)}
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Connectors[key]})
	})
	jsonAlias(mux, []string{"/v1/ai/assistant/bootstrap"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{
			"assistant":   map[string]any{"name": "Nexa", "mission": "可接入的企业钉钉 · 协作本体 + Agent"},
			"skillsTotal": len(skills),
			"processes":   []string{"nexa-core", "nexa-iam", "nexa-agent"},
		}})
	})
	jsonAlias(mux, []string{"/v1/ai/sense"}, func(w http.ResponseWriter, r *http.Request) {
		var body struct {
			Type, Source, Severity, Message string
			Payload                         map[string]any
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		s.mu.Lock()
		defer s.mu.Unlock()
		ev := senseEv{ID: "sense_" + time.Now().Format("150405"), TenantID: tenantID(r), Type: body.Type, Source: body.Source, Severity: body.Severity, Message: body.Message, Payload: body.Payload, At: time.Now().Format(time.RFC3339)}
		s.db.Sense = append([]senseEv{ev}, s.db.Sense...)
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{"event": ev}})
	})
	jsonAlias(mux, []string{"/v1/ai/sense/recent"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		out := make([]senseEv, 0)
		for _, x := range s.db.Sense {
			if matchTenant(tid, x.TenantID) {
				out = append(out, x)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
	})
}

func registerDataCenter(mux *http.ServeMux, s *store) {
	tpls := []map[string]any{
		{"id": "order", "label": "订单明细", "category": "order"},
		{"id": "pf_record", "label": "赔付明细", "category": "pf"},
		{"id": "work_ticket", "label": "工单明细", "category": "work"},
	}
	hT := func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": tpls, "total": len(tpls)})
	}
	hJ := func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		if r.Method == http.MethodGet {
			tid := tenantID(r)
			out := make([]exportJob, 0)
			for _, x := range s.db.ExportJobs {
				if matchTenant(tid, x.TenantID) {
					out = append(out, x)
				}
			}
			writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
			return
		}
		if r.Method == http.MethodPost {
			var body struct {
				TemplateID string `json:"templateId"`
			}
			_ = json.NewDecoder(r.Body).Decode(&body)
			if body.TemplateID == "" {
				body.TemplateID = "order"
			}
			id := "job_" + time.Now().Format("150405")
			j := exportJob{ID: id, TenantID: tenantID(r), TemplateID: body.TemplateID, State: "done", CreatedAt: time.Now().Format(time.RFC3339), Message: "core lite export", Download: "/jobs/" + id + "/xlsx"}
			s.db.ExportJobs = append([]exportJob{j}, s.db.ExportJobs...)
			_ = s.save()
			writeJSON(w, 200, map[string]any{"code": 0, "data": j})
			return
		}
		writeJSON(w, 405, map[string]any{"code": 405})
	}
	jsonAlias(mux, []string{"/v1/data-center/templates", "/templates"}, hT)
	jsonAlias(mux, []string{"/v1/data-center/jobs", "/jobs"}, hJ)
	jsonAlias(mux, []string{"/v1/data-center/hall", "/hall"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{"templates": len(tpls), "jobs": len(s.db.ExportJobs), "mode": "core"}})
	})
}


func registerWorkbench(mux *http.ServeMux, s *store) {
	jsonAlias(mux, []string{"/v1/workbench/summary", "/app-api/workbench/summary"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		tid := tenantID(r)
		todoN, taskN, empN, convN := 0, 0, 0, 0
		for _, x := range s.db.Todos {
			if matchTenant(tid, x.TenantID) && x.Status == "open" {
				todoN++
			}
		}
		for _, x := range s.db.Tasks {
			if matchTenant(tid, x.TenantID) && x.Status == "pending" {
				taskN++
			}
		}
		for _, x := range s.db.Employees {
			if matchTenant(tid, x.TenantID) && x.Status == "active" {
				empN++
			}
		}
		for _, x := range s.db.Conversations {
			if matchTenant(tid, x.TenantID) {
				convN++
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{
			"openTodos":     todoN,
			"pendingTasks":  taskN,
			"activeEmployees": empN,
			"conversations": convN,
			"modules": []map[string]any{
				{"id": "hr", "title": "组织通讯录", "path": "/v1/hr/employees"},
				{"id": "bpm", "title": "审批", "path": "/v1/bpm/tasks/todo"},
				{"id": "im", "title": "消息", "path": "/v1/im/conversations"},
				{"id": "biz", "title": "待办", "path": "/v1/business/todos"},
				{"id": "data", "title": "数据中心", "path": "/v1/data-center/hall"},
				{"id": "ai", "title": "企业助手", "path": "/v1/ai/skills"},
			},
		}})
	})
}

func registerBPMProcesses(mux *http.ServeMux, s *store) {
	// simple process catalog for enterprise ding-like UX
	defs := []map[string]any{
		{"id": "leave", "name": "请假", "category": "HR", "nodes": []string{"start", "manager_approve", "end"}},
		{"id": "expense", "name": "报销", "category": "Finance", "nodes": []string{"start", "finance_approve", "end"}},
		{"id": "purchase", "name": "采购申请", "category": "ERP", "nodes": []string{"start", "manager_approve", "finance_approve", "end"}},
		{"id": "onboard", "name": "入职审批", "category": "HR", "nodes": []string{"start", "hr_approve", "end"}},
	}
	jsonAlias(mux, []string{"/v1/bpm/processes", "/app-api/bpm/processes"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": defs, "total": len(defs)})
	})
}
