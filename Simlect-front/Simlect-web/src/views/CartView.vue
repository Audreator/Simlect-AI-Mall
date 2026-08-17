<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="cart-page product-page-compact">
    <!-- [zh] 开始标签 `<el-skeleton>` -->
    <el-skeleton v-if="pageLoading" animated :rows="6" class="cart-skeleton" />

    <!-- [zh] 开始标签 `<div>` -->
    <div v-else-if="loadError" class="cart-error card-flat">
      <!-- [zh] 开始标签 `<p>` -->
      <p>{{ loadError }}</p>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" round @click="load">重试</el-button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-else-if="list.length" class="cart-list">
      <!-- [zh] 开始标签 `<SwipeDeleteRow>` -->
      <SwipeDeleteRow
        v-for="row in list"
        :key="row.cartId"
        :open="openSwipeId === row.cartId"
        @open="openSwipeId = row.cartId"
        @close="onSwipeClose(row.cartId)"
        @delete="del(row.cartId)"
      >
      <!-- [zh] 开始标签 `<article>` -->
      <article class="cart-item">
        <!-- [zh] 开始标签 `<el-checkbox>` -->
        <el-checkbox
          class="item-check"
          :model-value="selectedIds.has(row.cartId)"
          :disabled="!row.productOnSale"
          @change="onItemCheckChange(row.cartId, $event)"
        />
        <!-- [zh] 开始标签 `<RouterLink>` -->
        <RouterLink :to="`/product/${row.productId}`" class="item-cover-col">
          <!-- [zh] 开始标签 `<ProductImage>` -->
          <ProductImage :source="row.productCover" class="item-cover-img" />
        <!-- [zh] 闭合标签 `</RouterLink>` -->
        </RouterLink>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="item-body">
          <!-- [zh] 开始标签 `<RouterLink>` -->
          <RouterLink :to="`/product/${row.productId}`" class="item-name" :title="row.productName">
            <!-- [zh] Mustache 插值表达式 -->
            {{ row.productName }}
          <!-- [zh] 闭合标签 `</RouterLink>` -->
          </RouterLink>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="row.propertyData?.length" class="item-sku">
            <!-- [zh] 开始标签 `<span>` -->
            <span v-for="(p, i) in row.propertyData" :key="i">
              <!-- [zh] Mustache 插值表达式 -->
              {{ p.propertyName }}：{{ p.propertyValue }}
            <!-- [zh] 闭合标签 `</span>` -->
            </span>
          <!-- [zh] 闭合标签 `</p>` -->
          </p>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="!row.productOnSale" class="item-off">已下架</p>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="item-foot">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="price-block">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="unit-price">¥{{ formatUnitPrice(row) }}</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span v-if="priceDeltaText(row)" class="price-delta" :class="priceDeltaClass(row)">
                <!-- [zh] Mustache 插值表达式 -->
                {{ priceDeltaText(row) }}
              <!-- [zh] 闭合标签 `</span>` -->
              </span>
              <!-- [zh] 开始标签 `<span>` -->
              <span class="line-total">小计 ¥{{ formatLineTotal(row) }}</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div
              class="qty-stepper"
              :class="{ disabled: !row.productOnSale || updatingCartId === row.cartId }"
              @click.stop
            >
              <!-- [zh] 开始标签 `<button>` -->
              <button
                type="button"
                class="qty-btn"
                aria-label="减少数量"
                :disabled="!row.productOnSale || updatingCartId === row.cartId || getQty(row) <= 1"
                @click="decreaseQty(row)"
              >
                <!-- [zh] 模板内容：`−` -->
                −
              <!-- [zh] 闭合标签 `</button>` -->
              </button>
              <!-- [zh] 开始标签 `<input>` -->
              <input
                v-model.number="row.buyCount"
                class="qty-input"
                type="number"
                inputmode="numeric"
                min="1"
                :max="MAX_CART_QTY"
                aria-label="数量"
                :disabled="!row.productOnSale || updatingCartId === row.cartId"
                @focus="onQtyFocus(row)"
                @blur="commitQtyInput(row)"
                @keydown.enter="($event.target as HTMLInputElement).blur()"
              />
              <!-- [zh] 开始标签 `<button>` -->
              <button
                type="button"
                class="qty-btn"
                aria-label="增加数量"
                :disabled="!row.productOnSale || updatingCartId === row.cartId || getQty(row) >= MAX_CART_QTY"
                @click="increaseQty(row)"
              >
                <!-- [zh] 模板内容：`+` -->
                +
              <!-- [zh] 闭合标签 `</button>` -->
              </button>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</article>` -->
      </article>
      <!-- [zh] 闭合标签 `</SwipeDeleteRow>` -->
      </SwipeDeleteRow>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<el-empty>` -->
    <el-empty v-else-if="!pageLoading" description="购物车空空如也，去逛逛吧" class="cart-empty">
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" round @click="router.push('/')">去首页</el-button>
    <!-- [zh] 闭合标签 `</el-empty>` -->
    </el-empty>

    <!-- [zh] 开始标签 `<section>` -->
    <section v-if="recommendProducts.length" class="cart-recommend">
      <!-- [zh] 开始标签 `<header>` -->
      <header class="recommend-head">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="recommend-title">为你推荐</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="recommend-sub">猜你喜欢 · 更多好物</span>
      <!-- [zh] 闭合标签 `</header>` -->
      </header>
      <!-- [zh] 开始标签 `<div>` -->
      <div :class="isDesktop ? 'pc-cart-recommend-grid' : 'recommend-grid'">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-if="isDesktop">
          <!-- [zh] 开始标签 `<PcProductTile>` -->
          <PcProductTile
            v-for="p in recommendProducts"
            :key="p.productId"
            :product="p"
            @click="goProduct"
          />
        </template>
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-else>
          <!-- [zh] 开始标签 `<ProductCard>` -->
          <ProductCard
            v-for="p in recommendProducts"
            :key="p.productId"
            :product="p"
            compact
            @click="goProduct"
          />
        </template>
      </div>
    </section>

    <LiquidGlassSurface v-if="list.length && !pageLoading && !loadError" tag="footer" intensity="strong" class="cart-bar ignore">
      <el-checkbox
        class="bar-check-all"
        :model-value="allSelectableChecked"
        :indeterminate="isIndeterminate"
        @change="toggleAll"
      >
        全选
      </el-checkbox>
      <span class="bar-count">已选 {{ selectedCount }} 件</span>
      <div class="bar-right">
        <span class="bar-amount">
          合计：<strong>¥{{ selectedAmount }}</strong>
        </span>
        <el-button
          type="primary"
          size="large"
          round
          class="btn-checkout"
          :disabled="selectedCount === 0"
          @click="checkout"
        >
          结算
        </el-button>
      </div>
    </LiquidGlassSurface>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
