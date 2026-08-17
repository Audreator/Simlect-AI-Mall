<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<article>` -->
  <article class="discount-card" :class="{ 'is-disabled': !canReceive, 'is-purchased': hasBought }">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="card-left">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="price-wrap">
        <!-- [zh] 开始标签 `<span>` -->
        <span v-if="leftDisplay.prefix" class="currency">{{ leftDisplay.prefix }}</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="price">{{ leftDisplay.value }}</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span v-if="leftDisplay.suffix" class="suffix">{{ leftDisplay.suffix }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="threshold">{{ thresholdText }}</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="card-right">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="card-top">
        <!-- [zh] 开始标签 `<h4>` -->
        <h4 class="name">{{ coupon.couponName || '优惠券' }}</h4>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="type-tag" :class="`type-${coupon.couponType}`">{{ typeLabel }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="card-middle">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="meta-row">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">有效期</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="value">{{ validEndText }}</span>
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="coupon.rushingStartTime && coupon.rushingEndTime" class="meta-row rush">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">秒杀</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="value">{{ rushRangeText }}</span>
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="card-bottom">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="stock-block">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="progress-bar">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="progress-fill" :style="{ width: `${progress}%` }" />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="stock-text">已抢 {{ sold }} / {{ stockTotalLabel }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button
          v-if="type === 'available'"
          class="buy-btn"
          :class="{ 'is-purchased': hasBought }"
          type="primary"
          round
          :plain="hasBought"
          :disabled="!canReceive || receiving"
          :loading="receiving"
          @click="$emit('receive', coupon)"
        >
          <!-- [zh] Mustache 插值表达式 -->
          {{ btnText }}
        <!-- [zh] 闭合标签 `</el-button>` -->
        </el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</article>` -->
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  canReceiveCoupon,
  couponLeftMainDisplay,
  couponReceiveBtnText,
  couponSoldCount,
  couponSoldProgress,
  couponStockTotalLabel,
  couponThresholdText,
  couponTypeLabel,
  formatCouponDateTime,
  resolveCouponHasBought
} from '@/utils/couponPlaza';

const props = withDefaults(
  defineProps<{ coupon: Record<string, any>; type: 'user' | 'available'; receiving?: boolean }>(),
  { receiving: false }
);
defineEmits<{ receive: [Record<string, any>] }>();

