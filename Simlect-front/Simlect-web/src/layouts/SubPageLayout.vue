<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div
    class="sub-page-layout"
    :class="{
      'is-mobile': isMobile,
      'no-tab-bar': hideTabBar,
      'is-agent-page': isAgentPage
    }"
  >
    <!-- [zh] 开始标签 `<LiquidGlassSurface>` -->
    <LiquidGlassSurface
      ref="subTopRef"
      tag="header"
      intensity="medium"
      class="sub-top-bar ignore"
    >
      <!-- [zh] 开始标签 `<div>` -->
      <div class="sub-top-inner">
        <!-- [zh] 开始标签 `<PageBackBar>` -->
        <PageBackBar fallback="/" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</LiquidGlassSurface>` -->
    </LiquidGlassSurface>
    <!-- [zh] 开始标签 `<main>` -->
    <main class="sub-main">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="sub-container" :class="{ 'is-agent-container': isAgentPage }">
        <!-- [zh] 开始标签 `<RouterView>` -->
        <RouterView v-slot="{ Component }">
          <!-- [zh] 开始标签 `<Transition>` -->
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" /></Transition>
        <!-- [zh] 闭合标签 `</RouterView>` -->
        </RouterView>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</main>` -->
    </main>
    <!-- [zh] 开始标签 `<MobileTabBar>` -->
    <MobileTabBar v-if="!hideTabBar" />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
import PageBackBar from '@/components/layout/PageBackBar.vue';
import MobileTabBar from '@/components/layout/MobileTabBar.vue';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';
import { restoreScrollForPath, saveScrollForPath } from '@/utils/scrollMemory';

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();
const isMobile = ref(false);
const subTopRef = ref<InstanceType<typeof LiquidGlassSurface> | null>(null);

router.beforeEach((to, from) => {
  if (from.fullPath && to.fullPath !== from.fullPath) {
    saveScrollForPath(from.fullPath);
  }
});

const restoreScrollPosition = () => {
  restoreScrollForPath(route.fullPath);
};

onMounted(() => {
  checkMobile();
  window.addEventListener('resize', checkMobile);
  window.addEventListener('resize', syncSubTopHeight);
  syncSubTopHeight();
  if (authStore.isLoggedIn) cartStore.fetchCartCount();
  router.afterEach(() => nextTick(() => restoreScrollPosition()));
  restoreScrollPosition();
});

const syncSubTopHeight = () => {
  const el = subTopRef.value?.$el as HTMLElement | undefined;
  const h = el?.offsetHeight;
  if (h && h > 0) {
    document.documentElement.style.setProperty('--sub-top-height', `${h}px`);
  }
};

const hideTabBar = computed(() => route.matched.some((r) => r.meta.hideTabBar === true));

