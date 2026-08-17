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

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ProductCard from '@/components/business/ProductCard.vue';
import { usePageListCache } from '@/composables/usePageListCache';
import { productApi } from '@/api/modules';
import { useSearchStore } from '@/stores/search';
import { filterOnSaleProducts } from '@/utils/product';
import { toast } from '@/utils/toast';
import { usePageRefresh } from '@/composables/pullRefresh';

const router = useRouter();
const route = useRoute();
const searchStore = useSearchStore();

const resolveKeywords = () => {
  const fromQuery = typeof route.query.q === 'string' ? route.query.q.trim() : '';
  if (fromQuery) return fromQuery;
  return searchStore.payload.keyWords.trim();
};

const pageNo = ref(0);
const pageTotal = ref(1);
const total = ref(0);
const list = ref<any[]>([]);
const loading = ref(false);
const loadingMore = ref(false);
const loadError = ref('');
const finished = ref(false);
const sentinelRef = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;
const activeKeywords = ref('');

type SortMode = '' | 'price-asc' | 'price-desc' | 'sale';

const query = reactive({
  keyWords: searchStore.payload.keyWords,
  priceFrom: searchStore.payload.priceFrom,
  priceTo: searchStore.payload.priceTo,
  sortType: searchStore.payload.sortType,
  sortField: searchStore.payload.sortField
});

const resolveSortMode = (): SortMode => {
  if (query.sortField === 'price') {
    return query.sortType === 'asc' ? 'price-asc' : 'price-desc';
  }
  if (query.sortField === 'sale') return 'sale';
  return '';
};

const applySortMode = (mode: SortMode) => {
  if (mode === 'price-asc') {
    query.sortField = 'price';
    query.sortType = 'asc';
  } else if (mode === 'price-desc') {
    query.sortField = 'price';
    query.sortType = 'desc';
  } else if (mode === 'sale') {
    query.sortField = 'sale';
    query.sortType = '';
  } else {
    query.sortField = '';
    query.sortType = '';
  }
};

const sortMode = computed<SortMode>({
  get: resolveSortMode,
  set: applySortMode
});

const cacheKey = () =>
  [
    '/search-result',
    query.keyWords.trim(),
    query.priceFrom,
    query.priceTo,
    query.sortField,
    query.sortType,
    searchStore.payload.categoryId
  ].join('|');

const setupObserver = () => {
  observer?.disconnect();
  if (!sentinelRef.value) return;
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) loadMore();
    },
    { rootMargin: '120px' }
  );
  observer.observe(sentinelRef.value);
};

const pageCache = usePageListCache({
  cacheKey,
  getState: () => ({
    query: { ...query },
    list: list.value,
    pageNo: pageNo.value,
    pageTotal: pageTotal.value,
    total: total.value,
    finished: finished.value,
    activeKeywords: activeKeywords.value
  }),
  setState: (state) => {
    const q = state.query as typeof query;
    if (q) Object.assign(query, q);
    list.value = (state.list as any[]) || [];
    pageNo.value = Number(state.pageNo) || 0;
    pageTotal.value = Number(state.pageTotal) || 1;
    total.value = Number(state.total) || 0;
    finished.value = !!state.finished;
    activeKeywords.value = String(state.activeKeywords ?? '');
    loading.value = false;
    loadingMore.value = false;
  },
  afterRestore: setupObserver
});

const loadMore = async () => {
  const keyWords = query.keyWords.trim();
  if (!keyWords) return;
  if (loadingMore.value || finished.value) return;
  if (pageNo.value >= pageTotal.value && pageNo.value > 0) {
    finished.value = true;
    return;
  }

  loadingMore.value = true;
  if (!list.value.length) loading.value = true;
  activeKeywords.value = keyWords;
  loadError.value = '';
  try {
    const next = pageNo.value + 1;
    const r = await productApi.search({
      keyWords,
      pageNo: next,
      categoryId: searchStore.payload.categoryId || undefined,
      priceFrom: query.priceFrom || undefined,
      priceTo: query.priceTo || undefined,
      sortType: query.sortType || undefined,
      sortField: query.sortField || undefined
    });
    const chunk = filterOnSaleProducts(r?.list);
    if (next === 1) list.value = chunk;
    else list.value = list.value.concat(chunk);
    pageNo.value = r?.pageNo ?? next;
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    total.value = r?.totalCount ?? list.value.length;
    finished.value = pageNo.value >= pageTotal.value;
  } catch (e: any) {
    loadError.value = e?.info || e?.message || '搜索失败，请稍后重试';
  } finally {
    loadingMore.value = false;
    loading.value = false;
  }
};

const resetAndLoad = () => {
  pageNo.value = 0;
  pageTotal.value = 1;
  finished.value = false;
  list.value = [];
  total.value = 0;
  window.scrollTo(0, 0);
  loadMore();
};

const onSortChange = () => {
  if (!query.keyWords.trim()) return;
  pageCache.clear();
  searchStore.setSearch({
    keyWords: query.keyWords.trim(),
    categoryId: searchStore.payload.categoryId,
    priceFrom: query.priceFrom,
    priceTo: query.priceTo,
    sortType: query.sortField === 'price' ? query.sortType : '',
    sortField: query.sortField
  });
  resetAndLoad();
};

const onSearch = () => {
  pageCache.clear();
  const keyWords = query.keyWords.trim();
  if (!keyWords) {
    toast.warning('请输入搜索关键词');
    return;
  }
  if (query.sortField === 'price' && !query.sortType) {
    query.sortType = 'desc';
  }
  searchStore.setSearch({
    keyWords,
    categoryId: searchStore.payload.categoryId,
    priceFrom: query.priceFrom,
    priceTo: query.priceTo,
    sortType: query.sortField === 'price' ? query.sortType : '',
    sortField: query.sortField
  });
  if (route.query.q !== keyWords) {
    router.replace({ path: '/search-result', query: { q: keyWords } });
  }
  resetAndLoad();
};

const goDetail = (p: any) => router.push(`/product/${p.productId}`);

const applySearch = (keyWords: string) => {
  searchStore.setSearch({
    ...searchStore.payload,
    keyWords
  });
  query.keyWords = keyWords;
  activeKeywords.value = keyWords;
  resetAndLoad();
};

onMounted(async () => {
  const keyWords = resolveKeywords();
  if (!keyWords) {
    router.replace('/search-portal');
    return;
  }
  Object.assign(query, searchStore.payload);
  query.keyWords = keyWords;

  const restored = await pageCache.tryRestore();
  if (!restored) {
    applySearch(keyWords);
  } else {
    activeKeywords.value = query.keyWords.trim();
  }
  setupObserver();
});

watch(
  () => route.query.q,
  (q) => {
    if (typeof q !== 'string') return;
    const keyWords = q.trim();
    if (!keyWords || keyWords === query.keyWords.trim()) return;
    applySearch(keyWords);
  }
);

usePageRefresh(() => {
  pageCache.clear();
  resetAndLoad();
});

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
