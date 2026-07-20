#!/usr/bin/env node
// 扫 ordersys Java Controllers + VO 提取真实参数描述
// 之前的 scan-ordersys-api.mjs 只扫了 Vue 前端 (order-ui/src/api/*.js)，
// 只拿 URL 没有参数 — 导致 agent 面对 /list 类接口时不知道有 pageSize 之类
//
// 本脚本源码级读 Java：
//   1. 递归扫 ordersys/**/*Controller.java
//   2. 找 @GetMapping / @PostMapping + 方法签名 (Xxx reqVO) 拿 VO 类名
//   3. 读同项目里的 XxxReqVO.java，抓字段 @Schema(description=...)
//   4. 特殊处理 PageParam 继承 → 追加 pageNo/pageSize
//
// 输出：generated/ordersys-java-api.json
// 由 build-api-index.mjs 消费，覆盖原来的 Vue-based 参数
import { readdirSync, readFileSync, mkdirSync, writeFileSync, statSync } from 'node:fs';
import { basename, dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const agentRoot = resolve(scriptDir, '..');
const ORDERSYS_ROOT =
  process.env.ORDERSYS_ROOT ||
  resolve(agentRoot, '..', '..', '..', 'ordersys');
const outputPath = resolve(agentRoot, 'generated', 'ordersys-java-api.json');

// 只读 GET 类端点（跟旧扫描逻辑一致，写方法都过滤掉）
const BLOCKED_PATH_RE =
  /(^|[-/])(create|update|delete|remove|submit|approve|reject|sync|import|export|upload|login|logout|auth|oauth|oauth2|authorize|token|password|captcha|callback|save|assign|transfer|start|stop|enable|disable|reset|send|bind|unbind|pay|refund|write-off|settle|confirm|edit|batch-set)([-/]|$)/i;
const BLOCKED_ACTION_SUFFIX_RE = /\/(add|do[A-Z][a-z]+)$/;

function walk(dir, out = []) {
  let entries;
  try { entries = readdirSync(dir, { withFileTypes: true }); } catch { return out; }
  for (const e of entries) {
    if (e.name === 'target' || e.name === 'node_modules' || e.name.startsWith('.')) continue;
    const full = join(dir, e.name);
    if (e.isDirectory()) walk(full, out);
    else if (e.isFile() && e.name.endsWith('.java')) out.push(full);
  }
  return out;
}

// 收集所有 Java 文件，按类名索引
const allJavaFiles = walk(ORDERSYS_ROOT);
console.error(`[scan] ${allJavaFiles.length} Java files under ${ORDERSYS_ROOT}`);

const classNameToPath = new Map(); // "XxxReqVO" -> "/path/to/XxxReqVO.java"
for (const p of allJavaFiles) {
  const name = basename(p, '.java');
  classNameToPath.set(name, p);
}

// 缓存 VO 类的字段解析
const voCache = new Map();

// 简单 @Schema(description="…") 或 @Schema(description = "xxx", …) 抓取
function extractSchemaDesc(annotationBlock) {
  const m = annotationBlock.match(/description\s*=\s*"((?:[^"\\]|\\.)*)"/);
  return m ? m[1].replace(/\\"/g, '"') : undefined;
}

// PageParam 或其他继承 —— 读父类，追加公共字段
const KNOWN_PAGE_PARENTS = new Set(['PageParam', 'PageParamVO']);

// Ruoyi 分页默认继承 BaseEntity 或 PageParam —— 所有 /list 类接口都支持分页
const RUOYI_INHERIT_PAGE = new Set(['PageParam', 'BaseEntity', 'PageParamVO', 'PageQuery']);
// 追加的公共分页字段（Ruoyi 用 pageNum/pageSize 不是 pageNo）
const RUOYI_PAGE_FIELDS = [
  { name: 'pageNum', type: 'int', notes: '分页页码 (默认 1)', source: 'Ruoyi 通用' },
  { name: 'pageSize', type: 'int', notes: '分页大小 (强烈建议 ≤30；只取 total 传 1)', source: 'Ruoyi 通用' },
  { name: 'orderByColumn', type: 'String', notes: '排序列名 (可选)', source: 'Ruoyi 通用' },
  { name: 'isAsc', type: 'String', notes: '升序 asc / 降序 desc', source: 'Ruoyi 通用' },
];

