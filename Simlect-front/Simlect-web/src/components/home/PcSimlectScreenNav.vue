<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="sl-screen-nav-wrap ignore">
    <!-- [zh] 开始标签 `<nav>` -->
    <nav class="sl-screen-nav" aria-label="快捷导航">
      <!-- [zh] 开始标签 `<ul>` -->
      <ul class="sl-nav-links">
        <!-- [zh] 开始标签 `<li>` -->
        <li v-for="item in navLinks" :key="item.path">
          <!-- [zh] 开始标签 `<RouterLink>` -->
          <RouterLink :to="item.path">{{ item.label }}</RouterLink>
        <!-- [zh] 闭合标签 `</li>` -->
        </li>
      <!-- [zh] 闭合标签 `</ul>` -->
      </ul>
    <!-- [zh] 闭合标签 `</nav>` -->
    </nav>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue-router`
import { RouterLink } from 'vue-router';


// [zh] 声明变量 `navLinks`
const navLinks = [
  // [zh] TypeScript/JS 语句：`{ label: '精选好物', path: '/' },`
  { label: '精选好物', path: '/' },
  // [zh] TypeScript/JS 语句：`{ label: '全部分类', path: '/search' },`
  { label: '全部分类', path: '/search' },
  // [zh] TypeScript/JS 语句：`{ label: '优惠券', path: '/coupons' },`
  { label: '优惠券', path: '/coupons' },
  // [zh] TypeScript/JS 语句：`{ label: '签到有礼', path: '/sign' },`
  { label: '签到有礼', path: '/sign' },
  // [zh] TypeScript/JS 语句：`{ label: '会员中心', path: '/member-center' },`
  { label: '会员中心', path: '/member-center' },
  // [zh] TypeScript/JS 语句：`{ label: '我的收藏', path: '/wishlist' },`
  { label: '我的收藏', path: '/wishlist' },
  // [zh] TypeScript/JS 语句：`{ label: '我的订单', path: '/orders' },`
  { label: '我的订单', path: '/orders' },
  // [zh] TypeScript/JS 语句：`{ label: '智能客服', path: '/ai-assistant' }`
  { label: '智能客服', path: '/ai-assistant' }
// [zh] 语句
];
</script>
