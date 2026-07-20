package store

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"time"
)

// JobState 生命周期状态
type JobState string

const (
	StatePending   JobState = "pending"
	StateQueued    JobState = "queued"
	StateRunning   JobState = "running"
	StateDone      JobState = "done"
	StateFailed    JobState = "failed"
	StateCancelled JobState = "cancelled"
)

// Job export_jobs 一行
type Job struct {
	ID          string     `json:"id"`
	OwnerID     int64      `json:"owner_id"`
	OwnerName   string     `json:"owner_name"`
	TemplateID  string     `json:"template_id"`
	// TemplateLabel 由 API handler 从 Registry 补上；DB 不存（模板重命名时旧任务也能拿到当前名）
	TemplateLabel string   `json:"template_label,omitempty"`
	ParamsJSON  string     `json:"params_json"`
	State       JobState   `json:"state"`
	RowsWritten int        `json:"rows_written"`
	RowsTotal   *int       `json:"rows_total,omitempty"`
	FilePath    string     `json:"file_path,omitempty"`
	FileSize    *int64     `json:"file_size,omitempty"`
	ErrorMsg    string     `json:"error_msg,omitempty"`
	QueuePos    *int       `json:"queue_pos,omitempty"`
	CreatedAt   time.Time  `json:"created_at"`
	StartedAt   *time.Time `json:"started_at,omitempty"`
	FinishedAt  *time.Time `json:"finished_at,omitempty"`
}

// Store 封装 export_jobs / export_logs 的 CRUD
type Store struct {
	db *sql.DB
}

func New(db *sql.DB) *Store { return &Store{db: db} }

// Insert 新建任务
func (s *Store) Insert(ctx context.Context, j *Job) error {
	_, err := s.db.ExecContext(ctx, `
		INSERT INTO export_jobs
		  (id, owner_id, owner_name, template_id, params_json, state, queue_pos)
		VALUES (?,?,?,?,?,?,?)`,
		j.ID, j.OwnerID, j.OwnerName, j.TemplateID, j.ParamsJSON, string(j.State), j.QueuePos,
	)
	return err
}

// Get 按 id 查
func (s *Store) Get(ctx context.Context, id string) (*Job, error) {
	row := s.db.QueryRowContext(ctx, `
		SELECT id, owner_id, owner_name, template_id, params_json, state, rows_written, rows_total,
		       file_path, file_size, IFNULL(error_msg,''), queue_pos,
		       created_at, started_at, finished_at
		FROM export_jobs WHERE id=?`, id)
	var j Job
	var rowsTotal, queuePos sql.NullInt64
	var filePath, errorMsg sql.NullString
	var fileSize sql.NullInt64
	var startedAt, finishedAt sql.NullTime
	err := row.Scan(&j.ID, &j.OwnerID, &j.OwnerName, &j.TemplateID, &j.ParamsJSON, &j.State,
		&j.RowsWritten, &rowsTotal, &filePath, &fileSize, &errorMsg, &queuePos,
		&j.CreatedAt, &startedAt, &finishedAt)
	if err == sql.ErrNoRows {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, err
	}
	if rowsTotal.Valid {
		n := int(rowsTotal.Int64)
		j.RowsTotal = &n
	}
	if queuePos.Valid {
		n := int(queuePos.Int64)
		j.QueuePos = &n
	}
	if filePath.Valid {
		j.FilePath = filePath.String
	}
	if fileSize.Valid {
		j.FileSize = &fileSize.Int64
	}
	if errorMsg.Valid {
		j.ErrorMsg = errorMsg.String
	}
	if startedAt.Valid {
		t := startedAt.Time
		j.StartedAt = &t
	}
	if finishedAt.Valid {
		t := finishedAt.Time
		j.FinishedAt = &t
	}
	return &j, nil
}

