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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { inject, nextTick, onMounted, onUnmounted, ref, watch, computed } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@/composables/agentEmbed`
import { agentComposerEmbeddedKey } from '@/composables/agentEmbed';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/api/modules`
import { agentApi } from '@/api/modules';
// [zh] 导入 `@/components/common/LiquidGlassSurface.vue`
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
// [zh] 导入 `@/components/agent/AgentConsultProductCard.vue`
import AgentConsultProductCard from '@/components/agent/AgentConsultProductCard.vue';
// [zh] 导入 `@/constants/backendEnums`
import { AGENT_OUTPUT_TYPE } from '@/constants/backendEnums';
// [zh] 导入 `@/stores/agentMessage`
import { useAgentMessageStore } from '@/stores/agentMessage';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/stores/pcAgentPanel`
import { usePcAgentPanelStore } from '@/stores/pcAgentPanel';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`buildProductConsultMessage,`
  buildProductConsultMessage,
  // [zh] TypeScript/JS 语句：`clearAgentConsultProduct,`
  clearAgentConsultProduct,
  // [zh] TypeScript/JS 语句：`loadAgentConsultProduct,`
  loadAgentConsultProduct,
  // [zh] TypeScript 类型 `AgentConsultProduct`（类比 Java interface/DTO）
  type AgentConsultProduct
// [zh] 语句
} from '@/utils/agentProductConsult';
// [zh] 导入 `@/utils/websocket/manager`
import { ensureAppWebSocket } from '@/utils/websocket/manager';
// [zh] 导入 `@/utils/eventBus`
import { mitter } from '@/utils/eventBus';
// [zh] 导入 `@/utils/mobileViewport`
import { lockViewportAfterInput, recoverIosViewportZoom } from '@/utils/mobileViewport';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/utils/topAlert`
import { showTopAlert } from '@/utils/topAlert';

// [zh] 声明变量 `TIP_POOL`
const TIP_POOL = [
  // [zh] TypeScript/JS 语句：`'帮我推荐热销商品',`
  '帮我推荐热销商品',
  // [zh] TypeScript/JS 语句：`'我的订单到哪了',`
  '我的订单到哪了',
  // [zh] TypeScript/JS 语句：`'如何申请退款',`
  '如何申请退款',
  // [zh] TypeScript/JS 语句：`'有什么优惠活动',`
  '有什么优惠活动',
  // [zh] TypeScript/JS 语句：`'我要评价订单',`
  '我要评价订单',
  // [zh] TypeScript/JS 语句：`'我的优惠券在哪用',`
  '我的优惠券在哪用',
  // [zh] TypeScript/JS 语句：`'最近有什么新品',`
  '最近有什么新品',
  // [zh] TypeScript/JS 语句：`'如何修改收货地址',`
  '如何修改收货地址',
  // [zh] TypeScript/JS 语句：`'怎么查看物流信息',`
  '怎么查看物流信息',
  // [zh] TypeScript/JS 语句：`'如何取消订单',`
  '如何取消订单',
  // [zh] TypeScript/JS 语句：`'商品有质量问题怎么办',`
  '商品有质量问题怎么办',
  // [zh] TypeScript/JS 语句：`'如何查看我的足迹',`
  '如何查看我的足迹',
  // [zh] TypeScript/JS 语句：`'我的收藏在哪里看',`
  '我的收藏在哪里看',
  // [zh] TypeScript/JS 语句：`'签到有什么奖励',`
  '签到有什么奖励',
  // [zh] TypeScript/JS 语句：`'怎么查看会员等级',`
  '怎么查看会员等级',
  // [zh] TypeScript/JS 语句：`'支付方式有哪些',`
  '支付方式有哪些',
  // [zh] TypeScript/JS 语句：`'上次买的商品能再买一次吗',`
  '上次买的商品能再买一次吗',
  // [zh] TypeScript/JS 语句：`'如何换货',`
  '如何换货',
  // [zh] TypeScript/JS 语句：`'如何举报评价',`
  '如何举报评价',
  // [zh] TypeScript/JS 语句：`'我的消息在哪里看'`
  '我的消息在哪里看'
// [zh] 语句
];

