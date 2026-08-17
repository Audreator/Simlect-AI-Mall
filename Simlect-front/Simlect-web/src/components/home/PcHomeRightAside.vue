<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<aside>` -->
  <aside class="pc-home-aside ignore" aria-label="快捷服务">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="user-card">
      <!-- [zh] 开始标签 `<el-avatar>` -->
      <el-avatar :size="48" :src="avatarUrl" class="user-avatar">
        <!-- [zh] Mustache 插值表达式 -->
        {{ avatarLetter }}
      <!-- [zh] 闭合标签 `</el-avatar>` -->
      </el-avatar>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="authStore.isLoggedIn" class="greet">
        <!-- [zh] 模板内容：`您好，<strong>{{ authStore.userInfo?.nickName ||` -->
        您好，<strong>{{ authStore.userInfo?.nickName || '用户' }}</strong>
      </p>
      <p v-else class="greet">登录后享更多优惠</p>
      <RouterLink
        v-if="!authStore.isLoggedIn"
        class="btn-login"
        to="/login"
      >
        立即登录
      </RouterLink>
      <template v-else>
        <p v-if="authStore.memberLevelName" class="member-line">{{ authStore.memberLevelName }} · 成长值 {{ authStore.memberGrowthValue }}</p>
        <RouterLink class="btn-login outline" to="/account">个人中心</RouterLink>
        <div class="member-links">
          <RouterLink to="/member-center" class="mini-link">会员中心</RouterLink>
          <RouterLink to="/notifications" class="mini-link">
            消息<span v-if="unreadCount" class="mini-badge">{{ unreadCount }}</span>
          </RouterLink>
        </div>
      </template>
    </div>

    <nav class="quick-grid" aria-label="功能入口">
      <button
        v-for="item in quickItems"
        :key="item.path"
        type="button"
        class="quick-item"
        @click="onQuickClick(item)"
      >
        <el-icon :size="20" class="quick-icon"><component :is="item.icon" /></el-icon>
        <span class="quick-label">{{ item.label }}</span>
      </button>
    </nav>

    <div class="link-row">
      <RouterLink v-for="link in textLinks" :key="link.path" :to="link.path" class="link-chip">
        {{ link.label }}
      </RouterLink>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { useUnreadCount } from '@/composables/useUnreadCount';
import {
  ChatDotRound,
  List,
  ShoppingCart,
  Star,
  Ticket,
  Present
} from '@element-plus/icons-vue';
import { useOpenAgent } from '@/composables/useOpenAgent';
import { useAuthStore } from '@/stores/auth';
import { resolveAvatarUrl } from '@/utils/image';

const router = useRouter();
const { openAgent } = useOpenAgent();

const onQuickClick = (item: { path: string }) => {
  if (item.path === '/ai-assistant') {
    openAgent();
    return;
  }
  router.push(item.path);
};
const authStore = useAuthStore();
const { unreadCount } = useUnreadCount();

const avatarUrl = computed(() => resolveAvatarUrl(authStore.userInfo?.avatar));
const avatarLetter = computed(() => (authStore.userInfo?.nickName || '访')[0]);

const quickItems = [
  { label: '订单', path: '/orders', icon: List },
  { label: '购物车', path: '/cart', icon: ShoppingCart },
  { label: '优惠券', path: '/coupons', icon: Ticket },
  { label: '收藏', path: '/wishlist', icon: Star },
  { label: '签到', path: '/sign', icon: Present },
  { label: '客服', path: '/ai-assistant', icon: ChatDotRound }
];

const textLinks = [
  { label: '领券中心', path: '/coupons' },
  { label: '我的券', path: '/my-coupons' },
  { label: '足迹', path: '/footprint' }
];

onMounted(async () => {
  if (!authStore.isLoggedIn) return;
  try {
    await authStore.loadMemberCenter();
  } catch {

  }
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-home-aside.ignore {` */
.pc-home-aside.ignore {
  /* [zh] 样式规则 `width: 180px;` */
  width: 180px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;

  /* [zh] 样式规则 `.user-card {` */
  .user-card {
    /* [zh] 样式规则 `padding: 14px 12px;` */
    padding: 14px 12px;
    /* [zh] 样式规则 `background: $color-card;` */
    background: $color-card;
    /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
    border: 1px solid $color-border-gray;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-avatar {` */
  .user-avatar {
    /* [zh] 样式规则 `margin: 0 auto 8px;` */
    margin: 0 auto 8px;
    /* [zh] 样式规则 `background: $color-primary-soft;` */
    background: $color-primary-soft;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.greet {` */
  .greet {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `word-break: break-all;` */
    word-break: break-all;

    /* [zh] 样式规则 `strong {` */
    strong {
      /* [zh] 样式规则 `color: $color-text-primary;` */
      color: $color-text-primary;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.member-line {` */
  .member-line {
    /* [zh] 样式规则 `margin: 0 0 8px;` */
    margin: 0 0 8px;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.member-links {` */
  .member-links {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `margin-top: 8px;` */
    margin-top: 8px;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.mini-link {` */
  .mini-link {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
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

  /* [zh] 样式规则 `.mini-badge {` */
  .mini-badge {
    /* [zh] 样式规则 `margin-left: 2px;` */
    margin-left: 2px;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.btn-login {` */
  .btn-login {
    /* [zh] 样式规则 `display: block;` */
    display: block;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `padding: 7px 0;` */
    padding: 7px 0;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `background: $color-primary;` */
    background: $color-primary;
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
    /* [zh] 样式规则 `box-sizing: border-box;` */
    box-sizing: border-box;

    /* [zh] 样式规则 `&.outline {` */
    &.outline {
      /* [zh] 样式规则 `background: #fff;` */
      background: #fff;
      /* [zh] 样式规则 `border: 1px solid $color-primary;` */
      border: 1px solid $color-primary;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.quick-grid {` */
  .quick-grid {
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `grid-template-columns: repeat(3, 1fr);` */
    grid-template-columns: repeat(3, 1fr);
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;
    /* [zh] 样式规则 `padding: 10px 8px;` */
    padding: 10px 8px;
    /* [zh] 样式规则 `background: $color-card;` */
    background: $color-card;
    /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
    border: 1px solid $color-border-gray;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.quick-item {` */
  .quick-item {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 2px;` */
    gap: 2px;
    /* [zh] 样式规则 `padding: 6px 2px;` */
    padding: 6px 2px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: $color-cat-hover-bg;` */
      background: $color-cat-hover-bg;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.quick-icon {` */
  .quick-icon {
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.quick-label {` */
  .quick-label {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.link-row {` */
  .link-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
    /* [zh] 样式规则 `padding: 10px;` */
    padding: 10px;
    /* [zh] 样式规则 `background: $color-card;` */
    background: $color-card;
    /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
    border: 1px solid $color-border-gray;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.link-chip {` */
  .link-chip {
    /* [zh] 样式规则 `flex: 1 1 auto;` */
    flex: 1 1 auto;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `padding: 4px 8px;` */
    padding: 4px 8px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `line-height: 1.3;` */
    line-height: 1.3;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `background: #fafafa;` */
    background: #fafafa;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `background: $color-cat-hover-bg;` */
      background: $color-cat-hover-bg;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
