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

<script setup lang="ts">
defineProps<{ title: string; desc: string }>();
</script>
