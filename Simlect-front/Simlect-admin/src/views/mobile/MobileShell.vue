<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="admin-mobile-root">

    <!-- [zh] 开始标签 `<header>` -->
    <header class="m-topbar glass-card glass-strong">
      <!-- [zh] 开始标签 `<button>` -->
      <button v-if="showBack" type="button" class="m-top-btn" aria-label="返回" @click="goBack">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="iconfont icon-down back-icon"></span>
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-top-title">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="m-title-text">{{ title }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="m-top-btn m-desktop-btn" title="切换到电脑版" @click="switchToDesktop">
        <!-- [zh] 模板内容：`电脑版` -->
        电脑版
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="m-top-btn" aria-label="退出" @click="logout">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="iconfont icon-setting"></span>
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</header>` -->
    </header>

    <!-- [zh] 开始标签 `<main>` -->
    <main class="m-content">
      <!-- [zh] 开始标签 `<router-view>` -->
      <router-view v-slot="{ Component }">
        <!-- [zh] 开始标签 `<transition>` -->
        <transition name="m-fade" mode="out-in">
          <keep-alive :max="10">
            <component :is="Component" />
          </keep-alive></transition>
      <!-- [zh] 闭合标签 `</router-view>` -->
      </router-view>
    <!-- [zh] 闭合标签 `</main>` -->
    </main>

    <!-- [zh] 开始标签 `<Teleport>` -->
    <Teleport to="body">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-tabbar-host" :class="{ 'is-jelly': barJelly }">
    <!-- [zh] 开始标签 `<LiquidGlassSurface>` -->
    <LiquidGlassSurface tag="nav" intensity="strong" class="m-tabbar" aria-label="底部导航">
      <!-- [zh] 开始标签 `<div>` -->
      <div
        class="m-tabbar-inner"
        :style="{ '--tab-count': tabs.length, '--active-index': activeTabIndex }"
      >
        <!-- [zh] 开始标签 `<LiquidGlassSurface>` -->
        <LiquidGlassSurface
          intensity="strong"
          variant="active"
          class="m-tab-active-glass"
          :class="{ 'is-jelly': glassJelly }"
          aria-hidden="true"
        />
        <!-- [zh] 开始标签 `<button>` -->
        <button
          v-for="tab in tabs"
          :key="tab.path"
          type="button"
          class="m-tab"
          :class="{ active: isTabActive(tab) }"
          @click="onTabClick(tab)"
        >
          <!-- [zh] 开始标签 `<span>` -->
          <span class="iconfont m-tab-icon" :class="`icon-${tab.icon}`"></span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="m-tab-label">{{ tab.label }}</span>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</LiquidGlassSurface>` -->
    </LiquidGlassSurface>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 闭合标签 `</Teleport>` -->
    </Teleport>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup>
