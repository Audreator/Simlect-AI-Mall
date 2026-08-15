
<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { getCurrentInstance, onMounted } from 'vue'
// [zh] 导入 `@vueuse/core`
import { useWebSocket } from '@vueuse/core'
// [zh] 导入 `@/stores/messagestore`
import { useMessageStore } from '@/stores/messagestore'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `messageStore`
const messageStore = useMessageStore()

// [zh] 声明变量 `wsClose`
let wsClose = null

// [zh] 声明变量 `parseMessage`
const parseMessage = (raw) => {
  // [zh] 代码块开始
  try {
    // [zh] return 返回
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return null
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `initWebSocket`
const initWebSocket = () => {
  // [zh] 声明变量 `wsUrl`
  const wsUrl = import.meta.env.VITE_WS
  // [zh] 条件判断 if
  if (!wsUrl) return

  // [zh] 代码块开始
  const { close } = useWebSocket(wsUrl.replace(/\?token=.*$/i, ''), {
    // [zh] 代码块开始
    onMessage: (_ws, { data }) => {
      // [zh] 声明变量 `payload`
      const payload = parseMessage(data)
      // [zh] 条件判断 if
      if (payload && payload !== 'pong') {
        // [zh] TypeScript/JS 语句：`messageStore.onMessage(payload)`
        messageStore.onMessage(payload)
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    onDisconnected: () => {
      // [zh] TypeScript/JS 语句：`console.log('websocket断开链接')`
      console.log('websocket断开链接')
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    autoReconnect: {
      // [zh] TypeScript/JS 语句：`retries: 3,`
      retries: 3,
      // [zh] TypeScript/JS 语句：`delay: 5000,`
      delay: 5000,
      // [zh] 代码块开始
      onFailed() {
        // [zh] TypeScript/JS 语句：`console.log('websocket链接失败')`
        console.log('websocket链接失败')
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    heartbeat: {
      // [zh] TypeScript/JS 语句：`message: 'ping',`
      message: 'ping',
      // [zh] TypeScript/JS 语句：`interval: 10000,`
      interval: 10000,
      // [zh] TypeScript/JS 语句：`pongTimeout: 3000,`
      pongTimeout: 3000,
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] TypeScript/JS 语句：`wsClose = close`
  wsClose = close
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] 代码块开始
  try {
    // [zh] TypeScript/JS 语句：`initWebSocket()`
    initWebSocket()
  // [zh] 代码块开始
  } catch (error) {
    // [zh] TypeScript/JS 语句：`console.error('初始化 WebSocket 失败:', error)`
    console.error('初始化 WebSocket 失败:', error)
    // [zh] TypeScript/JS 语句：`proxy?.Message?.error?.('WebSocket 连接初始化失败')`
    proxy?.Message?.error?.('WebSocket 连接初始化失败')
  // [zh] 语法符号
  }
// [zh] TypeScript/JS 语句：`})`
})
</script>
