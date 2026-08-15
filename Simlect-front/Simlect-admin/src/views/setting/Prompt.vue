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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`ref,`
  ref,
  // [zh] TypeScript/JS 语句：`reactive,`
  reactive,
  // [zh] TypeScript/JS 语句：`getCurrentInstance,`
  getCurrentInstance,
  // [zh] TypeScript/JS 语句：`nextTick,`
  nextTick,
  // [zh] TypeScript/JS 语句：`renderSlot,`
  renderSlot,
  // [zh] TypeScript/JS 语句：`onMounted,`
  onMounted,
// [zh] TypeScript/JS 语句：`} from 'vue'`
} from 'vue'
// [zh] 导入 `vue-router`
import { useRouter, useRoute } from 'vue-router'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()
// [zh] 声明变量 `route`
const route = useRoute()

// [zh] 声明变量 `activeName`
const activeName = ref()

// [zh] 声明变量 `formData`
const formData = ref({})
// [zh] 声明变量 `formDataRef`
const formDataRef = ref()
// [zh] 声明变量 `rules`
const rules = {
  // [zh] TypeScript/JS 语句：`prompt: [{ required: true, message: '请输入提示词' }],`
  prompt: [{ required: true, message: '请输入提示词' }],
// [zh] 语法符号
}

// [zh] 声明变量 `promptList`
const promptList = ref([])
// [zh] 声明变量 `loadPrompt`
const loadPrompt = async () => {
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadPromptList,`
    url: proxy.Api.loadPromptList,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`promptList.value = result.data`
  promptList.value = result.data
  // [zh] TypeScript/JS 语句：`activeName.value = result.data[0].key`
  activeName.value = result.data[0].key

  // [zh] TypeScript/JS 语句：`getPromptDetail()`
  getPromptDetail()
// [zh] 语法符号
}

// [zh] 声明变量 `getPromptDetail`
const getPromptDetail = async () => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextTick()
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.getPromptDetail,`
    url: proxy.Api.getPromptDetail,
    // [zh] 代码块开始
    params: {
      // [zh] TypeScript/JS 语句：`key: activeName.value,`
      key: activeName.value,
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
  // [zh] TypeScript/JS 语句：`formData.value.prompt = result.data`
  formData.value.prompt = result.data
// [zh] 语法符号
}

// [zh] 声明变量 `savePrompt`
const savePrompt = () => {
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
    // [zh] TypeScript/JS 语句：`params.key = activeName.value`
    params.key = activeName.value
    // [zh] 声明变量 `result`
    let result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.savePrompt,`
      url: proxy.Api.savePrompt,
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
    // [zh] TypeScript/JS 语句：`proxy.Message.success('保存成功')`
    proxy.Message.success('保存成功')
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `cleanPrompt`
const cleanPrompt = () => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: '确定要清空缓存吗，清空缓存后将使用系统默认提示词',`
    message: '确定要清空缓存吗，清空缓存后将使用系统默认提示词',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.cleanPromptCache,`
        url: proxy.Api.cleanPromptCache,
        // [zh] 代码块开始
        params: {
          // [zh] TypeScript/JS 语句：`key: activeName.value,`
          key: activeName.value,
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
      // [zh] TypeScript/JS 语句：`proxy.Message.success('清空缓存成功')`
      proxy.Message.success('清空缓存成功')

      // [zh] TypeScript/JS 语句：`getPromptDetail()`
      getPromptDetail()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`loadPrompt()`
  loadPrompt()
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
