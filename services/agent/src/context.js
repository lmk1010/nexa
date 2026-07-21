import { randomUUID } from 'node:crypto';

function firstHeader(value) {
  if (Array.isArray(value)) {
    return value[0] || '';
  }
  return typeof value === 'string' ? value : '';
}

export function parseLoginUserHeader(value) {
  const raw = firstHeader(value);
  if (!raw) {
    return null;
  }

  const candidates = [raw];
  try {
    const decoded = decodeURIComponent(raw);
    if (decoded !== raw) {
      candidates.push(decoded);
    }
  } catch {
    // Keep the raw value below.
  }

  for (const candidate of candidates) {
    try {
      const parsed = JSON.parse(candidate);
      return parsed && typeof parsed === 'object' ? parsed : null;
    } catch {
      // Try the next candidate.
    }
  }

  return null;
}

function safeUser(loginUser) {
  if (!loginUser || typeof loginUser !== 'object') {
    return null;
  }

  return {
    id: loginUser.id ?? loginUser.userId ?? null,
    userId: loginUser.userId ?? loginUser.id ?? null,
    username: loginUser.username ?? loginUser.userName ?? null,
    nickname: loginUser.nickname ?? loginUser.nickName ?? null,
    deptId: loginUser.deptId ?? null,
    tenantId: loginUser.tenantId ?? null,
  };
}

export function getRequestContext(req) {
  const loginUserHeader = firstHeader(req.headers['login-user']);
  const tenantId = firstHeader(req.headers['tenant-id'] || req.headers['tenant_id']);
  const visitTenantId = firstHeader(req.headers['visit-tenant-id'] || req.headers['visit_tenant_id']);
  const tenantIgnore = firstHeader(req.headers['tenant-ignore'] || req.headers['tenant_ignore']);
  const authorization = firstHeader(req.headers.authorization);

  let loginUser = parseLoginUserHeader(loginUserHeader);
  // nexa-core injects X-User-Id / X-Username / X-Tenant-Id when gateway login-user is absent
  if (!loginUser) {
    const uid = firstHeader(req.headers['x-user-id']);
    const un = firstHeader(req.headers['x-username']);
    const xtid = firstHeader(req.headers['x-tenant-id']) || tenantId;
    if (uid || un) {
      loginUser = {
        id: uid ? Number(uid) : un,
        userId: uid ? Number(uid) : un,
        username: un || String(uid || ''),
        nickname: un || '',
        tenantId: xtid ? Number(xtid) : null,
      };
    }
  }
  return {
    requestId: firstHeader(req.headers['x-request-id']) || randomUUID(),
    tenantId: tenantId || firstHeader(req.headers['x-tenant-id']),
    visitTenantId,
    tenantIgnore,
    authorization,
    loginUser,
    rawLoginUserHeader: loginUserHeader,
    userAgent: firstHeader(req.headers['user-agent']),
    remoteAddress: req.socket.remoteAddress || '',
  };
}

export function publicRequestContext(context) {
  return {
    requestId: context.requestId,
    tenantId: context.tenantId || null,
    visitTenantId: context.visitTenantId || null,
    tenantIgnore: context.tenantIgnore || null,
    hasAuthorization: Boolean(context.authorization),
    loginUser: safeUser(context.loginUser),
    userAgent: context.userAgent || null,
  };
}

export function forwardHeaders(context) {
  const headers = {};
  if (context.authorization) {
    headers.Authorization = context.authorization;
  }
  if (context.tenantId) {
    headers['tenant-id'] = context.tenantId;
  }
  if (context.visitTenantId) {
    headers['visit-tenant-id'] = context.visitTenantId;
  }
  if (context.tenantIgnore) {
    headers['tenant-ignore'] = context.tenantIgnore;
  }
  if (context.rawLoginUserHeader) {
    headers['login-user'] = context.rawLoginUserHeader;
  }
  if (context.requestId) {
    headers['x-request-id'] = context.requestId;
  }
  return headers;
}
