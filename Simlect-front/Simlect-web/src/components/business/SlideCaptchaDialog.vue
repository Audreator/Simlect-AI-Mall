<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
    <!-- [zh] 开始标签 `<Transition>` -->
    <Transition name="slide-captcha-fade">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="visible" class="slide-captcha-overlay" @click.self="cancel">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="slide-captcha-panel" role="dialog" aria-modal="true" aria-label="安全验证">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="panel-glow" aria-hidden="true" />

          <!-- [zh] 开始标签 `<header>` -->
          <header class="panel-header">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="header-icon">
              <!-- [zh] 开始标签 `<svg>` -->
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <!-- [zh] 开始标签 `<path>` -->
                <path
                  d="M12 2l7 4v6c0 5-3 9-7 10C8 21 5 17 5 12V6l7-4z"
                  stroke-linejoin="round"
                />
                <!-- [zh] 开始标签 `<path>` -->
                <path d="M9.5 12.5l1.8 1.8 3.5-4.2" stroke-linecap="round" stroke-linejoin="round" />
              <!-- [zh] 闭合标签 `</svg>` -->
              </svg>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="header-text">
              <!-- [zh] 开始标签 `<h3>` -->
              <h3>安全验证</h3>
              <!-- [zh] 开始标签 `<p>` -->
              <p>拖动滑块完成拼图</p>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="btn-close" aria-label="关闭" @click="cancel">
              <!-- [zh] 开始标签 `<svg>` -->
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <!-- [zh] 开始标签 `<path>` -->
                <path d="M6 6l12 12M18 6L6 18" stroke-linecap="round" />
              <!-- [zh] 闭合标签 `</svg>` -->
              </svg>
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
          <!-- [zh] 闭合标签 `</header>` -->
          </header>

          <!-- [zh] 开始标签 `<div>` -->
          <div class="panel-body">
            <!-- [zh] 开始标签 `<div>` -->
            <div
              ref="imageWrapRef"
              class="image-wrap"
              :class="{ success: status === 'success', fail: status === 'fail', dragging }"
              @pointerdown="onDragStart"
              @touchstart="onTouchStart"
            >
              <!-- [zh] 开始标签 `<div>` -->
              <div v-if="loading" class="image-skeleton">
                <!-- [zh] 开始标签 `<span>` -->
                <span class="skeleton-shimmer" />
                <!-- [zh] 开始标签 `<p>` -->
                <p>加载验证图…</p>
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
              <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
              <template v-else>
                <!-- [zh] 开始标签 `<img>` -->
                <img v-if="bgSrc" :src="bgSrc" class="bg-img" alt="" draggable="false" />
                <!-- [zh] 开始标签 `<img>` -->
                <img
                  v-if="blockSrc"
                  :src="blockSrc"
                  class="block-img"
                  :style="{ transform: `translateX(${moveX}px)` }"
                  alt=""
                  draggable="false"
                />
                <!-- [zh] 开始标签 `<div>` -->
                <div v-if="status === 'success'" class="status-badge success-badge">
                  <!-- [zh] 开始标签 `<svg>` -->
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                    <!-- [zh] 开始标签 `<path>` -->
                    <path d="M5 12.5l4.2 4.3L19 7" stroke-linecap="round" stroke-linejoin="round" />
                  <!-- [zh] 闭合标签 `</svg>` -->
                  </svg>
                  <!-- [zh] 模板内容：`验证通过` -->
                  验证通过
                <!-- [zh] 闭合标签 `</div>` -->
                </div>
              </template>
              <button
                type="button"
                class="btn-refresh"
                :disabled="loading || verifying"
                title="换一张"
                @click="loadCaptcha"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 12a8 8 0 0 1 13.7-5.7M20 12a8 8 0 0 1-13.7 5.7" stroke-linecap="round" />
                  <path d="M16 4h4V0M4 20h4v4" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </button>
            </div>

            <div
              ref="sliderWrapRef"
              class="slider-wrap"
              @pointerdown="onDragStart"
              @touchstart="onTouchStart"
            >
              <div class="slider-track">
                <div class="slider-fill" :style="{ width: `${Math.max(0, moveX + knobSize / 2)}px` }" />
                <span class="slider-hint" :class="{ hidden: dragging || moveX > 4 }">
                  {{ hintText }}
                </span>
              </div>
              <div
                ref="knobRef"
                class="slider-knob"
                :class="{ dragging, success: status === 'success', fail: status === 'fail' }"
                :style="{ transform: `translateX(${moveX}px)` }"
              >
                <svg v-if="status === 'success'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <path d="M5 12.5l4.2 4.3L19 7" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                <svg v-else-if="status === 'fail'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <path d="M6 6l12 12M18 6L6 18" stroke-linecap="round" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M5 12h14M13 6l6 6-6 6" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </div>
            </div>

            <p v-if="tip" class="tip" :class="status">{{ tip }}</p>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, nextTick, onBeforeUnmount, ref } from 'vue';
