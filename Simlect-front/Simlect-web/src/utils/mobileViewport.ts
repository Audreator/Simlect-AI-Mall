// [zh] 声明变量 `MOBILE_MAX_WIDTH`
const MOBILE_MAX_WIDTH = 767;

// [zh] 导出模块成员
export const isTouchMobile = () =>
  // [zh] TypeScript/JS 语句：`window.matchMedia(`(max-width: ${MOBILE_MAX_WIDTH}px)`)`
  window.matchMedia(`(max-width: ${MOBILE_MAX_WIDTH}px)`).matches &&
  // [zh] 语句
  (window.matchMedia('(pointer: coarse)').matches || 'ontouchstart' in window);

// [zh] 导出模块成员
export const isIosDevice = () =>
  // [zh] TypeScript/JS 语句：`typeof navigator !== 'undefined' &&`
  typeof navigator !== 'undefined' &&
  // [zh] 语句
  /iPhone|iPad|iPod/i.test(navigator.userAgent);

// [zh] 声明变量 `ZOOM_ALLOW_SELECTOR`
const ZOOM_ALLOW_SELECTOR =
  // [zh] 语句
  'img, .allow-pinch-zoom, .eshop-image-lightbox, .eshop-image-lightbox__img';

// [zh] 声明变量 `INTERACTIVE_SELECTOR`
const INTERACTIVE_SELECTOR =
  // [zh] 语句
  'input, textarea, select, button, a, label, [contenteditable], [role="button"], .el-button, .el-input, .el-textarea, .el-checkbox, .el-radio, .nav-back, .tip-chip, .btn-send, .agent-chat-textarea';

// [zh] 声明变量 `SCROLLABLE_SELECTOR`
const SCROLLABLE_SELECTOR =
  // [zh] 语句
  '.toolbar-row, .sub-category-bar, .filter-bar, [overflow-x="auto"], [overflow-x="scroll"]';

// [zh] 声明变量 `isZoomAllowedTarget`
const isZoomAllowedTarget = (target: EventTarget | null) => {
  // [zh] 条件判断 if
  if (!(target instanceof Element)) return false;
  // [zh] return 返回
  return !!target.closest(ZOOM_ALLOW_SELECTOR);
// [zh] 语句
};

// [zh] 声明变量 `isInteractiveTarget`
const isInteractiveTarget = (target: EventTarget | null) => {
  // [zh] 条件判断 if
  if (!(target instanceof Element)) return false;
  // [zh] return 返回
  return !!target.closest(INTERACTIVE_SELECTOR);
// [zh] 语句
};

// [zh] 声明变量 `isScrollableTarget`
const isScrollableTarget = (target: EventTarget | null) => {
  // [zh] 条件判断 if
  if (!(target instanceof Element)) return false;
  // [zh] 声明变量 `el`
  const el = target.closest(SCROLLABLE_SELECTOR);
  // [zh] 条件判断 if
  if (el) return true;
  // [zh] 声明变量 `parent`
  const parent = target.parentElement;
  // [zh] 条件判断 if
  if (!parent) return false;
  // [zh] 声明变量 `style`
  const style = window.getComputedStyle(parent);
  // [zh] return 返回
  return style.overflowX === 'auto' || style.overflowX === 'scroll';
// [zh] 语句
};

// [zh] 声明变量 `installed`
let installed = false;
// [zh] 声明变量 `viewportSyncInstalled`
let viewportSyncInstalled = false;
// [zh] 声明变量 `iosZoomRecoveryInstalled`
let iosZoomRecoveryInstalled = false;

// [zh] 导出模块成员
export const MOBILE_VIEWPORT_LOCKED =
  // [zh] 语句
  'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, viewport-fit=cover';

// [zh] 导出模块成员
export const MOBILE_VIEWPORT_CONTENT = MOBILE_VIEWPORT_LOCKED;

// [zh] 声明变量 `getViewportMeta`
const getViewportMeta = () => document.querySelector('meta[name="viewport"]');

