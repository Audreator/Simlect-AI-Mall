<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
    <!-- [zh] 开始标签 `<Transition>` -->
    <Transition name="eshop-lightbox-fade">
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
      </div>
    </Transition>
  <!-- [zh] 闭合标签 `</Teleport>` -->
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue';
import { closeImagePreview, imagePreviewState } from '@/composables/imagePreview';
import { useImageLightboxZoom } from '@/composables/useImageLightboxZoom';

const currentUrl = computed(() => imagePreviewState.urls[imagePreviewState.index] ?? '');

const {
  imgStyle,
  resetTransform,
  onWheel,
  onTouchStart,
  onTouchMove,
  onTouchEnd,
  onMouseDown,
  onMouseMove,
  onMouseUp
} = useImageLightboxZoom();

const prev = () => {
  const n = imagePreviewState.urls.length;
  if (n <= 1) return;
  imagePreviewState.index = (imagePreviewState.index - 1 + n) % n;
};

const next = () => {
  const n = imagePreviewState.urls.length;
  if (n <= 1) return;
  imagePreviewState.index = (imagePreviewState.index + 1) % n;
};

const onKeydown = (e: KeyboardEvent) => {
  if (!imagePreviewState.visible) return;
  if (e.key === 'Escape') closeImagePreview();
  else if (e.key === 'ArrowLeft') prev();
  else if (e.key === 'ArrowRight') next();
};

watch(currentUrl, () => resetTransform());

watch(
  () => imagePreviewState.visible,
  (open) => {
    document.body.style.overflow = open ? 'hidden' : '';
    if (!open) resetTransform();
  }
);

onMounted(() => window.addEventListener('keydown', onKeydown));
onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown);
  document.body.style.overflow = '';
  resetTransform();
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
