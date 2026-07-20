# Unified API Index Schema

Two sources (OA Java controllers, ordersys Vue api modules) collapse into ONE
JSON file that both `api_search` (hybrid retrieval) and `api_call` (execution)
read at boot.

## File layout

```
generated/
  api-index.json           ← unified, checked in
  api-embeddings.bin        ← float32 vectors, one row per endpoint
```

## Endpoint record

```jsonc
{
  "id": "ordersys:GET:/user/statistics/sh/pf",
  "domain": "ordersys",              // "oa" | "ordersys"
  "path": "/user/statistics/sh/pf",
  "method": "GET",
  "purpose": "售后赔付统计。按月/门店/部门维度返回平台/门店/卖家承担金额、车损费、施工费、成本费。老板问'平台本月赔多少''平台承担占比多少''哪个部门赔最多'时用这里。关键指标：myMoney(平台承担)、outletsMoney(门店承担)、sellerMoney(卖家承担)、totalMoney(合计)。",
  "params": [
    { "name": "month", "type": "string", "notes": "YYYY-MM，默认本月" },
    { "name": "deptId", "type": "int", "notes": "可选，部门ID过滤" }
  ],
  "response": "{ myMoney, outletsMoney, sellerMoney, totalMoney, bcMoney, csMoney, sgMoney, cbMoney }",
  "sample": { "month": "2026-07" },
  "keywords": ["赔付", "承担", "损失", "myMoney", "平台", "车损", "施工费"],
  "source": "order-ui/src/api/system/statistics.js",
  "protection": {                    // optional
    "pageSizeMax": 30,
    "slow": false
  }
}
```

### Field rules

| field       | required | who fills it                                                                                        |
|-------------|----------|-----------------------------------------------------------------------------------------------------|
| id          | yes      | auto — `${domain}:${method}:${path}`                                                                |
| domain      | yes      | auto — from source                                                                                  |
| path        | yes      | auto — scanner                                                                                      |
| method      | yes      | auto — scanner                                                                                      |
| purpose     | yes      | **hand-written** for priority-20; auto-seeded from Java `@Operation.summary` or Vue JSDoc otherwise |
| params      | yes      | auto — Java `@Parameter/@Schema` or Vue params destructuring; may be empty                          |
| response    | optional | hand-written when high-value; otherwise `"object"`                                                  |
| sample      | optional | hand-written; makes LLM adoption faster                                                             |
| keywords    | auto     | auto-extracted from purpose + Chinese/English tokens; hand-augmentable                              |
| source      | yes      | auto — file path relative to repo                                                                   |
| protection  | optional | inherited from OA old index                                                                         |

### Read-only guarantee

An endpoint is admitted only if:
- method === "GET"
- path doesn't hit BLOCKED_PATH_RE (create/update/delete/... segments)
- summary/comment doesn't hit BLOCKED_SUMMARY_RE
- controller/vue source doesn't use `HttpServletResponse` or `EXPORT` operate-type
- for OA: `@PermitAll` or explicit `@PreAuthorize` is preserved as-is (executor
  will forward the caller's Authorization header; permission enforcement stays
  in Java)

## Retrieval index (built from records above)

```
api-embeddings.bin  (Float32Array packed)
  [ endpoint_0 vec (D=1024/1536), endpoint_1 vec, ... ]
api-index.json
  { meta, endpoints:[{ ...record, _embeddingRowIndex }] }
```

`api_search(query, k=8, domain?)`:
1. Embed query once via kyx-service-ai's OpenAI-compatible `/embeddings` route.
2. Cosine similarity vs full vector array → top 30.
3. BM25 over (purpose + keywords) → normalized rank.
4. Final score = 0.7 · cosine + 0.3 · bm25.
5. Return top-k with ALL fields except `_embeddingRowIndex`.

`api_call({path, params})` or `api_call({batch:[{path,params}, ...]})`:
1. Validate path is in the index and method is GET.
2. Forward caller auth headers (existing `callInternalApi` / `ordersysGet`).
3. Return `{ ok, status, code, data, truncated }` verbatim.
4. Batch runs in parallel (`Promise.all`) with the existing per-user queue.

No `intent` mode. No auto path guessing. No fallback cascade.
