
<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
</template>

<script setup>
import { getCurrentInstance, onMounted } from 'vue'
import { useWebSocket } from '@vueuse/core'
import { useMessageStore } from '@/stores/messagestore'

const { proxy } = getCurrentInstance()
const messageStore = useMessageStore()

let wsClose = null

const parseMessage = (raw) => {
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch {
    return null
  }
}

const initWebSocket = () => {
  const wsUrl = import.meta.env.VITE_WS
  if (!wsUrl) return

  const { close } = useWebSocket(wsUrl.replace(/\?token=.*$/i, ''), {
    onMessage: (_ws, { data }) => {
      const payload = parseMessage(data)
      if (payload && payload !== 'pong') {
        messageStore.onMessage(payload)
      }
    },
    onDisconnected: () => {
      console.log('websocket断开链接')
    },
    autoReconnect: {
      retries: 3,
      delay: 5000,
      onFailed() {
        console.log('websocket链接失败')
      },
    },
    heartbeat: {
      message: 'ping',
      interval: 10000,
      pongTimeout: 3000,
    },
  })
  wsClose = close
}

onMounted(() => {
  try {
    initWebSocket()
  } catch (error) {
    console.error('初始化 WebSocket 失败:', error)
    proxy?.Message?.error?.('WebSocket 连接初始化失败')
  }
})
</script>
