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

<script setup>
import { getCurrentInstance, ref } from 'vue'

const { proxy } = getCurrentInstance()
const tableRef = ref()
const tableData = ref({})
const searchForm = ref({ userId: '', addresseeFuzzy: '', phoneFuzzy: '' })

const columns = [
  { label: '地址ID', prop: 'addressId', width: 150 },
  { label: '用户ID', prop: 'userId', width: 120 },
  { label: '收货人', prop: 'addressee', width: 100 },
  { label: '手机号', prop: 'phone', width: 130 },
  { label: '详细地址', prop: 'address' },
  { label: '默认', prop: 'defaultType', width: 80, scopedSlots: 'slotDefault' },
  { label: '操作', prop: 'op', width: 80, scopedSlots: 'slotOp' },
]

const loadDataList = async () => {
  const params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
    orderBy: 'address_id desc',
  }
  Object.assign(params, searchForm.value)
  const result = await proxy.Request({
    url: proxy.Api.userAddressLoadList,
    params,
  })
  if (!result) return
  Object.assign(tableData.value, result.data)
}

const delRow = (row) => {
  proxy.Confirm({
    message: `确定删除用户 ${row.userId} 的地址吗？`,
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.userAddressDelete,
        params: { addressId: row.addressId },
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
