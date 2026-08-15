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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  draggable: {
    // [zh] TypeScript/JS 语句：`type: Boolean,`
    type: Boolean,
    // [zh] TypeScript/JS 语句：`default: true,`
    default: true,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  title: {
    // [zh] TypeScript/JS 语句：`type: String,`
    type: String,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  show: {
    // [zh] TypeScript/JS 语句：`type: Boolean,`
    type: Boolean,
    // [zh] TypeScript/JS 语句：`default: false,`
    default: false,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  showClose: {
    // [zh] TypeScript/JS 语句：`type: Boolean,`
    type: Boolean,
    // [zh] TypeScript/JS 语句：`default: true,`
    default: true,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  showCancel: {
    // [zh] TypeScript/JS 语句：`type: Boolean,`
    type: Boolean,
    // [zh] TypeScript/JS 语句：`default: true,`
    default: true,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  top: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 50,`
    default: 50,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  width: {
    // [zh] TypeScript/JS 语句：`type: String,`
    type: String,
    // [zh] TypeScript/JS 语句：`default: "30%",`
    default: "30%",
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  buttons: {
    // [zh] TypeScript/JS 语句：`type: Array,`
    type: Array,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  padding: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 15,`
    default: 15,
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] 语句
});

// [zh] 声明变量 `maxHeight`
const maxHeight =
  // [zh] TypeScript/JS 语句：`window.innerHeight -`
  window.innerHeight -
  // [zh] TypeScript/JS 语句：`props.top -`
  props.top -
  // [zh] 语句
  (!props.buttons || props.buttons.length == 0 ? 70 : 120);

// [zh] 声明变量 `emit`
const emit = defineEmits();
// [zh] 声明变量 `close`
const close = () => {
  // [zh] 语句
  emit("close");
// [zh] 语句
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