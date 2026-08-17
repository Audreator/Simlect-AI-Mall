<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<article>` -->
  <article class="user-coupon-card" :class="{ 'is-used': coupon.status === 1, 'is-expired': coupon.status === 2 }">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="card-left">
      <!-- [zh] 开始标签 `<p>` -->
      <p class="main-value">{{ couponMainValue(coupon) }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="condition">{{ couponConditionText(coupon) }}</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="card-right">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="head-row">
        <!-- [zh] 开始标签 `<h4>` -->
        <h4 class="name">{{ coupon.couponName || '优惠券' }}</h4>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="type-tag">{{ couponTypeLabel(coupon.couponType) }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="time-line">有效期至 {{ formatCouponTime(coupon.validEndTime) }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="status-line">{{ USER_COUPON_STATUS_MAP[coupon.status] || '未知' }}</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</article>` -->
  </article>
</template>

<script setup lang="ts">
import {
  USER_COUPON_STATUS_MAP,
  couponConditionText,
  couponMainValue,
  couponTypeLabel,
  formatCouponTime
} from '@/utils/coupon';

defineProps<{ coupon: Record<string, any> }>();
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.user-coupon-card {` */
.user-coupon-card {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;

  /* [zh] 样式声明 */
  &.is-used,
  /* [zh] 样式规则 `&.is-expired {` */
  &.is-expired {
    /* [zh] 样式规则 `opacity: 0.72;` */
    opacity: 0.72;
    /* [zh] 样式规则 `filter: grayscale(0.2);` */
    filter: grayscale(0.2);
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
  /* [zh] 样式规则 `padding: 12px 8px;` */
  padding: 12px 8px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;

  /* [zh] 样式规则 `.main-value {` */
  .main-value {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 22px;` */
    font-size: 22px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `line-height: 1.1;` */
    line-height: 1.1;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.condition {` */
  .condition {
    /* [zh] 样式规则 `margin: 6px 0 0;` */
    margin: 6px 0 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `opacity: 0.92;` */
    opacity: 0.92;
    /* [zh] 样式规则 `line-height: 1.3;` */
    line-height: 1.3;
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

  /* [zh] 样式规则 `.head-row {` */
  .head-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: flex-start;` */
    align-items: flex-start;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `margin-bottom: 6px;` */
    margin-bottom: 6px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.name {` */
  .name {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `line-height: 1.35;` */
    line-height: 1.35;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.type-tag {` */
  .type-tag {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `padding: 2px 8px;` */
    padding: 2px 8px;
    /* [zh] 样式规则 `border-radius: $radius-pill;` */
    border-radius: $radius-pill;
    /* [zh] 样式规则 `background: $color-gold-soft;` */
    background: $color-gold-soft;
    /* [zh] 样式规则 `border: 1px solid $color-gold-muted;` */
    border: 1px solid $color-gold-muted;
    /* [zh] 样式规则 `color: $color-gold;` */
    color: $color-gold;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  .time-line,
  /* [zh] 样式规则 `.status-line {` */
  .status-line {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.status-line {` */
  .status-line {
    /* [zh] 样式规则 `margin-top: 4px;` */
    margin-top: 4px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
