<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-drawer>` -->
  <el-drawer :size="width" :model-value="show" header-class="cust-drawer-header" body-class="cust-drawer-body"
    footer-class="cust-drawer-footer">
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #header>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="title">{{ title }}</div>
    </template>
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #default>
      <!-- [zh] 开始标签 `<slot>` -->
      <slot></slot>
    </template>
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #footer v-if="(buttons && buttons.length > 0) || showCancel">
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button link @click="close" v-if="showCancel"> 取消 </el-button>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button v-for="btn in buttons" :type="btn.type || 'primary'" @click="btn.click">
        <!-- [zh] Mustache 插值表达式 -->
        {{ btn.text }}
      <!-- [zh] 闭合标签 `</el-button>` -->
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup>
const props = defineProps({
  title: {
    type: String,
  },
  show: {
    type: Boolean,
    default: false,
  },
  showCancel: {
    type: Boolean,
    default: true,
  },
  width: {
    type: String,
    default: '50%',
  },
  buttons: {
    type: Array,
  },
})

const emit = defineEmits()
const close = () => {
  emit('close')
}
</script>

/* [zh] 样式声明 */
<style lang="scss">
/* [zh] 样式规则 `.cust-drawer-header {` */
.cust-drawer-header {
  /* [zh] 样式规则 `margin-bottom: 0px;` */
  margin-bottom: 0px;
  /* [zh] 样式规则 `padding: 5px 10px;` */
  padding: 5px 10px;

  /* [zh] 样式规则 `.title {` */
  .title {
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: bold;` */
    font-weight: bold;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cust-drawer-body {` */
.cust-drawer-body {
  /* [zh] 样式规则 `padding: 5px 15px 15px 15px;` */
  padding: 5px 15px 15px 15px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cust-drawer-footer {` */
.cust-drawer-footer {
  /* [zh] 样式规则 `padding: 10px;` */
  padding: 10px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}
</style>