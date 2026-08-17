<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-page-embed">
    <!-- [zh] 开始标签 `<CouponsView>` -->
    <CouponsView />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import CouponsView from '@/views/CouponsView.vue';
</script>
