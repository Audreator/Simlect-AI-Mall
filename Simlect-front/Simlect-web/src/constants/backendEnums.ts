

// [zh] 导出模块成员
export type IntLabelMap = Record<number, string>;
// [zh] 导出模块成员
export type StrLabelMap = Record<string, string>;

// [zh] 导出模块成员
export function labelOf(map: IntLabelMap, value?: number | null, fallback = '未知') {
  // [zh] 条件判断 if
  if (value === undefined || value === null) return fallback;
  // [zh] return 返回
  return map[value] ?? fallback;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function labelOfStr(map: StrLabelMap, value?: string | null, fallback = '未知') {
  // [zh] 条件判断 if
  if (!value) return fallback;
  // [zh] return 返回
  return map[value] ?? fallback;
// [zh] 语法符号
}

// [zh] 导出模块成员
export const ORDER_STATUS: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`[-1]: '已删除',`
  [-1]: '已删除',
  // [zh] TypeScript/JS 语句：`0: '待付款',`
  0: '待付款',
  // [zh] TypeScript/JS 语句：`1: '已付款,待发货',`
  1: '已付款,待发货',
  // [zh] TypeScript/JS 语句：`2: '已发货',`
  2: '已发货',
  // [zh] TypeScript/JS 语句：`3: '已完成',`
  3: '已完成',
  // [zh] TypeScript/JS 语句：`4: '交易取消',`
  4: '交易取消',
  // [zh] TypeScript/JS 语句：`5: '交易关闭',`
  5: '交易关闭',
  // [zh] TypeScript/JS 语句：`6: '已退款,交易关闭',`
  6: '已退款,交易关闭',
  // [zh] TypeScript/JS 语句：`7: '部分退款'`
  7: '部分退款'
// [zh] 语句
};

// [zh] 导出模块成员
export const ORDER_ITEM_STATUS: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '已退款',`
  0: '已退款',
  // [zh] TypeScript/JS 语句：`1: '正常'`
  1: '正常'
// [zh] 语句
};

// [zh] 导出模块成员
export const ORDER_COMMENT_STATUS: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '未评价',`
  0: '未评价',
  // [zh] TypeScript/JS 语句：`1: '已评价',`
  1: '已评价',
  // [zh] TypeScript/JS 语句：`2: '已追评'`
  2: '已追评'
// [zh] 语句
};

// [zh] 导出模块成员
export const ORDER_FROM_TYPE: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '商品页',`
  0: '商品页',
  // [zh] TypeScript/JS 语句：`1: '购物车'`
  1: '购物车'
// [zh] 语句
};

// [zh] 导出模块成员
export const LOGISTICS_STATUS: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '待发货',`
  0: '待发货',
  // [zh] TypeScript/JS 语句：`1: '运输中',`
  1: '运输中',
  // [zh] TypeScript/JS 语句：`2: '已送达',`
  2: '已送达',
  // [zh] TypeScript/JS 语句：`3: '订单取消'`
  3: '订单取消'
// [zh] 语句
};

// [zh] 导出模块成员
export const orderStatusLabel = (s?: number | null) => labelOf(ORDER_STATUS, s, '未知状态');
// [zh] 导出模块成员
export const orderCommentStatusLabel = (s?: number | null) => labelOf(ORDER_COMMENT_STATUS, s, '');
// [zh] 导出模块成员
export const logisticsStatusLabel = (s?: number | null) => labelOf(LOGISTICS_STATUS, s);

// [zh] 导出模块成员
export function displayOrderStatusText(order: {
  // [zh] 语句
  orderStatus?: number | null;
  // [zh] 语句
  orderStatusName?: string | null;
  // [zh] 语句
  commentStatus?: number | null;
// [zh] 代码块开始
}) {
  // [zh] 声明变量 `status`
  const status = order.orderStatus;
  // [zh] 条件判断 if
  if (status === 3) {
    // [zh] 声明变量 `cs`
    const cs = order.commentStatus;
    // [zh] 条件判断 if
    if (cs === 0) return '待评价';
    // [zh] 条件判断 if
    if (cs === 1) return '已评价';
    // [zh] 条件判断 if
    if (cs === 2) return '已追评';
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (order.orderStatusName) return order.orderStatusName;
  // [zh] return 返回
  return orderStatusLabel(status);
// [zh] 语法符号
}

// [zh] 导出模块成员
export const PRODUCT_STATUS: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`[-1]: '已删除',`
  [-1]: '已删除',
  // [zh] TypeScript/JS 语句：`0: '未上架',`
  0: '未上架',
  // [zh] TypeScript/JS 语句：`1: '已上架'`
  1: '已上架'
// [zh] 语句
};

// [zh] 导出模块成员
export const COMMEND_TYPE: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '未推荐',`
  0: '未推荐',
  // [zh] TypeScript/JS 语句：`1: '已推荐'`
  1: '已推荐'
// [zh] 语句
};

// [zh] 导出模块成员
export const SEARCH_FIELD: StrLabelMap = {
  // [zh] TypeScript/JS 语句：`composite: '综合',`
  composite: '综合',
  // [zh] TypeScript/JS 语句：`sale: '销量',`
  sale: '销量',
  // [zh] TypeScript/JS 语句：`price: '价格'`
  price: '价格'
// [zh] 语句
};

// [zh] 导出模块成员
export const SEARCH_SORT_TYPE: StrLabelMap = {
  // [zh] TypeScript/JS 语句：`asc: '升序',`
  asc: '升序',
  // [zh] TypeScript/JS 语句：`desc: '降序'`
  desc: '降序'
// [zh] 语句
};

// [zh] 导出模块成员
export const productStatusLabel = (s?: number | null) => labelOf(PRODUCT_STATUS, s);

// [zh] 导出模块成员
export function isProductOnSale(product?: { status?: number | null } | null): boolean {
  // [zh] 条件判断 if
  if (product?.status === undefined || product?.status === null) return true;
  // [zh] return 返回
  return Number(product.status) === 1;
// [zh] 语法符号
}

// [zh] 导出模块成员
export const COUPON_TYPE: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`1: '满减券',`
  1: '满减券',
  // [zh] TypeScript/JS 语句：`2: '折扣券',`
  2: '折扣券',
  // [zh] TypeScript/JS 语句：`3: '无门槛券'`
  3: '无门槛券'
// [zh] 语句
};

// [zh] 导出模块成员
export const COUPON_TEMPLATE_STATUS: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '已停用',`
  0: '已停用',
  // [zh] TypeScript/JS 语句：`1: '正常',`
  1: '正常',
  // [zh] TypeScript/JS 语句：`2: '已过期',`
  2: '已过期',
  // [zh] TypeScript/JS 语句：`3: '已发完'`
  3: '已发完'
