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

<script setup>
import { ref, reactive, getCurrentInstance, nextTick, onMounted } from 'vue'
const { proxy } = getCurrentInstance()

const formData = ref({})
const formDataRef = ref()
const rules = {
  senderName: [{ required: true, message: '请输入发件人' }],
  senderPhone: [{ required: true, message: '请输入发件人' }],
  senderDddress: [{ required: true, message: '请输入发件人' }],
}

const getSysLogistic = async () => {
  let result = await proxy.Request({
    url: proxy.Api.getSysLogistics,
  })
  if (!result) {
    return
  }
  formData.value = result.data || {}
}

const saveSetting = async () => {
  let result = await proxy.Request({
    url: proxy.Api.saveSysSaveLogistics,
    params: formData.value,
  })
  if (!result) {
    return
  }
  proxy.Message.success('保存成功')
}

onMounted(() => {
  getSysLogistic()
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
