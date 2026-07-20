// python_exec —— LLM 的通用数据处理工具
//
// 设计：
//   - cwd 强制 = workspace dir，脚本只能操作这个会话的文件
//   - python3 装了 pandas/numpy/openpyxl/xlsxwriter/python-dateutil（Dockerfile 里装的）
//   - 30s 超时；stdout cap 32KB；stderr cap 8KB
//   - 无 rlimit/nobody 层（agent 容器本身已经 512MB/0.3CPU 限，容器边界就是沙箱）
//   - 输出 written_files 让 LLM 知道跑完写了什么，可以直接接 export_excel 上传
import { spawn } from 'node:child_process';
import { promises as fs } from 'node:fs';
import { z } from 'zod';
import { tool } from '@mk-co/neox-sdk';
import { ensureWorkspace, listWorkspaceFiles } from './workspace.js';

const DEFAULT_TIMEOUT_MS = 30_000;
const MAX_TIMEOUT_MS = 60_000;
const MAX_STDOUT = 32 * 1024;
const MAX_STDERR = 8 * 1024;
const MAX_CODE_LEN = 32 * 1024;

async function runPython({ code, cwd, timeoutMs }) {
  return new Promise((resolve, reject) => {
    let child;
    try {
      child = spawn('python3', ['-I', '-c', code], {
        cwd,
        stdio: ['pipe', 'pipe', 'pipe'],
        env: {
          PATH: '/usr/bin:/bin',
          LANG: 'C.UTF-8',
          LC_ALL: 'C.UTF-8',
          HOME: cwd,
          PYTHONDONTWRITEBYTECODE: '1',
          PYTHONUNBUFFERED: '1',
        },
        timeout: timeoutMs,
      });
    } catch (err) {
      return reject(new Error(`spawn python3 失败: ${err.message}`));
    }

    let outLen = 0, errLen = 0;
    const outChunks = [], errChunks = [];
    let truncatedOut = false, truncatedErr = false;
    let settled = false;

    const settle = (result) => { if (settled) return; settled = true; resolve(result); };

    child.stdout.on('data', (buf) => {
      if (outLen >= MAX_STDOUT) { truncatedOut = true; return; }
      const remain = MAX_STDOUT - outLen;
      if (buf.length > remain) {
        outChunks.push(buf.subarray(0, remain));
        outLen += remain;
        truncatedOut = true;
        try { child.stdout.destroy(); } catch {}
      } else { outChunks.push(buf); outLen += buf.length; }
    });
    child.stderr.on('data', (buf) => {
      if (errLen >= MAX_STDERR) { truncatedErr = true; return; }
      const remain = MAX_STDERR - errLen;
      if (buf.length > remain) {
        errChunks.push(buf.subarray(0, remain));
        errLen += remain;
        truncatedErr = true;
      } else { errChunks.push(buf); errLen += buf.length; }
    });
    child.on('error', (err) => settle({
      exitCode: -1,
      stdout: '', stderr: `spawn error: ${err.message}`,
      truncatedOut: false, truncatedErr: false, timedOut: false,
    }));
    child.on('close', (exitCode, signal) => {
      const timedOut = signal === 'SIGTERM' || signal === 'SIGKILL';
      settle({
        exitCode: exitCode ?? -1,
        stdout: Buffer.concat(outChunks).toString('utf8'),
        stderr: Buffer.concat(errChunks).toString('utf8'),
        truncatedOut, truncatedErr, timedOut,
      });
    });
    try { child.stdin.end(); } catch {}
  });
}

// 快照 workspace 里的文件时间戳，对比运行前后知道哪些是新写的
async function snapshotFiles(dir) {
  const list = await listWorkspaceFiles(dir).catch(() => []);
  const map = new Map();
  for (const f of list) map.set(f.name, f.mtime);
  return map;
}

