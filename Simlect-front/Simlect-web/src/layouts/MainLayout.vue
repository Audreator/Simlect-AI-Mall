<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="layout" :class="layoutClasses">
    <!-- [zh] 开始标签 `<HomeSearchHeader>` -->
    <HomeSearchHeader v-if="isHomeTab" />
    <!-- [zh] 开始标签 `<NotificationPopup>` -->
    <NotificationPopup @click="handleNotificationClick" />

    <!-- [zh] 开始标签 `<TabPageHeader>` -->
    <TabPageHeader v-if="tabHeader" :title="tabHeader.title">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-if="route.path === '/search'" #right>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="tab-header-actions">
          <!-- [zh] 开始标签 `<AgentServiceEntry>` -->
          <AgentServiceEntry />
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon class="header-action" :size="22" @click="openSearchPortal"><Search /></el-icon>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-else-if="route.path === '/account'" #right>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="tab-header-actions">
          <!-- [zh] 开始标签 `<AgentServiceEntry>` -->
          <AgentServiceEntry />
          <!-- [zh] 开始标签 `<button>` -->
          <button
            type="button"
            class="header-action header-action-btn"
            aria-label="消息"
            @click="router.push('/notifications')"
          >
            <!-- [zh] 开始标签 `<el-badge>` -->
            <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
              <!-- [zh] 开始标签 `<el-icon>` -->
              <el-icon :size="22"><Bell /></el-icon>
            <!-- [zh] 闭合标签 `</el-badge>` -->
            </el-badge>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon
            class="header-action"
            :size="22"
            aria-label="设置"
            @click="router.push('/account/manage')"
          >
            <!-- [zh] 开始标签 `<Setting>` -->
            <Setting />
          <!-- [zh] 闭合标签 `</el-icon>` -->
          </el-icon>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-else-if="route.path === '/cart'" #right>
        <!-- [zh] 开始标签 `<AgentServiceEntry>` -->
        <AgentServiceEntry />
      </template>
    </TabPageHeader>

    <header v-if="showSiteHeader" class="site-header">
      <div class="header-inner">
        <RouterLink class="brand" to="/">
          <BrandMark class="brand-icon" />
          <span class="brand-text">简选</span>
        </RouterLink>

        <div class="mobile-tools">
          <el-icon class="tool-btn" :size="22" @click="openSearchPortal"><Search /></el-icon>
          <el-badge :value="cartStore.cartCount" :hidden="!cartStore.cartCount" :max="99">
            <el-icon class="tool-btn" :size="22" @click="router.push('/cart')"><ShoppingCart /></el-icon>
          </el-badge>
          <el-icon class="tool-btn" :size="22" @click="mobileMenuOpen = true"><Menu /></el-icon>
        </div>

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
            @keyup.enter="goSearch"
          />
          <button type="button" class="search-submit" @click="goSearch">
            <el-icon><Search /></el-icon>
            <span>搜索</span>
          </button>
        </div>

        <nav class="header-actions">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-if="!authStore.isLoggedIn">
            <!-- [zh] 开始标签 `<RouterLink>` -->
            <RouterLink class="action-link" to="/login">请登录</RouterLink>
            <!-- [zh] 开始标签 `<RouterLink>` -->
            <RouterLink class="action-link highlight" to="/register">免费注册</RouterLink>
          </template>
          <el-dropdown v-else trigger="click" popper-class="user-dropdown-popper">
            <button type="button" class="user-trigger">
              <el-avatar :size="28" :src="avatarUrl" class="user-avatar">
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
                <el-dropdown-item @click="router.push('/sign')">签到中心</el-dropdown-item>
                <!-- [zh] 开始标签 `<el-dropdown-item>` -->
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              <!-- [zh] 闭合标签 `</el-dropdown-menu>` -->
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <button type="button" class="icon-action" title="搜索" @click="openSearchPortal">
            <el-icon :size="22"><Search /></el-icon>
            <span class="icon-label">搜索</span>
          </button>

          <button type="button" class="icon-action" title="购物车" @click="router.push('/cart')">
            <el-badge :value="cartStore.cartCount" :hidden="!cartStore.cartCount" :max="99" class="cart-badge">
              <el-icon :size="22"><ShoppingCart /></el-icon>
            </el-badge>
            <span class="icon-label">购物车</span>
          </button>

          <button type="button" class="icon-action" title="智能客服" @click="router.push('/ai-assistant')">
            <el-icon :size="22"><ChatDotRound /></el-icon>
            <span class="icon-label">客服</span>
          </button>

          <button type="button" class="icon-action" title="消息" @click="router.push('/notifications')">
            <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
              <el-icon :size="22"><Bell /></el-icon>
            </el-badge>
            <span class="icon-label">消息</span>
          </button>
        </nav>
      </div>
    </header>

    <el-drawer v-model="mobileMenuOpen" direction="rtl" size="72%" title="菜单">
      <div class="mobile-menu">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-if="!authStore.isLoggedIn">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" class="menu-btn" @click="navAndClose('/login')">登录</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button @click="navAndClose('/register')">注册</el-button>
        </template>
        <el-menu :default-active="route.path" @select="navAndClose">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/search">商品分类</el-menu-item>
          <el-menu-item index="/coupons">优惠券广场</el-menu-item>
          <el-menu-item index="/cart">购物车</el-menu-item>
          <el-menu-item index="/orders">我的订单</el-menu-item>
          <el-menu-item index="/account">个人中心</el-menu-item>
          <el-menu-item index="/notifications">消息中心</el-menu-item>
          <el-menu-item index="/member-center">会员中心</el-menu-item>
          <el-menu-item index="/ai-assistant">智能客服</el-menu-item>
        </el-menu>
      </div>
    </el-drawer>

    <main class="main-wrap">
      <div class="page-container">
        <PullRefreshHost>
          <PageBackBar v-if="showPageBack" />
          <RouterView v-slot="{ Component, route: viewRoute }">
            <KeepAlive :max="4">
              <component :is="Component" :key="viewRoute.path" class="page-view-root" />
            </KeepAlive>
          </RouterView>
        </PullRefreshHost>
      </div>
    </main>

    <AppFooter v-if="showFooter" />
    <MobileTabBar />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { ArrowDown, Bell, ChatDotRound, Menu, Search, Setting, ShoppingCart } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { productApi } from '@/api/modules';
