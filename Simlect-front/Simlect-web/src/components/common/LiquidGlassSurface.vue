<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<component>` -->
  <component
    :is="tag"
    ref="rootRef"
    class="liquid-glass-surface"
    :class="[
      `liquid-glass-surface--${intensity}`,
      variant !== 'default' ? `liquid-glass-surface--${variant}` : null,
      attrs.class
    ]"
    v-bind="passthroughAttrs"
  >
    <!-- [zh] 开始标签 `<div>` -->
    <div class="liquid-glass-surface__effect" aria-hidden="true" />
    <!-- [zh] 开始标签 `<div>` -->
    <div class="liquid-glass-surface__tint" aria-hidden="true" />
    <!-- [zh] 开始标签 `<div>` -->
    <div class="liquid-glass-surface__shine" aria-hidden="true" />
    <!-- [zh] 开始标签 `<div>` -->
    <div class="liquid-glass-surface__content">
      <!-- [zh] 开始标签 `<slot>` -->
      <slot />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</component>` -->
  </component>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted, ref, useAttrs, watch } from 'vue';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`applyLiquidGlassEffect,`
  applyLiquidGlassEffect,
  // [zh] TypeScript 类型 `LiquidGlassIntensity`（类比 Java interface/DTO）
  type LiquidGlassIntensity
// [zh] 语句
} from '@/utils/liquidGlassFilters';

// [zh] 导出模块成员
export type { LiquidGlassIntensity };
// [zh] 导出模块成员
export type LiquidGlassVariant = 'default' | 'active';

// [zh] 声明变量 `props`
const props = withDefaults(
  // [zh] 代码块开始
  defineProps<{
    // [zh] 语句
    tag?: string;
    // [zh] 语句
    intensity?: LiquidGlassIntensity;
    // [zh] 语句
    variant?: LiquidGlassVariant;
  // [zh] TypeScript/JS 语句：`}>(),`
  }>(),
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`tag: 'div',`
    tag: 'div',
    // [zh] TypeScript/JS 语句：`intensity: 'medium',`
    intensity: 'medium',
    // [zh] TypeScript/JS 语句：`variant: 'default'`
    variant: 'default'
  // [zh] 语法符号
  }
// [zh] 语句
);

// [zh] 语句
defineOptions({ inheritAttrs: false });

// [zh] 声明变量 `attrs`
const attrs = useAttrs();
// [zh] 声明变量 `rootRef`
const rootRef = ref<HTMLElement | null>(null);

// [zh] 声明变量 `passthroughAttrs`
const passthroughAttrs = computed(() => {
  // [zh] 语句
  const { class: _class, ...rest } = attrs;
  // [zh] return 返回
  return rest;
// [zh] 语句
});

// [zh] 函数 `resolveRootEl`
function resolveRootEl(): HTMLElement | null {
  // [zh] 声明变量 `raw`
  const raw = rootRef.value as HTMLElement | { $el?: HTMLElement } | null;
  // [zh] 条件判断 if
  if (!raw) return null;
  // [zh] 条件判断 if
  if (raw instanceof HTMLElement) return raw;
  // [zh] return 返回
  return raw.$el ?? null;
// [zh] 语法符号
}

// [zh] 函数 `syncEffectStyles`
function syncEffectStyles() {
  // [zh] 声明变量 `effect`
  const effect = resolveRootEl()?.querySelector<HTMLElement>('.liquid-glass-surface__effect');
  // [zh] 条件判断 if
  if (!effect) return;
  // [zh] 语句
  applyLiquidGlassEffect(effect, props.intensity, props.variant === 'active');
// [zh] 语法符号
}

// [zh] 语句
onMounted(syncEffectStyles);
// [zh] 语句
watch(() => [props.intensity, props.variant], syncEffectStyles);
</script>
