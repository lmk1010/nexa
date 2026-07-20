package config

import (
	"fmt"
	"os"

	"gopkg.in/yaml.v3"
)

type Config struct {
	HTTP        HTTPConfig        `yaml:"http"`
	Log         LogConfig         `yaml:"log"`
	DataCenter  DataCenterConfig  `yaml:"data_center"`
	Warehouse   WarehouseConfig   `yaml:"warehouse"`
	DingTalk    DingTalkConfig    `yaml:"dingtalk"`
}

type HTTPConfig struct {
	Addr string `yaml:"addr"`
}

type LogConfig struct {
	Level string `yaml:"level"`
}

type DataCenterConfig struct {
	BaseURL string `yaml:"base_url"`
}

type WarehouseConfig struct {
	DSN string `yaml:"dsn"`
}

type DingTalkConfig struct {
	AppKey    string `yaml:"app_key"`
	AppSecret string `yaml:"app_secret"`
}

func Load(path string) (*Config, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read config: %w", err)
	}
	var c Config
	if err := yaml.Unmarshal(raw, &c); err != nil {
		return nil, fmt.Errorf("parse config: %w", err)
	}
	if c.HTTP.Addr == "" {
		c.HTTP.Addr = ":48091"
	}
	if c.Log.Level == "" {
		c.Log.Level = "info"
	}
	if v := os.Getenv("NEXA_AGENT_DINGTALK_APP_KEY"); v != "" {
		c.DingTalk.AppKey = v
	}
	if v := os.Getenv("NEXA_AGENT_DINGTALK_APP_SECRET"); v != "" {
		c.DingTalk.AppSecret = v
	}
	return &c, nil
}
