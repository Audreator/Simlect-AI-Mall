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

<script setup>
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from 'vue'

const { proxy } = getCurrentInstance()
const searchForm = reactive({ idempotencyKeyFuzzy: '', bizScene: '', status: '' })
const list = ref([])
const pageNo = ref(0)
const pageTotal = ref(1)
const loading = ref(false)
const finished = ref(false)
const sentinel = ref(null)
const detailVisible = ref(false)
const currentRow = ref(null)
const saving = ref(false)
const replaying = ref(false)
const handleForm = reactive({ status: 0, handleRemark: '' })
let observer = null

const STATUS = { 0: '待处理', 1: '处理中', 2: '已重放', 3: '重放失败', 4: '已忽略' }
const statusLabel = (s) => STATUS[s] ?? s

const loadList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    pageNo.value = 0
    pageTotal.value = 1
    finished.value = false
    list.value = []
  }
  if (finished.value) return
  loading.value = true
  try {
    const next = pageNo.value + 1
    const params = { pageNo: next, pageSize: 15 }
    if (searchForm.idempotencyKeyFuzzy) params.idempotencyKeyFuzzy = searchForm.idempotencyKeyFuzzy
    if (searchForm.bizScene) params.bizScene = searchForm.bizScene
    if (searchForm.status !== '') params.status = searchForm.status
    const result = await proxy.Request({ url: proxy.Api.mqCompensationLogLoadList, params, showLoading: false })
    if (!result) return
    const data = result.data || {}
    const chunk = data.list || []
    list.value = next === 1 ? chunk : list.value.concat(chunk)
    pageNo.value = Number(data.pageNo) || next
    pageTotal.value = Number(data.pageTotal) || pageNo.value
    finished.value = pageNo.value >= pageTotal.value
  } finally {
    loading.value = false
  }
}

const reload = () => loadList(true)

const openDetail = (row) => {
  currentRow.value = row
  handleForm.status = row.status ?? 0
  handleForm.handleRemark = row.handleRemark || ''
  detailVisible.value = true
}

const saveStatus = async () => {
  if (!currentRow.value) return
  saving.value = true
  try {
    const result = await proxy.Request({
      url: proxy.Api.mqCompensationLogUpdateStatus,
      params: {
        logId: currentRow.value.logId,
        status: handleForm.status,
        handleRemark: handleForm.handleRemark
      },
      showLoading: true
    })
    if (!result) return
    proxy.Message.success('已保存')
    detailVisible.value = false
    reload()
  } finally {
    saving.value = false
  }
}

const doReplay = async () => {
  if (!currentRow.value) return
  replaying.value = true
  try {
    const result = await proxy.Request({
      url: proxy.Api.mqCompensationLogReplay,
      params: { logId: currentRow.value.logId },
      showLoading: true
    })
    if (!result) return
    proxy.Message.success('重放已提交')
    detailVisible.value = false
    reload()
  } finally {
    replaying.value = false
  }
}

onMounted(() => {
  loadList(true)
  observer = new IntersectionObserver((entries) => {
    if (entries[0]?.isIntersecting) loadList(false)
  }, { rootMargin: '120px' })
  if (sentinel.value) observer.observe(sentinel.value)
})

onUnmounted(() => {
  if (observer) observer.disconnect()
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
