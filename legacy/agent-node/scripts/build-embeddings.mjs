#!/usr/bin/env node
// For each endpoint in generated/api-index.json, produce an embedding vector
// from its (purpose + keywords) "semantic fingerprint" and write:
//   - generated/api-embeddings.bin   Float32Array packed [row0, row1, ...]
//   - generated/api-embeddings.meta.json  { dim, count, hashes:[...] }
//
// Row order == endpoint order in api-index.json. api_search.js just does a
// dot-product against the packed array; no vector DB needed.
//
// Incremental: hash(input) is remembered per row across runs. Unchanged rows
// skip the API call and reuse the previous vector — free rebuilds when only
// unrelated endpoints changed.
//
// ARK volcengine (default): POST /embeddings, OpenAI-compatible payload
//   { model, input: [str, ...] } → { data: [{ embedding, index }] }
// Also works verbatim against Aliyun 百炼 / Tencent / any OpenAI-compatible.
// Env:
//   EMBEDDING_BASE_URL   default https://ark.cn-beijing.volces.com/api/v3
//   EMBEDDING_MODEL      default doubao-embedding-text-240515
//   EMBEDDING_API_KEY    required
//   EMBEDDING_BATCH      default 25 (ARK safe batch)
//   EMBEDDING_CONCURRENCY default 4

import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadLocalEnv } from '../src/env.js';

loadLocalEnv();

const scriptDir = dirname(fileURLToPath(import.meta.url));
const agentRoot = resolve(scriptDir, '..');
const INDEX = resolve(agentRoot, 'generated', 'api-index.json');
const BIN_OUT = resolve(agentRoot, 'generated', 'api-embeddings.bin');
const META_OUT = resolve(agentRoot, 'generated', 'api-embeddings.meta.json');

const BASE_URL = (process.env.EMBEDDING_BASE_URL || 'https://ark.cn-beijing.volces.com/api/v3').replace(/\/$/, '');
const MODEL = process.env.EMBEDDING_MODEL || 'doubao-embedding-text-240515';
const API_KEY = process.env.EMBEDDING_API_KEY;
const BATCH = Number.parseInt(process.env.EMBEDDING_BATCH || '25', 10);
const CONCURRENCY = Number.parseInt(process.env.EMBEDDING_CONCURRENCY || '4', 10);

if (!API_KEY) {
  console.error('EMBEDDING_API_KEY not set. Export it or add to backend/kyx-service-agent/.env.');
  process.exit(1);
}

function inputTextForEndpoint(ep) {
  // Semantic fingerprint: purpose句 + keywords + path tail.
  // Purpose carries meaning; keywords cover exact-match terms; path tail
  // gives the retriever an anchor for questions that literally quote a route.
  const pathTail = ep.path.split('/').filter(Boolean).slice(-3).join(' / ');
  return [ep.purpose || '', (ep.keywords || []).join(' '), pathTail].filter(Boolean).join(' \n');
}

function hashText(text) {
  return createHash('sha1').update(text).digest('hex').slice(0, 16);
}

// ARK has two shapes:
//  - /embeddings          batched, `input:[str,...]` → `data:[{embedding,index},...]`
//  - /embeddings/multimodal  single, `input:[{type:'text',text}]` → `data:{embedding}`
// The multimodal route is required for endpoints that back onto vision-family
// models (doubao-embedding-vision-*), which is what you get when you create an
// `ep-…` inference endpoint. Auto-detect by prefix so a config swap is enough.
const IS_MULTIMODAL = MODEL.startsWith('ep-');

async function callBatched(inputs) {
  const res = await fetch(`${BASE_URL}/embeddings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${API_KEY}` },
    body: JSON.stringify({ model: MODEL, input: inputs }),
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`embedding HTTP ${res.status}: ${text.slice(0, 200)}`);
  }
  const json = await res.json();
  if (!json?.data?.length) throw new Error('embedding response missing data');
  return json.data
    .sort((a, b) => (a.index ?? 0) - (b.index ?? 0))
    .map((row) => row.embedding);
}

