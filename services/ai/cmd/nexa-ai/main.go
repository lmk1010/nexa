// nexa-ai — AI control plane: skills, intent, sense, automation (Go).
package main

import (
	"context"
	"encoding/json"
	"errors"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"syscall"
	"time"
)

var version = "0.4.0-ai-native"

type config struct {
	Name string `json:"name"`
	HTTP struct {
		Addr string `json:"addr"`
	} `json:"http"`
	DataDir string `json:"dataDir"`
}

type skill struct {
	ID          string            `json:"id"`
	Domain      string            `json:"domain"`
	Title       string            `json:"title"`
	Description string            `json:"description"`
	Method      string            `json:"method"`
	Path        string            `json:"path"`
	Auth        bool              `json:"auth"`
	Keywords    []string          `json:"keywords"`
	Examples    []string          `json:"examples"`
	InputHint   map[string]string `json:"inputHint,omitempty"`
}

type senseEvent struct {
	ID        string         `json:"id"`
	Type      string         `json:"type"`
	Source    string         `json:"source"`
	Severity  string         `json:"severity"`
	Message   string         `json:"message"`
	Payload   map[string]any `json:"payload,omitempty"`
	At        string         `json:"at"`
	Handled   bool           `json:"handled"`
	Actions   []string       `json:"actions,omitempty"`
}

type autoRule struct {
	ID          string   `json:"id"`
	Name        string   `json:"name"`
	Enabled     bool     `json:"enabled"`
	SenseType   string   `json:"senseType"`
	Actions     []string `json:"actions"` // skill ids or notify:*
	Description string   `json:"description"`
}

type autoRun struct {
	ID        string   `json:"id"`
	RuleID    string   `json:"ruleId"`
	SenseID   string   `json:"senseId"`
	Actions   []string `json:"actions"`
	Status    string   `json:"status"`
	Message   string   `json:"message"`
	At        string   `json:"at"`
}

type db struct {
	Sense []senseEvent `json:"sense"`
	Rules []autoRule   `json:"rules"`
	Runs  []autoRun    `json:"runs"`
}

type server struct {
	cfg    config
	mu     sync.Mutex
	db     db
	path   string
	skills []skill
}

func main() {
	cfgPath := flag.String("config", "./configs/config.json", "config path")
	flag.Parse()
	cfg := config{Name: "nexa-ai", DataDir: "./data"}
	cfg.HTTP.Addr = ":48089"
	if raw, err := os.ReadFile(*cfgPath); err == nil {
		_ = json.Unmarshal(raw, &cfg)
	}
	if v := os.Getenv("NEXA_DATA_DIR"); v != "" {
		cfg.DataDir = v
	}
	if cfg.HTTP.Addr == "" {
		cfg.HTTP.Addr = ":48089"
	}
	if cfg.DataDir == "" {
		cfg.DataDir = "./data"
	}

	s := &server{cfg: cfg, path: filepath.Join(cfg.DataDir, "ai.json"), skills: defaultSkills()}
	if err := s.loadOrSeed(); err != nil {
		log.Fatalf("store: %v", err)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{"status": "UP", "service": cfg.Name, "version": version, "skills": len(s.skills)})
	})
	mux.HandleFunc("/v1/ai/skills", s.handleSkills)
	mux.HandleFunc("/v1/ai/skills/", s.handleSkillByID)
	mux.HandleFunc("/v1/ai/intent/route", s.handleIntent)
	mux.HandleFunc("/v1/ai/sense", s.handleSense)
	mux.HandleFunc("/v1/ai/sense/recent", s.handleSenseRecent)
	mux.HandleFunc("/v1/ai/automation/rules", s.handleRules)
	mux.HandleFunc("/v1/ai/automation/runs", s.handleRuns)
	mux.HandleFunc("/v1/ai/automation/tick", s.handleTick)
	mux.HandleFunc("/v1/ai/models", s.handleModels)
	mux.HandleFunc("/v1/ai/reception/config", s.handleReceptionConfig)
	mux.HandleFunc("/v1/ai/reception/records", s.handleReceptionRecords)
	mux.HandleFunc("/v1/ai/assistant/bootstrap", s.handleBootstrap)
	mux.HandleFunc("/v1/ai/connectors", s.handleConnectors)
	mux.HandleFunc("/", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{
			"service": cfg.Name,
			"version": version,
			"role":    "ai-control-plane",
			"apis": []string{
				"/v1/ai/skills", "/v1/ai/intent/route", "/v1/ai/sense", "/v1/ai/sense/recent",
				"/v1/ai/automation/rules", "/v1/ai/automation/runs", "/v1/ai/automation/tick",
				"/v1/ai/assistant/bootstrap",
				"/v1/ai/connectors",
			},
		})
	})

	srv := &http.Server{Addr: cfg.HTTP.Addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("[boot] %s %s on %s skills=%d data=%s", cfg.Name, version, cfg.HTTP.Addr, len(s.skills), s.path)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("http: %v", err)
		}
	}()
	// background automation ticker
	go s.loop(ctx)
	<-ctx.Done()
	shCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shCtx)
}

