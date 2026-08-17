<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="orders-page">
    <!-- [zh] 开始标签 `<div>` -->
    <div ref="tabsRef" class="orders-tabs card-flat tabs-scroll-single">
      <!-- [zh] 开始标签 `<el-tabs>` -->
      <el-tabs v-model="tab" @tab-change="onTabChange">
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="全部" name="" />
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="待付款" name="0" />
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="待发货" name="1" />
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="待收货" name="2" />
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="已完成" name="completed" />
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="待评价" name="evaluate" />
      <!-- [zh] 闭合标签 `</el-tabs>` -->
      </el-tabs>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="scrollRoot" class="orders-body">
      <!-- [zh] 开始标签 `<el-skeleton>` -->
      <el-skeleton :loading="loading && !list.length" animated :count="2">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #default>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="displayList.length" class="order-list">
            <!-- [zh] 开始标签 `<SwipeDeleteRow>` -->
            <SwipeDeleteRow
              v-for="order in displayList"
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
                      v-if="canRefundItem(order, item)"
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

                <!-- [zh] 开始标签 `<footer>` -->
                <footer class="order-foot">
                  <!-- [zh] 开始标签 `<OrderAmountSummary>` -->
                  <OrderAmountSummary :order="order" compact />
                  <!-- [zh] 开始标签 `<div>` -->
                  <div class="order-ops">
                    <!-- [zh] 开始标签 `<el-button>` -->
                    <el-button
                      v-if="order.orderStatus === 0"
                      size="small"
                      @click.stop="cancel(order.orderId)"
                    >
                      <!-- [zh] 模板内容：`取消订单` -->
                      取消订单
                    <!-- [zh] 闭合标签 `</el-button>` -->
                    </el-button>
                    <!-- [zh] 开始标签 `<el-button>` -->
                    <el-button
                      v-if="order.orderStatus === 0"
                      type="primary"
                      size="small"
                      @click.stop="goPay(order.payOrderId)"
                    >
                      <!-- [zh] 模板内容：`去支付` -->
                      去支付
                    <!-- [zh] 闭合标签 `</el-button>` -->
                    </el-button>
                    <!-- [zh] 开始标签 `<el-button>` -->
                    <el-button
                      v-if="order.orderStatus === 2"
                      type="primary"
                      size="small"
                      @click.stop="confirmReceive(order.orderId)"
                    >
                      <!-- [zh] 模板内容：`确认收货` -->
                      确认收货
                    <!-- [zh] 闭合标签 `</el-button>` -->
                    </el-button>
                    <!-- [zh] 开始标签 `<el-button>` -->
                    <el-button
                      v-if="canComment(order)"
                      type="primary"
                      size="small"
                      @click.stop="openComment(order.orderId)"
                    >
                      <!-- [zh] 模板内容：`评价` -->
                      评价
                    <!-- [zh] 闭合标签 `</el-button>` -->
                    </el-button>
                    <!-- [zh] 开始标签 `<el-button>` -->
                    <el-button
                      v-if="canRecomment(order)"
                      size="small"
                      @click.stop="openRecomment(order.orderId)"
                    >
                      <!-- [zh] 模板内容：`追评` -->
                      追评
                    <!-- [zh] 闭合标签 `</el-button>` -->
                    </el-button>
                    <!-- [zh] 开始标签 `<el-button>` -->
                    <el-button
                      v-if="canViewComment(order)"
                      size="small"
                      @click.stop="openCommentPreview(order.orderId)"
                    >
                      <!-- [zh] 模板内容：`查看评价` -->
                      查看评价
                    <!-- [zh] 闭合标签 `</el-button>` -->
                    </el-button>
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
          <el-empty v-else-if="!loading" description="暂无相关订单" class="orders-empty" />
          <!-- [zh] 开始标签 `<div>` -->
          <div ref="sentinelRef" class="load-sentinel" />
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="loadingMore" class="load-tip">加载中…</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-else-if="finished && displayList.length" class="load-tip muted">没有更多了</p>
        </template>
      </el-skeleton>
    </div>

    <OrderCommentDialog ref="commentDialogRef" @success="onTabChange" />
    <OrderRecommentDialog ref="recommentDialogRef" @success="onTabChange" />
    <OrderCommentPreviewDialog ref="commentPreviewDialogRef" />
  </div>

