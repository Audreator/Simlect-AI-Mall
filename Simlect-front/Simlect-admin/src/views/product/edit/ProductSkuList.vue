<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div v-if="isMobileAdmin" class="m-sku-list-wrap">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="glass-card m-sku-block">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="block-title">SKU 列表</h3>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="block-desc">为每个规格组合设置价格与库存</p>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="productEditStore.skuList.length" class="sku-mobile-list">
        <!-- [zh] 开始标签 `<div>` -->
        <div
          v-for="(row, index) in productEditStore.skuList"
          :key="index"
          class="glass-card m-sku-list-card"
        >
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sku-card-head">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="sku-index">SKU #{{ index + 1 }}</span>
            <!-- [zh] 开始标签 `<button>` -->
            <button
              v-if="productEditStore.skuList.length > 1"
              <!-- [zh] 模板内容：`type="button"` -->
              type="button"
              <!-- [zh] 模板内容：`class="op-btn sm danger"` -->
              class="op-btn sm danger"
              <!-- [zh] 模板内容：`@click="removeSku(index)"` -->
              @click="removeSku(index)"
            <!-- [zh] 模板内容：`>` -->
            >
              <!-- [zh] 模板内容：`删除` -->
              删除
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sku-specs">
            <!-- [zh] 开始标签 `<span>` -->
            <span
              v-for="property in productEditStore.productPropertyList"
              :key="property.propertyId"
              class="sku-spec-pill"
            >
              <!-- [zh] Mustache 插值表达式 -->
              {{ property.propertyName }}:
              <!-- [zh] 开始标签 `<strong>` -->
              <strong>{{ row[property.propertyId]?.propertyValue || '—' }}</strong>
            <!-- [zh] 闭合标签 `</span>` -->
            </span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sku-field">
            <!-- [zh] 开始标签 `<label>` -->
            <label>售价（元）</label>
            <!-- [zh] 开始标签 `<el-input-number>` -->
            <el-input-number v-model="row.price" :min="0" :precision="2" :step="1" style="width: 100%" />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sku-field">
            <!-- [zh] 开始标签 `<label>` -->
            <label>库存</label>
            <!-- [zh] 开始标签 `<el-input-number>` -->
            <el-input-number
              v-model="row.stock"
              :min="0"
              style="width: 100%"
              :disabled="route.params.productId != null"
            />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else class="m-empty-tip">请先在上方设置 SKU 属性</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>

  <!-- [zh] 开始标签 `<el-table>` -->
  <el-table v-else :data="productEditStore.skuList" border stripe height="100%">
    <!-- [zh] 开始标签 `<el-table-column>` -->
    <el-table-column type="index" width="50" />
    <!-- [zh] 开始标签 `<el-table-column>` -->
    <el-table-column
      v-for="property in productEditStore.productPropertyList"
      :key="property.id"
      :label="property.propertyName"
    >
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #default="{ row }">
        <!-- [zh] Mustache 插值表达式 -->
        {{ row[property.propertyId]?.propertyValue || '' }}
      </template>
    </el-table-column>

    <el-table-column label="价格" width="180">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #default="{ row }">
        <!-- [zh] 开始标签 `<el-input-number>` -->
        <el-input-number
          v-model="row.price"
          :min="0"
          :precision="2"
          :step="1"
          placeholder="价格"
          :disabled="showType == 1"
        ></el-input-number>
      </template>
    </el-table-column>

    <el-table-column v-if="showType === 1" label="当前库存" :width="100">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #default="{ row }">
        <!-- [zh] Mustache 插值表达式 -->
        {{ row.stock }}
      </template>
    </el-table-column>

    <el-table-column :label="showType == 0 ? '库存' : '库存增减'" :width="showType == 0 ? 155 : 350">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #default="{ row }">
        <!-- [zh] 开始标签 `<el-input-number>` -->
        <el-input-number
          v-model="row.stock"
          :min="0"
          placeholder="库存"
          :style="{ width: '130px' }"
          :disabled="route.params.productId != null"
          v-if="showType === 0"
        ></el-input-number>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="stock-update-panel" v-if="showType === 1">
          <!-- [zh] 开始标签 `<el-radio-group>` -->
          <el-radio-group v-model="row.stockUpdateType" fill="#F00033">
            <!-- [zh] 开始标签 `<el-radio-button>` -->
            <el-radio-button label="增加" value="1" />
            <!-- [zh] 开始标签 `<el-radio-button>` -->
            <el-radio-button label="减少" value="-1" />
          <!-- [zh] 闭合标签 `</el-radio-group>` -->
          </el-radio-group>
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input clearable placeholder="请输入数量" v-model="row.changeStock" class="stock-input"></el-input>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" @click="updateSkuStock(row)">确定</el-button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="53" v-if="showType == 0">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #default="{ $index }">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="iconfont icon-delete" @click="removeSku($index)"></span>
      </template>
    </el-table-column>
  </el-table>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { computed, getCurrentInstance } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 导入 `vue-router`
