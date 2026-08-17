<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
    <!-- [zh] 开始标签 `<Transition>` -->
    <Transition name="pc-agent-fade">
      <div
        v-if="pcAgentPanel.visible"
        class="pc-agent-float-root ignore"
        role="dialog"
        aria-label="智能客服"
        @click.self="pcAgentPanel.close()"
      >
        <section class="pc-agent-float-panel agent-page" @click.stop>
          <header class="pc-agent-float-head">
            <div class="head-title">
              <el-icon class="head-icon" :size="18"><ChatDotRound /></el-icon>
              <span>智能客服</span>
            </div>
            <button type="button" class="btn-close" aria-label="关闭" @click="pcAgentPanel.close()">
              <el-icon :size="18"><Close /></el-icon>
            </button>
          </header>
          <div v-if="sessionReady" class="pc-agent-float-body">
            <AgentChatList />
            <AgentSendPanel />
          </div>
          <div v-else class="pc-agent-float-loading" v-loading="true" />
        </section>
      </div>
    </Transition>
  <!-- [zh] 闭合标签 `</Teleport>` -->
  </Teleport>
</template>

<script setup lang="ts">
import { provide, ref, watch } from 'vue';
import { agentComposerEmbeddedKey } from '@/composables/agentEmbed';
import { ChatDotRound, Close } from '@element-plus/icons-vue';
import AgentChatList from '@/views/agent/AgentChatList.vue';
import AgentSendPanel from '@/views/agent/AgentSendPanel.vue';
import { useAgentSession } from '@/composables/useAgentSession';
import { usePcAgentPanelStore } from '@/stores/pcAgentPanel';

provide(agentComposerEmbeddedKey, true);

const pcAgentPanel = usePcAgentPanelStore();
const { start, stop } = useAgentSession();
const sessionReady = ref(false);

