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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { reactive, getCurrentInstance, onMounted } from 'vue'
// [zh] 导入 `element-plus/es/locale/lang/zh-cn`
import zhCn from 'element-plus/es/locale/lang/zh-cn'
// [zh] 导入 `vue-router`
import { useRouter, useRoute } from 'vue-router'
// [zh] 导入 `@/stores/device`
import { useDeviceStore } from '@/stores/device'
// [zh] 导入 `@/components/WebSocketProvider.vue`
import WebSocketProvider from '@/components/WebSocketProvider.vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()
// [zh] 声明变量 `route`
const route = useRoute()

// [zh] 声明变量 `device`
const device = useDeviceStore()
// [zh] TypeScript/JS 语句：`device.bind()`
device.bind()

// [zh] 声明变量 `config`
const config = reactive({
  // [zh] TypeScript/JS 语句：`max: 1,`
  max: 1,
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
</style>
