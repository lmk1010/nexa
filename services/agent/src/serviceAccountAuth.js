import { config } from './config.js';

let cachedCredential = null;
let pendingCredentialPromise = null;

function isConfigured() {
  return Boolean(
    config.serviceAccount.enabled &&
      config.serviceAccount.baseURL &&
      config.serviceAccount.username &&
      config.serviceAccount.password,
  );
}

export function parseExpiresTime(value) {
  if (value === undefined || value === null || value === '') {
    return null;
  }

  if (typeof value === 'number' && Number.isFinite(value)) {
    return value < 1_000_000_000_000 ? value * 1000 : value;
  }

  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }
    if (/^\d+$/.test(trimmed)) {
      return parseExpiresTime(Number.parseInt(trimmed, 10));
    }
    const normalized = trimmed.includes('T') ? trimmed : trimmed.replace(' ', 'T');
    const parsed = Date.parse(normalized);
    return Number.isFinite(parsed) ? parsed : null;
  }

  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value.map((item) =>
      Number.parseInt(String(item), 10),
    );
    if ([year, month, day, hour, minute, second].every(Number.isFinite)) {
      return new Date(year, month - 1, day, hour, minute, second).getTime();
    }
  }

  return null;
}

function isCredentialUsable(credential) {
  if (!credential?.accessToken) {
    return false;
  }
  if (!credential.expiresAt) {
    return true;
  }
  return credential.expiresAt - Date.now() > config.serviceAccount.refreshSkewMs;
}

function commonMessage(payload) {
  if (!payload || typeof payload !== 'object') {
    return '';
  }
  return payload.msg || payload.message || payload.error || payload.repMsg || '';
}

async function readJson(response) {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return { raw: text.slice(0, 200) };
  }
}

async function fetchAuthJson(method, path, { body, signal } = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.serviceAccount.timeoutMs);
  const signals = signal ? [controller.signal, signal] : [controller.signal];

  try {
    const response = await fetch(new URL(path, config.serviceAccount.baseURL), {
      method,
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'User-Agent': 'KYX-Agent-Service/1.0',
      },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.any(signals),
    });
    return {
      status: response.status,
      ok: response.ok,
      data: await readJson(response),
    };
  } finally {
    clearTimeout(timer);
  }
}

function unwrapCommonResult(result, action) {
  const code = typeof result.data?.code === 'number' ? result.data.code : result.status;
  const success = result.ok && (result.data?.code === undefined || result.data.code === 0);
  if (!success) {
    throw new Error(`${action} failed: status=${result.status}, code=${code}, msg=${commonMessage(result.data)}`);
  }
  return result.data?.data ?? result.data;
}

function getTenantId(tenant) {
  const value = tenant?.tenantId ?? tenant?.id;
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0 ? Math.floor(numberValue) : null;
}

function pickTenant(tenants) {
  if (!Array.isArray(tenants) || tenants.length === 0) {
    return null;
  }
  if (config.serviceAccount.tenantId) {
    const configuredTenant = tenants.find((tenant) => getTenantId(tenant) === config.serviceAccount.tenantId);
    if (configuredTenant) {
      return configuredTenant;
    }
  }
  return tenants.find((tenant) => tenant.isDefault || tenant.hasRole) || tenants[0];
}

function normalizeCredential(data, fallbackTenant) {
  if (!data?.accessToken) {
    throw new Error('Service account login succeeded without an access token');
  }

  const tenantId = data.tenantId ?? getTenantId(fallbackTenant) ?? config.serviceAccount.tenantId ?? null;
  return {
    accessToken: data.accessToken,
    refreshToken: data.refreshToken || '',
    expiresAt: parseExpiresTime(data.expiresTime),
    tenantId,
    tenantName: data.tenantName || fallbackTenant?.tenantName || fallbackTenant?.name || '',
    userId: data.userId ?? null,
  };
}

async function loginWithPassword(signal) {
  const loginBody = {
    username: config.serviceAccount.username,
    password: config.serviceAccount.password,
    deviceType: config.serviceAccount.deviceType,
    deviceId: config.serviceAccount.deviceId,
  };

  const preLogin = await fetchAuthJson('POST', '/admin-api/system/auth/pre-login', {
    body: loginBody,
    signal,
  });
  const preLoginData = unwrapCommonResult(preLogin, 'service account pre-login');
  const tenant = pickTenant(preLoginData.tenantList);
  const tenantId = config.serviceAccount.tenantId ?? getTenantId(tenant);
  if (!tenantId) {
    throw new Error('Service account pre-login returned no selectable tenant');
  }

  const tenantLogin = await fetchAuthJson('POST', '/admin-api/system/auth/tenant-login', {
    body: {
      preAuthToken: preLoginData.preAuthToken,
      tenantId,
      deviceType: config.serviceAccount.deviceType,
      deviceId: config.serviceAccount.deviceId,
    },
    signal,
  });

  return normalizeCredential(unwrapCommonResult(tenantLogin, 'service account tenant-login'), tenant);
}

async function refreshWithToken(signal) {
  if (!cachedCredential?.refreshToken) {
    return null;
  }

  const path = `/admin-api/system/auth/refresh-token?refreshToken=${encodeURIComponent(cachedCredential.refreshToken)}`;
  const result = await fetchAuthJson('POST', path, { signal });
  return normalizeCredential(unwrapCommonResult(result, 'service account refresh-token'), {
    tenantId: cachedCredential.tenantId,
    tenantName: cachedCredential.tenantName,
  });
}

async function resolveCredential(signal) {
  if (isCredentialUsable(cachedCredential)) {
    return cachedCredential;
  }

  if (pendingCredentialPromise) {
    return pendingCredentialPromise;
  }

  pendingCredentialPromise = (async () => {
    try {
      const credential = cachedCredential?.refreshToken
        ? await refreshWithToken(signal).catch(() => null)
        : null;
      cachedCredential = credential || (await loginWithPassword(signal));
      return cachedCredential;
    } finally {
      pendingCredentialPromise = null;
    }
  })();

  return pendingCredentialPromise;
}

export async function getServiceAccountAuthHeaders(context, signal) {
  if (context?.authorization || !isConfigured()) {
    return {};
  }

  const credential = await resolveCredential(signal);
  const tenantId = context?.tenantId || credential.tenantId || config.serviceAccount.tenantId;
  return {
    Authorization: `Bearer ${credential.accessToken}`,
    ...(tenantId ? { 'tenant-id': String(tenantId) } : {}),
  };
}

export function invalidateServiceAccountCredential() {
  cachedCredential = null;
}

export function serviceAccountPublicStatus() {
  return {
    enabled: Boolean(config.serviceAccount.enabled),
    configured: isConfigured(),
    baseURL: config.serviceAccount.baseURL,
    tenantId: config.serviceAccount.tenantId ?? null,
    deviceType: config.serviceAccount.deviceType,
    cached: Boolean(cachedCredential?.accessToken),
  };
}

export function resetServiceAccountAuthForTest() {
  cachedCredential = null;
  pendingCredentialPromise = null;
}
