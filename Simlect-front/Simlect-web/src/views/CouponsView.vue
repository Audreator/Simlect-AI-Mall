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

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { InfoFilled, Search } from '@element-plus/icons-vue';
import CouponCard from '@/components/business/CouponCard.vue';
import { usePageListCache } from '@/composables/usePageListCache';
import { couponApi } from '@/api/modules';
import { saveCheckoutSession, RUSHING_COUPON_PAY_AMOUNT } from '@/utils/checkout';
import { canReceiveCoupon, getCouponPlazaPhase, resolveCouponHasBought, isCouponUnlimitedStock } from '@/utils/couponPlaza';
import { confirmAction } from '@/utils/confirm';
import { toast } from '@/utils/toast';
import { usePageRefresh } from '@/composables/pullRefresh';
import { useAuthStore } from '@/stores/auth';

const tabs = [
  { label: '全部', value: 'all' },
  { label: '即将开始', value: 'upcoming' },
  { label: '进行中', value: 'ongoing' },
  { label: '已结束', value: 'ended' }
] as const;

const list = ref<any[]>([]);
const keyword = ref('');
const statusTab = ref<(typeof tabs)[number]['value']>('all');
const loading = ref(false);
const finished = ref(false);
const pageNo = ref(0);
const pageTotal = ref(1);
const router = useRouter();
const scrollRoot = ref<HTMLElement>();
const sentinelRef = ref<HTMLElement>();
const rushingCouponId = ref<string | null>(null);
let observer: IntersectionObserver | null = null;

const enrichCoupon = (item: Record<string, any>) => ({
  ...item,
  phase: getCouponPlazaPhase(item),
  hasBought: resolveCouponHasBought(item)
});

const setupObserver = () => {
  observer?.disconnect();
  if (!sentinelRef.value) return;
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) loadMore();
    },
    { root: scrollRoot.value, rootMargin: '100px' }
  );
  observer.observe(sentinelRef.value);
};

const pageCache = usePageListCache({
  cacheKey: () => `/coupons|${statusTab.value}|${keyword.value.trim()}`,
  scrollRef: scrollRoot,
  getState: () => ({
    statusTab: statusTab.value,
    keyword: keyword.value,
    list: list.value,
    pageNo: pageNo.value,
    pageTotal: pageTotal.value,
    finished: finished.value
  }),
  setState: (state) => {
    statusTab.value = state.statusTab as (typeof tabs)[number]['value'];
    keyword.value = String(state.keyword ?? '');
    list.value = (state.list as any[]) || [];
    pageNo.value = Number(state.pageNo) || 0;
    pageTotal.value = Number(state.pageTotal) || 1;
    finished.value = !!state.finished;
    loading.value = false;
  },
  afterRestore: setupObserver
});

const loadMore = async () => {
  if (loading.value || finished.value) return;
  if (pageNo.value >= pageTotal.value && pageNo.value > 0) {
    finished.value = true;
    return;
  }

  loading.value = true;
  try {
    const next = pageNo.value + 1;
    const r = await couponApi.loadDiscountCoupon({
      pageNo: next,
      pageSize: 20,
      status: statusTab.value,
      keyword: keyword.value.trim() || undefined
    });
    const chunk = (r?.list || []).map(enrichCoupon);
    if (next === 1) list.value = chunk;
    else list.value = list.value.concat(chunk);
    pageNo.value = r?.pageNo ?? next;
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    finished.value = pageNo.value >= pageTotal.value;
  } finally {
    loading.value = false;
  }
};

const resetAndLoad = () => {
  pageCache.clear();
  pageNo.value = 0;
  pageTotal.value = 1;
  finished.value = false;
  list.value = [];
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
  loadMore();
};

const switchTab = (val: (typeof tabs)[number]['value']) => {
  if (statusTab.value === val) return;
  pageCache.clear();
  statusTab.value = val;
  resetAndLoad();
};

const receive = async (c: any) => {
  if (!canReceiveCoupon(c)) {
    if (resolveCouponHasBought(c)) {
      toast.info('您已购买过该优惠券，不可重复下单');
    }
    return;
  }
  const ok = await confirmAction('确定要抢购该优惠券吗？', {
    title: '抢购确认',
    confirmButtonText: '立即抢购'
  });
  if (!ok) return;
  if (rushingCouponId.value) return;

  rushingCouponId.value = c.couponId;
  try {
    const prepared = await couponApi.rushCoupon(c.couponId);
    if (!prepared?.userCouponId) {
      return;
    }
    if (!prepared?.orderId) {
      toast.warning('下单失败，请重试');
      return;
    }
    saveCheckoutSession(
      [
        {
          productId: c.couponId,
          productName: prepared.couponName || c.couponName || '优惠券',
          productCover: c.cover,
          propertyValueIds: 'coupon_rush',
          propertyValueIdHash: 'coupon_rush',
          propertyData: [{ propertyName: '类型', propertyValue: '优惠券秒杀' }],
          price: Number(prepared.payAmount ?? RUSHING_COUPON_PAY_AMOUNT),
          buyCount: 1
        }
      ],
      2,
      {
        orderId: prepared.orderId,
        payOrderId: prepared.payOrderId,
        payExpireAt: Number(prepared.payExpireAt) || Date.now() + 60_000
      }
    );

    const idx = list.value.findIndex(item => item.couponId === c.couponId);
    if (idx !== -1) {
      list.value[idx] = {
        ...list.value[idx],
        hasBought: true,
        remainCount: isCouponUnlimitedStock(c)
          ? c.remainCount
          : Math.max(0, Number(c.remainCount ?? 1) - 1)
      };
    }
    router.push('/checkout');
  } catch {

  } finally {
    rushingCouponId.value = null;
  }
};

onMounted(async () => {

  await useAuthStore().tryRestoreSession();
  const restored = await pageCache.tryRestore();
  if (!restored) {
    await resetAndLoad();
  }
  setupObserver();
});

usePageRefresh(resetAndLoad, { getScrollEl: () => scrollRoot.value });

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
