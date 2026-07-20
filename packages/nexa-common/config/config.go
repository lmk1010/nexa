package config

import (
	"fmt"
	"os"

	"gopkg.in/yaml.v3"
)

// Service is shared bootstrap config for nexa microservices.
type Service struct {
	Name string `yaml:"name"`
	HTTP HTTP   `yaml:"http"`
	Log  Log    `yaml:"log"`
	MySQL MySQL `yaml:"mysql"`
}

type HTTP struct {
	Addr string `yaml:"addr"`
}

type Log struct {
	Level string `yaml:"level"`
}

type MySQL struct {
	DSN string `yaml:"dsn"`
}

func LoadService(path string, dest any) error {
	raw, err := os.ReadFile(path)
	if err != nil {
		return fmt.Errorf("read config: %w", err)
	}
	if err := yaml.Unmarshal(raw, dest); err != nil {
		return fmt.Errorf("parse config: %w", err)
	}
	return nil
}
