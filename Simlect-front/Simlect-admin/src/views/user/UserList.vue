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
          <el-form-item label="用户昵称">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input clearable placeholder="输入用户昵称" v-model="searchForm.nickNameFuzzy"></el-input>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="状态" prop="">
            <!-- [zh] 开始标签 `<el-select>` -->
            <el-select clearable placeholder="请选择状态" v-model="searchForm.status">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="0" label="禁用"></el-option>
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="1" label="启用"></el-option>
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
        <template #slotAvatar="{ index, row }">
          <!-- [zh] 开始标签 `<Avatar>` -->
          <Avatar :avatar="row.avatar || undefined" :width="50"></Avatar>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotNickName="{ index, row }">
          <!-- [zh] Mustache 插值表达式 -->
          {{row.nickName}} ({{SEX_MAP[row.sex]||"未知"}})
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotJoinTime="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div>加入时间：{{ row.joinTime }}</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div>最后登录时间：{{ row.lastLoginTime }}</div>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotStatus="{ index, row }">
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status == 0" effect="dark" type="danger">已禁用</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status == 1" effect="dark" type="success">正常</el-tag>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotOperation="{ index, row }">
          <!-- [zh] 开始标签 `<a>` -->
          <a href="javascript:void(0)" class="a-link" @click="changeStatus(row)">{{ row.status == 0 ? "启用" : "禁用" }}</a>
        </template>
      </Table>
    </div>
  </el-card>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()

// [zh] 声明变量 `SEX_MAP`
const SEX_MAP = {
  // [zh] TypeScript/JS 语句：`0: '女',`
  0: '女',
  // [zh] TypeScript/JS 语句：`1: '男',`
  1: '男',
  // [zh] TypeScript/JS 语句：`2: '保密',`
  2: '保密',
// [zh] 语法符号
}

// [zh] 声明变量 `columns`
const columns = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '用户ID',`
    label: '用户ID',
    // [zh] TypeScript/JS 语句：`prop: 'userId',`
    prop: 'userId',
    // [zh] TypeScript/JS 语句：`width: 220,`
    width: 220,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '头像',`
    label: '头像',
    // [zh] TypeScript/JS 语句：`prop: 'avatar',`
    prop: 'avatar',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotAvatar',`
    scopedSlots: 'slotAvatar',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '昵称',`
    label: '昵称',
    // [zh] TypeScript/JS 语句：`prop: 'nickName',`
    prop: 'nickName',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotNickName',`
    scopedSlots: 'slotNickName',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '邮箱',`
    label: '邮箱',
    // [zh] TypeScript/JS 语句：`prop: 'email',`
    prop: 'email',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '加入时间',`
    label: '加入时间',
    // [zh] TypeScript/JS 语句：`prop: 'joinTime',`
    prop: 'joinTime',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotJoinTime',`
    scopedSlots: 'slotJoinTime',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '最后登录IP',`
    label: '最后登录IP',
    // [zh] TypeScript/JS 语句：`prop: 'lastLoginIp',`
    prop: 'lastLoginIp',
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
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '操作',`
    label: '操作',
    // [zh] TypeScript/JS 语句：`prop: 'operation',`
    prop: 'operation',
    // [zh] TypeScript/JS 语句：`width: 80,`
    width: 80,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotOperation',`
    scopedSlots: 'slotOperation',
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
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadUser,`
    url: proxy.Api.loadUser,
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
  // [zh] TypeScript/JS 语句：`Object.assign(tableData.value, result.data)`
  Object.assign(tableData.value, result.data)
// [zh] 语法符号
}

// [zh] 声明变量 `changeStatus`
const changeStatus = (row) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定要${row.status == 0 ? '启用' : '禁用'}吗？`,`
    message: `确定要${row.status == 0 ? '启用' : '禁用'}吗？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.changeStatus,`
        url: proxy.Api.changeStatus,
        // [zh] 代码块开始
        params: {
          // [zh] TypeScript/JS 语句：`userId: row.userId,`
          userId: row.userId,
          // [zh] TypeScript/JS 语句：`status: row.status == 0 ? 1 : 0,`
          status: row.status == 0 ? 1 : 0,
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
/* [zh] 样式规则 `}` */
}
</style>
