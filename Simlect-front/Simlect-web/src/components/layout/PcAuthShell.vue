<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-auth-shell ignore">
    <!-- [zh] 开始标签 `<header>` -->
    <header class="pc-auth-header">
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/" class="brand">
        <!-- [zh] 开始标签 `<BrandMark>` -->
        <BrandMark class="brand-icon" />
        <!-- [zh] 开始标签 `<span>` -->
        <span class="brand-text">简选</span>
      <!-- [zh] 闭合标签 `</RouterLink>` -->
      </RouterLink>
      <!-- [zh] 开始标签 `<nav>` -->
      <nav class="auth-links">
        <!-- [zh] 开始标签 `<RouterLink>` -->
        <RouterLink to="/">网站首页</RouterLink>
        <!-- [zh] 开始标签 `<RouterLink>` -->
        <RouterLink v-if="!isLoginPage" to="/login">登录</RouterLink>
        <!-- [zh] 开始标签 `<RouterLink>` -->
        <RouterLink v-if="isLoginPage" to="/register">免费注册</RouterLink>
      <!-- [zh] 闭合标签 `</nav>` -->
      </nav>
    <!-- [zh] 闭合标签 `</header>` -->
    </header>
    <!-- [zh] 开始标签 `<main>` -->
    <main class="pc-auth-main">
      <!-- [zh] 开始标签 `<slot>` -->
      <slot />
    <!-- [zh] 闭合标签 `</main>` -->
    </main>
    <!-- [zh] 开始标签 `<footer>` -->
    <footer class="pc-auth-footer">
      <!-- [zh] 开始标签 `<span>` -->
      <span>© 简选</span>
    <!-- [zh] 闭合标签 `</footer>` -->
    </footer>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import BrandMark from '@/components/common/BrandMark.vue';

const route = useRoute();
const isLoginPage = computed(() => route.path === '/login');
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-auth-shell.ignore {` */
.pc-auth-shell.ignore {
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

  /* [zh] 样式规则 `.pc-auth-header {` */
  .pc-auth-header {
    /* [zh] 样式规则 `height: 62px;` */
    height: 62px;
    /* [zh] 样式规则 `padding: 0 24px;` */
    padding: 0 24px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `background: $color-card;` */
    background: $color-card;
    /* [zh] 样式规则 `border-bottom: 1px solid $color-border-g` */
    border-bottom: 1px solid $color-border-gray;
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
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.auth-links {` */
  .auth-links {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 20px;` */
    gap: 20px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;

    /* [zh] 样式规则 `a {` */
    a {
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
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.pc-auth-main {` */
  .pc-auth-main {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `padding: 40px 16px;` */
    padding: 40px 16px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.pc-auth-footer {` */
  .pc-auth-footer {
    /* [zh] 样式规则 `padding: 16px;` */
    padding: 16px;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
