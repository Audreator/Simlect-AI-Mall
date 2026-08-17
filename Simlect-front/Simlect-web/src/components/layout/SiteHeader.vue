<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<header>` -->
  <header class="site-header ignore" :class="{ 'is-simlect-search': simlectSearch }">

    <!-- [zh] 开始标签 `<div>` -->
    <div class="site-topbar">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="topbar-inner">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="topbar-left">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-if="authStore.isLoggedIn">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="topbar-text">欢迎您，{{ authStore.userInfo?.nickName || '用户' }}</span>
          </template>
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-else>
            <!-- [zh] 开始标签 `<RouterLink>` -->
            <RouterLink class="topbar-link" to="/login">你好，请登录</RouterLink>
            <!-- [zh] 开始标签 `<RouterLink>` -->
            <RouterLink class="topbar-link" to="/register">免费注册</RouterLink>
          </template>
        </div>
        <nav class="topbar-right" aria-label="顶部快捷入口">
          <RouterLink v-if="authStore.isLoggedIn" class="topbar-link" to="/notifications">
            消息<span v-if="unreadCount" class="topbar-badge">({{ unreadCount > 99 ? '99+' : unreadCount }})</span>
          </RouterLink>
          <RouterLink v-if="authStore.isLoggedIn" class="topbar-link topbar-link--fold" to="/member-center">会员中心</RouterLink>
          <RouterLink class="topbar-link topbar-link--fold" to="/orders">我的订单</RouterLink>
          <RouterLink class="topbar-link topbar-link--fold" to="/cart">购物车</RouterLink>
          <RouterLink class="topbar-link topbar-link--fold" to="/wishlist">收藏夹</RouterLink>
          <RouterLink class="topbar-link topbar-link--fold" to="/coupons">优惠券</RouterLink>
          <button type="button" class="topbar-link topbar-link--fold" @click="openAgent()">智能客服</button>
        </nav>
      </div>
    </div>

    <div class="site-search-row">
      <div class="search-row-inner">
        <RouterLink class="brand" to="/">
          <BrandMark class="brand-icon" />
          <span class="brand-text">简选</span>
          <span class="brand-en">Simlect</span>
        </RouterLink>

        <div class="search-block">
          <div class="search-box">
            <el-select
              v-model="searchCategoryId"
              placeholder="分类"
              class="search-category"
              popper-class="search-category-popper"
            >
              <el-option label="全部商品" value="" />
              <el-option
                v-for="c in categoryList"
                :key="c.categoryId"
                :label="c.categoryName"
                :value="c.categoryId"
              />
            </el-select>
            <div class="search-divider" />
            <input
              v-model="keyword"
              class="search-input"
              type="search"
              placeholder="搜索简选"
              @focus="openRecentPanel"
              @blur="onSearchBlur"
              @keyup.enter="goSearch"
            />
            <button type="button" class="search-submit" @click="goSearch">搜索</button>
          </div>
          <div v-if="showRecentPanel && authStore.isLoggedIn" class="recent-panel card-flat">
            <div class="recent-head">
              <span>最近搜索</span>
              <button v-if="recentWords.length" type="button" class="clear-link" @click="clearRecent">
                清空最近搜索记录
              </button>
            </div>
            <div v-if="recentWords.length" class="recent-list">
              <div v-for="word in recentWords" :key="word" class="recent-row">
                <button type="button" class="recent-word" @click="searchByWord(word)">{{ word }}</button>
                <button type="button" class="recent-del" aria-label="删除" @click="removeRecent(word)">×</button>
              </div>
            </div>
            <p v-else class="recent-empty">暂无搜索记录</p>
          </div>
          <div v-if="hotWords.length" class="hot-words">
            <button
              v-for="(word, i) in hotWords"
              :key="`hot-${i}`"
              type="button"
              class="hot-word"
              @click="searchByWord(word)"
            >
              {{ word }}
            </button>
            <button type="button" class="hot-word hot-more" @click="router.push('/search-portal')">
              更多
            </button>
          </div>
        </div>

        <nav class="header-actions">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-if="!authStore.isLoggedIn">
            <!-- [zh] 开始标签 `<RouterLink>` -->
            <RouterLink class="action-link" to="/login">请登录</RouterLink>
          </template>
          <el-dropdown v-else trigger="click" popper-class="user-dropdown-popper" :teleported="true">
            <button type="button" class="user-trigger">
              <el-avatar :size="36" :src="avatarUrl" class="user-avatar">
                {{ (authStore.userInfo?.nickName || '用')[0] }}
              </el-avatar>
              <span class="user-name">{{ authStore.userInfo?.nickName || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
            <template #dropdown>
              <!-- [zh] 开始标签 `<el-dropdown-menu>` -->
              <el-dropdown-menu>
                <!-- [zh] 开始标签 `<el-dropdown-item>` -->
                <el-dropdown-item @click="router.push('/orders')">我的订单</el-dropdown-item>
                <!-- [zh] 开始标签 `<el-dropdown-item>` -->
                <el-dropdown-item @click="router.push('/my-coupons')">我的优惠券</el-dropdown-item>
                <!-- [zh] 开始标签 `<el-dropdown-item>` -->
                <el-dropdown-item @click="router.push('/account')">个人中心</el-dropdown-item>
                <!-- [zh] 开始标签 `<el-dropdown-item>` -->
                <el-dropdown-item @click="router.push('/member-center')">会员中心</el-dropdown-item>
                <!-- [zh] 开始标签 `<el-dropdown-item>` -->
                <el-dropdown-item @click="router.push('/notifications')">
                  <!-- [zh] 模板内容：`消息中心<span v-if="unreadCount" class="menu-badg` -->
                  消息中心<span v-if="unreadCount" class="menu-badge">{{ unreadCount }}</span>
                <!-- [zh] 闭合标签 `</el-dropdown-item>` -->
                </el-dropdown-item>
                <!-- [zh] 开始标签 `<el-dropdown-item>` -->
                <el-dropdown-item @click="router.push('/sign')">签到中心</el-dropdown-item>
                <!-- [zh] 开始标签 `<el-dropdown-item>` -->
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              <!-- [zh] 闭合标签 `</el-dropdown-menu>` -->
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <button type="button" class="icon-action" title="购物车" @click="router.push('/cart')">
            <el-badge :value="cartStore.cartCount" :hidden="!cartStore.cartCount" :max="99" :offset="[-2, 8]">
              <el-icon :size="26"><ShoppingCart /></el-icon>
            </el-badge>
            <span class="icon-label">购物车</span>
          </button>

          <button
            v-if="authStore.isLoggedIn"
            type="button"
            class="icon-action"
            title="消息"
            @click="router.push('/notifications')"
          >
            <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99" :offset="[-2, 8]">
              <el-icon :size="26"><Bell /></el-icon>
            </el-badge>
            <span class="icon-label">消息</span>
          </button>
        </nav>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ArrowDown, Bell, ShoppingCart } from '@element-plus/icons-vue';
