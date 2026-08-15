<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="recommend-page">
    <!-- [zh] 开始标签 `<header>` -->
    <header class="page-header card">
      <!-- [zh] 开始标签 `<h1>` -->
      <h1 class="page-title">编辑精选</h1>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="page-subtitle">买手团队严选，品质之选</p>
    <!-- [zh] 闭合标签 `</header>` -->
    </header>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="card result-card">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="products.length" class="product-grid">
        <!-- [zh] 开始标签 `<ProductCard>` -->
        <ProductCard
          v-for="item in products"
          :key="item.productId"
          :product="item"
          compact
          @click="goDetail"
        />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-else-if="!loading" class="page-empty">
        <!-- [zh] 开始标签 `<el-empty>` -->
        <el-empty description="暂无推荐商品" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div ref="sentinelRef" class="load-sentinel" />
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="loadingMore" class="load-tip">加载中…</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else-if="finished && products.length" class="load-tip">已展示全部推荐商品</p>
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
// [zh] 导入 `@/components/business/ProductCard.vue`
import ProductCard from '@/components/business/ProductCard.vue';
// [zh] 导入 `@/api/modules`
import { productApi } from '@/api/modules';
// [zh] 导入 `@/utils/product`
import { filterOnSaleProducts } from '@/utils/product';

// [zh] 声明变量 `router`
const router = useRouter();

// [zh] 声明变量 `pageNo`
const pageNo = ref(0);
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(1);
// [zh] 声明变量 `products`
const products = ref<any[]>([]);
// [zh] 声明变量 `loading`
const loading = ref(false);
// [zh] 声明变量 `loadingMore`
const loadingMore = ref(false);
// [zh] 声明变量 `finished`
const finished = ref(false);
// [zh] 声明变量 `sentinelRef`
const sentinelRef = ref<HTMLElement | null>(null);
// [zh] 声明变量 `observer`
let observer: IntersectionObserver | null = null;

// [zh] 声明变量 `MAX_PRODUCTS`
const MAX_PRODUCTS = 60;

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

// [zh] 声明变量 `loadMore`
const loadMore = async () => {
  // [zh] 条件判断 if
  if (loadingMore.value || finished.value) return;
  // [zh] 条件判断 if
  if (products.value.length >= MAX_PRODUCTS) {
    // [zh] 语句
    finished.value = true;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
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
  if (!products.value.length) loading.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `next`
    const next = pageNo.value + 1;
    // [zh] 声明变量 `r`
    const r = await productApi.loadCommendProduct();
    // [zh] 声明变量 `list`
    const list = Array.isArray(r) ? r : r?.list || [];
    // [zh] 声明变量 `chunk`
    const chunk = filterOnSaleProducts(list);

    // [zh] 条件判断 if
    if (next === 1) products.value = chunk;
    // [zh] else 分支
    else products.value = products.value.concat(chunk);

    // [zh] 语句
    pageNo.value = next;
    // [zh] 语句
    pageTotal.value = Math.ceil(MAX_PRODUCTS / 10);

    // [zh] 条件判断 if
    if (products.value.length >= MAX_PRODUCTS) {
      // [zh] 语句
      finished.value = true;
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } catch (error) {
    // [zh] 语句
    console.error('RecommendView: loadMore error', error);
    // [zh] 语句
    finished.value = true;
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

// [zh] 声明变量 `goDetail`
const goDetail = (p: any) => router.push(`/product/${p.productId}`);

// [zh] 代码块开始
onMounted(() => {
  // [zh] 语句
  loadMore();
  // [zh] 语句
  setupObserver();
// [zh] 语句
});

// [zh] 语句
onUnmounted(() => observer?.disconnect());
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.recommend-page {` */
.recommend-page {
  /* [zh] 样式规则 `padding-bottom: $mobile-tab-reserved;` */
  padding-bottom: $mobile-tab-reserved;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.page-header {` */
.page-header {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;
  /* [zh] 样式规则 `background: linear-gradient(135deg, $col` */
  background: linear-gradient(135deg, $color-gold-soft 0%, rgba($color-gold, 0.06) 100%);
  /* [zh] 样式规则 `border: 1px solid rgba($color-gold, 0.15` */
  border: 1px solid rgba($color-gold, 0.15);

  /* [zh] 样式规则 `.page-title {` */
  .page-title {
    /* [zh] 样式规则 `margin: 0 0 4px;` */
    margin: 0 0 4px;
    /* [zh] 样式规则 `font-size: 18px;` */
    font-size: 18px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.page-subtitle {` */
  .page-subtitle {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.result-card {` */
.result-card {
  /* [zh] 样式规则 `margin-top: 12px;` */
  margin-top: 12px;
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-grid {` */
.product-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(2, 1fr);` */
  grid-template-columns: repeat(2, 1fr);
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
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

/* [zh] 样式规则 `.page-empty {` */
.page-empty {
  /* [zh] 样式规则 `padding: 40px 0;` */
  padding: 40px 0;
/* [zh] 样式规则 `}` */
}
</style>