func (s *server) loop(ctx context.Context) {
	t := time.NewTicker(30 * time.Second)
	defer t.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-t.C:
			s.mu.Lock()
			n := s.processAutomationLocked()
			if n > 0 {
				_ = s.save()
				log.Printf("[automation] handled %d sense events", n)
			}
			s.mu.Unlock()
		}
	}
}

func defaultSkills() []skill {
	return []skill{
		{ID: "iam.whoami", Domain: "iam", Title: "当前用户", Description: "查询当前登录用户与角色权限", Method: "GET", Path: "/v1/iam/me", Auth: true, Keywords: []string{"我是谁", "whoami", "当前用户", "权限", "me", "profile"}, Examples: []string{"我是谁", "我有哪些权限"}},
		{ID: "iam.permissions", Domain: "iam", Title: "权限点", Description: "列出当前用户权限点", Method: "GET", Path: "/v1/iam/permissions", Auth: true, Keywords: []string{"权限", "permission"}, Examples: []string{"列出我的权限"}},
		{ID: "hr.employees.search", Domain: "hr", Title: "员工列表", Description: "查询花名册/员工列表", Method: "GET", Path: "/v1/hr/employees", Auth: true, Keywords: []string{"员工", "花名册", "通讯录", "人员", "hr", "employee", "roster", "staff"}, Examples: []string{"查一下员工列表", "花名册有谁"}},
		{ID: "hr.depts.tree", Domain: "hr", Title: "组织树", Description: "查询部门组织树", Method: "GET", Path: "/v1/hr/departments/tree", Auth: true, Keywords: []string{"部门", "组织", "架构"}, Examples: []string{"公司组织架构"}},
		{ID: "hr.dingtalk.sync", Domain: "hr", Title: "钉钉同步", Description: "触发钉钉通讯录/花名册同步", Method: "POST", Path: "/v1/hr/dingtalk/sync", Auth: true, Keywords: []string{"钉钉", "同步", "dingtalk", "sync", "directory"}, Examples: []string{"同步钉钉通讯录"}, InputHint: map[string]string{"mode": "full|directory|roster"}},
		{ID: "hr.dingtalk.status", Domain: "hr", Title: "钉钉状态", Description: "查看钉钉同步状态", Method: "GET", Path: "/v1/hr/dingtalk/status", Auth: true, Keywords: []string{"钉钉状态", "同步状态"}, Examples: []string{"钉钉同步到哪了"}},
		{ID: "bpm.todo.list", Domain: "bpm", Title: "审批待办", Description: "查询待审批任务", Method: "GET", Path: "/v1/bpm/tasks/todo", Auth: true, Keywords: []string{"审批", "待办", "流程", "bpm", "approve", "todo", "approval", "pending"}, Examples: []string{"我有哪些审批待办"}},
		{ID: "bpm.done.list", Domain: "bpm", Title: "已办审批", Description: "查询已处理审批", Method: "GET", Path: "/v1/bpm/tasks/done", Auth: true, Keywords: []string{"已办", "审批历史"}, Examples: []string{"已处理的审批"}},
		{ID: "bpm.task.approve", Domain: "bpm", Title: "审批动作", Description: "通过或驳回审批任务", Method: "POST", Path: "/v1/bpm/tasks/approve", Auth: true, Keywords: []string{"通过", "驳回", "同意", "拒绝"}, Examples: []string{"通过请假单"}, InputHint: map[string]string{"taskId": "string", "action": "approve|reject", "reason": "string"}},
		{ID: "bpm.task.start", Domain: "bpm", Title: "发起审批", Description: "发起一个审批任务", Method: "POST", Path: "/v1/bpm/tasks/start", Auth: true, Keywords: []string{"发起审批", "提交申请"}, Examples: []string{"帮我发起采购审批"}},
		{ID: "biz.todo.list", Domain: "business", Title: "业务待办", Description: "查询业务待办事项", Method: "GET", Path: "/v1/business/todos", Auth: true, Keywords: []string{"待办", "todo", "任务"}, Examples: []string{"我的待办事项"}},
		{ID: "biz.todo.create", Domain: "business", Title: "创建待办", Description: "创建业务待办", Method: "POST", Path: "/v1/business/todos", Auth: true, Keywords: []string{"创建待办", "加待办"}, Examples: []string{"给我加个待办"}},
		{ID: "biz.work.requirements", Domain: "business", Title: "工作要求", Description: "查询工作要求", Method: "GET", Path: "/v1/business/work-requirements", Auth: true, Keywords: []string{"工作要求", "目标"}, Examples: []string{"有哪些工作要求"}},
		{ID: "biz.calendar.today", Domain: "business", Title: "日历事件", Description: "查询日历事件", Method: "GET", Path: "/v1/business/calendar/events", Auth: true, Keywords: []string{"日历", "会议", "日程"}, Examples: []string{"今天有什么会"}},
		{ID: "biz.reception.latest", Domain: "business", Title: "前台访客", Description: "最近前台接待记录", Method: "GET", Path: "/v1/business/reception/latest", Auth: true, Keywords: []string{"前台", "访客", "接待"}, Examples: []string{"最近有谁来访"}},
		{ID: "erp.stock.summary", Domain: "erp", Title: "库存概况", Description: "查询库存汇总", Method: "GET", Path: "/v1/erp/stock/summary", Auth: true, Keywords: []string{"库存", "仓", "sku", "erp", "stock", "inventory"}, Examples: []string{"库存怎么样"}},
		{ID: "erp.purchase.orders", Domain: "erp", Title: "采购单", Description: "查询采购订单", Method: "GET", Path: "/v1/erp/purchase/orders", Auth: true, Keywords: []string{"采购", "purchase"}, Examples: []string{"有哪些采购单"}},
		{ID: "finance.month.summary", Domain: "finance", Title: "月度财务", Description: "本月收支利润摘要", Method: "GET", Path: "/v1/finance/summary/monthly", Auth: true, Keywords: []string{"财务", "收支", "利润", "营收", "finance", "profit", "revenue"}, Examples: []string{"本月财务怎么样"}},
		{ID: "finance.ledger.recent", Domain: "finance", Title: "近期流水", Description: "最近财务流水", Method: "GET", Path: "/v1/finance/ledger/recent", Auth: true, Keywords: []string{"流水", "记账", "ledger"}, Examples: []string{"最近流水"}},
		{ID: "im.conversations", Domain: "im", Title: "会话列表", Description: "IM 会话列表", Method: "GET", Path: "/v1/im/conversations", Auth: true, Keywords: []string{"消息", "会话", "im", "聊天"}, Examples: []string{"我有哪些会话"}},
		{ID: "im.contacts", Domain: "im", Title: "IM 通讯录", Description: "IM 联系人", Method: "GET", Path: "/v1/im/contacts", Auth: true, Keywords: []string{"联系人", "好友"}, Examples: []string{"通讯录"}},
		{ID: "op.health", Domain: "op", Title: "运维状态", Description: "平台与服务健康概览", Method: "GET", Path: "/v1/op/status", Auth: true, Keywords: []string{"运维", "健康", "监控", "status", "health", "ops", "monitor"}, Examples: []string{"系统正常吗"}},
		{ID: "op.audit.recent", Domain: "op", Title: "审计日志", Description: "最近操作审计", Method: "GET", Path: "/v1/op/audit/recent", Auth: true, Keywords: []string{"审计", "日志"}, Examples: []string{"最近谁登录了"}},
		{ID: "ai.reception.config", Domain: "ai", Title: "接待配置", Description: "智能前台配置", Method: "GET", Path: "/v1/ai/reception/config", Auth: true, Keywords: []string{"接待配置", "asr"}, Examples: []string{"前台AI怎么配的"}},
		{ID: "ai.reception.records", Domain: "ai", Title: "接待记录", Description: "智能前台识别记录", Method: "GET", Path: "/v1/ai/reception/records", Auth: true, Keywords: []string{"接待记录"}, Examples: []string{"接待识别记录"}},
		{ID: "platform.services", Domain: "gateway", Title: "服务地图", Description: "网关 upstream 与路由", Method: "GET", Path: "/v1/platform/services", Auth: false, Keywords: []string{"服务", "路由", "网关"}, Examples: []string{"有哪些后端服务"}},
	}
}

