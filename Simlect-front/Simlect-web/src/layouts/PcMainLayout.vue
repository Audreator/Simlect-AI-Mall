<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-layout ignore pc-surface" :class="{ 'is-home-page': isHomePage }">
    <!-- [zh] 开始标签 `<SiteHeader>` -->
    <SiteHeader :simlect-search="isHomePage" />
    <!-- [zh] 开始标签 `<PcSimlectScreenNav>` -->
    <PcSimlectScreenNav v-if="isHomePage" />
    <!-- [zh] 开始标签 `<nav>` -->
    <nav v-else class="pc-nav ignore" aria-label="站点导航">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="pc-nav-inner">
        <!-- [zh] 开始标签 `<RouterLink>` -->
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="pc-nav-link"
          :class="{ active: isActive(item.path) }"
        >
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon class="nav-icon" :size="18"><component :is="item.icon" /></el-icon>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="nav-label">{{ item.label }}</span>
        <!-- [zh] 闭合标签 `</RouterLink>` -->
        </RouterLink>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</nav>` -->
    </nav>
    <!-- [zh] 开始标签 `<main>` -->
    <main class="pc-main">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="pc-page-container">
        <!-- [zh] 开始标签 `<RouterView>` -->
        <RouterView v-slot="{ Component, route: viewRoute }">
          <!-- [zh] 开始标签 `<KeepAlive>` -->
          <KeepAlive :max="4">
            <component :is="Component" :key="viewRoute.path" class="page-view-root" /></KeepAlive>
        <!-- [zh] 闭合标签 `</RouterView>` -->
        </RouterView>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</main>` -->
    </main>
    <!-- [zh] 开始标签 `<AppFooter>` -->
    <AppFooter />
    <!-- [zh] 开始标签 `<PcFloatToolbar>` -->
    <PcFloatToolbar />
    <!-- [zh] 开始标签 `<NotificationPopup>` -->
    <NotificationPopup @click="handleNotificationClick" />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  Grid,
  HomeFilled,
  List,
  ShoppingCart,
  Ticket,
  User
} from '@element-plus/icons-vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import AppFooter from '@/components/layout/AppFooter.vue';
import PcFloatToolbar from '@/components/layout/PcFloatToolbar.vue';
import SiteHeader from '@/components/layout/SiteHeader.vue';
import PcSimlectScreenNav from '@/components/home/PcSimlectScreenNav.vue';
import NotificationPopup from '@/components/business/NotificationPopup.vue';
import { useRouter } from 'vue-router';
import { useUnreadCount } from '@/composables/useUnreadCount';
import { navigateNotification, type NotificationData } from '@/utils/notification';

const route = useRoute();
const router = useRouter();
const { refreshUnreadCount } = useUnreadCount();
const isHomePage = computed(() => route.path === '/');

const navItems = [
  { path: '/', label: '首页', icon: HomeFilled },
  { path: '/search', label: '分类', icon: Grid },
  { path: '/coupons', label: '优惠券', icon: Ticket },
  { path: '/cart', label: '购物车', icon: ShoppingCart },
  { path: '/orders', label: '我的订单', icon: List },
  { path: '/account', label: '个人中心', icon: User }
];

const isActive = (path: string) => {
  if (path === '/') return route.path === '/';
  return route.path === path || route.path.startsWith(`${path}/`);
};

const handleNotificationClick = (notification: NotificationData) => {
  void navigateNotification(router, notification, { refreshUnread: refreshUnreadCount });
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-layout.ignore {` */
.pc-layout.ignore {
  /* [zh] 样式规则 `min-height: 100vh;` */
  min-height: 100vh;
  /* [zh] 样式规则 `min-height: var(--app-vh, 100dvh);` */
  min-height: var(--app-vh, 100dvh);
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `background: $color-bg;` */
  background: $color-bg;

  /* [zh] 样式规则 `.pc-nav {` */
  .pc-nav {
    /* [zh] 样式规则 `background: $color-card;` */
    background: $color-card;
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
    /* [zh] 样式规则 `box-shadow: 0 2px 8px rgba(15, 23, 42, 0` */
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.pc-nav-inner {` */
  .pc-nav-inner {
    /* [zh] 样式规则 `max-width: $content-width;` */
    max-width: $content-width;
    /* [zh] 样式规则 `margin: 0 auto;` */
    margin: 0 auto;
    /* [zh] 样式规则 `padding: 0 16px;` */
    padding: 0 16px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `flex-wrap: nowrap;` */
    flex-wrap: nowrap;
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;
    /* [zh] 样式规则 `min-height: 40px;` */
    min-height: 40px;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.pc-nav-link {` */
  .pc-nav-link {
    /* [zh] 样式规则 `display: inline-flex;` */
    display: inline-flex;
    /* [zh] 样式规则 `flex-direction: row;` */
    flex-direction: row;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
    /* [zh] 样式规则 `padding: 8px 14px;` */
    padding: 8px 14px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: $color-text-primary;` */
    color: $color-text-primary;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `transition: color $transition-fast;` */
    transition: color $transition-fast;

    /* [zh] 样式规则 `.nav-label {` */
    .nav-label {
      /* [zh] 样式规则 `white-space: nowrap;` */
      white-space: nowrap;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.nav-icon {` */
    .nav-icon {
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;

      /* [zh] 样式规则 `.nav-icon {` */
      .nav-icon {
        /* [zh] 样式规则 `color: $color-primary;` */
        color: $color-primary;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.active {` */
    &.active {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;

      /* [zh] 样式规则 `.nav-icon {` */
      .nav-icon {
        /* [zh] 样式规则 `color: $color-primary;` */
        color: $color-primary;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.pc-main {` */
  .pc-main {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `padding: 16px 0 32px;` */
    padding: 16px 0 32px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.pc-page-container {` */
  .pc-page-container {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `max-width: $content-max-width;` */
    max-width: $content-max-width;
    /* [zh] 样式规则 `margin: 0 auto;` */
    margin: 0 auto;
    /* [zh] 样式规则 `padding: 0 16px;` */
    padding: 0 16px;
    /* [zh] 样式规则 `box-sizing: border-box;` */
    box-sizing: border-box;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
