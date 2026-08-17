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

<script setup>
import { computed } from 'vue'
import { Ticket } from '@element-plus/icons-vue'

const props = defineProps({
  width: {
    type: Number,
    default: 80,
  },
  scale: {
    type: Number,
    default: 1,
  },
  borderRadius: {
    type: String,
    default: '5px',
  },
})

const iconSize = computed(() => {
  const w = props.width || 80
  if (w <= 52) return 26
  if (w <= 76) return 32
  return Math.round(w * 0.4)
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
