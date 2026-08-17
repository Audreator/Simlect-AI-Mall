<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="glass-card m-form">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="m-form-title">发货信息</h3>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-field">
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-label">发件人</label>
        <!-- [zh] 开始标签 `<input>` -->
        <input v-model.trim="formData.senderName" class="m-input" placeholder="请输入发件人" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-field">
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-label">联系电话</label>
        <!-- [zh] 开始标签 `<input>` -->
        <input
          v-model.trim="formData.senderPhone"
          class="m-input"
          type="tel"
          maxlength="11"
          placeholder="发货人联系电话"
        />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-field">
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-label">发货地址</label>
        <!-- [zh] 开始标签 `<textarea>` -->
        <textarea v-model.trim="formData.senderAddress" class="m-textarea" placeholder="请输入发货地址" rows="4" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-form-ops">
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn primary block" @click="saveSetting">保存</button>
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
const formData = ref({})

const getSysLogistic = async () => {
  const result = await proxy.Request({ url: proxy.Api.getSysLogistics })
  if (!result) return
  formData.value = result.data || {}
}

const saveSetting = async () => {
  if (!formData.value.senderName?.trim()) {
    proxy.Message.warning('请输入发件人')
    return
  }
  if (!formData.value.senderPhone?.trim()) {
    proxy.Message.warning('请输入联系电话')
    return
  }
  if (!formData.value.senderAddress?.trim()) {
    proxy.Message.warning('请输入发货地址')
    return
  }
  const result = await proxy.Request({
    url: proxy.Api.saveSysSaveLogistics,
    params: formData.value,
    showLoading: true,
  })
  if (!result) return
  proxy.Message.success('保存成功')
}

onMounted(getSysLogistic)
</script>
