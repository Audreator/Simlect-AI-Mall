<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<section>` -->
  <section class="trend-section">
    <!-- [zh] 开始标签 `<header>` -->
    <header class="section-head">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="section-title">经营趋势</h3>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="chart-tabs">
        <!-- [zh] 开始标签 `<button>` -->
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          :class="['chart-tab', { 'is-active': activeTab === tab.key }]"
          @click="switchTab(tab.key)"
        >
          <!-- [zh] Mustache 插值表达式 -->
          {{ tab.label }}
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</header>` -->
    </header>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="chart-main card">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="stat-row">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="stat-chip" v-for="item in asideStats" :key="item.key">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="stat-chip__label">{{ item.label }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="stat-chip__value">{{ item.value }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="stat-chip__sub">{{ item.sub }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="chart-canvas" ref="chartRef"></div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</section>` -->
  </section>
</template>

<script setup>
import { ref, getCurrentInstance, nextTick, shallowRef, onMounted, onUnmounted, computed } from 'vue'
import * as echarts from 'echarts'

const { proxy } = getCurrentInstance()

const tabs = [
  { key: 'sale', label: '销售' },
  { key: 'refund', label: '退款' },
]

const activeTab = ref('sale')
const chartRef = ref(null)
const chartInstance = shallowRef()

const saleData = ref({ date: [], orderAmount: [], orderCount: [] })
const refundData = ref({ date: [], orderAmount: [], orderCount: [] })

const sum = (arr) => (arr || []).reduce((a, b) => a + Number(b || 0), 0)
const fmt = (n, amount) => proxy.Utils.formatNumber(n, amount)

const asideStats = computed(() => {
  const isSale = activeTab.value === 'sale'
  const data = isSale ? saleData.value : refundData.value
  const amountTotal = sum(data.orderAmount)
  const countTotal = sum(data.orderCount)
  const prefix = isSale ? '销售' : '退款'
  return [
    {
      key: 'amount',
      label: `7日${prefix}额`,
      value: fmt(amountTotal, true),
      sub: `日均 ${fmt(Math.round(amountTotal / 7), true)}`,
    },
    {
      key: 'count',
      label: `7日${prefix}单`,
      value: fmt(countTotal, false),
      sub: `日均 ${fmt(Math.round(countTotal / 7), false)}`,
    },
  ]
})

const getChartOption = () => {
  const isSale = activeTab.value === 'sale'
  const data = isSale ? saleData.value : refundData.value
  const accent = isSale ? '#c9754f' : '#ff6b5a'
  const accentSoft = isSale ? 'rgba(201, 117, 79, 0.14)' : 'rgba(255, 107, 90, 0.12)'

  return {
    animationDuration: 600,
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(42, 36, 32, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 },
      formatter(params) {
        const date = params[0]?.axisValue || ''
        const amount = params.find((p) => p.seriesName === '金额')?.value ?? 0
        const count = params.find((p) => p.seriesName === '数量')?.value ?? 0
        return `${date}<br/>金额：${amount} 元<br/>数量：${count} 单`
      },
    },
    legend: {
      data: ['金额', '数量'],
      right: 0,
      top: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#8a8278', fontSize: 11 },
    },
    grid: { left: 4, right: 4, bottom: 0, top: 28, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.date,
      axisLine: { lineStyle: { color: '#e8e2da' } },
      axisTick: { show: false },
      axisLabel: { color: '#a39a90', fontSize: 11 },
    },
    yAxis: [
      {
        type: 'value',
        splitLine: { lineStyle: { type: 'dashed', color: '#f0ebe4' } },
        axisLabel: { color: '#a39a90', fontSize: 11 },
      },
      {
        type: 'value',
        splitLine: { show: false },
        axisLabel: { color: '#a39a90', fontSize: 11 },
      },
    ],
    series: [
      {
        name: '金额',
        type: 'line',
        smooth: 0.35,
        data: data.orderAmount,
        yAxisIndex: 0,
        lineStyle: { color: accent, width: 2.5 },
        itemStyle: { color: accent },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: accentSoft },
            { offset: 1, color: 'rgba(255,255,255,0)' },
          ]),
        },
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false,
        emphasis: { focus: 'series', showSymbol: true },
      },
      {
        name: '数量',
        type: 'bar',
        data: data.orderCount,
        yAxisIndex: 1,
        barMaxWidth: 14,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(42, 36, 32, 0.55)' },
            { offset: 1, color: 'rgba(42, 36, 32, 0.12)' },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
      },
    ],
  }
}

const renderChart = () => {
  if (!chartInstance.value) return
  chartInstance.value.setOption(getChartOption(), true)
}

const switchTab = (key) => {
  activeTab.value = key
  renderChart()
}

const loadStatisticsData = async () => {
  const result = await proxy.Request({
    url: proxy.Api.loadWeeklyStatisticsData,
    params: {},
  })
  if (!result) return

  const saleAmountData = result.data.find((item) => item.dataType == 1)
  const saleCountData = result.data.find((item) => item.dataType == 2)
  const refundAmountData = result.data.find((item) => item.dataType == 3)
  const refundCountData = result.data.find((item) => item.dataType == 4)

  saleData.value = {
    date: saleAmountData.dateList,
    orderAmount: saleAmountData.dataList,
    orderCount: saleCountData.dataList,
  }
  refundData.value = {
    date: refundAmountData.dateList,
    orderAmount: refundAmountData.dataList,
    orderCount: refundCountData.dataList,
  }
  renderChart()
}

const onResize = () => chartInstance.value?.resize()

const init = async () => {
  await nextTick()
  chartInstance.value = echarts.init(chartRef.value)
  loadStatisticsData()
  window.addEventListener('resize', onResize)
}

onMounted(() => {
  init()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chartInstance.value?.dispose()
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.card {` */
.card {
  /* [zh] 样式规则 `background: var(--surface);` */
  background: var(--surface);
  /* [zh] 样式规则 `border: 1px solid var(--header-border);` */
  border: 1px solid var(--header-border);
  /* [zh] 样式规则 `box-shadow: var(--shadow-card);` */
  box-shadow: var(--shadow-card);
  /* [zh] 样式规则 `border-radius: var(--card-radius);` */
  border-radius: var(--card-radius);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.trend-section {` */
.trend-section {
  /* [zh] 样式规则 `margin-top: 10px;` */
  margin-top: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.section-head {` */
.section-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;

  /* [zh] 样式规则 `.section-title {` */
  .section-title {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--text);` */
    color: var(--text);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.chart-tabs {` */
.chart-tabs {
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `padding: 3px;` */
  padding: 3px;
  /* [zh] 样式规则 `border-radius: 999px;` */
  border-radius: 999px;
  /* [zh] 样式规则 `background: var(--primary-muted);` */
  background: var(--primary-muted);
  /* [zh] 样式规则 `border: 1px solid var(--header-border);` */
  border: 1px solid var(--header-border);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.chart-tab {` */
.chart-tab {
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `padding: 4px 12px;` */
  padding: 4px 12px;
  /* [zh] 样式规则 `border-radius: 999px;` */
  border-radius: 999px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--text3);` */
  color: var(--text3);
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: background 0.15s ease, color` */
  transition: background 0.15s ease, color 0.15s ease;

  /* [zh] 样式规则 `&.is-active {` */
  &.is-active {
    /* [zh] 样式规则 `background: var(--surface);` */
    background: var(--surface);
    /* [zh] 样式规则 `color: var(--text);` */
    color: var(--text);
    /* [zh] 样式规则 `box-shadow: var(--shadow-sm);` */
    box-shadow: var(--shadow-sm);
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.chart-main {` */
.chart-main {
  /* [zh] 样式规则 `padding: 10px 12px 6px;` */
  padding: 10px 12px 6px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stat-row {` */
.stat-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 6px;` */
  margin-bottom: 6px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stat-chip {` */
.stat-chip {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: baseline;` */
  align-items: baseline;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `padding: 6px 10px;` */
  padding: 6px 10px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `background: var(--primary-muted);` */
  background: var(--primary-muted);

  /* [zh] 样式规则 `&__label {` */
  &__label {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: var(--text3);` */
    color: var(--text3);
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&__value {` */
  &__value {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: var(--text);` */
    color: var(--text);
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&__sub {` */
  &__sub {
    /* [zh] 样式规则 `margin-left: auto;` */
    margin-left: auto;
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `color: var(--text3);` */
    color: var(--text3);
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.chart-canvas {` */
.chart-canvas {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 158px;` */
  height: 158px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (max-width: 720px) {` */
@media (max-width: 720px) {
  /* [zh] 样式规则 `.stat-row {` */
  .stat-row {
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.stat-chip__sub {` */
  .stat-chip__sub {
    /* [zh] 样式规则 `margin-left: 0;` */
    margin-left: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
