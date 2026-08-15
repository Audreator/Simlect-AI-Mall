<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="sl-home-page ignore">
    <!-- [zh] 开始标签 `<PcSimlectHomeScreen>` -->
    <PcSimlectHomeScreen :categories="rootCategories" :hot-products="hotProducts" />

    <!-- [zh] 开始标签 `<section>` -->
    <section class="sl-home-feed">
      <!-- [zh] 开始标签 `<h2>` -->
      <h2 class="sl-feed-title">猜你喜欢</h2>
      <!-- [zh] 开始标签 `<el-skeleton>` -->
      <el-skeleton :loading="loading" animated :count="1">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #template>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sl-feed-grid">
            <!-- [zh] 开始标签 `<el-skeleton-item>` -->
            <el-skeleton-item v-for="n in 10" :key="n" variant="image" style="aspect-ratio: 1" />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>
        <div v-if="displayProducts.length" class="sl-feed-grid">
          <article
            v-for="item in displayProducts"
            :key="`${item.product.productId}-${item.displayIndex}`"
            class="sl-feed-tile"
            @click="goDetail(item.product)"
          >
            <div class="sl-feed-cover">
              <ProductImage :product="item.product" fit="contain" width="100%" height="100%" />
            </div>
            <p class="sl-feed-name">{{ item.product.productName }}</p>
            <p class="sl-feed-price"><span class="sym">¥</span>{{ formatPrice(item.product) }}</p>
          </article>
        </div>
        <el-empty v-else description="暂无商品，稍后再来看看" />
      </el-skeleton>
      <div ref="feedSentinel" class="sl-feed-sentinel">
        <span v-if="feedLoading" class="feed-tip">加载中…</span>
        <span v-else-if="feedFinished && displayProducts.length >= MAX_PRODUCTS" class="feed-tip">已展示全部推荐商品</span>
        <span v-else-if="feedFinished && displayProducts.length" class="feed-tip">没有更多了</span>
      </div>
    </section>
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/components/home/PcSimlectHomeScreen.vue`
import PcSimlectHomeScreen from '@/components/home/PcSimlectHomeScreen.vue';
// [zh] 导入 `@/api/modules`
import { productApi } from '@/api/modules';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';
// [zh] 导入 `@/utils/category`
import { normalizeCategoryTree } from '@/utils/category';
// [zh] 导入 `@/utils/product`
import { filterOnSaleProducts } from '@/utils/product';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `categories`
const categories = ref<any[]>([]);
// [zh] 声明变量 `hotProductsList`
const hotProductsList = ref<any[]>([]);
// [zh] 声明变量 `products`
const products = ref<any[]>([]);
// [zh] 声明变量 `loading`
const loading = ref(true);

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

// [zh] 声明变量 `rootCategories`
const rootCategories = computed(() => normalizeCategoryTree(categories.value));
// [zh] 声明变量 `hotProducts`
const hotProducts = computed(() => hotProductsList.value.slice(0, 10));

// [zh] 声明变量 `formatPrice`
const formatPrice = (p: Record<string, any>) => {
  // [zh] 声明变量 `val`
  const val = p.minPrice ?? p.price ?? p.salePrice;
  // [zh] return 返回
  return val != null ? Number(val).toFixed(2) : '--';
// [zh] 语句
};

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
    // [zh] 声明变量 `reachedMax`
    const reachedMax = products.value.length >= MAX_PRODUCTS;

    // [zh] 条件判断 if
    if (!hasMoreData || reachedMax) {
      // [zh] 语句
      feedFinished.value = true;
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } catch (error) {
    // [zh] 语句
    console.error('PcHomeView: loadFeed error', error);
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    feedLoading.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `load`
const load = async () => {
  // [zh] 语句
  loading.value = true;
  // [zh] 代码块开始
  try {
    // [zh] TypeScript/JS 语句：`const [cats, commend] = await Promise.all([`
    const [cats, commend] = await Promise.all([
      // [zh] TypeScript/JS 语句：`productApi.loadCategory(),`
      productApi.loadCategory(),
      // [zh] TypeScript/JS 语句：`productApi.loadCommendProduct()`
      productApi.loadCommendProduct()
    // [zh] 语句
    ]);
    // [zh] 语句
    categories.value = normalizeCategoryTree(cats || []);
    // [zh] 语句
    hotProductsList.value = filterOnSaleProducts(Array.isArray(commend) ? commend : commend?.list);
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await loadFeed(true);

    // [zh] 代码块开始
    while (!feedFinished.value && products.value.length < MAX_PRODUCTS) {
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await loadFeed();
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    loading.value = false;
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

// [zh] 声明变量 `setupInfiniteScroll`
const setupInfiniteScroll = async () => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextTick();

  // [zh] 条件判断 if
  if (!feedSentinel.value) {
    // [zh] 语句
    console.warn('PcHomeView: feedSentinel not found');
    // [zh] 语句
    return;
  // [zh] 语法符号
  }

  // [zh] TypeScript/JS 语句：`feedObserver = new IntersectionObserver(`
  feedObserver = new IntersectionObserver(
    // [zh] 代码块开始
    (entries) => {
      // [zh] for 循环
      for (const entry of entries) {
        // [zh] 条件判断 if
        if (entry.isIntersecting && !feedLoading.value && !feedFinished.value) {
          // [zh] 语句
          loadFeed();
        // [zh] 语法符号
        }
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`root: null,`
      root: null,
      // [zh] TypeScript/JS 语句：`rootMargin: '300px 0px',`
      rootMargin: '300px 0px',
      // [zh] TypeScript/JS 语句：`threshold: 0.01`
      threshold: 0.01
    // [zh] 语法符号
    }
  // [zh] 语句
  );

  // [zh] 语句
  feedObserver.observe(feedSentinel.value);
// [zh] 语句
};

// [zh] 代码块开始
onMounted(async () => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await load();
  // [zh] 语句
  setupInfiniteScroll();
// [zh] 语句
});

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 条件判断 if
  if (feedObserver) {
    // [zh] 语句
    feedObserver.disconnect();
    // [zh] 语句
    feedObserver = null;
  // [zh] 语法符号
  }
// [zh] 语句
});

// [zh] 语句
usePageRefresh(load);
</script>