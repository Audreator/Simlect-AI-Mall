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
        <!-- [zh] 模板内容：`class="swipe-actions"` -->
        class="swipe-actions"
        <!-- [zh] 模板内容：`:style="{` -->
        :style="{
          <!-- [zh] 模板内容：`width: `${actionWidth}px`,` -->
          width: `${actionWidth}px`,
          <!-- [zh] 模板内容：`opacity: actionOpacity,` -->
          opacity: actionOpacity,
          <!-- [zh] 模板内容：`transform: `scale(${actionScale})`` -->
          transform: `scale(${actionScale})`
        <!-- [zh] 模板内容：`}"` -->
        }"
      <!-- [zh] 模板内容：`>` -->
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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

// [zh] 声明变量 `props`
const props = withDefaults(
  // [zh] 代码块开始
  defineProps<{
    // [zh] 语句
    open?: boolean;
    // [zh] 语句
    actionWidth?: number;
  // [zh] TypeScript/JS 语句：`}>(),`
  }>(),
  // [zh] TypeScript/JS 语句：`{ open: false, actionWidth: 148 }`
  { open: false, actionWidth: 148 }
// [zh] 语句
);

// [zh] 声明变量 `emit`
const emit = defineEmits<{
  // [zh] 语句
  open: [];
  // [zh] 语句
  close: [];
// [zh] 语句
}>();

// [zh] 声明变量 `rootRef`
const rootRef = ref<HTMLElement | null>(null);
// [zh] 声明变量 `rowWidth`
const rowWidth = ref(0);
// [zh] 声明变量 `offsetX`
const offsetX = ref(0);
// [zh] 声明变量 `isDragging`
const isDragging = ref(false);

// [zh] 声明变量 `startX`
let startX = 0;
// [zh] 声明变量 `startOffset`
let startOffset = 0;
// [zh] 声明变量 `tracking`
let tracking = false;
// [zh] 声明变量 `resizeObserver`
let resizeObserver: ResizeObserver | null = null;

// [zh] 声明变量 `swipeProgress`
const swipeProgress = computed(() => {
  // [zh] 声明变量 `w`
  const w = props.actionWidth;
  // [zh] 条件判断 if
  if (!w) return 0;
  // [zh] return 返回
  return Math.min(1, Math.abs(offsetX.value) / w);
// [zh] 语句
});

// [zh] 声明变量 `actionOpacity`
const actionOpacity = computed(() => {
  // [zh] 声明变量 `p`
  const p = swipeProgress.value;
  // [zh] 条件判断 if
  if (p <= 0) return 0;
  // [zh] return 返回
  return 0.35 + 0.65 * p;
// [zh] 语句
});

// [zh] 声明变量 `actionScale`
const actionScale = computed(() => 0.88 + 0.12 * swipeProgress.value);

// [zh] 声明变量 `rowStyle`
const rowStyle = computed(() => ({
  // [zh] TypeScript/JS 语句：`'--swipe-row-w': rowWidth.value ? `${rowWidth.value}px``
  '--swipe-row-w': rowWidth.value ? `${rowWidth.value}px` : '100%',
  // [zh] TypeScript/JS 语句：`'--swipe-action-w': `${props.actionWidth}px``
  '--swipe-action-w': `${props.actionWidth}px`
// [zh] 语句
}));

// [zh] 声明变量 `clampOffset`
const clampOffset = (x: number) => {
  // [zh] 声明变量 `w`
  const w = props.actionWidth;
  // [zh] 条件判断 if
  if (!w) return 0;
  // [zh] return 返回
  return Math.min(0, Math.max(-w, x));
// [zh] 语句
};

// [zh] 声明变量 `snap`
const snap = () => {
  // [zh] 声明变量 `w`
  const w = props.actionWidth;
  // [zh] 条件判断 if
  if (!w) {
    // [zh] 语句
    offsetX.value = 0;
    // [zh] 语句
    emit('close');
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (Math.abs(offsetX.value) > w * 0.4) {
    // [zh] 语句
    offsetX.value = -w;
    // [zh] 语句
    emit('open');
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    offsetX.value = 0;
    // [zh] 语句
    emit('close');
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => props.open,`
  () => props.open,
  // [zh] 代码块开始
  (v) => {
    // [zh] 语句
    offsetX.value = v ? -props.actionWidth : 0;
  // [zh] 语法符号
  }
// [zh] 语句
);

// [zh] 声明变量 `onTouchStart`
const onTouchStart = (e: TouchEvent) => {
  // [zh] 语句
  tracking = true;
  // [zh] 语句
  isDragging.value = true;
  // [zh] 语句
  startX = e.touches[0].clientX;
  // [zh] 语句
  startOffset = offsetX.value;
// [zh] 语句
};

// [zh] 声明变量 `onTouchMove`
const onTouchMove = (e: TouchEvent) => {
  // [zh] 条件判断 if
  if (!tracking) return;
  // [zh] 声明变量 `dx`
  const dx = e.touches[0].clientX - startX;
  // [zh] 语句
  offsetX.value = clampOffset(startOffset + dx);
// [zh] 语句
};

// [zh] 声明变量 `onTouchEnd`
const onTouchEnd = () => {
  // [zh] 条件判断 if
  if (!tracking) return;
  // [zh] 语句
  tracking = false;
  // [zh] 语句
  isDragging.value = false;
  // [zh] 语句
  snap();
// [zh] 语句
};

// [zh] 声明变量 `onMouseDown`
const onMouseDown = (e: MouseEvent) => {
  // [zh] 条件判断 if
  if (e.button !== 0) return;
  // [zh] 语句
  tracking = true;
  // [zh] 语句
  isDragging.value = true;
  // [zh] 语句
  startX = e.clientX;
  // [zh] 语句
  startOffset = offsetX.value;
  // [zh] 语句
  document.addEventListener('mousemove', onMouseMove);
  // [zh] 语句
  document.addEventListener('mouseup', onMouseUp);
// [zh] 语句
};

// [zh] 声明变量 `onMouseMove`
const onMouseMove = (e: MouseEvent) => {
  // [zh] 条件判断 if
  if (!tracking) return;
  // [zh] 语句
  offsetX.value = clampOffset(startOffset + e.clientX - startX);
// [zh] 语句
};

// [zh] 声明变量 `onMouseUp`
const onMouseUp = () => {
  // [zh] 条件判断 if
  if (!tracking) return;
  // [zh] 语句
  tracking = false;
  // [zh] 语句
  isDragging.value = false;
  // [zh] 语句
  document.removeEventListener('mousemove', onMouseMove);
  // [zh] 语句
  document.removeEventListener('mouseup', onMouseUp);
  // [zh] 语句
  snap();
// [zh] 语句
};

// [zh] 代码块开始
onMounted(() => {
  // [zh] 声明变量 `el`
  const el = rootRef.value;
  // [zh] 条件判断 if
  if (!el) return;
  // [zh] 声明变量 `measure`
  const measure = () => {
    // [zh] 语句
    rowWidth.value = el.clientWidth;
  // [zh] 语句
  };
  // [zh] 语句
  measure();
  // [zh] 语句
  resizeObserver = new ResizeObserver(measure);
  // [zh] 语句
  resizeObserver.observe(el);
// [zh] 语句
});

// [zh] 代码块开始
onBeforeUnmount(() => {
  // [zh] 语句
  resizeObserver?.disconnect();
  // [zh] 语句
  document.removeEventListener('mousemove', onMouseMove);
  // [zh] 语句
  document.removeEventListener('mouseup', onMouseUp);
// [zh] 语句
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