// [zh] 声明变量 `scrollViewportToTop`
const scrollViewportToTop = () => {
  // [zh] 语句
  window.scrollTo(0, 0);
  // [zh] 语句
  document.documentElement.scrollTop = 0;
  // [zh] 语句
  document.body.scrollTop = 0;
// [zh] 语句
};

// [zh] 导出模块成员
export const resetViewportScale = () => {
  // [zh] 语句
  recoverIosViewportZoom();
// [zh] 语句
};

// [zh] 导出模块成员
export const recoverIosViewportZoom = () => {
  // [zh] 条件判断 if
  if (typeof window === 'undefined') return;

  // [zh] 语句
  scrollViewportToTop();

  // [zh] 声明变量 `meta`
  const meta = getViewportMeta();
  // [zh] 条件判断 if
  if (!meta) return;

  // [zh] 声明变量 `flush`
  const flush = () => {
    // [zh] 语句
    meta.setAttribute('content', MOBILE_VIEWPORT_LOCKED);
    // [zh] 语句
    scrollViewportToTop();
    // [zh] 语句
    syncVisualViewportHeight();
  // [zh] 语句
  };

  // [zh] TypeScript/JS 语句：`meta.setAttribute(`
  meta.setAttribute(
    // [zh] TypeScript/JS 语句：`'content',`
    'content',
    // [zh] TypeScript/JS 语句：`'width=device-width, initial-scale=1.0, minimum-scale=1`
    'width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, viewport-fit=cover'
  // [zh] 语句
  );
  // [zh] 语句
  requestAnimationFrame(flush);
  // [zh] 语句
  window.setTimeout(flush, 80);
  // [zh] 语句
  window.setTimeout(flush, 280);
// [zh] 语句
};

// [zh] 导出模块成员
export const lockViewportAfterInput = () => {
  // [zh] 语句
  recoverIosViewportZoom();
// [zh] 语句
};

