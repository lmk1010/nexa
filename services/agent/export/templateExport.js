// 模版导出 —— 载入预制 xlsx（保留全部样式、公式、合并单元格、边框），
// 只把数据驱动的 cell 填进去。相比 export_excel 从零画表，模版化能保证：
//   1. 严格视觉一致（表头 logo / 字体 / 颜色 / 边框 / 列宽 全部原样）
//   2. LLM 只提供业务数据，不用管样式
//   3. 迭代版式只改 template 文件本身，代码不动
//
// 加新模版：把 xlsx 放到 templates/，在 TEMPLATES 里加一个 renderer 即可
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import ExcelJS from 'exceljs';
import { z } from 'zod';
import { getServiceAccountAuthHeaders } from './serviceAccountAuth.js';
import { recordExport } from './fileHistoryStore.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const TEMPLATES_DIR = resolve(__dirname, '..', 'templates');

const OA_BASE_URL = process.env.OA_API_BASE_URL || process.env.INTERNAL_API_BASE_URL || 'http://kyx-gateway:48080';
const PUBLIC_DOWNLOAD_HOST = process.env.OA_PUBLIC_DOWNLOAD_HOST || 'http://43.139.24.244';

// ------------- attendance_monthly ---------------------------------------
// 结构（来自"6月-技术部考勤表.xlsx"）：
//   row 1  : year 在 C1，月份在 E1（模版里有 DATE(C1,E1,1) 公式自动推日期）
//   row 2  : 大标题（merged A2:AJ2）"安徽快易修网络科技有限公司\n——{deptName}考勤表"
//   row 3-5: 星期/日期表头，来自公式
//   row 6-7: 员工 1（上午/下午），A6/B6/C6 是 merged origin，覆盖 A6:A7 / B6:B7 / C6:C7
//   row 8-9: 员工 2 同理
//   ... 每 2 行一个员工，template 预留 85 个员工位（row 6~row 175）
const ATTENDANCE_MONTHLY_CAPACITY = 85;

function renderAttendanceMonthly(wb, { variables, employees, options }) {
  const sheet = wb.worksheets[0];
  const year = Number(variables.year);
  const month = Number(variables.month);
  if (!Number.isFinite(year) || year < 2000 || year > 2100) {
    throw new Error(`variables.year 需要 2000-2100，收到 ${variables.year}`);
  }
  if (!Number.isFinite(month) || month < 1 || month > 12) {
    throw new Error(`variables.month 需要 1-12，收到 ${variables.month}`);
  }
  const daysInMonth = new Date(year, month, 0).getDate();

  // 顶部年/月（公式会自动推所有日期表头）
  sheet.getCell('C1').value = year;
  sheet.getCell('E1').value = month;

  // 大标题
  const companyName = variables.companyName || '安徽快易修网络科技有限公司';
  const deptName = variables.deptName || '';
  const title = `${companyName}\n——${deptName}考勤表`;
  sheet.getCell('A2').value = title;

  // 员工数据
  const list = Array.isArray(employees) ? employees : [];
  if (list.length > ATTENDANCE_MONTHLY_CAPACITY) {
    throw new Error(
      `employees 太多（${list.length}），模版容量 ${ATTENDANCE_MONTHLY_CAPACITY}。请分批或换宽版模版。`,
    );
  }
  const defaultMark = options?.defaultMark || '√';
  const restMark = options?.restMark || '休';

  for (let i = 0; i < list.length; i++) {
    const emp = list[i];
    const amRow = 6 + i * 2;
    const pmRow = 7 + i * 2;
    // A/B/C 是垂直合并的（A6:A7 / B6:B7 / C6:C7），只写上格；exceljs 保留合并
    sheet.getCell(`A${amRow}`).value = i + 1;
    sheet.getCell(`B${amRow}`).value = emp.position || '';
    sheet.getCell(`C${amRow}`).value = emp.name || '';
    sheet.getCell(`D${amRow}`).value = '上午';
    sheet.getCell(`D${pmRow}`).value = '下午';

    // 日格：E=day1, F=day2, ..., AI=day31 (E 是第 5 列)
    // 只填当月存在的日，多余的天照 template 显示为空（周末逻辑靠 template 星期表头判断）
    const am = normalizeMarks(emp.am, daysInMonth, defaultMark, restMark, month, year, 'am');
    const pm = normalizeMarks(emp.pm, daysInMonth, defaultMark, restMark, month, year, 'pm');
    for (let d = 1; d <= daysInMonth; d++) {
      const col = 4 + d; // E=5=day1
      const c = sheet.getCell(amRow, col);
      c.value = am[d - 1];
      const c2 = sheet.getCell(pmRow, col);
      c2.value = pm[d - 1];
    }
  }
}

