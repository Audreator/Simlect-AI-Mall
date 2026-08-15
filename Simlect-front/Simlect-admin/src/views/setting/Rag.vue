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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./RagEdit.vue`
import RagEdit from './RagEdit.vue'
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`ref,`
  ref,
  // [zh] TypeScript/JS 语句：`reactive,`
  reactive,
  // [zh] TypeScript/JS 语句：`getCurrentInstance,`
  getCurrentInstance,
  // [zh] TypeScript/JS 语句：`nextTick,`
  nextTick,
  // [zh] TypeScript/JS 语句：`onMounted,`
  onMounted,
  // [zh] TypeScript/JS 语句：`onUnmounted,`
  onUnmounted,
// [zh] TypeScript/JS 语句：`} from 'vue'`
} from 'vue'
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()

// [zh] 声明变量 `columns`
const columns = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '问题',`
    label: '问题',
    // [zh] TypeScript/JS 语句：`prop: 'question',`
    prop: 'question',
    // [zh] TypeScript/JS 语句：`width: 300,`
    width: 300,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '相似问题',`
    label: '相似问题',
    // [zh] TypeScript/JS 语句：`prop: 'question',`
    prop: 'question',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotSimilarQuestion',`
    scopedSlots: 'slotSimilarQuestion',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '创建时间',`
    label: '创建时间',
    // [zh] TypeScript/JS 语句：`prop: 'createTime',`
    prop: 'createTime',
    // [zh] TypeScript/JS 语句：`width: 200,`
    width: 200,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '操作',`
    label: '操作',
    // [zh] TypeScript/JS 语句：`prop: 'op',`
    prop: 'op',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotOperation',`
    scopedSlots: 'slotOperation',
    // [zh] TypeScript/JS 语句：`width: 100,`
    width: 100,
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `tableInfoRef`
const tableInfoRef = ref()
// [zh] 声明变量 `searchForm`
const searchForm = ref({})
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
  // [zh] TypeScript/JS 语句：`Object.assign(params, searchForm.value)`
  Object.assign(params, searchForm.value)
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadRagQuestion,`
    url: proxy.Api.loadRagQuestion,
    // [zh] TypeScript/JS 语句：`params: params,`
    params: params,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  result.data.list.map((item) => {
    // [zh] 条件判断 if
    if (item.similarQuestion) {
      // [zh] TypeScript/JS 语句：`item.similarQuestion = JSON.parse(item.similarQuestion)`
      item.similarQuestion = JSON.parse(item.similarQuestion)
    // [zh] 语法符号
    }
    // [zh] return 返回
    return item
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] TypeScript/JS 语句：`Object.assign(tableData.value, result.data)`
  Object.assign(tableData.value, result.data)
// [zh] 语法符号
}

// [zh] 声明变量 `ragEditRef`
const ragEditRef = ref()
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
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.delRagQuestion,`
        url: proxy.Api.delRagQuestion,
        // [zh] 代码块开始
        params: {
          // [zh] TypeScript/JS 语句：`questionId: data.questionId,`
          questionId: data.questionId,
        // [zh] TypeScript/JS 语句：`},`
        },
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) {
        // [zh] TypeScript/JS 语句：`return`
        return
      // [zh] 语法符号
      }
      // [zh] TypeScript/JS 语句：`proxy.Message.success('删除成功')`
      proxy.Message.success('删除成功')
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
/* [zh] 样式规则 `}` */
}
</style>
