let ws: WebSocket | null = null;
let maxRetries = 5;
let retryInterval = 2000;
let isConnecting = false;
let retryCount = 0;
let heartbeatTimer: ReturnType<typeof setInterval> | null = null;
const HEARTBEAT_INTERVAL = 5000;
let wsUrl = '';
let needReconnect = true;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

const connectWs = () => {
  if (isConnecting || !needReconnect) return;
  isConnecting = true;
  try {
    ws = new WebSocket(wsUrl);
    ws.onopen = () => {
      isConnecting = false;
      retryCount = 0;
      startHeartbeat();
    };
    ws.onmessage = (event) => {
      const raw = typeof event.data === 'string' ? event.data.trim() : '';
      if (!raw || raw === 'ping' || raw === 'pong') return;
      try {
        const data = JSON.parse(raw);
        self.postMessage({ type: 'message', data });
      } catch {

      }
    };
    ws.onerror = () => handleReconnect();
    ws.onclose = (event) => {
      isConnecting = false;
      clearHeartbeat();
      if (event.code !== 1000) handleReconnect();
    };
  } catch {
    handleReconnect();
  }
};

const startHeartbeat = () => {
  clearHeartbeat();
  heartbeatTimer = setInterval(() => {
    if (ws?.readyState === WebSocket.OPEN) ws.send('ping');
  }, HEARTBEAT_INTERVAL);
};

const clearHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
};

const handleReconnect = () => {
  if (!needReconnect || isConnecting) return;
  if (retryCount >= maxRetries) {
    self.postMessage({ type: 'failed' });
    retryCount = 0;
    return;
  }
  retryCount += 1;
  isConnecting = false;
  const delay = retryInterval * Math.pow(1.5, retryCount - 1);
  self.postMessage({ type: 'reconnecting', retryCount, delay });
  if (reconnectTimer) clearTimeout(reconnectTimer);
  reconnectTimer = setTimeout(connectWs, delay);
};

const closeWs = () => {
  needReconnect = false;
  isConnecting = false;
  clearHeartbeat();
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    ws.close();
    ws = null;
  }
};

self.onmessage = (e: MessageEvent<{ type: string; data?: { wsUrl: string } }>) => {
  const { type, data } = e.data;
  if (type === 'init' && data?.wsUrl) {
    wsUrl = data.wsUrl;
    needReconnect = true;
    connectWs();
  } else if (type === 'close') {
    closeWs();
  }
};
