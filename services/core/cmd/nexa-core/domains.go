package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"
)

func registerHR(mux *http.ServeMux, s *store) {
	hEmp := func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		if r.Method == http.MethodGet {
			tid := tenantID(r)
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
		type node struct {
			ID       int64  `json:"id"`
			Name     string `json:"name"`
			Children []node `json:"children,omitempty"`
		}
		by := map[int64][]department{}
		for _, d := range s.db.Departments {
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
		writeJSON(w, 200, map[string]any{"code": 0, "data": build(0)})
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
	approve := func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body struct {
			TaskID, Action, Reason string
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		s.mu.Lock()
		defer s.mu.Unlock()
		for i := range s.db.Tasks {
			if s.db.Tasks[i].ID == body.TaskID {
				if body.Action == "reject" {
					s.db.Tasks[i].Status = "rejected"
				} else {
					s.db.Tasks[i].Status = "approved"
				}
				s.db.Tasks[i].Reason = body.Reason
				s.db.Tasks[i].UpdatedAt = time.Now().Format(time.RFC3339)
				_ = s.save()
				writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Tasks[i]})
				return
			}
		}
		writeJSON(w, 404, map[string]any{"code": 404})
	}
	start := func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body struct{ Title, ProcessName, Starter, Assignee string }
		_ = json.NewDecoder(r.Body).Decode(&body)
		s.mu.Lock()
		defer s.mu.Unlock()
		now := time.Now().Format(time.RFC3339)
		t := task{ID: "t" + time.Now().Format("150405"), TenantID: tenantID(r), Title: body.Title, ProcessName: body.ProcessName, Starter: body.Starter, Assignee: body.Assignee, Status: "pending", CreatedAt: now, UpdatedAt: now}
		if t.Assignee == "" {
			t.Assignee = "boss"
		}
		s.db.Tasks = append(s.db.Tasks, t)
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": t})
	}
	jsonAlias(mux, []string{"/v1/bpm/tasks/todo", "/app-api/bpm/tasks/todo", "/admin-api/bpm/tasks/todo"}, todo)
	jsonAlias(mux, []string{"/v1/bpm/tasks/approve", "/app-api/bpm/tasks/approve", "/admin-api/bpm/tasks/approve"}, approve)
	jsonAlias(mux, []string{"/v1/bpm/tasks/start"}, start)
	jsonAlias(mux, []string{"/v1/bpm/tasks/done"}, func(w http.ResponseWriter, r *http.Request) {
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
	})
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
	jsonAlias(mux, []string{"/v1/business/work-requirements"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": []map[string]any{{"id": "wr1", "title": "Q3 campaign", "status": "active"}}, "total": 1})
	})
	jsonAlias(mux, []string{"/v1/business/calendar/events"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": []map[string]any{{"id": "ev1", "title": "All-hands", "start": time.Now().Add(2 * time.Hour).Format(time.RFC3339)}}, "total": 1})
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
	jsonAlias(mux, []string{"/v1/im/conversations"}, func(w http.ResponseWriter, r *http.Request) {
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
	})
	jsonAlias(mux, []string{"/v1/im/contacts"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": []map[string]any{{"id": 1001, "name": "Zhang San"}, {"id": 1002, "name": "Li Si"}}})
	})
	jsonAlias(mux, []string{"/v1/im/messages/send"}, func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, 405, map[string]any{"code": 405})
			return
		}
		var body struct{ ConversationID, From, Text string }
		_ = json.NewDecoder(r.Body).Decode(&body)
		s.mu.Lock()
		defer s.mu.Unlock()
		m := msg{ID: "m" + time.Now().Format("150405"), TenantID: tenantID(r), ConversationID: body.ConversationID, From: body.From, Text: body.Text, At: time.Now().Format(time.RFC3339)}
		if m.ConversationID == "" {
			m.ConversationID = "c1"
		}
		s.db.Messages = append(s.db.Messages, m)
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": m})
	})
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
