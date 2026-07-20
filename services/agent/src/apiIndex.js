// Loader + hybrid retriever for the unified endpoint index.
//
// At boot:
//   - Read generated/api-index.json (record list)
//   - Memory-map generated/api-embeddings.bin (Float32Array) if present
//   - Read generated/api-embeddings.meta.json (dim, model, base URL)
//
// At query time (`search`):
//   1. Embed the query once against the same model (~50ms).
//   2. Cosine similarity vs every endpoint vector — fast enough for 1000s
//      of records (<5ms on a laptop). No vector DB.
//   3. Add a character-bigram BM25-lite score over (purpose + keywords).
//      This costs nothing (in-memory) and rescues queries that hit exact
//      Chinese terms which embeddings alone sometimes rank low.
//   4. Blend: 0.7·cosine + 0.3·keyword; return top-K cards.
//
// `getByPath` is what api_call uses to validate + look up parameter shape.

import { readFileSync, existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const INDEX_PATH = resolve(__dirname, '..', 'generated', 'api-index.json');
const BIN_PATH = resolve(__dirname, '..', 'generated', 'api-embeddings.bin');
const META_PATH = resolve(__dirname, '..', 'generated', 'api-embeddings.meta.json');

function loadJson(path, fallback) {
  if (!existsSync(path)) return fallback;
  try {
    return JSON.parse(readFileSync(path, 'utf8'));
  } catch {
    return fallback;
  }
}

function loadEmbeddings(dim, count) {
  if (!existsSync(BIN_PATH)) return null;
  const buf = readFileSync(BIN_PATH);
  if (buf.byteLength !== dim * count * 4) return null;
  // Copy into an aligned Float32Array (readFileSync buffer may not be aligned).
  const arr = new Float32Array(dim * count);
  const view = new Float32Array(buf.buffer, buf.byteOffset, dim * count);
  arr.set(view);
  // Pre-normalize each row for cheap cosine (dot product == cosine).
  for (let i = 0; i < count; i += 1) {
    let sum = 0;
    for (let j = 0; j < dim; j += 1) sum += arr[i * dim + j] * arr[i * dim + j];
    const norm = Math.sqrt(sum) || 1;
    for (let j = 0; j < dim; j += 1) arr[i * dim + j] /= norm;
  }
  return arr;
}

// Character bigrams for Chinese; whitespace-split for latin so both scripts
// contribute. Returns a Map<token, count>.
function tokenBag(text) {
  const bag = new Map();
  if (!text) return bag;
  const s = String(text).toLowerCase();
  // Latin words.
  for (const w of s.match(/[a-z0-9_-]{2,}/g) || []) {
    bag.set(w, (bag.get(w) || 0) + 1);
  }
  // Chinese bigrams — only useful signal for Chinese short queries.
  const chineseRuns = s.match(/[一-鿿]+/g) || [];
  for (const run of chineseRuns) {
    if (run.length === 1) {
      bag.set(run, (bag.get(run) || 0) + 1);
      continue;
    }
    for (let i = 0; i + 2 <= run.length; i += 1) {
      const t = run.slice(i, i + 2);
      bag.set(t, (bag.get(t) || 0) + 1);
    }
  }
  return bag;
}

function keywordScore(queryBag, endpointBag) {
  if (queryBag.size === 0 || endpointBag.size === 0) return 0;
  let overlap = 0;
  for (const [t, c] of queryBag) {
    if (endpointBag.has(t)) overlap += Math.min(c, endpointBag.get(t));
  }
  // Normalize against query mass — so a query hitting exactly one word
  // completely still scores 1.
  let queryMass = 0;
  for (const c of queryBag.values()) queryMass += c;
  return overlap / queryMass;
}

function cardFor(record) {
  return {
    id: record.id,
    domain: record.domain,
    path: record.path,
    method: record.method,
    purpose: record.purpose,
    params: record.params,
    response: record.response,
    sample: record.sample,
    source: record.source,
    // When false, api_call will refuse this path with a hint. Surfaced to the
    // LLM so it stops picking known-denied cards from the top-K and either
    // rephrases or tells the user the capability is not open.
    callable: record._allowed !== false,
  };
}

let state = null;

function ensureLoaded() {
  if (state) return state;
  const index = loadJson(INDEX_PATH, null);
  if (!index) throw new Error(`api-index.json missing at ${INDEX_PATH}. Run scripts/build-api-index.mjs.`);

  const records = index.endpoints || [];
  const byId = new Map(records.map((r) => [r.id, r]));
  const byPath = new Map(records.map((r) => [`${r.method}:${r.path}`, r]));
  const endpointBags = records.map((r) =>
    tokenBag((r.purpose || '') + ' ' + (r.keywords || []).join(' ') + ' ' + r.path),
  );

  const meta = loadJson(META_PATH, null);
  const embeddings = meta && meta.dim && meta.count === records.length
    ? loadEmbeddings(meta.dim, meta.count)
    : null;

  if (!embeddings) {
    console.warn('[apiIndex] embeddings missing — search will fall back to keyword-only ranking.');
  }

  state = { index, records, byId, byPath, endpointBags, embeddings, meta };
  return state;
}

async function embedQuery(text) {
  const { meta } = ensureLoaded();
  if (!meta) return null;
  const baseUrl = (process.env.EMBEDDING_BASE_URL || meta.baseUrl || '').replace(/\/$/, '');
  const model = process.env.EMBEDDING_MODEL || meta.model;
  const apiKey = process.env.EMBEDDING_API_KEY;
  if (!baseUrl || !apiKey) return null;
  const res = await fetch(`${baseUrl}/embeddings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${apiKey}` },
    body: JSON.stringify({ model, input: [text] }),
  });
  if (!res.ok) throw new Error(`query embed HTTP ${res.status}`);
  const json = await res.json();
  const emb = json?.data?.[0]?.embedding;
  if (!emb) return null;
  // Normalize so `dot` equals cosine.
  let sum = 0;
  for (const v of emb) sum += v * v;
  const norm = Math.sqrt(sum) || 1;
  return Float32Array.from(emb, (v) => v / norm);
}

