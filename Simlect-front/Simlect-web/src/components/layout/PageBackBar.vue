<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="page-nav-bar ignore">
    <!-- [zh] 开始标签 `<button>` -->
    <button type="button" class="nav-back" aria-label="返回" @click="goBack">
      <!-- [zh] 开始标签 `<el-icon>` -->
      <el-icon :size="20"><ArrowLeft /></el-icon>
    <!-- [zh] 闭合标签 `</button>` -->
    </button>
    <!-- [zh] 开始标签 `<h1>` -->
    <h1 class="nav-title">{{ displayTitle }}</h1>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="nav-side-placeholder" aria-hidden="true" />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { ArrowLeft } from '@element-plus/icons-vue';
// [zh] 导入 `@/utils/mobileViewport`
import { recoverIosViewportZoom } from '@/utils/mobileViewport';

// [zh] 声明变量 `props`
const props = defineProps<{
  // [zh] 语句
  title?: string;
  // [zh] 语句
  fallback?: string;
// [zh] 语句
}>();

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `route`
const route = useRoute();

// [zh] 声明变量 `displayTitle`
const displayTitle = computed(() => props.title || (route.meta.title as string) || '');

// [zh] 声明变量 `fallbackPath`
const fallbackPath = computed(() => props.fallback || '/');

// [zh] 声明变量 `isAuthPage`
const isAuthPage = () => route.path === '/login' || route.path === '/register';

// [zh] 声明变量 `canHistoryBackSafely`
const canHistoryBackSafely = () => {
  // [zh] 声明变量 `back`
  const back = window.history.state?.back as string | null | undefined;
  // [zh] 条件判断 if
  if (!back) return false;

  // [zh] 声明变量 `backResolved`
  let backResolved;
  // [zh] 代码块开始
  try {
    // [zh] 语句
    backResolved = router.resolve(back);
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return false;
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (backResolved.fullPath === route.fullPath) return false;

  // [zh] 声明变量 `redirectRaw`
  const redirectRaw = route.query.redirect;
  // [zh] 条件判断 if
  if (typeof redirectRaw === 'string' && redirectRaw) {
    // [zh] 代码块开始
    try {
      // [zh] 条件判断 if
      if (router.resolve(redirectRaw).fullPath === backResolved.fullPath) return false;
    // [zh] 代码块开始
    } catch {

    // [zh] 语法符号
    }
  // [zh] 语法符号
  }

  // [zh] return 返回
  return !backResolved.matched.some((r) => r.meta.requiresAuth === true);
// [zh] 语句
};

// [zh] 声明变量 `goBack`
const goBack = () => {
  // [zh] 语句
  recoverIosViewportZoom();
  // [zh] 语句
  (document.activeElement as HTMLElement | null)?.blur?.();

  // [zh] 条件判断 if
  if (isAuthPage() && route.query.redirect) {
    // [zh] 条件判断 if
    if (canHistoryBackSafely()) {
      // [zh] 语句
      router.back();
    // [zh] 代码块开始
    } else {
      // [zh] 语句
      router.replace(fallbackPath.value);
    // [zh] 语法符号
    }
    // [zh] 语句
    return;
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (window.history.length > 1) {
    // [zh] 语句
    router.back();
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  router.replace(fallbackPath.value);
// [zh] 语句
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.page-nav-bar {` */
.page-nav-bar {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: 44px 1fr 44px;` */
  grid-template-columns: 44px 1fr 44px;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `height: 48px;` */
  height: 48px;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.nav-back {` */
.nav-back {
  /* [zh] 样式规则 `width: 40px;` */
  width: 40px;
  /* [zh] 样式规则 `height: 40px;` */
  height: 40px;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
  border: 1px solid rgba(120, 120, 128, 0.18);
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.55);` */
  background: rgba(255, 255, 255, 0.55);
  /* [zh] 样式规则 `-webkit-backdrop-filter: var(--glass-blu` */
  -webkit-backdrop-filter: var(--glass-blur-sm);
  /* [zh] 样式规则 `backdrop-filter: var(--glass-blur-sm);` */
  backdrop-filter: var(--glass-blur-sm);
  /* [zh] 样式规则 `color: var(--m-ink, #333333);` */
  color: var(--m-ink, #333333);
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `touch-action: manipulation;` */
  touch-action: manipulation;
  /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
  -webkit-tap-highlight-color: transparent;
  /* [zh] 样式规则 `transition: background 0.2s, color 0.2s,` */
  transition: background 0.2s, color 0.2s, transform 0.15s, box-shadow 0.2s;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.78);` */
    background: rgba(255, 255, 255, 0.78);
    /* [zh] 样式规则 `color: var(--m-gold, #c9a962);` */
    color: var(--m-gold, #c9a962);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.94);` */
    transform: scale(0.94);
    /* [zh] 样式规则 `box-shadow: none;` */
    box-shadow: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.nav-title {` */
.nav-title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 0 8px;` */
  padding: 0 8px;
  /* [zh] 样式规则 `font-size: 17px;` */
  font-size: 17px;
  /* [zh] 样式规则 `font-weight: 700;` */
  font-weight: 700;
  /* [zh] 样式规则 `letter-spacing: -0.02em;` */
  letter-spacing: -0.02em;
  /* [zh] 样式规则 `line-height: 1.3;` */
  line-height: 1.3;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `text-overflow: ellipsis;` */
  text-overflow: ellipsis;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.nav-side-placeholder {` */
.nav-side-placeholder {
  /* [zh] 样式规则 `width: 40px;` */
  width: 40px;
  /* [zh] 样式规则 `height: 40px;` */
  height: 40px;
/* [zh] 样式规则 `}` */
}
</style>
