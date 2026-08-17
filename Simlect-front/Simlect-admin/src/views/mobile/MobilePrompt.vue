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

<script setup>
import { ref, getCurrentInstance, onMounted } from 'vue'

const { proxy } = getCurrentInstance()
const activeName = ref('')
const formData = ref({ prompt: '' })
const promptList = ref([])

const getPromptDetail = async () => {
  if (!activeName.value) return
  const result = await proxy.Request({
    url: proxy.Api.getPromptDetail,
    params: { key: activeName.value },
  })
  if (!result) return
  formData.value.prompt = result.data || ''
}

const switchPrompt = async (key) => {
  activeName.value = key
  await getPromptDetail()
}

const loadPrompt = async () => {
  const result = await proxy.Request({ url: proxy.Api.loadPromptList })
  if (!result) return
  promptList.value = result.data || []
  if (promptList.value.length) {
    activeName.value = promptList.value[0].key
    await getPromptDetail()
  }
}

const savePrompt = async () => {
  if (!formData.value.prompt?.trim()) {
    proxy.Message.warning('请输入提示词')
    return
  }
  const result = await proxy.Request({
    url: proxy.Api.savePrompt,
    params: { key: activeName.value, prompt: formData.value.prompt },
    showLoading: true,
  })
  if (!result) return
  proxy.Message.success('保存成功')
}

const cleanPrompt = () => {
  proxy.Confirm({
    message: '确定要清空缓存吗？清空后将使用系统默认提示词',
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.cleanPromptCache,
        params: { key: activeName.value },
        showLoading: true,
      })
      if (!result) return
      proxy.Message.success('清空缓存成功')
      getPromptDetail()
    },
  })
}

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
