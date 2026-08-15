
// [zh] 导出模块成员
export function resolveSafeRedirect(raw: unknown, fallback = '/'): string {
  // [zh] 声明变量 `path`
  const path = String(raw ?? '').trim();
  // [zh] 条件判断 if
  if (!path.startsWith('/') || path.startsWith('//')) return fallback;
  // [zh] 条件判断 if
  if (path.startsWith('/login') || path.startsWith('/register')) return fallback;
  // [zh] return 返回
  return path;
// [zh] 语法符号
}
