from pathlib import Path

# --- main.go seed + connectors field already partially done ---
p = Path("E:/code/nexa/services/core/cmd/nexa-core/main.go")
t = p.read_text(encoding="utf-8")

seeds = [
    (
        'Departments: []department{{ID: 1, Name: "HQ", ParentID: 0}, {ID: 10, Name: "R&D", ParentID: 1}, {ID: 20, Name: "Ops", ParentID: 1}, {ID: 30, Name: "HR", ParentID: 1}},',
        'Departments: []department{{ID: 1, TenantID: 1, Name: "HQ", ParentID: 0}, {ID: 10, TenantID: 1, Name: "R&D", ParentID: 1}, {ID: 20, TenantID: 1, Name: "Ops", ParentID: 1}, {ID: 30, TenantID: 1, Name: "HR", ParentID: 1}},',
    ),
    (
        'Stock:         []stockItem{{SKU: "SKU-001", Name: "Widget A", Qty: 120, Warehouse: "WH-East"}, {SKU: "SKU-002", Name: "Widget B", Qty: 45, Warehouse: "WH-East"}},',
        'Stock:         []stockItem{{TenantID: 1, SKU: "SKU-001", Name: "Widget A", Qty: 120, Warehouse: "WH-East"}, {TenantID: 1, SKU: "SKU-002", Name: "Widget B", Qty: 45, Warehouse: "WH-East"}},',
    ),
    (
        'Purchases:     []purchase{{ID: "PO-1", Vendor: "Supplier X", Amount: 3200.5, Status: "open", CreatedAt: now.Add(-48 * time.Hour).Format(time.RFC3339)}},',
        'Purchases:     []purchase{{ID: "PO-1", TenantID: 1, Vendor: "Supplier X", Amount: 3200.5, Status: "open", CreatedAt: now.Add(-48 * time.Hour).Format(time.RFC3339)}},',
    ),
    (
        'Ledger:        []ledger{{ID: "L1", Title: "Service revenue", Amount: 35000, At: now.Add(-24 * time.Hour).Format(time.RFC3339)}},',
        'Ledger:        []ledger{{ID: "L1", TenantID: 1, Title: "Service revenue", Amount: 35000, At: now.Add(-24 * time.Hour).Format(time.RFC3339)}},',
    ),
    (
        'Conversations: []conv{{ID: "c1", Title: "Ops group", Unread: 0, UpdatedAt: now.Format(time.RFC3339)}},',
        'Conversations: []conv{{ID: "c1", TenantID: 1, Title: "Ops group", Unread: 0, UpdatedAt: now.Format(time.RFC3339)}},',
    ),
]
for a, b in seeds:
    if a in t:
        t = t.replace(a, b)
        print("seed ok")
    else:
        print("seed miss")

old_load = """if raw, err := os.ReadFile(s.path()); err == nil {
		_ = json.Unmarshal(raw, &s.db)
		return
	}"""
new_load = """if raw, err := os.ReadFile(s.path()); err == nil {
		_ = json.Unmarshal(raw, &s.db)
		if s.db.Connectors == nil {
			s.db.Connectors = map[string]connectorCfg{}
		}
		return
	}"""
if old_load in t:
    t = t.replace(old_load, new_load)
    print("load ok")

p.write_text(t, encoding="utf-8")

# --- domains.go ---
d = Path("E:/code/nexa/services/core/cmd/nexa-core/domains.go")
dt = d.read_text(encoding="utf-8")

if '"fmt"' not in dt:
    dt = dt.replace(
        'import (\n\t"encoding/json"\n\t"net/http"\n\t"strings"\n\t"time"\n)',
        'import (\n\t"encoding/json"\n\t"fmt"\n\t"net/http"\n\t"strings"\n\t"time"\n)',
    )