import { useRoute } from 'vue-router'
// [zh] 声明变量 `route`
const route = useRoute()
// [zh] 导入 `@/stores/productEditStore`
import { useProductEditStore } from '@/stores/productEditStore'
// [zh] 声明变量 `productEditStore`
const productEditStore = useProductEditStore()
// [zh] 导入 `@/eventbus/eventBus.js`
import { mitter } from '@/eventbus/eventBus.js'

// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  showType: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 0,`
    default: 0,
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `isMobileAdmin`
const isMobileAdmin = computed(() => route.path.startsWith('/m/'))

// [zh] 声明变量 `removeSku`
const removeSku = (index) => {
  // [zh] 条件判断 if
  if (productEditStore.skuList.length <= 1) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('至少保留一个 SKU')`
    proxy.Message.warning('至少保留一个 SKU')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 声明变量 `row`
  const row = productEditStore.skuList[index]
  // [zh] 条件判断 if
  if (row?.propertyValueIdHash) {
    // [zh] TypeScript/JS 语句：`productEditStore.excludedSkuHashes.add(row.propertyValu`
    productEditStore.excludedSkuHashes.add(row.propertyValueIdHash)
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`productEditStore.skuList.splice(index, 1)`
  productEditStore.skuList.splice(index, 1)
// [zh] 语法符号
}

// [zh] 声明变量 `updateSkuStock`
const updateSkuStock = async (row) => {
  // [zh] 条件判断 if
  if (!row.stockUpdateType) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请选择库存增减类型（增加/减少）')`
    proxy.Message.warning('请选择库存增减类型（增加/减少）')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!row.changeStock || !proxy.Verify.checkNumber(row.changeStock)) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请输入正确的库存数量')`
    proxy.Message.warning('请输入正确的库存数量')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.ConfirmSensitive({
    // [zh] TypeScript/JS 语句：`message: `确定要${row.stockUpdateType > 0 ? '增加' : '减少'}库存`
    message: `确定要${row.stockUpdateType > 0 ? '增加' : '减少'}库存 ${row.changeStock} 吗？`,
    // [zh] 代码块开始
    okfun: async (sensitiveConfirmToken) => {
      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.updateSkuStock,`
        url: proxy.Api.updateSkuStock,
        // [zh] TypeScript/JS 语句：`sensitiveConfirmToken,`
        sensitiveConfirmToken,
        // [zh] 代码块开始
        params: {
          // [zh] TypeScript/JS 语句：`productId: row.productId,`
          productId: row.productId,
          // [zh] TypeScript/JS 语句：`propertyValueIdHash: row.propertyValueIdHash,`
          propertyValueIdHash: row.propertyValueIdHash,
          // [zh] TypeScript/JS 语句：`changeStock: row.stockUpdateType * row.changeStock,`
          changeStock: row.stockUpdateType * row.changeStock,
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
      // [zh] TypeScript/JS 语句：`row.stock = row.stock + row.stockUpdateType * row.chang`
      row.stock = row.stock + row.stockUpdateType * row.changeStock
      // [zh] TypeScript/JS 语句：`proxy.Message.success('库存更新成功')`
      proxy.Message.success('库存更新成功')
      // [zh] 代码块开始
      mitter.emit('updateStockCallback', {
        // [zh] TypeScript/JS 语句：`productId: row.productId,`
        productId: row.productId,
        // [zh] TypeScript/JS 语句：`totalStock: productEditStore.skuList.reduce((sum, item)`
        totalStock: productEditStore.skuList.reduce((sum, item) => sum + item.stock, 0),
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] TypeScript/JS 语句：`row.stockUpdateType = null`
      row.stockUpdateType = null
      // [zh] TypeScript/JS 语句：`row.changeStock = null`
      row.changeStock = null
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.icon-delete {` */
.icon-delete {
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stock-update-panel {` */
.stock-update-panel {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;

  /* [zh] 样式规则 `.stock-input {` */
  .stock-input {
    /* [zh] 样式规则 `margin: 0px 10px;` */
    margin: 0px 10px;
    /* [zh] 样式规则 `width: 120px;` */
    width: 120px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-sku-list-wrap {` */
.m-sku-list-wrap {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-mobile-list {` */
.sku-mobile-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-card-head {` */
.sku-card-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;

  /* [zh] 样式规则 `.sku-index {` */
  .sku-index {
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
