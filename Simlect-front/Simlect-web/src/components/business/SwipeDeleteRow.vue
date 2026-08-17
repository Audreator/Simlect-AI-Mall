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

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = withDefaults(
  defineProps<{
    open?: boolean;
    actionWidth?: number;

    deletable?: boolean;
  }>(),
  { open: false, actionWidth: 72, deletable: true }
);

const effectiveActionWidth = computed(() => (props.deletable ? props.actionWidth : 0));

const emit = defineEmits<{
  open: [];
  close: [];
  delete: [];
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
  const w = effectiveActionWidth.value;
  if (!w) return 0;
  return Math.min(1, Math.abs(offsetX.value) / w);
});

const actionOpacity = computed(() => {
  const p = swipeProgress.value;
  if (p <= 0) return 0;
  return 0.35 + 0.65 * p;
});

const actionScale = computed(() => 0.88 + 0.12 * swipeProgress.value);

const actionClickable = computed(() => swipeProgress.value > 0.45);

const rowStyle = computed(() => ({
  '--swipe-row-w': rowWidth.value ? `${rowWidth.value}px` : '100%',
}));

const clampOffset = (x: number) => {
  const w = effectiveActionWidth.value;
  if (!w) return 0;
  return Math.min(0, Math.max(-w, x));
};

const snap = () => {
  const w = effectiveActionWidth.value;
  if (!w) {
    offsetX.value = 0;
    emit('close');
    return;
  }
  if (offsetX.value <= -w / 2) {
    offsetX.value = -w;
    emit('open');
  } else {
    offsetX.value = 0;
    emit('close');
  }
};

const measureRow = () => {
  rowWidth.value = rootRef.value?.clientWidth ?? 0;
};

watch(
  () => props.open,
  (open) => {
    if (!isDragging.value) {
      const w = effectiveActionWidth.value;
      offsetX.value = open && w ? -w : 0;
    }
  },
  { immediate: true }
);

const onTouchStart = (e: TouchEvent) => {
  if (!props.deletable || e.touches.length !== 1) return;
  isDragging.value = true;
  tracking = true;
  startX = e.touches[0].clientX;
  startOffset = offsetX.value;
};

const onTouchMove = (e: TouchEvent) => {
  if (!tracking || e.touches.length !== 1) return;
  const dx = e.touches[0].clientX - startX;
  offsetX.value = clampOffset(startOffset + dx);
  if (Math.abs(dx) > 6) e.preventDefault();
};

const onTouchEnd = () => {
  if (!tracking) return;
  tracking = false;
  isDragging.value = false;
  snap();
};

const onMouseMove = (e: MouseEvent) => {
  if (!tracking) return;
  const dx = e.clientX - startX;
  offsetX.value = clampOffset(startOffset + dx);
};

const onMouseUp = () => {
  if (!tracking) return;
  tracking = false;
  isDragging.value = false;
  document.removeEventListener('mousemove', onMouseMove);
  document.removeEventListener('mouseup', onMouseUp);
  snap();
};

const onMouseDown = (e: MouseEvent) => {
  if (!props.deletable || e.button !== 0) return;
  const target = e.target as HTMLElement;
  if (target.closest('input, button, textarea, a, .el-checkbox')) return;

  isDragging.value = true;
  tracking = true;
  startX = e.clientX;
  startOffset = offsetX.value;
  document.addEventListener('mousemove', onMouseMove);
  document.addEventListener('mouseup', onMouseUp);
};

const onDelete = () => {
  emit('delete');
  offsetX.value = 0;
  emit('close');
};

onMounted(() => {
  measureRow();
  if (typeof ResizeObserver !== 'undefined' && rootRef.value) {
    resizeObserver = new ResizeObserver(measureRow);
    resizeObserver.observe(rootRef.value);
  }
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
