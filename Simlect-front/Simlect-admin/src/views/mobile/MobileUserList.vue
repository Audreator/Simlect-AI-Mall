<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-user">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-search glass-card glass-strong">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="iconfont icon-search search-icon"></span>
      <!-- [zh] 开始标签 `<input>` -->
      <input
        v-model="searchForm.nickNameFuzzy"
        class="search-input"
        type="search"
        placeholder="搜索用户昵称"
        @keyup.enter="reload"
      />
      <!-- [zh] 开始标签 `<select>` -->
      <select v-model="searchForm.status" class="search-select" @change="reload">
        <!-- [zh] 开始标签 `<option>` -->
        <option value="">全部</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="1">正常</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="0">禁用</option>
      <!-- [zh] 闭合标签 `</select>` -->
      </select>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="u in list" :key="u.userId" class="m-user-card glass-card">
        <!-- [zh] 开始标签 `<Avatar>` -->
        <Avatar :avatar="u.avatar || undefined" :width="46"></Avatar>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="user-info">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="user-top">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="user-name">{{ u.nickName }}</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="user-sex">{{ SEX_MAP[u.sex] || '未知' }}</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="m-tag" :class="u.status == 1 ? 'green' : 'danger'">
              <!-- [zh] Mustache 插值表达式 -->
              {{ u.status == 1 ? '正常' : '已禁用' }}
            <!-- [zh] 闭合标签 `</span>` -->
            </span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="user-id">ID：{{ u.userId }}</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="user-mail">{{ u.email || '未绑定邮箱' }}</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="user-time">加入 {{ u.joinTime }}</div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="user-op" :class="{ danger: u.status == 1 }" @click="changeStatus(u)">
          <!-- [zh] Mustache 插值表达式 -->
          {{ u.status == 0 ? '启用' : '禁用' }}
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="!loading" class="m-empty-tip">暂无用户</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="sentinel" class="m-sentinel">
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="loading">加载中…</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-else-if="finished && list.length">没有更多了</span>
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

// [zh] 声明变量 `SEX_MAP`
const SEX_MAP = { 0: '女', 1: '男', 2: '保密' }

// [zh] 声明变量 `searchForm`
const searchForm = reactive({ nickNameFuzzy: '', status: '' })
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
    const params = { pageNo: next, pageSize: 12 }
    // [zh] 条件判断 if
    if (searchForm.nickNameFuzzy) params.nickNameFuzzy = searchForm.nickNameFuzzy
    // [zh] 条件判断 if
    if (searchForm.status !== '') params.status = searchForm.status
    // [zh] 声明变量 `result`
    const result = await proxy.Request({ url: proxy.Api.loadUser, params, showLoading: false })
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

// [zh] 声明变量 `changeStatus`
const changeStatus = (u) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定要${u.status == 0 ? '启用' : '禁用'}【${u.nickNam`
    message: `确定要${u.status == 0 ? '启用' : '禁用'}【${u.nickName}】吗？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.changeStatus,`
        url: proxy.Api.changeStatus,
        // [zh] TypeScript/JS 语句：`params: { userId: u.userId, status: u.status == 0 ? 1 :`
        params: { userId: u.userId, status: u.status == 0 ? 1 : 0 }
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
/* [zh] 样式规则 `.m-user {` */
.m-user {
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

/* [zh] 样式规则 `.m-user-card {` */
.m-user-card {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;

  /* [zh] 样式规则 `.user-info {` */
  .user-info {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-top {` */
  .user-top {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;

    /* [zh] 样式规则 `.user-name {` */
    .user-name {
      /* [zh] 样式规则 `font-size: 14px;` */
      font-size: 14px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
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

    /* [zh] 样式规则 `.user-sex {` */
    .user-sex {
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-mail {` */
  .user-mail {
    /* [zh] 样式规则 `margin-top: 3px;` */
    margin-top: 3px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-id {` */
  .user-id {
    /* [zh] 样式规则 `margin-top: 2px;` */
    margin-top: 2px;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
    /* [zh] 样式规则 `word-break: break-all;` */
    word-break: break-all;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-time {` */
  .user-time {
    /* [zh] 样式规则 `margin-top: 2px;` */
    margin-top: 2px;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-op {` */
  .user-op {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
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

/* [zh] 样式规则 `.m-tag {` */
.m-tag {
  /* [zh] 样式规则 `padding: 2px 8px;` */
  padding: 2px 8px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;

  /* [zh] 样式规则 `&.green {` */
  &.green {
    /* [zh] 样式规则 `background: rgba(52, 199, 89, 0.16);` */
    background: rgba(52, 199, 89, 0.16);
    /* [zh] 样式规则 `color: #1c8c3c;` */
    color: #1c8c3c;
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
