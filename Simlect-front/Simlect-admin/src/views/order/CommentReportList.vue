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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./HandleReport.vue`
import HandleReport from './HandleReport.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()

// [zh] 声明变量 `columns`
const columns = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '订单号',`
    label: '订单号',
    // [zh] TypeScript/JS 语句：`prop: 'orderId',`
    prop: 'orderId',
    // [zh] TypeScript/JS 语句：`width: 180,`
    width: 180,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '举报人',`
    label: '举报人',
    // [zh] TypeScript/JS 语句：`prop: 'reporterUserId',`
    prop: 'reporterUserId',
    // [zh] TypeScript/JS 语句：`width: 120,`
    width: 120,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '举报理由',`
    label: '举报理由',
    // [zh] TypeScript/JS 语句：`prop: 'reason',`
    prop: 'reason',
    // [zh] TypeScript/JS 语句：`width: 120,`
    width: 120,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '补充说明',`
    label: '补充说明',
    // [zh] TypeScript/JS 语句：`prop: 'detail',`
    prop: 'detail',
    // [zh] TypeScript/JS 语句：`width: 180,`
    width: 180,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '评论快照',`
    label: '评论快照',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotCommentSnapshot',`
    scopedSlots: 'slotCommentSnapshot',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '状态',`
    label: '状态',
    // [zh] TypeScript/JS 语句：`prop: 'status',`
    prop: 'status',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotStatus',`
    scopedSlots: 'slotStatus',
    // [zh] TypeScript/JS 语句：`width: 90,`
    width: 90,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '举报时间',`
    label: '举报时间',
    // [zh] TypeScript/JS 语句：`prop: 'reportTime',`
    prop: 'reportTime',
    // [zh] TypeScript/JS 语句：`width: 160,`
    width: 160,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '操作',`
    label: '操作',
    // [zh] TypeScript/JS 语句：`prop: 'operation',`
    prop: 'operation',
    // [zh] TypeScript/JS 语句：`width: 120,`
    width: 120,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotOperation',`
    scopedSlots: 'slotOperation',
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `tableInfoRef`
const tableInfoRef = ref()
// [zh] 声明变量 `searchForm`
const searchForm = reactive({
  // [zh] TypeScript/JS 语句：`reasonFuzzy: '',`
  reasonFuzzy: '',
  // [zh] TypeScript/JS 语句：`status: undefined,`
  status: undefined,
// [zh] TypeScript/JS 语句：`})`
})
// [zh] 声明变量 `tableData`
const tableData = ref({})

// [zh] 声明变量 `loadDataList`
const loadDataList = async () => {
  // [zh] 声明变量 `params`
  let params = {
    // [zh] TypeScript/JS 语句：`pageNo: tableData.value.pageNo,`
    pageNo: tableData.value.pageNo,
    // [zh] TypeScript/JS 语句：`pageSize: tableData.value.pageSize,`
    pageSize: tableData.value.pageSize,
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (searchForm.reasonFuzzy) params.reasonFuzzy = searchForm.reasonFuzzy
  // [zh] 条件判断 if
  if (searchForm.status !== undefined && searchForm.status !== '') params.status = searchForm.status
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadCommentReport,`
    url: proxy.Api.loadCommentReport,
    // [zh] TypeScript/JS 语句：`params: params,`
    params: params,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`Object.assign(tableData.value, result.data)`
  Object.assign(tableData.value, result.data)
// [zh] 语法符号
}

// [zh] 声明变量 `handleRef`
const handleRef = ref()
// [zh] 声明变量 `handleHandler`
const handleHandler = (row) => {
  // [zh] TypeScript/JS 语句：`handleRef.value.show(row)`
  handleRef.value.show(row)
// [zh] 语法符号
}

// [zh] 声明变量 `delReport`
const delReport = (row) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定要删除该举报记录吗？`,`
    message: `确定要删除该举报记录吗？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.deleteCommentReport,`
        url: proxy.Api.deleteCommentReport,
        // [zh] TypeScript/JS 语句：`params: { reportId: row.reportId },`
        params: { reportId: row.reportId },
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('操作成功')`
      proxy.Message.success('操作成功')
      // [zh] TypeScript/JS 语句：`loadDataList()`
      loadDataList()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
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
