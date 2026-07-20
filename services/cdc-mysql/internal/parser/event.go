package parser

import "time"

// Action is the row change type.
type Action string

const (
	ActionInsert Action = "INSERT"
	ActionUpdate Action = "UPDATE"
	ActionDelete Action = "DELETE"
)

// Position is a binlog coordinate.
type Position struct {
	File string
	Pos  uint32
	GTID string
}

// RowEvent is a normalized row change after decoding a RowsEvent.
type RowEvent struct {
	Schema    string
	Table     string
	Action    Action
	Timestamp time.Time
	// Columns are physical column names when available from TableMap.
	Columns []string
	// Rows: for UPDATE, even indices are before-image, odd after-image
	// (or use Before/After maps below when filled).
	Rows   [][]any
	Before map[string]any
	After  map[string]any
	Pos    Position
}
