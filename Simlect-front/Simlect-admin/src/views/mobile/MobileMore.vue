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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { getCurrentInstance } from 'vue'
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()

// [zh] 声明变量 `groups`
const groups = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`title: '订单与评价',`
    title: '订单与评价',
    // [zh] TypeScript/JS 语句：`items: [`
    items: [
      // [zh] TypeScript/JS 语句：`{ label: '评价管理', icon: 'commend', path: '/m/order/comme`
      { label: '评价管理', icon: 'commend', path: '/m/order/comment' },
      // [zh] TypeScript/JS 语句：`{ label: '举报管理', icon: 'commend', path: '/m/order/repor`
      { label: '举报管理', icon: 'commend', path: '/m/order/report' },
      // [zh] TypeScript/JS 语句：`{ label: '图片违规复核', icon: 'search', path: '/m/more/image`
      { label: '图片违规复核', icon: 'search', path: '/m/more/imageModeration' },
      // [zh] TypeScript/JS 语句：`{ label: '客服记录', icon: 'robot', path: '/m/more/agent' }`
      { label: '客服记录', icon: 'robot', path: '/m/more/agent' }
    // [zh] TypeScript/JS 语句：`]`
    ]
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`title: '营销',`
    title: '营销',
    // [zh] TypeScript/JS 语句：`items: [`
    items: [
      // [zh] TypeScript/JS 语句：`{ label: '优惠券', icon: 'cart', path: '/m/more/coupon' },`
      { label: '优惠券', icon: 'cart', path: '/m/more/coupon' },
      // [zh] TypeScript/JS 语句：`{ label: '热搜词', icon: 'search', path: '/m/more/searchHo`
      { label: '热搜词', icon: 'search', path: '/m/more/searchHot' },
      // [zh] TypeScript/JS 语句：`{ label: '签到发券', icon: 'commend', path: '/m/more/signRe`
      { label: '签到发券', icon: 'commend', path: '/m/more/signReward' },
      // [zh] TypeScript/JS 语句：`{ label: '升级礼券', icon: 'account', path: '/m/more/member`
      { label: '升级礼券', icon: 'account', path: '/m/more/memberLevelReward' }
    // [zh] TypeScript/JS 语句：`]`
    ]
  // [zh] TypeScript/JS 语句：`},`
  },
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`title: '数据',`
      title: '数据',
      // [zh] TypeScript/JS 语句：`items: [`
      items: [
        // [zh] TypeScript/JS 语句：`{ label: '统计明细', icon: 'order-count', path: '/m/more/st`
        { label: '统计明细', icon: 'order-count', path: '/m/more/statistics' },
        // [zh] TypeScript/JS 语句：`{ label: 'MQ补偿日志', icon: 'setting', path: '/m/more/mqLo`
        { label: 'MQ补偿日志', icon: 'setting', path: '/m/more/mqLog' },
        // [zh] TypeScript/JS 语句：`{ label: '收货地址', icon: 'folder', path: '/m/more/address`
        { label: '收货地址', icon: 'folder', path: '/m/more/address' }
      // [zh] TypeScript/JS 语句：`]`
      ]
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`title: '系统设置',`
    title: '系统设置',
    // [zh] TypeScript/JS 语句：`items: [`
    items: [
      // [zh] TypeScript/JS 语句：`{ label: '发货信息', icon: 'setting', path: '/m/more/logist`
      { label: '发货信息', icon: 'setting', path: '/m/more/logistics' },
      // [zh] TypeScript/JS 语句：`{ label: '提示词', icon: 'edit', path: '/m/more/prompt' },`
      { label: '提示词', icon: 'edit', path: '/m/more/prompt' },
      // [zh] TypeScript/JS 语句：`{ label: 'RAG 知识库', icon: 'folder', path: '/m/more/rag'`
      { label: 'RAG 知识库', icon: 'folder', path: '/m/more/rag' },
      // [zh] TypeScript/JS 语句：`{ label: '敏感词', icon: 'search', path: '/m/more/sensitiv`
      { label: '敏感词', icon: 'search', path: '/m/more/sensitiveWord' },
      // [zh] TypeScript/JS 语句：`{ label: '分类管理', icon: 'product', path: '/m/more/catego`
      { label: '分类管理', icon: 'product', path: '/m/more/category' },
      // [zh] TypeScript/JS 语句：`{ label: '商品属性', icon: 'stock', path: '/m/more/productP`
      { label: '商品属性', icon: 'stock', path: '/m/more/productProperty' },
      // [zh] TypeScript/JS 语句：`{ label: '运营工具', icon: 'setting', path: '/m/more/tools'`
      { label: '运营工具', icon: 'setting', path: '/m/more/tools' }
    // [zh] TypeScript/JS 语句：`]`
    ]
  // [zh] 语法符号
  }
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `go`
const go = (item) => {
  // [zh] 条件判断 if
  if (item.path) {
    // [zh] TypeScript/JS 语句：`router.push(item.path)`
    router.push(item.path)
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `logout`
const logout = () => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: '确定要退出登录吗?',`
    message: '确定要退出登录吗?',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await proxy.Request({ url: proxy.Api.logout })
      // [zh] TypeScript/JS 语句：`router.push('/login')`
      router.push('/login')
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
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
