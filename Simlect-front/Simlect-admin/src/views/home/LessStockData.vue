<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Table>` -->
  <Table ref="tableInfoRef" :columns="columns" :fetch="loadDataList" :dataSource="tableData" :showPagination="false">
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #slotProduct="{ index, row }">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="product-info-panel">
        <!-- [zh] 开始标签 `<Cover>` -->
        <Cover :source="row.productCover" :width="70" class="cover"></Cover>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="product-info">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="product-name">{{ row.productName }}</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="product-id">ID:{{ row.productId }}</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="property-name">
            <!-- [zh] 开始标签 `<div>` -->
            <div v-for="(item, index) in row.propertyData">
              <!-- [zh] Mustache 插值表达式 -->
              {{ item.propertyName }}:{{ item.propertyValue }} <el-divider direction="vertical"
                <!-- [zh] 模板内容：`v-if="index < row.propertyData.length - 1" />` -->
                v-if="index < row.propertyData.length - 1" />
            <!-- [zh] 闭合标签 `</div>` -->
            </div>

          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #slotStock="{ index, row }">
      <!-- [zh] Mustache 插值表达式 -->
      {{ row.stock }} <span class="iconfont icon-edit" @click="updateStock(row)"></span>
    </template>
  </Table>
  <ProductStock ref="productStockRef"></ProductStock>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `@/views/product/edit/ProductStock.vue`
import ProductStock from '@/views/product/edit/ProductStock.vue'
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

// [zh] 声明变量 `columns`
const columns = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '库存预警',`
    label: '库存预警',
    // [zh] TypeScript/JS 语句：`prop: 'avatar',`
    prop: 'avatar',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotProduct',`
    scopedSlots: 'slotProduct',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '库存',`
    label: '库存',
    // [zh] TypeScript/JS 语句：`prop: 'stock',`
    prop: 'stock',
    // [zh] TypeScript/JS 语句：`width: 100,`
    width: 100,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotStock',`
    scopedSlots: 'slotStock',
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`]`
]

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
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`Object.assign(params, searchForm.value)`
  Object.assign(params, searchForm.value)
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadLessStockProduct,`
    url: proxy.Api.loadLessStockProduct,
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

// [zh] 声明变量 `productStockRef`
const productStockRef = ref()
// [zh] 声明变量 `updateStock`
const updateStock = (row) => {
  // [zh] TypeScript/JS 语句：`productStockRef.value.show(row.productId, row.propertyV`
  productStockRef.value.show(row.productId, row.propertyValueIdHash)
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] 代码块开始
  mitter.on('updateStockCallback', () => {
    // [zh] 语句
    productStockRef.value.close();
    // [zh] TypeScript/JS 语句：`loadDataList()`
    loadDataList()
  // [zh] TypeScript/JS 语句：`})`
  })
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
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `line-height: 1.3;` */
      line-height: 1.3;
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

    /* [zh] 样式规则 `.property-name {` */
    .property-name {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `color: #999999;` */
      color: #999999;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
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
</style>
