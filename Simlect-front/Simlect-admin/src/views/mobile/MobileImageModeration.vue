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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `@/views/setting/HandleImageModeration.vue`
import HandleImageModeration from '@/views/setting/HandleImageModeration.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `searchForm`
const searchForm = reactive({ userIdFuzzy: '', status: 0 })
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
// [zh] 声明变量 `handleRef`
const handleRef = ref(null)
// [zh] 声明变量 `observer`
let observer = null

// [zh] 声明变量 `imageUrl`
const imageUrl = (path) => `${proxy.Api.sourcePath}${encodeURIComponent(path)}`
// [zh] 声明变量 `sceneLabel`
const sceneLabel = (s) => (s === 'avatar' ? '头像' : s === 'comment' ? '评论' : s || '—')
// [zh] 声明变量 `statusLabel`
const statusLabel = (s) => {
  // [zh] 条件判断 if
  if (s === 1) return '已通过'
  // [zh] 条件判断 if
  if (s === 2) return '确认违规'
  // [zh] 条件判断 if
  if (s === 3) return '误报驳回'
  // [zh] return 返回
  return '待复核'
// [zh] 语法符号
}
// [zh] 声明变量 `statusClass`
const statusClass = (s) => {
  // [zh] 条件判断 if
  if (s === 1) return 'done'
  // [zh] 条件判断 if
  if (s === 2) return 'danger'
  // [zh] 条件判断 if
  if (s === 3) return 'reject'
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
    if (searchForm.userIdFuzzy) params.userIdFuzzy = searchForm.userIdFuzzy
    // [zh] 条件判断 if
    if (searchForm.status !== undefined && searchForm.status !== '') params.status = Number(searchForm.status)
    // [zh] 声明变量 `result`
    const result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.imageModerationLoadList,`
      url: proxy.Api.imageModerationLoadList,
      // [zh] TypeScript/JS 语句：`params,`
      params,
      // [zh] TypeScript/JS 语句：`showLoading: false`
      showLoading: false
    // [zh] TypeScript/JS 语句：`})`
    })
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
// [zh] 声明变量 `openHandle`
const openHandle = (row) => handleRef.value?.show(row)

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`reload()`
  reload()
  // [zh] TypeScript/JS 语句：`observer = new IntersectionObserver(`
  observer = new IntersectionObserver(
    // [zh] 代码块开始
    (entries) => {
      // [zh] 条件判断 if
      if (entries.some((e) => e.isIntersecting)) loadList()
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ rootMargin: '120px' }`
    { rootMargin: '120px' }
  // [zh] 语法符号
  )
  // [zh] 条件判断 if
  if (sentinel.value) observer.observe(sentinel.value)
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] TypeScript/JS 语句：`observer?.disconnect()`
  observer?.disconnect()
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
