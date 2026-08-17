<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-search glass-card glass-strong">
      <!-- [zh] 开始标签 `<input>` -->
      <input v-model="searchForm.userIdFuzzy" class="search-input" placeholder="用户ID" @keyup.enter="reload" />
      <!-- [zh] 开始标签 `<select>` -->
      <select v-model="searchForm.status" class="search-select" @change="reload">
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="0">待复核</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="undefined">全部</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="1">已通过</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="2">确认违规</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="3">误报驳回</option>
      <!-- [zh] 闭合标签 `</select>` -->
      </select>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="row in list" :key="row.recordId" class="glass-card mod-card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="mod-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="mod-scene">{{ sceneLabel(row.scene) }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="mod-status" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<img>` -->
        <img v-if="row.imagePath" class="mod-img" :src="imageUrl(row.imagePath)" alt="" />
        <!-- [zh] 开始标签 `<p>` -->
        <p class="mod-user">用户：{{ row.userId }}</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="mod-conc">{{ row.conclusion || '—' }}</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="mod-time">{{ row.createTime }}</p>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="mod-ops">
          <!-- [zh] 开始标签 `<button>` -->
          <button v-if="row.status === 0" type="button" class="op-btn primary" @click="openHandle(row)">复核</button>
          <!-- [zh] 开始标签 `<button>` -->
          <button v-else type="button" class="op-btn" @click="openHandle(row)">用户解封</button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="!loading" class="m-empty-tip">暂无待复核图片</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="sentinel" class="m-sentinel">
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="loading">加载中…</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-else-if="finished && list.length">没有更多了</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<HandleImageModeration>` -->
    <HandleImageModeration ref="handleRef" @reload="reload" />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup>
import HandleImageModeration from '@/views/setting/HandleImageModeration.vue'
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from 'vue'

const { proxy } = getCurrentInstance()
const searchForm = reactive({ userIdFuzzy: '', status: 0 })
const list = ref([])
const pageNo = ref(0)
const pageTotal = ref(1)
const loading = ref(false)
const finished = ref(false)
const sentinel = ref(null)
const handleRef = ref(null)
let observer = null

const imageUrl = (path) => `${proxy.Api.sourcePath}${encodeURIComponent(path)}`
const sceneLabel = (s) => (s === 'avatar' ? '头像' : s === 'comment' ? '评论' : s || '—')
const statusLabel = (s) => {
  if (s === 1) return '已通过'
  if (s === 2) return '确认违规'
  if (s === 3) return '误报驳回'
  return '待复核'
}
const statusClass = (s) => {
  if (s === 1) return 'done'
  if (s === 2) return 'danger'
  if (s === 3) return 'reject'
  return 'pending'
}

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
    const params = { pageNo: next, pageSize: 8 }
    if (searchForm.userIdFuzzy) params.userIdFuzzy = searchForm.userIdFuzzy
    if (searchForm.status !== undefined && searchForm.status !== '') params.status = Number(searchForm.status)
    const result = await proxy.Request({
      url: proxy.Api.imageModerationLoadList,
      params,
      showLoading: false
    })
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
const openHandle = (row) => handleRef.value?.show(row)

onMounted(() => {
  reload()
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) loadList()
    },
    { rootMargin: '120px' }
  )
  if (sentinel.value) observer.observe(sentinel.value)
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
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
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-search {` */
.m-search {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;

  /* [zh] 样式规则 `.search-input {` */
  .search-input {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `outline: none;` */
    outline: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-select {` */
  .search-select {
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.5);` */
    background: rgba(255, 255, 255, 0.5);
    /* [zh] 样式规则 `border-radius: 8px;` */
    border-radius: 8px;
    /* [zh] 样式规则 `padding: 4px 8px;` */
    padding: 4px 8px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.mod-card {` */
.mod-card {
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;

  /* [zh] 样式规则 `.mod-head {` */
  .mod-head {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `margin-bottom: 8px;` */
    margin-bottom: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.mod-scene {` */
  .mod-scene {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.mod-status {` */
  .mod-status {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `padding: 2px 8px;` */
    padding: 2px 8px;
    /* [zh] 样式规则 `border-radius: 999px;` */
    border-radius: 999px;

    /* [zh] 样式规则 `&.pending {` */
    &.pending {
      /* [zh] 样式规则 `background: rgba(255, 149, 0, 0.15);` */
      background: rgba(255, 149, 0, 0.15);
      /* [zh] 样式规则 `color: #ff9500;` */
      color: #ff9500;
    /* [zh] 样式规则 `}` */
    }
    /* [zh] 样式规则 `&.done {` */
    &.done {
      /* [zh] 样式规则 `background: rgba(52, 199, 89, 0.15);` */
      background: rgba(52, 199, 89, 0.15);
      /* [zh] 样式规则 `color: #34c759;` */
      color: #34c759;
    /* [zh] 样式规则 `}` */
    }
    /* [zh] 样式规则 `&.danger {` */
    &.danger {
      /* [zh] 样式规则 `background: rgba(255, 59, 48, 0.15);` */
      background: rgba(255, 59, 48, 0.15);
      /* [zh] 样式规则 `color: #ff3b30;` */
      color: #ff3b30;
    /* [zh] 样式规则 `}` */
    }
    /* [zh] 样式规则 `&.reject {` */
    &.reject {
      /* [zh] 样式规则 `background: rgba(142, 142, 147, 0.15);` */
      background: rgba(142, 142, 147, 0.15);
      /* [zh] 样式规则 `color: #8e8e93;` */
      color: #8e8e93;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.mod-img {` */
  .mod-img {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `max-height: 200px;` */
    max-height: 200px;
    /* [zh] 样式规则 `object-fit: contain;` */
    object-fit: contain;
    /* [zh] 样式规则 `border-radius: 10px;` */
    border-radius: 10px;
    /* [zh] 样式规则 `background: #f5f5f7;` */
    background: #f5f5f7;
    /* [zh] 样式规则 `margin-bottom: 8px;` */
    margin-bottom: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  .mod-user,
  /* [zh] 样式声明 */
  .mod-conc,
  /* [zh] 样式规则 `.mod-time {` */
  .mod-time {
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

  /* [zh] 样式规则 `.mod-ops {` */
  .mod-ops {
    /* [zh] 样式规则 `margin-top: 10px;` */
    margin-top: 10px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.op-btn {` */
  .op-btn {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `height: 34px;` */
    height: 34px;
    /* [zh] 样式规则 `border-radius: 10px;` */
    border-radius: 10px;
    /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
    border: 1px solid rgba(120, 120, 128, 0.24);
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.5);` */
    background: rgba(255, 255, 255, 0.5);
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;

    /* [zh] 样式规则 `&.primary {` */
    &.primary {
      /* [zh] 样式规则 `color: var(--m-gold);` */
      color: var(--m-gold);
      /* [zh] 样式规则 `border-color: rgba(201, 169, 98, 0.4);` */
      border-color: rgba(201, 169, 98, 0.4);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-sentinel {` */
.m-sentinel {
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-empty-tip {` */
.m-empty-tip {
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `padding: 40px 16px;` */
  padding: 40px 16px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
/* [zh] 样式规则 `}` */
}
</style>
