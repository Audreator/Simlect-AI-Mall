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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { onMounted, onUnmounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@/components/business/UserCouponCard.vue`
import UserCouponCard from '@/components/business/UserCouponCard.vue';
// [zh] 导入 `@/composables/usePageListCache`
import { usePageListCache } from '@/composables/usePageListCache';
// [zh] 导入 `@/api/modules`
import { couponApi } from '@/api/modules';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `scrollRoot`
const scrollRoot = ref<HTMLElement>();
// [zh] 声明变量 `sentinelRef`
const sentinelRef = ref<HTMLElement>();
// [zh] 声明变量 `list`
const list = ref<any[]>([]);
// [zh] 声明变量 `loading`
const loading = ref(false);
// [zh] 声明变量 `finished`
const finished = ref(false);
// [zh] 声明变量 `pageNo`
const pageNo = ref(0);
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(1);
// [zh] 声明变量 `statusTab`
const statusTab = ref<number | ''>('');

// [zh] 声明变量 `tabs`
const tabs = [
  // [zh] TypeScript/JS 语句：`{ label: '全部', value: '' as const },`
  { label: '全部', value: '' as const },
  // [zh] TypeScript/JS 语句：`{ label: '未使用', value: 0 },`
  { label: '未使用', value: 0 },
  // [zh] TypeScript/JS 语句：`{ label: '已使用', value: 1 },`
  { label: '已使用', value: 1 },
  // [zh] TypeScript/JS 语句：`{ label: '已过期', value: 2 }`
  { label: '已过期', value: 2 }
// [zh] 语句
];

// [zh] 声明变量 `observer`
let observer: IntersectionObserver | null = null;

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
  // [zh] TypeScript/JS 语句：`cacheKey: () => `/my-coupons|${statusTab.value}`,`
  cacheKey: () => `/my-coupons|${statusTab.value}`,
  // [zh] TypeScript/JS 语句：`scrollRef: scrollRoot,`
  scrollRef: scrollRoot,
  // [zh] 代码块开始
  getState: () => ({
    // [zh] TypeScript/JS 语句：`statusTab: statusTab.value,`
    statusTab: statusTab.value,
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
    statusTab.value = state.statusTab as number | '';
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
    // [zh] 声明变量 `params`
    const params: Record<string, unknown> = { pageNo: next };
    // [zh] 条件判断 if
    if (statusTab.value !== '') params.status = statusTab.value;
    // [zh] 声明变量 `r`
    const r = await couponApi.loadUserCoupon(params);
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
  } finally {
    // [zh] 语句
    loading.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `resetAndLoad`
const resetAndLoad = async () => {
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
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await loadMore();
// [zh] 语句
};

// [zh] 声明变量 `switchTab`
const switchTab = (val: number | '') => {
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

// [zh] 代码块开始
onMounted(async () => {
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

// [zh] 代码块开始
usePageRefresh(async () => {
  // [zh] 语句
  pageCache.clear();
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await resetAndLoad();
// [zh] 语句
}, { getScrollEl: () => scrollRoot.value });

// [zh] 语句
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
