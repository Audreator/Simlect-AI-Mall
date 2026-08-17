<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="layout">
    <!-- [zh] 开始标签 `<aside>` -->
    <aside class="left-side">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="left-side-content">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="logo">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="logo-row">
            <!-- [zh] 开始标签 `<BrandMark>` -->
            <BrandMark class="logo-mark" />
            <!-- [zh] 开始标签 `<div>` -->
            <div class="logo-copy">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="logo-text">简选</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span class="logo-sub">Simlect Admin</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<nav>` -->
        <nav class="menu-nav">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-for="item in menuList" :key="item.path || item.name">
            <!-- [zh] 开始标签 `<div>` -->
            <div :class="['menu-item', isMenuActive(item) ? 'active' : '']" @click="jump(item)">
              <!-- [zh] 开始标签 `<div>` -->
              <div :class="['iconfont', `icon-${item.icon}`, 'menu-icon']"></div>
              <!-- [zh] 开始标签 `<div>` -->
              <div class="menu-name">{{ item.name }}</div>
              <!-- [zh] 开始标签 `<div>` -->
              <div
                v-if="item.children"
                :class="['iconfont', 'icon-right', 'icon-down', item.opened ? 'icon-right-opened' : 'icon-right-closed']"
              ></div>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div
              v-if="item.children"
              :class="['submenu-container', item.opened ? 'submenu-opened' : 'submenu-closed']"
            >
              <!-- [zh] 开始标签 `<div>` -->
              <div
                v-for="sub in item.children"
                :key="sub.path"
                :class="['submenu-item', route.path === sub.path ? 'active' : '']"
                @click="jump(sub)"
              >
                <!-- [zh] 开始标签 `<span>` -->
                <span class="submenu-dot"></span>
                <!-- [zh] Mustache 插值表达式 -->
                {{ sub.name }}
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          </template>
        </nav>
      </div>
      <div class="sidebar-glow" aria-hidden="true"></div>
    </aside>

    <div class="right">
      <header class="top">
        <div class="top-main">
          <h1 class="page-title">{{ pageTitle }}</h1>
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item v-for="item in route.meta.itemList" :key="item">
              {{ item }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="top-actions">
          <button type="button" class="action-pill" @click="tool">
            <span class="iconfont icon-setting action-pill__icon"></span>
            小工具
          </button>
          <button type="button" class="action-pill action-pill--ghost" @click="switchToMobile">
            手机版
          </button>
          <div class="user-chip">
            <span class="user-avatar">管</span>
            <span class="user-name">管理员</span>
            <button type="button" class="logout-btn" @click="logout">退出</button>
          </div>
        </div>
      </header>
      <main class="right-body" :class="{ 'is-home': route.path === '/home' }">
        <router-view></router-view>
      </main>
    </div>
  </div>
  <Tool ref="toolRef"></Tool>
</template>

<script setup>
import Tool from './Tool.vue'
import BrandMark from '@/components/BrandMark.vue'
import { ref, getCurrentInstance, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { switchToMobileView } from '@/utils/device'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const pageTitle = computed(() => {
  const list = route.meta.itemList
  if (Array.isArray(list) && list.length) {
    return list[list.length - 1]
  }
  return '工作台'
})

const isMenuActive = (item) => {
  if (item.path && route.path === item.path) return true
  if (item.children) {
    return item.children.some((sub) => route.path === sub.path)
  }
  return false
}

const menuList = ref([
  {
    name: '首页',
    icon: 'home',
    path: '/home',
  },
  {
    name: '商品',
    icon: 'product',
    opened: true,
    children: [
      { name: '分类管理', path: '/product/category' },
      { name: '商品属性', path: '/product/ProductProperty' },
      { name: '商品管理', path: '/product' },
    ],
  },
  {
    name: '订单',
    icon: 'order',
    opened: true,
    children: [
      { name: '订单管理', path: '/order/orderList' },
      { name: '订单评论', path: '/order/comment' },
      { name: '举报管理', path: '/order/report' },
      { name: '图片违规复核', path: '/setting/imageModeration' },
    ],
  },
  {
    name: '用户管理',
    icon: 'user',
    opened: true,
    children: [
      { name: '用户列表', path: '/user/userList' },
      { name: '收货地址', path: '/user/address' },
    ],
  },
  {
    name: '数据中心',
    icon: 'home',
    opened: false,
    children: [
      { name: '统计明细', path: '/data/statistics' },
      { name: 'MQ补偿审查', path: '/data/mqCompensationLog' },
      { name: 'RAG同步失败', path: '/data/ragSyncFailure' },
      { name: '运营工具', path: '/data/tools' },
    ],
  },
  {
    name: '系统设置',
    icon: 'setting',
    opened: true,
    children: [
      { name: '发货信息管理', path: '/setting/logistics' },
      { name: '提示词管理', path: '/setting/prompt' },
      { name: 'RAG知识库', path: '/setting/rag' },
      { name: '敏感词管理', path: '/setting/sensitiveWord' },
      { name: '客服对话记录', path: '/setting/agentMessage' },
    ],
  },
  {
    name: '营销',
    icon: 'product',
    opened: true,
    children: [
      { name: '优惠券管理', path: '/discountCoupon' },
      { name: '热搜词管理', path: '/marketing/searchHot' },
      { name: '签到发券配置', path: '/marketing/signReward' },
      { name: '会员升级礼券', path: '/marketing/memberLevelReward' },
    ],
  },
])

const jump = (item) => {
  if (item.children) {
    item.opened = !item.opened
    return
  }
  router.push(item.path)
}

const logout = () => {
  proxy.Confirm({
    message: '确定要退出吗?',
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.logout,
      })
      if (!result) {
        return
      }
      router.push('/login')
    },
  })
}

const toolRef = ref()
const tool = () => {
  toolRef.value.show()
}

const switchToMobile = () => {
  switchToMobileView('/m/home')
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.layout {` */
.layout {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `min-height: 100vh;` */
  min-height: 100vh;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;

  /* [zh] 样式规则 `.left-side {` */
  .left-side {
    /* [zh] 样式规则 `position: relative;` */
    position: relative;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `width: 248px;` */
    width: 248px;
    /* [zh] 样式规则 `height: 100vh;` */
    height: 100vh;
    /* [zh] 样式规则 `overflow: auto;` */
    overflow: auto;
    /* [zh] 样式规则 `background: linear-gradient(185deg, var(` */
    background: linear-gradient(185deg, var(--sidebar-bg) 0%, var(--sidebar-bg-end) 100%);
    /* [zh] 样式规则 `border-right: 1px solid rgba(255, 255, 2` */
    border-right: 1px solid rgba(255, 255, 255, 0.06);
    /* [zh] 样式规则 `box-shadow: 4px 0 24px rgba(0, 0, 0, 0.0` */
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.08);

    /* [zh] 样式规则 `.sidebar-glow {` */
    .sidebar-glow {
      /* [zh] 样式规则 `position: absolute;` */
      position: absolute;
      /* [zh] 样式规则 `top: 0;` */
      top: 0;
      /* [zh] 样式规则 `right: 0;` */
      right: 0;
      /* [zh] 样式规则 `width: 120px;` */
      width: 120px;
      /* [zh] 样式规则 `height: 120px;` */
      height: 120px;
      /* [zh] 样式规则 `background: radial-gradient(circle, rgba` */
      background: radial-gradient(circle, rgba(201, 117, 79, 0.18) 0%, transparent 70%);
      /* [zh] 样式规则 `pointer-events: none;` */
      pointer-events: none;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.left-side-content {` */
    .left-side-content {
      /* [zh] 样式规则 `position: relative;` */
      position: relative;
      /* [zh] 样式规则 `z-index: 1;` */
      z-index: 1;
      /* [zh] 样式规则 `padding-bottom: 20px;` */
      padding-bottom: 20px;

      /* [zh] 样式规则 `.logo {` */
      .logo {
        /* [zh] 样式规则 `padding: 22px 18px 18px;` */
        padding: 22px 18px 18px;
        /* [zh] 样式规则 `border-bottom: 1px solid rgba(255, 255, ` */
        border-bottom: 1px solid rgba(255, 255, 255, 0.08);
        /* [zh] 样式规则 `margin-bottom: 10px;` */
        margin-bottom: 10px;

        /* [zh] 样式规则 `.logo-row {` */
        .logo-row {
          /* [zh] 样式规则 `display: flex;` */
          display: flex;
          /* [zh] 样式规则 `align-items: center;` */
          align-items: center;
          /* [zh] 样式规则 `gap: 12px;` */
          gap: 12px;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.logo-mark {` */
        .logo-mark {
          /* [zh] 样式规则 `width: 36px;` */
          width: 36px;
          /* [zh] 样式规则 `height: 40px;` */
          height: 40px;
          /* [zh] 样式规则 `flex-shrink: 0;` */
          flex-shrink: 0;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.logo-copy {` */
        .logo-copy {
          /* [zh] 样式规则 `display: flex;` */
          display: flex;
          /* [zh] 样式规则 `flex-direction: column;` */
          flex-direction: column;
          /* [zh] 样式规则 `gap: 2px;` */
          gap: 2px;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.logo-text {` */
        .logo-text {
          /* [zh] 样式规则 `font-size: 17px;` */
          font-size: 17px;
          /* [zh] 样式规则 `font-weight: 600;` */
          font-weight: 600;
          /* [zh] 样式规则 `letter-spacing: 0.06em;` */
          letter-spacing: 0.06em;
          /* [zh] 样式规则 `color: var(--sidebar-text-active);` */
          color: var(--sidebar-text-active);
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.logo-sub {` */
        .logo-sub {
          /* [zh] 样式规则 `font-size: 11px;` */
          font-size: 11px;
          /* [zh] 样式规则 `font-weight: 500;` */
          font-weight: 500;
          /* [zh] 样式规则 `color: var(--sidebar-text-muted);` */
          color: var(--sidebar-text-muted);
          /* [zh] 样式规则 `letter-spacing: 0.04em;` */
          letter-spacing: 0.04em;
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.menu-item {` */
      .menu-item {
        /* [zh] 样式规则 `display: flex;` */
        display: flex;
        /* [zh] 样式规则 `align-items: center;` */
        align-items: center;
        /* [zh] 样式规则 `height: 44px;` */
        height: 44px;
        /* [zh] 样式规则 `margin: 3px 12px;` */
        margin: 3px 12px;
        /* [zh] 样式规则 `padding: 0 12px;` */
        padding: 0 12px;
        /* [zh] 样式规则 `border-radius: 12px;` */
        border-radius: 12px;
        /* [zh] 样式规则 `font-size: 14px;` */
        font-size: 14px;
        /* [zh] 样式规则 `color: var(--sidebar-text);` */
        color: var(--sidebar-text);
        /* [zh] 样式规则 `cursor: pointer;` */
        cursor: pointer;
        /* [zh] 样式规则 `transition: background 0.2s, color 0.2s;` */
        transition: background 0.2s, color 0.2s;

        /* [zh] 样式规则 `&:hover {` */
        &:hover {
          /* [zh] 样式规则 `background: var(--sidebar-hover);` */
          background: var(--sidebar-hover);
          /* [zh] 样式规则 `color: var(--sidebar-text-active);` */
          color: var(--sidebar-text-active);
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `&.active {` */
        &.active {
          /* [zh] 样式规则 `background: var(--sidebar-active-bg);` */
          background: var(--sidebar-active-bg);
          /* [zh] 样式规则 `color: var(--sidebar-text-active);` */
          color: var(--sidebar-text-active);
          /* [zh] 样式规则 `font-weight: 600;` */
          font-weight: 600;
          /* [zh] 样式规则 `box-shadow: inset 3px 0 0 var(--accent);` */
          box-shadow: inset 3px 0 0 var(--accent);
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.menu-icon {` */
        .menu-icon {
          /* [zh] 样式规则 `font-size: 16px;` */
          font-size: 16px;
          /* [zh] 样式规则 `opacity: 0.9;` */
          opacity: 0.9;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.menu-name {` */
        .menu-name {
          /* [zh] 样式规则 `flex: 1;` */
          flex: 1;
          /* [zh] 样式规则 `margin-left: 10px;` */
          margin-left: 10px;
          /* [zh] 样式规则 `min-width: 0;` */
          min-width: 0;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.icon-right {` */
        .icon-right {
          /* [zh] 样式规则 `font-size: 11px;` */
          font-size: 11px;
          /* [zh] 样式规则 `opacity: 0.65;` */
          opacity: 0.65;
          /* [zh] 样式规则 `transition: transform 0.25s ease;` */
          transition: transform 0.25s ease;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.icon-right-opened {` */
        .icon-right-opened {
          /* [zh] 样式规则 `transform: rotate(180deg);` */
          transform: rotate(180deg);
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.submenu-item {` */
      .submenu-item {
        /* [zh] 样式规则 `display: flex;` */
        display: flex;
        /* [zh] 样式规则 `align-items: center;` */
        align-items: center;
        /* [zh] 样式规则 `gap: 8px;` */
        gap: 8px;
        /* [zh] 样式规则 `height: 38px;` */
        height: 38px;
        /* [zh] 样式规则 `margin: 2px 14px 2px 22px;` */
        margin: 2px 14px 2px 22px;
        /* [zh] 样式规则 `padding: 0 12px;` */
        padding: 0 12px;
        /* [zh] 样式规则 `border-radius: 10px;` */
        border-radius: 10px;
        /* [zh] 样式规则 `font-size: 13px;` */
        font-size: 13px;
        /* [zh] 样式规则 `color: var(--sidebar-text-muted);` */
        color: var(--sidebar-text-muted);
        /* [zh] 样式规则 `cursor: pointer;` */
        cursor: pointer;
        /* [zh] 样式规则 `transition: background 0.2s, color 0.2s;` */
        transition: background 0.2s, color 0.2s;

        /* [zh] 样式规则 `.submenu-dot {` */
        .submenu-dot {
          /* [zh] 样式规则 `width: 5px;` */
          width: 5px;
          /* [zh] 样式规则 `height: 5px;` */
          height: 5px;
          /* [zh] 样式规则 `border-radius: 50%;` */
          border-radius: 50%;
          /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.25);` */
          background: rgba(255, 255, 255, 0.25);
          /* [zh] 样式规则 `flex-shrink: 0;` */
          flex-shrink: 0;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `&:hover {` */
        &:hover {
          /* [zh] 样式规则 `background: var(--sidebar-hover);` */
          background: var(--sidebar-hover);
          /* [zh] 样式规则 `color: var(--sidebar-text);` */
          color: var(--sidebar-text);
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `&.active {` */
        &.active {
          /* [zh] 样式规则 `background: var(--sidebar-active-bg);` */
          background: var(--sidebar-active-bg);
          /* [zh] 样式规则 `color: var(--sidebar-text-active);` */
          color: var(--sidebar-text-active);
          /* [zh] 样式规则 `font-weight: 500;` */
          font-weight: 500;

          /* [zh] 样式规则 `.submenu-dot {` */
          .submenu-dot {
            /* [zh] 样式规则 `background: var(--accent);` */
            background: var(--accent);
          /* [zh] 样式规则 `}` */
          }
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.submenu-container {` */
      .submenu-container {
        /* [zh] 样式规则 `transition: max-height 0.28s ease, opaci` */
        transition: max-height 0.28s ease, opacity 0.22s ease;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.submenu-opened {` */
      .submenu-opened {
        /* [zh] 样式规则 `max-height: 520px;` */
        max-height: 520px;
        /* [zh] 样式规则 `opacity: 1;` */
        opacity: 1;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.submenu-closed {` */
      .submenu-closed {
        /* [zh] 样式规则 `max-height: 0;` */
        max-height: 0;
        /* [zh] 样式规则 `opacity: 0;` */
        opacity: 0;
        /* [zh] 样式规则 `overflow: hidden;` */
        overflow: hidden;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.right {` */
  .right {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `width: 0;` */
    width: 0;
    /* [zh] 样式规则 `min-height: 100vh;` */
    min-height: 100vh;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;

    /* [zh] 样式规则 `.top {` */
    .top {
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
      /* [zh] 样式规则 `justify-content: space-between;` */
      justify-content: space-between;
      /* [zh] 样式规则 `gap: 16px;` */
      gap: 16px;
      /* [zh] 样式规则 `min-height: 64px;` */
      min-height: 64px;
      /* [zh] 样式规则 `padding: 12px 24px;` */
      padding: 12px 24px;
      /* [zh] 样式规则 `background: var(--header-bg);` */
      background: var(--header-bg);
      /* [zh] 样式规则 `-webkit-backdrop-filter: blur(16px) satu` */
      -webkit-backdrop-filter: blur(16px) saturate(160%);
      /* [zh] 样式规则 `backdrop-filter: blur(16px) saturate(160` */
      backdrop-filter: blur(16px) saturate(160%);
      /* [zh] 样式规则 `border-bottom: 1px solid var(--header-bo` */
      border-bottom: 1px solid var(--header-border);
      /* [zh] 样式规则 `box-shadow: var(--shadow-sm);` */
      box-shadow: var(--shadow-sm);

      /* [zh] 样式规则 `.top-main {` */
      .top-main {
        /* [zh] 样式规则 `min-width: 0;` */
        min-width: 0;

        /* [zh] 样式规则 `.page-title {` */
        .page-title {
          /* [zh] 样式规则 `margin: 0 0 2px;` */
          margin: 0 0 2px;
          /* [zh] 样式规则 `font-size: 18px;` */
          font-size: 18px;
          /* [zh] 样式规则 `font-weight: 600;` */
          font-weight: 600;
          /* [zh] 样式规则 `color: var(--text);` */
          color: var(--text);
          /* [zh] 样式规则 `letter-spacing: 0.01em;` */
          letter-spacing: 0.01em;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.breadcrumb {` */
        .breadcrumb {
          /* [zh] 样式规则 `:deep(.el-breadcrumb) {` */
          :deep(.el-breadcrumb) {
            /* [zh] 样式规则 `line-height: 1.4;` */
            line-height: 1.4;
            /* [zh] 样式规则 `font-size: 12px;` */
            font-size: 12px;
          /* [zh] 样式规则 `}` */
          }

          /* [zh] 样式规则 `:deep(.el-breadcrumb__inner) {` */
          :deep(.el-breadcrumb__inner) {
            /* [zh] 样式规则 `color: var(--text3);` */
            color: var(--text3);
            /* [zh] 样式规则 `font-weight: 400;` */
            font-weight: 400;
          /* [zh] 样式规则 `}` */
          }

          /* [zh] 样式规则 `:deep(.el-breadcrumb__item:last-child .e` */
          :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
            /* [zh] 样式规则 `color: var(--text2);` */
            color: var(--text2);
          /* [zh] 样式规则 `}` */
          }
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.top-actions {` */
      .top-actions {
        /* [zh] 样式规则 `display: flex;` */
        display: flex;
        /* [zh] 样式规则 `align-items: center;` */
        align-items: center;
        /* [zh] 样式规则 `gap: 10px;` */
        gap: 10px;
        /* [zh] 样式规则 `flex-shrink: 0;` */
        flex-shrink: 0;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.action-pill {` */
      .action-pill {
        /* [zh] 样式规则 `display: inline-flex;` */
        display: inline-flex;
        /* [zh] 样式规则 `align-items: center;` */
        align-items: center;
        /* [zh] 样式规则 `gap: 6px;` */
        gap: 6px;
        /* [zh] 样式规则 `height: 36px;` */
        height: 36px;
        /* [zh] 样式规则 `padding: 0 14px;` */
        padding: 0 14px;
        /* [zh] 样式规则 `border: 1px solid var(--header-border);` */
        border: 1px solid var(--header-border);
        /* [zh] 样式规则 `border-radius: 999px;` */
        border-radius: 999px;
        /* [zh] 样式规则 `background: var(--surface);` */
        background: var(--surface);
        /* [zh] 样式规则 `color: var(--text2);` */
        color: var(--text2);
        /* [zh] 样式规则 `font-size: 13px;` */
        font-size: 13px;
        /* [zh] 样式规则 `font-weight: 500;` */
        font-weight: 500;
        /* [zh] 样式规则 `cursor: pointer;` */
        cursor: pointer;
        /* [zh] 样式规则 `transition: border-color 0.2s, color 0.2` */
        transition: border-color 0.2s, color 0.2s, box-shadow 0.2s;

        /* [zh] 样式规则 `.action-pill__icon {` */
        .action-pill__icon {
          /* [zh] 样式规则 `font-size: 14px;` */
          font-size: 14px;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `&:hover {` */
        &:hover {
          /* [zh] 样式规则 `color: var(--text);` */
          color: var(--text);
          /* [zh] 样式规则 `border-color: rgba(42, 36, 32, 0.14);` */
          border-color: rgba(42, 36, 32, 0.14);
          /* [zh] 样式规则 `box-shadow: var(--shadow-sm);` */
          box-shadow: var(--shadow-sm);
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `&.action-pill--ghost {` */
        &.action-pill--ghost {
          /* [zh] 样式规则 `background: transparent;` */
          background: transparent;
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.user-chip {` */
      .user-chip {
        /* [zh] 样式规则 `display: flex;` */
        display: flex;
        /* [zh] 样式规则 `align-items: center;` */
        align-items: center;
        /* [zh] 样式规则 `gap: 8px;` */
        gap: 8px;
        /* [zh] 样式规则 `height: 36px;` */
        height: 36px;
        /* [zh] 样式规则 `padding: 0 6px 0 4px;` */
        padding: 0 6px 0 4px;
        /* [zh] 样式规则 `border: 1px solid var(--header-border);` */
        border: 1px solid var(--header-border);
        /* [zh] 样式规则 `border-radius: 999px;` */
        border-radius: 999px;
        /* [zh] 样式规则 `background: var(--surface);` */
        background: var(--surface);

        /* [zh] 样式规则 `.user-avatar {` */
        .user-avatar {
          /* [zh] 样式规则 `width: 28px;` */
          width: 28px;
          /* [zh] 样式规则 `height: 28px;` */
          height: 28px;
          /* [zh] 样式规则 `border-radius: 50%;` */
          border-radius: 50%;
          /* [zh] 样式规则 `background: linear-gradient(135deg, var(` */
          background: linear-gradient(135deg, var(--accent) 0%, #a86542 100%);
          /* [zh] 样式规则 `color: #fff;` */
          color: #fff;
          /* [zh] 样式规则 `font-size: 12px;` */
          font-size: 12px;
          /* [zh] 样式规则 `font-weight: 600;` */
          font-weight: 600;
          /* [zh] 样式规则 `display: flex;` */
          display: flex;
          /* [zh] 样式规则 `align-items: center;` */
          align-items: center;
          /* [zh] 样式规则 `justify-content: center;` */
          justify-content: center;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.user-name {` */
        .user-name {
          /* [zh] 样式规则 `font-size: 13px;` */
          font-size: 13px;
          /* [zh] 样式规则 `color: var(--text2);` */
          color: var(--text2);
          /* [zh] 样式规则 `padding-right: 4px;` */
          padding-right: 4px;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.logout-btn {` */
        .logout-btn {
          /* [zh] 样式规则 `height: 28px;` */
          height: 28px;
          /* [zh] 样式规则 `padding: 0 12px;` */
          padding: 0 12px;
          /* [zh] 样式规则 `border: none;` */
          border: none;
          /* [zh] 样式规则 `border-radius: 999px;` */
          border-radius: 999px;
          /* [zh] 样式规则 `background: var(--primary-soft);` */
          background: var(--primary-soft);
          /* [zh] 样式规则 `color: var(--primary);` */
          color: var(--primary);
          /* [zh] 样式规则 `font-size: 12px;` */
          font-size: 12px;
          /* [zh] 样式规则 `font-weight: 500;` */
          font-weight: 500;
          /* [zh] 样式规则 `cursor: pointer;` */
          cursor: pointer;
          /* [zh] 样式规则 `transition: background 0.2s;` */
          transition: background 0.2s;

          /* [zh] 样式规则 `&:hover {` */
          &:hover {
            /* [zh] 样式规则 `background: var(--accent-soft);` */
            background: var(--accent-soft);
            /* [zh] 样式规则 `color: var(--accent-hover);` */
            color: var(--accent-hover);
          /* [zh] 样式规则 `}` */
          }
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.right-body {` */
    .right-body {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `margin: 16px 16px 16px 0;` */
      margin: 16px 16px 16px 0;
      /* [zh] 样式规则 `padding: 20px 22px;` */
      padding: 20px 22px;
      /* [zh] 样式规则 `border-radius: var(--card-radius);` */
      border-radius: var(--card-radius);
      /* [zh] 样式规则 `background: var(--surface);` */
      background: var(--surface);
      /* [zh] 样式规则 `border: 1px solid var(--header-border);` */
      border: 1px solid var(--header-border);
      /* [zh] 样式规则 `box-shadow: var(--shadow-card);` */
      box-shadow: var(--shadow-card);
      /* [zh] 样式规则 `overflow: auto;` */
      overflow: auto;
      /* [zh] 样式规则 `min-height: 0;` */
      min-height: 0;

      /* [zh] 样式规则 `&.is-home {` */
      &.is-home {
        /* [zh] 样式规则 `background: transparent;` */
        background: transparent;
        /* [zh] 样式规则 `border-color: transparent;` */
        border-color: transparent;
        /* [zh] 样式规则 `box-shadow: none;` */
        box-shadow: none;
        /* [zh] 样式规则 `padding: 8px 4px 20px 0;` */
        padding: 8px 4px 20px 0;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
