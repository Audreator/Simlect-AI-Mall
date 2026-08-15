<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<section>` -->
    <section class="glass-card m-form">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="m-form-title">{{ editing ? '编辑热搜词' : '新增热搜词' }}</h3>
      <!-- [zh] 开始标签 `<input>` -->
      <input v-model="form.keyword" class="m-input" placeholder="输入热搜词" maxlength="100" />
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-form-row">
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-label">排序</label>
        <!-- [zh] 开始标签 `<input>` -->
        <input v-model.number="form.sort" class="m-input small" type="number" min="0" />
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-label">状态</label>
        <!-- [zh] 开始标签 `<select>` -->
        <select v-model="form.status" class="m-input small">
          <!-- [zh] 开始标签 `<option>` -->
          <option :value="1">启用</option>
          <!-- [zh] 开始标签 `<option>` -->
          <option :value="0">停用</option>
        <!-- [zh] 闭合标签 `</select>` -->
        </select>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-form-ops">
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn primary" @click="save">保存</button>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn" @click="resetForm">重置</button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="row in list" :key="row.keyword" class="glass-card hot-row">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="hot-info">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="hot-word">{{ row.keyword }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="hot-meta">排序 {{ row.sort }} · {{ row.updateTime }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="m-tag" :class="row.status == 1 ? 'green' : 'muted'">{{ row.status == 1 ? '启用' : '停用' }}</span>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="hot-op" @click="editRow(row)">编辑</button>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="hot-op danger" @click="delRow(row)">删除</button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else class="m-empty-tip">暂无热搜词</p>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, onMounted } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `list`
const list = ref([])
// [zh] 声明变量 `editing`
const editing = ref(false)
// [zh] 声明变量 `form`
const form = reactive({ keyword: '', sort: 0, status: 1 })

// [zh] 声明变量 `loadList`
const loadList = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({ url: proxy.Api.searchHotKeywordLoadList, showLoading: false })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`list.value = result.data || []`
  list.value = result.data || []
// [zh] 语法符号
}

// [zh] 声明变量 `resetForm`
const resetForm = () => {
  // [zh] TypeScript/JS 语句：`form.keyword = ''`
  form.keyword = ''
  // [zh] TypeScript/JS 语句：`form.sort = 0`
  form.sort = 0
  // [zh] TypeScript/JS 语句：`form.status = 1`
  form.status = 1
  // [zh] TypeScript/JS 语句：`editing.value = false`
  editing.value = false
// [zh] 语法符号
}

// [zh] 声明变量 `editRow`
const editRow = (row) => {
  // [zh] TypeScript/JS 语句：`form.keyword = row.keyword`
  form.keyword = row.keyword
  // [zh] TypeScript/JS 语句：`form.sort = row.sort ?? 0`
  form.sort = row.sort ?? 0
  // [zh] TypeScript/JS 语句：`form.status = row.status ?? 1`
  form.status = row.status ?? 1
  // [zh] TypeScript/JS 语句：`editing.value = true`
  editing.value = true
// [zh] 语法符号
}

// [zh] 声明变量 `save`
const save = async () => {
  // [zh] 条件判断 if
  if (!form.keyword || !form.keyword.trim()) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请输入热搜词')`
    proxy.Message.warning('请输入热搜词')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.searchHotKeywordSave,`
    url: proxy.Api.searchHotKeywordSave,
    // [zh] TypeScript/JS 语句：`params: { keyword: form.keyword.trim(), sort: form.sort`
    params: { keyword: form.keyword.trim(), sort: form.sort, status: form.status },
    // [zh] TypeScript/JS 语句：`showLoading: true`
    showLoading: true
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`proxy.Message.success('保存成功')`
  proxy.Message.success('保存成功')
  // [zh] TypeScript/JS 语句：`resetForm()`
  resetForm()
  // [zh] TypeScript/JS 语句：`loadList()`
  loadList()
// [zh] 语法符号
}

// [zh] 声明变量 `delRow`
const delRow = (row) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定删除热搜词「${row.keyword}」吗？`,`
    message: `确定删除热搜词「${row.keyword}」吗？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({ url: proxy.Api.searchHotKeywordDel, params: { keyword: row.keyword } })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('已删除')`
      proxy.Message.success('已删除')
      // [zh] TypeScript/JS 语句：`loadList()`
      loadList()
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] TypeScript/JS 语句：`onMounted(loadList)`
onMounted(loadList)
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

/* [zh] 样式规则 `.m-form {` */
.m-form {
  /* [zh] 样式规则 `padding: 14px;` */
  padding: 14px;

  /* [zh] 样式规则 `.m-form-title {` */
  .m-form-title {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-input {` */
.m-input {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 40px;` */
  height: 40px;
  /* [zh] 样式规则 `padding: 0 12px;` */
  padding: 0 12px;
  /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
  border: 1px solid rgba(120, 120, 128, 0.24);
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.6);` */
  background: rgba(255, 255, 255, 0.6);
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: var(--m-ink);` */
  color: var(--m-ink);
  /* [zh] 样式规则 `outline: none;` */
  outline: none;

  /* [zh] 样式规则 `&.small {` */
  &.small {
    /* [zh] 样式规则 `width: 84px;` */
    width: 84px;
    /* [zh] 样式规则 `height: 36px;` */
    height: 36px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-form-row {` */
.m-form-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-top: 10px;` */
  margin-top: 10px;

  /* [zh] 样式规则 `.m-label {` */
  .m-label {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-form-ops {` */
.m-form-ops {
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
  /* [zh] 样式规则 `height: 38px;` */
  height: 38px;
  /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
  border: 1px solid rgba(120, 120, 128, 0.24);
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.6);` */
  background: rgba(255, 255, 255, 0.6);
  /* [zh] 样式规则 `color: var(--m-ink-2);` */
  color: var(--m-ink-2);
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

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

/* [zh] 样式规则 `.m-list {` */
.m-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.hot-row {` */
.hot-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;

  /* [zh] 样式规则 `.hot-info {` */
  .hot-info {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;

    /* [zh] 样式规则 `.hot-word {` */
    .hot-word {
      /* [zh] 样式规则 `font-size: 14px;` */
      font-size: 14px;
      /* [zh] 样式规则 `color: var(--m-ink);` */
      color: var(--m-ink);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.hot-meta {` */
    .hot-meta {
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.hot-op {` */
  .hot-op {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;

    /* [zh] 样式规则 `&.danger {` */
    &.danger {
      /* [zh] 样式规则 `color: var(--m-danger);` */
      color: var(--m-danger);
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

  /* [zh] 样式规则 `&.muted {` */
  &.muted {
    /* [zh] 样式规则 `background: rgba(120, 120, 128, 0.16);` */
    background: rgba(120, 120, 128, 0.16);
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
  /* [zh] 样式规则 `}` */
  }
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