// employees.am / .pm 可以是：
//  - Array<string> 长度 = 当月天数：直接用
//  - Object { '1':'√', '2':'休', ... }：按 key 映射
//  - undefined / null / 'auto' → 用 defaultMark，周末用 restMark
function normalizeMarks(input, days, defaultMark, restMark, month, year, half) {
  const out = new Array(days).fill('');
  const isWeekend = (d) => {
    const dow = new Date(year, month - 1, d).getDay();
    return dow === 0 || dow === 6;
  };
  if (Array.isArray(input)) {
    for (let d = 1; d <= days; d++) {
      out[d - 1] = input[d - 1] != null ? String(input[d - 1]) : (isWeekend(d) ? restMark : defaultMark);
    }
    return out;
  }
  if (input && typeof input === 'object') {
    for (let d = 1; d <= days; d++) {
      const v = input[d] ?? input[String(d)];
      out[d - 1] = v != null ? String(v) : (isWeekend(d) ? restMark : defaultMark);
    }
    return out;
  }
  // auto / 空 → 工作日 √，周末 休
  for (let d = 1; d <= days; d++) {
    out[d - 1] = isWeekend(d) ? restMark : defaultMark;
  }
  return out;
}

// ------------- 模版注册表 -------------
const TEMPLATES = {
  attendance_monthly: {
    file: 'attendance_monthly.xlsx',
    render: renderAttendanceMonthly,
    // 默认文件名格式化
    defaultFilename: (vars) => `${vars.year}-${String(vars.month).padStart(2, '0')}-${vars.deptName || '部门'}考勤表`,
    description: '月度部门考勤表：每人 2 行（上午/下午）× 31 天。周末列会按模版星期表头显示，"√"=出勤 "休"=休息 "迟"=迟到 "假"=请假 "调"=调休。模版预留 85 个员工位。',
  },
};

export function listTemplates() {
  return Object.entries(TEMPLATES).map(([key, t]) => ({
    name: key,
    description: t.description,
  }));
}

export async function renderTemplate({ template, variables, employees, options, filename, context, conversationId }) {
  const def = TEMPLATES[template];
  if (!def) {
    throw new Error(`未知模版 '${template}'。可用：${Object.keys(TEMPLATES).join(', ')}`);
  }
  const filePath = resolve(TEMPLATES_DIR, def.file);
  const raw = await readFile(filePath);
  const wb = new ExcelJS.Workbook();
  await wb.xlsx.load(raw);

  def.render(wb, { variables: variables || {}, employees: employees || [], options: options || {} });

  const buffer = await wb.xlsx.writeBuffer();

  const rawName = filename || def.defaultFilename?.(variables || {}) || `${template}-export`;
  const safeName = String(rawName).replace(/[\/\\:*?"<>|]/g, '_');
  const fullName = safeName.endsWith('.xlsx') ? safeName : `${safeName}.xlsx`;

  // 上传到 OA infra/file （跟 exportTool 走同一条路，保持下载链接一致）
  const authHeaders = await getServiceAccountAuthHeaders({ tenantId: null });
  const form = new FormData();
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
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
  if (body?.code !== 0) {
    throw new Error(`OA file upload rejected: code=${body?.code} msg=${body?.msg}`);
  }
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
      rows: Array.isArray(employees) ? employees.length : 0,
      bytes: buffer.byteLength,
      conversationId,
    }).catch(() => {});
  }

  return {
    url: urlWithHint,
    filename: fullName,
    template,
    employees: Array.isArray(employees) ? employees.length : 0,
    bytes: buffer.byteLength,
    markdown,
    instruction: '把 markdown 字段原样贴到回答里；不要改成"点击这里"。',
  };
}

// zod schema
const attendanceEmployeeSchema = z.object({
  position: z.string().optional().describe('岗位/部门标签，比如"技术""主管""前端"，填在 B 列'),
  name: z.string().min(1).describe('员工姓名'),
  am: z.union([
    z.array(z.string()),
    z.record(z.string()),
    z.literal('auto'),
    z.null(),
  ]).optional().describe('上午每日打卡标记；不传/传 auto = 工作日√ 周末休；传数组时长度 = 当月天数'),
  pm: z.union([
    z.array(z.string()),
    z.record(z.string()),
    z.literal('auto'),
    z.null(),
  ]).optional().describe('下午同 am'),
});

export const RENDER_TEMPLATE_SCHEMA = z.object({
  template: z.enum(Object.keys(TEMPLATES)).describe('模版名'),
  variables: z.object({
    year: z.number().int().min(2000).max(2100),
    month: z.number().int().min(1).max(12),
    companyName: z.string().optional().describe('公司名，默认"安徽快易修网络科技有限公司"'),
    deptName: z.string().describe('部门名，用于标题和文件名'),
  }).describe('模版顶部变量'),
  employees: z.array(attendanceEmployeeSchema).describe('员工列表（每个模版格式不同，见 template 说明）'),
  options: z.object({
    defaultMark: z.string().max(4).optional().describe('工作日默认标记，默认"√"'),
    restMark: z.string().max(4).optional().describe('周末标记，默认"休"'),
  }).optional(),
  filename: z.string().optional().describe('文件名，不传按模版默认命名'),
});
