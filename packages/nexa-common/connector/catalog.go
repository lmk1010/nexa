package connector

// Kind classifies external connectors into nexa.
type Kind string

const (
	KindDingTalkImport Kind = "dingtalk_import" // optional directory import
	KindBusinessAPI    Kind = "business_api"    // HTTP business systems
	KindCDC            Kind = "cdc_mysql"       // binlog warehouse feed
	KindWarehouseRO    Kind = "warehouse_ro"    // analytics readonly
)

// Descriptor is the product-facing connector metadata.
type Descriptor struct {
	ID          string         `json:"id"`
	Kind        Kind           `json:"kind"`
	Name        string         `json:"name"`
	Description string         `json:"description"`
	Enabled     bool           `json:"enabled"`
	Config      map[string]any `json:"config,omitempty"`
	Status      string         `json:"status,omitempty"` // ready|error|disabled
}

// Catalog is the built-in connector list for a tenant product.
func Catalog() []Descriptor {
	return []Descriptor{
		{
			ID:          "dingtalk-import",
			Kind:        KindDingTalkImport,
			Name:        "钉钉通讯录导入",
			Description: "可选：从已有钉钉企业导入部门/成员。不是 nexa 主数据权威。",
			Enabled:     true,
			Status:      "ready",
		},
		{
			ID:          "cdc-mysql",
			Kind:        KindCDC,
			Name:        "MySQL CDC",
			Description: "从业务库 binlog 同步到 warehouse，供分析/导出。",
			Enabled:     true,
			Status:      "ready",
		},
		{
			ID:          "data-center",
			Kind:        KindWarehouseRO,
			Name:        "数据中心导出",
			Description: "模板化明细导出与经营取数。",
			Enabled:     true,
			Status:      "ready",
		},
		{
			ID:          "business-http",
			Kind:        KindBusinessAPI,
			Name:        "HTTP 业务连接器",
			Description: "通用 HTTP API 接入外部业务系统。",
			Enabled:     true,
			Status:      "ready",
		},
	}
}
