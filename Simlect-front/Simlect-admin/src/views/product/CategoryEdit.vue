<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Dialog>` -->
  <Dialog :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" :showCancel="true"
    @close="dialogConfig.show = false">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="formData" :rules="rules" ref="formDataRef" label-width="80px" @submit.prevent>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="分类名称" prop="categoryName">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input :maxLength="10" v-model="formData.categoryName" :show-word-limit="true" :maxlength="30" />
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
  title: '新增分类',
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
  categoryName: [{ required: true, message: '请输入分类名称' }],
}

const show = (data) => {
  dialogConfig.value.show = true
  nextTick(() => {
    formDataRef.value.resetFields()
    if (data.categoryId == null) {
      dialogConfig.value.title = '新增分类'
    } else {
      dialogConfig.value.title = '修改分类'
    }
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
    delete params.productPropertyList
    let result = await proxy.Request({
      url: proxy.Api.saveCategory,
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
