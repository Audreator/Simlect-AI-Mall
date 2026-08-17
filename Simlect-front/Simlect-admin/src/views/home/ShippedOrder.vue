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

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from "vue"
const { proxy } = getCurrentInstance();
import Delivery from '@/views/order/Delivery.vue'
import OrderItem from '@/views/order/OrderItem.vue'
const columns = [
  {
    label: '待发货信息',
    prop: 'order',
    scopedSlots: 'slotOrder',
  },
]
const tableInfoRef = ref()
const searchForm = ref({})
const tableData = ref({ pageNo: 1, pageSize: 4 })
const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo || 1,
    pageSize: tableData.value.pageSize || 4,
    orderStatus: 1
  }
  Object.assign(params, searchForm.value)
  let result = await proxy.Request({
    url: proxy.Api.loadOrder,
    params: params,
  })
  if (!result) {
    return
  }
  Object.assign(tableData.value, result.data)
}

const deliveryRef = ref()
const deliveryHandler = (data) => {
  deliveryRef.value.show(data.orderId)
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
