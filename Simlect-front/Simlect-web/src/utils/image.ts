
// [zh] 导出模块成员
export const IMAGE_BASE = '/api/file/getResource?sourceName=';

// [zh] 声明变量 `THUMB_SUFFIX`
const THUMB_SUFFIX = '_thumbnail';

// [zh] 声明变量 `INVALID_IMAGE_PATH`
const INVALID_IMAGE_PATH = /^\[object Object\]$/i;

// [zh] 导出模块成员
export function extractUploadPath(value: unknown): string {
  // [zh] 条件判断 if
  if (typeof value === 'string') {
    // [zh] 声明变量 `trimmed`
    const trimmed = value.trim();
    // [zh] 条件判断 if
    if (!trimmed || INVALID_IMAGE_PATH.test(trimmed)) return '';
    // [zh] return 返回
    return trimmed;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (value && typeof value === 'object') {
    // [zh] 声明变量 `record`
    const record = value as Record<string, unknown>;
    // [zh] 条件判断 if
    if (typeof record.path === 'string') {
      // [zh] return 返回
      return extractUploadPath(record.path);
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return '';
// [zh] 语法符号
}

// [zh] 导出模块成员
export function joinImagePaths(paths: unknown[]): string {
  // [zh] return 返回
  return paths.map(extractUploadPath).filter(Boolean).join(',');
// [zh] 语法符号
}

// [zh] 导出模块成员
export function splitImagePaths(raw?: string | null): string[] {
  // [zh] 条件判断 if
  if (!raw) return [];
  // [zh] return 返回
  return String(raw)
    // [zh] TypeScript/JS 语句：`.split(',')`
    .split(',')
    // [zh] TypeScript/JS 语句：`.map((item) => extractUploadPath(item))`
    .map((item) => extractUploadPath(item))
    // [zh] 语句
    .filter(Boolean);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function resolveImageUrl(
  // [zh] TypeScript/JS 语句：`source?: string | null,`
  source?: string | null,
  // [zh] TypeScript/JS 语句：`options?: { useThumbnail?: boolean; index?: number }`
  options?: { useThumbnail?: boolean; index?: number }
// [zh] 代码块开始
): string {
  // [zh] 条件判断 if
  if (!source) return '';
  // [zh] 声明变量 `raw`
  const raw = String(source).trim();
  // [zh] 条件判断 if
  if (!raw) return '';
  // [zh] 条件判断 if
  if (raw.startsWith('http://') || raw.startsWith('https://')) {
    // [zh] return 返回
    return raw.split(',')[0].trim();
  // [zh] 语法符号
  }

  // [zh] 声明变量 `parts`
  const parts = raw.split(',').map((s) => s.trim()).filter(Boolean);
  // [zh] 声明变量 `path`
  let path = parts[options?.index ?? 0] ?? '';
  // [zh] 条件判断 if
  if (!path) return '';

  // [zh] 声明变量 `useThumbnail`
  const useThumbnail = options?.useThumbnail !== false;
  // [zh] 条件判断 if
  if (!useThumbnail && path.includes(THUMB_SUFFIX)) {
    // [zh] 语句
    path = path.replace(THUMB_SUFFIX, '');
  // [zh] 语法符号
  }

  // [zh] return 返回
  return `${IMAGE_BASE}${path}`;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function resolveAvatarUrl(avatar?: string | null): string {
  // [zh] 条件判断 if
  if (!avatar || !String(avatar).trim()) return '';
  // [zh] 声明变量 `av`
  const av = String(avatar).trim();
  // [zh] 条件判断 if
  if (av === 'avatar.png') return '';
  // [zh] 条件判断 if
  if (av.startsWith('http://') || av.startsWith('https://')) return av;
  // [zh] return 返回
  return `${IMAGE_BASE}${av}`;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function pickProductCover(product: Record<string, any>): string {
  // [zh] return 返回
  return product?.cover || product?.productCover || product?.image || '';
// [zh] 语法符号
}
