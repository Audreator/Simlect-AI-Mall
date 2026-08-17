<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="agent-page ignore">
    <!-- [zh] 开始标签 `<AgentChatList>` -->
    <AgentChatList />
    <!-- [zh] 开始标签 `<AgentSendPanel>` -->
    <AgentSendPanel />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { useAgentSession } from '@/composables/useAgentSession';
import { useDevice } from '@/composables/useDevice';
import { useOpenAgent } from '@/composables/useOpenAgent';
import { recoverIosViewportZoom, syncVisualViewportHeight } from '@/utils/mobileViewport';
import AgentChatList from '@/views/agent/AgentChatList.vue';
import AgentSendPanel from '@/views/agent/AgentSendPanel.vue';

const { isDesktop } = useDevice();
const { openAgent } = useOpenAgent();
const { start, stop } = useAgentSession();

onMounted(async () => {
  if (isDesktop.value) {
    openAgent();
    return;
  }
  document.body.classList.add('ios-agent-immersive');
  syncVisualViewportHeight();
  await start();
});

onUnmounted(() => {
  if (isDesktop.value) return;
  document.body.classList.remove('ios-agent-immersive');
  recoverIosViewportZoom();
  stop();
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.agent-page {` */
.agent-page {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `flex: 1 1 auto;` */
  flex: 1 1 auto;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;

  /* [zh] 样式规则 `> .chat-scroll {` */
  > .chat-scroll {
    /* [zh] 样式规则 `flex: 1 1 0;` */
    flex: 1 1 0;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  :deep(.bubble),
  /* [zh] 样式声明 */
  :deep(.markdown-content),
  /* [zh] 样式声明 */
  :deep(.stream-text),
  /* [zh] 样式声明 */
  :deep(.typing),
  /* [zh] 样式声明 */
  :deep(.cancel-tip),
  /* [zh] 样式声明 */
  :deep(.biz-title),
  /* [zh] 样式声明 */
  :deep(.user-bubble .text),
  /* [zh] 样式声明 */
  :deep(.welcome),
  /* [zh] 样式声明 */
  :deep(.tip-chip),
  /* [zh] 样式声明 */
  :deep(.tips-label),
  /* [zh] 样式规则 `:deep(.float-label) {` */
  :deep(.float-label) {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.markdown-content table) {` */
  :deep(.markdown-content table) {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.agent-orders) {` */
  :deep(.agent-orders) {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
