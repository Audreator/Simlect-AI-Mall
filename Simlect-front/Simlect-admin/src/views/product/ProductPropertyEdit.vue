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
<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
const { proxy } = getCurrentInstance()
const dialogConfig = ref({
  show: false,
  title: '编辑属性',
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
  propertyName: [{ required: true, message: '请输入SKU属性名称' }],
  coverType: [{ required: true, message: '请选择是否包含图片' }],
}

const show = (data) => {
  dialogConfig.value.show = true
  nextTick(() => {
    formDataRef.value.resetFields()
    formData.value = Object.assign({}, data)
  })
}

defineExpose({
  show,
})

const emit = defineEmits(['reload'])
const submitForm = async () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    let params = {}
    Object.assign(params, formData.value)
    delete params.children
    let result = await proxy.Request({
      url: proxy.Api.saveProductProperty,
      params,
    })
    if (!result) {
      return
    }
    dialogConfig.value.show = false
    proxy.Message.success('保存成功')
    emit('reload')
  })
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
