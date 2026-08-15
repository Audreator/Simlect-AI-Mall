<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Drawer>` -->
  <Drawer :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" width="1300px"
    :showCancel="false" @close="dialogConfig.show = false">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="product-detail-view">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="product-image-panel">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="image-list">
          <!-- [zh] 开始标签 `<div>` -->
          <div :class="['image-item', { active: image == selectedImage }]"
            v-for="image in productInfo?.cover?.split(',')">
            <!-- [zh] 开始标签 `<Cover>` -->
            <Cover :source="image" :width="100" @click="selectImage(image)"></Cover>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="image-view">
          <!-- [zh] 开始标签 `<Vue3ImageMagnifier>` -->
          <Vue3ImageMagnifier :src="showImage" :zoom-src="showImage" width="100%" :zoom-width="500" :zoom-scale="2" />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="product-info-panel">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="product-name">{{ productInfo.productName }}</div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="price-panel">
          <!-- [zh] 开始标签 `<Price>` -->
          <Price :price="selectedSku.price" :size="26"></Price>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="property-list">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="property-item" v-for="property in productPropertyList">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="property-name">{{ property.propertyName }}</div>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="property-values">
              <!-- [zh] 开始标签 `<div>` -->
              <div
                :class="['property-value-panel', { active: selectedProperty[property.propertyId] == value.propertyValueId }]"
                v-for="value in property.propertyValues" @click="selectProperty(property, value)">
                <!-- [zh] 开始标签 `<Cover>` -->
                <Cover v-if="value.propertyCover" :source="value.propertyCover" :width="25"></Cover>
                <!-- [zh] 开始标签 `<div>` -->
                <div class="property-value">
                  <!-- [zh] Mustache 插值表达式 -->
                  {{ value.propertyValue }}
                  <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
                  <template v-if="value.propertyRemark">({{ value.propertyRemark }})</template>
                <!-- [zh] 闭合标签 `</div>` -->
                </div>
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="stock-panel">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="stock-label">库存</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="stock-value">{{ selectedSku.stock }}</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="stock-tips" v-if="selectedSku.stock <= 5">库存紧张</div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</Drawer>` -->
  </Drawer>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick, computed } from 'vue'
// [zh] 导入 `vue-router`
import { useRouter, useRoute } from 'vue-router'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()
// [zh] 声明变量 `route`
const route = useRoute()

// [zh] 导入 `vue3-image-magnifier`
import Vue3ImageMagnifier from 'vue3-image-magnifier'
// [zh] 导入 `模块`
import 'vue3-image-magnifier/dist/vue3-image-magnifier.css'

// [zh] 声明变量 `dialogConfig`
const dialogConfig = ref({
  // [zh] TypeScript/JS 语句：`show: false,`
  show: false,
  // [zh] TypeScript/JS 语句：`title: '商品预览',`
  title: '商品预览',
// [zh] TypeScript/JS 语句：`})`
})
// [zh] 声明变量 `productInfo`
const productInfo = ref({})
// [zh] 声明变量 `productPropertyList`
const productPropertyList = ref([])
// [zh] 声明变量 `skuList`
const skuList = ref([])
// [zh] 声明变量 `getProduct`
const getProduct = async (productId) => {
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
  // [zh] TypeScript/JS 语句：`productInfo.value = result.data.productInfo`
  productInfo.value = result.data.productInfo
  
  // [zh] TypeScript/JS 语句：`selectImage(productInfo.value.cover.split(',')[0])`
  selectImage(productInfo.value.cover.split(',')[0])

  // [zh] TypeScript/JS 语句：`productPropertyList.value = result.data.productProperty`
  productPropertyList.value = result.data.productPropertyList
  // [zh] TypeScript/JS 语句：`skuList.value = result.data.skuList`
  skuList.value = result.data.skuList
  
  // [zh] TypeScript/JS 语句：`initDefaultPropertySelected()`
  initDefaultPropertySelected()
// [zh] 语法符号
}

