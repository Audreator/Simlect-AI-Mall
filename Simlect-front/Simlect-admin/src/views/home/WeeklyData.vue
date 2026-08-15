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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, getCurrentInstance, nextTick, shallowRef, onMounted, onUnmounted, computed } from 'vue'
// [zh] 导入 `echarts`
import * as echarts from 'echarts'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()

// [zh] 声明变量 `tabs`
const tabs = [
  // [zh] TypeScript/JS 语句：`{ key: 'sale', label: '销售' },`
  { key: 'sale', label: '销售' },
  // [zh] TypeScript/JS 语句：`{ key: 'refund', label: '退款' },`
  { key: 'refund', label: '退款' },
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `activeTab`
const activeTab = ref('sale')
// [zh] 声明变量 `chartRef`
const chartRef = ref(null)
// [zh] 声明变量 `chartInstance`
const chartInstance = shallowRef()

// [zh] 声明变量 `saleData`
const saleData = ref({ date: [], orderAmount: [], orderCount: [] })
// [zh] 声明变量 `refundData`
const refundData = ref({ date: [], orderAmount: [], orderCount: [] })

// [zh] 声明变量 `sum`
const sum = (arr) => (arr || []).reduce((a, b) => a + Number(b || 0), 0)
// [zh] 声明变量 `fmt`
const fmt = (n, amount) => proxy.Utils.formatNumber(n, amount)

// [zh] 声明变量 `asideStats`
const asideStats = computed(() => {
  // [zh] 声明变量 `isSale`
  const isSale = activeTab.value === 'sale'
  // [zh] 声明变量 `data`
  const data = isSale ? saleData.value : refundData.value
  // [zh] 声明变量 `amountTotal`
  const amountTotal = sum(data.orderAmount)
  // [zh] 声明变量 `countTotal`
  const countTotal = sum(data.orderCount)
  // [zh] 声明变量 `prefix`
  const prefix = isSale ? '销售' : '退款'
  // [zh] return 返回
  return [
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`key: 'amount',`
      key: 'amount',
      // [zh] TypeScript/JS 语句：`label: `7日${prefix}额`,`
      label: `7日${prefix}额`,
      // [zh] TypeScript/JS 语句：`value: fmt(amountTotal, true),`
      value: fmt(amountTotal, true),
      // [zh] TypeScript/JS 语句：`sub: `日均 ${fmt(Math.round(amountTotal / 7), true)}`,`
      sub: `日均 ${fmt(Math.round(amountTotal / 7), true)}`,
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`key: 'count',`
      key: 'count',
      // [zh] TypeScript/JS 语句：`label: `7日${prefix}单`,`
      label: `7日${prefix}单`,
      // [zh] TypeScript/JS 语句：`value: fmt(countTotal, false),`
      value: fmt(countTotal, false),
      // [zh] TypeScript/JS 语句：`sub: `日均 ${fmt(Math.round(countTotal / 7), false)}`,`
      sub: `日均 ${fmt(Math.round(countTotal / 7), false)}`,
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`]`
  ]
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `getChartOption`
const getChartOption = () => {
  // [zh] 声明变量 `isSale`
  const isSale = activeTab.value === 'sale'
  // [zh] 声明变量 `data`
  const data = isSale ? saleData.value : refundData.value
  // [zh] 声明变量 `accent`
  const accent = isSale ? '#c9754f' : '#ff6b5a'
  // [zh] 声明变量 `accentSoft`
  const accentSoft = isSale ? 'rgba(201, 117, 79, 0.14)' : 'rgba(255, 107, 90, 0.12)'

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`animationDuration: 600,`
    animationDuration: 600,
    // [zh] 代码块开始
    tooltip: {
      // [zh] TypeScript/JS 语句：`trigger: 'axis',`
      trigger: 'axis',
      // [zh] TypeScript/JS 语句：`backgroundColor: 'rgba(42, 36, 32, 0.92)',`
      backgroundColor: 'rgba(42, 36, 32, 0.92)',
      // [zh] TypeScript/JS 语句：`borderWidth: 0,`
      borderWidth: 0,
      // [zh] TypeScript/JS 语句：`textStyle: { color: '#fff', fontSize: 12 },`
      textStyle: { color: '#fff', fontSize: 12 },
      // [zh] 代码块开始
      formatter(params) {
        // [zh] 声明变量 `date`
        const date = params[0]?.axisValue || ''
        // [zh] 声明变量 `amount`
        const amount = params.find((p) => p.seriesName === '金额')?.value ?? 0
        // [zh] 声明变量 `count`
        const count = params.find((p) => p.seriesName === '数量')?.value ?? 0
        // [zh] return 返回
        return `${date}<br/>金额：${amount} 元<br/>数量：${count} 单`
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    legend: {
      // [zh] TypeScript/JS 语句：`data: ['金额', '数量'],`
      data: ['金额', '数量'],
      // [zh] TypeScript/JS 语句：`right: 0,`
      right: 0,
      // [zh] TypeScript/JS 语句：`top: 0,`
      top: 0,
      // [zh] TypeScript/JS 语句：`itemWidth: 10,`
      itemWidth: 10,
      // [zh] TypeScript/JS 语句：`itemHeight: 10,`
      itemHeight: 10,
      // [zh] TypeScript/JS 语句：`textStyle: { color: '#8a8278', fontSize: 11 },`
      textStyle: { color: '#8a8278', fontSize: 11 },
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`grid: { left: 4, right: 4, bottom: 0, top: 28, containL`
    grid: { left: 4, right: 4, bottom: 0, top: 28, containLabel: true },
    // [zh] 代码块开始
    xAxis: {
      // [zh] TypeScript/JS 语句：`type: 'category',`
      type: 'category',
      // [zh] TypeScript/JS 语句：`boundaryGap: false,`
      boundaryGap: false,
      // [zh] TypeScript/JS 语句：`data: data.date,`
      data: data.date,
      // [zh] TypeScript/JS 语句：`axisLine: { lineStyle: { color: '#e8e2da' } },`
      axisLine: { lineStyle: { color: '#e8e2da' } },
      // [zh] TypeScript/JS 语句：`axisTick: { show: false },`
      axisTick: { show: false },
      // [zh] TypeScript/JS 语句：`axisLabel: { color: '#a39a90', fontSize: 11 },`
      axisLabel: { color: '#a39a90', fontSize: 11 },
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`yAxis: [`
    yAxis: [
      // [zh] 代码块开始
      {
        // [zh] TypeScript/JS 语句：`type: 'value',`
        type: 'value',
        // [zh] TypeScript/JS 语句：`splitLine: { lineStyle: { type: 'dashed', color: '#f0eb`
        splitLine: { lineStyle: { type: 'dashed', color: '#f0ebe4' } },
        // [zh] TypeScript/JS 语句：`axisLabel: { color: '#a39a90', fontSize: 11 },`
        axisLabel: { color: '#a39a90', fontSize: 11 },
      // [zh] TypeScript/JS 语句：`},`
      },
      // [zh] 代码块开始
      {
        // [zh] TypeScript/JS 语句：`type: 'value',`
        type: 'value',
        // [zh] TypeScript/JS 语句：`splitLine: { show: false },`
        splitLine: { show: false },
        // [zh] TypeScript/JS 语句：`axisLabel: { color: '#a39a90', fontSize: 11 },`
        axisLabel: { color: '#a39a90', fontSize: 11 },
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] 语法符号
    ],
    // [zh] TypeScript/JS 语句：`series: [`
    series: [
      // [zh] 代码块开始
      {
        // [zh] TypeScript/JS 语句：`name: '金额',`
        name: '金额',
        // [zh] TypeScript/JS 语句：`type: 'line',`
        type: 'line',
        // [zh] TypeScript/JS 语句：`smooth: 0.35,`
        smooth: 0.35,
        // [zh] TypeScript/JS 语句：`data: data.orderAmount,`
        data: data.orderAmount,
        // [zh] TypeScript/JS 语句：`yAxisIndex: 0,`
        yAxisIndex: 0,
        // [zh] TypeScript/JS 语句：`lineStyle: { color: accent, width: 2.5 },`
        lineStyle: { color: accent, width: 2.5 },
        // [zh] TypeScript/JS 语句：`itemStyle: { color: accent },`
        itemStyle: { color: accent },
        // [zh] 代码块开始
        areaStyle: {
          // [zh] TypeScript/JS 语句：`color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [`
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            // [zh] TypeScript/JS 语句：`{ offset: 0, color: accentSoft },`
            { offset: 0, color: accentSoft },
            // [zh] TypeScript/JS 语句：`{ offset: 1, color: 'rgba(255,255,255,0)' },`
            { offset: 1, color: 'rgba(255,255,255,0)' },
          // [zh] TypeScript/JS 语句：`]),`
          ]),
        // [zh] TypeScript/JS 语句：`},`
        },
        // [zh] TypeScript/JS 语句：`symbol: 'circle',`
        symbol: 'circle',
        // [zh] TypeScript/JS 语句：`symbolSize: 6,`
        symbolSize: 6,
        // [zh] TypeScript/JS 语句：`showSymbol: false,`
        showSymbol: false,
        // [zh] TypeScript/JS 语句：`emphasis: { focus: 'series', showSymbol: true },`
        emphasis: { focus: 'series', showSymbol: true },
      // [zh] TypeScript/JS 语句：`},`
      },
      // [zh] 代码块开始
      {
        // [zh] TypeScript/JS 语句：`name: '数量',`
        name: '数量',
        // [zh] TypeScript/JS 语句：`type: 'bar',`
        type: 'bar',
        // [zh] TypeScript/JS 语句：`data: data.orderCount,`
        data: data.orderCount,
        // [zh] TypeScript/JS 语句：`yAxisIndex: 1,`
        yAxisIndex: 1,
        // [zh] TypeScript/JS 语句：`barMaxWidth: 14,`
        barMaxWidth: 14,
        // [zh] 代码块开始
        itemStyle: {
          // [zh] TypeScript/JS 语句：`color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [`
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            // [zh] TypeScript/JS 语句：`{ offset: 0, color: 'rgba(42, 36, 32, 0.55)' },`
            { offset: 0, color: 'rgba(42, 36, 32, 0.55)' },
            // [zh] TypeScript/JS 语句：`{ offset: 1, color: 'rgba(42, 36, 32, 0.12)' },`
            { offset: 1, color: 'rgba(42, 36, 32, 0.12)' },
          // [zh] TypeScript/JS 语句：`]),`
          ]),
          // [zh] TypeScript/JS 语句：`borderRadius: [4, 4, 0, 0],`
          borderRadius: [4, 4, 0, 0],
        // [zh] TypeScript/JS 语句：`},`
        },
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] 语法符号
    ],
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `renderChart`
const renderChart = () => {
  // [zh] 条件判断 if
  if (!chartInstance.value) return
  // [zh] TypeScript/JS 语句：`chartInstance.value.setOption(getChartOption(), true)`
  chartInstance.value.setOption(getChartOption(), true)
// [zh] 语法符号
}

// [zh] 声明变量 `switchTab`
const switchTab = (key) => {
  // [zh] TypeScript/JS 语句：`activeTab.value = key`
  activeTab.value = key
  // [zh] TypeScript/JS 语句：`renderChart()`
  renderChart()
// [zh] 语法符号
}

// [zh] 声明变量 `loadStatisticsData`
const loadStatisticsData = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadWeeklyStatisticsData,`
    url: proxy.Api.loadWeeklyStatisticsData,
    // [zh] TypeScript/JS 语句：`params: {},`
    params: {},
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return

  // [zh] 声明变量 `saleAmountData`
  const saleAmountData = result.data.find((item) => item.dataType == 1)
  // [zh] 声明变量 `saleCountData`
  const saleCountData = result.data.find((item) => item.dataType == 2)
  // [zh] 声明变量 `refundAmountData`
  const refundAmountData = result.data.find((item) => item.dataType == 3)
  // [zh] 声明变量 `refundCountData`
  const refundCountData = result.data.find((item) => item.dataType == 4)

  // [zh] 代码块开始
  saleData.value = {
    // [zh] TypeScript/JS 语句：`date: saleAmountData.dateList,`
    date: saleAmountData.dateList,
    // [zh] TypeScript/JS 语句：`orderAmount: saleAmountData.dataList,`
    orderAmount: saleAmountData.dataList,
    // [zh] TypeScript/JS 语句：`orderCount: saleCountData.dataList,`
    orderCount: saleCountData.dataList,
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  refundData.value = {
    // [zh] TypeScript/JS 语句：`date: refundAmountData.dateList,`
    date: refundAmountData.dateList,
    // [zh] TypeScript/JS 语句：`orderAmount: refundAmountData.dataList,`
    orderAmount: refundAmountData.dataList,
    // [zh] TypeScript/JS 语句：`orderCount: refundCountData.dataList,`
    orderCount: refundCountData.dataList,
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`renderChart()`
  renderChart()
// [zh] 语法符号
}

// [zh] 声明变量 `onResize`
const onResize = () => chartInstance.value?.resize()

// [zh] 声明变量 `init`
const init = async () => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextTick()
  // [zh] TypeScript/JS 语句：`chartInstance.value = echarts.init(chartRef.value)`
  chartInstance.value = echarts.init(chartRef.value)
  // [zh] TypeScript/JS 语句：`loadStatisticsData()`
  loadStatisticsData()
  // [zh] TypeScript/JS 语句：`window.addEventListener('resize', onResize)`
  window.addEventListener('resize', onResize)
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`init()`
  init()
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] TypeScript/JS 语句：`window.removeEventListener('resize', onResize)`
  window.removeEventListener('resize', onResize)
  // [zh] TypeScript/JS 语句：`chartInstance.value?.dispose()`
  chartInstance.value?.dispose()
// [zh] TypeScript/JS 语句：`})`
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
