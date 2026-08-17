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

<script setup>
import RagEdit from '@/views/setting/RagEdit.vue'
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from 'vue'

const { proxy } = getCurrentInstance()
const searchForm = reactive({ questionFuzzy: '' })
const list = ref([])
const pageNo = ref(0)
const pageTotal = ref(1)
const loading = ref(false)
const finished = ref(false)
const sentinel = ref(null)
const ragEditRef = ref(null)
let observer = null

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
  pageNo.value += 1
  try {
    const result = await proxy.Request({
      url: proxy.Api.loadRagQuestion,
      params: {
        pageNo: pageNo.value,
        pageSize: 15,
        ...searchForm,
      },
    })
    if (!result) {
      pageNo.value -= 1
      return
    }
    const data = result.data || {}
    pageTotal.value = data.pageTotal || 1
    const rows = (data.list || []).map((item) => {
      if (item.similarQuestion && typeof item.similarQuestion === 'string') {
        try {
          item.similarQuestion = JSON.parse(item.similarQuestion)
        } catch {
          item.similarQuestion = []
        }
      }
      return item
    })
    list.value.push(...rows)
    if (pageNo.value >= pageTotal.value) finished.value = true
  } finally {
    loading.value = false
  }
}

const reload = () => loadList(true)

const showEdit = (data) => {
  ragEditRef.value.show(data)
}

const delRag = (data) => {
  proxy.Confirm({
    message: '确定要删除吗?',
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.delRagQuestion,
        params: { questionId: data.questionId },
      })
      if (!result) return
      proxy.Message.success('删除成功')
      reload()
    },
  })
}

onMounted(() => {
  loadList(true)
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) loadList()
    },
    { rootMargin: '0px 0px 300px 0px' }
  )
  if (sentinel.value) observer.observe(sentinel.value)
})

onUnmounted(() => {
  observer && observer.disconnect()
  observer = null
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
