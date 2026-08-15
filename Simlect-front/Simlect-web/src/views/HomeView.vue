<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="home-page ignore">

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="homeScrollRef" class="simlect-home-scroll">

      <!-- [zh] 开始标签 `<HomeFeatureCards>` -->
      <HomeFeatureCards />

      <!-- [zh] 开始标签 `<AIGuideCard>` -->
      <AIGuideCard />

      <!-- [zh] 开始标签 `<section>` -->
      <section class="simlect-menus card">
        <!-- [zh] 开始标签 `<button>` -->
        <button v-for="item in menuItems" :key="item.path" type="button" class="simlect-menu-item" @click="router.push(item.path)">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="simlect-menu-icon" :style="{ color: menuIconStyle.color }"><el-icon :size="28"><component :is="item.icon" /></el-icon></span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="simlect-menu-label">{{ item.label }}</span>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<section>` -->
      <section v-if="editorPicks.length >= 2" class="editor-picks card">
        <!-- [zh] 开始标签 `<header>` -->
        <header class="editor-head">
          <!-- [zh] 开始标签 `<div>` -->
          <div>
            <!-- [zh] 开始标签 `<h3>` -->
            <h3 class="editor-title">编辑精选</h3>
            <!-- [zh] 开始标签 `<p>` -->
            <p class="editor-sub">买手团队严选，品质之选</p>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="editor-more" @click="router.push('/recommend')">
            <!-- [zh] 模板内容：`更多 <el-icon :size="14"><ArrowRight /></el-ico` -->
            更多 <el-icon :size="14"><ArrowRight /></el-icon>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</header>` -->
        </header>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="editor-grid">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-for="item in editorPicks"
            :key="item.productId"
            type="button"
            class="editor-item"
            @click="goDetail(item)"
          >
            <!-- [zh] 开始标签 `<div>` -->
            <div class="editor-img-wrap">
              <!-- [zh] 开始标签 `<ProductImage>` -->
              <ProductImage :product="item" fit="cover" width="100%" height="100%" class="editor-img" :lazy="false" />
              <!-- [zh] 开始标签 `<span>` -->
              <span class="editor-badge">甄选</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="editor-meta">
              <!-- [zh] 开始标签 `<h4>` -->
              <h4 class="editor-name">{{ item.productName }}</h4>
              <!-- [zh] 开始标签 `<p>` -->
              <p class="editor-price">¥{{ formatPrice(item.price) }}</p>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<section>` -->
      <section id="recommend-section" class="simlect-feed">
        <!-- [zh] 开始标签 `<header>` -->
        <header class="feed-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="feed-title">为你推荐</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="feed-sub">每日上新 · 品质精选</span>
        <!-- [zh] 闭合标签 `</header>` -->
        </header>
        <!-- [zh] 开始标签 `<el-skeleton>` -->
        <el-skeleton :loading="loading" animated :count="1">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template #template>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="simlect-waterfall">
              <!-- [zh] 开始标签 `<el-skeleton-item>` -->
              <el-skeleton-item v-for="n in 6" :key="n" variant="image" style="height: 200px" />
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          </template>
          <div v-if="displayProducts.length" class="simlect-waterfall">
            <ProductCard
              v-for="item in displayProducts"
              :key="`${item.product.productId}-${item.displayIndex}`"
              :product="item.product"
              compact
              :image-lazy="item.displayIndex >= 8"
              @click="goDetail"
            />
          </div>
          <el-empty v-else description="暂无商品，稍后再来看看" />
        </el-skeleton>
        <div ref="feedSentinel" class="feed-sentinel">
          <span v-if="feedLoading" class="feed-tip">加载中…</span>
          <p v-else-if="feedLoadError" class="feed-error">
            <button type="button" class="feed-retry" @click="retryFeed">加载失败，点击重试</button>
          </p>
          <span v-else-if="feedFinished && displayProducts.length >= MAX_PRODUCTS" class="feed-tip">已展示全部推荐商品</span>
          <span v-else-if="feedFinished && displayProducts.length" class="feed-tip">没有更多了</span>
        </div>
      </section>
    </div>
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`ArrowRight,`
  ArrowRight,
  // [zh] TypeScript/JS 语句：`ChatDotRound,`
  ChatDotRound,
  // [zh] TypeScript/JS 语句：`Grid,`
  Grid,
  // [zh] TypeScript/JS 语句：`List,`
  List,
  // [zh] TypeScript/JS 语句：`Present,`
  Present,
  // [zh] TypeScript/JS 语句：`Ticket`
  Ticket
// [zh] 语句
} from '@element-plus/icons-vue';
// [zh] 导入 `@/components/business/ProductCard.vue`
import ProductCard from '@/components/business/ProductCard.vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/components/business/AIGuideCard.vue`
import AIGuideCard from '@/components/business/AIGuideCard.vue';
// [zh] 导入 `@/components/business/HomeFeatureCards.vue`
import HomeFeatureCards from '@/components/business/HomeFeatureCards.vue';
// [zh] 导入 `@/api/modules`
import { productApi } from '@/api/modules';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';
// [zh] 导入 `@/utils/standalone`
import { isStandaloneDisplay } from '@/utils/standalone';
// [zh] 导入 `@/utils/product`
import { filterOnSaleProducts } from '@/utils/product';
// [zh] 导入 `@/integrations/featureRegistry`
import { isFeatureSupported } from '@/integrations/featureRegistry';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`applyScroll,`
  applyScroll,
  // [zh] TypeScript/JS 语句：`getScrollForPath,`
  getScrollForPath,
  // [zh] TypeScript/JS 语句：`restoreScrollForPath,`
  restoreScrollForPath,
  // [zh] TypeScript/JS 语句：`saveScrollForPath`
  saveScrollForPath