function cosineTopK(queryVec, k) {
  const { records, embeddings, meta } = state;
  const dim = meta.dim;
  const scores = new Array(records.length);
  for (let i = 0; i < records.length; i += 1) {
    let dot = 0;
    const off = i * dim;
    for (let j = 0; j < dim; j += 1) dot += queryVec[j] * embeddings[off + j];
    scores[i] = dot;
  }
  return scores;
}

export async function search(query, options = {}) {
  const k = Math.max(1, Math.min(30, options.k || 8));
  const domain = options.domain;
  const cur = ensureLoaded();

  const queryBag = tokenBag(query);

  let cosineScores = null;
  try {
    if (cur.embeddings) {
      const qvec = await embedQuery(query);
      if (qvec) cosineScores = cosineTopK(qvec, k);
    }
  } catch (err) {
    // Log and continue — pure keyword still returns reasonable results.
    console.warn(`[apiIndex] query embed failed: ${err.message}`);
  }

  const combined = cur.records.map((r, i) => {
    if (domain && r.domain !== domain) return { i, score: -1 };
    const cos = cosineScores ? cosineScores[i] : 0;
    const kw = keywordScore(queryBag, cur.endpointBags[i]);
    // 0.7 cosine + 0.3 kw when both present. If no cosine, kw carries all.
    const score = cosineScores ? cos * 0.7 + kw * 0.3 : kw;
    return { i, score };
  });

  combined.sort((a, b) => b.score - a.score);
  const top = combined.slice(0, k).filter((row) => row.score > 0);

  return top.map((row) => ({
    ...cardFor(cur.records[row.i]),
    score: Number(row.score.toFixed(4)),
  }));
}

export function getByPath(path, method = 'GET') {
  const cur = ensureLoaded();
  const key = `${method.toUpperCase()}:${path}`;
  return cur.byPath.get(key) || null;
}

export function stats() {
  const cur = ensureLoaded();
  return {
    total: cur.records.length,
    oa: cur.records.filter((r) => r.domain === 'oa').length,
    ordersys: cur.records.filter((r) => r.domain === 'ordersys').length,
    curated: cur.records.filter((r) => r._curated).length,
    embeddings: !!cur.embeddings,
    model: cur.meta?.model || null,
  };
}

export function _resetForTest() {
  state = null;
}
