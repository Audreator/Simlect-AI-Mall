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

<script setup lang="ts">
import { RouterLink } from 'vue-router';


const navLinks = [
  { label: '精选好物', path: '/' },
  { label: '全部分类', path: '/search' },
  { label: '优惠券', path: '/coupons' },
  { label: '签到有礼', path: '/sign' },
  { label: '会员中心', path: '/member-center' },
  { label: '我的收藏', path: '/wishlist' },
  { label: '我的订单', path: '/orders' },
  { label: '智能客服', path: '/ai-assistant' }
];
</script>