// PageParam Java(kyx-foundation 风格) 用 pageNo
const KYX_PAGE_FIELDS = [
  { name: 'pageNo', type: 'int', notes: '分页页码 (默认 1)', source: 'PageParam' },
  { name: 'pageSize', type: 'int', notes: '分页大小 (推荐 ≤30)', source: 'PageParam' },
];

function parseVO(className, depth = 0) {
  if (depth > 5) return [];
  if (voCache.has(className)) return voCache.get(className);
  voCache.set(className, []); // 防循环引用
  const path = classNameToPath.get(className);
  if (!path) return [];
  const src = safeRead(path);
  if (!src) return [];

  const fields = [];

  // 处理父类继承
  const extendsMatch = src.match(/class\s+\w+\s+extends\s+(\w+)/);
  if (extendsMatch) {
    const parent = extendsMatch[1];
    if (KNOWN_PAGE_PARENTS.has(parent)) {
      fields.push(...KYX_PAGE_FIELDS);
    } else if (RUOYI_INHERIT_PAGE.has(parent)) {
      fields.push(...RUOYI_PAGE_FIELDS);
    } else {
      // 递归解析父类字段
      const parentFields = parseVO(parent, depth + 1);
      fields.push(...parentFields);
    }
  }

  // 抓所有 private 字段 —— Ruoyi 用 @ApiModelProperty 而不是 @Schema
  const fieldRe =
    /((?:@Schema\s*\([^)]*\)|@ApiModelProperty\s*\([^)]*\)|\/\*\*[\s\S]*?\*\/)\s*)?(?:@\w+\s*(?:\([^)]*\))?\s*)*private\s+([\w<>\[\],\s?]+?)\s+(\w+)\s*[;=]/g;
  let m;
  while ((m = fieldRe.exec(src)) !== null) {
    const annBlock = m[1] || '';
    const type = m[2].trim();
    const name = m[3];
    if (name === 'serialVersionUID') continue;
    if (fields.some((f) => f.name === name)) continue;

    let notes = extractSchemaDesc(annBlock);
    if (!notes) {
      // @ApiModelProperty("xxx") 或 @ApiModelProperty(value="xxx")
      const apiMatch = annBlock.match(/@ApiModelProperty\s*\(\s*(?:value\s*=\s*)?"([^"]*)"/);
      if (apiMatch) notes = apiMatch[1];
    }
    if (!notes) {
      // Javadoc /** 描述 */
      const jd = annBlock.match(/\/\*\*([\s\S]*?)\*\//);
      if (jd) {
        const line = jd[1]
          .split('\n')
          .map((l) => l.replace(/^\s*\*\s?/, '').trim())
          .find((l) => l && !l.startsWith('@'));
        if (line) notes = line;
      }
    }
    fields.push({ name, type: type.replace(/\s+/g, ''), notes });
  }
  voCache.set(className, fields);
  return fields;
}

function safeRead(p) {
  try { return readFileSync(p, 'utf8'); } catch { return null; }
}

// 从 @RequestMapping("/xxx") 或类级 @RequestMapping 拿 base
function extractClassBase(src) {
  const m = src.match(/@RequestMapping\s*\(\s*(?:value\s*=\s*)?"([^"]+)"/);
  return m ? m[1] : '';
}

