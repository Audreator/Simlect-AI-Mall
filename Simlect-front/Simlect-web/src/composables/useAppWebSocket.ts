// [zh] 导入 `vue`
import { onMounted, onUnmounted, watch } from 'vue';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/composables/useUnreadCount`
import { useUnreadCount } from '@/composables/useUnreadCount';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`bindUnreadRefreshHandler,`
  bindUnreadRefreshHandler,
  // [zh] TypeScript/JS 语句：`closeAppWebSocket,`
  closeAppWebSocket,
  // [zh] TypeScript/JS 语句：`initAppWebSocket`
  initAppWebSocket
// [zh] 语句
} from '@/utils/websocket/manager';


// [zh] 导出模块成员
export function useAppWebSocket() {
  // [zh] 声明变量 `authStore`
  const authStore = useAuthStore();
  // [zh] 语句
  const { refreshUnreadCount } = useUnreadCount();

  // [zh] 代码块开始
  onMounted(() => {
    // [zh] 代码块开始
    bindUnreadRefreshHandler(() => {
      // [zh] 语句
      void refreshUnreadCount();
    // [zh] 语句
    });
  // [zh] 语句
  });

  // [zh] 代码块开始
  onUnmounted(() => {
    // [zh] 语句
    bindUnreadRefreshHandler(null);
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
        initAppWebSocket();
      // [zh] 代码块开始
      } else {
        // [zh] 语句
        closeAppWebSocket();
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ immediate: true }`
    { immediate: true }
  // [zh] 语句
  );
// [zh] 语法符号
}
