// 上下文自动压缩 —— 消息总长度超过阈值就把老历史 summarize 成一段
// 目的：长对话（例如老板一次追问 20 轮）不会因 context 撑爆导致 LLM 拒绝/漂移
//
// 策略：
//   1. 估算 total chars（中文 1 char ≈ 1 token，英文 4 char ≈ 1 token，粗略用 char 数 × 1.2 当 token 数上限估计）
//   2. 达到 70% × maxContextChars → 触发压缩
//   3. 保留最近 KEEP_RECENT 轮原样；老的 summarize 成一段 assistant/system 消息
//   4. Summary 通过 OpenAI-compat provider 的 /chat/completions 端点直接调用，短 prompt 300 字左右完成
import { config } from './config.js';

// 阈值（char）。deepseek-v4-flash 上下文约 128k tokens，我们保守 40k chars 作 70% 触发点
const MAX_CONTEXT_CHARS = Number.parseInt(
  process.env.AGENT_CONTEXT_MAX_CHARS || '40000',
  10,
);
const COMPRESS_TRIGGER = Math.floor(MAX_CONTEXT_CHARS * 0.7);
const KEEP_RECENT = Number.parseInt(
  process.env.AGENT_CONTEXT_KEEP_RECENT || '6',
  10,
);

function totalChars(messages) {
  let sum = 0;
  for (const m of messages) {
    if (typeof m?.content === 'string') sum += m.content.length;
    else if (Array.isArray(m?.content)) {
      for (const p of m.content) {
        if (typeof p?.text === 'string') sum += p.text.length;
      }
    }
  }
  return sum;
}

// 把老消息拼成一段用于 summarize 的 prompt
function joinForSummary(messages) {
  return messages
    .map((m) => {
      const role = m.role || 'user';
      const c = typeof m.content === 'string'
        ? m.content
        : JSON.stringify(m.content ?? '');
      return `【${role}】${c}`;
    })
    .join('\n\n');
}

async function summarize(text, signal) {
  const cfg = config.agent;
  if (!cfg?.baseURL || !cfg?.apiKey) return null;
  const prompt = [
    '把下面这段"KYX 企业助手" 与老板的对话历史压缩成一段简明中文摘要（350 字以内）。',
    '要求：',
    '1) 保留关键结论、数字、决策、待办',
    '2) 舍弃寒暄、重复、无结论的探索',
    '3) 保留时间线（"7月初问了赔付、7月中问了工单排行" 这样）',
    '4) 用第三人称摘要口吻，不模仿助手回答，不加 markdown',
    '',
    '=== 对话历史 ===',
    text,
    '=== 结束 ===',
  ].join('\n');

  try {
    const url = `${cfg.baseURL.replace(/\/+$/, '')}/chat/completions`;
    const controller = new AbortController();
    // 30s 硬超时；signal 也一起挂上
    const t = setTimeout(() => controller.abort(), 30_000);
    const onAbort = () => controller.abort();
    if (signal) signal.addEventListener?.('abort', onAbort, { once: true });
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${cfg.apiKey}`,
      },
      body: JSON.stringify({
        model: cfg.model,
        messages: [{ role: 'user', content: prompt }],
        max_tokens: 900,
        temperature: 0.2,
        stream: false,
      }),
      signal: controller.signal,
    });
    clearTimeout(t);
    if (!res.ok) return null;
    const j = await res.json();
    const summary = j?.choices?.[0]?.message?.content;
    return typeof summary === 'string' && summary.trim() ? summary.trim() : null;
  } catch {
    return null;
  }
}

// 主入口：拿到 messages 数组，如果超阈值就压缩老部分，否则原样返回
// 返回 { messages, compressed: bool, originalChars, compressedChars }
export async function compactMessages(messages, signal) {
  if (!Array.isArray(messages) || messages.length === 0) {
    return { messages: messages ?? [], compressed: false };
  }
  const original = totalChars(messages);
  if (original < COMPRESS_TRIGGER) {
    return { messages, compressed: false, originalChars: original };
  }
  if (messages.length <= KEEP_RECENT + 2) {
    // 只有 8 条以下，硬砍老的没意义
    return { messages, compressed: false, originalChars: original };
  }
  const cut = messages.length - KEEP_RECENT;
  const older = messages.slice(0, cut);
  const recent = messages.slice(cut);
  const summary = await summarize(joinForSummary(older), signal);
  if (!summary) {
    // summarize 失败：退化为直接砍掉最老的一半（安全兜底，防超限崩溃）
    const halfDrop = Math.floor(cut / 2);
    const trimmed = messages.slice(halfDrop);
    return {
      messages: trimmed,
      compressed: true,
      compressionMethod: 'truncate',
      originalChars: original,
      compressedChars: totalChars(trimmed),
    };
  }
  const summaryMessage = {
    role: 'assistant',
    content:
      `【已压缩的历史摘要 / 共 ${older.length} 轮】\n${summary}\n\n` +
      `（以下是最近 ${recent.length} 轮原始对话）`,
  };
  const compacted = [summaryMessage, ...recent];
  return {
    messages: compacted,
    compressed: true,
    compressionMethod: 'summary',
    originalChars: original,
    compressedChars: totalChars(compacted),
  };
}

export const _test = { totalChars, joinForSummary, MAX_CONTEXT_CHARS, COMPRESS_TRIGGER };
