// export_excel 工具 —— LLM 生成表格 → OA infra/file 上传 → 返回下载链接
// 走 OA 现成的文件服务（`/admin-api/infra/file/upload` + `/download/{fileId}`），
// 而不是直连 minio。好处：
//   1. 用户下载走 nginx-ui port 80（对外已开），不用暴露 minio 9000
//   2. 复用 OA 的存储配置（local / minio / OSS 任何后端都行）
//   3. 下载 URL 是 `@PermitAll @TenantIgnore` 免鉴权，随便谁点开都能下
//   4. 生命周期由 OA 的文件表 kyx_infra_file 管理，方便清理
import ExcelJS from 'exceljs';
import { z } from 'zod';
import { promises as fs } from 'node:fs';
import { resolve } from 'node:path';
import { getServiceAccountAuthHeaders } from './serviceAccountAuth.js';
import { recordExport } from './fileHistoryStore.js';
import { ensureWorkspace } from './workspace.js';

const OA_BASE_URL = process.env.OA_API_BASE_URL || process.env.INTERNAL_API_BASE_URL || 'http://kyx-gateway:48080';
// 用户从公网访问的下载 URL 前缀。默认走对外 nginx（80 端口，无端口后缀）
const PUBLIC_DOWNLOAD_HOST = process.env.OA_PUBLIC_DOWNLOAD_HOST || 'http://43.139.24.244';

