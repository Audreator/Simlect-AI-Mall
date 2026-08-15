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
// [zh] 导入 `@/utils/product`
import { filterOnSaleProducts } from '@/utils/product';
// [zh] 导入 `@/utils/category`
import { findCategoryInTree, findParentCategory, normalizeCategoryTree } from '@/utils/category';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `router`
const router = useRouter();

// [zh] 声明变量 `pageNo`
const pageNo = ref(0);
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(1);
// [zh] 声明变量 `total`
const total = ref(0);
// [zh] 声明变量 `list`
const list = ref<any[]>([]);
// [zh] 声明变量 `loadingMore`
const loadingMore = ref(false);
// [zh] 声明变量 `finished`
const finished = ref(false);
// [zh] 声明变量 `sentinelRef`
const sentinelRef = ref<HTMLElement | null>(null);
// [zh] 声明变量 `observer`
let observer: IntersectionObserver | null = null;
// [zh] 声明变量 `categoryTree`
const categoryTree = ref<any[]>([]);
// [zh] 声明变量 `activeTabKey`
const activeTabKey = ref('');

// [zh] TypeScript 类型 `SortMode`（类比 Java interface/DTO）
type SortMode = '' | 'price-asc' | 'price-desc' | 'sale';

// [zh] 声明变量 `filter`
const filter = reactive({
  // [zh] TypeScript/JS 语句：`priceFrom: '',`
  priceFrom: '',
  // [zh] TypeScript/JS 语句：`priceTo: '',`
  priceTo: '',
  // [zh] TypeScript/JS 语句：`sortField: '',`
  sortField: '',
  // [zh] TypeScript/JS 语句：`sortType: ''`
  sortType: ''
// [zh] 语句
});

// [zh] 声明变量 `routeCategoryId`
const routeCategoryId = computed(() => String(route.params.categoryId || ''));

