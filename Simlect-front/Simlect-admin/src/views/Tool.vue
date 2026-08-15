<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Dialog>` -->
  <Dialog :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" width="440px"
    :showCancel="false" @close="dialogConfig.show = false">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="tool-buttons">
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button @click="buttonClick(1)" type="primary">同步统计数据</el-button>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button @click="buttonClick(2)" type="primary">同步商品数据</el-button>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button @click="buttonClick(3)" type="primary">同步RAG数据</el-button>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button @click="buttonClick(4)" type="primary">加入延时队列</el-button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</Dialog>` -->
  </Dialog>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick } from "vue"
// [zh] 语句
const { proxy } = getCurrentInstance();
// [zh] 声明变量 `dialogConfig`
const dialogConfig = ref({
  // [zh] TypeScript/JS 语句：`show: false,`
  show: false,
  // [zh] TypeScript/JS 语句：`title: "小工具",`
  title: "小工具",
// [zh] 语句
});

// [zh] 声明变量 `show`
const show = () => {
  // [zh] 语句
  dialogConfig.value.show = true;
// [zh] 语法符号
}
// [zh] 代码块开始
defineExpose({
  // [zh] TypeScript/JS 语句：`show`
  show
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `API_MAP`
const API_MAP = {
  // [zh] TypeScript/JS 语句：`1: "toolStatistics",`
  1: "toolStatistics",
  // [zh] TypeScript/JS 语句：`2: "toolProductData",`
  2: "toolProductData",
  // [zh] TypeScript/JS 语句：`3: "toolRagData",`
  3: "toolRagData",
  // [zh] TypeScript/JS 语句：`4: "toolAddAllOrderToDelayQueue",`
  4: "toolAddAllOrderToDelayQueue",
// [zh] 语法符号
}
// [zh] 声明变量 `buttonClick`
const buttonClick = async (type) => {
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api[API_MAP[type]],`
    url: proxy.Api[API_MAP[type]],
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) {
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  proxy.Message.success("操作成功");
// [zh] 语句
};
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.tool-buttons {` */
.tool-buttons {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;

  /* [zh] 样式规则 `.el-button {` */
  .el-button {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: calc(50% - 5px);` */
    min-width: calc(50% - 5px);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
