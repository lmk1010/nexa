from pathlib import Path

# Fix main.go - single package declaration
main_path = Path("E:/code/nexa/services/core/cmd/nexa-core/main.go")
t = main_path.read_text(encoding="utf-8")
imp = t.find("import (")
if imp < 0:
    raise SystemExit("no import")
body = t[imp:]
t = (
    "// nexa-core — single business process: gateway edge + all product domains.\n"
    "// IAM stays separate (auth). Agent stays Node. CDC optional separate.\n"
    "package main\n\n"
    + body
)

# Fix types and seed literals to named fields
replacements = [
    (
        """type employee struct {
	ID, TenantID, DeptID int64
	Name, Mobile, DeptName, JobNo, Status, DingTalkID, UpdatedAt string
}""",
        """type employee struct {
	ID         int64  `json:"id"`
	TenantID   int64  `json:"tenantId,omitempty"`
	Name       string `json:"name"`
	Mobile     string `json:"mobile"`
	DeptID     int64  `json:"deptId"`
	DeptName   string `json:"deptName"`
	JobNo      string `json:"jobNo"`
	Status     string `json:"status"`
	DingTalkID string `json:"dingTalkId,omitempty"`
	UpdatedAt  string `json:"updatedAt"`
}""",
    ),
    (
        'type department struct{ ID, ParentID int64; Name string }',
        """type department struct {
	ID       int64  `json:"id"`
	Name     string `json:"name"`
	ParentID int64  `json:"parentId"`
}""",
    ),
    (
        """type task struct {
	ID, Title, ProcessName, Starter, Assignee, Status, Reason, CreatedAt, UpdatedAt string
	TenantID int64
}""",
        """type task struct {
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
}""",
    ),
    (
        'type stockItem struct{ SKU, Name, Warehouse string; Qty int }',
        """type stockItem struct {
	SKU       string `json:"sku"`
	Name      string `json:"name"`
	Qty       int    `json:"qty"`
	Warehouse string `json:"warehouse"`
}""",
    ),
    (
        'type purchase struct{ ID, Vendor, Status, CreatedAt string; Amount float64 }',
        """type purchase struct {
	ID        string  `json:"id"`
	Vendor    string  `json:"vendor"`
	Amount    float64 `json:"amount"`
	Status    string  `json:"status"`
	CreatedAt string  `json:"createdAt"`
}""",
    ),
    (
        'type ledger struct{ ID, Title, At string; Amount float64 }',
        """type ledger struct {
	ID     string  `json:"id"`
	Title  string  `json:"title"`
	Amount float64 `json:"amount"`
	At     string  `json:"at"`
}""",
    ),
    (
        'type conv struct{ ID, Title, UpdatedAt string; Unread int }',
        """type conv struct {
	ID        string `json:"id"`
	Title     string `json:"title"`
	Unread    int    `json:"unread"`
	UpdatedAt string `json:"updatedAt"`
}""",
    ),
    (
        """type autoRule struct {
	ID, Name, SenseType string
	Enabled bool
	Actions []string
}""",
        """type autoRule struct {
	ID        string
	Name      string
	Enabled   bool
	SenseType string
	Actions   []string
}""",
    ),
]
for a, b in replacements:
    if a in t:
        t = t.replace(a, b)

