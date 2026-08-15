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
  // [zh] TypeScript/JS 语句：`inject,`
  inject,
  // [zh] TypeScript/JS 语句：`computed,`
  computed,
// [zh] TypeScript/JS 语句：`} from 'vue'`
} from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router'
// [zh] 声明变量 `route`
const route = useRoute()
// [zh] 声明变量 `router`
const router = useRouter()

// [zh] 导入 `@/utils/Api.js`
import { uploadImage } from '@/utils/Api.js'

// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  width: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 100,`
    default: 100,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  modelValue: {
    // [zh] TypeScript/JS 语句：`type: [String, File],`
    type: [String, File],
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  cutWidth: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 150,`
    default: 150,
  // [zh] TypeScript/JS 语句：`},`
  },
  
  // [zh] 代码块开始
  scale: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 1,`
    default: 1,
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})
// [zh] 声明变量 `emits`
const emits = defineEmits(['update:modelValue'])
// [zh] 声明变量 `selectImage`
const selectImage = async (file) => {
  // [zh] 声明变量 `result`
  const result = await uploadImage(file.file, true)
  // [zh] TypeScript/JS 语句：`emits('update:modelValue', result)`
  emits('update:modelValue', result)
// [zh] 语法符号
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
