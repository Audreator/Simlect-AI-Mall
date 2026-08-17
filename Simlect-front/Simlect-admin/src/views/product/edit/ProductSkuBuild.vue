<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
const { proxy } = getCurrentInstance()
import { useProductEditStore } from '@/stores/productEditStore'
const productEditStore = useProductEditStore()
import md5 from 'js-md5'

const generateSkuList = () => {
  if (productEditStore.productPropertyList?.length == 0) {
    return
  }
  const existingSkuMap = new Map()
  productEditStore.skuList.forEach((sku) => {

    const { hash, propertyValueIds } = getPropertyValueIds(sku)

    if (sku.price !== 0 || sku.stock !== 0) {
      existingSkuMap.set(hash, {
        price: sku.price,
        stock: sku.stock,
        propertyValueIds,
      })
    }
  })

  let newSkuList = generateCombinations(productEditStore.productPropertyList)

  newSkuList.forEach((sku) => {
    const { hash, propertyValueIds } = getPropertyValueIds(sku)
    const existingData =
      existingSkuMap.get(hash) || productEditStore.skuData?.get(hash)
    if (existingData) {
      sku.price = existingData.price
      sku.stock = existingData.stock
      sku.productId = existingData.productId
    }
    sku.propertyValueIdHash = hash
    sku.propertyValueIds = propertyValueIds
  })

  if (productEditStore.skuData.size > 0) {
    newSkuList = newSkuList.filter((sku) => productEditStore.skuData.has(getPropertyValueIds(sku).hash))
    productEditStore.skuData.clear()
  }
  if (productEditStore.excludedSkuHashes?.size > 0) {
    newSkuList = newSkuList.filter(
      (sku) => !productEditStore.excludedSkuHashes.has(getPropertyValueIds(sku).hash)
    )
  }
  productEditStore.skuList = newSkuList
}

const getPropertyValueIds = (sku) => {
  const propertyValueIds = productEditStore.productPropertyList
    .map((prop) => `${sku[prop.propertyId]?.propertyValueId || ''}`)
    .join('-')
  return {
    hash: md5(propertyValueIds),
    propertyValueIds,
  }
}

const generateCombinations = (arrays, index = 0, current = {}) => {
  if (index === arrays.length) {
    return [{ ...current, price: 0, stock: 0 }]
  }
  const property = arrays[index]
  const result = []
  for (const value of property.propertyValues) {
    const newCurrent = {
      ...current,
      [property.propertyId]: {
        propertyId: property.propertyId,
        propertyName: property.propertyName,
        ...value,
      },
    }
    const arrayResult = generateCombinations(arrays, index + 1, newCurrent)
    if (arrayResult.length > 0) {
      result.push(...arrayResult)
    }
  }
  return result
}

defineExpose({
  generateSkuList,
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped></style>
