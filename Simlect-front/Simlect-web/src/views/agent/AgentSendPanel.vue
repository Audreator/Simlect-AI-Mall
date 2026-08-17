<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div
    ref="composerRef"
    class="agent-composer-stack ignore"
    :class="{ 'is-embedded': composerEmbedded }"
  >
    <!-- [zh] 开始标签 `<section>` -->
    <section v-if="pendingProduct" class="consult-product-float">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="float-head">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="float-label">{{ resumeConsult ? '继续咨询商品' : '咨询商品' }}</p>
        <!-- [zh] 开始标签 `<button>` -->
        <button
          type="button"
          class="float-close"
          aria-label="结束商品咨询"
          @click="dismissProductConsult"
        >
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon :size="14"><Close /></el-icon>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<AgentConsultProductCard>` -->
      <AgentConsultProductCard
        :product="pendingProduct"
        :resuming="resumeConsult"
        variant="composer"
        clickable
        @send="sendProductConsult"
      />
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<LiquidGlassSurface>` -->
    <LiquidGlassSurface intensity="medium" class="agent-composer-dock">
    <!-- [zh] 开始标签 `<section>` -->
    <section class="quick-tips">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="tips-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="tips-label">试试这样说</span>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="tips-scroll-wrap">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-if="isDesktop && canScrollLeft"
            type="button"
            class="scroll-arrow scroll-left"
            aria-label="向左滚动"
            @click="scrollTips(-1)"
          >
            <!-- [zh] 开始标签 `<el-icon>` -->
            <el-icon :size="12"><ArrowLeft /></el-icon>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
          <!-- [zh] 开始标签 `<div>` -->
          <div
            ref="tipsScrollRef"
            class="tips-scroll"
            role="list"
            aria-label="快捷提问"
            @scroll="onTipsScroll"
          >
            <!-- [zh] 开始标签 `<button>` -->
            <button
              v-for="tip in tips"
              :key="tip"
              type="button"
              class="tip-chip"
              role="listitem"
              @click="applyTip(tip)"
            >
              <!-- [zh] Mustache 插值表达式 -->
              {{ tip }}
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-if="isDesktop && canScrollRight"
            type="button"
            class="scroll-arrow scroll-right"
            aria-label="向右滚动"
            @click="scrollTips(1)"
          >
            <!-- [zh] 开始标签 `<el-icon>` -->
            <el-icon :size="12"><ArrowRight /></el-icon>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<footer>` -->
    <footer class="chat-input-bar ignore">
      <!-- [zh] 开始标签 `<textarea>` -->
      <textarea
        ref="textareaRef"
        v-model="input"
        class="agent-chat-textarea"
        rows="1"
        maxlength="500"
        enterkeyhint="send"
        autocomplete="off"
        autocorrect="on"
        placeholder="输入你想咨询的问题"
        :disabled="answering"
        :readonly="!isDesktop"
        @focus="onTextareaFocus"
        @blur="onTextareaBlur"
        @input="onTextareaInput"
        @keydown="onTextareaKeydown"
      />
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-if="!answering"
        type="button"
        class="btn-send btn-send-native"
        aria-label="发送消息"
        :disabled="!input.trim()"
        @click="sendMessage"
      >
        <!-- [zh] 模板内容：`发送` -->
        发送
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
      <!-- [zh] 开始标签 `<button>` -->
      <button v-else type="button" class="btn-send btn-send-native btn-stop" aria-label="停止生成" @click="stop">停止</button>
    <!-- [zh] 闭合标签 `</footer>` -->
    </footer>
    <!-- [zh] 闭合标签 `</LiquidGlassSurface>` -->
    </LiquidGlassSurface>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { inject, nextTick, onMounted, onUnmounted, ref, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { agentComposerEmbeddedKey } from '@/composables/agentEmbed';
import { useDevice } from '@/composables/useDevice';
import { agentApi } from '@/api/modules';
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
import AgentConsultProductCard from '@/components/agent/AgentConsultProductCard.vue';
import { AGENT_OUTPUT_TYPE } from '@/constants/backendEnums';
import { useAgentMessageStore } from '@/stores/agentMessage';
import { useAuthStore } from '@/stores/auth';
import { usePcAgentPanelStore } from '@/stores/pcAgentPanel';
import {
  buildProductConsultMessage,
  clearAgentConsultProduct,
  loadAgentConsultProduct,
  type AgentConsultProduct
} from '@/utils/agentProductConsult';
import { ensureAppWebSocket } from '@/utils/websocket/manager';
import { mitter } from '@/utils/eventBus';
import { lockViewportAfterInput, recoverIosViewportZoom } from '@/utils/mobileViewport';
import { toast } from '@/utils/toast';
import { showTopAlert } from '@/utils/topAlert';

const TIP_POOL = [
  '帮我推荐热销商品',
  '我的订单到哪了',
  '如何申请退款',
  '有什么优惠活动',
  '我要评价订单',
  '我的优惠券在哪用',
  '最近有什么新品',
  '如何修改收货地址',
  '怎么查看物流信息',
  '如何取消订单',
  '商品有质量问题怎么办',
  '如何查看我的足迹',
  '我的收藏在哪里看',
  '签到有什么奖励',
  '怎么查看会员等级',
  '支付方式有哪些',
  '上次买的商品能再买一次吗',
  '如何换货',
  '如何举报评价',
  '我的消息在哪里看'
];

const PRODUCT_CONSULT_TIPS = [
  '这件商品有货吗',
  '有哪些规格可选',
  '适合什么人用',
  '和同类比有什么优势',
  '退换货怎么算',
  '大概几天能到货'
];

function pickRandom<T>(arr: T[], count: number): T[] {
  const shuffled = [...arr];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled.slice(0, count);
}

const pendingProduct = ref<AgentConsultProduct | null>(null);
const resumeConsult = ref(false);

const tips = computed(() => {
  if (pendingProduct.value) {
    return PRODUCT_CONSULT_TIPS;
  }
  return pickRandom(TIP_POOL, 3);
});

const composerEmbedded = inject(agentComposerEmbeddedKey, false);

const { isDesktop } = useDevice();

const tipsScrollRef = ref<HTMLElement | null>(null);
const canScrollLeft = ref(false);
const canScrollRight = ref(false);

const checkScrollable = () => {
  const el = tipsScrollRef.value;
  if (!el) { canScrollLeft.value = false; canScrollRight.value = false; return; }
  canScrollLeft.value = el.scrollLeft > 2;
  canScrollRight.value = el.scrollLeft < el.scrollWidth - el.clientWidth - 2;
};

const onTipsScroll = () => { checkScrollable(); };

const scrollTips = (dir: number) => {
  const el = tipsScrollRef.value;
  if (!el) return;
  const step = Math.max(el.clientWidth * 0.6, 120);
  el.scrollBy({ left: dir * step, behavior: 'smooth' });

  setTimeout(checkScrollable, 350);
};

const agentMessageStore = useAgentMessageStore();
const authStore = useAuthStore();
const pcAgentPanel = usePcAgentPanelStore();
const route = useRoute();
const router = useRouter();

const currentUserId = () => authStore.userInfo?.userId as string | undefined;

const initPendingConsultProduct = () => {
  const userId = currentUserId();
  if (composerEmbedded) {
    if (pcAgentPanel.consumeFromProduct()) {
      pendingProduct.value = loadAgentConsultProduct(userId);
      return;
    }
    pendingProduct.value = null;
    return;
  }
  const product = loadAgentConsultProduct(userId) || loadAgentConsultProduct();
  if (product) {
    pendingProduct.value = product;
    clearAgentConsultProduct(userId);
    clearAgentConsultProduct();
  }
};

const syncConsultResumeState = async () => {
  resumeConsult.value = false;
  const product = pendingProduct.value;
  if (!product?.productId || !authStore.isLoggedIn) return;
  try {
    const ctx = await agentApi.getProductConsultContext();
    resumeConsult.value = ctx?.productId === product.productId;
  } catch {
    resumeConsult.value = false;
  }
};

const resolveConsultProductId = (): string | undefined => {
  if (pendingProduct.value?.productId) return pendingProduct.value.productId;
  const m = window.location.pathname.match(/^\/product\/([^/]+)$/);
  return m?.[1];
};

const composerRef = ref<HTMLElement | null>(null);
const textareaRef = ref<HTMLTextAreaElement | null>(null);
const input = ref('');
const TEXTAREA_MAX_HEIGHT = 96;
const answering = ref(false);
const messageId = ref<number | null>(null);

const resetComposerIdle = () => {
  answering.value = false;
  messageId.value = null;
};

let composerResizeObserver: ResizeObserver | null = null;

let composerInsetReadyEmitted = false;

const syncComposerInset = () => {
  if (isDesktop.value) {
    document.documentElement.style.setProperty('--agent-composer-inset', '0px');
    if (!composerInsetReadyEmitted) {
      composerInsetReadyEmitted = true;
      mitter.emit('agentComposerReady');
    }
    return;
  }
  const height = composerRef.value?.offsetHeight ?? 0;
  document.documentElement.style.setProperty('--agent-composer-inset', `${height}px`);
  if (!composerInsetReadyEmitted && height > 0) {
    composerInsetReadyEmitted = true;
    mitter.emit('agentComposerReady');
  }
};

const teardownComposerInset = () => {
  composerResizeObserver?.disconnect();
  composerResizeObserver = null;
  document.documentElement.style.removeProperty('--agent-composer-inset');
};

const adjustTextareaHeight = () => {
  const el = textareaRef.value;
  if (!el) return;
  el.style.height = 'auto';
  const next = Math.min(el.scrollHeight, TEXTAREA_MAX_HEIGHT);
  el.style.height = `${Math.max(next, 40)}px`;
};

const onTextareaFocus = (event: FocusEvent) => {
  const el = event.target as HTMLTextAreaElement;
  el.removeAttribute('readonly');
  adjustTextareaHeight();
};

const onTextareaBlur = () => {
  const el = textareaRef.value;
  if (el) el.setAttribute('readonly', '');
  recoverIosViewportZoom();
  window.setTimeout(lockViewportAfterInput, 300);
};

const onTextareaInput = () => {
  adjustTextareaHeight();
};

const onTextareaKeydown = (event: KeyboardEvent) => {
  if (!isDesktop.value) return;
  if (event.key !== 'Enter') return;
  if (event.shiftKey || event.ctrlKey || event.altKey || event.metaKey) return;
  if (event.isComposing || event.keyCode === 229) return;
  event.preventDefault();
  void sendMessage();
};

const applyTip = (tip: string) => {
  input.value = tip;
  adjustTextareaHeight();
};

const dispatchSend = async (text: string) => {
  if (!text || answering.value) return false;

  ensureAppWebSocket();

  try {
    const path = window.location.pathname;
    const isProductPage = /^\/product\/\d+$/.test(path);
    const fromProduct = pendingProduct.value !== null || isProductPage;
    const consultProductId = fromProduct ? resolveConsultProductId() : undefined;
    const data = await agentApi.sendMessage(text, fromProduct, consultProductId);
    if (!data?.messageId) {
      toast.error('发送失败，请重试');
      return false;
    }
    messageId.value = data.messageId;
    data.assistantMessage = '';
    answering.value = true;
    mitter.emit('sendMessage', { ...data });
    return true;
  } catch (e: any) {
    if (e?.info === 'AI购物体验已经结束') {
      showTopAlert('AI购物体验已经结束');
      answering.value = false;
      return false;
    }
    toast.error(e?.info || '发送失败，请重试');
    return false;
  }
};

const sendMessage = async () => {
  const text = input.value.trim();
  if (!text || answering.value) return;
  const ok = await dispatchSend(text);
  if (ok) {
    input.value = '';
    adjustTextareaHeight();
    if (!isDesktop.value) {
      textareaRef.value?.blur();
      recoverIosViewportZoom();
    } else {
      textareaRef.value?.focus();
    }
  }
};

const sendProductConsult = async () => {
  const product = pendingProduct.value;
  if (!product || answering.value) return;
  const text = buildProductConsultMessage(product);
  const ok = await dispatchSend(text);
  if (ok) {
    pendingProduct.value = null;
    clearAgentConsultProduct(currentUserId());
  }
};

const dismissProductConsult = async () => {
  pendingProduct.value = null;
  clearAgentConsultProduct(currentUserId());
  clearAgentConsultProduct();
  try {
    await agentApi.clearProductConsult();
  } catch {

  }
};

const stop = async () => {
  const id = messageId.value;
  if (id == null) {
    toast.error('无法取消，请刷新页面后重试');
    return;
  }

  mitter.emit('cancelMessage', { messageId: id });
  answering.value = false;
  messageId.value = null;
};

const onAnswering = (val: unknown) => {
  if (typeof val === 'boolean') {
    if (val) {
      answering.value = true;
    } else {
      resetComposerIdle();
    }
    return;
  }
  const payload = val as { answering?: boolean; messageId?: number };
  if (payload.answering) {
    answering.value = true;
    if (payload.messageId != null) messageId.value = payload.messageId;
  } else {
    resetComposerIdle();
  }
};

watch(input, () => {
  adjustTextareaHeight();
});

watch(
  () => agentMessageStore.message,
  (msg) => {
    if (!msg) return;
    const outputType = Number(msg.outPutType);
    if (outputType === AGENT_OUTPUT_TYPE.DONE || outputType === AGENT_OUTPUT_TYPE.ERROR) {
      resetComposerIdle();
    }
  }
);

const onSendMessageSync = (payload: unknown) => {
  const msg = payload as { messageId?: number };
  if (msg?.messageId != null) messageId.value = msg.messageId;
};

onMounted(() => {
  initPendingConsultProduct();
  void syncConsultResumeState();

  const fromProductEntry = route.query.fromProduct === '1';
  if (!pendingProduct.value && !fromProductEntry && authStore.isLoggedIn) {
    void agentApi.pauseProductConsult().catch(() => {});
  }
  mitter.on('answering', onAnswering);
  mitter.on('sendMessage', onSendMessageSync);
  syncComposerInset();
  composerResizeObserver = new ResizeObserver(syncComposerInset);
  if (composerRef.value) composerResizeObserver.observe(composerRef.value);
  adjustTextareaHeight();

  nextTick(() => {
    checkScrollable();
    const scrollEl = tipsScrollRef.value;
    if (scrollEl) {
      const ro = new ResizeObserver(() => { checkScrollable(); });
      ro.observe(scrollEl);
    }
  });

  const presetMsg = route.query.msg as string | undefined;
  if (presetMsg) {
    router.replace({ query: { ...route.query, msg: undefined } });
    setTimeout(() => {
      if (!answering.value) {
        input.value = presetMsg;
        adjustTextareaHeight();
        sendMessage();
      }
    }, 400);
  }
});

onUnmounted(() => {
  recoverIosViewportZoom();
  mitter.off('answering', onAnswering);
  mitter.off('sendMessage', onSendMessageSync);
  teardownComposerInset();
  composerInsetReadyEmitted = false;
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.agent-composer-stack {` */
.agent-composer-stack {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `left: 0;` */
  left: 0;
  /* [zh] 样式规则 `right: 0;` */
  right: 0;
  /* [zh] 样式规则 `bottom: 0;` */
  bottom: 0;
  /* [zh] 样式规则 `z-index: $z-index-agent-composer;` */
  z-index: $z-index-agent-composer;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: none;` */
  max-width: none;
  /* [zh] 样式规则 `padding-left: env(safe-area-inset-left, ` */
  padding-left: env(safe-area-inset-left, 0);
  /* [zh] 样式规则 `padding-right: env(safe-area-inset-right` */
  padding-right: env(safe-area-inset-right, 0);
  /* [zh] 样式规则 `padding-bottom: env(safe-area-inset-bott` */
  padding-bottom: env(safe-area-inset-bottom, 0);
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;

  /* [zh] 样式规则 `&.is-embedded {` */
  &.is-embedded {
    /* [zh] 样式规则 `position: static;` */
    position: static;
    /* [zh] 样式规则 `left: auto;` */
    left: auto;
    /* [zh] 样式规则 `right: auto;` */
    right: auto;
    /* [zh] 样式规则 `bottom: auto;` */
    bottom: auto;
    /* [zh] 样式规则 `z-index: 1;` */
    z-index: 1;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `max-width: 100%;` */
    max-width: 100%;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `pointer-events: auto;` */
    pointer-events: auto;
    /* [zh] 样式规则 `gap: 0;` */
    gap: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.consult-product-float {` */
.consult-product-float {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `margin: 0 $app-page-gutter;` */
  margin: 0 $app-page-gutter;
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `background: var(--glass-bg-light);` */
  background: var(--glass-bg-light);
  /* [zh] 样式规则 `-webkit-backdrop-filter: var(--glass-blu` */
  -webkit-backdrop-filter: var(--glass-blur-sm);
  /* [zh] 样式规则 `backdrop-filter: var(--glass-blur-sm);` */
  backdrop-filter: var(--glass-blur-sm);
  /* [zh] 样式规则 `border: 1px solid var(--glass-border-sof` */
  border: 1px solid var(--glass-border-soft);
  /* [zh] 样式规则 `box-shadow: var(--glass-shadow-sm);` */
  box-shadow: var(--glass-shadow-sm);
  /* [zh] 样式规则 `pointer-events: auto;` */
  pointer-events: auto;

  /* [zh] 样式规则 `.float-label {` */
  .float-label {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.float-head {` */
.float-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.float-close {` */
.float-close {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `width: 24px;` */
  width: 24px;
  /* [zh] 样式规则 `height: 24px;` */
  height: 24px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.agent-composer-dock {` */
.agent-composer-dock {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `border-top: 1px solid var(--glass-border` */
  border-top: 1px solid var(--glass-border-soft);
  /* [zh] 样式规则 `box-shadow: 0 -6px 24px rgba(20, 22, 26,` */
  box-shadow: 0 -6px 24px rgba(20, 22, 26, 0.06);
  /* [zh] 样式规则 `pointer-events: auto;` */
  pointer-events: auto;
  /* [zh] 样式规则 `touch-action: manipulation;` */
  touch-action: manipulation;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.quick-tips {` */
.quick-tips {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `padding: 10px 0 10px $app-page-gutter;` */
  padding: 10px 0 10px $app-page-gutter;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tips-row {` */
.tips-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tips-label {` */
.tips-label {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tips-scroll-wrap {` */
.tips-scroll-wrap {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `padding-right: $app-page-gutter;` */
  padding-right: $app-page-gutter;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tips-scroll {` */
.tips-scroll {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `overflow-x: auto;` */
  overflow-x: auto;
  /* [zh] 样式规则 `overflow-y: hidden;` */
  overflow-y: hidden;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
  -webkit-overflow-scrolling: touch;
  /* [zh] 样式规则 `scroll-snap-type: x proximity;` */
  scroll-snap-type: x proximity;
  /* [zh] 样式规则 `scrollbar-width: none;` */
  scrollbar-width: none;

  /* [zh] 样式规则 `&::-webkit-scrollbar {` */
  &::-webkit-scrollbar {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.scroll-arrow {` */
.scroll-arrow {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `width: 20px;` */
  width: 20px;
  /* [zh] 样式规则 `height: 20px;` */
  height: 20px;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
  border: 1px solid $color-border-gray;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: color $transition-fast, bord` */
  transition: color $transition-fast, border-color $transition-fast;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `border-color: $color-text-muted;` */
    border-color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tip-chip {` */
.tip-chip {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `scroll-snap-align: start;` */
  scroll-snap-align: start;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
  /* [zh] 样式规则 `border: 1px solid rgba($color-primary, 0` */
  border: 1px solid rgba($color-primary, 0.2);
  /* [zh] 样式规则 `background: $color-surface-inset;` */
  background: $color-surface-inset;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `padding: 7px 12px;` */
  padding: 7px 12px;
  /* [zh] 样式规则 `border-radius: $radius-pill;` */
  border-radius: $radius-pill;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: background $transition-fast,` */
  transition: background $transition-fast, border-color $transition-fast, color $transition-fast;
  /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
  -webkit-tap-highlight-color: transparent;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `background: $color-primary-soft;` */
    background: $color-primary-soft;
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.35)` */
    border-color: rgba($color-primary, 0.35);
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.chat-input-bar {` */
.chat-input-bar {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `align-items: flex-end;` */
  align-items: flex-end;
  /* [zh] 样式规则 `padding: 10px $app-page-gutter;` */
  padding: 10px $app-page-gutter;

  /* [zh] 样式规则 `.agent-chat-textarea {` */
  .agent-chat-textarea {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `min-height: 40px;` */
    min-height: 40px;
    /* [zh] 样式规则 `max-height: 96px;` */
    max-height: 96px;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `padding: 9px 12px;` */
    padding: 9px 12px;
    /* [zh] 样式规则 `border: 1px solid $color-border;` */
    border: 1px solid $color-border;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `line-height: 1.45;` */
    line-height: 1.45;
    /* [zh] 样式规则 `font-family: inherit;` */
    font-family: inherit;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `background: #fff;` */
    background: #fff;
    /* [zh] 样式规则 `resize: none;` */
    resize: none;
    /* [zh] 样式规则 `outline: none;` */
    outline: none;
    /* [zh] 样式规则 `box-sizing: border-box;` */
    box-sizing: border-box;
    /* [zh] 样式规则 `touch-action: manipulation;` */
    touch-action: manipulation;
    /* [zh] 样式规则 `-webkit-text-size-adjust: 100%;` */
    -webkit-text-size-adjust: 100%;
    /* [zh] 样式规则 `appearance: none;` */
    appearance: none;

    /* [zh] 样式规则 `&::placeholder {` */
    &::placeholder {
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&:disabled {` */
    &:disabled {
      /* [zh] 样式规则 `background: $color-surface-inset;` */
      background: $color-surface-inset;
      /* [zh] 样式规则 `color: $color-text-disabled;` */
      color: $color-text-disabled;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.btn-send-native {` */
  .btn-send-native {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `height: 40px;` */
    height: 40px;
    /* [zh] 样式规则 `min-width: 72px;` */
    min-width: 72px;
    /* [zh] 样式规则 `padding: 0 16px;` */
    padding: 0 16px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `background: $color-primary;` */
    background: $color-primary;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `touch-action: manipulation;` */
    touch-action: manipulation;
    /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
    -webkit-tap-highlight-color: transparent;

    /* [zh] 样式规则 `&:disabled {` */
    &:disabled {
      /* [zh] 样式规则 `opacity: 0.45;` */
      opacity: 0.45;
      /* [zh] 样式规则 `cursor: not-allowed;` */
      cursor: not-allowed;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.btn-stop {` */
    &.btn-stop {
      /* [zh] 样式规则 `color: $color-error;` */
      color: $color-error;
      /* [zh] 样式规则 `background: #fff;` */
      background: #fff;
      /* [zh] 样式规则 `border: 1px solid $color-error-border;` */
      border: 1px solid $color-error-border;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
