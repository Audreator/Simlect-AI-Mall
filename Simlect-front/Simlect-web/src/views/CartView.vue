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
                <!-- [zh] 模板内容：`@click="increaseQty(row)"` -->
                @click="increaseQty(row)"
              <!-- [zh] 模板内容：`>` -->
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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted, ref, watch } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@/components/common/LiquidGlassSurface.vue`
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
// [zh] 导入 `@/components/business/SwipeDeleteRow.vue`
import SwipeDeleteRow from '@/components/business/SwipeDeleteRow.vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/components/business/ProductCard.vue`
import ProductCard from '@/components/business/ProductCard.vue';
// [zh] 导入 `@/components/pc/PcProductTile.vue`
import PcProductTile from '@/components/pc/PcProductTile.vue';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/utils/confirm`
import { confirmAction } from '@/utils/confirm';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/utils/checkout`
import { saveCheckoutSession, type CheckoutLineItem } from '@/utils/checkout';
// [zh] 导入 `@/constants/validation`
import { MAX_CART_QTY } from '@/constants/validation';
// [zh] 导入 `@/api/modules`
import { cartApi, productApi } from '@/api/modules';
// [zh] 导入 `@/utils/product`
import { filterOnSaleProducts } from '@/utils/product';
// [zh] 导入 `@/stores/cart`
import { useCartStore } from '@/stores/cart';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `cartStore`
const cartStore = useCartStore();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 语句
const { isDesktop } = useDevice();
// [zh] 声明变量 `list`
const list = ref<any[]>([]);
// [zh] 声明变量 `pageLoading`
const pageLoading = ref(true);
// [zh] 声明变量 `loadError`
const loadError = ref('');
// [zh] 声明变量 `recommendProducts`
const recommendProducts = ref<any[]>([]);
// [zh] 声明变量 `selectedIds`
const selectedIds = ref<Set<string>>(new Set());
// [zh] 声明变量 `updatingCartId`
const updatingCartId = ref<string | null>(null);
// [zh] 声明变量 `openSwipeId`
const openSwipeId = ref<string | null>(null);

// [zh] 声明变量 `goProduct`
const goProduct = (p: any) => {
  // [zh] 条件判断 if
  if (p?.productId) router.push(`/product/${p.productId}`);
// [zh] 语句
};

