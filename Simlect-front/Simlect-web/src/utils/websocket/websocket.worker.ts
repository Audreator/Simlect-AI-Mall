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
// [zh] 声明变量 `wsUrl`
let wsUrl = '';
// [zh] 声明变量 `needReconnect`
let needReconnect = true;
// [zh] 声明变量 `reconnectTimer`
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

// [zh] 声明变量 `connectWs`
const connectWs = () => {
  // [zh] 条件判断 if
  if (isConnecting || !needReconnect) return;
  // [zh] 语句
  isConnecting = true;
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
        // [zh] 声明变量 `data`
        const data = JSON.parse(raw);
        // [zh] 语句
        self.postMessage({ type: 'message', data });
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

// [zh] 声明变量 `handleReconnect`
const handleReconnect = () => {
  // [zh] 条件判断 if
  if (!needReconnect || isConnecting) return;
  // [zh] 条件判断 if
  if (retryCount >= maxRetries) {
    // [zh] 语句
    self.postMessage({ type: 'failed' });
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
  // [zh] 语句
  self.postMessage({ type: 'reconnecting', retryCount, delay });
  // [zh] 条件判断 if
  if (reconnectTimer) clearTimeout(reconnectTimer);
  // [zh] 语句
  reconnectTimer = setTimeout(connectWs, delay);
// [zh] 语句
};

// [zh] 声明变量 `closeWs`
const closeWs = () => {
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
    // [zh] 语句
    ws = null;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 代码块开始
self.onmessage = (e: MessageEvent<{ type: string; data?: { wsUrl: string } }>) => {
  // [zh] 语句
  const { type, data } = e.data;
  // [zh] 条件判断 if
  if (type === 'init' && data?.wsUrl) {
    // [zh] 语句
    wsUrl = data.wsUrl;
    // [zh] 语句
    needReconnect = true;
    // [zh] 语句
    connectWs();
  // [zh] 代码块开始
  } else if (type === 'close') {
    // [zh] 语句
    closeWs();
  // [zh] 语法符号
  }
// [zh] 语句
};
