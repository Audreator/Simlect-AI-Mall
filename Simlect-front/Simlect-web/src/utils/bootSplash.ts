// [zh] 声明变量 `BOOT_SPLASH_MIN_MS`
const BOOT_SPLASH_MIN_MS = 800;
// [zh] 声明变量 `BOOT_SPLASH_FADE_MS`
const BOOT_SPLASH_FADE_MS = 300;

// [zh] 导出模块成员
export const BOOT_SPLASH_MAX_MS = 3000;
// [zh] 代码块开始
declare global {
  // [zh] TypeScript 类型 `Window`（类比 Java interface/DTO）
  interface Window {
    // [zh] 语句
    __pwaBootSplashAt?: number;
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 导出模块成员
export function dismissBootSplash(fadeMs = BOOT_SPLASH_FADE_MS) {
  // [zh] 声明变量 `el`
  const el = document.getElementById('pwa-boot-splash');
  // [zh] 条件判断 if
  if (!el) return;
  // [zh] 语句
  el.classList.add('is-hidden');
  // [zh] 语句
  window.setTimeout(() => el.remove(), fadeMs);
// [zh] 语法符号
}

// [zh] 导出模块成员
export async function scheduleBootSplashDismiss(ready: () => Promise<unknown>) {
  // [zh] 声明变量 `started`
  const started = window.__pwaBootSplashAt ?? performance.now();
  // [zh] 声明变量 `minWait`
  const minWait = new Promise<void>((resolve) => {
    // [zh] 声明变量 `elapsed`
    const elapsed = performance.now() - started;
    // [zh] 语句
    window.setTimeout(resolve, Math.max(0, BOOT_SPLASH_MIN_MS - elapsed));
  // [zh] 语句
  });

  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await Promise.race([
    // [zh] TypeScript/JS 语句：`Promise.all([ready(), minWait]),`
    Promise.all([ready(), minWait]),
    // [zh] TypeScript/JS 语句：`new Promise<void>((resolve) => window.setTimeout(resolv`
    new Promise<void>((resolve) => window.setTimeout(resolve, BOOT_SPLASH_MAX_MS))
  // [zh] 语句
  ]);
  // [zh] 语句
  dismissBootSplash();
// [zh] 语法符号
}
