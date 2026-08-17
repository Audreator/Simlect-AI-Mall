<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-page-embed pc-cart-embed">
    <!-- [zh] 开始标签 `<CartView>` -->
    <CartView />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import CartView from '@/views/CartView.vue';
</script>