// [zh] 导入 `@/utils/slideCaptcha`
import { checkSlideCaptcha, cancelSlideCaptchaToken, fetchSlideCaptcha, toCaptchaX, type CaptchaGetData } from '@/utils/slideCaptcha';

// [zh] TypeScript 类型 `Status`（类比 Java interface/DTO）
type Status = 'idle' | 'success' | 'fail';

// [zh] 声明变量 `visible`
const visible = ref(false);
// [zh] 声明变量 `loading`
const loading = ref(false);
// [zh] 声明变量 `verifying`
const verifying = ref(false);
// [zh] 声明变量 `dragging`
const dragging = ref(false);
// [zh] 声明变量 `moveX`
const moveX = ref(0);
// [zh] 声明变量 `status`
const status = ref<Status>('idle');
// [zh] 声明变量 `tip`
const tip = ref('');
// [zh] 声明变量 `hintText`
const hintText = computed(() => (status.value === 'fail' ? '请重新拖动滑块' : '向右拖动滑块完成拼图'));

// [zh] 声明变量 `imageWrapRef`
const imageWrapRef = ref<HTMLElement | null>(null);
// [zh] 声明变量 `sliderWrapRef`
const sliderWrapRef = ref<HTMLElement | null>(null);
// [zh] 声明变量 `knobRef`
const knobRef = ref<HTMLElement | null>(null);
// [zh] 声明变量 `knobSize`
const knobSize = 46;

// [zh] 声明变量 `bgSrc`
const bgSrc = ref('');
// [zh] 声明变量 `blockSrc`
const blockSrc = ref('');
// [zh] 声明变量 `captchaMeta`
const captchaMeta = ref<CaptchaGetData | null>(null);
// [zh] 声明变量 `startTime`
const startTime = ref(0);
// [zh] 声明变量 `trackWidth`
const trackWidth = ref(0);

// [zh] 声明变量 `resolveFn`
let resolveFn: ((v: string) => void) | null = null;
// [zh] 声明变量 `rejectFn`
let rejectFn: (() => void) | null = null;
// [zh] 声明变量 `startClientX`
let startClientX = 0;
// [zh] 声明变量 `startMoveX`
let startMoveX = 0;
// [zh] 声明变量 `settled`
let settled = false;
// [zh] 声明变量 `activePointerId`
let activePointerId: number | null = null;
// [zh] 声明变量 `touchDragActive`
let touchDragActive = false;

// [zh] 声明变量 `toImgSrc`
const toImgSrc = (b64: string) => (b64.startsWith('data:') ? b64 : `data:image/png;base64,${b64}`);

// [zh] 声明变量 `resetState`
const resetState = () => {
  // [zh] 语句
  moveX.value = 0;
  // [zh] 语句
  status.value = 'idle';
  // [zh] 语句
  tip.value = '';
  // [zh] 语句
  dragging.value = false;
  // [zh] 语句
  verifying.value = false;
// [zh] 语句
};

