const MOBILE_MAX_WIDTH = 767;

export const isTouchMobile = () =>
  window.matchMedia(`(max-width: ${MOBILE_MAX_WIDTH}px)`).matches &&
  (window.matchMedia('(pointer: coarse)').matches || 'ontouchstart' in window);

export const isIosDevice = () =>
  typeof navigator !== 'undefined' &&
  /iPhone|iPad|iPod/i.test(navigator.userAgent);

const ZOOM_ALLOW_SELECTOR =
  'img, .allow-pinch-zoom, .eshop-image-lightbox, .eshop-image-lightbox__img';

const INTERACTIVE_SELECTOR =
  'input, textarea, select, button, a, label, [contenteditable], [role="button"], .el-button, .el-input, .el-textarea, .el-checkbox, .el-radio, .nav-back, .tip-chip, .btn-send, .agent-chat-textarea';

const SCROLLABLE_SELECTOR =
  '.toolbar-row, .sub-category-bar, .filter-bar, [overflow-x="auto"], [overflow-x="scroll"]';

const isZoomAllowedTarget = (target: EventTarget | null) => {
  if (!(target instanceof Element)) return false;
  return !!target.closest(ZOOM_ALLOW_SELECTOR);
};

const isInteractiveTarget = (target: EventTarget | null) => {
  if (!(target instanceof Element)) return false;
  return !!target.closest(INTERACTIVE_SELECTOR);
};

const isScrollableTarget = (target: EventTarget | null) => {
  if (!(target instanceof Element)) return false;
  const el = target.closest(SCROLLABLE_SELECTOR);
  if (el) return true;
  const parent = target.parentElement;
  if (!parent) return false;
  const style = window.getComputedStyle(parent);
  return style.overflowX === 'auto' || style.overflowX === 'scroll';
};

let installed = false;
let viewportSyncInstalled = false;
let iosZoomRecoveryInstalled = false;

export const MOBILE_VIEWPORT_LOCKED =
  'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, viewport-fit=cover';

export const MOBILE_VIEWPORT_CONTENT = MOBILE_VIEWPORT_LOCKED;

const getViewportMeta = () => document.querySelector('meta[name="viewport"]');

const scrollViewportToTop = () => {
  window.scrollTo(0, 0);
  document.documentElement.scrollTop = 0;
  document.body.scrollTop = 0;
};

export const resetViewportScale = () => {
  recoverIosViewportZoom();
};

export const recoverIosViewportZoom = () => {
  if (typeof window === 'undefined') return;

  scrollViewportToTop();

  const meta = getViewportMeta();
  if (!meta) return;

  const flush = () => {
    meta.setAttribute('content', MOBILE_VIEWPORT_LOCKED);
    scrollViewportToTop();
    syncVisualViewportHeight();
  };

  meta.setAttribute(
    'content',
    'width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, viewport-fit=cover'
  );
  requestAnimationFrame(flush);
  window.setTimeout(flush, 80);
  window.setTimeout(flush, 280);
};

export const lockViewportAfterInput = () => {
  recoverIosViewportZoom();
};

export const syncVisualViewportHeight = () => {
  const vv = window.visualViewport;
  const height = vv?.height ?? window.innerHeight;
  document.documentElement.style.setProperty('--app-vh', `${Math.round(height)}px`);

  if (!isIosDevice() || !vv) return;

  const scale = vv.scale ?? 1;
  const root = document.documentElement;
  if (scale > 1.02) {
    root.classList.add('ios-viewport-zoomed');
    scrollViewportToTop();
  } else {
    root.classList.remove('ios-viewport-zoomed');
  }
};

const installIosZoomRecovery = () => {
  if (iosZoomRecoveryInstalled || !isIosDevice()) return;
  iosZoomRecoveryInstalled = true;

  const onViewportChange = () => {
    syncVisualViewportHeight();
    const scale = window.visualViewport?.scale ?? 1;
    const active = document.activeElement;
    const typing =
      active instanceof HTMLInputElement ||
      active instanceof HTMLTextAreaElement ||
      active?.closest('.agent-chat-textarea');

    if (!typing && scale > 1.02) {
      recoverIosViewportZoom();
    }
  };

  window.visualViewport?.addEventListener('resize', onViewportChange);
  window.visualViewport?.addEventListener('scroll', onViewportChange);
};

export const installVisualViewportSync = () => {
  if (viewportSyncInstalled || typeof window === 'undefined') return;
  viewportSyncInstalled = true;

  syncVisualViewportHeight();
  installIosZoomRecovery();
  window.addEventListener('resize', syncVisualViewportHeight);
  window.visualViewport?.addEventListener('resize', syncVisualViewportHeight);
  window.visualViewport?.addEventListener('scroll', syncVisualViewportHeight);
};

export const installMobileViewportGuards = () => {
  if (installed || !isTouchMobile()) return;
  installed = true;

  installVisualViewportSync();

  if (!isIosDevice()) return;

  const onGesture = (event: Event) => {
    if (!isZoomAllowedTarget(event.target)) event.preventDefault();
  };

  document.addEventListener('gesturestart', onGesture, { passive: false });
  document.addEventListener('gesturechange', onGesture, { passive: false });
  document.addEventListener('gestureend', onGesture, { passive: false });

  document.addEventListener(
    'touchmove',
    (event) => {
      if (event.touches.length > 1 && !isZoomAllowedTarget(event.target)) {
        event.preventDefault();
      }
      if (event.touches.length === 1 && isScrollableTarget(event.target)) {
        return;
      }
    },
    { passive: false }
  );

  let lastTouchEnd = 0;
  document.addEventListener(
    'touchend',
    (event) => {
      const now = Date.now();
      const isDoubleTap = now - lastTouchEnd <= 300;
      lastTouchEnd = now;
      if (
        isDoubleTap &&
        !isZoomAllowedTarget(event.target) &&
        !isInteractiveTarget(event.target)
      ) {
        event.preventDefault();
      }
    },
    { passive: false }
  );

  document.addEventListener(
    'focusout',
    (event) => {
      const target = event.target;
      if (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) {
        window.setTimeout(recoverIosViewportZoom, 0);
        window.setTimeout(recoverIosViewportZoom, 150);
        window.setTimeout(lockViewportAfterInput, 320);
      }
    },
    true
  );
};
