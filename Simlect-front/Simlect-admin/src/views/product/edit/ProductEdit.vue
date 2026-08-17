<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="form-style" :class="{ 'is-mobile-admin': isMobileAdmin }">

    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template v-if="isMobileAdmin">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-product-edit">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="m-edit-steps glass-card glass-strong">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            type="button"
            :class="['step', activeName === 'base' ? 'active' : '']"
            @click="activeName = 'base'"
          >
            <!-- [zh] 模板内容：`基础信息` -->
            基础信息
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" :class="['step', activeName === 'sku' ? 'active' : '']" @click="switchToSku">
            <!-- [zh] 模板内容：`SKU 规格` -->
            SKU 规格
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div v-show="activeName === 'base'">
          <!-- [zh] 开始标签 `<ProductBase>` -->
          <ProductBase :productInfo="productInfo"></ProductBase>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-show="activeName === 'sku'" class="m-sku-layout content-panel">
          <!-- [zh] 开始标签 `<ProductSkuProperty>` -->
          <ProductSkuProperty></ProductSkuProperty>
          <!-- [zh] 开始标签 `<ProductSkuList>` -->
          <ProductSkuList></ProductSkuList>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-edit-footer">
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="footer-cancel" @click="cancelPost">取消</button>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="footer-submit" @click="submitProduct()">
          <!-- [zh] Mustache 插值表达式 -->
          {{ route.params.productId ? '保存商品' : '发布商品' }}
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>

    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template v-else>
      <!-- [zh] 开始标签 `<el-tabs>` -->
      <el-tabs v-model="activeName" @tab-click="tabClick">
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="基础信息" name="base">
          <!-- [zh] 开始标签 `<ProductBase>` -->
          <ProductBase :productInfo="productInfo"></ProductBase>
        <!-- [zh] 闭合标签 `</el-tab-pane>` -->
        </el-tab-pane>
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="SKU信息" name="sku">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="content-panel">
            <!-- [zh] 开始标签 `<ProductSkuProperty>` -->
            <ProductSkuProperty></ProductSkuProperty>
            <!-- [zh] 开始标签 `<ProductSkuList>` -->
            <ProductSkuList></ProductSkuList>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</el-tab-pane>` -->
        </el-tab-pane>
      <!-- [zh] 闭合标签 `</el-tabs>` -->
      </el-tabs>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="post-panel">
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button @click="cancelPost" link>取消</el-button>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button @click="submitProduct()" type="primary">发布商品</el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>
  </div>
</template>

<script setup>
import ProductSkuList from './ProductSkuList.vue'
import ProductSkuProperty from './ProductSkuProperty.vue'
import ProductBase from './ProductBase.vue'
import { ref, getCurrentInstance, computed, onMounted } from 'vue'
const { proxy } = getCurrentInstance()
import { useRouter, useRoute } from 'vue-router'
const router = useRouter()
const route = useRoute()

import { useProductEditStore } from '@/stores/productEditStore'
const productEditStore = useProductEditStore()

const activeName = ref('base')
const isMobileAdmin = computed(() => route.path.startsWith('/m/'))
const productListPath = () => (isMobileAdmin.value ? '/m/product' : '/product')

const tabClick = async (e) => {
  if (e.paneName == 'sku' && !productInfo.value.categoryId) {
    proxy.Message.warning('请先选择分类')
    return
  }
}

const switchToSku = () => {
  if (!productInfo.value.categoryId) {
    proxy.Message.warning('请先选择分类')
    activeName.value = 'base'
    return
  }
  activeName.value = 'sku'
}

const productInfo = ref({
  cover: Array(proxy.productMainImageCount).fill(''),
})

const getProductInfo = async () => {
  if (!route.params.productId) {
    return
  }
  let result = await proxy.Request({
    url: proxy.Api.getProductInfo,
    params: {
      productId: route.params.productId,
    },
  })
  if (!result) {
    return
  }
  productInfo.value = {
    ...result.data.productInfo,
    cover: result.data.productInfo.cover.split(','),
  }
  productEditStore.productPropertyList = result.data.productPropertyList
  productEditStore.skuData = new Map(
    result.data.skuList.map((sku) => [sku.propertyValueIdHash, sku])
  )
  productEditStore.excludedSkuHashes = new Set()
}

const cancelPost = () => {
  router.push(productListPath())
}

const submitProduct = async (sensitiveConfirmToken) => {
  // @click 可能传入 MouseEvent；仅字符串才视为已确认的管理员密码
  const confirmPwd = typeof sensitiveConfirmToken === 'string' ? sensitiveConfirmToken : undefined
  activeName.value = 'base'
  if (!productInfo.value.cover.every((item) => item !== '' && item != null)) {
    proxy.Message.warning('请上传商品主图')
    return
  }
  if (!productInfo.value.productName?.trim()) {
    proxy.Message.warning('请输入商品名称')
    return
  }

  if (!productInfo.value.productDesc?.trim()) {
    proxy.Message.warning('请输入商品描述')
    return
  }
  activeName.value = 'sku'
  if (productEditStore.skuList.length === 0) {
    proxy.Message.warning('请先设置SKU属性并生成SKU列表')
    return
  }

  for (const property of productEditStore.productPropertyList) {
    for (const [index, value] of property.propertyValues.entries()) {
      if (property.coverType === 1 && !value.propertyCover) {
        proxy.Message.warning(
          `请上传【${property.propertyName}】属性第(${index + 1})行的图片`
        )
        return
      }
      if (!value.propertyValue.trim()) {
        proxy.Message.warning(
          `请填写【${property.propertyName}】属性第(${index + 1})行的值`
        )
        return
      }
    }
  }

  for (const [index, sku] of productEditStore.skuList.entries()) {
    if (sku.price <= 0) {
      proxy.Message.warning(`请设置sku列表第(${index + 1})行的价格`)
      return
    }
  }

  const productInfoResultData = { ...productInfo.value }
  productInfoResultData.cover = productInfoResultData.cover.join(',')
  productInfoResultData.pCategoryId = productInfoResultData.categoryId[0]
  productInfoResultData.categoryId = productInfoResultData.categoryId[1]

  const productPropertyListResultData = []
  for (let property of productEditStore.productPropertyList) {
    for (let [index, propertyValue] of property.propertyValues.entries()) {
      const resultValue = {
        ...property,
        ...propertyValue,
        sort: index,
      }
      delete resultValue.categoryId
      delete resultValue.pCategoryId
      delete resultValue.propertyValues
      productPropertyListResultData.push(resultValue)
    }
  }

  const skuListResultData = []
  for (let [index, sku] of productEditStore.skuList.entries()) {
    skuListResultData.push({
      price: sku.price,
      stock: sku.stock,
      sort: index,
      propertyValueIdHash: sku.propertyValueIdHash,
      propertyValueIds: sku.propertyValueIds,
    })
  }

  const doSave = async (confirmPwd) => {
    let result = await proxy.Request({
      url: route.params.productId ? proxy.Api.updateProduct : proxy.Api.addProduct,
      dataType: 'json',
      sensitiveConfirmToken: confirmPwd,
      params: {
        productInfo: productInfoResultData,
        productPropertyList: productPropertyListResultData,
        skuList: skuListResultData,
      },
    })
    if (!result) {
      return
    }
    proxy.Message.success('保存成功')
    router.push(productListPath())
  }

  if (route.params.productId && !confirmPwd) {
    proxy.ConfirmSensitive({
      message: '保存将更新商品价格与库存等信息，是否继续？',
      okfun: doSave,
    })
    return
  }

  await doSave(confirmPwd)
}

const reset = () => {
  productInfo.value = { cover: Array(proxy.productMainImageCount).fill('') }
  productEditStore.resetSkuState()
}

onMounted(() => {
  reset()
  getProductInfo()
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.form-style {` */
.form-style {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;

  /* [zh] 样式规则 `.post-panel {` */
  .post-panel {
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `top: 3px;` */
    top: 3px;
    /* [zh] 样式规则 `right: 20px;` */
    right: 20px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.content-panel {` */
  .content-panel {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `height: calc(100vh - 130px);` */
    height: calc(100vh - 130px);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-mobile-admin {` */
  &.is-mobile-admin {
    /* [zh] 样式规则 `.content-panel {` */
    .content-panel {
      /* [zh] 样式规则 `flex-direction: column;` */
      flex-direction: column;
      /* [zh] 样式规则 `height: auto;` */
      height: auto;
      /* [zh] 样式规则 `min-height: 0;` */
      min-height: 0;
      /* [zh] 样式规则 `gap: 12px;` */
      gap: 12px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
