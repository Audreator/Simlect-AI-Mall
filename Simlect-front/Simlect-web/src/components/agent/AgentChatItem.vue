<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div v-if="showAi" class="bubble-row ai">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="ai-avatar-mini">
      <!-- [zh] 开始标签 `<el-icon>` -->
      <el-icon :size="16"><Service /></el-icon>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="bubble ai-bubble" :class="{ 'is-wide': isWideBubble }">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="messageStatus === 0 && !hasRenderableContent" class="cancel-tip">已取消回复</div>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-else>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-if="productList?.length">
        <!-- [zh] 开始标签 `<MarkdownContent>` -->
        <MarkdownContent
          v-if="productIntro"
          class="product-intro"
          :content="productIntro"
        />
        <!-- [zh] 开始标签 `<p>` -->
        <p v-else class="biz-title">为您推荐以下商品</p>
        <!-- [zh] 开始标签 `<AgentProductList>` -->
        <AgentProductList :list="productList" />
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-else-if="isProductSearchEmpty">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="biz-title">商品搜索</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="empty-hint">未找到相关商品，请换个关键词试试，或让我为您推荐热销商品。</p>
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-else-if="orderList?.length">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="biz-title">为您查询到以下订单</p>
        <!-- [zh] 开始标签 `<AgentOrderList>` -->
        <AgentOrderList :list="orderList" />
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-else-if="actionConfirmCard">
        <!-- [zh] 开始标签 `<AgentConfirmCard>` -->
        <AgentConfirmCard :card="actionConfirmCard" @updated="onActionCardUpdated" />
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-else-if="isOrderSearchEmpty">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="biz-title">订单查询</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="empty-hint">未查询到相关订单，请核对订单号或稍后再试。</p>
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-else-if="isStreaming && streamText">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="stream-text">
          <!-- [zh] 开始标签 `<span>` -->
          <span>{{ streamText }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="stream-cursor" aria-hidden="true" />
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-else-if="isStreaming || waiting">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="typing">正在为您查询，请稍候…</p>
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-else-if="displayText">
        <!-- [zh] 开始标签 `<MarkdownContent>` -->
        <MarkdownContent :content="displayText" :agent-rich="agentRich" />
      </template>
      <p v-if="messageStatus === 3 && hasRenderableContent" class="interrupt-tip">回复已中断，以上为已生成内容</p>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Service } from '@element-plus/icons-vue';
import MarkdownContent from '@/components/common/MarkdownContent.vue';
import AgentProductList from '@/components/agent/AgentProductList.vue';
import AgentOrderList from '@/components/agent/AgentOrderList.vue';
import AgentConfirmCard, { type ActionConfirmCardData } from '@/components/agent/AgentConfirmCard.vue';
import { cleanAgentActionStreamText, containsAgentTable, stripEmbeddedProductJson } from '@/utils/agentMessageRender';

const props = defineProps<{
  data: Record<string, any>;
  waiting?: boolean;
}>();

const messageStatus = computed(() => Number(props.data.status ?? 2));

const isStreaming = computed(() => messageStatus.value === 1);

const parseJsonList = (raw?: string | null) => {
  if (!raw || typeof raw !== 'string') return null;
  const text = raw.trim();
  if (!text.startsWith('[') && !text.startsWith('{')) return null;
  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) return parsed;
    if (parsed && Array.isArray(parsed.list)) return parsed.list;
    return null;
  } catch {
    return null;
  }
};

const isEmptyJsonList = (raw?: string | null) => {
  const parsed = parseJsonList(raw);
  if (parsed !== null) return parsed.length === 0;
  return typeof raw === 'string' && raw.trim() === '[]';
};

const isProductBiz = (bizType?: string | null) =>
  bizType === 'product_search' ||
  bizType === 'product_search.txt' ||
  bizType === 'BROWSE_RECOMMEND';

const isOrderBiz = (bizType?: string | null) =>
  bizType === 'query_order' || bizType === 'query_order.txt';

const looksLikeOrderCards = (list: unknown[] | null) => {
  if (!list?.length) return false;
  const first = list[0] as Record<string, unknown> | null;
  return !!(first && typeof first === 'object' && (first.orderId || first.order_id));
};

const parseJsonObject = (raw?: string | null) => {
  if (!raw || typeof raw !== 'string') return null;
  const text = raw.trim();
  if (!text.startsWith('{')) return null;
  try {
    const parsed = JSON.parse(text);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : null;
  } catch {
    return null;
  }
};

const actionConfirmCard = ref<ActionConfirmCardData | null>(null);