</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Ticket } from '@element-plus/icons-vue';
import ProductImage from '@/components/common/ProductImage.vue';
import SwipeDeleteRow from '@/components/business/SwipeDeleteRow.vue';
import OrderAmountSummary from '@/components/business/OrderAmountSummary.vue';
import OrderItemIdText from '@/components/business/OrderItemIdText.vue';
const OrderCommentDialog = defineAsyncComponent(
  () => import('@/components/business/OrderCommentDialog.vue')
);
const OrderRecommentDialog = defineAsyncComponent(
  () => import('@/components/business/OrderRecommentDialog.vue')
);
const OrderCommentPreviewDialog = defineAsyncComponent(
  () => import('@/components/business/OrderCommentPreviewDialog.vue')
);
import { usePageListCache } from '@/composables/usePageListCache';
import { usePageRefresh } from '@/composables/pullRefresh';
import { useDevice } from '@/composables/useDevice';
import { orderApi } from '@/api/modules';
import { displayOrderStatusText } from '@/constants/backendEnums';
import { confirmAction } from '@/utils/confirm';
import { toast } from '@/utils/toast';

const router = useRouter();
const route = useRoute();

const initTab = (() => {
  const q = route.query.status as string | undefined;
  if (q === '3') return 'completed';
  if (q === '8') return 'evaluate';
  return q || '';
})();

const tab = ref(initTab);
const apiStatus = computed(() => {
  if (tab.value === 'completed') return '3';
  if (tab.value === 'evaluate') return '8';
  return tab.value || undefined;
});
const pageNo = ref(0);
const pageTotal = ref(1);
const list = ref<any[]>([]);
const loading = ref(false);
const loadingMore = ref(false);
const loadError = ref('');
const finished = ref(false);
const scrollRoot = ref<HTMLElement>();
const tabsRef = ref<HTMLElement>();
const sentinelRef = ref<HTMLElement>();
const commentDialogRef = ref<InstanceType<typeof OrderCommentDialog>>();
const recommentDialogRef = ref<InstanceType<typeof OrderRecommentDialog>>();
const commentPreviewDialogRef = ref<InstanceType<typeof OrderCommentPreviewDialog>>();
const openSwipeId = ref<string | null>(null);
let observer: IntersectionObserver | null = null;
let tabsResizeObserver: ResizeObserver | null = null;

const { isMobile } = useDevice();

const usesWindowScroll = () => isMobile.value;

const scrollPageToTop = () => {
  if (usesWindowScroll()) {
    window.scrollTo(0, 0);
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;
    return;
  }
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
};

const syncOrdersTabsInset = () => {
  document.documentElement.style.removeProperty('--orders-tabs-height');
};

const isCouponOrder = (order: Record<string, any>) => String(order.payScene) === '2';

const displayList = computed(() => {
  if (tab.value === 'evaluate') {
    return list.value.filter((o) => !isCouponOrder(o) && Number(o.commentStatus) === 0);
  }
  return list.value;
});

const canDeleteOrder = (order: Record<string, any>) => {
  const s = Number(order.orderStatus);
  return s === 3 || s === 4 || s === 5 || s === 6;
};

const formatMoney = (val: unknown) => Number(val ?? 0).toFixed(2);
const displayStatus = (order: Record<string, any>) => {
  if (tab.value === 'completed' && order.orderStatus === 3) return '已完成';
  const text = displayOrderStatusText(order);
  if (isCouponOrder(order)) {
    if (order.orderStatus === 3) return '已完成';
    if (order.orderStatus === 4) return '已取消';
  }
  return text;
};

const statusClass = (order: Record<string, any>) => {
  const status = order.orderStatus;
  if (status === 3) {
    if (tab.value === 'completed') return '';
    if (isCouponOrder(order)) return '';
    if (order.commentStatus === 0) return 'is-comment-pending';
    if (order.commentStatus === 1) return 'is-commented';
    if (order.commentStatus === 2) return 'is-recommented';
  }
  if (status === 0) return 'is-wait-pay';
  if (status === 2) return 'is-shipped';
  if (status === 4 || status === 5) return 'is-cancel';
  return '';
};

const canComment = (order: Record<string, any>) =>
  !isCouponOrder(order) && order.orderStatus === 3 && Number(order.commentStatus) === 0;

const canRecomment = (order: Record<string, any>) =>
  !isCouponOrder(order) && order.orderStatus === 3 && Number(order.commentStatus) === 1;

