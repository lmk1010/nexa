import mysql from 'mysql2/promise';
import { config } from './config.js';

const ARK_BASE_URL = 'https://ark.cn-beijing.volces.com/api/v3';

let cachedConfig = null;
let cachedConfigExpiresAt = 0;

function isEnabledStatus(status) {
  return Number(status) === 0;
}

function resolveBaseURL(platform, url) {
  if (url) {
    return url;
  }
  return platform === 'Ark' ? ARK_BASE_URL : undefined;
}

function resolveProviderType(platform) {
  switch (platform) {
    case 'Ark':
    case 'Aliyun':
    case 'Moonshot':
    case 'Tencent':
      return 'openai-compatible';
    default:
      return config.agent.providerType;
  }
}

function cacheKey(requestedModel) {
  return [
    config.aiConfig.source,
    config.aiConfig.db.host,
    config.aiConfig.db.port,
    config.aiConfig.db.database,
    config.aiConfig.tenantId || '',
    config.aiConfig.platform || '',
    requestedModel || '',
    config.aiConfig.modelType,
  ].join('|');
}

async function createConnection() {
  return mysql.createConnection({
    host: config.aiConfig.db.host,
    port: config.aiConfig.db.port,
    user: config.aiConfig.db.user,
    password: config.aiConfig.db.password,
    database: config.aiConfig.db.database,
    connectTimeout: config.aiConfig.db.connectTimeoutMs,
    charset: 'utf8mb4',
  });
}

function buildModelWhere(requestedModel) {
  const conditions = [
    "m.deleted = b'0'",
    "k.deleted = b'0'",
    'm.status = 0',
    'k.status = 0',
    'm.type = ?',
  ];
  const params = [config.aiConfig.modelType];

  if (config.aiConfig.platform) {
    conditions.push('m.platform = ?');
    params.push(config.aiConfig.platform);
  }
  if (config.aiConfig.tenantId) {
    conditions.push('m.tenant_id = ?');
    params.push(config.aiConfig.tenantId);
  }
  if (requestedModel) {
    conditions.push('m.model = ?');
    params.push(requestedModel);
  }

  return {
    sql: conditions.join(' AND '),
    params,
  };
}

async function queryDatabaseAgentConfig(requestedModel) {
  const connection = await createConnection();
  try {
    const where = buildModelWhere(requestedModel);
    const [rows] = await connection.execute(
      `
        SELECT
          m.id AS modelId,
          m.tenant_id AS tenantId,
          m.name AS modelName,
          m.model,
          m.platform,
          m.type,
          m.status AS modelStatus,
          m.default_flag AS defaultFlag,
          m.temperature,
          m.max_tokens AS maxTokens,
          m.max_contexts AS maxContexts,
          k.id AS keyId,
          k.name AS keyName,
          k.api_key AS apiKey,
          COALESCE(NULLIF(k.url, ''), '') AS baseURL,
          k.status AS keyStatus
        FROM ai_model m
        JOIN ai_api_key k ON k.id = m.key_id
        WHERE ${where.sql}
        ORDER BY m.default_flag DESC, m.sort ASC, m.id ASC
        LIMIT 1
      `,
      where.params,
    );

    const row = rows[0];
    if (!row || !row.apiKey || !isEnabledStatus(row.modelStatus) || !isEnabledStatus(row.keyStatus)) {
      return null;
    }

    return {
      source: 'database',
      tenantId: row.tenantId,
      modelId: row.modelId,
      keyId: row.keyId,
      keyName: row.keyName,
      modelName: row.modelName,
      model: row.model,
      platform: row.platform,
      providerType: resolveProviderType(row.platform),
      apiKey: row.apiKey,
      baseURL: resolveBaseURL(row.platform, row.baseURL),
      maxTokens: row.maxTokens,
      maxContexts: row.maxContexts,
      temperature: row.temperature == null ? null : Number(row.temperature),
    };
  } finally {
    await connection.end();
  }
}

export async function resolveDatabaseAgentConfig(requestedModel) {
  if (config.aiConfig.source !== 'database') {
    return null;
  }

  const key = cacheKey(requestedModel);
  const now = Date.now();
  if (cachedConfig && cachedConfig.key === key && cachedConfigExpiresAt > now) {
    return cachedConfig.value;
  }

  const value = await queryDatabaseAgentConfig(requestedModel);
  cachedConfig = { key, value };
  cachedConfigExpiresAt = now + config.aiConfig.cacheMs;
  return value;
}
