<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<PcAuthShell>` -->
  <PcAuthShell v-if="pcLayout === 'auth'" class="pc-sub-layout ignore pc-surface">
    <!-- [zh] 开始标签 `<RouterView>` -->
    <RouterView v-slot="{ Component }">
      <!-- [zh] 开始标签 `<Transition>` -->
      <Transition name="page-fade" mode="out-in">
        <!-- [zh] 开始标签 `<component>` -->
        <component :is="Component" />
      <!-- [zh] 闭合标签 `</Transition>` -->
      </Transition>
    <!-- [zh] 闭合标签 `</RouterView>` -->
    </RouterView>
  <!-- [zh] 闭合标签 `</PcAuthShell>` -->
  </PcAuthShell>

  <!-- [zh] 开始标签 `<div>` -->
  <div v-else class="pc-sub-layout ignore pc-surface">
    <!-- [zh] 开始标签 `<SiteHeader>` -->
    <SiteHeader />
    <!-- [zh] 开始标签 `<main>` -->
    <main class="pc-sub-main">
      <!-- [zh] 开始标签 `<div>` -->
      <div
        class="pc-sub-container"
        :class="{
          'is-user-center': pcLayout === 'user',
          'is-agent-pc': route.path === '/ai-assistant'
        }"
      >
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="pcLayout === 'user'" class="user-center-layout">
          <!-- [zh] 开始标签 `<PcUserSidebar>` -->
          <PcUserSidebar />
          <!-- [zh] 开始标签 `<div>` -->
          <div class="user-center-content">
            <!-- [zh] 开始标签 `<header>` -->
            <header v-if="pageTitle" class="user-page-head">
              <!-- [zh] 开始标签 `<h1>` -->
              <h1>{{ pageTitle }}</h1>
            <!-- [zh] 闭合标签 `</header>` -->
            </header>
            <!-- [zh] 开始标签 `<RouterView>` -->
            <RouterView v-slot="{ Component }">
              <!-- [zh] 开始标签 `<Transition>` -->
              <Transition name="page-fade" mode="out-in">
                <!-- [zh] 开始标签 `<component>` -->
                <component :is="Component" />
              <!-- [zh] 闭合标签 `</Transition>` -->
              </Transition>
            <!-- [zh] 闭合标签 `</RouterView>` -->
            </RouterView>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-else>
          <!-- [zh] 开始标签 `<header>` -->
          <header v-if="showPlainPageHead" class="user-page-head plain-page-head">
            <!-- [zh] 开始标签 `<h1>` -->
            <h1>{{ pageTitle }}</h1>
          <!-- [zh] 闭合标签 `</header>` -->
          </header>
          <!-- [zh] 开始标签 `<RouterView>` -->
          <RouterView v-slot="{ Component }">
            <!-- [zh] 开始标签 `<Transition>` -->
            <Transition name="page-fade" mode="out-in">
              <!-- [zh] 开始标签 `<div>` -->
              <div
                class="pc-plain-body"
                :class="{
                  'is-agent-pc': route.path === '/ai-assistant',
                  'is-product-detail': isProductDetailPage
                }"
              >
                <!-- [zh] 开始标签 `<component>` -->
                <component :is="Component" />
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
            <!-- [zh] 闭合标签 `</Transition>` -->
            </Transition>
          <!-- [zh] 闭合标签 `</RouterView>` -->
          </RouterView>
        </template>
      </div>
    </main>
    <AppFooter />
    <PcFloatToolbar />
    <NotificationPopup @click="handleNotificationClick" />
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted } from 'vue';
// [zh] 导入 `vue-router`
import { RouterView, useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@/components/layout/AppFooter.vue`
import AppFooter from '@/components/layout/AppFooter.vue';
// [zh] 导入 `@/components/business/NotificationPopup.vue`
import NotificationPopup from '@/components/business/NotificationPopup.vue';
// [zh] 导入 `@/composables/useUnreadCount`
import { useUnreadCount } from '@/composables/useUnreadCount';
// [zh] 导入 `@/utils/notification`
import { navigateNotification, type NotificationData } from '@/utils/notification';
// [zh] 导入 `@/components/layout/PcAuthShell.vue`
import PcAuthShell from '@/components/layout/PcAuthShell.vue';
// [zh] 导入 `@/components/layout/PcFloatToolbar.vue`
import PcFloatToolbar from '@/components/layout/PcFloatToolbar.vue';
// [zh] 导入 `@/components/layout/PcUserSidebar.vue`
import PcUserSidebar from '@/components/layout/PcUserSidebar.vue';
// [zh] 导入 `@/components/layout/SiteHeader.vue`
import SiteHeader from '@/components/layout/SiteHeader.vue';
// [zh] 导入 `@/constants/pcUserNav`
import { resolvePcLayoutMode } from '@/constants/pcUserNav';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/stores/cart`
import { useCartStore } from '@/stores/cart';

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `cartStore`
const cartStore = useCartStore();
// [zh] 语句
const { refreshUnreadCount } = useUnreadCount();

// [zh] 声明变量 `pcLayout`
const pcLayout = computed(() => resolvePcLayoutMode(route.path));
// [zh] 声明变量 `pageTitle`
const pageTitle = computed(() => String(route.meta.title || ''));
// [zh] 声明变量 `isProductDetailPage`
const isProductDetailPage = computed(
  // [zh] TypeScript/JS 语句：`() => /^\/product\/[^/]+$/.test(route.path) && !route.p`
  () => /^\/product\/[^/]+$/.test(route.path) && !route.path.endsWith('/comments')
// [zh] 语句
);
// [zh] 声明变量 `showPlainPageHead`
const showPlainPageHead = computed(
  // [zh] TypeScript/JS 语句：`() => !!pageTitle.value && !route.meta.hidePcPageHead &`
  () => !!pageTitle.value && !route.meta.hidePcPageHead && !isProductDetailPage.value
// [zh] 语句
);

// [zh] 代码块开始
onMounted(() => {
  // [zh] 条件判断 if
  if (authStore.isLoggedIn) cartStore.fetchCartCount();
// [zh] 语句
});

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

/* [zh] 样式规则 `.pc-sub-layout.ignore {` */
.pc-sub-layout.ignore {

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

  /* [zh] 样式规则 `.pc-sub-main {` */
  .pc-sub-main {

    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;

    /* [zh] 样式规则 `padding: 16px 0 32px;` */
    padding: 16px 0 32px;

  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.pc-sub-container {` */
  .pc-sub-container {

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

    /* [zh] 样式规则 `&.is-user-center {` */
    &.is-user-center {

      /* [zh] 样式规则 `max-width: $content-max-width;` */
      max-width: $content-max-width;

    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(.page-nav-bar) {` */
    :deep(.page-nav-bar) {

      /* [zh] 样式规则 `margin-bottom: 16px;` */
      margin-bottom: 16px;

    /* [zh] 样式规则 `}` */
    }

  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-center-layout {` */
  .user-center-layout {

    /* [zh] 样式规则 `display: flex;` */
    display: flex;

    /* [zh] 样式规则 `align-items: flex-start;` */
    align-items: flex-start;

    /* [zh] 样式规则 `gap: 16px;` */
    gap: 16px;

  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-center-content {` */
  .user-center-content {

    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;

    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;

    /* [zh] 样式规则 `background: $color-card;` */
    background: $color-card;

    /* [zh] 样式规则 `border-radius: $radius-card;` */
    border-radius: $radius-card;

    /* [zh] 样式规则 `box-shadow: $shadow-card;` */
    box-shadow: $shadow-card;

    /* [zh] 样式规则 `padding: 16px 20px 20px;` */
    padding: 16px 20px 20px;

  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-page-head {` */
  .user-page-head {

    /* [zh] 样式规则 `margin-bottom: 16px;` */
    margin-bottom: 16px;

    /* [zh] 样式规则 `padding-bottom: 12px;` */
    padding-bottom: 12px;

    /* [zh] 样式规则 `border-bottom: 1px solid $color-border-g` */
    border-bottom: 1px solid $color-border-gray;

    /* [zh] 样式规则 `h1 {` */
    h1 {

      /* [zh] 样式规则 `margin: 0;` */
      margin: 0;

      /* [zh] 样式规则 `font-size: 18px;` */
      font-size: 18px;

      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;

      /* [zh] 样式规则 `color: $color-text-primary;` */
      color: $color-text-primary;

    /* [zh] 样式规则 `}` */
    }

  /* [zh] 样式规则 `}` */
  }

/* [zh] 样式规则 `}` */
}

</style>

