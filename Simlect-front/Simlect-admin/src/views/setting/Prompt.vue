<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-tabs>` -->
  <el-tabs v-model="activeName" class="demo-tabs" @tab-click="getPromptDetail">
    <!-- [zh] 开始标签 `<el-tab-pane>` -->
    <el-tab-pane :label="item.desc" :name="item.key" v-for="item in promptList"></el-tab-pane>
  <!-- [zh] 闭合标签 `</el-tabs>` -->
  </el-tabs>
  <!-- [zh] 开始标签 `<el-form>` -->
  <el-form :model="formData" :rules="rules" ref="formDataRef" label-width="80px" @submit.prevent class="form-panel">
    
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item label="提示词" prop="prompt">
      <!-- [zh] 开始标签 `<el-input>` -->
      <el-input ref="inputRef" clearable v-model="formData.prompt" type="textarea" resize="none"
        :autosize="{ minRows: 20, maxRows: 40 }" placeholder="请输入提示词"></el-input>
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
    
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item label="" prop="">
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" @click="savePrompt">保存</el-button>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="danger" @click="cleanPrompt">清空redis缓存</el-button>
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
  <!-- [zh] 闭合标签 `</el-form>` -->
  </el-form>
</template>

<script setup>
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  renderSlot,
  onMounted,
} from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const activeName = ref()

const formData = ref({})
const formDataRef = ref()
const rules = {
  prompt: [{ required: true, message: '请输入提示词' }],
}

const promptList = ref([])
const loadPrompt = async () => {
  let result = await proxy.Request({
    url: proxy.Api.loadPromptList,
  })
  if (!result) {
    return
  }
  promptList.value = result.data
  activeName.value = result.data[0].key

  getPromptDetail()
}

const getPromptDetail = async () => {
  await nextTick()
  let result = await proxy.Request({
    url: proxy.Api.getPromptDetail,
    params: {
      key: activeName.value,
    },
  })
  if (!result) {
    return
  }
  formData.value.prompt = result.data
}

const savePrompt = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    let params = {}
    Object.assign(params, formData.value)
    params.key = activeName.value
    let result = await proxy.Request({
      url: proxy.Api.savePrompt,
      params,
    })
    if (!result) {
      return
    }
    proxy.Message.success('保存成功')
  })
}

const cleanPrompt = () => {
  proxy.Confirm({
    message: '确定要清空缓存吗，清空缓存后将使用系统默认提示词',
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.cleanPromptCache,
        params: {
          key: activeName.value,
        },
      })
      if (!result) {
        return
      }
      proxy.Message.success('清空缓存成功')

      getPromptDetail()
    },
  })
}

onMounted(() => {
  loadPrompt()
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
