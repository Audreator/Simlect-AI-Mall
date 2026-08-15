<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
    <!-- [zh] 开始标签 `<Transition>` -->
    <Transition name="eshop-lightbox-fade">
      <!-- [zh] 开始标签 `<div>` -->
      <div
        v-if="imagePreviewState.visible"
        class="eshop-image-lightbox allow-pinch-zoom"
        role="dialog"
        aria-modal="true"
        @click="closeImagePreview"
        @wheel.prevent="onWheel"
        @mouseup="onMouseUp"
        @mouseleave="onMouseUp"
      >
        <!-- [zh] 开始标签 `<img>` -->
        <img
          :key="currentUrl"
          :src="currentUrl"
          class="eshop-image-lightbox__img allow-pinch-zoom"
          :style="imgStyle"
          alt=""
          draggable="false"
          @click.stop
          @touchstart="onTouchStart"
          @touchmove="onTouchMove"
          @touchend="onTouchEnd"
          @touchcancel="onTouchEnd"
          @mousedown="onMouseDown"
          @mousemove="onMouseMove"
        />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</Transition>` -->
    </Transition>
  <!-- [zh] 闭合标签 `</Teleport>` -->
  </Teleport>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted, onUnmounted, watch } from 'vue';
// [zh] 导入 `@/composables/imagePreview`
import { closeImagePreview, imagePreviewState } from '@/composables/imagePreview';
// [zh] 导入 `@/composables/useImageLightboxZoom`
import { useImageLightboxZoom } from '@/composables/useImageLightboxZoom';

// [zh] 声明变量 `currentUrl`
const currentUrl = computed(() => imagePreviewState.urls[imagePreviewState.index] ?? '');

// [zh] 代码块开始
const {
  // [zh] TypeScript/JS 语句：`imgStyle,`
  imgStyle,
  // [zh] TypeScript/JS 语句：`resetTransform,`
  resetTransform,
  // [zh] TypeScript/JS 语句：`onWheel,`
  onWheel,
  // [zh] TypeScript/JS 语句：`onTouchStart,`
  onTouchStart,
  // [zh] TypeScript/JS 语句：`onTouchMove,`
  onTouchMove,
  // [zh] TypeScript/JS 语句：`onTouchEnd,`
  onTouchEnd,
  // [zh] TypeScript/JS 语句：`onMouseDown,`
  onMouseDown,
  // [zh] TypeScript/JS 语句：`onMouseMove,`
  onMouseMove,
  // [zh] TypeScript/JS 语句：`onMouseUp`
  onMouseUp
// [zh] 语句
} = useImageLightboxZoom();

// [zh] 声明变量 `prev`
const prev = () => {
  // [zh] 声明变量 `n`
  const n = imagePreviewState.urls.length;
  // [zh] 条件判断 if
  if (n <= 1) return;
  // [zh] 语句
  imagePreviewState.index = (imagePreviewState.index - 1 + n) % n;
// [zh] 语句
};

// [zh] 声明变量 `next`
const next = () => {
  // [zh] 声明变量 `n`
  const n = imagePreviewState.urls.length;
  // [zh] 条件判断 if
  if (n <= 1) return;
  // [zh] 语句
  imagePreviewState.index = (imagePreviewState.index + 1) % n;
// [zh] 语句
};

// [zh] 声明变量 `onKeydown`
const onKeydown = (e: KeyboardEvent) => {
  // [zh] 条件判断 if
  if (!imagePreviewState.visible) return;
  // [zh] 条件判断 if
  if (e.key === 'Escape') closeImagePreview();
  // [zh] else 分支
  else if (e.key === 'ArrowLeft') prev();
  // [zh] else 分支
  else if (e.key === 'ArrowRight') next();
// [zh] 语句
};

// [zh] 语句
watch(currentUrl, () => resetTransform());

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => imagePreviewState.visible,`
  () => imagePreviewState.visible,
  // [zh] 代码块开始
  (open) => {
    // [zh] 语句
    document.body.style.overflow = open ? 'hidden' : '';
    // [zh] 条件判断 if
    if (!open) resetTransform();
  // [zh] 语法符号
  }
// [zh] 语句
);

// [zh] 语句
onMounted(() => window.addEventListener('keydown', onKeydown));
// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 语句
  window.removeEventListener('keydown', onKeydown);
  // [zh] 语句
  document.body.style.overflow = '';
  // [zh] 语句
  resetTransform();
// [zh] 语句
});
</script>

/* [zh] 样式声明 */
<style lang="scss">
/* [zh] 样式规则 `.eshop-image-lightbox {` */
.eshop-image-lightbox {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `z-index: 4000;` */
  z-index: 4000;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;
  /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.88);` */
  background: rgba(0, 0, 0, 0.88);
  /* [zh] 样式规则 `cursor: zoom-out;` */
  cursor: zoom-out;

  /* [zh] 样式规则 `&__img {` */
  &__img {
    /* [zh] 样式规则 `max-width: 100%;` */
    max-width: 100%;
    /* [zh] 样式规则 `max-height: 100%;` */
    max-height: 100%;
    /* [zh] 样式规则 `object-fit: contain;` */
    object-fit: contain;
    /* [zh] 样式规则 `user-select: none;` */
    user-select: none;
    /* [zh] 样式规则 `-webkit-user-drag: none;` */
    -webkit-user-drag: none;
    /* [zh] 样式规则 `transform-origin: center center;` */
    transform-origin: center center;
    /* [zh] 样式规则 `will-change: transform;` */
    will-change: transform;
    /* [zh] 样式规则 `touch-action: none;` */
    touch-action: none;
    /* [zh] 样式规则 `cursor: grab;` */
    cursor: grab;

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `cursor: grabbing;` */
      cursor: grabbing;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.eshop-lightbox-fade-enter-active,
/* [zh] 样式规则 `.eshop-lightbox-fade-leave-active {` */
.eshop-lightbox-fade-leave-active {
  /* [zh] 样式规则 `transition: opacity 0.2s ease;` */
  transition: opacity 0.2s ease;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.eshop-lightbox-fade-enter-from,
/* [zh] 样式规则 `.eshop-lightbox-fade-leave-to {` */
.eshop-lightbox-fade-leave-to {
  /* [zh] 样式规则 `opacity: 0;` */
  opacity: 0;
/* [zh] 样式规则 `}` */
}
</style>
