// Package writer 流式 xlsx 生成
// excelize StreamWriter 内存 O(1), 一行一 flush。
package writer

import (
	"database/sql"
	"fmt"
	"os"
	"time"

	"github.com/xuri/excelize/v2"

	"github.com/kyx/kyx-data-center/internal/template"
)

// Stream 把 rows 逐行写进 xlsx 文件. progress 每 N 行回调一次。
type Stream struct {
	Template *template.Template
	Path     string
	OnProgress func(rowsWritten int) // 建议每 500-1000 行调一次, 更新 db 进度
	Ctx      DoneChecker            // 取消检查
}

// DoneChecker 每行前查一下是否取消
type DoneChecker interface{ Done() <-chan struct{} }

// Write 主循环: rows.Next() → 拼 row → sw.SetRow → progress
// 返回写入行数, 文件大小, error
func (s *Stream) Write(rows *sql.Rows) (int, int64, error) {
	f := excelize.NewFile()
	defer f.Close()
	sheet := "Sheet1"
	// 用 default sheet index 0

	sw, err := f.NewStreamWriter(sheet)
	if err != nil {
		return 0, 0, fmt.Errorf("new stream writer: %w", err)
	}

	// 表头
	headers := make([]any, len(s.Template.Columns))
	for i, c := range s.Template.Columns {
		headers[i] = excelize.Cell{Value: c.Header, StyleID: 0}
	}
	if err := sw.SetRow("A1", headers, excelize.RowOpts{Height: 20}); err != nil {
		return 0, 0, fmt.Errorf("write header: %w", err)
	}

	// 列宽
	for i, c := range s.Template.Columns {
		if c.Width > 0 {
			col := colName(i + 1)
			_ = sw.SetColWidth(int(colIdx(col)), int(colIdx(col)), float64(c.Width))
		}
	}

	cols, err := rows.Columns()
	if err != nil {
		return 0, 0, fmt.Errorf("get columns: %w", err)
	}
	// SQL 列名 → 模板列位置
	// 模板 columns.field 就是 SQL SELECT 的别名
	fieldIdx := make(map[string]int, len(s.Template.Columns))
	for i, c := range s.Template.Columns {
		fieldIdx[c.Field] = i
	}

	// 每行 scan 用的 []*any
	holders := make([]any, len(cols))
	holderPtrs := make([]any, len(cols))
	for i := range holders {
		holderPtrs[i] = &holders[i]
	}

	rowNum := 2 // Excel 从 1 开始, 1 是表头
	written := 0
	batchThreshold := 500

	for rows.Next() {
		// 取消检查
		if s.Ctx != nil {
			select {
			case <-s.Ctx.Done():
				return written, 0, fmt.Errorf("cancelled")
			default:
			}
		}
		if err := rows.Scan(holderPtrs...); err != nil {
			return written, 0, fmt.Errorf("scan row %d: %w", rowNum, err)
		}
		row := make([]any, len(s.Template.Columns))
		for i, colName := range cols {
			pos, ok := fieldIdx[colName]
			if !ok {
				continue // SQL 列在模板里没定义就跳过
			}
			raw := holders[i]
			row[pos] = renderCell(s.Template, s.Template.Columns[pos], raw)
		}
		if err := sw.SetRow(fmt.Sprintf("A%d", rowNum), row); err != nil {
			return written, 0, fmt.Errorf("set row %d: %w", rowNum, err)
		}
		rowNum++
		written++
		if s.OnProgress != nil && written%batchThreshold == 0 {
			s.OnProgress(written)
		}
	}
	if err := rows.Err(); err != nil {
		return written, 0, err
	}
	if err := sw.Flush(); err != nil {
		return written, 0, fmt.Errorf("flush: %w", err)
	}

	// 保存
	if err := f.SaveAs(s.Path); err != nil {
		return written, 0, fmt.Errorf("save: %w", err)
	}
	// 文件大小
	st, err := os.Stat(s.Path)
	if err != nil {
		return written, 0, err
	}
	if s.OnProgress != nil {
		s.OnProgress(written)
	}
	return written, st.Size(), nil
}

// renderCell 值 → 单元格显示
func renderCell(t *template.Template, c template.Column, raw any) any {
	if raw == nil {
		return ""
	}
	// mysql VARCHAR/CHAR 常返回 []byte, 统一转 string, 让后续 enum/datetime 逻辑好处理
	if b, ok := raw.([]byte); ok {
		raw = string(b)
	}
	// enum 映射
	if c.Enum != "" {
		e := t.Enums[c.Enum]
		key := fmt.Sprint(raw)
		if v, ok := e[key]; ok {
			return v
		}
		// 兜底: 没命中原样输出
		return key
	}
	// datetime
	switch v := raw.(type) {
	case time.Time:
		if v.IsZero() {
			return ""
		}
		return v.Format("2006-01-02 15:04:05")
	}
	return raw
}

// colName 数字 → A B ... Z AA AB
func colName(n int) string {
	name := ""
	for n > 0 {
		n--
		name = string(rune('A'+n%26)) + name
		n /= 26
	}
	return name
}

// colIdx A → 1 (excelize.ColumnNameToNumber wrap, 但避免依赖)
func colIdx(name string) int {
	n := 0
	for _, c := range name {
		n = n*26 + int(c-'A') + 1
	}
	return n
}
