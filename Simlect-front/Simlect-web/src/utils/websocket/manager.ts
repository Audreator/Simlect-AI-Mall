// [zh] 导入 `@/stores/agentMessage`
import { useAgentMessageStore } from '@/stores/agentMessage';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/utils/notification`
import { showNotification, type NotificationData } from '@/utils/notification';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/utils/websocket/url`
import { resolveAgentWsUrl } from '@/utils/websocket/url';

// [zh] 导出模块成员
export interface AppWsMessage {
  // [zh] 语句
  messageType?: 'agent' | 'notify' | string;
  // [zh] 语句
  messageId?: number | string;
  // [zh] 语句
  userMessage?: string;
  // [zh] 语句
  assistantMessage?: string;
  // [zh] 语句
  bizType?: string;
  // [zh] 语句
  bizId?: string;
  // [zh] 语句
  outPutType?: number;
  // [zh] 语句
  userId?: string;
  // [zh] 语句
  notificationId?: string;
  // [zh] 语句
  title?: string;
  // [zh] 语句
  content?: string;
  // [zh] 语句
  createTime?: string;
// [zh] 语法符号
}

// [zh] TypeScript 类型 `UnreadRefreshHandler`（类比 Java interface/DTO）
type UnreadRefreshHandler = () => void | Promise<void>;

// [zh] 声明变量 `ws`
let ws: WebSocket | null = null;
// [zh] 声明变量 `maxRetries`
let maxRetries = 5;
// [zh] 声明变量 `retryInterval`
let retryInterval = 2000;
// [zh] 声明变量 `isConnecting`
let isConnecting = false;
// [zh] 声明变量 `retryCount`
let retryCount = 0;
// [zh] 声明变量 `heartbeatTimer`
let heartbeatTimer: ReturnType<typeof setInterval> | null = null;
// [zh] 声明变量 `HEARTBEAT_INTERVAL`
const HEARTBEAT_INTERVAL = 5000;
// [zh] 声明变量 `needReconnect`
let needReconnect = true;
// [zh] 声明变量 `reconnectTimer`
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
// [zh] 声明变量 `unreadRefreshHandler`
let unreadRefreshHandler: UnreadRefreshHandler | null = null;

// [zh] 声明变量 `wsCheckEnabled`
const wsCheckEnabled = () => import.meta.env.VITE_WS_CHECK === 'true';

// [zh] 声明变量 `isNotifyMessage`
const isNotifyMessage = (data: AppWsMessage) =>
  // [zh] 语句
  data.messageType === 'notify' || Boolean(data.notificationId && data.title);

// [zh] 声明变量 `handleNotifyMessage`
const handleNotifyMessage = (data: AppWsMessage) => {
  // [zh] 声明变量 `payload`
  const payload: NotificationData = {
    // [zh] TypeScript/JS 语句：`notificationId: data.notificationId || '',`
    notificationId: data.notificationId || '',
    // [zh] TypeScript/JS 语句：`title: data.title || '',`
    title: data.title || '',
    // [zh] TypeScript/JS 语句：`content: data.content || '',`
    content: data.content || '',
    // [zh] TypeScript/JS 语句：`bizType: data.bizType,`
    bizType: data.bizType,
    // [zh] TypeScript/JS 语句：`bizId: data.bizId,`
    bizId: data.bizId,
    // [zh] TypeScript/JS 语句：`createTime: data.createTime,`
    createTime: data.createTime,
    // [zh] TypeScript/JS 语句：`readStatus: 0`
    readStatus: 0
  // [zh] 语句
  };
  // [zh] 条件判断 if
  if (payload.notificationId && payload.title) {
    // [zh] 语句
    showNotification(payload);
  // [zh] 语法符号
  }
  // [zh] 语句
  void unreadRefreshHandler?.();
// [zh] 语句
};

// [zh] 声明变量 `handleAgentMessage`
const handleAgentMessage = (data: AppWsMessage) => {
  // [zh] 语句
  useAgentMessageStore().onMessage(data);
// [zh] 语句
};