// [zh] 声明变量 `discardCurrentToken`
const discardCurrentToken = () => {
  // [zh] 声明变量 `token`
  const token = captchaMeta.value?.token;
  // [zh] 条件判断 if
  if (token) {
    // [zh] 语句
    cancelSlideCaptchaToken(token);
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `loadCaptcha`
const loadCaptcha = async () => {
  // [zh] 语句
  discardCurrentToken();
  // [zh] 语句
  loading.value = true;
  // [zh] 语句
  resetState();
  // [zh] 语句
  bgSrc.value = '';
  // [zh] 语句
  blockSrc.value = '';
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `data`
    const data = await fetchSlideCaptcha();
    // [zh] 语句
    captchaMeta.value = data;
    // [zh] 语句
    bgSrc.value = toImgSrc(data.originalImageBase64);
    // [zh] 语句
    blockSrc.value = toImgSrc(data.jigsawImageBase64);
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await nextTick();
    // [zh] 语句
    measureTrack();
  // [zh] 代码块开始
  } catch (e: unknown) {
    // [zh] 语句
    tip.value = e instanceof Error ? e.message : '加载失败';
    // [zh] 语句
    status.value = 'fail';
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    loading.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `open`
const open = (): Promise<string> => {
  // [zh] 条件判断 if
  if (visible.value) {
    // [zh] return 返回
    return Promise.reject(new Error('验证窗口已打开'));
  // [zh] 语法符号
  }
  // [zh] 语句
  settled = false;
  // [zh] 语句
  visible.value = true;
  // [zh] 语句
  loadCaptcha();
  // [zh] return 返回
  return new Promise((resolve, reject) => {
    // [zh] 语句
    resolveFn = resolve;
    // [zh] 语句
    rejectFn = reject;
  // [zh] 语句
  });
// [zh] 语句
};

// [zh] 声明变量 `finish`
const finish = (token: string) => {
  // [zh] 条件判断 if
  if (settled) return;
  // [zh] 语句
  settled = true;
  // [zh] 语句
  status.value = 'success';
  // [zh] 声明变量 `seconds`
  const seconds = ((Date.now() - startTime.value) / 1000).toFixed(1);
  // [zh] 语句
  tip.value = `${seconds}s 验证成功`;
  // [zh] 代码块开始
  setTimeout(() => {
    // [zh] 语句
    visible.value = false;
    // [zh] 语句
    resolveFn?.(token);
    // [zh] 语句
    cleanup();
  // [zh] 语句
  }, 650);
// [zh] 语句
};

// [zh] 声明变量 `cancel`
const cancel = () => {
  // [zh] 条件判断 if
  if (settled) return;
  // [zh] 语句
  settled = true;
  // [zh] 语句
  discardCurrentToken();
  // [zh] 语句
  visible.value = false;
  // [zh] 语句
  rejectFn?.();
  // [zh] 语句
  cleanup();
// [zh] 语句
};

// [zh] 声明变量 `cleanup`
const cleanup = () => {
  // [zh] 语句
  resolveFn = null;
  // [zh] 语句
  rejectFn = null;
  // [zh] 语句
  resetState();
  // [zh] 语句
  captchaMeta.value = null;
  // [zh] 语句
  bgSrc.value = '';
  // [zh] 语句
  blockSrc.value = '';
// [zh] 语句
};

// [zh] 声明变量 `measureTrack`
const measureTrack = () => {
  // [zh] 语句
  trackWidth.value = sliderWrapRef.value?.clientWidth || imageWrapRef.value?.clientWidth || 310;
// [zh] 语句
};

// [zh] 声明变量 `maxMove`
const maxMove = () => Math.max(0, trackWidth.value - knobSize);

// [zh] 声明变量 `canDrag`
const canDrag = () => !loading.value && !verifying.value && status.value !== 'success';

// [zh] 声明变量 `updateMove`
const updateMove = (clientX: number) => {
  // [zh] 声明变量 `delta`
  const delta = clientX - startClientX;
  // [zh] 语句
  moveX.value = Math.min(maxMove(), Math.max(0, startMoveX + delta));
// [zh] 语句
};

// [zh] 声明变量 `bindDragListeners`
const bindDragListeners = () => {
  // [zh] 语句
  window.addEventListener('pointermove', onPointerMove, { passive: false });
  // [zh] 语句
  window.addEventListener('pointerup', onPointerUp);
  // [zh] 语句
  window.addEventListener('pointercancel', onPointerUp);
  // [zh] 语句
  window.addEventListener('touchmove', onTouchMove, { passive: false });
  // [zh] 语句
  window.addEventListener('touchend', onTouchEnd);
  // [zh] 语句
  window.addEventListener('touchcancel', onTouchEnd);
// [zh] 语句
};

// [zh] 声明变量 `unbindDragListeners`
const unbindDragListeners = () => {
  // [zh] 语句
  window.removeEventListener('pointermove', onPointerMove);
  // [zh] 语句
  window.removeEventListener('pointerup', onPointerUp);
  // [zh] 语句
  window.removeEventListener('pointercancel', onPointerUp);
  // [zh] 语句
  window.removeEventListener('touchmove', onTouchMove);
  // [zh] 语句
  window.removeEventListener('touchend', onTouchEnd);
  // [zh] 语句
  window.removeEventListener('touchcancel', onTouchEnd);
// [zh] 语句
};

// [zh] 声明变量 `beginDrag`
const beginDrag = (clientX: number) => {
  // [zh] 条件判断 if
  if (!canDrag() || dragging.value) return false;
  // [zh] 语句
  measureTrack();
  // [zh] 语句
  dragging.value = true;
  // [zh] 语句
  status.value = 'idle';
  // [zh] 语句
  tip.value = '';
  // [zh] 语句
  startTime.value = Date.now();
  // [zh] 语句
  startClientX = clientX;
  // [zh] 语句
  startMoveX = moveX.value;
  // [zh] 语句
  bindDragListeners();
  // [zh] return 返回
  return true;
// [zh] 语句
};

// [zh] 声明变量 `onDragStart`
const onDragStart = (e: PointerEvent) => {
  // [zh] 条件判断 if
  if (!canDrag()) return;
  // [zh] 条件判断 if
  if ((e.target as HTMLElement).closest('.btn-refresh')) return;
  // [zh] 语句
  e.preventDefault();
  // [zh] 语句
  e.stopPropagation();
  // [zh] 条件判断 if
  if (!beginDrag(e.clientX)) return;
  // [zh] 语句
  activePointerId = e.pointerId;
  // [zh] 语句
  touchDragActive = e.pointerType === 'touch';
  // [zh] 语句
  knobRef.value?.setPointerCapture?.(e.pointerId);
// [zh] 语句
};

// [zh] 声明变量 `onTouchStart`
const onTouchStart = (e: TouchEvent) => {
  // [zh] 条件判断 if
  if (!canDrag() || dragging.value) return;
  // [zh] 条件判断 if
  if ((e.target as HTMLElement).closest('.btn-refresh')) return;
  // [zh] 声明变量 `touch`
  const touch = e.touches[0];
  // [zh] 条件判断 if
  if (!touch) return;
  // [zh] 条件判断 if
  if (!beginDrag(touch.clientX)) return;
  // [zh] 语句
  touchDragActive = true;
  // [zh] 语句
  e.preventDefault();
// [zh] 语句
};

// [zh] 声明变量 `onPointerMove`
const onPointerMove = (e: PointerEvent) => {
  // [zh] 条件判断 if
  if (!dragging.value) return;
  // [zh] 条件判断 if
  if (activePointerId != null && e.pointerId !== activePointerId) return;
  // [zh] 语句
  e.preventDefault();
  // [zh] 语句
  updateMove(e.clientX);
// [zh] 语句
};

// [zh] 声明变量 `onTouchMove`
const onTouchMove = (e: TouchEvent) => {
  // [zh] 条件判断 if
  if (!dragging.value || !touchDragActive) return;
  // [zh] 声明变量 `touch`
  const touch = e.touches[0];
  // [zh] 条件判断 if
  if (!touch) return;
  // [zh] 语句
  e.preventDefault();
  // [zh] 语句
  updateMove(touch.clientX);
// [zh] 语句
};

// [zh] 声明变量 `endDrag`
const endDrag = async () => {
  // [zh] 条件判断 if
  if (!dragging.value) return;
  // [zh] 语句
  dragging.value = false;
  // [zh] 语句
  activePointerId = null;
  // [zh] 语句
  touchDragActive = false;
  // [zh] 语句
  unbindDragListeners();

  // [zh] 声明变量 `meta`
  const meta = captchaMeta.value;
  // [zh] 条件判断 if
  if (!meta?.token) return;

  // [zh] 语句
  verifying.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `captchaX`
    const captchaX = toCaptchaX(moveX.value, trackWidth.value);
    // [zh] 声明变量 `captchaVerification`
    const captchaVerification = await checkSlideCaptcha({
      // [zh] TypeScript/JS 语句：`token: meta.token,`
      token: meta.token,
      // [zh] TypeScript/JS 语句：`secretKey: meta.secretKey,`
      secretKey: meta.secretKey,
      // [zh] TypeScript/JS 语句：`moveX: captchaX`
      moveX: captchaX
    // [zh] 语句
    });
    // [zh] 语句
    finish(captchaVerification);
  // [zh] 代码块开始
  } catch (e: unknown) {
    // [zh] 语句
    status.value = 'fail';
    // [zh] 语句
    tip.value = e instanceof Error ? e.message : '验证失败';
    // [zh] 代码块开始
    setTimeout(() => {
      // [zh] 语句
      resetState();
      // [zh] 语句
      loadCaptcha();
    // [zh] 语句
    }, 900);
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    verifying.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `onPointerUp`
const onPointerUp = (e: PointerEvent) => {
  // [zh] 条件判断 if
  if (touchDragActive) return;
  // [zh] 条件判断 if
  if (activePointerId != null && e.pointerId !== activePointerId) return;
  // [zh] 语句
  void endDrag();
// [zh] 语句
};

// [zh] 声明变量 `onTouchEnd`
const onTouchEnd = () => {
  // [zh] 条件判断 if
  if (!touchDragActive) return;
  // [zh] 语句
  void endDrag();
// [zh] 语句
};

// [zh] 代码块开始
onBeforeUnmount(() => {
  // [zh] 语句
  discardCurrentToken();
  // [zh] 语句
  unbindDragListeners();
// [zh] 语句
});

// [zh] 语句
defineExpose({ open });
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.slide-captcha-overlay {` */
.slide-captcha-overlay {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `z-index: 4000;` */
  z-index: 4000;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `padding: 20px;` */
  padding: 20px;
  /* [zh] 样式规则 `background: rgba(15, 15, 18, 0.55);` */
  background: rgba(15, 15, 18, 0.55);
  /* [zh] 样式规则 `backdrop-filter: blur(10px);` */
  backdrop-filter: blur(10px);
  /* [zh] 样式规则 `touch-action: none;` */
  touch-action: none;
  /* [zh] 样式规则 `overscroll-behavior: contain;` */
  overscroll-behavior: contain;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.slide-captcha-panel {` */
.slide-captcha-panel {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `width: min(92vw, 380px);` */
  width: min(92vw, 380px);
  /* [zh] 样式规则 `border-radius: 22px;` */
  border-radius: 22px;
  /* [zh] 样式规则 `background: linear-gradient(165deg, #fff` */
  background: linear-gradient(165deg, #fff 0%, #fafafa 100%);
  /* [zh] 样式规则 `border: 1px solid rgba(255, 255, 255, 0.` */
  border: 1px solid rgba(255, 255, 255, 0.8);
  /* [zh] 样式规则 `box-shadow:` */
  box-shadow:
    /* [zh] 样式声明 */
    0 24px 64px rgba(0, 0, 0, 0.22),
    /* [zh] 样式声明 */
    0 0 0 1px rgba(29, 29, 31, 0.04);
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.panel-glow {` */
.panel-glow {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `inset: -40% -20% auto;` */
  inset: -40% -20% auto;
  /* [zh] 样式规则 `height: 180px;` */
  height: 180px;
  /* [zh] 样式规则 `background: radial-gradient(ellipse at 5` */
  background: radial-gradient(ellipse at 50% 0%, rgba($color-gold, 0.18), transparent 70%);
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.panel-header {` */
.panel-header {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `padding: 20px 20px 14px;` */
  padding: 20px 20px 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.header-icon {` */
.header-icon {
  /* [zh] 样式规则 `width: 42px;` */
  width: 42px;
  /* [zh] 样式规则 `height: 42px;` */
  height: 42px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `border-radius: 14px;` */
  border-radius: 14px;
  /* [zh] 样式规则 `color: $color-gold;` */
  color: $color-gold;
  /* [zh] 样式规则 `background: linear-gradient(145deg, $col` */
  background: linear-gradient(145deg, $color-gold-soft, #fff);
  /* [zh] 样式规则 `box-shadow: inset 0 1px 0 rgba(255, 255,` */
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);

  /* [zh] 样式规则 `svg {` */
  svg {
    /* [zh] 样式规则 `width: 22px;` */
    width: 22px;
    /* [zh] 样式规则 `height: 22px;` */
    height: 22px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.header-text {` */
.header-text {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;

  /* [zh] 样式规则 `h3 {` */
  h3 {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 17px;` */
    font-size: 17px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `letter-spacing: 0.02em;` */
    letter-spacing: 0.02em;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `p {` */
  p {
    /* [zh] 样式规则 `margin: 4px 0 0;` */
    margin: 4px 0 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `line-height: 1.45;` */
    line-height: 1.45;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.btn-close {` */
.btn-close {
  /* [zh] 样式规则 `width: 32px;` */
  width: 32px;
  /* [zh] 样式规则 `height: 32px;` */
  height: 32px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 10px;` */
  border-radius: 10px;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;

  /* [zh] 样式规则 `svg {` */
  svg {
    /* [zh] 样式规则 `width: 18px;` */
    width: 18px;
    /* [zh] 样式规则 `height: 18px;` */
    height: 18px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `background: $color-primary-muted;` */
    background: $color-primary-muted;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.panel-body {` */
.panel-body {
  /* [zh] 样式规则 `padding: 0 20px 20px;` */
  padding: 0 20px 20px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.image-wrap {` */
.image-wrap {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `aspect-ratio: 310 / 155;` */
  aspect-ratio: 310 / 155;
  /* [zh] 样式规则 `border-radius: 14px;` */
  border-radius: 14px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: #1a1a1e;` */
  background: #1a1a1e;
  /* [zh] 样式规则 `box-shadow: inset 0 0 0 1px rgba(255, 25` */
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
  /* [zh] 样式规则 `touch-action: none;` */
  touch-action: none;
  /* [zh] 样式规则 `user-select: none;` */
  user-select: none;
  /* [zh] 样式规则 `-webkit-user-select: none;` */
  -webkit-user-select: none;
  /* [zh] 样式规则 `cursor: grab;` */
  cursor: grab;

  /* [zh] 样式规则 `&.dragging {` */
  &.dragging {
    /* [zh] 样式规则 `cursor: grabbing;` */
    cursor: grabbing;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.success {` */
  &.success {
    /* [zh] 样式规则 `box-shadow: 0 0 0 2px rgba($color-succes` */
    box-shadow: 0 0 0 2px rgba($color-success, 0.45);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.fail {` */
  &.fail {
    /* [zh] 样式规则 `animation: shake 0.45s ease;` */
    animation: shake 0.45s ease;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@keyframes shake {` */
@keyframes shake {
  /* [zh] 样式声明 */
  0%,
  /* [zh] 样式规则 `100% {` */
  100% {
    /* [zh] 样式规则 `transform: translateX(0);` */
    transform: translateX(0);
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `25% {` */
  25% {
    /* [zh] 样式规则 `transform: translateX(-4px);` */
    transform: translateX(-4px);
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `75% {` */
  75% {
    /* [zh] 样式规则 `transform: translateX(4px);` */
    transform: translateX(4px);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.bg-img,
/* [zh] 样式规则 `.block-img {` */
.block-img {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
  /* [zh] 样式规则 `object-fit: cover;` */
  object-fit: cover;
  /* [zh] 样式规则 `user-select: none;` */
  user-select: none;
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.block-img {` */
.block-img {
  /* [zh] 样式规则 `width: auto;` */
  width: auto;
  /* [zh] 样式规则 `max-width: none;` */
  max-width: none;
  /* [zh] 样式规则 `transition: transform 0.05s linear;` */
  transition: transform 0.05s linear;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.image-skeleton {` */
.image-skeleton {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `color: rgba(255, 255, 255, 0.65);` */
  color: rgba(255, 255, 255, 0.65);
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `background: linear-gradient(135deg, #2a2` */
  background: linear-gradient(135deg, #2a2a30, #1a1a1e);

  /* [zh] 样式规则 `.skeleton-shimmer {` */
  .skeleton-shimmer {
    /* [zh] 样式规则 `width: 48px;` */
    width: 48px;
    /* [zh] 样式规则 `height: 48px;` */
    height: 48px;
    /* [zh] 样式规则 `border-radius: 50%;` */
    border-radius: 50%;
    /* [zh] 样式规则 `border: 3px solid rgba(255, 255, 255, 0.` */
    border: 3px solid rgba(255, 255, 255, 0.12);
    /* [zh] 样式规则 `border-top-color: $color-gold;` */
    border-top-color: $color-gold;
    /* [zh] 样式规则 `animation: spin 0.8s linear infinite;` */
    animation: spin 0.8s linear infinite;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@keyframes spin {` */
@keyframes spin {
  /* [zh] 样式规则 `to {` */
  to {
    /* [zh] 样式规则 `transform: rotate(360deg);` */
    transform: rotate(360deg);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.btn-refresh {` */
.btn-refresh {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `top: 10px;` */
  top: 10px;
  /* [zh] 样式规则 `right: 10px;` */
  right: 10px;
  /* [zh] 样式规则 `width: 34px;` */
  width: 34px;
  /* [zh] 样式规则 `height: 34px;` */
  height: 34px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 10px;` */
  border-radius: 10px;
  /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.45);` */
  background: rgba(0, 0, 0, 0.45);
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `backdrop-filter: blur(6px);` */
  backdrop-filter: blur(6px);
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `touch-action: manipulation;` */
  touch-action: manipulation;
  /* [zh] 样式规则 `z-index: 2;` */
  z-index: 2;

  /* [zh] 样式规则 `svg {` */
  svg {
    /* [zh] 样式规则 `width: 16px;` */
    width: 16px;
    /* [zh] 样式规则 `height: 16px;` */
    height: 16px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:hover:not(:disabled) {` */
  &:hover:not(:disabled) {
    /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.62);` */
    background: rgba(0, 0, 0, 0.62);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:disabled {` */
  &:disabled {
    /* [zh] 样式规则 `opacity: 0.45;` */
    opacity: 0.45;
    /* [zh] 样式规则 `cursor: not-allowed;` */
    cursor: not-allowed;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.status-badge {` */
.status-badge {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `background: rgba(82, 196, 26, 0.72);` */
  background: rgba(82, 196, 26, 0.72);
  /* [zh] 样式规则 `backdrop-filter: blur(2px);` */
  backdrop-filter: blur(2px);

  /* [zh] 样式规则 `svg {` */
  svg {
    /* [zh] 样式规则 `width: 22px;` */
    width: 22px;
    /* [zh] 样式规则 `height: 22px;` */
    height: 22px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.slider-wrap {` */
.slider-wrap {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `margin-top: 14px;` */
  margin-top: 14px;
  /* [zh] 样式规则 `height: 46px;` */
  height: 46px;
  /* [zh] 样式规则 `touch-action: none;` */
  touch-action: none;
  /* [zh] 样式规则 `user-select: none;` */
  user-select: none;
  /* [zh] 样式规则 `-webkit-user-select: none;` */
  -webkit-user-select: none;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.slider-track {` */
.slider-track {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `border-radius: 23px;` */
  border-radius: 23px;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.slider-fill {` */
.slider-fill {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `left: 0;` */
  left: 0;
  /* [zh] 样式规则 `top: 0;` */
  top: 0;
  /* [zh] 样式规则 `bottom: 0;` */
  bottom: 0;
  /* [zh] 样式规则 `border-radius: 23px 0 0 23px;` */
  border-radius: 23px 0 0 23px;
  /* [zh] 样式规则 `background: linear-gradient(90deg, rgba(` */
  background: linear-gradient(90deg, rgba($color-gold, 0.25), rgba($color-gold, 0.08));
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
  /* [zh] 样式规则 `transition: width 0.05s linear;` */
  transition: width 0.05s linear;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.slider-hint {` */
.slider-hint {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
  /* [zh] 样式规则 `transition: opacity 0.2s;` */
  transition: opacity 0.2s;

  /* [zh] 样式规则 `&.hidden {` */
  &.hidden {
    /* [zh] 样式规则 `opacity: 0;` */
    opacity: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.slider-knob {` */
.slider-knob {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `z-index: 1;` */
  z-index: 1;
  /* [zh] 样式规则 `width: 46px;` */
  width: 46px;
  /* [zh] 样式规则 `height: 46px;` */
  height: 46px;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `cursor: grab;` */
  cursor: grab;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `background: linear-gradient(180deg, #fff` */
  background: linear-gradient(180deg, #fff 0%, #f3f3f5 100%);
  /* [zh] 样式规则 `box-shadow:` */
  box-shadow:
    /* [zh] 样式声明 */
    0 4px 14px rgba(0, 0, 0, 0.12),
    /* [zh] 样式声明 */
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  /* [zh] 样式规则 `touch-action: none;` */
  touch-action: none;
  /* [zh] 样式规则 `transition: box-shadow 0.2s, color 0.2s,` */
  transition: box-shadow 0.2s, color 0.2s, background 0.2s;

  /* [zh] 样式规则 `svg {` */
  svg {
    /* [zh] 样式规则 `width: 18px;` */
    width: 18px;
    /* [zh] 样式规则 `height: 18px;` */
    height: 18px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.dragging {` */
  &.dragging {
    /* [zh] 样式规则 `cursor: grabbing;` */
    cursor: grabbing;
    /* [zh] 样式规则 `box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1` */
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.18);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.success {` */
  &.success {
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `background: linear-gradient(145deg, #6dd` */
    background: linear-gradient(145deg, #6dd400, $color-success);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.fail {` */
  &.fail {
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `background: linear-gradient(145deg, #ff7` */
    background: linear-gradient(145deg, #ff7875, $color-error);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tip {` */
.tip {
  /* [zh] 样式规则 `margin: 10px 0 0;` */
  margin: 10px 0 0;
  /* [zh] 样式规则 `min-height: 18px;` */
  min-height: 18px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;

  /* [zh] 样式规则 `&.success {` */
  &.success {
    /* [zh] 样式规则 `color: $color-success;` */
    color: $color-success;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.fail {` */
  &.fail {
    /* [zh] 样式规则 `color: $color-error;` */
    color: $color-error;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.slide-captcha-fade-enter-active,
/* [zh] 样式规则 `.slide-captcha-fade-leave-active {` */
.slide-captcha-fade-leave-active {
  /* [zh] 样式规则 `transition: opacity 0.25s ease;` */
  transition: opacity 0.25s ease;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.slide-captcha-fade-enter-from,
/* [zh] 样式规则 `.slide-captcha-fade-leave-to {` */
.slide-captcha-fade-leave-to {
  /* [zh] 样式规则 `opacity: 0;` */
  opacity: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.slide-captcha-fade-enter-active .slide-captcha-panel,
/* [zh] 样式规则 `.slide-captcha-fade-leave-active .slide-` */
.slide-captcha-fade-leave-active .slide-captcha-panel {
  /* [zh] 样式规则 `transition: transform 0.28s cubic-bezier` */
  transition: transform 0.28s cubic-bezier(0.34, 1.2, 0.64, 1);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.slide-captcha-fade-enter-from .slide-captcha-panel,
/* [zh] 样式规则 `.slide-captcha-fade-leave-to .slide-capt` */
.slide-captcha-fade-leave-to .slide-captcha-panel {
  /* [zh] 样式规则 `transform: translateY(16px) scale(0.96);` */
  transform: translateY(16px) scale(0.96);
/* [zh] 样式规则 `}` */
}
</style>