// [zh] 语句
};

// [zh] 导出模块成员
export const USER_COUPON_STATUS: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '未使用',`
  0: '未使用',
  // [zh] TypeScript/JS 语句：`1: '已使用',`
  1: '已使用',
  // [zh] TypeScript/JS 语句：`2: '已过期',`
  2: '已过期',
  // [zh] TypeScript/JS 语句：`3: '已作废'`
  3: '已作废'
// [zh] 语句
};

// [zh] 导出模块成员
export const RUSHING_COUPON_FLAG: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '否',`
  0: '否',
  // [zh] TypeScript/JS 语句：`1: '是'`
  1: '是'
// [zh] 语句
};

// [zh] 导出模块成员
export const RUSHING_TAB: StrLabelMap = {
  // [zh] TypeScript/JS 语句：`all: '全部',`
  all: '全部',
  // [zh] TypeScript/JS 语句：`upcoming: '即将开始',`
  upcoming: '即将开始',
  // [zh] TypeScript/JS 语句：`ongoing: '进行中',`
  ongoing: '进行中',
  // [zh] TypeScript/JS 语句：`ended: '已结束'`
  ended: '已结束'
// [zh] 语句
};

// [zh] 导出模块成员
export const couponTypeLabel = (t?: number | null) => labelOf(COUPON_TYPE, t, '优惠券');
// [zh] 导出模块成员
export const userCouponStatusLabel = (s?: number | null) => labelOf(USER_COUPON_STATUS, s);

// [zh] 导出模块成员
export const USER_STATUS: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '禁用',`
  0: '禁用',
  // [zh] TypeScript/JS 语句：`1: '启用'`
  1: '启用'
// [zh] 语句
};

// [zh] 导出模块成员
export const USER_SEX: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '女',`
  0: '女',
  // [zh] TypeScript/JS 语句：`1: '男',`
  1: '男',
  // [zh] TypeScript/JS 语句：`2: '保密'`
  2: '保密'
// [zh] 语句
};

// [zh] 导出模块成员
export const DEFAULT_TYPE: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '非默认',`
  0: '非默认',
  // [zh] TypeScript/JS 语句：`1: '默认'`
  1: '默认'
// [zh] 语句
};

// [zh] 导出模块成员
export const userSexLabel = (s?: number | null) => labelOf(USER_SEX, s, '保密');

// [zh] 导出模块成员
export const COMMENT_STATUS: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '正常',`
  0: '正常',
  // [zh] TypeScript/JS 语句：`1: '已删除'`
  1: '已删除'
// [zh] 语句
};

// [zh] 导出模块成员
export const AGENT_MESSAGE_STATUS: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '已取消',`
  0: '已取消',
  // [zh] TypeScript/JS 语句：`1: '回答中',`
  1: '回答中',
  // [zh] TypeScript/JS 语句：`2: '已完成',`
  2: '已完成',
  // [zh] TypeScript/JS 语句：`3: '已中断'`
  3: '已中断'
// [zh] 语句
};

// [zh] 导出模块成员
export const AGENT_OUTPUT_TYPE = {
  // [zh] TypeScript/JS 语句：`OUTPUTTING: 0,`
  OUTPUTTING: 0,
  // [zh] TypeScript/JS 语句：`DONE: 1,`
  DONE: 1,
  // [zh] TypeScript/JS 语句：`ERROR: 2`
  ERROR: 2
// [zh] 语句
} as const;

// [zh] 导出模块成员
export const AGENT_BIZ_TYPE: StrLabelMap = {
  // [zh] TypeScript/JS 语句：`product_search: '商品搜索',`
  product_search: '商品搜索',
  // [zh] TypeScript/JS 语句：`query_order: '订单查询',`
  query_order: '订单查询',
  // [zh] TypeScript/JS 语句：`chat: '普通聊天'`
  chat: '普通聊天'
// [zh] 语句
};

// [zh] 导出模块成员
export const PROPERTY_COVER_TYPE: IntLabelMap = {
  // [zh] TypeScript/JS 语句：`0: '无需封面',`
  0: '无需封面',
  // [zh] TypeScript/JS 语句：`1: '需封面'`
  1: '需封面'
// [zh] 语句
};

// [zh] 导出模块成员
export const RESPONSE_CODE = {
  // [zh] TypeScript/JS 语句：`SUCCESS: 200,`
  SUCCESS: 200,
  // [zh] TypeScript/JS 语句：`NOT_FOUND: 404,`
  NOT_FOUND: 404,
  // [zh] TypeScript/JS 语句：`SERVER_ERROR: 500,`
  SERVER_ERROR: 500,
  // [zh] TypeScript/JS 语句：`LOGIN_TIMEOUT: 901`
  LOGIN_TIMEOUT: 901
// [zh] 语句
} as const;
