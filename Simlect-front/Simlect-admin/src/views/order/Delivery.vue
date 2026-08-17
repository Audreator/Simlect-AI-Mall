<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Dialog>` -->
  <Dialog :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" width="800px"
    @close="dialogConfig.show = false">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="formData" :rules="rules" ref="formDataRef" label-width="80px" @submit.prevent>
      
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="收件人" prop="receiverName">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input clearable placeholder="请输入收件人" v-model.trim="formData.receiverName"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="联系电话" prop="receiverPhone">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input clearable placeholder="请输入联系电话" v-model.trim="formData.receiverPhone" :maxlength="11"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="收货地址" prop="receiverAddress">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input type="textarea" clearable placeholder="请输入收货地址" v-model.trim="formData.receiverAddress"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="发件人" prop="senderName">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input clearable placeholder="请输入发件人" v-model.trim="formData.senderName"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="联系电话" prop="senderPhone">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input clearable placeholder="请输入发货人联系电话" v-model.trim="formData.senderPhone" :maxlength=" 11"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="发货地址" prop="senderAddress">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input type="textarea" clearable placeholder="请输入发货地址" v-model.trim="formData.senderAddress"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>

      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="物流公司" prop="logisticsCompany">
        
        <!-- [zh] 开始标签 `<el-select>` -->
        <el-select clearable placeholder="选择物流公司" v-model="formData.logisticsCompany">
          <!-- [zh] 开始标签 `<el-option>` -->
          <el-option value="顺丰" label="顺丰"></el-option>
          <!-- [zh] 开始标签 `<el-option>` -->
          <el-option value="中通" label="中通"></el-option>
          <!-- [zh] 开始标签 `<el-option>` -->
          <el-option value="圆通" label="圆通"></el-option>
          <!-- [zh] 开始标签 `<el-option>` -->
          <el-option value="韵达" label="韵达"></el-option>
        <!-- [zh] 闭合标签 `</el-select>` -->
        </el-select>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="物流单号" prop="logisticsNo">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input clearable placeholder="请输入物流单号" v-model.trim="formData.logisticsNo"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
    <!-- [zh] 闭合标签 `</el-form>` -->
    </el-form>
  <!-- [zh] 闭合标签 `</Dialog>` -->
  </Dialog>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { loadRouteLocation } from 'vue-router'
const { proxy } = getCurrentInstance()

const dialogConfig = ref({
  show: false,
  title: '发货',
  buttons: [
    {
      type: 'primary',
      text: '确定',
      click: (e) => {
        submitForm()
      },
    },
  ],
})

const formData = ref({})
const formDataRef = ref()
const rules = {
  receiverName: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
  ],
  receiverAddress: [
    { required: true, message: '请输入收货地址', trigger: 'blur' },
  ],
  senderName: [{ required: true, message: '请输入发件人', trigger: 'blur' }],
  senderPhone: [
    { required: true, message: '请输入发货人联系电话', trigger: 'blur' },
  ],
  senderAddress: [
    { required: true, message: '请输入发货地址', trigger: 'blur' },
  ],
  logisticsCompany: [
    { required: true, message: '选择物流公司', trigger: 'blur' },
  ],
  logisticsNo: [{ required: true, message: '请输入物流单号', trigger: 'blur' }],
}

const show = async (orderId) => {
  dialogConfig.value.show = true
  await nextTick()
  formDataRef.value.resetFields()
  let result = await proxy.Request({
    url: proxy.Api.getLogistics,
    params: {
      orderId,
    },
  })
  if (!result) {
    return
  }
  formData.value = result.data
}

defineExpose({
  show,
})

const emit = defineEmits(['reload'])
const submitForm = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    let params = {}
    Object.assign(params, formData.value)
    delete params.recordList
    let result = await proxy.Request({
      url: proxy.Api.delivery,
      params,
    })
    if (!result) {
      return
    }
    dialogConfig.value.show = false
    emit('reload')
  })
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
