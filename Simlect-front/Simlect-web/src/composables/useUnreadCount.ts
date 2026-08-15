// [zh] 导入 `vue`
import { onMounted, onUnmounted, ref, watch } from 'vue';
// [zh] 导入 `@/api/modules`
import { notificationApi } from '@/api/modules';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';

// [zh] 声明变量 `unreadCount`
const unreadCount = ref(0);
// [zh] 声明变量 `previousUnreadCount`
const previousUnreadCount = ref(0);
// [zh] 声明变量 `mountedConsumers`
let mountedConsumers = 0;
// [zh] 声明变量 `pollTimer`
let pollTimer: ReturnType<typeof setInterval> | null = null;

// [zh] 导出模块成员
export function useUnreadCount() {
  // [zh] 声明变量 `authStore`
  const authStore = useAuthStore();

  // [zh] 声明变量 `refreshUnreadCount`
  const refreshUnreadCount = async () => {
    // [zh] 条件判断 if
    if (!authStore.isLoggedIn) {
      // [zh] 语句
      unreadCount.value = 0;
      // [zh] 语句
      previousUnreadCount.value = 0;
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `n`
      const n = await notificationApi.countUnread();
      // [zh] 声明变量 `newCount`
      const newCount = typeof n === 'number' ? n : Number(n) || 0;
      // [zh] 语句
      previousUnreadCount.value = unreadCount.value;
      // [zh] 语句
      unreadCount.value = newCount;
      // [zh] return 返回
      return newCount;
    // [zh] 代码块开始
    } catch {
      // [zh] 语句
      unreadCount.value = 0;
      // [zh] 语句
      previousUnreadCount.value = 0;
      // [zh] return 返回
      return 0;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `hasNewNotification`
  const hasNewNotification = () => {
    // [zh] return 返回
    return unreadCount.value > previousUnreadCount.value;
  // [zh] 语句
  };

  // [zh] 声明变量 `startPolling`
  const startPolling = () => {
    // [zh] 条件判断 if
    if (pollTimer) return;

    // [zh] 代码块开始
    pollTimer = setInterval(async () => {
      // [zh] 条件判断 if
      if (authStore.isLoggedIn) {
        // [zh] await 等待 Promise（类比 CompletableFuture.get）
        await refreshUnreadCount();
      // [zh] 语法符号
      }
    // [zh] 语句
    }, 30000);
  // [zh] 语句
  };

  // [zh] 声明变量 `stopPolling`
  const stopPolling = () => {
    // [zh] 条件判断 if
    if (pollTimer) {
      // [zh] 语句
      clearInterval(pollTimer);
      // [zh] 语句
      pollTimer = null;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 代码块开始
  onMounted(() => {
    // [zh] 语句
    mountedConsumers += 1;
    // [zh] 条件判断 if
    if (mountedConsumers === 1 && authStore.isLoggedIn) {

      // [zh] 语句
      setTimeout(() => refreshUnreadCount(), 2000);
      // [zh] 语句
      startPolling();
    // [zh] 语法符号
    }
  // [zh] 语句
  });

  // [zh] 代码块开始
  onUnmounted(() => {
    // [zh] 语句
    mountedConsumers -= 1;
    // [zh] 条件判断 if
    if (mountedConsumers <= 0) {
      // [zh] 语句
      stopPolling();
    // [zh] 语法符号
    }
  // [zh] 语句
  });

  // [zh] TypeScript/JS 语句：`watch(`
  watch(
    // [zh] TypeScript/JS 语句：`() => authStore.isLoggedIn,`
    () => authStore.isLoggedIn,
    // [zh] 代码块开始
    (loggedIn) => {
      // [zh] 条件判断 if
      if (loggedIn) {
        // [zh] 语句
        refreshUnreadCount();
        // [zh] 语句
        startPolling();
      // [zh] 代码块开始
      } else {
        // [zh] 语句
        unreadCount.value = 0;
        // [zh] 语句
        previousUnreadCount.value = 0;
        // [zh] 语句
        stopPolling();
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ immediate: true }`
    { immediate: true }
  // [zh] 语句
  );

  // [zh] return 返回
  return { unreadCount, refreshUnreadCount, hasNewNotification };
// [zh] 语法符号
}
