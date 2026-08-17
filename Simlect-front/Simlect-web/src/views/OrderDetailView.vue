<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="order-detail-page" v-loading="loading">
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template v-if="order">
      <!-- [zh] 开始标签 `<section>` -->
      <section class="status-card card">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="status-text" :class="statusClass">{{ displayStatus }}</span>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="order.subject" class="status-sub">{{ order.subject }}</p>
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<section>` -->
      <section class="info-card card">
        <!-- [zh] 开始标签 `<h3>` -->
        <h3 class="section-title">订单信息</h3>
        <!-- [zh] 开始标签 `<dl>` -->
        <dl class="info-list">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="info-row">
            <!-- [zh] 开始标签 `<dt>` -->
            <dt>订单编号</dt>
            <!-- [zh] 开始标签 `<dd>` -->
            <dd>{{ order.orderId }}</dd>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="info-row">
            <!-- [zh] 开始标签 `<dt>` -->
            <dt>下单时间</dt>
            <!-- [zh] 开始标签 `<dd>` -->
            <dd>{{ formatTime(order.orderTime) }}</dd>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="order.payOrderId" class="info-row">
            <!-- [zh] 开始标签 `<dt>` -->
            <dt>支付单号</dt>
            <!-- [zh] 开始标签 `<dd>` -->
            <dd>{{ order.payOrderId }}</dd>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="order.payChannel" class="info-row">
            <!-- [zh] 开始标签 `<dt>` -->
            <dt>支付方式</dt>
            <!-- [zh] 开始标签 `<dd>` -->
            <dd>{{ order.payChannel }}</dd>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</dl>` -->
        </dl>
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<section>` -->
      <section class="items-card card">
        <!-- [zh] 开始标签 `<h3>` -->
        <h3 class="section-title">{{ isCouponOrder ? '优惠券信息' : '商品明细' }}</h3>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="itemList.length" class="goods-list">
          <!-- [zh] 开始标签 `<component>` -->
          <component
            :is="isCouponOrder ? 'div' : 'button'"
            v-for="item in itemList"
            :key="item.orderItemId"
            :type="isCouponOrder ? undefined : 'button'"
            class="goods-row"
            @click="!isCouponOrder && goProduct(item.productId)"
          >
            <!-- [zh] 开始标签 `<div>` -->
            <div class="goods-cover-col" :class="{ 'is-coupon': isCouponOrder }">
              <!-- [zh] 开始标签 `<el-icon>` -->
              <el-icon v-if="isCouponOrder" class="coupon-icon"><Ticket /></el-icon>
              <!-- [zh] 开始标签 `<ProductImage>` -->
              <ProductImage v-else :source="item.cover" class="goods-cover" />
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="goods-info">
              <!-- [zh] 开始标签 `<p>` -->
              <p class="goods-name">{{ item.productName }}</p>
              <!-- [zh] 开始标签 `<p>` -->
              <p v-if="item.propertyInfo && !isCouponOrder" class="goods-sku">{{ item.propertyInfo }}</p>
              <!-- [zh] 开始标签 `<p>` -->
              <p class="goods-remark">买家备注：{{ item.remark?.trim() || '暂无' }}</p>
              <!-- [zh] 开始标签 `<OrderItemIdText>` -->
              <OrderItemIdText :id="item.orderItemId" />
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="goods-price">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="price">¥{{ formatMoney(item.itemAmount) }}</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span class="qty">×{{ item.buyCount }}</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div
              v-if="canRefundItem(item)"
              class="goods-action"
            >
              <!-- [zh] 开始标签 `<el-button>` -->
              <el-button size="small" text type="danger" @click.stop="refundItem(item.orderItemId)">
                <!-- [zh] 模板内容：`退款` -->
                退款
              <!-- [zh] 闭合标签 `</el-button>` -->
              </el-button>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</component>` -->
          </component>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<el-empty>` -->
        <el-empty v-else :description="isCouponOrder ? '暂无优惠券信息' : '暂无商品明细'" :image-size="64" />
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<section>` -->
      <section v-if="!isCouponOrder && hasOrderCouponDiscount(order)" class="coupon-card card">
        <!-- [zh] 开始标签 `<h3>` -->
        <h3 class="section-title">优惠券</h3>
        <!-- [zh] 开始标签 `<dl>` -->
        <dl class="info-list">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="info-row">
            <!-- [zh] 开始标签 `<dt>` -->
            <dt>使用优惠券</dt>
            <!-- [zh] 开始标签 `<dd>` -->
            <dd>{{ orderCouponSummaryText(order) }}</dd>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</dl>` -->
        </dl>
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<section>` -->
      <section class="amount-card card">
        <!-- [zh] 开始标签 `<OrderAmountSummary>` -->
        <OrderAmountSummary :order="order" />
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="showPayBtn || showLogisticsBtn" class="detail-actions">
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button v-if="showPayBtn" type="primary" round @click="goPay">去支付</el-button>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button v-if="showLogisticsBtn" type="primary" plain round @click="goLogistics">查看物流</el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>

    <el-empty v-else-if="!loading" description="订单不存在或无权查看" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Ticket } from '@element-plus/icons-vue';
