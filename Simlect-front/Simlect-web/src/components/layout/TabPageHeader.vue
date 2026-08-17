<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<LiquidGlassSurface>` -->
  <LiquidGlassSurface tag="header" intensity="medium" class="tab-page-header ignore">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="side side-left">
      <!-- [zh] 开始标签 `<slot>` -->
      <slot name="left" />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="center">
      <!-- [zh] 开始标签 `<h1>` -->
      <h1 class="title">{{ title }}</h1>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="subtitle" class="subtitle">{{ subtitle }}</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="side side-right">
      <!-- [zh] 开始标签 `<slot>` -->
      <slot name="right" />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</LiquidGlassSurface>` -->
  </LiquidGlassSurface>
</template>

<script setup lang="ts">
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';

defineProps<{
  title: string;
  subtitle?: string;
}>();
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.tab-page-header {` */
.tab-page-header {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `top: 0;` */
  top: 0;
  /* [zh] 样式规则 `left: 0;` */
  left: 0;
  /* [zh] 样式规则 `right: 0;` */
  right: 0;
  /* [zh] 样式规则 `z-index: 1000;` */
  z-index: 1000;
  /* [zh] 样式规则 `height: $tab-header-height;` */
  height: $tab-header-height;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `padding: 0 $app-page-gutter;` */
  padding: 0 $app-page-gutter;
  /* [zh] 样式规则 `padding-top: env(safe-area-inset-top, 0)` */
  padding-top: env(safe-area-inset-top, 0);
  /* [zh] 样式规则 `min-height: calc($tab-header-height + en` */
  min-height: calc($tab-header-height + env(safe-area-inset-top, 0));
  /* [zh] 样式规则 `border-bottom: 1px solid var(--glass-bor` */
  border-bottom: 1px solid var(--glass-border-soft);
  /* [zh] 样式规则 `box-shadow: var(--glass-shadow-sm);` */
  box-shadow: var(--glass-shadow-sm);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tab-page-header :deep(.liquid-glass-sur` */
.tab-page-header :deep(.liquid-glass-surface__content) {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: 44px 1fr 44px;` */
  grid-template-columns: 44px 1fr 44px;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.side {` */
.side {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `min-height: 44px;` */
  min-height: 44px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.side-right {` */
.side-right {
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.center {` */
.center {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: baseline;` */
  align-items: baseline;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `padding: 0 4px;` */
  padding: 0 4px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.title {` */
.title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 17px;` */
  font-size: 17px;
  /* [zh] 样式规则 `font-weight: 700;` */
  font-weight: 700;
  /* [zh] 样式规则 `letter-spacing: -0.03em;` */
  letter-spacing: -0.03em;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.subtitle {` */
.subtitle {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}
</style>
