<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Transition>` -->
  <Transition name="ios-notification">
    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="visible" class="ios-notification-host">
      <!-- [zh] 开始标签 `<div>` -->
      <div
        class="ios-notification-shell"
        :class="{ dragging: isDragging }"
        :style="dragStyle"
      >
        <!-- [zh] 开始标签 `<LiquidGlassSurface>` -->
        <LiquidGlassSurface
          intensity="subtle"
          class="ios-notification-banner"
          role="button"
          tabindex="0"
          @click="handleBannerClick"
          @keydown.enter="handleBannerClick"
          @touchstart.passive="onTouchStart"
          @touchmove.passive="onTouchMove"
          @touchend="onTouchEnd"
        >
          <!-- [zh] 开始标签 `<div>` -->
          <div class="ios-notification-inner">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="ios-notification-icon" :class="`ios-notification-icon--${iconVisual.theme}`">
              <!-- [zh] 开始标签 `<BrandMark>` -->
              <BrandMark
                v-if="iconVisual.useBrand"
                class="ios-app-mark"
                variant="light"
              />
              <!-- [zh] 开始标签 `<el-icon>` -->
              <el-icon v-else class="ios-type-icon">
                <!-- [zh] 开始标签 `<component>` -->
                <component :is="iconVisual.icon" />
              <!-- [zh] 闭合标签 `</el-icon>` -->
              </el-icon>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>

            <!-- [zh] 开始标签 `<div>` -->
            <div class="ios-notification-copy">
              <!-- [zh] 开始标签 `<div>` -->
              <div class="ios-notification-meta">
                <!-- [zh] 开始标签 `<span>` -->
                <span class="ios-app-name">简选</span>
                <!-- [zh] 开始标签 `<span>` -->
                <span class="ios-meta-sep" aria-hidden="true">·</span>
                <!-- [zh] 开始标签 `<span>` -->
                <span class="ios-meta-time">{{ formatTime(notification.createTime) }}</span>
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
              <!-- [zh] 开始标签 `<p>` -->
              <p class="ios-notification-title">{{ notification.title }}</p>
              <!-- [zh] 开始标签 `<p>` -->
              <p class="ios-notification-body">{{ notification.content }}</p>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>

            <!-- [zh] 开始标签 `<span>` -->
            <span class="ios-notification-chevron" aria-hidden="true">
              <!-- [zh] 开始标签 `<el-icon>` -->
              <el-icon :size="14"><ArrowRight /></el-icon>
            <!-- [zh] 闭合标签 `</span>` -->
            </span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</LiquidGlassSurface>` -->
        </LiquidGlassSurface>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</Transition>` -->
  </Transition>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { ref, computed, onMounted, onUnmounted, type Component } from 'vue';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`AlarmClock,`
  AlarmClock,
  // [zh] TypeScript/JS 语句：`ArrowRight,`
  ArrowRight,
  // [zh] TypeScript/JS 语句：`Box,`
  Box,
  // [zh] TypeScript/JS 语句：`Calendar,`
  Calendar,
  // [zh] TypeScript/JS 语句：`ShoppingBag,`
  ShoppingBag,
  // [zh] TypeScript/JS 语句：`StarFilled,`
  StarFilled,
  // [zh] TypeScript/JS 语句：`Ticket`
  Ticket
// [zh] 语句
} from '@element-plus/icons-vue';
// [zh] 导入 `@/components/common/BrandMark.vue`
import BrandMark from '@/components/common/BrandMark.vue';
// [zh] 导入 `@/components/common/LiquidGlassSurface.vue`
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
// [zh] 导入 `@/api/modules`
import { notificationApi } from '@/api/modules';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `模块`
import type { NotificationData } from '@/utils/notification';
// [zh] 导入 `@/utils/formatDateTime`
import { formatDisplayDateTime } from '@/utils/formatDateTime';

// [zh] TypeScript 类型 `Notification`（类比 Java interface/DTO）
interface Notification {
  // [zh] 语句
  notificationId: string;
  // [zh] 语句
  title: string;
  // [zh] 语句
  content: string;
  // [zh] 语句
  bizType?: string;
  // [zh] 语句
  bizId?: string;
  // [zh] 语句
  createTime?: string;
// [zh] 语法符号
}

// [zh] TypeScript 类型 `IconTheme`（类比 Java interface/DTO）
type IconTheme = 'app' | 'logistics' | 'coupon' | 'coupon-warn' | 'order' | 'member' | 'sign';

// [zh] 声明变量 `emit`
const emit = defineEmits<{
  // [zh] 语句
  (e: 'close'): void;
  // [zh] 语句
  (e: 'click', notification: Notification): void;
// [zh] 语句
}>();

// [zh] 声明变量 `shownNotificationIds`
const shownNotificationIds = new Set<string>();
// [zh] 声明变量 `globalPollInitialized`
let globalPollInitialized = false;

// [zh] 声明变量 `visible`
const visible = ref(false);
// [zh] 声明变量 `notification`
const notification = ref<Notification>({
  // [zh] TypeScript/JS 语句：`notificationId: '',`
  notificationId: '',
  // [zh] TypeScript/JS 语句：`title: '',`
  title: '',
  // [zh] TypeScript/JS 语句：`content: ''`
  content: ''
// [zh] 语句
});

// [zh] 声明变量 `iconVisual`
const iconVisual = computed((): { theme: IconTheme; useBrand: boolean; icon: Component } => {
  // [zh] 声明变量 `bizType`
  const bizType = notification.value.bizType || '';

  // [zh] 条件判断 if
  if (bizType === 'rush_coupon' || bizType === 'coupon_rush') {
    // [zh] return 返回
    return { theme: 'coupon', useBrand: false, icon: Ticket };
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (bizType === 'coupon_expire' || bizType === 'coupon') {
    // [zh] return 返回
    return { theme: 'coupon-warn', useBrand: false, icon: AlarmClock };
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (bizType === 'logistics') {
    // [zh] return 返回
    return { theme: 'logistics', useBrand: false, icon: Box };
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (bizType === 'order') {
    // [zh] return 返回
    return { theme: 'order', useBrand: false, icon: ShoppingBag };
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (bizType === 'member_level') {
    // [zh] return 返回
    return { theme: 'member', useBrand: false, icon: StarFilled };
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (bizType === 'sign') {
    // [zh] return 返回
    return { theme: 'sign', useBrand: false, icon: Calendar };
  // [zh] 语法符号
  }
  // [zh] return 返回
  return { theme: 'app', useBrand: true, icon: Ticket };
// [zh] 语句
});

// [zh] 声明变量 `startX`
const startX = ref(0);
// [zh] 声明变量 `startY`
const startY = ref(0);
// [zh] 声明变量 `currentX`
const currentX = ref(0);
// [zh] 声明变量 `currentY`
const currentY = ref(0);
// [zh] 声明变量 `isDragging`
const isDragging = ref(false);
// [zh] 声明变量 `translateX`
const translateX = ref(0);
// [zh] 声明变量 `translateY`
const translateY = ref(0);
// [zh] 声明变量 `didSwipeDismiss`
const didSwipeDismiss = ref(false);

// [zh] 声明变量 `dragStyle`
const dragStyle = computed(() => {
  // [zh] 声明变量 `scale`
  const scale = isDragging.value ? Math.max(0.94, 1 + translateY.value / 400) : 1;
  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`transform: `translate3d(${translateX.value}px, ${transl`
    transform: `translate3d(${translateX.value}px, ${translateY.value}px, 0) scale(${scale})`
  // [zh] 语句
  };
