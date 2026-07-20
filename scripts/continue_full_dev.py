from pathlib import Path

# Login button
p = Path("E:/code/nexa/apps/mobile/lib/pages/login_page.dart")
t = p.read_text(encoding="utf-8")
old = """children: [
                          _buildBusinessHeader(),
                          const SizedBox(height: 28),
                          _buildBusinessLogo(),
                          const SizedBox(height: 14),
                          _buildBusinessWelcomeText(),
                          const SizedBox(height: 26),
                          _buildBusinessLoginForm(),
                          const SizedBox(height: 34),
                          _buildBusinessFooter(),
                        ],"""
new = """children: [
                          _buildBusinessHeader(),
                          const SizedBox(height: 28),
                          _buildBusinessLogo(),
                          const SizedBox(height: 14),
                          _buildBusinessWelcomeText(),
                          const SizedBox(height: 26),
                          _buildBusinessLoginForm(),
                          const SizedBox(height: 12),
                          TextButton(
                            onPressed: () => Navigator.of(context).pushNamed('/onboarding'),
                            child: const Text('还没有企业？开通 / 加入 Nexa'),
                          ),
                          const SizedBox(height: 22),
                          _buildBusinessFooter(),
                        ],"""
if old in t:
    t = t.replace(old, new)
    print("login button ok")
else:
    print("login layout miss")
if "tenant_onboarding_page.dart" not in t:
    t = t.replace(
        "import 'package:flutter/material.dart';",
        "import 'package:flutter/material.dart';\nimport 'tenant_onboarding_page.dart';",
        1,
    )
p.write_text(t, encoding="utf-8")

# HR bootstrap tenant org
dp = Path("E:/code/nexa/services/core/cmd/nexa-core/domains.go")
dt = dp.read_text(encoding="utf-8")
if "bootstrap-tenant" not in dt:
    insert = r'''
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

'''
    dt = dt.replace("func registerHR(mux *http.ServeMux, s *store) {\n", "func registerHR(mux *http.ServeMux, s *store) {\n"+insert)
    dt = dt.replace(
        """if r.Method == http.MethodGet {
			tid := tenantID(r)
			out := make([]employee, 0)
			for _, e := range s.db.Employees {
				if matchTenant(tid, e.TenantID) {
					out = append(out, e)
				}
			}
			writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
			return
		}""",
        """if r.Method == http.MethodGet {
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
		}""",
    )
    # contacts fallback already maybe - enhance if simple version
    dt = dt.replace(
        """jsonAlias(mux, []string{"/v1/im/contacts"}, contacts)""",
        """jsonAlias(mux, []string{"/v1/im/contacts"}, contacts)""",
    )
    # patch contacts function body if present
    old_c = """out := make([]map[string]any, 0)
		for _, e := range s.db.Employees {
			if matchTenant(tid, e.TenantID) {
				out = append(out, map[string]any{"id": e.ID, "name": e.Name, "dept": e.DeptName, "jobNo": e.JobNo})
			}
		}
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})"""
    new_c = """out := make([]map[string]any, 0)
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
		writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})"""
    if old_c in dt:
        dt = dt.replace(old_c, new_c)
        print("contacts fallback")
    dp = Path("E:/code/nexa/services/core/cmd/nexa-core/domains.go")
    dp.write_text(dt, encoding="utf-8")
    print("bootstrap ok")
else:
    print("bootstrap exists")

# SQL schemas
Path("E:/code/nexa/services/iam/sql").mkdir(parents=True, exist_ok=True)
Path("E:/code/nexa/services/iam/sql/001_iam.sql").write_text(
    """-- nexa IAM MySQL schema (optional; file store is default)
CREATE TABLE IF NOT EXISTS nexa_tenant (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  code VARCHAR(64) NOT NULL UNIQUE,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  created_at DATETIME(3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  nickname VARCHAR(128) NOT NULL DEFAULT '',
  password_hash VARCHAR(128) NOT NULL,
  tenant_id BIGINT NOT NULL,
  roles_json JSON NOT NULL,
  perms_json JSON NOT NULL,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_invite (
  code VARCHAR(96) PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  used_by VARCHAR(64) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_token (
  token VARCHAR(128) PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  expires_at DATETIME(3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    encoding="utf-8",
)

Path("E:/code/nexa/services/core/sql").mkdir(parents=True, exist_ok=True)
Path("E:/code/nexa/services/core/sql/001_core.sql").write_text(
    """-- nexa-core optional MySQL schema (file store is default)
CREATE TABLE IF NOT EXISTS nexa_employee (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  mobile VARCHAR(32) NOT NULL DEFAULT '',
  dept_id BIGINT NOT NULL DEFAULT 0,
  dept_name VARCHAR(128) NOT NULL DEFAULT '',
  job_no VARCHAR(64) NOT NULL DEFAULT '',
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  dingtalk_id VARCHAR(64) NOT NULL DEFAULT '',
  updated_at DATETIME(3) NOT NULL,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_department (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_bpm_task (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  process_name VARCHAR(64) NOT NULL DEFAULT '',
  starter VARCHAR(64) NOT NULL DEFAULT '',
  assignee VARCHAR(64) NOT NULL DEFAULT '',
  status VARCHAR(32) NOT NULL,
  reason VARCHAR(512) NOT NULL DEFAULT '',
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  history_json JSON NULL,
  KEY idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_todo (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  assignee VARCHAR(64) NOT NULL DEFAULT '',
  status VARCHAR(32) NOT NULL,
  due_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_connector (
  tenant_id BIGINT NOT NULL,
  connector_id VARCHAR(64) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  config_json JSON NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (tenant_id, connector_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_im_conversation (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL,
  unread INT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nexa_im_message (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  conversation_id VARCHAR(64) NOT NULL,
  sender VARCHAR(64) NOT NULL,
  body TEXT NOT NULL,
  created_at DATETIME(3) NOT NULL,
  KEY idx_conv (tenant_id, conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    encoding="utf-8",
)

# docs note for mysql
Path("E:/code/nexa/docs/storage.md").write_text(
    """# Storage

## Default: file JSON

- IAM: `{dataDir}/iam.json`
- Core: `{dataDir}/core.json`
- Config: `dataDir` or env `NEXA_DATA_DIR`

Good for local demo and single-node. Not for multi-instance HA.

## Optional: MySQL

Schemas prepared:

- `services/iam/sql/001_iam.sql`
- `services/core/sql/001_core.sql`

Runtime still defaults to file store. Next step: implement `NEXA_IAM_DSN` / `NEXA_CORE_DSN` drivers behind the same HTTP APIs.

## Recommendation

1. Demo / early customers: file store + volume backup
2. Production multi-node: MySQL (or Postgres) + object storage for exports
""",
    encoding="utf-8",
)
print("all written")