func (s *server) loadOrSeed() error {
	_ = os.MkdirAll(filepath.Dir(s.path), 0o755)
	raw, err := os.ReadFile(s.path)
	if err == nil {
		return json.Unmarshal(raw, &s.db)
	}
	if !errors.Is(err, os.ErrNotExist) {
		return err
	}
	s.db = db{
		Sense: []senseEvent{},
		Rules: []autoRule{
			{ID: "rule_bpm_overdue", Name: "审批超时催办", Enabled: true, SenseType: "bpm.task.overdue", Actions: []string{"notify:assignee", "bpm.todo.list"}, Description: "审批超时后提醒处理人"},
			{ID: "rule_hr_joined", Name: "入职自动待办", Enabled: true, SenseType: "hr.employee.joined", Actions: []string{"biz.todo.create", "notify:hr"}, Description: "新员工入职创建 onboarding 待办"},
			{ID: "rule_stock_low", Name: "库存预警", Enabled: true, SenseType: "erp.stock.low", Actions: []string{"erp.stock.summary", "biz.todo.create", "notify:ops"}, Description: "低库存创建工作待办并通知运营"},
			{ID: "rule_reception", Name: "访客感知", Enabled: true, SenseType: "biz.reception.detected", Actions: []string{"biz.reception.latest", "notify:host"}, Description: "前台检测到访客后通知主人"},
			{ID: "rule_service_down", Name: "服务宕机告警", Enabled: true, SenseType: "op.service.down", Actions: []string{"op.health", "notify:admin"}, Description: "服务不可用时告警"},
		},
		Runs: []autoRun{},
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

func (s *server) handleSkills(w http.ResponseWriter, r *http.Request) {
	domain := r.URL.Query().Get("domain")
	out := make([]skill, 0, len(s.skills))
	for _, sk := range s.skills {
		if domain == "" || sk.Domain == domain {
			out = append(out, sk)
		}
	}
	writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(out)})
}

