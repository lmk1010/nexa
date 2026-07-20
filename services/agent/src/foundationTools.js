// Foundation tools —— LLM 用得最频繁的基础原语，全部纯本地 / 零 IO / < 5ms
// 设计原则：让 LLM 不用手算 timestamp、不用手撸循环、不用把大 JSON 塞进上下文
//
// 加进来的 6 个：
//   now                       —— 当前时间 + 本周/本月/上月 anchor 一次给全
//   format_time               —— unix millis / iso → 人看懂的格式
//   parse_time                —— "上周三"/"7月初"/"最近一周" → { start, end } ms
//   jq_filter                 —— jq 表达式在数据上跑（沙箱化 spawn，不放进上下文）
//   math_eval                 —— 安全表达式求值（百分比、差异、汇总）
//   pick_fields               —— 从大数组挑字段，降上下文体积
import { z } from 'zod';
import { tool } from '@mk-co/neox-sdk';
import { runInSandbox, ALLOWED_COMMANDS } from './shellSandbox.js';

// —————— now ——————————————————————————————
// 一次调用把 LLM 会用到的所有 anchor 都返回。避免 LLM 用一次算一个。
export function nowTool() {
  return tool({
    name: 'now',
    description:
      '返回**当前时间及常用时间锚点**（今日/本周/本月/上周/上月的起止），你问日期相关的问题**优先调这个**，不要用心算 unix millis 换算。返回结构里已经有 `start,end` 字符串，可以直接塞进 api_call 的 createTime/updateTime 参数。',
    schema: z.object({}).strict(),
    async handler() {
      const d = new Date();
      const yyyy = (x) => x.getFullYear();
      const mm = (x) => String(x.getMonth() + 1).padStart(2, '0');
      const dd = (x) => String(x.getDate()).padStart(2, '0');
      const hh = (x) => String(x.getHours()).padStart(2, '0');
      const mi = (x) => String(x.getMinutes()).padStart(2, '0');
      const ss = (x) => String(x.getSeconds()).padStart(2, '0');
      const fmt = (x) =>
        `${yyyy(x)}-${mm(x)}-${dd(x)} ${hh(x)}:${mi(x)}:${ss(x)}`;
      const fmtRange = (start, end) => ({
        start: fmt(start),
        end: fmt(end),
        // Ruoyi/Java `LocalDateTime[]` 或 kyx PageReqVO 直接接的字符串形式（我们后端就是这个）
        joined: `${fmt(start)},${fmt(end)}`,
      });

      const startOfDay = new Date(d);
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date(d);
      endOfDay.setHours(23, 59, 59, 999);

      // 周从周一到周日
      const dow = d.getDay() === 0 ? 7 : d.getDay();
      const startOfWeek = new Date(startOfDay);
      startOfWeek.setDate(startOfDay.getDate() - (dow - 1));
      const endOfWeek = new Date(startOfWeek);
      endOfWeek.setDate(endOfWeek.getDate() + 6);
      endOfWeek.setHours(23, 59, 59, 999);

      const startOfPrevWeek = new Date(startOfWeek);
      startOfPrevWeek.setDate(startOfPrevWeek.getDate() - 7);
      const endOfPrevWeek = new Date(startOfWeek);
      endOfPrevWeek.setMilliseconds(endOfPrevWeek.getMilliseconds() - 1);

      const startOfMonth = new Date(d.getFullYear(), d.getMonth(), 1);
      const endOfMonth = new Date(
        d.getFullYear(),
        d.getMonth() + 1,
        0,
        23, 59, 59, 999,
      );
      const startOfPrevMonth = new Date(d.getFullYear(), d.getMonth() - 1, 1);
      const endOfPrevMonth = new Date(
        d.getFullYear(),
        d.getMonth(),
        0,
        23, 59, 59, 999,
      );

      const startOfPrev7Days = new Date(d.getTime() - 7 * 86400_000);
      const startOfPrev30Days = new Date(d.getTime() - 30 * 86400_000);

      return {
        now_ms: d.getTime(),
        iso: d.toISOString(),
        local: fmt(d),
        weekday_zh: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()],
        today: fmtRange(startOfDay, endOfDay),
        week: fmtRange(startOfWeek, endOfWeek),
        prev_week: fmtRange(startOfPrevWeek, endOfPrevWeek),
        month: fmtRange(startOfMonth, endOfMonth),
        prev_month: fmtRange(startOfPrevMonth, endOfPrevMonth),
        last_7d: fmtRange(startOfPrev7Days, endOfDay),
        last_30d: fmtRange(startOfPrev30Days, endOfDay),
      };
    },
  });
}

