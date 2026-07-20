import { readFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

function loadJSON(rel) {
  const here = dirname(fileURLToPath(import.meta.url));
  const candidates = [
    join(here, '../curated', rel),
    join(process.cwd(), 'curated', rel),
    join(process.cwd(), 'services/agent/curated', rel),
  ];
  for (const f of candidates) {
    if (existsSync(f)) {
      return JSON.parse(readFileSync(f, 'utf8'));
    }
  }
  return null;
}

export function getNexaAssistantMeta() {
  return (
    loadJSON('nexa-assistant.json') || {
      gateway: process.env.NEXA_GATEWAY_URL || process.env.KYX_GATEWAY_URL || 'http://127.0.0.1:48080',
      bootstrapPath: '/v1/ai/assistant/bootstrap',
      intentPath: '/v1/ai/intent/route',
      skillsPath: '/v1/ai/skills',
    }
  );
}

export function getNexaSkills() {
  const doc = loadJSON('nexa-skills.json');
  return doc?.data || [];
}

export function gatewayBase() {
  return (
    process.env.NEXA_GATEWAY_URL ||
    process.env.KYX_GATEWAY_URL ||
    process.env.OA_API_BASE_URL ||
    process.env.INTERNAL_API_BASE_URL ||
    getNexaAssistantMeta().gateway ||
    'http://127.0.0.1:48080'
  ).replace(/\/$/, '');
}

export async function fetchJSON(path, { method = 'GET', token, body, signal } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
  const res = await fetch(`${gatewayBase()}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  });
  const text = await res.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = { raw: text };
  }
  return { status: res.status, ok: res.ok, data };
}

export async function routeIntent(text, { token, signal } = {}) {
  return fetchJSON('/v1/ai/intent/route', {
    method: 'POST',
    token,
    body: { text },
    signal,
  });
}

export async function bootstrapAssistant({ token, signal } = {}) {
  return fetchJSON('/v1/ai/assistant/bootstrap', { token, signal });
}

export function createNexaSkillTools() {
  return [
    {
      name: 'nexa_intent_route',
      description:
        'Route natural language to Nexa enterprise skills (AI control plane). Returns ranked skill id/path for gateway calls.',
      parameters: {
        type: 'object',
        properties: {
          text: { type: 'string', description: 'user intent text' },
        },
        required: ['text'],
      },
      execute: async ({ text }, ctx = {}) => {
        const token = ctx.authorization || ctx.token || ctx.headers?.authorization;
        return routeIntent(text, { token, signal: ctx.signal });
      },
    },
    {
      name: 'nexa_skills_list',
      description: 'List curated Nexa enterprise skills (id, domain, method, path).',
      parameters: { type: 'object', properties: {} },
      execute: async () => ({ skills: getNexaSkills(), gateway: gatewayBase() }),
    },
    {
      name: 'nexa_assistant_bootstrap',
      description: 'Fetch AI assistant bootstrap mission/skills summary from gateway.',
      parameters: { type: 'object', properties: {} },
      execute: async (_args, ctx = {}) => {
        const token = ctx.authorization || ctx.token || ctx.headers?.authorization;
        return bootstrapAssistant({ token, signal: ctx.signal });
      },
    },
  ];
}
