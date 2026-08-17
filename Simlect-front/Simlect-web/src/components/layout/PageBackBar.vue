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

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft } from '@element-plus/icons-vue';
import { recoverIosViewportZoom } from '@/utils/mobileViewport';

const props = defineProps<{
  title?: string;
  fallback?: string;
}>();

const router = useRouter();
const route = useRoute();

const displayTitle = computed(() => props.title || (route.meta.title as string) || '');

const fallbackPath = computed(() => props.fallback || '/');

const isAuthPage = () => route.path === '/login' || route.path === '/register';

const canHistoryBackSafely = () => {
  const back = window.history.state?.back as string | null | undefined;
  if (!back) return false;

  let backResolved;
  try {
    backResolved = router.resolve(back);
  } catch {
    return false;
  }

  if (backResolved.fullPath === route.fullPath) return false;

  const redirectRaw = route.query.redirect;
  if (typeof redirectRaw === 'string' && redirectRaw) {
    try {
      if (router.resolve(redirectRaw).fullPath === backResolved.fullPath) return false;
    } catch {

    }
  }

  return !backResolved.matched.some((r) => r.meta.requiresAuth === true);
};

const goBack = () => {
  recoverIosViewportZoom();
  (document.activeElement as HTMLElement | null)?.blur?.();

  if (isAuthPage() && route.query.redirect) {
    if (canHistoryBackSafely()) {
      router.back();
    } else {
      router.replace(fallbackPath.value);
    }
    return;
  }

  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.replace(fallbackPath.value);
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
