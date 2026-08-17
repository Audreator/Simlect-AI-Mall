<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<component>` -->
  <component
    :is="clickable ? 'button' : 'div'"
    type="button"
    class="consult-product-card"
    :class="{ 'is-composer': variant === 'composer', 'is-bubble': variant === 'bubble' }"
    @click="onClick"
  >
    <!-- [zh] 开始标签 `<div>` -->
    <div class="cover-wrap">
      <!-- [zh] 开始标签 `<ProductImage>` -->
      <ProductImage :source="product.cover" class="cover" width="100%" height="100%" fit="cover" />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="meta">
      <!-- [zh] 开始标签 `<p>` -->
      <p class="name">{{ product.productName }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="priceText" class="price">¥{{ priceText }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="variant === 'composer'" class="hint">
        <!-- [zh] Mustache 插值表达式 -->
        {{ resuming ? '点击继续上次咨询' : '点击卡片，开始咨询这件商品' }}
      <!-- [zh] 闭合标签 `</p>` -->
      </p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</component>` -->
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import ProductImage from '@/components/common/ProductImage.vue';
import type { AgentConsultProduct } from '@/utils/agentProductConsult';

const props = withDefaults(
  defineProps<{
    product: AgentConsultProduct;
    variant?: 'composer' | 'bubble';
    clickable?: boolean;

    resuming?: boolean;
  }>(),
  { variant: 'composer', clickable: false, resuming: false }
);

const emit = defineEmits<{ send: [] }>();

const priceText = computed(() => {
  const n = Number(props.product.minPrice);
  return Number.isFinite(n) ? n.toFixed(2) : '';
});

const onClick = () => {
  if (props.clickable) emit('send');
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.consult-product-card {` */
.consult-product-card {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;
  /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
  -webkit-tap-highlight-color: transparent;

  /* [zh] 样式规则 `&.is-composer {` */
  &.is-composer {
    /* [zh] 样式规则 `padding: 10px 12px;` */
    padding: 10px 12px;
    /* [zh] 样式规则 `border-radius: 12px;` */
    border-radius: 12px;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.75);` */
    background: rgba(255, 255, 255, 0.75);
    /* [zh] 样式规则 `background: var(--glass-bg-light, rgba(2` */
    background: var(--glass-bg-light, rgba(255, 255, 255, 0.75));
    /* [zh] 样式规则 `-webkit-backdrop-filter: blur(12px) satu` */
    -webkit-backdrop-filter: blur(12px) saturate(160%);
    /* [zh] 样式规则 `-webkit-backdrop-filter: var(--glass-blu` */
    -webkit-backdrop-filter: var(--glass-blur-sm, blur(12px) saturate(160%));
    /* [zh] 样式规则 `backdrop-filter: blur(12px) saturate(160` */
    backdrop-filter: blur(12px) saturate(160%);
    /* [zh] 样式规则 `backdrop-filter: var(--glass-blur-sm, bl` */
    backdrop-filter: var(--glass-blur-sm, blur(12px) saturate(160%));
    /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
    border: 1px solid rgba(120, 120, 128, 0.18);
    /* [zh] 样式规则 `border: 1px solid var(--glass-border-sof` */
    border: 1px solid var(--glass-border-soft, rgba(120, 120, 128, 0.18));
    /* [zh] 样式规则 `box-shadow: 0 2px 12px rgba(17, 23, 41, ` */
    box-shadow: 0 2px 12px rgba(17, 23, 41, 0.08);
    /* [zh] 样式规则 `box-shadow: var(--glass-shadow-sm, 0 2px` */
    box-shadow: var(--glass-shadow-sm, 0 2px 12px rgba(17, 23, 41, 0.08));
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-bubble {` */
  &.is-bubble {
    /* [zh] 样式规则 `width: fit-content;` */
    width: fit-content;
    /* [zh] 样式规则 `max-width: min(75%, 280px);` */
    max-width: min(75%, 280px);
    /* [zh] 样式规则 `margin-left: auto;` */
    margin-left: auto;
    /* [zh] 样式规则 `padding: 8px 10px;` */
    padding: 8px 10px;
    /* [zh] 样式规则 `background: var(--glass-bg-light);` */
    background: var(--glass-bg-light);
    /* [zh] 样式规则 `-webkit-backdrop-filter: var(--glass-blu` */
    -webkit-backdrop-filter: var(--glass-blur-sm);
    /* [zh] 样式规则 `backdrop-filter: var(--glass-blur-sm);` */
    backdrop-filter: var(--glass-blur-sm);
    /* [zh] 样式规则 `border: 1px solid var(--glass-border);` */
    border: 1px solid var(--glass-border);
    /* [zh] 样式规则 `box-shadow: var(--glass-shadow-sm);` */
    box-shadow: var(--glass-shadow-sm);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:is(button) {` */
  &:is(button) {
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `opacity: 0.85;` */
      opacity: 0.85;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cover-wrap {` */
.cover-wrap {
  /* [zh] 样式规则 `flex: 0 0 48px;` */
  flex: 0 0 48px;
  /* [zh] 样式规则 `width: 48px;` */
  width: 48px;
  /* [zh] 样式规则 `height: 48px;` */
  height: 48px;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;

  /* [zh] 样式规则 `:deep(.product-image) {` */
  :deep(.product-image) {
    /* [zh] 样式规则 `width: 100% !important;` */
    width: 100% !important;
    /* [zh] 样式规则 `height: 100% !important;` */
    height: 100% !important;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.meta {` */
.meta {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;

  /* [zh] 样式规则 `.name {` */
  .name {
    /* [zh] 样式规则 `margin: 0 0 4px;` */
    margin: 0 0 4px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `display: -webkit-box;` */
    display: -webkit-box;
    /* [zh] 样式规则 `-webkit-line-clamp: 2;` */
    -webkit-line-clamp: 2;
    /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
    -webkit-box-orient: vertical;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price {` */
  .price {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.hint {` */
  .hint {
    /* [zh] 样式规则 `margin: 4px 0 0;` */
    margin: 4px 0 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
