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
          <el-form-item label="统计日期">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input v-model="searchForm.statisticsDate" clearable placeholder="如 2026-05-30" />
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="数据类型">
            <!-- [zh] 开始标签 `<el-select>` -->
            <el-select v-model="searchForm.dataType" clearable placeholder="全部" style="width: 100%">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option v-for="item in dataTypeOptions" :key="item.type" :value="item.type" :label="item.desc" />
            <!-- [zh] 闭合标签 `</el-select>` -->
            </el-select>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="8" class="toolbar-actions">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" @click="loadDataList">搜索</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="warning" plain @click="syncStatistics">手动同步统计</el-button>
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
      <template #slotType="{ row }">
        <!-- [zh] Mustache 插值表达式 -->
        {{ dataTypeLabel(row.dataType) }}
      </template>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #slotValue="{ row }">
        <!-- [zh] Mustache 插值表达式 -->
        {{ row.dataValue }}
      </template>
    </Table>
  </el-card>
</template>

<script setup>
import { getCurrentInstance, ref } from 'vue'

const { proxy } = getCurrentInstance()
const tableRef = ref()
const tableData = ref({})
const searchForm = ref({ statisticsDate: '', dataType: null })

const dataTypeOptions = [
  { type: 1, desc: '销售金额' },
  { type: 2, desc: '订单数量' },
  { type: 3, desc: '退款金额' },
  { type: 4, desc: '退款数量' },
]

const dataTypeLabel = (type) => dataTypeOptions.find((i) => i.type === type)?.desc || type

const columns = [
  { label: '统计日期', prop: 'statisticsDate', width: 140 },
  { label: '数据类型', prop: 'dataType', width: 120, scopedSlots: 'slotType' },
  { label: '数值', prop: 'dataValue', scopedSlots: 'slotValue' },
]

const loadDataList = async () => {
  const params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
  }
  if (searchForm.value.statisticsDate) {
    params.statisticsDate = searchForm.value.statisticsDate
  }
  if (searchForm.value.dataType != null && searchForm.value.dataType !== '') {
    params.dataType = searchForm.value.dataType
  }
  const result = await proxy.Request({
    url: proxy.Api.statisticsInfoLoadList,
    params,
  })
  if (!result) return
  Object.assign(tableData.value, result.data)
}

const syncStatistics = () => {
  proxy.Confirm({
    message: '将重新计算并写入统计数据，确定继续？',
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.toolStatistics,
        showLoading: true,
      })
      if (!result) return
      proxy.Message.success('同步成功')
      loadDataList()
    },
  })
}
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `.toolbar-actions {` */
.toolbar-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
/* [zh] 样式规则 `}` */
}
</style>
