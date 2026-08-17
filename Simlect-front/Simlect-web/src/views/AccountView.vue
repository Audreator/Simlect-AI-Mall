<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="account-page" :class="{ ignore: isDesktop, 'user-center-layout': isDesktop }">
    <!-- [zh] 开始标签 `<PcUserSidebar>` -->
    <PcUserSidebar v-if="isDesktop" />
    <!-- [zh] 开始标签 `<div>` -->
    <div :class="isDesktop ? 'user-center-content account-dashboard' : 'account-body'">
    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="!isDesktop" class="simlect-user-top">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="simlect-user-head">
        <!-- [zh] 开始标签 `<h1>` -->
        <h1 class="simlect-user-title">个人中心</h1>
        <!-- [zh] 开始标签 `<button>` -->
        <button
          type="button"
          class="simlect-user-setting"
          aria-label="设置"
          @click="router.push('/account/manage')"
        >
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon :size="20"><Setting /></el-icon>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="profile-card">
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="profile-main" @click="goProfile">
        <!-- [zh] 开始标签 `<UserAvatar>` -->
        <UserAvatar :avatar="user?.avatar" :size="56" />
        <!-- [zh] 开始标签 `<div>` -->
        <div class="profile-info">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="nick-row">
            <!-- [zh] 开始标签 `<h2>` -->
            <h2 class="nick">{{ user?.nickName || '简选用户' }}</h2>
            <!-- [zh] 开始标签 `<RouterLink>` -->
            <RouterLink v-if="memberProfile" to="/member-center" class="level-tag" :class="levelTagClass" @click.stop>
              <!-- [zh] Mustache 插值表达式 -->
              {{ memberProfile.levelName || '普通会员' }}
            <!-- [zh] 闭合标签 `</RouterLink>` -->
            </RouterLink>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="account">{{ user?.email || '完善资料享更多权益' }}</p>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="memberProfile" class="exp-bar-wrap" @click.stop="router.push('/member-center')">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="exp-bar">
              <!-- [zh] 开始标签 `<div>` -->
              <div class="exp-bar-fill" :style="{ width: growthPercent + '%' }"></div>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="exp-bar-text">{{ memberProfile.growthValue ?? 0 }}/{{ nextLevelGrowth }}</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon class="profile-arrow"><ArrowRight /></el-icon>
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="wallet-strip-top">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="wallet-item-top" @click="router.push('/orders')">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="wallet-value-top">{{ totalOrderCount }}</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="wallet-label-top">订单</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="wallet-item-top" @click="router.push('/wishlist')">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="wallet-value-top">{{ wishlistCount }}</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="wallet-label-top">收藏</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="wallet-item-top" @click="router.push('/my-coupons')">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="wallet-value-top">{{ couponCount }}</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="wallet-label-top">优惠券</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<MemberSummaryCard>` -->
    <MemberSummaryCard
      v-if="isDesktop && memberProfile"
      :profile="memberProfile"
      :claimable-count="memberClaimableCount"
      :next-level-growth="memberNextLevelGrowth"
      :growth-to-next="memberGrowthToNext"
      class="member-summary--pc"
    />

    <!-- [zh] 开始标签 `<section>` -->
    <section class="order-card card">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="card-head">
        <!-- [zh] 开始标签 `<h3>` -->
        <h3>我的订单</h3>
        <!-- [zh] 开始标签 `<RouterLink>` -->
        <RouterLink to="/orders" class="link-more">全部订单</RouterLink>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="order-grid">
        <!-- [zh] 开始标签 `<button>` -->
        <button
          v-for="item in orderTabs"
          :key="item.code"
          type="button"
          class="order-tab"
          @click="goOrders(item.status)"
        >
          <!-- [zh] 开始标签 `<el-badge>` -->
          <el-badge :value="countMap[item.code] || 0" :hidden="!countMap[item.code]" :max="99">
            <!-- [zh] 开始标签 `<el-icon>` -->
            <el-icon :size="26"><component :is="item.icon" /></el-icon>
          <!-- [zh] 闭合标签 `</el-badge>` -->
          </el-badge>
          <!-- [zh] 开始标签 `<span>` -->
          <span>{{ item.name }}</span>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="menu-card card">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="menu-title">常用服务</h3>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="menu-grid">
        <!-- [zh] 开始标签 `<RouterLink>` -->
        <RouterLink v-for="m in menus" :key="m.path" :to="m.path" class="menu-item">
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon :size="24" class="menu-icon"><component :is="m.icon" /></el-icon>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="menu-label">{{ m.label }}</span>
        <!-- [zh] 闭合标签 `</RouterLink>` -->
        </RouterLink>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="discover-card card">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="discover-tabs toolbar-row">
        <!-- [zh] 开始标签 `<button>` -->
        <button
          type="button"
          class="discover-tab"
          :class="{ active: discoverTab === 'recommend' }"
          @click="discoverTab = 'recommend'"
        >
          <!-- [zh] 模板内容：`推荐` -->
          推荐
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
        <!-- [zh] 开始标签 `<button>` -->
        <button
          type="button"
          class="discover-tab"
          :class="{ active: discoverTab === 'reviews' }"
          @click="switchToReviews"
        >
          <!-- [zh] 模板内容：`我的评价` -->
          我的评价
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="discoverTab === 'recommend'" class="discover-body">
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="recommendProducts.length" :class="isDesktop ? 'pc-recommend-grid' : 'recommend-grid'">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-if="isDesktop">
            <!-- [zh] 开始标签 `<PcProductTile>` -->
            <PcProductTile
              v-for="item in recommendProducts"
              :key="`${item.product.productId}-${item.displayIndex}`"
              :product="item.product"
              @click="goProduct"
            />
          </template>
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-else>
            <!-- [zh] 开始标签 `<ProductCard>` -->
            <ProductCard
              v-for="item in recommendProducts"
              :key="`${item.product.productId}-${item.displayIndex}`"
              :product="item.product"
              compact
              @click="goProduct"
            />
          </template>
        </div>
        <el-empty v-else-if="!discoverLoading" description="暂无推荐商品" :image-size="72" />
        <p v-if="discoverLoading" class="discover-tip">加载中…</p>
        <div ref="recommendSentinel" class="feed-sentinel">
          <span v-if="discoverLoading" class="feed-tip">加载中…</span>
          <span v-else-if="recommendProducts.length >= MAX_RECOMMEND" class="feed-tip">已展示全部推荐商品</span>
          <span v-else-if="recommendSourceProducts.length === 0 && !discoverLoading" class="feed-tip">暂无推荐商品</span>
        </div>
      </div>

      <div v-else class="discover-body">
        <div v-if="sortedComments.length" class="review-list" :class="{ 'review-grid': isDesktop }">
          <button
            v-for="c in sortedComments"
            :key="c.orderId"
            type="button"
            class="review-item"
            @click="openCommentDetail(c)"
          >
            <div class="review-head">
              <img v-if="commentCover(c)" :src="commentCover(c)" class="review-cover" alt="" />
              <p class="product-name">
                {{ c.productName || '商品' }}
                <span v-if="c.orderItems && c.orderItems.length > 1" class="more-products-btn" @click.stop="showAllProducts(c)">等{{ c.orderItems.length }}件商品</span>
              </p>
              <el-rate v-if="c.star" :model-value="c.star" disabled size="small" />
            </div>
            <p class="review-text">{{ c.commentContent }}</p>
            <p v-if="c.commentBizReply" class="review-biz-reply">
              <span class="tag">商家回复</span>{{ c.commentBizReply }}
            </p>
            <div v-if="commentThumbImages(c).length" class="review-thumbs">
              <img
                v-for="(img, idx) in commentThumbImages(c)"
                :key="idx"
                :src="toCommentImg(img)"
                alt=""
              />
            </div>
            <p v-if="c.recommentContent" class="review-reply">
              <span class="tag">追评</span>{{ c.recommentContent }}
            </p>
            <p v-if="c.recommentTime || c.commentTime" class="review-time">{{ formatCommentTime(c.recommentTime || c.commentTime) }}</p>
          </button>
        </div>
        <el-empty v-else-if="!discoverLoading" description="暂无评价" :image-size="72" />
        <p v-if="discoverLoading" class="discover-tip">加载中…</p>
        <button
          v-if="sortedComments.length && !commentFinished && !discoverLoading"
          type="button"
          class="load-more-btn"
          @click="loadMoreComments"
        >
          加载更多评价
        </button>
      </div>
    </section>

    <OrderCommentPreviewDialog ref="commentPreviewRef" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import PcUserSidebar from '@/components/layout/PcUserSidebar.vue';
import { useDevice } from '@/composables/useDevice';
import {
  ArrowRight,
  Bell,
  ChatDotRound,
  Location,
  Medal,
  Present,
  Setting,
  Ticket,
  Wallet,
  Box,
  Van,
  Star
} from '@element-plus/icons-vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import MemberSummaryCard from '@/components/account/MemberSummaryCard.vue';
import ProductCard from '@/components/business/ProductCard.vue';
import PcProductTile from '@/components/pc/PcProductTile.vue';
import OrderCommentPreviewDialog from '@/components/business/OrderCommentPreviewDialog.vue';
import { resolveImageUrl, splitImagePaths } from '@/utils/image';
import { accountApi, commentApi, couponApi, favoriteApi, orderApi, productApi } from '@/api/modules';
import { useAuthStore } from '@/stores/auth';
import { filterOnSaleProducts } from '@/utils/product';
import { usePageRefresh } from '@/composables/pullRefresh';
import { ElMessageBox } from 'element-plus';

const router = useRouter();
const { isDesktop } = useDevice();
const authStore = useAuthStore();
const user = ref<Record<string, any>>({});
const countMap = reactive<Record<string, number>>({});
const memberProfile = ref<Record<string, any> | null>(null);
const memberClaimableCount = ref(0);
const memberNextLevelGrowth = ref<number | null>(null);
const memberGrowthToNext = ref<number | null>(null);
const wishlistCount = ref(0);
const couponCount = ref(0);

const discoverTab = ref<'recommend' | 'reviews'>('recommend');
const discoverLoading = ref(false);
const recommendProducts = ref<any[]>([]);
const recommendSourceProducts = ref<any[]>([]);
const recommendDisplayCount = ref(0);
const MAX_RECOMMEND = 90;
const recommendSentinel = ref<HTMLElement | null>(null);
let recommendObserver: IntersectionObserver | null = null;
const myComments = ref<any[]>([]);
const commentPageNo = ref(0);
const commentPageTotal = ref(1);
const commentFinished = ref(false);
const commentPreviewRef = ref<InstanceType<typeof OrderCommentPreviewDialog>>();

const splitCommentImages = (val: unknown) => splitImagePaths(val as string | null);

const toCommentImg = (path: string) => resolveImageUrl(path, { useThumbnail: true }) || path;

const commentCover = (c: Record<string, unknown>) => {
  const cover = c.cover as string | undefined;
  return cover ? resolveImageUrl(cover, { useThumbnail: true }) : '';
};

const commentThumbImages = (c: Record<string, unknown>) => {
  const all = [...splitCommentImages(c.commentImages), ...splitCommentImages(c.recommentImages)];
  return all.slice(0, 3);
};

const formatCommentTime = (val: unknown) => {
  if (!val) return '';
  if (typeof val === 'string') return val.replace('T', ' ').slice(0, 19);
  const d = new Date(val as string | number);
  if (Number.isNaN(d.getTime())) return String(val);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const commentSortTime = (c: Record<string, unknown>) => {
  const t = (c.recommentTime as string) || (c.commentTime as string);
  return t ? new Date(t).getTime() : 0;
};

const sortedComments = computed(() =>
  [...myComments.value].sort((a, b) => commentSortTime(b) - commentSortTime(a))
);

const totalOrderCount = computed(() => countMap['completed'] || 0);

const levelTagClass = computed(() => {
  const code = Number(memberProfile.value?.levelCode ?? 1);
  if (code >= 3) return 'level-gold';
  if (code >= 2) return 'level-silver';
  return 'level-default';
});

const nextLevelGrowth = computed(() => {
  const current = memberProfile.value?.growthValue ?? 0;
  const toNext = memberGrowthToNext.value ?? 0;
  return current + toNext;
});

const growthPercent = computed(() => {
  const current = memberProfile.value?.growthValue ?? 0;
  const total = nextLevelGrowth.value;
  if (total <= 0) return 0;
  return Math.min(Math.round((current / total) * 100), 100);
});

const openCommentDetail = (c: Record<string, unknown>) => {
  commentPreviewRef.value?.show(c);
};

const showAllProducts = (row: Record<string, any>) => {
  const items = row.orderItems || []
  let html = '<div style="max-height:400px;overflow-y:auto;">'
  items.forEach((item: Record<string, any>, idx: number) => {
    const cover = item.cover ? `<img src="${resolveImageUrl(item.cover)}" style="width:60px;height:60px;object-fit:cover;border-radius:6px;flex-shrink:0;" />` : ''
    html += `<div style="display:flex;gap:12px;padding:10px 0;${idx > 0 ? 'border-top:1px solid #eee;' : ''}">
      ${cover}
      <div style="flex:1;min-width:0;">
        <div style="font-size:14px;font-weight:500;margin-bottom:4px;color:#333333;">${item.productName || ''}</div>
        <div style="font-size:12px;color:#86868b;">${item.propertyInfo || ''}</div>
        <div style="font-size:12px;color:#86868b;margin-top:2px;">￥${item.itemAmount || 0} × ${item.buyCount || 0}</div>
      </div>
    </div>`
  })
  html += '</div>'
  try {
    ElMessageBox.alert(html, '该订单商品', {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭',
      showCancelButton: false,
      closeOnClickModal: true,
    })
  } catch (e) {
    console.error(e)
  }
}

const orderTabs = [
  { code: 'pendingPayment', name: '待付款', status: '0', icon: Wallet },
  { code: 'pendingShipment', name: '待发货', status: '1', icon: Box },
  { code: 'pendingReceipt', name: '待收货', status: '2', icon: Van },
  { code: 'pendingComment', name: '待评价', status: '8', icon: Star },
  { code: 'afterSale', name: '售后', status: 'all', icon: ChatDotRound }
];

const menus = [
  { label: '会员中心', path: '/member-center', icon: Medal },
  { label: '消息中心', path: '/notifications', icon: Bell },
  { label: '支付记录', path: '/pay-records', icon: Wallet },
  { label: '收货地址', path: '/address', icon: Location },
  { label: '优惠券', path: '/my-coupons', icon: Ticket },
  { label: '收藏', path: '/wishlist', icon: Star },
  { label: '足迹', path: '/footprint', icon: Box },
  { label: '签到中心', path: '/sign', icon: Present },
  { label: '智能客服', path: '/ai-assistant', icon: ChatDotRound }
];

const loadMember = async () => {
  try {
    const center: any = await authStore.loadMemberCenter();
    if (!center) return;
    memberProfile.value = center?.profile ?? null;
    const rewards = center?.rewards || [];
    memberClaimableCount.value = rewards.filter((r: { claimable?: boolean }) => r.claimable).length;
    memberNextLevelGrowth.value = center?.nextLevelGrowth ?? null;
    memberGrowthToNext.value = center?.growthToNext ?? null;
  } catch {
    memberProfile.value = null;
    memberClaimableCount.value = 0;
    memberNextLevelGrowth.value = null;
    memberGrowthToNext.value = null;
  }
};

const load = async () => {
  user.value = (await accountApi.getUserInfo()) || authStore.userInfo || {};
  authStore.userInfo = { ...authStore.userInfo, ...user.value };
  const counts = await orderApi.getOrderCountInfo();
  if (Array.isArray(counts)) {
    counts.forEach((item: { code: string; count: number }) => {
      countMap[item.code] = item.count ?? 0;
    });
  }
  if (authStore.isLoggedIn) {
    await loadMember();

    try {
      const [favRes, couponRes] = await Promise.all([
        favoriteApi.loadFavorite({ pageNo: 1 }),
        couponApi.loadUserCoupon({ pageNo: 1, status: 0 })
      ]);
      wishlistCount.value = favRes?.list?.length ?? 0;
      couponCount.value = couponRes?.list?.length ?? 0;
    } catch {
      wishlistCount.value = 0;
      couponCount.value = 0;
    }
  }
};

const loadRecommend = async () => {
  discoverLoading.value = true;
  try {
    const commend = await productApi.loadCommendProduct();
    const list = Array.isArray(commend) ? commend : commend?.list;
    recommendSourceProducts.value = filterOnSaleProducts(list);

    const initialCount = isDesktop.value ? 12 : 8;
    recommendDisplayCount.value = Math.min(initialCount, MAX_RECOMMEND);
    updateRecommendDisplay();
  } finally {
    discoverLoading.value = false;
  }
};

const updateRecommendDisplay = () => {
  if (recommendSourceProducts.value.length === 0) {
    recommendProducts.value = [];
    return;
  }

  const result: { product: any; displayIndex: number }[] = [];
  const sourceLength = recommendSourceProducts.value.length;

  for (let i = 0; i < recommendDisplayCount.value; i++) {
    const sourceIndex = i % sourceLength;
    result.push({ product: recommendSourceProducts.value[sourceIndex], displayIndex: i });
  }

  recommendProducts.value = result;
};

const loadMoreRecommend = () => {
  if (recommendDisplayCount.value >= MAX_RECOMMEND) return;

  const increment = isDesktop.value ? 12 : 8;
  recommendDisplayCount.value = Math.min(recommendDisplayCount.value + increment, MAX_RECOMMEND);
  updateRecommendDisplay();
};

const setupRecommendObserver = () => {
  if (recommendObserver) return;

  recommendObserver = new IntersectionObserver(
    (entries) => {
      const entry = entries[0];
      if (entry.isIntersecting && !discoverLoading.value && recommendDisplayCount.value < MAX_RECOMMEND) {
        loadMoreRecommend();
      }
    },
    {
      rootMargin: '200px',
      threshold: 0.1
    }
  );

  if (recommendSentinel.value) {
    recommendObserver.observe(recommendSentinel.value);
  }
};

const cleanupRecommendObserver = () => {
  if (recommendObserver) {
    recommendObserver.disconnect();
    recommendObserver = null;
  }
};

const loadComments = async (reset = false) => {
  if (reset) {
    commentPageNo.value = 0;
    commentPageTotal.value = 1;
    commentFinished.value = false;
    myComments.value = [];
  }
  if (commentFinished.value) return;
  discoverLoading.value = true;
  try {
    const next = commentPageNo.value + 1;
    const r = await commentApi.loadMyComment({ pageNo: next });
    const chunk = r?.list || [];
    if (next === 1) myComments.value = chunk;
    else myComments.value = myComments.value.concat(chunk);
    commentPageNo.value = r?.pageNo ?? next;
    commentPageTotal.value = r?.pageTotal ?? commentPageNo.value;
    commentFinished.value = commentPageNo.value >= commentPageTotal.value;
  } finally {
    discoverLoading.value = false;
  }
};

const switchToReviews = () => {
  discoverTab.value = 'reviews';
  if (!myComments.value.length && !commentFinished.value) {
    loadComments(true);
  }
};

const loadMoreComments = () => loadComments(false);

const goProfile = () => router.push('/account/profile');
const goProduct = (p: any) => {
  if (p?.productId) router.push(`/product/${p.productId}`);
};

const goOrders = (status: string) => {
  if (status === 'all') {
    router.push('/after-sale');
    return;
  }
  router.push({ path: '/orders', query: { status } });
};

const refreshPage = async () => {
  await load();
  if (authStore.isLoggedIn) await loadMember();
  if (discoverTab.value === 'reviews') {
    await loadComments(true);
  } else {
    await loadRecommend();
  }
};

onMounted(async () => {
  await load();
  await loadRecommend();

  setTimeout(() => {
    setupRecommendObserver();
  }, 100);
});

onUnmounted(() => {
  cleanupRecommendObserver();
});
usePageRefresh(refreshPage);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.account-page {` */
.account-page {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin: 0 (-$app-page-gutter);` */
  margin: 0 (-$app-page-gutter);
  /* [zh] 样式规则 `padding: 0 0 16px;` */
  padding: 0 0 16px;
  /* [zh] 样式规则 `background: linear-gradient(180deg, #FFF` */
  background: linear-gradient(180deg, #FFFFFF 0%, $color-bg 120px);

  /* [zh] 样式规则 `&.user-center-layout {` */
  &.user-center-layout {
    /* [zh] 样式规则 `flex-direction: row;` */
    flex-direction: row;
    /* [zh] 样式规则 `align-items: flex-start;` */
    align-items: flex-start;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `gap: 16px;` */
    gap: 16px;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.member-summary--pc) {` */
  :deep(.member-summary--pc) {
    /* [zh] 样式规则 `margin: 0 0 12px;` */
    margin: 0 0 12px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.profile-card {` */
.profile-card {
  /* [zh] 样式规则 `margin: 0 $app-page-gutter;` */
  margin: 0 $app-page-gutter;
  /* [zh] 样式规则 `padding: 0 4px 12px;` */
  padding: 0 4px 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.profile-main {` */
.profile-main {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;
  /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
  -webkit-tap-highlight-color: transparent;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `opacity: 0.92;` */
    opacity: 0.92;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.profile-info {` */
.profile-info {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;

  /* [zh] 样式规则 `.nick-row {` */
  .nick-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `margin-bottom: 2px;` */
    margin-bottom: 2px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.nick {` */
  .nick {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 18px;` */
    font-size: 18px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.level-tag {` */
  .level-tag {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `padding: 1px 8px;` */
    padding: 1px 8px;
    /* [zh] 样式规则 `border-radius: $radius-pill;` */
    border-radius: $radius-pill;
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
    /* [zh] 样式规则 `line-height: 1.6;` */
    line-height: 1.6;

    /* [zh] 样式规则 `&.level-default {` */
    &.level-default {
      /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.15);` */
      background: rgba(255, 255, 255, 0.15);
      /* [zh] 样式规则 `color: rgba(255, 255, 255, 0.85);` */
      color: rgba(255, 255, 255, 0.85);
      /* [zh] 样式规则 `border: 1px solid rgba(255, 255, 255, 0.` */
      border: 1px solid rgba(255, 255, 255, 0.2);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.level-silver {` */
    &.level-silver {
      /* [zh] 样式规则 `background: linear-gradient(135deg, #d4d` */
      background: linear-gradient(135deg, #d4d4d8, #a8a8ad);
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `border: 1px solid rgba(168, 168, 173, 0.` */
      border: 1px solid rgba(168, 168, 173, 0.45);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.level-gold {` */
    &.level-gold {
      /* [zh] 样式规则 `background: linear-gradient(135deg, #e8c` */
      background: linear-gradient(135deg, #e8c96a, #c9a962);
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `border: 1px solid rgba(201, 169, 98, 0.4` */
      border: 1px solid rgba(201, 169, 98, 0.45);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.account {` */
  .account {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.exp-bar-wrap {` */
.exp-bar-wrap {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `margin-top: 6px;` */
  margin-top: 6px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `.exp-bar {` */
  .exp-bar {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `height: 6px;` */
    height: 6px;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.15);` */
    background: rgba(255, 255, 255, 0.15);
    /* [zh] 样式规则 `border-radius: 3px;` */
    border-radius: 3px;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `min-width: 60px;` */
    min-width: 60px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.exp-bar-fill {` */
  .exp-bar-fill {
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `background: linear-gradient(90deg, #ffd7` */
    background: linear-gradient(90deg, #ffd700, #c9a962);
    /* [zh] 样式规则 `border-radius: 3px;` */
    border-radius: 3px;
    /* [zh] 样式规则 `transition: width 0.6s ease;` */
    transition: width 0.6s ease;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.exp-bar-text {` */
  .exp-bar-text {
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `color: rgba(255, 255, 255, 0.55);` */
    color: rgba(255, 255, 255, 0.55);
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.profile-arrow {` */
.profile-arrow {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `font-size: 16px;` */
  font-size: 16px;
  /* [zh] 样式规则 `color: $color-text-disabled;` */
  color: $color-text-disabled;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.wallet-strip-top {` */
.wallet-strip-top {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `margin: 8px $app-page-gutter 0;` */
  margin: 8px $app-page-gutter 0;
  /* [zh] 样式规则 `padding: 14px 0 4px;` */
  padding: 14px 0 4px;
  /* [zh] 样式规则 `border-top: 1px solid rgba(255, 255, 255` */
  border-top: 1px solid rgba(255, 255, 255, 0.1);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.wallet-item-top {` */
.wallet-item-top {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 2px;` */
  gap: 2px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `opacity: 0.8;` */
    opacity: 0.8;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.wallet-value-top {` */
.wallet-value-top {
  /* [zh] 样式规则 `font-size: 18px;` */
  font-size: 18px;
  /* [zh] 样式规则 `font-weight: 700;` */
  font-weight: 700;
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `line-height: 1.3;` */
  line-height: 1.3;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.wallet-label-top {` */
.wallet-label-top {
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `color: rgba(255, 255, 255, 0.6);` */
  color: rgba(255, 255, 255, 0.6);
  /* [zh] 样式规则 `line-height: 1.2;` */
  line-height: 1.2;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.order-card,
/* [zh] 样式声明 */
.menu-card,
/* [zh] 样式规则 `.discover-card {` */
.discover-card {
  /* [zh] 样式规则 `margin: 0 $app-page-gutter;` */
  margin: 0 $app-page-gutter;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-card {` */
.order-card {
  /* [zh] 样式规则 `padding: 14px 16px 16px;` */
  padding: 14px 16px 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.card-head {` */
.card-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `margin-bottom: 16px;` */
  margin-bottom: 16px;

  /* [zh] 样式规则 `h3 {` */
  h3 {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.link-more {` */
  .link-more {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-grid {` */
.order-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(5, 1fr);` */
  grid-template-columns: repeat(5, 1fr);
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-tab {` */
.order-tab {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 10px 4px;` */
  padding: 10px 4px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `border-radius: $radius-btn;` */
  border-radius: $radius-btn;
  /* [zh] 样式规则 `transition: background $transition-fast,` */
  transition: background $transition-fast, color $transition-fast, transform $transition-fast;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `background: $color-primary-muted;` */
    background: $color-primary-muted;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.96);` */
    transform: scale(0.96);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.menu-card {` */
.menu-card {
  /* [zh] 样式规则 `padding: 4px 0;` */
  padding: 4px 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.menu-title {` */
.menu-title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 14px 16px 8px;` */
  padding: 14px 16px 8px;
  /* [zh] 样式规则 `font-size: 16px;` */
  font-size: 16px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.menu-grid {` */
.menu-grid {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 0;` */
  gap: 0;
  /* [zh] 样式规则 `padding: 4px 0 8px;` */
  padding: 4px 0 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.menu-item {` */
.menu-item {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: row;` */
  flex-direction: row;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `padding: 12px 16px;` */
  padding: 12px 16px;
  /* [zh] 样式规则 `text-decoration: none;` */
  text-decoration: none;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `border-radius: $radius-btn;` */
  border-radius: $radius-btn;
  /* [zh] 样式规则 `transition: background $transition-fast;` */
  transition: background $transition-fast;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `background: rgba($color-primary, 0.04);` */
    background: rgba($color-primary, 0.04);
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;

    /* [zh] 样式规则 `.menu-icon {` */
    .menu-icon {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.menu-label {` */
  .menu-label {
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.menu-icon {` */
  .menu-icon {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.discover-card {` */
.discover-card {
  /* [zh] 样式规则 `padding: 0 0 12px;` */
  padding: 0 0 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.discover-tabs {` */
.discover-tabs {
  /* [zh] 样式规则 `padding: 0 12px;` */
  padding: 0 12px;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.discover-tab {` */
.discover-tab {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `padding: 12px 8px;` */
  padding: 12px 8px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `position: relative;` */
  position: relative;

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;

    /* [zh] 样式规则 `&::after {` */
    &::after {
      /* [zh] 样式规则 `content: '';` */
      content: '';
      /* [zh] 样式规则 `position: absolute;` */
      position: absolute;
      /* [zh] 样式规则 `left: 50%;` */
      left: 50%;
      /* [zh] 样式规则 `bottom: 0;` */
      bottom: 0;
      /* [zh] 样式规则 `transform: translateX(-50%);` */
      transform: translateX(-50%);
      /* [zh] 样式规则 `width: 28px;` */
      width: 28px;
      /* [zh] 样式规则 `height: 3px;` */
      height: 3px;
      /* [zh] 样式规则 `border-radius: $radius-xs;` */
      border-radius: $radius-xs;
      /* [zh] 样式规则 `background: linear-gradient(90deg, $colo` */
      background: linear-gradient(90deg, $color-primary, $color-primary-hover);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.discover-body {` */
.discover-body {
  /* [zh] 样式规则 `padding: 12px 12px 4px;` */
  padding: 12px 12px 4px;
  /* [zh] 样式规则 `min-height: 120px;` */
  min-height: 120px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feed-sentinel {` */
.feed-sentinel {
  /* [zh] 样式规则 `padding: 16px 0;` */
  padding: 16px 0;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feed-tip {` */
.feed-tip {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.recommend-grid {` */
.recommend-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(2, 1fr);` */
  grid-template-columns: repeat(2, 1fr);
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.review-list {` */
.review-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 0;` */
  gap: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.review-item {` */
.review-item {
  /* [zh] 样式规则 `display: block;` */
  display: block;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `padding: 12px 4px;` */
  padding: 12px 4px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;
  /* [zh] 样式规则 `background: $color-bg;` */
  background: $color-bg;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `z-index: 1;` */
  z-index: 1;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `background: rgba($color-primary, 0.04);` */
    background: rgba($color-primary, 0.04);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  .review-cover,
  /* [zh] 样式规则 `.review-thumbs img {` */
  .review-thumbs img {
    /* [zh] 样式规则 `filter: none !important;` */
    filter: none !important;
    /* [zh] 样式规则 `opacity: 1 !important;` */
    opacity: 1 !important;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  .product-name,
  /* [zh] 样式规则 `.review-text {` */
  .review-text {
    /* [zh] 样式规则 `color: $color-text-title !important;` */
    color: $color-text-title !important;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.review-head {` */
.review-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 6px;` */
  margin-bottom: 6px;

  /* [zh] 样式规则 `.review-cover {` */
  .review-cover {
    /* [zh] 样式规则 `width: 40px;` */
    width: 40px;
    /* [zh] 样式规则 `height: 40px;` */
    height: 40px;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `object-fit: cover;` */
    object-fit: cover;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.product-name {` */
  .product-name {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
    /* [zh] 样式规则 `.more-products-btn {` */
    .more-products-btn {
      /* [zh] 样式规则 `display: inline;` */
      display: inline;
      /* [zh] 样式规则 `margin-left: 4px;` */
      margin-left: 4px;
      /* [zh] 样式规则 `padding: 1px 5px;` */
      padding: 1px 5px;
      /* [zh] 样式规则 `border-radius: $radius-xs;` */
      border-radius: $radius-xs;
      /* [zh] 样式规则 `background: $color-warning-soft;` */
      background: $color-warning-soft;
      /* [zh] 样式规则 `color: $color-warning;` */
      color: $color-warning;
      /* [zh] 样式规则 `font-size: 10px;` */
      font-size: 10px;
      /* [zh] 样式规则 `font-weight: 500;` */
      font-weight: 500;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
      /* [zh] 样式规则 `white-space: nowrap;` */
      white-space: nowrap;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.review-thumbs {` */
.review-thumbs {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `margin-bottom: 6px;` */
  margin-bottom: 6px;

  /* [zh] 样式规则 `img {` */
  img {
    /* [zh] 样式规则 `width: 56px;` */
    width: 56px;
    /* [zh] 样式规则 `height: 56px;` */
    height: 56px;
    /* [zh] 样式规则 `object-fit: cover;` */
    object-fit: cover;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.review-text {` */
.review-text {
  /* [zh] 样式规则 `margin: 0 0 6px;` */
  margin: 0 0 6px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `display: -webkit-box;` */
  display: -webkit-box;
  /* [zh] 样式规则 `-webkit-line-clamp: 3;` */
  -webkit-line-clamp: 3;
  /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
  -webkit-box-orient: vertical;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.review-biz-reply,
/* [zh] 样式规则 `.review-reply {` */
.review-reply {
  /* [zh] 样式规则 `margin: 0 0 6px;` */
  margin: 0 0 6px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `line-height: 1.45;` */
  line-height: 1.45;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;

  /* [zh] 样式规则 `.tag {` */
  .tag {
    /* [zh] 样式规则 `display: inline-block;` */
    display: inline-block;
    /* [zh] 样式规则 `margin-right: 6px;` */
    margin-right: 6px;
    /* [zh] 样式规则 `padding: 0 6px;` */
    padding: 0 6px;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `background: $color-primary-muted;` */
    background: $color-primary-muted;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.review-biz-reply {` */
.review-biz-reply {
  /* [zh] 样式规则 `padding: 8px 10px;` */
  padding: 8px 10px;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.review-time {` */
.review-time {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `color: $color-text-disabled;` */
  color: $color-text-disabled;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.discover-tip {` */
.discover-tip {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `padding: 16px 0;` */
  padding: 16px 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.load-more-btn {` */
.load-more-btn {
  /* [zh] 样式规则 `display: block;` */
  display: block;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `margin-top: 8px;` */
  margin-top: 8px;
  /* [zh] 样式规则 `padding: 10px;` */
  padding: 10px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (min-width: $breakpoint-tablet) {` */
@media (min-width: $breakpoint-tablet) {
  /* [zh] 样式规则 `.recommend-grid {` */
  .recommend-grid {
    /* [zh] 样式规则 `grid-template-columns: repeat(6, 1fr);` */
    grid-template-columns: repeat(6, 1fr);
    /* [zh] 样式规则 `gap: 10px;` */
    gap: 10px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
