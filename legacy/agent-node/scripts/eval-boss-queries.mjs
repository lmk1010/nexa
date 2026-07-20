#!/usr/bin/env node
// One-off retrieval-quality probe. Runs the 22 boss questions against the
// currently-loaded embeddings and prints top-1 / top-3 hit rates + per-query
// misses. Used to compare 240715 vs 250515-large before deciding which one
// production runs on.
import { loadLocalEnv } from '../src/env.js';
loadLocalEnv();
const { search, stats } = await import('../src/apiIndex.js');

const CASES = [
  ['本月平台承担的赔付金额多少', '/user/statistics/sh/pf'],
  ['哪个部门赔的最多', '/user/statistics/rank/compensation'],
  ['本月营业额和利润', '/user/statistics/sh/order'],
  ['哪个部门订单最多', '/user/statistics/rank/order'],
  ['本月工单处理量', '/user/statistics/sh/work'],
  ['谁完结工单最多', '/user/statistics/rank/work'],
  ['玻璃膜车衣补发率', '/user/statistics/sh/bf'],
  ['哪个部门补发排行', '/user/statistics/rank/reissue'],
  ['本月好评差评统计', '/user/statistics/sh/rate'],
  ['谁的差评最多', '/user/statistics/sh/rate'],
  ['好评率部门排行榜', '/user/statistics/rank/evaluation'],
  ['问题原因分类 责任归因', '/user/statistics/sh/workHint'],
  ['撤单原因统计', '/user/statistics/sh/revoke'],
  ['派单错误率排行', '/user/statistics/rank/weight'],
  ['超时工单有哪些', '/work/timeout/list'],
  ['连图任务看板任务列表', '/admin/ttask/list'],
  ['任务看板整体概览', '/admin/ttask/dashboard/statistics'],
  ['连图工作台待办', '/workbench/todo/list'],
  ['员工花名册按姓名查', '/admin-api/hr/employee/page'],
  ['当前用户我的档案', '/admin-api/hr/employee/current'],
  ['需求管理概览', '/admin-api/business/work/requirement/overview'],
  ['总裁驾驶舱大盘', '/admin-api/business/executive-cockpit/overview'],
];

console.log('stats:', stats());
console.log('');

let top1 = 0, top3 = 0;
const misses = [];
for (const [q, expected] of CASES) {
  const hits = await search(q, { k: 5 });
  const paths = hits.map((h) => h.path);
  const rank = paths.indexOf(expected);
  if (rank === 0) top1 += 1;
  if (rank >= 0 && rank <= 2) top3 += 1;
  if (rank !== 0) {
    misses.push({
      q,
      expected,
      got_rank: rank,
      top3: paths.slice(0, 3),
      top3_scores: hits.slice(0, 3).map((h) => h.score),
    });
  }
}

console.log(`top-1: ${top1}/${CASES.length} (${((top1 / CASES.length) * 100).toFixed(1)}%)`);
console.log(`top-3: ${top3}/${CASES.length} (${((top3 / CASES.length) * 100).toFixed(1)}%)`);
console.log('');
if (misses.length > 0) {
  console.log('=== top-1 misses ===');
  for (const m of misses) {
    console.log(`  Q: ${m.q}`);
    console.log(`     expected(rank=${m.got_rank}): ${m.expected}`);
    m.top3.forEach((p, i) => console.log(`     ${i + 1}. [${m.top3_scores[i]}] ${p}`));
  }
}
