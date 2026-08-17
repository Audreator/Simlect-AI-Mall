<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-agent-page agent-page ignore">
    <!-- [zh] 开始标签 `<AgentChatList>` -->
    <AgentChatList />
    <!-- [zh] 开始标签 `<AgentSendPanel>` -->
    <AgentSendPanel />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import AgentChatList from '@/views/agent/AgentChatList.vue';
import AgentSendPanel from '@/views/agent/AgentSendPanel.vue';
import { useAgentSession } from '@/composables/useAgentSession';
import { useDevice } from '@/composables/useDevice';
import { useOpenAgent } from '@/composables/useOpenAgent';

const { isDesktop } = useDevice();
const { openAgent } = useOpenAgent();
const { start, stop } = useAgentSession();

onMounted(async () => {
  if (isDesktop.value) {
    openAgent();
    return;
  }
  await start();
});

onUnmounted(() => {
  if (!isDesktop.value) stop();
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">

</style>