func (s *server) handleSkillByID(w http.ResponseWriter, r *http.Request) {
	id := strings.TrimPrefix(r.URL.Path, "/v1/ai/skills/")
	for _, sk := range s.skills {
		if sk.ID == id {
			writeJSON(w, 200, map[string]any{"code": 0, "data": sk})
			return
		}
	}
	writeJSON(w, 404, map[string]any{"code": 404, "msg": "skill not found"})
}

func (s *server) handleIntent(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, 405, map[string]any{"code": 405})
		return
	}
	var body struct {
		Text string `json:"text"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || strings.TrimSpace(body.Text) == "" {
		writeJSON(w, 400, map[string]any{"code": 400, "msg": "text required"})
		return
	}
	type ranked struct {
		Skill  skill   `json:"skill"`
		Score  float64 `json:"score"`
		Reason string  `json:"reason"`
	}
	text := strings.ToLower(body.Text)
	var ranks []ranked
	for _, sk := range s.skills {
		score := 0.0
		reasons := []string{}
		for _, kw := range sk.Keywords {
			if strings.Contains(body.Text, kw) || strings.Contains(text, strings.ToLower(kw)) {
				score += 2
				reasons = append(reasons, "kw:"+kw)
			}
		}
		for _, ex := range sk.Examples {
			if strings.Contains(body.Text, ex) {
				score += 3
				reasons = append(reasons, "example")
			}
		}
		if strings.Contains(text, sk.Domain) {
			score += 1
			reasons = append(reasons, "domain")
		}
		if score > 0 {
			ranks = append(ranks, ranked{Skill: sk, Score: score, Reason: strings.Join(reasons, ",")})
		}
	}
	sort.Slice(ranks, func(i, j int) bool { return ranks[i].Score > ranks[j].Score })
	if len(ranks) > 5 {
		ranks = ranks[:5]
	}
	writeJSON(w, 200, map[string]any{
		"code": 0,
		"data": map[string]any{
			"text":    body.Text,
			"matches": ranks,
			"hint":    "Agent should call top skill.path via gateway with user token",
		},
	})
}

func (s *server) handleSense(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, 405, map[string]any{"code": 405})
		return
	}
	var body struct {
		Type     string         `json:"type"`
		Source   string         `json:"source"`
		Severity string         `json:"severity"`
		Message  string         `json:"message"`
		Payload  map[string]any `json:"payload"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.Type == "" {
		writeJSON(w, 400, map[string]any{"code": 400, "msg": "type required"})
		return
	}
	if body.Severity == "" {
		body.Severity = "info"
	}
	if body.Source == "" {
		body.Source = "unknown"
	}
	ev := senseEvent{
		ID:       "sense_" + time.Now().Format("20060102_150405.000"),
		Type:     body.Type,
		Source:   body.Source,
		Severity: body.Severity,
		Message:  body.Message,
		Payload:  body.Payload,
		At:       time.Now().Format(time.RFC3339),
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	s.db.Sense = append([]senseEvent{ev}, s.db.Sense...)
	if len(s.db.Sense) > 500 {
		s.db.Sense = s.db.Sense[:500]
	}
	n := s.processAutomationLocked()
	_ = s.save()
	outEv := ev
	if len(s.db.Sense) > 0 && s.db.Sense[0].ID == ev.ID {
		outEv = s.db.Sense[0]
	}
	writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{"event": outEv, "automationHandled": n}})
}

