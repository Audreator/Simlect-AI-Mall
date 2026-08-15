

// [zh] 导出模块成员
export interface AgentHistoryMessage {
  // [zh] 语句
  messageId: number;
  // [zh] 语句
  userMessage?: string;
  // [zh] 语句
  assistantMessage: string;
  // [zh] 语句
  status: number;
  // [zh] 语句
  bizType?: string;
  // [zh] 语句
  bizData?: string | null;
  // [zh] 语句
  sendTime?: string;
// [zh] 语法符号
}

// [zh] 声明变量 `pickField`
const pickField = (raw: Record<string, unknown>, ...keys: string[]) => {
  // [zh] for 循环
  for (const key of keys) {
    // [zh] 声明变量 `val`
    const val = raw[key];
    // [zh] 条件判断 if
    if (val != null && val !== '') return val;
  // [zh] 语法符号
  }
  // [zh] return 返回
  return undefined;
// [zh] 语句
};

// [zh] 导出模块成员
export const extractHistoryPage = (res: unknown) => {
  // [zh] 条件判断 if
  if (Array.isArray(res)) {
    // [zh] return 返回
    return { list: res, pageNo: 1, pageTotal: 1, totalCount: res.length };
  // [zh] 语法符号
  }

  // [zh] 声明变量 `page`
  let page: Record<string, unknown> | null = null;
  // [zh] 条件判断 if
  if (res && typeof res === 'object') {
    // [zh] 语句
    page = res as Record<string, unknown>;
    // [zh] 条件判断 if
    if (page.data && typeof page.data === 'object' && !Array.isArray(page.list)) {
      // [zh] 语句
      page = page.data as Record<string, unknown>;
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (!page) {
    // [zh] return 返回
    return { list: [], pageNo: 1, pageTotal: 1, totalCount: 0 };
  // [zh] 语法符号
  }

  // [zh] 声明变量 `list`
  const list = Array.isArray(page.list)
    // [zh] TypeScript/JS 语句：`? page.list`
    ? page.list
    // [zh] TypeScript/JS 语句：`: Array.isArray(page.records)`
    : Array.isArray(page.records)
      // [zh] TypeScript/JS 语句：`? page.records`
      ? page.records
      // [zh] TypeScript/JS 语句：`: Array.isArray(page.rows)`
      : Array.isArray(page.rows)
        // [zh] TypeScript/JS 语句：`? page.rows`
        ? page.rows
        // [zh] 语句
        : [];

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`list,`
    list,
    // [zh] TypeScript/JS 语句：`pageNo: Number(page.pageNo) || 1,`
    pageNo: Number(page.pageNo) || 1,
    // [zh] TypeScript/JS 语句：`pageTotal: Number(page.pageTotal) || 1,`
    pageTotal: Number(page.pageTotal) || 1,
    // [zh] TypeScript/JS 语句：`totalCount: Number(page.totalCount) || list.length`
    totalCount: Number(page.totalCount) || list.length
  // [zh] 语句
  };
// [zh] 语句
};

// [zh] 导出模块成员
export const normalizeAgentHistoryMessage = (raw: Record<string, unknown>): AgentHistoryMessage => {
  // [zh] 声明变量 `messageIdRaw`
  const messageIdRaw = pickField(raw, 'messageId', 'message_id');
  // [zh] 声明变量 `parsedId`
  const parsedId = messageIdRaw != null ? Number(messageIdRaw) : 0;
  // [zh] 声明变量 `assistantRaw`
  const assistantRaw = pickField(raw, 'assistantMessage', 'assistant_message');
  // [zh] 声明变量 `userRaw`
  const userRaw = pickField(raw, 'userMessage', 'user_message');
  // [zh] 声明变量 `bizTypeRaw`
  const bizTypeRaw = pickField(raw, 'bizType', 'biz_type');
  // [zh] 声明变量 `bizDataRaw`
  const bizDataRaw = pickField(raw, 'bizData', 'biz_data');
  // [zh] 声明变量 `sendTimeRaw`
  const sendTimeRaw = pickField(raw, 'sendTime', 'send_time');
  // [zh] 声明变量 `statusRaw`
  const statusRaw = pickField(raw, 'status');

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`messageId: Number.isFinite(parsedId) && parsedId > 0 ? `
    messageId: Number.isFinite(parsedId) && parsedId > 0 ? parsedId : 0,
    // [zh] TypeScript/JS 语句：`userMessage: userRaw != null ? String(userRaw) : undefi`
    userMessage: userRaw != null ? String(userRaw) : undefined,
    // [zh] TypeScript/JS 语句：`assistantMessage: assistantRaw != null ? String(assista`
    assistantMessage: assistantRaw != null ? String(assistantRaw) : '',
    // [zh] TypeScript/JS 语句：`status: statusRaw != null ? Number(statusRaw) : 2,`
    status: statusRaw != null ? Number(statusRaw) : 2,
    // [zh] TypeScript/JS 语句：`bizType: bizTypeRaw != null ? String(bizTypeRaw) : unde`
    bizType: bizTypeRaw != null ? String(bizTypeRaw) : undefined,
    // [zh] TypeScript/JS 语句：`bizData: bizDataRaw != null ? String(bizDataRaw) : null`
    bizData: bizDataRaw != null ? String(bizDataRaw) : null,
    // [zh] TypeScript/JS 语句：`sendTime: sendTimeRaw != null ? String(sendTimeRaw) : u`
    sendTime: sendTimeRaw != null ? String(sendTimeRaw) : undefined
  // [zh] 语句
  };
// [zh] 语句
};

// [zh] 导出模块成员
export const sortHistoryMessages = (list: AgentHistoryMessage[]) =>
  // [zh] 语句
  [...list].sort((a, b) => a.messageId - b.messageId);

// [zh] 导出模块成员
export const mergeHistoryMessages = (
  // [zh] TypeScript/JS 语句：`existing: AgentHistoryMessage[],`
  existing: AgentHistoryMessage[],
  // [zh] TypeScript/JS 语句：`incoming: AgentHistoryMessage[]`
  incoming: AgentHistoryMessage[]
// [zh] 代码块开始
) => {
  // [zh] 声明变量 `map`
  const map = new Map<number, AgentHistoryMessage>();
  // [zh] 代码块开始
  existing.forEach((item) => {
    // [zh] 条件判断 if
    if (item.messageId) map.set(item.messageId, item);
  // [zh] 语句
  });
  // [zh] 代码块开始
  incoming.forEach((item) => {
    // [zh] 条件判断 if
    if (item.messageId) map.set(item.messageId, item);
  // [zh] 语句
  });
  // [zh] return 返回
  return sortHistoryMessages([...map.values()]);
// [zh] 语句
};
