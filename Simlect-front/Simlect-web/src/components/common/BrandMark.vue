<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<svg>` -->
  <svg
    class="brand-mark"
    :class="rootClass"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="6 5 20 22"
    role="img"
    aria-label="简选 Simlect"
  >

    <!-- [zh] 开始标签 `<path>` -->
    <path
      fill="none"
      :stroke="markColor"
      :stroke-width="strokeWidth"
      stroke-linecap="round"
      stroke-linejoin="round"
      d="M20.5 8.5c-3.2 0-5.2 1.8-5.2 4.6 0 3.4 5.2 3.2 5.2 7.2 0 2.6-2.2 4.4-5.4 4.4-2.2 0-4-.8-5.1-2.1"
    />
    <!-- [zh] 开始标签 `<path>` -->
    <path
      fill="none"
      :stroke="markColor"
      :stroke-width="strokeWidth"
      stroke-linecap="round"
      d="M11.5 8.5v14.6"
    />
  <!-- [zh] 闭合标签 `</svg>` -->
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    rootClass?: string;
    variant?: 'default' | 'light';
  }>(),
  { variant: 'default' }
);

const markColor = computed(() => (props.variant === 'light' ? '#ffffff' : '#1D1D1F'));
const strokeWidth = 1.85;
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `.brand-mark {` */
.brand-mark {
  /* [zh] 样式规则 `display: block;` */
  display: block;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
/* [zh] 样式规则 `}` */
}
</style>
