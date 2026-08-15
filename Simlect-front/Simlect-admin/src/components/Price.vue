<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="price">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="tag" :style="{ 'font-size': `${size * 0.8}px` }">￥</div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="value" :style="{ 'font-size': `${size}px` }">{{ proxy.Utils.convert2Amount(price) }}</div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
// [zh] 导入 `vue-router`
import { useRouter, useRoute } from 'vue-router'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()
// [zh] 声明变量 `route`
const route = useRoute()

// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  price: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  size: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 14,`
    default: 14,
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.price {` */
.price {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `color: #ff0f23;` */
  color: #ff0f23;
  /* [zh] 样式规则 `font-weight: bold;` */
  font-weight: bold;
  /* [zh] 样式规则 `align-items: flex-end;` */
  align-items: flex-end;
/* [zh] 样式规则 `}` */
}
</style>
