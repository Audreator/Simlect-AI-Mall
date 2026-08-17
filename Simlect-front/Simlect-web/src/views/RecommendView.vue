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

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import ProductCard from '@/components/business/ProductCard.vue';
import { productApi } from '@/api/modules';
import { filterOnSaleProducts } from '@/utils/product';

const router = useRouter();

const pageNo = ref(0);
const pageTotal = ref(1);
const products = ref<any[]>([]);
const loading = ref(false);
const loadingMore = ref(false);
const finished = ref(false);
const sentinelRef = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;

const MAX_PRODUCTS = 60;

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

const loadMore = async () => {
  if (loadingMore.value || finished.value) return;
  if (products.value.length >= MAX_PRODUCTS) {
    finished.value = true;
    return;
  }
  if (pageNo.value >= pageTotal.value && pageNo.value > 0) {
    finished.value = true;
    return;
  }

  loadingMore.value = true;
  if (!products.value.length) loading.value = true;
  try {
    const next = pageNo.value + 1;
    const r = await productApi.loadCommendProduct();
    const list = Array.isArray(r) ? r : r?.list || [];
    const chunk = filterOnSaleProducts(list);

    if (next === 1) products.value = chunk;
    else products.value = products.value.concat(chunk);

    pageNo.value = next;
    pageTotal.value = Math.ceil(MAX_PRODUCTS / 10);

    if (products.value.length >= MAX_PRODUCTS) {
      finished.value = true;
    }
  } catch (error) {
    console.error('RecommendView: loadMore error', error);
    finished.value = true;
  } finally {
    loadingMore.value = false;
    loading.value = false;
  }
};

const goDetail = (p: any) => router.push(`/product/${p.productId}`);

onMounted(() => {
  loadMore();
  setupObserver();
});

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