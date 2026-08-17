<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-config-provider>` -->
  <el-config-provider :locale="zhCn" :message="config">
    <!-- [zh] 开始标签 `<WebSocketProvider>` -->
    <WebSocketProvider />
    <!-- [zh] 开始标签 `<router-view>` -->
    <router-view></router-view>
  <!-- [zh] 闭合标签 `</el-config-provider>` -->
  </el-config-provider>
</template>

<script setup>
import { reactive, getCurrentInstance, onMounted } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useRouter, useRoute } from 'vue-router'
import { useDeviceStore } from '@/stores/device'
import WebSocketProvider from '@/components/WebSocketProvider.vue'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const device = useDeviceStore()
device.bind()

const config = reactive({
  max: 1,
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
</style>
