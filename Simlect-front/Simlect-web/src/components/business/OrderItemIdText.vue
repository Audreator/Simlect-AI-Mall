<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<p>` -->
  <p v-if="displayId" class="order-item-id" :title="displayId">订单项 ID {{ displayId }}</p>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ id?: string | number | null }>();

const displayId = computed(() => {
  if (props.id == null || props.id === '') return '';
  return String(props.id);
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.order-item-id {` */
.order-item-id {
  /* [zh] 样式规则 `margin: 4px 0 0;` */
  margin: 4px 0 0;
  /* [zh] 样式规则 `font-size: 10px;` */
  font-size: 10px;
  /* [zh] 样式规则 `line-height: 1.35;` */
  line-height: 1.35;
  /* [zh] 样式规则 `font-family: ui-monospace, SFMono-Regula` */
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `word-break: break-all;` */
  word-break: break-all;
/* [zh] 样式规则 `}` */
}
</style>