import ProductImage from '@/components/common/ProductImage.vue';
import OrderAmountSummary from '@/components/business/OrderAmountSummary.vue';
import OrderItemIdText from '@/components/business/OrderItemIdText.vue';
import { orderApi } from '@/api/modules';
import { displayOrderStatusText } from '@/constants/backendEnums';
import { usePageRefresh } from '@/composables/pullRefresh';
import { confirmAction } from '@/utils/confirm';
import { toast } from '@/utils/toast';
import { hasOrderCouponDiscount, orderCouponSummaryText } from '@/utils/orderAmount';

const route = useRoute();
const router = useRouter();
const loading = ref(true);
const order = ref<Record<string, any> | null>(null);

const itemList = computed(() => {
  const list = order.value?.orderItemList;
  return Array.isArray(list) ? list : [];
});

const isCouponOrder = computed(() => String(order.value?.payScene) === '2');

const showPayBtn = computed(() => order.value?.orderStatus === 0 && order.value?.payOrderId);

const displayStatus = computed(() => {
  if (!order.value) return '';
  if (isCouponOrder.value && order.value.orderStatus === 3) return '已完成';
  return displayOrderStatusText(order.value);
});

const statusClass = computed(() => {
  const o = order.value;
  if (!o) return '';
  if (o.orderStatus === 0) return 'is-wait-pay';
  if (o.orderStatus === 2) return 'is-shipped';
  if (o.orderStatus === 4 || o.orderStatus === 5) return 'is-cancel';
  return '';
});

const showLogisticsBtn = computed(() => {
  if (isCouponOrder.value) return false;
  const s = order.value?.orderStatus;
  return s === 2 || s === 3 || s === 7;
});

const itemOrderStatus = (item: Record<string, any>) => Number(item.orderItemStatus ?? 1);

const canRefundItem = (item: Record<string, any>) => {
  if (isCouponOrder.value) return false;
  const s = order.value?.orderStatus;
  return (s === 1 || s === 2) && itemOrderStatus(item) === 1;
};

const refundItem = async (orderItemId: string) => {
  const ok = await confirmAction('确定要申请退款吗？退款将按原支付方式退回。', {
    title: '申请退款',
    confirmButtonText: '申请退款'
  });
  if (!ok) return;
  await orderApi.refundOrder(orderItemId);
  toast.success('退款申请已提交');
  load();
};

const formatMoney = (val: unknown) => Number(val ?? 0).toFixed(2);

