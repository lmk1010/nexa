import mysql from 'mysql2/promise';
import { config } from './config.js';

let pool = null;
function getPool() {
  if (pool) return pool;
  pool = mysql.createPool({
    host: config.aiConfig.db.host,
    port: config.aiConfig.db.port,
    user: config.aiConfig.db.user,
    password: config.aiConfig.db.password,
    database: config.aiConfig.db.database,
    connectionLimit: 5,
    waitForConnections: true,
    queueLimit: 0,
    connectTimeout: config.aiConfig.db.connectTimeoutMs,
    dateStrings: true,
  });
  return pool;
}

let schemaReady = null;
async function ensureSchema() {
  if (schemaReady) return schemaReady;
  schemaReady = (async () => {
    await getPool().query(`
      CREATE TABLE IF NOT EXISTS kyx_agent_message (
        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        tenant_id BIGINT NOT NULL DEFAULT 0,
        user_id BIGINT NOT NULL,
        conversation_id VARCHAR(128) NOT NULL,
        turn_index INT NOT NULL,
        role VARCHAR(16) NOT NULL,
        content MEDIUMTEXT NOT NULL,
        created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
        PRIMARY KEY (id),
        INDEX idx_conv (tenant_id, user_id, conversation_id, turn_index),
        INDEX idx_user_recent (tenant_id, user_id, created_at)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        COMMENT='KYX agent per-user chat history';
    `);
    await getPool().query(`
      CREATE TABLE IF NOT EXISTS kyx_agent_conversation (
        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        tenant_id BIGINT NOT NULL DEFAULT 0,
        user_id BIGINT NOT NULL,
        conversation_id VARCHAR(128) NOT NULL,
        title VARCHAR(255) NOT NULL DEFAULT '',
        scene VARCHAR(64) NOT NULL DEFAULT 'cockpit',
        message_count INT NOT NULL DEFAULT 0,
        last_message TEXT NULL,
        created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
        updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
        deleted TINYINT(1) NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        UNIQUE KEY uk_conv (tenant_id, user_id, conversation_id),
        INDEX idx_user_scene_recent (tenant_id, user_id, scene, updated_at)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        COMMENT='KYX agent conversation metadata';
    `);
  })().catch((err) => {
    schemaReady = null; // allow retry
    throw err;
  });
  return schemaReady;
}

function userIdOf(context) {
  const raw = context?.loginUser?.id ?? context?.loginUser?.userId;
  const n = Number(raw);
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : null;
}
function tenantIdOf(context) {
  const raw = context?.tenantId;
  const n = Number(raw);
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : 0;
}

function isSupportedConfig() {
  const db = config.aiConfig?.db;
  return Boolean(db?.host && db?.user && db?.database);
}

export async function loadHistory(context, conversationId, { limit = 40 } = {}) {
  if (!conversationId || !isSupportedConfig()) return [];
  const uid = userIdOf(context);
  if (!uid) return [];
  const tid = tenantIdOf(context);
  try {
    await ensureSchema();
    const [rows] = await getPool().query(
      `SELECT role, content
         FROM kyx_agent_message
         WHERE tenant_id = ? AND user_id = ? AND conversation_id = ?
         ORDER BY turn_index ASC
         LIMIT ?`,
      [tid, uid, conversationId, limit],
    );
    return rows.map((r) => ({ role: r.role, content: r.content }));
  } catch (error) {
    console.warn(
      JSON.stringify({
        level: 'warn',
        msg: 'agent history load failed',
        error: error?.message || String(error),
        userId: uid,
        tenantId: tid,
        conversationId,
      }),
    );
    return [];
  }
}

function deriveTitle(turns) {
  const firstUser = turns.find((t) => t.role === 'user' && t.content?.trim());
  const source = firstUser?.content || turns[0]?.content || '';
  const single = source.replace(/\s+/g, ' ').trim();
  if (!single) return '';
  return single.length > 40 ? single.slice(0, 40) + '…' : single;
}

// turns: [{ role: 'user'|'assistant'|'system', content: string }, ...]
export async function saveTurns(context, conversationId, turns, options = {}) {
  if (!conversationId || !isSupportedConfig()) return;
  if (!Array.isArray(turns) || turns.length === 0) return;
  const uid = userIdOf(context);
  if (!uid) return;
  const tid = tenantIdOf(context);
  const scene = typeof options.scene === 'string' && options.scene.trim()
    ? options.scene.trim().slice(0, 64)
    : 'cockpit';
  const rows = turns
    .filter(
      (t) =>
        t &&
        typeof t.role === 'string' &&
        typeof t.content === 'string' &&
        t.content.trim().length > 0,
    )
    .map((t) => ({
      role: t.role,
      content: t.content.length > 60_000 ? t.content.slice(0, 60_000) : t.content,
    }));
  if (rows.length === 0) return;

  try {
    await ensureSchema();
    const conn = await getPool().getConnection();
    try {
      await conn.beginTransaction();
      const [next] = await conn.query(
        `SELECT COALESCE(MAX(turn_index), -1) AS m
           FROM kyx_agent_message
           WHERE tenant_id = ? AND user_id = ? AND conversation_id = ?`,
        [tid, uid, conversationId],
      );
      let idx = Number(next[0]?.m ?? -1) + 1;
      const values = rows.map((r) => [
        tid,
        uid,
        conversationId,
        idx++,
        r.role,
        r.content,
      ]);
      await conn.query(
        `INSERT INTO kyx_agent_message
           (tenant_id, user_id, conversation_id, turn_index, role, content)
         VALUES ?`,
        [values],
      );

      const title = deriveTitle(rows);
      const lastAssistant = [...rows].reverse().find((r) => r.role === 'assistant');
      const preview = (lastAssistant?.content || rows[rows.length - 1].content)
        .replace(/\s+/g, ' ')
        .slice(0, 200);
      const addCount = rows.length;
      await conn.query(
        `INSERT INTO kyx_agent_conversation
           (tenant_id, user_id, conversation_id, title, scene, message_count, last_message)
         VALUES (?, ?, ?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE
           title = CASE WHEN title = '' THEN VALUES(title) ELSE title END,
           scene = VALUES(scene),
           message_count = message_count + VALUES(message_count),
           last_message = VALUES(last_message),
           deleted = 0,
           updated_at = CURRENT_TIMESTAMP(3)`,
        [tid, uid, conversationId, title, scene, addCount, preview],
      );
      await conn.commit();
    } catch (err) {
      await conn.rollback();
      throw err;
    } finally {
      conn.release();
    }
  } catch (error) {
    console.warn(
      JSON.stringify({
        level: 'warn',
        msg: 'agent history save failed',
        error: error?.message || String(error),
        userId: uid,
        tenantId: tid,
        conversationId,
      }),
    );
  }
}

