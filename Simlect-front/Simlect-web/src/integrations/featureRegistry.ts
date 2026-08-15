
// [zh] 导出模块成员
export type FeatureId =
  // [zh] TypeScript/JS 语句：`| 'home_recommend'`
  | 'home_recommend'
  // [zh] TypeScript/JS 语句：`| 'home_diy'`
  | 'home_diy'
  // [zh] TypeScript/JS 语句：`| 'category_tree'`
  | 'category_tree'
  // [zh] TypeScript/JS 语句：`| 'category_icon'`
  | 'category_icon'
  // [zh] TypeScript/JS 语句：`| 'product_list'`
  | 'product_list'
  // [zh] TypeScript/JS 语句：`| 'product_search'`
  | 'product_search'
  // [zh] TypeScript/JS 语句：`| 'product_detail'`
  | 'product_detail'
  // [zh] TypeScript/JS 语句：`| 'cart'`
  | 'cart'
  // [zh] TypeScript/JS 语句：`| 'checkout'`
  | 'checkout'
  // [zh] TypeScript/JS 语句：`| 'order_list'`
  | 'order_list'
  // [zh] TypeScript/JS 语句：`| 'order_pay'`
  | 'order_pay'
  // [zh] TypeScript/JS 语句：`| 'order_cancel'`
  | 'order_cancel'
  // [zh] TypeScript/JS 语句：`| 'order_confirm_receive'`
  | 'order_confirm_receive'
  // [zh] TypeScript/JS 语句：`| 'order_logistics'`
  | 'order_logistics'
  // [zh] TypeScript/JS 语句：`| 'order_refund'`
  | 'order_refund'
  // [zh] TypeScript/JS 语句：`| 'order_comment'`
  | 'order_comment'
  // [zh] TypeScript/JS 语句：`| 'user_login_email'`
  | 'user_login_email'
  // [zh] TypeScript/JS 语句：`| 'user_register'`
  | 'user_register'
  // [zh] TypeScript/JS 语句：`| 'user_profile'`
  | 'user_profile'
  // [zh] TypeScript/JS 语句：`| 'user_address'`
  | 'user_address'
  // [zh] TypeScript/JS 语句：`| 'coupon_plaza'`
  | 'coupon_plaza'
  // [zh] TypeScript/JS 语句：`| 'coupon_mine'`
  | 'coupon_mine'
  // [zh] TypeScript/JS 语句：`| 'favorite'`
  | 'favorite'
  // [zh] TypeScript/JS 语句：`| 'browse_history'`
  | 'browse_history'
  // [zh] TypeScript/JS 语句：`| 'sign_in'`
  | 'sign_in'
  // [zh] TypeScript/JS 语句：`| 'member_center'`
  | 'member_center'
  // [zh] TypeScript/JS 语句：`| 'notification'`
  | 'notification'
  // [zh] TypeScript/JS 语句：`| 'agent_chat'`
  | 'agent_chat'
  // [zh] TypeScript/JS 语句：`| 'pay_records'`
  | 'pay_records'
  // [zh] TypeScript/JS 语句：`| 'location_geocode'`
  | 'location_geocode'
  // [zh] TypeScript/JS 语句：`| 'wechat_login'`
  | 'wechat_login'
  // [zh] TypeScript/JS 语句：`| 'sms_login'`
  | 'sms_login'
  // [zh] TypeScript/JS 语句：`| 'distribution'`
  | 'distribution'
  // [zh] TypeScript/JS 语句：`| 'seckill'`
  | 'seckill'
  // [zh] TypeScript/JS 语句：`| 'bargain'`
  | 'bargain'
  // [zh] TypeScript/JS 语句：`| 'combination'`
  | 'combination'
  // [zh] TypeScript/JS 语句：`| 'points_mall'`
  | 'points_mall'
  // [zh] TypeScript/JS 语句：`| 'wallet_recharge'`
  | 'wallet_recharge'
  // [zh] TypeScript/JS 语句：`| 'invoice'`
  | 'invoice'
  // [zh] TypeScript/JS 语句：`| 'article_cms'`
  | 'article_cms'
  // [zh] TypeScript/JS 语句：`| 'multi_store'`
  | 'multi_store'
  // [zh] TypeScript/JS 语句：`| 'lottery'`
  | 'lottery'
  // [zh] 语句
  | 'pc_simlect_home';

