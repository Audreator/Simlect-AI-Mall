<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<article>` -->
  <article class="pc-product-tile" @click="$emit('click', product)">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="pc-product-tile__cover">
      <!-- [zh] 开始标签 `<ProductImage>` -->
      <ProductImage :product="product" fit="contain" width="100%" height="100%" />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p class="pc-product-tile__name">{{ product.productName || product.name }}</p>
    <!-- [zh] 开始标签 `<p>` -->
    <p class="pc-product-tile__price">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="sym">¥</span>{{ displayPrice }}
    </p>
  <!-- [zh] 闭合标签 `</article>` -->
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import ProductImage from '@/components/common/ProductImage.vue';

const props = defineProps<{ product: Record<string, any> }>();
defineEmits<{ click: [Record<string, any>] }>();

const displayPrice = computed(() => {
  const p = props.product.price ?? props.product.salePrice ?? props.product.minPrice;
  return p != null ? Number(p).toFixed(2) : '--';
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-product-tile {` */
.pc-product-tile {
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `padding: 10px 10px 12px;` */
  padding: 10px 10px 12px;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `transition: border-color $transition-fas` */
  transition: border-color $transition-fast, box-shadow 0.45s cubic-bezier(0.25, 0.1, 0.25, 1), transform 0.45s cubic-bezier(0.25, 0.1, 0.25, 1);

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.25)` */
    border-color: rgba($color-primary, 0.25);
    /* [zh] 样式规则 `box-shadow: $shadow-card-hover;` */
    box-shadow: $shadow-card-hover;
    /* [zh] 样式规则 `transform: translateY(-3px);` */
    transform: translateY(-3px);

    /* [zh] 样式规则 `.pc-product-tile__cover :deep(img) {` */
    .pc-product-tile__cover :deep(img) {
      /* [zh] 样式规则 `transform: scale(1.04);` */
      transform: scale(1.04);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&__cover {` */
  &__cover {
    /* [zh] 样式规则 `aspect-ratio: 1;` */
    aspect-ratio: 1;
    /* [zh] 样式规则 `margin-bottom: 8px;` */
    margin-bottom: 8px;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `background: #fafafa;` */
    background: #fafafa;

    /* [zh] 样式声明 */
    :deep(.product-image),
    /* [zh] 样式声明 */
    :deep(.el-image),
    /* [zh] 样式规则 `:deep(.el-image__inner) {` */
    :deep(.el-image__inner) {
      /* [zh] 样式规则 `width: 100% !important;` */
      width: 100% !important;
      /* [zh] 样式规则 `height: 100% !important;` */
      height: 100% !important;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(img) {` */
    :deep(img) {
      /* [zh] 样式规则 `transition: transform $transition-normal` */
      transition: transform $transition-normal;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&__name {` */
  &__name {
    /* [zh] 样式规则 `margin: 0 0 6px;` */
    margin: 0 0 6px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `line-height: 1.45;` */
    line-height: 1.45;
    /* [zh] 样式规则 `color: $color-text-primary;` */
    color: $color-text-primary;
    /* [zh] 样式规则 `display: -webkit-box;` */
    display: -webkit-box;
    /* [zh] 样式规则 `-webkit-line-clamp: 2;` */
    -webkit-line-clamp: 2;
    /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
    -webkit-box-orient: vertical;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `word-break: normal;` */
    word-break: normal;
    /* [zh] 样式规则 `overflow-wrap: break-word;` */
    overflow-wrap: break-word;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&__price {` */
  &__price {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 18px;` */
    font-size: 18px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;

    /* [zh] 样式规则 `.sym {` */
    .sym {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
