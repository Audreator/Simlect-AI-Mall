export const SCROLL_MEMORY_PREFIX = 'scroll_pos:';

export type ScrollTarget = 'window' | string;

export interface ScrollMemoryRecord {
  top: number;
  target: ScrollTarget;
}

const INTERNAL_SCROLL_SELECTORS = [
  '.orders-body',
  '.coupon-list-scroll',
  '.coupon-scroll',
  '.comment-scroll',
  '.footprint-scroll',
  '.wishlist-scroll',
  '.productSort .conter',
  '.productSort .aside'
] as const;

const isScrollable = (el: HTMLElement) => {
  const style = window.getComputedStyle(el);
  return (
    (style.overflowY === 'auto' || style.overflowY === 'scroll') &&
    el.scrollHeight > el.clientHeight + 1
  );
};


export function captureScrollPosition(): ScrollMemoryRecord {
  let best: ScrollMemoryRecord | null = null;

  for (const selector of INTERNAL_SCROLL_SELECTORS) {
    const el = document.querySelector(selector) as HTMLElement | null;
    if (!el || !isScrollable(el)) continue;
    const record = { top: el.scrollTop, target: selector };
    if (!best || record.top > best.top) best = record;
  }

  if (best) return best;

  return {
    top: window.scrollY || document.documentElement.scrollTop || 0,
    target: 'window'
  };
}

export function saveScrollForPath(fullPath: string, record?: ScrollMemoryRecord) {
  const data = record ?? captureScrollPosition();
  sessionStorage.setItem(SCROLL_MEMORY_PREFIX + fullPath, JSON.stringify(data));
}

export function getScrollForPath(fullPath: string): ScrollMemoryRecord | null {
  const raw = sessionStorage.getItem(SCROLL_MEMORY_PREFIX + fullPath);
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw) as ScrollMemoryRecord;
    if (typeof parsed?.top === 'number' && parsed.target) return parsed;
  } catch {
    const top = Number(raw);
    if (!Number.isNaN(top)) return { top, target: 'window' };
  }

  return null;
}

export function applyScroll(record: ScrollMemoryRecord) {
  if (record.target === 'window') {
    window.scrollTo(0, record.top);
    document.documentElement.scrollTop = record.top;
    document.body.scrollTop = record.top;
    return;
  }

  const el = document.querySelector(record.target) as HTMLElement | null;
  if (el) {
    el.scrollTop = record.top;
  } else {
    window.scrollTo(0, record.top);
    document.documentElement.scrollTop = record.top;
    document.body.scrollTop = record.top;
  }
}


export function restoreScrollForPath(fullPath: string) {
  const record = getScrollForPath(fullPath);
  if (!record) return;

  const run = () => applyScroll(record);
  run();
  requestAnimationFrame(run);
  setTimeout(run, 100);
  setTimeout(run, 300);
  setTimeout(run, 600);
}
