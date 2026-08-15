// [zh] 导入 `vue-router`
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/stores/device`
import { useDeviceStore } from '@/stores/device';
// [zh] 导入 `@/stores/pcAgentPanel`
import { usePcAgentPanelStore } from '@/stores/pcAgentPanel';

// [zh] 导入 `@/constants/tabPages`
import { PRIMARY_TAB_PATHS } from '@/constants/tabPages';
// [zh] 导入 `@/utils/navigation`
import { resolveSafeRedirect } from '@/utils/navigation';

// [zh] 导出模块成员
export { PRIMARY_TAB_PATHS };

// [zh] 声明变量 `mainChildren`
const mainChildren: RouteRecordRaw[] = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`path: '',`
    path: '',
    // [zh] TypeScript/JS 语句：`name: 'home',`
    name: 'home',
    // [zh] TypeScript/JS 语句：`component: () => import('@/components/page/DevicePage.v`
    component: () => import('@/components/page/DevicePage.vue'),
    // [zh] 代码块开始
    meta: {
      // [zh] TypeScript/JS 语句：`title: '首页',`
      title: '首页',
      // [zh] TypeScript/JS 语句：`level: 1,`
      level: 1,
      // [zh] TypeScript/JS 语句：`pageMobile: () => import('@/views/HomeView.vue'),`
      pageMobile: () => import('@/views/HomeView.vue'),
      // [zh] TypeScript/JS 语句：`pageDesktop: () => import('@/views/pc/PcHomeView.vue')`
      pageDesktop: () => import('@/views/pc/PcHomeView.vue')
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`{ path: 'search', name: 'search', component: () => impo`
  { path: 'search', name: 'search', component: () => import('@/views/SearchView.vue'), meta: { title: '分类', level: 1 } },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`path: 'cart',`
    path: 'cart',
    // [zh] TypeScript/JS 语句：`name: 'cart',`
    name: 'cart',
    // [zh] TypeScript/JS 语句：`component: () => import('@/components/page/DevicePage.v`
    component: () => import('@/components/page/DevicePage.vue'),
    // [zh] 代码块开始
    meta: {
      // [zh] TypeScript/JS 语句：`title: '购物车',`
      title: '购物车',
      // [zh] TypeScript/JS 语句：`level: 1,`
      level: 1,
      // [zh] TypeScript/JS 语句：`requiresAuth: true,`
      requiresAuth: true,
      // [zh] TypeScript/JS 语句：`pageMobile: () => import('@/views/CartView.vue'),`
      pageMobile: () => import('@/views/CartView.vue'),
      // [zh] TypeScript/JS 语句：`pageDesktop: () => import('@/views/pc/PcCartView.vue')`
      pageDesktop: () => import('@/views/pc/PcCartView.vue')
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`path: 'account',`
    path: 'account',
    // [zh] TypeScript/JS 语句：`name: 'account',`
    name: 'account',
    // [zh] TypeScript/JS 语句：`component: () => import('@/views/AccountView.vue'),`
    component: () => import('@/views/AccountView.vue'),
    // [zh] TypeScript/JS 语句：`meta: { title: '我的', level: 1, requiresAuth: true }`
    meta: { title: '我的', level: 1, requiresAuth: true }
  // [zh] 语法符号
  }
// [zh] 语句
];

// [zh] 函数 `subPage`
function subPage(
  // [zh] TypeScript/JS 语句：`path: string,`
  path: string,
  // [zh] TypeScript/JS 语句：`mobile: () => Promise<any>,`
  mobile: () => Promise<any>,
  // [zh] TypeScript/JS 语句：`meta: Record<string, unknown>,`
  meta: Record<string, unknown>,
  // [zh] TypeScript/JS 语句：`desktop?: () => Promise<any>`
  desktop?: () => Promise<any>
// [zh] 代码块开始
): RouteRecordRaw {
  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`path,`
    path,
    // [zh] TypeScript/JS 语句：`component: () => import('@/layouts/AdaptiveSubPageLayou`
    component: () => import('@/layouts/AdaptiveSubPageLayout.vue'),
    // [zh] TypeScript/JS 语句：`meta: { ...meta, showBack: true },`
    meta: { ...meta, showBack: true },
    // [zh] TypeScript/JS 语句：`children: [`
    children: [
      // [zh] 代码块开始
      {
        // [zh] TypeScript/JS 语句：`path: '',`
        path: '',
        // [zh] TypeScript/JS 语句：`component: () => import('@/components/page/DevicePage.v`
        component: () => import('@/components/page/DevicePage.vue'),
        // [zh] 代码块开始
        meta: {
          // [zh] TypeScript/JS 语句：`showBack: true,`
          showBack: true,
          // [zh] TypeScript/JS 语句：`...meta,`
          ...meta,
          // [zh] TypeScript/JS 语句：`pageMobile: mobile,`
          pageMobile: mobile,
          // [zh] TypeScript/JS 语句：`pageDesktop: desktop ?? mobile`
          pageDesktop: desktop ?? mobile
        // [zh] 语法符号
        }
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`]`
    ]
  // [zh] 语句
  };
// [zh] 语法符号
}

// [zh] 声明变量 `router`
const router = createRouter({
  // [zh] TypeScript/JS 语句：`history: createWebHistory(),`
  history: createWebHistory(),
  // [zh] TypeScript/JS 语句：`routes: [`
  routes: [
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`path: '/',`
      path: '/',
      // [zh] TypeScript/JS 语句：`component: () => import('@/layouts/AdaptiveMainLayout.v`
      component: () => import('@/layouts/AdaptiveMainLayout.vue'),
      // [zh] TypeScript/JS 语句：`children: mainChildren`
      children: mainChildren
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`subPage('/search-portal', () => import('@/views/SearchP`
    subPage('/search-portal', () => import('@/views/SearchPortalView.vue'), { title: '搜索' }),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/search-result',`
      '/search-result',
      // [zh] TypeScript/JS 语句：`() => import('@/views/SearchResultView.vue'),`
      () => import('@/views/SearchResultView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '搜索结果' },`
      { title: '搜索结果' },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcSearchResultView.vue')`
      () => import('@/views/pc/PcSearchResultView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] TypeScript/JS 语句：`subPage('/category/:categoryId', () => import('@/views/`
    subPage('/category/:categoryId', () => import('@/views/CategoryView.vue'), { title: '分类商品' }),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/product/:productId',`
      '/product/:productId',
      // [zh] TypeScript/JS 语句：`() => import('@/views/ProductDetailView.vue'),`
      () => import('@/views/ProductDetailView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '商品详情', hideTabBar: true, hidePcPageHead: true`
      { title: '商品详情', hideTabBar: true, hidePcPageHead: true },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcProductDetailView.vue')`
      () => import('@/views/pc/PcProductDetailView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] TypeScript/JS 语句：`subPage('/product/:productId/comments', () => import('@`
    subPage('/product/:productId/comments', () => import('@/views/ProductCommentsView.vue'), { title: '商品评价' }),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/coupons',`
      '/coupons',
      // [zh] TypeScript/JS 语句：`() => import('@/views/CouponsView.vue'),`
      () => import('@/views/CouponsView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '优惠券广场' },`
      { title: '优惠券广场' },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcCouponsView.vue')`
      () => import('@/views/pc/PcCouponsView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/wishlist',`
      '/wishlist',
      // [zh] TypeScript/JS 语句：`() => import('@/views/WishlistView.vue'),`
      () => import('@/views/WishlistView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '我的收藏' },`
      { title: '我的收藏' },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcWishlistView.vue')`
      () => import('@/views/pc/PcWishlistView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/footprint',`
      '/footprint',
      // [zh] TypeScript/JS 语句：`() => import('@/views/FootprintView.vue'),`
      () => import('@/views/FootprintView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '我的足迹' },`
      { title: '我的足迹' },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcFootprintView.vue')`
      () => import('@/views/pc/PcFootprintView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] TypeScript/JS 语句：`subPage('/member-center', () => import('@/views/MemberC`
    subPage('/member-center', () => import('@/views/MemberCenterView.vue'), { title: '会员中心', requiresAuth: true }),
    // [zh] TypeScript/JS 语句：`subPage('/pay-records', () => import('@/views/PayRecord`
    subPage('/pay-records', () => import('@/views/PayRecordView.vue'), { title: '支付记录', requiresAuth: true }),
    // [zh] TypeScript/JS 语句：`subPage('/notifications', () => import('@/views/Notific`
    subPage('/notifications', () => import('@/views/NotificationView.vue'), { title: '消息中心', requiresAuth: true }),
    // [zh] TypeScript/JS 语句：`subPage('/after-sale', () => import('@/views/AfterSaleV`
    subPage('/after-sale', () => import('@/views/AfterSaleView.vue'), { title: '售后管理' }),
    // [zh] TypeScript/JS 语句：`subPage('/recommend', () => import('@/views/RecommendVi`
    subPage('/recommend', () => import('@/views/RecommendView.vue'), { title: '编辑精选' }),
    // [zh] 代码块开始
    subPage('/login', () => import('@/views/LoginView.vue'), {
      // [zh] TypeScript/JS 语句：`title: '登录',`
      title: '登录',
      // [zh] TypeScript/JS 语句：`requiresAuth: false,`
      requiresAuth: false,
      // [zh] TypeScript/JS 语句：`hideTabBar: true`
      hideTabBar: true
    // [zh] TypeScript/JS 语句：`}),`
    }),
    // [zh] TypeScript/JS 语句：`subPage('/register', () => import('@/views/RegisterView`
    subPage('/register', () => import('@/views/RegisterView.vue'), { title: '注册', hideTabBar: true }),
    // [zh] TypeScript/JS 语句：`subPage('/forgot-password', () => import('@/views/Forgo`
    subPage('/forgot-password', () => import('@/views/ForgotPasswordView.vue'), { title: '找回密码', hideTabBar: true }),
    // [zh] 代码块开始
    subPage('/checkout', () => import('@/views/CheckoutView.vue'), {
      // [zh] TypeScript/JS 语句：`title: '确认订单',`
      title: '确认订单',
      // [zh] TypeScript/JS 语句：`requiresAuth: true,`
      requiresAuth: true,
      // [zh] TypeScript/JS 语句：`hideTabBar: true`
      hideTabBar: true
    // [zh] TypeScript/JS 语句：`}, () => import('@/views/pc/PcCheckoutView.vue')),`
    }, () => import('@/views/pc/PcCheckoutView.vue')),
    // [zh] 代码块开始
    subPage('/payment/:payOrderId', () => import('@/views/PaymentView.vue'), {
      // [zh] TypeScript/JS 语句：`title: '订单支付',`
      title: '订单支付',
      // [zh] TypeScript/JS 语句：`requiresAuth: true,`
      requiresAuth: true,
      // [zh] TypeScript/JS 语句：`hideTabBar: true`
      hideTabBar: true
    // [zh] TypeScript/JS 语句：`}, () => import('@/views/pc/PcPaymentView.vue')),`
    }, () => import('@/views/pc/PcPaymentView.vue')),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/orders',`
      '/orders',
      // [zh] TypeScript/JS 语句：`() => import('@/views/OrdersView.vue'),`
      () => import('@/views/OrdersView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '我的订单', requiresAuth: true },`
      { title: '我的订单', requiresAuth: true },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcOrdersView.vue')`
      () => import('@/views/pc/PcOrdersView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] TypeScript/JS 语句：`subPage('/order/:orderId', () => import('@/views/OrderD`
    subPage('/order/:orderId', () => import('@/views/OrderDetailView.vue'), { title: '订单详情', requiresAuth: true }),
    // [zh] 代码块开始
    subPage('/order/:orderId/logistics', () => import('@/views/OrderLogisticsView.vue'), {
      // [zh] TypeScript/JS 语句：`title: '物流信息',`
      title: '物流信息',
      // [zh] TypeScript/JS 语句：`requiresAuth: true`
      requiresAuth: true
    // [zh] TypeScript/JS 语句：`}),`
    }),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/my-coupons',`
      '/my-coupons',
      // [zh] TypeScript/JS 语句：`() => import('@/views/MyCouponsView.vue'),`
      () => import('@/views/MyCouponsView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '我的优惠券', requiresAuth: true },`
      { title: '我的优惠券', requiresAuth: true },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcMyCouponsView.vue')`
      () => import('@/views/pc/PcMyCouponsView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/address',`
      '/address',
      // [zh] TypeScript/JS 语句：`() => import('@/views/AddressView.vue'),`
      () => import('@/views/AddressView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '收货地址', requiresAuth: true },`
      { title: '收货地址', requiresAuth: true },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcAddressView.vue')`
      () => import('@/views/pc/PcAddressView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/sign',`
      '/sign',
      // [zh] TypeScript/JS 语句：`() => import('@/views/SignView.vue'),`
      () => import('@/views/SignView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '签到中心', requiresAuth: true },`
      { title: '签到中心', requiresAuth: true },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcSignView.vue')`
      () => import('@/views/pc/PcSignView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/account/manage',`
      '/account/manage',
      // [zh] TypeScript/JS 语句：`() => import('@/views/AccountManageView.vue'),`
      () => import('@/views/AccountManageView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '设置', requiresAuth: true },`
      { title: '设置', requiresAuth: true },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcAccountManageView.vue')`
      () => import('@/views/pc/PcAccountManageView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/account/profile',`
      '/account/profile',
      // [zh] TypeScript/JS 语句：`() => import('@/views/AccountProfileView.vue'),`
      () => import('@/views/AccountProfileView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '个人资料', requiresAuth: true },`
      { title: '个人资料', requiresAuth: true },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcAccountProfileView.vue')`
      () => import('@/views/pc/PcAccountProfileView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
    // [zh] 代码块开始
    subPage('/account/settings', () => import('@/views/AccountSettingsView.vue'), {
      // [zh] TypeScript/JS 语句：`title: '修改个人信息',`
      title: '修改个人信息',
      // [zh] TypeScript/JS 语句：`requiresAuth: true`
      requiresAuth: true
    // [zh] TypeScript/JS 语句：`}),`
    }),
    // [zh] 代码块开始
    subPage('/account/password', () => import('@/views/AccountPasswordView.vue'), {
      // [zh] TypeScript/JS 语句：`title: '修改密码',`
      title: '修改密码',
      // [zh] TypeScript/JS 语句：`requiresAuth: true`
      requiresAuth: true
    // [zh] TypeScript/JS 语句：`}),`
    }),
    // [zh] TypeScript/JS 语句：`subPage(`
    subPage(
      // [zh] TypeScript/JS 语句：`'/ai-assistant',`
      '/ai-assistant',
      // [zh] TypeScript/JS 语句：`() => import('@/views/AIAssistantView.vue'),`
      () => import('@/views/AIAssistantView.vue'),
      // [zh] TypeScript/JS 语句：`{ title: '智能客服', requiresAuth: true, hideTabBar: true }`
      { title: '智能客服', requiresAuth: true, hideTabBar: true },
      // [zh] TypeScript/JS 语句：`() => import('@/views/pc/PcAIAssistantView.vue')`
      () => import('@/views/pc/PcAIAssistantView.vue')
    // [zh] TypeScript/JS 语句：`),`
    ),
  // [zh] 语法符号
  ],
  // [zh] 代码块开始
  scrollBehavior(_to, _from, savedPosition) {
    // [zh] 条件判断 if
    if (savedPosition) {
      // [zh] return 返回
      return savedPosition;
    // [zh] 语法符号
    }
    // [zh] return 返回
    return { top: 0, left: 0 };
  // [zh] 语法符号
  }
// [zh] 语句
});

// [zh] 代码块开始
router.beforeEach(async (to, from) => {
  // [zh] 声明变量 `deviceStore`
  const deviceStore = useDeviceStore();
  // [zh] 语句
  deviceStore.sync();
  // [zh] 声明变量 `authStore`
  const authStore = useAuthStore();
  // [zh] 声明变量 `needsAuth`
  const needsAuth = to.matched.some((r) => r.meta.requiresAuth);
  // [zh] 条件判断 if
  if (needsAuth && !authStore.isLoggedIn) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await authStore.tryRestoreSession();
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (needsAuth && !authStore.isLoggedIn) {
    // [zh] 条件判断 if
    if (authStore.loggingOut) {
      // [zh] return 返回
      return { path: '/login', query: {}, replace: true };
    // [zh] 语法符号
    }
    // [zh] return 返回
    return {
      // [zh] TypeScript/JS 语句：`path: '/login',`
      path: '/login',
      // [zh] TypeScript/JS 语句：`query: { redirect: to.fullPath },`
      query: { redirect: to.fullPath },
      // [zh] TypeScript/JS 语句：`replace: true`
      replace: true
    // [zh] 语句
    };
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if ((to.path === '/login' || to.path === '/register') && authStore.isLoggedIn) {
    // [zh] return 返回
    return { path: resolveSafeRedirect(to.query.redirect), replace: true };
  // [zh] 语法符号
  }

  // [zh] 声明变量 `isAgentRoute`
  const isAgentRoute = to.path === '/ai-assistant';
  // [zh] 条件判断 if
  if (deviceStore.isDesktop && isAgentRoute) {
    // [zh] 语句
    usePcAgentPanelStore().open();
    // [zh] 条件判断 if
    if (from.path && from.path !== to.path && from.path !== '/login') {
      // [zh] return 返回
      return false;
    // [zh] 语法符号
    }
    // [zh] return 返回
    return { path: '/', replace: true };
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (to.path === '/login') {
    // [zh] 语句
    authStore.finishLogoutNavigation();
  // [zh] 语法符号
  }
  // [zh] return 返回
  return true;
// [zh] 语句
});

// [zh] 导出模块成员
export default router;
