<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
    <!-- [zh] 开始标签 `<Transition>` -->
    <Transition name="cropper-fade">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="visible" class="image-editor-overlay" @click.self="cancel">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="image-editor-panel">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="editor-tabs">
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="tab-btn" :class="{ active: step === 'crop' }" @click="step = 'crop'">
              <!-- [zh] 模板内容：`裁剪` -->
              裁剪
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="tab-btn" :class="{ active: step === 'doodle' }" @click="goDoodle">
              <!-- [zh] 模板内容：`涂鸦` -->
              涂鸦
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>

          <!-- [zh] 开始标签 `<div>` -->
          <div v-show="step === 'crop'" class="crop-step">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="cropper-viewport rect">
              <!-- [zh] 开始标签 `<Cropper>` -->
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
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<p>` -->
            <p class="editor-hint">拖动裁剪框或缩放图片，完成后可涂鸦标注</p>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>

          <!-- [zh] 开始标签 `<div>` -->
          <div v-show="step === 'doodle'" class="doodle-step">
            <!-- [zh] 开始标签 `<div>` -->
            <div ref="doodleWrapRef" class="doodle-viewport">
              <!-- [zh] 开始标签 `<canvas>` -->
              <canvas ref="baseCanvasRef" class="doodle-base" />
              <!-- [zh] 开始标签 `<canvas>` -->
              <canvas
                ref="doodleCanvasRef"
                class="doodle-layer"
                @pointerdown="onPointerDown"
                @pointermove="onPointerMove"
                @pointerup="onPointerUp"
                @pointerleave="onPointerUp"
                @pointercancel="onPointerUp"
              />
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="doodle-tools">
              <!-- [zh] 开始标签 `<button>` -->
              <button
                v-for="c in brushColors"
                :key="c"
                type="button"
                class="color-dot"
                :class="{ active: brushColor === c }"
                :style="{ background: c }"
                @click="brushColor = c"
              />
              <!-- [zh] 开始标签 `<el-slider>` -->
              <el-slider v-model="brushSize" :min="2" :max="24" :show-tooltip="false" class="brush-slider" />
              <!-- [zh] 开始标签 `<el-button>` -->
              <el-button size="small" round @click="undoStroke">撤销</el-button>
              <!-- [zh] 开始标签 `<el-button>` -->
              <el-button size="small" round @click="clearDoodle">清除</el-button>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>

          <!-- [zh] 开始标签 `<div>` -->
          <div class="editor-actions">
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
import { nextTick, ref, shallowRef } from 'vue';
// [zh] 导入 `vue-advanced-cropper`
import { Cropper } from 'vue-advanced-cropper';
// [zh] 导入 `模块`
import 'vue-advanced-cropper/dist/style.css';
// [zh] 导入 `@/utils/imageUpload`
import { canvasToJpegBlob } from '@/utils/imageUpload';

// [zh] TypeScript 类型 `Step`（类比 Java interface/DTO）
type Step = 'crop' | 'doodle';

// [zh] 声明变量 `visible`
const visible = ref(false);
// [zh] 声明变量 `step`
const step = ref<Step>('crop');
// [zh] 声明变量 `imgSrc`
const imgSrc = ref('');
// [zh] 声明变量 `confirming`
const confirming = ref(false);
// [zh] 声明变量 `cropperRef`
const cropperRef = shallowRef<InstanceType<typeof Cropper> | null>(null);
// [zh] 声明变量 `doodleWrapRef`
const doodleWrapRef = ref<HTMLElement | null>(null);
// [zh] 声明变量 `baseCanvasRef`
const baseCanvasRef = ref<HTMLCanvasElement | null>(null);
// [zh] 声明变量 `doodleCanvasRef`
const doodleCanvasRef = ref<HTMLCanvasElement | null>(null);

// [zh] 声明变量 `brushColors`
const brushColors = ['#1d1d1f', '#ff3b30', '#ff9500', '#34c759', '#007aff', '#ffffff'];
// [zh] 声明变量 `brushColor`
const brushColor = ref('#ff3b30');
// [zh] 声明变量 `brushSize`
const brushSize = ref(6);

// [zh] 声明变量 `_resolve`
let _resolve: ((blob: Blob) => void) | null = null;
// [zh] 声明变量 `_reject`
let _reject: (() => void) | null = null;
// [zh] 声明变量 `settled`
let settled = false;
// [zh] 声明变量 `drawing`
let drawing = false;
// [zh] 声明变量 `strokes`
const strokes: ImageData[] = [];

// [zh] 声明变量 `open`
const open = (file: File): Promise<Blob> => {
  // [zh] return 返回
  return new Promise((resolve, reject) => {
    // [zh] 语句
    settled = false;
    // [zh] 语句
    step.value = 'crop';
    // [zh] 语句
    strokes.length = 0;
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
    // [zh] 语句
    reader.readAsDataURL(file);
  // [zh] 语句
  });
// [zh] 语句
};

// [zh] 声明变量 `getCroppedCanvas`
const getCroppedCanvas = (): HTMLCanvasElement | null => {
  // [zh] 声明变量 `result`
  const result = cropperRef.value?.getResult();
  // [zh] 声明变量 `canvas`
  const canvas = result?.canvas;
  // [zh] 条件判断 if
  if (!canvas || canvas.width === 0 || canvas.height === 0) return null;
  // [zh] return 返回
  return canvas;
// [zh] 语句
};

// [zh] 声明变量 `fitDoodleCanvases`
const fitDoodleCanvases = async (source: HTMLCanvasElement) => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextTick();
  // [zh] 声明变量 `wrap`
  const wrap = doodleWrapRef.value;
  // [zh] 声明变量 `base`
  const base = baseCanvasRef.value;
  // [zh] 声明变量 `doodle`
  const doodle = doodleCanvasRef.value;
  // [zh] 条件判断 if
  if (!wrap || !base || !doodle) return;

  // [zh] 声明变量 `maxW`
  const maxW = wrap.clientWidth;
  // [zh] 声明变量 `maxH`
  const maxH = 320;
  // [zh] 声明变量 `ratio`
  const ratio = source.width / source.height;
  // [zh] 声明变量 `w`
  let w = maxW;
  // [zh] 声明变量 `h`
  let h = w / ratio;
  // [zh] 条件判断 if
  if (h > maxH) {
    // [zh] 语句
    h = maxH;
    // [zh] 语句
    w = h * ratio;
  // [zh] 语法符号
  }

  // [zh] 语句
  base.width = Math.round(w);
  // [zh] 语句
  base.height = Math.round(h);
  // [zh] 语句
  doodle.width = Math.round(w);
  // [zh] 语句
  doodle.height = Math.round(h);

  // [zh] 声明变量 `ctx`
  const ctx = base.getContext('2d');
  // [zh] 语句
  ctx?.clearRect(0, 0, base.width, base.height);
  // [zh] 语句
  ctx?.drawImage(source, 0, 0, base.width, base.height);

  // [zh] 声明变量 `dctx`
  const dctx = doodle.getContext('2d');
  // [zh] 语句
  dctx?.clearRect(0, 0, doodle.width, doodle.height);
  // [zh] 语句
  strokes.length = 0;
// [zh] 语句
};

// [zh] 声明变量 `goDoodle`
const goDoodle = async () => {
  // [zh] 声明变量 `cropped`
  const cropped = getCroppedCanvas();
  // [zh] 条件判断 if
  if (!cropped) return;
  // [zh] 语句
  step.value = 'doodle';
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await fitDoodleCanvases(cropped);
// [zh] 语句
};

// [zh] 声明变量 `getDoodlePoint`
const getDoodlePoint = (e: PointerEvent) => {
  // [zh] 声明变量 `canvas`
  const canvas = doodleCanvasRef.value!;
  // [zh] 声明变量 `rect`
  const rect = canvas.getBoundingClientRect();
  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`x: ((e.clientX - rect.left) / rect.width) * canvas.widt`
    x: ((e.clientX - rect.left) / rect.width) * canvas.width,
    // [zh] TypeScript/JS 语句：`y: ((e.clientY - rect.top) / rect.height) * canvas.heig`
    y: ((e.clientY - rect.top) / rect.height) * canvas.height
  // [zh] 语句
  };
// [zh] 语句
};

// [zh] 声明变量 `onPointerDown`
const onPointerDown = (e: PointerEvent) => {
  // [zh] 声明变量 `canvas`
  const canvas = doodleCanvasRef.value;
  // [zh] 条件判断 if
  if (!canvas) return;
  // [zh] 语句
  drawing = true;
  // [zh] 语句
  canvas.setPointerCapture(e.pointerId);
  // [zh] 声明变量 `ctx`
  const ctx = canvas.getContext('2d');
  // [zh] 条件判断 if
  if (!ctx) return;
  // [zh] 语句
  strokes.push(ctx.getImageData(0, 0, canvas.width, canvas.height));
  // [zh] 声明变量 `p`
  const p = getDoodlePoint(e);
  // [zh] 语句
  ctx.strokeStyle = brushColor.value;
  // [zh] 语句
  ctx.lineWidth = brushSize.value;
  // [zh] 语句
  ctx.lineCap = 'round';
  // [zh] 语句
  ctx.lineJoin = 'round';
  // [zh] 语句
  ctx.beginPath();
  // [zh] 语句
  ctx.moveTo(p.x, p.y);
// [zh] 语句
};

// [zh] 声明变量 `onPointerMove`
const onPointerMove = (e: PointerEvent) => {
  // [zh] 条件判断 if
  if (!drawing) return;
  // [zh] 声明变量 `canvas`
  const canvas = doodleCanvasRef.value;
  // [zh] 声明变量 `ctx`
  const ctx = canvas?.getContext('2d');
  // [zh] 条件判断 if
  if (!ctx) return;
  // [zh] 声明变量 `p`
  const p = getDoodlePoint(e);
  // [zh] 语句
  ctx.lineTo(p.x, p.y);
  // [zh] 语句
  ctx.stroke();
// [zh] 语句
};

// [zh] 声明变量 `onPointerUp`
const onPointerUp = (e: PointerEvent) => {
  // [zh] 条件判断 if
  if (!drawing) return;
  // [zh] 语句
  drawing = false;
  // [zh] 语句
  doodleCanvasRef.value?.releasePointerCapture(e.pointerId);
// [zh] 语句
};

// [zh] 声明变量 `undoStroke`
const undoStroke = () => {
  // [zh] 声明变量 `canvas`
  const canvas = doodleCanvasRef.value;
  // [zh] 声明变量 `ctx`
  const ctx = canvas?.getContext('2d');
  // [zh] 声明变量 `prev`
  const prev = strokes.pop();
  // [zh] 条件判断 if
  if (!canvas || !ctx || !prev) return;
  // [zh] 语句
  ctx.putImageData(prev, 0, 0);
// [zh] 语句
};

// [zh] 声明变量 `clearDoodle`
const clearDoodle = () => {
  // [zh] 声明变量 `canvas`
  const canvas = doodleCanvasRef.value;
  // [zh] 声明变量 `ctx`
  const ctx = canvas?.getContext('2d');
  // [zh] 条件判断 if
  if (!canvas || !ctx) return;
  // [zh] 语句
  strokes.push(ctx.getImageData(0, 0, canvas.width, canvas.height));
  // [zh] 语句
  ctx.clearRect(0, 0, canvas.width, canvas.height);
// [zh] 语句
};

// [zh] 声明变量 `exportBlob`
const exportBlob = async (): Promise<Blob | null> => {
  // [zh] 声明变量 `source`
  let source = getCroppedCanvas();
  // [zh] 条件判断 if
  if (!source) return null;

  // [zh] 条件判断 if
  if (step.value === 'doodle' && baseCanvasRef.value && doodleCanvasRef.value) {
    // [zh] 声明变量 `out`
    const out = document.createElement('canvas');
    // [zh] 语句
    out.width = baseCanvasRef.value.width;
    // [zh] 语句
    out.height = baseCanvasRef.value.height;
    // [zh] 声明变量 `ctx`
    const ctx = out.getContext('2d');
    // [zh] 条件判断 if
    if (!ctx) return null;
    // [zh] 语句
    ctx.drawImage(baseCanvasRef.value, 0, 0);
    // [zh] 语句
    ctx.drawImage(doodleCanvasRef.value, 0, 0);
    // [zh] 语句
    source = out;
  // [zh] 语法符号
  }

  // [zh] 声明变量 `maxEdge`
  const maxEdge = 2048;
  // [zh] 声明变量 `w`
  let w = source.width;
  // [zh] 声明变量 `h`
  let h = source.height;
  // [zh] 声明变量 `scale`
  const scale = Math.min(1, maxEdge / Math.max(w, h, 1));
  // [zh] 语句
  w = Math.max(1, Math.round(w * scale));
  // [zh] 语句
  h = Math.max(1, Math.round(h * scale));

  // [zh] 声明变量 `outCanvas`
  const outCanvas = document.createElement('canvas');
  // [zh] 语句
  outCanvas.width = w;
  // [zh] 语句
  outCanvas.height = h;
  // [zh] 声明变量 `ctx`
  const ctx = outCanvas.getContext('2d');
  // [zh] 条件判断 if
  if (!ctx) return null;
  // [zh] 语句
  ctx.drawImage(source, 0, 0, w, h);

  // [zh] return 返回
  return canvasToJpegBlob(outCanvas, 0.9);
// [zh] 语句
};

// [zh] 声明变量 `onConfirm`
const onConfirm = async () => {
  // [zh] 条件判断 if
  if (settled) return;
  // [zh] 语句
  confirming.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `blob`
    const blob = await exportBlob();
    // [zh] 条件判断 if
    if (!blob || blob.size === 0) {
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
    settled = true;
    // [zh] 语句
    visible.value = false;
    // [zh] 语句
    _resolve?.(blob);
    // [zh] 语句
    cleanup();
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
