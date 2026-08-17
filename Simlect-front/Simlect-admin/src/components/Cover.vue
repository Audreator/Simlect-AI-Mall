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

<script setup>
import {
  ref,
  getCurrentInstance,
  computed,
  onMounted,
  onUnmounted,
  watch,
} from 'vue'
import { useImageLightboxZoom } from '@/composables/useImageLightboxZoom.js'

const { proxy } = getCurrentInstance()

const props = defineProps({
  source: {
    type: [String, File],
  },
  width: {
    type: Number,
  },
  scale: {
    type: Number,
    default: 1,
  },
  fit: {
    type: String,
    default: 'cover',
  },
  preview: {
    type: Boolean,
    default: false,
  },
  borderRadius: {
    type: String,
    default: '5px',
  },
  lazy: {
    type: Boolean,
    default: true,
  },
  preImageList: {
    type: Array,
    default: [],
  },
})

const coverFile = ref()
const getCover = async () => {
  if (props.source == 'avatar.png') {
    coverFile.value = proxy.Utils.getLocalResource('avatar.png')
    return
  }
  if (typeof props.source == 'string') {
    const trimmed = props.source.trim()
    if (!trimmed) {
      coverFile.value = ''
      return
    }
    coverFile.value = proxy.Api.sourcePath + trimmed
  } else if (props.source instanceof File) {
    let img = new FileReader()
    img.readAsDataURL(props.source)
    img.onload = ({ target }) => {
      coverFile.value = target.result
    }
  }
}

watch(
  () => props.source,
  async (newSource) => {
    getCover(newSource)
  },
  { immediate: true }
)

const imageList = computed(() => {
  return props.preImageList.map((item) => {
    return proxy.Api.sourcePath + item.replace(proxy.imageThumbnailSuffix, '')
  })
})

const viewerIndex = ref(0)
const showViewer = ref(false)

const viewerUrl = computed(() => imageList.value[viewerIndex.value] ?? '')

const {
  imgStyle,
  resetTransform,
  onWheel,
  onTouchStart,
  onTouchMove,
  onTouchEnd,
  onMouseDown,
  onMouseMove,
  onMouseUp,
} = useImageLightboxZoom()

const closeViewer = () => {
  showViewer.value = false
  document.body.style.overflow = ''
  resetTransform()
}

watch(viewerUrl, () => resetTransform())

const showViewerPrev = () => {
  const n = imageList.value.length
  if (n <= 1) return
  viewerIndex.value = (viewerIndex.value - 1 + n) % n
}

const showViewerNext = () => {
  const n = imageList.value.length
  if (n <= 1) return
  viewerIndex.value = (viewerIndex.value + 1) % n
}

const onViewerKeydown = (e) => {
  if (!showViewer.value) return
  if (e.key === 'Escape') closeViewer()
  else if (e.key === 'ArrowLeft') showViewerPrev()
  else if (e.key === 'ArrowRight') showViewerNext()
}

const showViewerHandler = () => {
  if (props.preImageList.length == 0) {
    return
  }
  const idx = props.preImageList.findIndex((item) => item === props.source)
  viewerIndex.value = idx >= 0 ? idx : 0
  showViewer.value = true
  document.body.style.overflow = 'hidden'
}

const coverRef = ref()
const loadingHeight = ref()
onMounted(() => {
  loadingHeight.value = coverRef.value.clientWidth * props.scale
  window.addEventListener('keydown', onViewerKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onViewerKeydown)
  document.body.style.overflow = ''
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
