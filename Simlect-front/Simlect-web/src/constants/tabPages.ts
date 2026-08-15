
// [zh] 导出模块成员
export const PRIMARY_TAB_PATHS = ['/', '/search', '/cart', '/account'] as const;

// [zh] 导出模块成员
export type PrimaryTabPath = (typeof PRIMARY_TAB_PATHS)[number];

// [zh] 导出模块成员
export function isPrimaryTabPath(path: string) {
  // [zh] return 返回
  return (PRIMARY_TAB_PATHS as readonly string[]).includes(path);
// [zh] 语法符号
}