import { useUnreadCount } from '@/composables/useUnreadCount';
import AppFooter from '@/components/layout/AppFooter.vue';
import MobileTabBar from '@/components/layout/MobileTabBar.vue';
import PageBackBar from '@/components/layout/PageBackBar.vue';
import TabPageHeader from '@/components/layout/TabPageHeader.vue';
import AgentServiceEntry from '@/components/agent/AgentServiceEntry.vue';
import BrandMark from '@/components/common/BrandMark.vue';
import HomeSearchHeader from '@/components/layout/HomeSearchHeader.vue';
import PullRefreshHost from '@/components/common/PullRefreshHost.vue';
import NotificationPopup from '@/components/business/NotificationPopup.vue';
import { isPrimaryTabPath } from '@/constants/tabPages';
import { resolveAvatarUrl } from '@/utils/image';
import { useSearchStore } from '@/stores/search';
import { flattenCategoryOptions, normalizeCategoryTree } from '@/utils/category';
import { confirmAction } from '@/utils/confirm';
import { restoreScrollForPath, saveScrollForPath } from '@/utils/scrollMemory';
import { navigateNotification, type NotificationData } from '@/utils/notification';
import { toast } from '@/utils/toast';

const router = useRouter();
const searchStore = useSearchStore();
const route = useRoute();
const authStore = useAuthStore();
const cartStore = useCartStore();

const keyword = ref('');
const searchCategoryId = ref('');
const categoryList = ref<any[]>([]);
const mobileMenuOpen = ref(false);
const isMobile = ref(false);
const { unreadCount, refreshUnreadCount } = useUnreadCount();

const isHomeTab = computed(() => route.path === '/');
const isPrimaryTab = computed(() => isPrimaryTabPath(route.path));

const showSiteHeader = computed(() => false);

const tabHeader = computed(() => {
  if (!isPrimaryTab.value || isHomeTab.value) return null;
  switch (route.path) {
    case '/search':
      if (isMobile.value) return null;
      return { title: '分类' };
    case '/cart':
      return {
        title:
          cartStore.cartCount > 0 ? `购物车(${cartStore.cartCount})` : '购物车'
      };
    case '/account':
      if (isMobile.value) return null;
      return { title: '我的' };
    default:
      return null;
  }
});

