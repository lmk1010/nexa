from pathlib import Path
import shutil

admin_html = r"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>Nexa Admin</title>
<style>
:root{--bg:#0d1117;--card:#161b22;--text:#e6edf3;--muted:#8b949e;--acc:#1f6feb;--ok:#3fb950;--line:#30363d}
*{box-sizing:border-box}body{margin:0;font-family:ui-sans-serif,system-ui,Segoe UI,Roboto,Helvetica,Arial;background:var(--bg);color:var(--text)}
header{padding:16px 20px;border-bottom:1px solid var(--line);display:flex;gap:12px;align-items:center;flex-wrap:wrap}
header img{width:32px;height:32px;border-radius:8px}
h1{font-size:16px;margin:0;font-weight:600}
main{display:grid;grid-template-columns:220px 1fr;min-height:calc(100vh - 58px)}
nav{border-right:1px solid var(--line);padding:12px}
nav button{width:100%;text-align:left;background:transparent;border:0;color:var(--muted);padding:10px 12px;border-radius:8px;cursor:pointer;margin-bottom:4px}
nav button.active,nav button:hover{background:var(--card);color:var(--text)}
section{padding:20px}
.card{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:16px;margin-bottom:12px}
label{display:block;font-size:12px;color:var(--muted);margin:8px 0 4px}
input{width:100%;padding:10px;border-radius:8px;border:1px solid var(--line);background:#0d1117;color:var(--text)}
button.primary{background:var(--acc);color:#fff;border:0;border-radius:8px;padding:10px 14px;cursor:pointer;margin-top:10px}
button.ghost{background:transparent;border:1px solid var(--line);color:var(--text);border-radius:8px;padding:8px 12px;cursor:pointer}
.row{display:flex;gap:8px;flex-wrap:wrap;align-items:center}
table{width:100%;border-collapse:collapse;font-size:13px}
th,td{border-bottom:1px solid var(--line);padding:8px;text-align:left}
.muted{color:var(--muted);font-size:12px}
.err{color:#f85149}
pre{white-space:pre-wrap;background:#0d1117;border:1px solid var(--line);padding:12px;border-radius:8px;overflow:auto}
@media(max-width:800px){main{grid-template-columns:1fr}}
</style>
</head>
<body>
<header>
  <img src="/admin/logo.svg" alt="nexa" onerror="this.style.display='none'"/>
  <h1>Nexa Admin</h1>
  <span class="muted" id="who">未登录</span>
  <div class="row" style="margin-left:auto">
    <input id="base" style="width:160px" placeholder="API base"/>
    <input id="user" style="width:110px" value="boss"/>
    <input id="pass" style="width:110px" type="password" value="boss123"/>
    <button class="primary" onclick="login()">登录</button>
    <button class="ghost" onclick="logout()">退出</button>
  </div>
</header>
<main>
  <nav id="nav">
    <button class="active" data-p="overview">总览</button>
    <button data-p="members">成员</button>
    <button data-p="approvals">审批</button>
    <button data-p="todos">待办</button>
    <button data-p="im">IM</button>
    <button data-p="connectors">连接器</button>
    <button data-p="onboarding">开通</button>
  </nav>
  <section id="view"></section>
</main>
<script>
const S={token:localStorage.getItem('nexa_token')||'', base:localStorage.getItem('nexa_base')||location.origin};
document.getElementById('base').value=S.base;
async function api(method,path,body){
  const headers={'Content-Type':'application/json'};
  if(S.token) headers.Authorization='Bearer '+S.token;
  const res=await fetch((S.base||'')+path,{method,headers,body:body?JSON.stringify(body):undefined});
  const data=await res.json().catch(()=>({}));
  if(!res.ok) throw new Error(data.msg||('HTTP '+res.status));
  return data;
}
function setWho(t){document.getElementById('who').textContent=t}
async function login(){
  S.base=document.getElementById('base').value.replace(/\/$/,'')||location.origin;
  localStorage.setItem('nexa_base',S.base);
  const data=await api('POST','/v1/iam/login',{username:document.getElementById('user').value,password:document.getElementById('pass').value});
  S.token=data.data.accessToken; localStorage.setItem('nexa_token',S.token);
  setWho((data.data.user.nickname||data.data.user.username)+' @ tenant '+data.data.user.tenantId);
  route('overview');
}
function logout(){S.token='';localStorage.removeItem('nexa_token');setWho('未登录');route('overview')}
async function route(name){
  document.querySelectorAll('nav button').forEach(b=>b.classList.toggle('active',b.dataset.p===name));
  const v=document.getElementById('view');
  try{
    if(name==='overview'){
      const plat=await api('GET','/v1/platform/services');
      let extra='';
      if(S.token){
        const me=await api('GET','/v1/iam/me');
        const onb=await api('GET','/v1/iam/onboarding/status');
        extra=`<div class="card"><h3>当前用户</h3><pre>${JSON.stringify(me.data,null,2)}</pre></div>
               <div class="card"><h3>开通进度</h3><pre>${JSON.stringify(onb.data,null,2)}</pre></div>`;
      }
      v.innerHTML=`<div class="card"><h2>总览</h2><p class="muted">${plat.note||''}</p><pre>${JSON.stringify(plat.processes||plat,null,2)}</pre></div>${extra||'<div class="card">请先登录</div>'}`;
    } else if(name==='members'){
      const data=await api('GET','/v1/iam/users');
      v.innerHTML=`<div class="card"><h2>成员 (${data.total})</h2>
        <table><tr><th>ID</th><th>用户</th><th>昵称</th><th>角色</th></tr>
        ${(data.data||[]).map(u=>`<tr><td>${u.id}</td><td>${u.username}</td><td>${u.nickname||''}</td><td>${(u.roles||[]).join(',')}</td></tr>`).join('')}
        </table>
        <button class="primary" onclick="invite()">生成邀请码</button><pre id="inv"></pre></div>`;
    } else if(name==='approvals'){
      const data=await api('GET','/v1/bpm/tasks/todo');
      v.innerHTML=`<div class="card"><h2>审批待办 (${data.total})</h2>
        <table><tr><th>ID</th><th>标题</th><th>状态</th><th></th></tr>
        ${(data.data||[]).map(t=>`<tr><td>${t.id}</td><td>${t.title}</td><td>${t.status}</td>
        <td><button class="ghost" onclick="approve('${t.id}','approve')">通过</button>
        <button class="ghost" onclick="approve('${t.id}','reject')">驳回</button></td></tr>`).join('')}
        </table>
        <input id="tTitle" placeholder="新审批标题"/><button class="primary" onclick="startTask()">发起</button></div>`;
    } else if(name==='todos'){
      const data=await api('GET','/v1/business/todos');
      v.innerHTML=`<div class="card"><h2>待办 (${data.total})</h2>
        <table><tr><th>ID</th><th>标题</th><th>状态</th><th></th></tr>
        ${(data.data||[]).map(t=>`<tr><td>${t.id}</td><td>${t.title||''}</td><td>${t.status}</td>
        <td><button class="ghost" onclick="doneTodo('${t.id}')">完成</button></td></tr>`).join('')}
        </table>
        <input id="todoTitle" placeholder="新待办"/><button class="primary" onclick="addTodo()">添加</button></div>`;
    } else if(name==='im'){
      const data=await api('GET','/v1/im/conversations');
      v.innerHTML=`<div class="card"><h2>会话 (${data.total})</h2>
        <table><tr><th>ID</th><th>标题</th><th>未读</th></tr>
        ${(data.data||[]).map(c=>`<tr><td>${c.id}</td><td>${c.title}</td><td>${c.unread}</td></tr>`).join('')}
        </table>
        <input id="convTitle" placeholder="会话名"/><button class="primary" onclick="newConv()">创建</button>
        <h3>发消息</h3><input id="msgCid" placeholder="conversationId"/><input id="msgText" placeholder="内容"/>
        <button class="primary" onclick="sendMsg()">发送</button><pre id="msgOut"></pre></div>`;
    } else if(name==='connectors'){
      const data=await api('GET','/v1/ai/connectors');
      v.innerHTML=`<div class="card"><h2>连接器</h2>
        <table><tr><th>ID</th><th>名称</th><th>启用</th><th></th></tr>
        ${(data.data||[]).map(c=>`<tr><td>${c.id}</td><td>${c.name}</td><td>${c.enabled}</td>
        <td><button class="ghost" onclick="toggleConn('${c.id}', ${!c.enabled})">${c.enabled?'禁用':'启用'}</button></td></tr>`).join('')}
        </table></div>`;
    } else if(name==='onboarding'){
      v.innerHTML=`<div class="card"><h2>注册新企业</h2>
        <label>企业名</label><input id="co"/>
        <label>管理员</label><input id="adm"/>
        <label>密码</label><input id="admp" type="password" value="pass123"/>
        <button class="primary" onclick="regTenant()">注册</button><pre id="regOut"></pre></div>`;
    }
  }catch(e){v.innerHTML=`<div class="card err">${e.message}</div>`}
}
async function invite(){const d=await api('POST','/v1/iam/invites',{role:'member'});document.getElementById('inv').textContent=JSON.stringify(d.data,null,2)}
async function approve(id,action){await api('POST','/v1/bpm/tasks/approve',{taskId:id,action,reason:'admin'});route('approvals')}
async function startTask(){await api('POST','/v1/bpm/tasks/start',{title:document.getElementById('tTitle').value||'New task'});route('approvals')}
async function addTodo(){await api('POST','/v1/business/todos',{title:document.getElementById('todoTitle').value||'todo'});route('todos')}
async function doneTodo(id){await api('POST','/v1/business/todos/complete',{id,status:'done'});route('todos')}
async function newConv(){await api('POST','/v1/im/conversations',{title:document.getElementById('convTitle').value||'Chat'});route('im')}
async function sendMsg(){
  const d=await api('POST','/v1/im/messages/send',{conversationId:document.getElementById('msgCid').value,text:document.getElementById('msgText').value});
  document.getElementById('msgOut').textContent=JSON.stringify(d.data,null,2); route('im');
}
async function toggleConn(id,enabled){await api('PUT','/v1/ai/connectors/config',{id,enabled,config:{}});route('connectors')}
async function regTenant(){
  const d=await api('POST','/v1/iam/tenants/register',{company:document.getElementById('co').value,adminUsername:document.getElementById('adm').value,password:document.getElementById('admp').value});
  document.getElementById('regOut').textContent=JSON.stringify(d.data,null,2);
}
document.getElementById('nav').onclick=e=>{if(e.target.dataset.p)route(e.target.dataset.p)};
(async()=>{
  if(S.token){try{const me=await api('GET','/v1/iam/me');setWho((me.data.nickname||me.data.username)+' @ tenant '+me.data.tenantId)}catch{S.token=''}}
  route('overview');
})();
</script>
</body>
</html>
"""

web = Path("E:/code/nexa/services/core/web")
web.mkdir(parents=True, exist_ok=True)
(web / "admin.html").write_text(admin_html, encoding="utf-8")
logo_src = Path("E:/code/nexa/assets/logo.svg")
if logo_src.exists():
    (web / "logo.svg").write_text(logo_src.read_text(encoding="utf-8"), encoding="utf-8")
print("admin html", len(admin_html))

# Wire static admin into core main.go
main = Path("E:/code/nexa/services/core/cmd/nexa-core/main.go")
t = main.read_text(encoding="utf-8")
if "/admin" not in t or "admin.html" not in t:
    # add embed import and FS
    if '"embed"' not in t:
        t = t.replace(
            '\t"encoding/json"\n',
            '\t"embed"\n\t"encoding/json"\n',
            1,
        )
    if "var adminFS embed.FS" not in t:
        t = t.replace(
            "var version = ",
            "//go:embed all:web\nvar adminFS embed.FS\n\nvar version = ",
            1,
        )
    # serve admin - need to fix embed path: embed is relative to package dir
    # move web under cmd/nexa-core/web OR use http.Dir
    # Prefer http.Dir relative to executable/working dir for simplicity
    serve = '''
	// Admin console (static)
	mux.HandleFunc("/admin", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/admin/", http.StatusFound)
	})
	mux.HandleFunc("/admin/", func(w http.ResponseWriter, r *http.Request) {
		// try embedded first, then local web/
		p := strings.TrimPrefix(r.URL.Path, "/admin/")
		if p == "" || p == "/" {
			p = "admin.html"
		}
		if p == "logo.svg" || p == "admin.html" {
			if b, err := adminFS.ReadFile("web/" + p); err == nil {
				if strings.HasSuffix(p, ".svg") {
					w.Header().Set("Content-Type", "image/svg+xml")
				} else {
					w.Header().Set("Content-Type", "text/html; charset=utf-8")
				}
				w.Write(b)
				return
			}
		}
		http.NotFound(w, r)
	})
'''
    # Actually embed path: files must be under cmd/nexa-core. Copy web there.
    if "adminFS.ReadFile" not in t:
        # insert before domain routes registerHR
        t = t.replace(
            "\t// Domain routes (in-process)\n",
            serve + "\n\t// Domain routes (in-process)\n",
        )
        # public admin
        if '"/admin"' not in t[t.find("publicExact") : t.find("publicExact") + 600]:
            t = t.replace(
                '"/healthz": true, "/v1/platform/services": true, "/": true,',
                '"/healthz": true, "/v1/platform/services": true, "/": true, "/admin": true, "/admin/": true,',
            )
        # also public prefix /admin
        if '"/admin"' not in t[t.find("publicPrefix") : t.find("publicPrefix") + 400]:
            t = t.replace(
                'publicPrefix := []string{"/agent"',
                'publicPrefix := []string{"/admin", "/agent"',
            )
    main.write_text(t, encoding="utf-8")
    print("main admin routes")
else:
    print("admin already in main")

# Copy web into cmd package for embed
cmd_web = Path("E:/code/nexa/services/core/cmd/nexa-core/web")
cmd_web.mkdir(parents=True, exist_ok=True)
(cmd_web / "admin.html").write_text((web / "admin.html").read_text(encoding="utf-8"), encoding="utf-8")
if (web / "logo.svg").exists():
    (cmd_web / "logo.svg").write_text((web / "logo.svg").read_text(encoding="utf-8"), encoding="utf-8")
print("cmd web copied")
