import { onMounted, onUnmounted, ref, watch } from 'vue';
import { notificationApi } from '@/api/modules';
import { useAuthStore } from '@/stores/auth';

const unreadCount = ref(0);
const previousUnreadCount = ref(0);
let mountedConsumers = 0;
let pollTimer: ReturnType<typeof setInterval> | null = null;

export function useUnreadCount() {
  const authStore = useAuthStore();

  const refreshUnreadCount = async () => {
    if (!authStore.isLoggedIn) {
      unreadCount.value = 0;
      previousUnreadCount.value = 0;
      return;
    }
    try {
      const n = await notificationApi.countUnread();
      const newCount = typeof n === 'number' ? n : Number(n) || 0;
      previousUnreadCount.value = unreadCount.value;
      unreadCount.value = newCount;
      return newCount;
    } catch {
      unreadCount.value = 0;
      previousUnreadCount.value = 0;
      return 0;
    }
  };

  const hasNewNotification = () => {
    return unreadCount.value > previousUnreadCount.value;
  };

  const startPolling = () => {
    if (pollTimer) return;

    pollTimer = setInterval(async () => {
      if (authStore.isLoggedIn) {
        await refreshUnreadCount();
      }
    }, 30000);
  };

  const stopPolling = () => {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  };

  onMounted(() => {
    mountedConsumers += 1;
    if (mountedConsumers === 1 && authStore.isLoggedIn) {

      setTimeout(() => refreshUnreadCount(), 2000);
      startPolling();
    }
  });

  onUnmounted(() => {
    mountedConsumers -= 1;
    if (mountedConsumers <= 0) {
      stopPolling();
    }
  });

  watch(
    () => authStore.isLoggedIn,
    (loggedIn) => {
      if (loggedIn) {
        refreshUnreadCount();
        startPolling();
      } else {
        unreadCount.value = 0;
        previousUnreadCount.value = 0;
        stopPolling();
      }
    },
    { immediate: true }
  );

  return { unreadCount, refreshUnreadCount, hasNewNotification };
}
