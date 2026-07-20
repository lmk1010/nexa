// Workspace —— 每个 conversation 一个私有目录，用来放数据文件和 python 中间产物
// 设计准则：
//   1. **落盘不入上下文**：api_call 大数据一律落这里，LLM 只拿到 preview + file path
//   2. **命名可预测**：conv_id 用 sha1 前 12 位当目录名，LLM 用 python 时 cwd 直接是这里
//   3. **磁盘上限**：单文件 100MB / 单会话 500MB，超了就拒（保护宿主机）
//   4. **空闲 30min 清理**：后台每 5min 扫，mtime > 30min 全删
//   5. **无横向可见**：目录 0700，别的 conv 看不见
//
// 宿主机 bind-mount：/opt/kyx/agent-workspace → 容器 /workspace
import { createHash } from 'node:crypto';
import { promises as fs, createReadStream } from 'node:fs';
import { join, resolve } from 'node:path';

const ROOT = process.env.AGENT_WORKSPACE_ROOT || '/workspace';
export const WORKSPACE_ROOT = ROOT;

// 上限
const MAX_FILE_BYTES = 100 * 1024 * 1024;        // 单文件 100MB
const MAX_SESSION_BYTES = 500 * 1024 * 1024;     // 单会话 500MB
const IDLE_TTL_MS = 30 * 60 * 1000;              // 30min 空闲清理
const GC_INTERVAL_MS = 5 * 60 * 1000;            // 每 5min 扫一次

// LLM 上下文里能塞多少 —— 超了自动 offload
export const CONTEXT_INLINE_MAX = 8 * 1024;      // 8KB

function convDir(convId) {
  const hash = createHash('sha1').update(String(convId || 'anon')).digest('hex').slice(0, 12);
  return join(ROOT, hash);
}

export async function ensureWorkspace(convId) {
  const dir = convDir(convId);
  await fs.mkdir(dir, { recursive: true, mode: 0o700 });
  // touch 一下用于 GC 判定
  const now = new Date();
  try { await fs.utimes(dir, now, now); } catch {}
  return dir;
}

async function sessionSize(dir) {
  let total = 0;
  try {
    const files = await fs.readdir(dir);
    for (const f of files) {
      try {
        const s = await fs.stat(join(dir, f));
        if (s.isFile()) total += s.size;
      } catch {}
    }
  } catch {}
  return total;
}

// 落盘 —— buffer 或 string，返回 {file, bytes}
export async function writeToWorkspace(convId, filename, content) {
  const dir = await ensureWorkspace(convId);
  const buf = Buffer.isBuffer(content) ? content : Buffer.from(String(content));
  if (buf.byteLength > MAX_FILE_BYTES) {
    throw new Error(`单文件超过 ${MAX_FILE_BYTES / 1024 / 1024}MB 上限（当前 ${(buf.byteLength/1024/1024).toFixed(1)}MB）`);
  }
  const used = await sessionSize(dir);
  if (used + buf.byteLength > MAX_SESSION_BYTES) {
    throw new Error(
      `会话累计已用 ${(used/1024/1024).toFixed(1)}MB，加上本次会超 ${MAX_SESSION_BYTES / 1024 / 1024}MB 上限。请缩小数据范围或删旧文件。`,
    );
  }
  // 只拦真正危险的：.. / 分隔符 / 隐藏；中文/UTF-8 都允许
  const raw = String(filename).slice(0, 200);
  if (raw.includes('..') || raw.includes('/') || raw.includes('\\') || raw.startsWith('.')) {
    throw new Error(`非法文件名: ${raw}`);
  }
  const full = resolve(dir, raw);
  if (!full.startsWith(dir + '/') && full !== dir) {
    throw new Error('非法文件名（试图越界）');
  }
  await fs.writeFile(full, buf, { mode: 0o600 });
  return { file: raw, absPath: full, bytes: buf.byteLength };
}

export async function listWorkspaceFiles(convId) {
  const dir = await ensureWorkspace(convId);
  try {
    const files = await fs.readdir(dir);
    const out = [];
    for (const f of files) {
      try {
        const s = await fs.stat(join(dir, f));
        if (s.isFile()) out.push({ name: f, bytes: s.size, mtime: s.mtime.toISOString() });
      } catch {}
    }
    return out;
  } catch { return []; }
}

