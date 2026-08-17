<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-upload>` -->
  <el-upload ref="uploaderRef" :multiple="false" :show-file-list="false" :http-request="selectImage"
    :accept="proxy.imageAccept">
    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="props.modelValue" class="cover">
      <!-- [zh] 开始标签 `<Cover>` -->
      <Cover :source="props.modelValue" :width="width" :scale="scale"></Cover>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div v-else class="iconfont icon-image image-upload"
      :style="{ width: width + 'px', height: width + 'px', 'font-size': width / 2 + 'px' }">
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</el-upload>` -->
  </el-upload>
</template>
<script setup>
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  inject,
  computed,
} from 'vue'
const { proxy } = getCurrentInstance()
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()

import { uploadImage } from '@/utils/Api.js'

const props = defineProps({
  width: {
    type: Number,
    default: 100,
  },
  modelValue: {
    type: [String, File],
  },
  cutWidth: {
    type: Number,
    default: 150,
  },
  
  scale: {
    type: Number,
    default: 1,
  },
})
const emits = defineEmits(['update:modelValue'])
const selectImage = async (file) => {
  const result = await uploadImage(file.file, true)
  emits('update:modelValue', result)
}

</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.image-upload {` */
.image-upload {
  /* [zh] 样式规则 `border: 1px dashed #d5dcfb;` */
  border: 1px dashed #d5dcfb;
  /* [zh] 样式规则 `border-radius: 5px;` */
  border-radius: 5px;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `color: #646464;` */
  color: #646464;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cover {` */
.cover {
  /* [zh] 样式规则 `background: #f0f0f0;` */
  background: #f0f0f0;
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `border-radius: 5px;` */
  border-radius: 5px;
/* [zh] 样式规则 `}` */
}
</style>
