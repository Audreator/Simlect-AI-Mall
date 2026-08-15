<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="search-panel">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form @submit.prevent>
      <!-- [zh] 开始标签 `<el-row>` -->
      <el-row :gutter="10">
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="8">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="热搜词">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input v-model="form.keyword" clearable placeholder="输入热搜词" maxlength="100" />
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="4">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="排序">
            <!-- [zh] 开始标签 `<el-input-number>` -->
            <el-input-number v-model="form.sort" :min="0" :max="9999" />
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="4">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="状态">
            <!-- [zh] 开始标签 `<el-select>` -->
            <el-select v-model="form.status" style="width: 100%">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="1" label="启用" />
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="0" label="停用" />
            <!-- [zh] 闭合标签 `</el-select>` -->
            </el-select>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="8" class="form-actions">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" @click="save">保存</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button @click="resetForm">重置</el-button>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
      <!-- [zh] 闭合标签 `</el-row>` -->
      </el-row>
    <!-- [zh] 闭合标签 `</el-form>` -->
    </el-form>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>

  <!-- [zh] 开始标签 `<el-card>` -->
  <el-card class="table-data-card">
    <!-- [zh] 开始标签 `<Table>` -->
    <Table :columns="columns" :dataSource="tableData" :fetch="loadDataList">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #slotStatus="{ row }">
        <!-- [zh] 开始标签 `<el-tag>` -->
        <el-tag v-if="row.status == 1" type="success">启用</el-tag>
        <!-- [zh] 开始标签 `<el-tag>` -->
        <el-tag v-else type="info">停用</el-tag>
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #slotOp="{ row }">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="list-op-panel">
          <!-- [zh] 开始标签 `<OpBtn>` -->
          <OpBtn icon="icon-edit" tips="编辑" @click="editRow(row)" />
          <!-- [zh] 开始标签 `<OpBtn>` -->
          <OpBtn icon="icon-delete" type="danger" tips="删除" @click="delRow(row)" />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      </template>
    </Table>
  </el-card>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { getCurrentInstance, reactive, ref } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `tableData`
const tableData = ref({ list: [], pageNo: 1, pageTotal: 1 })
// [zh] 声明变量 `form`
const form = reactive({ keyword: '', sort: 0, status: 1 })

// [zh] 声明变量 `columns`
const columns = [
  // [zh] TypeScript/JS 语句：`{ label: '热搜词', prop: 'keyword' },`
  { label: '热搜词', prop: 'keyword' },
  // [zh] TypeScript/JS 语句：`{ label: '排序', prop: 'sort', width: 100 },`
  { label: '排序', prop: 'sort', width: 100 },
  // [zh] TypeScript/JS 语句：`{ label: '状态', prop: 'status', width: 100, scopedSlots:`
  { label: '状态', prop: 'status', width: 100, scopedSlots: 'slotStatus' },
  // [zh] TypeScript/JS 语句：`{ label: '更新时间', prop: 'updateTime', width: 180 },`
  { label: '更新时间', prop: 'updateTime', width: 180 },
  // [zh] TypeScript/JS 语句：`{ label: '操作', prop: 'op', width: 140, scopedSlots: 'sl`
  { label: '操作', prop: 'op', width: 140, scopedSlots: 'slotOp' },
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `loadDataList`
const loadDataList = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({ url: proxy.Api.searchHotKeywordLoadList })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`tableData.value = { list: result.data || [], pageNo: 1,`
  tableData.value = { list: result.data || [], pageNo: 1, pageTotal: 1 }
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
// [zh] 语法符号
}

// [zh] 声明变量 `save`
const save = async () => {
  // [zh] 条件判断 if
  if (!form.keyword?.trim()) {
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
    // [zh] 代码块开始
    params: {
      // [zh] TypeScript/JS 语句：`keyword: form.keyword.trim(),`
      keyword: form.keyword.trim(),
      // [zh] TypeScript/JS 语句：`sort: form.sort,`
      sort: form.sort,
      // [zh] TypeScript/JS 语句：`status: form.status,`
      status: form.status,
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`showLoading: true,`
    showLoading: true,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`proxy.Message.success('保存成功')`
  proxy.Message.success('保存成功')
  // [zh] TypeScript/JS 语句：`resetForm()`
  resetForm()
  // [zh] TypeScript/JS 语句：`loadDataList()`
  loadDataList()
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
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.searchHotKeywordDel,`
        url: proxy.Api.searchHotKeywordDel,
        // [zh] TypeScript/JS 语句：`params: { keyword: row.keyword },`
        params: { keyword: row.keyword },
        // [zh] TypeScript/JS 语句：`showLoading: true,`
        showLoading: true,
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('已删除')`
      proxy.Message.success('已删除')
      // [zh] TypeScript/JS 语句：`loadDataList()`
      loadDataList()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] TypeScript/JS 语句：`loadDataList()`
loadDataList()
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `.form-actions {` */
.form-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-end;` */
  align-items: flex-end;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding-bottom: 4px;` */
  padding-bottom: 4px;
/* [zh] 样式规则 `}` */
}
</style>
