package config

import (
	"fmt"
	"os"
	"time"

	"gopkg.in/yaml.v3"
)

// Config is the root configuration for nexa-cdc-mysql.
type Config struct {
	HTTP   HTTPConfig   `yaml:"http"`
	Source SourceConfig `yaml:"source"`
	Sink   SinkConfig   `yaml:"sink"`
	Store  StoreConfig  `yaml:"store"`
	// IncludeTables are schema.table patterns, e.g. "ordersys.t_order_pf".
	// Empty means deny-all (safe default).
	IncludeTables []string `yaml:"include_tables"`
	// ServerID must be unique among MySQL replicas on the source.
	ServerID uint32 `yaml:"server_id"`
	// Flavor: "mysql" or "mariadb".
	Flavor string `yaml:"flavor"`
}

type HTTPConfig struct {
	Addr string `yaml:"addr"`
}

type SourceConfig struct {
	Host     string `yaml:"host"`
	Port     int    `yaml:"port"`
	User     string `yaml:"user"`
	Password string `yaml:"password"`
	// Charset for connection metadata.
	Charset string `yaml:"charset"`
}

type SinkConfig struct {
	DSN string `yaml:"dsn"` // warehouse write DSN
	// BatchSize is the max rows per flush.
	BatchSize int `yaml:"batch_size"`
	// FlushInterval is the max time between flushes.
	FlushInterval time.Duration `yaml:"flush_interval"`
}

type StoreConfig struct {
	// Driver: "memory" | "mysql"
	Driver string `yaml:"driver"`
	// DSN for position table when driver=mysql (can reuse sink DSN).
	DSN string `yaml:"dsn"`
	// Channel is a logical stream name for multi-source.
	Channel string `yaml:"channel"`
}

// Load reads YAML from path.
func Load(path string) (*Config, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read config: %w", err)
	}
	var c Config
	if err := yaml.Unmarshal(raw, &c); err != nil {
		return nil, fmt.Errorf("parse config: %w", err)
	}
	c.applyDefaults()
	if err := c.validate(); err != nil {
		return nil, err
	}
	return &c, nil
}

func (c *Config) applyDefaults() {
	if c.HTTP.Addr == "" {
		c.HTTP.Addr = ":48093"
	}
	if c.Source.Port == 0 {
		c.Source.Port = 3306
	}
	if c.Source.Charset == "" {
		c.Source.Charset = "utf8mb4"
	}
	if c.ServerID == 0 {
		c.ServerID = 19001
	}
	if c.Flavor == "" {
		c.Flavor = "mysql"
	}
	if c.Sink.BatchSize == 0 {
		c.Sink.BatchSize = 200
	}
	if c.Sink.FlushInterval == 0 {
		c.Sink.FlushInterval = 2 * time.Second
	}
	if c.Store.Driver == "" {
		c.Store.Driver = "memory"
	}
	if c.Store.Channel == "" {
		c.Store.Channel = "default"
	}
}

func (c *Config) validate() error {
	if c.Source.Host == "" {
		return fmt.Errorf("source.host is required")
	}
	if c.Source.User == "" {
		return fmt.Errorf("source.user is required")
	}
	if len(c.IncludeTables) == 0 {
		return fmt.Errorf("include_tables must not be empty (safe default deny-all)")
	}
	return nil
}

// SourceAddr returns host:port.
func (c *Config) SourceAddr() string {
	return fmt.Sprintf("%s:%d", c.Source.Host, c.Source.Port)
}