# seed
t = t.replace(
    'Departments: []department{{1, "HQ", 0}, {10, "R&D", 1}, {20, "Ops", 1}, {30, "HR", 1}},',
    'Departments: []department{{ID:1, Name:"HQ", ParentID:0}, {ID:10, Name:"R&D", ParentID:1}, {ID:20, Name:"Ops", ParentID:1}, {ID:30, Name:"HR", ParentID:1}},',
)
t = t.replace(
    '{1001, 1, "Zhang San", "13800000001", 10, "R&D", "NEXA001", "active", "", now.Format(time.RFC3339)},\n\t\t\t{1002, 1, "Li Si", "13800000002", 20, "Ops", "NEXA002", "active", "", now.Format(time.RFC3339)},',
    '{ID:1001, TenantID:1, Name:"Zhang San", Mobile:"13800000001", DeptID:10, DeptName:"R&D", JobNo:"NEXA001", Status:"active", UpdatedAt:now.Format(time.RFC3339)},\n\t\t\t{ID:1002, TenantID:1, Name:"Li Si", Mobile:"13800000002", DeptID:20, DeptName:"Ops", JobNo:"NEXA002", Status:"active", UpdatedAt:now.Format(time.RFC3339)},',
)
t = t.replace(
    '{"t1", 1, "Leave request", "leave", "Zhang San", "boss", "pending", "", now.Add(-2 * time.Hour).Format(time.RFC3339), now.Add(-2 * time.Hour).Format(time.RFC3339)},',
    '{ID:"t1", TenantID:1, Title:"Leave request", ProcessName:"leave", Starter:"Zhang San", Assignee:"boss", Status:"pending", CreatedAt:now.Add(-2*time.Hour).Format(time.RFC3339), UpdatedAt:now.Add(-2*time.Hour).Format(time.RFC3339)},',
)
t = t.replace(
    'Todos: []todo{{"td1", 1, "Review weekly report", "boss", "open", now.Add(24 * time.Hour).Format(time.RFC3339), now.Format(time.RFC3339)}},',
    'Todos: []todo{{ID:"td1", TenantID:1, Title:"Review weekly report", Assignee:"boss", Status:"open", DueAt:now.Add(24*time.Hour).Format(time.RFC3339), CreatedAt:now.Format(time.RFC3339)}},',
)
t = t.replace(
    'Stock: []stockItem{{"SKU-001", "Widget A", 120, "WH-East"}, {"SKU-002", "Widget B", 45, "WH-East"}},',
    'Stock: []stockItem{{SKU:"SKU-001", Name:"Widget A", Qty:120, Warehouse:"WH-East"}, {SKU:"SKU-002", Name:"Widget B", Qty:45, Warehouse:"WH-East"}},',
)
t = t.replace(
    'Purchases: []purchase{{"PO-1", "Supplier X", 3200.5, "open", now.Add(-48 * time.Hour).Format(time.RFC3339)}},',
    'Purchases: []purchase{{ID:"PO-1", Vendor:"Supplier X", Amount:3200.5, Status:"open", CreatedAt:now.Add(-48*time.Hour).Format(time.RFC3339)}},',
)
t = t.replace(
    'Ledger: []ledger{{"L1", "Service revenue", 35000, now.Add(-24 * time.Hour).Format(time.RFC3339)}},',
    'Ledger: []ledger{{ID:"L1", Title:"Service revenue", Amount:35000, At:now.Add(-24*time.Hour).Format(time.RFC3339)}},',
)
t = t.replace(
    'Conversations: []conv{{"c1", "Ops group", 0, now.Format(time.RFC3339)}},',
    'Conversations: []conv{{ID:"c1", Title:"Ops group", Unread:0, UpdatedAt:now.Format(time.RFC3339)}},',
)
t = t.replace(
    '{"rule_bpm", "审批超时", true, "bpm.task.overdue", []string{"notify:assignee"}},\n\t\t\t{"rule_join", "入职待办", true, "hr.employee.joined", []string{"biz.todo.create"}},',
    '{ID:"rule_bpm", Name:"审批超时", Enabled:true, SenseType:"bpm.task.overdue", Actions:[]string{"notify:assignee"}},\n\t\t\t{ID:"rule_join", Name:"入职待办", Enabled:true, SenseType:"hr.employee.joined", Actions:[]string{"biz.todo.create"}},',
)

main_path.write_text(t, encoding="utf-8")
print("main fixed, package count", t.count("package main"))

# domains import strings
d = Path("E:/code/nexa/services/core/cmd/nexa-core/domains.go")
dt = d.read_text(encoding="utf-8")
if '"strings"' not in dt:
    dt = dt.replace(
        'import (\n\t"encoding/json"\n\t"net/http"\n\t"time"\n)',
        'import (\n\t"encoding/json"\n\t"net/http"\n\t"strings"\n\t"time"\n)',
    )
    d.write_text(dt, encoding="utf-8")

# meta files
Path("E:/code/nexa/services/core").mkdir(parents=True, exist_ok=True)
Path("E:/code/nexa/services/core/configs").mkdir(parents=True, exist_ok=True)
Path("E:/code/nexa/services/core/go.mod").write_text(
    "module github.com/lmk1010/nexa/services/core\n\ngo 1.22\n", encoding="utf-8"
)
Path("E:/code/nexa/services/core/configs/config.example.json").write_text(
    """{
  "name": "nexa-core",
  "http": { "addr": ":48080" },
  "dataDir": "./data",
  "iamUrl": "http://127.0.0.1:48081",
  "agentUrl": "http://127.0.0.1:48091",
  "auth": { "enabled": true }
}
""",
    encoding="utf-8",
)
Path("E:/code/nexa/services/core/README.md").write_text(
    """# nexa-core

合并后的业务进程（省资源）：一个端口托管网关边缘 + 全部业务域。

- `:48080` nexa-core（gateway + hr/bpm/business/erp/finance/im/op/ai/data-center）
- `:48081` nexa-iam（认证/租户，独立）
- `:48091` nexa-agent（NeoX，独立）
- 可选 CDC

`services/{gateway,hr,bpm,...}` 保留作对照，部署优先 core。
""",
    encoding="utf-8",
)
print("meta ok")
