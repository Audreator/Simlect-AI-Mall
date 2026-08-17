<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-image>` -->
  <el-image
    :src="src"
    :fit="fit"
    :lazy="lazy"
    class="product-image"
    :class="{ 'is-dense': dense }"
    :style="sizeStyle"
  >
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #placeholder>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="img-placeholder">
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon :size="iconSize"><Picture /></el-icon>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #error>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="img-placeholder">
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon :size="iconSize"><Picture /></el-icon>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>
  </el-image>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Picture } from '@element-plus/icons-vue';
import { pickProductCover, resolveImageUrl } from '@/utils/image';

const props = withDefaults(
  defineProps<{
    source?: string | null;
    product?: Record<string, any>;
    width?: number | string;
    height?: number | string;
    fit?: 'cover' | 'contain' | 'fill';
    lazy?: boolean;
    useThumbnail?: boolean;

    dense?: boolean;
  }>(),
  { fit: 'cover', lazy: true, useThumbnail: true, dense: false }
);

const src = computed(() => {
  const raw = props.source ?? (props.product ? pickProductCover(props.product) : '');
  return resolveImageUrl(raw, { useThumbnail: props.useThumbnail });
});

const toCssSize = (val?: number | string) => {
  if (val == null || val === '') return undefined;
  if (typeof val === 'number' && Number.isFinite(val)) return `${val}px`;
  const text = String(val).trim();
  if (/^\d+(\.\d+)?$/.test(text)) return `${text}px`;
  return text;
};

const sizeStyle = computed(() => ({
  width: toCssSize(props.width) ?? '100%',
  height: toCssSize(props.height) ?? '100%'
}));

const iconSize = computed(() => {
  const w = Number(props.width);
  if (!Number.isNaN(w) && w > 0) return Math.min(32, Math.max(20, Math.floor(w / 3)));
  return 28;
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.product-image {` */
.product-image {
  /* [zh] 样式规则 `display: block;` */
  display: block;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;

  /* [zh] 样式规则 `:deep(.el-image) {` */
  :deep(.el-image) {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `display: block;` */
    display: block;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.el-image__inner) {` */
  :deep(.el-image__inner) {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `max-width: 100%;` */
    max-width: 100%;
    /* [zh] 样式规则 `max-height: 100%;` */
    max-height: 100%;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.img-placeholder {` */
.img-placeholder {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
  /* [zh] 样式规则 `min-height: 60px;` */
  min-height: 60px;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `color: #ccc;` */
  color: #ccc;
  /* [zh] 样式规则 `background: linear-gradient(135deg, #fff` */
  background: linear-gradient(135deg, #ffffff, #f8f8f8);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-image.is-dense .img-placeholder` */
.product-image.is-dense .img-placeholder {
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
/* [zh] 样式规则 `}` */
}
</style>
