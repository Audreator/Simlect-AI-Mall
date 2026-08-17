<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<button>` -->
  <button
    type="button"
    class="agent-service-entry"
    :class="{ compact }"
    aria-label="智能客服"
    @click="goAgent"
  >
    <!-- [zh] 开始标签 `<el-icon>` -->
    <el-icon :size="iconSize"><ChatDotRound /></el-icon>
    <!-- [zh] 开始标签 `<span>` -->
    <span v-if="showLabel" class="label">客服</span>
  <!-- [zh] 闭合标签 `</button>` -->
  </button>
</template>

<script setup lang="ts">
import { ChatDotRound } from '@element-plus/icons-vue';
import { useOpenAgent } from '@/composables/useOpenAgent';
import type { AgentConsultProduct } from '@/utils/agentProductConsult';

const props = withDefaults(
  defineProps<{
    compact?: boolean;
    showLabel?: boolean;
    iconSize?: number;

    consultProduct?: AgentConsultProduct | null;
  }>(),
  {
    compact: true,
    showLabel: false,
    iconSize: 22,
    consultProduct: null
  }
);

const { openAgent } = useOpenAgent();

const goAgent = () => {
  openAgent({
    consultProduct: props.consultProduct,
    fromProduct: !!props.consultProduct?.productId
  });
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.agent-service-entry {` */
.agent-service-entry {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
  -webkit-tap-highlight-color: transparent;
  /* [zh] 样式规则 `transition: background $transition-fast,` */
  transition: background $transition-fast, color $transition-fast, transform $transition-fast;

  /* [zh] 样式规则 `&.compact {` */
  &.compact {
    /* [zh] 样式规则 `width: 36px;` */
    width: 36px;
    /* [zh] 样式规则 `height: 36px;` */
    height: 36px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:not(.compact) {` */
  &:not(.compact) {
    /* [zh] 样式规则 `min-height: 36px;` */
    min-height: 36px;
    /* [zh] 样式规则 `padding: 0 12px;` */
    padding: 0 12px;
    /* [zh] 样式规则 `border-radius: $radius-pill;` */
    border-radius: $radius-pill;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.label {` */
  .label {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.92);` */
    transform: scale(0.92);
    /* [zh] 样式规则 `background: $color-primary-muted;` */
    background: $color-primary-muted;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
