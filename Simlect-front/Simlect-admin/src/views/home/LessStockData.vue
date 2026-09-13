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
            <div v-for="(item, index) in row.propertyData" :key="`${item.propertyName}-${index}`">
              <!-- [zh] Mustache 插值表达式 -->
              {{ item.propertyName }}:{{ item.propertyValue }}
              <el-divider v-if="index < row.propertyData.length - 1" direction="vertical" />
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

<script setup>
import ProductStock from '@/views/product/edit/ProductStock.vue'
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  onMounted,
  onUnmounted,
} from 'vue'
import { useRouter } from 'vue-router'
const { proxy } = getCurrentInstance()
import { mitter } from '@/eventbus/eventBus.js'

const columns = [
  {
    label: '库存预警',
    prop: 'avatar',
    scopedSlots: 'slotProduct',
  },
  {
    label: '库存',
    prop: 'stock',
    width: 100,
    scopedSlots: 'slotStock',
  },
]

const searchForm = ref({})
const tableData = ref({ pageNo: 1, pageSize: 4 })
const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo || 1,
    pageSize: tableData.value.pageSize || 4,
  }
  Object.assign(params, searchForm.value)
  let result = await proxy.Request({
    url: proxy.Api.loadLessStockProduct,
    params: params,
  })
  if (!result) {
    return
  }
  Object.assign(tableData.value, result.data)
}

const productStockRef = ref()
const updateStock = (row) => {
  productStockRef.value.show(row.productId, row.propertyValueIdHash)
}

onMounted(() => {
  mitter.on('updateStockCallback', () => {
    productStockRef.value.close();
    loadDataList()
  })
})

onUnmounted(() => {
  mitter.off('updateStockCallback')
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