// [zh] 声明变量 `showImage`
const showImage = computed(() => {
  // [zh] return 返回
  return (
    // [zh] TypeScript/JS 语句：`proxy.Api.sourcePath +`
    proxy.Api.sourcePath +
    // [zh] TypeScript/JS 语句：`selectedImage.value?.replace(proxy.imageThumbnailSuffix`
    selectedImage.value?.replace(proxy.imageThumbnailSuffix, '')
  // [zh] 语法符号
  )
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `selectedImage`
const selectedImage = ref()
// [zh] 声明变量 `selectImage`
const selectImage = (img) => {
  // [zh] TypeScript/JS 语句：`selectedImage.value = img`
  selectedImage.value = img
// [zh] 语法符号
}

// [zh] 声明变量 `show`
const show = (productId) => {
  // [zh] TypeScript/JS 语句：`dialogConfig.value.show = true`
  dialogConfig.value.show = true
  // [zh] TypeScript/JS 语句：`getProduct(productId)`
  getProduct(productId)
// [zh] 语法符号
}


// [zh] 声明变量 `selectedSku`
const selectedSku = ref({})
// [zh] 声明变量 `selectedProperty`
const selectedProperty = ref({})
// [zh] 声明变量 `propertyImageMap`
const propertyImageMap = ref({})
// [zh] 声明变量 `initDefaultPropertySelected`
const initDefaultPropertySelected = () => {
  // [zh] TypeScript/JS 语句：`selectedSku.value = skuList.value[0]`
  selectedSku.value = skuList.value[0]
  // [zh] 声明变量 `propertyValueIdArray`
  const propertyValueIdArray = selectedSku.value.propertyValueIds.split('-')
  
  // [zh] 声明变量 `initSelect`
  let initSelect = null
  // [zh] for 循环
  for (let [index, property] of productPropertyList.value.entries()) {
    // [zh] TypeScript/JS 语句：`selectedProperty.value[property.propertyId] = propertyV`
    selectedProperty.value[property.propertyId] = propertyValueIdArray[index]
    // [zh] for 循环
    for (const prop of property.propertyValues) {
      // [zh] 条件判断 if
      if (prop.propertyCover) {
        // [zh] TypeScript/JS 语句：`propertyImageMap.value[prop.propertyValueId] = prop.pro`
        propertyImageMap.value[prop.propertyValueId] = prop.propertyCover
        
        // [zh] 条件判断 if
        if (initSelect == null) {
          // [zh] TypeScript/JS 语句：`selectImage(prop.propertyCover)`
          selectImage(prop.propertyCover)
        // [zh] 语法符号
        }
        // [zh] TypeScript/JS 语句：`initSelect = prop.propertyCover`
        initSelect = prop.propertyCover
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
// [zh] 语法符号
}


// [zh] 声明变量 `selectProperty`
const selectProperty = (property, propertyValue) => {
  // [zh] 声明变量 `tempSelectedProperty`
  const tempSelectedProperty = { ...selectedProperty.value }
  // [zh] TypeScript/JS 语句：`tempSelectedProperty[property.propertyId] = propertyVal`
  tempSelectedProperty[property.propertyId] = propertyValue.propertyValueId
  
  // [zh] 声明变量 `selectedPropertyValueIds`
  const selectedPropertyValueIds = productPropertyList.value
    // [zh] TypeScript/JS 语句：`.map((prop) => tempSelectedProperty[prop.propertyId])`
    .map((prop) => tempSelectedProperty[prop.propertyId])
    // [zh] TypeScript/JS 语句：`.join('-')`
    .join('-')
  // [zh] 声明变量 `matchedSku`
  const matchedSku = skuList.value.find(
    // [zh] TypeScript/JS 语句：`(sku) => sku.propertyValueIds === selectedPropertyValue`
    (sku) => sku.propertyValueIds === selectedPropertyValueIds
  // [zh] 语法符号
  )
  // [zh] 条件判断 if
  if (!matchedSku) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('sku不存在')`
    proxy.Message.warning('sku不存在')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (matchedSku.stock === 0) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('sku对应的库存为0')`
    proxy.Message.warning('sku对应的库存为0')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`selectedProperty.value[property.propertyId] = propertyV`
  selectedProperty.value[property.propertyId] = propertyValue.propertyValueId
  // [zh] TypeScript/JS 语句：`selectedSku.value = matchedSku`
  selectedSku.value = matchedSku
  
  // [zh] 声明变量 `image`
  const image = propertyImageMap.value[propertyValue.propertyValueId]
  // [zh] 条件判断 if
  if (image) {
    // [zh] TypeScript/JS 语句：`selectImage(image)`
    selectImage(image)
  // [zh] 语法符号
  }
// [zh] 语法符号
}
// [zh] 代码块开始
defineExpose({
  // [zh] TypeScript/JS 语句：`show,`
  show,
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.product-detail-view {` */
.product-detail-view {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;

  /* [zh] 样式规则 `.product-image-panel {` */
  .product-image-panel {
    /* [zh] 样式规则 `width: 810px;` */
    width: 810px;
    /* [zh] 样式规则 `height: 700px;` */
    height: 700px;
    /* [zh] 样式规则 `max-height: calc(100vh);` */
    max-height: calc(100vh);
    /* [zh] 样式规则 `display: flex;` */
    display: flex;

    /* [zh] 样式规则 `.image-list {` */
    .image-list {
      /* [zh] 样式规则 `width: 100px;` */
      width: 100px;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `flex-direction: column;` */
      flex-direction: column;
      /* [zh] 样式规则 `justify-content: center;` */
      justify-content: center;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;

      /* [zh] 样式规则 `.image-item {` */
      .image-item {
        /* [zh] 样式规则 `margin-bottom: 10px;` */
        margin-bottom: 10px;
        /* [zh] 样式规则 `border-radius: 5px;` */
        border-radius: 5px;
        /* [zh] 样式规则 `overflow: hidden;` */
        overflow: hidden;
        /* [zh] 样式规则 `border: 1px solid #fff;` */
        border: 1px solid #fff;
        /* [zh] 样式规则 `transition: border-color 0.2s ease;` */
        transition: border-color 0.2s ease;

        /* [zh] 样式规则 `&:hover {` */
        &:hover {
          /* [zh] 样式规则 `border: 1px solid var(--gold-border);` */
          border: 1px solid var(--gold-border);
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.active {` */
      .active {
        /* [zh] 样式规则 `border: 1px solid var(--gold);` */
        border: 1px solid var(--gold);
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.image-view {` */
    .image-view {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `width: 0;` */
      width: 0;
      /* [zh] 样式规则 `background: var(--pink);` */
      background: var(--pink);
      /* [zh] 样式规则 `margin-left: 10px;` */
      margin-left: 10px;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;

      /* [zh] 样式规则 `:deep(.magnifier-container) {` */
      :deep(.magnifier-container) {
        /* [zh] 样式规则 `height: 100%;` */
        height: 100%;

        /* [zh] 样式规则 `img {` */
        img {
          /* [zh] 样式规则 `object-fit: contain;` */
          object-fit: contain;
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }

    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.product-info-panel {` */
  .product-info-panel {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `width: 0;` */
    width: 0;
    /* [zh] 样式规则 `border-radius: 5px;` */
    border-radius: 5px;
    /* [zh] 样式规则 `margin-left: 10px;` */
    margin-left: 10px;
    /* [zh] 样式规则 `padding: 10px;` */
    padding: 10px;

    /* [zh] 样式规则 `.product-name {` */
    .product-name {
      /* [zh] 样式规则 `font-size: 24px;` */
      font-size: 24px;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
      /* [zh] 样式规则 `color: #333;` */
      color: #333;
      /* [zh] 样式规则 `margin-bottom: 15px;` */
      margin-bottom: 15px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.price-panel {` */
    .price-panel {
      /* [zh] 样式规则 `margin-top: 10px;` */
      margin-top: 10px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.property-list {` */
    .property-list {
      /* [zh] 样式规则 `.property-item {` */
      .property-item {
        /* [zh] 样式规则 `margin-top: 20px;` */
        margin-top: 20px;

        /* [zh] 样式规则 `.property-name {` */
        .property-name {
          /* [zh] 样式规则 `font-size: 16px;` */
          font-size: 16px;
          /* [zh] 样式规则 `font-weight: 600;` */
          font-weight: 600;
          /* [zh] 样式规则 `color: #333;` */
          color: #333;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.property-values {` */
        .property-values {
          /* [zh] 样式规则 `display: flex;` */
          display: flex;
          /* [zh] 样式规则 `flex-wrap: wrap;` */
          flex-wrap: wrap;
          /* [zh] 样式规则 `margin-top: 5px;` */
          margin-top: 5px;

          /* [zh] 样式规则 `.property-value-panel {` */
          .property-value-panel {
            /* [zh] 样式规则 `display: flex;` */
            display: flex;
            /* [zh] 样式规则 `align-items: center;` */
            align-items: center;
            /* [zh] 样式规则 `border: 1px solid #ddd;` */
            border: 1px solid #ddd;
            /* [zh] 样式规则 `background-color: #f8f8f8;` */
            background-color: #f8f8f8;
            /* [zh] 样式规则 `border-radius: 6px;` */
            border-radius: 6px;
            /* [zh] 样式规则 `margin-right: 15px;` */
            margin-right: 15px;
            /* [zh] 样式规则 `padding: 5px;` */
            padding: 5px;
            /* [zh] 样式规则 `margin-bottom: 10px;` */
            margin-bottom: 10px;

            /* [zh] 样式规则 `:deep(.image-panel) {` */
            :deep(.image-panel) {
              /* [zh] 样式规则 `margin-right: 5px;` */
              margin-right: 5px;
            /* [zh] 样式规则 `}` */
            }

            /* [zh] 样式规则 `&:hover {` */
            &:hover {
              /* [zh] 样式规则 `border: 1px solid var(--gold-border);` */
              border: 1px solid var(--gold-border);
              /* [zh] 样式规则 `background: var(--gold-soft);` */
              background: var(--gold-soft);
            /* [zh] 样式规则 `}` */
            }

            /* [zh] 样式规则 `.property-value {` */
            .property-value {
              /* [zh] 样式规则 `cursor: pointer;` */
              cursor: pointer;
              /* [zh] 样式规则 `transition: all 0.2s ease;` */
              transition: all 0.2s ease;
              /* [zh] 样式规则 `font-size: 14px;` */
              font-size: 14px;
              /* [zh] 样式规则 `color: #333;` */
              color: #333;
              /* [zh] 样式规则 `position: relative;` */
              position: relative;
              /* [zh] 样式规则 `padding: 3px;` */
              padding: 3px;
            /* [zh] 样式规则 `}` */
            }
          /* [zh] 样式规则 `}` */
          }

          /* [zh] 样式规则 `.active {` */
          .active {
            /* [zh] 样式规则 `border: 1px solid var(--gold);` */
            border: 1px solid var(--gold);
            /* [zh] 样式规则 `background: var(--gold-soft);` */
            background: var(--gold-soft);
          /* [zh] 样式规则 `}` */
          }
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.stock-panel {` */
    .stock-panel {
      /* [zh] 样式规则 `margin-top: 20px;` */
      margin-top: 20px;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;

      /* [zh] 样式规则 `.stock-label {` */
      .stock-label {
        /* [zh] 样式规则 `font-size: 16px;` */
        font-size: 16px;
        /* [zh] 样式规则 `font-weight: 600;` */
        font-weight: 600;
        /* [zh] 样式规则 `color: #333;` */
        color: #333;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.stock-value {` */
      .stock-value {
        /* [zh] 样式规则 `margin-left: 10px;` */
        margin-left: 10px;
        /* [zh] 样式规则 `display: flex;` */
        display: flex;
        /* [zh] 样式规则 `align-items: center;` */
        align-items: center;
        /* [zh] 样式规则 `color: var(--red);` */
        color: var(--red);
        /* [zh] 样式规则 `font-size: 16px;` */
        font-size: 16px;
        /* [zh] 样式规则 `font-weight: 600;` */
        font-weight: 600;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.stock-tips {` */
      .stock-tips {
        /* [zh] 样式规则 `margin-left: 5px;` */
        margin-left: 5px;
        /* [zh] 样式规则 `color: #ffa202;` */
        color: #ffa202;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
