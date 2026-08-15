<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Table>` -->
  <Table ref="tableInfoRef" :columns="columns" :fetch="loadDataList" :dataSource="tableData" :showPagination="false">
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #slotOrder="{ index, row }">
      <!-- [zh] 开始标签 `<OrderItem>` -->
      <OrderItem :data="row" @delivery="deliveryHandler"></OrderItem>
    </template>
  </Table>
  <Delivery ref="deliveryRef" @reload="loadDataList"></Delivery>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick } from "vue"
// [zh] 语句
const { proxy } = getCurrentInstance();
// [zh] 导入 `@/views/order/Delivery.vue`
import Delivery from '@/views/order/Delivery.vue'
// [zh] 导入 `@/views/order/OrderItem.vue`
import OrderItem from '@/views/order/OrderItem.vue'
// [zh] 声明变量 `columns`
const columns = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '待发货信息',`
    label: '待发货信息',
    // [zh] TypeScript/JS 语句：`prop: 'order',`
    prop: 'order',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotOrder',`
    scopedSlots: 'slotOrder',
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`]`
]
// [zh] 声明变量 `tableInfoRef`
const tableInfoRef = ref()
// [zh] 声明变量 `searchForm`
const searchForm = ref({})
// [zh] 声明变量 `tableData`
const tableData = ref({ pageNo: 1, pageSize: 4 })
// [zh] 声明变量 `loadDataList`
const loadDataList = async () => {
  // [zh] 声明变量 `params`
  let params = {
    // [zh] TypeScript/JS 语句：`pageNo: tableData.value.pageNo || 1,`
    pageNo: tableData.value.pageNo || 1,
    // [zh] TypeScript/JS 语句：`pageSize: tableData.value.pageSize || 4,`
    pageSize: tableData.value.pageSize || 4,
    // [zh] TypeScript/JS 语句：`orderStatus: 1`
    orderStatus: 1
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`Object.assign(params, searchForm.value)`
  Object.assign(params, searchForm.value)
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadOrder,`
    url: proxy.Api.loadOrder,
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

// [zh] 声明变量 `deliveryRef`
const deliveryRef = ref()
// [zh] 声明变量 `deliveryHandler`
const deliveryHandler = (data) => {
  // [zh] TypeScript/JS 语句：`deliveryRef.value.show(data.orderId)`
  deliveryRef.value.show(data.orderId)
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
