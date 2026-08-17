import MarkdownIt from 'markdown-it';
import { normalizeProductDesc } from '@/utils/productDesc';

const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true
});

const TABLE_BLOCK_RE = /<table[\s\S]*?<\/table>/i;

const escapeHtml = (text: string) =>
  text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

export const sanitizeLogisticsTableHtml = (raw: string): string => {
  const match = raw.match(TABLE_BLOCK_RE);
  if (!match) return '';

  const doc = new DOMParser().parseFromString(match[0], 'text/html');
  const table = doc.querySelector('table');
  if (!table) return '';

  const parts: string[] = ['<table>'];
  table.querySelectorAll('tr').forEach((row) => {
    parts.push('<tr>');
    row.querySelectorAll('th, td').forEach((cell) => {
      const tag = cell.tagName.toLowerCase();
      const text = (cell.textContent || '').trim();
      parts.push(`<${tag}>${escapeHtml(text)}</${tag}>`);
    });
    parts.push('</tr>');
  });
  parts.push('</table>');
  return parts.join('');
};

export const renderAgentMessageHtml = (content?: string | null): string => {
  const normalized = normalizeProductDesc(content);
  if (!normalized) return '';

  try {
    const tableMatch = normalized.match(TABLE_BLOCK_RE);
    if (!tableMatch || tableMatch.index == null) {
      return md.render(normalized);
    }

    const start = tableMatch.index;
    const end = start + tableMatch[0].length;
    const before = normalized.slice(0, start).trim();
    const after = normalized.slice(end).trim();
    const tableHtml = sanitizeLogisticsTableHtml(tableMatch[0]);

    let html = '';
    if (before) html += md.render(before);
    if (tableHtml) html += tableHtml;
    if (after) html += md.render(after);
    return html;
  } catch (err) {
    console.warn('客服消息渲染失败，已降级为纯文本', err);
    return md.render(normalized.replace(/<[^>]+>/g, ''));
  }
};

export const containsAgentTable = (content?: string | null) => {
  const normalized = normalizeProductDesc(content);
  return !!normalized && TABLE_BLOCK_RE.test(normalized);
};

export const stripEmbeddedProductJson = (content?: string | null) => {
  if (!content) return '';
  let text = String(content);
  const marker = '"PRODUCT_SEARCH_RESULT"';
  while (text.includes(marker)) {
    const idx = text.indexOf(marker);
    const start = text.lastIndexOf('{', idx);
    if (start < 0) {
      text = text.replace(marker, '');
      break;
    }
    let depth = 0;
    let end = -1;
    let inString = false;
    let escape = false;
    for (let i = start; i < text.length; i += 1) {
      const ch = text[i];
      if (inString) {
        if (escape) escape = false;
        else if (ch === '\\') escape = true;
        else if (ch === '"') inString = false;
        continue;
      }
      if (ch === '"') inString = true;
      else if (ch === '{') depth += 1;
      else if (ch === '}') {
        depth -= 1;
        if (depth === 0) {
          end = i + 1;
          break;
        }
      }
    }
    if (end < 0) {
      text = text.slice(0, start).trimEnd();
      break;
    }
    let before = text.slice(0, start).trimEnd();
    const after = text.slice(end).trimStart();
    if (before.endsWith(':') || before.endsWith('：')) {
      before = before.slice(0, -1).trimEnd();
    }
    text = `${before}${after}`.trim();
  }

  // Strip bare [{"productId":"..."}, ...] echoed by the model.
  for (let n = 0; n < 8; n += 1) {
    const start = text.indexOf('[');
    if (start < 0) break;
    let depth = 0;
    let end = -1;
    let inString = false;
    let escape = false;
    for (let i = start; i < text.length; i += 1) {
      const ch = text[i];
      if (inString) {
        if (escape) escape = false;
        else if (ch === '\\') escape = true;
        else if (ch === '"') inString = false;
        continue;
      }
      if (ch === '"') inString = true;
      else if (ch === '[') depth += 1;
      else if (ch === ']') {
        depth -= 1;
        if (depth === 0) {
          end = i + 1;
          break;
        }
      }
    }
    if (end < 0) break;
    const blob = text.slice(start, end);
    try {
      const parsed = JSON.parse(blob);
      const isProductArr =
        Array.isArray(parsed) &&
        parsed.length > 0 &&
        parsed.every(
          (item) =>
            item &&
            typeof item === 'object' &&
            !Array.isArray(item) &&
            (item.productId != null || item.product_id != null)
        );
      if (!isProductArr) break;
      let before = text.slice(0, start).trimEnd();
      const after = text.slice(end).trimStart();
      if (before.endsWith(':') || before.endsWith('：')) {
        before = before.slice(0, -1).trimEnd();
      }
      text = `${before}${after}`.trim();
    } catch {
      break;
    }
  }
  return text.trim();
};

export const cleanAgentActionStreamText = (content?: string | null) => {
  if (!content) return '';
  let text = stripEmbeddedProductJson(String(content))
    .replace(/【act_[a-f0-9]{32}】/gi, '')
    .replace(/【act_[^】]+】/g, '')
    .replace(/【[^】]*(成功|失败)[^】]*】/g, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim();

  if (/【act_/.test(content)) {
    const brief = text.split('\n').find((line) => line.trim())?.trim() || '';
    if (brief.length > 120 || !brief) {
      return '已生成确认卡片，请在下方卡片中核对并提交。';
    }
    return brief.length > 120 ? `${brief.slice(0, 120)}…` : brief;
  }
  return text;
};