import SwipeDeleteRow from '@/components/business/SwipeDeleteRow.vue';
import ProductImage from '@/components/common/ProductImage.vue';
import ProductCard from '@/components/business/ProductCard.vue';
import PcProductTile from '@/components/pc/PcProductTile.vue';
import { useDevice } from '@/composables/useDevice';
import { confirmAction } from '@/utils/confirm';
import { toast } from '@/utils/toast';
import { saveCheckoutSession, type CheckoutLineItem } from '@/utils/checkout';
import { MAX_CART_QTY } from '@/constants/validation';
import { cartApi, productApi } from '@/api/modules';
import { filterOnSaleProducts } from '@/utils/product';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';
import { usePageRefresh } from '@/composables/pullRefresh';

const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();
const { isDesktop } = useDevice();
const list = ref<any[]>([]);
const pageLoading = ref(true);
const loadError = ref('');
const recommendProducts = ref<any[]>([]);
const selectedIds = ref<Set<string>>(new Set());
const updatingCartId = ref<string | null>(null);
const openSwipeId = ref<string | null>(null);

const goProduct = (p: any) => {
  if (p?.productId) router.push(`/product/${p.productId}`);
};

const loadRecommend = async () => {
  if (recommendProducts.value.length) return;
  try {
    const commend = await productApi.loadCommendProduct();
    const arr = Array.isArray(commend) ? commend : commend?.list;
    const filtered = filterOnSaleProducts(arr);
    const targetCount = isDesktop.value ? 12 : 6;
    if (isDesktop.value && filtered.length < targetCount && filtered.length > 0) {

      const items = [...filtered];
      while (items.length < targetCount) {
        items.push(filtered[items.length % filtered.length]);
      }
      recommendProducts.value = items;
    } else {
      recommendProducts.value = filtered.slice(0, targetCount);
    }
  } catch {

  }
};

const onSwipeClose = (cartId: string) => {
  if (openSwipeId.value === cartId) openSwipeId.value = null;
};