const canViewComment = (order: Record<string, any>) =>
  !isCouponOrder(order) &&
  order.orderStatus === 3 &&
  (Number(order.commentStatus) === 1 || Number(order.commentStatus) === 2);

const showLogistics = (order: Record<string, any>) =>
  !isCouponOrder(order) &&
  (order.orderStatus === 2 || order.orderStatus === 3 || order.orderStatus === 7);

const canRefundItem = (order: Record<string, any>, item: Record<string, any>) =>
  !isCouponOrder(order) &&
  (order.orderStatus === 1 || order.orderStatus === 2) &&
  Number(item.orderItemStatus) === 1;

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
  cacheKey: () => `/orders|tab=${tab.value}`,
  getState: () => ({
    tab: tab.value,
    list: list.value,
    pageNo: pageNo.value,
    pageTotal: pageTotal.value,
    finished: finished.value
  }),
  setState: (state) => {
    tab.value = String(state.tab ?? '');
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
    const r = await orderApi.loadMyOrder({
      pageNo: next,
      status: apiStatus.value
    });
    const chunk = r?.list || [];
    if (next === 1) list.value = chunk;
    else list.value = list.value.concat(chunk);
    pageNo.value = r?.pageNo ?? next;
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    finished.value = pageNo.value >= pageTotal.value;
  } catch (e: any) {
    loadError.value = e?.info || e?.message || '订单加载失败，请稍后重试';
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
  scrollPageToTop();

  const queryStatus = apiStatus.value || undefined;
  router.replace({ path: route.path, query: queryStatus ? { status: queryStatus } : {} });
  loadMore();
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

const cancel = async (id: string) => {
  const ok = await confirmAction('取消后订单将关闭，确定要取消该订单吗？', {
    title: '取消订单',
    confirmButtonText: '取消订单'
  });
  if (!ok) return;
  await orderApi.cancelOrder(id);
  toast.success('订单已取消');
  onTabChange();
};

const confirmReceive = async (id: string) => {
  const ok = await confirmAction('确认收货后将无法发起退款，确定已收到商品吗？', {
    title: '确认收货',
    confirmButtonText: '确认收货'
  });
  if (!ok) return;
  await orderApi.confirmOrder(id);
  toast.success('已确认收货');
  onTabChange();
};

const goPay = (id: string) => {
  if (id) router.push(`/payment/${id}`);
};

const openComment = (orderId: string) => {
  commentDialogRef.value?.show(orderId);
};

const openRecomment = (orderId: string) => {
  recommentDialogRef.value?.show(orderId);
};

const openCommentPreview = (orderId: string) => {
  commentPreviewDialogRef.value?.show(orderId);
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
  syncOrdersTabsInset();
  tabsResizeObserver = new ResizeObserver(syncOrdersTabsInset);
  if (tabsRef.value) tabsResizeObserver.observe(tabsRef.value);

  const restored = await pageCache.tryRestore();
  if (!restored) {
    await loadMore();
  }
  setupObserver();
});

usePageRefresh(onTabChange, {
  getScrollEl: () => (usesWindowScroll() ? null : scrollRoot.value)
});

