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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./ProductSkuList.vue`
import ProductSkuList from './ProductSkuList.vue'
// [zh] 导入 `./ProductSkuProperty.vue`
import ProductSkuProperty from './ProductSkuProperty.vue'
// [zh] 导入 `./ProductBase.vue`
import ProductBase from './ProductBase.vue'
// [zh] 导入 `vue`
import { ref, getCurrentInstance, computed, onMounted } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 导入 `vue-router`
import { useRouter, useRoute } from 'vue-router'
// [zh] 声明变量 `router`
const router = useRouter()
// [zh] 声明变量 `route`
const route = useRoute()

// [zh] 导入 `@/stores/productEditStore`
import { useProductEditStore } from '@/stores/productEditStore'
// [zh] 声明变量 `productEditStore`
const productEditStore = useProductEditStore()

// [zh] 声明变量 `activeName`
const activeName = ref('base')
// [zh] 声明变量 `isMobileAdmin`
const isMobileAdmin = computed(() => route.path.startsWith('/m/'))
// [zh] 声明变量 `productListPath`
const productListPath = () => (isMobileAdmin.value ? '/m/product' : '/product')

// [zh] 声明变量 `tabClick`
const tabClick = async (e) => {
  // [zh] 条件判断 if
  if (e.paneName == 'sku' && !productInfo.value.categoryId) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请先选择分类')`
    proxy.Message.warning('请先选择分类')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `switchToSku`
const switchToSku = () => {
  // [zh] 条件判断 if
  if (!productInfo.value.categoryId) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请先选择分类')`
    proxy.Message.warning('请先选择分类')
    // [zh] TypeScript/JS 语句：`activeName.value = 'base'`
    activeName.value = 'base'
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`activeName.value = 'sku'`
  activeName.value = 'sku'
// [zh] 语法符号
}

