<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="logistics-panel">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="formData" :rules="rules" ref="formDataRef" label-width="80px" @submit.prevent>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="发件人" prop="">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input clearable placeholder="请输入发件人" v-model.trim="formData.senderName"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="联系电话" prop="">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input clearable placeholder="请输入发货人联系电话" v-model.trim="formData.senderPhone" :maxlength="11"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="发货地址" prop="">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input type="textarea" clearable placeholder="请输入发货地址" v-model.trim="formData.senderAddress"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>

      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="" prop="">
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button @click="saveSetting" type="primary">保存</el-button>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
    <!-- [zh] 闭合标签 `</el-form>` -->
    </el-form>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>

</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick, onMounted } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()

// [zh] 声明变量 `formData`
const formData = ref({})
// [zh] 声明变量 `formDataRef`
const formDataRef = ref()
// [zh] 声明变量 `rules`
const rules = {
  // [zh] TypeScript/JS 语句：`senderName: [{ required: true, message: '请输入发件人' }],`
  senderName: [{ required: true, message: '请输入发件人' }],
  // [zh] TypeScript/JS 语句：`senderPhone: [{ required: true, message: '请输入发件人' }],`
  senderPhone: [{ required: true, message: '请输入发件人' }],
  // [zh] TypeScript/JS 语句：`senderDddress: [{ required: true, message: '请输入发件人' }],`
  senderDddress: [{ required: true, message: '请输入发件人' }],
// [zh] 语法符号
}

// [zh] 声明变量 `getSysLogistic`
const getSysLogistic = async () => {
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.getSysLogistics,`
    url: proxy.Api.getSysLogistics,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`formData.value = result.data || {}`
  formData.value = result.data || {}
// [zh] 语法符号
}

// [zh] 声明变量 `saveSetting`
const saveSetting = async () => {
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.saveSysSaveLogistics,`
    url: proxy.Api.saveSysSaveLogistics,
    // [zh] TypeScript/JS 语句：`params: formData.value,`
    params: formData.value,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`proxy.Message.success('保存成功')`
  proxy.Message.success('保存成功')
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`getSysLogistic()`
  getSysLogistic()
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.logistics-panel {` */
.logistics-panel {
  /* [zh] 样式规则 `padding: 20px;` */
  padding: 20px;
  /* [zh] 样式规则 `width: 600px;` */
  width: 600px;
/* [zh] 样式规则 `}` */
}
</style>
