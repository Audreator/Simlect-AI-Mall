<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-search glass-card glass-strong">
      <!-- [zh] 开始标签 `<input>` -->
      <input
        v-model="searchForm.idempotencyKeyFuzzy"
        class="search-input"
        placeholder="幂等键模糊搜索"
        @keyup.enter="reload"
      />
      <!-- [zh] 开始标签 `<select>` -->
      <select v-model="searchForm.bizScene" class="search-select" @change="reload">
        <!-- [zh] 开始标签 `<option>` -->
        <option value="">全部场景</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option value="RAG">RAG</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option value="NOTIFY">通知</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option value="BROWSE">足迹</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option value="SIGN">签到</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option value="PAY">订单</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option value="CONSUME">消费失败</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option value="OTHER">其他</option>
      <!-- [zh] 闭合标签 `</select>` -->
      </select>
      <!-- [zh] 开始标签 `<select>` -->
      <select v-model="searchForm.status" class="search-select" @change="reload">
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="''">全部状态</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="0">待处理</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="1">处理中</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="2">已重放成功</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="3">重放失败</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="4">已忽略</option>
      <!-- [zh] 闭合标签 `</select>` -->
      </select>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="row in list" :key="row.logId" class="glass-card log-row" @click="openDetail(row)">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="log-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="log-id">#{{ row.logId }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="log-scene">{{ row.bizScene || '—' }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="log-status" :class="'st-' + row.status">{{ statusLabel(row.status) }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="log-key">{{ row.idempotencyKey }}</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="log-err">{{ row.errorMessage || '—' }}</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="log-meta">{{ row.createTime }} · 重试 {{ row.retryCount ?? 0 }}</p>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="!loading" class="m-empty-tip">暂无补偿日志</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="sentinel" class="m-sentinel">
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="loading">加载中…</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-else-if="finished && list.length">没有更多了</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="detailVisible" class="m-sheet-mask" @click.self="detailVisible = false">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-sheet glass-card">
        <!-- [zh] 开始标签 `<h3>` -->
        <h3 class="sheet-title">MQ 补偿 #{{ currentRow?.logId }}</h3>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="currentRow" class="sheet-body">
          <!-- [zh] 开始标签 `<p>` -->
          <p><strong>场景</strong> {{ currentRow.bizScene }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p><strong>幂等键</strong> {{ currentRow.idempotencyKey }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p><strong>路由键</strong> {{ currentRow.routingKey }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p><strong>失败原因</strong> {{ currentRow.errorMessage || '—' }}</p>
          <!-- [zh] 开始标签 `<pre>` -->
          <pre class="payload-pre">{{ currentRow.payloadJson || '—' }}</pre>
          <!-- [zh] 开始标签 `<label>` -->
          <label class="m-label">处理状态</label>
          <!-- [zh] 开始标签 `<select>` -->
          <select v-model="handleForm.status" class="m-select">
            <!-- [zh] 开始标签 `<option>` -->
            <option :value="0">待处理</option>
            <!-- [zh] 开始标签 `<option>` -->
            <option :value="1">处理中</option>
            <!-- [zh] 开始标签 `<option>` -->
            <option :value="2">已重放成功</option>
            <!-- [zh] 开始标签 `<option>` -->
            <option :value="3">重放失败</option>
            <!-- [zh] 开始标签 `<option>` -->
            <option :value="4">已忽略</option>
          <!-- [zh] 闭合标签 `</select>` -->
          </select>
          <!-- [zh] 开始标签 `<label>` -->
          <label class="m-label">备注</label>
          <!-- [zh] 开始标签 `<textarea>` -->
          <textarea v-model="handleForm.handleRemark" class="m-textarea" rows="3" maxlength="512" />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sheet-ops">
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn" @click="detailVisible = false">关闭</button>
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn warn" :disabled="replaying" @click="doReplay">重放</button>
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn primary" :disabled="saving" @click="saveStatus">保存</button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `searchForm`
const searchForm = reactive({ idempotencyKeyFuzzy: '', bizScene: '', status: '' })
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
// [zh] 声明变量 `detailVisible`
const detailVisible = ref(false)
// [zh] 声明变量 `currentRow`
const currentRow = ref(null)
// [zh] 声明变量 `saving`
const saving = ref(false)
// [zh] 声明变量 `replaying`
const replaying = ref(false)
// [zh] 声明变量 `handleForm`
const handleForm = reactive({ status: 0, handleRemark: '' })
// [zh] 声明变量 `observer`
let observer = null

// [zh] 声明变量 `STATUS`
const STATUS = { 0: '待处理', 1: '处理中', 2: '已重放', 3: '重放失败', 4: '已忽略' }
// [zh] 声明变量 `statusLabel`
const statusLabel = (s) => STATUS[s] ?? s

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
    const params = { pageNo: next, pageSize: 15 }
    // [zh] 条件判断 if
    if (searchForm.idempotencyKeyFuzzy) params.idempotencyKeyFuzzy = searchForm.idempotencyKeyFuzzy
    // [zh] 条件判断 if
    if (searchForm.bizScene) params.bizScene = searchForm.bizScene
    // [zh] 条件判断 if
    if (searchForm.status !== '') params.status = searchForm.status
    // [zh] 声明变量 `result`
    const result = await proxy.Request({ url: proxy.Api.mqCompensationLogLoadList, params, showLoading: false })
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

// [zh] 声明变量 `openDetail`
const openDetail = (row) => {
  // [zh] TypeScript/JS 语句：`currentRow.value = row`
  currentRow.value = row
  // [zh] TypeScript/JS 语句：`handleForm.status = row.status ?? 0`
  handleForm.status = row.status ?? 0
  // [zh] TypeScript/JS 语句：`handleForm.handleRemark = row.handleRemark || ''`
  handleForm.handleRemark = row.handleRemark || ''
  // [zh] TypeScript/JS 语句：`detailVisible.value = true`
  detailVisible.value = true
// [zh] 语法符号
}

// [zh] 声明变量 `saveStatus`
const saveStatus = async () => {
  // [zh] 条件判断 if
  if (!currentRow.value) return
  // [zh] TypeScript/JS 语句：`saving.value = true`
  saving.value = true
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `result`
    const result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.mqCompensationLogUpdateStatus,`
      url: proxy.Api.mqCompensationLogUpdateStatus,
      // [zh] 代码块开始
      params: {
        // [zh] TypeScript/JS 语句：`logId: currentRow.value.logId,`
        logId: currentRow.value.logId,
        // [zh] TypeScript/JS 语句：`status: handleForm.status,`
        status: handleForm.status,
        // [zh] TypeScript/JS 语句：`handleRemark: handleForm.handleRemark`
        handleRemark: handleForm.handleRemark
      // [zh] TypeScript/JS 语句：`},`
      },
      // [zh] TypeScript/JS 语句：`showLoading: true`
      showLoading: true
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 条件判断 if
    if (!result) return
    // [zh] TypeScript/JS 语句：`proxy.Message.success('已保存')`
    proxy.Message.success('已保存')
    // [zh] TypeScript/JS 语句：`detailVisible.value = false`
    detailVisible.value = false
    // [zh] TypeScript/JS 语句：`reload()`
    reload()
  // [zh] 代码块开始
  } finally {
    // [zh] TypeScript/JS 语句：`saving.value = false`
    saving.value = false
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `doReplay`
const doReplay = async () => {
  // [zh] 条件判断 if
  if (!currentRow.value) return
  // [zh] TypeScript/JS 语句：`replaying.value = true`
  replaying.value = true
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `result`
    const result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.mqCompensationLogReplay,`
      url: proxy.Api.mqCompensationLogReplay,
      // [zh] TypeScript/JS 语句：`params: { logId: currentRow.value.logId },`
      params: { logId: currentRow.value.logId },
      // [zh] TypeScript/JS 语句：`showLoading: true`
      showLoading: true
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 条件判断 if
    if (!result) return
    // [zh] TypeScript/JS 语句：`proxy.Message.success('重放已提交')`
    proxy.Message.success('重放已提交')
    // [zh] TypeScript/JS 语句：`detailVisible.value = false`
    detailVisible.value = false
    // [zh] TypeScript/JS 语句：`reload()`
    reload()
  // [zh] 代码块开始
  } finally {
    // [zh] TypeScript/JS 语句：`replaying.value = false`
    replaying.value = false
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`loadList(true)`
  loadList(true)
  // [zh] 代码块开始
  observer = new IntersectionObserver((entries) => {
    // [zh] 条件判断 if
    if (entries[0]?.isIntersecting) loadList(false)
  // [zh] TypeScript/JS 语句：`}, { rootMargin: '120px' })`
  }, { rootMargin: '120px' })
  // [zh] 条件判断 if
  if (sentinel.value) observer.observe(sentinel.value)
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] 条件判断 if
  if (observer) observer.disconnect()
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.m-search {` */
.m-search {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.search-input,
/* [zh] 样式规则 `.search-select {` */
.search-select {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 40px;` */
  height: 40px;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
  border: 1px solid rgba(120, 120, 128, 0.2);
  /* [zh] 样式规则 `padding: 0 12px;` */
  padding: 0 12px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.6);` */
  background: rgba(255, 255, 255, 0.6);
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

/* [zh] 样式规则 `.log-row {` */
.log-row {
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.log-head {` */
.log-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 6px;` */
  margin-bottom: 6px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.log-id {` */
.log-id {
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.log-scene {` */
.log-scene {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--m-ink-2);` */
  color: var(--m-ink-2);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.log-status {` */
.log-status {
  /* [zh] 样式规则 `margin-left: auto;` */
  margin-left: auto;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `padding: 2px 8px;` */
  padding: 2px 8px;
  /* [zh] 样式规则 `border-radius: 999px;` */
  border-radius: 999px;
  /* [zh] 样式规则 `background: rgba(120, 120, 128, 0.15);` */
  background: rgba(120, 120, 128, 0.15);

  /* [zh] 样式规则 `&.st-0 { color: #b56a00; background: rgb` */
  &.st-0 { color: #b56a00; background: rgba(255, 149, 0, 0.15); }
  /* [zh] 样式规则 `&.st-2 { color: #248a3d; background: rgb` */
  &.st-2 { color: #248a3d; background: rgba(52, 199, 89, 0.15); }
  /* [zh] 样式规则 `&.st-3 { color: var(--m-danger); backgro` */
  &.st-3 { color: var(--m-danger); background: rgba(255, 59, 48, 0.12); }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.log-key,
/* [zh] 样式声明 */
.log-err,
/* [zh] 样式规则 `.log-meta {` */
.log-meta {
  /* [zh] 样式规则 `margin: 0 0 4px;` */
  margin: 0 0 4px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--m-ink-2);` */
  color: var(--m-ink-2);
  /* [zh] 样式规则 `word-break: break-all;` */
  word-break: break-all;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.log-err {` */
.log-err {
  /* [zh] 样式规则 `color: var(--m-ink);` */
  color: var(--m-ink);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-sheet-mask {` */
.m-sheet-mask {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `inset: 0;` */
  inset: 0;
  /* [zh] 样式规则 `z-index: 200;` */
  z-index: 200;
  /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.35);` */
  background: rgba(0, 0, 0, 0.35);
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-end;` */
  align-items: flex-end;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-sheet {` */
.m-sheet {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-height: 85vh;` */
  max-height: 85vh;
  /* [zh] 样式规则 `overflow: auto;` */
  overflow: auto;
  /* [zh] 样式规则 `border-radius: 16px 16px 0 0;` */
  border-radius: 16px 16px 0 0;
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sheet-title {` */
.sheet-title {
  /* [zh] 样式规则 `margin: 0 0 12px;` */
  margin: 0 0 12px;
  /* [zh] 样式规则 `font-size: 16px;` */
  font-size: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sheet-body p {` */
.sheet-body p {
  /* [zh] 样式规则 `margin: 0 0 8px;` */
  margin: 0 0 8px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `word-break: break-all;` */
  word-break: break-all;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.payload-pre {` */
.payload-pre {
  /* [zh] 样式规则 `margin: 8px 0;` */
  margin: 8px 0;
  /* [zh] 样式规则 `padding: 8px;` */
  padding: 8px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.04);` */
  background: rgba(0, 0, 0, 0.04);
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `max-height: 120px;` */
  max-height: 120px;
  /* [zh] 样式规则 `overflow: auto;` */
  overflow: auto;
  /* [zh] 样式规则 `white-space: pre-wrap;` */
  white-space: pre-wrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sheet-ops {` */
.sheet-ops {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-top: 12px;` */
  margin-top: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.op-btn {` */
.op-btn {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `height: 42px;` */
  height: 42px;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
  border: 1px solid rgba(120, 120, 128, 0.25);
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;

  /* [zh] 样式规则 `&.primary {` */
  &.primary {
    /* [zh] 样式规则 `background: var(--m-ink);` */
    background: var(--m-ink);
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `border: none;` */
    border: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.warn {` */
  &.warn {
    /* [zh] 样式规则 `color: #b56a00;` */
    color: #b56a00;
    /* [zh] 样式规则 `border-color: rgba(255, 149, 0, 0.4);` */
    border-color: rgba(255, 149, 0, 0.4);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
