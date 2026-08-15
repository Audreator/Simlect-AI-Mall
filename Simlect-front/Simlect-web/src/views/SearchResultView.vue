<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="search-result-page">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="card filter-card filter-sticky">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="search-row">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input
          v-model="query.keyWords"
          size="small"
          placeholder="搜索关键词"
          clearable
          class="search-keyword"
          @keyup.enter="onSearch"
        />
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button size="small" type="primary" class="search-submit" @click="onSearch">搜索</el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="filter-conditions toolbar-form toolbar-row">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input
          v-model="query.priceFrom"
          size="small"
          placeholder="最低价"
          class="toolbar-form-price"
        />
        <!-- [zh] 开始标签 `<span>` -->
        <span class="toolbar-form-sep">—</span>
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input v-model="query.priceTo" size="small" placeholder="最高价" class="toolbar-form-price" />
        <!-- [zh] 开始标签 `<el-select>` -->
        <el-select
          v-model="sortMode"
          size="small"
          placeholder="排序"
          class="toolbar-form-sort toolbar-form-sort--wide"
          clearable
          teleported
          :popper-options="{ strategy: 'fixed' }"
          @change="onSortChange"
        >
          <!-- [zh] 开始标签 `<el-option>` -->
          <el-option label="综合" value="" />
          <!-- [zh] 开始标签 `<el-option>` -->
          <el-option label="价格从低到高" value="price-asc" />
          <!-- [zh] 开始标签 `<el-option>` -->
          <el-option label="价格从高到低" value="price-desc" />
          <!-- [zh] 开始标签 `<el-option>` -->
          <el-option label="销量" value="sale" />
        <!-- [zh] 闭合标签 `</el-select>` -->
        </el-select>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="card result-card">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="card-section-title">
        <!-- [zh] 开始标签 `<h3>` -->
        <h3>「{{ activeKeywords }}」共 {{ total }} 件</h3>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="list.length" class="product-grid product-grid--dense">
        <!-- [zh] 开始标签 `<ProductCard>` -->
        <ProductCard v-for="p in list" :key="p.productId" :product="p" compact @click="goDetail" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-else-if="loadError && !loading" class="page-empty">
        <!-- [zh] 开始标签 `<el-empty>` -->
        <el-empty :description="loadError">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" @click="resetAndLoad">重试</el-button>
        <!-- [zh] 闭合标签 `</el-empty>` -->
        </el-empty>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-else-if="!loading" class="page-empty">
        <!-- [zh] 开始标签 `<el-empty>` -->
        <el-empty description="暂无搜索结果">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" @click="router.push('/')">去首页</el-button>
        <!-- [zh] 闭合标签 `</el-empty>` -->
        </el-empty>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div ref="sentinelRef" class="load-sentinel" />
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="loadingMore" class="load-tip">加载中…</p>
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
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@/components/business/ProductCard.vue`
import ProductCard from '@/components/business/ProductCard.vue';
// [zh] 导入 `@/composables/usePageListCache`
import { usePageListCache } from '@/composables/usePageListCache';
// [zh] 导入 `@/api/modules`
import { productApi } from '@/api/modules';
// [zh] 导入 `@/stores/search`
import { useSearchStore } from '@/stores/search';
// [zh] 导入 `@/utils/product`
import { filterOnSaleProducts } from '@/utils/product';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `searchStore`
const searchStore = useSearchStore();

// [zh] 声明变量 `resolveKeywords`
const resolveKeywords = () => {
  // [zh] 声明变量 `fromQuery`
  const fromQuery = typeof route.query.q === 'string' ? route.query.q.trim() : '';
  // [zh] 条件判断 if
  if (fromQuery) return fromQuery;
  // [zh] return 返回
  return searchStore.payload.keyWords.trim();
// [zh] 语句
};

// [zh] 声明变量 `pageNo`
const pageNo = ref(0);
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(1);
// [zh] 声明变量 `total`
const total = ref(0);
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
// [zh] 声明变量 `sentinelRef`
const sentinelRef = ref<HTMLElement | null>(null);
// [zh] 声明变量 `observer`
let observer: IntersectionObserver | null = null;
// [zh] 声明变量 `activeKeywords`
const activeKeywords = ref('');

// [zh] TypeScript 类型 `SortMode`（类比 Java interface/DTO）
type SortMode = '' | 'price-asc' | 'price-desc' | 'sale';

// [zh] 声明变量 `query`
const query = reactive({
  // [zh] TypeScript/JS 语句：`keyWords: searchStore.payload.keyWords,`
  keyWords: searchStore.payload.keyWords,
  // [zh] TypeScript/JS 语句：`priceFrom: searchStore.payload.priceFrom,`
  priceFrom: searchStore.payload.priceFrom,
  // [zh] TypeScript/JS 语句：`priceTo: searchStore.payload.priceTo,`
  priceTo: searchStore.payload.priceTo,
  // [zh] TypeScript/JS 语句：`sortType: searchStore.payload.sortType,`
  sortType: searchStore.payload.sortType,
  // [zh] TypeScript/JS 语句：`sortField: searchStore.payload.sortField`
  sortField: searchStore.payload.sortField
// [zh] 语句
});

// [zh] 声明变量 `resolveSortMode`
const resolveSortMode = (): SortMode => {
  // [zh] 条件判断 if
  if (query.sortField === 'price') {
    // [zh] return 返回
    return query.sortType === 'asc' ? 'price-asc' : 'price-desc';
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (query.sortField === 'sale') return 'sale';
  // [zh] return 返回
  return '';
// [zh] 语句
};

// [zh] 声明变量 `applySortMode`
const applySortMode = (mode: SortMode) => {
  // [zh] 条件判断 if
  if (mode === 'price-asc') {
    // [zh] 语句
    query.sortField = 'price';
    // [zh] 语句
    query.sortType = 'asc';
  // [zh] 代码块开始
  } else if (mode === 'price-desc') {
    // [zh] 语句
    query.sortField = 'price';
    // [zh] 语句
    query.sortType = 'desc';
  // [zh] 代码块开始
  } else if (mode === 'sale') {
    // [zh] 语句
    query.sortField = 'sale';
    // [zh] 语句
    query.sortType = '';
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    query.sortField = '';
    // [zh] 语句
    query.sortType = '';
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `sortMode`
const sortMode = computed<SortMode>({
  // [zh] TypeScript/JS 语句：`get: resolveSortMode,`
  get: resolveSortMode,
  // [zh] TypeScript/JS 语句：`set: applySortMode`
  set: applySortMode
// [zh] 语句
});

// [zh] 声明变量 `cacheKey`
const cacheKey = () =>
  // [zh] TypeScript/JS 语句：`[`
  [
    // [zh] TypeScript/JS 语句：`'/search-result',`
    '/search-result',
    // [zh] TypeScript/JS 语句：`query.keyWords.trim(),`
    query.keyWords.trim(),
    // [zh] TypeScript/JS 语句：`query.priceFrom,`
    query.priceFrom,
    // [zh] TypeScript/JS 语句：`query.priceTo,`
    query.priceTo,
    // [zh] TypeScript/JS 语句：`query.sortField,`
    query.sortField,
    // [zh] TypeScript/JS 语句：`query.sortType,`
    query.sortType,
    // [zh] TypeScript/JS 语句：`searchStore.payload.categoryId`
    searchStore.payload.categoryId
  // [zh] 语句
  ].join('|');

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
    // [zh] TypeScript/JS 语句：`{ rootMargin: '120px' }`
    { rootMargin: '120px' }
  // [zh] 语句
  );
  // [zh] 语句
  observer.observe(sentinelRef.value);
// [zh] 语句
};

// [zh] 声明变量 `pageCache`
const pageCache = usePageListCache({
  // [zh] TypeScript/JS 语句：`cacheKey,`
  cacheKey,
  // [zh] 代码块开始
  getState: () => ({
    // [zh] TypeScript/JS 语句：`query: { ...query },`
    query: { ...query },
    // [zh] TypeScript/JS 语句：`list: list.value,`
    list: list.value,
    // [zh] TypeScript/JS 语句：`pageNo: pageNo.value,`
    pageNo: pageNo.value,
    // [zh] TypeScript/JS 语句：`pageTotal: pageTotal.value,`
    pageTotal: pageTotal.value,
    // [zh] TypeScript/JS 语句：`total: total.value,`
    total: total.value,
    // [zh] TypeScript/JS 语句：`finished: finished.value,`
    finished: finished.value,
    // [zh] TypeScript/JS 语句：`activeKeywords: activeKeywords.value`
    activeKeywords: activeKeywords.value
  // [zh] TypeScript/JS 语句：`}),`
  }),
  // [zh] 代码块开始
  setState: (state) => {
    // [zh] 声明变量 `q`
    const q = state.query as typeof query;
    // [zh] 条件判断 if
    if (q) Object.assign(query, q);
    // [zh] 语句
    list.value = (state.list as any[]) || [];
    // [zh] 语句
    pageNo.value = Number(state.pageNo) || 0;
    // [zh] 语句
    pageTotal.value = Number(state.pageTotal) || 1;
    // [zh] 语句
    total.value = Number(state.total) || 0;
    // [zh] 语句
    finished.value = !!state.finished;
    // [zh] 语句
    activeKeywords.value = String(state.activeKeywords ?? '');
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
  // [zh] 声明变量 `keyWords`
  const keyWords = query.keyWords.trim();
  // [zh] 条件判断 if
  if (!keyWords) return;
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
  activeKeywords.value = keyWords;
  // [zh] 语句
  loadError.value = '';
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `next`
    const next = pageNo.value + 1;
    // [zh] 声明变量 `r`
    const r = await productApi.search({
      // [zh] TypeScript/JS 语句：`keyWords,`
      keyWords,
      // [zh] TypeScript/JS 语句：`pageNo: next,`
      pageNo: next,
      // [zh] TypeScript/JS 语句：`categoryId: searchStore.payload.categoryId || undefined`
      categoryId: searchStore.payload.categoryId || undefined,
      // [zh] TypeScript/JS 语句：`priceFrom: query.priceFrom || undefined,`
      priceFrom: query.priceFrom || undefined,
      // [zh] TypeScript/JS 语句：`priceTo: query.priceTo || undefined,`
      priceTo: query.priceTo || undefined,
      // [zh] TypeScript/JS 语句：`sortType: query.sortType || undefined,`
      sortType: query.sortType || undefined,
      // [zh] TypeScript/JS 语句：`sortField: query.sortField || undefined`
      sortField: query.sortField || undefined
    // [zh] 语句
    });
    // [zh] 声明变量 `chunk`
    const chunk = filterOnSaleProducts(r?.list);
    // [zh] 条件判断 if
    if (next === 1) list.value = chunk;
    // [zh] else 分支
    else list.value = list.value.concat(chunk);
    // [zh] 语句
    pageNo.value = r?.pageNo ?? next;
    // [zh] 语句
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    // [zh] 语句
    total.value = r?.totalCount ?? list.value.length;
    // [zh] 语句
    finished.value = pageNo.value >= pageTotal.value;
  // [zh] 代码块开始
  } catch (e: any) {
    // [zh] 语句
    loadError.value = e?.info || e?.message || '搜索失败，请稍后重试';
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    loadingMore.value = false;
    // [zh] 语句
    loading.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `resetAndLoad`
const resetAndLoad = () => {
  // [zh] 语句
  pageNo.value = 0;
  // [zh] 语句
  pageTotal.value = 1;
  // [zh] 语句
  finished.value = false;
  // [zh] 语句
  list.value = [];
  // [zh] 语句
  total.value = 0;
  // [zh] 语句
  window.scrollTo(0, 0);
  // [zh] 语句
  loadMore();
// [zh] 语句
};

// [zh] 声明变量 `onSortChange`
const onSortChange = () => {
  // [zh] 条件判断 if
  if (!query.keyWords.trim()) return;
  // [zh] 语句
  pageCache.clear();
  // [zh] 代码块开始
  searchStore.setSearch({
    // [zh] TypeScript/JS 语句：`keyWords: query.keyWords.trim(),`
    keyWords: query.keyWords.trim(),
    // [zh] TypeScript/JS 语句：`categoryId: searchStore.payload.categoryId,`
    categoryId: searchStore.payload.categoryId,
    // [zh] TypeScript/JS 语句：`priceFrom: query.priceFrom,`
    priceFrom: query.priceFrom,
    // [zh] TypeScript/JS 语句：`priceTo: query.priceTo,`
    priceTo: query.priceTo,
    // [zh] TypeScript/JS 语句：`sortType: query.sortField === 'price' ? query.sortType `
    sortType: query.sortField === 'price' ? query.sortType : '',
    // [zh] TypeScript/JS 语句：`sortField: query.sortField`
    sortField: query.sortField
  // [zh] 语句
  });
  // [zh] 语句
  resetAndLoad();
// [zh] 语句
};

// [zh] 声明变量 `onSearch`
const onSearch = () => {
  // [zh] 语句
  pageCache.clear();
  // [zh] 声明变量 `keyWords`
  const keyWords = query.keyWords.trim();
  // [zh] 条件判断 if
  if (!keyWords) {
    // [zh] 语句
    toast.warning('请输入搜索关键词');
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (query.sortField === 'price' && !query.sortType) {
    // [zh] 语句
    query.sortType = 'desc';
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  searchStore.setSearch({
    // [zh] TypeScript/JS 语句：`keyWords,`
    keyWords,
    // [zh] TypeScript/JS 语句：`categoryId: searchStore.payload.categoryId,`
    categoryId: searchStore.payload.categoryId,
    // [zh] TypeScript/JS 语句：`priceFrom: query.priceFrom,`
    priceFrom: query.priceFrom,
    // [zh] TypeScript/JS 语句：`priceTo: query.priceTo,`
    priceTo: query.priceTo,
    // [zh] TypeScript/JS 语句：`sortType: query.sortField === 'price' ? query.sortType `
    sortType: query.sortField === 'price' ? query.sortType : '',
    // [zh] TypeScript/JS 语句：`sortField: query.sortField`
    sortField: query.sortField
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (route.query.q !== keyWords) {
    // [zh] 语句
    router.replace({ path: '/search-result', query: { q: keyWords } });
  // [zh] 语法符号
  }
  // [zh] 语句
  resetAndLoad();
// [zh] 语句
};

// [zh] 声明变量 `goDetail`
const goDetail = (p: any) => router.push(`/product/${p.productId}`);

// [zh] 声明变量 `applySearch`
const applySearch = (keyWords: string) => {
  // [zh] 代码块开始
  searchStore.setSearch({
    // [zh] TypeScript/JS 语句：`...searchStore.payload,`
    ...searchStore.payload,
    // [zh] TypeScript/JS 语句：`keyWords`
    keyWords
  // [zh] 语句
  });
  // [zh] 语句
  query.keyWords = keyWords;
  // [zh] 语句
  activeKeywords.value = keyWords;
  // [zh] 语句
  resetAndLoad();
// [zh] 语句
};

// [zh] 代码块开始
onMounted(async () => {
  // [zh] 声明变量 `keyWords`
  const keyWords = resolveKeywords();
  // [zh] 条件判断 if
  if (!keyWords) {
    // [zh] 语句
    router.replace('/search-portal');
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  Object.assign(query, searchStore.payload);
  // [zh] 语句
  query.keyWords = keyWords;

  // [zh] 声明变量 `restored`
  const restored = await pageCache.tryRestore();
  // [zh] 条件判断 if
  if (!restored) {
    // [zh] 语句
    applySearch(keyWords);
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    activeKeywords.value = query.keyWords.trim();
  // [zh] 语法符号
  }
  // [zh] 语句
  setupObserver();
// [zh] 语句
});

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => route.query.q,`
  () => route.query.q,
  // [zh] 代码块开始
  (q) => {
    // [zh] 条件判断 if
    if (typeof q !== 'string') return;
    // [zh] 声明变量 `keyWords`
    const keyWords = q.trim();
    // [zh] 条件判断 if
    if (!keyWords || keyWords === query.keyWords.trim()) return;
    // [zh] 语句
    applySearch(keyWords);
  // [zh] 语法符号
  }
