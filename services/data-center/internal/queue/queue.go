// Package queue 简单的 FIFO 内存队列 + 状态发布
// 服务重启时用 store.ListActive 恢复 queued/running 任务重入队。
package queue

import (
	"errors"
	"sync"
)

var ErrQueueFull = errors.New("queue full")
var ErrClosed = errors.New("queue closed")

// Queue FIFO 无优先级
type Queue struct {
	mu     sync.Mutex
	items  []string
	c      *sync.Cond
	maxCap int
	closed bool
}

func New(maxCap int) *Queue {
	q := &Queue{items: make([]string, 0, maxCap), maxCap: maxCap}
	q.c = sync.NewCond(&q.mu)
	return q
}

// Push 入队, 返回入队后位次 (1-based)
func (q *Queue) Push(id string) (int, error) {
	q.mu.Lock()
	defer q.mu.Unlock()
	if q.closed {
		return 0, ErrClosed
	}
	if len(q.items) >= q.maxCap {
		return 0, ErrQueueFull
	}
	q.items = append(q.items, id)
	pos := len(q.items)
	q.c.Signal()
	return pos, nil
}

// Pop 阻塞取一条; 队列关闭返回 "", ErrClosed
func (q *Queue) Pop() (string, error) {
	q.mu.Lock()
	defer q.mu.Unlock()
	for len(q.items) == 0 && !q.closed {
		q.c.Wait()
	}
	if q.closed && len(q.items) == 0 {
		return "", ErrClosed
	}
	id := q.items[0]
	q.items = q.items[1:]
	return id, nil
}

// Len 当前队列长度
func (q *Queue) Len() int {
	q.mu.Lock()
	defer q.mu.Unlock()
	return len(q.items)
}

// Position 某 job 当前排队位次 (1-based), 不在队列返回 0
func (q *Queue) Position(id string) int {
	q.mu.Lock()
	defer q.mu.Unlock()
	for i, s := range q.items {
		if s == id {
			return i + 1
		}
	}
	return 0
}

// Remove 用户主动取消: 从队列中删掉
func (q *Queue) Remove(id string) bool {
	q.mu.Lock()
	defer q.mu.Unlock()
	for i, s := range q.items {
		if s == id {
			q.items = append(q.items[:i], q.items[i+1:]...)
			return true
		}
	}
	return false
}

// Close 关队列 (worker 会读到 ErrClosed 并退出)
func (q *Queue) Close() {
	q.mu.Lock()
	defer q.mu.Unlock()
	q.closed = true
	q.c.Broadcast()
}
