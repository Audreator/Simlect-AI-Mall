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
        class="history-top-tip"
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

<script setup lang="ts">
import { inject, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { agentApi } from '@/api/modules';
import { agentComposerEmbeddedKey } from '@/composables/agentEmbed';
import AgentChatItem from '@/components/agent/AgentChatItem.vue';
import AgentUserBubble from '@/components/agent/AgentUserBubble.vue';
import { useAgentMessageStore } from '@/stores/agentMessage';
import { useAuthStore } from '@/stores/auth';
import { AGENT_OUTPUT_TYPE } from '@/constants/backendEnums';
import { mitter } from '@/utils/eventBus';
import { toast } from '@/utils/toast';
import {
  extractHistoryPage,
  mergeHistoryMessages,
  normalizeAgentHistoryMessage,
  sortHistoryMessages,
  type AgentHistoryMessage
} from '@/utils/agentHistory';

const composerEmbedded = inject(agentComposerEmbeddedKey, false);
const agentMessageStore = useAgentMessageStore();
const listRef = ref<HTMLElement>();
const loadingHistory = ref(false);
const historyLoadFailed = ref(false);
const answering = ref(false);
const streamWaiting = ref(false);
const currentMessage = ref<AgentHistoryMessage | null>(null);

const messageList = ref<AgentHistoryMessage[]>([]);
const pageNo = ref(0);
const pageTotal = ref(0);

let maxMessageId: number | null = null;
let oldScrollHeight = 0;
let initialLoadDone = false;

const shouldShowAi = (item: AgentHistoryMessage) => {
  const status = Number(item.status);
  if (status === 0 || status === 1 || status === 3) return true;
  return !!(item.assistantMessage || '').trim();
};

const stickToBottom = ref(true);

const isNearBottom = (el: HTMLElement, threshold = 80) =>
  el.scrollHeight - el.scrollTop - el.clientHeight <= threshold;

const onListScroll = () => {
  const el = listRef.value;
  if (!el) return;
  stickToBottom.value = isNearBottom(el);
  if (!initialLoadDone || loadingHistory.value) return;
  if (el.scrollTop > 8) return;
  if (pageNo.value >= pageTotal.value) return;
  oldScrollHeight = el.scrollHeight;
  void loadHistoryMessage();
};

const resolveActiveMessage = (messageId?: number | string) => {
  if (messageId == null) return currentMessage.value;
  if (currentMessage.value && String(currentMessage.value.messageId) === String(messageId)) {
    return currentMessage.value;
  }
  return messageList.value.find((m) => String(m.messageId) === String(messageId)) ?? null;
};

const finishAnswering = (newVal: Record<string, any>, outputType: number) => {
  streamWaiting.value = false;
  answering.value = false;
  mitter.emit('answering', false);

  let target = resolveActiveMessage(newVal.messageId);
  if (!target && currentMessage.value) {
    target = currentMessage.value;
  }
  if (target) {
    if (outputType === AGENT_OUTPUT_TYPE.ERROR) {
      target.assistantMessage = newVal.assistantMessage || '服务器返回错误，请联系管理员';
    } else {
      const finalText = newVal.assistantMessage;
      if (finalText != null && String(finalText).trim() !== '') {
        target.assistantMessage = String(finalText);
      }
      if (newVal.bizType) {
        target.bizType = newVal.bizType;
      }
    }
    target.status = 2;
  }

  if (
    !newVal.messageId ||
    !currentMessage.value ||
    String(currentMessage.value.messageId) === String(newVal.messageId)
  ) {
    currentMessage.value = null;
  }

  void scrollBottom(true);
};

const scrollBottom = async (force = false) => {
  await nextTick();
  const el = listRef.value;
  if (!el) return;
  if (!force && !stickToBottom.value) return;

  const apply = () => {
    el.scrollTop = Math.max(0, el.scrollHeight - el.clientHeight);
    stickToBottom.value = isNearBottom(el);
  };

  apply();
  requestAnimationFrame(apply);
};

const scrollToLatestOnOpen = async () => {
  stickToBottom.value = true;
  await scrollBottom(true);
  window.setTimeout(() => void scrollBottom(true), 120);
  window.setTimeout(() => void scrollBottom(true), 320);
};

const restoreScrollAfterPrepend = async () => {
  await nextTick();
  const el = listRef.value;
  if (!el) return;
  el.scrollTop = Math.max(0, el.scrollHeight - oldScrollHeight);
};

watch(
  () => agentMessageStore.message,
  (newVal) => {
    if (!newVal) return;

    const outputType = Number(newVal.outPutType);
    if (outputType === AGENT_OUTPUT_TYPE.DONE || outputType === AGENT_OUTPUT_TYPE.ERROR) {
      finishAnswering(newVal, outputType);
      return;
    }

    const target = resolveActiveMessage(newVal.messageId);
    if (!target) return;

    streamWaiting.value = false;
    target.assistantMessage = (target.assistantMessage || '') + (newVal.assistantMessage || '');
    target.status = 1;
    if (newVal.bizType) target.bizType = newVal.bizType;
    void scrollBottom();
  },
  { deep: true }
);

const onSendMessage = (payload?: unknown) => {
  const message = payload as Record<string, any>;
  if (!message?.messageId) return;

  currentMessage.value = {
    messageId: Number(message.messageId),
    userMessage: message.userMessage,
    assistantMessage: '',
    status: 1,
    bizType: message.bizType,
    sendTime: message.sendTime
  };
  messageList.value.push(currentMessage.value);

  answering.value = true;
  streamWaiting.value = true;
  stickToBottom.value = true;
  mitter.emit('answering', { answering: true, messageId: message.messageId });
  void scrollBottom(true);
};

const onCancelMessage = async (payload?: unknown) => {
  const data = payload as { messageId?: number };
  const id = data?.messageId;
  let target = currentMessage.value;
  if (!target && id != null) {
    target = messageList.value.find((m) => String(m.messageId) === String(id)) ?? null;
  }
  if (!target || id == null) return;

  const partial = (target.assistantMessage || '').trim();
  if (partial) {
    target.status = 3;
  } else {
    target.status = 0;
    target.assistantMessage = '';
    target.bizType = undefined;
  }
  answering.value = false;
  streamWaiting.value = false;
  mitter.emit('answering', false);
  if (currentMessage.value === target) currentMessage.value = null;

  try {
    await agentApi.cancelMessage(id, partial || undefined);
  } catch {
    toast.error('停止失败，请重试');
  }
};

const loadHistoryMessage = async () => {
  if (loadingHistory.value) return;

  const nextPage = pageNo.value + 1;
  if (pageTotal.value > 0 && nextPage > pageTotal.value) return;

  loadingHistory.value = true;
  historyLoadFailed.value = false;

  try {
    const res = await agentApi.loadHistoryMessage({
      pageNo: nextPage,
      ...(maxMessageId != null ? { maxMessageId } : {})
    });
    const page = extractHistoryPage(res);
    const rawList = page.list as Record<string, unknown>[];

    if (nextPage === 1 && rawList.length > 0) {
      maxMessageId = Number(rawList[0]?.messageId) || null;
    }

    const incoming = sortHistoryMessages(
      rawList.map((row) => normalizeAgentHistoryMessage(row))
    );

    if (nextPage === 1) {
      messageList.value = incoming;
    } else {
      messageList.value = mergeHistoryMessages(incoming, messageList.value);
    }

    pageNo.value = page.pageNo || nextPage;
    pageTotal.value = page.pageTotal || 1;

    const last = messageList.value[messageList.value.length - 1];
    if (last && Number(last.status) === 1) {
      currentMessage.value = last;
      answering.value = true;
      streamWaiting.value = true;
      mitter.emit('answering', { answering: true, messageId: last.messageId });
    }

    if (nextPage === 1) {
      initialLoadDone = true;
      await scrollToLatestOnOpen();
    } else {
      await restoreScrollAfterPrepend();
    }
  } catch (err) {
    console.error('加载历史消息失败', err);
    historyLoadFailed.value = true;
  } finally {
    loadingHistory.value = false;
  }
};

const reloadHistory = () => {
  messageList.value = [];
  pageNo.value = 0;
  pageTotal.value = 0;
  maxMessageId = null;
  initialLoadDone = false;
  void loadHistoryMessage();
};

const onComposerReady = () => {
  if (stickToBottom.value) void scrollBottom(true);
};

onMounted(async () => {
  const authed = await useAuthStore().ensureSession();
  if (!authed) {
    historyLoadFailed.value = true;
    return;
  }

  mitter.on('sendMessage', onSendMessage as (p?: unknown) => void);
  mitter.on('cancelMessage', onCancelMessage);
  mitter.on('agentComposerReady', onComposerReady);
  await loadHistoryMessage();
});

onUnmounted(() => {
  mitter.off('sendMessage', onSendMessage as (p?: unknown) => void);
  mitter.off('cancelMessage', onCancelMessage);
  mitter.off('agentComposerReady', onComposerReady);
  currentMessage.value = null;
  answering.value = false;
  maxMessageId = null;
  initialLoadDone = false;
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
