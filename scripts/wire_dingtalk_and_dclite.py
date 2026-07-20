from pathlib import Path

# Fix dc-lite job id construction
p = Path("E:/code/nexa/services/data-center/cmd/nexa-dc-lite/main.go")
t = p.read_text(encoding="utf-8")
old = '''s.seq++
		now := time.Now().Format(time.RFC3339)
		j := job{
			ID:         "job_" + time.Now().Format("150405") ,
			TemplateID: tid,
			State:      "done",
			Filters:    body.Filters,
			CreatedAt:  now,
			UpdatedAt:  now,
			Message:    "lite mode simulated export (no warehouse)",
			Download:   "/jobs/" + "job_demo" + "/xlsx",
		}
		// fix id without trailing space issues
		j.ID = "job_" + time.Now().Format("150405")
		j.Download = "/jobs/" + j.ID + "/xlsx"
		s.jobs = append([]job{j}, s.jobs...)
		writeJSON(w, 200, map[string]any{"code": 0, "data": j})'''
new = '''s.seq++
		now := time.Now().Format(time.RFC3339)
		id := "job_" + time.Now().Format("150405")
		j := job{
			ID:         id,
			TemplateID: tid,
			State:      "done",
			Filters:    body.Filters,
			CreatedAt:  now,
			UpdatedAt:  now,
			Message:    "lite mode simulated export (no warehouse)",
			Download:   "/jobs/" + id + "/xlsx",
		}
		s.jobs = append([]job{j}, s.jobs...)
		writeJSON(w, 200, map[string]any{"code": 0, "data": j})'''
if old in t:
    t = t.replace(old, new)
    p.write_text(t, encoding="utf-8")
    print("dc-lite fixed")
else:
    # try looser fix
    t2 = t.replace('"job_" + time.Now().Format("150405") ,', '"job_" + time.Now().Format("150405"),')
    if t2 != t:
        p.write_text(t2, encoding="utf-8")
        print("dc-lite partial fix")
    else:
        print("dc-lite no change needed or pattern different")

# Patch HR to use real dingtalk client when env configured
hr = Path("E:/code/nexa/services/hr/cmd/nexa-hr/main.go")
t = hr.read_text(encoding="utf-8")
if "github.com/lmk1010/nexa/services/hr/internal/dingtalk" not in t:
    t = t.replace(
        '\t"time"\n)',
        '\t"time"\n\n\t"github.com/lmk1010/nexa/services/hr/internal/dingtalk"\n)',
    )
for imp in ['"bytes"', '"strings"']:
    if imp not in t:
        t = t.replace('\t"time"\n', f'\t"time"\n\t{imp}\n')

start = t.find("func (s *server) handleDingSync")
end = t.find("func (s *server) handleDingJobs")
if start < 0 or end < start:
    print("hr markers missing", start, end)