// [zh] 导出模块成员
export interface FeatureMeta {
  // [zh] 语句
  id: FeatureId;
  // [zh] 语句
  label: string;
  
  // [zh] 语句
  simlectRef?: string;
  // [zh] 语句
  supported: boolean;
  
  // [zh] 语句
  eshopApi?: string;
  
  // [zh] 语句
  extensionHint?: string;
// [zh] 语法符号
}

// [zh] 导出模块成员
export const FEATURE_REGISTRY: FeatureMeta[] = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'home_recommend',`
    id: 'home_recommend',
    // [zh] TypeScript/JS 语句：`label: '首页推荐/商品流',`
    label: '首页推荐/商品流',
    // [zh] TypeScript/JS 语句：`simlectRef: 'pages/index',`
    simlectRef: 'pages/index',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'product/loadCommendProduct, loadProduct'`
    eshopApi: 'product/loadCommendProduct, loadProduct'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'home_diy',`
    id: 'home_diy',
    // [zh] TypeScript/JS 语句：`label: '首页 DIY 装修',`
    label: '首页 DIY 装修',
    // [zh] TypeScript/JS 语句：`simlectRef: 'v2/diy',`
    simlectRef: 'v2/diy',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '页面配置 JSON + 管理端发布，或短期静态配置'`
    extensionHint: '页面配置 JSON + 管理端发布，或短期静态配置'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'category_tree',`
    id: 'category_tree',
    // [zh] TypeScript/JS 语句：`label: '分类树',`
    label: '分类树',
    // [zh] TypeScript/JS 语句：`simlectRef: 'goods_cate/goods_cate1',`
    simlectRef: 'goods_cate/goods_cate1',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'product/loadCategory'`
    eshopApi: 'product/loadCategory'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'category_icon',`
    id: 'category_icon',
    // [zh] TypeScript/JS 语句：`label: '分类图标',`
    label: '分类图标',
    // [zh] TypeScript/JS 语句：`simlectRef: 'category.pic',`
    simlectRef: 'category.pic',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: 'SysCategory 增加 iconUrl + 管理端上传'`
    extensionHint: 'SysCategory 增加 iconUrl + 管理端上传'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'product_list',`
    id: 'product_list',
    // [zh] TypeScript/JS 语句：`label: '分类商品列表',`
    label: '分类商品列表',
    // [zh] TypeScript/JS 语句：`simlectRef: 'goods/goods_list',`
    simlectRef: 'goods/goods_list',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'product/loadProduct'`
    eshopApi: 'product/loadProduct'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'product_search',`
    id: 'product_search',
    // [zh] TypeScript/JS 语句：`label: '搜索',`
    label: '搜索',
    // [zh] TypeScript/JS 语句：`simlectRef: 'goods/goods_search',`
    simlectRef: 'goods/goods_search',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'product/search, search/*'`
    eshopApi: 'product/search, search/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'product_detail',`
    id: 'product_detail',
    // [zh] TypeScript/JS 语句：`label: '商品详情',`
    label: '商品详情',
    // [zh] TypeScript/JS 语句：`simlectRef: 'goods_details',`
    simlectRef: 'goods_details',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'product/getProduct'`
    eshopApi: 'product/getProduct'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'cart',`
    id: 'cart',
    // [zh] TypeScript/JS 语句：`label: '购物车',`
    label: '购物车',
    // [zh] TypeScript/JS 语句：`simlectRef: 'order_addcart',`
    simlectRef: 'order_addcart',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'productCart/*'`
    eshopApi: 'productCart/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'checkout',`
    id: 'checkout',
    // [zh] TypeScript/JS 语句：`label: '结算下单',`
    label: '结算下单',
    // [zh] TypeScript/JS 语句：`simlectRef: 'order_confirm',`
    simlectRef: 'order_confirm',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'order/postOrder'`
    eshopApi: 'order/postOrder'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'order_list',`
    id: 'order_list',
    // [zh] TypeScript/JS 语句：`label: '我的订单',`
    label: '我的订单',
    // [zh] TypeScript/JS 语句：`simlectRef: 'order_list',`
    simlectRef: 'order_list',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'order/loadMyOrder'`
    eshopApi: 'order/loadMyOrder'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'order_pay',`
    id: 'order_pay',
    // [zh] TypeScript/JS 语句：`label: '支付',`
    label: '支付',
    // [zh] TypeScript/JS 语句：`simlectRef: 'order_pay',`
    simlectRef: 'order_pay',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'order/getPayInfo + 支付宝回调'`
    eshopApi: 'order/getPayInfo + 支付宝回调'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'order_cancel',`
    id: 'order_cancel',
    // [zh] TypeScript/JS 语句：`label: '取消订单',`
    label: '取消订单',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'order/cancelOrder'`
    eshopApi: 'order/cancelOrder'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'order_confirm_receive',`
    id: 'order_confirm_receive',
    // [zh] TypeScript/JS 语句：`label: '确认收货',`
    label: '确认收货',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'order/confirmOrder'`
    eshopApi: 'order/confirmOrder'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'order_logistics',`
    id: 'order_logistics',
    // [zh] TypeScript/JS 语句：`label: '物流',`
    label: '物流',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'order/getLogistics'`
    eshopApi: 'order/getLogistics'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'order_refund',`
    id: 'order_refund',
    // [zh] TypeScript/JS 语句：`label: '退款',`
    label: '退款',
    // [zh] TypeScript/JS 语句：`simlectRef: 'user_return_list',`
    simlectRef: 'user_return_list',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'order/refundOrder（前端待封装）',`
    eshopApi: 'order/refundOrder（前端待封装）',
    // [zh] TypeScript/JS 语句：`extensionHint: '建议增加用户端退款单列表分页'`
    extensionHint: '建议增加用户端退款单列表分页'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'order_comment',`
    id: 'order_comment',
    // [zh] TypeScript/JS 语句：`label: '评价',`
    label: '评价',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'order/comment/*'`
    eshopApi: 'order/comment/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'user_login_email',`
    id: 'user_login_email',
    // [zh] TypeScript/JS 语句：`label: '邮箱登录',`
    label: '邮箱登录',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'account/login'`
    eshopApi: 'account/login'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'user_register',`
    id: 'user_register',
    // [zh] TypeScript/JS 语句：`label: '注册',`
    label: '注册',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'account/register'`
    eshopApi: 'account/register'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'user_profile',`
    id: 'user_profile',
    // [zh] TypeScript/JS 语句：`label: '个人资料',`
    label: '个人资料',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'account/getUserInfo, updateUserInfo'`
    eshopApi: 'account/getUserInfo, updateUserInfo'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'user_address',`
    id: 'user_address',
    // [zh] TypeScript/JS 语句：`label: '收货地址',`
    label: '收货地址',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'userAddress/*'`
    eshopApi: 'userAddress/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'coupon_plaza',`
    id: 'coupon_plaza',
    // [zh] TypeScript/JS 语句：`label: '优惠券广场',`
    label: '优惠券广场',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'discountCoupon/loadDiscountCoupon'`
    eshopApi: 'discountCoupon/loadDiscountCoupon'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'coupon_mine',`
    id: 'coupon_mine',
    // [zh] TypeScript/JS 语句：`label: '我的优惠券',`
    label: '我的优惠券',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'discountCoupon/loadUserCoupon'`
    eshopApi: 'discountCoupon/loadUserCoupon'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'favorite',`
    id: 'favorite',
    // [zh] TypeScript/JS 语句：`label: '收藏',`
    label: '收藏',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'userFavorite/*'`
    eshopApi: 'userFavorite/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'browse_history',`
    id: 'browse_history',
    // [zh] TypeScript/JS 语句：`label: '浏览足迹',`
    label: '浏览足迹',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'browseHistory/*'`
    eshopApi: 'browseHistory/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'sign_in',`
    id: 'sign_in',
    // [zh] TypeScript/JS 语句：`label: '签到',`
    label: '签到',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'sign/*'`
    eshopApi: 'sign/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'member_center',`
    id: 'member_center',
    // [zh] TypeScript/JS 语句：`label: '会员中心',`
    label: '会员中心',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'userMember/*'`
    eshopApi: 'userMember/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'notification',`
    id: 'notification',
    // [zh] TypeScript/JS 语句：`label: '消息中心',`
    label: '消息中心',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'userNotification/*'`
    eshopApi: 'userNotification/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'agent_chat',`
    id: 'agent_chat',
    // [zh] TypeScript/JS 语句：`label: '智能客服',`
    label: '智能客服',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'agent/*'`
    eshopApi: 'agent/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'pay_records',`
    id: 'pay_records',
    // [zh] TypeScript/JS 语句：`label: '支付记录',`
    label: '支付记录',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'payTrade/loadMyTrades'`
    eshopApi: 'payTrade/loadMyTrades'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'location_geocode',`
    id: 'location_geocode',
    // [zh] TypeScript/JS 语句：`label: '定位逆地理',`
    label: '定位逆地理',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: 'location/*'`
    eshopApi: 'location/*'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'wechat_login',`
    id: 'wechat_login',
    // [zh] TypeScript/JS 语句：`label: '微信登录',`
    label: '微信登录',
    // [zh] TypeScript/JS 语句：`simlectRef: 'wechat/*',`
    simlectRef: 'wechat/*',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '微信 OAuth + 账号绑定'`
    extensionHint: '微信 OAuth + 账号绑定'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'sms_login',`
    id: 'sms_login',
    // [zh] TypeScript/JS 语句：`label: '手机验证码登录',`
    label: '手机验证码登录',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '短信网关 + loginByMobile'`
    extensionHint: '短信网关 + loginByMobile'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'distribution',`
    id: 'distribution',
    // [zh] TypeScript/JS 语句：`label: '分销推广',`
    label: '分销推广',
    // [zh] TypeScript/JS 语句：`simlectRef: 'spread/*',`
    simlectRef: 'spread/*',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '分销员、佣金、提现'`
    extensionHint: '分销员、佣金、提现'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'seckill',`
    id: 'seckill',
    // [zh] TypeScript/JS 语句：`label: '秒杀',`
    label: '秒杀',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '秒杀活动与库存'`
    extensionHint: '秒杀活动与库存'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'bargain',`
    id: 'bargain',
    // [zh] TypeScript/JS 语句：`label: '砍价',`
    label: '砍价',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '砍价活动'`
    extensionHint: '砍价活动'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'combination',`
    id: 'combination',
    // [zh] TypeScript/JS 语句：`label: '拼团',`
    label: '拼团',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '拼团活动'`
    extensionHint: '拼团活动'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'points_mall',`
    id: 'points_mall',
    // [zh] TypeScript/JS 语句：`label: '积分商城',`
    label: '积分商城',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '积分商品与兑换订单'`
    extensionHint: '积分商品与兑换订单'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'wallet_recharge',`
    id: 'wallet_recharge',
    // [zh] TypeScript/JS 语句：`label: '余额充值提现',`
    label: '余额充值提现',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '钱包账户体系'`
    extensionHint: '钱包账户体系'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'invoice',`
    id: 'invoice',
    // [zh] TypeScript/JS 语句：`label: '发票',`
    label: '发票',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '发票抬头与开票'`
    extensionHint: '发票抬头与开票'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'article_cms',`
    id: 'article_cms',
    // [zh] TypeScript/JS 语句：`label: '文章资讯',`
    label: '文章资讯',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: 'CMS 文章 API'`
    extensionHint: 'CMS 文章 API'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'multi_store',`
    id: 'multi_store',
    // [zh] TypeScript/JS 语句：`label: '多门店',`
    label: '多门店',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '门店与门店库存'`
    extensionHint: '门店与门店库存'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'lottery',`
    id: 'lottery',
    // [zh] TypeScript/JS 语句：`label: '抽奖',`
    label: '抽奖',
    // [zh] TypeScript/JS 语句：`supported: false,`
    supported: false,
    // [zh] TypeScript/JS 语句：`extensionHint: '抽奖活动'`
    extensionHint: '抽奖活动'
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`id: 'pc_simlect_home',`
    id: 'pc_simlect_home',
    // [zh] TypeScript/JS 语句：`label: 'PC Simlect 首页',`
    label: 'PC Simlect 首页',
    // [zh] TypeScript/JS 语句：`simlectRef: 'simlect-origin/index.html',`
    simlectRef: 'simlect-origin/index.html',
    // [zh] TypeScript/JS 语句：`supported: true,`
    supported: true,
    // [zh] TypeScript/JS 语句：`eshopApi: '同首页商品/分类接口'`
    eshopApi: '同首页商品/分类接口'
  // [zh] 语法符号
  }
// [zh] 语句
];

// [zh] 导出模块成员
export function isFeatureSupported(id: FeatureId): boolean {
  // [zh] return 返回
  return FEATURE_REGISTRY.find((f) => f.id === id)?.supported ?? false;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function unsupportedFeatures(): FeatureMeta[] {
  // [zh] return 返回
  return FEATURE_REGISTRY.filter((f) => !f.supported);
// [zh] 语法符号
}