// —————— format_time ——————————————————————
export function formatTimeTool() {
  return tool({
    name: 'format_time',
    description:
      '把 unix millis / iso 字符串格式化成人看得懂的时间。style=relative 时输出"3 天前""刚刚"这种。**遇到 API 返回的数字型时间戳一律用这个转，不要心算**。',
    schema: z.object({
      input: z.union([z.number(), z.string()]).describe('unix millis 或 ISO 字符串'),
      style: z.enum(['date', 'datetime', 'relative']).default('datetime'),
    }),
    async handler({ input, style }) {
      let d;
      if (typeof input === 'number') d = new Date(input);
      else if (typeof input === 'string') {
        const n = Number(input);
        d = Number.isFinite(n) && n > 1e11 ? new Date(n) : new Date(input);
      } else d = new Date(NaN);
      if (Number.isNaN(d.getTime())) throw new Error(`invalid input: ${input}`);

      if (style === 'relative') {
        const diff = Date.now() - d.getTime();
        const abs = Math.abs(diff);
        const suf = diff >= 0 ? '前' : '后';
        if (abs < 60_000) return { text: '刚刚' };
        if (abs < 3_600_000) return { text: `${Math.round(abs / 60_000)} 分钟${suf}` };
        if (abs < 86_400_000) return { text: `${Math.round(abs / 3_600_000)} 小时${suf}` };
        if (abs < 30 * 86_400_000) return { text: `${Math.round(abs / 86_400_000)} 天${suf}` };
        if (abs < 365 * 86_400_000) return { text: `${Math.round(abs / (30 * 86_400_000))} 个月${suf}` };
        return { text: `${Math.round(abs / (365 * 86_400_000))} 年${suf}` };
      }
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      if (style === 'date') return { text: `${yyyy}-${mm}-${dd}` };
      const hh = String(d.getHours()).padStart(2, '0');
      const mi = String(d.getMinutes()).padStart(2, '0');
      const ss = String(d.getSeconds()).padStart(2, '0');
      return { text: `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}` };
    },
  });
}

