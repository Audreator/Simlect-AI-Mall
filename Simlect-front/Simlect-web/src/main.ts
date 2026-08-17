import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import App from './App.vue';
import router from './router';
import './styles/element-theme.scss';
import './styles/global.scss';
import { useDeviceStore } from './stores/device';
import { installMobileViewportGuards, installVisualViewportSync } from './utils/mobileViewport';
import { ensureLiquidGlassFilters } from './utils/liquidGlassFilters';
import { scheduleBootSplashDismiss, BOOT_SPLASH_MAX_MS } from './utils/bootSplash';
import { createHomeSplashPaintGate, prepareHomeForSplash } from './utils/homeBootstrap';
import { isStandaloneDisplay } from './utils/standalone';
import { useAuthStore } from './stores/auth';

ensureLiquidGlassFilters();

const pinia = createPinia();
const deviceStore = useDeviceStore(pinia);
const authStore = useAuthStore(pinia);
const standaloneLaunch = isStandaloneDisplay();
deviceStore.sync();

const homeSplashPaintGate = standaloneLaunch ? createHomeSplashPaintGate() : null;
const homeBootTask = standaloneLaunch
  ? Promise.race([
      Promise.all([
        prepareHomeForSplash(false).catch(() => undefined),
        authStore.loadMemberLevel().catch(() => undefined)
      ]),
      new Promise<void>((resolve) => window.setTimeout(resolve, BOOT_SPLASH_MAX_MS))
    ])
  : undefined;

if (deviceStore.isMobile) {
  installVisualViewportSync();
  installMobileViewportGuards();
}
window.addEventListener('resize', () => deviceStore.sync());

const app = createApp(App);

Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
  app.component(key, component);
});

app.use(pinia);
app.use(router);
app.use(ElementPlus);

const bootReady = (async () => {
  await router.isReady();
  if (homeBootTask) await homeBootTask;
  if (homeSplashPaintGate) {
    await Promise.race([
      homeSplashPaintGate,
      new Promise<void>((resolve) => window.setTimeout(resolve, BOOT_SPLASH_MAX_MS))
    ]);
  }
})();

app.mount('#app');

if (standaloneLaunch) {
  void scheduleBootSplashDismiss(() => bootReady);
}
