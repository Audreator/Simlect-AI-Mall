// [zh] 导出模块成员
export type PcLayoutMode = 'auth' | 'user' | 'plain';

// [zh] 导出模块成员
export const PC_USER_NAV_GROUPS = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`title: '我的简选',`
    title: '我的简选',
    // [zh] TypeScript/JS 语句：`items: [`
    items: [
      // [zh] TypeScript/JS 语句：`{ label: '个人中心', path: '/account' },`
      { label: '个人中心', path: '/account' },
      // [zh] TypeScript/JS 语句：`{ label: '会员中心', path: '/member-center' },`
      { label: '会员中心', path: '/member-center' },
      // [zh] TypeScript/JS 语句：`{ label: '消息中心', path: '/notifications' },`
      { label: '消息中心', path: '/notifications' },
      // [zh] TypeScript/JS 语句：`{ label: '我的购物车', path: '/cart' }`
      { label: '我的购物车', path: '/cart' }
    // [zh] TypeScript/JS 语句：`]`
    ]
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`title: '订单中心',`
    title: '订单中心',
    // [zh] TypeScript/JS 语句：`items: [{ label: '我的订单', path: '/orders' }]`
    items: [{ label: '我的订单', path: '/orders' }]
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`title: '资产',`
    title: '资产',
    // [zh] TypeScript/JS 语句：`items: [`
    items: [
      // [zh] TypeScript/JS 语句：`{ label: '我的优惠券', path: '/my-coupons' },`
      { label: '我的优惠券', path: '/my-coupons' },
      // [zh] TypeScript/JS 语句：`{ label: '领券中心', path: '/coupons' }`
      { label: '领券中心', path: '/coupons' }
    // [zh] TypeScript/JS 语句：`]`
    ]
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`title: '收藏',`
    title: '收藏',
    // [zh] TypeScript/JS 语句：`items: [`
    items: [
      // [zh] TypeScript/JS 语句：`{ label: '我的收藏', path: '/wishlist' },`
      { label: '我的收藏', path: '/wishlist' },
      // [zh] TypeScript/JS 语句：`{ label: '我的足迹', path: '/footprint' }`
      { label: '我的足迹', path: '/footprint' }
    // [zh] TypeScript/JS 语句：`]`
    ]
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`title: '账户设置',`
    title: '账户设置',
    // [zh] TypeScript/JS 语句：`items: [`
    items: [
      // [zh] TypeScript/JS 语句：`{ label: '收货地址', path: '/address' },`
      { label: '收货地址', path: '/address' },
      // [zh] TypeScript/JS 语句：`{ label: '签到中心', path: '/sign' },`
      { label: '签到中心', path: '/sign' },
      // [zh] TypeScript/JS 语句：`{ label: '个人资料', path: '/account/profile' },`
      { label: '个人资料', path: '/account/profile' },
      // [zh] TypeScript/JS 语句：`{ label: '账号设置', path: '/account/manage' }`
      { label: '账号设置', path: '/account/manage' }
    // [zh] TypeScript/JS 语句：`]`
    ]
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`title: '服务',`
    title: '服务',
    // [zh] TypeScript/JS 语句：`items: [{ label: '智能客服', path: '/ai-assistant' }]`
    items: [{ label: '智能客服', path: '/ai-assistant' }]
  // [zh] 语法符号
  }
// [zh] 语句
] as const;

// [zh] 导出模块成员
export const PC_USER_CENTER_PATHS = [
  // [zh] TypeScript/JS 语句：`'/orders',`
  '/orders',
  // [zh] TypeScript/JS 语句：`'/order/',`
  '/order/',
  // [zh] TypeScript/JS 语句：`'/wishlist',`
  '/wishlist',
  // [zh] TypeScript/JS 语句：`'/footprint',`
  '/footprint',
  // [zh] TypeScript/JS 语句：`'/my-coupons',`
  '/my-coupons',
  // [zh] TypeScript/JS 语句：`'/address',`
  '/address',
  // [zh] TypeScript/JS 语句：`'/sign',`
  '/sign',
  // [zh] TypeScript/JS 语句：`'/member-center',`
  '/member-center',
  // [zh] TypeScript/JS 语句：`'/notifications',`
  '/notifications',
  // [zh] TypeScript/JS 语句：`'/pay-records',`
  '/pay-records',
  // [zh] TypeScript/JS 语句：`'/account/manage',`
  '/account/manage',
  // [zh] TypeScript/JS 语句：`'/account/profile',`
  '/account/profile',
  // [zh] TypeScript/JS 语句：`'/account/settings',`
  '/account/settings',
  // [zh] TypeScript/JS 语句：`'/account/password'`
  '/account/password'
// [zh] 语句
];

// [zh] 导出模块成员
export const resolvePcLayoutMode = (path: string): PcLayoutMode => {
  // [zh] 条件判断 if
  if (path === '/login' || path === '/register') return 'auth';
  // [zh] 条件判断 if
  if (PC_USER_CENTER_PATHS.some((p) => path === p || path.startsWith(p))) {
    // [zh] return 返回
    return 'user';
  // [zh] 语法符号
  }
  // [zh] return 返回
  return 'plain';
// [zh] 语句
};
