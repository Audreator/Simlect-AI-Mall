<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-order">

    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-status-tabs">
      <!-- [zh] 开始标签 `<button>` -->
      <button
        type="button"
        class="status-chip"
        :class="{ active: searchForm.orderStatus === '' }"
        @click="pickStatus('')"
      >
        <!-- [zh] 模板内容：`全部` -->
        全部
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-for="st in orderStatusList"
        :key="st.status"
        type="button"
        class="status-chip"
        :class="{ active: searchForm.orderStatus === st.status }"
        @click="pickStatus(st.status)"
      >
        <!-- [zh] Mustache 插值表达式 -->
        {{ st.desc }}
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-search glass-card glass-strong">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="iconfont icon-search search-icon"></span>
      <!-- [zh] 开始标签 `<input>` -->
      <input
        v-model="searchForm.productNameFuzzy"
        class="search-input"
        type="search"
        placeholder="搜索商品名称"
        @keyup.enter="reload"
      />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="order in list" :key="order.orderId" class="m-order-card glass-card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="order-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="order-no">#{{ order.orderId }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="order-status">{{ order.orderStatusName }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="order-meta">
          <!-- [zh] 开始标签 `<span>` -->
          <span>{{ order.orderTime }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span>买家：{{ order.nickName }} (ID:{{ order.userId }})</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="order-goods">
          <!-- [zh] 开始标签 `<div>` -->
          <div v-for="(sub, i) in order.orderItemList" :key="i" class="goods-row">
            <!-- [zh] 开始标签 `<CouponOrderCover>` -->
            <CouponOrderCover v-if="isCouponItem(order, sub)" :width="48" border-radius="10px" />
            <!-- [zh] 开始标签 `<Cover>` -->
            <Cover v-else :source="sub.cover" :width="48" border-radius="10px"></Cover>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="goods-info">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="goods-name">{{ sub.productName }}</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span class="goods-prop">{{ sub.propertyInfo }}</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span class="goods-remark">买家备注：{{ sub.remark?.trim() || '暂无' }}</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="goods-amount">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="amt">¥{{ amount(sub.itemAmount) }}</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span class="cnt">x{{ sub.buyCount }}</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="order-foot">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="pay-summary">
            <!-- [zh] 开始标签 `<p>` -->
            <p v-if="hasCoupon(order)" class="origin-line">商品总价 ¥{{ amount(order.originalAmount) }}</p>
            <!-- [zh] 开始标签 `<p>` -->
            <p v-if="hasCoupon(order)" class="coupon-line">
              <!-- [zh] 模板内容：`优惠券：{{ couponText(order) }} · -¥{{ amount(ord` -->
              优惠券：{{ couponText(order) }} · -¥{{ amount(order.couponDiscountAmount) }}
            <!-- [zh] 闭合标签 `</p>` -->
            </p>
            <!-- [zh] 开始标签 `<p>` -->
            <p class="pay-amount">实付 <strong>¥{{ amount(order.amount) }}</strong></p>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="order-ops">
            <!-- [zh] 开始标签 `<button>` -->
            <button v-if="order.orderStatus == 1" type="button" class="op-btn primary" @click="deliver(order)">
              <!-- [zh] 模板内容：`发货` -->
              发货
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
            <!-- [zh] 开始标签 `<button>` -->
            <button v-if="order.logisticsNo" type="button" class="op-btn" @click="viewLogistics(order)">
              <!-- [zh] 模板内容：`物流` -->
              物流
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
            <!-- [zh] 开始标签 `<button>` -->
            <button v-if="order.commentStatus != 0" type="button" class="op-btn" @click="reply(order)">
              <!-- [zh] 模板内容：`回复` -->
              回复
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="!loading" class="m-empty-tip">暂无订单</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="sentinel" class="m-sentinel">
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="loading">加载中…</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-else-if="finished && list.length">没有更多了</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<Delivery>` -->
    <Delivery ref="deliveryRef" @reload="reload"></Delivery>
    <!-- [zh] 开始标签 `<CommentReply>` -->
    <CommentReply ref="commentRef" @reload="reload"></CommentReply>

    <!-- [zh] 开始标签 `<Dialog>` -->
    <Dialog :show="logisticsShow" title="物流信息" width="92%" :showCancel="false" :buttons="[]" @close="logisticsShow = false">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="logisticsInfo" class="logistics-box">
        <!-- [zh] 开始标签 `<p>` -->
        <p><b>{{ logisticsInfo.logisticsCompany }}</b> · {{ logisticsInfo.logisticsNo }}</p>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="logisticsTraces.length" class="trace-list">
          <!-- [zh] 开始标签 `<div>` -->
          <div v-for="(t, i) in logisticsTraces" :key="i" class="trace-item">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="trace-time">{{ t.time || t.AcceptTime }}</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="trace-text">{{ t.context || t.AcceptStation }}</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-else class="m-empty-tip">暂无物流轨迹</p>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</Dialog>` -->
    </Dialog>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `@/views/order/Delivery.vue`
import Delivery from '@/views/order/Delivery.vue'
// [zh] 导入 `@/views/order/CommentReply.vue`
import CommentReply from '@/views/order/CommentReply.vue'
// [zh] 导入 `@/utils/order.js`
import { isCouponOrder, isCouponOrderItem } from '@/utils/order.js'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from 'vue'
// [zh] 导入 `vue-router`
import { useRoute } from 'vue-router'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `route`
const route = useRoute()

// [zh] 声明变量 `orderStatusList`
const orderStatusList = ref([])
// [zh] 声明变量 `searchForm`
const searchForm = reactive({ orderStatus: '', productNameFuzzy: '' })
// [zh] 声明变量 `list`
const list = ref([])
// [zh] 声明变量 `pageNo`
const pageNo = ref(0)
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(1)
// [zh] 声明变量 `loading`
const loading = ref(false)
// [zh] 声明变量 `finished`
const finished = ref(false)
// [zh] 声明变量 `sentinel`
const sentinel = ref(null)
// [zh] 声明变量 `observer`
let observer = null

// [zh] 声明变量 `amount`
const amount = (v) => Number(v || 0).toFixed(2)

// [zh] 声明变量 `isCouponItem`
const isCouponItem = (order, item) => isCouponOrder(order) || isCouponOrderItem(item)

// [zh] 声明变量 `hasCoupon`
const hasCoupon = (order) => Number(order?.couponDiscountAmount ?? 0) > 0

// [zh] 声明变量 `couponText`
const couponText = (order) => {
  // [zh] 声明变量 `name`
  const name = order?.couponName
  // [zh] 条件判断 if
  if (!name) return '优惠券'
  // [zh] 声明变量 `typeMap`
  const typeMap = { 1: '满减券', 2: '折扣券', 3: '无门槛券' }
  // [zh] 声明变量 `type`
  const type = typeMap[order?.couponType]
  // [zh] return 返回
  return type ? `${name}（${type}）` : String(name)
// [zh] 语法符号
}

// [zh] 声明变量 `loadOrderStatus`
const loadOrderStatus = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({ url: proxy.Api.loadOrderStatus, showLoading: false })
  // [zh] 条件判断 if
  if (result) orderStatusList.value = result.data || []
// [zh] 语法符号
}