func (s *server) handleSenseRecent(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	limit := 20
	out := s.db.Sense
	if len(out) > limit {
		out = out[:limit]
	}
	writeJSON(w, 200, map[string]any{"code": 0, "data": out, "total": len(s.db.Sense)})
}

func (s *server) handleRules(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if r.Method == http.MethodPost {
		var rule autoRule
		if err := json.NewDecoder(r.Body).Decode(&rule); err != nil {
			writeJSON(w, 400, map[string]any{"code": 400})
			return
		}
		if rule.ID == "" {
			rule.ID = "rule_" + time.Now().Format("150405")
		}
		s.db.Rules = append(s.db.Rules, rule)
		_ = s.save()
		writeJSON(w, 200, map[string]any{"code": 0, "data": rule})
		return
	}
	writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Rules, "total": len(s.db.Rules)})
}

func (s *server) handleRuns(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	writeJSON(w, 200, map[string]any{"code": 0, "data": s.db.Runs, "total": len(s.db.Runs)})
}

func (s *server) handleTick(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, 405, map[string]any{"code": 405})
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	n := s.processAutomationLocked()
	_ = s.save()
	writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{"handled": n}})
}

func (s *server) processAutomationLocked() int {
	handled := 0
	for i := range s.db.Sense {
		ev := &s.db.Sense[i]
		if ev.Handled {
			continue
		}
		for _, rule := range s.db.Rules {
			if !rule.Enabled || rule.SenseType != ev.Type {
				continue
			}
			run := autoRun{
				ID:      "run_" + time.Now().Format("150405.000"),
				RuleID:  rule.ID,
				SenseID: ev.ID,
				Actions: rule.Actions,
				Status:  "ok",
				Message: "queued actions for agent/gateway execution",
				At:      time.Now().Format(time.RFC3339),
			}
			s.db.Runs = append([]autoRun{run}, s.db.Runs...)
			if len(s.db.Runs) > 200 {
				s.db.Runs = s.db.Runs[:200]
			}
			ev.Handled = true
			ev.Actions = append(ev.Actions, rule.Actions...)
			handled++
			break
		}
	}
	return handled
}