watch(
  () => pcAgentPanel.visible,
  async (open) => {
    if (open) {
      sessionReady.value = false;
      await start();
      sessionReady.value = true;
    } else {
      sessionReady.value = false;
      stop();
    }
  }
);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-agent-float-root {` */
.pc-agent-float-root {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `z-index: $z-index-float-agent;` */
  z-index: $z-index-float-agent;
  /* [zh] 样式规则 `background: rgba(16, 24, 40, 0.28);` */
  background: rgba(16, 24, 40, 0.28);
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `padding: 24px;` */
  padding: 24px;
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-agent-float-panel {` */
.pc-agent-float-panel {
  /* [zh] 样式规则 `flex: 0 0 auto;` */
  flex: 0 0 auto;
  /* [zh] 样式规则 `align-self: center;` */
  align-self: center;
  /* [zh] 样式规则 `width: min(420px, calc(100vw - 48px));` */
  width: min(420px, calc(100vw - 48px));
  /* [zh] 样式规则 `height: min(680px, calc(100vh - 80px));` */
  height: min(680px, calc(100vh - 80px));
  /* [zh] 样式规则 `min-height: 520px;` */
  min-height: 520px;
  /* [zh] 样式规则 `max-width: 420px;` */
  max-width: 420px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `box-shadow: 0 12px 40px rgba(16, 24, 40,` */
  box-shadow: 0 12px 40px rgba(16, 24, 40, 0.18);
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
  border: 1px solid $color-border-gray;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-agent-float-head {` */
.pc-agent-float-head {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border-g` */
  border-bottom: 1px solid $color-border-gray;
  /* [zh] 样式规则 `background: linear-gradient(90deg, rgba(` */
  background: linear-gradient(90deg, rgba($color-primary, 0.08), transparent);

  /* [zh] 样式规则 `.head-title {` */
  .head-title {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.head-icon {` */
  .head-icon {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.btn-close {` */
  .btn-close {
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `place-items: center;` */
    place-items: center;
    /* [zh] 样式规则 `width: 32px;` */
    width: 32px;
    /* [zh] 样式规则 `height: 32px;` */
    height: 32px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: $color-bg-subtle;` */
      background: $color-bg-subtle;
      /* [zh] 样式规则 `color: $color-text-title;` */
      color: $color-text-title;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-agent-float-body {` */
.pc-agent-float-body {
  /* [zh] 样式规则 `flex: 1 1 0;` */
  flex: 1 1 0;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `:deep(.chat-scroll) {` */
  :deep(.chat-scroll) {
    /* [zh] 样式规则 `flex: 1 1 0;` */
    flex: 1 1 0;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `padding-bottom: 12px !important;` */
    padding-bottom: 12px !important;
    /* [zh] 样式规则 `background: #fafafa;` */
    background: #fafafa;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.agent-composer-stack) {` */
  :deep(.agent-composer-stack) {
    /* [zh] 样式规则 `position: static !important;` */
    position: static !important;
    /* [zh] 样式规则 `left: auto !important;` */
    left: auto !important;
    /* [zh] 样式规则 `right: auto !important;` */
    right: auto !important;
    /* [zh] 样式规则 `bottom: auto !important;` */
    bottom: auto !important;
    /* [zh] 样式规则 `z-index: 1 !important;` */
    z-index: 1 !important;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `width: 100% !important;` */
    width: 100% !important;
    /* [zh] 样式规则 `max-width: 100% !important;` */
    max-width: 100% !important;
    /* [zh] 样式规则 `margin: 0 !important;` */
    margin: 0 !important;
    /* [zh] 样式规则 `padding: 0 !important;` */
    padding: 0 !important;
    /* [zh] 样式规则 `pointer-events: auto !important;` */
    pointer-events: auto !important;
    /* [zh] 样式规则 `gap: 0 !important;` */
    gap: 0 !important;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.consult-product-float) {` */
  :deep(.consult-product-float) {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `border-radius: 0;` */
    border-radius: 0;
    /* [zh] 样式规则 `border-bottom: 1px solid $color-border-l` */
    border-bottom: 1px solid $color-border-light;
    /* [zh] 样式规则 `box-shadow: none;` */
    box-shadow: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.agent-composer-dock) {` */
  :deep(.agent-composer-dock) {
    /* [zh] 样式规则 `border-top: 1px solid $color-border-gray` */
    border-top: 1px solid $color-border-gray;
    /* [zh] 样式规则 `box-shadow: none;` */
    box-shadow: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.quick-tips) {` */
  :deep(.quick-tips) {
    /* [zh] 样式规则 `padding: 8px 10px;` */
    padding: 8px 10px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.chat-input-bar) {` */
  :deep(.chat-input-bar) {
    /* [zh] 样式规则 `padding: 8px 10px;` */
    padding: 8px 10px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.agent-products) {` */
  :deep(.agent-products) {
    /* [zh] 样式规则 `grid-template-columns: repeat(2, minmax(` */
    grid-template-columns: repeat(2, minmax(0, 1fr));
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `max-height: 220px;` */
    max-height: 220px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.product-link) {` */
  :deep(.product-link) {
    /* [zh] 样式规则 `padding: 6px;` */
    padding: 6px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.product-link .product-image) {` */
  :deep(.product-link .product-image) {
    /* [zh] 样式规则 `width: 52px !important;` */
    width: 52px !important;
    /* [zh] 样式规则 `height: 52px !important;` */
    height: 52px !important;
    /* [zh] 样式规则 `margin: 0 auto;` */
    margin: 0 auto;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.consult-product-card .cover-wrap)` */
  :deep(.consult-product-card .cover-wrap) {
    /* [zh] 样式规则 `flex: 0 0 48px;` */
    flex: 0 0 48px;
    /* [zh] 样式规则 `width: 48px;` */
    width: 48px;
    /* [zh] 样式规则 `height: 48px;` */
    height: 48px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-agent-float-loading {` */
.pc-agent-float-loading {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-height: 200px;` */
  min-height: 200px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.pc-agent-fade-enter-active,
/* [zh] 样式规则 `.pc-agent-fade-leave-active {` */
.pc-agent-fade-leave-active {
  /* [zh] 样式规则 `transition: opacity 0.2s ease;` */
  transition: opacity 0.2s ease;

  /* [zh] 样式规则 `.pc-agent-float-panel {` */
  .pc-agent-float-panel {
    /* [zh] 样式规则 `transition: transform 0.22s ease, opacit` */
    transition: transform 0.22s ease, opacity 0.2s ease;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.pc-agent-fade-enter-from,
/* [zh] 样式规则 `.pc-agent-fade-leave-to {` */
.pc-agent-fade-leave-to {
  /* [zh] 样式规则 `opacity: 0;` */
  opacity: 0;

  /* [zh] 样式规则 `.pc-agent-float-panel {` */
  .pc-agent-float-panel {
    /* [zh] 样式规则 `transform: scale(0.96);` */
    transform: scale(0.96);
    /* [zh] 样式规则 `opacity: 0;` */
    opacity: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