// [zh] 声明变量 `resolveSortMode`
const resolveSortMode = (): SortMode => {
  // [zh] 条件判断 if
  if (filter.sortField === 'price') {
    // [zh] return 返回
    return filter.sortType === 'asc' ? 'price-asc' : 'price-desc';
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (filter.sortField === 'sale') return 'sale';
  // [zh] return 返回
  return '';
// [zh] 语句
};

// [zh] 声明变量 `applySortMode`
const applySortMode = (mode: SortMode) => {
  // [zh] 条件判断 if
  if (mode === 'price-asc') {
    // [zh] 语句
    filter.sortField = 'price';
    // [zh] 语句
    filter.sortType = 'asc';
  // [zh] 代码块开始
  } else if (mode === 'price-desc') {
    // [zh] 语句
    filter.sortField = 'price';
    // [zh] 语句
    filter.sortType = 'desc';
  // [zh] 代码块开始
  } else if (mode === 'sale') {
    // [zh] 语句
    filter.sortField = 'sale';
    // [zh] 语句
    filter.sortType = '';
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    filter.sortField = '';
    // [zh] 语句
    filter.sortType = '';
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

// [zh] 声明变量 `currentCategory`
const currentCategory = computed(() => findCategoryInTree(categoryTree.value, routeCategoryId.value));

// [zh] 声明变量 `level1Category`
const level1Category = computed(() => {
  // [zh] 声明变量 `cur`
  const cur = currentCategory.value;
  // [zh] 条件判断 if
  if (!cur) return null;
  // [zh] 条件判断 if
  if (cur.children?.length) return cur;
  // [zh] return 返回
  return findParentCategory(categoryTree.value, cur.categoryId);
// [zh] 语句
});

// [zh] 声明变量 `subTabs`
const subTabs = computed(() => {
  // [zh] 声明变量 `parent`
  const parent = level1Category.value;
  // [zh] 条件判断 if
  if (!parent?.children?.length) return [];
  // [zh] return 返回
  return [
    // [zh] TypeScript/JS 语句：`{ key: `all-${parent.categoryId}`, label: '全部', categor`
    { key: `all-${parent.categoryId}`, label: '全部', categoryId: parent.categoryId },
    // [zh] 代码块开始
    ...parent.children.map((c: any) => ({
      // [zh] TypeScript/JS 语句：`key: c.categoryId,`
      key: c.categoryId,
      // [zh] TypeScript/JS 语句：`label: c.categoryName,`
      label: c.categoryName,
      // [zh] TypeScript/JS 语句：`categoryId: c.categoryId`
      categoryId: c.categoryId
    // [zh] TypeScript/JS 语句：`}))`
    }))
  // [zh] 语句
  ];
// [zh] 语句
});

// [zh] 声明变量 `pageTitle`
const pageTitle = computed(() => {
  // [zh] 声明变量 `tab`
  const tab = subTabs.value.find((t) => t.key === activeTabKey.value);
  // [zh] 条件判断 if
  if (tab && tab.key.startsWith('all-')) return level1Category.value?.categoryName || '分类商品';
  // [zh] return 返回
  return tab?.label || currentCategory.value?.categoryName || '分类商品';
// [zh] 语句
});

// [zh] 声明变量 `queryCategoryId`
const queryCategoryId = computed(() => {
  // [zh] 声明变量 `tab`
  const tab = subTabs.value.find((t) => t.key === activeTabKey.value);
  // [zh] return 返回
  return tab?.categoryId || routeCategoryId.value;
// [zh] 语句
});

// [zh] 声明变量 `syncActiveTabFromRoute`
const syncActiveTabFromRoute = () => {
  // [zh] 条件判断 if
  if (!subTabs.value.length) {
    // [zh] 语句
    activeTabKey.value = routeCategoryId.value;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `matched`
  const matched = subTabs.value.find((t) => t.categoryId === routeCategoryId.value);
  // [zh] 条件判断 if
  if (matched) {
    // [zh] 语句
    activeTabKey.value = matched.key;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `cur`
  const cur = currentCategory.value;
  // [zh] 条件判断 if
  if (cur?.children?.length) {
    // [zh] 语句
    activeTabKey.value = `all-${cur.categoryId}`;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  activeTabKey.value = subTabs.value[0]?.key || routeCategoryId.value;
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
  // [zh] TypeScript/JS 语句：`cacheKey: () =>`
  cacheKey: () =>
    // [zh] TypeScript/JS 语句：``/category/${routeCategoryId.value}|${activeTabKey.valu`
    `/category/${routeCategoryId.value}|${activeTabKey.value}|${filter.priceFrom}|${filter.priceTo}|${filter.sortField}|${filter.sortType}`,
  // [zh] 代码块开始
  getState: () => ({
    // [zh] TypeScript/JS 语句：`activeTabKey: activeTabKey.value,`
    activeTabKey: activeTabKey.value,
    // [zh] TypeScript/JS 语句：`filter: { ...filter },`
    filter: { ...filter },
    // [zh] TypeScript/JS 语句：`list: list.value,`
    list: list.value,
    // [zh] TypeScript/JS 语句：`pageNo: pageNo.value,`
    pageNo: pageNo.value,
    // [zh] TypeScript/JS 语句：`pageTotal: pageTotal.value,`
    pageTotal: pageTotal.value,
    // [zh] TypeScript/JS 语句：`total: total.value,`
    total: total.value,
    // [zh] TypeScript/JS 语句：`finished: finished.value`
    finished: finished.value
  // [zh] TypeScript/JS 语句：`}),`
  }),
  // [zh] 代码块开始
  setState: (state) => {
    // [zh] 语句
    activeTabKey.value = String(state.activeTabKey ?? '');
    // [zh] 声明变量 `f`
    const f = state.filter as typeof filter;
    // [zh] 条件判断 if
    if (f) Object.assign(filter, f);
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
    loadingMore.value = false;
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`afterRestore: setupObserver`
  afterRestore: setupObserver
// [zh] 语句
});

// [zh] 声明变量 `onFilterChange`
const onFilterChange = () => {
  // [zh] 语句
  pageCache.clear();
  // [zh] 语句
  resetAndLoad();
// [zh] 语句
};

// [zh] 声明变量 `selectSubTab`
const selectSubTab = (tab: { key: string; categoryId: string }) => {
  // [zh] 语句
  pageCache.clear();
  // [zh] 语句
  activeTabKey.value = tab.key;
  // [zh] 条件判断 if
  if (tab.categoryId !== routeCategoryId.value) {
    // [zh] 语句
    router.replace(`/category/${tab.categoryId}`);
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    resetAndLoad();
  // [zh] 语法符号
  }
// [zh] 语句
};

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
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `next`
    const next = pageNo.value + 1;
    // [zh] 声明变量 `r`
    const r = await productApi.loadProduct({
      // [zh] TypeScript/JS 语句：`pageNo: next,`
      pageNo: next,
      // [zh] TypeScript/JS 语句：`categoryId: queryCategoryId.value,`
      categoryId: queryCategoryId.value,
      // [zh] TypeScript/JS 语句：`priceFrom: filter.priceFrom.trim() || undefined,`
      priceFrom: filter.priceFrom.trim() || undefined,
      // [zh] TypeScript/JS 语句：`priceTo: filter.priceTo.trim() || undefined,`
      priceTo: filter.priceTo.trim() || undefined,
      // [zh] TypeScript/JS 语句：`sortField: filter.sortField || undefined,`
      sortField: filter.sortField || undefined,
      // [zh] TypeScript/JS 语句：`sortType: filter.sortType || undefined`
      sortType: filter.sortType || undefined
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
  } finally {
    // [zh] 语句
    loadingMore.value = false;
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
  // [zh] 语句
  window.scrollTo(0, 0);
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await loadMore();
// [zh] 语句
};

// [zh] 声明变量 `goDetail`
const goDetail = (p: any) => router.push(`/product/${p.productId}`);

// [zh] 声明变量 `init`
const init = async () => {
  // [zh] 声明变量 `cats`
  const cats = await productApi.loadCategory();
  // [zh] 语句
  categoryTree.value = normalizeCategoryTree(cats || []);
  // [zh] 语句
  syncActiveTabFromRoute();
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
};

// [zh] 代码块开始
watch(routeCategoryId, async () => {
  // [zh] 语句
  pageCache.clear();
  // [zh] 语句
  syncActiveTabFromRoute();
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await resetAndLoad();
// [zh] 语句
});

// [zh] 语句
onMounted(init);
// [zh] 语句
usePageRefresh(resetAndLoad);
// [zh] 语句
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
