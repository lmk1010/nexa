package template

import (
	"fmt"
	"strconv"
	"strings"
	"time"
)

// BuildSQL 用户提交的 filters 编成 WHERE + args, 拼进模板 SQL, 再加 ORDER BY / LIMIT
// 返回 (sql, args, error)
//
// filters 例:
//   {"date":{"from":"2026-01-01","to":"2026-06-30"},"state":["4","5"],"seller_id":123}
//
// 校验：
//   - required filter 必须提供
//   - range/in 值格式合法
//   - 时间字符串必须 YYYY-MM-DD 或 datetime
func (t *Template) BuildSQL(filters map[string]any, limit int) (string, []any, error) {
	if limit <= 0 || limit > t.MaxRows {
		limit = t.MaxRows
	}

	wheres := make([]string, 0, len(t.Filters))
	args := make([]any, 0, len(t.Filters)*2)

	// 稳定顺序遍历 (map 遍历不确定)
	keys := make([]string, 0, len(t.Filters))
	for k := range t.Filters {
		keys = append(keys, k)
	}
	// 简单排序
	for i := 1; i < len(keys); i++ {
		for j := i; j > 0 && keys[j-1] > keys[j]; j-- {
			keys[j-1], keys[j] = keys[j], keys[j-1]
		}
	}

	for _, k := range keys {
		f := t.Filters[k]
		val, ok := filters[k]
		if !ok || isEmpty(val) {
			if f.Required {
				return "", nil, fmt.Errorf("过滤字段 %s 必填", k)
			}
			continue
		}
		w, a, err := f.build(val)
		if err != nil {
			return "", nil, fmt.Errorf("过滤字段 %s: %w", k, err)
		}
		wheres = append(wheres, w)
		args = append(args, a...)
	}

	whereClause := ""
	if len(wheres) > 0 {
		whereClause = strings.Join(wheres, " AND ")
	} else {
		whereClause = "1=1"
	}

	sql := strings.Replace(t.SQL, "{WHERE}", whereClause, 1)

	if len(t.Sort) > 0 {
		sql += " ORDER BY " + strings.Join(t.Sort, ", ")
	}
	sql += fmt.Sprintf(" LIMIT %d", limit)

	return sql, args, nil
}

func (f Filter) build(val any) (string, []any, error) {
	switch f.Type {
	case FilterRange:
		return buildRange(f.Col, val)
	case FilterIn, FilterLookup:
		// lookup 前端给回来的就是 ID 集合，跟 in 同构
		return buildIn(f.Col, val)
	case FilterEq:
		return buildEq(f.Col, val)
	case FilterLike:
		return buildLike(f.Col, val)
	}
	return "", nil, fmt.Errorf("不支持的 filter 类型: %s", f.Type)
}

func buildRange(col string, val any) (string, []any, error) {
	m, ok := val.(map[string]any)
	if !ok {
		return "", nil, fmt.Errorf("range 值必须是 {from,to}")
	}
	from, err := parseDate(m["from"])
	if err != nil {
		return "", nil, fmt.Errorf("from: %w", err)
	}
	to, err := parseDate(m["to"])
	if err != nil {
		return "", nil, fmt.Errorf("to: %w", err)
	}
	// 半开区间: [from, to+1day) 让"选到 6-30"包含 6-30 整天
	toPlus := to.AddDate(0, 0, 1)
	return fmt.Sprintf("%s >= ? AND %s < ?", col, col),
		[]any{from.Format("2006-01-02 15:04:05"), toPlus.Format("2006-01-02 15:04:05")}, nil
}

func buildIn(col string, val any) (string, []any, error) {
	arr, ok := val.([]any)
	if !ok {
		return "", nil, fmt.Errorf("in 值必须是数组")
	}
	if len(arr) == 0 {
		return "", nil, fmt.Errorf("in 值不能空")
	}
	if len(arr) > 500 {
		return "", nil, fmt.Errorf("in 值太多 (%d > 500)", len(arr))
	}
	placeholders := make([]string, len(arr))
	args := make([]any, len(arr))
	for i, v := range arr {
		placeholders[i] = "?"
		args[i] = normalizeArg(v)
	}
	return fmt.Sprintf("%s IN (%s)", col, strings.Join(placeholders, ",")), args, nil
}

func buildEq(col string, val any) (string, []any, error) {
	return fmt.Sprintf("%s = ?", col), []any{normalizeArg(val)}, nil
}

func buildLike(col string, val any) (string, []any, error) {
	s, ok := val.(string)
	if !ok {
		return "", nil, fmt.Errorf("like 值必须是字符串")
	}
	// 简单的用户输入清洗: 剥掉 % 和 _ 通配符
	s = strings.ReplaceAll(s, "%", "")
	s = strings.ReplaceAll(s, "_", "")
	if s == "" {
		return "", nil, fmt.Errorf("like 值不能空")
	}
	return fmt.Sprintf("%s LIKE ?", col), []any{"%" + s + "%"}, nil
}

func parseDate(v any) (time.Time, error) {
	s, ok := v.(string)
	if !ok || s == "" {
		return time.Time{}, fmt.Errorf("空日期")
	}
	// 允许 2006-01-02 或 2006-01-02 15:04:05
	for _, layout := range []string{"2006-01-02", "2006-01-02 15:04:05", time.RFC3339} {
		if t, err := time.ParseInLocation(layout, s, time.Local); err == nil {
			return t, nil
		}
	}
	return time.Time{}, fmt.Errorf("非法日期: %s", s)
}

// normalizeArg JSON 里数字进来是 float64, 打给 mysql 时候如果模板期望是 int/string 可能出问题
// 保守做法: 数值原样传, mysql 驱动会转
func normalizeArg(v any) any {
	switch t := v.(type) {
	case float64:
		// 整数保留 int
		if t == float64(int64(t)) {
			return int64(t)
		}
	case string:
		// 尝试转 int 如果看起来像数字
		if n, err := strconv.ParseInt(t, 10, 64); err == nil {
			return n
		}
	}
	return v
}

func isEmpty(v any) bool {
	if v == nil {
		return true
	}
	switch t := v.(type) {
	case string:
		return t == ""
	case []any:
		return len(t) == 0
	case map[string]any:
		return len(t) == 0
	}
	return false
}
