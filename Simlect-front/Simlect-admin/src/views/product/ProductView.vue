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

<script setup>
import { ref, reactive, getCurrentInstance, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

import Vue3ImageMagnifier from 'vue3-image-magnifier'
import 'vue3-image-magnifier/dist/vue3-image-magnifier.css'

const dialogConfig = ref({
  show: false,
  title: '商品预览',
})
const productInfo = ref({})
const productPropertyList = ref([])
const skuList = ref([])
const getProduct = async (productId) => {
  let result = await proxy.Request({
    url: proxy.Api.getProductInfo,
    params: {
      productId,
    },
  })
  if (!result) {
    return
  }
  productInfo.value = result.data.productInfo
  
  selectImage(productInfo.value.cover.split(',')[0])

  productPropertyList.value = result.data.productPropertyList
  skuList.value = result.data.skuList
  
  initDefaultPropertySelected()
}

const showImage = computed(() => {
  return (
    proxy.Api.sourcePath +
    selectedImage.value?.replace(proxy.imageThumbnailSuffix, '')
  )
})

const selectedImage = ref()
const selectImage = (img) => {
  selectedImage.value = img
}

const show = (productId) => {
  dialogConfig.value.show = true
  getProduct(productId)
}


const selectedSku = ref({})
const selectedProperty = ref({})
const propertyImageMap = ref({})
const initDefaultPropertySelected = () => {
  selectedSku.value = skuList.value[0]
  const propertyValueIdArray = selectedSku.value.propertyValueIds.split('-')
  
  let initSelect = null
  for (let [index, property] of productPropertyList.value.entries()) {
    selectedProperty.value[property.propertyId] = propertyValueIdArray[index]
    for (const prop of property.propertyValues) {
      if (prop.propertyCover) {
        propertyImageMap.value[prop.propertyValueId] = prop.propertyCover
        
        if (initSelect == null) {
          selectImage(prop.propertyCover)
        }
        initSelect = prop.propertyCover
      }
    }
  }
}


const selectProperty = (property, propertyValue) => {
  const tempSelectedProperty = { ...selectedProperty.value }
  tempSelectedProperty[property.propertyId] = propertyValue.propertyValueId
  
  const selectedPropertyValueIds = productPropertyList.value
    .map((prop) => tempSelectedProperty[prop.propertyId])
    .join('-')
  const matchedSku = skuList.value.find(
    (sku) => sku.propertyValueIds === selectedPropertyValueIds
  )
  if (!matchedSku) {
    proxy.Message.warning('sku不存在')
    return
  }
  if (matchedSku.stock === 0) {
    proxy.Message.warning('sku对应的库存为0')
    return
  }
  selectedProperty.value[property.propertyId] = propertyValue.propertyValueId
  selectedSku.value = matchedSku
  
  const image = propertyImageMap.value[propertyValue.propertyValueId]
  if (image) {
    selectImage(image)
  }
}
defineExpose({
  show,
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