const qtySnapshot = new Map<string, number>();

const getQty = (row: { buyCount?: number }) => Math.max(1, Number(row.buyCount) || 1);

const formatUnitPrice = (row: { price?: number }) => Number(row.price || 0).toFixed(2);

const formatLineTotal = (row: { price?: number; buyCount?: number }) =>
  (Number(row.price || 0) * getQty(row)).toFixed(2);

const priceDiff = (row: { price?: number; addPrice?: number }) => {
  if (row.addPrice == null || row.price == null) return 0;
  return Number(row.price) - Number(row.addPrice);
};

const priceDeltaText = (row: { price?: number; addPrice?: number }) => {
  const diff = priceDiff(row);
  if (Math.abs(diff) < 0.005) return '';
  const abs = Math.abs(diff).toFixed(2);
  return diff > 0 ? `比添加时多了${abs}元` : `比添加时少了${abs}元`;
};

const priceDeltaClass = (row: { price?: number; addPrice?: number }) => {
  const diff = priceDiff(row);
  if (diff > 0) return 'up';
  if (diff < 0) return 'down';
  return '';
};

const normalizeCartRow = (row: Record<string, unknown>) => ({
  ...row,
  buyCount: Math.max(1, Math.floor(Number(row.buyCount) || 1))
});

const selectableItems = computed(() => list.value.filter((row) => row.productOnSale));

const selectedCount = computed(() =>
  selectableItems.value
    .filter((row) => selectedIds.value.has(row.cartId))
    .reduce((sum, row) => sum + (Number(row.buyCount) || 0), 0)
);

const selectedAmount = computed(() => {
  const total = selectableItems.value
    .filter((row) => selectedIds.value.has(row.cartId))
    .reduce((sum, row) => sum + Number(row.price) * (Number(row.buyCount) || 0), 0);
  return total.toFixed(2);
});

const allSelectableChecked = computed(
  () =>
    selectableItems.value.length > 0 &&
    selectableItems.value.every((row) => selectedIds.value.has(row.cartId))
);

const isIndeterminate = computed(() => {
  const selected = selectableItems.value.filter((row) => selectedIds.value.has(row.cartId)).length;
  return selected > 0 && selected < selectableItems.value.length;
});

const syncDefaultSelection = () => {
  selectedIds.value = new Set(selectableItems.value.map((row) => row.cartId));
};

const onItemCheckChange = (cartId: string, checked: boolean | string | number) => {
  toggleItem(cartId, !!checked);
};

const toggleItem = (cartId: string, checked: boolean) => {
  const next = new Set(selectedIds.value);
  if (checked) next.add(cartId);
  else next.delete(cartId);
  selectedIds.value = next;
};

const toggleAll = (checked: boolean) => {
  if (checked) syncDefaultSelection();
  else selectedIds.value = new Set();
};

const load = async () => {
  pageLoading.value = true;
  loadError.value = '';
  try {
    const ok = await authStore.ensureSession();
    if (!ok) {
      list.value = [];
      selectedIds.value = new Set();
      cartStore.resetCart();
      return;
    }

    const res = await cartApi.loadProductCart({ pageNo: 1 });
    const rows = Array.isArray(res?.list) ? res.list : [];
    list.value = rows.map((row: Record<string, unknown>) => normalizeCartRow(row));
    syncDefaultSelection();
    await cartStore.fetchCartCount();
  } catch (e: any) {
    loadError.value = e?.info || '购物车加载失败，请重试';
    list.value = [];
    selectedIds.value = new Set();
  } finally {
    pageLoading.value = false;
  }
};

watch(
  () => authStore.userInfo?.userId,
  (userId, prevUserId) => {
    if (userId !== prevUserId) {
      list.value = [];
      selectedIds.value = new Set();
      if (userId) void load();
      else cartStore.resetCart();
    }
  }
);

const decreaseQty = (row: Record<string, any>) => {
  void changeQty(row, getQty(row) - 1);
};

const increaseQty = (row: Record<string, any>) => {
  void changeQty(row, getQty(row) + 1);
};

const onQtyFocus = (row: Record<string, any>) => {
  qtySnapshot.set(String(row.cartId), getQty(row));
};

