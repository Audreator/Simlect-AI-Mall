// [zh] 导入 `vue`
import { computed, ref, type CSSProperties } from 'vue';

// [zh] 声明变量 `MIN_SCALE`
const MIN_SCALE = 1;
// [zh] 声明变量 `MAX_SCALE`
const MAX_SCALE = 5;
// [zh] 声明变量 `WHEEL_FACTOR`
const WHEEL_FACTOR = 1.12;

// [zh] 导出模块成员
export function useImageLightboxZoom() {
  // [zh] 声明变量 `scale`
  const scale = ref(1);
  // [zh] 声明变量 `translateX`
  const translateX = ref(0);
  // [zh] 声明变量 `translateY`
  const translateY = ref(0);

  // [zh] 声明变量 `imgStyle`
  const imgStyle = computed<CSSProperties>(() => ({
    // [zh] TypeScript/JS 语句：`transform: `translate3d(${translateX.value}px, ${transl`
    transform: `translate3d(${translateX.value}px, ${translateY.value}px, 0) scale(${scale.value})`
  // [zh] 语句
  }));

  // [zh] 声明变量 `resetTransform`
  const resetTransform = () => {
    // [zh] 语句
    scale.value = 1;
    // [zh] 语句
    translateX.value = 0;
    // [zh] 语句
    translateY.value = 0;
  // [zh] 语句
  };

  // [zh] 声明变量 `clampScale`
  const clampScale = (value: number) => Math.min(MAX_SCALE, Math.max(MIN_SCALE, value));

  // [zh] 声明变量 `onWheel`
  const onWheel = (event: WheelEvent) => {
    // [zh] 语句
    event.preventDefault();
    // [zh] 声明变量 `factor`
    const factor = event.deltaY < 0 ? WHEEL_FACTOR : 1 / WHEEL_FACTOR;
    // [zh] 声明变量 `next`
    const next = clampScale(scale.value * factor);
    // [zh] 语句
    scale.value = next;
    // [zh] 条件判断 if
    if (next <= MIN_SCALE) {
      // [zh] 语句
      translateX.value = 0;
      // [zh] 语句
      translateY.value = 0;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `pinchStartDistance`
  let pinchStartDistance = 0;
  // [zh] 声明变量 `pinchStartScale`
  let pinchStartScale = 1;
  // [zh] 声明变量 `panStartX`
  let panStartX = 0;
  // [zh] 声明变量 `panStartY`
  let panStartY = 0;
  // [zh] 声明变量 `panOriginX`
  let panOriginX = 0;
  // [zh] 声明变量 `panOriginY`
  let panOriginY = 0;
  // [zh] 声明变量 `gestureMode`
  let gestureMode: 'none' | 'pinch' | 'pan' = 'none';

  // [zh] 声明变量 `touchDistance`
  const touchDistance = (touches: TouchList) => {
    // [zh] 语句
    const [a, b] = [touches[0], touches[1]];
    // [zh] return 返回
    return Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
  // [zh] 语句
  };

  // [zh] 声明变量 `onTouchStart`
  const onTouchStart = (event: TouchEvent) => {
    // [zh] 条件判断 if
    if (event.touches.length === 2) {
      // [zh] 语句
      gestureMode = 'pinch';
      // [zh] 语句
      pinchStartDistance = touchDistance(event.touches);
      // [zh] 语句
      pinchStartScale = scale.value;
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (event.touches.length === 1 && scale.value > MIN_SCALE) {
      // [zh] 语句
      gestureMode = 'pan';
      // [zh] 语句
      panStartX = event.touches[0].clientX;
      // [zh] 语句
      panStartY = event.touches[0].clientY;
      // [zh] 语句
      panOriginX = translateX.value;
      // [zh] 语句
      panOriginY = translateY.value;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `onTouchMove`
  const onTouchMove = (event: TouchEvent) => {
    // [zh] 条件判断 if
    if (gestureMode === 'pinch' && event.touches.length === 2) {
      // [zh] 语句
      event.preventDefault();
      // [zh] 声明变量 `distance`
      const distance = touchDistance(event.touches);
      // [zh] 条件判断 if
      if (!pinchStartDistance) return;
      // [zh] 语句
      scale.value = clampScale(pinchStartScale * (distance / pinchStartDistance));
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (gestureMode === 'pan' && event.touches.length === 1) {
      // [zh] 语句
      event.preventDefault();
      // [zh] 语句
      translateX.value = panOriginX + (event.touches[0].clientX - panStartX);
      // [zh] 语句
      translateY.value = panOriginY + (event.touches[0].clientY - panStartY);
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `onTouchEnd`
  const onTouchEnd = (event: TouchEvent) => {
    // [zh] 条件判断 if
    if (event.touches.length < 2 && gestureMode === 'pinch') {
      // [zh] 语句
      gestureMode = 'none';
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (event.touches.length === 0) {
      // [zh] 语句
      gestureMode = 'none';
      // [zh] 条件判断 if
      if (scale.value <= MIN_SCALE) resetTransform();
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `mousePanning`
  let mousePanning = false;

  // [zh] 声明变量 `onMouseDown`
  const onMouseDown = (event: MouseEvent) => {
    // [zh] 条件判断 if
    if (scale.value <= MIN_SCALE || event.button !== 0) return;
    // [zh] 语句
    mousePanning = true;
    // [zh] 语句
    panStartX = event.clientX;
    // [zh] 语句
    panStartY = event.clientY;
    // [zh] 语句
    panOriginX = translateX.value;
    // [zh] 语句
    panOriginY = translateY.value;
  // [zh] 语句
  };

  // [zh] 声明变量 `onMouseMove`
  const onMouseMove = (event: MouseEvent) => {
    // [zh] 条件判断 if
    if (!mousePanning) return;
    // [zh] 语句
    translateX.value = panOriginX + (event.clientX - panStartX);
    // [zh] 语句
    translateY.value = panOriginY + (event.clientY - panStartY);
  // [zh] 语句
  };

  // [zh] 声明变量 `onMouseUp`
  const onMouseUp = () => {
    // [zh] 语句
    mousePanning = false;
  // [zh] 语句
  };

  // [zh] return 返回
  return {
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
    // [zh] TypeScript/JS 语句：`onMouseUp`
    onMouseUp
  // [zh] 语句
  };
// [zh] 语法符号
}
