import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useDeviceStore } from '@/stores/device';
import { usePcAgentPanelStore } from '@/stores/pcAgentPanel';

import { PRIMARY_TAB_PATHS } from '@/constants/tabPages';
import { resolveSafeRedirect } from '@/utils/navigation';

export { PRIMARY_TAB_PATHS };

const mainChildren: RouteRecordRaw[] = [
  {
    path: '',
    name: 'home',
    component: () => import('@/components/page/DevicePage.vue'),
    meta: {
      title: '首页',
      level: 1,
      pageMobile: () => import('@/views/HomeView.vue'),
      pageDesktop: () => import('@/views/pc/PcHomeView.vue')
    }
  },
  { path: 'search', name: 'search', component: () => import('@/views/SearchView.vue'), meta: { title: '分类', level: 1 } },
  {
    path: 'cart',
    name: 'cart',
    component: () => import('@/components/page/DevicePage.vue'),
    meta: {
      title: '购物车',
      level: 1,
      requiresAuth: true,
      pageMobile: () => import('@/views/CartView.vue'),
      pageDesktop: () => import('@/views/pc/PcCartView.vue')
    }
  },
  {
    path: 'account',
    name: 'account',
    component: () => import('@/views/AccountView.vue'),
    meta: { title: '我的', level: 1, requiresAuth: true }
  }
];

