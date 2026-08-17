<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="after-sale-page">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="after-sale-tabs card-flat tabs-scroll-single">
      <!-- [zh] 开始标签 `<el-tabs>` -->
      <el-tabs v-model="tab" @tab-change="onTabChange">
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="全部" name="all" />
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="已退款" name="6" />
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="部分退款" name="7" />
      <!-- [zh] 闭合标签 `</el-tabs>` -->
      </el-tabs>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="scrollRoot" class="after-sale-body">
      <!-- [zh] 开始标签 `<el-skeleton>` -->
      <el-skeleton :loading="loading && !list.length" animated :count="2">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #default>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="list.length" class="order-list">
            <!-- [zh] 开始标签 `<SwipeDeleteRow>` -->
            <SwipeDeleteRow
              v-for="order in list"
              :key="order.orderId"
              :deletable="canDeleteOrder(order)"
              :open="openSwipeId === order.orderId"
              @open="openSwipeId = order.orderId"
              @close="onSwipeClose(order.orderId)"
              @delete="removeOrder(order.orderId)"
            >
              <!-- [zh] 开始标签 `<section>` -->
              <section class="order-card card">
                <!-- [zh] 开始标签 `<header>` -->
                <header class="order-head">
                  <!-- [zh] 开始标签 `<span>` -->
                  <span class="order-no">订单号：{{ order.orderId }}</span>
                  <!-- [zh] 开始标签 `<span>` -->
                  <span class="order-status" :class="statusClass(order)">
                    <!-- [zh] Mustache 插值表达式 -->
                    {{ displayStatus(order) }}
                  <!-- [zh] 闭合标签 `</span>` -->
                  </span>
                <!-- [zh] 闭合标签 `</header>` -->
                </header>

                <!-- [zh] 开始标签 `<div>` -->
                <div class="goods-list">
                  <!-- [zh] 开始标签 `<component>` -->
                  <component
                    :is="isCouponOrder(order) ? 'div' : 'button'"
                    v-for="item in order.orderItemList || []"
                    :key="item.orderItemId"
                    :type="isCouponOrder(order) ? undefined : 'button'"
                    class="goods-row"
                    @click="!isCouponOrder(order) && goProduct(item.productId)"
                  >
                    <!-- [zh] 开始标签 `<div>` -->
                    <div class="goods-cover-col" :class="{ 'is-coupon': isCouponOrder(order) }">
                      <!-- [zh] 开始标签 `<el-icon>` -->
                      <el-icon v-if="isCouponOrder(order)" class="coupon-icon"><Ticket /></el-icon>
                      <!-- [zh] 开始标签 `<ProductImage>` -->
                      <ProductImage v-else :source="item.cover" class="goods-cover" />
                    <!-- [zh] 闭合标签 `</div>` -->
                    </div>
                    <!-- [zh] 开始标签 `<div>` -->
                    <div class="goods-info">
                      <!-- [zh] 开始标签 `<p>` -->
                      <p class="goods-name">{{ item.productName }}</p>
                      <!-- [zh] 开始标签 `<p>` -->
                      <p v-if="item.propertyInfo && !isCouponOrder(order)" class="goods-sku">
                        <!-- [zh] Mustache 插值表达式 -->
                        {{ item.propertyInfo }}
                      <!-- [zh] 闭合标签 `</p>` -->
                      </p>
                      <!-- [zh] 开始标签 `<OrderItemIdText>` -->
                      <OrderItemIdText :id="item.orderItemId" />
                      <!-- [zh] 开始标签 `<p>` -->
                      <p v-if="Number(item.orderItemStatus) === 0" class="item-refund-tag">已退款</p>
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
                    <div v-if="canRefundItem(order, item)" class="goods-action">
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

                <!-- [zh] 开始标签 `<footer>` -->
                <footer class="order-foot">
                  <!-- [zh] 开始标签 `<div>` -->
                  <div class="pay-line">
                    <!-- [zh] 开始标签 `<span>` -->
                    <span class="label">实付款</span>
                    <!-- [zh] 开始标签 `<span>` -->
                    <span class="amount">¥{{ formatMoney(order.amount) }}</span>
                  <!-- [zh] 闭合标签 `</div>` -->
                  </div>
                  <!-- [zh] 开始标签 `<div>` -->
                  <div class="order-ops">
                    <!-- [zh] 开始标签 `<el-button>` -->
                    <el-button
                      v-if="showLogistics(order)"
                      size="small"
                      @click.stop="goLogistics(order.orderId)"
                    >
                      <!-- [zh] 模板内容：`查看物流` -->
                      查看物流
                    <!-- [zh] 闭合标签 `</el-button>` -->
                    </el-button>
                    <!-- [zh] 开始标签 `<el-button>` -->
                    <el-button size="small" link @click.stop="goOrderDetail(order.orderId)">
                      <!-- [zh] 模板内容：`订单详情` -->
                      订单详情
                    <!-- [zh] 闭合标签 `</el-button>` -->
                    </el-button>
                  <!-- [zh] 闭合标签 `</div>` -->
                  </div>
                <!-- [zh] 闭合标签 `</footer>` -->
                </footer>
              <!-- [zh] 闭合标签 `</section>` -->
              </section>
            <!-- [zh] 闭合标签 `</SwipeDeleteRow>` -->
            </SwipeDeleteRow>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<el-empty>` -->
          <el-empty v-else-if="loadError && !loading" :description="loadError" class="orders-empty">
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button type="primary" @click="onTabChange">重试</el-button>
          <!-- [zh] 闭合标签 `</el-empty>` -->
          </el-empty>
          <!-- [zh] 开始标签 `<el-empty>` -->
          <el-empty v-else-if="!loading" description="暂无售后订单" class="orders-empty" />
          <!-- [zh] 开始标签 `<div>` -->
          <div ref="sentinelRef" class="load-sentinel" />
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="loadingMore" class="load-tip">加载中…</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-else-if="finished && list.length" class="load-tip muted">没有更多了</p>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { Ticket } from '@element-plus/icons-vue';
import ProductImage from '@/components/common/ProductImage.vue';
import OrderItemIdText from '@/components/business/OrderItemIdText.vue';
import SwipeDeleteRow from '@/components/business/SwipeDeleteRow.vue';
import { usePageListCache } from '@/composables/usePageListCache';
import { usePageRefresh } from '@/composables/pullRefresh';
import { useDevice } from '@/composables/useDevice';
import { orderApi } from '@/api/modules';
import { orderStatusLabel } from '@/constants/backendEnums';
import { confirmAction } from '@/utils/confirm';
import { toast } from '@/utils/toast';

const router = useRouter();

const tab = ref('all');
const pageNo = ref(0);
const pageTotal = ref(1);
const list = ref<any[]>([]);
const loading = ref(false);
const loadingMore = ref(false);
const loadError = ref('');
const finished = ref(false);
const scrollRoot = ref<HTMLElement>();
const sentinelRef = ref<HTMLElement>();
const openSwipeId = ref<string | null>(null);
let observer: IntersectionObserver | null = null;

const { isMobile } = useDevice();
const usesWindowScroll = () => isMobile.value;

const isCouponOrder = (order: Record<string, any>) => String(order.payScene) === '2';

const formatMoney = (val: unknown) => Number(val ?? 0).toFixed(2);

const displayStatus = (order: Record<string, any>) =>
  order.orderStatusName || orderStatusLabel(order.orderStatus);

const statusClass = (order: Record<string, any>) => {
  const status = Number(order.orderStatus);
  if (status === 6) return 'is-refunded';
  if (status === 7) return 'is-partial-refund';
  return '';
};

const canDeleteOrder = (order: Record<string, any>) => Number(order.orderStatus) === 6;

const showLogistics = (order: Record<string, any>) =>
  !isCouponOrder(order) && Number(order.orderStatus) === 7;

const canRefundItem = (order: Record<string, any>, item: Record<string, any>) =>
  !isCouponOrder(order) &&
  (Number(order.orderStatus) === 1 ||
    Number(order.orderStatus) === 2 ||
    Number(order.orderStatus) === 7) &&
  Number(item.orderItemStatus) === 1;

const mergeOrders = (rows: any[]) => {
  const map = new Map<string, any>();
  for (const row of rows) {
    if (row?.orderId) map.set(row.orderId, row);
  }
  return [...map.values()].sort((a, b) => {
    const ta = new Date(a.orderTime || 0).getTime();
    const tb = new Date(b.orderTime || 0).getTime();
    return tb - ta;
  });
};

const fetchPage = async (next: number, status?: number) => {
  if (status != null) {
    return orderApi.loadMyOrder({ pageNo: next, status });
  }
  const [r6, r7] = await Promise.all([
    orderApi.loadMyOrder({ pageNo: next, status: 6 }),
    orderApi.loadMyOrder({ pageNo: next, status: 7 })
  ]);
  return {
    list: mergeOrders([...(r6?.list || []), ...(r7?.list || [])]),
    pageNo: Math.max(r6?.pageNo ?? next, r7?.pageNo ?? next),
    pageTotal: Math.max(r6?.pageTotal ?? next, r7?.pageTotal ?? next)
  };
};

const setupObserver = () => {
  observer?.disconnect();
  if (!sentinelRef.value) return;
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) loadMore();
    },
    { root: usesWindowScroll() ? null : scrollRoot.value, rootMargin: '100px' }
  );
  observer.observe(sentinelRef.value);
};

const pageCache = usePageListCache({
  cacheKey: () => `/after-sale|tab=${tab.value}`,
  getState: () => ({
    tab: tab.value,
    list: list.value,
    pageNo: pageNo.value,
    pageTotal: pageTotal.value,
    finished: finished.value
  }),
  setState: (state) => {
    tab.value = String(state.tab ?? 'all');
    list.value = (state.list as any[]) || [];
    pageNo.value = Number(state.pageNo) || 0;
    pageTotal.value = Number(state.pageTotal) || 1;
    finished.value = !!state.finished;
    loading.value = false;
    loadingMore.value = false;
  },
  afterRestore: setupObserver
});

const loadMore = async () => {
  if (loadingMore.value || finished.value) return;
  if (pageNo.value >= pageTotal.value && pageNo.value > 0) {
    finished.value = true;
    return;
  }

  loadingMore.value = true;
  if (!list.value.length) loading.value = true;
  loadError.value = '';
  try {
    const next = pageNo.value + 1;
    const status = tab.value === 'all' ? undefined : Number(tab.value);
    const r = await fetchPage(next, status);
    const chunk = r?.list || [];
    if (next === 1) list.value = chunk;
    else list.value = mergeOrders(list.value.concat(chunk));
    pageNo.value = r?.pageNo ?? next;
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    finished.value = pageNo.value >= pageTotal.value;
  } catch (e: any) {
    loadError.value = e?.info || e?.message || '售后订单加载失败，请稍后重试';
  } finally {
    loadingMore.value = false;
    loading.value = false;
    nextTick(() => setupObserver());
  }
};

const onTabChange = () => {
  pageCache.clear();
  pageNo.value = 0;
  pageTotal.value = 1;
  finished.value = false;
  list.value = [];
  if (usesWindowScroll()) window.scrollTo(0, 0);
  else if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
  loadMore();
};

const refundItem = async (orderItemId: string) => {
  const ok = await confirmAction('确定要申请退款吗？退款将按原支付方式退回。', {
    title: '申请退款',
    confirmButtonText: '申请退款'
  });
  if (!ok) return;
  await orderApi.refundOrder(orderItemId);
  toast.success('退款申请已提交');
  onTabChange();
};

const goProduct = (productId: string) => {
  if (productId) router.push(`/product/${productId}`);
};

const goOrderDetail = (orderId: string) => {
  router.push(`/order/${orderId}`);
};

const goLogistics = (orderId: string) => {
  router.push(`/order/${orderId}/logistics`);
};

const onSwipeClose = (orderId: string) => {
  if (openSwipeId.value === orderId) openSwipeId.value = null;
};

const removeOrder = async (orderId: string) => {
  const ok = await confirmAction('删除后订单将从列表中移除，确定删除吗？', {
    title: '删除订单',
    confirmButtonText: '删除'
  });
  if (!ok) return;
  await orderApi.deleteOrder(orderId);
  list.value = list.value.filter((o) => o.orderId !== orderId);
  if (openSwipeId.value === orderId) openSwipeId.value = null;
  toast.success('订单已删除');
};

onMounted(async () => {
  const restored = await pageCache.tryRestore();
  if (!restored) await loadMore();
  setupObserver();
});

usePageRefresh(onTabChange, {
  getScrollEl: () => (usesWindowScroll() ? null : scrollRoot.value)
});

onUnmounted(() => {
  observer?.disconnect();
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.after-sale-page {` */
.after-sale-page {
  /* [zh] 样式规则 `min-height: calc(100vh - var(--sub-top-h` */
  min-height: calc(100vh - var(--sub-top-height, 60px));
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.after-sale-tabs {` */
.after-sale-tabs {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `padding: 0 4px;` */
  padding: 0 4px;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.after-sale-body {` */
.after-sale-body {
  /* [zh] 样式规则 `min-height: 120px;` */
  min-height: 120px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-list {` */
.order-list {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `z-index: 0;` */
  z-index: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-list :deep(.swipe-delete-row) {` */
.order-list :deep(.swipe-delete-row) {
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-card {` */
.order-card {
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-head {` */
.order-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `background: #fafafa;` */
  background: #fafafa;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;

  /* [zh] 样式规则 `.order-no {` */
  .order-no {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
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
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;

    /* [zh] 样式规则 `&.is-refunded {` */
    &.is-refunded {
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.is-partial-refund {` */
    &.is-partial-refund {
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-list {` */
.goods-list {
  /* [zh] 样式规则 `padding: 0 12px;` */
  padding: 0 12px;
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
  /* [zh] 样式规则 `padding: 12px 0;` */
  padding: 12px 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `& + & {` */
  & + & {
    /* [zh] 样式规则 `border-top: 1px solid $color-border;` */
    border-top: 1px solid $color-border;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-cover-col {` */
.goods-cover-col {
  /* [zh] 样式规则 `width: 72px;` */
  width: 72px;
  /* [zh] 样式规则 `height: 72px;` */
  height: 72px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;

  /* [zh] 样式规则 `&.is-coupon {` */
  &.is-coupon {
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `place-items: center;` */
    place-items: center;
    /* [zh] 样式规则 `background: linear-gradient(135deg, #fff` */
    background: linear-gradient(135deg, #fff7e6, #ffe7ba);

    /* [zh] 样式规则 `.coupon-icon {` */
    .coupon-icon {
      /* [zh] 样式规则 `font-size: 28px;` */
      font-size: 28px;
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-cover {` */
.goods-cover {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-info {` */
.goods-info {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-name {` */
.goods-name {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
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
  /* [zh] 样式规则 `margin: 4px 0 0;` */
  margin: 4px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-refund-tag {` */
.item-refund-tag {
  /* [zh] 样式规则 `margin: 4px 0 0;` */
  margin: 4px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-price {` */
.goods-price {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `text-align: right;` */
  text-align: right;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;

  /* [zh] 样式规则 `.price {` */
  .price {
    /* [zh] 样式规则 `display: block;` */
    display: block;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.qty {` */
  .qty {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-action {` */
.goods-action {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `align-self: center;` */
  align-self: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-foot {` */
.order-foot {
  /* [zh] 样式规则 `padding: 10px 12px 12px;` */
  padding: 10px 12px 12px;
  /* [zh] 样式规则 `border-top: 1px solid $color-border;` */
  border-top: 1px solid $color-border;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-line {` */
.pay-line {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;
  /* [zh] 样式规则 `align-items: baseline;` */
  align-items: baseline;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;

  /* [zh] 样式规则 `.label {` */
  .label {
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.amount {` */
  .amount {
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-ops {` */
.order-ops {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.load-sentinel {` */
.load-sentinel {
  /* [zh] 样式规则 `height: 1px;` */
  height: 1px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.load-tip {` */
.load-tip {
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `padding: 12px 0;` */
  padding: 12px 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;

  /* [zh] 样式规则 `&.muted {` */
  &.muted {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.orders-empty {` */
.orders-empty {
  /* [zh] 样式规则 `padding: 32px 0;` */
  padding: 32px 0;
/* [zh] 样式规则 `}` */
}
</style>
