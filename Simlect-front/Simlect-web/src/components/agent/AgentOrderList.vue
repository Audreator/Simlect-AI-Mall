<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="agent-orders">
    <!-- [zh] 开始标签 `<div>` -->
    <div v-for="order in list" :key="order.orderId" class="order-block">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="order-meta">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="order-id" :title="order.orderId">订单号 {{ order.orderId }}</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="order-status">{{ order.orderStatusName || statusText(order.orderStatus) }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="item in order.orderItemList || []" :key="item.orderItemId" class="item-row">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="item-cover" :class="{ 'is-coupon': isCouponOrder(order) }">
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon v-if="isCouponOrder(order)" class="coupon-icon"><Ticket /></el-icon>
          <!-- [zh] 开始标签 `<ProductImage>` -->
          <ProductImage v-else :source="item.cover" width="48" height="48" />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="item-info">
          <!-- [zh] 开始标签 `<p>` -->
          <p class="name">{{ item.productName }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="item.propertyInfo && !isCouponOrder(order)" class="sku">{{ item.propertyInfo }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="item.orderItemId" class="item-id" :title="item.orderItemId">
            <!-- [zh] 模板内容：`订单项 ID {{ item.orderItemId }}` -->
            订单项 ID {{ item.orderItemId }}
          <!-- [zh] 闭合标签 `</p>` -->
          </p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-if="!list.length" class="empty">暂无订单信息</p>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { Ticket } from '@element-plus/icons-vue';
import ProductImage from '@/components/common/ProductImage.vue';
import { orderStatusLabel } from '@/constants/backendEnums';

defineProps<{ list: Record<string, any>[] }>();

const COUPON_ORDER_PAY_SCENE = '2';
const COUPON_ORDER_PROPERTY = '优惠券秒杀';

const isCouponOrder = (order: Record<string, any>) =>
  String(order.payScene) === COUPON_ORDER_PAY_SCENE
  || (order.orderItemList || []).some((item: Record<string, any>) => item?.propertyInfo === COUPON_ORDER_PROPERTY);

const statusText = (s?: number) => (s != null ? orderStatusLabel(s) : '订单');
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.agent-orders {` */
.agent-orders {
  /* [zh] 样式规则 `max-height: 300px;` */
  max-height: 300px;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `line-height: 1.35;` */
  line-height: 1.35;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-block {` */
.order-block {
  /* [zh] 样式规则 `padding: 8px 0;` */
  padding: 8px 0;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-meta {` */
.order-meta {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `margin-bottom: 6px;` */
  margin-bottom: 6px;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-id {` */
.order-id {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `font-size: 10px;` */
  font-size: 10px;
  /* [zh] 样式规则 `line-height: 1.3;` */
  line-height: 1.3;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `text-overflow: ellipsis;` */
  text-overflow: ellipsis;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-status {` */
.order-status {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `max-width: 42%;` */
  max-width: 42%;
  /* [zh] 样式规则 `font-size: 10px;` */
  font-size: 10px;
  /* [zh] 样式规则 `line-height: 1.2;` */
  line-height: 1.2;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `padding: 2px 6px;` */
  padding: 2px 6px;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `background: $color-surface-inset;` */
  background: $color-surface-inset;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `text-overflow: ellipsis;` */
  text-overflow: ellipsis;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-row {` */
.item-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `margin-top: 6px;` */
  margin-top: 6px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-cover {` */
.item-cover {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `width: 48px;` */
  width: 48px;
  /* [zh] 样式规则 `height: 48px;` */
  height: 48px;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `:deep(.product-image) {` */
  :deep(.product-image) {
    /* [zh] 样式规则 `width: 48px !important;` */
    width: 48px !important;
    /* [zh] 样式规则 `height: 48px !important;` */
    height: 48px !important;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-coupon {` */
  &.is-coupon {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `background: linear-gradient(135deg, rgba` */
    background: linear-gradient(135deg, rgba($color-primary, 0.12), rgba($color-price, 0.1));

    /* [zh] 样式规则 `.coupon-icon {` */
    .coupon-icon {
      /* [zh] 样式规则 `font-size: 26px;` */
      font-size: 26px;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-info {` */
.item-info {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;

  /* [zh] 样式规则 `.name {` */
  .name {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `line-height: 1.35;` */
    line-height: 1.35;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sku {` */
  .sku {
    /* [zh] 样式规则 `margin: 2px 0 0;` */
    margin: 2px 0 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.item-id {` */
  .item-id {
    /* [zh] 样式规则 `margin: 4px 0 0;` */
    margin: 4px 0 0;
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `line-height: 1.35;` */
    line-height: 1.35;
    /* [zh] 样式规则 `font-family: ui-monospace, SFMono-Regula` */
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `word-break: break-all;` */
    word-break: break-all;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.empty {` */
.empty {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}
</style>
