export type PcLayoutMode = 'auth' | 'user' | 'plain';

export const PC_USER_NAV_GROUPS = [
  {
    title: '我的简选',
    items: [
      { label: '个人中心', path: '/account' },
      { label: '会员中心', path: '/member-center' },
      { label: '消息中心', path: '/notifications' },
      { label: '我的购物车', path: '/cart' }
    ]
  },
  {
    title: '订单中心',
    items: [{ label: '我的订单', path: '/orders' }]
  },
  {
    title: '资产',
    items: [
      { label: '我的优惠券', path: '/my-coupons' },
      { label: '领券中心', path: '/coupons' }
    ]
  },
  {
    title: '收藏',
    items: [
      { label: '我的收藏', path: '/wishlist' },
      { label: '我的足迹', path: '/footprint' }
    ]
  },
  {
    title: '账户设置',
    items: [
      { label: '收货地址', path: '/address' },
      { label: '签到中心', path: '/sign' },
      { label: '个人资料', path: '/account/profile' },
      { label: '账号设置', path: '/account/manage' }
    ]
  },
  {
    title: '服务',
    items: [{ label: '智能客服', path: '/ai-assistant' }]
  }
] as const;

export const PC_USER_CENTER_PATHS = [
  '/orders',
  '/order/',
  '/wishlist',
  '/footprint',
  '/my-coupons',
  '/address',
  '/sign',
  '/member-center',
  '/notifications',
  '/pay-records',
  '/account/manage',
  '/account/profile',
  '/account/settings',
  '/account/password'
];

export const resolvePcLayoutMode = (path: string): PcLayoutMode => {
  if (path === '/login' || path === '/register') return 'auth';
  if (PC_USER_CENTER_PATHS.some((p) => path === p || path.startsWith(p))) {
    return 'user';
  }
  return 'plain';
};
