<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div ref="rootRef" class="swipe-actions-row" :style="rowStyle">
    <!-- [zh] 开始标签 `<div>` -->
    <div
      class="swipe-track"
      :class="{ dragging: isDragging }"
      :style="{ transform: `translate3d(${offsetX}px, 0, 0)` }"
      @touchstart="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
      @touchcancel="onTouchEnd"
      @mousedown="onMouseDown"
    >
      <!-- [zh] 开始标签 `<div>` -->
      <div class="swipe-content">
        <!-- [zh] 开始标签 `<slot>` -->
        <slot />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div
        v-if="actionWidth > 0"
        class="swipe-actions"
        :style="{
          width: `${actionWidth}px`,
          opacity: actionOpacity,
          transform: `scale(${actionScale})`
        }"
      >
        <!-- [zh] 开始标签 `<slot>` -->
        <slot name="actions" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = withDefaults(
  defineProps<{
    open?: boolean;
    actionWidth?: number;
  }>(),
  { open: false, actionWidth: 148 }
);

const emit = defineEmits<{
  open: [];
  close: [];
}>();

const rootRef = ref<HTMLElement | null>(null);
const rowWidth = ref(0);
const offsetX = ref(0);
const isDragging = ref(false);

let startX = 0;
let startOffset = 0;
let tracking = false;
let resizeObserver: ResizeObserver | null = null;

const swipeProgress = computed(() => {
  const w = props.actionWidth;
  if (!w) return 0;
  return Math.min(1, Math.abs(offsetX.value) / w);
});

const actionOpacity = computed(() => {
  const p = swipeProgress.value;
  if (p <= 0) return 0;
  return 0.35 + 0.65 * p;
});

const actionScale = computed(() => 0.88 + 0.12 * swipeProgress.value);

const rowStyle = computed(() => ({
  '--swipe-row-w': rowWidth.value ? `${rowWidth.value}px` : '100%',
  '--swipe-action-w': `${props.actionWidth}px`
}));

const clampOffset = (x: number) => {
  const w = props.actionWidth;
  if (!w) return 0;
  return Math.min(0, Math.max(-w, x));
};

const snap = () => {
  const w = props.actionWidth;
  if (!w) {
    offsetX.value = 0;
    emit('close');
    return;
  }
  if (Math.abs(offsetX.value) > w * 0.4) {
    offsetX.value = -w;
    emit('open');
  } else {
    offsetX.value = 0;
    emit('close');
  }
};

watch(
  () => props.open,
  (v) => {
    offsetX.value = v ? -props.actionWidth : 0;
  }
);

const onTouchStart = (e: TouchEvent) => {
  tracking = true;
  isDragging.value = true;
  startX = e.touches[0].clientX;
  startOffset = offsetX.value;
};

const onTouchMove = (e: TouchEvent) => {
  if (!tracking) return;
  const dx = e.touches[0].clientX - startX;
  offsetX.value = clampOffset(startOffset + dx);
};

const onTouchEnd = () => {
  if (!tracking) return;
  tracking = false;
  isDragging.value = false;
  snap();
};

const onMouseDown = (e: MouseEvent) => {
  if (e.button !== 0) return;
  tracking = true;
  isDragging.value = true;
  startX = e.clientX;
  startOffset = offsetX.value;
  document.addEventListener('mousemove', onMouseMove);
  document.addEventListener('mouseup', onMouseUp);
};

const onMouseMove = (e: MouseEvent) => {
  if (!tracking) return;
  offsetX.value = clampOffset(startOffset + e.clientX - startX);
};

const onMouseUp = () => {
  if (!tracking) return;
  tracking = false;
  isDragging.value = false;
  document.removeEventListener('mousemove', onMouseMove);
  document.removeEventListener('mouseup', onMouseUp);
  snap();
};

onMounted(() => {
  const el = rootRef.value;
  if (!el) return;
  const measure = () => {
    rowWidth.value = el.clientWidth;
  };
  measure();
  resizeObserver = new ResizeObserver(measure);
  resizeObserver.observe(el);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  document.removeEventListener('mousemove', onMouseMove);
  document.removeEventListener('mouseup', onMouseUp);
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.swipe-actions-row {` */
.swipe-actions-row {
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.swipe-track {` */
.swipe-track {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `width: calc(var(--swipe-row-w, 100%) + v` */
  width: calc(var(--swipe-row-w, 100%) + var(--swipe-action-w, 148px));
  /* [zh] 样式规则 `will-change: transform;` */
  will-change: transform;
  /* [zh] 样式规则 `transition: transform 0.22s ease;` */
  transition: transform 0.22s ease;

  /* [zh] 样式规则 `&.dragging {` */
  &.dragging {
    /* [zh] 样式规则 `transition: none;` */
    transition: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.swipe-content {` */
.swipe-content {
  /* [zh] 样式规则 `flex: 0 0 var(--swipe-row-w, 100%);` */
  flex: 0 0 var(--swipe-row-w, 100%);
  /* [zh] 样式规则 `width: var(--swipe-row-w, 100%);` */
  width: var(--swipe-row-w, 100%);
  /* [zh] 样式规则 `min-width: var(--swipe-row-w, 100%);` */
  min-width: var(--swipe-row-w, 100%);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.swipe-actions {` */
.swipe-actions {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: stretch;` */
  align-items: stretch;
  /* [zh] 样式规则 `transform-origin: right center;` */
  transform-origin: right center;

  /* [zh] 样式规则 `:deep(button) {` */
  :deep(button) {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-height: 100%;` */
    min-height: 100%;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
