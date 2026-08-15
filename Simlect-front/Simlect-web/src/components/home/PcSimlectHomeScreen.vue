<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="sl-screen-box-wrap ignore">

    <!-- [zh] 开始标签 `<aside>` -->
    <aside class="sl-cat" aria-label="商品分类" @mouseleave="hoverCategory = null">
      <!-- [zh] 开始标签 `<ul>` -->
      <ul class="sl-cat-list">
        <!-- [zh] 开始标签 `<li>` -->
        <li
          v-for="cat in topCategories"
          :key="cat.categoryId"
          class="sl-cat-item"
          :class="{ active: hoverCategory === cat.categoryId }"
          @mouseenter="hoverCategory = cat.categoryId"
        >
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="sl-cat-link" @click="goCategory(cat.categoryId)">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="sl-cat-name">{{ cat.categoryName }}</span>
            <!-- [zh] 开始标签 `<el-icon>` -->
            <el-icon v-if="cat.children?.length" class="sl-cat-arrow"><ArrowRight /></el-icon>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</li>` -->
        </li>
      <!-- [zh] 闭合标签 `</ul>` -->
      </ul>

      <!-- [zh] 开始标签 `<transition>` -->
      <transition name="sl-flyout">
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="activeFlyout" class="sl-cat-flyout">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sl-flyout-head">{{ activeFlyout.categoryName }}</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sl-flyout-body">
            <!-- [zh] 开始标签 `<button>` -->
            <button
              v-for="sub in activeFlyout.children"
              :key="sub.categoryId"
              type="button"
              class="sl-flyout-sub"
              @click="goCategory(sub.categoryId)"
            >
              <!-- [zh] Mustache 插值表达式 -->
              {{ sub.categoryName }}
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
            <!-- [zh] 开始标签 `<p>` -->
            <p v-if="!activeFlyout.children?.length" class="sl-flyout-empty">暂无子分类</p>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</transition>` -->
      </transition>
    <!-- [zh] 闭合标签 `</aside>` -->
    </aside>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="sl-stage">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="sl-banner">
        <!-- [zh] 开始标签 `<button>` -->
        <button
          v-if="bannerProduct"
          type="button"
          class="sl-banner-btn"
          @click="goDetail(bannerProduct)"
        >
          <!-- [zh] 开始标签 `<ProductImage>` -->
          <ProductImage :product="bannerProduct" fit="cover" width="100%" height="100%" class="sl-banner-bg" />
          <!-- [zh] 开始标签 `<ProductImage>` -->
          <ProductImage :product="bannerProduct" fit="contain" width="100%" height="100%" class="sl-banner-fg" />
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sl-banner-meta">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="sl-banner-name">{{ bannerProduct.productName }}</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="sl-banner-price">¥{{ formatPrice(bannerProduct) }}</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-else class="sl-banner-placeholder">
          <!-- [zh] 开始标签 `<BrandMark>` -->
          <BrandMark class="sl-banner-logo" />
          <!-- [zh] 开始标签 `<p>` -->
          <p>简选 Simlect</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="bannerList.length > 1" class="sl-banner-dots">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-for="(_, i) in bannerList"
            :key="i"
            type="button"
            :class="{ active: carouselIndex === i }"
            :aria-label="`切换到第 ${i + 1} 张`"
            @click="goBanner(i)"
          />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="sl-hot-strip">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sl-hot-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="sl-hot-badge">简选</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="sl-hot-title">简选好物</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sl-hot-grid">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-for="p in stripProducts"
            :key="p.productId"
            type="button"
            class="sl-hot-tile"
            @click="goDetail(p)"
          >
            <!-- [zh] 开始标签 `<ProductImage>` -->
            <ProductImage :product="p" fit="contain" width="100%" height="100%" />
            <!-- [zh] 开始标签 `<span>` -->
            <span class="sl-hot-price">¥{{ formatPrice(p) }}</span>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<aside>` -->
    <aside class="sl-right" aria-label="会员与服务">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="sl-member-wrap">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sl-img-box">
          <!-- [zh] 开始标签 `<el-avatar>` -->
          <el-avatar :size="54" :src="avatarUrl">{{ avatarLetter }}</el-avatar>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sl-member-info">
          <!-- [zh] Mustache 插值表达式 -->
          {{ authStore.isLoggedIn ? `Hi，${authStore.userInfo?.nickName || '用户'}` : 'Hi! 欢迎来到简选' }}
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sl-login-and-reg">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-if="!authStore.isLoggedIn">
            <!-- [zh] 开始标签 `<RouterLink>` -->
            <RouterLink to="/login">登录</RouterLink>
            <!-- [zh] 开始标签 `<RouterLink>` -->
            <RouterLink to="/register">注册</RouterLink>
          </template>
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-else>
            <!-- [zh] 开始标签 `<RouterLink>` -->
            <RouterLink to="/account">个人中心</RouterLink>
            <!-- [zh] 开始标签 `<RouterLink>` -->
            <RouterLink to="/orders">我的订单</RouterLink>
          </template>
        </div>
        <div class="sl-member-ft">
          <RouterLink to="/sign" class="t"><span class="sl-coin">◆</span>签到有礼</RouterLink>
          <RouterLink to="/member-center" class="h"><span class="sl-coin gold">◆</span>会员中心</RouterLink>
        </div>
      </div>

      <button type="button" class="sl-service-banner" @click="openAgent()">
        <el-icon :size="16"><ChatDotRound /></el-icon>
        <span>智能客服 · 在线为你解答</span>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </button>

      <div class="sl-moudle-wrap">
        <ul>
          <li v-for="item in serviceGrid" :key="item.path">
            <button type="button" @click="onServiceClick(item)">
              <el-icon :size="24"><component :is="item.icon" /></el-icon>
              <p>{{ item.label }}</p>
            </button>
          </li>
        </ul>
      </div>
    </aside>
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onUnmounted, ref, watch } from 'vue';
// [zh] 导入 `vue-router`
import { RouterLink, useRouter } from 'vue-router';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`ArrowRight,`
  ArrowRight,
  // [zh] TypeScript/JS 语句：`ChatDotRound,`
  ChatDotRound,
  // [zh] TypeScript/JS 语句：`Grid,`
  Grid,
  // [zh] TypeScript/JS 语句：`List,`
  List,
  // [zh] TypeScript/JS 语句：`Location,`
  Location,
  // [zh] TypeScript/JS 语句：`Present,`
  Present,
  // [zh] TypeScript/JS 语句：`Star,`
  Star,
  // [zh] TypeScript/JS 语句：`Ticket,`
  Ticket,
  // [zh] TypeScript/JS 语句：`Wallet`
  Wallet
// [zh] 语句
} from '@element-plus/icons-vue';
// [zh] 导入 `@/components/common/BrandMark.vue`
import BrandMark from '@/components/common/BrandMark.vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/composables/useOpenAgent`
import { useOpenAgent } from '@/composables/useOpenAgent';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/utils/image`
import { resolveAvatarUrl } from '@/utils/image';

// [zh] 声明变量 `props`
const props = defineProps<{
  // [zh] 代码块开始
  categories: Array<{
    // [zh] 语句
    categoryId: string;
    // [zh] 语句
    categoryName: string;
    // [zh] 语句
    children?: Array<{ categoryId: string; categoryName: string }>;
  // [zh] 语句
  }>;
  // [zh] 语句
  hotProducts: any[];
// [zh] 语句
}>();

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 语句
const { openAgent } = useOpenAgent();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `carouselIndex`
const carouselIndex = ref(0);
// [zh] 声明变量 `hoverCategory`
const hoverCategory = ref<string | null>(null);
// [zh] 声明变量 `carouselTimer`
let carouselTimer: ReturnType<typeof setInterval> | null = null;

// [zh] 声明变量 `avatarUrl`
const avatarUrl = computed(() => resolveAvatarUrl(authStore.userInfo?.avatar));
// [zh] 声明变量 `avatarLetter`
const avatarLetter = computed(() => (authStore.userInfo?.nickName || '访')[0]);

// [zh] 声明变量 `topCategories`
const topCategories = computed(() => props.categories.slice(0, 11));

// [zh] 声明变量 `activeFlyout`
const activeFlyout = computed(
  // [zh] TypeScript/JS 语句：`() => topCategories.value.find((c) => c.categoryId === `
  () => topCategories.value.find((c) => c.categoryId === hoverCategory.value) || null
// [zh] 语句
);

// [zh] 声明变量 `formatPrice`
const formatPrice = (p: Record<string, any>) => {
  // [zh] 声明变量 `val`
  const val = p?.minPrice ?? p?.price ?? p?.salePrice;
  // [zh] return 返回
  return val != null ? Number(val).toFixed(2) : '--';
// [zh] 语句
};

// [zh] 声明变量 `bannerList`
const bannerList = computed(() => props.hotProducts.slice(0, 5));
// [zh] 声明变量 `bannerProduct`
const bannerProduct = computed(() => bannerList.value[carouselIndex.value] || bannerList.value[0]);
// [zh] 声明变量 `stripProducts`
const stripProducts = computed(() => props.hotProducts.slice(0, 4));

// [zh] 声明变量 `startCarousel`
const startCarousel = () => {
  // [zh] 条件判断 if
  if (carouselTimer) clearInterval(carouselTimer);
  // [zh] 条件判断 if
  if (bannerList.value.length <= 1) return;
  // [zh] 代码块开始
  carouselTimer = setInterval(() => {
    // [zh] 语句
    carouselIndex.value = (carouselIndex.value + 1) % bannerList.value.length;
  // [zh] 语句
  }, 4500);
// [zh] 语句
};

// [zh] 声明变量 `goBanner`
const goBanner = (index: number) => {
  // [zh] 声明变量 `total`
  const total = bannerList.value.length;
  // [zh] 条件判断 if
  if (!total) return;
  // [zh] 语句
  carouselIndex.value = (index + total) % total;
  // [zh] 语句
  startCarousel();
// [zh] 语句
};

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => props.hotProducts.length,`
  () => props.hotProducts.length,
  // [zh] 代码块开始
  (len) => {
    // [zh] 条件判断 if
    if (len && carouselIndex.value >= len) carouselIndex.value = 0;
    // [zh] 语句
    startCarousel();
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`{ immediate: true }`
  { immediate: true }
// [zh] 语句
);

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 条件判断 if
  if (carouselTimer) clearInterval(carouselTimer);
// [zh] 语句
});