import { computed, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { resolveDesktopPath, switchToDesktopView } from '@/utils/device'
import LiquidGlassSurface from '@/components/LiquidGlassSurface.vue'
import { useTabBarJelly } from '@/composables/useTabBarJelly.js'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const tabs = [
  { label: '工作台', path: '/m/home', icon: 'home' },
  { label: '商品', path: '/m/product', icon: 'product' },
  { label: '订单', path: '/m/order', icon: 'order' },
  { label: '用户', path: '/m/user', icon: 'user' },
  { label: '更多', path: '/m/more', icon: 'setting' }
]

const tabPaths = tabs.map((t) => t.path)
const title = computed(() => route.meta?.title || '简选运营')
const showBack = computed(() => !tabPaths.includes(route.path) && !!route.meta?.showBack)

const isTabActive = (tab) => {
  if (route.path === tab.path) return true

  return route.meta?.tab === tab.path
}

const activeTabIndex = computed(() => {
  const idx = tabs.findIndex((tab) => isTabActive(tab))
  return idx >= 0 ? idx : 0
})

const { barJelly, glassJelly, onTabPress } = useTabBarJelly(activeTabIndex)

const onTabClick = (tab) => {
  onTabPress()
  goTab(tab.path)
}

const goTab = (path) => {
  if (route.path !== path) router.push(path)
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/m/home')
}

const logout = () => {
  proxy.Confirm({
    message: '确定要退出登录吗?',
    okfun: async () => {
      await proxy.Request({ url: proxy.Api.logout })
      router.push('/login')
    }
  })
}

const switchToDesktop = () => {
  const desktopPath = resolveDesktopPath(route.path)
  proxy.Confirm({
    message: '切换到电脑版将使用桌面布局，在当前设备上可能需要横屏或左右滑动查看，确定继续吗？',
    okfun: () => switchToDesktopView(desktopPath),
  })
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `@use '@/styles/tab-bar-jelly' as jelly;` */
@use '@/styles/tab-bar-jelly' as jelly;

/* [zh] 样式规则 `.admin-mobile-root {` */
.admin-mobile-root {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `min-height: 100vh;` */
  min-height: 100vh;
  /* [zh] 样式规则 `min-height: 100dvh;` */
  min-height: 100dvh;
  /* [zh] 样式规则 `overflow-x: hidden;` */
  overflow-x: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-topbar {` */
.m-topbar {
  /* [zh] 样式规则 `position: sticky;` */
  position: sticky;
  /* [zh] 样式规则 `top: 0;` */
  top: 0;
  /* [zh] 样式规则 `z-index: 50;` */
  z-index: 50;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `height: 52px;` */
  height: 52px;
  /* [zh] 样式规则 `margin: 8px 10px 0;` */
  margin: 8px 10px 0;
  /* [zh] 样式规则 `padding: 0 8px;` */
  padding: 0 8px;
  /* [zh] 样式规则 `border-radius: 18px;` */
  border-radius: 18px;

  /* [zh] 样式规则 `.m-top-title {` */
  .m-top-title {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;

    /* [zh] 样式规则 `.m-title-text {` */
    .m-title-text {
      /* [zh] 样式规则 `font-size: 17px;` */
      font-size: 17px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `letter-spacing: 0.02em;` */
      letter-spacing: 0.02em;
      /* [zh] 样式规则 `color: var(--m-ink);` */
      color: var(--m-ink);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.m-top-btn {` */
  .m-top-btn {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `width: 38px;` */
    width: 38px;
    /* [zh] 样式规则 `height: 38px;` */
    height: 38px;
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `place-items: center;` */
    place-items: center;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `border-radius: 12px;` */
    border-radius: 12px;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.5);` */
    background: rgba(255, 255, 255, 0.5);
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `transition: background 0.2s, transform 0` */
    transition: background 0.2s, transform 0.15s;

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `transform: scale(0.92);` */
      transform: scale(0.92);
      /* [zh] 样式规则 `background: var(--m-gold-soft);` */
      background: var(--m-gold-soft);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.iconfont {` */
    .iconfont {
      /* [zh] 样式规则 `font-size: 18px;` */
      font-size: 18px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.back-icon {` */
    .back-icon {
      /* [zh] 样式规则 `transform: rotate(90deg);` */
      transform: rotate(90deg);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.m-desktop-btn {` */
  .m-desktop-btn {
    /* [zh] 样式规则 `width: auto;` */
    width: auto;
    /* [zh] 样式规则 `min-width: 52px;` */
    min-width: 52px;
    /* [zh] 样式规则 `padding: 0 10px;` */
    padding: 0 10px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--m-gold);` */
    color: var(--m-gold);
    /* [zh] 样式规则 `background: var(--m-gold-soft);` */
    background: var(--m-gold-soft);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-content {` */
.m-content {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `padding: 12px 10px calc(78px + env(safe-` */
  padding: 12px 10px calc(78px + env(safe-area-inset-bottom, 0));
  /* [zh] 样式规则 `overflow-x: auto;` */
  overflow-x: auto;

  /* [zh] 样式声明 */
  :deep(.table-panel),
  /* [zh] 样式声明 */
  :deep(.table-data-card),
  /* [zh] 样式声明 */
  :deep(.top-panel),
  /* [zh] 样式规则 `:deep(.form-style) {` */
  :deep(.form-style) {
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-tabbar-host {` */
.m-tabbar-host {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `left: 10px;` */
  left: 10px;
  /* [zh] 样式规则 `right: 10px;` */
  right: 10px;
  /* [zh] 样式规则 `bottom: calc(10px + env(safe-area-inset-` */
  bottom: calc(10px + env(safe-area-inset-bottom, 0));
  /* [zh] 样式规则 `z-index: 60;` */
  z-index: 60;
  /* [zh] 样式规则 `width: auto;` */
  width: auto;
  /* [zh] 样式规则 `max-width: 480px;` */
  max-width: 480px;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
  /* [zh] 样式规则 `@include jelly.tab-bar-jelly-host;` */
  @include jelly.tab-bar-jelly-host;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-tabbar-host .m-tabbar {` */
.m-tabbar-host .m-tabbar {
  /* [zh] 样式规则 `pointer-events: auto;` */
  pointer-events: auto;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-tabbar {` */
.m-tabbar {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `left: auto;` */
  left: auto;
  /* [zh] 样式规则 `right: auto;` */
  right: auto;
  /* [zh] 样式规则 `bottom: auto;` */
  bottom: auto;
  /* [zh] 样式规则 `transform: none;` */
  transform: none;
  /* [zh] 样式规则 `z-index: auto;` */
  z-index: auto;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: none;` */
  max-width: none;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 999px;` */
  border-radius: 999px;
  /* [zh] 样式规则 `@include jelly.tab-bar-jelly-shell;` */
  @include jelly.tab-bar-jelly-shell;
  /* [zh] 样式规则 `box-shadow:` */
  box-shadow:
    /* [zh] 样式声明 */
    0 6px 6px rgba(0, 0, 0, 0.18),
    /* [zh] 样式声明 */
    0 0 20px rgba(0, 0, 0, 0.08);

  /* [zh] 样式规则 `:deep(.liquid-glass-surface__content) {` */
  :deep(.liquid-glass-surface__content) {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-tabbar-inner {` */
.m-tabbar-inner {
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
  /* [zh] 样式规则 `height: 52px;` */
  height: 52px;
  /* [zh] 样式规则 `padding: 4px 6px;` */
  padding: 4px 6px;
  /* [zh] 样式规则 `overflow: visible;` */
  overflow: visible;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-tab-active-glass {` */
.m-tab-active-glass {
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

/* [zh] 样式规则 `.m-tab {` */
.m-tab {
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
  /* [zh] 样式规则 `gap: 2px;` */
  gap: 2px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: color 0.25s ease;` */
  transition: color 0.25s ease;
  /* [zh] 样式规则 `padding: 0 2px;` */
  padding: 0 2px;
  /* [zh] 样式规则 `@include jelly.tab-bar-jelly-items;` */
  @include jelly.tab-bar-jelly-items;

  /* [zh] 样式规则 `.m-tab-icon {` */
  .m-tab-icon {
    /* [zh] 样式规则 `font-size: 20px;` */
    font-size: 20px;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.m-tab-label {` */
  .m-tab-label {
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: none;` */
    transform: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `color: var(--m-gold);` */
    color: var(--m-gold);

    /* [zh] 样式规则 `.m-tab-icon {` */
    .m-tab-icon {
      /* [zh] 样式规则 `color: var(--m-gold);` */
      color: var(--m-gold);
      /* [zh] 样式规则 `transform: scale(1.06);` */
      transform: scale(1.06);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.m-tab-label {` */
    .m-tab-label {
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.m-fade-enter-active,
/* [zh] 样式规则 `.m-fade-leave-active {` */
.m-fade-leave-active {
  /* [zh] 样式规则 `transition: opacity 0.18s ease, transfor` */
  transition: opacity 0.18s ease, transform 0.18s ease;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-fade-enter-from {` */
.m-fade-enter-from {
  /* [zh] 样式规则 `opacity: 0;` */
  opacity: 0;
  /* [zh] 样式规则 `transform: translateY(6px);` */
  transform: translateY(6px);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-fade-leave-to {` */
.m-fade-leave-to {
  /* [zh] 样式规则 `opacity: 0;` */
  opacity: 0;
  /* [zh] 样式规则 `transform: translateY(-4px);` */
  transform: translateY(-4px);
/* [zh] 样式规则 `}` */
}
</style>
