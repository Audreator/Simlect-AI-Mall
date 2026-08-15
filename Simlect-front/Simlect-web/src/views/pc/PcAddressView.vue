<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-page-embed">
    <!-- [zh] 开始标签 `<AddressView>` -->
    <AddressView />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `@/views/AddressView.vue`
import AddressView from '@/views/AddressView.vue';
</script>
