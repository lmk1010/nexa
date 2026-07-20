// lookup 包 —— 给模板 filter 类型 "lookup" 提供搜索源。
//
// 每个注册的 lookup 是一段 SQL，接受一个 "q" 关键字（模糊匹配 name/user_name/mobile），
// 返回 [{id, label, sub_label?}] 供 APP 搜索下拉展示。用户选中的 ID 集合传回来当 IN 过滤。
//
// 典型 lookup:
//   sellers    — 卖家列表（4900+，按名/账号/手机模糊）
//   shops      — 门店/网点（46000+，按名/区域模糊）
//   employees  — 员工/客服（200+，按名/账号/手机模糊）
//   projects   — 施工项目字典（1300+）
package lookup

import (
	"context"
	"database/sql"
	"fmt"
	"strings"
)

type Item struct {
	ID       string `json:"id"`
	Label    string `json:"label"`
	SubLabel string `json:"sub_label,omitempty"`
}

// Spec 一个 lookup 的定义
type Spec struct {
	Name  string
	Label string
	// SQL 需要 3 个 ? 参数（q, q, limit）；返回列必须是 (id, label, sub_label)
	SQL string
}

// 注册的 lookup 表 —— 加新条只要在这加一个 Spec
var registry = []*Spec{
	{
		Name:  "sellers",
		Label: "卖家",
		SQL: `SELECT user_id AS id,
		             name AS label,
		             CONCAT_WS(' · ', NULLIF(user_name,''), NULLIF(mobile,'')) AS sub_label
		      FROM sys_seller
		      WHERE (? = '' OR name LIKE CONCAT('%', ?, '%')
		                    OR user_name LIKE CONCAT('%', ?, '%')
		                    OR mobile LIKE CONCAT('%', ?, '%'))
		      ORDER BY integral DESC, user_id DESC
		      LIMIT ? OFFSET ?`,
	},
	{
		Name:  "shops",
		Label: "门店/网点",
		SQL: `SELECT id AS id,
		             name AS label,
		             region_path_str AS sub_label
		      FROM outlets_shop
		      WHERE deleted = 0
		        AND (? = '' OR name LIKE CONCAT('%', ?, '%')
		                    OR region_path_str LIKE CONCAT('%', ?, '%')
		                    OR CAST(id AS CHAR) = ?)
		      ORDER BY order_total_num DESC, id DESC
		      LIMIT ? OFFSET ?`,
	},
	{
		Name:  "employees",
		Label: "员工/客服",
		SQL: `SELECT user_id AS id,
		             name AS label,
		             CONCAT_WS(' · ', NULLIF(user_name,''), NULLIF(mobile,'')) AS sub_label
		      FROM sys_user
		      WHERE deleted = 0
		        AND (? = '' OR name LIKE CONCAT('%', ?, '%')
		                    OR user_name LIKE CONCAT('%', ?, '%')
		                    OR mobile LIKE CONCAT('%', ?, '%'))
		      ORDER BY status ASC, user_id DESC
		      LIMIT ? OFFSET ?`,
	},
	{
		Name:  "projects",
		Label: "施工项目",
		SQL: `SELECT id AS id,
		             name AS label,
		             CAST(level AS CHAR) AS sub_label
		      FROM t_project
		      WHERE deleted = 0
		        AND (? = '' OR name LIKE CONCAT('%', ?, '%')
		                    OR CAST(id AS CHAR) = ?
		                    OR CAST(cid AS CHAR) = ?)
		      ORDER BY level ASC, id DESC
		      LIMIT ? OFFSET ?`,
	},
	{
		Name:  "departments",
		Label: "部门",
		SQL: `SELECT dept_id AS id,
		             dept_name AS label,
		             '' AS sub_label
		      FROM sys_dept
		      WHERE deleted = 0
		        AND (? = '' OR dept_name LIKE CONCAT('%', ?, '%')
		                    OR CAST(dept_id AS CHAR) = ?)
		      ORDER BY dept_id ASC
		      LIMIT ? OFFSET ?`,
	},
}

var byName = map[string]*Spec{}

func init() {
	for _, s := range registry {
		byName[s.Name] = s
	}
}

// Get 拿一个 lookup 定义
func Get(name string) (*Spec, bool) {
	s, ok := byName[name]
	return s, ok
}

// AvailableNames 列出所有已注册的 lookup 名（便于前端/文档）
func AvailableNames() []string {
	out := make([]string, 0, len(registry))
	for _, s := range registry {
		out = append(out, s.Name)
	}
	return out
}

// Search 跑一次搜索，支持分页
func (s *Spec) Search(ctx context.Context, db *sql.DB, q string, limit, offset int) ([]Item, error) {
	if limit <= 0 || limit > 100 {
		limit = 30
	}
	if offset < 0 {
		offset = 0
	}
	// SQL 里有几个 ? 参数取决于具体定义 —— 最后 2 个 ? 是 limit / offset，前面全是 q
	nq := strings.Count(s.SQL, "?") - 2
	args := make([]any, 0, nq+2)
	for i := 0; i < nq; i++ {
		args = append(args, q)
	}
	args = append(args, limit, offset)

	rows, err := db.QueryContext(ctx, s.SQL, args...)
	if err != nil {
		return nil, fmt.Errorf("lookup %s: %w", s.Name, err)
	}
	defer rows.Close()
	out := make([]Item, 0, limit)
	for rows.Next() {
		var it Item
		var idAny any
		var sub sql.NullString
		if err := rows.Scan(&idAny, &it.Label, &sub); err != nil {
			return nil, err
		}
		it.ID = fmt.Sprintf("%v", idAny)
		if sub.Valid {
			it.SubLabel = sub.String
		}
		out = append(out, it)
	}
	return out, rows.Err()
}