const leftDisplay = computed(() => couponLeftMainDisplay(props.coupon));
const thresholdText = computed(() => couponThresholdText(props.coupon));
const typeLabel = computed(() => couponTypeLabel(props.coupon.couponType));
const progress = computed(() => couponSoldProgress(props.coupon));
const sold = computed(() => couponSoldCount(props.coupon));
const stockTotalLabel = computed(() => couponStockTotalLabel(props.coupon));
const hasBought = computed(() => resolveCouponHasBought(props.coupon));
const canReceive = computed(() => (props.type === 'available' ? canReceiveCoupon(props.coupon) : false));
const btnText = computed(() => couponReceiveBtnText(props.coupon));
const validEndText = computed(() =>
  props.coupon.validEndTime ? `至 ${formatCouponDateTime(props.coupon.validEndTime)}` : '长期有效'
);
const rushRangeText = computed(() =>
  `${formatCouponDateTime(props.coupon.rushingStartTime)} ~ ${formatCouponDateTime(props.coupon.rushingEndTime)}`
);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.discount-card {` */
.discount-card {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `box-shadow: 0 2px 12px rgba(0, 0, 0, 0.0` */
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  /* [zh] 样式规则 `&.is-disabled {` */
  &.is-disabled {
    /* [zh] 样式规则 `opacity: 0.88;` */
    opacity: 0.88;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.card-left {` */
.card-left {
  /* [zh] 样式规则 `width: 108px;` */
  width: 108px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `background: $color-accent-gradient-gold;` */
  background: $color-accent-gradient-gold;
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `padding: 14px 8px;` */
  padding: 14px 8px;
  /* [zh] 样式规则 `position: relative;` */
  position: relative;

  /* [zh] 样式规则 `&::after {` */
  &::after {
    /* [zh] 样式规则 `content: '';` */
    content: '';
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `top: 0;` */
    top: 0;
    /* [zh] 样式规则 `bottom: 0;` */
    bottom: 0;
    /* [zh] 样式规则 `right: -6px;` */
    right: -6px;
    /* [zh] 样式规则 `width: 12px;` */
    width: 12px;
    /* [zh] 样式规则 `background: repeating-linear-gradient(` */
    background: repeating-linear-gradient(
      /* [zh] 样式声明 */
      to bottom,
      /* [zh] 样式声明 */
      $color-bg 0,
      /* [zh] 样式声明 */
      $color-bg 4px,
      /* [zh] 样式声明 */
      transparent 4px,
      /* [zh] 样式声明 */
      transparent 8px
    /* [zh] 样式声明 */
    );
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price-wrap {` */
  .price-wrap {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: baseline;` */
    align-items: baseline;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;

    /* [zh] 样式规则 `.currency {` */
    .currency {
      /* [zh] 样式规则 `font-size: 14px;` */
      font-size: 14px;
      /* [zh] 样式规则 `font-weight: 500;` */
      font-weight: 500;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.price {` */
    .price {
      /* [zh] 样式规则 `font-size: 32px;` */
      font-size: 32px;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.suffix {` */
    .suffix {
      /* [zh] 样式规则 `font-size: 14px;` */
      font-size: 14px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `margin-left: 2px;` */
      margin-left: 2px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.threshold {` */
  .threshold {
    /* [zh] 样式规则 `margin: 6px 0 0;` */
    margin: 6px 0 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `opacity: 0.9;` */
    opacity: 0.9;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.card-right {` */
.card-right {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.card-top {` */
.card-top {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;

  /* [zh] 样式规则 `.name {` */
  .name {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.type-tag {` */
  .type-tag {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `padding: 2px 6px;` */
    padding: 2px 6px;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;

    /* [zh] 样式声明 */
    &.type-1,
    /* [zh] 样式声明 */
    &.type-2,
    /* [zh] 样式规则 `&.type-3 {` */
    &.type-3 {
      /* [zh] 样式规则 `background: $color-primary-soft;` */
      background: $color-primary-soft;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.meta-row {` */
.meta-row {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;

  /* [zh] 样式规则 `.label {` */
  .label {
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `margin-right: 4px;` */
    margin-right: 4px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  &.rush .label,
  /* [zh] 样式规则 `&.rush .value {` */
  &.rush .value {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.card-bottom {` */
.card-bottom {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `margin-top: auto;` */
  margin-top: auto;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stock-block {` */
.stock-block {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;

  /* [zh] 样式规则 `.progress-bar {` */
  .progress-bar {
    /* [zh] 样式规则 `height: 5px;` */
    height: 5px;
    /* [zh] 样式规则 `background: $color-bg;` */
    background: $color-bg;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;

    /* [zh] 样式规则 `.progress-fill {` */
    .progress-fill {
      /* [zh] 样式规则 `height: 100%;` */
      height: 100%;
      /* [zh] 样式规则 `background: $color-accent-gradient-gold;` */
      background: $color-accent-gradient-gold;
      /* [zh] 样式规则 `transition: width 0.3s;` */
      transition: width 0.3s;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.stock-text {` */
  .stock-text {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.buy-btn {` */
.buy-btn {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `min-width: 84px;` */
  min-width: 84px;

  /* [zh] 样式声明 */
  &.is-purchased,
  /* [zh] 样式规则 `&:disabled.is-purchased {` */
  &:disabled.is-purchased {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `border-color: $color-border-gray;` */
    border-color: $color-border-gray;
    /* [zh] 样式规则 `background: $color-bg;` */
    background: $color-bg;
    /* [zh] 样式规则 `cursor: not-allowed;` */
    cursor: not-allowed;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
