<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-home">

    <!-- [zh] 开始标签 `<section>` -->
    <section class="m-stat-grid">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="item in todayFields" :key="item.key" class="m-stat glass-card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="stat-icon iconfont" :class="`icon-${item.icon}`"></div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="stat-body">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="stat-name">{{ item.name }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="stat-value">{{ proxy.Utils.formatNumber(item.value, item.amount) }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="stat-yesterday">
            <!-- [zh] 模板内容：`昨日 {{ proxy.Utils.formatNumber(item.yesterday` -->
            昨日 {{ proxy.Utils.formatNumber(item.yesterday, item.amount) }}
            <!-- [zh] 开始标签 `<em>` -->
            <em :class="riseClass(item.increase)">{{ formatPercent(item.increase) }}</em>
          <!-- [zh] 闭合标签 `</span>` -->
          </span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="glass-card m-quick">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="m-block-title">快捷操作</h3>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="quick-grid">
        <!-- [zh] 开始标签 `<button>` -->
        <button v-for="q in quickEntries" :key="q.path" type="button" class="quick-item" @click="go(q.path)">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="iconfont quick-icon" :class="`icon-${q.icon}`"></span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="quick-label">{{ q.label }}</span>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="glass-card m-weekly">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="m-block-title">近 7 日销售</h3>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="weekly.length" class="weekly-chart">
        <!-- [zh] 开始标签 `<div>` -->
        <div v-for="(d, i) in weekly" :key="i" class="weekly-col">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="bar-track">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="bar-fill" :style="{ height: barHeight(d.amount) }"></div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="bar-amount">{{ shortAmount(d.amount) }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="bar-day">{{ d.label }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else class="m-empty-tip">暂无统计数据</p>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="glass-card m-stock">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-block-head">
        <!-- [zh] 开始标签 `<h3>` -->
        <h3 class="m-block-title">库存预警</h3>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="link-more" @click="go('/m/product')">去管理</button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="lessStock.length" class="stock-list">
        <!-- [zh] 开始标签 `<div>` -->
        <div v-for="row in lessStock" :key="row.productId + (row.propertyValueIdHash || '')" class="stock-row">
          <!-- [zh] 开始标签 `<Cover>` -->
          <Cover :source="firstImg(row.productCover)" :width="48" border-radius="10px"></Cover>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="stock-info">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="stock-name">{{ row.productName }}</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="stock-prop">{{ propText(row.propertyData) }}</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="stock-num" :class="{ danger: row.stock <= 5 }">剩 {{ row.stock }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else class="m-empty-tip">暂无库存预警</p>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, getCurrentInstance, onMounted } from 'vue'
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()

// [zh] 声明变量 `todayFields`
const todayFields = ref([
  // [zh] TypeScript/JS 语句：`{ key: 'orderAmount', name: '今日销售额', icon: 'sale-amount`
  { key: 'orderAmount', name: '今日销售额', icon: 'sale-amount', amount: true, value: 0, yesterday: 0, increase: 0 },
  // [zh] TypeScript/JS 语句：`{ key: 'orderCount', name: '今日订单', icon: 'order-count',`
  { key: 'orderCount', name: '今日订单', icon: 'order-count', amount: false, value: 0, yesterday: 0, increase: 0 },
  // [zh] TypeScript/JS 语句：`{ key: 'userCount', name: '新增用户', icon: 'user-add', amo`
  { key: 'userCount', name: '新增用户', icon: 'user-add', amount: false, value: 0, yesterday: 0, increase: 0 },
  // [zh] TypeScript/JS 语句：`{ key: 'refundAmount', name: '今日退款', icon: 'refund-amou`
  { key: 'refundAmount', name: '今日退款', icon: 'refund-amount', amount: true, value: 0, yesterday: 0, increase: 0 }
// [zh] TypeScript/JS 语句：`])`
])

// [zh] 声明变量 `weekly`
const weekly = ref([])
// [zh] 声明变量 `lessStock`
const lessStock = ref([])
// [zh] 声明变量 `maxAmount`
const maxAmount = ref(1)

// [zh] 声明变量 `quickEntries`
const quickEntries = [
  // [zh] TypeScript/JS 语句：`{ label: '发布商品', path: '/m/product/edit', icon: 'add' }`
  { label: '发布商品', path: '/m/product/edit', icon: 'add' },
  // [zh] TypeScript/JS 语句：`{ label: '待发货', path: '/m/order?status=1', icon: 'order`
  { label: '待发货', path: '/m/order?status=1', icon: 'order' },
  // [zh] TypeScript/JS 语句：`{ label: '评价回复', path: '/m/order/comment', icon: 'comme`
  { label: '评价回复', path: '/m/order/comment', icon: 'commend' },
  // [zh] TypeScript/JS 语句：`{ label: 'MQ补偿日志', path: '/m/more/mqLog', icon: 'settin`
  { label: 'MQ补偿日志', path: '/m/more/mqLog', icon: 'setting' },
  // [zh] TypeScript/JS 语句：`{ label: '客服记录', path: '/m/more/agent', icon: 'robot' }`
  { label: '客服记录', path: '/m/more/agent', icon: 'robot' },
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `riseClass`
const riseClass = (v) => (v > 0 ? 'rise' : v < 0 ? 'decline' : 'flat')
// [zh] 声明变量 `formatPercent`
const formatPercent = (v) => `${v > 0 ? '+' : ''}${(Number(v) || 0).toFixed(1)}%`
// [zh] 声明变量 `firstImg`
const firstImg = (cover) => (cover ? String(cover).split(',')[0] : '')
// [zh] 声明变量 `propText`
const propText = (arr) =>
  // [zh] TypeScript/JS 语句：`Array.isArray(arr) ? arr.map((p) => `${p.propertyName}:`
  Array.isArray(arr) ? arr.map((p) => `${p.propertyName}:${p.propertyValue}`).join(' / ') : ''

// [zh] 声明变量 `shortAmount`
const shortAmount = (v) => {
  // [zh] 声明变量 `n`
  const n = Number(v) || 0
  // [zh] 条件判断 if
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  // [zh] 条件判断 if
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  // [zh] return 返回
  return String(Math.round(n))
// [zh] 语法符号
}
// [zh] 声明变量 `barHeight`
const barHeight = (v) => {
  // [zh] 声明变量 `ratio`
  const ratio = maxAmount.value > 0 ? (Number(v) || 0) / maxAmount.value : 0
  // [zh] return 返回
  return Math.max(4, Math.round(ratio * 100)) + '%'
// [zh] 语法符号
}

// [zh] 声明变量 `getToday`
const getToday = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({ url: proxy.Api.getTodayData, showLoading: false })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] 声明变量 `map`
  const map = new Map(result.data.map((it) => [it.type, it]))
  // [zh] 代码块开始
  todayFields.value = todayFields.value.map((it) => {
    // [zh] 声明变量 `d`
    const d = map.get(it.key) || {}
    // [zh] return 返回
    return { ...it, value: d.todayValue || 0, yesterday: d.yesterdayValue || 0, increase: d.increase || 0 }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `getWeekly`
const getWeekly = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({ url: proxy.Api.loadWeeklyStatisticsData, showLoading: false })
  // [zh] 条件判断 if
  if (!result || !Array.isArray(result.data)) return
  // [zh] 声明变量 `saleAmount`
  const saleAmount = result.data.find(d => d.dataType === 1)
  // [zh] 条件判断 if
  if (!saleAmount) return
  // [zh] 声明变量 `list`
  const list = (saleAmount.dateList || []).map((date, i) => ({
    // [zh] TypeScript/JS 语句：`label: String(date).slice(5),`
    label: String(date).slice(5),
    // [zh] TypeScript/JS 语句：`amount: Number(saleAmount.dataList[i] || 0)`
    amount: Number(saleAmount.dataList[i] || 0)
  // [zh] TypeScript/JS 语句：`}))`
  }))
  // [zh] TypeScript/JS 语句：`weekly.value = list.slice(-7)`
  weekly.value = list.slice(-7)
  // [zh] TypeScript/JS 语句：`maxAmount.value = Math.max(1, ...weekly.value.map((d) =`
  maxAmount.value = Math.max(1, ...weekly.value.map((d) => d.amount))
// [zh] 语法符号
}

// [zh] 声明变量 `getLessStock`
const getLessStock = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadLessStockProduct,`
    url: proxy.Api.loadLessStockProduct,
    // [zh] TypeScript/JS 语句：`params: { pageNo: 1, pageSize: 6 },`
    params: { pageNo: 1, pageSize: 6 },
    // [zh] TypeScript/JS 语句：`showLoading: false`
    showLoading: false
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`lessStock.value = (result.data && result.data.list) || `
  lessStock.value = (result.data && result.data.list) || []
// [zh] 语法符号
}

// [zh] 声明变量 `go`
const go = (path) => router.push(path)

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`getToday()`
  getToday()
  // [zh] TypeScript/JS 语句：`getWeekly()`
  getWeekly()
  // [zh] TypeScript/JS 语句：`getLessStock()`
  getLessStock()
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.m-home {` */
.m-home {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-block-title {` */
.m-block-title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: var(--m-ink);` */
  color: var(--m-ink);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-block-head {` */
.m-block-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;

  /* [zh] 样式规则 `.link-more {` */
  .link-more {
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `color: var(--m-gold);` */
    color: var(--m-gold);
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-empty-tip {` */
.m-empty-tip {
  /* [zh] 样式规则 `margin: 8px 0 0;` */
  margin: 8px 0 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-stat-grid {` */
.m-stat-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(2, 1fr);` */
  grid-template-columns: repeat(2, 1fr);
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-stat {` */
.m-stat {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding: 14px 12px;` */
  padding: 14px 12px;

  /* [zh] 样式规则 `.stat-icon {` */
  .stat-icon {
    /* [zh] 样式规则 `width: 42px;` */
    width: 42px;
    /* [zh] 样式规则 `height: 42px;` */
    height: 42px;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `place-items: center;` */
    place-items: center;
    /* [zh] 样式规则 `border-radius: 13px;` */
    border-radius: 13px;
    /* [zh] 样式规则 `font-size: 21px;` */
    font-size: 21px;
    /* [zh] 样式规则 `background: var(--m-gold-soft);` */
    background: var(--m-gold-soft);
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);

    /* [zh] 样式规则 `&.icon-order-count {` */
    &.icon-order-count {
      /* [zh] 样式规则 `background: rgba(0, 113, 227, 0.12);` */
      background: rgba(0, 113, 227, 0.12);
      /* [zh] 样式规则 `color: var(--m-blue);` */
      color: var(--m-blue);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.icon-user-add {` */
    &.icon-user-add {
      /* [zh] 样式规则 `background: rgba(52, 199, 89, 0.14);` */
      background: rgba(52, 199, 89, 0.14);
      /* [zh] 样式规则 `color: var(--m-green);` */
      color: var(--m-green);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.icon-refund-amount {` */
    &.icon-refund-amount {
      /* [zh] 样式规则 `background: rgba(255, 59, 48, 0.12);` */
      background: rgba(255, 59, 48, 0.12);
      /* [zh] 样式规则 `color: var(--m-danger);` */
      color: var(--m-danger);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.stat-body {` */
  .stat-body {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.stat-name {` */
  .stat-name {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.stat-value {` */
  .stat-value {
    /* [zh] 样式规则 `font-size: 19px;` */
    font-size: 19px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
    /* [zh] 样式规则 `line-height: 1.25;` */
    line-height: 1.25;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.stat-yesterday {` */
  .stat-yesterday {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);

    /* [zh] 样式规则 `em {` */
    em {
      /* [zh] 样式规则 `font-style: normal;` */
      font-style: normal;
      /* [zh] 样式规则 `margin-left: 4px;` */
      margin-left: 4px;

      /* [zh] 样式规则 `&.rise {` */
      &.rise {
        /* [zh] 样式规则 `color: var(--m-danger);` */
        color: var(--m-danger);
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `&.decline {` */
      &.decline {
        /* [zh] 样式规则 `color: var(--m-green);` */
        color: var(--m-green);
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-quick {` */
.m-quick {
  /* [zh] 样式规则 `padding: 14px;` */
  padding: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.quick-grid {` */
.quick-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(4, 1fr);` */
  grid-template-columns: repeat(4, 1fr);
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-top: 12px;` */
  margin-top: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.quick-item {` */
.quick-item {
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
    /* [zh] 样式规则 `transform: scale(0.94);` */
    transform: scale(0.94);
    /* [zh] 样式规则 `background: var(--m-gold-soft);` */
    background: var(--m-gold-soft);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.quick-icon {` */
  .quick-icon {
    /* [zh] 样式规则 `font-size: 22px;` */
    font-size: 22px;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.quick-label {` */
  .quick-label {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-weekly {` */
.m-weekly {
  /* [zh] 样式规则 `padding: 14px;` */
  padding: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.weekly-chart {` */
.weekly-chart {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-end;` */
  align-items: flex-end;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `height: 140px;` */
  height: 140px;
  /* [zh] 样式规则 `margin-top: 14px;` */
  margin-top: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.weekly-col {` */
.weekly-col {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;

  /* [zh] 样式规则 `.bar-track {` */
  .bar-track {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `width: 60%;` */
    width: 60%;
    /* [zh] 样式规则 `max-width: 22px;` */
    max-width: 22px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: flex-end;` */
    align-items: flex-end;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.bar-fill {` */
  .bar-fill {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `border-radius: 8px 8px 4px 4px;` */
    border-radius: 8px 8px 4px 4px;
    /* [zh] 样式规则 `background: linear-gradient(180deg, var(` */
    background: linear-gradient(180deg, var(--m-gold) 0%, #b8923f 100%);
    /* [zh] 样式规则 `transition: height 0.4s ease;` */
    transition: height 0.4s ease;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.bar-amount {` */
  .bar-amount {
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.bar-day {` */
  .bar-day {
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-stock {` */
.m-stock {
  /* [zh] 样式规则 `padding: 14px;` */
  padding: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stock-list {` */
.stock-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stock-row {` */
.stock-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;

  /* [zh] 样式规则 `.stock-info {` */
  .stock-info {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.stock-name {` */
  .stock-name {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.stock-prop {` */
  .stock-prop {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.stock-num {` */
  .stock-num {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);

    /* [zh] 样式规则 `&.danger {` */
    &.danger {
      /* [zh] 样式规则 `color: var(--m-danger);` */
      color: var(--m-danger);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
