// [zh] 导入 `vue`
import { ref } from 'vue';
// [zh] 导入 `pinia`
import { defineStore } from 'pinia';

// [zh] 导出模块成员
export interface AgentWsMessage {
  // [zh] 语句
  messageId?: number | string;
  // [zh] 语句
  userMessage?: string;
  // [zh] 语句
  assistantMessage?: string;
  // [zh] 语句
  bizType?: string;
  // [zh] 语句
  outPutType?: number;
  // [zh] 语句
  userId?: string;
// [zh] 语法符号
}

// [zh] 导出模块成员
export const useAgentMessageStore = defineStore('agentMessage', () => {
  // [zh] 声明变量 `message`
  const message = ref<AgentWsMessage | null>(null);

  // [zh] 声明变量 `onMessage`
  const onMessage = (payload: AgentWsMessage) => {
    // [zh] 语句
    message.value = payload;
  // [zh] 语句
  };

  // [zh] 声明变量 `clearMessage`
  const clearMessage = () => {
    // [zh] 语句
    message.value = null;
  // [zh] 语句
  };

  // [zh] return 返回
  return { message, onMessage, clearMessage };
// [zh] 语句
});
