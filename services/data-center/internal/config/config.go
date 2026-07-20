package config

import (
	"fmt"
	"os"
	"time"

	"gopkg.in/yaml.v3"
)

type Config struct {
	HTTP      HTTPConfig      `yaml:"http"`
	Warehouse WarehouseConfig `yaml:"warehouse"`
	Queue     QueueConfig     `yaml:"queue"`
	Storage   StorageConfig   `yaml:"storage"`
	Templates TemplatesConfig `yaml:"templates"`
	Quota     QuotaConfig     `yaml:"quota"`
	Limits    LimitsConfig    `yaml:"limits"`
	Log       LogConfig       `yaml:"log"`
}

type HTTPConfig struct {
	Addr string `yaml:"addr"`
}

type WarehouseConfig struct {
	Host            string        `yaml:"host"`
	Port            int           `yaml:"port"`
	User            string        `yaml:"user"`
	Password        string        `yaml:"password"`
	Database        string        `yaml:"database"`
	MaxOpenConns    int           `yaml:"max_open_conns"`
	MaxIdleConns    int           `yaml:"max_idle_conns"`
	ConnMaxLifetime time.Duration `yaml:"conn_max_lifetime"`
}

type QueueConfig struct {
	MaxSize        int `yaml:"max_size"`
	WorkerPoolSize int `yaml:"worker_pool_size"`
}

type StorageConfig struct {
	Dir      string `yaml:"dir"`
	TTLHours int    `yaml:"ttl_hours"`
}

type TemplatesConfig struct {
	Dir string `yaml:"dir"`
}

type QuotaConfig struct {
	PerUserActive int `yaml:"per_user_active"`
	PerUserDaily  int `yaml:"per_user_daily"`
}

type LimitsConfig struct {
	MaxRowsDefault int `yaml:"max_rows_default"`
}

type LogConfig struct {
	Level string `yaml:"level"`
}

// Load 读 yaml，允许被 env 覆盖某些字段（TODO）
func Load(path string) (*Config, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read config %s: %w", path, err)
	}
	var c Config
	if err := yaml.Unmarshal(b, &c); err != nil {
		return nil, fmt.Errorf("parse config: %w", err)
	}
	c.applyDefaults()
	return &c, nil
}

func (c *Config) applyDefaults() {
	if c.HTTP.Addr == "" {
		c.HTTP.Addr = ":48092"
	}
	if c.Warehouse.MaxOpenConns == 0 {
		c.Warehouse.MaxOpenConns = 8
	}
	if c.Warehouse.MaxIdleConns == 0 {
		c.Warehouse.MaxIdleConns = 2
	}
	if c.Warehouse.ConnMaxLifetime == 0 {
		c.Warehouse.ConnMaxLifetime = 5 * time.Minute
	}
	if c.Queue.MaxSize == 0 {
		c.Queue.MaxSize = 100
	}
	if c.Queue.WorkerPoolSize == 0 {
		c.Queue.WorkerPoolSize = 3
	}
	if c.Storage.Dir == "" {
		c.Storage.Dir = "/data/exports"
	}
	if c.Storage.TTLHours == 0 {
		c.Storage.TTLHours = 24
	}
	if c.Templates.Dir == "" {
		c.Templates.Dir = "/etc/kyx-data-center/templates"
	}
	if c.Quota.PerUserActive == 0 {
		c.Quota.PerUserActive = 1
	}
	if c.Quota.PerUserDaily == 0 {
		c.Quota.PerUserDaily = 20
	}
	if c.Limits.MaxRowsDefault == 0 {
		c.Limits.MaxRowsDefault = 200000
	}
	if c.Log.Level == "" {
		c.Log.Level = "info"
	}
}
