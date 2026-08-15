<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="coupons-page">
    <!-- [zh] 开始标签 `<header>` -->
    <header class="page-header card-flat">
      <!-- [zh] 开始标签 `<h2>` -->
      <h2 class="page-title">优惠券秒杀</h2>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="rush-notice">
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon class="notice-icon"><InfoFilled /></el-icon>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="notice-text">
          <!-- [zh] 模板内容：`所有优惠券均为 <strong>0.01 元</strong> 抢购，每人每张<stron` -->
          所有优惠券均为 <strong>0.01 元</strong> 抢购，每人每张<strong>不可重复购买</strong>。
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="search-row">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input
          v-model="keyword"
          placeholder="搜索优惠券"
          clearable
          class="search-input"
          @keyup.enter="resetAndLoad"
        >
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template #suffix>
            <!-- [zh] 开始标签 `<el-icon>` -->
            <el-icon class="search-icon" @click="resetAndLoad"><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </header>

    <nav class="status-tabs card-flat toolbar-row">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        class="tab-btn"
        :class="{ active: statusTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </nav>

    <div ref="scrollRoot" v-loading="!!rushingCouponId" class="coupon-list-scroll">
      <CouponCard
        v-for="c in list"
        :key="c.couponId"
        :coupon="c"
        type="available"
        :receiving="rushingCouponId === c.couponId"
        @receive="receive"
      />
      <el-empty v-if="!list.length && !loading" description="暂无优惠券" class="list-empty" />
      <div ref="sentinelRef" class="load-sentinel" />
      <p v-if="loading" class="load-tip">加载中…</p>
      <p v-else-if="finished && list.length" class="load-tip">没有更多了</p>
    </div>
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { onMounted, onUnmounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { InfoFilled, Search } from '@element-plus/icons-vue';
// [zh] 导入 `@/components/business/CouponCard.vue`
import CouponCard from '@/components/business/CouponCard.vue';
// [zh] 导入 `@/composables/usePageListCache`
import { usePageListCache } from '@/composables/usePageListCache';
// [zh] 导入 `@/api/modules`
import { couponApi } from '@/api/modules';
// [zh] 导入 `@/utils/checkout`
import { saveCheckoutSession, RUSHING_COUPON_PAY_AMOUNT } from '@/utils/checkout';
// [zh] 导入 `@/utils/couponPlaza`
import { canReceiveCoupon, getCouponPlazaPhase, resolveCouponHasBought, isCouponUnlimitedStock } from '@/utils/couponPlaza';
// [zh] 导入 `@/utils/confirm`
import { confirmAction } from '@/utils/confirm';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';

// [zh] 声明变量 `tabs`
const tabs = [
  // [zh] TypeScript/JS 语句：`{ label: '全部', value: 'all' },`
  { label: '全部', value: 'all' },
  // [zh] TypeScript/JS 语句：`{ label: '即将开始', value: 'upcoming' },`
  { label: '即将开始', value: 'upcoming' },
  // [zh] TypeScript/JS 语句：`{ label: '进行中', value: 'ongoing' },`
  { label: '进行中', value: 'ongoing' },
  // [zh] TypeScript/JS 语句：`{ label: '已结束', value: 'ended' }`
  { label: '已结束', value: 'ended' }
// [zh] 语句
] as const;

// [zh] 声明变量 `list`
const list = ref<any[]>([]);
// [zh] 声明变量 `keyword`
const keyword = ref('');
// [zh] 声明变量 `statusTab`
const statusTab = ref<(typeof tabs)[number]['value']>('all');
// [zh] 声明变量 `loading`
const loading = ref(false);
// [zh] 声明变量 `finished`
const finished = ref(false);
// [zh] 声明变量 `pageNo`
const pageNo = ref(0);
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(1);
// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `scrollRoot`
const scrollRoot = ref<HTMLElement>();
// [zh] 声明变量 `sentinelRef`
const sentinelRef = ref<HTMLElement>();
// [zh] 声明变量 `rushingCouponId`
const rushingCouponId = ref<string | null>(null);
// [zh] 声明变量 `observer`
let observer: IntersectionObserver | null = null;

// [zh] 声明变量 `enrichCoupon`
const enrichCoupon = (item: Record<string, any>) => ({
  // [zh] TypeScript/JS 语句：`...item,`
  ...item,
  // [zh] TypeScript/JS 语句：`phase: getCouponPlazaPhase(item),`
  phase: getCouponPlazaPhase(item),
  // [zh] TypeScript/JS 语句：`hasBought: resolveCouponHasBought(item)`
  hasBought: resolveCouponHasBought(item)
// [zh] 语句
});

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
    // [zh] TypeScript/JS 语句：`{ root: scrollRoot.value, rootMargin: '100px' }`
    { root: scrollRoot.value, rootMargin: '100px' }
  // [zh] 语句
  );
  // [zh] 语句
  observer.observe(sentinelRef.value);
