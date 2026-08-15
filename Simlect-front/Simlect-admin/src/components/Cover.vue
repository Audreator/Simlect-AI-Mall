<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="image-panel" ref="coverRef" :style="{
    'border-radius': borderRadius,
    width: width ? width + 'px' : '100%',
    'aspect-ratio': scale
  }">
    <!-- [zh] 开始标签 `<el-image>` -->
    <el-image v-if="coverFile" :lazy="lazy" :src="coverFile" :fit="fit" @click="showViewerHandler">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #placeholder>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="loading" :style="{ height: loadingHeight + 'px' }">
          <!-- [zh] 开始标签 `<img>` -->
          <img :src="proxy.Utils.getLocalResource('loading.gif')" />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      </template>
    </el-image>
    <div v-else class="cover-empty">
      <span class="iconfont icon-image-error"></span>
    </div>
    <Teleport to="body">
      <div
        v-if="showViewer"
        class="eshop-image-lightbox allow-pinch-zoom"
        @click="closeViewer"
        @wheel.prevent="onWheel"
        @mouseup="onMouseUp"
        @mouseleave="onMouseUp"
      >
        <img
          :key="viewerUrl"
          :src="viewerUrl"
          class="eshop-image-lightbox__img allow-pinch-zoom"
          :style="imgStyle"
          alt=""
          draggable="false"
          @click.stop
          @touchstart="onTouchStart"
          @touchmove="onTouchMove"
          @touchend="onTouchEnd"
          @touchcancel="onTouchEnd"
          @mousedown="onMouseDown"
          @mousemove="onMouseMove"
        />
      </div>
    </Teleport>
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`ref,`
  ref,
  // [zh] TypeScript/JS 语句：`getCurrentInstance,`
  getCurrentInstance,
  // [zh] TypeScript/JS 语句：`computed,`
  computed,
  // [zh] TypeScript/JS 语句：`onMounted,`
  onMounted,
  // [zh] TypeScript/JS 语句：`onUnmounted,`
  onUnmounted,
  // [zh] TypeScript/JS 语句：`watch,`
  watch,
// [zh] TypeScript/JS 语句：`} from 'vue'`
} from 'vue'
// [zh] 导入 `@/composables/useImageLightboxZoom.js`
import { useImageLightboxZoom } from '@/composables/useImageLightboxZoom.js'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()

// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  source: {
    // [zh] TypeScript/JS 语句：`type: [String, File],`
    type: [String, File],
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  width: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
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
  // [zh] 代码块开始
  fit: {
    // [zh] TypeScript/JS 语句：`type: String,`
    type: String,
    // [zh] TypeScript/JS 语句：`default: 'cover',`
    default: 'cover',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  preview: {
    // [zh] TypeScript/JS 语句：`type: Boolean,`
    type: Boolean,
    // [zh] TypeScript/JS 语句：`default: false,`
    default: false,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  borderRadius: {
    // [zh] TypeScript/JS 语句：`type: String,`
    type: String,
    // [zh] TypeScript/JS 语句：`default: '5px',`
    default: '5px',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  lazy: {
    // [zh] TypeScript/JS 语句：`type: Boolean,`
    type: Boolean,
    // [zh] TypeScript/JS 语句：`default: true,`
    default: true,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  preImageList: {
    // [zh] TypeScript/JS 语句：`type: Array,`
    type: Array,
    // [zh] TypeScript/JS 语句：`default: [],`
    default: [],
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `coverFile`
const coverFile = ref()
// [zh] 声明变量 `getCover`
const getCover = async () => {
  // [zh] 条件判断 if
  if (props.source == 'avatar.png') {
    // [zh] TypeScript/JS 语句：`coverFile.value = proxy.Utils.getLocalResource('avatar.`
    coverFile.value = proxy.Utils.getLocalResource('avatar.png')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (typeof props.source == 'string') {
    // [zh] 声明变量 `trimmed`
    const trimmed = props.source.trim()
    // [zh] 条件判断 if
    if (!trimmed) {
      // [zh] TypeScript/JS 语句：`coverFile.value = ''`
      coverFile.value = ''
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
    // [zh] TypeScript/JS 语句：`coverFile.value = proxy.Api.sourcePath + trimmed`
    coverFile.value = proxy.Api.sourcePath + trimmed
  // [zh] 代码块开始
  } else if (props.source instanceof File) {
    // [zh] 声明变量 `img`
    let img = new FileReader()
    // [zh] TypeScript/JS 语句：`img.readAsDataURL(props.source)`
    img.readAsDataURL(props.source)
    // [zh] 代码块开始
    img.onload = ({ target }) => {
      // [zh] TypeScript/JS 语句：`coverFile.value = target.result`
      coverFile.value = target.result
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => props.source,`
  () => props.source,
  // [zh] 代码块开始
  async (newSource) => {
    // [zh] TypeScript/JS 语句：`getCover(newSource)`
    getCover(newSource)
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`{ immediate: true }`
  { immediate: true }
// [zh] 语法符号
)

// [zh] 声明变量 `imageList`
const imageList = computed(() => {
  // [zh] return 返回
  return props.preImageList.map((item) => {
    // [zh] return 返回
    return proxy.Api.sourcePath + item.replace(proxy.imageThumbnailSuffix, '')
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `viewerIndex`
const viewerIndex = ref(0)
// [zh] 声明变量 `showViewer`
const showViewer = ref(false)

// [zh] 声明变量 `viewerUrl`
const viewerUrl = computed(() => imageList.value[viewerIndex.value] ?? '')

// [zh] 代码块开始
const {
  // [zh] TypeScript/JS 语句：`imgStyle,`
  imgStyle,
  // [zh] TypeScript/JS 语句：`resetTransform,`
  resetTransform,
  // [zh] TypeScript/JS 语句：`onWheel,`
  onWheel,
  // [zh] TypeScript/JS 语句：`onTouchStart,`
  onTouchStart,
  // [zh] TypeScript/JS 语句：`onTouchMove,`
  onTouchMove,
  // [zh] TypeScript/JS 语句：`onTouchEnd,`
  onTouchEnd,
  // [zh] TypeScript/JS 语句：`onMouseDown,`
  onMouseDown,
  // [zh] TypeScript/JS 语句：`onMouseMove,`
  onMouseMove,
  // [zh] TypeScript/JS 语句：`onMouseUp,`
  onMouseUp,
// [zh] TypeScript/JS 语句：`} = useImageLightboxZoom()`
} = useImageLightboxZoom()

// [zh] 声明变量 `closeViewer`
const closeViewer = () => {
  // [zh] TypeScript/JS 语句：`showViewer.value = false`
  showViewer.value = false
  // [zh] TypeScript/JS 语句：`document.body.style.overflow = ''`
  document.body.style.overflow = ''
  // [zh] TypeScript/JS 语句：`resetTransform()`
  resetTransform()
// [zh] 语法符号
}

// [zh] TypeScript/JS 语句：`watch(viewerUrl, () => resetTransform())`
watch(viewerUrl, () => resetTransform())

// [zh] 声明变量 `showViewerPrev`
const showViewerPrev = () => {
  // [zh] 声明变量 `n`
  const n = imageList.value.length
  // [zh] 条件判断 if
  if (n <= 1) return
  // [zh] TypeScript/JS 语句：`viewerIndex.value = (viewerIndex.value - 1 + n) % n`
  viewerIndex.value = (viewerIndex.value - 1 + n) % n
// [zh] 语法符号
}

// [zh] 声明变量 `showViewerNext`
const showViewerNext = () => {
  // [zh] 声明变量 `n`
  const n = imageList.value.length
  // [zh] 条件判断 if
  if (n <= 1) return
  // [zh] TypeScript/JS 语句：`viewerIndex.value = (viewerIndex.value + 1) % n`
  viewerIndex.value = (viewerIndex.value + 1) % n
// [zh] 语法符号
}

// [zh] 声明变量 `onViewerKeydown`
const onViewerKeydown = (e) => {
  // [zh] 条件判断 if
  if (!showViewer.value) return
  // [zh] 条件判断 if
  if (e.key === 'Escape') closeViewer()
  // [zh] else 分支
  else if (e.key === 'ArrowLeft') showViewerPrev()
  // [zh] else 分支
  else if (e.key === 'ArrowRight') showViewerNext()
// [zh] 语法符号
}

// [zh] 声明变量 `showViewerHandler`
const showViewerHandler = () => {
  // [zh] 条件判断 if
  if (props.preImageList.length == 0) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 声明变量 `idx`
  const idx = props.preImageList.findIndex((item) => item === props.source)
  // [zh] TypeScript/JS 语句：`viewerIndex.value = idx >= 0 ? idx : 0`
  viewerIndex.value = idx >= 0 ? idx : 0
  // [zh] TypeScript/JS 语句：`showViewer.value = true`
  showViewer.value = true
  // [zh] TypeScript/JS 语句：`document.body.style.overflow = 'hidden'`
  document.body.style.overflow = 'hidden'
// [zh] 语法符号
}

// [zh] 声明变量 `coverRef`
const coverRef = ref()
// [zh] 声明变量 `loadingHeight`
const loadingHeight = ref()
// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`loadingHeight.value = coverRef.value.clientWidth * prop`
  loadingHeight.value = coverRef.value.clientWidth * props.scale
  // [zh] TypeScript/JS 语句：`window.addEventListener('keydown', onViewerKeydown)`
  window.addEventListener('keydown', onViewerKeydown)
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] TypeScript/JS 语句：`window.removeEventListener('keydown', onViewerKeydown)`
  window.removeEventListener('keydown', onViewerKeydown)
  // [zh] TypeScript/JS 语句：`document.body.style.overflow = ''`
  document.body.style.overflow = ''
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.image-panel {` */
.image-panel {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `background: #f8f8f8;` */
  background: #f8f8f8;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;

  /* [zh] 样式规则 `:deep(.el-image) {` */
  :deep(.el-image) {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.is-loading) {` */
  :deep(.is-loading) {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.el-image__wrapper) {` */
  :deep(.el-image__wrapper) {
    /* [zh] 样式规则 `position: relative;` */
    position: relative;
    /* [zh] 样式规则 `vertical-align: top;` */
    vertical-align: top;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.icon-image-error {` */
  .icon-image-error {
    /* [zh] 样式规则 `margin: 0px auto;` */
    margin: 0px auto;
    /* [zh] 样式规则 `font-size: 20px;` */
    font-size: 20px;
    /* [zh] 样式规则 `color: #838383;` */
    color: #838383;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.loading {` */
  .loading {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;

    /* [zh] 样式规则 `img {` */
    img {
      /* [zh] 样式规则 `width: 20px;` */
      width: 20px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cover-empty {` */
  .cover-empty {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `color: #838383;` */
    color: #838383;
    /* [zh] 样式规则 `font-size: 20px;` */
    font-size: 20px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
