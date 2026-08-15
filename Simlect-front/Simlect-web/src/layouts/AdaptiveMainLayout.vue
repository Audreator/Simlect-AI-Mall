<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<component>` -->
  <component :is="layoutComponent" :key="platform" />
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted } from 'vue';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/layouts/MainLayout.vue`
import MainLayout from '@/layouts/MainLayout.vue';
// [zh] 导入 `@/layouts/PcMainLayout.vue`
import PcMainLayout from '@/layouts/PcMainLayout.vue';

// [zh] 语句
const { isMobile, platform, sync } = useDevice();

// [zh] 声明变量 `layoutComponent`
const layoutComponent = computed(() => (isMobile.value ? MainLayout : PcMainLayout));

// [zh] 代码块开始
onMounted(() => {
  // [zh] 语句
  sync();
// [zh] 语句
});
</script>