// [zh] 语句
};

// [zh] 声明变量 `pageCache`
const pageCache = usePageListCache({
  // [zh] TypeScript/JS 语句：`cacheKey: () => `/coupons|${statusTab.value}|${keyword.`
  cacheKey: () => `/coupons|${statusTab.value}|${keyword.value.trim()}`,
  // [zh] TypeScript/JS 语句：`scrollRef: scrollRoot,`
  scrollRef: scrollRoot,
  // [zh] 代码块开始
  getState: () => ({
    // [zh] TypeScript/JS 语句：`statusTab: statusTab.value,`
    statusTab: statusTab.value,
    // [zh] TypeScript/JS 语句：`keyword: keyword.value,`
    keyword: keyword.value,
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
    statusTab.value = state.statusTab as (typeof tabs)[number]['value'];
    // [zh] 语句
    keyword.value = String(state.keyword ?? '');
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
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`afterRestore: setupObserver`
  afterRestore: setupObserver
// [zh] 语句
});

// [zh] 声明变量 `loadMore`
const loadMore = async () => {
  // [zh] 条件判断 if
  if (loading.value || finished.value) return;
  // [zh] 条件判断 if
  if (pageNo.value >= pageTotal.value && pageNo.value > 0) {
    // [zh] 语句
    finished.value = true;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }

  // [zh] 语句
  loading.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `next`
    const next = pageNo.value + 1;
    // [zh] 声明变量 `r`
    const r = await couponApi.loadDiscountCoupon({
      // [zh] TypeScript/JS 语句：`pageNo: next,`
      pageNo: next,
      // [zh] TypeScript/JS 语句：`pageSize: 20,`
      pageSize: 20,
      // [zh] TypeScript/JS 语句：`status: statusTab.value,`
      status: statusTab.value,
      // [zh] TypeScript/JS 语句：`keyword: keyword.value.trim() || undefined`
      keyword: keyword.value.trim() || undefined
    // [zh] 语句
    });
    // [zh] 声明变量 `chunk`
    const chunk = (r?.list || []).map(enrichCoupon);
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
  } finally {
    // [zh] 语句
    loading.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `resetAndLoad`
const resetAndLoad = () => {
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
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
  // [zh] 语句
  loadMore();
// [zh] 语句
};

// [zh] 声明变量 `switchTab`
const switchTab = (val: (typeof tabs)[number]['value']) => {
  // [zh] 条件判断 if
  if (statusTab.value === val) return;
  // [zh] 语句
  pageCache.clear();
  // [zh] 语句
  statusTab.value = val;
  // [zh] 语句
  resetAndLoad();
// [zh] 语句
};

// [zh] 声明变量 `receive`
const receive = async (c: any) => {
  // [zh] 条件判断 if
  if (!canReceiveCoupon(c)) {
    // [zh] 条件判断 if
    if (resolveCouponHasBought(c)) {
      // [zh] 语句
      toast.info('您已购买过该优惠券，不可重复下单');
    // [zh] 语法符号
    }
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `ok`
  const ok = await confirmAction('确定要抢购该优惠券吗？', {
    // [zh] TypeScript/JS 语句：`title: '抢购确认',`
    title: '抢购确认',
    // [zh] TypeScript/JS 语句：`confirmButtonText: '立即抢购'`
    confirmButtonText: '立即抢购'
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (!ok) return;
  // [zh] 条件判断 if
  if (rushingCouponId.value) return;

  // [zh] 语句
  rushingCouponId.value = c.couponId;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `prepared`
    const prepared = await couponApi.rushCoupon(c.couponId);
    // [zh] 条件判断 if
    if (!prepared?.userCouponId) {
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (!prepared?.orderId) {
      // [zh] 语句
      toast.warning('下单失败，请重试');
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] TypeScript/JS 语句：`saveCheckoutSession(`
    saveCheckoutSession(
      // [zh] TypeScript/JS 语句：`[`
      [
        // [zh] 代码块开始
        {
          // [zh] TypeScript/JS 语句：`productId: c.couponId,`
          productId: c.couponId,
          // [zh] TypeScript/JS 语句：`productName: prepared.couponName || c.couponName || '优惠`
          productName: prepared.couponName || c.couponName || '优惠券',
          // [zh] TypeScript/JS 语句：`productCover: c.cover,`
          productCover: c.cover,
          // [zh] TypeScript/JS 语句：`propertyValueIds: 'coupon_rush',`
          propertyValueIds: 'coupon_rush',
          // [zh] TypeScript/JS 语句：`propertyValueIdHash: 'coupon_rush',`
          propertyValueIdHash: 'coupon_rush',
          // [zh] TypeScript/JS 语句：`propertyData: [{ propertyName: '类型', propertyValue: '优惠`
          propertyData: [{ propertyName: '类型', propertyValue: '优惠券秒杀' }],
          // [zh] TypeScript/JS 语句：`price: Number(prepared.payAmount ?? RUSHING_COUPON_PAY_`
          price: Number(prepared.payAmount ?? RUSHING_COUPON_PAY_AMOUNT),
          // [zh] TypeScript/JS 语句：`buyCount: 1`
          buyCount: 1
        // [zh] 语法符号
        }
      // [zh] 语法符号
      ],
      // [zh] TypeScript/JS 语句：`2,`
      2,
      // [zh] 代码块开始
      {
        // [zh] TypeScript/JS 语句：`orderId: prepared.orderId,`
        orderId: prepared.orderId,
        // [zh] TypeScript/JS 语句：`payOrderId: prepared.payOrderId,`
        payOrderId: prepared.payOrderId,
        // [zh] TypeScript/JS 语句：`payExpireAt: Number(prepared.payExpireAt) || Date.now()`
        payExpireAt: Number(prepared.payExpireAt) || Date.now() + 60_000
      // [zh] 语法符号
      }
    // [zh] 语句
    );

    // [zh] 声明变量 `idx`
    const idx = list.value.findIndex(item => item.couponId === c.couponId);
    // [zh] 条件判断 if
    if (idx !== -1) {
      // [zh] 代码块开始
      list.value[idx] = {
        // [zh] TypeScript/JS 语句：`...list.value[idx],`
        ...list.value[idx],
        // [zh] TypeScript/JS 语句：`hasBought: true,`
        hasBought: true,
        // [zh] TypeScript/JS 语句：`remainCount: isCouponUnlimitedStock(c)`
        remainCount: isCouponUnlimitedStock(c)
          // [zh] TypeScript/JS 语句：`? c.remainCount`
          ? c.remainCount
          // [zh] TypeScript/JS 语句：`: Math.max(0, Number(c.remainCount ?? 1) - 1)`
          : Math.max(0, Number(c.remainCount ?? 1) - 1)
      // [zh] 语句
      };
    // [zh] 语法符号
    }
    // [zh] 语句
    router.push('/checkout');
  // [zh] 代码块开始
  } catch {

  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    rushingCouponId.value = null;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 代码块开始
onMounted(async () => {

  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await useAuthStore().tryRestoreSession();
  // [zh] 声明变量 `restored`
  const restored = await pageCache.tryRestore();
  // [zh] 条件判断 if
  if (!restored) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await resetAndLoad();
  // [zh] 语法符号
  }
  // [zh] 语句
  setupObserver();
// [zh] 语句
});

// [zh] 语句
usePageRefresh(resetAndLoad, { getScrollEl: () => scrollRoot.value });

// [zh] 语句
onUnmounted(() => observer?.disconnect());
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.coupons-page {` */
.coupons-page {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `min-height: calc(100vh - 120px);` */
  min-height: calc(100vh - 120px);
  /* [zh] 样式规则 `max-height: calc(100vh - 120px);` */
  max-height: calc(100vh - 120px);
  /* [zh] 样式规则 `background: $color-bg;` */
  background: $color-bg;
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.page-header {` */
.page-header {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `padding: 14px 12px 10px;` */
  padding: 14px 12px 10px;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
  /* [zh] 样式规则 `border-bottom: none;` */
  border-bottom: none;

  /* [zh] 样式规则 `.page-title {` */
  .page-title {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 18px;` */
    font-size: 18px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.rush-notice {` */
.rush-notice {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;
  /* [zh] 样式规则 `border-radius: $radius-btn;` */
  border-radius: $radius-btn;
  /* [zh] 样式规则 `background: rgba($color-primary, 0.08);` */
  background: rgba($color-primary, 0.08);
  /* [zh] 样式规则 `border: 1px solid rgba($color-primary, 0` */
  border: 1px solid rgba($color-primary, 0.18);

  /* [zh] 样式规则 `.notice-icon {` */
  .notice-icon {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `margin-top: 2px;` */
    margin-top: 2px;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.notice-text {` */
  .notice-text {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `line-height: 1.55;` */
    line-height: 1.55;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;

    /* [zh] 样式规则 `strong {` */
    strong {
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `color: $color-text-title;` */
      color: $color-text-title;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.search-row {` */
.search-row {
  /* [zh] 样式规则 `.search-input {` */
  .search-input {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-icon {` */
  .search-icon {
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.status-tabs {` */
.status-tabs {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
  /* [zh] 样式规则 `border-bottom: none;` */
  border-bottom: none;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;

  /* [zh] 样式规则 `.tab-btn {` */
  .tab-btn {
    /* [zh] 样式规则 `flex: 1 0 auto;` */
    flex: 1 0 auto;
    /* [zh] 样式规则 `min-width: 72px;` */
    min-width: 72px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `padding: 11px 10px;` */
    padding: 11px 10px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `position: relative;` */
    position: relative;

    /* [zh] 样式规则 `&.active {` */
    &.active {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;

      /* [zh] 样式规则 `&::after {` */
      &::after {
        /* [zh] 样式规则 `content: '';` */
        content: '';
        /* [zh] 样式规则 `position: absolute;` */
        position: absolute;
        /* [zh] 样式规则 `left: 50%;` */
        left: 50%;
        /* [zh] 样式规则 `bottom: 0;` */
        bottom: 0;
        /* [zh] 样式规则 `transform: translateX(-50%);` */
        transform: translateX(-50%);
        /* [zh] 样式规则 `width: 28px;` */
        width: 28px;
        /* [zh] 样式规则 `height: 3px;` */
        height: 3px;
        /* [zh] 样式规则 `border-radius: $radius-xs;` */
        border-radius: $radius-xs;
        /* [zh] 样式规则 `background: linear-gradient(90deg, $colo` */
        background: linear-gradient(90deg, $color-primary, $color-primary-hover);
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.coupon-list-scroll {` */
.coupon-list-scroll {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
  -webkit-overflow-scrolling: touch;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.list-empty {` */
.list-empty {
  /* [zh] 样式规则 `padding: 40px 0;` */
  padding: 40px 0;
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
/* [zh] 样式规则 `}` */
}
</style>
