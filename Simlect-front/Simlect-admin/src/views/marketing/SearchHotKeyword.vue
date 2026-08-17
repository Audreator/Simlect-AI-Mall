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

<script setup>
import { getCurrentInstance, reactive, ref } from 'vue'

const { proxy } = getCurrentInstance()
const tableData = ref({ list: [], pageNo: 1, pageTotal: 1 })
const form = reactive({ keyword: '', sort: 0, status: 1 })

const columns = [
  { label: '热搜词', prop: 'keyword' },
  { label: '排序', prop: 'sort', width: 100 },
  { label: '状态', prop: 'status', width: 100, scopedSlots: 'slotStatus' },
  { label: '更新时间', prop: 'updateTime', width: 180 },
  { label: '操作', prop: 'op', width: 140, scopedSlots: 'slotOp' },
]

const loadDataList = async () => {
  const result = await proxy.Request({ url: proxy.Api.searchHotKeywordLoadList })
  if (!result) return
  tableData.value = { list: result.data || [], pageNo: 1, pageTotal: 1 }
}

const resetForm = () => {
  form.keyword = ''
  form.sort = 0
  form.status = 1
}

const editRow = (row) => {
  form.keyword = row.keyword
  form.sort = row.sort ?? 0
  form.status = row.status ?? 1
}

const save = async () => {
  if (!form.keyword?.trim()) {
    proxy.Message.warning('请输入热搜词')
    return
  }
  const result = await proxy.Request({
    url: proxy.Api.searchHotKeywordSave,
    params: {
      keyword: form.keyword.trim(),
      sort: form.sort,
      status: form.status,
    },
    showLoading: true,
  })
  if (!result) return
  proxy.Message.success('保存成功')
  resetForm()
  loadDataList()
}

const delRow = (row) => {
  proxy.Confirm({
    message: `确定删除热搜词「${row.keyword}」吗？`,
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.searchHotKeywordDel,
        params: { keyword: row.keyword },
        showLoading: true,
      })
      if (!result) return
      proxy.Message.success('已删除')
      loadDataList()
    },
  })
}

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
