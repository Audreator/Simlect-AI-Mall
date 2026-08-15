<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<main>` -->
  <main
    ref="listRef"
    class="chat-scroll"
    :class="{ 'is-embedded-composer': composerEmbedded }"
    @scroll="onListScroll"
  >
    <!-- [zh] 开始标签 `<div>` -->
    <div class="chat-scroll-inner">
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="loadingHistory && !messageList.length" class="history-loading">历史消息加载中…</p>
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-else-if="historyLoadFailed && !messageList.length"
        type="button"
        class="history-error"
        @click="reloadHistory"
      >
        <!-- [zh] 模板内容：`历史消息加载失败，点击重试` -->
        历史消息加载失败，点击重试
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else-if="!messageList.length && !loadingHistory" class="welcome">
        <!-- [zh] 模板内容：`您好，我是简选智能客服，可以帮您查商品、查订单、推荐精选。` -->
        您好，我是简选智能客服，可以帮您查商品、查订单、推荐精选。
      <!-- [zh] 闭合标签 `</p>` -->
      </p>
      <!-- [zh] 开始标签 `<p>` -->
      <p
        v-if="messageList.length && pageNo >= pageTotal && !loadingHistory"
        <!-- [zh] 模板内容：`class="history-top-tip"` -->
        class="history-top-tip"
      <!-- [zh] 模板内容：`>` -->
      >
        <!-- [zh] 模板内容：`没有更多历史消息了` -->
        没有更多历史消息了
      <!-- [zh] 闭合标签 `</p>` -->
      </p>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else-if="loadingHistory && messageList.length" class="history-top-tip">加载更早的消息…</p>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="(item, index) in messageList" :key="item.messageId || `msg-${index}`" class="msg-group">
        <!-- [zh] 开始标签 `<AgentUserBubble>` -->
        <AgentUserBubble v-if="item.userMessage" :user-message="item.userMessage" />
        <!-- [zh] 开始标签 `<AgentChatItem>` -->
        <AgentChatItem
          v-if="shouldShowAi(item)"
          :data="item"
          :waiting="Number(item.status) === 1 && streamWaiting && item === currentMessage"
        />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</main>` -->
  </main>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { inject, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
// [zh] 导入 `@/api/modules`
import { agentApi } from '@/api/modules';
// [zh] 导入 `@/composables/agentEmbed`
import { agentComposerEmbeddedKey } from '@/composables/agentEmbed';
// [zh] 导入 `@/components/agent/AgentChatItem.vue`
import AgentChatItem from '@/components/agent/AgentChatItem.vue';
// [zh] 导入 `@/components/agent/AgentUserBubble.vue`
import AgentUserBubble from '@/components/agent/AgentUserBubble.vue';
// [zh] 导入 `@/stores/agentMessage`
import { useAgentMessageStore } from '@/stores/agentMessage';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/constants/backendEnums`
import { AGENT_OUTPUT_TYPE } from '@/constants/backendEnums';
// [zh] 导入 `@/utils/eventBus`
import { mitter } from '@/utils/eventBus';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`extractHistoryPage,`
  extractHistoryPage,
  // [zh] TypeScript/JS 语句：`mergeHistoryMessages,`
  mergeHistoryMessages,
  // [zh] TypeScript/JS 语句：`normalizeAgentHistoryMessage,`
  normalizeAgentHistoryMessage,
  // [zh] TypeScript/JS 语句：`sortHistoryMessages,`
  sortHistoryMessages,
  // [zh] TypeScript 类型 `AgentHistoryMessage`（类比 Java interface/DTO）
  type AgentHistoryMessage
// [zh] 语句
} from '@/utils/agentHistory';

// [zh] 声明变量 `composerEmbedded`
const composerEmbedded = inject(agentComposerEmbeddedKey, false);
// [zh] 声明变量 `agentMessageStore`
const agentMessageStore = useAgentMessageStore();
// [zh] 声明变量 `listRef`
const listRef = ref<HTMLElement>();
// [zh] 声明变量 `loadingHistory`
const loadingHistory = ref(false);
// [zh] 声明变量 `historyLoadFailed`
const historyLoadFailed = ref(false);
// [zh] 声明变量 `answering`
const answering = ref(false);
// [zh] 声明变量 `streamWaiting`
const streamWaiting = ref(false);
// [zh] 声明变量 `currentMessage`
const currentMessage = ref<AgentHistoryMessage | null>(null);

// [zh] 声明变量 `messageList`
const messageList = ref<AgentHistoryMessage[]>([]);
// [zh] 声明变量 `pageNo`
const pageNo = ref(0);
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(0);

// [zh] 声明变量 `maxMessageId`
let maxMessageId: number | null = null;
// [zh] 声明变量 `oldScrollHeight`
let oldScrollHeight = 0;
// [zh] 声明变量 `initialLoadDone`
let initialLoadDone = false;

// [zh] 声明变量 `shouldShowAi`
const shouldShowAi = (item: AgentHistoryMessage) => {
  // [zh] 声明变量 `status`
  const status = Number(item.status);
  // [zh] 条件判断 if
  if (status === 0 || status === 1 || status === 3) return true;
  // [zh] return 返回
  return !!(item.assistantMessage || '').trim();
// [zh] 语句
};

// [zh] 声明变量 `stickToBottom`
const stickToBottom = ref(true);

// [zh] 声明变量 `isNearBottom`
const isNearBottom = (el: HTMLElement, threshold = 80) =>
  // [zh] 语句
  el.scrollHeight - el.scrollTop - el.clientHeight <= threshold;

// [zh] 声明变量 `onListScroll`
const onListScroll = () => {
  // [zh] 声明变量 `el`
  const el = listRef.value;
  // [zh] 条件判断 if
  if (!el) return;
  // [zh] 语句
  stickToBottom.value = isNearBottom(el);
  // [zh] 条件判断 if
  if (!initialLoadDone || loadingHistory.value) return;
  // [zh] 条件判断 if
  if (el.scrollTop > 8) return;
  // [zh] 条件判断 if
  if (pageNo.value >= pageTotal.value) return;
  // [zh] 语句
  oldScrollHeight = el.scrollHeight;
  // [zh] 语句
  void loadHistoryMessage();
// [zh] 语句
};

// [zh] 声明变量 `resolveActiveMessage`
const resolveActiveMessage = (messageId?: number | string) => {
  // [zh] 条件判断 if
  if (messageId == null) return currentMessage.value;
  // [zh] 条件判断 if
  if (currentMessage.value && String(currentMessage.value.messageId) === String(messageId)) {
    // [zh] return 返回
    return currentMessage.value;
  // [zh] 语法符号
  }
  // [zh] return 返回
  return messageList.value.find((m) => String(m.messageId) === String(messageId)) ?? null;
// [zh] 语句
};

// [zh] 声明变量 `finishAnswering`
const finishAnswering = (newVal: Record<string, any>, outputType: number) => {
  // [zh] 语句
  streamWaiting.value = false;
  // [zh] 语句
  answering.value = false;
  // [zh] 语句
  mitter.emit('answering', false);

  // [zh] 声明变量 `target`
  let target = resolveActiveMessage(newVal.messageId);
  // [zh] 条件判断 if
  if (!target && currentMessage.value) {
    // [zh] 语句
    target = currentMessage.value;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (target) {
    // [zh] 条件判断 if
    if (outputType === AGENT_OUTPUT_TYPE.ERROR) {
      // [zh] 语句
      target.assistantMessage = newVal.assistantMessage || '服务器返回错误，请联系管理员';
    // [zh] 代码块开始
    } else {
      // [zh] 声明变量 `finalText`
      const finalText = newVal.assistantMessage;
      // [zh] 条件判断 if
      if (finalText != null && String(finalText).trim() !== '') {
        // [zh] 语句
        target.assistantMessage = String(finalText);
      // [zh] 语法符号
      }
      // [zh] 条件判断 if
      if (newVal.bizType) {
        // [zh] 语句
        target.bizType = newVal.bizType;
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
    // [zh] 语句
    target.status = 2;
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (
    // [zh] TypeScript/JS 语句：`!newVal.messageId ||`
    !newVal.messageId ||
    // [zh] TypeScript/JS 语句：`!currentMessage.value ||`
    !currentMessage.value ||
    // [zh] TypeScript/JS 语句：`String(currentMessage.value.messageId) === String(newVa`
    String(currentMessage.value.messageId) === String(newVal.messageId)
  // [zh] 代码块开始
  ) {
    // [zh] 语句
    currentMessage.value = null;
  // [zh] 语法符号
  }

  // [zh] 语句
  void scrollBottom(true);
// [zh] 语句
};

// [zh] 声明变量 `scrollBottom`
const scrollBottom = async (force = false) => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextTick();
  // [zh] 声明变量 `el`
  const el = listRef.value;
  // [zh] 条件判断 if
  if (!el) return;
  // [zh] 条件判断 if
  if (!force && !stickToBottom.value) return;

  // [zh] 声明变量 `apply`
  const apply = () => {
    // [zh] 语句
    el.scrollTop = Math.max(0, el.scrollHeight - el.clientHeight);
    // [zh] 语句
    stickToBottom.value = isNearBottom(el);
  // [zh] 语句
  };

  // [zh] 语句
  apply();
  // [zh] 语句
  requestAnimationFrame(apply);
// [zh] 语句
};

// [zh] 声明变量 `scrollToLatestOnOpen`
const scrollToLatestOnOpen = async () => {
  // [zh] 语句
  stickToBottom.value = true;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await scrollBottom(true);
  // [zh] 语句
  window.setTimeout(() => void scrollBottom(true), 120);
  // [zh] 语句
  window.setTimeout(() => void scrollBottom(true), 320);
// [zh] 语句
};

// [zh] 声明变量 `restoreScrollAfterPrepend`
const restoreScrollAfterPrepend = async () => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextTick();
  // [zh] 声明变量 `el`
  const el = listRef.value;
  // [zh] 条件判断 if
  if (!el) return;
  // [zh] 语句
  el.scrollTop = Math.max(0, el.scrollHeight - oldScrollHeight);
// [zh] 语句
};

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => agentMessageStore.message,`
  () => agentMessageStore.message,
  // [zh] 代码块开始
  (newVal) => {
    // [zh] 条件判断 if
    if (!newVal) return;

    // [zh] 声明变量 `outputType`
    const outputType = Number(newVal.outPutType);
    // [zh] 条件判断 if
    if (outputType === AGENT_OUTPUT_TYPE.DONE || outputType === AGENT_OUTPUT_TYPE.ERROR) {
      // [zh] 语句
      finishAnswering(newVal, outputType);
      // [zh] 语句
      return;
    // [zh] 语法符号
    }

    // [zh] 声明变量 `target`
    const target = resolveActiveMessage(newVal.messageId);
    // [zh] 条件判断 if
    if (!target) return;

    // [zh] 语句
    streamWaiting.value = false;
    // [zh] 语句
    target.assistantMessage = (target.assistantMessage || '') + (newVal.assistantMessage || '');
    // [zh] 语句
    target.status = 1;
    // [zh] 条件判断 if
    if (newVal.bizType) target.bizType = newVal.bizType;
    // [zh] 语句
    void scrollBottom();
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`{ deep: true }`
  { deep: true }
// [zh] 语句
);

// [zh] 声明变量 `onSendMessage`
const onSendMessage = (payload?: unknown) => {
  // [zh] 声明变量 `message`
  const message = payload as Record<string, any>;
  // [zh] 条件判断 if
  if (!message?.messageId) return;

  // [zh] 代码块开始
  currentMessage.value = {
    // [zh] TypeScript/JS 语句：`messageId: Number(message.messageId),`
    messageId: Number(message.messageId),
    // [zh] TypeScript/JS 语句：`userMessage: message.userMessage,`
    userMessage: message.userMessage,
    // [zh] TypeScript/JS 语句：`assistantMessage: '',`
    assistantMessage: '',
    // [zh] TypeScript/JS 语句：`status: 1,`
    status: 1,
    // [zh] TypeScript/JS 语句：`bizType: message.bizType,`
    bizType: message.bizType,
    // [zh] TypeScript/JS 语句：`sendTime: message.sendTime`
    sendTime: message.sendTime
  // [zh] 语句
  };
  // [zh] 语句
  messageList.value.push(currentMessage.value);

  // [zh] 语句
  answering.value = true;
  // [zh] 语句
  streamWaiting.value = true;
  // [zh] 语句
  stickToBottom.value = true;
  // [zh] 语句
  mitter.emit('answering', { answering: true, messageId: message.messageId });
  // [zh] 语句
  void scrollBottom(true);
// [zh] 语句
};

// [zh] 声明变量 `onCancelMessage`
const onCancelMessage = async (payload?: unknown) => {
  // [zh] 声明变量 `data`
  const data = payload as { messageId?: number };
  // [zh] 声明变量 `id`
  const id = data?.messageId;
  // [zh] 声明变量 `target`
  let target = currentMessage.value;
  // [zh] 条件判断 if
  if (!target && id != null) {
    // [zh] 语句
    target = messageList.value.find((m) => String(m.messageId) === String(id)) ?? null;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!target || id == null) return;

  // [zh] 声明变量 `partial`
  const partial = (target.assistantMessage || '').trim();
  // [zh] 条件判断 if
  if (partial) {
    // [zh] 语句
    target.status = 3;
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    target.status = 0;
    // [zh] 语句
    target.assistantMessage = '';
    // [zh] 语句
    target.bizType = undefined;
  // [zh] 语法符号
  }
  // [zh] 语句
  answering.value = false;
  // [zh] 语句
  streamWaiting.value = false;
  // [zh] 语句
  mitter.emit('answering', false);
  // [zh] 条件判断 if
  if (currentMessage.value === target) currentMessage.value = null;

  // [zh] 代码块开始
  try {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await agentApi.cancelMessage(id, partial || undefined);
  // [zh] 代码块开始
  } catch {
    // [zh] 语句
    toast.error('停止失败，请重试');
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `loadHistoryMessage`
const loadHistoryMessage = async () => {
  // [zh] 条件判断 if
  if (loadingHistory.value) return;

  // [zh] 声明变量 `nextPage`
  const nextPage = pageNo.value + 1;
  // [zh] 条件判断 if
  if (pageTotal.value > 0 && nextPage > pageTotal.value) return;

  // [zh] 语句
  loadingHistory.value = true;
  // [zh] 语句
  historyLoadFailed.value = false;

  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `res`
    const res = await agentApi.loadHistoryMessage({
      // [zh] TypeScript/JS 语句：`pageNo: nextPage,`
      pageNo: nextPage,
      // [zh] TypeScript/JS 语句：`...(maxMessageId != null ? { maxMessageId } : {})`
      ...(maxMessageId != null ? { maxMessageId } : {})
    // [zh] 语句
    });
    // [zh] 声明变量 `page`
    const page = extractHistoryPage(res);
    // [zh] 声明变量 `rawList`
    const rawList = page.list as Record<string, unknown>[];

    // [zh] 条件判断 if
    if (nextPage === 1 && rawList.length > 0) {
      // [zh] 语句
      maxMessageId = Number(rawList[0]?.messageId) || null;
    // [zh] 语法符号
    }

    // [zh] 声明变量 `incoming`
    const incoming = sortHistoryMessages(
      // [zh] TypeScript/JS 语句：`rawList.map((row) => normalizeAgentHistoryMessage(row))`
      rawList.map((row) => normalizeAgentHistoryMessage(row))
    // [zh] 语句
    );

    // [zh] 条件判断 if
    if (nextPage === 1) {
      // [zh] 语句
      messageList.value = incoming;
    // [zh] 代码块开始
    } else {
      // [zh] 语句
      messageList.value = mergeHistoryMessages(incoming, messageList.value);
    // [zh] 语法符号
    }

    // [zh] 语句
    pageNo.value = page.pageNo || nextPage;
    // [zh] 语句
    pageTotal.value = page.pageTotal || 1;

    // [zh] 声明变量 `last`
    const last = messageList.value[messageList.value.length - 1];
    // [zh] 条件判断 if
    if (last && Number(last.status) === 1) {
      // [zh] 语句
      currentMessage.value = last;
      // [zh] 语句
      answering.value = true;
      // [zh] 语句
      streamWaiting.value = true;
      // [zh] 语句
      mitter.emit('answering', { answering: true, messageId: last.messageId });
    // [zh] 语法符号
    }

    // [zh] 条件判断 if
    if (nextPage === 1) {
      // [zh] 语句
      initialLoadDone = true;
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await scrollToLatestOnOpen();
    // [zh] 代码块开始
    } else {
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await restoreScrollAfterPrepend();
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } catch (err) {
    // [zh] 语句
    console.error('加载历史消息失败', err);
    // [zh] 语句
    historyLoadFailed.value = true;
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    loadingHistory.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `reloadHistory`
const reloadHistory = () => {
  // [zh] 语句
  messageList.value = [];
  // [zh] 语句
  pageNo.value = 0;
  // [zh] 语句
  pageTotal.value = 0;
  // [zh] 语句
  maxMessageId = null;
  // [zh] 语句
  initialLoadDone = false;
  // [zh] 语句
  void loadHistoryMessage();
// [zh] 语句
};

// [zh] 声明变量 `onComposerReady`
const onComposerReady = () => {
  // [zh] 条件判断 if
  if (stickToBottom.value) void scrollBottom(true);
// [zh] 语句
};

// [zh] 代码块开始
onMounted(async () => {
  // [zh] 声明变量 `authed`
  const authed = await useAuthStore().ensureSession();
  // [zh] 条件判断 if
  if (!authed) {
    // [zh] 语句
    historyLoadFailed.value = true;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }

  // [zh] 语句
  mitter.on('sendMessage', onSendMessage as (p?: unknown) => void);
  // [zh] 语句
  mitter.on('cancelMessage', onCancelMessage);
  // [zh] 语句
  mitter.on('agentComposerReady', onComposerReady);
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await loadHistoryMessage();
// [zh] 语句
});

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 语句
  mitter.off('sendMessage', onSendMessage as (p?: unknown) => void);
  // [zh] 语句
  mitter.off('cancelMessage', onCancelMessage);
  // [zh] 语句
  mitter.off('agentComposerReady', onComposerReady);
  // [zh] 语句
  currentMessage.value = null;
  // [zh] 语句
  answering.value = false;
  // [zh] 语句
  maxMessageId = null;
  // [zh] 语句
  initialLoadDone = false;
// [zh] 语句
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.chat-scroll {` */
.chat-scroll {
  /* [zh] 样式规则 `flex: 1 1 0;` */
  flex: 1 1 0;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `overflow-x: hidden;` */
  overflow-x: hidden;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
  /* [zh] 样式规则 `overscroll-behavior: contain;` */
  overscroll-behavior: contain;
  /* [zh] 样式规则 `padding-bottom: calc(var(--agent-compose` */
  padding-bottom: calc(var(--agent-composer-inset, 148px) + 12px);
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
  -webkit-overflow-scrolling: touch;
  /* [zh] 样式规则 `touch-action: pan-y;` */
  touch-action: pan-y;
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `z-index: 1;` */
  z-index: 1;

  /* [zh] 样式规则 `&.is-embedded-composer {` */
  &.is-embedded-composer {
    /* [zh] 样式规则 `padding-bottom: 12px;` */
    padding-bottom: 12px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.chat-scroll-inner {` */
.chat-scroll-inner {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `min-height: 100%;` */
  min-height: 100%;
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.msg-group {` */
.msg-group {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `margin-bottom: 14px;` */
  margin-bottom: 14px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: stretch;` */
  align-items: stretch;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `:deep(.bubble-row.user) {` */
:deep(.bubble-row.user) {
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;

  /* [zh] 样式声明 */
  .bubble,
  /* [zh] 样式规则 `.consult-product-card.is-bubble {` */
  .consult-product-card.is-bubble {
    /* [zh] 样式规则 `margin-left: auto;` */
    margin-left: auto;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `:deep(.bubble-row.ai) {` */
:deep(.bubble-row.ai) {
  /* [zh] 样式规则 `justify-content: flex-start;` */
  justify-content: flex-start;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.welcome,
/* [zh] 样式声明 */
.history-loading,
/* [zh] 样式声明 */
.history-error,
/* [zh] 样式规则 `.history-top-tip {` */
.history-top-tip {
  /* [zh] 样式规则 `margin: 16px 0;` */
  margin: 16px 0;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `line-height: 1.6;` */
  line-height: 1.6;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.history-error {` */
.history-error {
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: none;` */
  background: none;
  /* [zh] 样式规则 `font: inherit;` */
  font: inherit;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.history-top-tip {` */
.history-top-tip {
  /* [zh] 样式规则 `margin: 8px 0 12px;` */
  margin: 8px 0 12px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
/* [zh] 样式规则 `}` */
}
</style>