// [zh] 声明变量 `productInfo`
const productInfo = ref({
  // [zh] TypeScript/JS 语句：`cover: Array(proxy.productMainImageCount).fill(''),`
  cover: Array(proxy.productMainImageCount).fill(''),
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `getProductInfo`
const getProductInfo = async () => {
  // [zh] 条件判断 if
  if (!route.params.productId) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.getProductInfo,`
    url: proxy.Api.getProductInfo,
    // [zh] 代码块开始
    params: {
      // [zh] TypeScript/JS 语句：`productId: route.params.productId,`
      productId: route.params.productId,
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
  // [zh] 代码块开始
  productInfo.value = {
    // [zh] TypeScript/JS 语句：`...result.data.productInfo,`
    ...result.data.productInfo,
    // [zh] TypeScript/JS 语句：`cover: result.data.productInfo.cover.split(','),`
    cover: result.data.productInfo.cover.split(','),
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`productEditStore.productPropertyList = result.data.prod`
  productEditStore.productPropertyList = result.data.productPropertyList
  // [zh] TypeScript/JS 语句：`productEditStore.skuData = new Map(`
  productEditStore.skuData = new Map(
    // [zh] TypeScript/JS 语句：`result.data.skuList.map((sku) => [sku.propertyValueIdHa`
    result.data.skuList.map((sku) => [sku.propertyValueIdHash, sku])
  // [zh] 语法符号
  )
  // [zh] TypeScript/JS 语句：`productEditStore.excludedSkuHashes = new Set()`
  productEditStore.excludedSkuHashes = new Set()
// [zh] 语法符号
}

// [zh] 声明变量 `cancelPost`
const cancelPost = () => {
  // [zh] TypeScript/JS 语句：`router.push(productListPath())`
  router.push(productListPath())
// [zh] 语法符号
}

// [zh] 声明变量 `submitProduct`
const submitProduct = async (sensitiveConfirmToken) => {
  // @click 可能传入 MouseEvent；仅字符串才视为已确认的管理员密码
  // [zh] 声明变量 `confirmPwd`
  const confirmPwd = typeof sensitiveConfirmToken === 'string' ? sensitiveConfirmToken : undefined
  // [zh] TypeScript/JS 语句：`activeName.value = 'base'`
  activeName.value = 'base'
  // [zh] 条件判断 if
  if (!productInfo.value.cover.every((item) => item !== '' && item != null)) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请上传商品主图')`
    proxy.Message.warning('请上传商品主图')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!productInfo.value.productName?.trim()) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请输入商品名称')`
    proxy.Message.warning('请输入商品名称')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (!productInfo.value.productDesc?.trim()) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请输入商品描述')`
    proxy.Message.warning('请输入商品描述')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`activeName.value = 'sku'`
  activeName.value = 'sku'
  // [zh] 条件判断 if
  if (productEditStore.skuList.length === 0) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请先设置SKU属性并生成SKU列表')`
    proxy.Message.warning('请先设置SKU属性并生成SKU列表')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }

  // [zh] for 循环
  for (const property of productEditStore.productPropertyList) {
    // [zh] for 循环
    for (const [index, value] of property.propertyValues.entries()) {
      // [zh] 条件判断 if
      if (property.coverType === 1 && !value.propertyCover) {
        // [zh] TypeScript/JS 语句：`proxy.Message.warning(`
        proxy.Message.warning(
          // [zh] TypeScript/JS 语句：``请上传【${property.propertyName}】属性第(${index + 1})行的图片``
          `请上传【${property.propertyName}】属性第(${index + 1})行的图片`
        // [zh] 语法符号
        )
        // [zh] TypeScript/JS 语句：`return`
        return
      // [zh] 语法符号
      }
      // [zh] 条件判断 if
      if (!value.propertyValue.trim()) {
        // [zh] TypeScript/JS 语句：`proxy.Message.warning(`
        proxy.Message.warning(
          // [zh] TypeScript/JS 语句：``请填写【${property.propertyName}】属性第(${index + 1})行的值``
          `请填写【${property.propertyName}】属性第(${index + 1})行的值`
        // [zh] 语法符号
        )
        // [zh] TypeScript/JS 语句：`return`
        return
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }

  // [zh] for 循环
  for (const [index, sku] of productEditStore.skuList.entries()) {
    // [zh] 条件判断 if
    if (sku.price <= 0) {
      // [zh] TypeScript/JS 语句：`proxy.Message.warning(`请设置sku列表第(${index + 1})行的价格`)`
      proxy.Message.warning(`请设置sku列表第(${index + 1})行的价格`)
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }

  // [zh] 声明变量 `productInfoResultData`
  const productInfoResultData = { ...productInfo.value }
  // [zh] TypeScript/JS 语句：`productInfoResultData.cover = productInfoResultData.cov`
  productInfoResultData.cover = productInfoResultData.cover.join(',')
  // [zh] TypeScript/JS 语句：`productInfoResultData.pCategoryId = productInfoResultDa`
  productInfoResultData.pCategoryId = productInfoResultData.categoryId[0]
  // [zh] TypeScript/JS 语句：`productInfoResultData.categoryId = productInfoResultDat`
  productInfoResultData.categoryId = productInfoResultData.categoryId[1]

  // [zh] 声明变量 `productPropertyListResultData`
  const productPropertyListResultData = []
  // [zh] for 循环
  for (let property of productEditStore.productPropertyList) {
    // [zh] for 循环
    for (let [index, propertyValue] of property.propertyValues.entries()) {
      // [zh] 声明变量 `resultValue`
      const resultValue = {
        // [zh] TypeScript/JS 语句：`...property,`
        ...property,
        // [zh] TypeScript/JS 语句：`...propertyValue,`
        ...propertyValue,
        // [zh] TypeScript/JS 语句：`sort: index,`
        sort: index,
      // [zh] 语法符号
      }
      // [zh] TypeScript/JS 语句：`delete resultValue.categoryId`
      delete resultValue.categoryId
      // [zh] TypeScript/JS 语句：`delete resultValue.pCategoryId`
      delete resultValue.pCategoryId
      // [zh] TypeScript/JS 语句：`delete resultValue.propertyValues`
      delete resultValue.propertyValues
      // [zh] TypeScript/JS 语句：`productPropertyListResultData.push(resultValue)`
      productPropertyListResultData.push(resultValue)
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }

  // [zh] 声明变量 `skuListResultData`
  const skuListResultData = []
  // [zh] for 循环
  for (let [index, sku] of productEditStore.skuList.entries()) {
    // [zh] 代码块开始
    skuListResultData.push({
      // [zh] TypeScript/JS 语句：`price: sku.price,`
      price: sku.price,
      // [zh] TypeScript/JS 语句：`stock: sku.stock,`
      stock: sku.stock,
      // [zh] TypeScript/JS 语句：`sort: index,`
      sort: index,
      // [zh] TypeScript/JS 语句：`propertyValueIdHash: sku.propertyValueIdHash,`
      propertyValueIdHash: sku.propertyValueIdHash,
      // [zh] TypeScript/JS 语句：`propertyValueIds: sku.propertyValueIds,`
      propertyValueIds: sku.propertyValueIds,
    // [zh] TypeScript/JS 语句：`})`
    })
  // [zh] 语法符号
  }

  // [zh] 声明变量 `doSave`
  const doSave = async (confirmPwd) => {
    // [zh] 声明变量 `result`
    let result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: route.params.productId ? proxy.Api.updateProduct :`
      url: route.params.productId ? proxy.Api.updateProduct : proxy.Api.addProduct,
      // [zh] TypeScript/JS 语句：`dataType: 'json',`
      dataType: 'json',
      // [zh] TypeScript/JS 语句：`sensitiveConfirmToken: confirmPwd,`
      sensitiveConfirmToken: confirmPwd,
      // [zh] 代码块开始
      params: {
        // [zh] TypeScript/JS 语句：`productInfo: productInfoResultData,`
        productInfo: productInfoResultData,
        // [zh] TypeScript/JS 语句：`productPropertyList: productPropertyListResultData,`
        productPropertyList: productPropertyListResultData,
        // [zh] TypeScript/JS 语句：`skuList: skuListResultData,`
        skuList: skuListResultData,
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
    // [zh] TypeScript/JS 语句：`proxy.Message.success('保存成功')`
    proxy.Message.success('保存成功')
    // [zh] TypeScript/JS 语句：`router.push(productListPath())`
    router.push(productListPath())
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (route.params.productId && !confirmPwd) {
    // [zh] 代码块开始
    proxy.ConfirmSensitive({
      // [zh] TypeScript/JS 语句：`message: '保存将更新商品价格与库存等信息，是否继续？',`
      message: '保存将更新商品价格与库存等信息，是否继续？',
      // [zh] TypeScript/JS 语句：`okfun: doSave,`
      okfun: doSave,
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }

  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await doSave(confirmPwd)
// [zh] 语法符号
}

// [zh] 声明变量 `reset`
const reset = () => {
  // [zh] TypeScript/JS 语句：`productInfo.value = { cover: Array(proxy.productMainIma`
  productInfo.value = { cover: Array(proxy.productMainImageCount).fill('') }
  // [zh] TypeScript/JS 语句：`productEditStore.resetSkuState()`
  productEditStore.resetSkuState()
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`reset()`
  reset()
  // [zh] TypeScript/JS 语句：`getProductInfo()`
  getProductInfo()
// [zh] TypeScript/JS 语句：`})`
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