async function callMultimodal(inputs) {
  // Single-input endpoint — parallelise at the caller.
  const out = new Array(inputs.length);
  await Promise.all(
    inputs.map(async (text, i) => {
      const res = await fetch(`${BASE_URL}/embeddings/multimodal`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${API_KEY}` },
        body: JSON.stringify({ model: MODEL, input: [{ type: 'text', text }] }),
      });
      if (!res.ok) {
        const t = await res.text().catch(() => '');
        throw new Error(`embedding HTTP ${res.status}: ${t.slice(0, 200)}`);
      }
      const json = await res.json();
      const emb = json?.data?.embedding;
      if (!Array.isArray(emb)) throw new Error('multimodal embedding response missing embedding');
      out[i] = emb;
    }),
  );
  return out;
}

async function callEmbeddings(inputs) {
  return IS_MULTIMODAL ? callMultimodal(inputs) : callBatched(inputs);
}

async function callWithRetry(inputs, attempt = 0) {
  try {
    return await callEmbeddings(inputs);
  } catch (err) {
    if (attempt >= 3) throw err;
    const backoff = 800 * Math.pow(2, attempt);
    console.warn(`embedding batch failed (${err.message}); retry in ${backoff}ms`);
    await new Promise((r) => setTimeout(r, backoff));
    return callWithRetry(inputs, attempt + 1);
  }
}

function loadPreviousCache() {
  if (!existsSync(META_OUT) || !existsSync(BIN_OUT)) return null;
  try {
    const meta = JSON.parse(readFileSync(META_OUT, 'utf8'));
    const buf = readFileSync(BIN_OUT);
    const vectors = new Float32Array(buf.buffer, buf.byteOffset, buf.byteLength / 4);
    if (meta.dim && meta.count && vectors.length === meta.dim * meta.count) {
      const byHash = new Map();
      for (let i = 0; i < meta.count; i += 1) {
        const h = meta.hashes[i];
        if (!h) continue;
        byHash.set(h, vectors.slice(i * meta.dim, (i + 1) * meta.dim));
      }
      return { dim: meta.dim, byHash };
    }
  } catch (err) {
    console.warn(`could not reload previous embeddings: ${err.message}`);
  }
  return null;
}

async function main() {
  const index = JSON.parse(readFileSync(INDEX, 'utf8'));
  const endpoints = index.endpoints || [];
  console.log(`endpoints: ${endpoints.length}`);
  console.log(`model: ${MODEL} via ${BASE_URL}`);

  const inputs = endpoints.map(inputTextForEndpoint);
  const hashes = inputs.map(hashText);

  const cache = loadPreviousCache();
  const cacheHits = [];
  const toEmbed = [];
  for (let i = 0; i < endpoints.length; i += 1) {
    const cached = cache?.byHash.get(hashes[i]);
    if (cached && cached.length) {
      cacheHits.push(i);
    } else {
      toEmbed.push(i);
    }
  }
  console.log(`cache hits: ${cacheHits.length}, to embed: ${toEmbed.length}`);

  // Discover dim from either cache or first live call.
  let dim = cache?.dim || 0;

  // Kick off live embedding for uncached rows in parallel batches.
  const results = new Array(endpoints.length);
  for (const i of cacheHits) results[i] = cache.byHash.get(hashes[i]);

  const batches = [];
  for (let i = 0; i < toEmbed.length; i += BATCH) {
    batches.push(toEmbed.slice(i, i + BATCH));
  }

  let inFlight = 0;
  let finished = 0;
  const total = batches.length;

  async function runBatch(rowIndexes) {
    const texts = rowIndexes.map((i) => inputs[i]);
    const vectors = await callWithRetry(texts);
    if (!dim && vectors[0]) dim = vectors[0].length;
    for (let k = 0; k < rowIndexes.length; k += 1) {
      results[rowIndexes[k]] = Float32Array.from(vectors[k]);
    }
    finished += 1;
    if (finished % 5 === 0 || finished === total) {
      console.log(`  batch ${finished}/${total} done`);
    }
  }

  const queue = batches.slice();
  const workers = new Array(Math.min(CONCURRENCY, queue.length)).fill(0).map(async () => {
    while (queue.length) {
      const batch = queue.shift();
      inFlight += 1;
      try {
        await runBatch(batch);
      } finally {
        inFlight -= 1;
      }
    }
  });
  await Promise.all(workers);

  if (!dim) {
    console.error('no dim resolved — likely all rows cached but cache invalid');
    process.exit(1);
  }

  // Pack into a single Float32Array.
  const packed = new Float32Array(endpoints.length * dim);
  for (let i = 0; i < endpoints.length; i += 1) {
    const v = results[i];
    if (!v || v.length !== dim) {
      console.error(`row ${i} (${endpoints[i].id}) missing/wrong-dim vector`);
      process.exit(1);
    }
    packed.set(v, i * dim);
  }

  writeFileSync(BIN_OUT, Buffer.from(packed.buffer, packed.byteOffset, packed.byteLength));
  writeFileSync(
    META_OUT,
    JSON.stringify(
      {
        generatedAt: new Date().toISOString(),
        model: MODEL,
        baseUrl: BASE_URL,
        dim,
        count: endpoints.length,
        hashes,
      },
      null,
      2,
    ),
  );

  console.log(`\nwrote ${BIN_OUT} (${packed.byteLength} bytes, ${endpoints.length}×${dim})`);
  console.log(`wrote ${META_OUT}`);
}

main().catch((err) => {
  console.error(err.stack || err.message);
  process.exit(1);
});
