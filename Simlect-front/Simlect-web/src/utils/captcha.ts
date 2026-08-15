
// [zh] 导出模块成员
export function formatCaptchaSrc(checkCode?: string): string {
  // [zh] 条件判断 if
  if (!checkCode) return '';
  // [zh] 条件判断 if
  if (checkCode.startsWith('data:')) return checkCode;
  // [zh] return 返回
  return `data:image/png;base64,${checkCode}`;
// [zh] 语法符号
}

// [zh] 导出模块成员
export interface CheckCodeResult {
  // [zh] 语句
  checkCode: string;
  // [zh] 语句
  checkCodeKey: string;
// [zh] 语法符号
}