// [zh] 声明变量 `serviceGrid`
const serviceGrid = [
  // [zh] TypeScript/JS 语句：`{ label: '全部分类', path: '/search', icon: Grid },`
  { label: '全部分类', path: '/search', icon: Grid },
  // [zh] TypeScript/JS 语句：`{ label: '优惠券', path: '/coupons', icon: Ticket },`
  { label: '优惠券', path: '/coupons', icon: Ticket },
  // [zh] TypeScript/JS 语句：`{ label: '我的订单', path: '/orders', icon: List },`
  { label: '我的订单', path: '/orders', icon: List },
  // [zh] TypeScript/JS 语句：`{ label: '购物车', path: '/cart', icon: Wallet },`
  { label: '购物车', path: '/cart', icon: Wallet },
  // [zh] TypeScript/JS 语句：`{ label: '签到有礼', path: '/sign', icon: Present },`
  { label: '签到有礼', path: '/sign', icon: Present },
  // [zh] TypeScript/JS 语句：`{ label: '我的收藏', path: '/wishlist', icon: Star },`
  { label: '我的收藏', path: '/wishlist', icon: Star },
  // [zh] TypeScript/JS 语句：`{ label: '收货地址', path: '/address', icon: Location },`
  { label: '收货地址', path: '/address', icon: Location },
  // [zh] TypeScript/JS 语句：`{ label: '消息中心', path: '/notifications', icon: ChatDotR`
  { label: '消息中心', path: '/notifications', icon: ChatDotRound }
// [zh] 语句
];

// [zh] 声明变量 `goCategory`
const goCategory = (id: string) => {
  // [zh] 条件判断 if
  if (id) router.push(`/category/${id}`);
  // [zh] else 分支
  else router.push('/search');
// [zh] 语句
};

// [zh] 声明变量 `goDetail`
const goDetail = (p: any) => {
  // [zh] 条件判断 if
  if (p?.productId) router.push(`/product/${p.productId}`);
// [zh] 语句
};

// [zh] 声明变量 `onServiceClick`
const onServiceClick = (item: { path: string }) => {
  // [zh] 条件判断 if
  if (item.path === '/ai-assistant') openAgent();
  // [zh] else 分支
  else router.push(item.path);
// [zh] 语句
};
</script>
