<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="order-amount-summary" :class="{ compact }">
    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="showOriginal" class="amount-row">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="label">{{ originalLabel }}</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="value">¥{{ formatOrderMoney(order?.originalAmount) }}</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="showCoupon" class="amount-row discount">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="label">{{ couponLabel }}</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="value">-¥{{ formatOrderMoney(order?.couponDiscountAmount) }}</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="amount-row pay">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="label">{{ payLabel }}</span>
      <!-- [zh] 开始标签 `<strong>` -->
      <strong class="value pay-value">¥{{ formatOrderMoney(order?.amount) }}</strong>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed } from 'vue';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`formatOrderMoney,`
  formatOrderMoney,
  // [zh] TypeScript/JS 语句：`hasOrderCouponDiscount,`
  hasOrderCouponDiscount,
  // [zh] TypeScript/JS 语句：`orderCouponSummaryText`
  orderCouponSummaryText
// [zh] 语句
} from '@/utils/orderAmount';

// [zh] 声明变量 `props`
const props = withDefaults(
  // [zh] 代码块开始
  defineProps<{
    // [zh] 语句
    order?: Record<string, any> | null;
    // [zh] 语句
    compact?: boolean;
    // [zh] 语句
    originalLabel?: string;
    // [zh] 语句
    payLabel?: string;
  // [zh] TypeScript/JS 语句：`}>(),`
  }>(),
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`order: null,`
    order: null,
    // [zh] TypeScript/JS 语句：`compact: false,`
    compact: false,
    // [zh] TypeScript/JS 语句：`originalLabel: '商品总价',`
    originalLabel: '商品总价',
    // [zh] TypeScript/JS 语句：`payLabel: '实付款'`
    payLabel: '实付款'
  // [zh] 语法符号
  }
// [zh] 语句
);

// [zh] 声明变量 `showCoupon`
const showCoupon = computed(() => hasOrderCouponDiscount(props.order));

// [zh] 声明变量 `showOriginal`
const showOriginal = computed(() => showCoupon.value);

// [zh] 声明变量 `couponLabel`
const couponLabel = computed(() => {
  // [zh] 声明变量 `summary`
  const summary = orderCouponSummaryText(props.order);
  // [zh] return 返回
  return summary ? `优惠券：${summary}` : '优惠券';
// [zh] 语句
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.order-amount-summary {` */
.order-amount-summary {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;

  /* [zh] 样式规则 `&.compact {` */
  &.compact {
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;
    /* [zh] 样式规则 `align-items: flex-end;` */
    align-items: flex-end;
    /* [zh] 样式规则 `text-align: right;` */
    text-align: right;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.amount-row {` */
.amount-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: baseline;` */
  align-items: baseline;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;

  /* [zh] 样式规则 `.label {` */
  .label {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `word-break: break-all;` */
    word-break: break-all;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.value {` */
  .value {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.discount .value {` */
  &.discount .value {
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.pay {` */
  &.pay {
    /* [zh] 样式规则 `margin-top: 2px;` */
    margin-top: 2px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;

    /* [zh] 样式规则 `.pay-value {` */
    .pay-value {
      /* [zh] 样式规则 `font-size: inherit;` */
      font-size: inherit;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-amount-summary.compact .amount-ro` */
.order-amount-summary.compact .amount-row {
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;

  /* [zh] 样式规则 `.label {` */
  .label {
    /* [zh] 样式规则 `flex: unset;` */
    flex: unset;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
