<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="category-nav-strip" aria-label="商品分类">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="category-scroll">
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-for="cat in displayCategories"
        :key="cat.categoryId"
        type="button"
        class="category-item"
        @click="goCategory(cat)"
      >
        <!-- [zh] Mustache 插值表达式 -->
        {{ cat.categoryName }}
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { productApi } from '@/api/modules';
import { normalizeCategoryTree } from '@/utils/category';

const router = useRouter();
const categories = ref<any[]>([]);

const displayCategories = computed(() => {
  const flat: { categoryId: string; categoryName: string }[] = [];
  const walk = (nodes: any[]) => {
    for (const n of nodes) {
      flat.push({ categoryId: n.categoryId, categoryName: n.categoryName });
      if (flat.length >= 12) return;
      if (n.children?.length) walk(n.children);
      if (flat.length >= 12) return;
    }
  };
  walk(categories.value);
  return flat.slice(0, 12);
});

const goCategory = (cat: { categoryId: string }) => {
  router.push(`/category/${cat.categoryId}`);
};

onMounted(async () => {
  try {
    const cats = await productApi.loadCategory();
    categories.value = normalizeCategoryTree(cats || []);
  } catch {

  }
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.category-nav-strip {` */
.category-nav-strip {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `margin: 8px 0 0;` */
  margin: 8px 0 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.category-scroll {` */
.category-scroll {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 2px;` */
  gap: 2px;
  /* [zh] 样式规则 `overflow-x: auto;` */
  overflow-x: auto;
  /* [zh] 样式规则 `scrollbar-width: none;` */
  scrollbar-width: none;
  /* [zh] 样式规则 `-ms-overflow-style: none;` */
  -ms-overflow-style: none;
  /* [zh] 样式规则 `padding: 2px 0 0;` */
  padding: 2px 0 0;

  /* [zh] 样式规则 `&::-webkit-scrollbar {` */
  &::-webkit-scrollbar {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.category-item:first-child {` */
  .category-item:first-child {
    /* [zh] 样式规则 `margin-left: $app-page-gutter;` */
    margin-left: $app-page-gutter;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&::after {` */
  &::after {
    /* [zh] 样式规则 `content: '';` */
    content: '';
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `width: $app-page-gutter;` */
    width: $app-page-gutter;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.category-item {` */
.category-item {
  /* [zh] 样式规则 `flex: 0 0 auto;` */
  flex: 0 0 auto;
  /* [zh] 样式规则 `padding: 3px 10px;` */
  padding: 3px 10px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `color: rgba(60, 60, 67, 0.82);` */
  color: rgba(60, 60, 67, 0.82);
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `line-height: 1.2;` */
  line-height: 1.2;
  /* [zh] 样式规则 `letter-spacing: -0.01em;` */
  letter-spacing: -0.01em;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `transition: background $transition-fast,` */
  transition: background $transition-fast, color $transition-fast;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.05);` */
    background: rgba(0, 0, 0, 0.05);
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
