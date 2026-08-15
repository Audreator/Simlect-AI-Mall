

// [zh] 声明变量 `INVALID`
const INVALID = /^\[object Object\]$/i;

// [zh] 导出模块成员
export function normalizeCommentImagePath(value: unknown): string {
  // [zh] 条件判断 if
  if (typeof value === 'string') {
    // [zh] 声明变量 `trimmed`
    const trimmed = value.trim();
    // [zh] 条件判断 if
    if (!trimmed || INVALID.test(trimmed)) return '';
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
      return normalizeCommentImagePath(record.path);
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return '';
// [zh] 语法符号
}

// [zh] 导出模块成员
export function serializeCommentImagePaths(values: readonly unknown[]): string {
  // [zh] return 返回
  return values.map(normalizeCommentImagePath).filter(Boolean).join(',');
// [zh] 语法符号
}
