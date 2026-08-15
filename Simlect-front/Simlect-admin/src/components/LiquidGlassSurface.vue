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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { computed, onMounted, ref, useAttrs, watch } from 'vue'
// [zh] 导入 `@/utils/liquidGlassFilters.js`
import { applyLiquidGlassEffect } from '@/utils/liquidGlassFilters.js'

// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] TypeScript/JS 语句：`tag: { type: String, default: 'div' },`
  tag: { type: String, default: 'div' },
  // [zh] TypeScript/JS 语句：`intensity: { type: String, default: 'medium' },`
  intensity: { type: String, default: 'medium' },
  // [zh] TypeScript/JS 语句：`variant: { type: String, default: 'default' }`
  variant: { type: String, default: 'default' }
// [zh] TypeScript/JS 语句：`})`
})

// [zh] TypeScript/JS 语句：`defineOptions({ inheritAttrs: false })`
defineOptions({ inheritAttrs: false })

// [zh] 声明变量 `attrs`
const attrs = useAttrs()
// [zh] 声明变量 `rootRef`
const rootRef = ref(null)

// [zh] 声明变量 `passthroughAttrs`
const passthroughAttrs = computed(() => {
  // [zh] TypeScript/JS 语句：`const { class: _class, ...rest } = attrs`
  const { class: _class, ...rest } = attrs
  // [zh] return 返回
  return rest
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 函数 `resolveRootEl`
function resolveRootEl() {
  // [zh] 声明变量 `raw`
  const raw = rootRef.value
  // [zh] 条件判断 if
  if (!raw) return null
  // [zh] 条件判断 if
  if (raw instanceof HTMLElement) return raw
  // [zh] return 返回
  return raw.$el ?? null
// [zh] 语法符号
}

// [zh] 函数 `syncEffectStyles`
function syncEffectStyles() {
  // [zh] 声明变量 `effect`
  const effect = resolveRootEl()?.querySelector('.liquid-glass-surface__effect')
  // [zh] 条件判断 if
  if (!effect) return
  // [zh] TypeScript/JS 语句：`applyLiquidGlassEffect(effect, props.intensity, props.v`
  applyLiquidGlassEffect(effect, props.intensity, props.variant === 'active')
// [zh] 语法符号
}

// [zh] TypeScript/JS 语句：`onMounted(syncEffectStyles)`
onMounted(syncEffectStyles)
// [zh] TypeScript/JS 语句：`watch(() => [props.intensity, props.variant], syncEffec`
watch(() => [props.intensity, props.variant], syncEffectStyles)
</script>
