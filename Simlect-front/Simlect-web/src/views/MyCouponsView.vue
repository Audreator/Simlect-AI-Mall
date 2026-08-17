<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="my-coupons-page">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="status-tabs card-flat toolbar-row toolbar-row--chips">
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        class="toolbar-chip"
        :class="{ active: statusTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        <!-- [zh] Mustache 插值表达式 -->
        {{ tab.label }}
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="scrollRoot" class="coupon-scroll">
      <!-- [zh] 开始标签 `<UserCouponCard>` -->
      <UserCouponCard v-for="c in list" :key="c.userCouponId || c.couponId" :coupon="c" />
      <!-- [zh] 开始标签 `<el-empty>` -->
      <el-empty v-if="!list.length && !loading" description="还没有优惠券">
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" round @click="router.push('/coupons')">去领取</el-button>
      <!-- [zh] 闭合标签 `</el-empty>` -->
      </el-empty>
      <!-- [zh] 开始标签 `<div>` -->
      <div ref="sentinelRef" class="load-sentinel" />
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="loading" class="load-tip">加载中…</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else-if="finished && list.length" class="load-tip">没有更多了</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import UserCouponCard from '@/components/business/UserCouponCard.vue';
import { usePageListCache } from '@/composables/usePageListCache';
import { couponApi } from '@/api/modules';
import { usePageRefresh } from '@/composables/pullRefresh';

const router = useRouter();
const scrollRoot = ref<HTMLElement>();
const sentinelRef = ref<HTMLElement>();
const list = ref<any[]>([]);
const loading = ref(false);
const finished = ref(false);
const pageNo = ref(0);
const pageTotal = ref(1);
const statusTab = ref<number | ''>('');

const tabs = [
  { label: '全部', value: '' as const },
  { label: '未使用', value: 0 },
  { label: '已使用', value: 1 },
  { label: '已过期', value: 2 }
];

let observer: IntersectionObserver | null = null;

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
  cacheKey: () => `/my-coupons|${statusTab.value}`,
  scrollRef: scrollRoot,
  getState: () => ({
    statusTab: statusTab.value,
    list: list.value,
    pageNo: pageNo.value,
    pageTotal: pageTotal.value,
    finished: finished.value
  }),
  setState: (state) => {
    statusTab.value = state.statusTab as number | '';
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
    const params: Record<string, unknown> = { pageNo: next };
    if (statusTab.value !== '') params.status = statusTab.value;
    const r = await couponApi.loadUserCoupon(params);
    const chunk = r?.list || [];
    if (next === 1) list.value = chunk;
    else list.value = list.value.concat(chunk);
    pageNo.value = r?.pageNo ?? next;
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    finished.value = pageNo.value >= pageTotal.value;
  } finally {
    loading.value = false;
  }
};

const resetAndLoad = async () => {
  pageNo.value = 0;
  pageTotal.value = 1;
  finished.value = false;
  list.value = [];
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
  await loadMore();
};

const switchTab = (val: number | '') => {
  if (statusTab.value === val) return;
  pageCache.clear();
  statusTab.value = val;
  resetAndLoad();
};

onMounted(async () => {
  const restored = await pageCache.tryRestore();
  if (!restored) {
    await resetAndLoad();
  }
  setupObserver();
});

usePageRefresh(async () => {
  pageCache.clear();
  await resetAndLoad();
}, { getScrollEl: () => scrollRoot.value });

onUnmounted(() => observer?.disconnect());
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.my-coupons-page {` */
.my-coupons-page {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `min-height: calc(100vh - 120px);` */
  min-height: calc(100vh - 120px);
  /* [zh] 样式规则 `max-height: calc(100vh - 120px);` */
  max-height: calc(100vh - 120px);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.status-tabs {` */
.status-tabs {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.coupon-scroll {` */
.coupon-scroll {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
  -webkit-overflow-scrolling: touch;
  /* [zh] 样式规则 `padding-bottom: 12px;` */
  padding-bottom: 12px;
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
