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
          <el-form-item label="问题">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input clearable placeholder="输入问题" v-model="searchForm.questionFuzzy"></el-input>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" @click="loadDataList">搜索</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="success" @click="showEdit()">新增</el-button>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
      <!-- [zh] 闭合标签 `</el-row>` -->
      </el-row>
    <!-- [zh] 闭合标签 `</el-form>` -->
    </el-form>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
  <!-- [zh] 开始标签 `<el-card>` -->
  <el-card>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="table-panel">
      <!-- [zh] 开始标签 `<Table>` -->
      <Table ref="tableInfoRef" :columns="columns" :fetch="loadDataList" :dataSource="tableData">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotSimilarQuestion="{ row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div v-for="(item,index) in row.similarQuestion">{{index+1}}、{{item}}</div>
        </template>
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotOperation="{ index, row }">
          <!-- [zh] 开始标签 `<a>` -->
          <a href="javascript:void(0)" class="a-link" @click="showEdit(row)">修改</a>
          <!-- [zh] 开始标签 `<el-divider>` -->
          <el-divider direction="vertical" />
          <!-- [zh] 开始标签 `<a>` -->
          <a href="javascript:void(0)" class="a-link" @click="delRag(row)">删除</a>
        </template>
      </Table>
    </div>
  </el-card>
  <RagEdit ref="ragEditRef" @reload="loadDataList"></RagEdit>
</template>

<script setup>
import RagEdit from './RagEdit.vue'
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  onMounted,
  onUnmounted,
} from 'vue'
import { useRouter } from 'vue-router'
const { proxy } = getCurrentInstance()

const columns = [
  {
    label: '问题',
    prop: 'question',
    width: 300,
  },
  {
    label: '相似问题',
    prop: 'question',
    scopedSlots: 'slotSimilarQuestion',
  },
  {
    label: '创建时间',
    prop: 'createTime',
    width: 200,
  },
  {
    label: '操作',
    prop: 'op',
    scopedSlots: 'slotOperation',
    width: 100,
  },
]

const tableInfoRef = ref()
const searchForm = ref({})
const tableData = ref({})
const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
  }
  Object.assign(params, searchForm.value)
  let result = await proxy.Request({
    url: proxy.Api.loadRagQuestion,
    params: params,
  })
  if (!result) {
    return
  }
  result.data.list.map((item) => {
    if (item.similarQuestion) {
      item.similarQuestion = JSON.parse(item.similarQuestion)
    }
    return item
  })
  Object.assign(tableData.value, result.data)
}

const ragEditRef = ref()
const showEdit = (data) => {
  ragEditRef.value.show(data)
}

const delRag = (data) => {
  proxy.Confirm({
    message: '确定要删除吗?',
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.delRagQuestion,
        params: {
          questionId: data.questionId,
        },
      })
      if (!result) {
        return
      }
      proxy.Message.success('删除成功')
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
/* [zh] 样式规则 `}` */
}
</style>
