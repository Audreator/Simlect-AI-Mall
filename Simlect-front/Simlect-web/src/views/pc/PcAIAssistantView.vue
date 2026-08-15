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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { onMounted, onUnmounted } from 'vue';
// [zh] 导入 `@/views/agent/AgentChatList.vue`
import AgentChatList from '@/views/agent/AgentChatList.vue';
// [zh] 导入 `@/views/agent/AgentSendPanel.vue`
import AgentSendPanel from '@/views/agent/AgentSendPanel.vue';
// [zh] 导入 `@/composables/useAgentSession`
import { useAgentSession } from '@/composables/useAgentSession';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/composables/useOpenAgent`
import { useOpenAgent } from '@/composables/useOpenAgent';

// [zh] 语句
const { isDesktop } = useDevice();
// [zh] 语句
const { openAgent } = useOpenAgent();
// [zh] 语句
const { start, stop } = useAgentSession();

// [zh] 代码块开始
onMounted(async () => {
  // [zh] 条件判断 if
  if (isDesktop.value) {
    // [zh] 语句
    openAgent();
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await start();
// [zh] 语句
});

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 条件判断 if
  if (!isDesktop.value) stop();
// [zh] 语句
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">

</style>
