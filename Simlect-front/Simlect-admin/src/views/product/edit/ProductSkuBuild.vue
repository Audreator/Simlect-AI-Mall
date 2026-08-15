<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 导入 `@/stores/productEditStore`
import { useProductEditStore } from '@/stores/productEditStore'
// [zh] 声明变量 `productEditStore`
const productEditStore = useProductEditStore()
// [zh] 导入 `js-md5`
import md5 from 'js-md5'

// [zh] 声明变量 `generateSkuList`
const generateSkuList = () => {
  // [zh] 条件判断 if
  if (productEditStore.productPropertyList?.length == 0) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 声明变量 `existingSkuMap`
  const existingSkuMap = new Map()
  // [zh] 代码块开始
  productEditStore.skuList.forEach((sku) => {

    // [zh] TypeScript/JS 语句：`const { hash, propertyValueIds } = getPropertyValueIds(`
    const { hash, propertyValueIds } = getPropertyValueIds(sku)

    // [zh] 条件判断 if
    if (sku.price !== 0 || sku.stock !== 0) {
      // [zh] 代码块开始
      existingSkuMap.set(hash, {
        // [zh] TypeScript/JS 语句：`price: sku.price,`
        price: sku.price,
        // [zh] TypeScript/JS 语句：`stock: sku.stock,`
        stock: sku.stock,
        // [zh] TypeScript/JS 语句：`propertyValueIds,`
        propertyValueIds,
      // [zh] TypeScript/JS 语句：`})`
      })
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })

  // [zh] 声明变量 `newSkuList`
  let newSkuList = generateCombinations(productEditStore.productPropertyList)

  // [zh] 代码块开始
  newSkuList.forEach((sku) => {
    // [zh] TypeScript/JS 语句：`const { hash, propertyValueIds } = getPropertyValueIds(`
    const { hash, propertyValueIds } = getPropertyValueIds(sku)
    // [zh] 声明变量 `existingData`
    const existingData =
      // [zh] TypeScript/JS 语句：`existingSkuMap.get(hash) || productEditStore.skuData?.g`
      existingSkuMap.get(hash) || productEditStore.skuData?.get(hash)
    // [zh] 条件判断 if
    if (existingData) {
      // [zh] TypeScript/JS 语句：`sku.price = existingData.price`
      sku.price = existingData.price
      // [zh] TypeScript/JS 语句：`sku.stock = existingData.stock`
      sku.stock = existingData.stock
      // [zh] TypeScript/JS 语句：`sku.productId = existingData.productId`
      sku.productId = existingData.productId
    // [zh] 语法符号
    }
    // [zh] TypeScript/JS 语句：`sku.propertyValueIdHash = hash`
    sku.propertyValueIdHash = hash
    // [zh] TypeScript/JS 语句：`sku.propertyValueIds = propertyValueIds`
    sku.propertyValueIds = propertyValueIds
  // [zh] TypeScript/JS 语句：`})`
  })

  // [zh] 条件判断 if
  if (productEditStore.skuData.size > 0) {
    // [zh] TypeScript/JS 语句：`newSkuList = newSkuList.filter((sku) => productEditStor`
    newSkuList = newSkuList.filter((sku) => productEditStore.skuData.has(getPropertyValueIds(sku).hash))
    // [zh] TypeScript/JS 语句：`productEditStore.skuData.clear()`
    productEditStore.skuData.clear()
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (productEditStore.excludedSkuHashes?.size > 0) {
    // [zh] TypeScript/JS 语句：`newSkuList = newSkuList.filter(`
    newSkuList = newSkuList.filter(
      // [zh] TypeScript/JS 语句：`(sku) => !productEditStore.excludedSkuHashes.has(getPro`
      (sku) => !productEditStore.excludedSkuHashes.has(getPropertyValueIds(sku).hash)
    // [zh] 语法符号
    )
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`productEditStore.skuList = newSkuList`
  productEditStore.skuList = newSkuList
// [zh] 语法符号
}

// [zh] 声明变量 `getPropertyValueIds`
const getPropertyValueIds = (sku) => {
  // [zh] 声明变量 `propertyValueIds`
  const propertyValueIds = productEditStore.productPropertyList
    // [zh] TypeScript/JS 语句：`.map((prop) => `${sku[prop.propertyId]?.propertyValueId`
    .map((prop) => `${sku[prop.propertyId]?.propertyValueId || ''}`)
    // [zh] TypeScript/JS 语句：`.join('-')`
    .join('-')
  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`hash: md5(propertyValueIds),`
    hash: md5(propertyValueIds),
    // [zh] TypeScript/JS 语句：`propertyValueIds,`
    propertyValueIds,
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `generateCombinations`
const generateCombinations = (arrays, index = 0, current = {}) => {
  // [zh] 条件判断 if
  if (index === arrays.length) {
    // [zh] return 返回
    return [{ ...current, price: 0, stock: 0 }]
  // [zh] 语法符号
  }
  // [zh] 声明变量 `property`
  const property = arrays[index]
  // [zh] 声明变量 `result`
  const result = []
  // [zh] for 循环
  for (const value of property.propertyValues) {
    // [zh] 声明变量 `newCurrent`
    const newCurrent = {
      // [zh] TypeScript/JS 语句：`...current,`
      ...current,
      // [zh] 代码块开始
      [property.propertyId]: {
        // [zh] TypeScript/JS 语句：`propertyId: property.propertyId,`
        propertyId: property.propertyId,
        // [zh] TypeScript/JS 语句：`propertyName: property.propertyName,`
        propertyName: property.propertyName,
        // [zh] TypeScript/JS 语句：`...value,`
        ...value,
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] 语法符号
    }
    // [zh] 声明变量 `arrayResult`
    const arrayResult = generateCombinations(arrays, index + 1, newCurrent)
    // [zh] 条件判断 if
    if (arrayResult.length > 0) {
      // [zh] TypeScript/JS 语句：`result.push(...arrayResult)`
      result.push(...arrayResult)
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return result
// [zh] 语法符号
}

// [zh] 代码块开始
defineExpose({
  // [zh] TypeScript/JS 语句：`generateSkuList,`
  generateSkuList,
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
