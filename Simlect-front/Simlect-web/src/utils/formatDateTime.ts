
// [zh] 导出模块成员
export function formatDisplayDateTime(val?: string | number | null): string {
  // [zh] 条件判断 if
  if (val === null || val === undefined || val === '') return '';
  // [zh] 条件判断 if
  if (typeof val === 'string') {
    // [zh] 声明变量 `normalized`
    const normalized = val.replace('T', ' ').trim();
    // [zh] 条件判断 if
    if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(normalized)) {
      // [zh] return 返回
      return normalized;
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] 声明变量 `d`
  const d = new Date(val);
  // [zh] 条件判断 if
  if (Number.isNaN(d.getTime())) {
    // [zh] return 返回
    return String(val).replace('T', ' ').slice(0, 19);
  // [zh] 语法符号
  }
  // [zh] 声明变量 `pad`
  const pad = (n: number) => String(n).padStart(2, '0');
  // [zh] return 返回
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
// [zh] 语法符号
}
