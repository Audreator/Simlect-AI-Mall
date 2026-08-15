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
          <el-form-item label="收货人">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input v-model="searchForm.addresseeFuzzy" clearable placeholder="收货人" />
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="手机号">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input v-model="searchForm.phoneFuzzy" clearable placeholder="手机号" />
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
      <template #slotDefault="{ row }">
        <!-- [zh] 开始标签 `<el-tag>` -->
        <el-tag v-if="row.defaultType == 1" type="success" size="small">默认</el-tag>
        <!-- [zh] 开始标签 `<span>` -->
        <span v-else class="text-muted">—</span>
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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { getCurrentInstance, ref } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `tableRef`
const tableRef = ref()
// [zh] 声明变量 `tableData`
const tableData = ref({})
// [zh] 声明变量 `searchForm`
const searchForm = ref({ userId: '', addresseeFuzzy: '', phoneFuzzy: '' })

// [zh] 声明变量 `columns`
const columns = [
  // [zh] TypeScript/JS 语句：`{ label: '地址ID', prop: 'addressId', width: 150 },`
  { label: '地址ID', prop: 'addressId', width: 150 },
  // [zh] TypeScript/JS 语句：`{ label: '用户ID', prop: 'userId', width: 120 },`
  { label: '用户ID', prop: 'userId', width: 120 },
  // [zh] TypeScript/JS 语句：`{ label: '收货人', prop: 'addressee', width: 100 },`
  { label: '收货人', prop: 'addressee', width: 100 },
  // [zh] TypeScript/JS 语句：`{ label: '手机号', prop: 'phone', width: 130 },`
  { label: '手机号', prop: 'phone', width: 130 },
  // [zh] TypeScript/JS 语句：`{ label: '详细地址', prop: 'address' },`
  { label: '详细地址', prop: 'address' },
  // [zh] TypeScript/JS 语句：`{ label: '默认', prop: 'defaultType', width: 80, scopedSl`
  { label: '默认', prop: 'defaultType', width: 80, scopedSlots: 'slotDefault' },
  // [zh] TypeScript/JS 语句：`{ label: '操作', prop: 'op', width: 80, scopedSlots: 'slo`
  { label: '操作', prop: 'op', width: 80, scopedSlots: 'slotOp' },
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `loadDataList`
const loadDataList = async () => {
  // [zh] 声明变量 `params`
  const params = {
    // [zh] TypeScript/JS 语句：`pageNo: tableData.value.pageNo,`
    pageNo: tableData.value.pageNo,
    // [zh] TypeScript/JS 语句：`pageSize: tableData.value.pageSize,`
    pageSize: tableData.value.pageSize,
    // [zh] TypeScript/JS 语句：`orderBy: 'address_id desc',`
    orderBy: 'address_id desc',
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`Object.assign(params, searchForm.value)`
  Object.assign(params, searchForm.value)
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.userAddressLoadList,`
    url: proxy.Api.userAddressLoadList,
    // [zh] TypeScript/JS 语句：`params,`
    params,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`Object.assign(tableData.value, result.data)`
  Object.assign(tableData.value, result.data)
// [zh] 语法符号
}

// [zh] 声明变量 `delRow`
const delRow = (row) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定删除用户 ${row.userId} 的地址吗？`,`
    message: `确定删除用户 ${row.userId} 的地址吗？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.userAddressDelete,`
        url: proxy.Api.userAddressDelete,
        // [zh] TypeScript/JS 语句：`params: { addressId: row.addressId },`
        params: { addressId: row.addressId },
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
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `.text-muted {` */
.text-muted {
  /* [zh] 样式规则 `color: #999;` */
  color: #999;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
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
