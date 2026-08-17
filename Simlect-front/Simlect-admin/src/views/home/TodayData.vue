<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<section>` -->
  <section class="dashboard-overview">
    <!-- [zh] 开始标签 `<header>` -->
    <header class="overview-head">
      <!-- [zh] 开始标签 `<h2>` -->
      <h2 class="overview-title">今日概览</h2>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="overview-date">{{ todayLabel }}</span>
    <!-- [zh] 闭合标签 `</header>` -->
    </header>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="metric-bento">
      <!-- [zh] 开始标签 `<article>` -->
      <article
        v-for="item in todayDataField"
        :key="item.key"
        :class="['metric-card', `metric-card--${item.key}`]"
      >
        <!-- [zh] 开始标签 `<div>` -->
        <div class="metric-card__top">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="metric-icon" :style="{ background: item.bg, color: item.color }">
            <!-- [zh] 开始标签 `<span>` -->
            <span :class="['iconfont', `icon-${item.icon}`]"></span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<span>` -->
          <span :class="['trend-pill', trendClass(item.dataValue.increase)]">
            <!-- [zh] 开始标签 `<span>` -->
            <span :class="['iconfont', changeIcon(item.dataValue.increase)]"></span>
            <!-- [zh] Mustache 插值表达式 -->
            {{ formatTrend(item.dataValue.increase) }}
          <!-- [zh] 闭合标签 `</span>` -->
          </span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="metric-label">{{ item.name }}</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="metric-value">
          <!-- [zh] Mustache 插值表达式 -->
          {{ proxy.Utils.formatNumber(item.dataValue.todayValue, item.amount) }}
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="metric-yesterday">
          <!-- [zh] 模板内容：`昨日 {{ proxy.Utils.formatNumber(item.dataValue` -->
          昨日 {{ proxy.Utils.formatNumber(item.dataValue.yesterdayValue, item.amount) }}
        </p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted, computed } from 'vue'

const { proxy } = getCurrentInstance()

