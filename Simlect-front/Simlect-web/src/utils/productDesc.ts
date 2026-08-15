// [zh] 导入 `@/utils/image`
import { IMAGE_BASE } from '@/utils/image';

// [zh] 导出模块成员
export function normalizeProductDesc(desc?: string | null): string {
  // [zh] 条件判断 if
  if (!desc) return '';

  // [zh] 声明变量 `text`
  let text = String(desc).trim();

  // [zh] 代码块开始
  text = text.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_match, alt, url) => {
    // [zh] 声明变量 `raw`
    const raw = String(url).trim();
    // [zh] 条件判断 if
    if (!raw) return '';

    // [zh] 条件判断 if
    if (raw.startsWith('http://') || raw.startsWith('https://')) {
      // [zh] return 返回
      return `![${alt}](${raw})`;
    // [zh] 语法符号
    }

    // [zh] 条件判断 if
    if (raw.includes('/api/file/getResource')) {
      // [zh] return 返回
      return `![${alt}](${raw.startsWith('/') ? raw : `/${raw}`})`;
    // [zh] 语法符号
    }

    // [zh] 条件判断 if
    if (raw.startsWith('sourceName=')) {
      // [zh] return 返回
      return `![${alt}](${IMAGE_BASE}${raw.replace(/^sourceName=/, '')})`;
    // [zh] 语法符号
    }

    // [zh] return 返回
    return `![${alt}](${IMAGE_BASE}${raw})`;
  // [zh] 语句
  });

  // [zh] 代码块开始
  text = text.replace(/^(202601\/[^\s\n]+\.(?:png|jpg|jpeg|gif|webp))$/gim, (path) => {
    // [zh] return 返回
    return `![](${IMAGE_BASE}${path})`;
  // [zh] 语句
  });

  // [zh] return 返回
  return text;
// [zh] 语法符号
}
