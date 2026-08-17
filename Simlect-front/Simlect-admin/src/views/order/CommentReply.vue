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

<script setup>
import CommentDetail from './CommentDetail.vue'
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

import { uploadImage } from '@/utils/Api.js'

const dialogConfig = ref({
  show: false,
  title: '商家评论',
  buttons: [
    {
      type: 'primary',
      text: '确定',
      click: (e) => {
        submitComment()
      },
    },
  ],
})

const formData = ref({ reCommentImages: [] })
const formDataRef = ref()

const rules = {
  commentBizReply: [{ required: true, message: '请输入评论内容' }],
}

const comemntInfo = ref({ commentImages: [] })
const getCommentInfo = async (orderId) => {
  let result = await proxy.Request({
    url: proxy.Api.getComment,
    params: {
      orderId,
    },
  })
  if (!result) {
    return
  }
  comemntInfo.value = result.data
  comemntInfo.value.commentImages = result.data.commentImages
    ? result.data.commentImages.split(',')
    : []
  comemntInfo.value.recommentImages = result.data.recommentImages
    ? result.data.recommentImages.split(',')
    : []

  formData.value.commentBizReply = comemntInfo.value.commentBizReply
}
const show = async (orderId) => {
  dialogConfig.value.show = true
  getCommentInfo(orderId)
  await nextTick()
  formDataRef.value.resetFields()
  formData.value.orderId = orderId
}
defineExpose({
  show,
})

const submitComment = async () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    let params = {}
    Object.assign(params, formData.value)
    let result = await proxy.Request({
      url: proxy.Api.bizComment,
      params,
    })
    if (!result) {
      return
    }
    proxy.Message.success('评价成功')
    dialogConfig.value.show = false
    emit('reload')
  })
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
</style>