// [zh] 声明变量 `loadRecommend`
const loadRecommend = async () => {
  // [zh] 条件判断 if
  if (recommendProducts.value.length) return;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `commend`
    const commend = await productApi.loadCommendProduct();
    // [zh] 声明变量 `arr`
    const arr = Array.isArray(commend) ? commend : commend?.list;
    // [zh] 声明变量 `filtered`
    const filtered = filterOnSaleProducts(arr);
    // [zh] 声明变量 `targetCount`
    const targetCount = isDesktop.value ? 12 : 6;
    // [zh] 条件判断 if
    if (isDesktop.value && filtered.length < targetCount && filtered.length > 0) {

      // [zh] 声明变量 `items`
      const items = [...filtered];
      // [zh] 代码块开始
      while (items.length < targetCount) {
        // [zh] 语句
        items.push(filtered[items.length % filtered.length]);
      // [zh] 语法符号
      }
      // [zh] 语句
      recommendProducts.value = items;
    // [zh] 代码块开始
    } else {
      // [zh] 语句
      recommendProducts.value = filtered.slice(0, targetCount);
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } catch {

  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `onSwipeClose`
const onSwipeClose = (cartId: string) => {
  // [zh] 条件判断 if
  if (openSwipeId.value === cartId) openSwipeId.value = null;
// [zh] 语句
};

// [zh] 声明变量 `qtySnapshot`
const qtySnapshot = new Map<string, number>();

// [zh] 声明变量 `getQty`
const getQty = (row: { buyCount?: number }) => Math.max(1, Number(row.buyCount) || 1);

// [zh] 声明变量 `formatUnitPrice`
const formatUnitPrice = (row: { price?: number }) => Number(row.price || 0).toFixed(2);

// [zh] 声明变量 `formatLineTotal`
const formatLineTotal = (row: { price?: number; buyCount?: number }) =>
  // [zh] 语句
  (Number(row.price || 0) * getQty(row)).toFixed(2);

// [zh] 声明变量 `priceDiff`
const priceDiff = (row: { price?: number; addPrice?: number }) => {
  // [zh] 条件判断 if
  if (row.addPrice == null || row.price == null) return 0;
  // [zh] return 返回
  return Number(row.price) - Number(row.addPrice);
// [zh] 语句
};

// [zh] 声明变量 `priceDeltaText`
const priceDeltaText = (row: { price?: number; addPrice?: number }) => {
  // [zh] 声明变量 `diff`
  const diff = priceDiff(row);
  // [zh] 条件判断 if
  if (Math.abs(diff) < 0.005) return '';
  // [zh] 声明变量 `abs`
  const abs = Math.abs(diff).toFixed(2);
  // [zh] return 返回
  return diff > 0 ? `比添加时多了${abs}元` : `比添加时少了${abs}元`;
// [zh] 语句
};

// [zh] 声明变量 `priceDeltaClass`
const priceDeltaClass = (row: { price?: number; addPrice?: number }) => {
  // [zh] 声明变量 `diff`
  const diff = priceDiff(row);
  // [zh] 条件判断 if
  if (diff > 0) return 'up';
  // [zh] 条件判断 if
  if (diff < 0) return 'down';
  // [zh] return 返回
  return '';
// [zh] 语句
};

// [zh] 声明变量 `normalizeCartRow`
const normalizeCartRow = (row: Record<string, unknown>) => ({
  // [zh] TypeScript/JS 语句：`...row,`
  ...row,
  // [zh] TypeScript/JS 语句：`buyCount: Math.max(1, Math.floor(Number(row.buyCount) |`
  buyCount: Math.max(1, Math.floor(Number(row.buyCount) || 1))
// [zh] 语句
});

// [zh] 声明变量 `selectableItems`
const selectableItems = computed(() => list.value.filter((row) => row.productOnSale));

// [zh] 声明变量 `selectedCount`
const selectedCount = computed(() =>
  // [zh] TypeScript/JS 语句：`selectableItems.value`
  selectableItems.value
    // [zh] TypeScript/JS 语句：`.filter((row) => selectedIds.value.has(row.cartId))`
    .filter((row) => selectedIds.value.has(row.cartId))
    // [zh] TypeScript/JS 语句：`.reduce((sum, row) => sum + (Number(row.buyCount) || 0)`
    .reduce((sum, row) => sum + (Number(row.buyCount) || 0), 0)
// [zh] 语句
);

// [zh] 声明变量 `selectedAmount`
const selectedAmount = computed(() => {
  // [zh] 声明变量 `total`
  const total = selectableItems.value
    // [zh] TypeScript/JS 语句：`.filter((row) => selectedIds.value.has(row.cartId))`
    .filter((row) => selectedIds.value.has(row.cartId))
    // [zh] 语句
    .reduce((sum, row) => sum + Number(row.price) * (Number(row.buyCount) || 0), 0);
  // [zh] return 返回
  return total.toFixed(2);
// [zh] 语句
});

// [zh] 声明变量 `allSelectableChecked`
const allSelectableChecked = computed(
  // [zh] TypeScript/JS 语句：`() =>`
  () =>
    // [zh] TypeScript/JS 语句：`selectableItems.value.length > 0 &&`
    selectableItems.value.length > 0 &&
    // [zh] TypeScript/JS 语句：`selectableItems.value.every((row) => selectedIds.value.`
    selectableItems.value.every((row) => selectedIds.value.has(row.cartId))
// [zh] 语句
);

// [zh] 声明变量 `isIndeterminate`
const isIndeterminate = computed(() => {
  // [zh] 声明变量 `selected`
  const selected = selectableItems.value.filter((row) => selectedIds.value.has(row.cartId)).length;
  // [zh] return 返回
  return selected > 0 && selected < selectableItems.value.length;
// [zh] 语句
});

// [zh] 声明变量 `syncDefaultSelection`
const syncDefaultSelection = () => {
  // [zh] 语句
  selectedIds.value = new Set(selectableItems.value.map((row) => row.cartId));
// [zh] 语句
};

// [zh] 声明变量 `onItemCheckChange`
const onItemCheckChange = (cartId: string, checked: boolean | string | number) => {
  // [zh] 语句
  toggleItem(cartId, !!checked);
// [zh] 语句
};

// [zh] 声明变量 `toggleItem`
const toggleItem = (cartId: string, checked: boolean) => {
  // [zh] 声明变量 `next`
  const next = new Set(selectedIds.value);
  // [zh] 条件判断 if
  if (checked) next.add(cartId);
  // [zh] else 分支
  else next.delete(cartId);
  // [zh] 语句
  selectedIds.value = next;
// [zh] 语句
};

// [zh] 声明变量 `toggleAll`
const toggleAll = (checked: boolean) => {
  // [zh] 条件判断 if
  if (checked) syncDefaultSelection();
  // [zh] else 分支
  else selectedIds.value = new Set();
// [zh] 语句
};

// [zh] 声明变量 `load`
const load = async () => {
  // [zh] 语句
  pageLoading.value = true;
  // [zh] 语句
  loadError.value = '';
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `ok`
    const ok = await authStore.ensureSession();
    // [zh] 条件判断 if
    if (!ok) {
      // [zh] 语句
      list.value = [];
      // [zh] 语句
      selectedIds.value = new Set();
      // [zh] 语句
      cartStore.resetCart();
      // [zh] 语句
      return;
    // [zh] 语法符号
    }

    // [zh] 声明变量 `res`
    const res = await cartApi.loadProductCart({ pageNo: 1 });
    // [zh] 声明变量 `rows`
    const rows = Array.isArray(res?.list) ? res.list : [];
    // [zh] 语句
    list.value = rows.map((row: Record<string, unknown>) => normalizeCartRow(row));
    // [zh] 语句
    syncDefaultSelection();
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await cartStore.fetchCartCount();
  // [zh] 代码块开始
  } catch (e: any) {
    // [zh] 语句
    loadError.value = e?.info || '购物车加载失败，请重试';
    // [zh] 语句
    list.value = [];
    // [zh] 语句
    selectedIds.value = new Set();
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    pageLoading.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => authStore.userInfo?.userId,`
  () => authStore.userInfo?.userId,
  // [zh] 代码块开始
  (userId, prevUserId) => {
    // [zh] 条件判断 if
    if (userId !== prevUserId) {
      // [zh] 语句
      list.value = [];
      // [zh] 语句
      selectedIds.value = new Set();
      // [zh] 条件判断 if
      if (userId) void load();
      // [zh] else 分支
      else cartStore.resetCart();
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
// [zh] 语句
);

// [zh] 声明变量 `decreaseQty`
const decreaseQty = (row: Record<string, any>) => {
  // [zh] 语句
  void changeQty(row, getQty(row) - 1);
// [zh] 语句
};

// [zh] 声明变量 `increaseQty`
const increaseQty = (row: Record<string, any>) => {
  // [zh] 语句
  void changeQty(row, getQty(row) + 1);
// [zh] 语句
};

// [zh] 声明变量 `onQtyFocus`
const onQtyFocus = (row: Record<string, any>) => {
  // [zh] 语句
  qtySnapshot.set(String(row.cartId), getQty(row));
// [zh] 语句
};

// [zh] 声明变量 `commitQtyInput`
const commitQtyInput = (row: Record<string, any>) => {
  // [zh] 声明变量 `cartId`
  const cartId = String(row.cartId);
  // [zh] 声明变量 `prev`
  const prev = qtySnapshot.get(cartId) ?? getQty(row);
  // [zh] 语句
  qtySnapshot.delete(cartId);

  // [zh] 声明变量 `count`
  let count = Math.floor(Number(row.buyCount));
  // [zh] 条件判断 if
  if (!Number.isFinite(count) || count < 1) {
    // [zh] 语句
    row.buyCount = prev;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (count > MAX_CART_QTY) {
    // [zh] 语句
    count = MAX_CART_QTY;
    // [zh] 语句
    toast.warning(`单个商品最多购买 ${MAX_CART_QTY} 件`);
  // [zh] 语法符号
  }
  // [zh] 语句
  row.buyCount = count;
  // [zh] 语句
  void changeQty(row, count, prev);
// [zh] 语句
};

// [zh] 声明变量 `changeQty`
const changeQty = async (
  // [zh] TypeScript/JS 语句：`row: Record<string, any>,`
  row: Record<string, any>,
  // [zh] TypeScript/JS 语句：`val: number | undefined,`
  val: number | undefined,
  // [zh] TypeScript/JS 语句：`prevOverride?: number`
  prevOverride?: number
// [zh] 代码块开始
) => {
  // [zh] 条件判断 if
  if (!row.productOnSale || updatingCartId.value) return;

  // [zh] 声明变量 `prev`
  const prev = prevOverride ?? (Number(row.buyCount) || 1);
  // [zh] 声明变量 `count`
  let count = Math.floor(Number(val) || 0);

  // [zh] 条件判断 if
  if (count < 1) count = 1;
  // [zh] 条件判断 if
  if (count > MAX_CART_QTY) {
    // [zh] 语句
    count = MAX_CART_QTY;
    // [zh] 条件判断 if
    if (prev < MAX_CART_QTY) toast.warning(`单个商品最多购买 ${MAX_CART_QTY} 件`);
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (count === prev) {
    // [zh] 语句
    row.buyCount = count;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }

  // [zh] 声明变量 `delta`
  const delta = count - prev;
  // [zh] 条件判断 if
  if (delta === 0) return;

  // [zh] 语句
  updatingCartId.value = row.cartId;
  // [zh] 代码块开始
  try {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await cartApi.add2Cart({
      // [zh] TypeScript/JS 语句：`productId: row.productId,`
      productId: row.productId,
      // [zh] TypeScript/JS 语句：`propertyValueIds: row.propertyValueIds,`
      propertyValueIds: row.propertyValueIds,
      // [zh] TypeScript/JS 语句：`buyCount: delta`
      buyCount: delta
    // [zh] 语句
    });
    // [zh] 语句
    row.buyCount = prev + delta;
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await cartStore.fetchCartCount();
  // [zh] 代码块开始
  } catch {
    // [zh] 语句
    row.buyCount = prev;
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    updatingCartId.value = null;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `del`
const del = async (id: string) => {
  // [zh] 声明变量 `ok`
  const ok = await confirmAction('确定要将该商品移出购物车吗？', {
    // [zh] TypeScript/JS 语句：`title: '移出购物车',`
    title: '移出购物车',
    // [zh] TypeScript/JS 语句：`confirmButtonText: '移出'`
    confirmButtonText: '移出'
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (!ok) return;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await cartApi.deleteCart(id);
  // [zh] 条件判断 if
  if (openSwipeId.value === id) openSwipeId.value = null;
  // [zh] 语句
  toast.success('已移出购物车');
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await load();
// [zh] 语句
};

// [zh] 声明变量 `checkout`
const checkout = () => {
  // [zh] 声明变量 `rows`
  const rows = selectableItems.value.filter((row) => selectedIds.value.has(row.cartId));
  // [zh] 条件判断 if
  if (!rows.length) {
    // [zh] 语句
    toast.warning('请先勾选要结算的商品');
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `items`
  const items: CheckoutLineItem[] = rows.map((row) => ({
    // [zh] TypeScript/JS 语句：`cartId: row.cartId,`
    cartId: row.cartId,
    // [zh] TypeScript/JS 语句：`productId: row.productId,`
    productId: row.productId,
    // [zh] TypeScript/JS 语句：`productName: row.productName,`
    productName: row.productName,
    // [zh] TypeScript/JS 语句：`productCover: row.productCover,`
    productCover: row.productCover,
    // [zh] TypeScript/JS 语句：`propertyValueIds: row.propertyValueIds,`
    propertyValueIds: row.propertyValueIds,
    // [zh] TypeScript/JS 语句：`propertyValueIdHash: row.propertyValueIdHash,`
    propertyValueIdHash: row.propertyValueIdHash,
    // [zh] TypeScript/JS 语句：`propertyData: row.propertyData,`
    propertyData: row.propertyData,
    // [zh] TypeScript/JS 语句：`price: Number(row.price),`
    price: Number(row.price),
    // [zh] TypeScript/JS 语句：`buyCount: Number(row.buyCount) || 1`
    buyCount: Number(row.buyCount) || 1
  // [zh] 语句
  }));
  // [zh] 语句
  saveCheckoutSession(items, 1);
  // [zh] 语句
  router.push('/checkout');
// [zh] 语句
};

// [zh] 代码块开始
onMounted(() => {
  // [zh] 语句
  load();
  // [zh] 语句
  loadRecommend();
// [zh] 语句
});
// [zh] 语句
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
