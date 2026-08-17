<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="home-dashboard">
    <!-- [zh] 开始标签 `<TodayData>` -->
    <TodayData />
    <!-- [zh] 开始标签 `<WeeklyData>` -->
    <WeeklyData />
    <!-- [zh] 开始标签 `<SaleData>` -->
    <SaleData />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup>
import SaleData from './SaleData.vue'
import TodayData from './TodayData.vue'
import WeeklyData from './WeeklyData.vue'
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.home-dashboard {` */
.home-dashboard {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 0;` */
  gap: 0;
  /* [zh] 样式规则 `max-width: 100%;` */
  max-width: 100%;
/* [zh] 样式规则 `}` */
}
</style>
