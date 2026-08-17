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
          <el-form-item label="敏感词">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input v-model="form.word" clearable placeholder="输入敏感词" maxlength="100" />
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="6">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="替换词">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input v-model="form.replaceWord" clearable placeholder="替换词(默认为***)" maxlength="100" />
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
        <el-col :span="6" class="form-actions">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" @click="save">{{ form.id ? '更新' : '新增' }}</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button @click="resetForm">重置</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="warning" @click="refreshCache">刷新缓存</el-button>
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
const form = reactive({ id: null, word: '', replaceWord: '', status: 1 })

const columns = [
  { label: '敏感词', prop: 'word' },
  { label: '替换词', prop: 'replaceWord', width: 120 },
  { label: '状态', prop: 'status', width: 100, scopedSlots: 'slotStatus' },
  { label: '创建时间', prop: 'createTime', width: 180 },
  { label: '更新时间', prop: 'updateTime', width: 180 },
  { label: '操作', prop: 'op', width: 140, scopedSlots: 'slotOp' },
]

const loadDataList = async (pageNo = 1) => {
  const result = await proxy.Request({
    url: proxy.Api.sensitiveWordList,
    params: { pageNo, pageSize: 15 },
  })
  if (!result) return
  tableData.value = result.data
}

const resetForm = () => {
  form.id = null
  form.word = ''
  form.replaceWord = ''
  form.status = 1
}

const editRow = (row) => {
  form.id = row.id
  form.word = row.word
  form.replaceWord = row.replaceWord
  form.status = row.status
}

const save = async () => {
  if (!form.word?.trim()) {
    proxy.Message.warning('请输入敏感词')
    return
  }
  const result = await proxy.Request({
    url: proxy.Api.sensitiveWordSave,
    params: {
      id: form.id,
      word: form.word.trim(),
      replaceWord: form.replaceWord?.trim() || '***',
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
    message: `确定删除敏感词「${row.word}」吗？`,
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.sensitiveWordDelete,
        params: { id: row.id },
        showLoading: true,
      })
      if (!result) return
      proxy.Message.success('已删除')
      loadDataList()
    },
  })
}

const refreshCache = async () => {
  const result = await proxy.Request({
    url: proxy.Api.sensitiveWordRefresh,
    showLoading: true,
  })
  if (!result) return
  proxy.Message.success('缓存已刷新')
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
