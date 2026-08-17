<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="top-panel">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="searchForm" @submit.prevent>
      <!-- [zh] 开始标签 `<el-row>` -->
      <el-row :gutter="10">
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="举报理由">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input clearable placeholder="输入举报理由" v-model="searchForm.reasonFuzzy"></el-input>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="状态">
            <!-- [zh] 开始标签 `<el-select>` -->
            <el-select clearable placeholder="全部" v-model="searchForm.status">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option label="待处理" :value="0" />
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option label="已处理" :value="1" />
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option label="已驳回" :value="2" />
            <!-- [zh] 闭合标签 `</el-select>` -->
            </el-select>
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
    <!-- [zh] 开始标签 `<div>` -->
    <div class="table-panel">
      <!-- [zh] 开始标签 `<Table>` -->
      <Table ref="tableInfoRef" :columns="columns" :fetch="loadDataList" :dataSource="tableData">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotCommentSnapshot="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="snapshot-text">{{ row.commentSnapshot || '--' }}</div>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotStatus="{ index, row }">
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status === 0" type="warning" size="small">待处理</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-else-if="row.status === 1" type="success" size="small">已处理</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-else-if="row.status === 2" type="info" size="small">已驳回</el-tag>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotOperation="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="list-op-panel">
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn
              v-if="row.status === 0"
              icon="icon-edit"
              tips="处理"
              @click="handleHandler(row)"
            />
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn icon="icon-delete" type="danger" tips="删除" @click="delReport(row)" />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>
      </Table>
    </div>
  </el-card>
  <HandleReport ref="handleRef" @reload="loadDataList"></HandleReport>
</template>

<script setup>
import HandleReport from './HandleReport.vue'
import { ref, reactive, getCurrentInstance } from 'vue'

const { proxy } = getCurrentInstance()

const columns = [
  {
    label: '订单号',
    prop: 'orderId',
    width: 180,
  },
  {
    label: '举报人',
    prop: 'reporterUserId',
    width: 120,
  },
  {
    label: '举报理由',
    prop: 'reason',
    width: 120,
  },
  {
    label: '补充说明',
    prop: 'detail',
    width: 180,
  },
  {
    label: '评论快照',
    scopedSlots: 'slotCommentSnapshot',
  },
  {
    label: '状态',
    prop: 'status',
    scopedSlots: 'slotStatus',
    width: 90,
  },
  {
    label: '举报时间',
    prop: 'reportTime',
    width: 160,
  },
  {
    label: '操作',
    prop: 'operation',
    width: 120,
    scopedSlots: 'slotOperation',
  },
]

const tableInfoRef = ref()
const searchForm = reactive({
  reasonFuzzy: '',
  status: undefined,
})
const tableData = ref({})

const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
  }
  if (searchForm.reasonFuzzy) params.reasonFuzzy = searchForm.reasonFuzzy
  if (searchForm.status !== undefined && searchForm.status !== '') params.status = searchForm.status
  let result = await proxy.Request({
    url: proxy.Api.loadCommentReport,
    params: params,
  })
  if (!result) return
  Object.assign(tableData.value, result.data)
}

const handleRef = ref()
const handleHandler = (row) => {
  handleRef.value.show(row)
}

const delReport = (row) => {
  proxy.Confirm({
    message: `确定要删除该举报记录吗？`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.deleteCommentReport,
        params: { reportId: row.reportId },
      })
      if (!result) return
      proxy.Message.success('操作成功')
      loadDataList()
    },
  })
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.table-panel {` */
.table-panel {
  /* [zh] 样式规则 `height: calc(100vh - 135px);` */
  height: calc(100vh - 135px);

  /* [zh] 样式规则 `.snapshot-text {` */
  .snapshot-text {
    /* [zh] 样式规则 `max-width: 300px;` */
    max-width: 300px;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
