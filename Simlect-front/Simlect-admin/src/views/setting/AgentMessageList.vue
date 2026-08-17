<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="search-panel">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="searchForm" @submit.prevent>
      <!-- [zh] 开始标签 `<el-row>` -->
      <el-row :gutter="10">
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="用户ID">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input v-model="searchForm.userId" clearable placeholder="用户ID" />
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="业务类型">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input v-model="searchForm.bizType" clearable placeholder="如 product_search" />
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" @click="loadDataList">搜索</el-button>
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
    <Table ref="tableRef" :columns="columns" :fetch="loadDataList" :dataSource="tableData">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #slotStatus="{ row }">
        <!-- [zh] 开始标签 `<el-tag>` -->
        <el-tag v-if="row.status === 0" type="info">已取消</el-tag>
        <!-- [zh] 开始标签 `<el-tag>` -->
        <el-tag v-else-if="row.status === 1" type="warning">回答中</el-tag>
        <!-- [zh] 开始标签 `<el-tag>` -->
        <el-tag v-else type="success">完成</el-tag>
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #slotUser="{ row }">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="msg-line user">{{ row.userMessage || '—' }}</p>
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #slotAi="{ row }">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="msg-line ai">{{ clipText(row.assistantMessage) }}</p>
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #slotOp="{ row }">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="list-op-panel">
          <!-- [zh] 开始标签 `<OpBtn>` -->
          <OpBtn icon="icon-delete" type="danger" tips="删除" @click="delRow(row)" />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      </template>
    </Table>
  </el-card>
</template>

<script setup>
import { getCurrentInstance, ref } from 'vue'

const { proxy } = getCurrentInstance()
const tableRef = ref()
const tableData = ref({})
const searchForm = ref({ userId: '', bizType: '' })

const clipText = (text) => {
  if (!text) return '—'
  const s = String(text)
  return s.length > 120 ? `${s.slice(0, 120)}…` : s
}

const columns = [
  { label: 'ID', prop: 'messageId', width: 80 },
  { label: '用户ID', prop: 'userId', width: 120 },
  { label: '状态', prop: 'status', width: 90, scopedSlots: 'slotStatus' },
  { label: '业务类型', prop: 'bizType', width: 140 },
  { label: '用户消息', prop: 'userMessage', scopedSlots: 'slotUser' },
  { label: 'AI回复', prop: 'assistantMessage', scopedSlots: 'slotAi' },
  { label: '时间', prop: 'sendTime', width: 170 },
  { label: '操作', prop: 'op', width: 80, scopedSlots: 'slotOp' },
]

const loadDataList = async () => {
  const params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
    orderBy: 'send_time desc',
  }
  if (searchForm.value.userId) params.userId = searchForm.value.userId
  if (searchForm.value.bizType) params.bizType = searchForm.value.bizType
  const result = await proxy.Request({
    url: proxy.Api.agentMessageLoadList,
    params,
  })
  if (!result) return
  Object.assign(tableData.value, result.data)
}

const delRow = (row) => {
  proxy.Confirm({
    message: `确定删除对话记录 #${row.messageId} 吗？`,
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.agentMessageDelete,
        params: { messageId: row.messageId },
        showLoading: true,
      })
      if (!result) return
      proxy.Message.success('已删除')
      loadDataList()
    },
  })
}
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `.msg-line {` */
.msg-line {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.45;` */
  line-height: 1.45;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;
  /* [zh] 样式规则 `word-break: break-word;` */
  word-break: break-word;

  /* [zh] 样式规则 `&.user {` */
  &.user {
    /* [zh] 样式规则 `color: #333;` */
    color: #333;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.ai {` */
  &.ai {
    /* [zh] 样式规则 `color: #666;` */
    color: #666;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.list-op-panel {` */
.list-op-panel {
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
/* [zh] 样式规则 `}` */
}
</style>
