<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
    <!-- [zh] 开始标签 `<Transition>` -->
    <Transition name="cropper-fade">
      <div v-if="visible" class="avatar-cropper-overlay" @click.self="cancel">
        <div class="avatar-cropper-panel">
          <p class="cropper-title">移动和缩放图片</p>
          <div class="cropper-viewport">
            <Cropper
              ref="cropperRef"
              class="cropper-area"
              :src="imgSrc"
              :stencil-props="{ aspectRatio: 1, movable: false, resizable: false }"
              :stencil-component="CircleStencil"
              image-restriction="stencil"
              :min-zoom="0.3"
              :max-zoom="3"
              background-class="cropper-bg"
            />
          </div>
          <p class="cropper-hint">拖动图片调整位置，双指或滚轮缩放</p>
          <div class="cropper-actions">
            <el-button round @click="cancel">取消</el-button>
            <el-button type="primary" round :loading="confirming" @click="onConfirm">确定</el-button>
          </div>
        </div>
      </div>
    </Transition>
  <!-- [zh] 闭合标签 `</Teleport>` -->
  </Teleport>
</template>

<script setup lang="ts">
import { ref, shallowRef } from 'vue';
import { Cropper, CircleStencil } from 'vue-advanced-cropper';
import 'vue-advanced-cropper/dist/style.css';

const visible = ref(false);
const imgSrc = ref('');
const confirming = ref(false);
const cropperRef = shallowRef<InstanceType<typeof Cropper> | null>(null);

let _resolve: ((blob: Blob) => void) | null = null;
let _reject: (() => void) | null = null;
let settled = false;

const open = (file: File): Promise<Blob> => {
  return new Promise((resolve, reject) => {
    settled = false;
    _resolve = resolve;
    _reject = reject;
    const reader = new FileReader();
    reader.onload = (e) => {
      const result = e.target?.result as string;
      if (!result || result.length < 100) {
        settled = true;
        reject(new Error('文件读取不完整'));
        return;
      }
      imgSrc.value = result;
      visible.value = true;
    };
    reader.onerror = () => {
      settled = true;
      reject(new Error('读取文件失败'));
    };
    reader.onabort = () => {
      settled = true;
      reject(new Error('文件读取被中断'));
    };
    reader.readAsDataURL(file);
  });
};

const onConfirm = () => {
  if (!cropperRef.value || settled) return;
  confirming.value = true;
  try {
    const result = cropperRef.value.getResult();
    const srcCanvas = result?.canvas;
    if (!srcCanvas || srcCanvas.width === 0 || srcCanvas.height === 0) {
      settled = true;
      visible.value = false;
      _reject?.();
      cleanup();
      return;
    }

    const SIZE = 256;
    const outCanvas = document.createElement('canvas');
    outCanvas.width = SIZE;
    outCanvas.height = SIZE;
    const ctx = outCanvas.getContext('2d');
    if (!ctx) {
      settled = true;
      visible.value = false;
      _reject?.();
      cleanup();
      return;
    }
    ctx.drawImage(srcCanvas, 0, 0, SIZE, SIZE);

    try {

      const dataUrl = outCanvas.toDataURL('image/png');
      const parts = dataUrl.split(',');
      if (parts.length !== 2 || !parts[1]) {
        throw new Error('dataUrl 为空');
      }
      const byteString = atob(parts[1]);
      if (!byteString || byteString.length === 0) {
        throw new Error('解码后数据为空');
      }
      const ab = new ArrayBuffer(byteString.length);
      const ia = new Uint8Array(ab);
      for (let i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i);
      }
      const blob = new Blob([ab], { type: 'image/png' });
      if (blob.size === 0) {
        throw new Error('生成的 blob 为空');
      }
      settled = true;
      visible.value = false;
      _resolve?.(blob);
    } catch (e) {
      settled = true;
      visible.value = false;
      _reject?.();
      cleanup();
    }
  } finally {
    confirming.value = false;
  }
};

const cancel = () => {
  if (settled) return;
  settled = true;
  visible.value = false;
  _reject?.();
  cleanup();
};

const cleanup = () => {
  imgSrc.value = '';
  _resolve = null;
  _reject = null;
};

defineExpose({ open });
</script>

/* [zh] 样式声明 */
<style lang="scss">
/* [zh] 样式规则 `.avatar-cropper-overlay {` */
.avatar-cropper-overlay {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `z-index: 3000;` */
  z-index: 3000;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.7);` */
  background: rgba(0, 0, 0, 0.7);
  /* [zh] 样式规则 `backdrop-filter: blur(4px);` */
  backdrop-filter: blur(4px);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.avatar-cropper-panel {` */
.avatar-cropper-panel {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
  /* [zh] 样式规则 `width: min(92vw, 360px);` */
  width: min(92vw, 360px);
  /* [zh] 样式规则 `padding: 24px 20px 20px;` */
  padding: 24px 20px 20px;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `border-radius: 20px;` */
  border-radius: 20px;
  /* [zh] 样式规则 `box-shadow: 0 12px 40px rgba(0, 0, 0, 0.` */
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cropper-title {` */
.cropper-title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 16px;` */
  font-size: 16px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: #1d1d1f;` */
  color: #1d1d1f;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cropper-viewport {` */
.cropper-viewport {
  /* [zh] 样式规则 `width: 280px;` */
  width: 280px;
  /* [zh] 样式规则 `height: 280px;` */
  height: 280px;
  /* [zh] 样式规则 `border-radius: 16px;` */
  border-radius: 16px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: #1a1a1a;` */
  background: #1a1a1a;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cropper-area {` */
.cropper-area {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cropper-bg {` */
.cropper-bg {
  /* [zh] 样式规则 `background: #1a1a1a;` */
  background: #1a1a1a;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cropper-hint {` */
.cropper-hint {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: #86868b;` */
  color: #86868b;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cropper-actions {` */
.cropper-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;

  /* [zh] 样式规则 `.el-button {` */
  .el-button {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.cropper-fade-enter-active,
/* [zh] 样式规则 `.cropper-fade-leave-active {` */
.cropper-fade-leave-active {
  /* [zh] 样式规则 `transition: opacity 0.25s ease;` */
  transition: opacity 0.25s ease;
/* [zh] 样式规则 `}` */
}
/* [zh] 样式声明 */
.cropper-fade-enter-from,
/* [zh] 样式规则 `.cropper-fade-leave-to {` */
.cropper-fade-leave-to {
  /* [zh] 样式规则 `opacity: 0;` */
  opacity: 0;
/* [zh] 样式规则 `}` */
}
</style>
