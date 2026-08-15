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
            <el-select clearable placeholder="请选择状态" v-model="searchForm.status">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="0" label="未上架"></el-option>
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="1" label="已上架"></el-option>
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="-1" label="已删除"></el-option>
            <!-- [zh] 闭合标签 `</el-select>` -->
            </el-select>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="推荐" prop="">
            <!-- [zh] 开始标签 `<el-select>` -->
            <el-select clearable placeholder="请选择推荐类型" v-model="searchForm.commendType">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="0" label="未推荐"></el-option>
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="1" label="已推荐"></el-option>
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
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button @click="proxy.Utils.jump('/product/addProduct')" type="success">发布商品</el-button>
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
        <template #slotProduct="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="product-info-panel">
            <!-- [zh] 开始标签 `<Cover>` -->
            <Cover :source="row.cover.split(',')[0]" :width="70" class="cover"></Cover>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="product-info">
              <!-- [zh] 开始标签 `<div>` -->
              <div class="product-name">{{ row.productName }}</div>
              <!-- [zh] 开始标签 `<div>` -->
              <div class="product-id">ID:{{ row.productId }}</div>
              <!-- [zh] 开始标签 `<div>` -->
              <div class="category-name">
                <!-- [zh] 模板内容：`分类: {{ row.categoryName }}` -->
                分类: {{ row.categoryName }}
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotStatus="{ index, row }">
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status == 0" effect="dark" type="danger">未上架</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status == 1" effect="dark" type="success">已上架</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status == -1" effect="dark" type="danger">已删除</el-tag>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotPrice="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="price-panel">
            <!-- [zh] 开始标签 `<Price>` -->
            <Price :price="row.minPrice" :size="16"></Price>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="line">~</div>
            <!-- [zh] 开始标签 `<Price>` -->
            <Price :price="row.maxPrice" :size="16"></Price>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotStock="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="row.status != -1">
            <!-- [zh] Mustache 插值表达式 -->
            {{ row.totalStock }} <span class="iconfont icon-edit" @click="updateStock(row)"></span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #commend="{ index, row }">
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.commendType == 0" effect="dark" type="danger">未推荐</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.commendType == 1" effect="dark" type="success">已推荐</el-tag>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotOp="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="list-op-panel" v-if="row.status != -1">
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn icon="icon-view" tips="预览" @click="viewProduct(row)"></OpBtn>
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn
              v-if="row.commendType == 0"
              icon="icon-commend"
              tips="推荐"
              :disabled="!canCommend(row)"
              :disabled-tips="getCommendBlockReason(row)"
              @click="commend(row)"
            />
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn
              v-if="row.commendType == 1"
              icon="icon-cancel-commend"
              tips="取消推荐"
              @click="commend(row)"
            />
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn icon="icon-stock" tips="更新库存" @click="updateStock(row)"></OpBtn>
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn
              :icon="row.status == 0 ? 'icon-shangjia' : 'icon-xiajia'"
              :tips="row.status == 0 ? '上架' : '下架'"
              :disabled="row.status == 1 && !canDelist(row)"
              :disabled-tips="getDelistBlockReason(row)"
              @click="changeStatus(row)"
            />
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn icon="icon-edit" tips="修改" @click="proxy.Utils.jump(`/product/updateProduct/${row.productId}`)">
            <!-- [zh] 闭合标签 `</OpBtn>` -->
            </OpBtn>
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn
              icon="icon-delete"
              type="danger"
              tips="删除"
              :disabled="!canDelete(row)"
              :disabled-tips="getDeleteBlockReason(row)"
              @click="del(row)"
            />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>
      </Table>
    </div>
  </el-card>
  <ProductStock ref="productStockRef"></ProductStock>
  <ProductView ref="productViewRef"></ProductView>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./ProductView.vue`
import ProductView from './ProductView.vue'
// [zh] 导入 `./edit/ProductStock.vue`
import ProductStock from './edit/ProductStock.vue'
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
// [zh] 导入 `@/eventbus/eventBus.js`
import { mitter } from '@/eventbus/eventBus.js'
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`canCommend,`
  canCommend,
  // [zh] TypeScript/JS 语句：`canDelist,`
  canDelist,
  // [zh] TypeScript/JS 语句：`canDelete,`
  canDelete,
  // [zh] TypeScript/JS 语句：`getCommendBlockReason,`
  getCommendBlockReason,
  // [zh] TypeScript/JS 语句：`getDelistBlockReason,`
  getDelistBlockReason,
  // [zh] TypeScript/JS 语句：`getDeleteBlockReason`
  getDeleteBlockReason
// [zh] TypeScript/JS 语句：`} from '@/utils/productRules.js'`
} from '@/utils/productRules.js'

// [zh] 声明变量 `columns`
const columns = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '商品信息',`
    label: '商品信息',
    // [zh] TypeScript/JS 语句：`prop: 'avatar',`
    prop: 'avatar',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotProduct',`
    scopedSlots: 'slotProduct',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '价格区间',`
    label: '价格区间',
    // [zh] TypeScript/JS 语句：`prop: 'price',`
    prop: 'price',
    // [zh] TypeScript/JS 语句：`width: 200,`
    width: 200,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotPrice',`
    scopedSlots: 'slotPrice',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '总库存',`
    label: '总库存',
    // [zh] TypeScript/JS 语句：`prop: 'stock',`
    prop: 'stock',
    // [zh] TypeScript/JS 语句：`width: 100,`
    width: 100,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotStock',`
    scopedSlots: 'slotStock',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: 'SKU数量',`
    label: 'SKU数量',
    // [zh] TypeScript/JS 语句：`prop: 'skuCount',`
    prop: 'skuCount',
    // [zh] TypeScript/JS 语句：`width: 100,`
    width: 100,
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
    // [zh] TypeScript/JS 语句：`width: 100,`
    width: 100,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '推荐',`
    label: '推荐',
    // [zh] TypeScript/JS 语句：`prop: 'commend',`
    prop: 'commend',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'commend',`
    scopedSlots: 'commend',
    // [zh] TypeScript/JS 语句：`width: 100,`
    width: 100,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '操作',`
    label: '操作',
    // [zh] TypeScript/JS 语句：`prop: 'op',`
    prop: 'op',
    // [zh] TypeScript/JS 语句：`width: 380,`
    width: 380,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotOp',`
    scopedSlots: 'slotOp',
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
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadProduct,`
    url: proxy.Api.loadProduct,
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

// [zh] 声明变量 `del`
const del = (row) => {
  // [zh] 声明变量 `block`
  const block = getDeleteBlockReason(row)
  // [zh] 条件判断 if
  if (block) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning(block)`
    proxy.Message.warning(block)
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.ConfirmSensitive({
    // [zh] TypeScript/JS 语句：`message: `确定要删除【${row.productName}】吗？`,`
    message: `确定要删除【${row.productName}】吗？`,
    // [zh] 代码块开始
    okfun: async (sensitiveConfirmToken) => {
      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.deleteProduct,`
        url: proxy.Api.deleteProduct,
        // [zh] TypeScript/JS 语句：`sensitiveConfirmToken,`
        sensitiveConfirmToken,
        // [zh] 代码块开始
        params: {
          // [zh] TypeScript/JS 语句：`productId: row.productId,`
          productId: row.productId,
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

// [zh] 声明变量 `changeStatus`
const changeStatus = (row) => {
  // [zh] 条件判断 if
  if (row.status == 1) {
    // [zh] 声明变量 `block`
    const block = getDelistBlockReason(row)
    // [zh] 条件判断 if
    if (block) {
      // [zh] TypeScript/JS 语句：`proxy.Message.warning(block)`
      proxy.Message.warning(block)
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.ConfirmSensitive({
    // [zh] TypeScript/JS 语句：`message: `确定要【${row.status == 0 ? '上架' : '下架'}】吗？`,`
    message: `确定要【${row.status == 0 ? '上架' : '下架'}】吗？`,
    // [zh] 代码块开始
    okfun: async (sensitiveConfirmToken) => {
      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.updateProductStatus,`
        url: proxy.Api.updateProductStatus,
        // [zh] TypeScript/JS 语句：`sensitiveConfirmToken,`
        sensitiveConfirmToken,
        // [zh] 代码块开始
        params: {
          // [zh] TypeScript/JS 语句：`productId: row.productId,`
          productId: row.productId,
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

// [zh] 声明变量 `commend`
const commend = (row) => {
  // [zh] 条件判断 if
  if (row.commendType == 0) {
    // [zh] 声明变量 `block`
    const block = getCommendBlockReason(row)
    // [zh] 条件判断 if
    if (block) {
      // [zh] TypeScript/JS 语句：`proxy.Message.warning(block)`
      proxy.Message.warning(block)
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定要【${row.commendType == 0 ? '推荐' : '取消推荐'}】吗`
    message: `确定要【${row.commendType == 0 ? '推荐' : '取消推荐'}】吗？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.commendProduct,`
        url: proxy.Api.commendProduct,
        // [zh] 代码块开始
        params: {
          // [zh] TypeScript/JS 语句：`productId: row.productId,`
          productId: row.productId,
          // [zh] TypeScript/JS 语句：`commendType: row.commendType == 0 ? 1 : 0,`
          commendType: row.commendType == 0 ? 1 : 0,
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

// [zh] 声明变量 `productStockRef`
const productStockRef = ref()
// [zh] 声明变量 `updateStock`
const updateStock = (row) => {
  // [zh] TypeScript/JS 语句：`productStockRef.value.show(row.productId)`
  productStockRef.value.show(row.productId)
// [zh] 语法符号
}

// [zh] 声明变量 `updateStockHandler`
const updateStockHandler = ({ productId, totalStock }) => {
  // [zh] 声明变量 `row`
  const row = tableData.value.list.find((item) => {
    // [zh] return 返回
    return item.productId == productId
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] TypeScript/JS 语句：`row.totalStock = totalStock`
  row.totalStock = totalStock
// [zh] 语法符号
}

// [zh] 声明变量 `productViewRef`
const productViewRef = ref()
// [zh] 声明变量 `viewProduct`
const viewProduct = (row) => {
  // [zh] TypeScript/JS 语句：`productViewRef.value.show(row.productId)`
  productViewRef.value.show(row.productId)
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`mitter.on('updateStockCallback', updateStockHandler)`
  mitter.on('updateStockCallback', updateStockHandler)
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] TypeScript/JS 语句：`mitter.off('updateStockCallback')`
  mitter.off('updateStockCallback')
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.table-panel {` */
.table-panel {
  /* [zh] 样式规则 `height: calc(100vh - 135px);` */
  height: calc(100vh - 135px);

  /* [zh] 样式规则 `.product-info-panel {` */
  .product-info-panel {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;

    /* [zh] 样式规则 `.cover {` */
    .cover {
      /* [zh] 样式规则 `margin-right: 10px;` */
      margin-right: 10px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.product-info {` */
    .product-info {
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `flex-direction: column;` */
      flex-direction: column;

      /* [zh] 样式规则 `.product-name {` */
      .product-name {
        /* [zh] 样式规则 `font-size: 16px;` */
        font-size: 16px;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.product-id {` */
      .product-id {
        /* [zh] 样式规则 `margin-top: 3px;` */
        margin-top: 3px;
        /* [zh] 样式规则 `font-size: 12px;` */
        font-size: 12px;
        /* [zh] 样式规则 `color: #999999;` */
        color: #999999;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.category-name {` */
      .category-name {
        /* [zh] 样式规则 `font-size: 13px;` */
        font-size: 13px;
        /* [zh] 样式规则 `color: #999999;` */
        color: #999999;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price-panel {` */
  .price-panel {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;

    /* [zh] 样式规则 `.line {` */
    .line {
      /* [zh] 样式规则 `color: var(--text3);` */
      color: var(--text3);
      /* [zh] 样式规则 `margin: 0px 10px;` */
      margin: 0px 10px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.icon-edit {` */
  .icon-edit {
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.list-op-panel {` */
  .list-op-panel {
    /* [zh] 样式规则 `display: inline-flex;` */
    display: inline-flex;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: flex-start;` */
    justify-content: flex-start;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `max-width: 100%;` */
    max-width: 100%;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
