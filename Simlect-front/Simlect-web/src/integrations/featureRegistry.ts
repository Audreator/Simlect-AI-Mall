
export type FeatureId =
  | 'home_recommend'
  | 'home_diy'
  | 'category_tree'
  | 'category_icon'
  | 'product_list'
  | 'product_search'
  | 'product_detail'
  | 'cart'
  | 'checkout'
  | 'order_list'
  | 'order_pay'
  | 'order_cancel'
  | 'order_confirm_receive'
  | 'order_logistics'
  | 'order_refund'
  | 'order_comment'
  | 'user_login_email'
  | 'user_register'
  | 'user_profile'
  | 'user_address'
  | 'coupon_plaza'
  | 'coupon_mine'
  | 'favorite'
  | 'browse_history'
  | 'sign_in'
  | 'member_center'
  | 'notification'
  | 'agent_chat'
  | 'pay_records'
  | 'location_geocode'
  | 'wechat_login'
  | 'sms_login'
  | 'distribution'
  | 'seckill'
  | 'bargain'
  | 'combination'
  | 'points_mall'
  | 'wallet_recharge'
  | 'invoice'
  | 'article_cms'
  | 'multi_store'
  | 'lottery'
  | 'pc_simlect_home';

export interface FeatureMeta {
  id: FeatureId;
  label: string;
  
  simlectRef?: string;
  supported: boolean;
  
  eshopApi?: string;
  
  extensionHint?: string;
}

