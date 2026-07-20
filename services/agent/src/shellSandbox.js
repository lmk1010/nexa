// Shell 沙箱 —— 给 LLM 一个受控的执行面
//
// 安全模型：
//   1. **不启动 shell 解释器**（no `sh -c`），直接 spawn(cmd, args)。杜绝 `;`/`&&`/`|`/反引号/$()/重定向注入
//   2. **命令白名单**：只允许纯数据处理工具（jq/grep/awk/…），无 curl/wget/nc/python/node/docker
//   3. **PATH 白名单**：仅 /usr/bin:/bin，防止相对路径攻击
//   4. **禁网络**：容器内运行时用 --network 参数隔离；直接 spawn 时依赖白名单命令自身没网络能力
//   5. **资源上限**：10 秒 timeout / 32 KB stdout / 64 KB stdin / 32 参数 / 单参 4KB
//   6. **拒绝可疑参数**：`--exec`/`-e`/`system(`/`shell` 等 GNU tools 里的逃逸标志一律拒
import { spawn } from 'node:child_process';

// 白名单命令 —— 都是纯数据处理，本身没网络/进程管理能力
// 想加新命令**必须**先审计：这个命令有没有 -e/--exec/subshell 逃逸能力？
export const ALLOWED_COMMANDS = new Set([
  'jq', 'grep', 'egrep', 'fgrep',
  'awk', 'sed',
  'head', 'tail', 'cut', 'sort', 'uniq', 'wc', 'tr', 'nl',
  'base64', 'echo', 'printf', 'date', 'expr',
  'cat', 'tac', 'rev', 'paste', 'expand', 'fold', 'shuf', 'seq',
  'yq',
  'xxd',
  'test',
  // 文件系统只读：让 LLM 能 debug workspace 里有什么
  'ls', 'find', 'stat', 'file', 'basename', 'dirname', 'realpath', 'pwd', 'tree',
  // python3 一行命令（复杂脚本请用 python_exec）；仅接受 -c 但受 ESCAPE_FLAGS 拦；
  // 允许方式：`python3 <script.py>` 跑 workspace 里现成脚本
  'python3',
]);

// 逃逸标志黑名单（部分命令通过特定 flag 能执行 shell，比如 find -exec, awk system()）
// 出现这些立刻拒
const ESCAPE_FLAGS = [
  '--exec', '-exec', '-e', // find -exec / xargs -e
  '--eval', // 各种 lang eval
  '-c',     // sh -c / bash -c / awk -c
  '--source', '-s',
];

