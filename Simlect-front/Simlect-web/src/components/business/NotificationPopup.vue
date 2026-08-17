<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Transition>` -->
  <Transition name="ios-notification">
    <div v-if="visible" class="ios-notification-host">
      <div
        class="ios-notification-shell"
        :class="{ dragging: isDragging }"
        :style="dragStyle"
      >
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
          <div class="ios-notification-inner">
            <div class="ios-notification-icon" :class="`ios-notification-icon--${iconVisual.theme}`">
              <BrandMark
                v-if="iconVisual.useBrand"
                class="ios-app-mark"
                variant="light"
              />
              <el-icon v-else class="ios-type-icon">
                <component :is="iconVisual.icon" />
              </el-icon>
            </div>

            <div class="ios-notification-copy">
              <div class="ios-notification-meta">
                <span class="ios-app-name">简选</span>
                <span class="ios-meta-sep" aria-hidden="true">·</span>
                <span class="ios-meta-time">{{ formatTime(notification.createTime) }}</span>
              </div>
              <p class="ios-notification-title">{{ notification.title }}</p>
              <p class="ios-notification-body">{{ notification.content }}</p>
            </div>

            <span class="ios-notification-chevron" aria-hidden="true">
              <el-icon :size="14"><ArrowRight /></el-icon>
            </span>
          </div>
        </LiquidGlassSurface>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, type Component } from 'vue';
import {
  AlarmClock,
  ArrowRight,
  Box,
  Calendar,
  ShoppingBag,
  StarFilled,
  Ticket
} from '@element-plus/icons-vue';
import BrandMark from '@/components/common/BrandMark.vue';
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
import { notificationApi } from '@/api/modules';
import { useAuthStore } from '@/stores/auth';
import type { NotificationData } from '@/utils/notification';
import { formatDisplayDateTime } from '@/utils/formatDateTime';

interface Notification {
  notificationId: string;
  title: string;
  content: string;
  bizType?: string;
  bizId?: string;
  createTime?: string;
}

type IconTheme = 'app' | 'logistics' | 'coupon' | 'coupon-warn' | 'order' | 'member' | 'sign';

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'click', notification: Notification): void;
}>();

const shownNotificationIds = new Set<string>();
let globalPollInitialized = false;

const visible = ref(false);
const notification = ref<Notification>({
  notificationId: '',
  title: '',
  content: ''
});

const iconVisual = computed((): { theme: IconTheme; useBrand: boolean; icon: Component } => {
  const bizType = notification.value.bizType || '';

  if (bizType === 'rush_coupon' || bizType === 'coupon_rush') {
    return { theme: 'coupon', useBrand: false, icon: Ticket };
  }
  if (bizType === 'coupon_expire' || bizType === 'coupon') {
    return { theme: 'coupon-warn', useBrand: false, icon: AlarmClock };
  }
  if (bizType === 'logistics') {
    return { theme: 'logistics', useBrand: false, icon: Box };
  }
  if (bizType === 'order') {
    return { theme: 'order', useBrand: false, icon: ShoppingBag };
  }
  if (bizType === 'member_level') {
    return { theme: 'member', useBrand: false, icon: StarFilled };
  }
  if (bizType === 'sign') {
    return { theme: 'sign', useBrand: false, icon: Calendar };
  }
  return { theme: 'app', useBrand: true, icon: Ticket };
});

const startX = ref(0);
const startY = ref(0);
const currentX = ref(0);
const currentY = ref(0);
const isDragging = ref(false);
const translateX = ref(0);
const translateY = ref(0);
const didSwipeDismiss = ref(false);

const dragStyle = computed(() => {
  const scale = isDragging.value ? Math.max(0.94, 1 + translateY.value / 400) : 1;
  return {
    transform: `translate3d(${translateX.value}px, ${translateY.value}px, 0) scale(${scale})`
  };
});

const show = (data: Notification) => {
  if (!data.notificationId) {
    return;
  }
  if (shownNotificationIds.has(data.notificationId)) {
    return;
  }
  shownNotificationIds.add(data.notificationId);
  notification.value = data;
  visible.value = true;
  startAutoCloseTimer();

  notificationApi.clearPopupNotification(data.notificationId).catch(() => {});
};

const close = () => {
  visible.value = false;
  stopAutoCloseTimer();

  if (notification.value.notificationId) {
    notificationApi.clearPopupNotification(notification.value.notificationId).catch((e) => {
      console.error('清除未弹窗通知标记失败', e);
    });
  }

  emit('close');
};

let autoCloseTimer: ReturnType<typeof setTimeout> | null = null;

const startAutoCloseTimer = () => {
  stopAutoCloseTimer();
  autoCloseTimer = setTimeout(() => {
    if (visible.value) close();
  }, 5000);
};

const stopAutoCloseTimer = () => {
  if (autoCloseTimer) {
    clearTimeout(autoCloseTimer);
    autoCloseTimer = null;
  }
};

const handleBannerClick = () => {
  if (didSwipeDismiss.value) {
    didSwipeDismiss.value = false;
    return;
  }
  emit('click', notification.value);
  close();
};

const formatTime = (time?: string) => {
  if (!time) return '现在';
  const now = new Date();
  const date = new Date(time);
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);

  if (minutes < 1) return '现在';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  return formatDisplayDateTime(time);
};

const onTouchStart = (e: TouchEvent) => {
  const touch = e.touches[0];
  startX.value = touch.clientX;
  startY.value = touch.clientY;
  currentX.value = touch.clientX;
  currentY.value = touch.clientY;
  isDragging.value = true;
  didSwipeDismiss.value = false;
  stopAutoCloseTimer();
};

const onTouchMove = (e: TouchEvent) => {
  if (!isDragging.value) return;
  const touch = e.touches[0];
  currentX.value = touch.clientX;
  currentY.value = touch.clientY;

  const deltaX = currentX.value - startX.value;
  const deltaY = currentY.value - startY.value;

  translateX.value = Math.max(-36, Math.min(36, deltaX * 0.35));
  translateY.value = Math.max(-72, Math.min(8, deltaY));
};

const onTouchEnd = () => {
  if (!isDragging.value) return;

  const deltaX = startX.value - currentX.value;
  const deltaY = startY.value - currentY.value;
  const threshold = 44;

  if (deltaX > threshold || deltaY > threshold) {
    didSwipeDismiss.value = true;
    close();
  } else {
    startAutoCloseTimer();
  }

  isDragging.value = false;
  startX.value = 0;
  startY.value = 0;
  currentX.value = 0;
  currentY.value = 0;
  translateX.value = 0;
  translateY.value = 0;
};

const loadPopupNotification = async () => {
  const authStore = useAuthStore();
  if (!authStore.isLoggedIn) return;

  try {
    const result = await notificationApi.getPopupNotification();
    if (result?.notificationId) {
      show(result);
    }
  } catch (e) {
    console.error('获取未弹窗通知失败', e);
  }
};

const ensureGlobalPoll = () => {
  if (globalPollInitialized) return;
  globalPollInitialized = true;
  setInterval(loadPopupNotification, 60000);
  setTimeout(loadPopupNotification, 2000);
};

const onRealtimeNotification = (event: Event) => {
  const detail = (event as CustomEvent<NotificationData>).detail;
  if (detail?.notificationId) {
    show(detail);
  }
};

onMounted(() => {
  window.addEventListener('newNotification', onRealtimeNotification as EventListener);
  ensureGlobalPoll();
});

onUnmounted(() => {
  window.removeEventListener('newNotification', onRealtimeNotification as EventListener);
  stopAutoCloseTimer();
});

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
