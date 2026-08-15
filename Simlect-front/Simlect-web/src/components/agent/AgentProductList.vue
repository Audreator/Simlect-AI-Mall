<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="agent-products">
    <!-- [zh] 开始标签 `<RouterLink>` -->
    <RouterLink
      v-for="(item, index) in list"
      :key="item.productId || `p-${index}`"
      :to="`/product/${item.productId}`"
      class="product-link"
      @click="onProductClick(item)"
    >
      <!-- [zh] 开始标签 `<ProductImage>` -->
      <ProductImage :product="item" :width="52" :height="52" />
      <!-- [zh] 开始标签 `<p>` -->
      <p class="name">{{ item.productName }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="price">¥{{ formatPrice(item.minPrice ?? item.price) }}</p>
    <!-- [zh] 闭合标签 `</RouterLink>` -->
    </RouterLink>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-if="!list.length" class="empty">暂无相关商品</p>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue-router`
import { RouterLink } from 'vue-router';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/utils/agentProductConsult`
import { saveAgentConsultProduct } from '@/utils/agentProductConsult';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';

// [zh] 语句
defineProps<{ list: Record<string, any>[] }>();

// [zh] 声明变量 `authStore`
const authStore = useAuthStore();

// [zh] 声明变量 `onProductClick`
const onProductClick = (item: Record<string, any>) => {
  // [zh] 条件判断 if
  if (!item?.productId || !item?.productName) return;
  // [zh] TypeScript/JS 语句：`saveAgentConsultProduct(`
  saveAgentConsultProduct(
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`productId: String(item.productId),`
      productId: String(item.productId),
      // [zh] TypeScript/JS 语句：`productName: String(item.productName),`
      productName: String(item.productName),
      // [zh] TypeScript/JS 语句：`cover: item.cover ? String(item.cover) : undefined,`
      cover: item.cover ? String(item.cover) : undefined,
      // [zh] TypeScript/JS 语句：`minPrice: item.minPrice`
      minPrice: item.minPrice
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`authStore.userInfo?.userId as string | undefined`
    authStore.userInfo?.userId as string | undefined
  // [zh] 语句
  );
// [zh] 语句
};

// [zh] 声明变量 `formatPrice`
const formatPrice = (val: unknown) => Number(val ?? 0).toFixed(2);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.agent-products {` */
.agent-products {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(2, minmax(` */
  grid-template-columns: repeat(2, minmax(0, 1fr));
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `max-height: 280px;` */
  max-height: 280px;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-link {` */
.product-link {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `padding: 8px;` */
  padding: 8px;
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `text-decoration: none;` */
  text-decoration: none;
  /* [zh] 样式规则 `color: inherit;` */
  color: inherit;
  /* [zh] 样式规则 `background: #fafafa;` */
  background: #fafafa;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.4);` */
    border-color: rgba($color-primary, 0.4);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.name {` */
  .name {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
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
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.empty {` */
.empty {
  /* [zh] 样式规则 `grid-column: 1 / -1;` */
  grid-column: 1 / -1;
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}
</style>
