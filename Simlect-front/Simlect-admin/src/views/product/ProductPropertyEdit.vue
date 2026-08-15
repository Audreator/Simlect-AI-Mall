<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Dialog>` -->
  <Dialog :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" :showCancel="true"
    @close="dialogConfig.show = false">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="formData" :rules="rules" ref="formDataRef" label-width="110px" @submit.prevent>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="SKU属性名称" prop="propertyName">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input :maxLength="10" placeholder="请输入SKU属性名称，比如颜色" v-model="formData.propertyName" :show-word-limit="true"
          :maxlength="30" />
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="是否包含图片" prop="coverType">
        <!-- [zh] 开始标签 `<el-radio-group>` -->
        <el-radio-group v-model="formData.coverType">
          <!-- [zh] 开始标签 `<el-radio>` -->
          <el-radio :label="1">包含</el-radio>
          <!-- [zh] 开始标签 `<el-radio>` -->
          <el-radio :label="0">不包含</el-radio>
        <!-- [zh] 闭合标签 `</el-radio-group>` -->
        </el-radio-group>
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
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `dialogConfig`
const dialogConfig = ref({
  // [zh] TypeScript/JS 语句：`show: false,`
  show: false,
  // [zh] TypeScript/JS 语句：`title: '编辑属性',`
  title: '编辑属性',
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
  // [zh] TypeScript/JS 语句：`propertyName: [{ required: true, message: '请输入SKU属性名称' `
  propertyName: [{ required: true, message: '请输入SKU属性名称' }],
  // [zh] TypeScript/JS 语句：`coverType: [{ required: true, message: '请选择是否包含图片' }],`
  coverType: [{ required: true, message: '请选择是否包含图片' }],
// [zh] 语法符号
}

// [zh] 声明变量 `show`
const show = (data) => {
  // [zh] TypeScript/JS 语句：`dialogConfig.value.show = true`
  dialogConfig.value.show = true
  // [zh] 代码块开始
  nextTick(() => {
    // [zh] TypeScript/JS 语句：`formDataRef.value.resetFields()`
    formDataRef.value.resetFields()
    // [zh] TypeScript/JS 语句：`formData.value = Object.assign({}, data)`
    formData.value = Object.assign({}, data)
  // [zh] TypeScript/JS 语句：`})`
  })
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
const submitForm = async () => {
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
    // [zh] TypeScript/JS 语句：`delete params.children`
    delete params.children
    // [zh] 声明变量 `result`
    let result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.saveProductProperty,`
      url: proxy.Api.saveProductProperty,
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
    // [zh] TypeScript/JS 语句：`proxy.Message.success('保存成功')`
    proxy.Message.success('保存成功')
    // [zh] TypeScript/JS 语句：`emit('reload')`
    emit('reload')
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
