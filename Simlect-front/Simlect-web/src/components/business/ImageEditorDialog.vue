<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
    <!-- [zh] 开始标签 `<Transition>` -->
    <Transition name="cropper-fade">
      <div v-if="visible" class="image-editor-overlay" @click.self="cancel">
        <div class="image-editor-panel">
          <div class="editor-tabs">
            <button type="button" class="tab-btn" :class="{ active: step === 'crop' }" @click="step = 'crop'">
              裁剪
            </button>
            <button type="button" class="tab-btn" :class="{ active: step === 'doodle' }" @click="goDoodle">
              涂鸦
            </button>
          </div>

          <div v-show="step === 'crop'" class="crop-step">
            <div class="cropper-viewport rect">
              <Cropper
                ref="cropperRef"
                class="cropper-area"
                :src="imgSrc"
                :stencil-props="{ movable: true, resizable: true }"
                image-restriction="stencil"
                :min-zoom="0.2"
                :max-zoom="4"
                background-class="cropper-bg"
              />
            </div>
            <p class="editor-hint">拖动裁剪框或缩放图片，完成后可涂鸦标注</p>
          </div>

          <div v-show="step === 'doodle'" class="doodle-step">
            <div ref="doodleWrapRef" class="doodle-viewport">
              <canvas ref="baseCanvasRef" class="doodle-base" />
              <canvas
                ref="doodleCanvasRef"
                class="doodle-layer"
                @pointerdown="onPointerDown"
                @pointermove="onPointerMove"
                @pointerup="onPointerUp"
                @pointerleave="onPointerUp"
                @pointercancel="onPointerUp"
              />
            </div>
            <div class="doodle-tools">
              <button
                v-for="c in brushColors"
                :key="c"
                type="button"
                class="color-dot"
                :class="{ active: brushColor === c }"
                :style="{ background: c }"
                @click="brushColor = c"
              />
              <el-slider v-model="brushSize" :min="2" :max="24" :show-tooltip="false" class="brush-slider" />
              <el-button size="small" round @click="undoStroke">撤销</el-button>
              <el-button size="small" round @click="clearDoodle">清除</el-button>
            </div>
          </div>

          <div class="editor-actions">
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
import { nextTick, ref, shallowRef } from 'vue';
import { Cropper } from 'vue-advanced-cropper';
import 'vue-advanced-cropper/dist/style.css';
import { canvasToJpegBlob } from '@/utils/imageUpload';

type Step = 'crop' | 'doodle';

const visible = ref(false);
const step = ref<Step>('crop');
const imgSrc = ref('');
const confirming = ref(false);
const cropperRef = shallowRef<InstanceType<typeof Cropper> | null>(null);
const doodleWrapRef = ref<HTMLElement | null>(null);
const baseCanvasRef = ref<HTMLCanvasElement | null>(null);
const doodleCanvasRef = ref<HTMLCanvasElement | null>(null);

const brushColors = ['#1d1d1f', '#ff3b30', '#ff9500', '#34c759', '#007aff', '#ffffff'];
const brushColor = ref('#ff3b30');
const brushSize = ref(6);

let _resolve: ((blob: Blob) => void) | null = null;
let _reject: (() => void) | null = null;
let settled = false;
let drawing = false;
const strokes: ImageData[] = [];

const open = (file: File): Promise<Blob> => {
  return new Promise((resolve, reject) => {
    settled = false;
    step.value = 'crop';
    strokes.length = 0;
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
    reader.readAsDataURL(file);
  });
};

const getCroppedCanvas = (): HTMLCanvasElement | null => {
  const result = cropperRef.value?.getResult();
  const canvas = result?.canvas;
  if (!canvas || canvas.width === 0 || canvas.height === 0) return null;
  return canvas;
};

const fitDoodleCanvases = async (source: HTMLCanvasElement) => {
  await nextTick();
  const wrap = doodleWrapRef.value;
  const base = baseCanvasRef.value;
  const doodle = doodleCanvasRef.value;
  if (!wrap || !base || !doodle) return;

  const maxW = wrap.clientWidth;
  const maxH = 320;
  const ratio = source.width / source.height;
  let w = maxW;
  let h = w / ratio;
  if (h > maxH) {
    h = maxH;
    w = h * ratio;
  }

  base.width = Math.round(w);
  base.height = Math.round(h);
  doodle.width = Math.round(w);
  doodle.height = Math.round(h);

  const ctx = base.getContext('2d');
  ctx?.clearRect(0, 0, base.width, base.height);
  ctx?.drawImage(source, 0, 0, base.width, base.height);

  const dctx = doodle.getContext('2d');
  dctx?.clearRect(0, 0, doodle.width, doodle.height);
  strokes.length = 0;
};

const goDoodle = async () => {
  const cropped = getCroppedCanvas();
  if (!cropped) return;
  step.value = 'doodle';
  await fitDoodleCanvases(cropped);
};

const getDoodlePoint = (e: PointerEvent) => {
  const canvas = doodleCanvasRef.value!;
  const rect = canvas.getBoundingClientRect();
  return {
    x: ((e.clientX - rect.left) / rect.width) * canvas.width,
    y: ((e.clientY - rect.top) / rect.height) * canvas.height
  };
};

const onPointerDown = (e: PointerEvent) => {
  const canvas = doodleCanvasRef.value;
  if (!canvas) return;
  drawing = true;
  canvas.setPointerCapture(e.pointerId);
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  strokes.push(ctx.getImageData(0, 0, canvas.width, canvas.height));
  const p = getDoodlePoint(e);
  ctx.strokeStyle = brushColor.value;
  ctx.lineWidth = brushSize.value;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.beginPath();
  ctx.moveTo(p.x, p.y);
};