const formatTime = (val: unknown) => {
  if (!val) return '--';
  if (typeof val === 'string') return val;
  const d = new Date(val as string | number);
  if (Number.isNaN(d.getTime())) return String(val);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const goProduct = (productId: string) => {
  if (productId) router.push(`/product/${productId}`);
};

const goLogistics = () => {
  router.push(`/order/${route.params.orderId}/logistics`);
};

const goPay = () => {
  const payOrderId = order.value?.payOrderId;
  if (payOrderId) router.push(`/payment/${payOrderId}`);
};

const load = async () => {
  loading.value = true;
  try {
    order.value = (await orderApi.getMyOrderDetail(String(route.params.orderId))) || null;
  } finally {
    loading.value = false;
  }
};

onMounted(load);
usePageRefresh(load);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.order-detail-page {` */
.order-detail-page {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding-bottom: 16px;` */
  padding-bottom: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.card {` */
.card {
  /* [zh] 样式规则 `padding: 14px 16px;` */
  padding: 14px 16px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.status-card {` */
.status-card {
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `padding: 20px 16px;` */
  padding: 20px 16px;

  /* [zh] 样式规则 `.status-text {` */
  .status-text {
    /* [zh] 样式规则 `font-size: 18px;` */
    font-size: 18px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;

    /* [zh] 样式规则 `&.is-wait-pay {` */
    &.is-wait-pay {
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.is-shipped {` */
    &.is-shipped {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.is-cancel {` */
    &.is-cancel {
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.status-sub {` */
  .status-sub {
    /* [zh] 样式规则 `margin: 8px 0 0;` */
    margin: 8px 0 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.section-title {` */
.section-title {
  /* [zh] 样式规则 `margin: 0 0 12px;` */
  margin: 0 0 12px;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.info-list {` */
.info-list {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.info-row {` */
.info-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `padding: 8px 0;` */
  padding: 8px 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border-l` */
  border-bottom: 1px solid $color-border-light;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `dt {` */
  dt {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `font-weight: 400;` */
    font-weight: 400;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `dd {` */
  dd {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `text-align: right;` */
    text-align: right;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `word-break: break-all;` */
    word-break: break-all;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-list {` */
.goods-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-row {` */
.goods-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `padding: 10px 0;` */
  padding: 10px 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border-l` */
  border-bottom: 1px solid $color-border-light;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `background: rgba($color-primary, 0.04);` */
    background: rgba($color-primary, 0.04);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-cover-col {` */
.goods-cover-col {
  /* [zh] 样式规则 `flex: 0 0 72px;` */
  flex: 0 0 72px;
  /* [zh] 样式规则 `width: 72px;` */
  width: 72px;
  /* [zh] 样式规则 `aspect-ratio: 1;` */
  aspect-ratio: 1;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `:deep(.product-image) {` */
  :deep(.product-image) {
    /* [zh] 样式规则 `width: 100% !important;` */
    width: 100% !important;
    /* [zh] 样式规则 `height: 100% !important;` */
    height: 100% !important;
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
      /* [zh] 样式规则 `font-size: 32px;` */
      font-size: 32px;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-info {` */
.goods-info {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;

  /* [zh] 样式规则 `.goods-name {` */
  .goods-name {
    /* [zh] 样式规则 `margin: 0 0 4px;` */
    margin: 0 0 4px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
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

  /* [zh] 样式规则 `.goods-sku {` */
  .goods-sku {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.goods-remark {` */
  .goods-remark {
    /* [zh] 样式规则 `margin: 4px 0 0;` */
    margin: 4px 0 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
    /* [zh] 样式规则 `color: #000;` */
    color: #000;
    /* [zh] 样式规则 `word-break: break-all;` */
    word-break: break-all;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-price {` */
.goods-price {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: flex-end;` */
  align-items: flex-end;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;

  /* [zh] 样式规则 `.price {` */
  .price {
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.qty {` */
  .qty {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-action {` */
.goods-action {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-end;` */
  align-items: flex-end;
  /* [zh] 样式规则 `padding-left: 8px;` */
  padding-left: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.amount-card {` */
.amount-card {
  /* [zh] 样式规则 `.amount-row {` */
  .amount-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: baseline;` */
    align-items: baseline;
    /* [zh] 样式规则 `justify-content: flex-end;` */
    justify-content: flex-end;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;

    /* [zh] 样式规则 `.amount {` */
    .amount {
      /* [zh] 样式规则 `font-size: 20px;` */
      font-size: 20px;
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

/* [zh] 样式规则 `.detail-actions {` */
.detail-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding: 4px 0 8px;` */
  padding: 4px 0 8px;
/* [zh] 样式规则 `}` */
}
</style>