// ListByOwner 查我自己的
func (s *Store) ListByOwner(ctx context.Context, ownerID int64, limit int) ([]*Job, error) {
	if limit <= 0 {
		limit = 20
	}
	rows, err := s.db.QueryContext(ctx, `
		SELECT id, template_id, state, rows_written, IFNULL(rows_total,0), IFNULL(file_size,0),
		       IFNULL(error_msg,''), IFNULL(queue_pos,0), created_at, started_at, finished_at
		FROM export_jobs WHERE owner_id=? ORDER BY created_at DESC LIMIT ?`, ownerID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	return scanJobList(rows, ownerID)
}

// ListActive 大厅：所有 queued / running 的任务
func (s *Store) ListActive(ctx context.Context) ([]*Job, error) {
	rows, err := s.db.QueryContext(ctx, `
		SELECT id, template_id, state, rows_written, IFNULL(rows_total,0), 0,
		       '', IFNULL(queue_pos,0), created_at, started_at, finished_at
		FROM export_jobs WHERE state IN ('queued','running') ORDER BY created_at ASC LIMIT 50`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	return scanJobList(rows, 0)
}

// CountActiveByOwner 用户当前正在跑/排队的数量（quota）
func (s *Store) CountActiveByOwner(ctx context.Context, ownerID int64) (int, error) {
	var n int
	err := s.db.QueryRowContext(ctx,
		`SELECT COUNT(*) FROM export_jobs WHERE owner_id=? AND state IN ('pending','queued','running')`,
		ownerID).Scan(&n)
	return n, err
}

// MarkQueued
func (s *Store) MarkQueued(ctx context.Context, id string, pos int) error {
	_, err := s.db.ExecContext(ctx,
		`UPDATE export_jobs SET state='queued', queue_pos=? WHERE id=? AND state='pending'`, pos, id)
	return err
}

// MarkRunning
func (s *Store) MarkRunning(ctx context.Context, id string) error {
	_, err := s.db.ExecContext(ctx,
		`UPDATE export_jobs SET state='running', started_at=NOW(), queue_pos=NULL WHERE id=?`, id)
	return err
}

// UpdateProgress
func (s *Store) UpdateProgress(ctx context.Context, id string, rowsWritten int) error {
	_, err := s.db.ExecContext(ctx,
		`UPDATE export_jobs SET rows_written=? WHERE id=? AND state='running'`, rowsWritten, id)
	return err
}

// MarkDone
func (s *Store) MarkDone(ctx context.Context, id string, filePath string, fileSize int64, rowsWritten int) error {
	_, err := s.db.ExecContext(ctx,
		`UPDATE export_jobs SET state='done', file_path=?, file_size=?, rows_written=?, finished_at=NOW() WHERE id=?`,
		filePath, fileSize, rowsWritten, id)
	return err
}

// MarkFailed
func (s *Store) MarkFailed(ctx context.Context, id, errMsg string) error {
	_, err := s.db.ExecContext(ctx,
		`UPDATE export_jobs SET state='failed', error_msg=?, finished_at=NOW() WHERE id=?`, errMsg, id)
	return err
}

// MarkCancelled
func (s *Store) MarkCancelled(ctx context.Context, id string, ownerID int64) error {
	_, err := s.db.ExecContext(ctx, `
		UPDATE export_jobs SET state='cancelled', finished_at=NOW()
		WHERE id=? AND owner_id=? AND state IN ('pending','queued','running')`, id, ownerID)
	return err
}

// UnmarshalParams 反序列化 filters
func (j *Job) UnmarshalParams() (map[string]any, error) {
	var m map[string]any
	if err := json.Unmarshal([]byte(j.ParamsJSON), &m); err != nil {
		return nil, err
	}
	return m, nil
}

var ErrNotFound = errors.New("job not found")

func scanJobList(rows *sql.Rows, ownerHint int64) ([]*Job, error) {
	out := make([]*Job, 0, 16)
	for rows.Next() {
		var j Job
		var rowsTotal, fileSize, queuePos int64
		var errorMsg string
		var startedAt, finishedAt sql.NullTime
		if err := rows.Scan(&j.ID, &j.TemplateID, &j.State, &j.RowsWritten,
			&rowsTotal, &fileSize, &errorMsg, &queuePos,
			&j.CreatedAt, &startedAt, &finishedAt); err != nil {
			return nil, err
		}
		if rowsTotal > 0 {
			n := int(rowsTotal)
			j.RowsTotal = &n
		}
		if fileSize > 0 {
			j.FileSize = &fileSize
		}
		if errorMsg != "" {
			j.ErrorMsg = errorMsg
		}
		if queuePos > 0 {
			n := int(queuePos)
			j.QueuePos = &n
		}
		if startedAt.Valid {
			t := startedAt.Time
			j.StartedAt = &t
		}
		if finishedAt.Valid {
			t := finishedAt.Time
			j.FinishedAt = &t
		}
		j.OwnerID = ownerHint
		out = append(out, &j)
	}
	return out, rows.Err()
}

// EnsureSchema 启动时 CREATE TABLE IF NOT EXISTS
func EnsureSchema(ctx context.Context, db *sql.DB) error {
	stmts := []string{
		`CREATE TABLE IF NOT EXISTS export_jobs (
			id CHAR(36) PRIMARY KEY,
			owner_id BIGINT NOT NULL,
			owner_name VARCHAR(100) NOT NULL,
			template_id VARCHAR(50) NOT NULL,
			params_json JSON NOT NULL,
			state VARCHAR(16) NOT NULL DEFAULT 'pending',
			rows_written INT NOT NULL DEFAULT 0,
			rows_total INT DEFAULT NULL,
			file_path VARCHAR(255) DEFAULT NULL,
			file_size BIGINT DEFAULT NULL,
			error_msg TEXT DEFAULT NULL,
			queue_pos INT DEFAULT NULL,
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			started_at DATETIME DEFAULT NULL,
			finished_at DATETIME DEFAULT NULL,
			INDEX idx_owner_state (owner_id, state),
			INDEX idx_state_created (state, created_at),
			INDEX idx_created (created_at)
		) ENGINE=InnoDB CHARSET=utf8mb4`,
		`CREATE TABLE IF NOT EXISTS export_schedule (
			id CHAR(36) PRIMARY KEY,
			owner_id BIGINT NOT NULL,
			owner_name VARCHAR(100) NOT NULL,
			template_id VARCHAR(50) NOT NULL,
			params_json JSON NOT NULL,
			cron_expr VARCHAR(64) NOT NULL,
			enabled TINYINT(1) NOT NULL DEFAULT 1,
			next_run_at DATETIME NOT NULL,
			last_run_at DATETIME DEFAULT NULL,
			last_job_id CHAR(36) DEFAULT NULL,
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			INDEX idx_next_run (enabled, next_run_at)
		) ENGINE=InnoDB CHARSET=utf8mb4`,
	}
	for _, s := range stmts {
		if _, err := db.ExecContext(ctx, s); err != nil {
			return fmt.Errorf("ensure schema: %w", err)
		}
	}
	return nil
}
