<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-search glass-card glass-strong">
      <!-- [zh] 开始标签 `<input>` -->
      <input
        v-model="searchForm.questionFuzzy"
        class="search-input"
        placeholder="搜索问题"
        @keyup.enter="reload"
      />
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="op-btn sm primary" @click="reload">搜索</button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<button>` -->
    <button type="button" class="op-btn primary block" @click="showEdit()">新增问答</button>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="row in list" :key="row.questionId" class="glass-card rag-card">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="rag-question">{{ row.question }}</p>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="row.similarQuestion?.length" class="rag-similar">
          <!-- [zh] 开始标签 `<span>` -->
          <span v-for="(item, index) in row.similarQuestion" :key="index" class="sim-item">
            <!-- [zh] Mustache 插值表达式 -->
            {{ index + 1 }}. {{ item }}
          <!-- [zh] 闭合标签 `</span>` -->
          </span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="rag-time">{{ row.createTime }}</p>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="rag-ops">
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn sm" @click="showEdit(row)">修改</button>
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn sm danger" @click="delRag(row)">删除</button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="!loading" class="m-empty-tip">暂无问答</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="sentinel" class="m-sentinel">
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="loading">加载中…</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-else-if="finished && list.length">没有更多了</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<RagEdit>` -->
    <RagEdit ref="ragEditRef" @reload="reload" />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `@/views/setting/RagEdit.vue`
import RagEdit from '@/views/setting/RagEdit.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `searchForm`
const searchForm = reactive({ questionFuzzy: '' })
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
// [zh] 声明变量 `ragEditRef`
const ragEditRef = ref(null)
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
  // [zh] TypeScript/JS 语句：`pageNo.value += 1`
  pageNo.value += 1
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `result`
    const result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.loadRagQuestion,`
      url: proxy.Api.loadRagQuestion,
      // [zh] 代码块开始
      params: {
        // [zh] TypeScript/JS 语句：`pageNo: pageNo.value,`
        pageNo: pageNo.value,
        // [zh] TypeScript/JS 语句：`pageSize: 15,`
        pageSize: 15,
        // [zh] TypeScript/JS 语句：`...searchForm,`
        ...searchForm,
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 条件判断 if
    if (!result) {
      // [zh] TypeScript/JS 语句：`pageNo.value -= 1`
      pageNo.value -= 1
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
    // [zh] 声明变量 `data`
    const data = result.data || {}
    // [zh] TypeScript/JS 语句：`pageTotal.value = data.pageTotal || 1`
    pageTotal.value = data.pageTotal || 1
    // [zh] 声明变量 `rows`
    const rows = (data.list || []).map((item) => {
      // [zh] 条件判断 if
      if (item.similarQuestion && typeof item.similarQuestion === 'string') {
        // [zh] 代码块开始
        try {
          // [zh] TypeScript/JS 语句：`item.similarQuestion = JSON.parse(item.similarQuestion)`
          item.similarQuestion = JSON.parse(item.similarQuestion)
        // [zh] 代码块开始
        } catch {
          // [zh] TypeScript/JS 语句：`item.similarQuestion = []`
          item.similarQuestion = []
        // [zh] 语法符号
        }
      // [zh] 语法符号
      }
      // [zh] return 返回
      return item
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] TypeScript/JS 语句：`list.value.push(...rows)`
    list.value.push(...rows)
    // [zh] 条件判断 if
    if (pageNo.value >= pageTotal.value) finished.value = true
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

// [zh] 声明变量 `showEdit`
const showEdit = (data) => {
  // [zh] TypeScript/JS 语句：`ragEditRef.value.show(data)`
  ragEditRef.value.show(data)
// [zh] 语法符号
}

// [zh] 声明变量 `delRag`
const delRag = (data) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: '确定要删除吗?',`
    message: '确定要删除吗?',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.delRagQuestion,`
        url: proxy.Api.delRagQuestion,
        // [zh] TypeScript/JS 语句：`params: { questionId: data.questionId },`
        params: { questionId: data.questionId },
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('删除成功')`
      proxy.Message.success('删除成功')
      // [zh] TypeScript/JS 语句：`reload()`
      reload()
    // [zh] TypeScript/JS 语句：`},`
    },
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
/* [zh] 样式规则 `.rag-card {` */
.rag-card {
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.rag-question {` */
.rag-question {
  /* [zh] 样式规则 `margin: 0 0 8px;` */
  margin: 0 0 8px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: var(--m-ink);` */
  color: var(--m-ink);
  /* [zh] 样式规则 `line-height: 1.45;` */
  line-height: 1.45;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.rag-similar {` */
.rag-similar {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;

  /* [zh] 样式规则 `.sim-item {` */
  .sim-item {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.rag-time {` */
.rag-time {
  /* [zh] 样式规则 `margin: 0 0 10px;` */
  margin: 0 0 10px;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.rag-ops {` */
.rag-ops {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}
</style>
