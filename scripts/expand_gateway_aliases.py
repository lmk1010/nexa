from pathlib import Path

# Expand gateway routes
p = Path("E:/code/nexa/services/gateway/cmd/nexa-gateway/main.go")
t = p.read_text(encoding="utf-8")
marker = '{Prefix: "/admin-api/bpm", Upstream: "bpm"},'
extra = """{Prefix: "/admin-api/bpm", Upstream: "bpm"},
		{Prefix: "/app-api/business", Upstream: "business"},
		{Prefix: "/admin-api/business", Upstream: "business"},
		{Prefix: "/app-api/erp", Upstream: "erp"},
		{Prefix: "/admin-api/erp", Upstream: "erp"},
		{Prefix: "/app-api/finance", Upstream: "finance"},
		{Prefix: "/admin-api/finance", Upstream: "finance"},
		{Prefix: "/app-api/ai", Upstream: "ai"},
		{Prefix: "/admin-api/ai", Upstream: "ai"},
		{Prefix: "/app-api/op", Upstream: "op"},
		{Prefix: "/admin-api/op", Upstream: "op"},
		{Prefix: "/app-api/im", Upstream: "im"},
		{Prefix: "/admin-api/im", Upstream: "im"},
		{Prefix: "/app-api/data-center", Upstream: "data-center", Strip: true},
		{Prefix: "/admin-api/data-center", Upstream: "data-center", Strip: true},"""
if "/app-api/business" not in t and marker in t:
    t = t.replace(marker, extra)
    p.write_text(t, encoding="utf-8")
    print("gateway routes expanded")
else:
    print("gateway routes skip")


def add_alias(file: str, src: str, dst: str) -> None:
    path = Path(file)
    text = path.read_text(encoding="utf-8")
    if f'mux.HandleFunc("{src}"' in text:
        print("exists", src)
        return
    lines = text.splitlines()
    out = []
    added = False
    for line in lines:
        out.append(line)
        if (not added) and f'mux.HandleFunc("{dst}"' in line:
            handler = line.split(",", 1)[1]
            out.append(f'\tmux.HandleFunc("{src}",{handler}')
            added = True
            print("alias", src)
    if added:
        path.write_text("\n".join(out) + "\n", encoding="utf-8")


add_alias("E:/code/nexa/services/iam/cmd/nexa-iam/main.go", "/app-api/system/user/profile/get", "/v1/iam/me")
add_alias("E:/code/nexa/services/iam/cmd/nexa-iam/main.go", "/admin-api/system/user/profile/get", "/v1/iam/me")
add_alias("E:/code/nexa/services/hr/cmd/nexa-hr/main.go", "/admin-api/hr/employees", "/v1/hr/employees")
add_alias("E:/code/nexa/services/hr/cmd/nexa-hr/main.go", "/app-api/hr/departments/tree", "/v1/hr/departments/tree")
add_alias("E:/code/nexa/services/hr/cmd/nexa-hr/main.go", "/admin-api/hr/departments/tree", "/v1/hr/departments/tree")
add_alias("E:/code/nexa/services/bpm/cmd/nexa-bpm/main.go", "/app-api/bpm/tasks/todo", "/v1/bpm/tasks/todo")
add_alias("E:/code/nexa/services/bpm/cmd/nexa-bpm/main.go", "/admin-api/bpm/tasks/todo", "/v1/bpm/tasks/todo")
add_alias("E:/code/nexa/services/bpm/cmd/nexa-bpm/main.go", "/app-api/bpm/tasks/approve", "/v1/bpm/tasks/approve")
add_alias("E:/code/nexa/services/bpm/cmd/nexa-bpm/main.go", "/admin-api/bpm/tasks/approve", "/v1/bpm/tasks/approve")

# start-dev expand data dirs
sp = Path("E:/code/nexa/scripts/start-dev.sh")
st = sp.read_text(encoding="utf-8")
st2 = st.replace(
    'for svc_port in "iam:48081" "hr:48083" "bpm:48082"; do\n  svc="${svc_port%%:*}"\n  port="${svc_port##*:}"\n  cat >',
    'for svc_port in "iam:48081" "bpm:48082" "hr:48083" "business:48084" "erp:48085" "finance:48086" "im:48087" "op:48088" "ai:48089"; do\n  svc="${svc_port%%:*}"\n  port="${svc_port##*:}"\n  mkdir -p "$ROOT/.run/data/$svc"\n  cat >',
)
if st2 != st:
    sp.write_text(st2, encoding="utf-8")
    print("start-dev expanded")
else:
    print("start-dev unchanged")
