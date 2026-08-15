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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, ref, watch } from 'vue';
// [zh] 导入 `@element-plus/icons-vue`
import { Service } from '@element-plus/icons-vue';
// [zh] 导入 `@/components/common/MarkdownContent.vue`
import MarkdownContent from '@/components/common/MarkdownContent.vue';
// [zh] 导入 `@/components/agent/AgentProductList.vue`
import AgentProductList from '@/components/agent/AgentProductList.vue';
// [zh] 导入 `@/components/agent/AgentOrderList.vue`
import AgentOrderList from '@/components/agent/AgentOrderList.vue';
// [zh] 导入 `模块`
import AgentConfirmCard, { type ActionConfirmCardData } from '@/components/agent/AgentConfirmCard.vue';
// [zh] 导入 `@/utils/agentMessageRender`
import { cleanAgentActionStreamText, containsAgentTable, stripEmbeddedProductJson } from '@/utils/agentMessageRender';

// [zh] 声明变量 `props`
const props = defineProps<{
  // [zh] 语句
  data: Record<string, any>;
  // [zh] 语句
  waiting?: boolean;
// [zh] 语句
}>();

// [zh] 声明变量 `messageStatus`
const messageStatus = computed(() => Number(props.data.status ?? 2));

// [zh] 声明变量 `isStreaming`
const isStreaming = computed(() => messageStatus.value === 1);

