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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { onMounted, onUnmounted, ref, nextTick } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { Ticket } from '@element-plus/icons-vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/components/business/OrderItemIdText.vue`
import OrderItemIdText from '@/components/business/OrderItemIdText.vue';
// [zh] 导入 `@/components/business/SwipeDeleteRow.vue`
import SwipeDeleteRow from '@/components/business/SwipeDeleteRow.vue';
// [zh] 导入 `@/composables/usePageListCache`
import { usePageListCache } from '@/composables/usePageListCache';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/api/modules`
import { orderApi } from '@/api/modules';
// [zh] 导入 `@/constants/backendEnums`
import { orderStatusLabel } from '@/constants/backendEnums';
// [zh] 导入 `@/utils/confirm`
import { confirmAction } from '@/utils/confirm';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';

// [zh] 声明变量 `router`
const router = useRouter();

// [zh] 声明变量 `tab`
const tab = ref('all');
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
// [zh] 声明变量 `sentinelRef`
const sentinelRef = ref<HTMLElement>();
// [zh] 声明变量 `openSwipeId`
const openSwipeId = ref<string | null>(null);
// [zh] 声明变量 `observer`
let observer: IntersectionObserver | null = null;

// [zh] 语句
const { isMobile } = useDevice();
// [zh] 声明变量 `usesWindowScroll`
const usesWindowScroll = () => isMobile.value;

// [zh] 声明变量 `isCouponOrder`
const isCouponOrder = (order: Record<string, any>) => String(order.payScene) === '2';

// [zh] 声明变量 `formatMoney`
const formatMoney = (val: unknown) => Number(val ?? 0).toFixed(2);

// [zh] 声明变量 `displayStatus`
const displayStatus = (order: Record<string, any>) =>
  // [zh] 语句
  order.orderStatusName || orderStatusLabel(order.orderStatus);

// [zh] 声明变量 `statusClass`
const statusClass = (order: Record<string, any>) => {
  // [zh] 声明变量 `status`
  const status = Number(order.orderStatus);
  // [zh] 条件判断 if
  if (status === 6) return 'is-refunded';
  // [zh] 条件判断 if
  if (status === 7) return 'is-partial-refund';
  // [zh] return 返回
  return '';
// [zh] 语句
};

// [zh] 声明变量 `canDeleteOrder`
const canDeleteOrder = (order: Record<string, any>) => Number(order.orderStatus) === 6;

// [zh] 声明变量 `showLogistics`
const showLogistics = (order: Record<string, any>) =>
  // [zh] 语句
  !isCouponOrder(order) && Number(order.orderStatus) === 7;

// [zh] 声明变量 `canRefundItem`
const canRefundItem = (order: Record<string, any>, item: Record<string, any>) =>
  // [zh] TypeScript/JS 语句：`!isCouponOrder(order) &&`
  !isCouponOrder(order) &&
  // [zh] TypeScript/JS 语句：`(Number(order.orderStatus) === 1 ||`
  (Number(order.orderStatus) === 1 ||
    // [zh] TypeScript/JS 语句：`Number(order.orderStatus) === 2 ||`
    Number(order.orderStatus) === 2 ||
    // [zh] TypeScript/JS 语句：`Number(order.orderStatus) === 7) &&`
    Number(order.orderStatus) === 7) &&
  // [zh] 语句
  Number(item.orderItemStatus) === 1;

// [zh] 声明变量 `mergeOrders`
const mergeOrders = (rows: any[]) => {
  // [zh] 声明变量 `map`
  const map = new Map<string, any>();
  // [zh] for 循环
  for (const row of rows) {
    // [zh] 条件判断 if
    if (row?.orderId) map.set(row.orderId, row);
  // [zh] 语法符号
  }
  // [zh] return 返回
  return [...map.values()].sort((a, b) => {
    // [zh] 声明变量 `ta`
    const ta = new Date(a.orderTime || 0).getTime();
    // [zh] 声明变量 `tb`
    const tb = new Date(b.orderTime || 0).getTime();
    // [zh] return 返回
    return tb - ta;
  // [zh] 语句
  });
// [zh] 语句
};

// [zh] 声明变量 `fetchPage`
const fetchPage = async (next: number, status?: number) => {
  // [zh] 条件判断 if
  if (status != null) {
    // [zh] return 返回
    return orderApi.loadMyOrder({ pageNo: next, status });
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`const [r6, r7] = await Promise.all([`
  const [r6, r7] = await Promise.all([
    // [zh] TypeScript/JS 语句：`orderApi.loadMyOrder({ pageNo: next, status: 6 }),`
    orderApi.loadMyOrder({ pageNo: next, status: 6 }),
    // [zh] TypeScript/JS 语句：`orderApi.loadMyOrder({ pageNo: next, status: 7 })`
    orderApi.loadMyOrder({ pageNo: next, status: 7 })
  // [zh] 语句
  ]);
  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`list: mergeOrders([...(r6?.list || []), ...(r7?.list ||`
    list: mergeOrders([...(r6?.list || []), ...(r7?.list || [])]),
    // [zh] TypeScript/JS 语句：`pageNo: Math.max(r6?.pageNo ?? next, r7?.pageNo ?? next`
    pageNo: Math.max(r6?.pageNo ?? next, r7?.pageNo ?? next),
    // [zh] TypeScript/JS 语句：`pageTotal: Math.max(r6?.pageTotal ?? next, r7?.pageTota`
    pageTotal: Math.max(r6?.pageTotal ?? next, r7?.pageTotal ?? next)
  // [zh] 语句
  };
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
  // [zh] TypeScript/JS 语句：`cacheKey: () => `/after-sale|tab=${tab.value}`,`
  cacheKey: () => `/after-sale|tab=${tab.value}`,
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
    tab.value = String(state.tab ?? 'all');
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
    // [zh] 声明变量 `status`
    const status = tab.value === 'all' ? undefined : Number(tab.value);
    // [zh] 声明变量 `r`
    const r = await fetchPage(next, status);
    // [zh] 声明变量 `chunk`
    const chunk = r?.list || [];
    // [zh] 条件判断 if
    if (next === 1) list.value = chunk;
    // [zh] else 分支
    else list.value = mergeOrders(list.value.concat(chunk));
    // [zh] 语句
    pageNo.value = r?.pageNo ?? next;
    // [zh] 语句
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    // [zh] 语句
    finished.value = pageNo.value >= pageTotal.value;
  // [zh] 代码块开始
  } catch (e: any) {
    // [zh] 语句
    loadError.value = e?.info || e?.message || '售后订单加载失败，请稍后重试';
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
  // [zh] 条件判断 if
  if (usesWindowScroll()) window.scrollTo(0, 0);
  // [zh] else 分支
  else if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
  // [zh] 语句
  loadMore();
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
  onTabChange();
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
  // [zh] 声明变量 `restored`
  const restored = await pageCache.tryRestore();
  // [zh] 条件判断 if
  if (!restored) await loadMore();
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