export const FEATURE_REGISTRY: FeatureMeta[] = [
  {
    id: 'home_recommend',
    label: '首页推荐/商品流',
    simlectRef: 'pages/index',
    supported: true,
    eshopApi: 'product/loadCommendProduct, loadProduct'
  },
  {
    id: 'home_diy',
    label: '首页 DIY 装修',
    simlectRef: 'v2/diy',
    supported: false,
    extensionHint: '页面配置 JSON + 管理端发布，或短期静态配置'
  },
  {
    id: 'category_tree',
    label: '分类树',
    simlectRef: 'goods_cate/goods_cate1',
    supported: true,
    eshopApi: 'product/loadCategory'
  },
  {
    id: 'category_icon',
    label: '分类图标',
    simlectRef: 'category.pic',
    supported: false,
    extensionHint: 'SysCategory 增加 iconUrl + 管理端上传'
  },
  {
    id: 'product_list',
    label: '分类商品列表',
    simlectRef: 'goods/goods_list',
    supported: true,
    eshopApi: 'product/loadProduct'
  },
  {
    id: 'product_search',
    label: '搜索',
    simlectRef: 'goods/goods_search',
    supported: true,
    eshopApi: 'product/search, search/*'
  },
  {
    id: 'product_detail',
    label: '商品详情',
    simlectRef: 'goods_details',
    supported: true,
    eshopApi: 'product/getProduct'
  },
  {
    id: 'cart',
    label: '购物车',
    simlectRef: 'order_addcart',
    supported: true,
    eshopApi: 'productCart/*'
  },
  {
    id: 'checkout',
    label: '结算下单',
    simlectRef: 'order_confirm',
    supported: true,
    eshopApi: 'order/postOrder'
  },
  {
    id: 'order_list',
    label: '我的订单',
    simlectRef: 'order_list',
    supported: true,
    eshopApi: 'order/loadMyOrder'
  },
  {
    id: 'order_pay',
    label: '支付',
    simlectRef: 'order_pay',
    supported: true,
    eshopApi: 'order/getPayInfo + 支付宝回调'
  },
  {
    id: 'order_cancel',
    label: '取消订单',
    supported: true,
    eshopApi: 'order/cancelOrder'
  },
  {
    id: 'order_confirm_receive',
    label: '确认收货',
    supported: true,
    eshopApi: 'order/confirmOrder'
  },
  {
    id: 'order_logistics',
    label: '物流',
    supported: true,
    eshopApi: 'order/getLogistics'
  },
  {
    id: 'order_refund',
    label: '退款',
    simlectRef: 'user_return_list',
    supported: true,
    eshopApi: 'order/refundOrder（前端待封装）',
    extensionHint: '建议增加用户端退款单列表分页'
  },
  {
    id: 'order_comment',
    label: '评价',
    supported: true,
    eshopApi: 'order/comment/*'
  },
  {
    id: 'user_login_email',
    label: '邮箱登录',
    supported: true,
    eshopApi: 'account/login'
  },
  {
    id: 'user_register',
    label: '注册',
    supported: true,
    eshopApi: 'account/register'
  },
  {
    id: 'user_profile',
    label: '个人资料',
    supported: true,
    eshopApi: 'account/getUserInfo, updateUserInfo'
  },
  {
    id: 'user_address',
    label: '收货地址',
    supported: true,
    eshopApi: 'userAddress/*'
  },
  {
    id: 'coupon_plaza',
    label: '优惠券广场',
    supported: true,
    eshopApi: 'discountCoupon/loadDiscountCoupon'
  },
  {
    id: 'coupon_mine',
    label: '我的优惠券',
    supported: true,
    eshopApi: 'discountCoupon/loadUserCoupon'
  },
  {
    id: 'favorite',
    label: '收藏',
    supported: true,
    eshopApi: 'userFavorite/*'
  },
  {
    id: 'browse_history',
    label: '浏览足迹',
    supported: true,
    eshopApi: 'browseHistory/*'
  },
  {
    id: 'sign_in',
    label: '签到',
    supported: true,
    eshopApi: 'sign/*'
  },
  {
    id: 'member_center',
    label: '会员中心',
    supported: true,
    eshopApi: 'userMember/*'
  },
  {
    id: 'notification',
    label: '消息中心',
    supported: true,
    eshopApi: 'userNotification/*'
  },
  {
    id: 'agent_chat',
    label: '智能客服',
    supported: true,
    eshopApi: 'agent/*'
  },
  {
    id: 'pay_records',
    label: '支付记录',
    supported: true,
    eshopApi: 'payTrade/loadMyTrades'
  },
  {
    id: 'location_geocode',
    label: '定位逆地理',
    supported: true,
    eshopApi: 'location/*'
  },
  {
    id: 'wechat_login',
    label: '微信登录',
    simlectRef: 'wechat/*',
    supported: false,
    extensionHint: '微信 OAuth + 账号绑定'
  },
  {
    id: 'sms_login',
    label: '手机验证码登录',
    supported: false,
    extensionHint: '短信网关 + loginByMobile'
  },
  {
    id: 'distribution',
    label: '分销推广',
    simlectRef: 'spread/*',
    supported: false,
    extensionHint: '分销员、佣金、提现'
  },
  {
    id: 'seckill',
    label: '秒杀',
    supported: false,
    extensionHint: '秒杀活动与库存'
  },
  {
    id: 'bargain',
    label: '砍价',
    supported: false,
    extensionHint: '砍价活动'
  },
  {
    id: 'combination',
    label: '拼团',
    supported: false,
    extensionHint: '拼团活动'
  },
  {
    id: 'points_mall',
    label: '积分商城',
    supported: false,
    extensionHint: '积分商品与兑换订单'
  },
  {
    id: 'wallet_recharge',
    label: '余额充值提现',
    supported: false,
    extensionHint: '钱包账户体系'
  },
  {
    id: 'invoice',
    label: '发票',
    supported: false,
    extensionHint: '发票抬头与开票'
  },
  {
    id: 'article_cms',
    label: '文章资讯',
    supported: false,
    extensionHint: 'CMS 文章 API'
  },
  {
    id: 'multi_store',
    label: '多门店',
    supported: false,
    extensionHint: '门店与门店库存'
  },
  {
    id: 'lottery',
    label: '抽奖',
    supported: false,
    extensionHint: '抽奖活动'
  },
  {
    id: 'pc_simlect_home',
    label: 'PC Simlect 首页',
    simlectRef: 'simlect-origin/index.html',
    supported: true,
    eshopApi: '同首页商品/分类接口'
  }
];

export function isFeatureSupported(id: FeatureId): boolean {
  return FEATURE_REGISTRY.find((f) => f.id === id)?.supported ?? false;
}

export function unsupportedFeatures(): FeatureMeta[] {
  return FEATURE_REGISTRY.filter((f) => !f.supported);
}
