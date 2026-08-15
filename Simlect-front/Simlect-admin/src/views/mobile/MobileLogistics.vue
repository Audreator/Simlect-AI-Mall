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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, getCurrentInstance, onMounted } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `formData`
const formData = ref({})

// [zh] 声明变量 `getSysLogistic`
const getSysLogistic = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({ url: proxy.Api.getSysLogistics })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`formData.value = result.data || {}`
  formData.value = result.data || {}
// [zh] 语法符号
}

// [zh] 声明变量 `saveSetting`
const saveSetting = async () => {
  // [zh] 条件判断 if
  if (!formData.value.senderName?.trim()) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请输入发件人')`
    proxy.Message.warning('请输入发件人')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!formData.value.senderPhone?.trim()) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请输入联系电话')`
    proxy.Message.warning('请输入联系电话')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!formData.value.senderAddress?.trim()) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请输入发货地址')`
    proxy.Message.warning('请输入发货地址')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.saveSysSaveLogistics,`
    url: proxy.Api.saveSysSaveLogistics,
    // [zh] TypeScript/JS 语句：`params: formData.value,`
    params: formData.value,
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

// [zh] TypeScript/JS 语句：`onMounted(getSysLogistic)`
onMounted(getSysLogistic)
</script>
