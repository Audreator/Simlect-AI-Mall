<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<article>` -->
  <article
    class="product-card"
    :class="{ 'is-compact': compact, ignore: isDesktop }"
    @click="$emit('click', product)"
  >
    <!-- [zh] 开始标签 `<div>` -->
    <div class="cover-wrap">
      <!-- [zh] 开始标签 `<ProductImage>` -->
      <ProductImage
        :product="product"
        class="cover"
        width="100%"
        height="100%"
        :fit="resolvedFit"
        :lazy="imageLazy"
      />

      <!-- [zh] 开始标签 `<button>` -->
      <button
        type="button"
        class="cart-float"
        aria-label="选择规格"
        @click.stop="goSkuPick"
      >
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon :size="18"><ShoppingCart /></el-icon>
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="hover-mask">
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" round size="small" @click.stop="goSkuPick">
          <!-- [zh] 模板内容：`选规格` -->
          选规格
        <!-- [zh] 闭合标签 `</el-button>` -->
        </el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="info">
      <!-- [zh] 开始标签 `<h4>` -->
      <h4 class="name">
        <!-- [zh] Mustache 插值表达式 -->
        {{ product.productName || product.name }}
      <!-- [zh] 闭合标签 `</h4>` -->
      </h4>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="price-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="price"><span class="symbol">¥</span>{{ displayPrice }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="sale">销量 {{ product.totalSale ?? product.saleCount ?? 0 }}</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</article>` -->
  </article>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed } from 'vue';
// [zh] 导入 `@element-plus/icons-vue`
import { ShoppingCart } from '@element-plus/icons-vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/composables/useProductSkuSheet`
import { useProductSkuSheet } from '@/composables/useProductSkuSheet';

// [zh] 语句
const { isDesktop } = useDevice();

// [zh] 声明变量 `props`
const props = withDefaults(
  // [zh] 代码块开始
  defineProps<{
    // [zh] 语句
    product: Record<string, any>;
    // [zh] 语句
    compact?: boolean;

    // [zh] 语句
    imageFit?: 'cover' | 'contain' | 'fill';
    // [zh] 语句
    imageLazy?: boolean;
  // [zh] TypeScript/JS 语句：`}>(),`
  }>(),
  // [zh] TypeScript/JS 语句：`{ compact: false, imageLazy: true }`
  { compact: false, imageLazy: true }
// [zh] 语句
);

// [zh] 语句
const { open: openSkuSheet } = useProductSkuSheet();
// [zh] 声明变量 `resolvedFit`
const resolvedFit = computed(() => props.imageFit ?? (props.compact ? 'contain' : 'cover'));
// [zh] 语句
defineEmits<{ click: [Record<string, any>]; addToCart: [Record<string, any>] }>();

// [zh] 声明变量 `goSkuPick`
const goSkuPick = () => {
  // [zh] 声明变量 `id`
  const id = props.product?.productId;
  // [zh] 条件判断 if
  if (!id) return;
  // [zh] 语句
  openSkuSheet(id);
// [zh] 语句
};

// [zh] 声明变量 `displayPrice`
const displayPrice = computed(() => {
  // [zh] 声明变量 `p`
  const p = props.product.price ?? props.product.salePrice ?? props.product.minPrice;
  // [zh] return 返回
  return p != null ? Number(p).toFixed(2) : '--';
// [zh] 语句
});