// [zh] 导出模块成员
export const syncVisualViewportHeight = () => {
  // [zh] 声明变量 `vv`
  const vv = window.visualViewport;
  // [zh] 声明变量 `height`
  const height = vv?.height ?? window.innerHeight;
  // [zh] 语句
  document.documentElement.style.setProperty('--app-vh', `${Math.round(height)}px`);

  // [zh] 条件判断 if
  if (!isIosDevice() || !vv) return;

  // [zh] 声明变量 `scale`
  const scale = vv.scale ?? 1;
  // [zh] 声明变量 `root`
  const root = document.documentElement;
  // [zh] 条件判断 if
  if (scale > 1.02) {
    // [zh] 语句
    root.classList.add('ios-viewport-zoomed');
    // [zh] 语句
    scrollViewportToTop();
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    root.classList.remove('ios-viewport-zoomed');
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `installIosZoomRecovery`
const installIosZoomRecovery = () => {
  // [zh] 条件判断 if
  if (iosZoomRecoveryInstalled || !isIosDevice()) return;
  // [zh] 语句
  iosZoomRecoveryInstalled = true;

  // [zh] 声明变量 `onViewportChange`
  const onViewportChange = () => {
    // [zh] 语句
    syncVisualViewportHeight();
    // [zh] 声明变量 `scale`
    const scale = window.visualViewport?.scale ?? 1;
    // [zh] 声明变量 `active`
    const active = document.activeElement;
    // [zh] 声明变量 `typing`
    const typing =
      // [zh] TypeScript/JS 语句：`active instanceof HTMLInputElement ||`
      active instanceof HTMLInputElement ||
      // [zh] TypeScript/JS 语句：`active instanceof HTMLTextAreaElement ||`
      active instanceof HTMLTextAreaElement ||
      // [zh] 语句
      active?.closest('.agent-chat-textarea');

    // [zh] 条件判断 if
    if (!typing && scale > 1.02) {
      // [zh] 语句
      recoverIosViewportZoom();
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 语句
  window.visualViewport?.addEventListener('resize', onViewportChange);
  // [zh] 语句
  window.visualViewport?.addEventListener('scroll', onViewportChange);
// [zh] 语句
};

// [zh] 导出模块成员
export const installVisualViewportSync = () => {
  // [zh] 条件判断 if
  if (viewportSyncInstalled || typeof window === 'undefined') return;
  // [zh] 语句
  viewportSyncInstalled = true;

  // [zh] 语句
  syncVisualViewportHeight();
  // [zh] 语句
  installIosZoomRecovery();
  // [zh] 语句
  window.addEventListener('resize', syncVisualViewportHeight);
  // [zh] 语句
  window.visualViewport?.addEventListener('resize', syncVisualViewportHeight);
  // [zh] 语句
  window.visualViewport?.addEventListener('scroll', syncVisualViewportHeight);
// [zh] 语句
};

// [zh] 导出模块成员
export const installMobileViewportGuards = () => {
  // [zh] 条件判断 if
  if (installed || !isTouchMobile()) return;
  // [zh] 语句
  installed = true;

  // [zh] 语句
  installVisualViewportSync();

  // [zh] 条件判断 if
  if (!isIosDevice()) return;

  // [zh] 声明变量 `onGesture`
  const onGesture = (event: Event) => {
    // [zh] 条件判断 if
    if (!isZoomAllowedTarget(event.target)) event.preventDefault();
  // [zh] 语句
  };

  // [zh] 语句
  document.addEventListener('gesturestart', onGesture, { passive: false });
  // [zh] 语句
  document.addEventListener('gesturechange', onGesture, { passive: false });
  // [zh] 语句
  document.addEventListener('gestureend', onGesture, { passive: false });

  // [zh] TypeScript/JS 语句：`document.addEventListener(`
  document.addEventListener(
    // [zh] TypeScript/JS 语句：`'touchmove',`
    'touchmove',
    // [zh] 代码块开始
    (event) => {
      // [zh] 条件判断 if
      if (event.touches.length > 1 && !isZoomAllowedTarget(event.target)) {
        // [zh] 语句
        event.preventDefault();
      // [zh] 语法符号
      }
      // [zh] 条件判断 if
      if (event.touches.length === 1 && isScrollableTarget(event.target)) {
        // [zh] 语句
        return;
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ passive: false }`
    { passive: false }
  // [zh] 语句
  );

  // [zh] 声明变量 `lastTouchEnd`
  let lastTouchEnd = 0;
  // [zh] TypeScript/JS 语句：`document.addEventListener(`
  document.addEventListener(
    // [zh] TypeScript/JS 语句：`'touchend',`
    'touchend',
    // [zh] 代码块开始
    (event) => {
      // [zh] 声明变量 `now`
      const now = Date.now();
      // [zh] 声明变量 `isDoubleTap`
      const isDoubleTap = now - lastTouchEnd <= 300;
      // [zh] 语句
      lastTouchEnd = now;
      // [zh] 条件判断 if
      if (
        // [zh] TypeScript/JS 语句：`isDoubleTap &&`
        isDoubleTap &&
        // [zh] TypeScript/JS 语句：`!isZoomAllowedTarget(event.target) &&`
        !isZoomAllowedTarget(event.target) &&
        // [zh] TypeScript/JS 语句：`!isInteractiveTarget(event.target)`
        !isInteractiveTarget(event.target)
      // [zh] 代码块开始
      ) {
        // [zh] 语句
        event.preventDefault();
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ passive: false }`
    { passive: false }
  // [zh] 语句
  );

  // [zh] TypeScript/JS 语句：`document.addEventListener(`
  document.addEventListener(
    // [zh] TypeScript/JS 语句：`'focusout',`
    'focusout',
    // [zh] 代码块开始
    (event) => {
      // [zh] 声明变量 `target`
      const target = event.target;
      // [zh] 条件判断 if
      if (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) {
        // [zh] 语句
        window.setTimeout(recoverIosViewportZoom, 0);
        // [zh] 语句
        window.setTimeout(recoverIosViewportZoom, 150);
        // [zh] 语句
        window.setTimeout(lockViewportAfterInput, 320);
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`true`
    true
  // [zh] 语句
  );
// [zh] 语句
};
