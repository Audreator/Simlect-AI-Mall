<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="card">
    <!-- [zh] 开始标签 `<h2>` -->
    <h2>{{ title }}</h2>
    <!-- [zh] 开始标签 `<p>` -->
    <p>{{ desc }}</p>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 语句
defineProps<{ title: string; desc: string }>();
</script>
