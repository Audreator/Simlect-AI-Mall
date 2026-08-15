<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="promptList.length" class="m-chip-tabs">
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-for="item in promptList"
        :key="item.key"
        type="button"
        class="chip"
        :class="{ active: activeName === item.key }"
        @click="switchPrompt(item.key)"
      >
        <!-- [zh] Mustache 插值表达式 -->
        {{ item.desc }}
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="glass-card m-form">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-field">
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-label">提示词内容</label>
        <!-- [zh] 开始标签 `<textarea>` -->
        <textarea
          v-model="formData.prompt"
          class="m-textarea prompt-area"
          placeholder="请输入提示词"
          rows="16"
        />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-form-ops">
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn primary" @click="savePrompt">保存</button>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn warning" @click="cleanPrompt">清空缓存</button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, getCurrentInstance, onMounted } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `activeName`
const activeName = ref('')
// [zh] 声明变量 `formData`
const formData = ref({ prompt: '' })
// [zh] 声明变量 `promptList`
const promptList = ref([])

// [zh] 声明变量 `getPromptDetail`
const getPromptDetail = async () => {
  // [zh] 条件判断 if
  if (!activeName.value) return
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.getPromptDetail,`
    url: proxy.Api.getPromptDetail,
    // [zh] TypeScript/JS 语句：`params: { key: activeName.value },`
    params: { key: activeName.value },
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`formData.value.prompt = result.data || ''`
  formData.value.prompt = result.data || ''
// [zh] 语法符号
}

// [zh] 声明变量 `switchPrompt`
const switchPrompt = async (key) => {
  // [zh] TypeScript/JS 语句：`activeName.value = key`
  activeName.value = key
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await getPromptDetail()
// [zh] 语法符号
}

// [zh] 声明变量 `loadPrompt`
const loadPrompt = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({ url: proxy.Api.loadPromptList })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`promptList.value = result.data || []`
  promptList.value = result.data || []
  // [zh] 条件判断 if
  if (promptList.value.length) {
    // [zh] TypeScript/JS 语句：`activeName.value = promptList.value[0].key`
    activeName.value = promptList.value[0].key
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await getPromptDetail()
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `savePrompt`
const savePrompt = async () => {
  // [zh] 条件判断 if
  if (!formData.value.prompt?.trim()) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请输入提示词')`
    proxy.Message.warning('请输入提示词')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.savePrompt,`
    url: proxy.Api.savePrompt,
    // [zh] TypeScript/JS 语句：`params: { key: activeName.value, prompt: formData.value`
    params: { key: activeName.value, prompt: formData.value.prompt },
    // [zh] TypeScript/JS 语句：`showLoading: true,`
    showLoading: true,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`proxy.Message.success('保存成功')`
  proxy.Message.success('保存成功')
// [zh] 语法符号
}

// [zh] 声明变量 `cleanPrompt`
const cleanPrompt = () => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: '确定要清空缓存吗？清空后将使用系统默认提示词',`
    message: '确定要清空缓存吗？清空后将使用系统默认提示词',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.cleanPromptCache,`
        url: proxy.Api.cleanPromptCache,
        // [zh] TypeScript/JS 语句：`params: { key: activeName.value },`
        params: { key: activeName.value },
        // [zh] TypeScript/JS 语句：`showLoading: true,`
        showLoading: true,
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
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

// [zh] TypeScript/JS 语句：`onMounted(loadPrompt)`
onMounted(loadPrompt)
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.prompt-area {` */
.prompt-area {
  /* [zh] 样式规则 `min-height: 280px;` */
  min-height: 280px;
/* [zh] 样式规则 `}` */
}
</style>