const todayLabel = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getMonth() + 1}月${d.getDate()}日 · 周${week[d.getDay()]}`
})

const changeIcon = (increase) => {
  if (increase > 0) return 'icon-rise'
  if (increase < 0) return 'icon-decline'
  return 'icon-horizontal'
}

const trendClass = (increase) => {
  if (increase > 0) return 'is-up'
  if (increase < 0) return 'is-down'
  return 'is-flat'
}

const formatTrend = (increase) => {
  const n = Number(increase)
  if (Number.isNaN(n)) return '—'
  if (n === 0) return '持平'
  const text = proxy.Utils.formatNumber(Math.abs(n), false)
  return n > 0 ? `+${text}%` : `-${text}%`
}

const todayDataField = ref([
  {
    name: '今日销售额',
    icon: 'sale-amount',
    color: '#fff',
    bg: 'linear-gradient(135deg, #c9754f 0%, #a86542 100%)',
    key: 'orderAmount',
    amount: true,
    dataValue: {},
  },
  {
    name: '今日订单',
    icon: 'order-count',
    color: '#2a2420',
    bg: 'rgba(42, 36, 32, 0.08)',
    key: 'orderCount',
    dataValue: {},
  },
  {
    name: '新增用户',
    icon: 'user-add',
    color: '#0071e3',
    bg: 'rgba(0, 113, 227, 0.12)',
    key: 'userCount',
    dataValue: {},
  },
  {
    name: '今日退款',
    icon: 'refund-amount',
    color: '#ff3b30',
    bg: 'rgba(255, 59, 48, 0.12)',
    key: 'refundAmount',
    amount: true,
    dataValue: {},
  },
])

const getTodayData = async () => {
  const result = await proxy.Request({
    url: proxy.Api.getTodayData,
  })
  if (!result) return
  const todayData = new Map(result.data.map((item) => [item.type, item]))
  todayDataField.value = todayDataField.value.map((item) => ({
    ...item,
    dataValue: todayData.get(item.key) || {},
  }))
}

onMounted(() => {
  getTodayData()
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.dashboard-overview {` */
.dashboard-overview {
  /* [zh] 样式规则 `margin-bottom: 4px;` */
  margin-bottom: 4px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.overview-head {` */
.overview-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;

  /* [zh] 样式规则 `.overview-title {` */
  .overview-title {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--text);` */
    color: var(--text);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.overview-date {` */
  .overview-date {
    /* [zh] 样式规则 `padding: 4px 10px;` */
    padding: 4px 10px;
    /* [zh] 样式规则 `border-radius: 999px;` */
    border-radius: 999px;
    /* [zh] 样式规则 `background: var(--surface);` */
    background: var(--surface);
    /* [zh] 样式规则 `border: 1px solid var(--header-border);` */
    border: 1px solid var(--header-border);
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: var(--text3);` */
    color: var(--text3);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.metric-bento {` */
.metric-bento {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: minmax(0, 1.2fr) ` */
  grid-template-columns: minmax(0, 1.2fr) repeat(3, minmax(0, 1fr));
  /* [zh] 样式规则 `grid-template-areas: 'hero order user re` */
  grid-template-areas: 'hero order user refund';
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.metric-card {` */
.metric-card {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `background: var(--surface);` */
  background: var(--surface);
  /* [zh] 样式规则 `border: 1px solid var(--header-border);` */
  border: 1px solid var(--header-border);
  /* [zh] 样式规则 `box-shadow: var(--shadow-card);` */
  box-shadow: var(--shadow-card);
  /* [zh] 样式规则 `transition: box-shadow 0.2s ease;` */
  transition: box-shadow 0.2s ease;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `box-shadow: var(--shadow-float);` */
    box-shadow: var(--shadow-float);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--orderAmount {` */
  &--orderAmount {
    /* [zh] 样式规则 `grid-area: hero;` */
    grid-area: hero;
    /* [zh] 样式规则 `background: linear-gradient(145deg, #2a2` */
    background: linear-gradient(145deg, #2a2420 0%, #1f1b19 100%);
    /* [zh] 样式规则 `border-color: rgba(255, 255, 255, 0.08);` */
    border-color: rgba(255, 255, 255, 0.08);

    /* [zh] 样式声明 */
    .metric-label,
    /* [zh] 样式规则 `.metric-yesterday {` */
    .metric-yesterday {
      /* [zh] 样式规则 `color: rgba(255, 255, 255, 0.55);` */
      color: rgba(255, 255, 255, 0.55);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.metric-value {` */
    .metric-value {
      /* [zh] 样式规则 `font-size: 22px;` */
      font-size: 22px;
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.trend-pill {` */
    .trend-pill {
      /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.1);` */
      background: rgba(255, 255, 255, 0.1);
      /* [zh] 样式规则 `border-color: rgba(255, 255, 255, 0.12);` */
      border-color: rgba(255, 255, 255, 0.12);
      /* [zh] 样式规则 `color: rgba(255, 255, 255, 0.85);` */
      color: rgba(255, 255, 255, 0.85);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--orderCount {` */
  &--orderCount {
    /* [zh] 样式规则 `grid-area: order;` */
    grid-area: order;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--userCount {` */
  &--userCount {
    /* [zh] 样式规则 `grid-area: user;` */
    grid-area: user;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--refundAmount {` */
  &--refundAmount {
    /* [zh] 样式规则 `grid-area: refund;` */
    grid-area: refund;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&__top {` */
  &__top {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
    /* [zh] 样式规则 `margin-bottom: 8px;` */
    margin-bottom: 8px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.metric-icon {` */
.metric-icon {
  /* [zh] 样式规则 `width: 32px;` */
  width: 32px;
  /* [zh] 样式规则 `height: 32px;` */
  height: 32px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `border-radius: 9px;` */
  border-radius: 9px;
  /* [zh] 样式规则 `font-size: 16px;` */
  font-size: 16px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.trend-pill {` */
.trend-pill {
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 2px;` */
  gap: 2px;
  /* [zh] 样式规则 `height: 20px;` */
  height: 20px;
  /* [zh] 样式规则 `padding: 0 7px;` */
  padding: 0 7px;
  /* [zh] 样式规则 `border-radius: 999px;` */
  border-radius: 999px;
  /* [zh] 样式规则 `border: 1px solid var(--header-border);` */
  border: 1px solid var(--header-border);
  /* [zh] 样式规则 `background: var(--primary-soft);` */
  background: var(--primary-soft);
  /* [zh] 样式规则 `font-size: 10px;` */
  font-size: 10px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;

  /* [zh] 样式规则 `.iconfont {` */
  .iconfont {
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-up {` */
  &.is-up {
    /* [zh] 样式规则 `color: #c44;` */
    color: #c44;
    /* [zh] 样式规则 `background: rgba(255, 59, 48, 0.08);` */
    background: rgba(255, 59, 48, 0.08);
    /* [zh] 样式规则 `border-color: rgba(255, 59, 48, 0.15);` */
    border-color: rgba(255, 59, 48, 0.15);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-down {` */
  &.is-down {
    /* [zh] 样式规则 `color: #1c8c3c;` */
    color: #1c8c3c;
    /* [zh] 样式规则 `background: rgba(52, 199, 89, 0.1);` */
    background: rgba(52, 199, 89, 0.1);
    /* [zh] 样式规则 `border-color: rgba(52, 199, 89, 0.2);` */
    border-color: rgba(52, 199, 89, 0.2);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-flat {` */
  &.is-flat {
    /* [zh] 样式规则 `color: var(--text3);` */
    color: var(--text3);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.metric-label {` */
.metric-label {
  /* [zh] 样式规则 `margin: 0 0 4px;` */
  margin: 0 0 4px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--text3);` */
  color: var(--text3);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.metric-value {` */
.metric-value {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 20px;` */
  font-size: 20px;
  /* [zh] 样式规则 `font-weight: 700;` */
  font-weight: 700;
  /* [zh] 样式规则 `line-height: 1.2;` */
  line-height: 1.2;
  /* [zh] 样式规则 `color: var(--text);` */
  color: var(--text);
  /* [zh] 样式规则 `letter-spacing: -0.02em;` */
  letter-spacing: -0.02em;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.metric-yesterday {` */
.metric-yesterday {
  /* [zh] 样式规则 `margin: 6px 0 0;` */
  margin: 6px 0 0;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `color: var(--text3);` */
  color: var(--text3);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (max-width: 1100px) {` */
@media (max-width: 1100px) {
  /* [zh] 样式规则 `.metric-bento {` */
  .metric-bento {
    /* [zh] 样式规则 `grid-template-columns: repeat(2, minmax(` */
    grid-template-columns: repeat(2, minmax(0, 1fr));
    /* [zh] 样式规则 `grid-template-areas:` */
    grid-template-areas:
      /* [zh] 样式声明 */
      'hero hero'
      /* [zh] 样式声明 */
      'order user'
      /* [zh] 样式声明 */
      'refund refund';
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (max-width: 640px) {` */
@media (max-width: 640px) {
  /* [zh] 样式规则 `.metric-bento {` */
  .metric-bento {
    /* [zh] 样式规则 `grid-template-columns: 1fr;` */
    grid-template-columns: 1fr;
    /* [zh] 样式规则 `grid-template-areas:` */
    grid-template-areas:
      /* [zh] 样式声明 */
      'hero'
      /* [zh] 样式声明 */
      'order'
      /* [zh] 样式声明 */
      'user'
      /* [zh] 样式声明 */
      'refund';
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