// [zh] 声明变量 `PRODUCT_CONSULT_TIPS`
const PRODUCT_CONSULT_TIPS = [
  // [zh] TypeScript/JS 语句：`'这件商品有货吗',`
  '这件商品有货吗',
  // [zh] TypeScript/JS 语句：`'有哪些规格可选',`
  '有哪些规格可选',
  // [zh] TypeScript/JS 语句：`'适合什么人用',`
  '适合什么人用',
  // [zh] TypeScript/JS 语句：`'和同类比有什么优势',`
  '和同类比有什么优势',
  // [zh] TypeScript/JS 语句：`'退换货怎么算',`
  '退换货怎么算',
  // [zh] TypeScript/JS 语句：`'大概几天能到货'`
  '大概几天能到货'
// [zh] 语句
];

// [zh] 函数 `pickRandom`
function pickRandom<T>(arr: T[], count: number): T[] {
  // [zh] 声明变量 `shuffled`
  const shuffled = [...arr];
  // [zh] for 循环
  for (let i = shuffled.length - 1; i > 0; i--) {
    // [zh] 声明变量 `j`
    const j = Math.floor(Math.random() * (i + 1));
    // [zh] 语句
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  // [zh] 语法符号
  }
  // [zh] return 返回
  return shuffled.slice(0, count);
// [zh] 语法符号
}

// [zh] 声明变量 `pendingProduct`
const pendingProduct = ref<AgentConsultProduct | null>(null);
// [zh] 声明变量 `resumeConsult`
const resumeConsult = ref(false);

// [zh] 声明变量 `tips`
const tips = computed(() => {
  // [zh] 条件判断 if
  if (pendingProduct.value) {
    // [zh] return 返回
    return PRODUCT_CONSULT_TIPS;
  // [zh] 语法符号
  }
  // [zh] return 返回
  return pickRandom(TIP_POOL, 3);
// [zh] 语句
});

// [zh] 声明变量 `composerEmbedded`
const composerEmbedded = inject(agentComposerEmbeddedKey, false);

// [zh] 语句
const { isDesktop } = useDevice();

// [zh] 声明变量 `tipsScrollRef`
const tipsScrollRef = ref<HTMLElement | null>(null);
// [zh] 声明变量 `canScrollLeft`
const canScrollLeft = ref(false);
// [zh] 声明变量 `canScrollRight`
const canScrollRight = ref(false);

// [zh] 声明变量 `checkScrollable`
const checkScrollable = () => {
  // [zh] 声明变量 `el`
  const el = tipsScrollRef.value;
  // [zh] 条件判断 if
  if (!el) { canScrollLeft.value = false; canScrollRight.value = false; return; }
  // [zh] 语句
  canScrollLeft.value = el.scrollLeft > 2;
  // [zh] 语句
  canScrollRight.value = el.scrollLeft < el.scrollWidth - el.clientWidth - 2;
// [zh] 语句
};

// [zh] 声明变量 `onTipsScroll`
const onTipsScroll = () => { checkScrollable(); };

// [zh] 声明变量 `scrollTips`
const scrollTips = (dir: number) => {
  // [zh] 声明变量 `el`
  const el = tipsScrollRef.value;
  // [zh] 条件判断 if
  if (!el) return;
  // [zh] 声明变量 `step`
  const step = Math.max(el.clientWidth * 0.6, 120);
  // [zh] 语句
  el.scrollBy({ left: dir * step, behavior: 'smooth' });

  // [zh] 语句
  setTimeout(checkScrollable, 350);
// [zh] 语句
};

// [zh] 声明变量 `agentMessageStore`
const agentMessageStore = useAgentMessageStore();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `pcAgentPanel`
const pcAgentPanel = usePcAgentPanelStore();
// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `router`
const router = useRouter();

// [zh] 声明变量 `currentUserId`
const currentUserId = () => authStore.userInfo?.userId as string | undefined;

