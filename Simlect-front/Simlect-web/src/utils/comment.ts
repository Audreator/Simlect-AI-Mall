
// [zh] 导出模块成员
export type CommentLevel = '' | 'good' | 'medium' | 'bad';

// [zh] 导出模块成员
export const matchCommentLevel = (star: unknown, level: CommentLevel): boolean => {
  // [zh] 条件判断 if
  if (!level) return true;
  // [zh] 声明变量 `s`
  const s = Number(star);
  // [zh] 条件判断 if
  if (!Number.isFinite(s)) return false;
  // [zh] 条件判断 if
  if (level === 'good') return s === 5;
  // [zh] 条件判断 if
  if (level === 'medium') return s >= 3 && s <= 4;
  // [zh] 条件判断 if
  if (level === 'bad') return s >= 1 && s <= 2;
  // [zh] return 返回
  return true;
// [zh] 语句
};


// [zh] 导出模块成员
export const maskCommenterName = (name?: string | null): string => {
  // [zh] 声明变量 `raw`
  const raw = String(name ?? '').trim();
  // [zh] 声明变量 `stars`
  const stars = '*****';
  // [zh] 条件判断 if
  if (!raw) return `匿${stars}户`;
  // [zh] 声明变量 `chars`
  const chars = Array.from(raw); 
  // [zh] 条件判断 if
  if (chars.length === 1) return `${chars[0]}${stars}`;
  // [zh] return 返回
  return `${chars[0]}${stars}${chars[chars.length - 1]}`;
// [zh] 语句
};
