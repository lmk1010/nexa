import { loadLocalEnv } from './env.js';

loadLocalEnv();

const providerTypes = new Set([
  'anthropic',
  'openai',
  'openai-responses',
  'openai-compatible',
  'deepseek',
  'gemini',
  'kimi',
  'glm',
  'doubao',
]);

const permissionModes = new Set(['auto', 'ask', 'readonly']);
const configSources = new Set(['env', 'database']);

function isTruthy(value) {
  return ['1', 'true', 'yes', 'on'].includes(String(value ?? '').trim().toLowerCase());
}

function parseInteger(name, fallback) {
  const raw = process.env[name];
  if (raw === undefined || raw === '') {
    return fallback;
  }

  const value = Number.parseInt(raw, 10);
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(`${name} must be a positive integer`);
  }
  return value;
}

function parseOptionalInteger(name) {
  const raw = process.env[name];
  if (raw === undefined || raw === '') {
    return null;
  }

  const value = Number.parseInt(raw, 10);
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(`${name} must be a positive integer`);
  }
  return value;
}

function parseList(name, fallback) {
  const raw = process.env[name];
  if (!raw) {
    return fallback;
  }

  const values = raw
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);

  return values.length > 0 ? values : fallback;
}

function enumValue(name, fallback, allowedValues) {
  const value = process.env[name] || fallback;
  if (!allowedValues.has(value)) {
    throw new Error(`${name} must be one of: ${Array.from(allowedValues).join(', ')}`);
  }
  return value;
}

function defaultSystemPrompt() {
  // v3 —— workspace 架构。大数据留文件，python 处理，上下文只见摘要
  return [
    '# KYX 企业助手',
    '',
    '给 KYX 老板/管理层查数据、下判断、出结论。',
    '',
    '## 数据管道（重要 —— 你的工作方式）',
    '',
    '**大数据不进你的上下文**。api_call 拿到 >8KB 数据时会自动落盘到 workspace，返回给你：',
    '```',
    '{ file: "api_xxx.json", rows: 297, sample_fields: [...], preview: [...前3行] }',
    '```',
    '**你不能"看"完整数据**。想处理就用 `python_exec` 打开这个文件。',
    '',
    '## 工具',
    '',
    '### Compute（核心 —— 大部分数据处理都用它）',
    '- `python_exec`：跑 python3 脚本，cwd = 会话 workspace。已装 pandas/numpy/openpyxl/xlsxwriter/python-dateutil。**任何数据加工、汇总、格式化、透视、写 xlsx/csv 都用这个**。',
    '- `shell_exec`：白名单命令（jq/grep/awk/sed/…）快速处理',
    '',
    '### Time',
    '- `now`：当前时间 + 各种 anchor（joined 直接塞进 API 时间参数）',
    '- `parse_time`：中文自然语言 → 时间范围',
    '',
    '### Data',
    '- `api_search`：自然语言 → 端点卡片 top-8',
    '- `api_call`：单/批量 GET。**大响应自动落盘**',
    '- `paginate_all`：**分页神器**。用户说"全部"→ 一次调用拉完所有页',
    '- `get_request_context`：当前用户/租户',
    '',
    '### Output',
    '- `render_chart`：画图（bar/pie/line/stat）',
    '- `export_excel`：小数据（<50 行）直接传 rows 数组导出',
    '- `export_excel_from_file`：**大数据用**。你先在 python_exec 里 `df.to_excel("out.xlsx")` 或 xlsxwriter constant_memory 写好，再调这个上传，返回下载链接',
    '- `fetch_url`',
    '',
    '## 决策框架（每次动手前想 3 层）',
    '',
    '1. **能不能不问 API？** 时间换算、数字计算、格式转换、字段挑选都用 Foundation 工具，别塞给 api_call。',
    '',
    '2. **API 支持这个筛选吗？** 看卡片 `params` 和 `params.notes`：',
    '   - 支持 → 服务端筛（快，省 context）',
    '   - 不支持 → 先跟用户说"接口只支持 X"，别拿全部回来自己 jq',
    '',
    '3. **一次能出结果的问题**不要走 3 轮。',
    '   - 多个独立查询用 `api_call({batch:[...]})` 并发',
    '   - 不要"api_search → api_call → api_search 换关键字 → api_call → ..."的兜圈',
    '',
    '## 领域路由',
    '',
    '- oa (KYX 内部)：需求 / 员工 / 考勤 / 请假 / 绩效 / 问卷 / 考试 / 待办 / ERP / 财务 / BPM',
    '- ordersys (连图)：订单 / 工单 / 售后（赔付/补发/评价/撤单/超时）/ 排行榜 / 任务看板 / 社区',
    '',
    '不确定用哪个域就 `api_search` 不带 domain 全库搜。卡片会告诉你答案。',
    '',
    '## 硬约束',
    '',
    '- **卡片 `callable:false` 一律跳过**，别试',
    '- **返回 `cached_failure:true`** 或 `REPEAT_GUARD` = 本轮问过了，看上一次结果，不要再打',
    '- **单次 api_call > 30s** = 数据量爆了，告诉用户缩范围，别重试',
    '- **跨度 > 30 天** 先跟用户确认，别默认拆成 12 次调用',
    '- **响应体截断了** = 你没传 pageSize，加上 pageSize <= 30 再来',
    '',
    '## 数据纪律',
    '',
    '- 数字/姓名/编号/日期从工具结果原样搬，不改写',
    '- 事实、推断、建议**分开写**',
    '- 没查到就说没查到；不脑补',
    '- 不暴露 Authorization / token / API key',
    '',
    '## 回答风格',
    '',
    '- 中文，先结论后数据',
    '- 对比用表格；趋势/分布/排名必须 `render_chart`',
    '- 下载文件必须用 markdown 链接语法 `[点击下载 xxx.xlsx](url)`',
    '- 短、准，老板视角',
  ].join('\n');
}