patches = [
    (
        'if tid == 0 || e.TenantID == 0 || e.TenantID == tid {',
        'if matchTenant(tid, e.TenantID) {',
    ),
    (
        'if t.Status == "pending" && (tid == 0 || t.TenantID == 0 || t.TenantID == tid) {',
        'if t.Status == "pending" && matchTenant(tid, t.TenantID) {',
    ),
    (
        """out := []task{}
		for _, t := range s.db.Tasks {
			if t.Status != "pending" {
				out = append(out, t)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})""",
        """tid := tenantID(r)
		out := []task{}
		for _, tsk := range s.db.Tasks {
			if tsk.Status != "pending" && matchTenant(tid, tsk.TenantID) {
				out = append(out, tsk)
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})""",
    ),
    (
        """if r.Method == http.MethodGet {
			writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Todos, "total": len(s.db.Todos)})
			return
		}""",
        """if r.Method == http.MethodGet {
			tid := tenantID(r)
			out := make([]todo, 0)
			for _, x := range s.db.Todos {
				if matchTenant(tid, x.TenantID) {
					out = append(out, x)
				}
			}
			writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
			return
		}""",
    ),
    (
        """jsonAlias(mux, []string{"/v1/erp/stock/summary"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Stock, "total": len(s.db.Stock)})
	})""",
        """jsonAlias(mux, []string{"/v1/erp/stock/summary"}, func(w http.ResponseWriter, r *http.Request) {
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
	})""",
    ),
    (
        """jsonAlias(mux, []string{"/v1/erp/purchase/orders"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Purchases, "total": len(s.db.Purchases)})
	})""",
        """jsonAlias(mux, []string{"/v1/erp/purchase/orders"}, func(w http.ResponseWriter, r *http.Request) {
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
	})""",
    ),
    (
        """jsonAlias(mux, []string{"/v1/finance/ledger/recent"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Ledger, "total": len(s.db.Ledger)})
	})""",
        """jsonAlias(mux, []string{"/v1/finance/ledger/recent"}, func(w http.ResponseWriter, r *http.Request) {
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
	})""",
    ),
    (
        """jsonAlias(mux, []string{"/v1/im/conversations"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Conversations, "total": len(s.db.Conversations)})
	})""",
        """jsonAlias(mux, []string{"/v1/im/conversations"}, func(w http.ResponseWriter, r *http.Request) {
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
	})""",
    ),
    (
        'm := msg{ID: "m" + time.Now().Format("150405"), ConversationID: body.ConversationID, From: body.From, Text: body.Text, At: time.Now().Format(time.RFC3339)}',
        'm := msg{ID: "m" + time.Now().Format("150405"), TenantID: tenantID(r), ConversationID: body.ConversationID, From: body.From, Text: body.Text, At: time.Now().Format(time.RFC3339)}',
    ),
    (
        'job := syncJob{ID: "ding_" + time.Now().Format("150405"), Type: "full", Status: "success", Message: "sync completed (simulated; use IAM-separated HR module for OpenAPI client)", StartedAt: time.Now().Format(time.RFC3339), FinishedAt: time.Now().Format(time.RFC3339), Stats: map[string]int{"employeesUpserted": len(s.db.Employees), "departments": len(s.db.Departments)}}',
        'job := syncJob{ID: "ding_" + time.Now().Format("150405"), TenantID: tenantID(r), Type: "full", Status: "success", Message: "sync completed (simulated; OpenAPI connector optional)", StartedAt: time.Now().Format(time.RFC3339), FinishedAt: time.Now().Format(time.RFC3339), Stats: map[string]int{"employeesUpserted": len(s.db.Employees), "departments": len(s.db.Departments)}}',
    ),
    (
        """if r.Method == http.MethodGet {
			writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.ExportJobs, "total": len(s.db.ExportJobs)})
			return
		}""",
        """if r.Method == http.MethodGet {
			tid := tenantID(r)
			out := make([]exportJob, 0)
			for _, x := range s.db.ExportJobs {
				if matchTenant(tid, x.TenantID) {
					out = append(out, x)
				}
			}
			writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
			return
		}""",
    ),
    (
        'j := exportJob{ID: id, TemplateID: body.TemplateID, State: "done", CreatedAt: time.Now().Format(time.RFC3339), Message: "core lite export", Download: "/jobs/" + id + "/xlsx"}',
        'j := exportJob{ID: id, TenantID: tenantID(r), TemplateID: body.TemplateID, State: "done", CreatedAt: time.Now().Format(time.RFC3339), Message: "core lite export", Download: "/jobs/" + id + "/xlsx"}',
    ),
    (
        'ev := senseEv{ID: "sense_" + time.Now().Format("150405"), Type: body.Type, Source: body.Source, Severity: body.Severity, Message: body.Message, Payload: body.Payload, At: time.Now().Format(time.RFC3339)}',
        'ev := senseEv{ID: "sense_" + time.Now().Format("150405"), TenantID: tenantID(r), Type: body.Type, Source: body.Source, Severity: body.Severity, Message: body.Message, Payload: body.Payload, At: time.Now().Format(time.RFC3339)}',
    ),
    (
        """jsonAlias(mux, []string{"/v1/ai/sense/recent"}, func(w http.ResponseWriter, r *http.Request) {
		s.mu.Lock()
		defer s.mu.Unlock()
		writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Sense, "total": len(s.db.Sense)})
	})""",
        """jsonAlias(mux, []string{"/v1/ai/sense/recent"}, func(w http.ResponseWriter, r *http.Request) {
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
	})""",
    ),
]

for a, b in patches:
    if a in dt:
        dt = dt.replace(a, b)
        print("patch ok")
    else:
        print("patch miss")

# connectors config API
old_conn = """jsonAlias(mux, []string{"/v1/ai/connectors"}, func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": 0, "data": []map[string]any{
			{"id": "dingtalk-import", "kind": "dingtalk_import", "name": "钉钉通讯录导入", "description": "可选导入，非主数据权威"},
			{"id": "cdc-mysql", "kind": "cdc_mysql", "name": "MySQL CDC", "description": "业务库 binlog -> warehouse"},
			{"id": "data-center", "kind": "warehouse_ro", "name": "数据中心", "description": "模板导出"},
		}, "total": 3, "note": "connectors plug into nexa body"})
	})"""

new_conn = r"""jsonAlias(mux, []string{"/v1/ai/connectors"}, func(w http.ResponseWriter, r *http.Request) {
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
	})"""

if old_conn in dt:
    dt = dt.replace(old_conn, new_conn)
    print("connectors API ok")
else:
    print("connectors API miss")

d.write_text(dt, encoding="utf-8")
print("domains written")
