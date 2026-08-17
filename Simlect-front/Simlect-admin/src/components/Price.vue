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

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const props = defineProps({
  price: {
    type: Number,
  },
  size: {
    type: Number,
    default: 14,
  },
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
