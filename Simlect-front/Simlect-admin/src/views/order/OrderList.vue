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
          <el-form-item label="商品名称">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input clearable placeholder="输入商品名称" v-model="searchForm.productNameFuzzy"></el-input>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="状态" prop="">
            <!-- [zh] 开始标签 `<el-select>` -->
            <el-select clearable placeholder="请选择状态" v-model="searchForm.orderStatus">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="item.status" :label="item.desc" v-for="item in orderStatusList"></el-option>
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
        <template #slotOrder="{ index, row }">
          <!-- [zh] 开始标签 `<OrderItem>` -->
          <OrderItem :data="row" @delivery="deliveryHandler" @comment="commentHandler"></OrderItem>
        </template>
      </Table>
    </div>
  </el-card>

  <Delivery ref="deliveryRef" @reload="loadDataList"></Delivery>

  <CommentReply ref="commentRef" @reload="loadDataList"></CommentReply>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./CommentReply.vue`
import CommentReply from './CommentReply.vue'
// [zh] 导入 `./Delivery.vue`
import Delivery from './Delivery.vue'
// [zh] 导入 `./OrderItem.vue`
import OrderItem from './OrderItem.vue'
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

// [zh] 声明变量 `orderStatusList`
const orderStatusList = ref([])
// [zh] 声明变量 `loadOrderStatus`
const loadOrderStatus = async () => {
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadOrderStatus,`
    url: proxy.Api.loadOrderStatus,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`orderStatusList.value = result.data`
  orderStatusList.value = result.data
// [zh] 语法符号
}
// [zh] TypeScript/JS 语句：`loadOrderStatus()`
loadOrderStatus()

// [zh] 声明变量 `columns`
const columns = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '订单信息',`
    label: '订单信息',
    // [zh] TypeScript/JS 语句：`prop: 'avatar',`
    prop: 'avatar',
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

// [zh] 声明变量 `commentRef`
const commentRef = ref()
// [zh] 声明变量 `commentHandler`
const commentHandler = (orderId) => {
  // [zh] TypeScript/JS 语句：`commentRef.value.show(orderId)`
  commentRef.value.show(orderId)
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