// [zh] 语句
} from '@/utils/scrollMemory';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`clearHomeBootstrap,`
  clearHomeBootstrap,
  // [zh] TypeScript/JS 语句：`prefetchHomeBootstrap,`
  prefetchHomeBootstrap,
  // [zh] TypeScript/JS 语句：`signalHomeSplashPaintReady,`
  signalHomeSplashPaintReady,
  // [zh] TypeScript/JS 语句：`takeHomeBootstrap,`
  takeHomeBootstrap,
  // [zh] TypeScript/JS 语句：`waitForHomeVisibleImagesInDom`
  waitForHomeVisibleImagesInDom
// [zh] 语句
} from '@/utils/homeBootstrap';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `loading`
const loading = ref(true);
// [zh] 声明变量 `feedLoadError`
const feedLoadError = ref(false);
// [zh] 声明变量 `products`
const products = ref<any[]>([]);
// [zh] 声明变量 `hotProductsList`
const hotProductsList = ref<any[]>([]);
// [zh] 声明变量 `homeScrollRef`
const homeScrollRef = ref<HTMLElement | null>(null);

// [zh] 声明变量 `menuIconStyle`
const menuIconStyle = computed(() => {
  // [zh] 声明变量 `level`
  const level = authStore.memberLevelCode;
  // [zh] 条件判断 if
  if (level >= 3) {
    // [zh] return 返回
    return { color: '#B8860B' };
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (level >= 2) {
    // [zh] return 返回
    return { color: '#757575' };
  // [zh] 语法符号
  }
  // [zh] return 返回
  return { color: '#1D1D1F' };
// [zh] 语句
});

// [zh] 声明变量 `feedSentinel`
const feedSentinel = ref<HTMLElement | null>(null);
// [zh] 声明变量 `feedPageNo`
const feedPageNo = ref(0);
// [zh] 声明变量 `feedPageTotal`
const feedPageTotal = ref(1);
// [zh] 声明变量 `feedLoading`
const feedLoading = ref(false);
// [zh] 声明变量 `feedFinished`
const feedFinished = ref(false);
// [zh] 声明变量 `MAX_PRODUCTS`
const MAX_PRODUCTS = 120;
// [zh] 声明变量 `feedObserver`
let feedObserver: IntersectionObserver | null = null;

// [zh] 声明变量 `menuItems`
const menuItems = computed(() => {

  // [zh] 声明变量 `all`
  const all = [
    // [zh] TypeScript/JS 语句：`{ label: '全部分类', icon: Grid, path: '/search', feature: `
    { label: '全部分类', icon: Grid, path: '/search', feature: 'category_tree' as const },
    // [zh] TypeScript/JS 语句：`{ label: '优惠券', icon: Ticket, path: '/coupons', feature`
    { label: '优惠券', icon: Ticket, path: '/coupons', feature: 'coupon_plaza' as const },
    // [zh] TypeScript/JS 语句：`{ label: '我的订单', icon: List, path: '/orders', feature: `
    { label: '我的订单', icon: List, path: '/orders', feature: 'order_list' as const },
    // [zh] TypeScript/JS 语句：`{ label: '签到有礼', icon: Present, path: '/sign', feature:`
    { label: '签到有礼', icon: Present, path: '/sign', feature: 'sign_in' as const },
    // [zh] TypeScript/JS 语句：`{ label: '智能客服', icon: ChatDotRound, path: '/ai-assista`
    { label: '智能客服', icon: ChatDotRound, path: '/ai-assistant', feature: 'agent_chat' as const }
  // [zh] 语句
  ];
  // [zh] return 返回
  return all.filter((m) => isFeatureSupported(m.feature));
// [zh] 语句
});

// [zh] 声明变量 `editorPicks`
const editorPicks = computed(() => hotProductsList.value.slice(0, 4));

// [zh] 声明变量 `formatPrice`
const formatPrice = (price: any): string => {
  // [zh] 声明变量 `n`
  const n = Number(price);
  // [zh] 条件判断 if
  if (isNaN(n)) return '--';
  // [zh] return 返回
  return n.toFixed(2);
// [zh] 语句
};

// [zh] 声明变量 `displayProducts`
const displayProducts = computed(() => {
  // [zh] 条件判断 if
  if (products.value.length === 0) return [];

  // [zh] 声明变量 `result`
  const result: { product: any; displayIndex: number }[] = [];
  // [zh] 声明变量 `sourceLength`
  const sourceLength = products.value.length;

  // [zh] for 循环
  for (let i = 0; i < MAX_PRODUCTS; i++) {
    // [zh] 声明变量 `sourceIndex`
    const sourceIndex = i % sourceLength;
    // [zh] 语句
    result.push({ product: products.value[sourceIndex], displayIndex: i });
  // [zh] 语法符号
  }

  // [zh] return 返回
  return result;
// [zh] 语句
});

// [zh] 声明变量 `loadFeed`
const loadFeed = async (reset = false) => {
  // [zh] 条件判断 if
  if (feedLoading.value) return;

  // [zh] 条件判断 if
  if (reset) {
    // [zh] 语句
    feedPageNo.value = 0;
    // [zh] 语句
    feedPageTotal.value = 1;
    // [zh] 语句
    feedFinished.value = false;
    // [zh] 语句
    products.value = [];
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (feedFinished.value) return;

  // [zh] 语句
  feedLoading.value = true;
  // [zh] 语句
  feedLoadError.value = false;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `next`
    const next = feedPageNo.value + 1;
    // [zh] 声明变量 `page`
    const page = await productApi.loadProduct({ pageNo: next });
    // [zh] 声明变量 `chunk`
    const chunk = filterOnSaleProducts(page?.list);

    // [zh] 条件判断 if
    if (chunk.length > 0) {
      // [zh] 声明变量 `existingIds`
      const existingIds = new Set(products.value.map(p => p.productId));
      // [zh] 声明变量 `filtered`
      const filtered = chunk.filter(p => !existingIds.has(p.productId));
      // [zh] 语句
      products.value = products.value.concat(filtered);
    // [zh] 语法符号
    }

    // [zh] 语句
    feedPageNo.value = Number(page?.pageNo) || next;
    // [zh] 语句
    feedPageTotal.value = Number(page?.pageTotal) || feedPageNo.value;

    // [zh] 声明变量 `hasMoreData`
    const hasMoreData = chunk.length > 0 && feedPageNo.value < feedPageTotal.value;

    // [zh] 条件判断 if
    if (!hasMoreData) {
      // [zh] 语句
      feedFinished.value = true;
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } catch (error) {
    // [zh] 语句
    console.error('HomeView: loadFeed error', error);
    // [zh] 语句
    feedLoadError.value = true;
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    feedLoading.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `retryFeed`
const retryFeed = () => {
  // [zh] 语句
  void loadFeed(products.value.length === 0);
// [zh] 语句
};

// [zh] 声明变量 `resetHomeScroll`
const resetHomeScroll = () => {
  // [zh] 语句
  saveScrollForPath('/', { top: 0, target: 'window' });
  // [zh] 语句
  applyScroll({ top: 0, target: 'window' });
  // [zh] 条件判断 if
  if (homeScrollRef.value) homeScrollRef.value.scrollTop = 0;
// [zh] 语句
};

// [zh] 声明变量 `load`
const load = async (opts?: { prefetch?: boolean; fromPullRefresh?: boolean }) => {
  // [zh] 语句
  loading.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `commend`
    const commend = await productApi.loadCommendProduct();
    // [zh] 语句
    hotProductsList.value = filterOnSaleProducts(Array.isArray(commend) ? commend : commend?.list);
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await loadFeed(true);

    // [zh] 条件判断 if
    if (opts?.prefetch) {
      // [zh] 代码块开始
      while (!feedFinished.value && products.value.length < MAX_PRODUCTS) {
        // [zh] await 等待 Promise（类比 CompletableFuture.get）
        await loadFeed();
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    loading.value = false;
    // [zh] 条件判断 if
    if (opts?.fromPullRefresh) {
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await nextTick();
      // [zh] 语句
      resetHomeScroll();
      // [zh] 语句
      requestAnimationFrame(resetHomeScroll);
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `goDetail`
const goDetail = (p: any) => {
  // [zh] 条件判断 if
  if (p?.productId) router.push(`/product/${p.productId}`);
// [zh] 语句
};

// [zh] 代码块开始
onMounted(async () => {
  // [zh] 声明变量 `savedScroll`
  const savedScroll = getScrollForPath('/');
  // [zh] 声明变量 `shouldPrefetch`
  const shouldPrefetch = !savedScroll || savedScroll.top > 0;
  // [zh] 声明变量 `bootstrap`
  let bootstrap = takeHomeBootstrap();
  // [zh] 条件判断 if
  if (!bootstrap) {
    // [zh] 代码块开始
    try {
      // [zh] 语句
      bootstrap = await prefetchHomeBootstrap(shouldPrefetch);
    // [zh] 代码块开始
    } catch {
      // [zh] 语句
      bootstrap = null;
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (bootstrap) {
    // [zh] 语句
    hotProductsList.value = bootstrap.hotProducts;
    // [zh] 语句
    products.value = bootstrap.products;
    // [zh] 语句
    feedPageNo.value = bootstrap.feedPageNo;
    // [zh] 语句
    feedPageTotal.value = bootstrap.feedPageTotal;
    // [zh] 语句
    feedFinished.value = bootstrap.feedFinished;
    // [zh] 语句
    loading.value = false;
    // [zh] 条件判断 if
    if (shouldPrefetch && !bootstrap.feedFinished) {
      // [zh] 代码块开始
      void (async () => {
        // [zh] 代码块开始
        while (!feedFinished.value && products.value.length < MAX_PRODUCTS) {
          // [zh] await 等待 Promise（类比 CompletableFuture.get）
          await loadFeed();
        // [zh] 语法符号
        }
      // [zh] 语句
      })();
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } else {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await Promise.all([
      // [zh] TypeScript/JS 语句：`authStore.loadMemberLevel(),`
      authStore.loadMemberLevel(),
      // [zh] TypeScript/JS 语句：`load({ prefetch: shouldPrefetch })`
      load({ prefetch: shouldPrefetch })
    // [zh] 语句
    ]);
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (savedScroll && savedScroll.top > 0) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await nextTick();
    // [zh] 语句
    restoreScrollForPath('/');
  // [zh] 语法符号
  }

  // [zh] TypeScript/JS 语句：`feedObserver = new IntersectionObserver(`
  feedObserver = new IntersectionObserver(
    // [zh] 代码块开始
    (entries) => {
      // [zh] 条件判断 if
      if (entries.some((e) => e.isIntersecting)) loadFeed();
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ rootMargin: '400px 0px' }`
    { rootMargin: '400px 0px' }
  // [zh] 语句
  );
  // [zh] 条件判断 if
  if (feedSentinel.value) feedObserver.observe(feedSentinel.value);

  // [zh] 条件判断 if
  if (isStandaloneDisplay()) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await waitForHomeVisibleImagesInDom(homeScrollRef.value);
    // [zh] 语句
    signalHomeSplashPaintReady();
  // [zh] 语法符号
  }
// [zh] 语句
});

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 语句
  feedObserver?.disconnect();
  // [zh] 语句
  feedObserver = null;
// [zh] 语句
});

// [zh] 代码块开始
usePageRefresh(() => {
  // [zh] 语句
  clearHomeBootstrap();
  // [zh] return 返回
  return load({ prefetch: false, fromPullRefresh: true });
// [zh] 语句
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.home-page.ignore {` */
.home-page.ignore {
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-home-placeholder {` */
.pc-home-placeholder {
  /* [zh] 样式规则 `padding: 80px 16px;` */
  padding: 80px 16px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-home-scroll {` */
.simlect-home-scroll {
  /* [zh] 样式规则 `padding-bottom: $mobile-tab-reserved;` */
  padding-bottom: $mobile-tab-reserved;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-menus {` */
.simlect-menus {
  /* [zh] 样式规则 `margin: 10px $app-page-gutter 0;` */
  margin: 10px $app-page-gutter 0;
  /* [zh] 样式规则 `padding: 14px 8px 8px;` */
  padding: 14px 8px 8px;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(5, 1fr);` */
  grid-template-columns: repeat(5, 1fr);
  /* [zh] 样式规则 `gap: 6px 4px;` */
  gap: 6px 4px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-menu-item {` */
.simlect-menu-item {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `padding: 2px 0 6px;` */
  padding: 2px 0 6px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-menu-icon {` */
.simlect-menu-icon {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `line-height: 1;` */
  line-height: 1;
  /* [zh] 样式规则 `transition: transform 0.2s ease;` */
  transition: transform 0.2s ease;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-menu-item:hover .simlect-menu-i` */
.simlect-menu-item:hover .simlect-menu-icon {
  /* [zh] 样式规则 `transform: translateY(-2px);` */
  transform: translateY(-2px);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-menu-item:active .simlect-menu-` */
.simlect-menu-item:active .simlect-menu-icon {
  /* [zh] 样式规则 `transform: scale(0.95);` */
  transform: scale(0.95);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-menu-label {` */
.simlect-menu-label {
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `line-height: 1.2;` */
  line-height: 1.2;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-sort {` */
.simlect-sort {
  /* [zh] 样式规则 `margin: 10px $app-page-gutter 0;` */
  margin: 10px $app-page-gutter 0;
  /* [zh] 样式规则 `padding: 16px 12px;` */
  padding: 16px 12px;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-sort-scroll {` */
.simlect-sort-scroll {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
  /* [zh] 样式规则 `overflow-x: auto;` */
  overflow-x: auto;
  /* [zh] 样式规则 `scrollbar-width: none;` */
  scrollbar-width: none;

  /* [zh] 样式规则 `&::-webkit-scrollbar {` */
  &::-webkit-scrollbar {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-sort-item {` */
.simlect-sort-item {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;

  /* [zh] 样式规则 `&.active .simlect-sort-avatar {` */
  &.active .simlect-sort-avatar {
    /* [zh] 样式规则 `border-color: $color-gold;` */
    border-color: $color-gold;
    /* [zh] 样式规则 `color: $color-gold;` */
    color: $color-gold;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.active .simlect-sort-name {` */
  &.active .simlect-sort-name {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-sort-avatar {` */
.simlect-sort-avatar {
  /* [zh] 样式规则 `width: 45px;` */
  width: 45px;
  /* [zh] 样式规则 `height: 45px;` */
  height: 45px;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `border: 2px solid $color-border;` */
  border: 2px solid $color-border;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `font-size: 16px;` */
  font-size: 16px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-sort-name {` */
.simlect-sort-name {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `max-width: 56px;` */
  max-width: 56px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `text-overflow: ellipsis;` */
  text-overflow: ellipsis;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-picks {` */
.editor-picks {
  /* [zh] 样式规则 `margin: 12px $app-page-gutter 0;` */
  margin: 12px $app-page-gutter 0;
  /* [zh] 样式规则 `padding: 16px 12px 14px;` */
  padding: 16px 12px 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-head {` */
.editor-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `margin-bottom: 14px;` */
  margin-bottom: 14px;

  /* [zh] 样式规则 `.editor-title {` */
  .editor-title {
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `line-height: 1.3;` */
    line-height: 1.3;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.editor-sub {` */
  .editor-sub {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `margin: 2px 0 0;` */
    margin: 2px 0 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-more {` */
.editor-more {
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 2px;` */
  gap: 2px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-grid {` */
.editor-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(2, 1fr);` */
  grid-template-columns: repeat(2, 1fr);
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-item {` */
.editor-item {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;
  /* [zh] 样式规则 `transition: transform 0.2s ease;` */
  transition: transform 0.2s ease;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.97);` */
    transform: scale(0.97);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-img-wrap {` */
.editor-img-wrap {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `aspect-ratio: 1 / 1;` */
  aspect-ratio: 1 / 1;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-img {` */
.editor-img {
  /* [zh] 样式声明 */
  :deep(.product-image),
  /* [zh] 样式规则 `:deep(.el-image) {` */
  :deep(.el-image) {
    /* [zh] 样式规则 `width: 100% !important;` */
    width: 100% !important;
    /* [zh] 样式规则 `height: 100% !important;` */
    height: 100% !important;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-badge {` */
.editor-badge {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `top: 6px;` */
  top: 6px;
  /* [zh] 样式规则 `left: 6px;` */
  left: 6px;
  /* [zh] 样式规则 `padding: 2px 8px;` */
  padding: 2px 8px;
  /* [zh] 样式规则 `border-radius: $radius-tag;` */
  border-radius: $radius-tag;
  /* [zh] 样式规则 `background: rgba(29, 29, 31, 0.75);` */
  background: rgba(29, 29, 31, 0.75);
  /* [zh] 样式规则 `backdrop-filter: blur(8px);` */
  backdrop-filter: blur(8px);
  /* [zh] 样式规则 `color: $color-gold;` */
  color: $color-gold;
  /* [zh] 样式规则 `font-size: 10px;` */
  font-size: 10px;
  /* [zh] 样式规则 `font-weight: 700;` */
  font-weight: 700;
  /* [zh] 样式规则 `letter-spacing: 0.04em;` */
  letter-spacing: 0.04em;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-meta {` */
.editor-meta {
  /* [zh] 样式规则 `.editor-name {` */
  .editor-name {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `line-height: 1.3;` */
    line-height: 1.3;
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

  /* [zh] 样式规则 `.editor-price {` */
  .editor-price {
    /* [zh] 样式规则 `margin: 4px 0 0;` */
    margin: 4px 0 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-feed {` */
.simlect-feed {
  /* [zh] 样式规则 `margin: 14px $app-page-gutter 12px;` */
  margin: 14px $app-page-gutter 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feed-head {` */
.feed-head {
  /* [zh] 样式规则 `margin-bottom: 16px;` */
  margin-bottom: 16px;
  /* [zh] 样式规则 `padding-left: 4px;` */
  padding-left: 4px;

  /* [zh] 样式规则 `.feed-title {` */
  .feed-title {
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `line-height: 1.3;` */
    line-height: 1.3;
    /* [zh] 样式规则 `padding-right: 8px;` */
    padding-right: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.feed-sub {` */
  .feed-sub {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.simlect-waterfall {` */
.simlect-waterfall {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(2, 1fr);` */
  grid-template-columns: repeat(2, 1fr);
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feed-sentinel {` */
.feed-sentinel {
  /* [zh] 样式规则 `padding: 12px 0 16px;` */
  padding: 12px 0 16px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feed-error {` */
.feed-error {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 24px 0;` */
  padding: 24px 0;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feed-retry {` */
.feed-retry {
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: none;` */
  background: none;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `text-decoration: underline;` */
  text-decoration: underline;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feed-tip {` */
.feed-tip {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}
</style>
