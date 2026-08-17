

export interface AgentHistoryMessage {
  messageId: number;
  userMessage?: string;
  assistantMessage: string;
  status: number;
  bizType?: string;
  bizData?: string | null;
  sendTime?: string;
}

const pickField = (raw: Record<string, unknown>, ...keys: string[]) => {
  for (const key of keys) {
    const val = raw[key];
    if (val != null && val !== '') return val;
  }
  return undefined;
};

export const extractHistoryPage = (res: unknown) => {
  if (Array.isArray(res)) {
    return { list: res, pageNo: 1, pageTotal: 1, totalCount: res.length };
  }

  let page: Record<string, unknown> | null = null;
  if (res && typeof res === 'object') {
    page = res as Record<string, unknown>;
    if (page.data && typeof page.data === 'object' && !Array.isArray(page.list)) {
      page = page.data as Record<string, unknown>;
    }
  }

  if (!page) {
    return { list: [], pageNo: 1, pageTotal: 1, totalCount: 0 };
  }

  const list = Array.isArray(page.list)
    ? page.list
    : Array.isArray(page.records)
      ? page.records
      : Array.isArray(page.rows)
        ? page.rows
        : [];

  return {
    list,
    pageNo: Number(page.pageNo) || 1,
    pageTotal: Number(page.pageTotal) || 1,
    totalCount: Number(page.totalCount) || list.length
  };
};

export const normalizeAgentHistoryMessage = (raw: Record<string, unknown>): AgentHistoryMessage => {
  const messageIdRaw = pickField(raw, 'messageId', 'message_id');
  const parsedId = messageIdRaw != null ? Number(messageIdRaw) : 0;
  const assistantRaw = pickField(raw, 'assistantMessage', 'assistant_message');
  const userRaw = pickField(raw, 'userMessage', 'user_message');
  const bizTypeRaw = pickField(raw, 'bizType', 'biz_type');
  const bizDataRaw = pickField(raw, 'bizData', 'biz_data');
  const sendTimeRaw = pickField(raw, 'sendTime', 'send_time');
  const statusRaw = pickField(raw, 'status');

  return {
    messageId: Number.isFinite(parsedId) && parsedId > 0 ? parsedId : 0,
    userMessage: userRaw != null ? String(userRaw) : undefined,
    assistantMessage: assistantRaw != null ? String(assistantRaw) : '',
    status: statusRaw != null ? Number(statusRaw) : 2,
    bizType: bizTypeRaw != null ? String(bizTypeRaw) : undefined,
    bizData: bizDataRaw != null ? String(bizDataRaw) : null,
    sendTime: sendTimeRaw != null ? String(sendTimeRaw) : undefined
  };
};

export const sortHistoryMessages = (list: AgentHistoryMessage[]) =>
  [...list].sort((a, b) => a.messageId - b.messageId);

export const mergeHistoryMessages = (
  existing: AgentHistoryMessage[],
  incoming: AgentHistoryMessage[]
) => {
  const map = new Map<number, AgentHistoryMessage>();
  existing.forEach((item) => {
    if (item.messageId) map.set(item.messageId, item);
  });
  incoming.forEach((item) => {
    if (item.messageId) map.set(item.messageId, item);
  });
  return sortHistoryMessages([...map.values()]);
};
