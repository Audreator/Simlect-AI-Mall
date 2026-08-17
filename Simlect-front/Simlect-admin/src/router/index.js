import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'
import { useDeviceStore } from '@/stores/device'
import { resolveDesktopPath } from '@/utils/device'

const DESKTOP_TO_MOBILE = {
  '/home': '/m/home',
  '/product': '/m/product',
  '/product/addProduct': '/m/product/edit',
  '/order/orderList': '/m/order',
  '/order/comment': '/m/order/comment',
  '/order/report': '/m/order/report',
  '/setting/imageModeration': '/m/more/imageModeration',
  '/user/userList': '/m/user',
  '/user/address': '/m/more/address',
  '/setting/agentMessage': '/m/more/agent',
  '/discountCoupon': '/m/more/coupon',
  '/marketing/searchHot': '/m/more/searchHot',
  '/data/statistics': '/m/more/statistics',
  '/data/mqCompensationLog': '/m/more/mqLog',
  '/data/tools': '/m/more/tools',
  '/setting/sensitiveWord': '/m/more/sensitiveWord',
  '/product/category': '/m/more/category',
  '/product/ProductProperty': '/m/more/productProperty',
  '/setting/logistics': '/m/more/logistics',
  '/setting/prompt': '/m/more/prompt',
  '/setting/rag': '/m/more/rag',
  '/marketing/signReward': '/m/more/signReward',
  '/marketing/memberLevelReward': '/m/more/memberLevelReward'
}

