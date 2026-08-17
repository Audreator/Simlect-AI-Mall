<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Teleport>` -->
  <Teleport to="body">
    <!-- [zh] 开始标签 `<Transition>` -->
    <Transition name="slide-captcha-fade">
      <div v-if="visible" class="slide-captcha-overlay" @click.self="cancel">
        <div class="slide-captcha-panel" role="dialog" aria-modal="true" aria-label="安全验证">
          <div class="panel-glow" aria-hidden="true" />

          <header class="panel-header">
            <div class="header-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path
                  d="M12 2l7 4v6c0 5-3 9-7 10C8 21 5 17 5 12V6l7-4z"
                  stroke-linejoin="round"
                />
                <path d="M9.5 12.5l1.8 1.8 3.5-4.2" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </div>
            <div class="header-text">
              <h3>安全验证</h3>
              <p>拖动滑块完成拼图</p>
            </div>
            <button type="button" class="btn-close" aria-label="关闭" @click="cancel">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 6l12 12M18 6L6 18" stroke-linecap="round" />
              </svg>
            </button>
          </header>

          <div class="panel-body">
            <div
              ref="imageWrapRef"
              class="image-wrap"
              :class="{ success: status === 'success', fail: status === 'fail', dragging }"
              @pointerdown="onDragStart"
              @touchstart="onTouchStart"
            >
              <div v-if="loading" class="image-skeleton">
                <span class="skeleton-shimmer" />
                <p>加载验证图…</p>
              </div>
              <template v-else>
                <img v-if="bgSrc" :src="bgSrc" class="bg-img" alt="" draggable="false" />
                <img
                  v-if="blockSrc"
                  :src="blockSrc"
                  class="block-img"
                  :style="{ transform: `translateX(${moveX}px)` }"
                  alt=""
                  draggable="false"
                />
                <div v-if="status === 'success'" class="status-badge success-badge">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                    <path d="M5 12.5l4.2 4.3L19 7" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                  验证通过
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

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue';
import { checkSlideCaptcha, cancelSlideCaptchaToken, fetchSlideCaptcha, toCaptchaX, type CaptchaGetData } from '@/utils/slideCaptcha';

type Status = 'idle' | 'success' | 'fail';

const visible = ref(false);
const loading = ref(false);
const verifying = ref(false);
const dragging = ref(false);
const moveX = ref(0);
const status = ref<Status>('idle');
const tip = ref('');
const hintText = computed(() => (status.value === 'fail' ? '请重新拖动滑块' : '向右拖动滑块完成拼图'));

const imageWrapRef = ref<HTMLElement | null>(null);
const sliderWrapRef = ref<HTMLElement | null>(null);
const knobRef = ref<HTMLElement | null>(null);
const knobSize = 46;

const bgSrc = ref('');
const blockSrc = ref('');
const captchaMeta = ref<CaptchaGetData | null>(null);
const startTime = ref(0);
const trackWidth = ref(0);

let resolveFn: ((v: string) => void) | null = null;
let rejectFn: (() => void) | null = null;
let startClientX = 0;
let startMoveX = 0;
let settled = false;
let activePointerId: number | null = null;
let touchDragActive = false;

const toImgSrc = (b64: string) => (b64.startsWith('data:') ? b64 : `data:image/png;base64,${b64}`);

const resetState = () => {
  moveX.value = 0;
  status.value = 'idle';
  tip.value = '';
  dragging.value = false;
  verifying.value = false;
};

const discardCurrentToken = () => {
  const token = captchaMeta.value?.token;
  if (token) {
    cancelSlideCaptchaToken(token);
  }
};

const loadCaptcha = async () => {
  discardCurrentToken();
  loading.value = true;
  resetState();
  bgSrc.value = '';
  blockSrc.value = '';
  try {
    const data = await fetchSlideCaptcha();
    captchaMeta.value = data;
    bgSrc.value = toImgSrc(data.originalImageBase64);
    blockSrc.value = toImgSrc(data.jigsawImageBase64);
    await nextTick();
    measureTrack();
  } catch (e: unknown) {
    tip.value = e instanceof Error ? e.message : '加载失败';
    status.value = 'fail';
  } finally {
    loading.value = false;
  }
};

const open = (): Promise<string> => {
  if (visible.value) {
    return Promise.reject(new Error('验证窗口已打开'));
  }
  settled = false;
  visible.value = true;
  loadCaptcha();
  return new Promise((resolve, reject) => {
    resolveFn = resolve;
    rejectFn = reject;
  });
};

const finish = (token: string) => {
  if (settled) return;
  settled = true;
  status.value = 'success';
  const seconds = ((Date.now() - startTime.value) / 1000).toFixed(1);
  tip.value = `${seconds}s 验证成功`;
  setTimeout(() => {
    visible.value = false;
    resolveFn?.(token);
    cleanup();
  }, 650);
};

const cancel = () => {
  if (settled) return;
  settled = true;
  discardCurrentToken();
  visible.value = false;
  rejectFn?.();
  cleanup();
};

const cleanup = () => {
  resolveFn = null;
  rejectFn = null;
  resetState();
  captchaMeta.value = null;
  bgSrc.value = '';
  blockSrc.value = '';
};

const measureTrack = () => {
  trackWidth.value = sliderWrapRef.value?.clientWidth || imageWrapRef.value?.clientWidth || 310;
};

const maxMove = () => Math.max(0, trackWidth.value - knobSize);

const canDrag = () => !loading.value && !verifying.value && status.value !== 'success';

const updateMove = (clientX: number) => {
  const delta = clientX - startClientX;
  moveX.value = Math.min(maxMove(), Math.max(0, startMoveX + delta));
};

const bindDragListeners = () => {
  window.addEventListener('pointermove', onPointerMove, { passive: false });
  window.addEventListener('pointerup', onPointerUp);
  window.addEventListener('pointercancel', onPointerUp);
  window.addEventListener('touchmove', onTouchMove, { passive: false });
  window.addEventListener('touchend', onTouchEnd);
  window.addEventListener('touchcancel', onTouchEnd);
};

const unbindDragListeners = () => {
  window.removeEventListener('pointermove', onPointerMove);
  window.removeEventListener('pointerup', onPointerUp);
  window.removeEventListener('pointercancel', onPointerUp);
  window.removeEventListener('touchmove', onTouchMove);
  window.removeEventListener('touchend', onTouchEnd);
  window.removeEventListener('touchcancel', onTouchEnd);
};

const beginDrag = (clientX: number) => {
  if (!canDrag() || dragging.value) return false;
  measureTrack();
  dragging.value = true;
  status.value = 'idle';
  tip.value = '';
  startTime.value = Date.now();
  startClientX = clientX;
  startMoveX = moveX.value;
  bindDragListeners();
  return true;
};

const onDragStart = (e: PointerEvent) => {
  if (!canDrag()) return;
  if ((e.target as HTMLElement).closest('.btn-refresh')) return;
  e.preventDefault();
  e.stopPropagation();
  if (!beginDrag(e.clientX)) return;
  activePointerId = e.pointerId;
  touchDragActive = e.pointerType === 'touch';
  knobRef.value?.setPointerCapture?.(e.pointerId);
};

const onTouchStart = (e: TouchEvent) => {
  if (!canDrag() || dragging.value) return;
  if ((e.target as HTMLElement).closest('.btn-refresh')) return;
  const touch = e.touches[0];
  if (!touch) return;
  if (!beginDrag(touch.clientX)) return;
  touchDragActive = true;
  e.preventDefault();
};

const onPointerMove = (e: PointerEvent) => {
  if (!dragging.value) return;
  if (activePointerId != null && e.pointerId !== activePointerId) return;
  e.preventDefault();
  updateMove(e.clientX);
};

const onTouchMove = (e: TouchEvent) => {
  if (!dragging.value || !touchDragActive) return;
  const touch = e.touches[0];
  if (!touch) return;
  e.preventDefault();
  updateMove(touch.clientX);
};

const endDrag = async () => {
  if (!dragging.value) return;
  dragging.value = false;
  activePointerId = null;
  touchDragActive = false;
  unbindDragListeners();

  const meta = captchaMeta.value;
  if (!meta?.token) return;

  verifying.value = true;
  try {
    const captchaX = toCaptchaX(moveX.value, trackWidth.value);
    const captchaVerification = await checkSlideCaptcha({
      token: meta.token,
      secretKey: meta.secretKey,
      moveX: captchaX
    });
    finish(captchaVerification);
  } catch (e: unknown) {
    status.value = 'fail';
    tip.value = e instanceof Error ? e.message : '验证失败';
    setTimeout(() => {
      resetState();
      loadCaptcha();
    }, 900);
  } finally {
    verifying.value = false;
  }
};

const onPointerUp = (e: PointerEvent) => {
  if (touchDragActive) return;
  if (activePointerId != null && e.pointerId !== activePointerId) return;
  void endDrag();
};

const onTouchEnd = () => {
  if (!touchDragActive) return;
  void endDrag();
};

onBeforeUnmount(() => {
  discardCurrentToken();
  unbindDragListeners();
});

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
