

export type IntLabelMap = Record<number, string>;
export type StrLabelMap = Record<string, string>;

export function labelOf(map: IntLabelMap, value?: number | null, fallback = '未知') {
  if (value === undefined || value === null) return fallback;
  return map[value] ?? fallback;
}

export function labelOfStr(map: StrLabelMap, value?: string | null, fallback = '未知') {
  if (!value) return fallback;
  return map[value] ?? fallback;
}

export const ORDER_STATUS: IntLabelMap = {
  [-1]: '已删除',
  0: '待付款',
  1: '已付款,待发货',
  2: '已发货',
  3: '已完成',
  4: '交易取消',
  5: '交易关闭',
  6: '已退款,交易关闭',
  7: '部分退款'
};

export const ORDER_ITEM_STATUS: IntLabelMap = {
  0: '已退款',
  1: '正常'
};

export const ORDER_COMMENT_STATUS: IntLabelMap = {
  0: '未评价',
  1: '已评价',
  2: '已追评'
};

export const ORDER_FROM_TYPE: IntLabelMap = {
  0: '商品页',
  1: '购物车'
};

export const LOGISTICS_STATUS: IntLabelMap = {
  0: '待发货',
  1: '运输中',
  2: '已送达',
  3: '订单取消'
};

export const orderStatusLabel = (s?: number | null) => labelOf(ORDER_STATUS, s, '未知状态');
export const orderCommentStatusLabel = (s?: number | null) => labelOf(ORDER_COMMENT_STATUS, s, '');
export const logisticsStatusLabel = (s?: number | null) => labelOf(LOGISTICS_STATUS, s);

export function displayOrderStatusText(order: {
  orderStatus?: number | null;
  orderStatusName?: string | null;
  commentStatus?: number | null;
}) {
  const status = order.orderStatus;
  if (status === 3) {
    const cs = order.commentStatus;
    if (cs === 0) return '待评价';
    if (cs === 1) return '已评价';
    if (cs === 2) return '已追评';
  }
  if (order.orderStatusName) return order.orderStatusName;
  return orderStatusLabel(status);
}

export const PRODUCT_STATUS: IntLabelMap = {
  [-1]: '已删除',
  0: '未上架',
  1: '已上架'
};

export const COMMEND_TYPE: IntLabelMap = {
  0: '未推荐',
  1: '已推荐'
};

export const SEARCH_FIELD: StrLabelMap = {
  composite: '综合',
  sale: '销量',
  price: '价格'
};

export const SEARCH_SORT_TYPE: StrLabelMap = {
  asc: '升序',
  desc: '降序'
};

export const productStatusLabel = (s?: number | null) => labelOf(PRODUCT_STATUS, s);

export function isProductOnSale(product?: { status?: number | null } | null): boolean {
  if (product?.status === undefined || product?.status === null) return true;
  return Number(product.status) === 1;
}

export const COUPON_TYPE: IntLabelMap = {
  1: '满减券',
  2: '折扣券',
  3: '无门槛券'
};

export const COUPON_TEMPLATE_STATUS: IntLabelMap = {
  0: '已停用',
  1: '正常',
  2: '已过期',
  3: '已发完'
};

export const USER_COUPON_STATUS: IntLabelMap = {
  0: '未使用',
  1: '已使用',
  2: '已过期',
  3: '已作废'
};

export const RUSHING_COUPON_FLAG: IntLabelMap = {
  0: '否',
  1: '是'
};

export const RUSHING_TAB: StrLabelMap = {
  all: '全部',
  upcoming: '即将开始',
  ongoing: '进行中',
  ended: '已结束'
};

export const couponTypeLabel = (t?: number | null) => labelOf(COUPON_TYPE, t, '优惠券');
export const userCouponStatusLabel = (s?: number | null) => labelOf(USER_COUPON_STATUS, s);

export const USER_STATUS: IntLabelMap = {
  0: '禁用',
  1: '启用'
};

export const USER_SEX: IntLabelMap = {
  0: '女',
  1: '男',
  2: '保密'
};

export const DEFAULT_TYPE: IntLabelMap = {
  0: '非默认',
  1: '默认'
};

export const userSexLabel = (s?: number | null) => labelOf(USER_SEX, s, '保密');

export const COMMENT_STATUS: IntLabelMap = {
  0: '正常',
  1: '已删除'
};

export const AGENT_MESSAGE_STATUS: IntLabelMap = {
  0: '已取消',
  1: '回答中',
  2: '已完成',
  3: '已中断'
};

export const AGENT_OUTPUT_TYPE = {
  OUTPUTTING: 0,
  DONE: 1,
  ERROR: 2
} as const;

export const AGENT_BIZ_TYPE: StrLabelMap = {
  product_search: '商品搜索',
  query_order: '订单查询',
  chat: '普通聊天'
};

export const PROPERTY_COVER_TYPE: IntLabelMap = {
  0: '无需封面',
  1: '需封面'
};

export const RESPONSE_CODE = {
  SUCCESS: 200,
  NOT_FOUND: 404,
  SERVER_ERROR: 500,
  LOGIN_TIMEOUT: 901
} as const;
