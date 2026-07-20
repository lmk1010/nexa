package dingtalk

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"strings"
	"sync"
	"time"
)

const (
	oapiHost = "https://oapi.dingtalk.com"
	openHost = "https://api.dingtalk.com"
)

// Config from env or caller.
type Config struct {
	AppKey    string
	AppSecret string
	// AgentID optional
	AgentID string
}

func ConfigFromEnv() Config {
	return Config{
		AppKey:    firstEnv("NEXA_DINGTALK_APP_KEY", "DINGTALK_APP_KEY", "DING_APP_KEY"),
		AppSecret: firstEnv("NEXA_DINGTALK_APP_SECRET", "DINGTALK_APP_SECRET", "DING_APP_SECRET"),
		AgentID:   firstEnv("NEXA_DINGTALK_AGENT_ID", "DINGTALK_AGENT_ID"),
	}
}

func (c Config) Enabled() bool {
	return strings.TrimSpace(c.AppKey) != "" && strings.TrimSpace(c.AppSecret) != ""
}

func firstEnv(keys ...string) string {
	for _, k := range keys {
		if v := strings.TrimSpace(os.Getenv(k)); v != "" {
			return v
		}
	}
	return ""
}

// Client is a minimal DingTalk OpenAPI client (token + directory).
type Client struct {
	cfg    Config
	http   *http.Client
	mu     sync.Mutex
	token  string
	expiry time.Time
}

func NewClient(cfg Config) *Client {
	return &Client{
		cfg:  cfg,
		http: &http.Client{Timeout: 15 * time.Second},
	}
}

type Dept struct {
	DeptID   int64  `json:"dept_id"`
	Name     string `json:"name"`
	ParentID int64  `json:"parent_id"`
}

type User struct {
	UserID     string `json:"userid"`
	Name       string `json:"name"`
	Mobile     string `json:"mobile"`
	JobNumber  string `json:"job_number"`
	Title      string `json:"title"`
	DeptIDList []int64 `json:"dept_id_list"`
	Active     bool   `json:"active"`
}

func (c *Client) GetAccessToken() (string, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.token != "" && time.Now().Before(c.expiry.Add(-60*time.Second)) {
		return c.token, nil
	}
	u := fmt.Sprintf("%s/gettoken?appkey=%s&appsecret=%s", oapiHost, url.QueryEscape(c.cfg.AppKey), url.QueryEscape(c.cfg.AppSecret))
	resp, err := c.http.Get(u)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	var out struct {
		ErrCode     int    `json:"errcode"`
		ErrMsg      string `json:"errmsg"`
		AccessToken string `json:"access_token"`
		ExpiresIn   int    `json:"expires_in"`
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		return "", err
	}
	if out.ErrCode != 0 || out.AccessToken == "" {
		return "", fmt.Errorf("dingtalk gettoken: %d %s", out.ErrCode, out.ErrMsg)
	}
	c.token = out.AccessToken
	exp := out.ExpiresIn
	if exp <= 0 {
		exp = 7200
	}
	c.expiry = time.Now().Add(time.Duration(exp) * time.Second)
	return c.token, nil
}

func (c *Client) ListSubDepts(deptID int64) ([]Dept, error) {
	token, err := c.GetAccessToken()
	if err != nil {
		return nil, err
	}
	body := map[string]any{"dept_id": deptID}
	raw, err := c.postOAPI("/topapi/v2/department/listsub", token, body)
	if err != nil {
		return nil, err
	}
	var out struct {
		ErrCode int    `json:"errcode"`
		ErrMsg  string `json:"errmsg"`
		Result  []Dept `json:"result"`
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		return nil, err
	}
	if out.ErrCode != 0 {
		return nil, fmt.Errorf("listsub: %d %s", out.ErrCode, out.ErrMsg)
	}
	return out.Result, nil
}

// ListAllDepts BFS from root (1).
func (c *Client) ListAllDepts() ([]Dept, error) {
	seen := map[int64]bool{}
	var all []Dept
	queue := []int64{1}
	for len(queue) > 0 {
		id := queue[0]
		queue = queue[1:]
		if seen[id] {
			continue
		}
		seen[id] = true
		subs, err := c.ListSubDepts(id)
		if err != nil {
			// root may need special handling; return what we have with error
			if len(all) == 0 {
				return nil, err
			}
			break
		}
		for _, d := range subs {
			all = append(all, d)
			queue = append(queue, d.DeptID)
		}
	}
	// ensure root present
	all = append([]Dept{{DeptID: 1, Name: "Root", ParentID: 0}}, all...)
	return all, nil
}

func (c *Client) ListUserIDs(deptID int64) ([]string, error) {
	token, err := c.GetAccessToken()
	if err != nil {
		return nil, err
	}
	var ids []string
	cursor := int64(0)
	for {
		body := map[string]any{"dept_id": deptID, "cursor": cursor, "size": 100}
		raw, err := c.postOAPI("/topapi/user/listid", token, body)
		if err != nil {
			return nil, err
		}
		var out struct {
			ErrCode int    `json:"errcode"`
			ErrMsg  string `json:"errmsg"`
			Result  struct {
				UserIDList []string `json:"userid_list"`
				NextCursor int64    `json:"next_cursor"`
				HasMore    bool     `json:"has_more"`
			} `json:"result"`
		}
		if err := json.Unmarshal(raw, &out); err != nil {
			return nil, err
		}
		if out.ErrCode != 0 {
			return nil, fmt.Errorf("listid: %d %s", out.ErrCode, out.ErrMsg)
		}
		ids = append(ids, out.Result.UserIDList...)
		if !out.Result.HasMore {
			break
		}
		cursor = out.Result.NextCursor
	}
	return ids, nil
}

func (c *Client) GetUser(userID string) (*User, error) {
	token, err := c.GetAccessToken()
	if err != nil {
		return nil, err
	}
	body := map[string]any{"userid": userID, "language": "zh_CN"}
	raw, err := c.postOAPI("/topapi/v2/user/get", token, body)
	if err != nil {
		return nil, err
	}
	var out struct {
		ErrCode int    `json:"errcode"`
		ErrMsg  string `json:"errmsg"`
		Result  User   `json:"result"`
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		return nil, err
	}
	if out.ErrCode != 0 {
		return nil, fmt.Errorf("user get: %d %s", out.ErrCode, out.ErrMsg)
	}
	return &out.Result, nil
}

// FetchDirectory pulls depts + users (best-effort; may be slow on large orgs).
func (c *Client) FetchDirectory(maxUsers int) (depts []Dept, users []User, err error) {
	if maxUsers <= 0 {
		maxUsers = 500
	}
	depts, err = c.ListAllDepts()
	if err != nil {
		return nil, nil, err
	}
	seen := map[string]bool{}
	for _, d := range depts {
		if d.DeptID == 1 {
			continue
		}
		ids, e := c.ListUserIDs(d.DeptID)
		if e != nil {
			continue
		}
		for _, id := range ids {
			if seen[id] {
				continue
			}
			seen[id] = true
			u, e := c.GetUser(id)
			if e != nil {
				continue
			}
			users = append(users, *u)
			if len(users) >= maxUsers {
				return depts, users, nil
			}
		}
	}
	return depts, users, nil
}

func (c *Client) postOAPI(path, token string, body any) ([]byte, error) {
	u := oapiHost + path + "?access_token=" + url.QueryEscape(token)
	raw, _ := json.Marshal(body)
	resp, err := c.http.Post(u, "application/json", bytes.NewReader(raw))
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	return io.ReadAll(resp.Body)
}

// OpenAPIHost exported for future new API style calls.
func OpenAPIHost() string { return openHost }
