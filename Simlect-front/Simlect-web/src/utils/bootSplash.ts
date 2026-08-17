const BOOT_SPLASH_MIN_MS = 800;
const BOOT_SPLASH_FADE_MS = 300;

export const BOOT_SPLASH_MAX_MS = 3000;
declare global {
  interface Window {
    __pwaBootSplashAt?: number;
  }
}

export function dismissBootSplash(fadeMs = BOOT_SPLASH_FADE_MS) {
  const el = document.getElementById('pwa-boot-splash');
  if (!el) return;
  el.classList.add('is-hidden');
  window.setTimeout(() => el.remove(), fadeMs);
}

export async function scheduleBootSplashDismiss(ready: () => Promise<unknown>) {
  const started = window.__pwaBootSplashAt ?? performance.now();
  const minWait = new Promise<void>((resolve) => {
    const elapsed = performance.now() - started;
    window.setTimeout(resolve, Math.max(0, BOOT_SPLASH_MIN_MS - elapsed));
  });

  await Promise.race([
    Promise.all([ready(), minWait]),
    new Promise<void>((resolve) => window.setTimeout(resolve, BOOT_SPLASH_MAX_MS))
  ]);
  dismissBootSplash();
}
