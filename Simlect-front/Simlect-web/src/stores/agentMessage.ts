import { ref } from 'vue';
import { defineStore } from 'pinia';

export interface AgentWsMessage {
  messageId?: number | string;
  userMessage?: string;
  assistantMessage?: string;
  bizType?: string;
  outPutType?: number;
  userId?: string;
}

export const useAgentMessageStore = defineStore('agentMessage', () => {
  const message = ref<AgentWsMessage | null>(null);

  const onMessage = (payload: AgentWsMessage) => {
    message.value = payload;
  };

  const clearMessage = () => {
    message.value = null;
  };

  return { message, onMessage, clearMessage };
});
