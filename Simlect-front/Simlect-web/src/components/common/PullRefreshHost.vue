<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pull-refresh-host">
    <!-- [zh] 开始标签 `<div>` -->
    <div
      v-show="visibleIndicator"
      class="pull-refresh-indicator"
      :style="{ height: `${indicatorHeight}px`, opacity: indicatorOpacity }"
    >
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="refreshing" class="pull-text">刷新中…</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-else-if="pullDistance >= triggerDistance" class="pull-text">松开刷新</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-else-if="pullDistance > 4" class="pull-text">下拉刷新</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<slot>` -->
    <slot />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`computed,`
  computed,
  // [zh] TypeScript/JS 语句：`onMounted,`
  onMounted,
  // [zh] TypeScript/JS 语句：`onUnmounted,`
  onUnmounted,
  // [zh] TypeScript/JS 语句：`provide,`
  provide,
  // [zh] TypeScript/JS 语句：`ref,`
  ref,
  // [zh] TypeScript/JS 语句：`shallowRef,`
  shallowRef,
  // [zh] TypeScript/JS 语句：`watch,`
  watch,
  // [zh] TypeScript 类型 `PropType`（类比 Java interface/DTO）
  type PropType
// [zh] 语句
} from 'vue';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`getScrollTop,`
  getScrollTop,
  // [zh] TypeScript/JS 语句：`PULL_REFRESH_REGISTER_KEY,`
  PULL_REFRESH_REGISTER_KEY,
  // [zh] TypeScript 类型 `PageRefreshRegistration`（类比 Java interface/DTO）
  type PageRefreshRegistration
// [zh] 语句
} from '@/composables/pullRefresh';
// [zh] 导入 `@/utils/scrollMemory`
import { applyScroll, captureScrollPosition, saveScrollForPath } from '@/utils/scrollMemory';

// [zh] 声明变量 `props`
const props = defineProps({

  // [zh] 代码块开始
  defaultScrollEl: {
    // [zh] TypeScript/JS 语句：`type: Object as PropType<HTMLElement | null | undefined`
    type: Object as PropType<HTMLElement | null | undefined>,
    // [zh] TypeScript/JS 语句：`default: null`
    default: null
  // [zh] 语法符号
  }
// [zh] 语句
});

// [zh] 声明变量 `registration`
const registration = shallowRef<PageRefreshRegistration | null>(null);
// [zh] 声明变量 `pullDistance`
const pullDistance = ref(0);
// [zh] 声明变量 `refreshing`
const refreshing = ref(false);
// [zh] 声明变量 `triggerDistance`
const triggerDistance = 52;
// [zh] 声明变量 `maxPull`
const maxPull = 80;

// [zh] 声明变量 `startX`
let startX = 0;
// [zh] 声明变量 `startY`
let startY = 0;
// [zh] 声明变量 `pulling`
let pulling = false;
// [zh] 声明变量 `boundEl`
let boundEl: HTMLElement | null = null;

// [zh] 声明变量 `visibleIndicator`
const visibleIndicator = computed(() => refreshing.value || pullDistance.value > 2);
// [zh] 声明变量 `indicatorHeight`
const indicatorHeight = computed(() =>
  // [zh] TypeScript/JS 语句：`refreshing.value ? 44 : Math.min(pullDistance.value, ma`
  refreshing.value ? 44 : Math.min(pullDistance.value, maxPull)
// [zh] 语句
);
// [zh] 声明变量 `indicatorOpacity`
const indicatorOpacity = computed(() =>
  // [zh] TypeScript/JS 语句：`refreshing.value ? 1 : Math.min(1, pullDistance.value /`
  refreshing.value ? 1 : Math.min(1, pullDistance.value / triggerDistance)
// [zh] 语句
);

// [zh] 代码块开始
provide(PULL_REFRESH_REGISTER_KEY, (reg: PageRefreshRegistration | null) => {
  // [zh] 语句
  registration.value = reg;
// [zh] 语句
});

// [zh] 声明变量 `resolveScrollEl`
const resolveScrollEl = (): HTMLElement | null => {
  // [zh] 声明变量 `custom`
  const custom = registration.value?.getScrollEl?.();
  // [zh] 条件判断 if
  if (custom) return custom;
  // [zh] 条件判断 if
  if (props.defaultScrollEl) return props.defaultScrollEl;
  // [zh] return 返回
  return document.documentElement;
// [zh] 语句
};

// [zh] 声明变量 `canPull`
const canPull = () => !refreshing.value && !!registration.value;

// [zh] 声明变量 `onTouchStart`
const onTouchStart = (e: TouchEvent) => {
  // [zh] 条件判断 if
  if (!canPull()) return;
  // [zh] 声明变量 `el`
  const el = resolveScrollEl();
  // [zh] 条件判断 if
  if (getScrollTop(el) > 0) return;
  // [zh] 语句
  startX = e.touches[0]?.clientX ?? 0;
  // [zh] 语句
  startY = e.touches[0]?.clientY ?? 0;
  // [zh] 语句
  pulling = true;
// [zh] 语句
};

// [zh] 声明变量 `onTouchMove`
const onTouchMove = (e: TouchEvent) => {
  // [zh] 条件判断 if
  if (!pulling || !canPull()) return;
  // [zh] 声明变量 `el`
  const el = resolveScrollEl();
  // [zh] 声明变量 `x`
  const x = e.touches[0]?.clientX ?? 0;
  // [zh] 声明变量 `y`
  const y = e.touches[0]?.clientY ?? 0;
  // [zh] 声明变量 `dx`
  const dx = x - startX;
  // [zh] 声明变量 `dy`
  const dy = y - startY;

  // [zh] 条件判断 if
  if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 8) {
    // [zh] 语句
    pulling = false;
    // [zh] 语句
    pullDistance.value = 0;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (dy <= 0) {
    // [zh] 语句
    pullDistance.value = 0;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (getScrollTop(el) > 2) {
    // [zh] 语句
    pulling = false;
    // [zh] 语句
    pullDistance.value = 0;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (dy < 8) return;
  // [zh] 语句
  e.preventDefault();
  // [zh] 语句
  pullDistance.value = Math.min(dy * 0.45, maxPull);
// [zh] 语句
};

// [zh] 声明变量 `onTouchEnd`
const onTouchEnd = async () => {
  // [zh] 条件判断 if
  if (!pulling) return;
  // [zh] 语句
  pulling = false;
  // [zh] 声明变量 `reg`
  const reg = registration.value;
  // [zh] 条件判断 if
  if (!reg) {
    // [zh] 语句
    pullDistance.value = 0;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (pullDistance.value < triggerDistance) {
    // [zh] 语句
    pullDistance.value = 0;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  refreshing.value = true;
  // [zh] 语句
  pullDistance.value = 44;
  // [zh] 代码块开始
  try {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await reg.refresh();
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    refreshing.value = false;
    // [zh] 语句
    pullDistance.value = 0;

    // [zh] 声明变量 `captured`
    const captured = captureScrollPosition();
    // [zh] 声明变量 `fullPath`
    const fullPath =
      // [zh] 语句
      window.location.pathname + window.location.search + window.location.hash;
    // [zh] 声明变量 `resetRecord`
    const resetRecord = { top: 0, target: captured.target };
    // [zh] 语句
    saveScrollForPath(fullPath, resetRecord);
    // [zh] 声明变量 `resetTop`
    const resetTop = () => applyScroll(resetRecord);
    // [zh] 语句
    resetTop();
    // [zh] 语句
    requestAnimationFrame(resetTop);
    // [zh] 语句
    setTimeout(resetTop, 100);
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `bindTouch`
const bindTouch = (el: HTMLElement | null) => {
  // [zh] 条件判断 if
  if (boundEl) {
    // [zh] 语句
    boundEl.removeEventListener('touchstart', onTouchStart);
    // [zh] 语句
    boundEl.removeEventListener('touchmove', onTouchMove);
    // [zh] 语句
    boundEl.removeEventListener('touchend', onTouchEnd);
    // [zh] 语句
    boundEl.removeEventListener('touchcancel', onTouchEnd);
    // [zh] 语句
    boundEl = null;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!el) return;
  // [zh] 语句
  boundEl = el;
  // [zh] 语句
  el.addEventListener('touchstart', onTouchStart, { passive: true });
  // [zh] 语句
  el.addEventListener('touchmove', onTouchMove, { passive: false });
  // [zh] 语句
  el.addEventListener('touchend', onTouchEnd, { passive: true });
  // [zh] 语句
  el.addEventListener('touchcancel', onTouchEnd, { passive: true });
// [zh] 语句
};

// [zh] 声明变量 `updateBinding`
const updateBinding = () => {
  // [zh] 语句
  bindTouch(resolveScrollEl());
// [zh] 语句
};

// [zh] 声明变量 `isMobileViewport`
const isMobileViewport = () =>
  // [zh] 语句
  typeof window !== 'undefined' && window.matchMedia('(max-width: 767px)').matches;

// [zh] 代码块开始
onMounted(() => {
  // [zh] 条件判断 if
  if (isMobileViewport()) updateBinding();
// [zh] 语句
});

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 语句
  bindTouch(null);
// [zh] 语句
});

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => [registration.value, props.defaultScrollEl] as co`
  () => [registration.value, props.defaultScrollEl] as const,
  // [zh] TypeScript/JS 语句：`() => updateBinding(),`
  () => updateBinding(),
  // [zh] TypeScript/JS 语句：`{ flush: 'post' }`
  { flush: 'post' }
// [zh] 语句
);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pull-refresh-host {` */
.pull-refresh-host {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pull-refresh-indicator {` */
.pull-refresh-indicator {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-end;` */
  align-items: flex-end;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `transition: height 0.2s ease, opacity 0.` */
  transition: height 0.2s ease, opacity 0.15s ease;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pull-text {` */
.pull-text {
  /* [zh] 样式规则 `padding-bottom: 6px;` */
  padding-bottom: 6px;
/* [zh] 样式规则 `}` */
}
</style>