func (s *server) handleModels(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, 200, map[string]any{"code": 0, "data": []map[string]any{
		{"id": "deepseek-chat", "provider": "deepseek", "kind": "chat"},
		{"id": "neox-default", "provider": "neox", "kind": "agent"},
	}})
}

func (s *server) handleReceptionConfig(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{
		"enabled": true, "notifyEnabled": true, "keywords": []string{"面试", "快递", "访客"}, "updatedAt": time.Now().Format(time.RFC3339),
	}})
}

func (s *server) handleReceptionRecords(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, 200, map[string]any{"code": 0, "data": []map[string]any{
		{"id": "rr1", "text": "访客来访面试", "status": "done", "at": time.Now().Add(-1 * time.Hour).Format(time.RFC3339)},
	}, "total": 1})
}

func (s *server) handleBootstrap(w http.ResponseWriter, r *http.Request) {
	// Payload for agent system prompt / tool bootstrap
	domains := map[string]int{}
	for _, sk := range s.skills {
		domains[sk.Domain]++
	}
	writeJSON(w, 200, map[string]any{"code": 0, "data": map[string]any{
		"assistant": map[string]any{
			"name":    "Nexa Enterprise Assistant",
			"mission": "企业全能助手：用技能调用完成 OA/人事/审批/经营/运维，先感知再自动化，回答可解释。",
			"style":   []string{"先 intent 路由技能", "再 api_call 网关 path", "大数据走 data-center/warehouse", "不编造无权限数据"},
		},
		"skillsTotal": len(s.skills),
		"domains":     domains,
		"senseTypes":  []string{"bpm.task.overdue", "hr.employee.joined", "erp.stock.low", "biz.reception.detected", "op.service.down"},
		"gateway":     "http://127.0.0.1:48080",
		"docs":        []string{"docs/architecture/ai-native.md", "docs/GOAL.md"},
	}})
}


func (s *server) handleConnectors(w http.ResponseWriter, r *http.Request) {
	// Product connector catalog: nexa is the body; these are optional plugs.
	list := []map[string]any{
		{"id": "dingtalk-import", "kind": "dingtalk_import", "name": "钉钉通讯录导入", "description": "可选导入已有钉钉组织，非主数据权威", "enabled": true, "status": "ready", "env": []string{"NEXA_DINGTALK_APP_KEY", "NEXA_DINGTALK_APP_SECRET"}},
		{"id": "cdc-mysql", "kind": "cdc_mysql", "name": "MySQL CDC", "description": "业务库 binlog -> warehouse", "enabled": true, "status": "ready"},
		{"id": "data-center", "kind": "warehouse_ro", "name": "数据中心导出", "description": "模板导出与经营取数", "enabled": true, "status": "ready", "base": ":48092"},
		{"id": "business-http", "kind": "business_api", "name": "HTTP 业务连接器", "description": "通用外部业务 API", "enabled": true, "status": "ready"},
	}
	writeJSON(w, 200, map[string]any{"code": 0, "data": list, "total": len(list), "note": "connectors plug into nexa; nexa is not an OA-dingtalk adapter"})
}


func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