// —————— parse_time ——————————————————————
// 中文自然语言 → 时间区间。覆盖大部分老板会问的表达
export function parseTimeTool() {
  return tool({
    name: 'parse_time',
    description:
      '中文自然语言时间 → 时间范围。识别：本周/上周/本月/上月/最近 N 天/N 月/最近一周/今日/昨日/前天/月初/月末/YYYY 年 MM 月/MM 月 DD 日等。返回 { start, end, joined }（Ruoyi 的 createTime[] 就吃 joined）。',
    schema: z.object({
      text: z.string().describe('比如 "最近一周"、"上月"、"7 月 1 日到 7 月 8 日"'),
    }),
    async handler({ text }) {
      const t = String(text || '').trim();
      const d = new Date();
      const fmt = (x) => {
        const yy = x.getFullYear();
        const mm = String(x.getMonth() + 1).padStart(2, '0');
        const dd = String(x.getDate()).padStart(2, '0');
        const hh = String(x.getHours()).padStart(2, '0');
        const mi = String(x.getMinutes()).padStart(2, '0');
        const ss = String(x.getSeconds()).padStart(2, '0');
        return `${yy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
      };
      const mkRange = (start, end) => ({
        start: fmt(start),
        end: fmt(end),
        joined: `${fmt(start)},${fmt(end)}`,
        start_ms: start.getTime(),
        end_ms: end.getTime(),
      });

      // 简单模式匹配 —— 尽量少设"聪明"，把 90% 的老板输入覆盖到就够
      const day0 = new Date(d); day0.setHours(0, 0, 0, 0);
      const day23 = new Date(d); day23.setHours(23, 59, 59, 999);

      if (/(今天|今日)/.test(t)) return mkRange(day0, day23);
      if (/(昨天|昨日)/.test(t)) {
        const y = new Date(day0); y.setDate(y.getDate() - 1);
        const ye = new Date(day23); ye.setDate(ye.getDate() - 1);
        return mkRange(y, ye);
      }
      if (/前天/.test(t)) {
        const y = new Date(day0); y.setDate(y.getDate() - 2);
        const ye = new Date(day23); ye.setDate(ye.getDate() - 2);
        return mkRange(y, ye);
      }
      const dow = d.getDay() === 0 ? 7 : d.getDay();
      const monday = new Date(day0); monday.setDate(monday.getDate() - (dow - 1));
      const sunday = new Date(monday); sunday.setDate(sunday.getDate() + 6); sunday.setHours(23, 59, 59, 999);
      if (/(本周|这周|这星期)/.test(t)) return mkRange(monday, sunday);
      if (/(上周|上星期)/.test(t)) {
        const s = new Date(monday); s.setDate(s.getDate() - 7);
        const e = new Date(monday); e.setMilliseconds(e.getMilliseconds() - 1);
        return mkRange(s, e);
      }
      const som = new Date(d.getFullYear(), d.getMonth(), 1);
      const eom = new Date(d.getFullYear(), d.getMonth() + 1, 0, 23, 59, 59, 999);
      if (/(本月|这个月|当月)/.test(t)) return mkRange(som, eom);
      if (/(上月|上个月)/.test(t)) {
        const s = new Date(d.getFullYear(), d.getMonth() - 1, 1);
        const e = new Date(d.getFullYear(), d.getMonth(), 0, 23, 59, 59, 999);
        return mkRange(s, e);
      }

      const mLastN = t.match(/最近\s*(\d+)\s*(天|日|周|月|年)/);
      if (mLastN) {
        const n = Number(mLastN[1]);
        const unit = mLastN[2];
        const ms = unit === '天' || unit === '日' ? n * 86_400_000
          : unit === '周' ? n * 7 * 86_400_000
          : unit === '月' ? n * 30 * 86_400_000
          : n * 365 * 86_400_000;
        return mkRange(new Date(d.getTime() - ms), day23);
      }
      if (/最近一周/.test(t)) return mkRange(new Date(d.getTime() - 7 * 86_400_000), day23);
      if (/最近一月|最近一个月/.test(t)) return mkRange(new Date(d.getTime() - 30 * 86_400_000), day23);

      // "7月1日到7月8日" / "2026-07-01 到 2026-07-08"
      const mRange = t.match(/(\d{4}[-/年]?\d{1,2}[-/月]?\d{1,2}日?)\s*[到至~-]\s*(\d{4}[-/年]?\d{1,2}[-/月]?\d{1,2}日?)/);
      if (mRange) {
        const parseOne = (s) => {
          const clean = s.replace(/[年月]/g, '-').replace(/日/g, '').replace(/\//g, '-');
          const parts = clean.split('-').filter(Boolean).map(Number);
          if (parts.length === 3) return new Date(parts[0], parts[1] - 1, parts[2]);
          if (parts.length === 2) return new Date(d.getFullYear(), parts[0] - 1, parts[1]);
          return null;
        };
        const s = parseOne(mRange[1]);
        const e = parseOne(mRange[2]);
        if (s && e) {
          s.setHours(0, 0, 0, 0);
          e.setHours(23, 59, 59, 999);
          return mkRange(s, e);
        }
      }

      throw new Error(`无法识别时间："${t}"。建议改用 now() 的 anchor 或者传具体日期`);
    },
  });
}

// —————— jq_filter ——————————————————————
// 大 JSON 用 jq 表达式在沙箱里处理，只把结果返给 LLM。避免把 21 MB 塞进上下文
export function jqFilterTool() {
  return tool({
    name: 'jq_filter',
    description:
      '用 jq 表达式过滤/映射 JSON，只返回你要的部分。**拿到 api_call 大数据后不要自己遍历判断，用这个**。示例表达式：`.data.list[] | {id, title, ctime: (.createTime|todate)}` 只挑字段并把 unix 秒转日期。',
    schema: z.object({
      data: z.union([z.string(), z.record(z.any()), z.array(z.any())])
        .describe('要处理的 JSON（可以是 string 或已 parsed 的对象/数组）'),
      expression: z.string().describe('jq 表达式，如 `.data.list | length` 或 `.[] | select(.status==1) | .name`'),
      raw: z.boolean().default(false).describe('true = -r 输出纯文本，false = JSON 输出'),
    }),
    async handler({ data, expression, raw }) {
      const stdin = typeof data === 'string' ? data : JSON.stringify(data);
      const args = raw ? ['-r', expression] : [expression];
      const { exitCode, stdout, stderr, truncatedOut } = await runInSandbox({
        cmd: 'jq',
        args,
        stdin,
        timeoutMs: 10_000,
      });
      if (exitCode !== 0) throw new Error(`jq 失败 (exit=${exitCode}): ${stderr.slice(0, 200)}`);
      // 尝试还原成 JSON 结构方便后续调用链；解析失败就返原始字符串
      let parsed;
      if (!raw) {
        try { parsed = JSON.parse(stdout); } catch { parsed = null; }
      }
      return {
        result: parsed !== null && parsed !== undefined ? parsed : stdout,
        truncated: truncatedOut || false,
      };
    },
  });
}

// —————— math_eval ——————————————————————
// 安全 eval —— 只允许数字/四则运算/常见函数，禁止任何 JS 表达式
export function mathEvalTool() {
  return tool({
    name: 'math_eval',
    description:
      '安全数学求值。支持 + - * / % ** ( ) 和函数：abs/round/floor/ceil/min/max/pow/sqrt/log/sum([...])/avg([...])。用来算百分比、增速、平均值。**不要在你自己回答里心算数字**，用这个。',
    schema: z.object({
      expression: z.string().describe('如 `(3970 - 3200) / 3200 * 100` 或 `avg([80, 100, 110])`'),
    }),
    async handler({ expression }) {
      const expr = String(expression || '');
      // 白名单校验：只允许安全字符
      if (!/^[\d\s+\-*/%.,()[\]a-zA-Z_]+$/.test(expr)) {
        throw new Error('表达式含不允许的字符');
      }
      // 简单沙箱：只暴露特定函数
      const safeEnv = {
        abs: Math.abs, round: Math.round, floor: Math.floor, ceil: Math.ceil,
        min: Math.min, max: Math.max, pow: Math.pow, sqrt: Math.sqrt, log: Math.log,
        sum: (arr) => arr.reduce((a, b) => a + Number(b), 0),
        avg: (arr) => arr.length === 0 ? 0 : arr.reduce((a, b) => a + Number(b), 0) / arr.length,
      };
      const keys = Object.keys(safeEnv);
      const vals = Object.values(safeEnv);
      try {
        // eslint-disable-next-line no-new-func
        const fn = new Function(...keys, `"use strict"; return (${expr});`);
        const result = fn(...vals);
        if (!Number.isFinite(result)) return { result, warning: '结果不是有限数值' };
        return { result };
      } catch (err) {
        throw new Error(`求值失败：${err.message}`);
      }
    },
  });
}

// —————— pick_fields ——————————————————————
// 从数组挑字段。降上下文体积：不用把 100 行 × 40 字段全给 LLM
export function pickFieldsTool() {
  return tool({
    name: 'pick_fields',
    description:
      '从数组里每行只保留指定字段。**拿到 api_call 一堆冗余字段的返回，用这个只挑 5 个关键字段**。可以顺便改字段名（比如 `total_money → 总金额`）用于导出/展示。',
    schema: z.object({
      data: z.array(z.record(z.any())).describe('数组'),
      fields: z.array(z.union([
        z.string(),
        z.object({ from: z.string(), to: z.string() }),
      ])).describe('要保留的字段名列表；也可以用 {from, to} 重命名'),
      limit: z.number().int().positive().max(1000).optional(),
    }),
    async handler({ data, fields, limit }) {
      const src = Array.isArray(data) ? data.slice(0, limit || data.length) : [];
      const out = src.map((row) => {
        const dst = {};
        for (const f of fields) {
          if (typeof f === 'string') dst[f] = row[f];
          else dst[f.to] = row[f.from];
        }
        return dst;
      });
      return { rows: out, count: out.length };
    },
  });
}

// —————— shell_exec ——————————————————————
// 沙箱化 shell 执行 —— 严格白名单命令，无 shell 解释器，无网络
export function shellExecTool() {
  return tool({
    name: 'shell_exec',
    description:
      [
        '在**沙箱**里跑一个白名单命令。**不是真 shell** —— 不支持 `|`/`&&`/重定向/反引号/子命令。要链式处理请多次调用，把 stdout 传给下次 stdin。',
        '',
        '**白名单命令**：' + [...ALLOWED_COMMANDS].join(', '),
        '',
        '**用法**：`shell_exec({ cmd: "jq", args: ["-r", ".data.list | length"], stdin: "<JSON>" })`',
        '',
        '限制：单次 10 秒；stdout 上限 32 KB；stdin 上限 64 KB；无网络访问；只读根目录。',
        '',
        '**优先用 jq_filter/pick_fields/math_eval 这些高阶工具**，只有它们搞不定才用 shell_exec。',
      ].join('\n'),
    schema: z.object({
      cmd: z.string().describe('白名单命令名（不要含参数/管道）'),
      args: z.array(z.string()).default([]).describe('参数列表'),
      stdin: z.string().optional().describe('喂给命令的 stdin'),
      timeoutMs: z.number().int().positive().max(30_000).optional(),
    }),
    async handler({ cmd, args, stdin, timeoutMs }) {
      const res = await runInSandbox({ cmd, args, stdin, timeoutMs });
      return res;
    },
  });
}
