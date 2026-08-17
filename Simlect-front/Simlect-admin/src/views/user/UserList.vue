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

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()

const SEX_MAP = {
  0: '女',
  1: '男',
  2: '保密',
}

const columns = [
  {
    label: '用户ID',
    prop: 'userId',
    width: 220,
  },
  {
    label: '头像',
    prop: 'avatar',
    scopedSlots: 'slotAvatar',
  },
  {
    label: '昵称',
    prop: 'nickName',
    scopedSlots: 'slotNickName',
  },
  {
    label: '邮箱',
    prop: 'email',
  },
  {
    label: '加入时间',
    prop: 'joinTime',
    scopedSlots: 'slotJoinTime',
  },
  {
    label: '最后登录IP',
    prop: 'lastLoginIp',
  },
  {
    label: '状态',
    prop: 'status',
    scopedSlots: 'slotStatus',
  },
  {
    label: '操作',
    prop: 'operation',
    width: 80,
    scopedSlots: 'slotOperation',
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
    url: proxy.Api.loadUser,
    params: params,
  })
  if (!result) {
    return
  }
  Object.assign(tableData.value, result.data)
}

const changeStatus = (row) => {
  proxy.Confirm({
    message: `确定要${row.status == 0 ? '启用' : '禁用'}吗？`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.changeStatus,
        params: {
          userId: row.userId,
          status: row.status == 0 ? 1 : 0,
        },
      })
      if (!result) {
        return
      }
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
/* [zh] 样式规则 `}` */
}
</style>
