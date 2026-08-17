<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="bubble-row user" :class="{ 'is-product-card': parsed.card }">
    <!-- [zh] 开始标签 `<AgentConsultProductCard>` -->
    <AgentConsultProductCard
      v-if="parsed.card"
      :product="parsed.card"
      variant="bubble"
    />
    <!-- [zh] 开始标签 `<div>` -->
    <div v-else class="bubble user-bubble">
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="parsed.text" class="text">{{ parsed.text }}</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import AgentConsultProductCard from '@/components/agent/AgentConsultProductCard.vue';
import { parseProductConsultMessage } from '@/utils/agentProductConsult';

const props = defineProps<{
  userMessage?: string | null;
}>();

const parsed = computed(() => parseProductConsultMessage(props.userMessage));
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.bubble-row.user {` */
.bubble-row.user {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;
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
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.bubble-row.user.is-product-card {` */
.bubble-row.user.is-product-card {
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.user-bubble {` */
.user-bubble {
  /* [zh] 样式规则 `background: $color-primary;` */
  background: $color-primary;
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `border-bottom-right-radius: 4px;` */
  border-bottom-right-radius: 4px;

  /* [zh] 样式规则 `.text {` */
  .text {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `white-space: pre-wrap;` */
    white-space: pre-wrap;
    /* [zh] 样式规则 `word-break: break-word;` */
    word-break: break-word;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
