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

<script setup lang="ts">
import {
  computed,
  onMounted,
  onUnmounted,
  provide,
  ref,
  shallowRef,
  watch,
  type PropType
} from 'vue';
import {
  getScrollTop,
  PULL_REFRESH_REGISTER_KEY,
  type PageRefreshRegistration
} from '@/composables/pullRefresh';
import { applyScroll, captureScrollPosition, saveScrollForPath } from '@/utils/scrollMemory';

const props = defineProps({

  defaultScrollEl: {
    type: Object as PropType<HTMLElement | null | undefined>,
    default: null
  }
});

const registration = shallowRef<PageRefreshRegistration | null>(null);
const pullDistance = ref(0);
const refreshing = ref(false);
const triggerDistance = 52;
const maxPull = 80;

let startX = 0;
let startY = 0;
let pulling = false;
let boundEl: HTMLElement | null = null;

const visibleIndicator = computed(() => refreshing.value || pullDistance.value > 2);
const indicatorHeight = computed(() =>
  refreshing.value ? 44 : Math.min(pullDistance.value, maxPull)
);
const indicatorOpacity = computed(() =>
  refreshing.value ? 1 : Math.min(1, pullDistance.value / triggerDistance)
);

provide(PULL_REFRESH_REGISTER_KEY, (reg: PageRefreshRegistration | null) => {
  registration.value = reg;
});

const resolveScrollEl = (): HTMLElement | null => {
  const custom = registration.value?.getScrollEl?.();
  if (custom) return custom;
  if (props.defaultScrollEl) return props.defaultScrollEl;
  return document.documentElement;
};

const canPull = () => !refreshing.value && !!registration.value;

const onTouchStart = (e: TouchEvent) => {
  if (!canPull()) return;
  const el = resolveScrollEl();
  if (getScrollTop(el) > 0) return;
  startX = e.touches[0]?.clientX ?? 0;
  startY = e.touches[0]?.clientY ?? 0;
  pulling = true;
};

const onTouchMove = (e: TouchEvent) => {
  if (!pulling || !canPull()) return;
  const el = resolveScrollEl();
  const x = e.touches[0]?.clientX ?? 0;
  const y = e.touches[0]?.clientY ?? 0;
  const dx = x - startX;
  const dy = y - startY;

  if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 8) {
    pulling = false;
    pullDistance.value = 0;
    return;
  }

  if (dy <= 0) {
    pullDistance.value = 0;
    return;
  }
  if (getScrollTop(el) > 2) {
    pulling = false;
    pullDistance.value = 0;
    return;
  }
  if (dy < 8) return;
  e.preventDefault();
  pullDistance.value = Math.min(dy * 0.45, maxPull);
};

const onTouchEnd = async () => {
  if (!pulling) return;
  pulling = false;
  const reg = registration.value;
  if (!reg) {
    pullDistance.value = 0;
    return;
  }
  if (pullDistance.value < triggerDistance) {
    pullDistance.value = 0;
    return;
  }
  refreshing.value = true;
  pullDistance.value = 44;
  try {
    await reg.refresh();
  } finally {
    refreshing.value = false;
    pullDistance.value = 0;

    const captured = captureScrollPosition();
    const fullPath =
      window.location.pathname + window.location.search + window.location.hash;
    const resetRecord = { top: 0, target: captured.target };
    saveScrollForPath(fullPath, resetRecord);
    const resetTop = () => applyScroll(resetRecord);
    resetTop();
    requestAnimationFrame(resetTop);
    setTimeout(resetTop, 100);
  }
};

const bindTouch = (el: HTMLElement | null) => {
  if (boundEl) {
    boundEl.removeEventListener('touchstart', onTouchStart);
    boundEl.removeEventListener('touchmove', onTouchMove);
    boundEl.removeEventListener('touchend', onTouchEnd);
    boundEl.removeEventListener('touchcancel', onTouchEnd);
    boundEl = null;
  }
  if (!el) return;
  boundEl = el;
  el.addEventListener('touchstart', onTouchStart, { passive: true });
  el.addEventListener('touchmove', onTouchMove, { passive: false });
  el.addEventListener('touchend', onTouchEnd, { passive: true });
  el.addEventListener('touchcancel', onTouchEnd, { passive: true });
};

const updateBinding = () => {
  bindTouch(resolveScrollEl());
};

const isMobileViewport = () =>
  typeof window !== 'undefined' && window.matchMedia('(max-width: 767px)').matches;

onMounted(() => {
  if (isMobileViewport()) updateBinding();
});

onUnmounted(() => {
  bindTouch(null);
});

watch(
  () => [registration.value, props.defaultScrollEl] as const,
  () => updateBinding(),
  { flush: 'post' }
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