import { useUnreadCount } from '@/composables/useUnreadCount';
import { productApi, searchApi } from '@/api/modules';
import { DEFAULT_HOT_SEARCH_WORDS } from '@/constants/searchHotWords';
import { confirmAction } from '@/utils/confirm';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useSearchStore } from '@/stores/search';
import { flattenCategoryOptions, normalizeCategoryTree } from '@/utils/category';
import { resolveAvatarUrl } from '@/utils/image';
import { useOpenAgent } from '@/composables/useOpenAgent';
import BrandMark from '@/components/common/BrandMark.vue';
import { toast } from '@/utils/toast';

const props = withDefaults(
  defineProps<{
    simlectSearch?: boolean;
  }>(),
  { simlectSearch: false }
);

const router = useRouter();
const { openAgent } = useOpenAgent();
const searchStore = useSearchStore();
const authStore = useAuthStore();
const cartStore = useCartStore();
const { unreadCount } = useUnreadCount();

const keyword = ref('');
const searchCategoryId = ref('');
const categoryList = ref<any[]>([]);
const hotWords = ref<string[]>(DEFAULT_HOT_SEARCH_WORDS.slice(0, 8));
const recentWords = ref<string[]>([]);
const showRecentPanel = ref(false);

const avatarUrl = computed(() => resolveAvatarUrl(authStore.userInfo?.avatar));

const refreshRecent = async () => {
  if (!authStore.isLoggedIn) {
    recentWords.value = [];
    return;
  }
  try {
    const list = await searchApi.loadRecentKeywords();
    recentWords.value = Array.isArray(list) ? list.slice(0, 10) : [];
  } catch {
    recentWords.value = [];
  }
};

const openRecentPanel = async () => {
  showRecentPanel.value = true;
  await refreshRecent();
};

