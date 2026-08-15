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
            <!-- [zh] 开始标签 `<component>` -->
            <component :is="Component" :key="viewRoute.path" class="page-view-root" />
          <!-- [zh] 闭合标签 `</KeepAlive>` -->
          </KeepAlive>
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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed } from 'vue';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`Grid,`
  Grid,
  // [zh] TypeScript/JS 语句：`HomeFilled,`
  HomeFilled,
  // [zh] TypeScript/JS 语句：`List,`
  List,
  // [zh] TypeScript/JS 语句：`ShoppingCart,`
  ShoppingCart,
  // [zh] TypeScript/JS 语句：`Ticket,`
  Ticket,
  // [zh] TypeScript/JS 语句：`User`
  User
// [zh] 语句
} from '@element-plus/icons-vue';
// [zh] 导入 `vue-router`
import { RouterLink, RouterView, useRoute } from 'vue-router';
// [zh] 导入 `@/components/layout/AppFooter.vue`
import AppFooter from '@/components/layout/AppFooter.vue';
// [zh] 导入 `@/components/layout/PcFloatToolbar.vue`
import PcFloatToolbar from '@/components/layout/PcFloatToolbar.vue';
// [zh] 导入 `@/components/layout/SiteHeader.vue`
import SiteHeader from '@/components/layout/SiteHeader.vue';
// [zh] 导入 `@/components/home/PcSimlectScreenNav.vue`
import PcSimlectScreenNav from '@/components/home/PcSimlectScreenNav.vue';
// [zh] 导入 `@/components/business/NotificationPopup.vue`
import NotificationPopup from '@/components/business/NotificationPopup.vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@/composables/useUnreadCount`
import { useUnreadCount } from '@/composables/useUnreadCount';
// [zh] 导入 `@/utils/notification`
import { navigateNotification, type NotificationData } from '@/utils/notification';

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 语句
const { refreshUnreadCount } = useUnreadCount();
// [zh] 声明变量 `isHomePage`
const isHomePage = computed(() => route.path === '/');

// [zh] 声明变量 `navItems`
const navItems = [
  // [zh] TypeScript/JS 语句：`{ path: '/', label: '首页', icon: HomeFilled },`
  { path: '/', label: '首页', icon: HomeFilled },
  // [zh] TypeScript/JS 语句：`{ path: '/search', label: '分类', icon: Grid },`
  { path: '/search', label: '分类', icon: Grid },
  // [zh] TypeScript/JS 语句：`{ path: '/coupons', label: '优惠券', icon: Ticket },`
  { path: '/coupons', label: '优惠券', icon: Ticket },
  // [zh] TypeScript/JS 语句：`{ path: '/cart', label: '购物车', icon: ShoppingCart },`
  { path: '/cart', label: '购物车', icon: ShoppingCart },
  // [zh] TypeScript/JS 语句：`{ path: '/orders', label: '我的订单', icon: List },`
  { path: '/orders', label: '我的订单', icon: List },
  // [zh] TypeScript/JS 语句：`{ path: '/account', label: '个人中心', icon: User }`
  { path: '/account', label: '个人中心', icon: User }
// [zh] 语句
];

// [zh] 声明变量 `isActive`
const isActive = (path: string) => {
  // [zh] 条件判断 if
  if (path === '/') return route.path === '/';
  // [zh] return 返回
  return route.path === path || route.path.startsWith(`${path}/`);
// [zh] 语句
};

// [zh] 声明变量 `handleNotificationClick`
const handleNotificationClick = (notification: NotificationData) => {
  // [zh] 语句
  void navigateNotification(router, notification, { refreshUnread: refreshUnreadCount });
// [zh] 语句
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