function providerApiKeyEnv(providerType) {
  const envByProvider = {
    anthropic: 'ANTHROPIC_API_KEY',
    openai: 'OPENAI_API_KEY',
    'openai-responses': 'OPENAI_API_KEY',
    'openai-compatible': 'OPENAI_API_KEY',
    deepseek: 'DEEPSEEK_API_KEY',
    kimi: 'KIMI_API_KEY',
    gemini: 'GEMINI_API_KEY',
    glm: 'GLM_API_KEY',
    doubao: 'DOUBAO_API_KEY',
  };

  const envName = envByProvider[providerType];
  return envName ? process.env[envName] : undefined;
}

function knownProviderApiKey() {
  return (
    process.env.ANTHROPIC_API_KEY ||
    process.env.OPENAI_API_KEY ||
    process.env.DEEPSEEK_API_KEY ||
    process.env.KIMI_API_KEY ||
    process.env.GEMINI_API_KEY ||
    process.env.GLM_API_KEY ||
    process.env.DOUBAO_API_KEY ||
    ''
  );
}

const providerType = enumValue('AGENT_PROVIDER', 'openai-compatible', providerTypes);
const explicitProviderConfigured = Boolean(process.env.AGENT_API_KEY || process.env.AGENT_PROVIDER || process.env.AGENT_BASE_URL);
const internalApiBaseURL = process.env.INTERNAL_API_BASE_URL || process.env.KYX_GATEWAY_URL || 'http://localhost:48080';
const oaApiBaseURL = process.env.OA_API_BASE_URL || internalApiBaseURL;
const ordersysApiBaseURL = process.env.ORDERSYS_API_BASE_URL || 'https://order.liantucn.com/api';

