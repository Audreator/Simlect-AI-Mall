<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<aside>` -->
  <aside class="pc-float-toolbar ignore" aria-label="快捷工具">
    <!-- [zh] 开始标签 `<RouterLink>` -->
    <RouterLink to="/account" class="tool-item" title="个人中心">
      <!-- [zh] 开始标签 `<el-icon>` -->
      <el-icon :size="20"><User /></el-icon>
      <!-- [zh] 开始标签 `<span>` -->
      <span>我的</span>
    <!-- [zh] 闭合标签 `</RouterLink>` -->
    </RouterLink>
    <!-- [zh] 开始标签 `<RouterLink>` -->
    <RouterLink to="/cart" class="tool-item" title="购物车">
      <!-- [zh] 开始标签 `<el-badge>` -->
      <el-badge :value="cartStore.cartCount" :hidden="!cartStore.cartCount" :max="99">
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon :size="20"><ShoppingCart /></el-icon>
      <!-- [zh] 闭合标签 `</el-badge>` -->
      </el-badge>
      <!-- [zh] 开始标签 `<span>` -->
      <span>购物车</span>
    <!-- [zh] 闭合标签 `</RouterLink>` -->
    </RouterLink>
    <!-- [zh] 开始标签 `<RouterLink>` -->
    <RouterLink to="/wishlist" class="tool-item" title="收藏夹">
      <!-- [zh] 开始标签 `<el-icon>` -->
      <el-icon :size="20"><Star /></el-icon>
      <!-- [zh] 开始标签 `<span>` -->
      <span>收藏</span>
    <!-- [zh] 闭合标签 `</RouterLink>` -->
    </RouterLink>
    <!-- [zh] 开始标签 `<RouterLink>` -->
    <RouterLink to="/footprint" class="tool-item" title="足迹">
      <!-- [zh] 开始标签 `<el-icon>` -->
      <el-icon :size="20"><Clock /></el-icon>
      <!-- [zh] 开始标签 `<span>` -->
      <span>足迹</span>
    <!-- [zh] 闭合标签 `</RouterLink>` -->
    </RouterLink>
    <!-- [zh] 开始标签 `<RouterLink>` -->
    <RouterLink v-if="authStore.isLoggedIn" to="/notifications" class="tool-item" title="消息">
      <!-- [zh] 开始标签 `<el-badge>` -->
      <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon :size="20"><Bell /></el-icon>
      <!-- [zh] 闭合标签 `</el-badge>` -->
      </el-badge>
      <!-- [zh] 开始标签 `<span>` -->
      <span>消息</span>
    <!-- [zh] 闭合标签 `</RouterLink>` -->
    </RouterLink>
    <!-- [zh] 开始标签 `<button>` -->
    <button type="button" class="tool-item" title="智能客服" @click="openAgent()">
      <!-- [zh] 开始标签 `<el-icon>` -->
      <el-icon :size="20"><ChatDotRound /></el-icon>
      <!-- [zh] 开始标签 `<span>` -->
      <span>客服</span>
    <!-- [zh] 闭合标签 `</button>` -->
    </button>
    <!-- [zh] 开始标签 `<button>` -->
    <button type="button" class="tool-item" title="回到顶部" @click="scrollTop">
      <!-- [zh] 开始标签 `<el-icon>` -->
      <el-icon :size="20"><Top /></el-icon>
      <!-- [zh] 开始标签 `<span>` -->
      <span>顶部</span>
    <!-- [zh] 闭合标签 `</button>` -->
    </button>
  <!-- [zh] 闭合标签 `</aside>` -->
  </aside>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { onMounted } from 'vue';
// [zh] 导入 `vue-router`
import { RouterLink } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { Bell, ChatDotRound, Clock, ShoppingCart, Star, Top, User } from '@element-plus/icons-vue';
// [zh] 导入 `@/composables/useUnreadCount`
import { useUnreadCount } from '@/composables/useUnreadCount';
// [zh] 导入 `@/composables/useOpenAgent`
import { useOpenAgent } from '@/composables/useOpenAgent';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/stores/cart`
import { useCartStore } from '@/stores/cart';

// [zh] 语句
const { openAgent } = useOpenAgent();

// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `cartStore`
const cartStore = useCartStore();
// [zh] 语句
const { unreadCount } = useUnreadCount();

// [zh] 声明变量 `scrollTop`
const scrollTop = () => {
  // [zh] 语句
  window.scrollTo({ top: 0, behavior: 'smooth' });
// [zh] 语句
};

// [zh] 代码块开始
onMounted(() => {
  // [zh] 条件判断 if
  if (authStore.isLoggedIn) cartStore.fetchCartCount();
// [zh] 语句
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-float-toolbar.ignore {` */
.pc-float-toolbar.ignore {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `right: 0;` */
  right: 0;
  /* [zh] 样式规则 `top: 50%;` */
  top: 50%;
  /* [zh] 样式规则 `transform: translateY(-50%);` */
  transform: translateY(-50%);
  /* [zh] 样式规则 `z-index: 900;` */
  z-index: 900;
  /* [zh] 样式规则 `width: 54px;` */
  width: 54px;
  /* [zh] 样式规则 `padding: 8px 0;` */
  padding: 8px 0;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.96);` */
  background: rgba(255, 255, 255, 0.96);
  /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
  border: 1px solid $color-border-gray;
  /* [zh] 样式规则 `border-right: none;` */
  border-right: none;
  /* [zh] 样式规则 `border-radius: $radius-sm 0 0 $radius-sm` */
  border-radius: $radius-sm 0 0 $radius-sm;
  /* [zh] 样式规则 `box-shadow: -2px 0 8px rgba(0, 0, 0, 0.0` */
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.06);
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 2px;` */
  gap: 2px;

  /* [zh] 样式规则 `.tool-item {` */
  .tool-item {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 2px;` */
    gap: 2px;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `padding: 8px 4px;` */
    padding: 8px 4px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `transition: color $transition-fast, back` */
    transition: color $transition-fast, background $transition-fast;

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