const syncActionConfirmCard = () => {
  if (isStreaming.value) {
    actionConfirmCard.value = null;
    return;
  }
  const parsed = parseJsonObject(props.data.assistantMessage);
  if (parsed?.type === 'ACTION_CONFIRM') {
    actionConfirmCard.value = {
      ...(parsed as ActionConfirmCardData),
      status: Number((parsed as ActionConfirmCardData).status ?? 0)
    };
    return;
  }
  actionConfirmCard.value = null;
};

watch(
  () => [props.data.assistantMessage, props.data.bizType, props.data.status, props.waiting] as const,
  () => syncActionConfirmCard(),
  { immediate: true }
);

const onActionCardUpdated = (card: ActionConfirmCardData) => {
  actionConfirmCard.value = card;
};

const productSearchPayload = computed(() => {
  if (isStreaming.value) return null;
  const raw = props.data.assistantMessage;
  if (!raw || typeof raw !== 'string') return null;
  const text = raw.trim();
  if (!text.startsWith('{')) return null;
  try {
    const parsed = JSON.parse(text);
    if (parsed?.type === 'PRODUCT_SEARCH_RESULT' && Array.isArray(parsed.products)) {
      return {
        intro: typeof parsed.intro === 'string' ? parsed.intro.trim() : '',
        products: parsed.products
      };
    }
  } catch {
    return null;
  }
  return null;
});

const productList = computed(() => {
  if (isStreaming.value) return null;
  const wrapped = productSearchPayload.value;
  const fromWrapped = (wrapped?.products || []).filter(
    (p: any) => p?.productId && (p?.productName || p?.product_name)
  );
  if (fromWrapped.length) return fromWrapped;
  const raw = props.data.assistantMessage;
  const parsed = parseJsonList(raw);
  if (!parsed?.length) return null;
  const usable = parsed.filter((p: any) => p?.productId && (p?.productName || p?.product_name));
  if (!usable.length) return null;
  if (isProductBiz(props.data.bizType)) return usable;
  if (usable[0]?.productId && usable[0]?.productName) return usable;
  return null;
});

const productIntro = computed(() =>
  stripEmbeddedProductJson(productSearchPayload.value?.intro || '')
);

const isProductSearchEmpty = computed(() => {
  if (isStreaming.value || props.waiting) return false;
  if (!isProductBiz(props.data.bizType)) return false;
  if (productSearchPayload.value) return !productSearchPayload.value.products.length;
  return isEmptyJsonList(props.data.assistantMessage);
});

const orderList = computed(() => {
  if (isStreaming.value) return null;
  const parsed = parseJsonList(props.data.assistantMessage);
  if (looksLikeOrderCards(parsed)) return parsed;
  if (!isOrderBiz(props.data.bizType)) return null;
  if (!parsed?.length) return null;
  return parsed;
});

const isOrderSearchEmpty = computed(() => {
  if (isStreaming.value || props.waiting) return false;
  if (!isOrderBiz(props.data.bizType)) return false;
  if (looksLikeOrderCards(parseJsonList(props.data.assistantMessage))) return false;
  return isEmptyJsonList(props.data.assistantMessage);
});

const streamText = computed(() => cleanAgentActionStreamText(props.data.assistantMessage));

const displayText = computed(() => {
  if (productList.value?.length || orderList.value?.length) return '';
  if (actionConfirmCard.value) return '';
  if (isProductSearchEmpty.value || isOrderSearchEmpty.value) return '';
  const parsed = parseJsonObject(props.data.assistantMessage);
  if (parsed?.type === 'ACTION_CONFIRM') return '';
  if (parsed?.type === 'PRODUCT_SEARCH_RESULT') return '';
  let text = (props.data.assistantMessage || '').trim();
  if (text === '[]') return '';
  // Hide bare product-id JSON arrays the model sometimes dumps.
  const asList = parseJsonList(text);
  if (
    asList?.length &&
    asList.every((p: any) => p?.productId && !(p?.productName || p?.product_name))
  ) {
    return '';
  }
  text = stripEmbeddedProductJson(text);
  return text;
});

const agentRich = computed(
  () =>
    props.data.bizType === 'query_logistics' ||
    props.data.bizType === 'product_consult' ||
    containsAgentTable(displayText.value)
);

const showAi = computed(() => {
  if (messageStatus.value === 0) return hasRenderableContent.value;
  if (props.waiting) return true;
  if (isStreaming.value) return true;
  if (messageStatus.value === 3) return hasRenderableContent.value;
  if (productList.value?.length || orderList.value?.length) return true;
  if (actionConfirmCard.value) return true;
  if (isProductSearchEmpty.value || isOrderSearchEmpty.value) return true;
  if (displayText.value) return true;
  return false;
});

