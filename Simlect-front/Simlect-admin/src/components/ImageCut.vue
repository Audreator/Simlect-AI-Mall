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
        Math.round(props.cutWidth * props.scale)
      }}的图片
    </div>
  </Dialog>
</template>

<script setup>
import 'vue-cropper/dist/index.css'
import { VueCropper } from 'vue-cropper'

import { ref, reactive, getCurrentInstance, nextTick, inject } from 'vue'
const { proxy } = getCurrentInstance()
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()


const props = defineProps({
  cutWidth: {
    type: Number,
    default: 400,
  },
  
  scale: {
    type: Number,
    default: 0.5,
  },
})

const dialogConfig = ref({
  show: false,
  title: '上传图片',
  buttons: [
    {
      type: 'primary',
      text: '确定',
      click: (e) => {
        cutImage()
      },
    },
  ],
})

const cropperRef = ref()
const previewsImage = ref()
const prview = (data) => {
  cropperRef.value.getCropData((data) => {
    previewsImage.value = data
  })
}

const sourceImage = ref()
const sourceFile = ref()
const selectFile = (file) => {
  file = file.file
  sourceFile.value = file
  let img = new FileReader()
  img.readAsDataURL(file)
  img.onload = ({ target }) => {
    sourceImage.value = target.result
  }
}

const show = () => {
  dialogConfig.value.show = true
  sourceImage.value = ''
  nextTick(() => {
    previewsImage.value = ''
  })
}

defineExpose({
  show,
})

const emits = defineEmits(['cutImageCallback'])

const cutImage = () => {
  
  const cropW = Math.round(cropperRef.value.cropW)
  const cropH = Math.round(cropperRef.value.cropH)
  if (cropW == 0 || cropH == 0) {
    proxy.Message.warning(`请选择图片`)
    return
  }
  if (
    cropW < props.cutWidth ||
    cropH < Math.round(props.cutWidth * props.scale)
  ) {
    proxy.Message.warning(
      `图片尺寸至少满足(${props.cutWidth}*${Math.round(
        props.cutWidth * props.scale
      )}`
    )
    return
  }
  cropperRef.value.getCropBlob((blob) => {
    const file = new File(
      [blob],
      'temp.' + blob.type.substring(blob.type.indexOf('/') + 1),
      { type: blob.type }
    )
    dialogConfig.value.show = false

    emits('cutImageCallback', {
      file: sourceFile.value,
      cover: file,
    })
  })
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
