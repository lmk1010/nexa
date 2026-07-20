from pathlib import Path

p = Path("E:/code/nexa/services/core/cmd/nexa-core/main.go")
t = p.read_text(encoding="utf-8")

def rep(old, new, label):
    global t
    if old in t:
        t = t.replace(old, new)
        print("ok", label)
        return True
    print("miss", label)
    return False

rep(
    """type department struct {
	ID       int64  `json:"id"`
	Name     string `json:"name"`
	ParentID int64  `json:"parentId"`
}""",
    """type department struct {
	ID       int64  `json:"id"`
	TenantID int64  `json:"tenantId,omitempty"`
	Name     string `json:"name"`
	ParentID int64  `json:"parentId"`
}""",
    "department",
)

rep(
    """type stockItem struct {
	SKU       string `json:"sku"`
	Name      string `json:"name"`
	Qty       int    `json:"qty"`
	Warehouse string `json:"warehouse"`
}""",
    """type stockItem struct {
	TenantID  int64  `json:"tenantId,omitempty"`
	SKU       string `json:"sku"`
	Name      string `json:"name"`
	Qty       int    `json:"qty"`
	Warehouse string `json:"warehouse"`
}""",
    "stock",
)

rep(
    """type purchase struct {
	ID        string  `json:"id"`
	Vendor    string  `json:"vendor"`
	Amount    float64 `json:"amount"`
	Status    string  `json:"status"`
	CreatedAt string  `json:"createdAt"`
}""",
    """type purchase struct {
	ID        string  `json:"id"`
	TenantID  int64   `json:"tenantId,omitempty"`
	Vendor    string  `json:"vendor"`
	Amount    float64 `json:"amount"`
	Status    string  `json:"status"`
	CreatedAt string  `json:"createdAt"`
}""",
    "purchase",
)

rep(
    """type ledger struct {
	ID     string  `json:"id"`
	Title  string  `json:"title"`
	Amount float64 `json:"amount"`
	At     string  `json:"at"`
}""",
    """type ledger struct {
	ID       string  `json:"id"`
	TenantID int64   `json:"tenantId,omitempty"`
	Title    string  `json:"title"`
	Amount   float64 `json:"amount"`
	At       string  `json:"at"`
}""",
    "ledger",
)

rep(
    """type conv struct {
	ID        string `json:"id"`
	Title     string `json:"title"`
	Unread    int    `json:"unread"`
	UpdatedAt string `json:"updatedAt"`
}""",
    """type conv struct {
	ID        string `json:"id"`
	TenantID  int64  `json:"tenantId,omitempty"`
	Title     string `json:"title"`
	Unread    int    `json:"unread"`
	UpdatedAt string `json:"updatedAt"`
}""",
    "conv",
)

rep(
    "type msg struct{ ID, ConversationID, From, Text, At string }",
    """type msg struct {
	ID             string `json:"id"`
	TenantID       int64  `json:"tenantId,omitempty"`
	ConversationID string `json:"conversationId"`
	From           string `json:"from"`
	Text           string `json:"text"`
	At             string `json:"at"`
}""",
    "msg",
)

rep(
    """type exportJob struct {
	ID, TemplateID, State, CreatedAt, Message, Download string
}""",
    """type exportJob struct {
	ID         string `json:"id"`
	TenantID   int64  `json:"tenantId,omitempty"`
	TemplateID string `json:"templateId"`
	State      string `json:"state"`
	CreatedAt  string `json:"createdAt"`
	Message    string `json:"message,omitempty"`
	Download   string `json:"download,omitempty"`
}""",
    "export",
)

rep(
    """type senseEv struct {
	ID, Type, Source, Severity, Message, At string
	Handled                                 bool
	Actions                                 []string
	Payload                                 map[string]any
}""",
    """type senseEv struct {
	ID       string         `json:"id"`
	TenantID int64          `json:"tenantId,omitempty"`
	Type     string         `json:"type"`
	Source   string         `json:"source"`
	Severity string         `json:"severity"`
	Message  string         `json:"message"`
	At       string         `json:"at"`
	Handled  bool           `json:"handled"`
	Actions  []string       `json:"actions,omitempty"`
	Payload  map[string]any `json:"payload,omitempty"`
}""",
    "sense",
)