// [zh] 语句
);

// [zh] 代码块开始
usePageRefresh(() => {
  // [zh] 语句
  pageCache.clear();
  // [zh] 语句
  resetAndLoad();
// [zh] 语句
});

// [zh] 语句
onUnmounted(() => observer?.disconnect());
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.filter-sticky {` */
.filter-sticky {
  /* [zh] 样式规则 `position: sticky;` */
  position: sticky;
  /* [zh] 样式规则 `top: 0;` */
  top: 0;
  /* [zh] 样式规则 `z-index: 30;` */
  z-index: 30;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `box-shadow: 0 2px 10px rgba(0, 0, 0, 0.0` */
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  /* [zh] 样式规则 `overflow: visible;` */
  overflow: visible;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.search-row {` */
.search-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;

  /* [zh] 样式规则 `.search-keyword {` */
  .search-keyword {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-submit {` */
  .search-submit {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `min-width: 56px;` */
    min-width: 56px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.filter-conditions {` */
.filter-conditions {
  /* [zh] 样式规则 `padding-top: 2px;` */
  padding-top: 2px;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `overflow: visible;` */
  overflow: visible;
  /* [zh] 样式规则 `row-gap: 8px;` */
  row-gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.filter-conditions :deep(.toolbar-form-s` */
.filter-conditions :deep(.toolbar-form-sort--wide) {
  /* [zh] 样式规则 `width: 118px;` */
  width: 118px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.result-card {` */
.result-card {
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
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