const hasRenderableContent = computed(
  () =>
    !!(productList.value?.length ||
      orderList.value?.length ||
      actionConfirmCard.value ||
      isProductSearchEmpty.value ||
      isOrderSearchEmpty.value ||
      displayText.value ||
      props.waiting ||
      (isStreaming.value && streamText.value))
);

const isWideBubble = computed(
  () =>
    !!(
      productList.value?.length ||
      orderList.value?.length ||
      actionConfirmCard.value ||
      isProductSearchEmpty.value ||
      isOrderSearchEmpty.value ||
      agentRich.value
    )
);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.bubble-row.ai {` */
.bubble-row.ai {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: flex-start;` */
  justify-content: flex-start;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.bubble {` */
.bubble {
  /* [zh] 样式规则 `width: fit-content;` */
  width: fit-content;
  /* [zh] 样式规则 `max-width: min(75%, 520px);` */
  max-width: min(75%, 520px);
  /* [zh] 样式规则 `padding: 9px 12px;` */
  padding: 9px 12px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.45;` */
  line-height: 1.45;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `word-break: break-word;` */
  word-break: break-word;
  /* [zh] 样式规则 `flex: 0 1 auto;` */
  flex: 0 1 auto;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ai-avatar-mini {` */
.ai-avatar-mini {
  /* [zh] 样式规则 `width: 28px;` */
  width: 28px;
  /* [zh] 样式规则 `height: 28px;` */
  height: 28px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `box-shadow: $shadow-xs;` */
  box-shadow: $shadow-xs;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ai-bubble {` */
.ai-bubble {
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `border-bottom-left-radius: 4px;` */
  border-bottom-left-radius: 4px;
  /* [zh] 样式规则 `box-shadow: $shadow-xs;` */
  box-shadow: $shadow-xs;

  /* [zh] 样式规则 `&.is-wide {` */
  &.is-wide {
    /* [zh] 样式规则 `width: auto;` */
    width: auto;
    /* [zh] 样式规则 `max-width: calc(100% - 36px);` */
    max-width: calc(100% - 36px);
    /* [zh] 样式规则 `flex: 1 1 auto;` */
    flex: 1 1 auto;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.biz-title {` */
.biz-title {
  /* [zh] 样式规则 `margin: 0 0 8px;` */
  margin: 0 0 8px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.typing,
/* [zh] 样式声明 */
.cancel-tip,
/* [zh] 样式声明 */
.interrupt-tip,
/* [zh] 样式规则 `.empty-hint {` */
.empty-hint {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.interrupt-tip {` */
.interrupt-tip {
  /* [zh] 样式规则 `margin-top: 8px;` */
  margin-top: 8px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.empty-hint {` */
.empty-hint {
  /* [zh] 样式规则 `line-height: 1.55;` */
  line-height: 1.55;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-intro {` */
.product-intro {
  /* [zh] 样式规则 `margin: 0 0 8px;` */
  margin: 0 0 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stream-text {` */
.stream-text {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.55;` */
  line-height: 1.55;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `white-space: pre-wrap;` */
  white-space: pre-wrap;
  /* [zh] 样式规则 `word-break: break-word;` */
  word-break: break-word;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stream-cursor {` */
.stream-cursor {
  /* [zh] 样式规则 `display: inline-block;` */
  display: inline-block;
  /* [zh] 样式规则 `width: 2px;` */
  width: 2px;
  /* [zh] 样式规则 `height: 1em;` */
  height: 1em;
  /* [zh] 样式规则 `margin-left: 2px;` */
  margin-left: 2px;
  /* [zh] 样式规则 `vertical-align: text-bottom;` */
  vertical-align: text-bottom;
  /* [zh] 样式规则 `background: $color-primary;` */
  background: $color-primary;
  /* [zh] 样式规则 `animation: blink 0.9s step-end infinite;` */
  animation: blink 0.9s step-end infinite;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@keyframes blink {` */
@keyframes blink {
  /* [zh] 样式规则 `50% {` */
  50% {
    /* [zh] 样式规则 `opacity: 0;` */
    opacity: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `:deep(.markdown-content) {` */
:deep(.markdown-content) {
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.45;` */
  line-height: 1.45;
  /* [zh] 样式规则 `max-width: 100%;` */
  max-width: 100%;

  /* [zh] 样式规则 `p {` */
  p {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;

    /* [zh] 样式规则 `& + p {` */
    & + p {
      /* [zh] 样式规则 `margin-top: 6px;` */
      margin-top: 6px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ai-bubble:not(.is-wide) :deep(.markdown` */
.ai-bubble:not(.is-wide) :deep(.markdown-content) {
  /* [zh] 样式规则 `width: fit-content;` */
  width: fit-content;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `:deep(.agent-orders) {` */
:deep(.agent-orders) {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
/* [zh] 样式规则 `}` */
}
</style>