const layoutClasses = computed(() => ({
  'is-mobile': isMobile.value,
  'is-home-page': isHomeTab.value,
  'is-tab-page': !!tabHeader.value,
  'is-primary-tab': isMobile.value && isPrimaryTab.value,
  'is-account-tab': isMobile.value && route.path === '/account',
  'is-cate-tab': isMobile.value && route.path === '/search'
}));

const showFooter = computed(() => true);

const showPageBack = computed(() => {
  if (route.meta.level === 1 || isPrimaryTab.value) return false;
  return route.meta.showBack === true;
});

const avatarUrl = computed(() => resolveAvatarUrl(authStore.userInfo?.avatar));

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768;
};

const openSearchPortal = () => {
  const keyWords = keyword.value.trim();
  if (keyWords) {
    searchStore.setSearch({
      keyWords,
      categoryId: searchCategoryId.value || ''
    });
  }
  router.push('/search-portal');
};

const goSearch = () => {
  const keyWords = keyword.value.trim();
  if (!keyWords) {
    openSearchPortal();
    return;
  }
  searchStore.setSearch({
    keyWords,
    categoryId: searchCategoryId.value || ''
  });
  router.push({ path: '/search-result', query: { q: keyWords } });
};

const navAndClose = (path: string) => {
  router.push(path);
  mobileMenuOpen.value = false;
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

const handleNotificationClick = (notification: NotificationData) => {
  void navigateNotification(router, notification, { refreshUnread: refreshUnreadCount });
};

watch(
  () => route.path,
  (path) => {
    if (path === '/cart' && authStore.isLoggedIn) {
      cartStore.fetchCartCount();
    }
    if (path === '/notifications' || path === '/account') {
      refreshUnreadCount();
    }
  },
  { immediate: true }
);

watch(
  () => authStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      refreshUnreadCount();
    } else {
      unreadCount.value = 0;
    }
  }
);

onMounted(async () => {
  checkMobile();
  window.addEventListener('resize', checkMobile);
  const cats = await productApi.loadCategory();
  categoryList.value = flattenCategoryOptions(normalizeCategoryTree(cats || []));
  if (authStore.isLoggedIn) {
    cartStore.fetchCartCount();
    refreshUnreadCount();
  }
});

onUnmounted(() => window.removeEventListener('resize', checkMobile));

router.beforeEach((to, from) => {
  if (!from.fullPath || to.fullPath === from.fullPath) return;
  const tabSwitch = isPrimaryTabPath(from.path) && isPrimaryTabPath(to.path);
  if (!tabSwitch) saveScrollForPath(from.fullPath);
});

const restoreScrollPosition = (path = route.fullPath) => {
  restoreScrollForPath(path);
};