// [zh] 声明变量 `dispatchWsMessage`
const dispatchWsMessage = (data: AppWsMessage) => {
  // [zh] 条件判断 if
  if (isNotifyMessage(data)) {
    // [zh] 语句
    handleNotifyMessage(data);
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  handleAgentMessage(data);
// [zh] 语句
};

// [zh] 声明变量 `clearHeartbeat`
const clearHeartbeat = () => {
  // [zh] 条件判断 if
  if (heartbeatTimer) {
    // [zh] 语句
    clearInterval(heartbeatTimer);
    // [zh] 语句
    heartbeatTimer = null;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `startHeartbeat`
const startHeartbeat = () => {
  // [zh] 语句
  clearHeartbeat();
  // [zh] 代码块开始
  heartbeatTimer = setInterval(() => {
    // [zh] 条件判断 if
    if (ws?.readyState === WebSocket.OPEN) ws.send('ping');
  // [zh] 语句
  }, HEARTBEAT_INTERVAL);
// [zh] 语句
};

// [zh] 声明变量 `handleReconnect`
const handleReconnect = () => {
  // [zh] 条件判断 if
  if (!needReconnect || isConnecting) return;
  // [zh] 条件判断 if
  if (retryCount >= maxRetries) {
    // [zh] 语句
    closeAppWebSocket();
    // [zh] 条件判断 if
    if (wsCheckEnabled()) {
      // [zh] 语句
      toast.warning('实时连接断开，请刷新页面或重新进入智能客服');
    // [zh] 语法符号
    }
    // [zh] 语句
    retryCount = 0;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  retryCount += 1;
  // [zh] 语句
  isConnecting = false;
  // [zh] 声明变量 `delay`
  const delay = retryInterval * Math.pow(1.5, retryCount - 1);
  // [zh] 条件判断 if
  if (wsCheckEnabled()) {
    // [zh] 语句
    console.warn(`WebSocket 重连: 第 ${retryCount} 次，${delay}ms 后重试`);
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (reconnectTimer) clearTimeout(reconnectTimer);
  // [zh] 语句
  reconnectTimer = setTimeout(connectWs, delay);
// [zh] 语句
};

// [zh] 声明变量 `connectWs`
const connectWs = () => {
  // [zh] 条件判断 if
  if (isConnecting || !needReconnect) return;
  // [zh] 声明变量 `authStore`
  const authStore = useAuthStore();
  // [zh] 条件判断 if
  if (!authStore.isLoggedIn) return;

  // [zh] 语句
  isConnecting = true;
  // [zh] 声明变量 `wsUrl`
  const wsUrl = resolveAgentWsUrl();
  // [zh] 代码块开始
  try {
    // [zh] 语句
    ws = new WebSocket(wsUrl);
    // [zh] 代码块开始
    ws.onopen = () => {
      // [zh] 语句
      isConnecting = false;
      // [zh] 语句
      retryCount = 0;
      // [zh] 语句
      startHeartbeat();
    // [zh] 语句
    };
    // [zh] 代码块开始
    ws.onmessage = (event) => {
      // [zh] 声明变量 `raw`
      const raw = typeof event.data === 'string' ? event.data.trim() : '';
      // [zh] 条件判断 if
      if (!raw || raw === 'ping' || raw === 'pong') return;
      // [zh] 代码块开始
      try {
        // [zh] 语句
        dispatchWsMessage(JSON.parse(raw) as AppWsMessage);
      // [zh] 代码块开始
      } catch {

      // [zh] 语法符号
      }
    // [zh] 语句
    };
    // [zh] 语句
    ws.onerror = () => handleReconnect();
    // [zh] 代码块开始
    ws.onclose = (event) => {
      // [zh] 语句
      isConnecting = false;
      // [zh] 语句
      clearHeartbeat();
      // [zh] 条件判断 if
      if (event.code !== 1000) handleReconnect();
    // [zh] 语句
    };
  // [zh] 代码块开始
  } catch {
    // [zh] 语句
    handleReconnect();
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 导出模块成员
export const ensureAppWebSocket = () => {
  // [zh] 条件判断 if
  if (ws?.readyState === WebSocket.OPEN || ws?.readyState === WebSocket.CONNECTING) {
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  initAppWebSocket({ force: true });
// [zh] 语句
};

// [zh] 导出模块成员
export const initAppWebSocket = (options?: { force?: boolean }) => {
  // [zh] 条件判断 if
  if (options?.force) closeAppWebSocket();
  // [zh] 条件判断 if
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return;

  // [zh] 声明变量 `authStore`
  const authStore = useAuthStore();
  // [zh] 条件判断 if
  if (!authStore.isLoggedIn) return;

  // [zh] 语句
  needReconnect = true;
  // [zh] 语句
  connectWs();
// [zh] 语句
};

// [zh] 导出模块成员
export const closeAppWebSocket = () => {
  // [zh] 语句
  needReconnect = false;
  // [zh] 语句
  isConnecting = false;
  // [zh] 语句
  clearHeartbeat();
  // [zh] 条件判断 if
  if (reconnectTimer) {
    // [zh] 语句
    clearTimeout(reconnectTimer);
    // [zh] 语句
    reconnectTimer = null;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    // [zh] 语句
    ws.close();
  // [zh] 语法符号
  }
  // [zh] 语句
  ws = null;
// [zh] 语句
};

// [zh] 导出模块成员
export const bindUnreadRefreshHandler = (handler: UnreadRefreshHandler | null) => {
  // [zh] 语句
  unreadRefreshHandler = handler;
// [zh] 语句
};

// [zh] 导出模块成员
export const initAgentWebSocket = initAppWebSocket;

// [zh] 导出模块成员
export const closeAgentWebSocket = closeAppWebSocket;
