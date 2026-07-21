package main

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	bolt "go.etcd.io/bbolt"
)

var (
	bktMeta    = []byte("meta")
	bktTenants = []byte("tenants")
	bktUsers   = []byte("users")
	bktInvites = []byte("invites")
	bktTokens  = []byte("tokens")
	bktAudit   = []byte("audit")
)

func openBolt(path string) (*bolt.DB, error) {
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return nil, err
	}
	db, err := bolt.Open(path, 0o600, &bolt.Options{Timeout: 3 * time.Second})
	if err != nil {
		return nil, err
	}
	err = db.Update(func(tx *bolt.Tx) error {
		for _, name := range [][]byte{bktMeta, bktTenants, bktUsers, bktInvites, bktTokens, bktAudit} {
			if _, err := tx.CreateBucketIfNotExists(name); err != nil {
				return err
			}
		}
		return nil
	})
	if err != nil {
		_ = db.Close()
		return nil, err
	}
	return db, nil
}

func (s *server) loadFromBolt() error {
	if s.bolt == nil {
		return fmt.Errorf("bolt nil")
	}
	s.ensureMaps()
	return s.bolt.View(func(tx *bolt.Tx) error {
		// meta
		mb := tx.Bucket(bktMeta)
		if v := mb.Get([]byte("seq")); len(v) > 0 {
			fmt.Sscan(string(v), &s.db.Seq)
		}
		if v := mb.Get([]byte("tenantSeq")); len(v) > 0 {
			fmt.Sscan(string(v), &s.db.TenantSeq)
		}
		// tenants
		_ = tx.Bucket(bktTenants).ForEach(func(k, v []byte) error {
			var t tenant
			if err := json.Unmarshal(v, &t); err != nil {
				return err
			}
			s.db.Tenants[t.ID] = t
			return nil
		})
		_ = tx.Bucket(bktUsers).ForEach(func(k, v []byte) error {
			var u user
			if err := json.Unmarshal(v, &u); err != nil {
				return err
			}
			s.db.Users[u.Username] = u
			return nil
		})
		_ = tx.Bucket(bktInvites).ForEach(func(k, v []byte) error {
			var inv invite
			if err := json.Unmarshal(v, &inv); err != nil {
				return err
			}
			s.db.Invites[inv.Code] = inv
			return nil
		})
		_ = tx.Bucket(bktTokens).ForEach(func(k, v []byte) error {
			var rec tokenRecord
			if err := json.Unmarshal(v, &rec); err != nil {
				return err
			}
			s.db.Tokens[string(k)] = rec
			return nil
		})
		_ = tx.Bucket(bktAudit).ForEach(func(k, v []byte) error {
			var e auditEvent
			if err := json.Unmarshal(v, &e); err != nil {
				return err
			}
			s.db.Audit = append(s.db.Audit, e)
			return nil
		})
		return nil
	})
}

func (s *server) saveToBolt() error {
	if s.bolt == nil {
		return fmt.Errorf("bolt nil")
	}
	return s.bolt.Update(func(tx *bolt.Tx) error {
		// clear buckets
		for _, name := range [][]byte{bktTenants, bktUsers, bktInvites, bktTokens, bktAudit} {
			_ = tx.DeleteBucket(name)
			if _, err := tx.CreateBucket(name); err != nil {
				return err
			}
		}
		mb := tx.Bucket(bktMeta)
		if err := mb.Put([]byte("seq"), []byte(fmt.Sprintf("%d", s.db.Seq))); err != nil {
			return err
		}
		if err := mb.Put([]byte("tenantSeq"), []byte(fmt.Sprintf("%d", s.db.TenantSeq))); err != nil {
			return err
		}
		tb := tx.Bucket(bktTenants)
		for _, t := range s.db.Tenants {
			raw, _ := json.Marshal(t)
			if err := tb.Put([]byte(fmt.Sprintf("%d", t.ID)), raw); err != nil {
				return err
			}
		}
		ub := tx.Bucket(bktUsers)
		for _, u := range s.db.Users {
			raw, _ := json.Marshal(u)
			if err := ub.Put([]byte(u.Username), raw); err != nil {
				return err
			}
		}
		ib := tx.Bucket(bktInvites)
		for _, inv := range s.db.Invites {
			raw, _ := json.Marshal(inv)
			if err := ib.Put([]byte(inv.Code), raw); err != nil {
				return err
			}
		}
		tokb := tx.Bucket(bktTokens)
		for tok, rec := range s.db.Tokens {
			raw, _ := json.Marshal(rec)
			if err := tokb.Put([]byte(tok), raw); err != nil {
				return err
			}
		}
		ab := tx.Bucket(bktAudit)
		for _, e := range s.db.Audit {
			raw, _ := json.Marshal(e)
			key := []byte(fmt.Sprintf("%020d", e.ID))
			if err := ab.Put(key, raw); err != nil {
				return err
			}
		}
		return nil
	})
}

func resolveIAMBackend(cfg *config) string {
	if v := strings.TrimSpace(os.Getenv("NEXA_IAM_BACKEND")); v != "" {
		return strings.ToLower(v)
	}
	if v := strings.TrimSpace(os.Getenv("NEXA_DB_BACKEND")); v != "" {
		return strings.ToLower(v)
	}
	return "file" // file | bolt
}

func resolveIAMBoltPath(cfg config) string {
	if v := os.Getenv("NEXA_IAM_BOLT"); v != "" {
		return v
	}
	if v := os.Getenv("NEXA_BOLT_PATH"); v != "" {
		return v
	}
	return filepath.Join(cfg.DataDir, "iam.bolt")
}
