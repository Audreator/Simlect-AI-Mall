<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Dialog>` -->
  <Dialog :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" width="1000px"
    @close="dialogConfig.show = false">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="cut-image-panel">
      <!-- [zh] 开始标签 `<VueCropper>` -->
      <VueCropper ref="cropperRef" class="cropper" :img="sourceImage" outputType="png" :autoCrop="true"
        :autoCropWidth="props.cutWidth" :autoCropHeight="Math.round(props.cutWidth * props.scale)" :fixed="true"
        :fixedNumber="[1, props.scale]" :centerBox="true" :full="false" :fixedBox="true" @realTime="prview" mode="100%">
      <!-- [zh] 闭合标签 `</VueCropper>` -->
      </VueCropper>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="preview-panel">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="preview-image">
          <!-- [zh] 开始标签 `<img>` -->
          <img :src="previewsImage" />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<el-upload>` -->
        <el-upload :multiple="false" :show-file-list="false" :http-request="selectFile" :accept="proxy.imageAccept">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button class="select-btn" type="primary" @click="">选择图片</el-button>
        <!-- [zh] 闭合标签 `</el-upload>` -->
        </el-upload>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="info">
      <!-- [zh] 模板内容：`建议上传至少{{ props.cutWidth }}*{{` -->
      建议上传至少{{ props.cutWidth }}*{{
        <!-- [zh] 模板内容：`Math.round(props.cutWidth * props.scale)` -->
        Math.round(props.cutWidth * props.scale)
      <!-- [zh] 模板内容：`}}的图片` -->
      }}的图片
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</Dialog>` -->
  </Dialog>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `模块`
import 'vue-cropper/dist/index.css'
// [zh] 导入 `vue-cropper`
import { VueCropper } from 'vue-cropper'

// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick, inject } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router'
// [zh] 声明变量 `route`
const route = useRoute()
// [zh] 声明变量 `router`
const router = useRouter()


// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  cutWidth: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 400,`
    default: 400,
  // [zh] TypeScript/JS 语句：`},`
  },
  
  // [zh] 代码块开始
  scale: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 0.5,`
    default: 0.5,
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `dialogConfig`
const dialogConfig = ref({
  // [zh] TypeScript/JS 语句：`show: false,`
  show: false,
  // [zh] TypeScript/JS 语句：`title: '上传图片',`
  title: '上传图片',
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
        // [zh] TypeScript/JS 语句：`cutImage()`
        cutImage()
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] 语法符号
  ],
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `cropperRef`
const cropperRef = ref()
// [zh] 声明变量 `previewsImage`
const previewsImage = ref()
// [zh] 声明变量 `prview`
const prview = (data) => {
  // [zh] 代码块开始
  cropperRef.value.getCropData((data) => {
    // [zh] TypeScript/JS 语句：`previewsImage.value = data`
    previewsImage.value = data
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `sourceImage`
const sourceImage = ref()
// [zh] 声明变量 `sourceFile`
const sourceFile = ref()
// [zh] 声明变量 `selectFile`
const selectFile = (file) => {
  // [zh] TypeScript/JS 语句：`file = file.file`
  file = file.file
  // [zh] TypeScript/JS 语句：`sourceFile.value = file`
  sourceFile.value = file
  // [zh] 声明变量 `img`
  let img = new FileReader()
  // [zh] TypeScript/JS 语句：`img.readAsDataURL(file)`
  img.readAsDataURL(file)
  // [zh] 代码块开始
  img.onload = ({ target }) => {
    // [zh] TypeScript/JS 语句：`sourceImage.value = target.result`
    sourceImage.value = target.result
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `show`
const show = () => {
  // [zh] TypeScript/JS 语句：`dialogConfig.value.show = true`
  dialogConfig.value.show = true
  // [zh] TypeScript/JS 语句：`sourceImage.value = ''`
  sourceImage.value = ''
  // [zh] 代码块开始
  nextTick(() => {
    // [zh] TypeScript/JS 语句：`previewsImage.value = ''`
    previewsImage.value = ''
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 代码块开始
defineExpose({
  // [zh] TypeScript/JS 语句：`show,`
  show,
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `emits`
const emits = defineEmits(['cutImageCallback'])

// [zh] 声明变量 `cutImage`
const cutImage = () => {
  
  // [zh] 声明变量 `cropW`
  const cropW = Math.round(cropperRef.value.cropW)
  // [zh] 声明变量 `cropH`
  const cropH = Math.round(cropperRef.value.cropH)
  // [zh] 条件判断 if
  if (cropW == 0 || cropH == 0) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning(`请选择图片`)`
    proxy.Message.warning(`请选择图片`)
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (
    // [zh] TypeScript/JS 语句：`cropW < props.cutWidth ||`
    cropW < props.cutWidth ||
    // [zh] TypeScript/JS 语句：`cropH < Math.round(props.cutWidth * props.scale)`
    cropH < Math.round(props.cutWidth * props.scale)
  // [zh] 代码块开始
  ) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning(`
    proxy.Message.warning(
      // [zh] TypeScript/JS 语句：``图片尺寸至少满足(${props.cutWidth}*${Math.round(`
      `图片尺寸至少满足(${props.cutWidth}*${Math.round(
        // [zh] TypeScript/JS 语句：`props.cutWidth * props.scale`
        props.cutWidth * props.scale
      // [zh] TypeScript/JS 语句：`)}``
      )}`
    // [zh] 语法符号
    )
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  cropperRef.value.getCropBlob((blob) => {
    // [zh] 声明变量 `file`
    const file = new File(
      // [zh] TypeScript/JS 语句：`[blob],`
      [blob],
      // [zh] TypeScript/JS 语句：`'temp.' + blob.type.substring(blob.type.indexOf('/') + `
      'temp.' + blob.type.substring(blob.type.indexOf('/') + 1),
      // [zh] TypeScript/JS 语句：`{ type: blob.type }`
      { type: blob.type }
    // [zh] 语法符号
    )
    // [zh] TypeScript/JS 语句：`dialogConfig.value.show = false`
    dialogConfig.value.show = false

    // [zh] 代码块开始
    emits('cutImageCallback', {
      // [zh] TypeScript/JS 语句：`file: sourceFile.value,`
      file: sourceFile.value,
      // [zh] TypeScript/JS 语句：`cover: file,`
      cover: file,
    // [zh] TypeScript/JS 语句：`})`
    })
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.cut-image-panel {` */
.cut-image-panel {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;

  /* [zh] 样式规则 `.cropper {` */
  .cropper {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `height: 500px;` */
    height: 500px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.preview-panel {` */
  .preview-panel {
    /* [zh] 样式规则 `width: 200px;` */
    width: 200px;
    /* [zh] 样式规则 `margin-left: 20px;` */
    margin-left: 20px;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;

    /* [zh] 样式规则 `.preview-image {` */
    .preview-image {
      /* [zh] 样式规则 `width: 100%;` */
      width: 100%;
      /* [zh] 样式规则 `height: 200px;` */
      height: 200px;
      /* [zh] 样式规则 `background: #f6f6f6;` */
      background: #f6f6f6;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `img {` */
    img {
      /* [zh] 样式规则 `width: 100%;` */
      width: 100%;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.select-btn {` */
  .select-btn {
    /* [zh] 样式规则 `margin-top: 20px;` */
    margin-top: 20px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.info {` */
.info {
  /* [zh] 样式规则 `color: #6b6b6b;` */
  color: #6b6b6b;
/* [zh] 样式规则 `}` */
}
</style>
