<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="category-products product-page-compact">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="filter-bar card-flat toolbar-row toolbar-form">
      <!-- [zh] 开始标签 `<el-input>` -->
      <el-input v-model="filter.priceFrom" size="small" placeholder="最低价" class="toolbar-form-price" clearable />
      <!-- [zh] 开始标签 `<span>` -->
      <span class="toolbar-form-sep">—</span>
      <!-- [zh] 开始标签 `<el-input>` -->
      <el-input v-model="filter.priceTo" size="small" placeholder="最高价" class="toolbar-form-price" clearable />
      <!-- [zh] 开始标签 `<el-select>` -->
      <el-select
        v-model="sortMode"
        size="small"
        placeholder="排序"
        class="toolbar-form-sort toolbar-form-sort--wide"
        teleported
        :popper-options="{ strategy: 'fixed' }"
        @change="onFilterChange"
      >
        <!-- [zh] 开始标签 `<el-option>` -->
        <el-option label="综合" value="" />
        <!-- [zh] 开始标签 `<el-option>` -->
        <el-option label="销量" value="sale" />
        <!-- [zh] 开始标签 `<el-option>` -->
        <el-option label="价格↑" value="price-asc" />
        <!-- [zh] 开始标签 `<el-option>` -->
        <el-option label="价格↓" value="price-desc" />
      <!-- [zh] 闭合标签 `</el-select>` -->
      </el-select>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button size="small" type="primary" plain class="filter-apply" @click="onFilterChange">确定</el-button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="subTabs.length" class="sub-category-bar card toolbar-row toolbar-row--chips">
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-for="tab in subTabs"
        :key="tab.key"
        type="button"
        class="toolbar-chip"
        :class="{ active: activeTabKey === tab.key }"
        @click="selectSubTab(tab)"
      >
        <!-- [zh] Mustache 插值表达式 -->
        {{ tab.label }}
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="card">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="card-section-title">
        <!-- [zh] 开始标签 `<h3>` -->
        <h3>{{ pageTitle }}</h3>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="count">在售 {{ total }} 件</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="list.length" class="product-grid product-grid--dense">
        <!-- [zh] 开始标签 `<ProductCard>` -->
        <ProductCard
          v-for="p in list"
          :key="p.productId"
          :product="p"
          compact
          @click="goDetail"
        />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<el-empty>` -->
      <el-empty v-else description="该分类暂无在售商品" />
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
import { filterOnSaleProducts } from '@/utils/product';
import { findCategoryInTree, findParentCategory, normalizeCategoryTree } from '@/utils/category';
import { usePageRefresh } from '@/composables/pullRefresh';

const route = useRoute();
const router = useRouter();

const pageNo = ref(0);
const pageTotal = ref(1);
const total = ref(0);
const list = ref<any[]>([]);
const loadingMore = ref(false);
const finished = ref(false);
const sentinelRef = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;
const categoryTree = ref<any[]>([]);
const activeTabKey = ref('');

type SortMode = '' | 'price-asc' | 'price-desc' | 'sale';

const filter = reactive({
  priceFrom: '',
  priceTo: '',
  sortField: '',
  sortType: ''
});

const routeCategoryId = computed(() => String(route.params.categoryId || ''));

const resolveSortMode = (): SortMode => {
  if (filter.sortField === 'price') {
    return filter.sortType === 'asc' ? 'price-asc' : 'price-desc';
  }
  if (filter.sortField === 'sale') return 'sale';
  return '';
};

const applySortMode = (mode: SortMode) => {
  if (mode === 'price-asc') {
    filter.sortField = 'price';
    filter.sortType = 'asc';
  } else if (mode === 'price-desc') {
    filter.sortField = 'price';
    filter.sortType = 'desc';
  } else if (mode === 'sale') {
    filter.sortField = 'sale';
    filter.sortType = '';
  } else {
    filter.sortField = '';
    filter.sortType = '';
  }
};

const sortMode = computed<SortMode>({
  get: resolveSortMode,
  set: applySortMode
});

const currentCategory = computed(() => findCategoryInTree(categoryTree.value, routeCategoryId.value));

const level1Category = computed(() => {
  const cur = currentCategory.value;
  if (!cur) return null;
  if (cur.children?.length) return cur;
  return findParentCategory(categoryTree.value, cur.categoryId);
});

const subTabs = computed(() => {
  const parent = level1Category.value;
  if (!parent?.children?.length) return [];
  return [
    { key: `all-${parent.categoryId}`, label: '全部', categoryId: parent.categoryId },
    ...parent.children.map((c: any) => ({
      key: c.categoryId,
      label: c.categoryName,
      categoryId: c.categoryId
    }))
  ];
});

const pageTitle = computed(() => {
  const tab = subTabs.value.find((t) => t.key === activeTabKey.value);
  if (tab && tab.key.startsWith('all-')) return level1Category.value?.categoryName || '分类商品';
  return tab?.label || currentCategory.value?.categoryName || '分类商品';
});

const queryCategoryId = computed(() => {
  const tab = subTabs.value.find((t) => t.key === activeTabKey.value);
  return tab?.categoryId || routeCategoryId.value;
});

const syncActiveTabFromRoute = () => {
  if (!subTabs.value.length) {
    activeTabKey.value = routeCategoryId.value;
    return;
  }
  const matched = subTabs.value.find((t) => t.categoryId === routeCategoryId.value);
  if (matched) {
    activeTabKey.value = matched.key;
    return;
  }
  const cur = currentCategory.value;
  if (cur?.children?.length) {
    activeTabKey.value = `all-${cur.categoryId}`;
    return;
  }
  activeTabKey.value = subTabs.value[0]?.key || routeCategoryId.value;
};

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
  cacheKey: () =>
    `/category/${routeCategoryId.value}|${activeTabKey.value}|${filter.priceFrom}|${filter.priceTo}|${filter.sortField}|${filter.sortType}`,
  getState: () => ({
    activeTabKey: activeTabKey.value,
    filter: { ...filter },
    list: list.value,
    pageNo: pageNo.value,
    pageTotal: pageTotal.value,
    total: total.value,
    finished: finished.value
  }),
  setState: (state) => {
    activeTabKey.value = String(state.activeTabKey ?? '');
    const f = state.filter as typeof filter;
    if (f) Object.assign(filter, f);
    list.value = (state.list as any[]) || [];
    pageNo.value = Number(state.pageNo) || 0;
    pageTotal.value = Number(state.pageTotal) || 1;
    total.value = Number(state.total) || 0;
    finished.value = !!state.finished;
    loadingMore.value = false;
  },
  afterRestore: setupObserver
});

const onFilterChange = () => {
  pageCache.clear();
  resetAndLoad();
};

const selectSubTab = (tab: { key: string; categoryId: string }) => {
  pageCache.clear();
  activeTabKey.value = tab.key;
  if (tab.categoryId !== routeCategoryId.value) {
    router.replace(`/category/${tab.categoryId}`);
  } else {
    resetAndLoad();
  }
};

const loadMore = async () => {
  if (loadingMore.value || finished.value) return;
  if (pageNo.value >= pageTotal.value && pageNo.value > 0) {
    finished.value = true;
    return;
  }
  loadingMore.value = true;
  try {
    const next = pageNo.value + 1;
    const r = await productApi.loadProduct({
      pageNo: next,
      categoryId: queryCategoryId.value,
      priceFrom: filter.priceFrom.trim() || undefined,
      priceTo: filter.priceTo.trim() || undefined,
      sortField: filter.sortField || undefined,
      sortType: filter.sortType || undefined
    });
    const chunk = filterOnSaleProducts(r?.list);
    if (next === 1) list.value = chunk;
    else list.value = list.value.concat(chunk);
    pageNo.value = r?.pageNo ?? next;
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    total.value = r?.totalCount ?? list.value.length;
    finished.value = pageNo.value >= pageTotal.value;
  } finally {
    loadingMore.value = false;
  }
};

const resetAndLoad = async () => {
  pageNo.value = 0;
  pageTotal.value = 1;
  finished.value = false;
  list.value = [];
  window.scrollTo(0, 0);
  await loadMore();
};

const goDetail = (p: any) => router.push(`/product/${p.productId}`);

const init = async () => {
  const cats = await productApi.loadCategory();
  categoryTree.value = normalizeCategoryTree(cats || []);
  syncActiveTabFromRoute();
  const restored = await pageCache.tryRestore();
  if (!restored) {
    await resetAndLoad();
  }
  setupObserver();
};

watch(routeCategoryId, async () => {
  pageCache.clear();
  syncActiveTabFromRoute();
  await resetAndLoad();
});

onMounted(init);
usePageRefresh(resetAndLoad);
onUnmounted(() => observer?.disconnect());
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.filter-bar {` */
.filter-bar {
  /* [zh] 样式规则 `flex-wrap: nowrap;` */
  flex-wrap: nowrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;
  /* [zh] 样式规则 `overflow-x: auto;` */
  overflow-x: auto;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
  -webkit-overflow-scrolling: touch;

  /* [zh] 样式规则 `:deep(.toolbar-form-price) {` */
  :deep(.toolbar-form-price) {
    /* [zh] 样式规则 `width: 72px;` */
    width: 72px;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.toolbar-form-sort--wide) {` */
  :deep(.toolbar-form-sort--wide) {
    /* [zh] 样式规则 `width: 96px;` */
    width: 96px;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.filter-apply {` */
  .filter-apply {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-category-bar {` */
.sub-category-bar {
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: nowrap;` */
  flex-wrap: nowrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `overflow-x: auto !important;` */
  overflow-x: auto !important;
  /* [zh] 样式规则 `overflow-y: hidden !important;` */
  overflow-y: hidden !important;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch !impor` */
  -webkit-overflow-scrolling: touch !important;
  /* [zh] 样式规则 `touch-action: pan-x !important;` */
  touch-action: pan-x !important;

  /* [zh] 样式规则 `.toolbar-chip {` */
  .toolbar-chip {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.count {` */
.count {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `font-weight: 400;` */
  font-weight: 400;
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
