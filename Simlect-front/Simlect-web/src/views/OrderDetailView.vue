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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { Ticket } from '@element-plus/icons-vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/components/business/OrderAmountSummary.vue`
import OrderAmountSummary from '@/components/business/OrderAmountSummary.vue';
// [zh] 导入 `@/components/business/OrderItemIdText.vue`
import OrderItemIdText from '@/components/business/OrderItemIdText.vue';
// [zh] 导入 `@/api/modules`
import { orderApi } from '@/api/modules';
// [zh] 导入 `@/constants/backendEnums`
import { displayOrderStatusText } from '@/constants/backendEnums';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';
// [zh] 导入 `@/utils/confirm`
import { confirmAction } from '@/utils/confirm';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/utils/orderAmount`
import { hasOrderCouponDiscount, orderCouponSummaryText } from '@/utils/orderAmount';

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `loading`
const loading = ref(true);
// [zh] 声明变量 `order`
const order = ref<Record<string, any> | null>(null);

// [zh] 声明变量 `itemList`
const itemList = computed(() => {
  // [zh] 声明变量 `list`
  const list = order.value?.orderItemList;
  // [zh] return 返回
  return Array.isArray(list) ? list : [];
// [zh] 语句
});

// [zh] 声明变量 `isCouponOrder`
const isCouponOrder = computed(() => String(order.value?.payScene) === '2');

// [zh] 声明变量 `showPayBtn`
const showPayBtn = computed(() => order.value?.orderStatus === 0 && order.value?.payOrderId);

// [zh] 声明变量 `displayStatus`
const displayStatus = computed(() => {
  // [zh] 条件判断 if
  if (!order.value) return '';
  // [zh] 条件判断 if
  if (isCouponOrder.value && order.value.orderStatus === 3) return '已完成';
  // [zh] return 返回
  return displayOrderStatusText(order.value);
// [zh] 语句
});

// [zh] 声明变量 `statusClass`
const statusClass = computed(() => {
  // [zh] 声明变量 `o`
  const o = order.value;
  // [zh] 条件判断 if
  if (!o) return '';
  // [zh] 条件判断 if
  if (o.orderStatus === 0) return 'is-wait-pay';
  // [zh] 条件判断 if
  if (o.orderStatus === 2) return 'is-shipped';
  // [zh] 条件判断 if
  if (o.orderStatus === 4 || o.orderStatus === 5) return 'is-cancel';
  // [zh] return 返回
  return '';
// [zh] 语句
});

// [zh] 声明变量 `showLogisticsBtn`
const showLogisticsBtn = computed(() => {
  // [zh] 条件判断 if
  if (isCouponOrder.value) return false;
  // [zh] 声明变量 `s`
  const s = order.value?.orderStatus;
  // [zh] return 返回
  return s === 2 || s === 3 || s === 7;
// [zh] 语句
});

// [zh] 声明变量 `itemOrderStatus`
const itemOrderStatus = (item: Record<string, any>) => Number(item.orderItemStatus ?? 1);

// [zh] 声明变量 `canRefundItem`
const canRefundItem = (item: Record<string, any>) => {
  // [zh] 条件判断 if
  if (isCouponOrder.value) return false;
  // [zh] 声明变量 `s`
  const s = order.value?.orderStatus;
  // [zh] return 返回
  return (s === 1 || s === 2) && itemOrderStatus(item) === 1;
// [zh] 语句
};

// [zh] 声明变量 `refundItem`
const refundItem = async (orderItemId: string) => {
  // [zh] 声明变量 `ok`
  const ok = await confirmAction('确定要申请退款吗？退款将按原支付方式退回。', {
    // [zh] TypeScript/JS 语句：`title: '申请退款',`
    title: '申请退款',
    // [zh] TypeScript/JS 语句：`confirmButtonText: '申请退款'`
    confirmButtonText: '申请退款'
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (!ok) return;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await orderApi.refundOrder(orderItemId);
  // [zh] 语句
  toast.success('退款申请已提交');
  // [zh] 语句
  load();
// [zh] 语句
};

// [zh] 声明变量 `formatMoney`
const formatMoney = (val: unknown) => Number(val ?? 0).toFixed(2);

// [zh] 声明变量 `formatTime`
const formatTime = (val: unknown) => {
  // [zh] 条件判断 if
  if (!val) return '--';
  // [zh] 条件判断 if
  if (typeof val === 'string') return val;
  // [zh] 声明变量 `d`
  const d = new Date(val as string | number);
  // [zh] 条件判断 if
  if (Number.isNaN(d.getTime())) return String(val);
  // [zh] 声明变量 `pad`
  const pad = (n: number) => String(n).padStart(2, '0');
  // [zh] return 返回
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
// [zh] 语句
};

// [zh] 声明变量 `goProduct`
const goProduct = (productId: string) => {
  // [zh] 条件判断 if
  if (productId) router.push(`/product/${productId}`);
// [zh] 语句
};

// [zh] 声明变量 `goLogistics`
const goLogistics = () => {
  // [zh] 语句
  router.push(`/order/${route.params.orderId}/logistics`);
// [zh] 语句
};

// [zh] 声明变量 `goPay`
const goPay = () => {
  // [zh] 声明变量 `payOrderId`
  const payOrderId = order.value?.payOrderId;
  // [zh] 条件判断 if
  if (payOrderId) router.push(`/payment/${payOrderId}`);
// [zh] 语句
};

// [zh] 声明变量 `load`
const load = async () => {
  // [zh] 语句
  loading.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 语句
    order.value = (await orderApi.getMyOrderDetail(String(route.params.orderId))) || null;
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    loading.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 语句
onMounted(load);
// [zh] 语句
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