onUnmounted(() => {
  observer?.disconnect();
  tabsResizeObserver?.disconnect();
  document.documentElement.style.removeProperty('--orders-tabs-height');
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.orders-page {` */
.orders-page {
  /* [zh] 样式规则 `min-height: calc(100vh - var(--sub-top-h` */
  min-height: calc(100vh - var(--sub-top-height, 60px));
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.orders-tabs {` */
.orders-tabs {
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

/* [zh] 样式规则 `.orders-body {` */
.orders-body {
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

    /* [zh] 样式规则 `&.is-comment-pending {` */
    &.is-comment-pending {
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.is-commented {` */
    &.is-commented {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.is-recommented {` */
    &.is-recommented {
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
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
  /* [zh] 样式规则 `padding: 10px 0;` */
  padding: 10px 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;
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

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `background: rgba($color-primary, 0.04);` */
    background: rgba($color-primary, 0.04);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-cover-col {` */
.goods-cover-col {
  /* [zh] 样式规则 `flex: 0 0 25%;` */
  flex: 0 0 25%;
  /* [zh] 样式规则 `width: 25%;` */
  width: 25%;
  /* [zh] 样式规则 `max-width: 76px;` */
  max-width: 76px;
  /* [zh] 样式规则 `aspect-ratio: 1;` */
  aspect-ratio: 1;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `:deep(.product-image) {` */
  :deep(.product-image) {
    /* [zh] 样式规则 `width: 100% !important;` */
    width: 100% !important;
    /* [zh] 样式规则 `height: 100% !important;` */
    height: 100% !important;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
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
      /* [zh] 样式规则 `color: rgba($color-primary, 0.45);` */
      color: rgba($color-primary, 0.45);
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
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
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

/* [zh] 样式规则 `.order-foot {` */
.order-foot {
  /* [zh] 样式规则 `padding: 10px 12px 12px;` */
  padding: 10px 12px 12px;
  /* [zh] 样式规则 `border-top: 1px solid $color-border;` */
  border-top: 1px solid $color-border;

  /* [zh] 样式规则 `:deep(.order-amount-summary.compact) {` */
  :deep(.order-amount-summary.compact) {
    /* [zh] 样式规则 `margin-bottom: 8px;` */
    margin-bottom: 8px;
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
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.orders-empty {` */
.orders-empty {
  /* [zh] 样式规则 `padding: 48px 0;` */
  padding: 48px 0;
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
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `padding: 12px 0;` */
  padding: 12px 0;

  /* [zh] 样式规则 `&.muted {` */
  &.muted {
    /* [zh] 样式规则 `opacity: 0.85;` */
    opacity: 0.85;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (max-width: $breakpoint-mobile) {` */
@media (max-width: $breakpoint-mobile) {
  /* [zh] 样式规则 `.orders-page {` */
  .orders-page {
    /* [zh] 样式规则 `min-height: auto;` */
    min-height: auto;
    /* [zh] 样式规则 `max-height: none;` */
    max-height: none;
    /* [zh] 样式规则 `padding-top: 0;` */
    padding-top: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.orders-tabs {` */
  .orders-tabs {
    /* [zh] 样式规则 `position: sticky;` */
    position: sticky;
    /* [zh] 样式规则 `top: var(--sub-top-height, 60px);` */
    top: var(--sub-top-height, 60px);
    /* [zh] 样式规则 `z-index: 99;` */
    z-index: 99;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `border-radius: 0;` */
    border-radius: 0;
    /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
    border-bottom: 1px solid $color-border;
    /* [zh] 样式规则 `box-shadow: none;` */
    box-shadow: none;
    /* [zh] 样式规则 `background: #fff;` */
    background: #fff;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.orders-tabs.tabs-scroll-single {` */
  .orders-tabs.tabs-scroll-single {
    /* [zh] 样式规则 `:deep(.el-tabs__header) {` */
    :deep(.el-tabs__header) {
      /* [zh] 样式规则 `margin-bottom: 0;` */
      margin-bottom: 0;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(.el-tabs__content) {` */
    :deep(.el-tabs__content) {
      /* [zh] 样式规则 `display: none;` */
      display: none;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(.el-tabs__nav-scroll) {` */
    :deep(.el-tabs__nav-scroll) {
      /* [zh] 样式规则 `overflow-x: hidden;` */
      overflow-x: hidden;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(.el-tabs__nav) {` */
    :deep(.el-tabs__nav) {
      /* [zh] 样式规则 `width: 100%;` */
      width: 100%;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `justify-content: space-between;` */
      justify-content: space-between;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(.el-tabs__item) {` */
    :deep(.el-tabs__item) {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `min-width: 0;` */
      min-width: 0;
      /* [zh] 样式规则 `padding: 0 4px;` */
      padding: 0 4px;
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.orders-body {` */
  .orders-body {
    /* [zh] 样式规则 `overflow: visible;` */
    overflow: visible;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.order-list :deep(.swipe-delete-row) {` */
  .order-list :deep(.swipe-delete-row) {
    /* [zh] 样式规则 `z-index: 0;` */
    z-index: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-list {` */
.order-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

@media (min-width: #{$breakpoint-mobile + 1}) {
  /* [zh] 样式规则 `.orders-page {` */
  .orders-page {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `max-height: calc(100vh - var(--sub-top-h` */
    max-height: calc(100vh - var(--sub-top-height, 60px));
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.orders-body {` */
  .orders-body {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `overflow-y: auto;` */
    overflow-y: auto;
    /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
    -webkit-overflow-scrolling: touch;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
