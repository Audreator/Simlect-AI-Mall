import { computed, ref, type CSSProperties } from 'vue';

const MIN_SCALE = 1;
const MAX_SCALE = 5;
const WHEEL_FACTOR = 1.12;

export function useImageLightboxZoom() {
  const scale = ref(1);
  const translateX = ref(0);
  const translateY = ref(0);

  const imgStyle = computed<CSSProperties>(() => ({
    transform: `translate3d(${translateX.value}px, ${translateY.value}px, 0) scale(${scale.value})`
  }));

  const resetTransform = () => {
    scale.value = 1;
    translateX.value = 0;
    translateY.value = 0;
  };

  const clampScale = (value: number) => Math.min(MAX_SCALE, Math.max(MIN_SCALE, value));

  const onWheel = (event: WheelEvent) => {
    event.preventDefault();
    const factor = event.deltaY < 0 ? WHEEL_FACTOR : 1 / WHEEL_FACTOR;
    const next = clampScale(scale.value * factor);
    scale.value = next;
    if (next <= MIN_SCALE) {
      translateX.value = 0;
      translateY.value = 0;
    }
  };

  let pinchStartDistance = 0;
  let pinchStartScale = 1;
  let panStartX = 0;
  let panStartY = 0;
  let panOriginX = 0;
  let panOriginY = 0;
  let gestureMode: 'none' | 'pinch' | 'pan' = 'none';

  const touchDistance = (touches: TouchList) => {
    const [a, b] = [touches[0], touches[1]];
    return Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
  };

  const onTouchStart = (event: TouchEvent) => {
    if (event.touches.length === 2) {
      gestureMode = 'pinch';
      pinchStartDistance = touchDistance(event.touches);
      pinchStartScale = scale.value;
      return;
    }
    if (event.touches.length === 1 && scale.value > MIN_SCALE) {
      gestureMode = 'pan';
      panStartX = event.touches[0].clientX;
      panStartY = event.touches[0].clientY;
      panOriginX = translateX.value;
      panOriginY = translateY.value;
    }
  };

  const onTouchMove = (event: TouchEvent) => {
    if (gestureMode === 'pinch' && event.touches.length === 2) {
      event.preventDefault();
      const distance = touchDistance(event.touches);
      if (!pinchStartDistance) return;
      scale.value = clampScale(pinchStartScale * (distance / pinchStartDistance));
      return;
    }
    if (gestureMode === 'pan' && event.touches.length === 1) {
      event.preventDefault();
      translateX.value = panOriginX + (event.touches[0].clientX - panStartX);
      translateY.value = panOriginY + (event.touches[0].clientY - panStartY);
    }
  };

  const onTouchEnd = (event: TouchEvent) => {
    if (event.touches.length < 2 && gestureMode === 'pinch') {
      gestureMode = 'none';
    }
    if (event.touches.length === 0) {
      gestureMode = 'none';
      if (scale.value <= MIN_SCALE) resetTransform();
    }
  };

  let mousePanning = false;

  const onMouseDown = (event: MouseEvent) => {
    if (scale.value <= MIN_SCALE || event.button !== 0) return;
    mousePanning = true;
    panStartX = event.clientX;
    panStartY = event.clientY;
    panOriginX = translateX.value;
    panOriginY = translateY.value;
  };

  const onMouseMove = (event: MouseEvent) => {
    if (!mousePanning) return;
    translateX.value = panOriginX + (event.clientX - panStartX);
    translateY.value = panOriginY + (event.clientY - panStartY);
  };

  const onMouseUp = () => {
    mousePanning = false;
  };

  return {
    imgStyle,
    resetTransform,
    onWheel,
    onTouchStart,
    onTouchMove,
    onTouchEnd,
    onMouseDown,
    onMouseMove,
    onMouseUp
  };
}
