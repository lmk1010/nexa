package template

import (
	"encoding/json"
	"fmt"
	"strings"
)

// Template 模板定义（一个 tab / 一张卡片）
// JSON 落盘, 服务启动加载, 修改需重启（简单可靠 > 热重载）
type Template struct {
	ID          string            `json:"id"`
	Label       string            `json:"label"`          // "下1单时间"
	Category    string            `json:"category"`       // 分组: 订单 / 售后 / 财务
	Description string            `json:"description"`    // 卡片副标题
	Icon        string            `json:"icon"`           // material icon name
	Color       string            `json:"color"`          // hex #4C6EF5
	MaxRows     int               `json:"max_rows"`       // 单次上限
	Visibility  Visibility        `json:"visibility"`     // 权限
	Filters     map[string]Filter `json:"filters"`        // 过滤字段
	Sort        []string          `json:"sort,omitempty"` // ORDER BY clauses
	SQL         string            `json:"sql"`            // {WHERE} 占位符
	Columns     []Column          `json:"columns"`
	Enums       map[string]Enum   `json:"enums,omitempty"`
}

// Visibility 谁能看到 / 谁能用
// - roles: 允许的 role slug（match 大小写不敏感）
// - dept_ids: 允许的部门 id 白名单; 空则不限
// - deny_by_default: 默认拒绝, 只有命中 roles/dept_ids 才允许
type Visibility struct {
	Roles          []string `json:"roles,omitempty"`
	DeptIDs        []int64  `json:"dept_ids,omitempty"`
	DenyByDefault  bool     `json:"deny_by_default,omitempty"`
	AllowAllLogged bool     `json:"allow_all_logged,omitempty"` // 显式允许所有已登录用户
}

// Filter 过滤字段声明
type Filter struct {
	Type     FilterType `json:"type"`               // range / in / eq / like / lookup
	Col      string     `json:"col"`                // 目标列（含别名, 如 "t.create_time"）
	Required bool       `json:"required,omitempty"` // 必填（多用于日期）
	Enum     string     `json:"enum,omitempty"`     // 命中 template.enums 才允许输入
	// Lookup 引用一个注册好的 lookup 源（如 "sellers" / "shops"）。
	// 用户体验：前端渲染成"可搜索多选下拉" —— 输入关键字调 /lookups/{name} 查匹配项，
	// 选中的 ID 集合传回；后端和 "in" 一样生成 "col IN (?...)"。
	Lookup   string     `json:"lookup,omitempty"`
	Label    string     `json:"label,omitempty"`    // 前端显示名
}

type FilterType string

const (
	FilterRange  FilterType = "range"  // 日期区间: {"from":"2026-01-01","to":"2026-06-30"}
	FilterIn     FilterType = "in"     // 集合: [1,2,3]
	FilterEq     FilterType = "eq"     // 单值
	FilterLike   FilterType = "like"   // 模糊: LIKE '%kw%'
	FilterLookup FilterType = "lookup" // 可搜索下拉：ID 集合，UI 走 /lookups/{name}，SQL 同 in
)

// Column 输出列
type Column struct {
	Header string `json:"header"`         // Excel 表头
	Field  string `json:"field"`          // SELECT 别名
	Width  int    `json:"width,omitempty"`
	Type   string `json:"type,omitempty"` // string / number / datetime / enum
	Enum   string `json:"enum,omitempty"` // 值 → 显示名 映射
}

// Enum 枚举字典
type Enum map[string]string

// Validate 加载时校验
func (t *Template) Validate() error {
	if t.ID == "" {
		return fmt.Errorf("template.id 为空")
	}
	if t.Label == "" {
		return fmt.Errorf("template[%s].label 为空", t.ID)
	}
	if t.SQL == "" {
		return fmt.Errorf("template[%s].sql 为空", t.ID)
	}
	if !strings.Contains(t.SQL, "{WHERE}") {
		return fmt.Errorf("template[%s].sql 必须包含 {WHERE} 占位符", t.ID)
	}
	if len(t.Columns) == 0 {
		return fmt.Errorf("template[%s].columns 为空", t.ID)
	}
	if t.MaxRows <= 0 {
		return fmt.Errorf("template[%s].max_rows 必须 > 0", t.ID)
	}
	for k, f := range t.Filters {
		if f.Col == "" {
			return fmt.Errorf("template[%s].filters.%s.col 为空", t.ID, k)
		}
		switch f.Type {
		case FilterRange, FilterIn, FilterEq, FilterLike, FilterLookup:
		default:
			return fmt.Errorf("template[%s].filters.%s.type 不支持: %s", t.ID, k, f.Type)
		}
		if f.Enum != "" {
			if _, ok := t.Enums[f.Enum]; !ok {
				return fmt.Errorf("template[%s].filters.%s.enum 未定义: %s", t.ID, k, f.Enum)
			}
		}
	}
	for i, c := range t.Columns {
		if c.Header == "" || c.Field == "" {
			return fmt.Errorf("template[%s].columns[%d] header/field 为空", t.ID, i)
		}
		if c.Enum != "" {
			if _, ok := t.Enums[c.Enum]; !ok {
				return fmt.Errorf("template[%s].columns[%d].enum 未定义: %s", t.ID, i, c.Enum)
			}
		}
	}
	return nil
}

// AllowFor 判断某用户是否能看到 / 使用此模板
// - deny_by_default=true: 白名单模式, 必须命中 role 或 dept
// - allow_all_logged=true: 只要登录就通
// - 默认: 未设定 visibility 时, 允许所有登录
func (t *Template) AllowFor(userDeptID int64, roles []string) bool {
	v := &t.Visibility
	if v.AllowAllLogged {
		return true
	}
	if !v.DenyByDefault && len(v.Roles) == 0 && len(v.DeptIDs) == 0 {
		return true
	}
	for _, r := range roles {
		for _, allowed := range v.Roles {
			if strings.EqualFold(r, allowed) {
				return true
			}
		}
	}
	for _, id := range v.DeptIDs {
		if id == userDeptID {
			return true
		}
	}
	return false
}

// Parse 一个 json bytes 解析成 Template
func Parse(b []byte) (*Template, error) {
	var t Template
	if err := json.Unmarshal(b, &t); err != nil {
		return nil, fmt.Errorf("parse template json: %w", err)
	}
	if err := t.Validate(); err != nil {
		return nil, err
	}
	return &t, nil
}