function subPage(
  path: string,
  mobile: () => Promise<any>,
  meta: Record<string, unknown>,
  desktop?: () => Promise<any>
): RouteRecordRaw {
  return {
    path,
    component: () => import('@/layouts/AdaptiveSubPageLayout.vue'),
    meta: { ...meta, showBack: true },
    children: [
      {
        path: '',
        component: () => import('@/components/page/DevicePage.vue'),
        meta: {
          showBack: true,
          ...meta,
          pageMobile: mobile,
          pageDesktop: desktop ?? mobile
        }
      }
    ]
  };
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/AdaptiveMainLayout.vue'),
      children: mainChildren
    },
    subPage('/search-portal', () => import('@/views/SearchPortalView.vue'), { title: '搜索' }),
    subPage(
      '/search-result',
      () => import('@/views/SearchResultView.vue'),
      { title: '搜索结果' },
      () => import('@/views/pc/PcSearchResultView.vue')
    ),
    subPage('/category/:categoryId', () => import('@/views/CategoryView.vue'), { title: '分类商品' }),
    subPage(
      '/product/:productId',
      () => import('@/views/ProductDetailView.vue'),
      { title: '商品详情', hideTabBar: true, hidePcPageHead: true },
      () => import('@/views/pc/PcProductDetailView.vue')
    ),
    subPage('/product/:productId/comments', () => import('@/views/ProductCommentsView.vue'), { title: '商品评价' }),
    subPage(
      '/coupons',
      () => import('@/views/CouponsView.vue'),
      { title: '优惠券广场' },
      () => import('@/views/pc/PcCouponsView.vue')
    ),
    subPage(
      '/wishlist',
      () => import('@/views/WishlistView.vue'),
      { title: '我的收藏' },
      () => import('@/views/pc/PcWishlistView.vue')
    ),
    subPage(
      '/footprint',
      () => import('@/views/FootprintView.vue'),
      { title: '我的足迹' },
      () => import('@/views/pc/PcFootprintView.vue')
    ),
    subPage('/member-center', () => import('@/views/MemberCenterView.vue'), { title: '会员中心', requiresAuth: true }),
    subPage('/pay-records', () => import('@/views/PayRecordView.vue'), { title: '支付记录', requiresAuth: true }),
    subPage('/notifications', () => import('@/views/NotificationView.vue'), { title: '消息中心', requiresAuth: true }),
    subPage('/after-sale', () => import('@/views/AfterSaleView.vue'), { title: '售后管理' }),
    subPage('/recommend', () => import('@/views/RecommendView.vue'), { title: '编辑精选' }),
    subPage('/login', () => import('@/views/LoginView.vue'), {
      title: '登录',
      requiresAuth: false,
      hideTabBar: true
    }),
    subPage('/register', () => import('@/views/RegisterView.vue'), { title: '注册', hideTabBar: true }),
    subPage('/forgot-password', () => import('@/views/ForgotPasswordView.vue'), { title: '找回密码', hideTabBar: true }),
    subPage('/checkout', () => import('@/views/CheckoutView.vue'), {
      title: '确认订单',
      requiresAuth: true,
      hideTabBar: true
    }, () => import('@/views/pc/PcCheckoutView.vue')),
    subPage('/payment/:payOrderId', () => import('@/views/PaymentView.vue'), {
      title: '订单支付',
      requiresAuth: true,
      hideTabBar: true
    }, () => import('@/views/pc/PcPaymentView.vue')),
    subPage(
      '/orders',
      () => import('@/views/OrdersView.vue'),
      { title: '我的订单', requiresAuth: true },
      () => import('@/views/pc/PcOrdersView.vue')
    ),
    subPage('/order/:orderId', () => import('@/views/OrderDetailView.vue'), { title: '订单详情', requiresAuth: true }),
    subPage('/order/:orderId/logistics', () => import('@/views/OrderLogisticsView.vue'), {
      title: '物流信息',
      requiresAuth: true
    }),
    subPage(
      '/my-coupons',
      () => import('@/views/MyCouponsView.vue'),
      { title: '我的优惠券', requiresAuth: true },
      () => import('@/views/pc/PcMyCouponsView.vue')
    ),
    subPage(
      '/address',
      () => import('@/views/AddressView.vue'),
      { title: '收货地址', requiresAuth: true },
      () => import('@/views/pc/PcAddressView.vue')
    ),
    subPage(
      '/sign',
      () => import('@/views/SignView.vue'),
      { title: '签到中心', requiresAuth: true },
      () => import('@/views/pc/PcSignView.vue')
    ),
    subPage(
      '/account/manage',
      () => import('@/views/AccountManageView.vue'),
      { title: '设置', requiresAuth: true },
      () => import('@/views/pc/PcAccountManageView.vue')
    ),
    subPage(
      '/account/profile',
      () => import('@/views/AccountProfileView.vue'),
      { title: '个人资料', requiresAuth: true },
      () => import('@/views/pc/PcAccountProfileView.vue')
    ),
    subPage('/account/settings', () => import('@/views/AccountSettingsView.vue'), {
      title: '修改个人信息',
      requiresAuth: true
    }),
    subPage('/account/password', () => import('@/views/AccountPasswordView.vue'), {
      title: '修改密码',
      requiresAuth: true
    }),
    subPage(
      '/ai-assistant',
      () => import('@/views/AIAssistantView.vue'),
      { title: '智能客服', requiresAuth: true, hideTabBar: true },
      () => import('@/views/pc/PcAIAssistantView.vue')
    ),
  ],
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    return { top: 0, left: 0 };
  }
});

router.beforeEach(async (to, from) => {
  const deviceStore = useDeviceStore();
  deviceStore.sync();
  const authStore = useAuthStore();
  const needsAuth = to.matched.some((r) => r.meta.requiresAuth);
  if (needsAuth && !authStore.isLoggedIn) {
    await authStore.tryRestoreSession();
  }
  if (needsAuth && !authStore.isLoggedIn) {
    if (authStore.loggingOut) {
      return { path: '/login', query: {}, replace: true };
    }
    return {
      path: '/login',
      query: { redirect: to.fullPath },
      replace: true
    };
  }

  if ((to.path === '/login' || to.path === '/register') && authStore.isLoggedIn) {
    return { path: resolveSafeRedirect(to.query.redirect), replace: true };
  }

  const isAgentRoute = to.path === '/ai-assistant';
  if (deviceStore.isDesktop && isAgentRoute) {
    usePcAgentPanelStore().open();
    if (from.path && from.path !== to.path && from.path !== '/login') {
      return false;
    }
    return { path: '/', replace: true };
  }

  if (to.path === '/login') {
    authStore.finishLogoutNavigation();
  }
  return true;
});

export default router;
