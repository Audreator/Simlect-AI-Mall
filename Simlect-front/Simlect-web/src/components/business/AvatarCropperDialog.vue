<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
    <!-- [zh] 开始标签 `<Transition>` -->
    <Transition name="cropper-fade">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="visible" class="avatar-cropper-overlay" @click.self="cancel">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="avatar-cropper-panel">
          <!-- [zh] 开始标签 `<p>` -->
          <p class="cropper-title">移动和缩放图片</p>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="cropper-viewport">
            <!-- [zh] 开始标签 `<Cropper>` -->
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
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="cropper-hint">拖动图片调整位置，双指或滚轮缩放</p>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="cropper-actions">
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button round @click="cancel">取消</el-button>
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button type="primary" round :loading="confirming" @click="onConfirm">确定</el-button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</Transition>` -->
    </Transition>
  <!-- [zh] 闭合标签 `</Teleport>` -->
  </Teleport>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { ref, shallowRef } from 'vue';
// [zh] 导入 `vue-advanced-cropper`
import { Cropper, CircleStencil } from 'vue-advanced-cropper';
// [zh] 导入 `模块`
import 'vue-advanced-cropper/dist/style.css';

// [zh] 声明变量 `visible`
const visible = ref(false);
// [zh] 声明变量 `imgSrc`
const imgSrc = ref('');
// [zh] 声明变量 `confirming`
const confirming = ref(false);
// [zh] 声明变量 `cropperRef`
const cropperRef = shallowRef<InstanceType<typeof Cropper> | null>(null);

// [zh] 声明变量 `_resolve`
let _resolve: ((blob: Blob) => void) | null = null;
// [zh] 声明变量 `_reject`
let _reject: (() => void) | null = null;
// [zh] 声明变量 `settled`
let settled = false;

// [zh] 声明变量 `open`
const open = (file: File): Promise<Blob> => {
  // [zh] return 返回
  return new Promise((resolve, reject) => {
    // [zh] 语句
    settled = false;
    // [zh] 语句
    _resolve = resolve;
    // [zh] 语句
    _reject = reject;
    // [zh] 声明变量 `reader`
    const reader = new FileReader();
    // [zh] 代码块开始
    reader.onload = (e) => {
      // [zh] 声明变量 `result`
      const result = e.target?.result as string;
      // [zh] 条件判断 if
      if (!result || result.length < 100) {
        // [zh] 语句
        settled = true;
        // [zh] 语句
        reject(new Error('文件读取不完整'));
        // [zh] 语句
        return;
      // [zh] 语法符号
      }
      // [zh] 语句
      imgSrc.value = result;
      // [zh] 语句
      visible.value = true;
    // [zh] 语句
    };
    // [zh] 代码块开始
    reader.onerror = () => {
      // [zh] 语句
      settled = true;
      // [zh] 语句
      reject(new Error('读取文件失败'));
    // [zh] 语句
    };
    // [zh] 代码块开始
    reader.onabort = () => {
      // [zh] 语句
      settled = true;
      // [zh] 语句
      reject(new Error('文件读取被中断'));
    // [zh] 语句
    };
    // [zh] 语句
    reader.readAsDataURL(file);
  // [zh] 语句
  });
// [zh] 语句
};

// [zh] 声明变量 `onConfirm`
const onConfirm = () => {
  // [zh] 条件判断 if
  if (!cropperRef.value || settled) return;
  // [zh] 语句
  confirming.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `result`
    const result = cropperRef.value.getResult();
    // [zh] 声明变量 `srcCanvas`
    const srcCanvas = result?.canvas;
    // [zh] 条件判断 if
    if (!srcCanvas || srcCanvas.width === 0 || srcCanvas.height === 0) {
      // [zh] 语句
      settled = true;
      // [zh] 语句
      visible.value = false;
      // [zh] 语句
      _reject?.();
      // [zh] 语句
      cleanup();
      // [zh] 语句
      return;
    // [zh] 语法符号
    }

    // [zh] 声明变量 `SIZE`
    const SIZE = 256;
    // [zh] 声明变量 `outCanvas`
    const outCanvas = document.createElement('canvas');
    // [zh] 语句
    outCanvas.width = SIZE;
    // [zh] 语句
    outCanvas.height = SIZE;
    // [zh] 声明变量 `ctx`
    const ctx = outCanvas.getContext('2d');
    // [zh] 条件判断 if
    if (!ctx) {
      // [zh] 语句
      settled = true;
      // [zh] 语句
      visible.value = false;
      // [zh] 语句
      _reject?.();
      // [zh] 语句
      cleanup();
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 语句
    ctx.drawImage(srcCanvas, 0, 0, SIZE, SIZE);

    // [zh] 代码块开始
    try {

      // [zh] 声明变量 `dataUrl`
      const dataUrl = outCanvas.toDataURL('image/png');
      // [zh] 声明变量 `parts`
      const parts = dataUrl.split(',');
      // [zh] 条件判断 if
      if (parts.length !== 2 || !parts[1]) {
        // [zh] 语句
        throw new Error('dataUrl 为空');
      // [zh] 语法符号
      }
      // [zh] 声明变量 `byteString`
      const byteString = atob(parts[1]);
      // [zh] 条件判断 if
      if (!byteString || byteString.length === 0) {
        // [zh] 语句
        throw new Error('解码后数据为空');
      // [zh] 语法符号
      }
      // [zh] 声明变量 `ab`
      const ab = new ArrayBuffer(byteString.length);
      // [zh] 声明变量 `ia`
      const ia = new Uint8Array(ab);
      // [zh] for 循环
      for (let i = 0; i < byteString.length; i++) {
        // [zh] 语句
        ia[i] = byteString.charCodeAt(i);
      // [zh] 语法符号
      }
      // [zh] 声明变量 `blob`
      const blob = new Blob([ab], { type: 'image/png' });
      // [zh] 条件判断 if
      if (blob.size === 0) {
        // [zh] 语句
        throw new Error('生成的 blob 为空');
      // [zh] 语法符号
      }
      // [zh] 语句
      settled = true;
      // [zh] 语句
      visible.value = false;
      // [zh] 语句
      _resolve?.(blob);
    // [zh] 代码块开始
    } catch (e) {
      // [zh] 语句
      settled = true;
      // [zh] 语句
      visible.value = false;
      // [zh] 语句
      _reject?.();
      // [zh] 语句
      cleanup();
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    confirming.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `cancel`
const cancel = () => {
  // [zh] 条件判断 if
  if (settled) return;
  // [zh] 语句
  settled = true;
  // [zh] 语句
  visible.value = false;
  // [zh] 语句
  _reject?.();
  // [zh] 语句
  cleanup();
// [zh] 语句
};

// [zh] 声明变量 `cleanup`
const cleanup = () => {
  // [zh] 语句
  imgSrc.value = '';
  // [zh] 语句
  _resolve = null;
  // [zh] 语句
  _reject = null;
// [zh] 语句
};

// [zh] 语句
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
