<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
    <!-- [zh] 开始标签 `<Transition>` -->
    <Transition name="sku-sheet-fade">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="visible" class="sku-sheet-root" :class="{ 'is-desktop': isDesktop }" @click.self="close">
        <!-- [zh] 开始标签 `<Transition>` -->
        <Transition name="sku-sheet-slide">
          <!-- [zh] 开始标签 `<section>` -->
          <section v-if="visible" class="sku-sheet-panel ignore" role="dialog" aria-modal="true" aria-label="选择规格">
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="sheet-close" aria-label="关闭" @click="close">
              <!-- [zh] 开始标签 `<el-icon>` -->
              <el-icon :size="20"><Close /></el-icon>
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="sheet-drag" aria-hidden="true" />

            <!-- [zh] 开始标签 `<el-skeleton>` -->
            <el-skeleton v-if="loading" animated :rows="6" class="sheet-skeleton" />

            <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
            <template v-else-if="productInfo">
              <!-- [zh] 开始标签 `<header>` -->
              <header class="pick-header">
                <!-- [zh] 开始标签 `<ProductImage>` -->
                <ProductImage
                  :source="coverImage"
                  width="56px"
                  height="56px"
                  fit="contain"
                  class="pick-cover"
                />
                <!-- [zh] 开始标签 `<div>` -->
                <div class="pick-meta">
                  <!-- [zh] 开始标签 `<h2>` -->
                  <h2 class="pick-name">{{ productInfo.productName }}</h2>
                  <!-- [zh] 开始标签 `<p>` -->
                  <p class="pick-price">
                    <!-- [zh] 开始标签 `<span>` -->
                    <span class="sym">¥</span>{{ displayPrice }}
                  <!-- [zh] 闭合标签 `</p>` -->
                  </p>
                  <!-- [zh] 开始标签 `<p>` -->
                  <p class="pick-stock">
                    <!-- [zh] 模板内容：`库存 {{ selectedSku?.stock ?? '--' }}` -->
                    库存 {{ selectedSku?.stock ?? '--' }}
                    <!-- [zh] 开始标签 `<em>` -->
                    <em v-if="selectedSku?.stock != null && selectedSku.stock <= 5">紧张</em>
                  <!-- [zh] 闭合标签 `</p>` -->
                  </p>
                <!-- [zh] 闭合标签 `</div>` -->
                </div>
              <!-- [zh] 闭合标签 `</header>` -->
              </header>

              <!-- [zh] 开始标签 `<div>` -->
              <div class="pick-body">
                <!-- [zh] 开始标签 `<div>` -->
                <div v-for="prop in productPropertyList" :key="prop.propertyId" class="sku-row">
                  <!-- [zh] 开始标签 `<div>` -->
                  <div class="sku-label">{{ prop.propertyName }}</div>
                  <!-- [zh] 开始标签 `<div>` -->
                  <div class="sku-values">
                    <!-- [zh] 开始标签 `<button>` -->
                    <button
                      v-for="val in prop.propertyValues"
                      :key="val.propertyValueId"
                      type="button"
                      class="sku-tag"
                      :class="{ active: selectedProperty[prop.propertyId] === val.propertyValueId }"
                      @click="selectProperty(prop, val)"
                    >
                      <!-- [zh] 开始标签 `<ProductImage>` -->
                      <ProductImage
                        v-if="val.propertyCover"
                        :source="val.propertyCover"
                        :width="18"
                        :height="18"
                        fit="contain"
                        :lazy="false"
                        dense
                        class="sku-thumb ignore"
                      />
                      <!-- [zh] 开始标签 `<span>` -->
                      <span class="sku-text">{{ val.propertyValue }}</span>
                    <!-- [zh] 闭合标签 `</button>` -->
                    </button>
                  <!-- [zh] 闭合标签 `</div>` -->
                  </div>
                <!-- [zh] 闭合标签 `</div>` -->
                </div>

                <!-- [zh] 开始标签 `<div>` -->
                <div class="qty-row">
                  <!-- [zh] 开始标签 `<span>` -->
                  <span class="sku-label">数量</span>
                  <!-- [zh] 开始标签 `<el-input-number>` -->
                  <el-input-number v-model="quantity" :min="1" :max="maxBuy" size="small" />
                <!-- [zh] 闭合标签 `</div>` -->
                </div>
              <!-- [zh] 闭合标签 `</div>` -->
              </div>

              <!-- [zh] 开始标签 `<footer>` -->
              <footer class="pick-footer">
                <!-- [zh] 开始标签 `<el-button>` -->
                <el-button type="primary" round size="large" class="btn-confirm" :loading="submitting" @click="confirmAdd">
                  <!-- [zh] 模板内容：`加入购物车` -->
                  加入购物车
                <!-- [zh] 闭合标签 `</el-button>` -->
                </el-button>
              <!-- [zh] 闭合标签 `</footer>` -->
              </footer>
            </template>

            <el-empty v-else description="商品不存在或已下架" class="sheet-empty" />
          </section>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { onBeforeUnmount, watch, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { Close } from '@element-plus/icons-vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/api/modules`
import { cartApi } from '@/api/modules';
// [zh] 导入 `@/composables/useProductSku`
import { useProductSku } from '@/composables/useProductSku';
// [zh] 导入 `@/composables/useProductSkuSheet`
import { useProductSkuSheet } from '@/composables/useProductSkuSheet';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/stores/cart`
import { useCartStore } from '@/stores/cart';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `cartStore`
const cartStore = useCartStore();
// [zh] 语句
const { isDesktop } = useDevice();
// [zh] 语句
const { visible, productId, close } = useProductSkuSheet();
// [zh] 声明变量 `submitting`
const submitting = ref(false);

// [zh] 代码块开始
const {
  // [zh] TypeScript/JS 语句：`loading,`
  loading,
  // [zh] TypeScript/JS 语句：`productInfo,`
  productInfo,
  // [zh] TypeScript/JS 语句：`productPropertyList,`
  productPropertyList,
  // [zh] TypeScript/JS 语句：`quantity,`
  quantity,
  // [zh] TypeScript/JS 语句：`selectedSku,`
  selectedSku,
  // [zh] TypeScript/JS 语句：`selectedProperty,`
  selectedProperty,
  // [zh] TypeScript/JS 语句：`displayPrice,`
  displayPrice,
  // [zh] TypeScript/JS 语句：`maxBuy,`
  maxBuy,
  // [zh] TypeScript/JS 语句：`coverImage,`
  coverImage,
  // [zh] TypeScript/JS 语句：`selectProperty,`
  selectProperty,
  // [zh] TypeScript/JS 语句：`load,`
  load,
  // [zh] TypeScript/JS 语句：`validateSku`
  validateSku
// [zh] 语句
} = useProductSku(() => productId.value);

// [zh] 代码块开始
watch(visible, (show) => {
  // [zh] 语句
  document.body.style.overflow = show ? 'hidden' : '';
  // [zh] 条件判断 if
  if (show && productId.value) {
    // [zh] 语句
    void load();
  // [zh] 语法符号
  }
// [zh] 语句
});

// [zh] 代码块开始
onBeforeUnmount(() => {
  // [zh] 语句
  document.body.style.overflow = '';
// [zh] 语句
});

// [zh] 声明变量 `confirmAdd`
const confirmAdd = async () => {
  // [zh] 条件判断 if
  if (!authStore.isLoggedIn) {
    // [zh] 语句
    close();
    // [zh] 语句
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!validateSku() || !productInfo.value) return;

  // [zh] 语句
  submitting.value = true;
  // [zh] 代码块开始
  try {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await cartApi.add2Cart({
      // [zh] TypeScript/JS 语句：`productId: productInfo.value.productId,`
      productId: productInfo.value.productId,
      // [zh] TypeScript/JS 语句：`buyCount: quantity.value,`
      buyCount: quantity.value,
      // [zh] TypeScript/JS 语句：`propertyValueIds: selectedSku.value.propertyValueIds`
      propertyValueIds: selectedSku.value.propertyValueIds
    // [zh] 语句
    });
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await cartStore.fetchCartCount();
    // [zh] 语句
    toast.success('已加入购物车');
    // [zh] 语句
    close();
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    submitting.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.sku-sheet-root {` */
.sku-sheet-root {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `z-index: 1100;` */
  z-index: 1100;
  /* [zh] 样式规则 `background: rgba(16, 18, 22, 0.45);` */
  background: rgba(16, 18, 22, 0.45);
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-end;` */
  align-items: flex-end;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;

  /* [zh] 样式规则 `&.is-desktop {` */
  &.is-desktop {
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-sheet-panel {` */
.sku-sheet-panel {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: $content-width;` */
  max-width: $content-width;
  /* [zh] 样式规则 `max-height: min(78vh, 640px);` */
  max-height: min(78vh, 640px);
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card $radius-card` */
  border-radius: $radius-card $radius-card 0 0;
  /* [zh] 样式规则 `box-shadow: 0 -12px 40px rgba(16, 24, 40` */
  box-shadow: 0 -12px 40px rgba(16, 24, 40, 0.18);
  /* [zh] 样式规则 `padding-bottom: env(safe-area-inset-bott` */
  padding-bottom: env(safe-area-inset-bottom, 0);
  /* [zh] 样式规则 `margin-bottom: 50px;` */
  margin-bottom: 50px;

  /* [zh] 样式规则 `&::after {` */
  &::after {
    /* [zh] 样式规则 `content: '';` */
    content: '';
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `left: 0;` */
    left: 0;
    /* [zh] 样式规则 `right: 0;` */
    right: 0;
    /* [zh] 样式规则 `bottom: -50px;` */
    bottom: -50px;
    /* [zh] 样式规则 `height: 50px;` */
    height: 50px;
    /* [zh] 样式规则 `background: $color-card;` */
    background: $color-card;
    /* [zh] 样式规则 `z-index: -1;` */
    z-index: -1;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.is-desktop & {` */
  .is-desktop & {
    /* [zh] 样式规则 `border-radius: $radius-card;` */
    border-radius: $radius-card;
    /* [zh] 样式规则 `box-shadow: 0 12px 40px rgba(16, 24, 40,` */
    box-shadow: 0 12px 40px rgba(16, 24, 40, 0.18);
    /* [zh] 样式规则 `margin-bottom: 0;` */
    margin-bottom: 0;
    /* [zh] 样式规则 `max-width: 420px;` */
    max-width: 420px;

    /* [zh] 样式规则 `&::after {` */
    &::after {
      /* [zh] 样式规则 `display: none;` */
      display: none;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sheet-close {` */
.sheet-close {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `top: 10px;` */
  top: 10px;
  /* [zh] 样式规则 `right: 12px;` */
  right: 12px;
  /* [zh] 样式规则 `z-index: 2;` */
  z-index: 2;
  /* [zh] 样式规则 `width: 32px;` */
  width: 32px;
  /* [zh] 样式规则 `height: 32px;` */
  height: 32px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `background: $color-border-light;` */
    background: $color-border-light;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sheet-drag {` */
.sheet-drag {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `width: 36px;` */
  width: 36px;
  /* [zh] 样式规则 `height: 4px;` */
  height: 4px;
  /* [zh] 样式规则 `margin: 8px auto 4px;` */
  margin: 8px auto 4px;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `background: rgba($color-text-muted, 0.35` */
  background: rgba($color-text-muted, 0.35);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sheet-skeleton {` */
.sheet-skeleton {
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pick-header {` */
.pick-header {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding: 4px 44px 12px 16px;` */
  padding: 4px 44px 12px 16px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pick-cover {` */
.pick-cover {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pick-meta {` */
.pick-meta {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pick-name {` */
.pick-name {
  /* [zh] 样式规则 `margin: 0 0 4px;` */
  margin: 0 0 4px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
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

/* [zh] 样式规则 `.pick-price {` */
.pick-price {
  /* [zh] 样式规则 `margin: 0 0 2px;` */
  margin: 0 0 2px;
  /* [zh] 样式规则 `font-size: 18px;` */
  font-size: 18px;
  /* [zh] 样式规则 `font-weight: 700;` */
  font-weight: 700;
  /* [zh] 样式规则 `color: $color-price;` */
  color: $color-price;

  /* [zh] 样式规则 `.sym {` */
  .sym {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pick-stock {` */
.pick-stock {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;

  /* [zh] 样式规则 `em {` */
  em {
    /* [zh] 样式规则 `margin-left: 4px;` */
    margin-left: 4px;
    /* [zh] 样式规则 `font-style: normal;` */
    font-style: normal;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pick-body {` */
.pick-body {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
  /* [zh] 样式规则 `padding: 0 16px 12px;` */
  padding: 0 16px 12px;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
  -webkit-overflow-scrolling: touch;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-row {` */
.sku-row {
  /* [zh] 样式规则 `margin-bottom: 14px;` */
  margin-bottom: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-label {` */
.sku-label {
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-values {` */
.sku-values {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-tag {` */
.sku-tag {
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `padding: 6px 11px;` */
  padding: 6px 11px;
  /* [zh] 样式规则 `min-height: 30px;` */
  min-height: 30px;
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `border-radius: $radius-btn;` */
  border-radius: $radius-btn;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `line-height: 1.35;` */
  line-height: 1.35;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: border-color $transition-fas` */
  transition: border-color $transition-fast, background $transition-fast, color $transition-fast;

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `border-color: $color-primary;` */
    border-color: $color-primary;
    /* [zh] 样式规则 `background: $color-primary-soft;` */
    background: $color-primary-soft;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-text {` */
.sku-text {
  /* [zh] 样式规则 `line-height: 1.35;` */
  line-height: 1.35;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-thumb {` */
.sku-thumb {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `width: 18px;` */
  width: 18px;
  /* [zh] 样式规则 `height: 18px;` */
  height: 18px;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `border: 1px solid rgba($color-border, 0.` */
  border: 1px solid rgba($color-border, 0.55);
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;

  /* [zh] 样式规则 `:deep(.product-image) {` */
  :deep(.product-image) {
    /* [zh] 样式规则 `width: 18px !important;` */
    width: 18px !important;
    /* [zh] 样式规则 `height: 18px !important;` */
    height: 18px !important;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.qty-row {` */
.qty-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `padding-top: 8px;` */
  padding-top: 8px;
  /* [zh] 样式规则 `border-top: 1px solid $color-border-ligh` */
  border-top: 1px solid $color-border-light;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pick-footer {` */
.pick-footer {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `padding: 10px 16px 12px;` */
  padding: 10px 16px 12px;
  /* [zh] 样式规则 `border-top: 1px solid $color-border-ligh` */
  border-top: 1px solid $color-border-light;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.btn-confirm {` */
.btn-confirm {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sheet-empty {` */
.sheet-empty {
  /* [zh] 样式规则 `padding: 32px 16px;` */
  padding: 32px 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.sku-sheet-fade-enter-active,
/* [zh] 样式规则 `.sku-sheet-fade-leave-active {` */
.sku-sheet-fade-leave-active {
  /* [zh] 样式规则 `transition: opacity 0.22s ease;` */
  transition: opacity 0.22s ease;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.sku-sheet-fade-enter-from,
/* [zh] 样式规则 `.sku-sheet-fade-leave-to {` */
.sku-sheet-fade-leave-to {
  /* [zh] 样式规则 `opacity: 0;` */
  opacity: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.sku-sheet-slide-enter-active,
/* [zh] 样式规则 `.sku-sheet-slide-leave-active {` */
.sku-sheet-slide-leave-active {
  /* [zh] 样式规则 `transition: transform 0.28s cubic-bezier` */
  transition: transform 0.28s cubic-bezier(0.32, 0.72, 0, 1);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.sku-sheet-slide-enter-from,
/* [zh] 样式规则 `.sku-sheet-slide-leave-to {` */
.sku-sheet-slide-leave-to {
  /* [zh] 样式规则 `transform: translateY(100%);` */
  transform: translateY(100%);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.is-desktop .sku-sheet-slide-enter-from,
/* [zh] 样式规则 `.is-desktop .sku-sheet-slide-leave-to {` */
.is-desktop .sku-sheet-slide-leave-to {
  /* [zh] 样式规则 `transform: translateY(0) scale(0.95);` */
  transform: translateY(0) scale(0.95);
  /* [zh] 样式规则 `opacity: 0;` */
  opacity: 0;
/* [zh] 样式规则 `}` */
}
</style>