const closeRecentPanel = () => {
  showRecentPanel.value = false;
};

const onSearchBlur = () => {
  window.setTimeout(() => closeRecentPanel(), 180);
};

const goSearch = async () => {
  const keyWords = keyword.value.trim();
  if (!keyWords) {
    router.push('/search-portal');
    return;
  }
  if (authStore.isLoggedIn) {
    try {
      await searchApi.saveKeyword(keyWords);
    } catch {

    }
  }
  showRecentPanel.value = false;
  searchStore.setSearch({
    keyWords,
    categoryId: searchCategoryId.value || ''
  });
  router.push({ path: '/search-result', query: { q: keyWords } });
};

const searchByWord = (word: string) => {
  keyword.value = word;
  goSearch();
};

const clearRecent = async () => {
  const ok = await confirmAction('确定要清空全部最近搜索记录吗？', {
    title: '清空记录',
    confirmButtonText: '清空'
  });
  if (!ok) return;
  await searchApi.clearRecentKeywords();
  await refreshRecent();
};

const removeRecent = async (word: string) => {
  await searchApi.removeRecentKeyword(word);
  await refreshRecent();
};

const logout = async () => {
  const ok = await confirmAction('确定要退出当前账号吗？', {
    title: '退出登录',
    confirmButtonText: '退出'
  });
  if (!ok) return;
  authStore.prepareLogoutNavigation();
  await authStore.logout();
  toast.success('已退出登录');
  await router.replace({ path: '/login', query: {} });
};