rep(
    """type syncJob struct {
	ID, Type, Status, Message, StartedAt, FinishedAt string
	Stats                                            map[string]int
}""",
    """type syncJob struct {
	ID         string         `json:"id"`
	TenantID   int64          `json:"tenantId,omitempty"`
	Type       string         `json:"type"`
	Status     string         `json:"status"`
	Message    string         `json:"message"`
	StartedAt  string         `json:"startedAt"`
	FinishedAt string         `json:"finishedAt,omitempty"`
	Stats      map[string]int `json:"stats,omitempty"`
}""",
    "sync",
)

# seed tenant ids
t = t.replace(
    'Departments: []department{{ID:1, Name:"HQ", ParentID:0}, {ID:10, Name:"R&D", ParentID:1}, {ID:20, Name:"Ops", ParentID:1}, {ID:30, Name:"HR", ParentID:1}},',
    'Departments: []department{{ID:1, TenantID:1, Name:"HQ", ParentID:0}, {ID:10, TenantID:1, Name:"R&D", ParentID:1}, {ID:20, TenantID:1, Name:"Ops", ParentID:1}, {ID:30, TenantID:1, Name:"HR", ParentID:1}},',
)
t = t.replace(
    'Stock: []stockItem{{SKU:"SKU-001", Name:"Widget A", Qty:120, Warehouse:"WH-East"}, {SKU:"SKU-002", Name:"Widget B", Qty:45, Warehouse:"WH-East"}},',
    'Stock: []stockItem{{TenantID:1, SKU:"SKU-001", Name:"Widget A", Qty:120, Warehouse:"WH-East"}, {TenantID:1, SKU:"SKU-002", Name:"Widget B", Qty:45, Warehouse:"WH-East"}},',
)
t = t.replace(
    'Purchases: []purchase{{ID:"PO-1", Vendor:"Supplier X", Amount:3200.5, Status:"open", CreatedAt:now.Add(-48*time.Hour).Format(time.RFC3339)}},',
    'Purchases: []purchase{{ID:"PO-1", TenantID:1, Vendor:"Supplier X", Amount:3200.5, Status:"open", CreatedAt:now.Add(-48*time.Hour).Format(time.RFC3339)}},',
)
t = t.replace(
    'Ledger: []ledger{{ID:"L1", Title:"Service revenue", Amount:35000, At:now.Add(-24*time.Hour).Format(time.RFC3339)}},',
    'Ledger: []ledger{{ID:"L1", TenantID:1, Title:"Service revenue", Amount:35000, At:now.Add(-24*time.Hour).Format(time.RFC3339)}},',
)
t = t.replace(
    'Conversations: []conv{{ID:"c1", Title:"Ops group", Unread:0, UpdatedAt:now.Format(time.RFC3339)}},',
    'Conversations: []conv{{ID:"c1", TenantID:1, Title:"Ops group", Unread:0, UpdatedAt:now.Format(time.RFC3339)}},',
)

if "type connectorCfg struct" not in t:
    t = t.replace(
        "func jsonAlias(mux *http.ServeMux, paths []string, h http.HandlerFunc) {",
        """type connectorCfg struct {
	ID        string         `json:"id"`
	TenantID  int64          `json:"tenantId"`
	Enabled   bool           `json:"enabled"`
	Config    map[string]any `json:"config,omitempty"`
	UpdatedAt string         `json:"updatedAt"`
}

func matchTenant(tid, row int64) bool {
	if tid == 0 {
		return true
	}
	return row == 0 || row == tid
}

func jsonAlias(mux *http.ServeMux, paths []string, h http.HandlerFunc) {""",
    )
    print("helpers added")

# coreDB connectors field - find SyncJobs line variants
if "Connectors" not in t.split("type coreDB struct")[1][:800]:
    t = t.replace(
        "SyncJobs    []syncJob",
        "SyncJobs    []syncJob\n\tConnectors  map[string]connectorCfg",
    )
    # fix if produced bad tags - rewrite coreDB carefully later if needed
    print("connectors field attempt")

if "Connectors:" not in t and "Seq: 1002" in t:
    t = t.replace("Seq: 1002,", "Connectors: map[string]connectorCfg{},\n\t\tSeq:       1002,")
elif "Connectors:" not in t and "Seq:1002" in t:
    t = t.replace("Seq:1002", "Connectors: map[string]connectorCfg{}, Seq:1002")

p.write_text(t, encoding="utf-8")
print("main.go written")