// [zh] 声明变量 `loadList`
const loadList = async (reset = false) => {
  // [zh] 条件判断 if
  if (loading.value) return
  // [zh] 条件判断 if
  if (reset) {
    // [zh] TypeScript/JS 语句：`pageNo.value = 0`
    pageNo.value = 0
    // [zh] TypeScript/JS 语句：`pageTotal.value = 1`
    pageTotal.value = 1
    // [zh] TypeScript/JS 语句：`finished.value = false`
    finished.value = false
    // [zh] TypeScript/JS 语句：`list.value = []`
    list.value = []
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (finished.value) return
  // [zh] TypeScript/JS 语句：`loading.value = true`
  loading.value = true
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `next`
    const next = pageNo.value + 1
    // [zh] 声明变量 `params`
    const params = { pageNo: next, pageSize: 8 }
    // [zh] 条件判断 if
    if (searchForm.orderStatus !== '') params.orderStatus = searchForm.orderStatus
    // [zh] 条件判断 if
    if (searchForm.productNameFuzzy) params.productNameFuzzy = searchForm.productNameFuzzy
    // [zh] 声明变量 `result`
    const result = await proxy.Request({ url: proxy.Api.loadOrder, params, showLoading: false })
    // [zh] 条件判断 if
    if (!result) return
    // [zh] 声明变量 `data`
    const data = result.data || {}
    // [zh] 声明变量 `chunk`
    const chunk = data.list || []
    // [zh] TypeScript/JS 语句：`list.value = next === 1 ? chunk : list.value.concat(chu`
    list.value = next === 1 ? chunk : list.value.concat(chunk)
    // [zh] TypeScript/JS 语句：`pageNo.value = Number(data.pageNo) || next`
    pageNo.value = Number(data.pageNo) || next
    // [zh] TypeScript/JS 语句：`pageTotal.value = Number(data.pageTotal) || pageNo.valu`
    pageTotal.value = Number(data.pageTotal) || pageNo.value
    // [zh] TypeScript/JS 语句：`finished.value = pageNo.value >= pageTotal.value`
    finished.value = pageNo.value >= pageTotal.value
  // [zh] 代码块开始
  } finally {
    // [zh] TypeScript/JS 语句：`loading.value = false`
    loading.value = false
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `reload`
const reload = () => loadList(true)
// [zh] 声明变量 `pickStatus`
const pickStatus = (status) => {
  // [zh] TypeScript/JS 语句：`searchForm.orderStatus = status`
  searchForm.orderStatus = status
  // [zh] TypeScript/JS 语句：`reload()`
  reload()
// [zh] 语法符号
}

// [zh] 声明变量 `deliveryRef`
const deliveryRef = ref()
// [zh] 声明变量 `deliver`
const deliver = (order) => deliveryRef.value.show(order.orderId)

// [zh] 声明变量 `commentRef`
const commentRef = ref()
// [zh] 声明变量 `reply`
const reply = (order) => commentRef.value.show(order.orderId)

// [zh] 声明变量 `logisticsShow`
const logisticsShow = ref(false)
// [zh] 声明变量 `logisticsInfo`
const logisticsInfo = ref(null)
// [zh] 声明变量 `logisticsTraces`
const logisticsTraces = ref([])
// [zh] 声明变量 `viewLogistics`
const viewLogistics = async (order) => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.getLogistics,`
    url: proxy.Api.getLogistics,
    // [zh] TypeScript/JS 语句：`params: { orderId: order.orderId }`
    params: { orderId: order.orderId }
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`logisticsInfo.value = result.data || {}`
  logisticsInfo.value = result.data || {}
  // [zh] 声明变量 `traces`
  const traces = result.data && (result.data.traces || result.data.list || result.data.Traces)
  // [zh] TypeScript/JS 语句：`logisticsTraces.value = Array.isArray(traces) ? traces `
  logisticsTraces.value = Array.isArray(traces) ? traces : []
  // [zh] TypeScript/JS 语句：`logisticsShow.value = true`
  logisticsShow.value = true
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`loadOrderStatus()`
  loadOrderStatus()
  // [zh] 条件判断 if
  if (route.query.status != null && route.query.status !== '') {
    // [zh] TypeScript/JS 语句：`searchForm.orderStatus = Number(route.query.status)`
    searchForm.orderStatus = Number(route.query.status)
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`loadList(true)`
  loadList(true)
  // [zh] TypeScript/JS 语句：`observer = new IntersectionObserver(`
  observer = new IntersectionObserver(
    // [zh] 代码块开始
    (entries) => {
      // [zh] 条件判断 if
      if (entries.some((e) => e.isIntersecting)) loadList()
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ rootMargin: '0px 0px 300px 0px' }`
    { rootMargin: '0px 0px 300px 0px' }
  // [zh] 语法符号
  )
  // [zh] 条件判断 if
  if (sentinel.value) observer.observe(sentinel.value)
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] TypeScript/JS 语句：`observer && observer.disconnect()`
  observer && observer.disconnect()
  // [zh] TypeScript/JS 语句：`observer = null`
  observer = null
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.m-order {` */
.m-order {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-status-tabs {` */
.m-status-tabs {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `overflow-x: auto;` */
  overflow-x: auto;
  /* [zh] 样式规则 `padding-bottom: 2px;` */
  padding-bottom: 2px;
  /* [zh] 样式规则 `scrollbar-width: none;` */
  scrollbar-width: none;

  /* [zh] 样式规则 `&::-webkit-scrollbar {` */
  &::-webkit-scrollbar {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.status-chip {` */
  .status-chip {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `padding: 7px 14px;` */
    padding: 7px 14px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `border-radius: 12px;` */
    border-radius: 12px;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.55);` */
    background: rgba(255, 255, 255, 0.55);
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `transition: background 0.2s, color 0.2s;` */
    transition: background 0.2s, color 0.2s;

    /* [zh] 样式规则 `&.active {` */
    &.active {
      /* [zh] 样式规则 `background: var(--m-ink);` */
      background: var(--m-ink);
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-search {` */
.m-search {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `height: 42px;` */
  height: 42px;
  /* [zh] 样式规则 `padding: 0 12px;` */
  padding: 0 12px;
  /* [zh] 样式规则 `border-radius: 14px;` */
  border-radius: 14px;

  /* [zh] 样式规则 `.search-icon {` */
  .search-icon {
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-input {` */
  .search-input {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
    /* [zh] 样式规则 `outline: none;` */
    outline: none;

    /* [zh] 样式规则 `&::placeholder {` */
    &::placeholder {
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-list {` */
.m-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-order-card {` */
.m-order-card {
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;

  /* [zh] 样式规则 `.order-head {` */
  .order-head {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;

    /* [zh] 样式规则 `.order-no {` */
    .order-no {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `color: var(--m-ink-2);` */
      color: var(--m-ink-2);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.order-status {` */
    .order-status {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `color: var(--m-gold);` */
      color: var(--m-gold);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.order-meta {` */
  .order-meta {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 14px;` */
    gap: 14px;
    /* [zh] 样式规则 `margin-top: 4px;` */
    margin-top: 4px;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.order-goods {` */
  .order-goods {
    /* [zh] 样式规则 `margin-top: 10px;` */
    margin-top: 10px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.goods-row {` */
  .goods-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 10px;` */
    gap: 10px;

    /* [zh] 样式规则 `.goods-info {` */
    .goods-info {
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

    /* [zh] 样式规则 `.goods-name {` */
    .goods-name {
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

    /* [zh] 样式规则 `.goods-prop {` */
    .goods-prop {
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

    /* [zh] 样式规则 `.goods-remark {` */
    .goods-remark {
      /* [zh] 样式规则 `margin-top: 4px;` */
      margin-top: 4px;
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
      /* [zh] 样式规则 `line-height: 1.4;` */
      line-height: 1.4;
      /* [zh] 样式规则 `color: #000;` */
      color: #000;
      /* [zh] 样式规则 `word-break: break-all;` */
      word-break: break-all;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.goods-amount {` */
    .goods-amount {
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
      /* [zh] 样式规则 `text-align: right;` */
      text-align: right;

      /* [zh] 样式规则 `.amt {` */
      .amt {
        /* [zh] 样式规则 `display: block;` */
        display: block;
        /* [zh] 样式规则 `font-size: 13px;` */
        font-size: 13px;
        /* [zh] 样式规则 `font-weight: 600;` */
        font-weight: 600;
        /* [zh] 样式规则 `color: var(--m-ink);` */
        color: var(--m-ink);
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.cnt {` */
      .cnt {
        /* [zh] 样式规则 `font-size: 11px;` */
        font-size: 11px;
        /* [zh] 样式规则 `color: var(--m-ink-3);` */
        color: var(--m-ink-3);
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.order-foot {` */
  .order-foot {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: flex-end;` */
    align-items: flex-end;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
    /* [zh] 样式规则 `margin-top: 12px;` */
    margin-top: 12px;
    /* [zh] 样式规则 `padding-top: 10px;` */
    padding-top: 10px;
    /* [zh] 样式规则 `border-top: 1px solid rgba(120, 120, 128` */
    border-top: 1px solid rgba(120, 120, 128, 0.16);

    /* [zh] 样式规则 `.pay-summary {` */
    .pay-summary {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `min-width: 0;` */
      min-width: 0;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `flex-direction: column;` */
      flex-direction: column;
      /* [zh] 样式规则 `gap: 2px;` */
      gap: 2px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式声明 */
    .origin-line,
    /* [zh] 样式规则 `.coupon-line {` */
    .coupon-line {
      /* [zh] 样式规则 `margin: 0;` */
      margin: 0;
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
      /* [zh] 样式规则 `word-break: break-all;` */
      word-break: break-all;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.coupon-line {` */
    .coupon-line {
      /* [zh] 样式规则 `color: #c45c26;` */
      color: #c45c26;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.pay-amount {` */
    .pay-amount {
      /* [zh] 样式规则 `margin: 2px 0 0;` */
      margin: 2px 0 0;
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
      /* [zh] 样式规则 `color: var(--m-ink-2);` */
      color: var(--m-ink-2);

      /* [zh] 样式规则 `strong {` */
      strong {
        /* [zh] 样式规则 `font-size: 16px;` */
        font-size: 16px;
        /* [zh] 样式规则 `color: var(--m-ink);` */
        color: var(--m-ink);
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.order-ops {` */
  .order-ops {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;

    /* [zh] 样式规则 `.op-btn {` */
    .op-btn {
      /* [zh] 样式规则 `height: 32px;` */
      height: 32px;
      /* [zh] 样式规则 `padding: 0 14px;` */
      padding: 0 14px;
      /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
      border: 1px solid rgba(120, 120, 128, 0.24);
      /* [zh] 样式规则 `border-radius: 10px;` */
      border-radius: 10px;
      /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.5);` */
      background: rgba(255, 255, 255, 0.5);
      /* [zh] 样式规则 `color: var(--m-ink-2);` */
      color: var(--m-ink-2);
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
      /* [zh] 样式规则 `transition: transform 0.15s;` */
      transition: transform 0.15s;

      /* [zh] 样式规则 `&:active {` */
      &:active {
        /* [zh] 样式规则 `transform: scale(0.95);` */
        transform: scale(0.95);
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `&.primary {` */
      &.primary {
        /* [zh] 样式规则 `background: var(--m-ink);` */
        background: var(--m-ink);
        /* [zh] 样式规则 `border-color: var(--m-ink);` */
        border-color: var(--m-ink);
        /* [zh] 样式规则 `color: #fff;` */
        color: #fff;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.logistics-box {` */
.logistics-box {
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: var(--m-ink);` */
  color: var(--m-ink);

  /* [zh] 样式规则 `.trace-list {` */
  .trace-list {
    /* [zh] 样式规则 `margin-top: 10px;` */
    margin-top: 10px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `gap: 10px;` */
    gap: 10px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.trace-item {` */
  .trace-item {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `gap: 2px;` */
    gap: 2px;
    /* [zh] 样式规则 `padding-left: 12px;` */
    padding-left: 12px;
    /* [zh] 样式规则 `border-left: 2px solid var(--m-gold-soft` */
    border-left: 2px solid var(--m-gold-soft);

    /* [zh] 样式规则 `.trace-time {` */
    .trace-time {
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.trace-text {` */
    .trace-text {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `color: var(--m-ink-2);` */
      color: var(--m-ink-2);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-sentinel {` */
.m-sentinel {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `min-height: 28px;` */
  min-height: 28px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-empty-tip {` */
.m-empty-tip {
  /* [zh] 样式规则 `margin: 24px 0;` */
  margin: 24px 0;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
/* [zh] 样式规则 `}` */
}
</style>
