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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, defineAsyncComponent, nextTick, onMounted, onUnmounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter, useRoute } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { Ticket } from '@element-plus/icons-vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/components/business/SwipeDeleteRow.vue`
import SwipeDeleteRow from '@/components/business/SwipeDeleteRow.vue';
// [zh] 导入 `@/components/business/OrderAmountSummary.vue`
import OrderAmountSummary from '@/components/business/OrderAmountSummary.vue';
// [zh] 导入 `@/components/business/OrderItemIdText.vue`
import OrderItemIdText from '@/components/business/OrderItemIdText.vue';
// [zh] 声明变量 `OrderCommentDialog`
const OrderCommentDialog = defineAsyncComponent(
  // [zh] TypeScript/JS 语句：`() => import('@/components/business/OrderCommentDialog.`
  () => import('@/components/business/OrderCommentDialog.vue')
// [zh] 语句
);
// [zh] 声明变量 `OrderRecommentDialog`
const OrderRecommentDialog = defineAsyncComponent(
  // [zh] TypeScript/JS 语句：`() => import('@/components/business/OrderRecommentDialo`
  () => import('@/components/business/OrderRecommentDialog.vue')
// [zh] 语句
);
// [zh] 声明变量 `OrderCommentPreviewDialog`
const OrderCommentPreviewDialog = defineAsyncComponent(
  // [zh] TypeScript/JS 语句：`() => import('@/components/business/OrderCommentPreview`
  () => import('@/components/business/OrderCommentPreviewDialog.vue')
// [zh] 语句
);
// [zh] 导入 `@/composables/usePageListCache`
import { usePageListCache } from '@/composables/usePageListCache';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/api/modules`
import { orderApi } from '@/api/modules';
// [zh] 导入 `@/constants/backendEnums`
import { displayOrderStatusText } from '@/constants/backendEnums';
// [zh] 导入 `@/utils/confirm`
import { confirmAction } from '@/utils/confirm';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `route`
const route = useRoute();

// [zh] 声明变量 `initTab`
const initTab = (() => {
  // [zh] 声明变量 `q`
  const q = route.query.status as string | undefined;
  // [zh] 条件判断 if
  if (q === '3') return 'completed';
  // [zh] 条件判断 if
  if (q === '8') return 'evaluate';
  // [zh] return 返回
  return q || '';
// [zh] 语句
})();

// [zh] 声明变量 `tab`
const tab = ref(initTab);
// [zh] 声明变量 `apiStatus`
const apiStatus = computed(() => {
  // [zh] 条件判断 if
  if (tab.value === 'completed') return '3';
  // [zh] 条件判断 if
  if (tab.value === 'evaluate') return '8';
  // [zh] return 返回
  return tab.value || undefined;
// [zh] 语句
});
// [zh] 声明变量 `pageNo`
const pageNo = ref(0);
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(1);
// [zh] 声明变量 `list`
const list = ref<any[]>([]);
// [zh] 声明变量 `loading`
const loading = ref(false);
// [zh] 声明变量 `loadingMore`
const loadingMore = ref(false);
// [zh] 声明变量 `loadError`
const loadError = ref('');
// [zh] 声明变量 `finished`
const finished = ref(false);
// [zh] 声明变量 `scrollRoot`
const scrollRoot = ref<HTMLElement>();
// [zh] 声明变量 `tabsRef`
const tabsRef = ref<HTMLElement>();
// [zh] 声明变量 `sentinelRef`
const sentinelRef = ref<HTMLElement>();
// [zh] 声明变量 `commentDialogRef`
const commentDialogRef = ref<InstanceType<typeof OrderCommentDialog>>();
// [zh] 声明变量 `recommentDialogRef`
const recommentDialogRef = ref<InstanceType<typeof OrderRecommentDialog>>();
// [zh] 声明变量 `commentPreviewDialogRef`
const commentPreviewDialogRef = ref<InstanceType<typeof OrderCommentPreviewDialog>>();
// [zh] 声明变量 `openSwipeId`
const openSwipeId = ref<string | null>(null);
// [zh] 声明变量 `observer`
let observer: IntersectionObserver | null = null;
// [zh] 声明变量 `tabsResizeObserver`
let tabsResizeObserver: ResizeObserver | null = null;

// [zh] 语句
const { isMobile } = useDevice();

// [zh] 声明变量 `usesWindowScroll`
const usesWindowScroll = () => isMobile.value;

// [zh] 声明变量 `scrollPageToTop`
const scrollPageToTop = () => {
  // [zh] 条件判断 if
  if (usesWindowScroll()) {
    // [zh] 语句
    window.scrollTo(0, 0);
    // [zh] 语句
    document.documentElement.scrollTop = 0;
    // [zh] 语句
    document.body.scrollTop = 0;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
// [zh] 语句
};

// [zh] 声明变量 `syncOrdersTabsInset`
const syncOrdersTabsInset = () => {
  // [zh] 语句
  document.documentElement.style.removeProperty('--orders-tabs-height');
// [zh] 语句
};

// [zh] 声明变量 `isCouponOrder`
const isCouponOrder = (order: Record<string, any>) => String(order.payScene) === '2';

// [zh] 声明变量 `displayList`
const displayList = computed(() => {
  // [zh] 条件判断 if
  if (tab.value === 'evaluate') {
    // [zh] return 返回
    return list.value.filter((o) => !isCouponOrder(o) && Number(o.commentStatus) === 0);
  // [zh] 语法符号
  }
  // [zh] return 返回
  return list.value;
// [zh] 语句
});

// [zh] 声明变量 `canDeleteOrder`
const canDeleteOrder = (order: Record<string, any>) => {
  // [zh] 声明变量 `s`
  const s = Number(order.orderStatus);
  // [zh] return 返回
  return s === 3 || s === 4 || s === 5 || s === 6;
// [zh] 语句
};

// [zh] 声明变量 `formatMoney`
const formatMoney = (val: unknown) => Number(val ?? 0).toFixed(2);
// [zh] 声明变量 `displayStatus`
const displayStatus = (order: Record<string, any>) => {
  // [zh] 条件判断 if
  if (tab.value === 'completed' && order.orderStatus === 3) return '已完成';
  // [zh] 声明变量 `text`
  const text = displayOrderStatusText(order);
  // [zh] 条件判断 if
  if (isCouponOrder(order)) {
    // [zh] 条件判断 if
    if (order.orderStatus === 3) return '已完成';
    // [zh] 条件判断 if
    if (order.orderStatus === 4) return '已取消';
  // [zh] 语法符号
  }
  // [zh] return 返回
  return text;
// [zh] 语句
};

// [zh] 声明变量 `statusClass`
const statusClass = (order: Record<string, any>) => {
  // [zh] 声明变量 `status`
  const status = order.orderStatus;
  // [zh] 条件判断 if
  if (status === 3) {
    // [zh] 条件判断 if
    if (tab.value === 'completed') return '';
    // [zh] 条件判断 if
    if (isCouponOrder(order)) return '';
    // [zh] 条件判断 if
    if (order.commentStatus === 0) return 'is-comment-pending';
    // [zh] 条件判断 if
    if (order.commentStatus === 1) return 'is-commented';
    // [zh] 条件判断 if
    if (order.commentStatus === 2) return 'is-recommented';
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (status === 0) return 'is-wait-pay';
  // [zh] 条件判断 if
  if (status === 2) return 'is-shipped';
  // [zh] 条件判断 if
  if (status === 4 || status === 5) return 'is-cancel';
  // [zh] return 返回
  return '';
// [zh] 语句
};

// [zh] 声明变量 `canComment`
const canComment = (order: Record<string, any>) =>
  // [zh] 语句
  !isCouponOrder(order) && order.orderStatus === 3 && Number(order.commentStatus) === 0;

// [zh] 声明变量 `canRecomment`
const canRecomment = (order: Record<string, any>) =>
  // [zh] 语句
  !isCouponOrder(order) && order.orderStatus === 3 && Number(order.commentStatus) === 1;

// [zh] 声明变量 `canViewComment`
const canViewComment = (order: Record<string, any>) =>
  // [zh] TypeScript/JS 语句：`!isCouponOrder(order) &&`
  !isCouponOrder(order) &&
  // [zh] TypeScript/JS 语句：`order.orderStatus === 3 &&`
  order.orderStatus === 3 &&
  // [zh] 语句
  (Number(order.commentStatus) === 1 || Number(order.commentStatus) === 2);

// [zh] 声明变量 `showLogistics`
const showLogistics = (order: Record<string, any>) =>
  // [zh] TypeScript/JS 语句：`!isCouponOrder(order) &&`
  !isCouponOrder(order) &&
  // [zh] 语句
  (order.orderStatus === 2 || order.orderStatus === 3 || order.orderStatus === 7);

// [zh] 声明变量 `canRefundItem`
const canRefundItem = (order: Record<string, any>, item: Record<string, any>) =>
  // [zh] TypeScript/JS 语句：`!isCouponOrder(order) &&`
  !isCouponOrder(order) &&
  // [zh] TypeScript/JS 语句：`(order.orderStatus === 1 || order.orderStatus === 2) &&`
  (order.orderStatus === 1 || order.orderStatus === 2) &&
  // [zh] 语句
  Number(item.orderItemStatus) === 1;

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
  onTabChange();
// [zh] 语句
};

// [zh] 声明变量 `setupObserver`
const setupObserver = () => {
  // [zh] 语句
  observer?.disconnect();
  // [zh] 条件判断 if
  if (!sentinelRef.value) return;
  // [zh] TypeScript/JS 语句：`observer = new IntersectionObserver(`
  observer = new IntersectionObserver(
    // [zh] 代码块开始
    (entries) => {
      // [zh] 条件判断 if
      if (entries.some((e) => e.isIntersecting)) loadMore();
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ root: usesWindowScroll() ? null : scrollRoot.value, r`
    { root: usesWindowScroll() ? null : scrollRoot.value, rootMargin: '100px' }
  // [zh] 语句
  );
  // [zh] 语句
  observer.observe(sentinelRef.value);
