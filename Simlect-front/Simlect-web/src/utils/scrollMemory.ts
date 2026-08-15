// [zh] 导出模块成员
export const SCROLL_MEMORY_PREFIX = 'scroll_pos:';

// [zh] 导出模块成员
export type ScrollTarget = 'window' | string;

// [zh] 导出模块成员
export interface ScrollMemoryRecord {
  // [zh] 语句
  top: number;
  // [zh] 语句
  target: ScrollTarget;
// [zh] 语法符号
}

// [zh] 声明变量 `INTERNAL_SCROLL_SELECTORS`
const INTERNAL_SCROLL_SELECTORS = [
  // [zh] TypeScript/JS 语句：`'.orders-body',`
  '.orders-body',
  // [zh] TypeScript/JS 语句：`'.coupon-list-scroll',`
  '.coupon-list-scroll',
  // [zh] TypeScript/JS 语句：`'.coupon-scroll',`
  '.coupon-scroll',
  // [zh] TypeScript/JS 语句：`'.comment-scroll',`
  '.comment-scroll',
  // [zh] TypeScript/JS 语句：`'.footprint-scroll',`
  '.footprint-scroll',
  // [zh] TypeScript/JS 语句：`'.wishlist-scroll',`
  '.wishlist-scroll',
  // [zh] TypeScript/JS 语句：`'.productSort .conter',`
  '.productSort .conter',
  // [zh] TypeScript/JS 语句：`'.productSort .aside'`
  '.productSort .aside'
// [zh] 语句
] as const;

// [zh] 声明变量 `isScrollable`
const isScrollable = (el: HTMLElement) => {
  // [zh] 声明变量 `style`
  const style = window.getComputedStyle(el);
  // [zh] return 返回
  return (
    // [zh] TypeScript/JS 语句：`(style.overflowY === 'auto' || style.overflowY === 'scr`
    (style.overflowY === 'auto' || style.overflowY === 'scroll') &&
    // [zh] TypeScript/JS 语句：`el.scrollHeight > el.clientHeight + 1`
    el.scrollHeight > el.clientHeight + 1
  // [zh] 语句
  );
// [zh] 语句
};


// [zh] 导出模块成员
export function captureScrollPosition(): ScrollMemoryRecord {
  // [zh] 声明变量 `best`
  let best: ScrollMemoryRecord | null = null;

  // [zh] for 循环
  for (const selector of INTERNAL_SCROLL_SELECTORS) {
    // [zh] 声明变量 `el`
    const el = document.querySelector(selector) as HTMLElement | null;
    // [zh] 条件判断 if
    if (!el || !isScrollable(el)) continue;
    // [zh] 声明变量 `record`
    const record = { top: el.scrollTop, target: selector };
    // [zh] 条件判断 if
    if (!best || record.top > best.top) best = record;
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (best) return best;

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`top: window.scrollY || document.documentElement.scrollT`
    top: window.scrollY || document.documentElement.scrollTop || 0,
    // [zh] TypeScript/JS 语句：`target: 'window'`
    target: 'window'
  // [zh] 语句
  };
// [zh] 语法符号
}

// [zh] 导出模块成员
export function saveScrollForPath(fullPath: string, record?: ScrollMemoryRecord) {
  // [zh] 声明变量 `data`
  const data = record ?? captureScrollPosition();
  // [zh] 语句
  sessionStorage.setItem(SCROLL_MEMORY_PREFIX + fullPath, JSON.stringify(data));
// [zh] 语法符号
}

// [zh] 导出模块成员
export function getScrollForPath(fullPath: string): ScrollMemoryRecord | null {
  // [zh] 声明变量 `raw`
  const raw = sessionStorage.getItem(SCROLL_MEMORY_PREFIX + fullPath);
  // [zh] 条件判断 if
  if (!raw) return null;

  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `parsed`
    const parsed = JSON.parse(raw) as ScrollMemoryRecord;
    // [zh] 条件判断 if
    if (typeof parsed?.top === 'number' && parsed.target) return parsed;
  // [zh] 代码块开始
  } catch {
    // [zh] 声明变量 `top`
    const top = Number(raw);
    // [zh] 条件判断 if
    if (!Number.isNaN(top)) return { top, target: 'window' };
  // [zh] 语法符号
  }

  // [zh] return 返回
  return null;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function applyScroll(record: ScrollMemoryRecord) {
  // [zh] 条件判断 if
  if (record.target === 'window') {
    // [zh] 语句
    window.scrollTo(0, record.top);
    // [zh] 语句
    document.documentElement.scrollTop = record.top;
    // [zh] 语句
    document.body.scrollTop = record.top;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }

  // [zh] 声明变量 `el`
  const el = document.querySelector(record.target) as HTMLElement | null;
  // [zh] 条件判断 if
  if (el) {
    // [zh] 语句
    el.scrollTop = record.top;
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    window.scrollTo(0, record.top);
    // [zh] 语句
    document.documentElement.scrollTop = record.top;
    // [zh] 语句
    document.body.scrollTop = record.top;
  // [zh] 语法符号
  }
// [zh] 语法符号
}


// [zh] 导出模块成员
export function restoreScrollForPath(fullPath: string) {
  // [zh] 声明变量 `record`
  const record = getScrollForPath(fullPath);
  // [zh] 条件判断 if
  if (!record) return;

  // [zh] 声明变量 `run`
  const run = () => applyScroll(record);
  // [zh] 语句
  run();
  // [zh] 语句
  requestAnimationFrame(run);
  // [zh] 语句
  setTimeout(run, 100);
  // [zh] 语句
  setTimeout(run, 300);
  // [zh] 语句
  setTimeout(run, 600);
// [zh] 语法符号
}
