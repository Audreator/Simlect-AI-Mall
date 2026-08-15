// [zh] 导入 `@/stores/agentMessage`
import { useAgentMessageStore } from '@/stores/agentMessage';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/utils/websocket/manager`
import { ensureAppWebSocket } from '@/utils/websocket/manager';

// [zh] 导出模块成员
export function useAgentSession() {
  // [zh] 声明变量 `start`
  const start = async () => {
    // [zh] 语句
    document.documentElement.style.setProperty('--agent-composer-inset', '0px');
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await useAuthStore().ensureSession();
    // [zh] 语句
    ensureAppWebSocket();
  // [zh] 语句
  };

  // [zh] 声明变量 `stop`
  const stop = () => {
    // [zh] 语句
    document.documentElement.style.removeProperty('--agent-composer-inset');
    // [zh] 语句
    useAgentMessageStore().clearMessage();
  // [zh] 语句
  };

  // [zh] return 返回
  return { start, stop };
// [zh] 语法符号
}
