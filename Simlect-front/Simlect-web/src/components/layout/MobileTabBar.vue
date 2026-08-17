<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
  <!-- [zh] 开始标签 `<div>` -->
  <div ref="hostRef" class="mobile-tab-bar-host" :class="{ 'is-jelly': barJelly }">
  <!-- [zh] 开始标签 `<LiquidGlassSurface>` -->
  <LiquidGlassSurface
    tag="nav"
    intensity="strong"
    class="mobile-tab-bar ignore"
    aria-label="底部导航"
  >
    <!-- [zh] 开始标签 `<div>` -->
    <div
      class="tab-bar-inner"
      :style="{ '--tab-count': tabs.length, '--active-index': activeIndex }"
    >
      <!-- [zh] 开始标签 `<LiquidGlassSurface>` -->
      <LiquidGlassSurface
        v-show="activeIndex >= 0"
        intensity="strong"
        variant="active"
        class="tab-active-glass"
        :class="{ 'is-jelly': glassJelly }"
        aria-hidden="true"
      />

      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink
        v-for="item in tabs"
        :key="item.label"
        :to="item.path"
        replace
        class="tab-item"
        :class="{ active: isActive(item) }"
        :aria-current="isActive(item) ? 'page' : undefined"
        :aria-label="item.label"
        @click="onTabPress()"
      >
        <!-- [zh] 开始标签 `<span>` -->
        <span class="icon-wrap">
          <!-- [zh] 开始标签 `<el-badge>` -->
          <el-badge v-if="item.badge" :value="cartCount" :hidden="!cartCount" :max="99">
            <!-- [zh] 开始标签 `<el-icon>` -->
            <el-icon :size="16"><component :is="item.icon" /></el-icon>
          <!-- [zh] 闭合标签 `</el-badge>` -->
          </el-badge>
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon v-else :size="16"><component :is="item.icon" /></el-icon>
        <!-- [zh] 闭合标签 `</span>` -->
        </span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="label">{{ item.label }}</span>
      <!-- [zh] 闭合标签 `</RouterLink>` -->
      </RouterLink>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</LiquidGlassSurface>` -->
  </LiquidGlassSurface>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
  <!-- [zh] 闭合标签 `</Teleport>` -->
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { HomeFilled, Grid, ShoppingCart, User } from '@element-plus/icons-vue';
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
import { useTabBarJelly } from '@/composables/useTabBarJelly';
import { useCartStore } from '@/stores/cart';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const cartStore = useCartStore();
const authStore = useAuthStore();
const hostRef = ref<HTMLElement | null>(null);
const cartCount = computed(() => cartStore.cartCount);

const syncTabStackHeight = () => {
  const el = hostRef.value;
  if (!el) return;
  const rect = el.getBoundingClientRect();
  const stackHeight = window.innerHeight - rect.top;
  document.documentElement.style.setProperty('--mobile-tab-stack-height', `${stackHeight}px`);
};

onMounted(() => {
  nextTick(syncTabStackHeight);
  window.addEventListener('resize', syncTabStackHeight);
  window.visualViewport?.addEventListener('resize', syncTabStackHeight);
  window.visualViewport?.addEventListener('scroll', syncTabStackHeight);
});

onUnmounted(() => {
  window.removeEventListener('resize', syncTabStackHeight);
  window.visualViewport?.removeEventListener('resize', syncTabStackHeight);
  window.visualViewport?.removeEventListener('scroll', syncTabStackHeight);
});

const tabs = computed(() => [
  { path: '/', label: '首页', icon: HomeFilled, match: (p: string) => p === '/' },
  {
    path: '/search',
    label: '分类',
    icon: Grid,
    match: (p: string) => p === '/search' || p.startsWith('/category/')
  },
  { path: '/cart', label: '购物车', icon: ShoppingCart, badge: true, match: (p: string) => p === '/cart' },
  {
    path: authStore.isLoggedIn ? '/account' : '/login',
    label: '我的',
    icon: User,
    match: (p: string) => p === '/account' || (!authStore.isLoggedIn && p === '/login')
  }
]);

const isActive = (item: (typeof tabs.value)[0]) => item.match(route.path);

const activeIndex = computed(() => {
  const idx = tabs.value.findIndex((item) => isActive(item));
  return idx >= 0 ? idx : -1;
});

const { barJelly, glassJelly, onTabPress } = useTabBarJelly();
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;
/* [zh] 样式规则 `@use '@/styles/tab-bar-jelly' as jelly;` */
@use '@/styles/tab-bar-jelly' as jelly;

/* [zh] 样式规则 `.mobile-tab-bar-host {` */
.mobile-tab-bar-host {
  /* [zh] 样式规则 `display: none;` */
  display: none;
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `left: 2.5%;` */
  left: 2.5%;
  /* [zh] 样式规则 `right: 2.5%;` */
  right: 2.5%;
  /* [zh] 样式规则 `bottom: calc(12px + env(safe-area-inset-` */
  bottom: calc(12px + env(safe-area-inset-bottom, 0));
  /* [zh] 样式规则 `z-index: 99999;` */
  z-index: 99999;
  /* [zh] 样式规则 `width: auto;` */
  width: auto;
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
  /* [zh] 样式规则 `touch-action: manipulation;` */
  touch-action: manipulation;
  /* [zh] 样式规则 `@include jelly.tab-bar-jelly-host;` */
  @include jelly.tab-bar-jelly-host;

  /* [zh] 样式规则 `@media (max-width: $breakpoint-mobile) {` */
  @media (max-width: $breakpoint-mobile) {
    /* [zh] 样式规则 `display: block;` */
    display: block;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.mobile-tab-bar-host .mobile-tab-bar {` */
.mobile-tab-bar-host .mobile-tab-bar {
  /* [zh] 样式规则 `pointer-events: auto;` */
  pointer-events: auto;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.mobile-tab-bar {` */
.mobile-tab-bar {
  /* [zh] 样式规则 `position: relative !important;` */
  position: relative !important;
  /* [zh] 样式规则 `left: auto !important;` */
  left: auto !important;
  /* [zh] 样式规则 `right: auto !important;` */
  right: auto !important;
  /* [zh] 样式规则 `bottom: auto !important;` */
  bottom: auto !important;
  /* [zh] 样式规则 `transform: none !important;` */
  transform: none !important;
  /* [zh] 样式规则 `z-index: auto !important;` */
  z-index: auto !important;
  /* [zh] 样式规则 `width: 100% !important;` */
  width: 100% !important;
  /* [zh] 样式规则 `padding: 0 !important;` */
  padding: 0 !important;
  /* [zh] 样式规则 `border: none !important;` */
  border: none !important;
  /* [zh] 样式规则 `border-radius: 999px !important;` */
  border-radius: 999px !important;
  /* [zh] 样式规则 `color: inherit;` */
  color: inherit;
  /* [zh] 样式规则 `cursor: default;` */
  cursor: default;
  /* [zh] 样式规则 `@include jelly.tab-bar-jelly-shell;` */
  @include jelly.tab-bar-jelly-shell;
  /* [zh] 样式规则 `box-shadow:` */
  box-shadow:
    /* [zh] 样式声明 */
    0 6px 6px rgba(0, 0, 0, 0.2),
    /* [zh] 样式声明 */
    0 0 20px rgba(0, 0, 0, 0.1);

/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tab-bar-inner {` */
.tab-bar-inner {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-around;` */
  justify-content: space-around;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 40px;` */
  height: 40px;
  /* [zh] 样式规则 `padding: 4px 6px;` */
  padding: 4px 6px;
  /* [zh] 样式规则 `overflow: visible;` */
  overflow: visible;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tab-active-glass {` */
.tab-active-glass {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `top: 4px;` */
  top: 4px;
  /* [zh] 样式规则 `bottom: 4px;` */
  bottom: 4px;
  /* [zh] 样式规则 `left: 6px;` */
  left: 6px;
  /* [zh] 样式规则 `width: calc((100% - 12px) / var(--tab-co` */
  width: calc((100% - 12px) / var(--tab-count));
  /* [zh] 样式规则 `border-radius: 999px;` */
  border-radius: 999px;
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
  /* [zh] 样式规则 `z-index: 0;` */
  z-index: 0;
  /* [zh] 样式规则 `transform: translateX(calc(var(--active-` */
  transform: translateX(calc(var(--active-index) * 100%));
  /* [zh] 样式规则 `box-shadow:` */
  box-shadow:
    /* [zh] 样式声明 */
    0 2px 6px rgba(0, 0, 0, 0.12),
    /* [zh] 样式声明 */
    0 0 12px rgba(0, 0, 0, 0.06);
  /* [zh] 样式规则 `@include jelly.tab-bar-jelly-glass;` */
  @include jelly.tab-bar-jelly-glass;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tab-item {` */
.tab-item {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 1px;` */
  gap: 1px;
  /* [zh] 样式规则 `color: var(--m-ink-3, #8a8a8f);` */
  color: var(--m-ink-3, #8a8a8f);
  /* [zh] 样式规则 `text-decoration: none;` */
  text-decoration: none;
  /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
  -webkit-tap-highlight-color: transparent;
  /* [zh] 样式规则 `transition: color 0.25s ease;` */
  transition: color 0.25s ease;
  /* [zh] 样式规则 `padding: 0 4px;` */
  padding: 0 4px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `@include jelly.tab-bar-jelly-items;` */
  @include jelly.tab-bar-jelly-items;

  /* [zh] 样式规则 `.icon-wrap {` */
  .icon-wrap {
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `place-items: center;` */
    place-items: center;
    /* [zh] 样式规则 `width: 18px;` */
    width: 18px;
    /* [zh] 样式规则 `height: 18px;` */
    height: 18px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.label {` */
  .label {
    /* [zh] 样式规则 `font-size: 9px;` */
    font-size: 9px;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `transition: font-weight 0.2s;` */
    transition: font-weight 0.2s;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `color: var(--m-gold, #c9a962);` */
    color: var(--m-gold, #c9a962);

    /* [zh] 样式规则 `.label {` */
    .label {
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.icon-wrap {` */
    .icon-wrap {
      /* [zh] 样式规则 `transform: scale(1.08);` */
      transform: scale(1.08);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.el-icon {` */
    .el-icon {
      /* [zh] 样式规则 `color: var(--m-gold, #c9a962);` */
      color: var(--m-gold, #c9a962);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `:deep(.el-badge__content) {` */
:deep(.el-badge__content) {
  /* [zh] 样式规则 `border: 2px solid rgba(255, 255, 255, 0.` */
  border: 2px solid rgba(255, 255, 255, 0.72);
  /* [zh] 样式规则 `background: $color-primary;` */
  background: $color-primary;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `font-size: 10px;` */
  font-size: 10px;
/* [zh] 样式规则 `}` */
}
</style>
