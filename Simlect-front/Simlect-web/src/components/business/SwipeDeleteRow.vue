<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div ref="rootRef" class="swipe-delete-row" :style="rowStyle">
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
        v-if="deletable"
        class="swipe-action"
        :style="{
          width: `${effectiveActionWidth}px`,
          opacity: actionOpacity,
          transform: `scale(${actionScale})`,
        }"
      >
        <!-- [zh] 开始标签 `<button>` -->
        <button
          type="button"
          class="swipe-del-btn"
          :style="{ pointerEvents: actionClickable ? 'auto' : 'none' }"
          @click.stop="onDelete"
        >
          <!-- [zh] 模板内容：`删除` -->
          删除
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
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

    // [zh] 语句
    deletable?: boolean;
  // [zh] TypeScript/JS 语句：`}>(),`
  }>(),
  // [zh] TypeScript/JS 语句：`{ open: false, actionWidth: 72, deletable: true }`
  { open: false, actionWidth: 72, deletable: true }
// [zh] 语句
);

// [zh] 声明变量 `effectiveActionWidth`
const effectiveActionWidth = computed(() => (props.deletable ? props.actionWidth : 0));

// [zh] 声明变量 `emit`
const emit = defineEmits<{
  // [zh] 语句
  open: [];
  // [zh] 语句
  close: [];
  // [zh] 语句
  delete: [];
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
  const w = effectiveActionWidth.value;
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

// [zh] 声明变量 `actionClickable`
const actionClickable = computed(() => swipeProgress.value > 0.45);

// [zh] 声明变量 `rowStyle`
const rowStyle = computed(() => ({
  // [zh] TypeScript/JS 语句：`'--swipe-row-w': rowWidth.value ? `${rowWidth.value}px``
  '--swipe-row-w': rowWidth.value ? `${rowWidth.value}px` : '100%',
// [zh] 语句
}));

// [zh] 声明变量 `clampOffset`
const clampOffset = (x: number) => {
  // [zh] 声明变量 `w`
  const w = effectiveActionWidth.value;
  // [zh] 条件判断 if
  if (!w) return 0;
  // [zh] return 返回
  return Math.min(0, Math.max(-w, x));
// [zh] 语句
};

// [zh] 声明变量 `snap`
const snap = () => {
  // [zh] 声明变量 `w`
  const w = effectiveActionWidth.value;
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
  if (offsetX.value <= -w / 2) {
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

// [zh] 声明变量 `measureRow`
const measureRow = () => {
  // [zh] 语句
  rowWidth.value = rootRef.value?.clientWidth ?? 0;
// [zh] 语句
};

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => props.open,`
  () => props.open,
  // [zh] 代码块开始
  (open) => {
    // [zh] 条件判断 if
    if (!isDragging.value) {
      // [zh] 声明变量 `w`
      const w = effectiveActionWidth.value;
      // [zh] 语句
      offsetX.value = open && w ? -w : 0;
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`{ immediate: true }`
  { immediate: true }
// [zh] 语句
);

// [zh] 声明变量 `onTouchStart`
const onTouchStart = (e: TouchEvent) => {
  // [zh] 条件判断 if
  if (!props.deletable || e.touches.length !== 1) return;
  // [zh] 语句
  isDragging.value = true;
  // [zh] 语句
  tracking = true;
  // [zh] 语句
  startX = e.touches[0].clientX;
  // [zh] 语句
  startOffset = offsetX.value;
// [zh] 语句
};

// [zh] 声明变量 `onTouchMove`
const onTouchMove = (e: TouchEvent) => {
  // [zh] 条件判断 if
  if (!tracking || e.touches.length !== 1) return;
  // [zh] 声明变量 `dx`
  const dx = e.touches[0].clientX - startX;
  // [zh] 语句
  offsetX.value = clampOffset(startOffset + dx);
  // [zh] 条件判断 if
  if (Math.abs(dx) > 6) e.preventDefault();
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

// [zh] 声明变量 `onMouseMove`
const onMouseMove = (e: MouseEvent) => {
  // [zh] 条件判断 if
  if (!tracking) return;
  // [zh] 声明变量 `dx`
  const dx = e.clientX - startX;
  // [zh] 语句
  offsetX.value = clampOffset(startOffset + dx);
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

// [zh] 声明变量 `onMouseDown`
const onMouseDown = (e: MouseEvent) => {
  // [zh] 条件判断 if
  if (!props.deletable || e.button !== 0) return;
  // [zh] 声明变量 `target`
  const target = e.target as HTMLElement;
  // [zh] 条件判断 if
  if (target.closest('input, button, textarea, a, .el-checkbox')) return;

  // [zh] 语句
  isDragging.value = true;
  // [zh] 语句
  tracking = true;
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

// [zh] 声明变量 `onDelete`
const onDelete = () => {
  // [zh] 语句
  emit('delete');
  // [zh] 语句
  offsetX.value = 0;
  // [zh] 语句
  emit('close');
// [zh] 语句
};

// [zh] 代码块开始
onMounted(() => {
  // [zh] 语句
  measureRow();
  // [zh] 条件判断 if
  if (typeof ResizeObserver !== 'undefined' && rootRef.value) {
    // [zh] 语句
    resizeObserver = new ResizeObserver(measureRow);
    // [zh] 语句
    resizeObserver.observe(rootRef.value);
  // [zh] 语法符号
  }
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

/* [zh] 样式规则 `.swipe-delete-row {` */
.swipe-delete-row {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.swipe-track {` */
.swipe-track {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: row;` */
  flex-direction: row;
  /* [zh] 样式规则 `flex-wrap: nowrap;` */
  flex-wrap: nowrap;
  /* [zh] 样式规则 `width: max-content;` */
  width: max-content;
  /* [zh] 样式规则 `will-change: transform;` */
  will-change: transform;
  /* [zh] 样式规则 `transition: transform 0.28s cubic-bezier` */
  transition: transform 0.28s cubic-bezier(0.32, 0.72, 0, 1);
  /* [zh] 样式规则 `touch-action: pan-y;` */
  touch-action: pan-y;

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
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.swipe-action {` */
.swipe-action {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: stretch;` */
  align-items: stretch;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `transform-origin: center center;` */
  transform-origin: center center;
  /* [zh] 样式规则 `transition: opacity 0.12s ease-out, tran` */
  transition: opacity 0.12s ease-out, transform 0.12s ease-out;

  /* [zh] 样式规则 `.swipe-track.dragging & {` */
  .swipe-track.dragging & {
    /* [zh] 样式规则 `transition: none;` */
    transition: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.swipe-del-btn {` */
.swipe-del-btn {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `min-height: 100%;` */
  min-height: 100%;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 0 $radius-card $radius-ca` */
  border-radius: 0 $radius-card $radius-card 0;
  /* [zh] 样式规则 `background: $color-price;` */
  background: $color-price;
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
  -webkit-tap-highlight-color: transparent;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `filter: brightness(0.92);` */
    filter: brightness(0.92);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
