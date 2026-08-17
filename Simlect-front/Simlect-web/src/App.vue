<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-config-provider>` -->
  <el-config-provider :locale="zhCn" :size="elementSize">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="page-texture" />
    <!-- [zh] 开始标签 `<RouterView>` -->
    <RouterView />
    <!-- [zh] 开始标签 `<ProductSkuSheet>` -->
    <ProductSkuSheet />
    <!-- [zh] 开始标签 `<ImagePreviewHost>` -->
    <ImagePreviewHost />
    <!-- [zh] 开始标签 `<PcAgentFloatingPanel>` -->
    <PcAgentFloatingPanel v-if="isDesktop" />
  <!-- [zh] 闭合标签 `</el-config-provider>` -->
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterView } from 'vue-router';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import ProductSkuSheet from '@/components/business/ProductSkuSheet.vue';
import ImagePreviewHost from '@/components/common/ImagePreviewHost.vue';
import PcAgentFloatingPanel from '@/components/pc/PcAgentFloatingPanel.vue';
import { useDeviceStore } from './stores/device';
import { useAuthStore } from './stores/auth';
import { useAppWebSocket } from '@/composables/useAppWebSocket';

const deviceStore = useDeviceStore();

const elementSize = computed(() => (deviceStore.isDesktop ? 'small' : 'default'));
const isDesktop = computed(() => deviceStore.isDesktop);

useAuthStore().tryRestoreSession();
useAppWebSocket();

onMounted(() => {
  deviceStore.sync();
});
</script>
