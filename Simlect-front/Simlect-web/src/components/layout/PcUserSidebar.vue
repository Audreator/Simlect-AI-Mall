<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<aside>` -->
  <aside class="pc-user-sidebar ignore" aria-label="我的简选导航">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="sidebar-head">
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/account" class="sidebar-brand">我的简选</RouterLink>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<nav>` -->
    <nav v-for="group in PC_USER_NAV_GROUPS" :key="group.title" class="sidebar-group">
      <!-- [zh] 开始标签 `<h4>` -->
      <h4 class="group-title">{{ group.title }}</h4>
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-for="item in group.items"
        :key="item.path"
        type="button"
        class="sidebar-link"
        :class="{ active: isActive(item.path) }"
        @click="onNavClick(item.path)"
      >
        <!-- [zh] Mustache 插值表达式 -->
        {{ item.label }}
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</nav>` -->
    </nav>
  <!-- [zh] 闭合标签 `</aside>` -->
  </aside>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@/composables/useOpenAgent`
import { useOpenAgent } from '@/composables/useOpenAgent';
// [zh] 导入 `@/constants/pcUserNav`
import { PC_USER_NAV_GROUPS } from '@/constants/pcUserNav';

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 语句
const { openAgent } = useOpenAgent();

// [zh] 声明变量 `onNavClick`
const onNavClick = (path: string) => {
  // [zh] 条件判断 if
  if (path === '/ai-assistant') {
    // [zh] 语句
    openAgent();
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  router.push(path);
// [zh] 语句
};

// [zh] 声明变量 `isActive`
const isActive = (path: string) => {
  // [zh] 条件判断 if
  if (path === '/account') return route.path === '/account';
  // [zh] 条件判断 if
  if (path === '/orders') return route.path === '/orders' || route.path.startsWith('/order/');
  // [zh] 条件判断 if
  if (path === '/member-center') return route.path === '/member-center';
  // [zh] 条件判断 if
  if (path === '/notifications') return route.path === '/notifications';
  // [zh] return 返回
  return route.path === path || route.path.startsWith(`${path}/`);
// [zh] 语句
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-user-sidebar.ignore {` */
.pc-user-sidebar.ignore {
  /* [zh] 样式规则 `width: 180px;` */
  width: 180px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `padding: 12px 0;` */
  padding: 12px 0;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;
  /* [zh] 样式规则 `align-self: flex-start;` */
  align-self: flex-start;
  /* [zh] 样式规则 `position: sticky;` */
  position: sticky;
  /* [zh] 样式规则 `top: 16px;` */
  top: 16px;

  /* [zh] 样式规则 `.sidebar-head {` */
  .sidebar-head {
    /* [zh] 样式规则 `padding: 0 16px 12px;` */
    padding: 0 16px 12px;
    /* [zh] 样式规则 `border-bottom: 1px solid $color-border-g` */
    border-bottom: 1px solid $color-border-gray;
    /* [zh] 样式规则 `margin-bottom: 8px;` */
    margin-bottom: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sidebar-brand {` */
  .sidebar-brand {
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary-hover-bright;` */
      color: $color-primary-hover-bright;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sidebar-group {` */
  .sidebar-group {
    /* [zh] 样式规则 `padding: 8px 0;` */
    padding: 8px 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.group-title {` */
  .group-title {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `padding: 6px 16px;` */
    padding: 6px 16px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sidebar-link {` */
  .sidebar-link {
    /* [zh] 样式规则 `display: block;` */
    display: block;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `padding: 8px 16px;` */
    padding: 8px 16px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `text-align: left;` */
    text-align: left;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `color: $color-text-primary;` */
    color: $color-text-primary;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
    /* [zh] 样式规则 `border-left: 3px solid transparent;` */
    border-left: 3px solid transparent;
    /* [zh] 样式规则 `transition: color $transition-fast, back` */
    transition: color $transition-fast, background $transition-fast, border-color $transition-fast;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `background: $color-cat-hover-bg;` */
      background: $color-cat-hover-bg;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.active {` */
    &.active {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `background: $color-primary-soft;` */
      background: $color-primary-soft;
      /* [zh] 样式规则 `border-left-color: $color-primary;` */
      border-left-color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