const commitQtyInput = (row: Record<string, any>) => {
  const cartId = String(row.cartId);
  const prev = qtySnapshot.get(cartId) ?? getQty(row);
  qtySnapshot.delete(cartId);

  let count = Math.floor(Number(row.buyCount));
  if (!Number.isFinite(count) || count < 1) {
    row.buyCount = prev;
    return;
  }
  if (count > MAX_CART_QTY) {
    count = MAX_CART_QTY;
    toast.warning(`单个商品最多购买 ${MAX_CART_QTY} 件`);
  }
  row.buyCount = count;
  void changeQty(row, count, prev);
};

const changeQty = async (
  row: Record<string, any>,
  val: number | undefined,
  prevOverride?: number
) => {
  if (!row.productOnSale || updatingCartId.value) return;

  const prev = prevOverride ?? (Number(row.buyCount) || 1);
  let count = Math.floor(Number(val) || 0);

  if (count < 1) count = 1;
  if (count > MAX_CART_QTY) {
    count = MAX_CART_QTY;
    if (prev < MAX_CART_QTY) toast.warning(`单个商品最多购买 ${MAX_CART_QTY} 件`);
  }
  if (count === prev) {
    row.buyCount = count;
    return;
  }

  const delta = count - prev;
  if (delta === 0) return;

  updatingCartId.value = row.cartId;
  try {
    await cartApi.add2Cart({
      productId: row.productId,
      propertyValueIds: row.propertyValueIds,
      buyCount: delta
    });
    row.buyCount = prev + delta;
    await cartStore.fetchCartCount();
  } catch {
    row.buyCount = prev;
  } finally {
    updatingCartId.value = null;
  }
};

const del = async (id: string) => {
  const ok = await confirmAction('确定要将该商品移出购物车吗？', {
    title: '移出购物车',
    confirmButtonText: '移出'
  });
  if (!ok) return;
  await cartApi.deleteCart(id);
  if (openSwipeId.value === id) openSwipeId.value = null;
  toast.success('已移出购物车');
  await load();
};

const checkout = () => {
  const rows = selectableItems.value.filter((row) => selectedIds.value.has(row.cartId));
  if (!rows.length) {
    toast.warning('请先勾选要结算的商品');
    return;
  }
  const items: CheckoutLineItem[] = rows.map((row) => ({
    cartId: row.cartId,
    productId: row.productId,
    productName: row.productName,
    productCover: row.productCover,
    propertyValueIds: row.propertyValueIds,
    propertyValueIdHash: row.propertyValueIdHash,
    propertyData: row.propertyData,
    price: Number(row.price),
    buyCount: Number(row.buyCount) || 1
  }));
  saveCheckoutSession(items, 1);
  router.push('/checkout');
};

