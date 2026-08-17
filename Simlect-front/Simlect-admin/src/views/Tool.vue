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

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from "vue"
const { proxy } = getCurrentInstance();
const dialogConfig = ref({
  show: false,
  title: "小工具",
});

const show = () => {
  dialogConfig.value.show = true;
}
defineExpose({
  show
})

const API_MAP = {
  1: "toolStatistics",
  2: "toolProductData",
  3: "toolRagData",
  4: "toolAddAllOrderToDelayQueue",
}
const buttonClick = async (type) => {
  let result = await proxy.Request({
    url: proxy.Api[API_MAP[type]],
  })
  if (!result) {
    return;
  }
  proxy.Message.success("操作成功");
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