</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.product-card:not(.ignore) {` */
.product-card:not(.ignore) {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;
  /* [zh] 样式规则 `transition: transform 0.3s ease,` */
  transition: transform 0.3s ease,
              /* [zh] 样式声明 */
              box-shadow 0.3s ease,
              /* [zh] 样式声明 */
              border-color 0.3s ease,
              /* [zh] 样式声明 */
              opacity 0.3s ease;

  /* [zh] 样式规则 `@media (hover: hover) {` */
  @media (hover: hover) {
    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `transform: translateY(-4px);` */
      transform: translateY(-4px);
      /* [zh] 样式规则 `box-shadow: 0 12px 24px rgba(0, 0, 0, 0.` */
      box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
      /* [zh] 样式规则 `border-color: rgba($color-gold, 0.3);` */
      border-color: rgba($color-gold, 0.3);

      /* [zh] 样式规则 `.hover-mask {` */
      .hover-mask {
        /* [zh] 样式规则 `opacity: 1;` */
        opacity: 1;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.cart-float {` */
      .cart-float {
        /* [zh] 样式规则 `opacity: 1;` */
        opacity: 1;
        /* [zh] 样式规则 `transform: scale(1);` */
        transform: scale(1);
        /* [zh] 样式规则 `box-shadow: 0 4px 12px rgba($color-gold,` */
        box-shadow: 0 4px 12px rgba($color-gold, 0.35);
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.97);` */
    transform: scale(0.97);
    /* [zh] 样式规则 `box-shadow: 0 4px 12px rgba(0, 0, 0, 0.0` */
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    /* [zh] 样式规则 `transition-duration: 0.1s;` */
    transition-duration: 0.1s;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:focus-visible {` */
  &:focus-visible {
    /* [zh] 样式规则 `outline: 2px solid $color-gold;` */
    outline: 2px solid $color-gold;
    /* [zh] 样式规则 `outline-offset: 3px;` */
    outline-offset: 3px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cover-wrap {` */
  .cover-wrap {
    /* [zh] 样式规则 `position: relative;` */
    position: relative;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `aspect-ratio: 1;` */
    aspect-ratio: 1;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;

    /* [zh] 样式规则 `:deep(.product-image) {` */
    :deep(.product-image) {
      /* [zh] 样式规则 `width: 100%;` */
      width: 100%;
      /* [zh] 样式规则 `height: 100%;` */
      height: 100%;
    /* [zh] 样式规则 `}` */
    }

  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cart-float {` */
  .cart-float {
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `bottom: 8px;` */
    bottom: 8px;
    /* [zh] 样式规则 `right: 8px;` */
    right: 8px;
    /* [zh] 样式规则 `z-index: 2;` */
    z-index: 2;
    /* [zh] 样式规则 `width: 32px;` */
    width: 32px;
    /* [zh] 样式规则 `height: 32px;` */
    height: 32px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `border-radius: 50%;` */
    border-radius: 50%;
    /* [zh] 样式规则 `background: $color-primary;` */
    background: $color-primary;
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `place-items: center;` */
    place-items: center;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `opacity: 1;` */
    opacity: 1;
    /* [zh] 样式规则 `transform: scale(1);` */
    transform: scale(1);
    /* [zh] 样式规则 `box-shadow: $shadow-xs;` */
    box-shadow: $shadow-xs;
    /* [zh] 样式规则 `transition: transform $transition-fast, ` */
    transition: transform $transition-fast, background $transition-fast;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: $color-primary-hover;` */
      background: $color-primary-hover;
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.hover-mask {` */
  .hover-mask {
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `inset: 0;` */
    inset: 0;
    /* [zh] 样式规则 `background: linear-gradient(180deg, tran` */
    background: linear-gradient(180deg, transparent 30%, rgba(0, 0, 0, 0.45));
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: flex-end;` */
    align-items: flex-end;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `padding-bottom: 16px;` */
    padding-bottom: 16px;
    /* [zh] 样式规则 `opacity: 0;` */
    opacity: 0;
    /* [zh] 样式规则 `transition: opacity $transition-normal;` */
    transition: opacity $transition-normal;
    /* [zh] 样式规则 `pointer-events: none;` */
    pointer-events: none;

    /* [zh] 样式规则 `.el-button {` */
    .el-button {
      /* [zh] 样式规则 `pointer-events: auto;` */
      pointer-events: auto;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.info {` */
  .info {
    /* [zh] 样式规则 `padding: 10px 12px 12px;` */
    padding: 10px 12px 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.name {` */
  .name {
    /* [zh] 样式规则 `margin: 0 0 6px;` */
    margin: 0 0 6px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `line-height: 1.45;` */
    line-height: 1.45;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `height: 38px;` */
    height: 38px;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `display: -webkit-box;` */
    display: -webkit-box;
    /* [zh] 样式规则 `-webkit-line-clamp: 2;` */
    -webkit-line-clamp: 2;
    /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
    -webkit-box-orient: vertical;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price-row {` */
  .price-row {
    /* [zh] 样式规则 `margin-bottom: 4px;` */
    margin-bottom: 4px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: baseline;` */
    align-items: baseline;
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;

    /* [zh] 样式规则 `.price {` */
    .price {
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
      /* [zh] 样式规则 `font-size: 18px;` */
      font-size: 18px;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
      /* [zh] 样式规则 `font-variant-numeric: tabular-nums;` */
      font-variant-numeric: tabular-nums;
      /* [zh] 样式规则 `letter-spacing: -0.02em;` */
      letter-spacing: -0.02em;
      /* [zh] 样式规则 `line-height: 1.2;` */
      line-height: 1.2;

      /* [zh] 样式规则 `.symbol {` */
      .symbol {
        /* [zh] 样式规则 `font-size: 12px;` */
        font-size: 12px;
        /* [zh] 样式规则 `margin-right: 1px;` */
        margin-right: 1px;
        /* [zh] 样式规则 `font-weight: 600;` */
        font-weight: 600;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sale {` */
  .sale {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-compact {` */
  &.is-compact {
    /* [zh] 样式规则 `border-radius: $radius-card;` */
    border-radius: $radius-card;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `box-shadow: none;` */
    box-shadow: none;

    /* [zh] 样式规则 `@media (hover: hover) {` */
    @media (hover: hover) {
      /* [zh] 样式规则 `&:hover {` */
      &:hover {
        /* [zh] 样式规则 `transform: none;` */
        transform: none;
        /* [zh] 样式规则 `box-shadow: $shadow-xs;` */
        box-shadow: $shadow-xs;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.cover-wrap {` */
    .cover-wrap {
      /* [zh] 样式规则 `aspect-ratio: 1;` */
      aspect-ratio: 1;
      /* [zh] 样式规则 `padding: 0;` */
      padding: 0;
      /* [zh] 样式规则 `background: $color-card;` */
      background: $color-card;
      /* [zh] 样式规则 `border-radius: $radius-card $radius-card` */
      border-radius: $radius-card $radius-card 0 0;
      /* [zh] 样式规则 `overflow: hidden;` */
      overflow: hidden;

      /* [zh] 样式规则 `:deep(.product-image) {` */
      :deep(.product-image) {
        /* [zh] 样式规则 `border-radius: 0;` */
        border-radius: 0;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.cart-float {` */
    .cart-float {
      /* [zh] 样式规则 `width: 28px;` */
      width: 28px;
      /* [zh] 样式规则 `height: 28px;` */
      height: 28px;
      /* [zh] 样式规则 `bottom: 4px;` */
      bottom: 4px;
      /* [zh] 样式规则 `right: 4px;` */
      right: 4px;
      /* [zh] 样式规则 `opacity: 1;` */
      opacity: 1;
      /* [zh] 样式规则 `transform: scale(1);` */
      transform: scale(1);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.hover-mask {` */
    .hover-mask {
      /* [zh] 样式规则 `display: none;` */
      display: none;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.info {` */
    .info {
      /* [zh] 样式规则 `padding: 8px 10px 10px;` */
      padding: 8px 10px 10px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.name {` */
    .name {
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
      /* [zh] 样式规则 `height: 32px;` */
      height: 32px;
      /* [zh] 样式规则 `line-height: 1.35;` */
      line-height: 1.35;
      /* [zh] 样式规则 `font-weight: 500;` */
      font-weight: 500;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.price-row {` */
    .price-row {
      /* [zh] 样式规则 `margin-bottom: 2px;` */
      margin-bottom: 2px;

      /* [zh] 样式规则 `.price {` */
      .price {
        /* [zh] 样式规则 `font-size: 16px;` */
        font-size: 16px;

        /* [zh] 样式规则 `.symbol {` */
        .symbol {
          /* [zh] 样式规则 `font-size: 11px;` */
          font-size: 11px;
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.sale {` */
    .sale {
      /* [zh] 样式规则 `font-size: 10px;` */
      font-size: 10px;
      /* [zh] 样式规则 `color: $color-text-disabled;` */
      color: $color-text-disabled;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-card.ignore {` */
.product-card.ignore {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `height: auto;` */
  height: auto;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
  /* [zh] 样式规则 `box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04` */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  /* [zh] 样式规则 `transition:` */
  transition:
    /* [zh] 样式声明 */
    border-color 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    /* [zh] 样式声明 */
    box-shadow 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
    /* [zh] 样式声明 */
    transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);

  /* [zh] 样式规则 `.cover-wrap {` */
  .cover-wrap {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `aspect-ratio: 1;` */
    aspect-ratio: 1;
    /* [zh] 样式规则 `background: linear-gradient(180deg, #faf` */
    background: linear-gradient(180deg, #fafafa 0%, #f5f5f5 100%);
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `border-radius: 7px 7px 0 0;` */
    border-radius: 7px 7px 0 0;
    /* [zh] 样式规则 `transition: background 0.3s ease;` */
    transition: background 0.3s ease;

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
      /* [zh] 样式规则 `border-radius: 0 !important;` */
      border-radius: 0 !important;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(img) {` */
    :deep(img) {
      /* [zh] 样式规则 `transition: transform 0.5s cubic-bezier(` */
      transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cart-float {` */
  .cart-float {
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `bottom: 8px;` */
    bottom: 8px;
    /* [zh] 样式规则 `right: 8px;` */
    right: 8px;
    /* [zh] 样式规则 `z-index: 2;` */
    z-index: 2;
    /* [zh] 样式规则 `width: 32px;` */
    width: 32px;
    /* [zh] 样式规则 `height: 32px;` */
    height: 32px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `border-radius: 50%;` */
    border-radius: 50%;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.98);` */
    background: rgba(255, 255, 255, 0.98);
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `place-items: center;` */
    place-items: center;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `opacity: 0;` */
    opacity: 0;
    /* [zh] 样式规则 `transform: translateY(8px) scale(0.8);` */
    transform: translateY(8px) scale(0.8);
    /* [zh] 样式规则 `box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1` */
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    /* [zh] 样式规则 `transition:` */
    transition:
      /* [zh] 样式声明 */
      opacity 0.3s ease,
      /* [zh] 样式声明 */
      transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
      /* [zh] 样式声明 */
      background 0.2s ease,
      /* [zh] 样式声明 */
      box-shadow 0.2s ease;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.hover-mask {` */
  .hover-mask {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.info {` */
  .info {
    /* [zh] 样式规则 `flex: 0 0 auto;` */
    flex: 0 0 auto;
    /* [zh] 样式规则 `padding: 10px 12px 12px;` */
    padding: 10px 12px 12px;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.name {` */
  .name {
    /* [zh] 样式规则 `margin: 0 0 6px;` */
    margin: 0 0 6px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
    /* [zh] 样式规则 `color: $color-text-primary;` */
    color: $color-text-primary;
    /* [zh] 样式规则 `height: auto;` */
    height: auto;
    /* [zh] 样式规则 `max-height: 34px;` */
    max-height: 34px;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `display: -webkit-box;` */
    display: -webkit-box;
    /* [zh] 样式规则 `-webkit-line-clamp: 2;` */
    -webkit-line-clamp: 2;
    /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
    -webkit-box-orient: vertical;
    /* [zh] 样式规则 `word-break: normal;` */
    word-break: normal;
    /* [zh] 样式规则 `overflow-wrap: break-word;` */
    overflow-wrap: break-word;
    /* [zh] 样式规则 `white-space: normal;` */
    white-space: normal;
    /* [zh] 样式规则 `transition: color 0.2s ease;` */
    transition: color 0.2s ease;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price-row {` */
  .price-row {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: baseline;` */
    align-items: baseline;
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price-row .price {` */
  .price-row .price {
    /* [zh] 样式规则 `display: inline;` */
    display: inline;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `letter-spacing: -0.01em;` */
    letter-spacing: -0.01em;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
    /* [zh] 样式规则 `transition: color 0.2s ease;` */
    transition: color 0.2s ease;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price-row .symbol {` */
  .price-row .symbol {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `margin-right: 1px;` */
    margin-right: 1px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sale {` */
  .sale {
    /* [zh] 样式规则 `margin: 4px 0 0;` */
    margin: 4px 0 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `border-color: rgba($color-gold, 0.3);` */
    border-color: rgba($color-gold, 0.3);
    /* [zh] 样式规则 `box-shadow:` */
    box-shadow:
      /* [zh] 样式声明 */
      0 12px 32px rgba(0, 0, 0, 0.12),
      /* [zh] 样式声明 */
      0 4px 12px rgba(0, 0, 0, 0.06),
      /* [zh] 样式声明 */
      0 0 0 1px rgba($color-gold, 0.1);
    /* [zh] 样式规则 `transform: translateY(-6px) scale(1.02);` */
    transform: translateY(-6px) scale(1.02);

    /* [zh] 样式规则 `.cover-wrap :deep(img) {` */
    .cover-wrap :deep(img) {
      /* [zh] 样式规则 `transform: scale(1.08);` */
      transform: scale(1.08);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.cover-wrap {` */
    .cover-wrap {
      /* [zh] 样式规则 `background: linear-gradient(180deg, #fff` */
      background: linear-gradient(180deg, #ffffff 0%, #fafafa 100%);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.name {` */
    .name {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.price-row .price {` */
    .price-row .price {
      /* [zh] 样式规则 `color: $color-gold;` */
      color: $color-gold;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.cart-float {` */
    .cart-float {
      /* [zh] 样式规则 `opacity: 1;` */
      opacity: 1;
      /* [zh] 样式规则 `transform: translateY(0) scale(1);` */
      transform: translateY(0) scale(1);
      /* [zh] 样式规则 `box-shadow: 0 6px 20px rgba(201, 169, 98` */
      box-shadow: 0 6px 20px rgba(201, 169, 98, 0.25);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: translateY(-2px) scale(0.99);` */
    transform: translateY(-2px) scale(0.99);
    /* [zh] 样式规则 `box-shadow: 0 4px 12px rgba(0, 0, 0, 0.0` */
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    /* [zh] 样式规则 `transition-duration: 0.1s;` */
    transition-duration: 0.1s;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:focus-visible {` */
  &:focus-visible {
    /* [zh] 样式规则 `outline: 2px solid $color-gold;` */
    outline: 2px solid $color-gold;
    /* [zh] 样式规则 `outline-offset: 3px;` */
    outline-offset: 3px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (max-width: $breakpoint-mobile) {` */
@media (max-width: $breakpoint-mobile) {
  /* [zh] 样式规则 `.product-card:not(.is-compact) .hover-ma` */
  .product-card:not(.is-compact) .hover-mask {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.product-card:not(.is-compact) .cart-flo` */
  .product-card:not(.is-compact) .cart-float {
    /* [zh] 样式规则 `opacity: 1;` */
    opacity: 1;
    /* [zh] 样式规则 `transform: scale(1);` */
    transform: scale(1);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
