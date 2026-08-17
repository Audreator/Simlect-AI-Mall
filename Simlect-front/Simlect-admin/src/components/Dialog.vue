<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-dialog>` -->
  <el-dialog
    :show-close="showClose"
    :draggable="draggable"
    :model-value="show"
    :close-on-click-modal="false"
    class="cust-dialog"
    :top="top + 'px'"
    :width="width"
    @close="close"
  >
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #header="{ close, titleId, titleClass }">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="title" class="title">{{ title }}</div>
      <!-- [zh] 开始标签 `<slot>` -->
      <slot v-else name="header"></slot>
    </template>
    <div
      class="dialog-body"
      :style="{ 'max-height': maxHeight + 'px', padding: padding + 'px' }"
    >
      <slot></slot>
    </div>
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template v-if="(buttons && buttons.length > 0) || showCancel">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="dialog-footer">
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button link @click="close" v-if="showCancel"> 取消 </el-button>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button
          v-for="btn in buttons"
          :type="btn.type || 'primary'"
          @click="btn.click"
        >
          <!-- [zh] Mustache 插值表达式 -->
          {{ btn.text }}
        <!-- [zh] 闭合标签 `</el-button>` -->
        </el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
const props = defineProps({
  draggable: {
    type: Boolean,
    default: true,
  },
  title: {
    type: String,
  },
  show: {
    type: Boolean,
    default: false,
  },
  showClose: {
    type: Boolean,
    default: true,
  },
  showCancel: {
    type: Boolean,
    default: true,
  },
  top: {
    type: Number,
    default: 50,
  },
  width: {
    type: String,
    default: "30%",
  },
  buttons: {
    type: Array,
  },
  padding: {
    type: Number,
    default: 15,
  },
});

const maxHeight =
  window.innerHeight -
  props.top -
  (!props.buttons || props.buttons.length == 0 ? 70 : 120);

const emit = defineEmits();
const close = () => {
  emit("close");
};
</script>

/* [zh] 样式声明 */
<style lang="scss">
/* [zh] 样式规则 `.cust-dialog {` */
.cust-dialog {
  /* [zh] 样式规则 `padding: 0px !important;` */
  padding: 0px !important;
  /* [zh] 样式规则 `margin-bottom: 5px !important;` */
  margin-bottom: 5px !important;
  /* [zh] 样式规则 `.el-dialog__header {` */
  .el-dialog__header {
    /* [zh] 样式规则 `padding: 16px;` */
    padding: 16px;
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `.title {` */
  .title {
    /* [zh] 样式规则 `font-size: 20px;` */
    font-size: 20px;
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `.dialog-body {` */
  .dialog-body {
    /* [zh] 样式规则 `min-height: 80px;` */
    min-height: 80px;
    /* [zh] 样式规则 `overflow: auto;` */
    overflow: auto;
    /* [zh] 样式规则 `overflow-x: hidden;` */
    overflow-x: hidden;
    /* [zh] 样式规则 `padding: 20px;` */
    padding: 20px;
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `.dialog-footer {` */
  .dialog-footer {
    /* [zh] 样式规则 `border-top: 1px solid #ddd;` */
    border-top: 1px solid #ddd;
    /* [zh] 样式规则 `text-align: right;` */
    text-align: right;
    /* [zh] 样式规则 `padding: 5px 20px;` */
    padding: 5px 20px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>