// [zh] 声明变量 `parseJsonList`
const parseJsonList = (raw?: string | null) => {
  // [zh] 条件判断 if
  if (!raw || typeof raw !== 'string') return null;
  // [zh] 声明变量 `text`
  const text = raw.trim();
  // [zh] 条件判断 if
  if (!text.startsWith('[') && !text.startsWith('{')) return null;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `parsed`
    const parsed = JSON.parse(text);
    // [zh] 条件判断 if
    if (Array.isArray(parsed)) return parsed;
    // [zh] 条件判断 if
    if (parsed && Array.isArray(parsed.list)) return parsed.list;
    // [zh] return 返回
    return null;
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return null;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `isEmptyJsonList`
const isEmptyJsonList = (raw?: string | null) => {
  // [zh] 声明变量 `parsed`
  const parsed = parseJsonList(raw);
  // [zh] 条件判断 if
  if (parsed !== null) return parsed.length === 0;
  // [zh] return 返回
  return typeof raw === 'string' && raw.trim() === '[]';
// [zh] 语句
};

// [zh] 声明变量 `isProductBiz`
const isProductBiz = (bizType?: string | null) =>
  // [zh] TypeScript/JS 语句：`bizType === 'product_search' ||`
  bizType === 'product_search' ||
  // [zh] TypeScript/JS 语句：`bizType === 'product_search.txt' ||`
  bizType === 'product_search.txt' ||
  // [zh] 语句
  bizType === 'BROWSE_RECOMMEND';

// [zh] 声明变量 `isOrderBiz`
const isOrderBiz = (bizType?: string | null) =>
  // [zh] 语句
  bizType === 'query_order' || bizType === 'query_order.txt';

// [zh] 声明变量 `looksLikeOrderCards`
const looksLikeOrderCards = (list: unknown[] | null) => {
  // [zh] 条件判断 if
  if (!list?.length) return false;
  // [zh] 声明变量 `first`
  const first = list[0] as Record<string, unknown> | null;
  // [zh] return 返回
  return !!(first && typeof first === 'object' && (first.orderId || first.order_id));
// [zh] 语句
};

// [zh] 声明变量 `parseJsonObject`
const parseJsonObject = (raw?: string | null) => {
  // [zh] 条件判断 if
  if (!raw || typeof raw !== 'string') return null;
  // [zh] 声明变量 `text`
  const text = raw.trim();
  // [zh] 条件判断 if
  if (!text.startsWith('{')) return null;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `parsed`
    const parsed = JSON.parse(text);
    // [zh] return 返回
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : null;
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return null;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `actionConfirmCard`
const actionConfirmCard = ref<ActionConfirmCardData | null>(null);

// [zh] 声明变量 `syncActionConfirmCard`
const syncActionConfirmCard = () => {
  // [zh] 条件判断 if
  if (isStreaming.value) {
    // [zh] 语句
    actionConfirmCard.value = null;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `parsed`
  const parsed = parseJsonObject(props.data.assistantMessage);
  // [zh] 条件判断 if
  if (parsed?.type === 'ACTION_CONFIRM') {
    // [zh] 代码块开始
    actionConfirmCard.value = {
      // [zh] TypeScript/JS 语句：`...(parsed as ActionConfirmCardData),`
      ...(parsed as ActionConfirmCardData),
      // [zh] TypeScript/JS 语句：`status: Number((parsed as ActionConfirmCardData).status`
      status: Number((parsed as ActionConfirmCardData).status ?? 0)
    // [zh] 语句
    };
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  actionConfirmCard.value = null;
// [zh] 语句
};

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => [props.data.assistantMessage, props.data.bizType,`
  () => [props.data.assistantMessage, props.data.bizType, props.data.status, props.waiting] as const,
  // [zh] TypeScript/JS 语句：`() => syncActionConfirmCard(),`
  () => syncActionConfirmCard(),
  // [zh] TypeScript/JS 语句：`{ immediate: true }`
  { immediate: true }
// [zh] 语句
);

// [zh] 声明变量 `onActionCardUpdated`
const onActionCardUpdated = (card: ActionConfirmCardData) => {
  // [zh] 语句
  actionConfirmCard.value = card;
// [zh] 语句
};

// [zh] 声明变量 `productSearchPayload`
const productSearchPayload = computed(() => {
  // [zh] 条件判断 if
  if (isStreaming.value) return null;
  // [zh] 声明变量 `raw`
  const raw = props.data.assistantMessage;
  // [zh] 条件判断 if
  if (!raw || typeof raw !== 'string') return null;
  // [zh] 声明变量 `text`
  const text = raw.trim();
  // [zh] 条件判断 if
  if (!text.startsWith('{')) return null;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `parsed`
    const parsed = JSON.parse(text);
    // [zh] 条件判断 if
    if (parsed?.type === 'PRODUCT_SEARCH_RESULT' && Array.isArray(parsed.products)) {
      // [zh] return 返回
      return {
        // [zh] TypeScript/JS 语句：`intro: typeof parsed.intro === 'string' ? parsed.intro.`
        intro: typeof parsed.intro === 'string' ? parsed.intro.trim() : '',
        // [zh] TypeScript/JS 语句：`products: parsed.products`
        products: parsed.products
      // [zh] 语句
      };
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return null;
  // [zh] 语法符号
  }
  // [zh] return 返回
  return null;
// [zh] 语句
});

// [zh] 声明变量 `productList`
const productList = computed(() => {
  // [zh] 条件判断 if
  if (isStreaming.value) return null;
  // [zh] 声明变量 `wrapped`
  const wrapped = productSearchPayload.value;
  // [zh] 声明变量 `fromWrapped`
  const fromWrapped = (wrapped?.products || []).filter(
    // [zh] TypeScript/JS 语句：`(p: any) => p?.productId && (p?.productName || p?.produ`
    (p: any) => p?.productId && (p?.productName || p?.product_name)
  // [zh] 语句
  );
  // [zh] 条件判断 if
  if (fromWrapped.length) return fromWrapped;
  // [zh] 声明变量 `raw`
  const raw = props.data.assistantMessage;
  // [zh] 声明变量 `parsed`
  const parsed = parseJsonList(raw);
  // [zh] 条件判断 if
  if (!parsed?.length) return null;
  // [zh] 声明变量 `usable`
  const usable = parsed.filter((p: any) => p?.productId && (p?.productName || p?.product_name));
  // [zh] 条件判断 if
  if (!usable.length) return null;
  // [zh] 条件判断 if
  if (isProductBiz(props.data.bizType)) return usable;
  // [zh] 条件判断 if
  if (usable[0]?.productId && usable[0]?.productName) return usable;
  // [zh] return 返回
  return null;
// [zh] 语句
});

// [zh] 声明变量 `productIntro`
const productIntro = computed(() =>
  // [zh] TypeScript/JS 语句：`stripEmbeddedProductJson(productSearchPayload.value?.in`
  stripEmbeddedProductJson(productSearchPayload.value?.intro || '')
// [zh] 语句
);

// [zh] 声明变量 `isProductSearchEmpty`
const isProductSearchEmpty = computed(() => {
  // [zh] 条件判断 if
  if (isStreaming.value || props.waiting) return false;
  // [zh] 条件判断 if
  if (!isProductBiz(props.data.bizType)) return false;
  // [zh] 条件判断 if
  if (productSearchPayload.value) return !productSearchPayload.value.products.length;
  // [zh] return 返回
  return isEmptyJsonList(props.data.assistantMessage);
// [zh] 语句
});

// [zh] 声明变量 `orderList`
const orderList = computed(() => {
  // [zh] 条件判断 if
  if (isStreaming.value) return null;
  // [zh] 声明变量 `parsed`
  const parsed = parseJsonList(props.data.assistantMessage);
  // [zh] 条件判断 if
  if (looksLikeOrderCards(parsed)) return parsed;
  // [zh] 条件判断 if
  if (!isOrderBiz(props.data.bizType)) return null;
  // [zh] 条件判断 if
  if (!parsed?.length) return null;
  // [zh] return 返回
  return parsed;
// [zh] 语句
});

// [zh] 声明变量 `isOrderSearchEmpty`
const isOrderSearchEmpty = computed(() => {
  // [zh] 条件判断 if
  if (isStreaming.value || props.waiting) return false;
  // [zh] 条件判断 if
  if (!isOrderBiz(props.data.bizType)) return false;
  // [zh] 条件判断 if
  if (looksLikeOrderCards(parseJsonList(props.data.assistantMessage))) return false;
  // [zh] return 返回
  return isEmptyJsonList(props.data.assistantMessage);
// [zh] 语句
});

// [zh] 声明变量 `streamText`
const streamText = computed(() => cleanAgentActionStreamText(props.data.assistantMessage));

// [zh] 声明变量 `displayText`
const displayText = computed(() => {
  // [zh] 条件判断 if
  if (productList.value?.length || orderList.value?.length) return '';
  // [zh] 条件判断 if
  if (actionConfirmCard.value) return '';
  // [zh] 条件判断 if
  if (isProductSearchEmpty.value || isOrderSearchEmpty.value) return '';
  // [zh] 声明变量 `parsed`
  const parsed = parseJsonObject(props.data.assistantMessage);
  // [zh] 条件判断 if
  if (parsed?.type === 'ACTION_CONFIRM') return '';
  // [zh] 条件判断 if
  if (parsed?.type === 'PRODUCT_SEARCH_RESULT') return '';
  // [zh] 声明变量 `text`
  let text = (props.data.assistantMessage || '').trim();
  // [zh] 条件判断 if
  if (text === '[]') return '';
  // Hide bare product-id JSON arrays the model sometimes dumps.
  // [zh] 声明变量 `asList`
  const asList = parseJsonList(text);
  // [zh] 条件判断 if
  if (
    // [zh] TypeScript/JS 语句：`asList?.length &&`
    asList?.length &&
    // [zh] TypeScript/JS 语句：`asList.every((p: any) => p?.productId && !(p?.productNa`
    asList.every((p: any) => p?.productId && !(p?.productName || p?.product_name))
  // [zh] 代码块开始
  ) {
    // [zh] return 返回
    return '';
  // [zh] 语法符号
  }
  // [zh] 语句
  text = stripEmbeddedProductJson(text);
  // [zh] return 返回
  return text;
// [zh] 语句
});

// [zh] 声明变量 `agentRich`
const agentRich = computed(
  // [zh] TypeScript/JS 语句：`() =>`
  () =>
    // [zh] TypeScript/JS 语句：`props.data.bizType === 'query_logistics' ||`
    props.data.bizType === 'query_logistics' ||
    // [zh] TypeScript/JS 语句：`props.data.bizType === 'product_consult' ||`
    props.data.bizType === 'product_consult' ||
    // [zh] TypeScript/JS 语句：`containsAgentTable(displayText.value)`
    containsAgentTable(displayText.value)
// [zh] 语句
);

// [zh] 声明变量 `showAi`
const showAi = computed(() => {
  // [zh] 条件判断 if
  if (messageStatus.value === 0) return hasRenderableContent.value;
  // [zh] 条件判断 if
  if (props.waiting) return true;
  // [zh] 条件判断 if
  if (isStreaming.value) return true;
  // [zh] 条件判断 if
  if (messageStatus.value === 3) return hasRenderableContent.value;
  // [zh] 条件判断 if
  if (productList.value?.length || orderList.value?.length) return true;
  // [zh] 条件判断 if
  if (actionConfirmCard.value) return true;
  // [zh] 条件判断 if
  if (isProductSearchEmpty.value || isOrderSearchEmpty.value) return true;
  // [zh] 条件判断 if
  if (displayText.value) return true;
  // [zh] return 返回
  return false;
// [zh] 语句
});

// [zh] 声明变量 `hasRenderableContent`
const hasRenderableContent = computed(
  // [zh] TypeScript/JS 语句：`() =>`
  () =>
    // [zh] TypeScript/JS 语句：`!!(productList.value?.length ||`
    !!(productList.value?.length ||
      // [zh] TypeScript/JS 语句：`orderList.value?.length ||`
      orderList.value?.length ||
      // [zh] TypeScript/JS 语句：`actionConfirmCard.value ||`
      actionConfirmCard.value ||
      // [zh] TypeScript/JS 语句：`isProductSearchEmpty.value ||`
      isProductSearchEmpty.value ||
      // [zh] TypeScript/JS 语句：`isOrderSearchEmpty.value ||`
      isOrderSearchEmpty.value ||
      // [zh] TypeScript/JS 语句：`displayText.value ||`
      displayText.value ||
      // [zh] TypeScript/JS 语句：`props.waiting ||`
      props.waiting ||
      // [zh] TypeScript/JS 语句：`(isStreaming.value && streamText.value))`
      (isStreaming.value && streamText.value))
// [zh] 语句
);

// [zh] 声明变量 `isWideBubble`
const isWideBubble = computed(
  // [zh] TypeScript/JS 语句：`() =>`
  () =>
    // [zh] TypeScript/JS 语句：`!!(`
    !!(
      // [zh] TypeScript/JS 语句：`productList.value?.length ||`
      productList.value?.length ||
      // [zh] TypeScript/JS 语句：`orderList.value?.length ||`
      orderList.value?.length ||
      // [zh] TypeScript/JS 语句：`actionConfirmCard.value ||`
      actionConfirmCard.value ||
      // [zh] TypeScript/JS 语句：`isProductSearchEmpty.value ||`
      isProductSearchEmpty.value ||
      // [zh] TypeScript/JS 语句：`isOrderSearchEmpty.value ||`
      isOrderSearchEmpty.value ||
      // [zh] TypeScript/JS 语句：`agentRich.value`
      agentRich.value
    // [zh] 语法符号
    )
// [zh] 语句
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