// [zh] 语句
};

// [zh] 声明变量 `pageCache`
const pageCache = usePageListCache({
  // [zh] TypeScript/JS 语句：`cacheKey: () => `/orders|tab=${tab.value}`,`
  cacheKey: () => `/orders|tab=${tab.value}`,
  // [zh] 代码块开始
  getState: () => ({
    // [zh] TypeScript/JS 语句：`tab: tab.value,`
    tab: tab.value,
    // [zh] TypeScript/JS 语句：`list: list.value,`
    list: list.value,
    // [zh] TypeScript/JS 语句：`pageNo: pageNo.value,`
    pageNo: pageNo.value,
    // [zh] TypeScript/JS 语句：`pageTotal: pageTotal.value,`
    pageTotal: pageTotal.value,
    // [zh] TypeScript/JS 语句：`finished: finished.value`
    finished: finished.value
  // [zh] TypeScript/JS 语句：`}),`
  }),
  // [zh] 代码块开始
  setState: (state) => {
    // [zh] 语句
    tab.value = String(state.tab ?? '');
    // [zh] 语句
    list.value = (state.list as any[]) || [];
    // [zh] 语句
    pageNo.value = Number(state.pageNo) || 0;
    // [zh] 语句
    pageTotal.value = Number(state.pageTotal) || 1;
    // [zh] 语句
    finished.value = !!state.finished;
    // [zh] 语句
    loading.value = false;
    // [zh] 语句
    loadingMore.value = false;
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`afterRestore: setupObserver`
  afterRestore: setupObserver
// [zh] 语句
});

// [zh] 声明变量 `loadMore`
const loadMore = async () => {
  // [zh] 条件判断 if
  if (loadingMore.value || finished.value) return;
  // [zh] 条件判断 if
  if (pageNo.value >= pageTotal.value && pageNo.value > 0) {
    // [zh] 语句
    finished.value = true;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }

  // [zh] 语句
  loadingMore.value = true;
  // [zh] 条件判断 if
  if (!list.value.length) loading.value = true;
  // [zh] 语句
  loadError.value = '';
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `next`
    const next = pageNo.value + 1;
    // [zh] 声明变量 `r`
    const r = await orderApi.loadMyOrder({
      // [zh] TypeScript/JS 语句：`pageNo: next,`
      pageNo: next,
      // [zh] TypeScript/JS 语句：`status: apiStatus.value`
      status: apiStatus.value
    // [zh] 语句
    });
    // [zh] 声明变量 `chunk`
    const chunk = r?.list || [];
    // [zh] 条件判断 if
    if (next === 1) list.value = chunk;
    // [zh] else 分支
    else list.value = list.value.concat(chunk);
    // [zh] 语句
    pageNo.value = r?.pageNo ?? next;
    // [zh] 语句
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    // [zh] 语句
    finished.value = pageNo.value >= pageTotal.value;
  // [zh] 代码块开始
  } catch (e: any) {
    // [zh] 语句
    loadError.value = e?.info || e?.message || '订单加载失败，请稍后重试';
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    loadingMore.value = false;
    // [zh] 语句
    loading.value = false;
    // [zh] 语句
    nextTick(() => setupObserver());
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `onTabChange`
const onTabChange = () => {
  // [zh] 语句
  pageCache.clear();
  // [zh] 语句
  pageNo.value = 0;
  // [zh] 语句
  pageTotal.value = 1;
  // [zh] 语句
  finished.value = false;
  // [zh] 语句
  list.value = [];
  // [zh] 语句
  scrollPageToTop();

  // [zh] 声明变量 `queryStatus`
  const queryStatus = apiStatus.value || undefined;
  // [zh] 语句
  router.replace({ path: route.path, query: queryStatus ? { status: queryStatus } : {} });
  // [zh] 语句
  loadMore();
// [zh] 语句
};

// [zh] 声明变量 `goProduct`
const goProduct = (productId: string) => {
  // [zh] 条件判断 if
  if (productId) router.push(`/product/${productId}`);
// [zh] 语句
};

// [zh] 声明变量 `goOrderDetail`
const goOrderDetail = (orderId: string) => {
  // [zh] 语句
  router.push(`/order/${orderId}`);
// [zh] 语句
};

// [zh] 声明变量 `goLogistics`
const goLogistics = (orderId: string) => {
  // [zh] 语句
  router.push(`/order/${orderId}/logistics`);
// [zh] 语句
};

// [zh] 声明变量 `cancel`
const cancel = async (id: string) => {
  // [zh] 声明变量 `ok`
  const ok = await confirmAction('取消后订单将关闭，确定要取消该订单吗？', {
    // [zh] TypeScript/JS 语句：`title: '取消订单',`
    title: '取消订单',
    // [zh] TypeScript/JS 语句：`confirmButtonText: '取消订单'`
    confirmButtonText: '取消订单'
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (!ok) return;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await orderApi.cancelOrder(id);
  // [zh] 语句
  toast.success('订单已取消');
  // [zh] 语句
  onTabChange();
// [zh] 语句
};

// [zh] 声明变量 `confirmReceive`
const confirmReceive = async (id: string) => {
  // [zh] 声明变量 `ok`
  const ok = await confirmAction('确认收货后将无法发起退款，确定已收到商品吗？', {
    // [zh] TypeScript/JS 语句：`title: '确认收货',`
    title: '确认收货',
    // [zh] TypeScript/JS 语句：`confirmButtonText: '确认收货'`
    confirmButtonText: '确认收货'
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (!ok) return;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await orderApi.confirmOrder(id);
  // [zh] 语句
  toast.success('已确认收货');
  // [zh] 语句
  onTabChange();
// [zh] 语句
};

// [zh] 声明变量 `goPay`
const goPay = (id: string) => {
  // [zh] 条件判断 if
  if (id) router.push(`/payment/${id}`);
// [zh] 语句
};

// [zh] 声明变量 `openComment`
const openComment = (orderId: string) => {
  // [zh] 语句
  commentDialogRef.value?.show(orderId);
// [zh] 语句
};

// [zh] 声明变量 `openRecomment`
const openRecomment = (orderId: string) => {
  // [zh] 语句
  recommentDialogRef.value?.show(orderId);
// [zh] 语句
};

// [zh] 声明变量 `openCommentPreview`
const openCommentPreview = (orderId: string) => {
  // [zh] 语句
  commentPreviewDialogRef.value?.show(orderId);
// [zh] 语句
};

// [zh] 声明变量 `onSwipeClose`
const onSwipeClose = (orderId: string) => {
  // [zh] 条件判断 if
  if (openSwipeId.value === orderId) openSwipeId.value = null;
// [zh] 语句
};

// [zh] 声明变量 `removeOrder`
const removeOrder = async (orderId: string) => {
  // [zh] 声明变量 `ok`
  const ok = await confirmAction('删除后订单将从列表中移除，确定删除吗？', {
    // [zh] TypeScript/JS 语句：`title: '删除订单',`
    title: '删除订单',
    // [zh] TypeScript/JS 语句：`confirmButtonText: '删除'`
    confirmButtonText: '删除'
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (!ok) return;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await orderApi.deleteOrder(orderId);
  // [zh] 语句
  list.value = list.value.filter((o) => o.orderId !== orderId);
  // [zh] 条件判断 if
  if (openSwipeId.value === orderId) openSwipeId.value = null;
  // [zh] 语句
  toast.success('订单已删除');
// [zh] 语句
};

// [zh] 代码块开始
onMounted(async () => {
  // [zh] 语句
  syncOrdersTabsInset();
  // [zh] 语句
  tabsResizeObserver = new ResizeObserver(syncOrdersTabsInset);
  // [zh] 条件判断 if
  if (tabsRef.value) tabsResizeObserver.observe(tabsRef.value);

  // [zh] 声明变量 `restored`
  const restored = await pageCache.tryRestore();
  // [zh] 条件判断 if
  if (!restored) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await loadMore();
  // [zh] 语法符号
  }
  // [zh] 语句
  setupObserver();
// [zh] 语句
});

// [zh] 代码块开始
usePageRefresh(onTabChange, {
  // [zh] TypeScript/JS 语句：`getScrollEl: () => (usesWindowScroll() ? null : scrollR`
  getScrollEl: () => (usesWindowScroll() ? null : scrollRoot.value)
// [zh] 语句
});

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 语句
  observer?.disconnect();
  // [zh] 语句
  tabsResizeObserver?.disconnect();
  // [zh] 语句
  document.documentElement.style.removeProperty('--orders-tabs-height');
// [zh] 语句
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

/* [zh] 样式规则 `@media (min-width: #{$breakpoint-mobile ` */
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