export async function generateExcel({ rows, columns, filename, sheetName, context, conversationId }) {
  const wb = new ExcelJS.Workbook();
  wb.creator = 'KYX Agent';
  wb.created = new Date();
  const sheet = wb.addWorksheet(sheetName || 'Sheet1');

  const cols = Array.isArray(columns) && columns.length > 0
    ? columns
    : Object.keys(rows[0] || {}).map((k) => ({ key: k, header: k }));

  sheet.columns = cols.map((c) => ({
    header: c.header || c.key,
    key: c.key,
    width: Math.max(12, String(c.header || c.key).length + 4),
  }));

  // 表头样式
  sheet.getRow(1).font = { bold: true, size: 11 };
  sheet.getRow(1).alignment = { vertical: 'middle', horizontal: 'center' };
  sheet.getRow(1).fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FFF3F3F1' },
  };

  for (const row of rows) {
    sheet.addRow(row);
  }

  // 数字列格式
  for (let i = 0; i < cols.length; i++) {
    const col = sheet.getColumn(i + 1);
    if (cols[i].numeric || cols[i].money) {
      col.numFmt = cols[i].money ? '#,##0.00' : '#,##0';
      col.alignment = { horizontal: 'right' };
    }
  }

  const buffer = await wb.xlsx.writeBuffer();

  const safeName = String(filename || 'export').replace(/[\/\\:*?"<>|]/g, '_');
  const fullName = safeName.endsWith('.xlsx') ? safeName : `${safeName}.xlsx`;

  // 上传到 OA infra/file/upload —— multipart/form-data
  const authHeaders = await getServiceAccountAuthHeaders({ tenantId: null });
  const form = new FormData();
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  form.append('file', blob, fullName);
  form.append('directory', 'agent-exports');

  const uploadUrl = `${OA_BASE_URL.replace(/\/+$/, '')}/admin-api/infra/file/upload`;
  const res = await fetch(uploadUrl, {
    method: 'POST',
    headers: {
      ...authHeaders,
      // 不设 Content-Type，让 fetch 自动加 boundary
    },
    body: form,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`OA file upload failed: ${res.status} ${text.slice(0, 200)}`);
  }
  const body = await res.json().catch(() => ({}));
  if (body?.code !== 0) {
    throw new Error(`OA file upload rejected: code=${body?.code} msg=${body?.msg}`);
  }
  const returned = body.data;
  // OA FileController.uploadFile 返回 fileService.createFile 的结果 = fileId (UUID)
  // 兼容返回完整 URL 的情况（不同 fileConfig 可能不同）
  let downloadUrl;
  if (typeof returned === 'string' && /^https?:\/\//.test(returned)) {
    // 已经是完整 URL —— 把内部 host 换成对外 host
    downloadUrl = returned.replace(/^https?:\/\/[^/]+/, PUBLIC_DOWNLOAD_HOST.replace(/\/+$/, ''));
  } else {
    const fileId = String(returned || '').trim();
    downloadUrl = `${PUBLIC_DOWNLOAD_HOST.replace(/\/+$/, '')}/admin-api/infra/file/download/${fileId}`;
  }

  // 落库到文件历史（异步，不阻塞返回）
  if (context) {
    recordExport({
      tenantId: context?.tenantId,
      userId: context?.loginUser?.id ?? context?.loginUser?.userId,
      filename: fullName,
      downloadUrl,
      rows: rows.length,
      bytes: buffer.byteLength,
      conversationId,
    }).catch(() => {});
  }

  // 关键：LLM 经常把 anchor 文字写成 "download" 而不是文件名 —— 直接给它写好的 markdown
  // 前端 file card 会优先看 anchor 里的 xxx.xlsx；URL 加个 filename hint 兜底
  const urlWithHint = downloadUrl.includes('?')
    ? `${downloadUrl}&filename=${encodeURIComponent(fullName)}`
    : `${downloadUrl}?filename=${encodeURIComponent(fullName)}`;
  const markdown = `[点击下载 ${fullName}](${urlWithHint})`;

  return {
    url: urlWithHint,
    filename: fullName,
    rows: rows.length,
    bytes: buffer.byteLength,
    // LLM **直接把 markdown 字段贴到回答里**，不要自己拼
    markdown,
    instruction: '把上面的 markdown 字段原样贴到回答里，不要改成"点击这里"或 "download" 这种通用词。',
  };
}

// 从 workspace 里读一个已经写好的文件（.xlsx / .csv / .pdf 等），上传到 OA infra/file，
// 返回可下载 URL。给 python_exec 用：先写 out.xlsx，再调这个上传。
// 好处：xlsxwriter constant_memory 模式能流式写 100 万行 xlsx，比 exceljs 内存 buffer 快 10 倍
export async function uploadWorkspaceFile({ file, filename, context, conversationId }) {
  const convId = conversationId || context?.conversationId || context?.requestId || 'anon';
  const dir = await ensureWorkspace(convId);
  // **不 sanitize 中文** —— python_exec 写的 "刘明康_考勤.xlsx" 我们要能读回来
  // 只拦真正危险的路径穿越：..、绝对路径、路径分隔符
  const raw = String(file || '').trim();
  if (!raw) throw new Error('file 必填');
  if (raw.includes('..') || raw.includes('/') || raw.includes('\\') || raw.startsWith('.')) {
    throw new Error(`非法文件名: ${raw}（不允许 .. / \\ 或以点开头）`);
  }
  const full = resolve(dir, raw);
  // resolve 后再检查一次没越界
  if (!full.startsWith(dir + '/') && full !== dir) throw new Error('非法路径');
  const stat = await fs.stat(full).catch(() => null);
  if (!stat) {
    // 提示当前 workspace 里都有啥
    const files = await fs.readdir(dir).catch(() => []);
    throw new Error(`workspace 里没找到 "${raw}"。当前 workspace 里有: ${files.join(', ') || '(空)'}`);
  }
  const buffer = await fs.readFile(full);

  const displayName = filename || raw;
  const fullName = displayName.endsWith('.xlsx') || displayName.includes('.') ? displayName : `${displayName}.xlsx`;

  const authHeaders = await getServiceAccountAuthHeaders({ tenantId: null });
  const form = new FormData();
  const blob = new Blob([buffer], {
    type: fullName.endsWith('.xlsx')
      ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      : 'application/octet-stream',
  });
  form.append('file', blob, fullName);
  form.append('directory', 'agent-exports');
  const uploadUrl = `${OA_BASE_URL.replace(/\/+$/, '')}/admin-api/infra/file/upload`;
  const res = await fetch(uploadUrl, { method: 'POST', headers: authHeaders, body: form });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`OA file upload failed: ${res.status} ${text.slice(0, 200)}`);
  }
  const body = await res.json().catch(() => ({}));
  if (body?.code !== 0) throw new Error(`OA file upload rejected: code=${body?.code} msg=${body?.msg}`);
  const returned = body.data;
  let downloadUrl;
  if (typeof returned === 'string' && /^https?:\/\//.test(returned)) {
    downloadUrl = returned.replace(/^https?:\/\/[^/]+/, PUBLIC_DOWNLOAD_HOST.replace(/\/+$/, ''));
  } else {
    downloadUrl = `${PUBLIC_DOWNLOAD_HOST.replace(/\/+$/, '')}/admin-api/infra/file/download/${String(returned || '').trim()}`;
  }
  const urlWithHint = downloadUrl.includes('?')
    ? `${downloadUrl}&filename=${encodeURIComponent(fullName)}`
    : `${downloadUrl}?filename=${encodeURIComponent(fullName)}`;
  const markdown = `[点击下载 ${fullName}](${urlWithHint})`;

  if (context) {
    recordExport({
      tenantId: context?.tenantId,
      userId: context?.loginUser?.id ?? context?.loginUser?.userId,
      filename: fullName,
      downloadUrl: urlWithHint,
      rows: 0,
      bytes: buffer.byteLength,
      conversationId: convId,
    }).catch(() => {});
  }
  return {
    url: urlWithHint,
    filename: fullName,
    bytes: buffer.byteLength,
    markdown,
    instruction: '把 markdown 字段原样贴到回答里。',
  };
}

// 给 agent tool 用的 zod schema
export const EXPORT_EXCEL_SCHEMA = z.object({
  rows: z.array(z.record(z.any())).describe('每行一个对象 key=列名'),
  columns: z
    .array(
      z.object({
        key: z.string(),
        header: z.string().optional(),
        numeric: z.boolean().optional(),
        money: z.boolean().optional(),
      }),
    )
    .optional()
    .describe('列定义（可选，不传就用 rows[0] 的 keys）'),
  filename: z.string().optional().describe('文件名，不带 .xlsx 后缀'),
  sheetName: z.string().optional().describe('sheet 名'),
});
