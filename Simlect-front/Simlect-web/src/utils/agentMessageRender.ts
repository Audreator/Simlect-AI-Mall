// [zh] 导入 `markdown-it`
import MarkdownIt from 'markdown-it';
// [zh] 导入 `@/utils/productDesc`
import { normalizeProductDesc } from '@/utils/productDesc';

// [zh] 声明变量 `md`
const md = new MarkdownIt({
  // [zh] TypeScript/JS 语句：`html: false,`
  html: false,
  // [zh] TypeScript/JS 语句：`breaks: true,`
  breaks: true,
  // [zh] TypeScript/JS 语句：`linkify: true`
  linkify: true
// [zh] 语句
});

// [zh] 声明变量 `TABLE_BLOCK_RE`
const TABLE_BLOCK_RE = /<table[\s\S]*?<\/table>/i;

// [zh] 声明变量 `escapeHtml`
const escapeHtml = (text: string) =>
  // [zh] TypeScript/JS 语句：`text`
  text
    // [zh] TypeScript/JS 语句：`.replace(/&/g, '&amp;')`
    .replace(/&/g, '&amp;')
    // [zh] TypeScript/JS 语句：`.replace(/</g, '&lt;')`
    .replace(/</g, '&lt;')
    // [zh] TypeScript/JS 语句：`.replace(/>/g, '&gt;')`
    .replace(/>/g, '&gt;')
    // [zh] 语句
    .replace(/"/g, '&quot;');

// [zh] 导出模块成员
export const sanitizeLogisticsTableHtml = (raw: string): string => {
  // [zh] 声明变量 `match`
  const match = raw.match(TABLE_BLOCK_RE);
  // [zh] 条件判断 if
  if (!match) return '';

  // [zh] 声明变量 `doc`
  const doc = new DOMParser().parseFromString(match[0], 'text/html');
  // [zh] 声明变量 `table`
  const table = doc.querySelector('table');
  // [zh] 条件判断 if
  if (!table) return '';

  // [zh] 声明变量 `parts`
  const parts: string[] = ['<table>'];
  // [zh] 代码块开始
  table.querySelectorAll('tr').forEach((row) => {
    // [zh] 语句
    parts.push('<tr>');
    // [zh] 代码块开始
    row.querySelectorAll('th, td').forEach((cell) => {
      // [zh] 声明变量 `tag`
      const tag = cell.tagName.toLowerCase();
      // [zh] 声明变量 `text`
      const text = (cell.textContent || '').trim();
      // [zh] 语句
      parts.push(`<${tag}>${escapeHtml(text)}</${tag}>`);
    // [zh] 语句
    });
    // [zh] 语句
    parts.push('</tr>');
  // [zh] 语句
  });
  // [zh] 语句
  parts.push('</table>');
  // [zh] return 返回
  return parts.join('');
// [zh] 语句
};

// [zh] 导出模块成员
export const renderAgentMessageHtml = (content?: string | null): string => {
  // [zh] 声明变量 `normalized`
  const normalized = normalizeProductDesc(content);
  // [zh] 条件判断 if
  if (!normalized) return '';

  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `tableMatch`
    const tableMatch = normalized.match(TABLE_BLOCK_RE);
    // [zh] 条件判断 if
    if (!tableMatch || tableMatch.index == null) {
      // [zh] return 返回
      return md.render(normalized);
    // [zh] 语法符号
    }

    // [zh] 声明变量 `start`
    const start = tableMatch.index;
    // [zh] 声明变量 `end`
    const end = start + tableMatch[0].length;
    // [zh] 声明变量 `before`
    const before = normalized.slice(0, start).trim();
    // [zh] 声明变量 `after`
    const after = normalized.slice(end).trim();
    // [zh] 声明变量 `tableHtml`
    const tableHtml = sanitizeLogisticsTableHtml(tableMatch[0]);

    // [zh] 声明变量 `html`
    let html = '';
    // [zh] 条件判断 if
    if (before) html += md.render(before);
    // [zh] 条件判断 if
    if (tableHtml) html += tableHtml;
    // [zh] 条件判断 if
    if (after) html += md.render(after);
    // [zh] return 返回
    return html;
  // [zh] 代码块开始
  } catch (err) {
    // [zh] 语句
    console.warn('客服消息渲染失败，已降级为纯文本', err);
    // [zh] return 返回
    return md.render(normalized.replace(/<[^>]+>/g, ''));
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 导出模块成员
export const containsAgentTable = (content?: string | null) => {
  // [zh] 声明变量 `normalized`
  const normalized = normalizeProductDesc(content);
  // [zh] return 返回
  return !!normalized && TABLE_BLOCK_RE.test(normalized);
// [zh] 语句
};

// [zh] 导出模块成员
export const stripEmbeddedProductJson = (content?: string | null) => {
  // [zh] 条件判断 if
  if (!content) return '';
  // [zh] 声明变量 `text`
  let text = String(content);
  // [zh] 声明变量 `marker`
  const marker = '"PRODUCT_SEARCH_RESULT"';
  // [zh] 代码块开始
  while (text.includes(marker)) {
    // [zh] 声明变量 `idx`
    const idx = text.indexOf(marker);
    // [zh] 声明变量 `start`
    const start = text.lastIndexOf('{', idx);
    // [zh] 条件判断 if
    if (start < 0) {
      // [zh] 语句
      text = text.replace(marker, '');
      // [zh] 语句
      break;
    // [zh] 语法符号
    }
    // [zh] 声明变量 `depth`
    let depth = 0;
    // [zh] 声明变量 `end`
    let end = -1;
    // [zh] 声明变量 `inString`
    let inString = false;
    // [zh] 声明变量 `escape`
    let escape = false;
    // [zh] for 循环
    for (let i = start; i < text.length; i += 1) {
      // [zh] 声明变量 `ch`
      const ch = text[i];
      // [zh] 条件判断 if
      if (inString) {
        // [zh] 条件判断 if
        if (escape) escape = false;
        // [zh] else 分支
        else if (ch === '\\') escape = true;
        // [zh] else 分支
        else if (ch === '"') inString = false;
        // [zh] 语句
        continue;
      // [zh] 语法符号
      }
      // [zh] 条件判断 if
      if (ch === '"') inString = true;
      // [zh] else 分支
      else if (ch === '{') depth += 1;
      // [zh] else 分支
      else if (ch === '}') {
        // [zh] 语句
        depth -= 1;
        // [zh] 条件判断 if
        if (depth === 0) {
          // [zh] 语句
          end = i + 1;
          // [zh] 语句
          break;
        // [zh] 语法符号
        }
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (end < 0) {
      // [zh] 语句
      text = text.slice(0, start).trimEnd();
      // [zh] 语句
      break;
    // [zh] 语法符号
    }
    // [zh] 声明变量 `before`
    let before = text.slice(0, start).trimEnd();
    // [zh] 声明变量 `after`
    const after = text.slice(end).trimStart();
    // [zh] 条件判断 if
    if (before.endsWith(':') || before.endsWith('：')) {
      // [zh] 语句
      before = before.slice(0, -1).trimEnd();
    // [zh] 语法符号
    }
    // [zh] 语句
    text = `${before}${after}`.trim();
  // [zh] 语法符号
  }

  // Strip bare [{"productId":"..."}, ...] echoed by the model.
  // [zh] for 循环
  for (let n = 0; n < 8; n += 1) {
    // [zh] 声明变量 `start`
    const start = text.indexOf('[');
    // [zh] 条件判断 if
    if (start < 0) break;
    // [zh] 声明变量 `depth`
    let depth = 0;
    // [zh] 声明变量 `end`
    let end = -1;
    // [zh] 声明变量 `inString`
    let inString = false;
    // [zh] 声明变量 `escape`
    let escape = false;
    // [zh] for 循环
    for (let i = start; i < text.length; i += 1) {
      // [zh] 声明变量 `ch`
      const ch = text[i];
      // [zh] 条件判断 if
      if (inString) {
        // [zh] 条件判断 if
        if (escape) escape = false;
        // [zh] else 分支
        else if (ch === '\\') escape = true;
        // [zh] else 分支
        else if (ch === '"') inString = false;
        // [zh] 语句
        continue;
      // [zh] 语法符号
      }
      // [zh] 条件判断 if
      if (ch === '"') inString = true;
      // [zh] else 分支
      else if (ch === '[') depth += 1;
      // [zh] else 分支
      else if (ch === ']') {
        // [zh] 语句
        depth -= 1;
        // [zh] 条件判断 if
        if (depth === 0) {
          // [zh] 语句
          end = i + 1;
          // [zh] 语句
          break;
        // [zh] 语法符号
        }
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (end < 0) break;
    // [zh] 声明变量 `blob`
    const blob = text.slice(start, end);
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `parsed`
      const parsed = JSON.parse(blob);
      // [zh] 声明变量 `isProductArr`
      const isProductArr =
        // [zh] TypeScript/JS 语句：`Array.isArray(parsed) &&`
        Array.isArray(parsed) &&
        // [zh] TypeScript/JS 语句：`parsed.length > 0 &&`
        parsed.length > 0 &&
        // [zh] TypeScript/JS 语句：`parsed.every(`
        parsed.every(
          // [zh] TypeScript/JS 语句：`(item) =>`
          (item) =>
            // [zh] TypeScript/JS 语句：`item &&`
            item &&
            // [zh] TypeScript/JS 语句：`typeof item === 'object' &&`
            typeof item === 'object' &&
            // [zh] TypeScript/JS 语句：`!Array.isArray(item) &&`
            !Array.isArray(item) &&
            // [zh] TypeScript/JS 语句：`(item.productId != null || item.product_id != null)`
            (item.productId != null || item.product_id != null)
        // [zh] 语句
        );
      // [zh] 条件判断 if
      if (!isProductArr) break;
      // [zh] 声明变量 `before`
      let before = text.slice(0, start).trimEnd();
      // [zh] 声明变量 `after`
      const after = text.slice(end).trimStart();
      // [zh] 条件判断 if
      if (before.endsWith(':') || before.endsWith('：')) {
        // [zh] 语句
        before = before.slice(0, -1).trimEnd();
      // [zh] 语法符号
      }
      // [zh] 语句
      text = `${before}${after}`.trim();
    // [zh] 代码块开始
    } catch {
      // [zh] 语句
      break;
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return text.trim();
// [zh] 语句
};

// [zh] 导出模块成员
export const cleanAgentActionStreamText = (content?: string | null) => {
  // [zh] 条件判断 if
  if (!content) return '';
  // [zh] 声明变量 `text`
  let text = stripEmbeddedProductJson(String(content))
    // [zh] TypeScript/JS 语句：`.replace(/【act_[a-f0-9]{32}】/gi, '')`
    .replace(/【act_[a-f0-9]{32}】/gi, '')
    // [zh] TypeScript/JS 语句：`.replace(/【act_[^】]+】/g, '')`
    .replace(/【act_[^】]+】/g, '')
    // [zh] TypeScript/JS 语句：`.replace(/【[^】]*(成功|失败)[^】]*】/g, '')`
    .replace(/【[^】]*(成功|失败)[^】]*】/g, '')
    // [zh] TypeScript/JS 语句：`.replace(/\n{3,}/g, '\n\n')`
    .replace(/\n{3,}/g, '\n\n')
    // [zh] 语句
    .trim();

  // [zh] 条件判断 if
  if (/【act_/.test(content)) {
    // [zh] 声明变量 `brief`
    const brief = text.split('\n').find((line) => line.trim())?.trim() || '';
    // [zh] 条件判断 if
    if (brief.length > 120 || !brief) {
      // [zh] return 返回
      return '已生成确认卡片，请在下方卡片中核对并提交。';
    // [zh] 语法符号
    }
    // [zh] return 返回
    return brief.length > 120 ? `${brief.slice(0, 120)}…` : brief;
  // [zh] 语法符号
  }
  // [zh] return 返回
  return text;
// [zh] 语句
};
