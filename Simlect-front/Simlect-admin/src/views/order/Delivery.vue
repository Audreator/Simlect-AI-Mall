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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
// [zh] 导入 `vue-router`
import { loadRouteLocation } from 'vue-router'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()

// [zh] 声明变量 `dialogConfig`
const dialogConfig = ref({
  // [zh] TypeScript/JS 语句：`show: false,`
  show: false,
  // [zh] TypeScript/JS 语句：`title: '发货',`
  title: '发货',
  // [zh] TypeScript/JS 语句：`buttons: [`
  buttons: [
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`type: 'primary',`
      type: 'primary',
      // [zh] TypeScript/JS 语句：`text: '确定',`
      text: '确定',
      // [zh] 代码块开始
      click: (e) => {
        // [zh] TypeScript/JS 语句：`submitForm()`
        submitForm()
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] 语法符号
  ],
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `formData`
const formData = ref({})
// [zh] 声明变量 `formDataRef`
const formDataRef = ref()
// [zh] 声明变量 `rules`
const rules = {
  // [zh] TypeScript/JS 语句：`receiverName: [{ required: true, message: '请输入收件人', tri`
  receiverName: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  // [zh] TypeScript/JS 语句：`receiverPhone: [`
  receiverPhone: [
    // [zh] TypeScript/JS 语句：`{ required: true, message: '请输入联系电话', trigger: 'blur' }`
    { required: true, message: '请输入联系电话', trigger: 'blur' },
  // [zh] 语法符号
  ],
  // [zh] TypeScript/JS 语句：`receiverAddress: [`
  receiverAddress: [
    // [zh] TypeScript/JS 语句：`{ required: true, message: '请输入收货地址', trigger: 'blur' }`
    { required: true, message: '请输入收货地址', trigger: 'blur' },
  // [zh] 语法符号
  ],
  // [zh] TypeScript/JS 语句：`senderName: [{ required: true, message: '请输入发件人', trigg`
  senderName: [{ required: true, message: '请输入发件人', trigger: 'blur' }],
  // [zh] TypeScript/JS 语句：`senderPhone: [`
  senderPhone: [
    // [zh] TypeScript/JS 语句：`{ required: true, message: '请输入发货人联系电话', trigger: 'blur`
    { required: true, message: '请输入发货人联系电话', trigger: 'blur' },
  // [zh] 语法符号
  ],
  // [zh] TypeScript/JS 语句：`senderAddress: [`
  senderAddress: [
    // [zh] TypeScript/JS 语句：`{ required: true, message: '请输入发货地址', trigger: 'blur' }`
    { required: true, message: '请输入发货地址', trigger: 'blur' },
  // [zh] 语法符号
  ],
  // [zh] TypeScript/JS 语句：`logisticsCompany: [`
  logisticsCompany: [
    // [zh] TypeScript/JS 语句：`{ required: true, message: '选择物流公司', trigger: 'blur' },`
    { required: true, message: '选择物流公司', trigger: 'blur' },
  // [zh] 语法符号
  ],
  // [zh] TypeScript/JS 语句：`logisticsNo: [{ required: true, message: '请输入物流单号', tri`
  logisticsNo: [{ required: true, message: '请输入物流单号', trigger: 'blur' }],
// [zh] 语法符号
}

// [zh] 声明变量 `show`
const show = async (orderId) => {
  // [zh] TypeScript/JS 语句：`dialogConfig.value.show = true`
  dialogConfig.value.show = true
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextTick()
  // [zh] TypeScript/JS 语句：`formDataRef.value.resetFields()`
  formDataRef.value.resetFields()
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.getLogistics,`
    url: proxy.Api.getLogistics,
    // [zh] 代码块开始
    params: {
      // [zh] TypeScript/JS 语句：`orderId,`
      orderId,
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`formData.value = result.data`
  formData.value = result.data
// [zh] 语法符号
}

// [zh] 代码块开始
defineExpose({
  // [zh] TypeScript/JS 语句：`show,`
  show,
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `emit`
const emit = defineEmits(['reload'])
// [zh] 声明变量 `submitForm`
const submitForm = () => {
  // [zh] 代码块开始
  formDataRef.value.validate(async (valid) => {
    // [zh] 条件判断 if
    if (!valid) {
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
    // [zh] 声明变量 `params`
    let params = {}
    // [zh] TypeScript/JS 语句：`Object.assign(params, formData.value)`
    Object.assign(params, formData.value)
    // [zh] TypeScript/JS 语句：`delete params.recordList`
    delete params.recordList
    // [zh] 声明变量 `result`
    let result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.delivery,`
      url: proxy.Api.delivery,
      // [zh] TypeScript/JS 语句：`params,`
      params,
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 条件判断 if
    if (!result) {
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
    // [zh] TypeScript/JS 语句：`dialogConfig.value.show = false`
    dialogConfig.value.show = false
    // [zh] TypeScript/JS 语句：`emit('reload')`
    emit('reload')
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
