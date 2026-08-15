<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div
    class="coupon-order-cover"
    :style="{
      width: width ? `${width}px` : '100%',
      borderRadius,
      aspectRatio: scale,
    }"
  >
    <!-- [zh] 开始标签 `<el-icon>` -->
    <el-icon class="coupon-icon" :size="iconSize"><Ticket /></el-icon>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { computed } from 'vue'
// [zh] 导入 `@element-plus/icons-vue`
import { Ticket } from '@element-plus/icons-vue'

// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  width: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 80,`
    default: 80,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  scale: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 1,`
    default: 1,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  borderRadius: {
    // [zh] TypeScript/JS 语句：`type: String,`
    type: String,
    // [zh] TypeScript/JS 语句：`default: '5px',`
    default: '5px',
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `iconSize`
const iconSize = computed(() => {
  // [zh] 声明变量 `w`
  const w = props.width || 80
  // [zh] 条件判断 if
  if (w <= 52) return 26
  // [zh] 条件判断 if
  if (w <= 76) return 32
  // [zh] return 返回
  return Math.round(w * 0.4)
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.coupon-order-cover {` */
.coupon-order-cover {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: linear-gradient(135deg, rgba` */
  background: linear-gradient(135deg, rgba(29, 29, 31, 0.12), rgba(212, 162, 78, 0.1));

  /* [zh] 样式规则 `.coupon-icon {` */
  .coupon-icon {
    /* [zh] 样式规则 `color: rgba(29, 29, 31, 0.45);` */
    color: rgba(29, 29, 31, 0.45);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
