// [zh] 导入 `vue`
import { createApp } from 'vue';
// [zh] 导入 `pinia`
import { createPinia } from 'pinia';
// [zh] 导入 `element-plus`
import ElementPlus from 'element-plus';
// [zh] 导入 `模块`
import 'element-plus/dist/index.css';
// [zh] 导入 `@element-plus/icons-vue`
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
// [zh] 导入 `./App.vue`
import App from './App.vue';
// [zh] 导入 `./router`
import router from './router';
// [zh] 导入 `模块`
import './styles/element-theme.scss';
// [zh] 导入 `模块`
import './styles/global.scss';
// [zh] 导入 `./stores/device`
import { useDeviceStore } from './stores/device';
// [zh] 导入 `./utils/mobileViewport`
import { installMobileViewportGuards, installVisualViewportSync } from './utils/mobileViewport';
// [zh] 导入 `./utils/liquidGlassFilters`
import { ensureLiquidGlassFilters } from './utils/liquidGlassFilters';
// [zh] 导入 `./utils/bootSplash`
import { scheduleBootSplashDismiss, BOOT_SPLASH_MAX_MS } from './utils/bootSplash';
// [zh] 导入 `./utils/homeBootstrap`
import { createHomeSplashPaintGate, prepareHomeForSplash } from './utils/homeBootstrap';
// [zh] 导入 `./utils/standalone`
import { isStandaloneDisplay } from './utils/standalone';
// [zh] 导入 `./stores/auth`
import { useAuthStore } from './stores/auth';

// [zh] 语句
ensureLiquidGlassFilters();

// [zh] 声明变量 `pinia`
const pinia = createPinia();
// [zh] 声明变量 `deviceStore`
const deviceStore = useDeviceStore(pinia);
// [zh] 声明变量 `authStore`
const authStore = useAuthStore(pinia);
// [zh] 声明变量 `standaloneLaunch`
const standaloneLaunch = isStandaloneDisplay();
// [zh] 语句
deviceStore.sync();

// [zh] 声明变量 `homeSplashPaintGate`
const homeSplashPaintGate = standaloneLaunch ? createHomeSplashPaintGate() : null;
// [zh] 声明变量 `homeBootTask`
const homeBootTask = standaloneLaunch
  // [zh] TypeScript/JS 语句：`? Promise.race([`
  ? Promise.race([
      // [zh] TypeScript/JS 语句：`Promise.all([`
      Promise.all([
        // [zh] TypeScript/JS 语句：`prepareHomeForSplash(false).catch(() => undefined),`
        prepareHomeForSplash(false).catch(() => undefined),
        // [zh] TypeScript/JS 语句：`authStore.loadMemberLevel().catch(() => undefined)`
        authStore.loadMemberLevel().catch(() => undefined)
      // [zh] TypeScript/JS 语句：`]),`
      ]),
      // [zh] TypeScript/JS 语句：`new Promise<void>((resolve) => window.setTimeout(resolv`
      new Promise<void>((resolve) => window.setTimeout(resolve, BOOT_SPLASH_MAX_MS))
    // [zh] TypeScript/JS 语句：`])`
    ])
  // [zh] 语句
  : undefined;

// [zh] 条件判断 if
if (deviceStore.isMobile) {
  // [zh] 语句
  installVisualViewportSync();
  // [zh] 语句
  installMobileViewportGuards();
// [zh] 语法符号
}
// [zh] 语句
window.addEventListener('resize', () => deviceStore.sync());

// [zh] 声明变量 `app`
const app = createApp(App);

// [zh] 代码块开始
Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
  // [zh] 语句
  app.component(key, component);
// [zh] 语句
});

// [zh] 语句
app.use(pinia);
// [zh] 语句
app.use(router);
// [zh] 语句
app.use(ElementPlus);

// [zh] 声明变量 `bootReady`
const bootReady = (async () => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await router.isReady();
  // [zh] 条件判断 if
  if (homeBootTask) await homeBootTask;
  // [zh] 条件判断 if
  if (homeSplashPaintGate) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await Promise.race([
      // [zh] TypeScript/JS 语句：`homeSplashPaintGate,`
      homeSplashPaintGate,
      // [zh] TypeScript/JS 语句：`new Promise<void>((resolve) => window.setTimeout(resolv`
      new Promise<void>((resolve) => window.setTimeout(resolve, BOOT_SPLASH_MAX_MS))
    // [zh] 语句
    ]);
  // [zh] 语法符号
  }
// [zh] 语句
})();

// [zh] 语句
app.mount('#app');

// [zh] 条件判断 if
if (standaloneLaunch) {
  // [zh] 语句
  void scheduleBootSplashDismiss(() => bootReady);
// [zh] 语法符号
}
