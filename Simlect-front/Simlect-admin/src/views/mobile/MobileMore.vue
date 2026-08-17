<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-more">
    <!-- [zh] 开始标签 `<section>` -->
    <section v-for="group in groups" :key="group.title" class="glass-card m-group">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="m-group-title">{{ group.title }}</h3>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-grid">
        <!-- [zh] 开始标签 `<button>` -->
        <button
          v-for="item in group.items"
          :key="item.label"
          type="button"
          class="m-entry"
          @click="go(item)"
        >
          <!-- [zh] 开始标签 `<span>` -->
          <span class="iconfont entry-icon" :class="`icon-${item.icon}`"></span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="entry-label">{{ item.label }}</span>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="glass-card m-account">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="acc-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="iconfont icon-account acc-icon"></span>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="acc-info">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="acc-name">管理员</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="acc-sub">简选运营后台 · 移动端</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="acc-logout" @click="logout">退出登录</button>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup>
import { getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()

const groups = [
  {
    title: '订单与评价',
    items: [
      { label: '评价管理', icon: 'commend', path: '/m/order/comment' },
      { label: '举报管理', icon: 'commend', path: '/m/order/report' },
      { label: '图片违规复核', icon: 'search', path: '/m/more/imageModeration' },
      { label: '客服记录', icon: 'robot', path: '/m/more/agent' }
    ]
  },
  {
    title: '营销',
    items: [
      { label: '优惠券', icon: 'cart', path: '/m/more/coupon' },
      { label: '热搜词', icon: 'search', path: '/m/more/searchHot' },
      { label: '签到发券', icon: 'commend', path: '/m/more/signReward' },
      { label: '升级礼券', icon: 'account', path: '/m/more/memberLevelReward' }
    ]
  },
    {
      title: '数据',
      items: [
        { label: '统计明细', icon: 'order-count', path: '/m/more/statistics' },
        { label: 'MQ补偿日志', icon: 'setting', path: '/m/more/mqLog' },
        { label: '收货地址', icon: 'folder', path: '/m/more/address' }
      ]
    },
  {
    title: '系统设置',
    items: [
      { label: '发货信息', icon: 'setting', path: '/m/more/logistics' },
      { label: '提示词', icon: 'edit', path: '/m/more/prompt' },
      { label: 'RAG 知识库', icon: 'folder', path: '/m/more/rag' },
      { label: '敏感词', icon: 'search', path: '/m/more/sensitiveWord' },
      { label: '分类管理', icon: 'product', path: '/m/more/category' },
      { label: '商品属性', icon: 'stock', path: '/m/more/productProperty' },
      { label: '运营工具', icon: 'setting', path: '/m/more/tools' }
    ]
  }
]

const go = (item) => {
  if (item.path) {
    router.push(item.path)
  }
}

const logout = () => {
  proxy.Confirm({
    message: '确定要退出登录吗?',
    okfun: async () => {
      await proxy.Request({ url: proxy.Api.logout })
      router.push('/login')
    }
  })
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.m-more {` */
.m-more {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-group {` */
.m-group {
  /* [zh] 样式规则 `padding: 14px;` */
  padding: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-group-title {` */
.m-group-title {
  /* [zh] 样式规则 `margin: 0 0 12px;` */
  margin: 0 0 12px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: var(--m-ink);` */
  color: var(--m-ink);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-grid {` */
.m-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(4, 1fr);` */
  grid-template-columns: repeat(4, 1fr);
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-entry {` */
.m-entry {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `padding: 10px 4px;` */
  padding: 10px 4px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 14px;` */
  border-radius: 14px;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.45);` */
  background: rgba(255, 255, 255, 0.45);
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: transform 0.15s, background ` */
  transition: transform 0.15s, background 0.2s;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.93);` */
    transform: scale(0.93);
    /* [zh] 样式规则 `background: var(--m-gold-soft);` */
    background: var(--m-gold-soft);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.entry-icon {` */
  .entry-icon {
    /* [zh] 样式规则 `font-size: 22px;` */
    font-size: 22px;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.entry-label {` */
  .entry-label {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-account {` */
.m-account {
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;

  /* [zh] 样式规则 `.acc-row {` */
  .acc-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
    /* [zh] 样式规则 `margin-bottom: 14px;` */
    margin-bottom: 14px;

    /* [zh] 样式规则 `.acc-icon {` */
    .acc-icon {
      /* [zh] 样式规则 `width: 46px;` */
      width: 46px;
      /* [zh] 样式规则 `height: 46px;` */
      height: 46px;
      /* [zh] 样式规则 `display: grid;` */
      display: grid;
      /* [zh] 样式规则 `place-items: center;` */
      place-items: center;
      /* [zh] 样式规则 `border-radius: 14px;` */
      border-radius: 14px;
      /* [zh] 样式规则 `background: var(--m-gold-soft);` */
      background: var(--m-gold-soft);
      /* [zh] 样式规则 `color: var(--m-ink);` */
      color: var(--m-ink);
      /* [zh] 样式规则 `font-size: 24px;` */
      font-size: 24px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.acc-info {` */
    .acc-info {
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `flex-direction: column;` */
      flex-direction: column;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.acc-name {` */
    .acc-name {
      /* [zh] 样式规则 `font-size: 15px;` */
      font-size: 15px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `color: var(--m-ink);` */
      color: var(--m-ink);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.acc-sub {` */
    .acc-sub {
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.acc-logout {` */
  .acc-logout {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 42px;` */
    height: 42px;
    /* [zh] 样式规则 `border-radius: 13px;` */
    border-radius: 13px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `transition: transform 0.15s;` */
    transition: transform 0.15s;

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `transform: scale(0.98);` */
      transform: scale(0.98);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.acc-logout {` */
  .acc-logout {
    /* [zh] 样式规则 `border: 1px solid rgba(255, 59, 48, 0.28` */
    border: 1px solid rgba(255, 59, 48, 0.28);
    /* [zh] 样式规则 `background: rgba(255, 59, 48, 0.08);` */
    background: rgba(255, 59, 48, 0.08);
    /* [zh] 样式规则 `color: var(--m-danger);` */
    color: var(--m-danger);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
