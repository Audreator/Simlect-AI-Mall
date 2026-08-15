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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed } from 'vue';
// [zh] 导入 `@element-plus/icons-vue`
import { Picture } from '@element-plus/icons-vue';
// [zh] 导入 `@/utils/image`
import { pickProductCover, resolveImageUrl } from '@/utils/image';

// [zh] 声明变量 `props`
const props = withDefaults(
  // [zh] 代码块开始
  defineProps<{
    // [zh] 语句
    source?: string | null;
    // [zh] 语句
    product?: Record<string, any>;
    // [zh] 语句
    width?: number | string;
    // [zh] 语句
    height?: number | string;
    // [zh] 语句
    fit?: 'cover' | 'contain' | 'fill';
    // [zh] 语句
    lazy?: boolean;
    // [zh] 语句
    useThumbnail?: boolean;

    // [zh] 语句
    dense?: boolean;
  // [zh] TypeScript/JS 语句：`}>(),`
  }>(),
  // [zh] TypeScript/JS 语句：`{ fit: 'cover', lazy: true, useThumbnail: true, dense: `
  { fit: 'cover', lazy: true, useThumbnail: true, dense: false }
// [zh] 语句
);

// [zh] 声明变量 `src`
const src = computed(() => {
  // [zh] 声明变量 `raw`
  const raw = props.source ?? (props.product ? pickProductCover(props.product) : '');
  // [zh] return 返回
  return resolveImageUrl(raw, { useThumbnail: props.useThumbnail });
// [zh] 语句
});

// [zh] 声明变量 `toCssSize`
const toCssSize = (val?: number | string) => {
  // [zh] 条件判断 if
  if (val == null || val === '') return undefined;
  // [zh] 条件判断 if
  if (typeof val === 'number' && Number.isFinite(val)) return `${val}px`;
  // [zh] 声明变量 `text`
  const text = String(val).trim();
  // [zh] 条件判断 if
  if (/^\d+(\.\d+)?$/.test(text)) return `${text}px`;
  // [zh] return 返回
  return text;
// [zh] 语句
};

// [zh] 声明变量 `sizeStyle`
const sizeStyle = computed(() => ({
  // [zh] TypeScript/JS 语句：`width: toCssSize(props.width) ?? '100%',`
  width: toCssSize(props.width) ?? '100%',
  // [zh] TypeScript/JS 语句：`height: toCssSize(props.height) ?? '100%'`
  height: toCssSize(props.height) ?? '100%'
// [zh] 语句
}));

// [zh] 声明变量 `iconSize`
const iconSize = computed(() => {
  // [zh] 声明变量 `w`
  const w = Number(props.width);
  // [zh] 条件判断 if
  if (!Number.isNaN(w) && w > 0) return Math.min(32, Math.max(20, Math.floor(w / 3)));
  // [zh] return 返回
  return 28;
// [zh] 语句
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