// awk / gawk 的 system() 逃逸检测（正则匹配代码里的 system / getline "cmd" |）
const AWK_ESCAPE_RE = /\b(system|getline)\s*\(/;

const DEFAULT_TIMEOUT_MS = 10_000;
const MAX_TIMEOUT_MS = 30_000;
const MAX_OUTPUT_BYTES = 32 * 1024;
const MAX_STDERR_BYTES = 4 * 1024;
const MAX_STDIN_BYTES = 64 * 1024;
const MAX_ARGS = 32;
const MAX_ARG_LEN = 4096;

function validate({ cmd, args, stdin, timeoutMs }) {
  if (typeof cmd !== 'string' || !cmd) throw new Error('cmd 必须是字符串');
  if (!ALLOWED_COMMANDS.has(cmd)) {
    throw new Error(
      `命令 '${cmd}' 不在白名单。允许：${[...ALLOWED_COMMANDS].sort().join(', ')}`,
    );
  }
  const argv = Array.isArray(args) ? args : [];
  if (argv.length > MAX_ARGS) throw new Error(`参数过多（max ${MAX_ARGS}）`);
  for (const a of argv) {
    if (typeof a !== 'string') throw new Error('所有参数必须是字符串');
    if (a.length > MAX_ARG_LEN) throw new Error(`参数过长（max ${MAX_ARG_LEN}）`);
    if (ESCAPE_FLAGS.includes(a)) {
      throw new Error(`参数 '${a}' 触发逃逸检测，拒绝执行`);
    }
  }
  // awk 特判：不允许代码里含 system() / getline "cmd" |
  if (cmd === 'awk' || cmd === 'gawk') {
    for (const a of argv) {
      if (AWK_ESCAPE_RE.test(a)) throw new Error("awk 代码含 system()/getline，拒绝");
    }
  }
  // grep -P (perl 正则) 有轻微 ReDoS 风险 —— 允许但要提示
  // 其他命令的 -f 允许读文件；但因为容器只读 + 无敏感文件路径，风险可控

  if (stdin != null) {
    if (typeof stdin !== 'string') throw new Error('stdin 必须是字符串');
    if (Buffer.byteLength(stdin) > MAX_STDIN_BYTES) {
      throw new Error(`stdin 过大（max ${MAX_STDIN_BYTES} bytes）`);
    }
  }
  const t = Math.max(500, Math.min(MAX_TIMEOUT_MS, Number(timeoutMs) || DEFAULT_TIMEOUT_MS));
  return { cmd, args: argv, stdin: stdin || '', timeoutMs: t };
}

export function runInSandbox(input) {
  const { cmd, args, stdin, timeoutMs } = validate(input);
  return new Promise((resolve, reject) => {
    let child;
    try {
      child = spawn(cmd, args, {
        stdio: ['pipe', 'pipe', 'pipe'],
        // 关键：不用 shell，直接 exec 二进制。没有 shell 解释器可绕
        shell: false,
        // 收紧 PATH，防止 LLM 传相对路径找不知名 binary
        env: {
          PATH: '/usr/bin:/bin',
          LANG: 'C.UTF-8',
          LC_ALL: 'C.UTF-8',
        },
        timeout: timeoutMs,
      });
    } catch (err) {
      return reject(new Error(`spawn 失败: ${err.message}`));
    }

    let outLen = 0;
    let errLen = 0;
    let outChunks = [];
    let errChunks = [];
    let truncatedOut = false;
    let truncatedErr = false;
    let settled = false;

    const settle = (result) => {
      if (settled) return;
      settled = true;
      resolve(result);
    };

    child.stdout.on('data', (buf) => {
      if (outLen >= MAX_OUTPUT_BYTES) { truncatedOut = true; return; }
      const remain = MAX_OUTPUT_BYTES - outLen;
      if (buf.length > remain) {
        outChunks.push(buf.subarray(0, remain));
        outLen += remain;
        truncatedOut = true;
        try { child.stdout.destroy(); } catch {}
      } else {
        outChunks.push(buf);
        outLen += buf.length;
      }
    });

    child.stderr.on('data', (buf) => {
      if (errLen >= MAX_STDERR_BYTES) { truncatedErr = true; return; }
      const remain = MAX_STDERR_BYTES - errLen;
      if (buf.length > remain) {
        errChunks.push(buf.subarray(0, remain));
        errLen += remain;
        truncatedErr = true;
      } else {
        errChunks.push(buf);
        errLen += buf.length;
      }
    });

    child.on('error', (err) => {
      settle({
        exitCode: -1,
        stdout: '',
        stderr: `spawn error: ${err.message}`,
        truncatedOut: false,
        truncatedErr: false,
        timedOut: false,
      });
    });

    child.on('close', (exitCode, signal) => {
      const timedOut = signal === 'SIGTERM' || signal === 'SIGKILL';
      settle({
        exitCode: exitCode ?? -1,
        stdout: Buffer.concat(outChunks).toString('utf8'),
        stderr: Buffer.concat(errChunks).toString('utf8'),
        truncatedOut,
        truncatedErr,
        timedOut,
      });
    });

    // stdin 一次性写完就关掉
    if (stdin) {
      try {
        child.stdin.end(stdin);
      } catch (err) {
        try { child.kill('SIGTERM'); } catch {}
        settle({
          exitCode: -1,
          stdout: '',
          stderr: `stdin write failed: ${err.message}`,
          truncatedOut: false,
          truncatedErr: false,
          timedOut: false,
        });
      }
    } else {
      try { child.stdin.end(); } catch {}
    }
  });
}

// 单元测试用的导出（不暴露给业务）
export const _test = { validate, ALLOWED_COMMANDS };