export async function listConversations(context, { scene = 'cockpit', limit = 50 } = {}) {
  if (!isSupportedConfig()) return [];
  const uid = userIdOf(context);
  if (!uid) return [];
  const tid = tenantIdOf(context);
  try {
    await ensureSchema();
    const [rows] = await getPool().query(
      `SELECT conversation_id, title, scene, message_count, last_message,
              created_at, updated_at
         FROM kyx_agent_conversation
         WHERE tenant_id = ? AND user_id = ? AND scene = ? AND deleted = 0
         ORDER BY updated_at DESC
         LIMIT ?`,
      [tid, uid, scene, Math.max(1, Math.min(200, Number(limit) || 50))],
    );
    return rows.map((r) => ({
      conversationId: r.conversation_id,
      title: r.title || '',
      scene: r.scene,
      messageCount: Number(r.message_count) || 0,
      lastMessage: r.last_message || '',
      createdAt: r.created_at,
      updatedAt: r.updated_at,
    }));
  } catch (error) {
    console.warn(
      JSON.stringify({
        level: 'warn',
        msg: 'agent conversation list failed',
        error: error?.message || String(error),
        userId: uid,
        tenantId: tid,
      }),
    );
    return [];
  }
}

export async function getConversationMessages(context, conversationId, { limit = 200 } = {}) {
  if (!conversationId || !isSupportedConfig()) return [];
  const uid = userIdOf(context);
  if (!uid) return [];
  const tid = tenantIdOf(context);
  try {
    await ensureSchema();
    const [rows] = await getPool().query(
      `SELECT role, content, created_at
         FROM kyx_agent_message
         WHERE tenant_id = ? AND user_id = ? AND conversation_id = ?
         ORDER BY turn_index ASC
         LIMIT ?`,
      [tid, uid, conversationId, Math.max(1, Math.min(500, Number(limit) || 200))],
    );
    return rows.map((r) => ({
      role: r.role,
      content: r.content,
      createdAt: r.created_at,
    }));
  } catch (error) {
    console.warn(
      JSON.stringify({
        level: 'warn',
        msg: 'agent conversation messages failed',
        error: error?.message || String(error),
        userId: uid,
        tenantId: tid,
        conversationId,
      }),
    );
    return [];
  }
}

export async function renameConversation(context, conversationId, title) {
  if (!conversationId || !isSupportedConfig()) return false;
  const uid = userIdOf(context);
  if (!uid) return false;
  const tid = tenantIdOf(context);
  const clean = String(title || '').replace(/\s+/g, ' ').trim().slice(0, 255);
  if (!clean) return false;
  try {
    await ensureSchema();
    const [result] = await getPool().query(
      `UPDATE kyx_agent_conversation
          SET title = ?, updated_at = CURRENT_TIMESTAMP(3)
        WHERE tenant_id = ? AND user_id = ? AND conversation_id = ? AND deleted = 0`,
      [clean, tid, uid, conversationId],
    );
    return Number(result?.affectedRows) > 0;
  } catch (error) {
    console.warn(
      JSON.stringify({
        level: 'warn',
        msg: 'agent conversation rename failed',
        error: error?.message || String(error),
        conversationId,
      }),
    );
    return false;
  }
}

export async function deleteConversation(context, conversationId) {
  if (!conversationId || !isSupportedConfig()) return false;
  const uid = userIdOf(context);
  if (!uid) return false;
  const tid = tenantIdOf(context);
  try {
    await ensureSchema();
    const conn = await getPool().getConnection();
    try {
      await conn.beginTransaction();
      await conn.query(
        `UPDATE kyx_agent_conversation
            SET deleted = 1, updated_at = CURRENT_TIMESTAMP(3)
          WHERE tenant_id = ? AND user_id = ? AND conversation_id = ?`,
        [tid, uid, conversationId],
      );
      await conn.query(
        `DELETE FROM kyx_agent_message
          WHERE tenant_id = ? AND user_id = ? AND conversation_id = ?`,
        [tid, uid, conversationId],
      );
      await conn.commit();
      return true;
    } catch (err) {
      await conn.rollback();
      throw err;
    } finally {
      conn.release();
    }
  } catch (error) {
    console.warn(
      JSON.stringify({
        level: 'warn',
        msg: 'agent conversation delete failed',
        error: error?.message || String(error),
        conversationId,
      }),
    );
    return false;
  }
}

export async function _resetConversationStoreForTest() {
  if (pool) {
    await pool.end();
    pool = null;
  }
  schemaReady = null;
}

