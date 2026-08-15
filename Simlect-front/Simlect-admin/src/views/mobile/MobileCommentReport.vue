<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-search glass-card glass-strong">
      <!-- [zh] 开始标签 `<input>` -->
      <input v-model="searchForm.reasonFuzzy" class="search-input" placeholder="举报理由" @keyup.enter="reload" />
      <!-- [zh] 开始标签 `<select>` -->
      <select v-model="searchForm.status" class="search-select" @change="reload">
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="undefined">全部状态</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="0">待处理</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="1">已处理</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="2">已驳回</option>
      <!-- [zh] 闭合标签 `</select>` -->
      </select>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="row in list" :key="row.reportId" class="glass-card rpt-card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="rpt-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="rpt-reason">{{ row.reason }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="rpt-status" :class="statusClass(row.status)">
            <!-- [zh] Mustache 插值表达式 -->
            {{ statusLabel(row.status) }}
          <!-- [zh] 闭合标签 `</span>` -->
          </span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="rpt-order">订单号：{{ row.orderId }}</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="row.detail" class="rpt-detail">{{ row.detail }}</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="row.commentSnapshot" class="rpt-snapshot">"{{ row.commentSnapshot }}"</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="rpt-time">{{ row.reportTime }}</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="row.handleRemark" class="rpt-remark">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="tag">处理</span>{{ row.handleRemark }}
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="rpt-ops">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-if="row.status === 0"
            type="button"
            class="op-btn primary"
            @click="handleHandler(row)"
          >
            <!-- [zh] 模板内容：`处理` -->
            处理
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn danger" @click="del(row)">删除</button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="!loading" class="m-empty-tip">暂无举报记录</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="sentinel" class="m-sentinel">
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="loading">加载中…</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-else-if="finished && list.length">没有更多了</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<HandleReport>` -->
    <HandleReport ref="handleRef" @reload="reload"></HandleReport>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `@/views/order/HandleReport.vue`
import HandleReport from '@/views/order/HandleReport.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `searchForm`
const searchForm = reactive({ reasonFuzzy: '', status: undefined })
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

// [zh] 声明变量 `statusLabel`
const statusLabel = (s) => {
  // [zh] 条件判断 if
  if (s === 1) return '已处理'
  // [zh] 条件判断 if
  if (s === 2) return '已驳回'
  // [zh] return 返回
  return '待处理'
// [zh] 语法符号
}

// [zh] 声明变量 `statusClass`
const statusClass = (s) => {
  // [zh] 条件判断 if
  if (s === 1) return 'done'
  // [zh] 条件判断 if
  if (s === 2) return 'reject'
  // [zh] return 返回
  return 'pending'
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
    if (searchForm.reasonFuzzy) params.reasonFuzzy = searchForm.reasonFuzzy
    // [zh] 条件判断 if
    if (searchForm.status !== undefined && searchForm.status !== '') params.status = Number(searchForm.status)
    // [zh] 声明变量 `result`
    const result = await proxy.Request({ url: proxy.Api.loadCommentReport, params, showLoading: false })
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

// [zh] 声明变量 `handleRef`
const handleRef = ref()
// [zh] 声明变量 `handleHandler`
const handleHandler = (row) => handleRef.value.show(row)

// [zh] 声明变量 `del`
const del = (row) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: '确定要删除该举报记录吗？',`
    message: '确定要删除该举报记录吗？',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.deleteCommentReport,`
        url: proxy.Api.deleteCommentReport,
        // [zh] TypeScript/JS 语句：`params: { reportId: row.reportId }`
        params: { reportId: row.reportId }
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
/* [zh] 样式规则 `.m-simple {` */
.m-simple {
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
    /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
    border: 1px solid rgba(120, 120, 128, 0.24);
    /* [zh] 样式规则 `border-radius: 10px;` */
    border-radius: 10px;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.5);` */
    background: rgba(255, 255, 255, 0.5);
    /* [zh] 样式规则 `height: 30px;` */
    height: 30px;
    /* [zh] 样式规则 `padding: 0 6px;` */
    padding: 0 6px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `outline: none;` */
    outline: none;
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

/* [zh] 样式规则 `.rpt-card {` */
.rpt-card {
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;

  /* [zh] 样式规则 `.rpt-head {` */
  .rpt-head {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rpt-reason {` */
  .rpt-reason {
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rpt-status {` */
  .rpt-status {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `padding: 2px 8px;` */
    padding: 2px 8px;
    /* [zh] 样式规则 `border-radius: 8px;` */
    border-radius: 8px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;

    /* [zh] 样式规则 `&.pending {` */
    &.pending {
      /* [zh] 样式规则 `background: rgba(255, 149, 0, 0.12);` */
      background: rgba(255, 149, 0, 0.12);
      /* [zh] 样式规则 `color: #b87000;` */
      color: #b87000;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.done {` */
    &.done {
      /* [zh] 样式规则 `background: rgba(52, 199, 89, 0.12);` */
      background: rgba(52, 199, 89, 0.12);
      /* [zh] 样式规则 `color: #1a7a3a;` */
      color: #1a7a3a;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.reject {` */
    &.reject {
      /* [zh] 样式规则 `background: rgba(120, 120, 128, 0.12);` */
      background: rgba(120, 120, 128, 0.12);
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rpt-order {` */
  .rpt-order {
    /* [zh] 样式规则 `margin: 8px 0 4px;` */
    margin: 8px 0 4px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rpt-detail {` */
  .rpt-detail {
    /* [zh] 样式规则 `margin: 0 0 4px;` */
    margin: 0 0 4px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rpt-snapshot {` */
  .rpt-snapshot {
    /* [zh] 样式规则 `margin: 6px 0;` */
    margin: 6px 0;
    /* [zh] 样式规则 `padding: 6px 10px;` */
    padding: 6px 10px;
    /* [zh] 样式规则 `border-radius: 8px;` */
    border-radius: 8px;
    /* [zh] 样式规则 `background: rgba(120, 120, 128, 0.06);` */
    background: rgba(120, 120, 128, 0.06);
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
    /* [zh] 样式规则 `line-height: 1.45;` */
    line-height: 1.45;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rpt-time {` */
  .rpt-time {
    /* [zh] 样式规则 `margin: 4px 0 0;` */
    margin: 4px 0 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rpt-remark {` */
  .rpt-remark {
    /* [zh] 样式规则 `margin: 8px 0 0;` */
    margin: 8px 0 0;
    /* [zh] 样式规则 `padding: 8px 10px;` */
    padding: 8px 10px;
    /* [zh] 样式规则 `border-radius: 10px;` */
    border-radius: 10px;
    /* [zh] 样式规则 `background: rgba(120, 120, 128, 0.08);` */
    background: rgba(120, 120, 128, 0.08);
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);

    /* [zh] 样式规则 `.tag {` */
    .tag {
      /* [zh] 样式规则 `display: inline-block;` */
      display: inline-block;
      /* [zh] 样式规则 `margin-right: 6px;` */
      margin-right: 6px;
      /* [zh] 样式规则 `padding: 0 6px;` */
      padding: 0 6px;
      /* [zh] 样式规则 `border-radius: 6px;` */
      border-radius: 6px;
      /* [zh] 样式规则 `background: var(--m-gold-soft);` */
      background: var(--m-gold-soft);
      /* [zh] 样式规则 `color: #927535;` */
      color: #927535;
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rpt-ops {` */
  .rpt-ops {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `margin-top: 10px;` */
    margin-top: 10px;

    /* [zh] 样式规则 `.op-btn {` */
    .op-btn {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
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

      /* [zh] 样式规则 `&.primary {` */
      &.primary {
        /* [zh] 样式规则 `color: var(--m-blue);` */
        color: var(--m-blue);
        /* [zh] 样式规则 `border-color: rgba(0, 113, 227, 0.3);` */
        border-color: rgba(0, 113, 227, 0.3);
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