function resolveMobilePath(desktopPath) {
  if (desktopPath.startsWith('/product/updateProduct/')) {
    const id = desktopPath.split('/').pop()
    return id ? `/m/product/edit/${id}` : '/m/product'
  }
  return DESKTOP_TO_MOBILE[desktopPath] || '/m/home'
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/account/Account.vue'),
    },
    {
      path: '/m',
      name: 'MobileLayout',
      redirect: '/m/home',
      component: () => import('@/views/mobile/MobileShell.vue'),
      children: [
        { path: 'home', component: () => import('@/views/mobile/MobileHome.vue'), meta: { title: '工作台', tab: '/m/home' } },
        { path: 'product', component: () => import('@/views/mobile/MobileProductList.vue'), meta: { title: '商品管理', tab: '/m/product' } },
        { path: 'product/edit', component: () => import('@/views/product/edit/ProductEdit.vue'), meta: { title: '发布商品', tab: '/m/product', showBack: true } },
        { path: 'product/edit/:productId', component: () => import('@/views/product/edit/ProductEdit.vue'), meta: { title: '编辑商品', tab: '/m/product', showBack: true } },
        { path: 'order', component: () => import('@/views/mobile/MobileOrderList.vue'), meta: { title: '订单管理', tab: '/m/order' } },
        { path: 'order/comment', component: () => import('@/views/mobile/MobileOrderComment.vue'), meta: { title: '评价管理', tab: '/m/order', showBack: true } },
        { path: 'order/report', component: () => import('@/views/mobile/MobileCommentReport.vue'), meta: { title: '举报管理', tab: '/m/order', showBack: true } },
        { path: 'more/imageModeration', component: () => import('@/views/mobile/MobileImageModeration.vue'), meta: { title: '图片审核', tab: '/m/more', showBack: true } },
        { path: 'user', component: () => import('@/views/mobile/MobileUserList.vue'), meta: { title: '用户管理', tab: '/m/user' } },
        { path: 'more', component: () => import('@/views/mobile/MobileMore.vue'), meta: { title: '更多', tab: '/m/more' } },
        { path: 'more/agent', component: () => import('@/views/mobile/MobileAgentMessage.vue'), meta: { title: '客服记录', tab: '/m/more', showBack: true } },
        { path: 'more/coupon', component: () => import('@/views/mobile/MobileCoupon.vue'), meta: { title: '优惠券', tab: '/m/more', showBack: true } },
        { path: 'more/searchHot', component: () => import('@/views/mobile/MobileSearchHot.vue'), meta: { title: '热搜词', tab: '/m/more', showBack: true } },
        { path: 'more/statistics', component: () => import('@/views/mobile/MobileStatistics.vue'), meta: { title: '统计明细', tab: '/m/more', showBack: true } },
        { path: 'more/mqLog', component: () => import('@/views/mobile/MobileMqCompensationLog.vue'), meta: { title: 'MQ补偿日志', tab: '/m/more', showBack: true } },
        { path: 'more/address', component: () => import('@/views/mobile/MobileUserAddress.vue'), meta: { title: '收货地址', tab: '/m/more', showBack: true } },
        { path: 'more/tools', component: () => import('@/views/mobile/MobileTools.vue'), meta: { title: '运营工具', tab: '/m/more', showBack: true } },
        { path: 'more/sensitiveWord', component: () => import('@/views/mobile/MobileSensitiveWord.vue'), meta: { title: '敏感词管理', tab: '/m/more', showBack: true } },
        { path: 'more/category', component: () => import('@/views/mobile/MobileCategory.vue'), meta: { title: '分类管理', tab: '/m/more', showBack: true } },
        { path: 'more/productProperty', component: () => import('@/views/mobile/MobileProductProperty.vue'), meta: { title: '商品属性', tab: '/m/more', showBack: true } },
        { path: 'more/logistics', component: () => import('@/views/mobile/MobileLogistics.vue'), meta: { title: '发货信息', tab: '/m/more', showBack: true } },
        { path: 'more/prompt', component: () => import('@/views/mobile/MobilePrompt.vue'), meta: { title: '提示词', tab: '/m/more', showBack: true } },
        { path: 'more/rag', component: () => import('@/views/mobile/MobileRag.vue'), meta: { title: 'RAG 知识库', tab: '/m/more', showBack: true } },
        { path: 'more/signReward', component: () => import('@/views/mobile/MobileSignReward.vue'), meta: { title: '签到发券', tab: '/m/more', showBack: true } },
        { path: 'more/memberLevelReward', component: () => import('@/views/mobile/MobileMemberLevelReward.vue'), meta: { title: '升级礼券', tab: '/m/more', showBack: true } }
      ]
    },
    {
      path: '/',
      name: 'Layout',
      redirect: "/login",
      component: () => import('@/views/Layout.vue'),
      children: [
        {
          path: '/home',
          name: 'home',
          component: () => import('@/views/home/Home.vue'),
          meta: {
            itemList: ["首页"]
          }
        },
        {
          path: '/product/category',
          name: 'category',
          component: () => import('@/views/product/Category.vue'),
          meta: {
            itemList: ["商品", "分类管理"]
          }
        },
        {
          path: '/product/ProductProperty',
          name: 'productProperty',
          component: () => import('@/views/product/ProductProperty.vue'),
          meta: {
            itemList: ["商品", "商品属性"]
          }
        },
        {
          path: '/product/addProduct',
          name: 'addProduct',
          component: () => import('@/views/product/edit/ProductEdit.vue'),
          meta: {
            itemList: ["商品", "商品管理"]
          }
        },
        {
          path: '/product/updateProduct/:productId',
          name: 'updateProduct',
          component: () => import('@/views/product/edit/ProductEdit.vue'),
          meta: {
            itemList: ["商品", "商品管理"]
          }
        },
        {
          path: '/product',
          name: 'product',
          component: () => import('@/views/product/ProductList.vue'),
          meta: {
            itemList: ["商品", "商品管理"]
          }
        },
        {
          path: '/order/orderList',
          name: '订单管理',
          component: () => import('@/views/order/OrderList.vue'),
          meta: {
            itemList: ["订单", "订单管理"]
          }
        },
        {
          path: '/order/comment',
          name: '订单评论',
          component: () => import('@/views/order/OrderCommentList.vue'),
          meta: {
            itemList: ["订单", "订单评论"]
          }
        },
        {
          path: '/order/report',
          name: '举报管理',
          component: () => import('@/views/order/CommentReportList.vue'),
          meta: {
            itemList: ["订单", "举报管理"]
          }
        },
        {
          path: '/setting/imageModeration',
          name: '图片审核',
          component: () => import('@/views/setting/ImageModerationList.vue'),
          meta: {
            itemList: ["订单", "图片违规复核"]
          }
        },
        {
          path: '/user/userList',
          name: '用户管理',
          component: () => import('@/views/user/UserList.vue'),
          meta: {
            itemList: ["用户管理", "用户列表"]
          }
        },
        {
          path: '/setting/logistics',
          name: '发货地址',
          component: () => import('@/views/setting/Logistics.vue'),
          meta: {
            itemList: ["设置", "发货地址"]
          }
        },
        {
          path: '/setting/prompt',
          name: '提示词',
          component: () => import('@/views/setting/Prompt.vue'),
          meta: {
            itemList: ["设置", "提示词管理"]
          }
        },
        {
          path: '/setting/rag',
          name: 'RAG知识库',
          component: () => import('@/views/setting/Rag.vue'),
          meta: {
            itemList: ["设置", "RAG知识库"]
          }
        },
        {
          path: '/discountCoupon',
          name: '优惠券管理',
          component: () => import('@/views/discount/DiscountCouponList.vue'),
          meta: {
            itemList: ["营销", "优惠券管理"]
          }
        },
        {
          path: '/marketing/searchHot',
          name: '热搜词管理',
          component: () => import('@/views/marketing/SearchHotKeyword.vue'),
          meta: {
            itemList: ["营销", "热搜词管理"]
          }
        },
        {
          path: '/marketing/signReward',
          name: '签到发券配置',
          component: () => import('@/views/marketing/SignRewardConfig.vue'),
          meta: {
            itemList: ["营销", "签到发券配置"]
          }
        },
        {
          path: '/marketing/memberLevelReward',
          name: '会员升级礼券',
          component: () => import('@/views/marketing/MemberLevelRewardConfig.vue'),
          meta: {
            itemList: ["营销", "会员升级礼券"]
          }
        },
        {
          path: '/data/tools',
          name: '运营工具',
          component: () => import('@/views/data/OperateTools.vue'),
          meta: {
            itemList: ["数据中心", "运营工具"]
          }
        },
        {
          path: '/data/statistics',
          name: '统计明细',
          component: () => import('@/views/data/StatisticsList.vue'),
          meta: {
            itemList: ["数据中心", "统计明细"]
          }
        },
        {
          path: '/data/mqCompensationLog',
          name: 'MQ补偿审查',
          component: () => import('@/views/data/MqCompensationLogList.vue'),
          meta: {
            itemList: ["数据中心", "MQ补偿审查"]
          }
        },
        {
          path: '/data/ragSyncFailure',
          name: 'RAG同步失败',
          component: () => import('@/views/data/RagSyncFailureList.vue'),
          meta: {
            itemList: ["数据中心", "RAG同步失败"]
          }
        },
        {
          path: '/user/address',
          name: '收货地址',
          component: () => import('@/views/user/UserAddressList.vue'),
          meta: {
            itemList: ["用户管理", "收货地址"]
          }
        },
        {
          path: '/setting/agentMessage',
          name: '客服对话',
          component: () => import('@/views/setting/AgentMessageList.vue'),
          meta: {
            itemList: ["系统设置", "客服对话记录"]
          }
        },
        {
          path: '/setting/sensitiveWord',
          name: '敏感词管理',
          component: () => import('@/views/setting/SensitiveWord.vue'),
          meta: {
            itemList: ["系统设置", "敏感词管理"]
          }
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const device = useDeviceStore()
  device.sync()
  if (to.path === '/login') return true

  // 登录态校验：未登录一律重定向登录页（Cookie 为 httpOnly，须经后端会话接口确认）
  const checkLogin = async () => {
    try {
      const base = import.meta.env.PROD ? '/admin-api' : '/api'
      const res = await axios.get(`${base}/account/checkLogin`, { withCredentials: true })
      return res.data?.data === true
    } catch (e) {
      return false
    }
  }
  if (!(await checkLogin())) {
    return { path: '/login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }

  const isMobileRoute = to.path === '/m' || to.path.startsWith('/m/')

  if (device.isMobile && !isMobileRoute) {
    const target = resolveMobilePath(to.path)
    return target || '/m/home'
  }

  if (device.isDesktop && isMobileRoute) {
    return resolveDesktopPath(to.path)
  }

  return true
})

export default router
