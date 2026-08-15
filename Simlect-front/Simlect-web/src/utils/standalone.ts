
// [zh] 导出模块成员
export const isStandaloneDisplay = () => {
  // [zh] 条件判断 if
  if (typeof window === 'undefined') return false;
  // [zh] return 返回
  return (
    // [zh] TypeScript/JS 语句：`window.matchMedia('(display-mode: standalone)').matches`
    window.matchMedia('(display-mode: standalone)').matches ||
    
    // [zh] TypeScript/JS 语句：`(window.navigator as Navigator & { standalone?: boolean`
    (window.navigator as Navigator & { standalone?: boolean }).standalone === true
  // [zh] 语句
  );
// [zh] 语句
};