onMounted(() => {
  router.afterEach((to, from) => {
    const tabSwitch =
      isPrimaryTabPath(to.path) && isPrimaryTabPath(from.path);
    if (tabSwitch) return;
    nextTick(() => restoreScrollPosition(to.fullPath));
  });
  restoreScrollPosition();
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.layout {` */
.layout {
  /* [zh] 样式规则 `min-height: 100vh;` */
  min-height: 100vh;
  /* [zh] 样式规则 `min-height: 100dvh;` */
  min-height: 100dvh;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;

  /* [zh] 样式规则 `&.is-mobile {` */
  &.is-mobile {
    /* [zh] 样式规则 `overflow-x: clip;` */
    overflow-x: clip;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-tab-page .main-wrap {` */
  &.is-tab-page .main-wrap {
    padding-top: calc(#{$tab-header-height} + env(safe-area-inset-top, 0));
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-home-page .main-wrap {` */
  &.is-home-page .main-wrap {
    padding-top: calc(#{$home-search-bar-height} + env(safe-area-inset-top, 0));
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-account-tab .main-wrap {` */
  &.is-account-tab .main-wrap {
    /* [zh] 样式规则 `padding-top: 0;` */
    padding-top: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-cate-tab .main-wrap {` */
  &.is-cate-tab .main-wrap {
    /* [zh] 样式规则 `padding-top: 0;` */
    padding-top: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tab-header-actions {` */
.tab-header-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.header-action {` */
.header-action {
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `width: 36px;` */
  width: 36px;
  /* [zh] 样式规则 `height: 36px;` */
  height: 36px;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `transition: background $transition-fast,` */
  transition: background $transition-fast, color $transition-fast, transform $transition-fast;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.92);` */
    transform: scale(0.92);
    /* [zh] 样式规则 `background: $color-primary-muted;` */
    background: $color-primary-muted;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.header-action-btn {` */
.header-action-btn {
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.site-header {` */
.site-header {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `top: 0;` */
  top: 0;
  /* [zh] 样式规则 `left: 0;` */
  left: 0;
  /* [zh] 样式规则 `right: 0;` */
  right: 0;
  /* [zh] 样式规则 `z-index: 1000;` */
  z-index: 1000;
  /* [zh] 样式规则 `height: $header-height;` */
  height: $header-height;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `border-bottom: 1px solid var(--glass-bor` */
  border-bottom: 1px solid var(--glass-border-soft);
  /* [zh] 样式规则 `box-shadow: var(--glass-shadow-sm);` */
  box-shadow: var(--glass-shadow-sm);

  /* [zh] 样式规则 `&::after {` */
  &::after {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.header-inner {` */
  .header-inner {
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
    /* [zh] 样式规则 `grid-template-columns: auto minmax(200px` */
    grid-template-columns: auto minmax(200px, 1fr) auto;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `column-gap: 24px;` */
    column-gap: 24px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.brand {` */
.brand {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `justify-self: start;` */
  justify-self: start;
  /* [zh] 样式规则 `text-decoration: none;` */
  text-decoration: none;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
  /* [zh] 样式规则 `transition: opacity $transition-fast;` */
  transition: opacity $transition-fast;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `opacity: 0.92;` */
    opacity: 0.92;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-icon {` */
  .brand-icon {
    /* [zh] 样式规则 `width: 36px;` */
    width: 36px;
    /* [zh] 样式规则 `height: 40px;` */
    height: 40px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-text {` */
  .brand-text {
    /* [zh] 样式规则 `font-size: 20px;` */
    font-size: 20px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `letter-spacing: 0.08em;` */
    letter-spacing: 0.08em;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.mobile-tools {` */
.mobile-tools {
  /* [zh] 样式规则 `display: none;` */
  display: none;
  /* [zh] 样式规则 `justify-self: end;` */
  justify-self: end;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;

  /* [zh] 样式规则 `.tool-btn {` */
  .tool-btn {
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `width: 36px;` */
    width: 36px;
    /* [zh] 样式规则 `height: 36px;` */
    height: 36px;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `border-radius: 50%;` */
    border-radius: 50%;
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `place-items: center;` */
    place-items: center;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
    /* [zh] 样式规则 `transition: transform $transition-fast, ` */
    transition: transform $transition-fast, background $transition-fast;

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `transform: scale(0.92);` */
      transform: scale(0.92);
      /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.28);` */
      background: rgba(255, 255, 255, 0.28);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.search-box {` */
.search-box {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: 560px;` */
  max-width: 560px;
  /* [zh] 样式规则 `height: 42px;` */
  height: 42px;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `justify-self: center;` */
  justify-self: center;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `border-radius: $radius-search;` */
  border-radius: $radius-search;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `box-shadow: 0 4px 16px rgba(16, 24, 40, ` */
  box-shadow: 0 4px 16px rgba(16, 24, 40, 0.1);
  /* [zh] 样式规则 `transition: box-shadow $transition-fast,` */
  transition: box-shadow $transition-fast, transform $transition-fast;

  /* [zh] 样式规则 `&:focus-within {` */
  &:focus-within {
    /* [zh] 样式规则 `box-shadow: 0 6px 24px rgba(16, 24, 40, ` */
    box-shadow: 0 6px 24px rgba(16, 24, 40, 0.14);
    /* [zh] 样式规则 `transform: translateY(-1px);` */
    transform: translateY(-1px);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-category {` */
  .search-category {
    /* [zh] 样式规则 `width: 110px;` */
    width: 110px;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;

    /* [zh] 样式规则 `:deep(.el-select__wrapper) {` */
    :deep(.el-select__wrapper) {
      /* [zh] 样式规则 `box-shadow: none !important;` */
      box-shadow: none !important;
      /* [zh] 样式规则 `background: transparent;` */
      background: transparent;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-divider {` */
  .search-divider {
    /* [zh] 样式规则 `width: 1px;` */
    width: 1px;
    /* [zh] 样式规则 `height: 20px;` */
    height: 20px;
    /* [zh] 样式规则 `background: $color-border;` */
    background: $color-border;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
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
    /* [zh] 样式规则 `padding: 0 14px;` */
    padding: 0 14px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;

    /* [zh] 样式规则 `&::placeholder {` */
    &::placeholder {
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-submit {` */
  .search-submit {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `padding: 0 20px;` */
    padding: 0 20px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: linear-gradient(90deg, $colo` */
    background: linear-gradient(90deg, $color-primary-hover, $color-primary);
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;
    /* [zh] 样式规则 `transition: filter $transition-fast, tra` */
    transition: filter $transition-fast, transform $transition-fast;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `filter: brightness(1.05);` */
      filter: brightness(1.05);
      /* [zh] 样式规则 `transform: translateY(-1px);` */
      transform: translateY(-1px);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `filter: brightness(0.95);` */
      filter: brightness(0.95);
      /* [zh] 样式规则 `transform: translateY(1px) scale(0.98);` */
      transform: translateY(1px) scale(0.98);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&:focus-visible {` */
    &:focus-visible {
      /* [zh] 样式规则 `outline: 2px solid $color-gold;` */
      outline: 2px solid $color-gold;
      /* [zh] 样式规则 `outline-offset: 2px;` */
      outline-offset: 2px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.header-actions {` */
.header-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;
  /* [zh] 样式规则 `gap: 2px;` */
  gap: 2px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `justify-self: end;` */
  justify-self: end;

  /* [zh] 样式规则 `.action-link {` */
  .action-link {
    /* [zh] 样式规则 `display: inline-flex;` */
    display: inline-flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `color: rgba(255, 255, 255, 0.95);` */
    color: rgba(255, 255, 255, 0.95);
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
    /* [zh] 样式规则 `padding: 6px 10px;` */
    padding: 6px 10px;
    /* [zh] 样式规则 `border-radius: $radius-btn;` */
    border-radius: $radius-btn;
    /* [zh] 样式规则 `transition: background $transition-fast,` */
    transition: background $transition-fast, color $transition-fast;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.15);` */
      background: rgba(255, 255, 255, 0.15);
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.highlight {` */
    &.highlight {
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
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
    /* [zh] 样式规则 `padding: 4px 10px 4px 4px;` */
    padding: 4px 10px 4px 4px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `border-radius: 20px;` */
    border-radius: 20px;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.12);` */
    background: rgba(255, 255, 255, 0.12);
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `transition: background $transition-fast;` */
    transition: background $transition-fast;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.22);` */
      background: rgba(255, 255, 255, 0.22);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.user-name {` */
    .user-name {
      /* [zh] 样式规则 `max-width: 72px;` */
      max-width: 72px;
      /* [zh] 样式规则 `overflow: hidden;` */
      overflow: hidden;
      /* [zh] 样式规则 `text-overflow: ellipsis;` */
      text-overflow: ellipsis;
      /* [zh] 样式规则 `white-space: nowrap;` */
      white-space: nowrap;
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
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
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `gap: 2px;` */
    gap: 2px;
    /* [zh] 样式规则 `min-width: 52px;` */
    min-width: 52px;
    /* [zh] 样式规则 `padding: 4px 8px;` */
    padding: 4px 8px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `border-radius: $radius-btn;` */
    border-radius: $radius-btn;
    /* [zh] 样式规则 `transition: background $transition-fast,` */
    transition: background $transition-fast, transform $transition-fast;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;

    /* [zh] 样式规则 `.el-icon {` */
    .el-icon {
      /* [zh] 样式规则 `display: block;` */
      display: block;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.icon-label {` */
    .icon-label {
      /* [zh] 样式规则 `display: block;` */
      display: block;
      /* [zh] 样式规则 `width: 100%;` */
      width: 100%;
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
      /* [zh] 样式规则 `line-height: 1.2;` */
      line-height: 1.2;
      /* [zh] 样式规则 `text-align: center;` */
      text-align: center;
      /* [zh] 样式规则 `opacity: 0.95;` */
      opacity: 0.95;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.15);` */
      background: rgba(255, 255, 255, 0.15);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `transform: scale(0.95);` */
      transform: scale(0.95);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.cart-badge .el-badge__content) {` */
  :deep(.cart-badge .el-badge__content) {
    /* [zh] 样式规则 `background: $color-price;` */
    background: $color-price;
    /* [zh] 样式规则 `border: 2px solid $color-primary;` */
    border: 2px solid $color-primary;
    /* [zh] 样式规则 `animation: badge-pop 0.35s ease;` */
    animation: badge-pop 0.35s ease;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@keyframes badge-pop {` */
@keyframes badge-pop {
  /* [zh] 样式规则 `0% {` */
  0% {
    /* [zh] 样式规则 `transform: scale(0.6);` */
    transform: scale(0.6);
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `70% {` */
  70% {
    /* [zh] 样式规则 `transform: scale(1.15);` */
    transform: scale(1.15);
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `100% {` */
  100% {
    /* [zh] 样式规则 `transform: scale(1);` */
    transform: scale(1);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.main-wrap {` */
.main-wrap {
  /* [zh] 样式规则 `padding-top: 16px;` */
  padding-top: 16px;
  min-height: calc(100vh - #{$footer-height});
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.page-container {` */
.page-container {
  /* [zh] 样式规则 `max-width: $content-width;` */
  max-width: $content-width;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `padding: 0 16px 16px;` */
  padding: 0 16px 16px;

  /* [zh] 样式规则 `.layout.is-mobile.is-home-page & {` */
  .layout.is-mobile.is-home-page & {
    /* [zh] 样式规则 `max-width: 100%;` */
    max-width: 100%;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.layout.is-mobile.is-primary-tab & {` */
  .layout.is-mobile.is-primary-tab & {
    /* [zh] 样式规则 `padding: 0 0 $mobile-tab-reserved;` */
    padding: 0 0 $mobile-tab-reserved;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.layout.is-mobile .page-container:has(.a` */
  .layout.is-mobile .page-container:has(.agent-page) {
    /* [zh] 样式规则 `padding-bottom: 0;` */
    padding-bottom: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `@media (min-width: $breakpoint-tablet) {` */
  @media (min-width: $breakpoint-tablet) {
    /* [zh] 样式规则 `padding: 0 16px 24px;` */
    padding: 0 16px 24px;

    /* [zh] 样式规则 `.layout.is-home-page & {` */
    .layout.is-home-page & {
      /* [zh] 样式规则 `max-width: $content-width;` */
      max-width: $content-width;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.mobile-search-panel {` */
.mobile-search-panel {
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.mobile-menu .menu-btn {` */
.mobile-menu .menu-btn {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (max-width: $breakpoint-tablet) {` */
@media (max-width: $breakpoint-tablet) {
  /* [zh] 样式规则 `.header-inner {` */
  .header-inner {
    /* [zh] 样式规则 `column-gap: 12px;` */
    column-gap: 12px;
    /* [zh] 样式规则 `grid-template-columns: auto 1fr auto;` */
    grid-template-columns: auto 1fr auto;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-box {` */
  .search-box {
    /* [zh] 样式规则 `max-width: 100%;` */
    max-width: 100%;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.header-actions .icon-label {` */
  .header-actions .icon-label {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.header-actions .icon-action {` */
  .header-actions .icon-action {
    /* [zh] 样式规则 `min-width: 40px;` */
    min-width: 40px;
    /* [zh] 样式规则 `padding: 6px;` */
    padding: 6px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (max-width: $breakpoint-mobile) {` */
@media (max-width: $breakpoint-mobile) {
  /* [zh] 样式规则 `.brand {` */
  .brand {
    /* [zh] 样式规则 `max-width: calc(100vw - 130px);` */
    max-width: calc(100vw - 130px);
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand .brand-text {` */
  .brand .brand-text {
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.header-inner {` */
  .header-inner {
    /* [zh] 样式规则 `grid-template-columns: auto 1fr auto;` */
    grid-template-columns: auto 1fr auto;
    /* [zh] 样式规则 `column-gap: 8px;` */
    column-gap: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  .search-box,
  /* [zh] 样式规则 `.header-actions {` */
  .header-actions {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.mobile-tools {` */
  .mobile-tools {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
