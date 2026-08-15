<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-product">

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
      <!-- [zh] 开始标签 `<select>` -->
      <select v-model="searchForm.status" class="search-select" @change="reload">
        <!-- [zh] 开始标签 `<option>` -->
        <option value="">全部</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="1">已上架</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="0">未上架</option>
      <!-- [zh] 闭合标签 `</select>` -->
      </select>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<button>` -->
    <button type="button" class="m-add-btn" @click="goEdit()">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="iconfont icon-add"></span> 发布商品
    <!-- [zh] 闭合标签 `</button>` -->
    </button>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="row in list" :key="row.productId" class="m-prod-card glass-card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="prod-top">
          <!-- [zh] 开始标签 `<Cover>` -->
          <Cover :source="firstImg(row.cover)" :width="76" border-radius="12px" class="prod-cover"></Cover>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="prod-main">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="prod-name">{{ row.productName }}</div>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="prod-meta">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="prod-price">¥{{ priceText(row) }}</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span class="prod-stock">库存 {{ row.totalStock }}</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="prod-tags">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="m-tag" :class="statusClass(row.status)">{{ statusText(row.status) }}</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span v-if="row.commendType == 1" class="m-tag gold">已推荐</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="row.status != -1" class="prod-actions">
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="act-btn" @click="goEdit(row.productId)">编辑</button>
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="act-btn" @click="updateStock(row)">库存</button>
          <!-- [zh] 开始标签 `<button>` -->
          <button
            type="button"
            class="act-btn"
            :class="{ 'is-disabled': row.commendType == 0 && !canCommend(row) }"
            :title="row.commendType == 0 ? getCommendBlockReason(row) : ''"
            @click="commend(row)"
          >
            <!-- [zh] Mustache 插值表达式 -->
            {{ row.commendType == 1 ? '取消推荐' : '推荐' }}
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
          <!-- [zh] 开始标签 `<button>` -->
          <button
            type="button"
            class="act-btn"
            :class="{ 'is-disabled': row.status == 1 && !canDelist(row) }"
            :title="row.status == 1 ? getDelistBlockReason(row) : ''"
            @click="changeStatus(row)"
          >
            <!-- [zh] Mustache 插值表达式 -->
            {{ row.status == 0 ? '上架' : '下架' }}
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
          <!-- [zh] 开始标签 `<button>` -->
          <button
            type="button"
            class="act-btn danger"
            :class="{ 'is-disabled': !canDelete(row) }"
            :title="getDeleteBlockReason(row)"
            @click="del(row)"
          >
            <!-- [zh] 模板内容：`删除` -->
            删除
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="!loading" class="m-empty-tip">暂无商品</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="sentinel" class="m-sentinel">
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="loading">加载中…</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-else-if="finished && list.length">没有更多了</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<ProductStock>` -->
    <ProductStock ref="productStockRef"></ProductStock>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `@/views/product/edit/ProductStock.vue`
import ProductStock from '@/views/product/edit/ProductStock.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from 'vue'
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router'
// [zh] 导入 `@/eventbus/eventBus.js`
import { mitter } from '@/eventbus/eventBus.js'
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`canCommend,`
  canCommend,
  // [zh] TypeScript/JS 语句：`canDelist,`
  canDelist,
  // [zh] TypeScript/JS 语句：`canDelete,`
  canDelete,
  // [zh] TypeScript/JS 语句：`getCommendBlockReason,`
  getCommendBlockReason,
  // [zh] TypeScript/JS 语句：`getDelistBlockReason,`
  getDelistBlockReason,
  // [zh] TypeScript/JS 语句：`getDeleteBlockReason`
  getDeleteBlockReason
// [zh] TypeScript/JS 语句：`} from '@/utils/productRules.js'`
} from '@/utils/productRules.js'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()

// [zh] 声明变量 `searchForm`
const searchForm = reactive({ productNameFuzzy: '', status: '' })
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

// [zh] 声明变量 `firstImg`
const firstImg = (cover) => (cover ? String(cover).split(',')[0] : '')
// [zh] 声明变量 `priceText`
const priceText = (row) => {
  // [zh] 声明变量 `min`
  const min = Number(row.minPrice || 0).toFixed(2)
  // [zh] 声明变量 `max`
  const max = Number(row.maxPrice || 0).toFixed(2)
  // [zh] return 返回
  return min === max ? min : `${min}~${max}`
// [zh] 语法符号
}
// [zh] 声明变量 `statusText`
const statusText = (s) => (s == 1 ? '已上架' : s == -1 ? '已删除' : '未上架')
// [zh] 声明变量 `statusClass`
const statusClass = (s) => (s == 1 ? 'green' : s == -1 ? 'danger' : 'muted')
// [zh] 声明变量 `goEdit`
const goEdit = (id) => router.push(id ? `/m/product/edit/${id}` : '/m/product/edit')

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
    const params = { pageNo: next, pageSize: 10 }
    // [zh] 条件判断 if
    if (searchForm.productNameFuzzy) params.productNameFuzzy = searchForm.productNameFuzzy
    // [zh] 条件判断 if
    if (searchForm.status !== '') params.status = searchForm.status
    // [zh] 声明变量 `result`
    const result = await proxy.Request({ url: proxy.Api.loadProduct, params, showLoading: false })
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