// [zh] 语句
});

// [zh] 声明变量 `show`
const show = (data: Notification) => {
  // [zh] 条件判断 if
  if (!data.notificationId) {
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (shownNotificationIds.has(data.notificationId)) {
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  shownNotificationIds.add(data.notificationId);
  // [zh] 语句
  notification.value = data;
  // [zh] 语句
  visible.value = true;
  // [zh] 语句
  startAutoCloseTimer();

  // [zh] 语句
  notificationApi.clearPopupNotification(data.notificationId).catch(() => {});
// [zh] 语句
};

// [zh] 声明变量 `close`
const close = () => {
  // [zh] 语句
  visible.value = false;
  // [zh] 语句
  stopAutoCloseTimer();

  // [zh] 条件判断 if
  if (notification.value.notificationId) {
    // [zh] 代码块开始
    notificationApi.clearPopupNotification(notification.value.notificationId).catch((e) => {
      // [zh] 语句
      console.error('清除未弹窗通知标记失败', e);
    // [zh] 语句
    });
  // [zh] 语法符号
  }

  // [zh] 语句
  emit('close');
// [zh] 语句
};

// [zh] 声明变量 `autoCloseTimer`
let autoCloseTimer: ReturnType<typeof setTimeout> | null = null;

// [zh] 声明变量 `startAutoCloseTimer`
const startAutoCloseTimer = () => {
  // [zh] 语句
  stopAutoCloseTimer();
  // [zh] 代码块开始
  autoCloseTimer = setTimeout(() => {
    // [zh] 条件判断 if
    if (visible.value) close();
  // [zh] 语句
  }, 5000);
// [zh] 语句
};

// [zh] 声明变量 `stopAutoCloseTimer`
const stopAutoCloseTimer = () => {
  // [zh] 条件判断 if
  if (autoCloseTimer) {
    // [zh] 语句
    clearTimeout(autoCloseTimer);
    // [zh] 语句
    autoCloseTimer = null;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `handleBannerClick`
const handleBannerClick = () => {
  // [zh] 条件判断 if
  if (didSwipeDismiss.value) {
    // [zh] 语句
    didSwipeDismiss.value = false;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  emit('click', notification.value);
  // [zh] 语句
  close();
// [zh] 语句
};

// [zh] 声明变量 `formatTime`
const formatTime = (time?: string) => {
  // [zh] 条件判断 if
  if (!time) return '现在';
  // [zh] 声明变量 `now`
  const now = new Date();
  // [zh] 声明变量 `date`
  const date = new Date(time);
  // [zh] 声明变量 `diff`
  const diff = now.getTime() - date.getTime();
  // [zh] 声明变量 `minutes`
  const minutes = Math.floor(diff / 60000);
  // [zh] 声明变量 `hours`
  const hours = Math.floor(diff / 3600000);

  // [zh] 条件判断 if
  if (minutes < 1) return '现在';
  // [zh] 条件判断 if
  if (minutes < 60) return `${minutes}分钟前`;
  // [zh] 条件判断 if
  if (hours < 24) return `${hours}小时前`;
  // [zh] return 返回
  return formatDisplayDateTime(time);
// [zh] 语句
};

// [zh] 声明变量 `onTouchStart`
const onTouchStart = (e: TouchEvent) => {
  // [zh] 声明变量 `touch`
  const touch = e.touches[0];
  // [zh] 语句
  startX.value = touch.clientX;
  // [zh] 语句
  startY.value = touch.clientY;
  // [zh] 语句
  currentX.value = touch.clientX;
  // [zh] 语句
  currentY.value = touch.clientY;
  // [zh] 语句
  isDragging.value = true;
  // [zh] 语句
  didSwipeDismiss.value = false;
  // [zh] 语句
  stopAutoCloseTimer();
// [zh] 语句
};

// [zh] 声明变量 `onTouchMove`
const onTouchMove = (e: TouchEvent) => {
  // [zh] 条件判断 if
  if (!isDragging.value) return;
  // [zh] 声明变量 `touch`
  const touch = e.touches[0];
  // [zh] 语句
  currentX.value = touch.clientX;
  // [zh] 语句
  currentY.value = touch.clientY;

  // [zh] 声明变量 `deltaX`
  const deltaX = currentX.value - startX.value;
  // [zh] 声明变量 `deltaY`
  const deltaY = currentY.value - startY.value;

  // [zh] 语句
  translateX.value = Math.max(-36, Math.min(36, deltaX * 0.35));
  // [zh] 语句
  translateY.value = Math.max(-72, Math.min(8, deltaY));
// [zh] 语句
};

// [zh] 声明变量 `onTouchEnd`
const onTouchEnd = () => {
  // [zh] 条件判断 if
  if (!isDragging.value) return;

  // [zh] 声明变量 `deltaX`
  const deltaX = startX.value - currentX.value;
  // [zh] 声明变量 `deltaY`
  const deltaY = startY.value - currentY.value;
  // [zh] 声明变量 `threshold`
  const threshold = 44;

  // [zh] 条件判断 if
  if (deltaX > threshold || deltaY > threshold) {
    // [zh] 语句
    didSwipeDismiss.value = true;
    // [zh] 语句
    close();
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    startAutoCloseTimer();
  // [zh] 语法符号
  }

  // [zh] 语句
  isDragging.value = false;
  // [zh] 语句
  startX.value = 0;
  // [zh] 语句
  startY.value = 0;
  // [zh] 语句
  currentX.value = 0;
  // [zh] 语句
  currentY.value = 0;
  // [zh] 语句
  translateX.value = 0;
  // [zh] 语句
  translateY.value = 0;
// [zh] 语句
};

// [zh] 声明变量 `loadPopupNotification`
const loadPopupNotification = async () => {
  // [zh] 声明变量 `authStore`
  const authStore = useAuthStore();
  // [zh] 条件判断 if
  if (!authStore.isLoggedIn) return;

  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `result`
    const result = await notificationApi.getPopupNotification();
    // [zh] 条件判断 if
    if (result?.notificationId) {
      // [zh] 语句
      show(result);
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } catch (e) {
    // [zh] 语句
    console.error('获取未弹窗通知失败', e);
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `ensureGlobalPoll`
const ensureGlobalPoll = () => {
  // [zh] 条件判断 if
  if (globalPollInitialized) return;
  // [zh] 语句
  globalPollInitialized = true;
  // [zh] 语句
  setInterval(loadPopupNotification, 60000);
  // [zh] 语句
  setTimeout(loadPopupNotification, 2000);
// [zh] 语句
};

// [zh] 声明变量 `onRealtimeNotification`
const onRealtimeNotification = (event: Event) => {
  // [zh] 声明变量 `detail`
  const detail = (event as CustomEvent<NotificationData>).detail;
  // [zh] 条件判断 if
  if (detail?.notificationId) {
    // [zh] 语句
    show(detail);
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 代码块开始
onMounted(() => {
  // [zh] 语句
  window.addEventListener('newNotification', onRealtimeNotification as EventListener);
  // [zh] 语句
  ensureGlobalPoll();
// [zh] 语句
});

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 语句
  window.removeEventListener('newNotification', onRealtimeNotification as EventListener);
  // [zh] 语句
  stopAutoCloseTimer();
// [zh] 语句
});

// [zh] 语句
defineExpose({ show });
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.ios-notification-host {` */
.ios-notification-host {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `inset: 0 auto auto 0;` */
  inset: 0 auto auto 0;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `z-index: 100000;` */
  z-index: 100000;
  /* [zh] 样式规则 `padding: calc(env(safe-area-inset-top, 0` */
  padding: calc(env(safe-area-inset-top, 0px) + 10px) 10px 0;
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-notification-shell {` */
.ios-notification-shell {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: 420px;` */
  max-width: 420px;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `pointer-events: auto;` */
  pointer-events: auto;
  /* [zh] 样式规则 `transform-origin: top center;` */
  transform-origin: top center;
  /* [zh] 样式规则 `transition: transform 0.28s cubic-bezier` */
  transition: transform 0.28s cubic-bezier(0.25, 0.1, 0.25, 1);

  /* [zh] 样式规则 `&.dragging {` */
  &.dragging {
    /* [zh] 样式规则 `transition: none;` */
    transition: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-notification-banner {` */
.ios-notification-banner {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `border-radius: 22px;` */
  border-radius: 22px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
  -webkit-tap-highlight-color: transparent;
  /* [zh] 样式规则 `box-shadow:` */
  box-shadow:
    /* [zh] 样式声明 */
    0 0 0 0.5px rgba(255, 255, 255, 0.55) inset,
    /* [zh] 样式声明 */
    0 10px 40px rgba(0, 0, 0, 0.14),
    /* [zh] 样式声明 */
    0 2px 10px rgba(0, 0, 0, 0.08);
  /* [zh] 样式规则 `transition: transform 0.18s ease, box-sh` */
  transition: transform 0.18s ease, box-shadow 0.18s ease;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.985);` */
    transform: scale(0.985);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-notification-inner {` */
.ios-notification-inner {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `padding: 13px 14px 13px 13px;` */
  padding: 13px 14px 13px 13px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-notification-icon {` */
.ios-notification-icon {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `width: 38px;` */
  width: 38px;
  /* [zh] 样式规则 `height: 38px;` */
  height: 38px;
  /* [zh] 样式规则 `border-radius: 10px;` */
  border-radius: 10px;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `box-shadow:` */
  box-shadow:
    /* [zh] 样式声明 */
    0 1px 2px rgba(0, 0, 0, 0.18),
    /* [zh] 样式声明 */
    inset 0 1px 0 rgba(255, 255, 255, 0.28);

  /* [zh] 样式规则 `&--app {` */
  &--app {
    /* [zh] 样式规则 `background: linear-gradient(160deg, #3a3` */
    background: linear-gradient(160deg, #3a3a3c 0%, #1c1c1e 55%, #2c2c2e 100%);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--logistics {` */
  &--logistics {
    /* [zh] 样式规则 `background: linear-gradient(160deg, #64d` */
    background: linear-gradient(160deg, #64d2ff 0%, #0a84ff 100%);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--coupon {` */
  &--coupon {
    /* [zh] 样式规则 `background: linear-gradient(160deg, #ffe` */
    background: linear-gradient(160deg, #ffe066 0%, #ff9f0a 100%);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--coupon-warn {` */
  &--coupon-warn {
    /* [zh] 样式规则 `background: linear-gradient(160deg, #ffb` */
    background: linear-gradient(160deg, #ffb340 0%, #ff6723 100%);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--order {` */
  &--order {
    /* [zh] 样式规则 `background: linear-gradient(160deg, #bf5` */
    background: linear-gradient(160deg, #bf5af2 0%, #5e5ce6 100%);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--member {` */
  &--member {
    /* [zh] 样式规则 `background: linear-gradient(160deg, #ffd` */
    background: linear-gradient(160deg, #ffd426 0%, #c9a962 100%);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--sign {` */
  &--sign {
    /* [zh] 样式规则 `background: linear-gradient(160deg, #63e` */
    background: linear-gradient(160deg, #63e6a8 0%, #30d158 100%);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-app-mark {` */
.ios-app-mark {
  /* [zh] 样式规则 `width: 22px;` */
  width: 22px;
  /* [zh] 样式规则 `height: 22px;` */
  height: 22px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-type-icon {` */
.ios-type-icon {
  /* [zh] 样式规则 `font-size: 20px;` */
  font-size: 20px;
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `filter: drop-shadow(0 1px 1px rgba(0, 0,` */
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.12));
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-notification-copy {` */
.ios-notification-copy {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `padding-top: 1px;` */
  padding-top: 1px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-notification-meta {` */
.ios-notification-meta {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 5px;` */
  gap: 5px;
  /* [zh] 样式规则 `margin-bottom: 2px;` */
  margin-bottom: 2px;
  /* [zh] 样式规则 `font-family: -apple-system, BlinkMacSyst` */
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', sans-serif;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.2;` */
  line-height: 1.2;
  /* [zh] 样式规则 `color: rgba(60, 60, 67, 0.6);` */
  color: rgba(60, 60, 67, 0.6);
  /* [zh] 样式规则 `letter-spacing: -0.01em;` */
  letter-spacing: -0.01em;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-app-name {` */
.ios-app-name {
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: rgba(60, 60, 67, 0.72);` */
  color: rgba(60, 60, 67, 0.72);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-meta-sep {` */
.ios-meta-sep {
  /* [zh] 样式规则 `opacity: 0.55;` */
  opacity: 0.55;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-meta-time {` */
.ios-meta-time {
  /* [zh] 样式规则 `font-weight: 400;` */
  font-weight: 400;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-notification-title {` */
.ios-notification-title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-family: -apple-system, BlinkMacSyst` */
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'PingFang SC', 'Helvetica Neue', sans-serif;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `line-height: 1.25;` */
  line-height: 1.25;
  /* [zh] 样式规则 `letter-spacing: -0.02em;` */
  letter-spacing: -0.02em;
  /* [zh] 样式规则 `color: #1d1d1f;` */
  color: #1d1d1f;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `text-overflow: ellipsis;` */
  text-overflow: ellipsis;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-notification-body {` */
.ios-notification-body {
  /* [zh] 样式规则 `margin: 2px 0 0;` */
  margin: 2px 0 0;
  /* [zh] 样式规则 `font-family: -apple-system, BlinkMacSyst` */
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', sans-serif;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 400;` */
  font-weight: 400;
  /* [zh] 样式规则 `line-height: 1.32;` */
  line-height: 1.32;
  /* [zh] 样式规则 `letter-spacing: -0.01em;` */
  letter-spacing: -0.01em;
  /* [zh] 样式规则 `color: rgba(60, 60, 67, 0.88);` */
  color: rgba(60, 60, 67, 0.88);
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `text-overflow: ellipsis;` */
  text-overflow: ellipsis;
  /* [zh] 样式规则 `display: -webkit-box;` */
  display: -webkit-box;
  /* [zh] 样式规则 `-webkit-line-clamp: 2;` */
  -webkit-line-clamp: 2;
  /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
  -webkit-box-orient: vertical;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ios-notification-chevron {` */
.ios-notification-chevron {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `align-self: center;` */
  align-self: center;
  /* [zh] 样式规则 `color: rgba(60, 60, 67, 0.28);` */
  color: rgba(60, 60, 67, 0.28);
  /* [zh] 样式规则 `opacity: 0;` */
  opacity: 0;
  /* [zh] 样式规则 `transition: opacity 0.2s ease;` */
  transition: opacity 0.2s ease;

  /* [zh] 样式规则 `@media (min-width: 768px) {` */
  @media (min-width: 768px) {
    /* [zh] 样式规则 `opacity: 1;` */
    opacity: 1;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (min-width: 768px) {` */
@media (min-width: 768px) {
  /* [zh] 样式规则 `.ios-notification-host {` */
  .ios-notification-host {
    /* [zh] 样式规则 `padding-top: calc(env(safe-area-inset-to` */
    padding-top: calc(env(safe-area-inset-top, 0px) + 16px);
    /* [zh] 样式规则 `padding-inline: 16px;` */
    padding-inline: 16px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.ios-notification-shell {` */
  .ios-notification-shell {
    /* [zh] 样式规则 `max-width: 390px;` */
    max-width: 390px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.ios-notification-inner {` */
  .ios-notification-inner {
    /* [zh] 样式规则 `padding: 14px 16px 14px 14px;` */
    padding: 14px 16px 14px 14px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.ios-notification-enter-active .ios-notification-shell,
/* [zh] 样式规则 `.ios-notification-leave-active .ios-noti` */
.ios-notification-leave-active .ios-notification-shell {
  /* [zh] 样式规则 `transition:` */
  transition:
    /* [zh] 样式声明 */
    transform 0.42s cubic-bezier(0.22, 1, 0.36, 1),
    /* [zh] 样式声明 */
    opacity 0.32s ease;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.ios-notification-enter-from .ios-notification-shell,
/* [zh] 样式规则 `.ios-notification-leave-to .ios-notifica` */
.ios-notification-leave-to .ios-notification-shell {
  /* [zh] 样式规则 `transform: translate3d(0, calc(-100% - 1` */
  transform: translate3d(0, calc(-100% - 18px), 0) scale(0.96);
  /* [zh] 样式规则 `opacity: 0;` */
  opacity: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.ios-notification-enter-to .ios-notification-shell,
/* [zh] 样式规则 `.ios-notification-leave-from .ios-notifi` */
.ios-notification-leave-from .ios-notification-shell {
  /* [zh] 样式规则 `transform: translate3d(0, 0, 0) scale(1)` */
  transform: translate3d(0, 0, 0) scale(1);
  /* [zh] 样式规则 `opacity: 1;` */
  opacity: 1;
/* [zh] 样式规则 `}` */
}
</style>