else:
    newfn = r'''func (s *server) handleDingSync(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"code": 405})
		return
	}
	var body struct {
		Mode string `json:"mode"`
	}
	_ = json.NewDecoder(r.Body).Decode(&body)
	if body.Mode == "" {
		body.Mode = "full"
	}
	jobID := "ding_" + time.Now().Format("20060102_150405")
	started := time.Now().Format(time.RFC3339)
	s.mu.Lock()
	job := syncJob{ID: jobID, Type: body.Mode, Status: "running", Message: "sync started", StartedAt: started}
	s.db.SyncJobs = append([]syncJob{job}, s.db.SyncJobs...)
	_ = s.save()
	s.mu.Unlock()

	cfg := dingtalk.ConfigFromEnv()
	modeLabel := "simulated"
	var remoteDepts []department
	var remoteEmps []employee
	upserted := 0

	if cfg.Enabled() {
		modeLabel = "openapi"
		cli := dingtalk.NewClient(cfg)
		depts, users, err := cli.FetchDirectory(500)
		if err != nil {
			s.mu.Lock()
			if len(s.db.SyncJobs) > 0 && s.db.SyncJobs[0].ID == jobID {
				s.db.SyncJobs[0].Status = "failed"
				s.db.SyncJobs[0].Message = "dingtalk openapi: " + err.Error()
				s.db.SyncJobs[0].FinishedAt = time.Now().Format(time.RFC3339)
			}
			_ = s.save()
			s.mu.Unlock()
			writeJSON(w, http.StatusBadGateway, map[string]any{"code": 502, "msg": err.Error(), "jobId": jobID})
			return
		}
		for _, d := range depts {
			remoteDepts = append(remoteDepts, department{ID: d.DeptID, Name: d.Name, ParentID: d.ParentID})
		}
		for i, u := range users {
			deptID := int64(0)
			deptName := ""
			if len(u.DeptIDList) > 0 {
				deptID = u.DeptIDList[0]
			}
			for _, d := range remoteDepts {
				if d.ID == deptID {
					deptName = d.Name
					break
				}
			}
			status := "active"
			if !u.Active {
				status = "inactive"
			}
			jobNo := u.JobNumber
			if jobNo == "" {
				jobNo = u.UserID
			}
			remoteEmps = append(remoteEmps, employee{
				ID: int64(2000 + i), Name: u.Name, Mobile: u.Mobile, DeptID: deptID, DeptName: deptName,
				JobNo: jobNo, Status: status, DingTalkID: u.UserID, UpdatedAt: time.Now().Format(time.RFC3339),
			})
		}
	} else {
		remoteDepts = []department{
			{ID: 1, Name: "HQ", ParentID: 0},
			{ID: 10, Name: "R&D", ParentID: 1},
			{ID: 20, Name: "Ops", ParentID: 1},
			{ID: 30, Name: "HR", ParentID: 1},
			{ID: 40, Name: "Finance", ParentID: 1},
		}
		remoteEmps = []employee{
			{ID: 1001, Name: "Zhang San", Mobile: "13800000001", DeptID: 10, DeptName: "R&D", JobNo: "KYX001", Status: "active", DingTalkID: "dt_zhangsan"},
			{ID: 1002, Name: "Li Si", Mobile: "13800000002", DeptID: 20, DeptName: "Ops", JobNo: "KYX002", Status: "active", DingTalkID: "dt_lisi"},
			{ID: 1003, Name: "Wang Wu", Mobile: "13800000003", DeptID: 30, DeptName: "HR", JobNo: "KYX003", Status: "active", DingTalkID: "dt_wangwu"},
			{ID: 1004, Name: "Zhao Liu", Mobile: "13800000004", DeptID: 40, DeptName: "Finance", JobNo: "KYX004", Status: "active", DingTalkID: "dt_zhaoliu"},
		}
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	if body.Mode == "directory" || body.Mode == "full" {
		s.db.Departments = remoteDepts
		for _, d := range remoteDepts {
			if d.ID > s.db.SeqDept {
				s.db.SeqDept = d.ID
			}
		}
	}
	if body.Mode == "roster" || body.Mode == "full" {
		byJob := map[string]int{}
		for i, e := range s.db.Employees {
			byJob[e.JobNo] = i
		}
		now := time.Now().Format(time.RFC3339)
		for _, re := range remoteEmps {
			re.UpdatedAt = now
			if idx, ok := byJob[re.JobNo]; ok {
				re.ID = s.db.Employees[idx].ID
				s.db.Employees[idx] = re
			} else {
				if re.ID == 0 {
					s.db.SeqEmp++
					re.ID = s.db.SeqEmp
				}
				s.db.Employees = append(s.db.Employees, re)
			}
			upserted++
			if re.ID > s.db.SeqEmp {
				s.db.SeqEmp = re.ID
			}
		}
	}
	finished := time.Now().Format(time.RFC3339)
	if len(s.db.SyncJobs) > 0 && s.db.SyncJobs[0].ID == jobID {
		s.db.SyncJobs[0].Status = "success"
		s.db.SyncJobs[0].Message = "sync completed (" + modeLabel + ")"
		s.db.SyncJobs[0].FinishedAt = finished
		s.db.SyncJobs[0].Stats = map[string]int{"departments": len(s.db.Departments), "employeesUpserted": upserted}
	}
	_ = s.save()
	go emitSense("hr.dingtalk.sync.finished", "hr", "info", "dingtalk sync finished", map[string]any{"jobId": jobID, "mode": body.Mode, "upserted": upserted, "provider": modeLabel})
	writeJSON(w, http.StatusOK, map[string]any{"code": 0, "data": s.db.SyncJobs[0]})
}

'''
    t = t[:start] + newfn + t[end:]
    if "func emitSense" not in t:
        helper = '''
func emitSense(senseType, source, severity, message string, payload map[string]any) {
	body, _ := json.Marshal(map[string]any{"type": senseType, "source": source, "severity": severity, "message": message, "payload": payload})
	bases := []string{}
	if v := os.Getenv("NEXA_AI_URL"); v != "" {
		bases = append(bases, strings.TrimRight(v, "/"))
	}
	bases = append(bases, "http://127.0.0.1:48089")
	client := &http.Client{Timeout: 1500 * time.Millisecond}
	for _, base := range bases {
		req, err := http.NewRequest(http.MethodPost, base+"/v1/ai/sense", bytes.NewReader(body))
		if err != nil {
			continue
		}
		req.Header.Set("Content-Type", "application/json")
		resp, err := client.Do(req)
		if err != nil {
			continue
		}
		resp.Body.Close()
		return
	}
}

'''
        t = t.replace("func writeJSON", helper + "func writeJSON")
    hr.write_text(t, encoding="utf-8")
    print("hr dingtalk hybrid written")

# start-dev include dc-lite
sp = Path("E:/code/nexa/scripts/start-dev.sh")
st = sp.read_text(encoding="utf-8")
if "nexa-dc-lite" not in st:
    st += '''

# data-center lite (stdlib export surface for local AI/agent)
echo "[build] data-center lite"
(cd "$ROOT/services/data-center" && go build -o "$BIN/nexa-dc-lite.exe" ./cmd/nexa-dc-lite)
mkdir -p "$ROOT/.run/data/data-center"
cat > "$ROOT/.run/configs/data-center.json" <<JSON
{"name":"nexa-data-center","http":{"addr":":48092"},"templatesDir":"$ROOT/services/data-center/templates","dataDir":"$ROOT/.run/data/data-center"}
JSON
if [[ ! -f "$PIDDIR/data-center.pid" ]] || ! kill -0 "$(cat "$PIDDIR/data-center.pid" 2>/dev/null)" 2>/dev/null; then
  nohup "$BIN/nexa-dc-lite.exe" -config "$ROOT/.run/configs/data-center.json" >"$LOGDIR/data-center.log" 2>&1 &
  echo $! > "$PIDDIR/data-center.pid"
  echo "[start] data-center lite"
fi
'''
    sp.write_text(st, encoding="utf-8")
    print("start-dev dc-lite appended")
else:
    print("start-dev already has dc-lite")