async function diffFiles(dir, before) {
  const now = await listWorkspaceFiles(dir).catch(() => []);
  const added = [], modified = [];
  for (const f of now) {
    if (!before.has(f.name)) added.push({ name: f.name, bytes: f.bytes });
    else if (before.get(f.name) !== f.mtime) modified.push({ name: f.name, bytes: f.bytes });
  }
  return { added, modified };
}

// 自动注入的 preamble —— 常用库、常用函数直接可用，LLM 不用每次 import 一大堆
// 也把 workspace 文件列表打印出来，让 LLM 一眼看到有哪些数据
const PREAMBLE = `
import json, re, sys, os, math, csv, itertools, collections, datetime as _dt
from datetime import datetime, date, timedelta
from collections import Counter, defaultdict, OrderedDict
import pandas as pd
import numpy as np
from dateutil import parser as dtparse

# 便利工具
def load_json(path):
    """读 workspace 里的 JSON 文件，直接返 dict/list"""
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)

def to_df(path_or_data, key=None):
    """一步把 JSON 或列表变 DataFrame。key='list' 表示取 obj['list']"""
    if isinstance(path_or_data, str):
        d = load_json(path_or_data)
    else:
        d = path_or_data
    if key and isinstance(d, dict): d = d.get(key, [])
    return pd.DataFrame(d)

def ts_to_dt(x, unit='ms'):
    """unix 时间戳 → datetime；unit='ms' 或 's'"""
    return pd.to_datetime(x, unit=unit)

def write_xlsx(path, df, sheet='Sheet1', money_cols=None, int_cols=None):
    """一步写 xlsx，自动带表头样式 + 数字格式。适合几千行以内"""
    import xlsxwriter
    wb = xlsxwriter.Workbook(path, {'constant_memory': False})
    ws = wb.add_worksheet(sheet)
    hdr = wb.add_format({'bold': True, 'bg_color': '#F3F3F1', 'align': 'center'})
    money_fmt = wb.add_format({'num_format': '#,##0.00'})
    int_fmt = wb.add_format({'num_format': '#,##0'})
    cols = list(df.columns)
    for j, c in enumerate(cols): ws.write(0, j, c, hdr)
    for i, row in enumerate(df.itertuples(index=False, name=None), start=1):
        for j, v in enumerate(row):
            if v is None or (isinstance(v, float) and pd.isna(v)):
                ws.write(i, j, '')
            elif money_cols and cols[j] in money_cols: ws.write_number(i, j, float(v), money_fmt)
            elif int_cols and cols[j] in int_cols: ws.write_number(i, j, float(v), int_fmt)
            else: ws.write(i, j, v)
    for j, c in enumerate(cols):
        w = max(10, min(30, max(len(str(c)) + 2, 12)))
        ws.set_column(j, j, w)
    wb.close()
    return path

def write_xlsx_stream(path, rows_iter, columns, sheet='Sheet1'):
    """百万行流式写 —— 用 xlsxwriter constant_memory，不占内存"""
    import xlsxwriter
    wb = xlsxwriter.Workbook(path, {'constant_memory': True})
    ws = wb.add_worksheet(sheet)
    hdr = wb.add_format({'bold': True, 'bg_color': '#F3F3F1'})
    for j, c in enumerate(columns): ws.write(0, j, c, hdr)
    for i, row in enumerate(rows_iter, start=1):
        for j, c in enumerate(columns): ws.write(i, j, row.get(c) if isinstance(row, dict) else row[j])
    wb.close()
    return path

# 首行报当前 workspace 里有哪些文件 —— stderr 输出（不占 stdout 32KB 上限）
try:
    _files = sorted(os.listdir('.'))
    if _files:
        sys.stderr.write(f"[workspace-files] {', '.join(_files)}\\n")
except Exception: pass

# 执行用户代码（占位符会被替换）
_USER_CODE_HERE_
`;

