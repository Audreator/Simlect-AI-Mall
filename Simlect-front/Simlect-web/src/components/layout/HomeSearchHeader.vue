<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<LiquidGlassSurface>` -->
  <LiquidGlassSurface tag="header" intensity="medium" class="home-search-header ignore">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="home-search-header__row">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="search-bar">
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="search-field" @click="goSearchPortal">
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon class="search-icon" :size="19"><Search /></el-icon>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="placeholder">{{ placeholder }}</span>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="home-search-header__actions">
        <!-- [zh] 开始标签 `<button>` -->
        <button
          v-if="authStore.isLoggedIn"
          type="button"
          class="icon-btn"
          aria-label="消息"
          @click="goNotifications"
        >
          <!-- [zh] 开始标签 `<el-badge>` -->
          <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
            <!-- [zh] 开始标签 `<el-icon>` -->
            <el-icon :size="20"><Bell /></el-icon>
          <!-- [zh] 闭合标签 `</el-badge>` -->
          </el-badge>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="icon-btn" aria-label="智能客服" @click="goAgent">
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon :size="20"><ChatDotRound /></el-icon>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<CategoryNavCard>` -->
    <CategoryNavCard />
  <!-- [zh] 闭合标签 `</LiquidGlassSurface>` -->
  </LiquidGlassSurface>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `@/components/common/LiquidGlassSurface.vue`
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
// [zh] 导入 `@/components/business/CategoryNavCard.vue`
import CategoryNavCard from '@/components/business/CategoryNavCard.vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { Bell, ChatDotRound, Search } from '@element-plus/icons-vue';
// [zh] 导入 `@/composables/useOpenAgent`
import { useOpenAgent } from '@/composables/useOpenAgent';
// [zh] 导入 `@/composables/useUnreadCount`
import { useUnreadCount } from '@/composables/useUnreadCount';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';

// [zh] TypeScript/JS 语句：`withDefaults(`
withDefaults(
  // [zh] 代码块开始
  defineProps<{
    // [zh] 语句
    placeholder?: string;
  // [zh] TypeScript/JS 语句：`}>(),`
  }>(),
  // [zh] TypeScript/JS 语句：`{ placeholder: '搜索商品/品牌' }`
  { placeholder: '搜索商品/品牌' }
// [zh] 语句
);

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 语句
const { openAgent } = useOpenAgent();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 语句
const { unreadCount } = useUnreadCount();

// [zh] 声明变量 `goSearchPortal`
const goSearchPortal = () => router.push('/search-portal');
// [zh] 声明变量 `goNotifications`
const goNotifications = () => router.push('/notifications');
// [zh] 声明变量 `goAgent`
const goAgent = () => openAgent();
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.home-search-header {` */
.home-search-header {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `z-index: 1001;` */
  z-index: 1001;
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `top: 0;` */
  top: 0;
  /* [zh] 样式规则 `left: 0;` */
  left: 0;
  /* [zh] 样式规则 `right: 0;` */
  right: 0;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `padding-top: env(safe-area-inset-top, 0)` */
  padding-top: env(safe-area-inset-top, 0);
  /* [zh] 样式规则 `padding-bottom: 6px;` */
  padding-bottom: 6px;
  /* [zh] 样式规则 `min-height: calc($home-search-bar-height` */
  min-height: calc($home-search-bar-height + env(safe-area-inset-top, 0));
  /* [zh] 样式规则 `border-radius: 0;` */
  border-radius: 0;
  /* [zh] 样式规则 `border-bottom: 1px solid var(--glass-bor` */
  border-bottom: 1px solid var(--glass-border-soft);
  /* [zh] 样式规则 `box-shadow: var(--glass-shadow-sm);` */
  box-shadow: var(--glass-shadow-sm);

  /* [zh] 样式规则 `:deep(.liquid-glass-surface__content) {` */
  :deep(.liquid-glass-surface__content) {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.home-search-header__row {` */
.home-search-header__row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `min-height: 42px;` */
  min-height: 42px;
  /* [zh] 样式规则 `margin-top: 10px;` */
  margin-top: 10px;
  /* [zh] 样式规则 `padding: 0 $app-page-gutter;` */
  padding: 0 $app-page-gutter;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.search-bar {` */
.search-bar {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.search-field {` */
.search-field {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `height: 42px;` */
  height: 42px;
  /* [zh] 样式规则 `padding: 0 16px;` */
  padding: 0 16px;
  /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
  border: 1px solid rgba(120, 120, 128, 0.12);
  /* [zh] 样式规则 `border-radius: 14px;` */
  border-radius: 14px;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.55);` */
  background: rgba(255, 255, 255, 0.55);
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;
  /* [zh] 样式规则 `transition: background 0.2s ease, border` */
  transition: background 0.2s ease, border-color 0.2s ease;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.72);` */
    background: rgba(255, 255, 255, 0.72);
    /* [zh] 样式规则 `border-color: rgba(120, 120, 128, 0.18);` */
    border-color: rgba(120, 120, 128, 0.18);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-icon {` */
  .search-icon {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.placeholder {` */
  .placeholder {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.home-search-header__actions {` */
.home-search-header__actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.icon-btn {` */
.icon-btn {
  /* [zh] 样式规则 `width: 40px;` */
  width: 40px;
  /* [zh] 样式规则 `height: 40px;` */
  height: 40px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.38);` */
  background: rgba(255, 255, 255, 0.38);
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `transition: background 0.2s ease, transf` */
  transition: background 0.2s ease, transform 0.15s ease;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.62);` */
    background: rgba(255, 255, 255, 0.62);
    /* [zh] 样式规则 `transform: scale(0.96);` */
    transform: scale(0.96);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
