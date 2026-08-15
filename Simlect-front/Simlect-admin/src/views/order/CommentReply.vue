<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Dialog>` -->
  <Dialog :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" width="430px"
    :showCancel="false" @close="dialogConfig.show = false">
    <!-- [zh] 开始标签 `<CommentDetail>` -->
    <CommentDetail :data="comemntInfo" />
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="formData" :rules="rules" ref="formDataRef" @submit.prevent>
      
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="" prop="commentBizReply">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input ref="inputRef" clearable v-model="formData.commentBizReply" type="textarea" resize="none"
          :show-word-limit="true" :maxlength="300" :autosize="{ minRows: 3, maxRows: 5 }"
          placeholder="请输入评论"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
    <!-- [zh] 闭合标签 `</el-form>` -->
    </el-form>
  <!-- [zh] 闭合标签 `</Dialog>` -->
  </Dialog>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./CommentDetail.vue`
import CommentDetail from './CommentDetail.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
// [zh] 导入 `vue-router`
import { useRouter, useRoute } from 'vue-router'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()
// [zh] 声明变量 `route`
const route = useRoute()

// [zh] 导入 `@/utils/Api.js`
import { uploadImage } from '@/utils/Api.js'

// [zh] 声明变量 `dialogConfig`
const dialogConfig = ref({
  // [zh] TypeScript/JS 语句：`show: false,`
  show: false,
  // [zh] TypeScript/JS 语句：`title: '商家评论',`
  title: '商家评论',
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
        // [zh] TypeScript/JS 语句：`submitComment()`
        submitComment()
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] 语法符号
  ],
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `formData`
const formData = ref({ reCommentImages: [] })
// [zh] 声明变量 `formDataRef`
const formDataRef = ref()

// [zh] 声明变量 `rules`
const rules = {
  // [zh] TypeScript/JS 语句：`commentBizReply: [{ required: true, message: '请输入评论内容' `
  commentBizReply: [{ required: true, message: '请输入评论内容' }],
// [zh] 语法符号
}

// [zh] 声明变量 `comemntInfo`
const comemntInfo = ref({ commentImages: [] })
// [zh] 声明变量 `getCommentInfo`
const getCommentInfo = async (orderId) => {
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.getComment,`
    url: proxy.Api.getComment,
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
  // [zh] TypeScript/JS 语句：`comemntInfo.value = result.data`
  comemntInfo.value = result.data
  // [zh] TypeScript/JS 语句：`comemntInfo.value.commentImages = result.data.commentIm`
  comemntInfo.value.commentImages = result.data.commentImages
    // [zh] TypeScript/JS 语句：`? result.data.commentImages.split(',')`
    ? result.data.commentImages.split(',')
    // [zh] TypeScript/JS 语句：`: []`
    : []
  // [zh] TypeScript/JS 语句：`comemntInfo.value.recommentImages = result.data.recomme`
  comemntInfo.value.recommentImages = result.data.recommentImages
    // [zh] TypeScript/JS 语句：`? result.data.recommentImages.split(',')`
    ? result.data.recommentImages.split(',')
    // [zh] TypeScript/JS 语句：`: []`
    : []

  // [zh] TypeScript/JS 语句：`formData.value.commentBizReply = comemntInfo.value.comm`
  formData.value.commentBizReply = comemntInfo.value.commentBizReply
// [zh] 语法符号
}
// [zh] 声明变量 `show`
const show = async (orderId) => {
  // [zh] TypeScript/JS 语句：`dialogConfig.value.show = true`
  dialogConfig.value.show = true
  // [zh] TypeScript/JS 语句：`getCommentInfo(orderId)`
  getCommentInfo(orderId)
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextTick()
  // [zh] TypeScript/JS 语句：`formDataRef.value.resetFields()`
  formDataRef.value.resetFields()
  // [zh] TypeScript/JS 语句：`formData.value.orderId = orderId`
  formData.value.orderId = orderId
// [zh] 语法符号
}
// [zh] 代码块开始
defineExpose({
  // [zh] TypeScript/JS 语句：`show,`
  show,
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `submitComment`
const submitComment = async () => {
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
    // [zh] 声明变量 `result`
    let result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.bizComment,`
      url: proxy.Api.bizComment,
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
    // [zh] TypeScript/JS 语句：`proxy.Message.success('评价成功')`
    proxy.Message.success('评价成功')
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
<style lang="scss" scoped>
</style>
