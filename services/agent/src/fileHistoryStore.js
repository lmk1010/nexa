// Agent 导出文件历史 —— 记录每次 export_excel 的下载信息
// user_id 从 loginUser context 拿；表在 kyx_oa 库，跟 conversation/audit 共用
import mysql from 'mysql2/promise';
import { config } from './config.js';

let pool = null;
let schemaReady = null;

function getPool() {
  if (pool) return pool;
  pool = mysql.createPool({
    host: config.aiConfig.db.host,
    port: config.aiConfig.db.port,
    user: config.aiConfig.db.user,
    password: config.aiConfig.db.password,
    database: config.aiConfig.db.database,
    connectionLimit: 3,
    waitForConnections: true,
    queueLimit: 0,
    connectTimeout: config.aiConfig.db.connectTimeoutMs,
    dateStrings: true,
  });
  return pool;
}

async function ensureSchema() {
  if (schemaReady) return schemaReady;
  schemaReady = (async () => {
    await getPool().query(`
      CREATE TABLE IF NOT EXISTS kyx_agent_file_export (
        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        tenant_id BIGINT NOT NULL DEFAULT 0,
        user_id BIGINT NOT NULL DEFAULT 0,
        filename VARCHAR(200) NOT NULL,
        download_url VARCHAR(500) NOT NULL,
        rows_count INT UNSIGNED DEFAULT 0,
        bytes INT UNSIGNED DEFAULT 0,
        conversation_id VARCHAR(128) NULL,
        created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
        PRIMARY KEY (id),
        INDEX idx_user_created (user_id, tenant_id, created_at DESC)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='agent export_excel 历史';
    `);
  })();
  return schemaReady;
}

export async function recordExport({ tenantId, userId, filename, downloadUrl, rows, bytes, conversationId }) {
  if (!userId) return; // 没登录就不记
  try {
    await ensureSchema();
    await getPool().query(
      `INSERT INTO kyx_agent_file_export
       (tenant_id, user_id, filename, download_url, rows_count, bytes, conversation_id)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [tenantId || 0, userId, filename || 'export.xlsx', downloadUrl, rows || 0, bytes || 0, conversationId || null],
    );
  } catch (err) {
    console.warn(`[file-history] insert failed: ${err.message}`);
  }
}

export async function listUserExports(context, { limit = 50 } = {}) {
  const uid = Number(context?.loginUser?.id ?? context?.loginUser?.userId);
  const tid = Number(context?.tenantId);
  if (!Number.isFinite(uid) || uid <= 0) return [];
  try {
    await ensureSchema();
    const [rows] = await getPool().query(
      `SELECT id, filename, download_url, rows_count, bytes, conversation_id, created_at
       FROM kyx_agent_file_export
       WHERE user_id = ? AND tenant_id = ?
       ORDER BY created_at DESC
       LIMIT ?`,
      [uid, tid || 0, Math.max(1, Math.min(200, Number(limit) || 50))],
    );
    return rows.map((r) => ({
      id: Number(r.id),
      filename: r.filename,
      downloadUrl: r.download_url,
      rows: Number(r.rows_count) || 0,
      bytes: Number(r.bytes) || 0,
      conversationId: r.conversation_id,
      createdAt: r.created_at,
    }));
  } catch (err) {
    console.warn(`[file-history] list failed: ${err.message}`);
    return [];
  }
}