const isAgentPage = computed(() => route.path === '/ai-assistant' || route.path.endsWith('/ai-assistant'));

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768;
};

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile);
  window.removeEventListener('resize', syncSubTopHeight);
  document.documentElement.style.removeProperty('--sub-top-height');
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.sub-page-layout {` */
.sub-page-layout {
  /* [zh] 样式规则 `min-height: 100vh;` */
  min-height: 100vh;
  /* [zh] 样式规则 `background: var(--glass-page-bg);` */
  background: var(--glass-page-bg);
  /* [zh] 样式规则 `overflow-x: clip;` */
  overflow-x: clip;

  /* [zh] 样式规则 `&.is-mobile.no-tab-bar .sub-main {` */
  &.is-mobile.no-tab-bar .sub-main {
    /* [zh] 样式规则 `padding-bottom: calc(env(safe-area-inset` */
    padding-bottom: calc(env(safe-area-inset-bottom, 0) + 12px);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  &.is-mobile .sub-main:has(.agent-page),
  /* [zh] 样式声明 */
  &.is-mobile .sub-main:has(.checkout-page),
  /* [zh] 样式规则 `&.is-mobile .sub-main:has(.pay-page) {` */
  &.is-mobile .sub-main:has(.pay-page) {
    /* [zh] 样式规则 `padding-bottom: 0;` */
    padding-bottom: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-mobile.no-tab-bar.is-agent-page {` */
  &.is-mobile.no-tab-bar.is-agent-page {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `min-height: var(--app-vh, 100dvh);` */
    min-height: var(--app-vh, 100dvh);

    /* [zh] 样式规则 `.sub-main {` */
    .sub-main {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `min-height: 0;` */
      min-height: 0;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `flex-direction: column;` */
      flex-direction: column;
      /* [zh] 样式规则 `overflow: hidden;` */
      overflow: hidden;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-page-layout.is-agent-page {` */
.sub-page-layout.is-agent-page {
  /* [zh] 样式规则 `--sub-top-height: 60px;` */
  --sub-top-height: 60px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `height: var(--app-vh, 100dvh);` */
  height: var(--app-vh, 100dvh);
  /* [zh] 样式规则 `max-height: var(--app-vh, 100dvh);` */
  max-height: var(--app-vh, 100dvh);
  /* [zh] 样式规则 `min-height: var(--app-vh, 100dvh);` */
  min-height: var(--app-vh, 100dvh);
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `.sub-top-bar {` */
  .sub-top-bar {
    /* [zh] 样式规则 `position: fixed;` */
    position: fixed;
    /* [zh] 样式规则 `top: 0;` */
    top: 0;
    /* [zh] 样式规则 `left: 0;` */
    left: 0;
    /* [zh] 样式规则 `right: 0;` */
    right: 0;
    /* [zh] 样式规则 `z-index: 1300;` */
    z-index: 1300;
    /* [zh] 样式规则 `padding-top: env(safe-area-inset-top, 0)` */
    padding-top: env(safe-area-inset-top, 0);
    /* [zh] 样式规则 `background: var(--glass-bg-header);` */
    background: var(--glass-bg-header);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sub-main {` */
  .sub-main {
    /* [zh] 样式规则 `flex: 1 1 0;` */
    flex: 1 1 0;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `padding-top: calc(var(--sub-top-height, ` */
    padding-top: calc(var(--sub-top-height, 60px) + env(safe-area-inset-top, 0));
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sub-container {` */
  .sub-container {
    /* [zh] 样式规则 `flex: 1 1 0;` */
    flex: 1 1 0;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `max-width: none;` */
    max-width: none;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-page-layout:has(.agent-page) {` */
.sub-page-layout:has(.agent-page) {
  /* [zh] 样式规则 `--sub-top-height: 60px;` */
  --sub-top-height: 60px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `height: var(--app-vh, 100dvh);` */
  height: var(--app-vh, 100dvh);
  /* [zh] 样式规则 `max-height: var(--app-vh, 100dvh);` */
  max-height: var(--app-vh, 100dvh);
  /* [zh] 样式规则 `min-height: var(--app-vh, 100dvh);` */
  min-height: var(--app-vh, 100dvh);
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `.sub-top-bar {` */
  .sub-top-bar {
    /* [zh] 样式规则 `position: fixed;` */
    position: fixed;
    /* [zh] 样式规则 `top: 0;` */
    top: 0;
    /* [zh] 样式规则 `left: 0;` */
    left: 0;
    /* [zh] 样式规则 `right: 0;` */
    right: 0;
    /* [zh] 样式规则 `z-index: 1300;` */
    z-index: 1300;
    /* [zh] 样式规则 `padding-top: env(safe-area-inset-top, 0)` */
    padding-top: env(safe-area-inset-top, 0);
    /* [zh] 样式规则 `background: #fff;` */
    background: #fff;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sub-main {` */
  .sub-main {
    /* [zh] 样式规则 `flex: 1 1 0;` */
    flex: 1 1 0;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `padding-top: calc(var(--sub-top-height, ` */
    padding-top: calc(var(--sub-top-height, 60px) + env(safe-area-inset-top, 0));
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sub-container {` */
  .sub-container {
    /* [zh] 样式规则 `flex: 1 1 0;` */
    flex: 1 1 0;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.sub-container.is-agent-container,
/* [zh] 样式规则 `.sub-container:has(.agent-page) {` */
.sub-container:has(.agent-page) {
  /* [zh] 样式规则 `max-width: none;` */
  max-width: none;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;

  /* [zh] 样式规则 `:deep(.pull-refresh-host) {` */
  :deep(.pull-refresh-host) {
    /* [zh] 样式规则 `flex: 1 1 auto;` */
    flex: 1 1 auto;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;

    /* [zh] 样式规则 `> * {` */
    > * {
      /* [zh] 样式规则 `flex: 1 1 auto;` */
      flex: 1 1 auto;
      /* [zh] 样式规则 `min-height: 0;` */
      min-height: 0;
      /* [zh] 样式规则 `height: 100%;` */
      height: 100%;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `flex-direction: column;` */
      flex-direction: column;
      /* [zh] 样式规则 `overflow: hidden;` */
      overflow: hidden;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.agent-page) {` */
  :deep(.agent-page) {
    /* [zh] 样式规则 `flex: 1 1 0;` */
    flex: 1 1 0;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.chat-scroll) {` */
  :deep(.chat-scroll) {
    /* [zh] 样式规则 `flex: 1 1 0;` */
    flex: 1 1 0;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `overflow-x: clip;` */
    overflow-x: clip;
    /* [zh] 样式规则 `overflow-y: auto;` */
    overflow-y: auto;
    /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
    -webkit-overflow-scrolling: touch;
    /* [zh] 样式规则 `touch-action: pan-y;` */
    touch-action: pan-y;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-top-bar {` */
.sub-top-bar {
  /* [zh] 样式规则 `position: sticky;` */
  position: sticky;
  /* [zh] 样式规则 `top: 0;` */
  top: 0;
  /* [zh] 样式规则 `z-index: 100;` */
  z-index: 100;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `border-bottom: 1px solid var(--glass-bor` */
  border-bottom: 1px solid var(--glass-border-soft);
  /* [zh] 样式规则 `box-shadow: var(--glass-shadow-sm);` */
  box-shadow: var(--glass-shadow-sm);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-page-layout.is-mobile .sub-top-bar ` */
.sub-page-layout.is-mobile .sub-top-bar {
  /* [zh] 样式规则 `padding-top: env(safe-area-inset-top, 0)` */
  padding-top: env(safe-area-inset-top, 0);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-top-inner {` */
.sub-top-inner {
  /* [zh] 样式规则 `max-width: $content-width;` */
  max-width: $content-width;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `padding: 8px 16px;` */
  padding: 8px 16px;

  /* [zh] 样式规则 `:deep(.page-nav-bar) {` */
  :deep(.page-nav-bar) {
    /* [zh] 样式规则 `margin-bottom: 0;` */
    margin-bottom: 0;
    /* [zh] 样式规则 `box-shadow: none;` */
    box-shadow: none;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `height: 44px;` */
    height: 44px;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-main {` */
.sub-main {
  /* [zh] 样式规则 `padding: 16px 0 24px;` */
  padding: 16px 0 24px;

  /* [zh] 样式规则 `&:has(.agent-page) {` */
  &:has(.agent-page) {
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-page-layout.is-mobile .sub-main:has` */
.sub-page-layout.is-mobile .sub-main:has(.orders-page) {
  /* [zh] 样式规则 `padding-top: 0;` */
  padding-top: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-page-layout.is-mobile .sub-containe` */
.sub-page-layout.is-mobile .sub-container:has(.orders-page) {
  /* [zh] 样式规则 `padding-top: 0;` */
  padding-top: 0;
  /* [zh] 样式规则 `padding-left: 0;` */
  padding-left: 0;
  /* [zh] 样式规则 `padding-right: 0;` */
  padding-right: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-page-layout.is-mobile:has(.orders-p` */
.sub-page-layout.is-mobile:has(.orders-page) .sub-top-bar {
  /* [zh] 样式规则 `border-bottom: none;` */
  border-bottom: none;
  /* [zh] 样式规则 `box-shadow: none;` */
  box-shadow: none;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-container {` */
.sub-container {
  /* [zh] 样式规则 `max-width: $content-width;` */
  max-width: $content-width;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `padding: 0 16px;` */
  padding: 0 16px;

  /* [zh] 样式规则 `&:has(.auth-page) {` */
  &:has(.auth-page) {
    /* [zh] 样式规则 `max-width: none;` */
    max-width: none;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-main:has(.auth-page) {` */
.sub-main:has(.auth-page) {
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
/* [zh] 样式规则 `}` */
}
</style>