// 抓所有方法：@GetMapping("/list") + method(...)
// Ruoyi 风格：purpose 在 @GetMapping 上方的 Javadoc `/** ... */`
function extractMethods(src, filePath) {
  const methods = [];
  const mappingRe =
    /@(Get|Post|Put|Delete)Mapping\s*\(\s*(?:value\s*=\s*)?"([^"]+)"[^)]*\)([\s\S]*?)public\s+[\w<>\[\],\s?]+\s+(\w+)\s*\(([\s\S]*?)\)/g;
  let m;
  while ((m = mappingRe.exec(src)) !== null) {
    const httpMethod = m[1].toUpperCase();
    if (httpMethod !== 'GET') continue;
    const subPath = m[2];
    const between = m[3];
    const methodName = m[4];
    const paramsText = m[5];

    // purpose 优先级：
    //   1. @Operation(summary="…") — Ruoyi 老风格没有，但保留
    //   2. Ruoyi Javadoc /** 上方注释 */
    //   3. 方法名裸称呼
    let purpose = between.match(/@Operation\s*\([^)]*summary\s*=\s*"([^"]*)"/)?.[1];
    if (!purpose) {
      // Javadoc 上方（可能被 @PreAuthorize / @PostAuthorize 等隔开），
      // 拿 @GetMapping 前 800 字符里最后一个 /** ... */
      const beforePos = m.index;
      const chunk = src.substring(Math.max(0, beforePos - 800), beforePos);
      const allJd = [...chunk.matchAll(/\/\*\*([\s\S]*?)\*\//g)];
      const lastJd = allJd[allJd.length - 1];
      if (lastJd) {
        const lines = lastJd[1]
          .split('\n')
          .map((l) => l.replace(/^\s*\*\s?/, '').trim())
          .filter((l) => l && !l.startsWith('@'));
        if (lines.length > 0) purpose = lines[0];
      }
    }

    methods.push({ httpMethod, subPath, methodName, paramsText, purpose, filePath });
  }
  return methods;
}

// 从方法签名参数里找业务 VO 类型
// Ruoyi ordersys 风格：直接用 domain 实体（Community community）
// 或者 XxxReqVO/XxxQuery 之类
function extractParamVO(paramsText) {
  // 排除框架类型
  const skip = /^(Long|Integer|String|Boolean|Double|Float|Date|LocalDate|LocalDateTime|HttpServletResponse|HttpServletRequest|MultipartFile|List|Map)$/;
  // 找每个参数的类型 —— 忽略 @xxx 注解，抓类型词
  const paramRe = /(?:^|,)\s*(?:@\w+\s*(?:\([^)]*\))?\s*)*([A-Z]\w+(?:<[^>]+>)?)\s+\w+/g;
  let m;
  while ((m = paramRe.exec(paramsText)) !== null) {
    let type = m[1].replace(/<[^>]+>/g, ''); // 去掉泛型
    if (skip.test(type)) continue;
    return type;
  }
  return null;
}

// 处理 @RequestParam("name") 单参数
function extractRequestParams(paramsText) {
  const out = [];
  const re = /@RequestParam\s*(?:\(\s*(?:value\s*=\s*)?"([^"]+)"[^)]*\))?\s+[\w<>\[\],?]+\s+(\w+)/g;
  let m;
  while ((m = re.exec(paramsText)) !== null) {
    out.push({ name: m[1] || m[2], type: 'query' });
  }
  return out;
}

// 主流程
const endpoints = [];
const controllerFiles = allJavaFiles.filter((p) => /Controller\.java$/.test(p));
console.error(`[scan] ${controllerFiles.length} Controllers`);

for (const filePath of controllerFiles) {
  const src = safeRead(filePath);
  if (!src) continue;

  const classBase = extractClassBase(src);
  const methods = extractMethods(src, filePath);

  for (const m of methods) {
    let fullPath = classBase.endsWith('/') || m.subPath.startsWith('/')
      ? `${classBase}${m.subPath}`
      : `${classBase}/${m.subPath}`;
    fullPath = fullPath.replace(/\/+/g, '/');
    if (BLOCKED_PATH_RE.test(fullPath)) continue;
    if (BLOCKED_ACTION_SUFFIX_RE.test(fullPath)) continue;

    // 参数：优先看 VO，否则看 @RequestParam
    let params = [];
    const voClass = extractParamVO(m.paramsText);
    if (voClass) params = parseVO(voClass);
    if (params.length === 0) {
      params = extractRequestParams(m.paramsText);
    }

    endpoints.push({
      path: fullPath,
      method: m.httpMethod,
      purpose: m.purpose,
      params,
      source: filePath,
      voClass: voClass || null,
    });
  }
}

mkdirSync(dirname(outputPath), { recursive: true });
writeFileSync(outputPath, JSON.stringify({
  meta: {
    generatedAt: new Date().toISOString(),
    sourceRoot: ORDERSYS_ROOT,
    controllers: controllerFiles.length,
    endpoints: endpoints.length,
  },
  endpoints,
}, null, 2));
console.error(`[scan] wrote ${endpoints.length} endpoints → ${outputPath}`);
