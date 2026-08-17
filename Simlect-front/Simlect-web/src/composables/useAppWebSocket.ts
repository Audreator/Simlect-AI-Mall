import { onMounted, onUnmounted, watch } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useUnreadCount } from '@/composables/useUnreadCount';
import {
  bindUnreadRefreshHandler,
  closeAppWebSocket,
  initAppWebSocket
} from '@/utils/websocket/manager';


export function useAppWebSocket() {
  const authStore = useAuthStore();
  const { refreshUnreadCount } = useUnreadCount();

  onMounted(() => {
    bindUnreadRefreshHandler(() => {
      void refreshUnreadCount();
    });
  });

  onUnmounted(() => {
    bindUnreadRefreshHandler(null);
  });

  watch(
    () => authStore.isLoggedIn,
    (loggedIn) => {
      if (loggedIn) {
        initAppWebSocket();
      } else {
        closeAppWebSocket();
      }
    },
    { immediate: true }
  );
}