onMounted(() => {
  load();
  loadRecommend();
});
usePageRefresh(load);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.cart-page {` */
.cart-page {
  /* [zh] 样式规则 `min-height: 100%;` */
  min-height: 100%;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `padding-bottom: calc(var(--mobile-tab-st` */
  padding-bottom: calc(var(--mobile-tab-stack-height, #{$mobile-tab-height + 12px}) + 68px);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cart-skeleton {` */
.cart-skeleton {
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cart-error {` */
.cart-error {
  /* [zh] 样式规则 `margin: 16px;` */
  margin: 16px;
  /* [zh] 样式规则 `padding: 24px 16px;` */
  padding: 24px 16px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;

  /* [zh] 样式规则 `p {` */
  p {
    /* [zh] 样式规则 `margin: 0 0 12px;` */
    margin: 0 0 12px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: $color-text-secondary;` */
    color: $color-text-secondary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cart-list {` */
.cart-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;

  /* [zh] 样式规则 `:deep(.swipe-delete-row) {` */
  :deep(.swipe-delete-row) {
    /* [zh] 样式规则 `border-radius: $radius-card;` */
    border-radius: $radius-card;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `box-shadow: $shadow-card;` */
    box-shadow: $shadow-card;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.swipe-content) {` */
  :deep(.swipe-content) {
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `border-radius: $radius-card;` */
    border-radius: $radius-card;
    /* [zh] 样式规则 `box-shadow: none;` */
    box-shadow: none;
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cart-item {` */
.cart-item {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `padding: 12px 12px 12px 8px;` */
  padding: 12px 12px 12px 8px;
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `box-shadow: none;` */
  box-shadow: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-check {` */
.item-check {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `margin-top: 24px;` */
  margin-top: 24px;
  /* [zh] 样式规则 `height: auto;` */
  height: auto;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-cover-col {` */
.item-cover-col {
  /* [zh] 样式规则 `flex: 0 0 25%;` */
  flex: 0 0 25%;
  /* [zh] 样式规则 `width: 25%;` */
  width: 25%;
  /* [zh] 样式规则 `max-width: 76px;` */
  max-width: 76px;
  /* [zh] 样式规则 `aspect-ratio: 1;` */
  aspect-ratio: 1;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `:deep(.product-image) {` */
  :deep(.product-image) {
    /* [zh] 样式规则 `width: 100% !important;` */
    width: 100% !important;
    /* [zh] 样式规则 `height: 100% !important;` */
    height: 100% !important;
    /* [zh] 样式规则 `border-radius: 12px;` */
    border-radius: 12px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-body {` */
.item-body {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-name {` */
.item-name {
  /* [zh] 样式规则 `display: -webkit-box;` */
  display: -webkit-box;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `text-decoration: none;` */
  text-decoration: none;
  /* [zh] 样式规则 `line-height: 1.35;` */
  line-height: 1.35;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `-webkit-line-clamp: 2;` */
  -webkit-line-clamp: 2;
  /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
  -webkit-box-orient: vertical;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-sku {` */
.item-sku {
  /* [zh] 样式规则 `margin: 6px 0 0;` */
  margin: 6px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;

  /* [zh] 样式规则 `span + span::before {` */
  span + span::before {
    /* [zh] 样式规则 `content: ' · ';` */
    content: ' · ';
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-off {` */
.item-off {
  /* [zh] 样式规则 `margin: 4px 0 0;` */
  margin: 4px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-price;` */
  color: $color-price;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-foot {` */
.item-foot {
  /* [zh] 样式规则 `margin-top: 10px;` */
  margin-top: 10px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-end;` */
  align-items: flex-end;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.price-block {` */
.price-block {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;

  /* [zh] 样式规则 `.unit-price {` */
  .unit-price {
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price-delta {` */
  .price-delta {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;

    /* [zh] 样式规则 `&.up {` */
    &.up {
      /* [zh] 样式规则 `color: $color-error;` */
      color: $color-error;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.down {` */
    &.down {
      /* [zh] 样式规则 `color: $color-success;` */
      color: $color-success;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.line-total {` */
  .line-total {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.qty-stepper {` */
.qty-stepper {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `height: 30px;` */
  height: 30px;
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `&.disabled {` */
  &.disabled {
    /* [zh] 样式规则 `opacity: 0.55;` */
    opacity: 0.55;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.qty-btn {` */
.qty-btn {
  /* [zh] 样式规则 `width: 32px;` */
  width: 32px;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `font-size: 18px;` */
  font-size: 18px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `line-height: 1;` */
  line-height: 1;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: background $transition-fast,` */
  transition: background $transition-fast, color $transition-fast;

  /* [zh] 样式规则 `&:hover:not(:disabled) {` */
  &:hover:not(:disabled) {
    /* [zh] 样式规则 `background: $color-primary-muted;` */
    background: $color-primary-muted;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:disabled {` */
  &:disabled {
    /* [zh] 样式规则 `cursor: not-allowed;` */
    cursor: not-allowed;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.qty-input {` */
.qty-input {
  /* [zh] 样式规则 `width: 40px;` */
  width: 40px;
  /* [zh] 样式规则 `min-width: 36px;` */
  min-width: 36px;
  /* [zh] 样式规则 `max-width: 56px;` */
  max-width: 56px;
  /* [zh] 样式规则 `padding: 0 4px;` */
  padding: 0 4px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-left: 1px solid $color-border;` */
  border-left: 1px solid $color-border;
  /* [zh] 样式规则 `border-right: 1px solid $color-border;` */
  border-right: 1px solid $color-border;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `-moz-appearance: textfield;` */
  -moz-appearance: textfield;
  /* [zh] 样式规则 `appearance: textfield;` */
  appearance: textfield;

  /* [zh] 样式声明 */
  &::-webkit-outer-spin-button,
  /* [zh] 样式规则 `&::-webkit-inner-spin-button {` */
  &::-webkit-inner-spin-button {
    /* [zh] 样式规则 `-webkit-appearance: none;` */
    -webkit-appearance: none;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:focus {` */
  &:focus {
    /* [zh] 样式规则 `outline: none;` */
    outline: none;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:disabled {` */
  &:disabled {
    /* [zh] 样式规则 `cursor: not-allowed;` */
    cursor: not-allowed;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cart-empty {` */
.cart-empty {
  /* [zh] 样式规则 `padding: 48px 0;` */
  padding: 48px 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cart-recommend {` */
.cart-recommend {
  /* [zh] 样式规则 `margin-top: 14px;` */
  margin-top: 14px;
  /* [zh] 样式规则 `padding-bottom: 16px;` */
  padding-bottom: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.recommend-head {` */
.recommend-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: baseline;` */
  align-items: baseline;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;

  /* [zh] 样式声明 */
  &::before,
  /* [zh] 样式规则 `&::after {` */
  &::after {
    /* [zh] 样式规则 `content: '';` */
    content: '';
    /* [zh] 样式规则 `flex: 0 0 28px;` */
    flex: 0 0 28px;
    /* [zh] 样式规则 `height: 1px;` */
    height: 1px;
    /* [zh] 样式规则 `align-self: center;` */
    align-self: center;
    /* [zh] 样式规则 `background: linear-gradient(90deg, trans` */
    background: linear-gradient(90deg, transparent, $color-border-gray);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&::after {` */
  &::after {
    /* [zh] 样式规则 `background: linear-gradient(90deg, $colo` */
    background: linear-gradient(90deg, $color-border-gray, transparent);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.recommend-title {` */
  .recommend-title {
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `letter-spacing: $letter-spacing-heading;` */
    letter-spacing: $letter-spacing-heading;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.recommend-sub {` */
  .recommend-sub {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.recommend-grid {` */
.recommend-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(2, 1fr);` */
  grid-template-columns: repeat(2, 1fr);
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cart-bar {` */
.cart-bar {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `left: 12px;` */
  left: 12px;
  /* [zh] 样式规则 `right: 12px;` */
  right: 12px;
  /* [zh] 样式规则 `bottom: calc(var(--mobile-tab-stack-heig` */
  bottom: calc(var(--mobile-tab-stack-height, #{$mobile-tab-height + 12px}) + 6px);
  /* [zh] 样式规则 `z-index: 1000;` */
  z-index: 1000;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 22px;` */
  border-radius: 22px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `box-shadow:` */
  box-shadow:
    /* [zh] 样式声明 */
    0 6px 6px rgba(0, 0, 0, 0.16),
    /* [zh] 样式声明 */
    0 0 20px rgba(0, 0, 0, 0.08);

  /* [zh] 样式规则 `:deep(.liquid-glass-surface__content) {` */
  :deep(.liquid-glass-surface__content) {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `padding: 10px 14px;` */
    padding: 10px 14px;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `@media (min-width: $breakpoint-mobile) {` */
  @media (min-width: $breakpoint-mobile) {
    /* [zh] 样式规则 `position: sticky;` */
    position: sticky;
    /* [zh] 样式规则 `bottom: 0;` */
    bottom: 0;
    /* [zh] 样式规则 `margin-top: 16px;` */
    margin-top: 16px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.bar-check-all {` */
  .bar-check-all {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;

    /* [zh] 样式规则 `:deep(.el-checkbox__label) {` */
    :deep(.el-checkbox__label) {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `font-weight: 500;` */
      font-weight: 500;
      /* [zh] 样式规则 `color: $color-text-body;` */
      color: $color-text-body;
      /* [zh] 样式规则 `padding-left: 6px;` */
      padding-left: 6px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.bar-count {` */
  .bar-count {
    /* [zh] 样式规则 `flex: 0 1 auto;` */
    flex: 0 1 auto;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `font-weight: 400;` */
    font-weight: 400;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.bar-right {` */
  .bar-right {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
    /* [zh] 样式规则 `margin-left: auto;` */
    margin-left: auto;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.bar-amount {` */
  .bar-amount {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;

    /* [zh] 样式规则 `strong {` */
    strong {
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
      /* [zh] 样式规则 `font-size: 18px;` */
      font-size: 18px;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.btn-checkout {` */
  .btn-checkout {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `min-width: 96px;` */
    min-width: 96px;
    /* [zh] 样式规则 `height: 40px;` */
    height: 40px;
    /* [zh] 样式规则 `padding: 0 20px;` */
    padding: 0 20px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `border-radius: $radius-pill;` */
    border-radius: $radius-pill;
    /* [zh] 样式规则 `background: linear-gradient(90deg, $colo` */
    background: linear-gradient(90deg, $color-primary-hover, $color-primary);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