// 读文件（给下游 upload 用，比如 export_excel 读 python 写出的 xlsx）
export function readWorkspaceStream(convId, filename) {
  const dir = convDir(convId);
  const raw = String(filename);
  if (raw.includes('..') || raw.includes('/') || raw.includes('\\')) {
    throw new Error('非法文件名');
  }
  const full = resolve(dir, raw);
  if (!full.startsWith(dir + '/') && full !== dir) {
    throw new Error('非法文件名');
  }
  return createReadStream(full);
}

// -------------- 自动 offload --------------
// 大数据自动落盘 —— api_call/paginate_all 返回时调用
export async function offloadIfLarge(convId, data, hint = 'api-data') {
  const json = JSON.stringify(data);
  if (json.length <= CONTEXT_INLINE_MAX) return null; // 小的直接给 LLM 上下文

  // preview：数组前 3 项；对象里的 list 字段前 3 项；否则 keys 前 5
  let preview;
  let rows = null;
  let sample_fields = null;
  if (Array.isArray(data)) {
    preview = data.slice(0, 3);
    rows = data.length;
    if (data[0] && typeof data[0] === 'object') sample_fields = Object.keys(data[0]).slice(0, 20);
  } else if (data && typeof data === 'object' && Array.isArray(data.list)) {
    preview = { ...data, list: data.list.slice(0, 3), _list_len: data.list.length };
    rows = data.list.length;
    if (data.list[0] && typeof data.list[0] === 'object') sample_fields = Object.keys(data.list[0]).slice(0, 20);
  } else if (data && typeof data === 'object') {
    preview = Object.fromEntries(Object.entries(data).slice(0, 5));
  } else {
    preview = data;
  }

  const ts = Date.now();
  const filename = `${hint.replace(/[^a-zA-Z0-9._-]/g, '_')}_${ts}.json`;
  const { file, bytes } = await writeToWorkspace(convId, filename, json);
  return {
    _offloaded: true,
    file,
    bytes,
    rows,
    sample_fields,
    preview,
    hint: `数据 ${bytes} 字节已落盘为 workspace 下的 "${file}"，你可以在 python_exec 里用 pandas.read_json("${file}") 读它处理。不要要求把完整数据打印出来——只让 python 计算/汇总/导出结果。`,
  };
}

// -------------- GC --------------
async function gcOnce() {
  try {
    const entries = await fs.readdir(ROOT, { withFileTypes: true });
    const now = Date.now();
    for (const e of entries) {
      if (!e.isDirectory()) continue;
      const p = join(ROOT, e.name);
      try {
        const s = await fs.stat(p);
        if (now - s.mtimeMs > IDLE_TTL_MS) {
          await fs.rm(p, { recursive: true, force: true });
          console.log(`[workspace-gc] rm ${p} (idle ${((now-s.mtimeMs)/60000).toFixed(0)}min)`);
        }
      } catch {}
    }
  } catch {}
}

let gcTimer = null;
export function startWorkspaceGC() {
  if (gcTimer) return;
  gcOnce().catch(() => {});
  gcTimer = setInterval(() => { gcOnce().catch(() => {}); }, GC_INTERVAL_MS);
  gcTimer.unref?.();
}

// 运维统计端点用
export async function workspaceStats() {
  const stats = { root: ROOT, sessions: 0, files: 0, bytes: 0, oldest: null };
  try {
    const entries = await fs.readdir(ROOT, { withFileTypes: true });
    let oldest = null;
    for (const e of entries) {
      if (!e.isDirectory()) continue;
      stats.sessions += 1;
      const p = join(ROOT, e.name);
      try {
        const files = await fs.readdir(p);
        for (const f of files) {
          try {
            const s = await fs.stat(join(p, f));
            if (s.isFile()) { stats.files += 1; stats.bytes += s.size; }
            if (!oldest || s.mtimeMs < oldest) oldest = s.mtimeMs;
          } catch {}
        }
      } catch {}
    }
    if (oldest) stats.oldest = new Date(oldest).toISOString();
  } catch {}
  return stats;
}
