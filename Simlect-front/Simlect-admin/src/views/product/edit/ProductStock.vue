<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Dialog>` -->
  <Dialog :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" width="80%"
    :showCancel="false" @close="close">
    <!-- [zh] 开始标签 `<el-alert>` -->
    <el-alert title="填写后，与可售库存联动。提交时，在最新可售库存基础上，增加或减少" type="warning" show-icon :closable="false" />
    <!-- [zh] 开始标签 `<ProductSkuList>` -->
    <ProductSkuList :showType="1"></ProductSkuList>
  <!-- [zh] 闭合标签 `</Dialog>` -->
  </Dialog>
  <!-- [zh] 开始标签 `<ProductSkuBuild>` -->
  <ProductSkuBuild ref="productSkuBuildRef"></ProductSkuBuild>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./ProductSkuBuild.vue`
import ProductSkuBuild from './ProductSkuBuild.vue'
// [zh] 导入 `./ProductSkuList.vue`
import ProductSkuList from './ProductSkuList.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 导入 `@/stores/productEditStore`
import { useProductEditStore } from '@/stores/productEditStore'
// [zh] 声明变量 `productEditStore`
const productEditStore = useProductEditStore()


// [zh] 声明变量 `dialogConfig`
const dialogConfig = ref({
  // [zh] TypeScript/JS 语句：`show: false,`
  show: false,
  // [zh] TypeScript/JS 语句：`title: '更新库存',`
  title: '更新库存',
// [zh] TypeScript/JS 语句：`})`
})
// [zh] 声明变量 `productSkuBuildRef`
const productSkuBuildRef = ref()
// [zh] 声明变量 `getProductInfo`
const getProductInfo = async (productId, propertyValueIdHash) => {
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.getProductInfo,`
    url: proxy.Api.getProductInfo,
    // [zh] 代码块开始
    params: {
      // [zh] TypeScript/JS 语句：`productId,`
      productId,
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
  // [zh] TypeScript/JS 语句：`productEditStore.productPropertyList = result.data.prod`
  productEditStore.productPropertyList = result.data.productPropertyList
  // [zh] 声明变量 `skuList`
  let skuList = result.data.skuList;
  
  // [zh] 条件判断 if
  if (propertyValueIdHash) {
    // [zh] TypeScript/JS 语句：`skuList = skuList.filter(sku => sku.propertyValueIdHash`
    skuList = skuList.filter(sku => sku.propertyValueIdHash == propertyValueIdHash).map((sku) => [sku.propertyValueIdHash, sku])
  // [zh] 代码块开始
  } else {
    // [zh] TypeScript/JS 语句：`skuList = skuList.map((sku) => [sku.propertyValueIdHash`
    skuList = skuList.map((sku) => [sku.propertyValueIdHash, sku])
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`productEditStore.skuData = new Map(skuList)`
  productEditStore.skuData = new Map(skuList)
  // [zh] TypeScript/JS 语句：`productSkuBuildRef.value.generateSkuList()`
  productSkuBuildRef.value.generateSkuList()
// [zh] 语法符号
}

// [zh] 声明变量 `reset`
const reset = () => {
  // [zh] TypeScript/JS 语句：`productEditStore.productPropertyList = []`
  productEditStore.productPropertyList = []
  // [zh] TypeScript/JS 语句：`productEditStore.skuList = []`
  productEditStore.skuList = []
  // [zh] TypeScript/JS 语句：`productEditStore.skuData = new Map()`
  productEditStore.skuData = new Map()
// [zh] 语法符号
}

// [zh] 声明变量 `show`
const show = async (productId, propertyValueIdHash) => {
  // [zh] TypeScript/JS 语句：`dialogConfig.value.show = true`
  dialogConfig.value.show = true
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextTick()
  // [zh] TypeScript/JS 语句：`reset()`
  reset()
  // [zh] TypeScript/JS 语句：`getProductInfo(productId, propertyValueIdHash)`
  getProductInfo(productId, propertyValueIdHash)
// [zh] 语法符号
}

// [zh] 声明变量 `close`
const close = () => {
  // [zh] TypeScript/JS 语句：`dialogConfig.value.show = false`
  dialogConfig.value.show = false
// [zh] 语法符号
}

// [zh] 代码块开始
defineExpose({
  // [zh] TypeScript/JS 语句：`show,`
  show,
  // [zh] TypeScript/JS 语句：`close,`
  close,
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