export function pythonExecTool(context) {
  return tool({
    name: 'python_exec',
    description: [
      '**在 workspace 里跑 python3 脚本**。这是你的**主要数据处理工具** —— pivot/汇总/透视/格式化/写 Excel 全部走这里。',
      '',
      '## 环境（自动 import，不用再写）',
      '```py',
      'import pandas as pd, numpy as np, json, re, os, math',
      'from datetime import datetime, date, timedelta',
      'from collections import Counter, defaultdict',
      'from dateutil import parser as dtparse',
      '```',
      '',
      '## 已注入的便利函数',
      '- `load_json(path)` → dict/list',
      '- `to_df(path, key="list")` → 一步 JSON 文件 → DataFrame',
      '- `ts_to_dt(series, unit="ms")` → unix 时间戳转 datetime',
      '- `write_xlsx(path, df, money_cols=[...], int_cols=[...])` → 带样式写 xlsx（几千行）',
      '- `write_xlsx_stream(path, rows_iter, columns)` → **百万行不占内存**（constant_memory）',
      '',
      '## cwd = 会话 workspace',
      'api_call 返回的 `file` 字段就是这里的相对路径，直接读。stderr 首行会告诉你现有文件列表。',
      '',
      '## 典型模式（考勤 pivot）',
      '```py',
      'df = to_df("attendance_xxx.json", key="list")',
      'df["time"] = ts_to_dt(df["clockTime"]).dt.strftime("%H:%M")',
      'df["date"] = ts_to_dt(df["clockTime"]).dt.strftime("%Y-%m-%d")',
      'pivot = df.pivot_table(index=["profileId","name","date"], columns="type", values="time", aggfunc="first").reset_index()',
      'write_xlsx("out.xlsx", pivot)',
      'print(len(pivot), "rows")',
      '```',
      '然后调 `export_excel_from_file({file:"out.xlsx", filename:"7月考勤"})` 给用户。',
      '',
      '## 限制',
      '- 脚本 32KB / 单次 30s / stdout 32KB / stderr 8KB',
      '- **不要 `print(df)` 整表** —— 会撑爆输出。只 print 结果、行数、summary',
      '- 无网络（不能装新包 / 不能 requests）',
    ].join('\n'),
    schema: z.object({
      code: z.string().min(1).max(MAX_CODE_LEN).describe('python3 脚本；pandas/numpy/datetime 已自动 import，直接用即可'),
      timeout_ms: z.number().int().positive().max(MAX_TIMEOUT_MS).optional(),
    }).strict(),
    async handler({ code, timeout_ms }) {
      const convId = context?.conversationId || context?.requestId || 'anon';
      const cwd = await ensureWorkspace(convId);
      const before = await snapshotFiles(convId);
      const timeoutMs = Math.min(MAX_TIMEOUT_MS, timeout_ms || DEFAULT_TIMEOUT_MS);
      const wrapped = PREAMBLE.replace('_USER_CODE_HERE_', code);
      const res = await runPython({ code: wrapped, cwd, timeoutMs });
      const diff = await diffFiles(convId, before);
      // 从 stderr 里剪出 [workspace-files] 一行，单独放字段
      let workspaceBefore = null;
      const m = res.stderr.match(/^\[workspace-files\] ([^\n]*)/);
      if (m) workspaceBefore = m[1].split(',').map((s) => s.trim()).filter(Boolean);
      const cleanErr = res.stderr.replace(/^\[workspace-files\] [^\n]*\n/, '');
      return {
        exit_code: res.exitCode,
        stdout: res.stdout,
        stderr: cleanErr,
        stdout_truncated: res.truncatedOut,
        stderr_truncated: res.truncatedErr,
        timed_out: res.timedOut,
        files_written: diff.added,
        files_modified: diff.modified,
        workspace_files_before: workspaceBefore,
        hint: diff.added.length > 0
          ? `写了新文件 ${diff.added.map(f => f.name).join(', ')}；xlsx/csv 想给用户下载就 export_excel_from_file({file:"${diff.added[0].name}"})`
          : (res.exitCode !== 0 ? '出错了：看 stderr' : undefined),
      };
    },
  });
}