// [zh] 声明变量 `initPendingConsultProduct`
const initPendingConsultProduct = () => {
  // [zh] 声明变量 `userId`
  const userId = currentUserId();
  // [zh] 条件判断 if
  if (composerEmbedded) {
    // [zh] 条件判断 if
    if (pcAgentPanel.consumeFromProduct()) {
      // [zh] 语句
      pendingProduct.value = loadAgentConsultProduct(userId);
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 语句
    pendingProduct.value = null;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `product`
  const product = loadAgentConsultProduct(userId) || loadAgentConsultProduct();
  // [zh] 条件判断 if
  if (product) {
    // [zh] 语句
    pendingProduct.value = product;
    // [zh] 语句
    clearAgentConsultProduct(userId);
    // [zh] 语句
    clearAgentConsultProduct();
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `syncConsultResumeState`
const syncConsultResumeState = async () => {
  // [zh] 语句
  resumeConsult.value = false;
  // [zh] 声明变量 `product`
  const product = pendingProduct.value;
  // [zh] 条件判断 if
  if (!product?.productId || !authStore.isLoggedIn) return;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `ctx`
    const ctx = await agentApi.getProductConsultContext();
    // [zh] 语句
    resumeConsult.value = ctx?.productId === product.productId;
  // [zh] 代码块开始
  } catch {
    // [zh] 语句
    resumeConsult.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `resolveConsultProductId`
const resolveConsultProductId = (): string | undefined => {
  // [zh] 条件判断 if
  if (pendingProduct.value?.productId) return pendingProduct.value.productId;
  // [zh] 声明变量 `m`
  const m = window.location.pathname.match(/^\/product\/([^/]+)$/);
  // [zh] return 返回
  return m?.[1];
// [zh] 语句
};

// [zh] 声明变量 `composerRef`
const composerRef = ref<HTMLElement | null>(null);
// [zh] 声明变量 `textareaRef`
const textareaRef = ref<HTMLTextAreaElement | null>(null);
// [zh] 声明变量 `input`
const input = ref('');
// [zh] 声明变量 `TEXTAREA_MAX_HEIGHT`
const TEXTAREA_MAX_HEIGHT = 96;
// [zh] 声明变量 `answering`
const answering = ref(false);
// [zh] 声明变量 `messageId`
const messageId = ref<number | null>(null);

// [zh] 声明变量 `resetComposerIdle`
const resetComposerIdle = () => {
  // [zh] 语句
  answering.value = false;
  // [zh] 语句
  messageId.value = null;
// [zh] 语句
};

// [zh] 声明变量 `composerResizeObserver`
let composerResizeObserver: ResizeObserver | null = null;

// [zh] 声明变量 `composerInsetReadyEmitted`
let composerInsetReadyEmitted = false;

// [zh] 声明变量 `syncComposerInset`
const syncComposerInset = () => {
  // [zh] 条件判断 if
  if (isDesktop.value) {
    // [zh] 语句
    document.documentElement.style.setProperty('--agent-composer-inset', '0px');
    // [zh] 条件判断 if
    if (!composerInsetReadyEmitted) {
      // [zh] 语句
      composerInsetReadyEmitted = true;
      // [zh] 语句
      mitter.emit('agentComposerReady');
    // [zh] 语法符号
    }
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `height`
  const height = composerRef.value?.offsetHeight ?? 0;
  // [zh] 语句
  document.documentElement.style.setProperty('--agent-composer-inset', `${height}px`);
  // [zh] 条件判断 if
  if (!composerInsetReadyEmitted && height > 0) {
    // [zh] 语句
    composerInsetReadyEmitted = true;
    // [zh] 语句
    mitter.emit('agentComposerReady');
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `teardownComposerInset`
const teardownComposerInset = () => {
  // [zh] 语句
  composerResizeObserver?.disconnect();
  // [zh] 语句
  composerResizeObserver = null;
  // [zh] 语句
  document.documentElement.style.removeProperty('--agent-composer-inset');
// [zh] 语句
};

// [zh] 声明变量 `adjustTextareaHeight`
const adjustTextareaHeight = () => {
  // [zh] 声明变量 `el`
  const el = textareaRef.value;
  // [zh] 条件判断 if
  if (!el) return;
  // [zh] 语句
  el.style.height = 'auto';
  // [zh] 声明变量 `next`
  const next = Math.min(el.scrollHeight, TEXTAREA_MAX_HEIGHT);
  // [zh] 语句
  el.style.height = `${Math.max(next, 40)}px`;
// [zh] 语句
};

// [zh] 声明变量 `onTextareaFocus`
const onTextareaFocus = (event: FocusEvent) => {
  // [zh] 声明变量 `el`
  const el = event.target as HTMLTextAreaElement;
  // [zh] 语句
  el.removeAttribute('readonly');
  // [zh] 语句
  adjustTextareaHeight();
// [zh] 语句
};

// [zh] 声明变量 `onTextareaBlur`
const onTextareaBlur = () => {
  // [zh] 声明变量 `el`
  const el = textareaRef.value;
  // [zh] 条件判断 if
  if (el) el.setAttribute('readonly', '');
  // [zh] 语句
  recoverIosViewportZoom();
  // [zh] 语句
  window.setTimeout(lockViewportAfterInput, 300);
// [zh] 语句
};

// [zh] 声明变量 `onTextareaInput`
const onTextareaInput = () => {
  // [zh] 语句
  adjustTextareaHeight();
// [zh] 语句
};

// [zh] 声明变量 `onTextareaKeydown`
const onTextareaKeydown = (event: KeyboardEvent) => {
  // [zh] 条件判断 if
  if (!isDesktop.value) return;
  // [zh] 条件判断 if
  if (event.key !== 'Enter') return;
  // [zh] 条件判断 if
  if (event.shiftKey || event.ctrlKey || event.altKey || event.metaKey) return;
  // [zh] 条件判断 if
  if (event.isComposing || event.keyCode === 229) return;
  // [zh] 语句
  event.preventDefault();
  // [zh] 语句
  void sendMessage();
// [zh] 语句
};

// [zh] 声明变量 `applyTip`
const applyTip = (tip: string) => {
  // [zh] 语句
  input.value = tip;
  // [zh] 语句
  adjustTextareaHeight();
// [zh] 语句
};

// [zh] 声明变量 `dispatchSend`
const dispatchSend = async (text: string) => {
  // [zh] 条件判断 if
  if (!text || answering.value) return false;

  // [zh] 语句
  ensureAppWebSocket();

  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `path`
    const path = window.location.pathname;
    // [zh] 声明变量 `isProductPage`
    const isProductPage = /^\/product\/\d+$/.test(path);
    // [zh] 声明变量 `fromProduct`
    const fromProduct = pendingProduct.value !== null || isProductPage;
    // [zh] 声明变量 `consultProductId`
    const consultProductId = fromProduct ? resolveConsultProductId() : undefined;
    // [zh] 声明变量 `data`
    const data = await agentApi.sendMessage(text, fromProduct, consultProductId);
    // [zh] 条件判断 if
    if (!data?.messageId) {
      // [zh] 语句
      toast.error('发送失败，请重试');
      // [zh] return 返回
      return false;
    // [zh] 语法符号
    }
    // [zh] 语句
    messageId.value = data.messageId;
    // [zh] 语句
    data.assistantMessage = '';
    // [zh] 语句
    answering.value = true;
    // [zh] 语句
    mitter.emit('sendMessage', { ...data });
    // [zh] return 返回
    return true;
  // [zh] 代码块开始
  } catch (e: any) {
    // [zh] 条件判断 if
    if (e?.info === 'AI购物体验已经结束') {
      // [zh] 语句
      showTopAlert('AI购物体验已经结束');
      // [zh] 语句
      answering.value = false;
      // [zh] return 返回
      return false;
    // [zh] 语法符号
    }
    // [zh] 语句
    toast.error(e?.info || '发送失败，请重试');
    // [zh] return 返回
    return false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `sendMessage`
const sendMessage = async () => {
  // [zh] 声明变量 `text`
  const text = input.value.trim();
  // [zh] 条件判断 if
  if (!text || answering.value) return;
  // [zh] 声明变量 `ok`
  const ok = await dispatchSend(text);
  // [zh] 条件判断 if
  if (ok) {
    // [zh] 语句
    input.value = '';
    // [zh] 语句
    adjustTextareaHeight();
    // [zh] 条件判断 if
    if (!isDesktop.value) {
      // [zh] 语句
      textareaRef.value?.blur();
      // [zh] 语句
      recoverIosViewportZoom();
    // [zh] 代码块开始
    } else {
      // [zh] 语句
      textareaRef.value?.focus();
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `sendProductConsult`
const sendProductConsult = async () => {
  // [zh] 声明变量 `product`
  const product = pendingProduct.value;
  // [zh] 条件判断 if
  if (!product || answering.value) return;
  // [zh] 声明变量 `text`
  const text = buildProductConsultMessage(product);
  // [zh] 声明变量 `ok`
  const ok = await dispatchSend(text);
  // [zh] 条件判断 if
  if (ok) {
    // [zh] 语句
    pendingProduct.value = null;
    // [zh] 语句
    clearAgentConsultProduct(currentUserId());
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `dismissProductConsult`
const dismissProductConsult = async () => {
  // [zh] 语句
  pendingProduct.value = null;
  // [zh] 语句
  clearAgentConsultProduct(currentUserId());
  // [zh] 语句
  clearAgentConsultProduct();
  // [zh] 代码块开始
  try {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await agentApi.clearProductConsult();
  // [zh] 代码块开始
  } catch {

  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `stop`
const stop = async () => {
  // [zh] 声明变量 `id`
  const id = messageId.value;
  // [zh] 条件判断 if
  if (id == null) {
    // [zh] 语句
    toast.error('无法取消，请刷新页面后重试');
    // [zh] 语句
    return;
  // [zh] 语法符号
  }

  // [zh] 语句
  mitter.emit('cancelMessage', { messageId: id });
  // [zh] 语句
  answering.value = false;
  // [zh] 语句
  messageId.value = null;
// [zh] 语句
};

// [zh] 声明变量 `onAnswering`
const onAnswering = (val: unknown) => {
  // [zh] 条件判断 if
  if (typeof val === 'boolean') {
    // [zh] 条件判断 if
    if (val) {
      // [zh] 语句
      answering.value = true;
    // [zh] 代码块开始
    } else {
      // [zh] 语句
      resetComposerIdle();
    // [zh] 语法符号
    }
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `payload`
  const payload = val as { answering?: boolean; messageId?: number };
  // [zh] 条件判断 if
  if (payload.answering) {
    // [zh] 语句
    answering.value = true;
    // [zh] 条件判断 if
    if (payload.messageId != null) messageId.value = payload.messageId;
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    resetComposerIdle();
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 代码块开始
watch(input, () => {
  // [zh] 语句
  adjustTextareaHeight();
// [zh] 语句
});

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => agentMessageStore.message,`
  () => agentMessageStore.message,
  // [zh] 代码块开始
  (msg) => {
    // [zh] 条件判断 if
    if (!msg) return;
    // [zh] 声明变量 `outputType`
    const outputType = Number(msg.outPutType);
    // [zh] 条件判断 if
    if (outputType === AGENT_OUTPUT_TYPE.DONE || outputType === AGENT_OUTPUT_TYPE.ERROR) {
      // [zh] 语句
      resetComposerIdle();
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
// [zh] 语句
);

// [zh] 声明变量 `onSendMessageSync`
const onSendMessageSync = (payload: unknown) => {
  // [zh] 声明变量 `msg`
  const msg = payload as { messageId?: number };
  // [zh] 条件判断 if
  if (msg?.messageId != null) messageId.value = msg.messageId;
// [zh] 语句
};

// [zh] 代码块开始
onMounted(() => {
  // [zh] 语句
  initPendingConsultProduct();
  // [zh] 语句
  void syncConsultResumeState();

  // [zh] 声明变量 `fromProductEntry`
  const fromProductEntry = route.query.fromProduct === '1';
  // [zh] 条件判断 if
  if (!pendingProduct.value && !fromProductEntry && authStore.isLoggedIn) {
    // [zh] 语句
    void agentApi.pauseProductConsult().catch(() => {});
  // [zh] 语法符号
  }
  // [zh] 语句
  mitter.on('answering', onAnswering);
  // [zh] 语句
  mitter.on('sendMessage', onSendMessageSync);
  // [zh] 语句
  syncComposerInset();
  // [zh] 语句
  composerResizeObserver = new ResizeObserver(syncComposerInset);
  // [zh] 条件判断 if
  if (composerRef.value) composerResizeObserver.observe(composerRef.value);
  // [zh] 语句
  adjustTextareaHeight();

  // [zh] 代码块开始
  nextTick(() => {
    // [zh] 语句
    checkScrollable();
    // [zh] 声明变量 `scrollEl`
    const scrollEl = tipsScrollRef.value;
    // [zh] 条件判断 if
    if (scrollEl) {
      // [zh] 声明变量 `ro`
      const ro = new ResizeObserver(() => { checkScrollable(); });
      // [zh] 语句
      ro.observe(scrollEl);
    // [zh] 语法符号
    }
  // [zh] 语句
  });

  // [zh] 声明变量 `presetMsg`
  const presetMsg = route.query.msg as string | undefined;
  // [zh] 条件判断 if
  if (presetMsg) {
    // [zh] 语句
    router.replace({ query: { ...route.query, msg: undefined } });
    // [zh] 代码块开始
    setTimeout(() => {
      // [zh] 条件判断 if
      if (!answering.value) {
        // [zh] 语句
        input.value = presetMsg;
        // [zh] 语句
        adjustTextareaHeight();
        // [zh] 语句
        sendMessage();
      // [zh] 语法符号
      }
    // [zh] 语句
    }, 400);
  // [zh] 语法符号
  }
// [zh] 语句
});

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 语句
  recoverIosViewportZoom();
  // [zh] 语句
  mitter.off('answering', onAnswering);
  // [zh] 语句
  mitter.off('sendMessage', onSendMessageSync);
  // [zh] 语句
  teardownComposerInset();
  // [zh] 语句
  composerInsetReadyEmitted = false;
// [zh] 语句
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
