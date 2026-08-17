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

<script setup>
import ProductView from './ProductView.vue'
import ProductStock from './edit/ProductStock.vue'
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
import {
  canCommend,
  canDelist,
  canDelete,
  getCommendBlockReason,
  getDelistBlockReason,
  getDeleteBlockReason
} from '@/utils/productRules.js'

const columns = [
  {
    label: '商品信息',
    prop: 'avatar',
    scopedSlots: 'slotProduct',
  },
  {
    label: '价格区间',
    prop: 'price',
    width: 200,
    scopedSlots: 'slotPrice',
  },
  {
    label: '总库存',
    prop: 'stock',
    width: 100,
    scopedSlots: 'slotStock',
  },
  {
    label: 'SKU数量',
    prop: 'skuCount',
    width: 100,
  },
  {
    label: '状态',
    prop: 'status',
    scopedSlots: 'slotStatus',
    width: 100,
  },
  {
    label: '推荐',
    prop: 'commend',
    scopedSlots: 'commend',
    width: 100,
  },
  {
    label: '操作',
    prop: 'op',
    width: 380,
    scopedSlots: 'slotOp',
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
    url: proxy.Api.loadProduct,
    params: params,
  })
  if (!result) {
    return
  }
  Object.assign(tableData.value, result.data)
}

const del = (row) => {
  const block = getDeleteBlockReason(row)
  if (block) {
    proxy.Message.warning(block)
    return
  }
  proxy.ConfirmSensitive({
    message: `确定要删除【${row.productName}】吗？`,
    okfun: async (sensitiveConfirmToken) => {
      let result = await proxy.Request({
        url: proxy.Api.deleteProduct,
        sensitiveConfirmToken,
        params: {
          productId: row.productId,
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

const changeStatus = (row) => {
  if (row.status == 1) {
    const block = getDelistBlockReason(row)
    if (block) {
      proxy.Message.warning(block)
      return
    }
  }
  proxy.ConfirmSensitive({
    message: `确定要【${row.status == 0 ? '上架' : '下架'}】吗？`,
    okfun: async (sensitiveConfirmToken) => {
      let result = await proxy.Request({
        url: proxy.Api.updateProductStatus,
        sensitiveConfirmToken,
        params: {
          productId: row.productId,
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

const commend = (row) => {
  if (row.commendType == 0) {
    const block = getCommendBlockReason(row)
    if (block) {
      proxy.Message.warning(block)
      return
    }
  }
  proxy.Confirm({
    message: `确定要【${row.commendType == 0 ? '推荐' : '取消推荐'}】吗？`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.commendProduct,
        params: {
          productId: row.productId,
          commendType: row.commendType == 0 ? 1 : 0,
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

const productStockRef = ref()
const updateStock = (row) => {
  productStockRef.value.show(row.productId)
}

const updateStockHandler = ({ productId, totalStock }) => {
  const row = tableData.value.list.find((item) => {
    return item.productId == productId
  })
  row.totalStock = totalStock
}

const productViewRef = ref()
const viewProduct = (row) => {
  productViewRef.value.show(row.productId)
}

onMounted(() => {
  mitter.on('updateStockCallback', updateStockHandler)
})

onUnmounted(() => {
  mitter.off('updateStockCallback')
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