export const config = {
  service: {
    name: process.env.SERVICE_NAME || 'agent-server',
    port: parseInteger('PORT', 48091),
    bodyLimitBytes: parseInteger('REQUEST_BODY_LIMIT_BYTES', 1024 * 1024),
    corsAllowOrigin: process.env.CORS_ALLOW_ORIGIN || '',
  },
  agent: {
    model: process.env.AGENT_MODEL || 'deepseek-chat',
    providerType,
    apiKey: process.env.AGENT_API_KEY || (explicitProviderConfigured ? providerApiKeyEnv(providerType) || knownProviderApiKey() : ''),
    baseURL: process.env.AGENT_BASE_URL || '',
    timeoutMs: parseInteger('AGENT_TIMEOUT_MS', 120_000),
    maxSteps: parseInteger('AGENT_MAX_STEPS', 20),
    toolSearchEnabled: isTruthy(process.env.AGENT_ENABLE_TOOL_SEARCH),
    toolSearchMaxCallsPerRun: parseInteger('AGENT_TOOL_SEARCH_MAX_CALLS_PER_RUN', 2),
    permission: enumValue('AGENT_PERMISSION', 'auto', permissionModes),
    systemPrompt: process.env.AGENT_SYSTEM_PROMPT || defaultSystemPrompt(),
    useMock: isTruthy(process.env.AGENT_USE_MOCK),
    mockResponse: process.env.AGENT_MOCK_RESPONSE || 'ok',
    streamThinking: isTruthy(process.env.AGENT_STREAM_THINKING),
  },
  internalApi: {
    enabled: isTruthy(process.env.AGENT_ENABLE_INTERNAL_HTTP_TOOLS),
    rawToolsEnabled: isTruthy(process.env.AGENT_ENABLE_RAW_INTERNAL_HTTP_TOOLS),
    baseURL: internalApiBaseURL,
    allowPrefixes: parseList('INTERNAL_API_ALLOW_PREFIXES', ['/admin-api/', '/app-api/']),
    timeoutMs: parseInteger('INTERNAL_API_TIMEOUT_MS', 30_000),
    maxResponseBytes: parseInteger('INTERNAL_API_MAX_RESPONSE_BYTES', 512 * 1024),
    maxConcurrentRequests: parseInteger('INTERNAL_API_MAX_CONCURRENT_REQUESTS', 20),
    maxConcurrentRequestsPerUser: parseInteger('INTERNAL_API_MAX_CONCURRENT_REQUESTS_PER_USER', 6),
    queueTimeoutMs: parseInteger('INTERNAL_API_QUEUE_TIMEOUT_MS', 10_000),
    maxToolCallsPerRun: parseInteger('AGENT_INTERNAL_API_MAX_TOOL_CALLS_PER_RUN', 20),
  },
  serviceAccount: {
    enabled: isTruthy(process.env.SERVICE_ACCOUNT_ENABLED),
    baseURL: process.env.SERVICE_ACCOUNT_BASE_URL || oaApiBaseURL,
    username: process.env.SERVICE_ACCOUNT_USERNAME || process.env.OA_SERVICE_ACCOUNT_USERNAME || '',
    password: process.env.SERVICE_ACCOUNT_PASSWORD || process.env.OA_SERVICE_ACCOUNT_PASSWORD || '',
    tenantId: parseOptionalInteger('SERVICE_ACCOUNT_TENANT_ID') ?? parseOptionalInteger('OA_SERVICE_ACCOUNT_TENANT_ID'),
    deviceType: process.env.SERVICE_ACCOUNT_DEVICE_TYPE || 'APP',
    deviceId: process.env.SERVICE_ACCOUNT_DEVICE_ID || 'kyx-agent-service',
    refreshSkewMs: parseInteger('SERVICE_ACCOUNT_REFRESH_SKEW_MS', 5 * 60_000),
    timeoutMs: parseInteger('SERVICE_ACCOUNT_TIMEOUT_MS', 15_000),
  },
  ordersys: {
    authMode: process.env.ORDERSYS_AUTH_MODE || 'direct',
    baseURL: ordersysApiBaseURL,
    loginPath: process.env.ORDERSYS_LOGIN_PATH || '/login',
    username: process.env.ORDERSYS_SERVICE_ACCOUNT_USERNAME || '',
    password: process.env.ORDERSYS_SERVICE_ACCOUNT_PASSWORD || '',
    timeoutMs: parseInteger('ORDERSYS_API_TIMEOUT_MS', 12_000),
    tokenCacheMs: parseInteger('ORDERSYS_TOKEN_CACHE_MS', 30 * 60_000),
    refreshSkewMs: parseInteger('ORDERSYS_REFRESH_SKEW_MS', 60_000),
    maxConcurrentRequests: parseInteger('ORDERSYS_API_MAX_CONCURRENT_REQUESTS', 5),
    maxConcurrentRequestsPerUser: parseInteger('ORDERSYS_API_MAX_CONCURRENT_REQUESTS_PER_USER', 3),
    queueTimeoutMs: parseInteger('ORDERSYS_API_QUEUE_TIMEOUT_MS', 10_000),
    cooldownMs: parseInteger('ORDERSYS_API_COOLDOWN_MS', 60_000),
    pageSizeMax: parseInteger('ORDERSYS_API_PAGE_SIZE_MAX', 30),
    maxToolCallsPerRun: parseInteger('ORDERSYS_MAX_TOOL_CALLS_PER_RUN', 8),
  },
  aiConfig: {
    source: enumValue('AGENT_CONFIG_SOURCE', 'env', configSources),
    tenantId: parseOptionalInteger('AI_CONFIG_TENANT_ID'),
    platform: process.env.AI_CONFIG_PLATFORM || 'Ark',
    model: process.env.AI_CONFIG_MODEL || '',
    modelType: parseInteger('AI_CONFIG_MODEL_TYPE', 1),
    cacheMs: parseInteger('AI_CONFIG_CACHE_MS', 60_000),
    db: {
      host: process.env.AI_CONFIG_DB_HOST || process.env.MYSQL_HOST || 'localhost',
      port: parseInteger('AI_CONFIG_DB_PORT', Number.parseInt(process.env.MYSQL_PORT || '3306', 10)),
      user: process.env.AI_CONFIG_DB_USER || process.env.MYSQL_USERNAME || 'kyx_user',
      password: process.env.AI_CONFIG_DB_PASSWORD || process.env.MYSQL_PASSWORD || '',
      database: process.env.AI_CONFIG_DB_DATABASE || process.env.MYSQL_DATABASE || 'kyx_oa',
      connectTimeoutMs: parseInteger('AI_CONFIG_DB_CONNECT_TIMEOUT_MS', 10_000),
    },
  },
};
