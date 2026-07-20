package worker

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"log"
	"path/filepath"
	"sync"

	"github.com/kyx/kyx-data-center/internal/queue"
	"github.com/kyx/kyx-data-center/internal/store"
	"github.com/kyx/kyx-data-center/internal/template"
	"github.com/kyx/kyx-data-center/internal/writer"
)

// Pool 一组 worker goroutine, 从 queue 拉任务执行
type Pool struct {
	q         *queue.Queue
	store     *store.Store
	warehouse *sql.DB
	registry  *template.Registry
	storage   string // 输出目录
	size      int

	wg     sync.WaitGroup
	stopCh chan struct{}
}

// New 创建 pool
func New(size int, q *queue.Queue, s *store.Store, wh *sql.DB, reg *template.Registry, storageDir string) *Pool {
	return &Pool{
		q: q, store: s, warehouse: wh, registry: reg,
		storage: storageDir, size: size,
		stopCh: make(chan struct{}),
	}
}

// Start 起 N goroutine
func (p *Pool) Start(ctx context.Context) {
	for i := 0; i < p.size; i++ {
		p.wg.Add(1)
		go p.loop(ctx, i)
	}
}

func (p *Pool) loop(ctx context.Context, id int) {
	defer p.wg.Done()
	log.Printf("[worker %d] started", id)
	for {
		select {
		case <-ctx.Done():
			log.Printf("[worker %d] ctx done, exit", id)
			return
		case <-p.stopCh:
			log.Printf("[worker %d] stopCh, exit", id)
			return
		default:
		}
		jobID, err := p.q.Pop()
		if err != nil {
			if errors.Is(err, queue.ErrClosed) {
				log.Printf("[worker %d] queue closed, exit", id)
				return
			}
			log.Printf("[worker %d] pop err: %v", id, err)
			continue
		}
		p.runJob(ctx, jobID, id)
	}
}

func (p *Pool) runJob(ctx context.Context, jobID string, workerID int) {
	log.Printf("[worker %d] running job %s", workerID, jobID)
	if err := p.store.MarkRunning(ctx, jobID); err != nil {
		log.Printf("[worker %d] mark running failed: %v", workerID, err)
		return
	}

	j, err := p.store.Get(ctx, jobID)
	if err != nil {
		p.fail(ctx, jobID, "get job failed: "+err.Error())
		return
	}
	tpl, ok := p.registry.Get(j.TemplateID)
	if !ok {
		p.fail(ctx, jobID, "template not found: "+j.TemplateID)
		return
	}
	params, err := j.UnmarshalParams()
	if err != nil {
		p.fail(ctx, jobID, "parse params: "+err.Error())
		return
	}
	sqlStr, args, err := tpl.BuildSQL(params, tpl.MaxRows)
	if err != nil {
		p.fail(ctx, jobID, "build sql: "+err.Error())
		return
	}
	rows, err := p.warehouse.QueryContext(ctx, sqlStr, args...)
	if err != nil {
		p.fail(ctx, jobID, "query warehouse: "+err.Error())
		return
	}
	defer rows.Close()

	path := filepath.Join(p.storage, jobID+".xlsx")
	stream := &writer.Stream{
		Template: tpl,
		Path:     path,
		OnProgress: func(n int) {
			_ = p.store.UpdateProgress(context.Background(), jobID, n)
		},
		Ctx: ctx,
	}
	written, size, err := stream.Write(rows)
	if err != nil {
		p.fail(ctx, jobID, "stream: "+err.Error())
		return
	}
	if err := p.store.MarkDone(context.Background(), jobID, path, size, written); err != nil {
		log.Printf("[worker %d] mark done failed: %v", workerID, err)
	}
	log.Printf("[worker %d] job %s done: %d rows, %d bytes", workerID, jobID, written, size)
}

func (p *Pool) fail(ctx context.Context, jobID string, msg string) {
	log.Printf("[worker] job %s FAILED: %s", jobID, msg)
	if err := p.store.MarkFailed(ctx, jobID, msg); err != nil {
		log.Printf("[worker] mark failed err: %v", err)
	}
}

// Stop 关队列, 等 in-flight 完成
func (p *Pool) Stop() {
	close(p.stopCh)
	p.q.Close()
	p.wg.Wait()
}

// RecoverPending 服务重启: 把 store 里 queued/running 的 job 重新入队 (幂等, worker 会 mark running 再跑)
func (p *Pool) RecoverPending(ctx context.Context) error {
	list, err := p.store.ListActive(ctx)
	if err != nil {
		return err
	}
	for _, j := range list {
		// running 状态的重启后视为需重跑, 位置回 pending → queued
		if _, err := p.q.Push(j.ID); err != nil {
			log.Printf("[recover] push %s: %v", j.ID, err)
			continue
		}
		_ = p.store.MarkQueued(ctx, j.ID, p.q.Position(j.ID))
	}
	if len(list) > 0 {
		log.Printf("[recover] restored %d pending/queued/running jobs", len(list))
	}
	return nil
}

// Info 快照 (供 /hall 大厅 API)
type Info struct {
	QueueLen  int `json:"queue_len"`
	WorkerCap int `json:"worker_cap"`
}

func (p *Pool) Info() Info {
	return Info{QueueLen: p.q.Len(), WorkerCap: p.size}
}

// GCFile 用于清理超期文件（简单版, main 里跑个 ticker 调用）
func GCFile(path string) error {
	return fmt.Errorf("not implemented (TODO)")
}
