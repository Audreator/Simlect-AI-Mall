<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<component>` -->
  <component :is="layoutComponent" :key="platform" />
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useDevice } from '@/composables/useDevice';
import MainLayout from '@/layouts/MainLayout.vue';
import PcMainLayout from '@/layouts/PcMainLayout.vue';

const { isMobile, platform, sync } = useDevice();

const layoutComponent = computed(() => (isMobile.value ? MainLayout : PcMainLayout));

onMounted(() => {
  sync();
});
</script>