onMounted(async () => {
  const cats = await productApi.loadCategory();
  categoryList.value = flattenCategoryOptions(normalizeCategoryTree(cats || []), 'child');
  if (authStore.isLoggedIn) cartStore.fetchCartCount();
  try {
    const hot = await searchApi.loadHotKeywords();
    if (Array.isArray(hot) && hot.length) hotWords.value = hot.slice(0, 8);
  } catch {

  }
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.site-header.ignore {` */
.site-header.ignore {
  /* [zh] 样式规则 `position: sticky;` */
  position: sticky;
  /* [zh] 样式规则 `top: 0;` */
  top: 0;
  /* [zh] 样式规则 `z-index: 1000;` */
  z-index: 1000;
  background: var(--glass-bg-header, #{$color-card});
  /* [zh] 样式规则 `-webkit-backdrop-filter: var(--glass-blu` */
  -webkit-backdrop-filter: var(--glass-blur);
  /* [zh] 样式规则 `backdrop-filter: var(--glass-blur);` */
  backdrop-filter: var(--glass-blur);
  /* [zh] 样式规则 `box-shadow: 0 4px 12px rgba(15, 23, 42, ` */
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);

  /* [zh] 样式规则 `.site-topbar {` */
  .site-topbar {
    /* [zh] 样式规则 `height: $pc-topbar-height;` */
    height: $pc-topbar-height;
    /* [zh] 样式规则 `background: rgba(245, 245, 247, 0.55);` */
    background: rgba(245, 245, 247, 0.55);
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.topbar-inner {` */
  .topbar-inner {
    /* [zh] 样式规则 `max-width: $content-width;` */
    max-width: $content-width;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `margin: 0 auto;` */
    margin: 0 auto;
    /* [zh] 样式规则 `padding: 0 16px;` */
    padding: 0 16px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.topbar-left {` */
  .topbar-left {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 10px;` */
    gap: 10px;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `.topbar-right {` */
  .topbar-right {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 16px;` */
    gap: 16px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.topbar-text {` */
  .topbar-text {
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.topbar-link {` */
  .topbar-link {
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
    /* [zh] 样式规则 `transition: color $transition-fast;` */
    transition: color $transition-fast;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `font-family: inherit;` */
    font-family: inherit;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.topbar-badge {` */
  .topbar-badge {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.menu-badge {` */
  .menu-badge {
    /* [zh] 样式规则 `margin-left: 6px;` */
    margin-left: 6px;
    /* [zh] 样式规则 `padding: 0 6px;` */
    padding: 0 6px;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
    /* [zh] 样式规则 `background: $color-price;` */
    background: $color-price;
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `line-height: 18px;` */
    line-height: 18px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.site-search-row {` */
  .site-search-row {
    /* [zh] 样式规则 `height: $pc-search-row-height;` */
    height: $pc-search-row-height;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-row-inner {` */
  .search-row-inner {
    /* [zh] 样式规则 `max-width: $content-width;` */
    max-width: $content-width;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `margin: 0 auto;` */
    margin: 0 auto;
    /* [zh] 样式规则 `padding: 0 16px;` */
    padding: 0 16px;
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `grid-template-columns: auto minmax($pc-s` */
    grid-template-columns: auto minmax($pc-search-min-width, 620px) auto;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `column-gap: 16px;` */
    column-gap: 16px;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand {` */
  .brand {
    /* [zh] 样式规则 `grid-column: 1;` */
    grid-column: 1;
    /* [zh] 样式规则 `justify-self: start;` */
    justify-self: start;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;

    /* [zh] 样式规则 `.brand-icon {` */
    .brand-icon {
      /* [zh] 样式规则 `width: 38px;` */
      width: 38px;
      /* [zh] 样式规则 `height: 38px;` */
      height: 38px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.brand-text {` */
    .brand-text {
      /* [zh] 样式规则 `font-size: 21px;` */
      font-size: 21px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `letter-spacing: 0.12em;` */
      letter-spacing: 0.12em;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.brand-en {` */
    .brand-en {
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
      /* [zh] 样式规则 `font-weight: 500;` */
      font-weight: 500;
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
      /* [zh] 样式规则 `letter-spacing: 0.14em;` */
      letter-spacing: 0.14em;
      /* [zh] 样式规则 `text-transform: uppercase;` */
      text-transform: uppercase;
      /* [zh] 样式规则 `margin-left: -4px;` */
      margin-left: -4px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-block {` */
  .search-block {
    /* [zh] 样式规则 `grid-column: 2;` */
    grid-column: 2;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `min-width: $pc-search-min-width;` */
    min-width: $pc-search-min-width;
    /* [zh] 样式规则 `max-width: 620px;` */
    max-width: 620px;
    /* [zh] 样式规则 `justify-self: center;` */
    justify-self: center;
    /* [zh] 样式规则 `position: relative;` */
    position: relative;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.recent-panel {` */
  .recent-panel {
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `left: 0;` */
    left: 0;
    /* [zh] 样式规则 `right: 0;` */
    right: 0;
    /* [zh] 样式规则 `top: calc(100% + 4px);` */
    top: calc(100% + 4px);
    /* [zh] 样式规则 `z-index: 20;` */
    z-index: 20;
    /* [zh] 样式规则 `padding: 10px 12px;` */
    padding: 10px 12px;
    /* [zh] 样式规则 `background: #fff;` */
    background: #fff;
    /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
    border: 1px solid $color-border-gray;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
    /* [zh] 样式规则 `box-shadow: 0 8px 24px rgba(0, 0, 0, 0.0` */
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.recent-head {` */
  .recent-head {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `margin-bottom: 8px;` */
    margin-bottom: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.clear-link {` */
  .clear-link {
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: none;` */
    background: none;
    /* [zh] 样式规则 `color: $color-text-secondary;` */
    color: $color-text-secondary;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.recent-row {` */
  .recent-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `padding: 4px 0;` */
    padding: 4px 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.recent-word {` */
  .recent-word {
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: none;` */
    background: none;
    /* [zh] 样式规则 `text-align: left;` */
    text-align: left;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.recent-del {` */
  .recent-del {
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: none;` */
    background: none;
    /* [zh] 样式规则 `color: $color-text-secondary;` */
    color: $color-text-secondary;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `font-size: 18px;` */
    font-size: 18px;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.recent-empty {` */
  .recent-empty {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-secondary;` */
    color: $color-text-secondary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-box {` */
  .search-box {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `max-width: 620px;` */
    max-width: 620px;
    /* [zh] 样式规则 `height: 40px;` */
    height: 40px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: stretch;` */
    align-items: stretch;
    /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
    border: 1px solid $color-border-gray;
    /* [zh] 样式规则 `border-radius: $radius-search;` */
    border-radius: $radius-search;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `background: #fff;` */
    background: #fff;
    /* [zh] 样式规则 `transition: border-color $transition-fas` */
    transition: border-color $transition-fast, box-shadow $transition-fast;

    /* [zh] 样式规则 `&:focus-within {` */
    &:focus-within {
      /* [zh] 样式规则 `border-color: $color-primary;` */
      border-color: $color-primary;
      /* [zh] 样式规则 `box-shadow: 0 0 0 3px rgba(232, 104, 93,` */
      box-shadow: 0 0 0 3px rgba(232, 104, 93, 0.06);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.search-category {` */
    .search-category {
      /* [zh] 样式规则 `position: relative;` */
      position: relative;
      /* [zh] 样式规则 `width: 100px;` */
      width: 100px;
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;

      /* [zh] 样式规则 `:deep(.el-select__wrapper) {` */
      :deep(.el-select__wrapper) {
        /* [zh] 样式规则 `height: 100%;` */
        height: 100%;
        /* [zh] 样式规则 `box-shadow: none !important;` */
        box-shadow: none !important;
        /* [zh] 样式规则 `border-radius: 0;` */
        border-radius: 0;
        /* [zh] 样式规则 `background: transparent;` */
        background: transparent;
        /* [zh] 样式规则 `justify-content: center;` */
        justify-content: center;
        /* [zh] 样式规则 `padding-left: 8px;` */
        padding-left: 8px;
        /* [zh] 样式规则 `padding-right: 22px;` */
        padding-right: 22px;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `:deep(.el-select__selection) {` */
      :deep(.el-select__selection) {
        /* [zh] 样式规则 `flex: 1;` */
        flex: 1;
        /* [zh] 样式规则 `min-width: 0;` */
        min-width: 0;
        /* [zh] 样式规则 `justify-content: center;` */
        justify-content: center;
        /* [zh] 样式规则 `text-align: center;` */
        text-align: center;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式声明 */
      :deep(.el-select__selected-item),
      /* [zh] 样式规则 `:deep(.el-select__placeholder) {` */
      :deep(.el-select__placeholder) {
        /* [zh] 样式规则 `width: 100%;` */
        width: 100%;
        /* [zh] 样式规则 `text-align: center;` */
        text-align: center;
        /* [zh] 样式规则 `justify-content: center;` */
        justify-content: center;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `:deep(.el-select__suffix) {` */
      :deep(.el-select__suffix) {
        /* [zh] 样式规则 `position: absolute;` */
        position: absolute;
        /* [zh] 样式规则 `right: 6px;` */
        right: 6px;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.search-divider {` */
    .search-divider {
      /* [zh] 样式规则 `width: 1px;` */
      width: 1px;
      /* [zh] 样式规则 `align-self: center;` */
      align-self: center;
      /* [zh] 样式规则 `height: 20px;` */
      height: 20px;
      /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.08);` */
      background: rgba(0, 0, 0, 0.08);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.search-input {` */
    .search-input {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `min-width: 0;` */
      min-width: 0;
      /* [zh] 样式规则 `border: none;` */
      border: none;
      /* [zh] 样式规则 `outline: none;` */
      outline: none;
      /* [zh] 样式规则 `padding: 0 12px;` */
      padding: 0 12px;
      /* [zh] 样式规则 `font-size: 14px;` */
      font-size: 14px;
      /* [zh] 样式规则 `color: $color-text-primary;` */
      color: $color-text-primary;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.search-submit {` */
    .search-submit {
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
      /* [zh] 样式规则 `min-width: 72px;` */
      min-width: 72px;
      /* [zh] 样式规则 `padding: 0 20px;` */
      padding: 0 20px;
      /* [zh] 样式规则 `border: none;` */
      border: none;
      /* [zh] 样式规则 `border-left: none;` */
      border-left: none;
      /* [zh] 样式规则 `background: $color-primary;` */
      background: $color-primary;
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `font-size: 16px;` */
      font-size: 16px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
      /* [zh] 样式规则 `transition: background $transition-fast,` */
      transition: background $transition-fast, box-shadow $transition-fast, transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1), filter $transition-fast;

      /* [zh] 样式规则 `&:hover {` */
      &:hover {
        /* [zh] 样式规则 `background: $color-primary-hover;` */
        background: $color-primary-hover;
        /* [zh] 样式规则 `box-shadow: 0 4px 16px rgba(232, 104, 93` */
        box-shadow: 0 4px 16px rgba(232, 104, 93, 0.25);
        /* [zh] 样式规则 `transform: translateY(-1px);` */
        transform: translateY(-1px);
        /* [zh] 样式规则 `filter: brightness(1.03);` */
        filter: brightness(1.03);
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `&:active {` */
      &:active {
        /* [zh] 样式规则 `transform: scale(0.94);` */
        transform: scale(0.94);
        /* [zh] 样式规则 `background: $color-primary-active;` */
        background: $color-primary-active;
        /* [zh] 样式规则 `box-shadow: 0 2px 8px rgba(232, 104, 93,` */
        box-shadow: 0 2px 8px rgba(232, 104, 93, 0.18);
        /* [zh] 样式规则 `filter: brightness(0.92);` */
        filter: brightness(0.92);
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.hot-words {` */
  .hot-words {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `gap: 4px 12px;` */
    gap: 4px 12px;
    /* [zh] 样式规则 `margin-top: 6px;` */
    margin-top: 6px;
    /* [zh] 样式规则 `padding-left: 0;` */
    padding-left: 0;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.hot-label {` */
  .hot-label {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.hot-word {` */
  .hot-word {
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `text-decoration: underline;` */
      text-decoration: underline;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.header-actions {` */
  .header-actions {
    /* [zh] 样式规则 `grid-column: 3;` */
    grid-column: 3;
    /* [zh] 样式规则 `justify-self: end;` */
    justify-self: end;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: flex-end;` */
    justify-content: flex-end;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;

    /* [zh] 样式规则 `.action-link {` */
    .action-link {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `color: $color-text-body;` */
      color: $color-text-body;
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

    /* [zh] 样式规则 `.user-trigger {` */
    .user-trigger {
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
      /* [zh] 样式规则 `gap: 6px;` */
      gap: 6px;
      /* [zh] 样式规则 `padding: 4px 8px 4px 4px;` */
      padding: 4px 8px 4px 4px;
      /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
      border: 1px solid $color-border-gray;
      /* [zh] 样式规则 `border-radius: 20px;` */
      border-radius: 20px;
      /* [zh] 样式规则 `background: $color-card;` */
      background: $color-card;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
      /* [zh] 样式规则 `transition: border-color $transition-fas` */
      transition: border-color $transition-fast, box-shadow $transition-fast;

      /* [zh] 样式规则 `&:hover {` */
      &:hover {
        /* [zh] 样式规则 `border-color: rgba($color-primary, 0.3);` */
        border-color: rgba($color-primary, 0.3);
        /* [zh] 样式规则 `box-shadow: 0 2px 8px rgba(232, 104, 93,` */
        box-shadow: 0 2px 8px rgba(232, 104, 93, 0.08);
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.user-name {` */
      .user-name {
        /* [zh] 样式规则 `max-width: 80px;` */
        max-width: 80px;
        /* [zh] 样式规则 `overflow: hidden;` */
        overflow: hidden;
        /* [zh] 样式规则 `text-overflow: ellipsis;` */
        text-overflow: ellipsis;
        /* [zh] 样式规则 `white-space: nowrap;` */
        white-space: nowrap;
        /* [zh] 样式规则 `font-size: 13px;` */
        font-size: 13px;
        /* [zh] 样式规则 `color: $color-text-primary;` */
        color: $color-text-primary;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.icon-action {` */
    .icon-action {
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `flex-direction: column;` */
      flex-direction: column;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
      /* [zh] 样式规则 `gap: 2px;` */
      gap: 2px;
      /* [zh] 样式规则 `min-width: 56px;` */
      min-width: 56px;
      /* [zh] 样式规则 `padding: 4px;` */
      padding: 4px;
      /* [zh] 样式规则 `border: none;` */
      border: none;
      /* [zh] 样式规则 `background: transparent;` */
      background: transparent;
      /* [zh] 样式规则 `color: $color-text-primary;` */
      color: $color-text-primary;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
      /* [zh] 样式规则 `border-radius: $radius-xs;` */
      border-radius: $radius-xs;
      /* [zh] 样式规则 `transition: background $transition-fast,` */
      transition: background $transition-fast, color $transition-fast;

      /* [zh] 样式规则 `.icon-label {` */
      .icon-label {
        /* [zh] 样式规则 `font-size: 12px;` */
        font-size: 12px;
        /* [zh] 样式规则 `color: $color-text-body;` */
        color: $color-text-body;
        /* [zh] 样式规则 `transition: color $transition-fast;` */
        transition: color $transition-fast;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `&:hover {` */
      &:hover {
        /* [zh] 样式规则 `color: $color-primary;` */
        color: $color-primary;
        /* [zh] 样式规则 `background: $color-primary-soft;` */
        background: $color-primary-soft;

        /* [zh] 样式规则 `.icon-label {` */
        .icon-label {
          /* [zh] 样式规则 `color: $color-primary;` */
          color: $color-primary;
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