const onPointerMove = (e: PointerEvent) => {
  if (!drawing) return;
  const canvas = doodleCanvasRef.value;
  const ctx = canvas?.getContext('2d');
  if (!ctx) return;
  const p = getDoodlePoint(e);
  ctx.lineTo(p.x, p.y);
  ctx.stroke();
};

const onPointerUp = (e: PointerEvent) => {
  if (!drawing) return;
  drawing = false;
  doodleCanvasRef.value?.releasePointerCapture(e.pointerId);
};

const undoStroke = () => {
  const canvas = doodleCanvasRef.value;
  const ctx = canvas?.getContext('2d');
  const prev = strokes.pop();
  if (!canvas || !ctx || !prev) return;
  ctx.putImageData(prev, 0, 0);
};

const clearDoodle = () => {
  const canvas = doodleCanvasRef.value;
  const ctx = canvas?.getContext('2d');
  if (!canvas || !ctx) return;
  strokes.push(ctx.getImageData(0, 0, canvas.width, canvas.height));
  ctx.clearRect(0, 0, canvas.width, canvas.height);
};

const exportBlob = async (): Promise<Blob | null> => {
  let source = getCroppedCanvas();
  if (!source) return null;

  if (step.value === 'doodle' && baseCanvasRef.value && doodleCanvasRef.value) {
    const out = document.createElement('canvas');
    out.width = baseCanvasRef.value.width;
    out.height = baseCanvasRef.value.height;
    const ctx = out.getContext('2d');
    if (!ctx) return null;
    ctx.drawImage(baseCanvasRef.value, 0, 0);
    ctx.drawImage(doodleCanvasRef.value, 0, 0);
    source = out;
  }

  const maxEdge = 2048;
  let w = source.width;
  let h = source.height;
  const scale = Math.min(1, maxEdge / Math.max(w, h, 1));
  w = Math.max(1, Math.round(w * scale));
  h = Math.max(1, Math.round(h * scale));

  const outCanvas = document.createElement('canvas');
  outCanvas.width = w;
  outCanvas.height = h;
  const ctx = outCanvas.getContext('2d');
  if (!ctx) return null;
  ctx.drawImage(source, 0, 0, w, h);

  return canvasToJpegBlob(outCanvas, 0.9);
};

const onConfirm = async () => {
  if (settled) return;
  confirming.value = true;
  try {
    const blob = await exportBlob();
    if (!blob || blob.size === 0) {
      settled = true;
      visible.value = false;
      _reject?.();
      cleanup();
      return;
    }
    settled = true;
    visible.value = false;
    _resolve?.(blob);
    cleanup();
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
/* [zh] 样式规则 `.image-editor-overlay {` */
.image-editor-overlay {
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
  /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.72);` */
  background: rgba(0, 0, 0, 0.72);
  /* [zh] 样式规则 `backdrop-filter: blur(4px);` */
  backdrop-filter: blur(4px);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.image-editor-panel {` */
.image-editor-panel {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `width: min(94vw, 400px);` */
  width: min(94vw, 400px);
  /* [zh] 样式规则 `padding: 16px 16px 14px;` */
  padding: 16px 16px 14px;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `border-radius: 20px;` */
  border-radius: 20px;
  /* [zh] 样式规则 `box-shadow: 0 12px 40px rgba(0, 0, 0, 0.` */
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-tabs {` */
.editor-tabs {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 4px;` */
  padding: 4px;
  /* [zh] 样式规则 `background: #f2f2f7;` */
  background: #f2f2f7;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tab-btn {` */
.tab-btn {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 10px;` */
  border-radius: 10px;
  /* [zh] 样式规则 `padding: 8px 0;` */
  padding: 8px 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `color: #636366;` */
  color: #636366;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `background: #fff;` */
    background: #fff;
    /* [zh] 样式规则 `color: #1d1d1f;` */
    color: #1d1d1f;
    /* [zh] 样式规则 `box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08` */
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cropper-viewport.rect {` */
.cropper-viewport.rect {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: min(52vw, 300px);` */
  height: min(52vw, 300px);
  /* [zh] 样式规则 `border-radius: 14px;` */
  border-radius: 14px;
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

/* [zh] 样式规则 `.editor-hint {` */
.editor-hint {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: #86868b;` */
  color: #86868b;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.doodle-viewport {` */
.doodle-viewport {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `min-height: 200px;` */
  min-height: 200px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `background: #1a1a1a;` */
  background: #1a1a1a;
  /* [zh] 样式规则 `border-radius: 14px;` */
  border-radius: 14px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.doodle-base,
/* [zh] 样式规则 `.doodle-layer {` */
.doodle-layer {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `max-width: 100%;` */
  max-width: 100%;
  /* [zh] 样式规则 `touch-action: none;` */
  touch-action: none;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.doodle-layer {` */
.doodle-layer {
  /* [zh] 样式规则 `cursor: crosshair;` */
  cursor: crosshair;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.doodle-tools {` */
.doodle-tools {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.color-dot {` */
.color-dot {
  /* [zh] 样式规则 `width: 22px;` */
  width: 22px;
  /* [zh] 样式规则 `height: 22px;` */
  height: 22px;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `border: 2px solid transparent;` */
  border: 2px solid transparent;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `border-color: #1d1d1f;` */
    border-color: #1d1d1f;
    /* [zh] 样式规则 `transform: scale(1.08);` */
    transform: scale(1.08);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.brush-slider {` */
.brush-slider {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 80px;` */
  min-width: 80px;
  /* [zh] 样式规则 `margin: 0 4px;` */
  margin: 0 4px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-actions {` */
.editor-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;

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