// [zh] 声明变量 `refreshRow`
const refreshRow = ({ productId, totalStock }) => {
  // [zh] 声明变量 `row`
  const row = list.value.find((it) => it.productId == productId)
  // [zh] 条件判断 if
  if (row && totalStock != null) row.totalStock = totalStock
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
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
  // [zh] TypeScript/JS 语句：`mitter.on('updateStockCallback', refreshRow)`
  mitter.on('updateStockCallback', refreshRow)
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] TypeScript/JS 语句：`observer && observer.disconnect()`
  observer && observer.disconnect()
  // [zh] TypeScript/JS 语句：`observer = null`
  observer = null
  // [zh] TypeScript/JS 语句：`mitter.off('updateStockCallback')`
  mitter.off('updateStockCallback')
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `productStockRef`
const productStockRef = ref()
// [zh] 声明变量 `updateStock`
const updateStock = (row) => productStockRef.value.show(row.productId)

// [zh] 声明变量 `changeStatus`
const changeStatus = (row) => {
  // [zh] 条件判断 if
  if (row.status == 1) {
    // [zh] 声明变量 `block`
    const block = getDelistBlockReason(row)
    // [zh] 条件判断 if
    if (block) {
      // [zh] TypeScript/JS 语句：`proxy.Message.warning(block)`
      proxy.Message.warning(block)
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.ConfirmSensitive({
    // [zh] TypeScript/JS 语句：`message: `确定要【${row.status == 0 ? '上架' : '下架'}】该商品吗？`,`
    message: `确定要【${row.status == 0 ? '上架' : '下架'}】该商品吗？`,
    // [zh] 代码块开始
    okfun: async (sensitiveConfirmToken) => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.updateProductStatus,`
        url: proxy.Api.updateProductStatus,
        // [zh] TypeScript/JS 语句：`sensitiveConfirmToken,`
        sensitiveConfirmToken,
        // [zh] TypeScript/JS 语句：`params: { productId: row.productId, status: row.status `
        params: { productId: row.productId, status: row.status == 0 ? 1 : 0 }
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('操作成功')`
      proxy.Message.success('操作成功')
      // [zh] TypeScript/JS 语句：`reload()`
      reload()
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `commend`
const commend = (row) => {
  // [zh] 条件判断 if
  if (row.commendType == 0) {
    // [zh] 声明变量 `block`
    const block = getCommendBlockReason(row)
    // [zh] 条件判断 if
    if (block) {
      // [zh] TypeScript/JS 语句：`proxy.Message.warning(block)`
      proxy.Message.warning(block)
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定要【${row.commendType == 0 ? '推荐' : '取消推荐'}】吗`
    message: `确定要【${row.commendType == 0 ? '推荐' : '取消推荐'}】吗？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.commendProduct,`
        url: proxy.Api.commendProduct,
        // [zh] TypeScript/JS 语句：`params: { productId: row.productId, commendType: row.co`
        params: { productId: row.productId, commendType: row.commendType == 0 ? 1 : 0 }
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('操作成功')`
      proxy.Message.success('操作成功')
      // [zh] TypeScript/JS 语句：`reload()`
      reload()
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `del`
const del = (row) => {
  // [zh] 声明变量 `block`
  const block = getDeleteBlockReason(row)
  // [zh] 条件判断 if
  if (block) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning(block)`
    proxy.Message.warning(block)
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.ConfirmSensitive({
    // [zh] TypeScript/JS 语句：`message: `确定要删除【${row.productName}】吗？`,`
    message: `确定要删除【${row.productName}】吗？`,
    // [zh] 代码块开始
    okfun: async (sensitiveConfirmToken) => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.deleteProduct,`
        url: proxy.Api.deleteProduct,
        // [zh] TypeScript/JS 语句：`sensitiveConfirmToken,`
        sensitiveConfirmToken,
        // [zh] TypeScript/JS 语句：`params: { productId: row.productId }`
        params: { productId: row.productId }
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('操作成功')`
      proxy.Message.success('操作成功')
      // [zh] TypeScript/JS 语句：`reload()`
      reload()
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.m-product {` */
.m-product {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
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
  /* [zh] 样式规则 `height: 44px;` */
  height: 44px;
  /* [zh] 样式规则 `padding: 0 12px;` */
  padding: 0 12px;
  /* [zh] 样式规则 `border-radius: 16px;` */
  border-radius: 16px;

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

  /* [zh] 样式规则 `.search-select {` */
  .search-select {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.5);` */
    background: rgba(255, 255, 255, 0.5);
    /* [zh] 样式规则 `border-radius: 10px;` */
    border-radius: 10px;
    /* [zh] 样式规则 `padding: 4px 6px;` */
    padding: 4px 6px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
    /* [zh] 样式规则 `outline: none;` */
    outline: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-add-btn {` */
.m-add-btn {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `height: 42px;` */
  height: 42px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 14px;` */
  border-radius: 14px;
  /* [zh] 样式规则 `background: var(--m-ink);` */
  background: var(--m-ink);
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: transform 0.15s, opacity 0.2` */
  transition: transform 0.15s, opacity 0.2s;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.98);` */
    transform: scale(0.98);
    /* [zh] 样式规则 `opacity: 0.9;` */
    opacity: 0.9;
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

/* [zh] 样式规则 `.m-prod-card {` */
.m-prod-card {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;

  /* [zh] 样式规则 `.prod-top {` */
  .prod-top {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.prod-cover {` */
  .prod-cover {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.prod-main {` */
  .prod-main {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.prod-name {` */
  .prod-name {
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
    /* [zh] 样式规则 `display: -webkit-box;` */
    display: -webkit-box;
    /* [zh] 样式规则 `-webkit-line-clamp: 2;` */
    -webkit-line-clamp: 2;
    /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
    -webkit-box-orient: vertical;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.prod-meta {` */
  .prod-meta {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: baseline;` */
    align-items: baseline;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;

    /* [zh] 样式规则 `.prod-price {` */
    .prod-price {
      /* [zh] 样式规则 `font-size: 16px;` */
      font-size: 16px;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
      /* [zh] 样式规则 `color: var(--m-ink);` */
      color: var(--m-ink);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.prod-stock {` */
    .prod-stock {
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.prod-tags {` */
.prod-tags {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.prod-actions {` */
.prod-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `padding-top: 10px;` */
  padding-top: 10px;
  /* [zh] 样式规则 `border-top: 1px solid rgba(120, 120, 128` */
  border-top: 1px solid rgba(120, 120, 128, 0.16);

  /* [zh] 样式规则 `.act-btn {` */
  .act-btn {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 56px;` */
    min-width: 56px;
    /* [zh] 样式规则 `height: 32px;` */
    height: 32px;
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
    /* [zh] 样式规则 `transition: transform 0.15s, background ` */
    transition: transform 0.15s, background 0.2s, color 0.2s;

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `transform: scale(0.95);` */
      transform: scale(0.95);
      /* [zh] 样式规则 `background: var(--m-gold-soft);` */
      background: var(--m-gold-soft);
      /* [zh] 样式规则 `color: var(--m-ink);` */
      color: var(--m-ink);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.danger {` */
    &.danger {
      /* [zh] 样式规则 `color: var(--m-danger);` */
      color: var(--m-danger);
      /* [zh] 样式规则 `border-color: rgba(255, 59, 48, 0.3);` */
      border-color: rgba(255, 59, 48, 0.3);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.is-disabled {` */
    &.is-disabled {
      /* [zh] 样式规则 `opacity: 0.45;` */
      opacity: 0.45;
      /* [zh] 样式规则 `cursor: not-allowed;` */
      cursor: not-allowed;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-tag {` */
.m-tag {
  /* [zh] 样式规则 `padding: 2px 8px;` */
  padding: 2px 8px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;

  /* [zh] 样式规则 `&.green {` */
  &.green {
    /* [zh] 样式规则 `background: rgba(52, 199, 89, 0.16);` */
    background: rgba(52, 199, 89, 0.16);
    /* [zh] 样式规则 `color: #1c8c3c;` */
    color: #1c8c3c;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.muted {` */
  &.muted {
    /* [zh] 样式规则 `background: rgba(120, 120, 128, 0.16);` */
    background: rgba(120, 120, 128, 0.16);
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.danger {` */
  &.danger {
    /* [zh] 样式规则 `background: rgba(255, 59, 48, 0.14);` */
    background: rgba(255, 59, 48, 0.14);
    /* [zh] 样式规则 `color: var(--m-danger);` */
    color: var(--m-danger);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.gold {` */
  &.gold {
    /* [zh] 样式规则 `background: var(--m-gold-soft);` */
    background: var(--m-gold-soft);
    /* [zh] 样式规则 `color: #927535;` */
    color: #927535;
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
