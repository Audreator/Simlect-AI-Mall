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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted } from 'vue';
// [zh] 导入 `vue-router`
import { RouterView } from 'vue-router';
// [zh] 导入 `element-plus/es/locale/lang/zh-cn`
import zhCn from 'element-plus/es/locale/lang/zh-cn';
// [zh] 导入 `@/components/business/ProductSkuSheet.vue`
import ProductSkuSheet from '@/components/business/ProductSkuSheet.vue';
// [zh] 导入 `@/components/common/ImagePreviewHost.vue`
import ImagePreviewHost from '@/components/common/ImagePreviewHost.vue';
// [zh] 导入 `@/components/pc/PcAgentFloatingPanel.vue`
import PcAgentFloatingPanel from '@/components/pc/PcAgentFloatingPanel.vue';
// [zh] 导入 `./stores/device`
import { useDeviceStore } from './stores/device';
// [zh] 导入 `./stores/auth`
import { useAuthStore } from './stores/auth';
// [zh] 导入 `@/composables/useAppWebSocket`
import { useAppWebSocket } from '@/composables/useAppWebSocket';

// [zh] 声明变量 `deviceStore`
const deviceStore = useDeviceStore();

// [zh] 声明变量 `elementSize`
const elementSize = computed(() => (deviceStore.isDesktop ? 'small' : 'default'));
// [zh] 声明变量 `isDesktop`
const isDesktop = computed(() => deviceStore.isDesktop);

// [zh] 语句
useAuthStore().tryRestoreSession();
// [zh] 语句
useAppWebSocket();

// [zh] 代码块开始
onMounted(() => {
  // [zh] 语句
  deviceStore.sync();
// [zh] 语句
});
</script>
