<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-wishlist-page">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="pc-wishlist-toolbar">
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button v-if="list.length" size="small" plain @click="editing = !editing">
        <!-- [zh] Mustache 插值表达式 -->
        {{ editing ? '完成' : '管理' }}
      <!-- [zh] 闭合标签 `</el-button>` -->
      </el-button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="pc-wishlist-grid">
      <!-- [zh] 开始标签 `<article>` -->
      <article v-for="row in list" :key="row.favoriteId" class="pc-wishlist-card">
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="cover-btn" @click="goDetail(row.productId)">
          <!-- [zh] 开始标签 `<ProductImage>` -->
          <ProductImage :source="row.cover" class="cover" />
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="meta">
          <!-- [zh] 开始标签 `<p>` -->
          <p class="name">{{ row.productName }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="price">¥{{ formatMoney(row.minPrice) }}</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button
          v-if="editing"
          class="btn-remove"
          size="small"
          type="danger"
          plain
          @click="remove(row.favoriteId)"
        >
          <!-- [zh] 模板内容：`删除` -->
          删除
        <!-- [zh] 闭合标签 `</el-button>` -->
        </el-button>
      <!-- [zh] 闭合标签 `</article>` -->
      </article>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<el-empty>` -->
    <el-empty v-else-if="!loading" description="暂无收藏，去逛逛吧" />

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="sentinelRef" class="load-sentinel" />
    <!-- [zh] 开始标签 `<p>` -->
    <p v-if="loadingMore" class="load-tip">加载中…</p>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="finished && list.length" class="load-tip muted">没有更多了</p>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import ProductImage from '@/components/common/ProductImage.vue';
import { favoriteApi } from '@/api/modules';
import { confirmAction } from '@/utils/confirm';
import { toast } from '@/utils/toast';
import { usePageRefresh } from '@/composables/pullRefresh';

const router = useRouter();
const list = ref<any[]>([]);
const pageNo = ref(0);
const pageTotal = ref(1);
const loading = ref(false);
const loadingMore = ref(false);
const finished = ref(false);
const editing = ref(false);
const sentinelRef = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;

const formatMoney = (val: unknown) => Number(val ?? 0).toFixed(2);

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
  if (pageNo.value >= pageTotal.value && pageNo.value > 0) {
    finished.value = true;
    return;
  }
  loadingMore.value = true;
  if (!list.value.length) loading.value = true;
  try {
    const next = pageNo.value + 1;
    const r = await favoriteApi.loadFavorite({ pageNo: next });
    const chunk = r?.list || [];
    if (next === 1) list.value = chunk;
    else list.value = list.value.concat(chunk);
    pageNo.value = r?.pageNo ?? next;
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    finished.value = pageNo.value >= pageTotal.value;
  } finally {
    loadingMore.value = false;
    loading.value = false;
  }
};

const goDetail = (productId: string) => {
  if (productId) router.push(`/product/${productId}`);
};

const remove = async (favoriteId: string) => {
  const ok = await confirmAction('确定要删除该收藏吗？', { title: '删除收藏', confirmButtonText: '删除' });
  if (!ok) return;
  await favoriteApi.removeFavorite(favoriteId);
  toast.success('已删除');
  list.value = [];
  pageNo.value = 0;
  pageTotal.value = 1;
  finished.value = false;
  await loadMore();
};

const reloadFromStart = async () => {
  list.value = [];
  pageNo.value = 0;
  pageTotal.value = 1;
  finished.value = false;
  await loadMore();
};

onMounted(async () => {
  await loadMore();
  setupObserver();
});

usePageRefresh(reloadFromStart);
onUnmounted(() => observer?.disconnect());
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-wishlist-toolbar {` */
.pc-wishlist-toolbar {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-wishlist-grid {` */
.pc-wishlist-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(auto-fill,` */
  grid-template-columns: repeat(auto-fill, minmax($pc-product-tile-min-width, 1fr));
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `align-items: start;` */
  align-items: start;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-wishlist-card {` */
.pc-wishlist-card {
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `padding-bottom: 8px;` */
  padding-bottom: 8px;
  /* [zh] 样式规则 `transition: border-color $transition-fas` */
  transition: border-color $transition-fast, box-shadow $transition-fast, transform $transition-fast;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.35)` */
    border-color: rgba($color-primary, 0.35);
    /* [zh] 样式规则 `box-shadow: $shadow-card-hover;` */
    box-shadow: $shadow-card-hover;
    /* [zh] 样式规则 `transform: translateY(-2px);` */
    transform: translateY(-2px);

    /* [zh] 样式规则 `.cover-btn :deep(img) {` */
    .cover-btn :deep(img) {
      /* [zh] 样式规则 `transform: scale(1.04);` */
      transform: scale(1.04);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cover-btn {` */
  .cover-btn {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: #fafafa;` */
    background: #fafafa;
    /* [zh] 样式规则 `aspect-ratio: 1;` */
    aspect-ratio: 1;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;

    /* [zh] 样式规则 `.cover {` */
    .cover {
      /* [zh] 样式规则 `width: 100%;` */
      width: 100%;
      /* [zh] 样式规则 `height: 100%;` */
      height: 100%;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(img) {` */
    :deep(img) {
      /* [zh] 样式规则 `transition: transform $transition-normal` */
      transition: transform $transition-normal;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.meta {` */
  .meta {
    /* [zh] 样式规则 `padding: 8px 8px 0;` */
    padding: 8px 8px 0;

    /* [zh] 样式规则 `.name {` */
    .name {
      /* [zh] 样式规则 `margin: 0 0 4px;` */
      margin: 0 0 4px;
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `line-height: 1.35;` */
      line-height: 1.35;
      /* [zh] 样式规则 `color: $color-text-title;` */
      color: $color-text-title;
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

    /* [zh] 样式规则 `.price {` */
    .price {
      /* [zh] 样式规则 `margin: 0;` */
      margin: 0;
      /* [zh] 样式规则 `font-size: 14px;` */
      font-size: 14px;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.btn-remove {` */
  .btn-remove {
    /* [zh] 样式规则 `display: block;` */
    display: block;
    /* [zh] 样式规则 `margin: 8px auto 0;` */
    margin: 8px auto 0;
  /* [zh] 样式规则 `}` */
  }
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
