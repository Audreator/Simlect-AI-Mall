import { useAgentMessageStore } from '@/stores/agentMessage';
import { useAuthStore } from '@/stores/auth';
import { ensureAppWebSocket } from '@/utils/websocket/manager';

export function useAgentSession() {
  const start = async () => {
    document.documentElement.style.setProperty('--agent-composer-inset', '0px');
    await useAuthStore().ensureSession();
    ensureAppWebSocket();
  };

  const stop = () => {
    document.documentElement.style.removeProperty('--agent-composer-inset');
    useAgentMessageStore().clearMessage();
  };

  return { start, stop };
}
