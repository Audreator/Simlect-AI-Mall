<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>

  <!-- [zh] 开始标签 `<div>` -->
  <div v-if="isMobileAdmin" class="m-sku-property">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="glass-card m-sku-block">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="block-title">SKU 属性</h3>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="block-desc">设置规格维度与属性值，系统将自动生成 SKU 组合</p>

      <!-- [zh] 开始标签 `<div>` -->
      <div
        v-for="(property, pIndex) in productEditStore.productPropertyList"
        :key="property.propertyId || pIndex"
        class="glass-card m-dimension-card"
      >
        <!-- [zh] 开始标签 `<div>` -->
        <div class="dim-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="dim-name">{{ property.propertyName }}</span>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="dim-actions">
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="dim-icon-btn" title="添加属性值" @click="addPropertyValue(pIndex)">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="iconfont icon-add"></span>
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
            <!-- [zh] 开始标签 `<button>` -->
            <button
              v-if="productEditStore.productPropertyList.length > 1"
              type="button"
              class="dim-icon-btn danger"
              title="移除此维度"
              @click="removePropertyDimension(pIndex)"
            >
              <!-- [zh] 开始标签 `<span>` -->
              <span class="iconfont icon-delete"></span>
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div
          v-for="(propItem, vIndex) in property.propertyValues"
          :key="vIndex"
          class="dim-value-card"
        >
          <!-- [zh] 开始标签 `<div>` -->
          <div class="value-index">属性值 {{ vIndex + 1 }}</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="property.coverType === 1" class="value-row-cover">
            <!-- [zh] 开始标签 `<ImageSelect>` -->
            <ImageSelect v-model="propItem.propertyCover" :cutWidth="150" :width="48" :scale="1" />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input v-model="propItem.propertyValue" placeholder="属性值" clearable class="value-input" />
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input
            v-model="propItem.propertyRemark"
            placeholder="备注（可选）"
            clearable
            class="remark-input"
            style="margin-top: 8px"
          />
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-if="property.propertyValues.length > 1"
            type="button"
            class="op-btn sm danger block"
            style="margin-top: 8px"
            @click="removePropertyValue(pIndex, vIndex)"
          >
            <!-- [zh] 模板内容：`删除此属性值` -->
            删除此属性值
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="availableDimensions.length" class="m-add-dim">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="add-dim-label">已移除的 SKU 维度，点击恢复</span>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="add-dim-tags">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-for="item in availableDimensions"
            :key="item.propertyId"
            type="button"
            class="add-dim-tag"
            @click="restorePropertyDimension(item)"
          >
            <!-- [zh] 模板内容：`+ {{ item.propertyName }}` -->
            + {{ item.propertyName }}
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<ProductSkuBuild>` -->
    <ProductSkuBuild ref="productSkuBuildRef"></ProductSkuBuild>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>

  <!-- [zh] 开始标签 `<div>` -->
  <div v-else class="sku-properties">
    <!-- [zh] 开始标签 `<div>` -->
    <div v-for="(property, pIndex) in productEditStore.productPropertyList" :key="property.propertyId || pIndex">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="sku-name-panel">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sku-name">{{ property.propertyName }}</div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sku-name-actions">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="iconfont icon-add" title="添加属性值" @click="addPropertyValue(pIndex)"></div>
          <!-- [zh] 开始标签 `<div>` -->
          <div
            v-if="productEditStore.productPropertyList.length > 1"
            class="iconfont icon-delete"
            title="移除此 SKU 维度"
            @click="removePropertyDimension(pIndex)"
          ></div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="sku-values">
        <!-- [zh] 开始标签 `<div>` -->
        <div v-for="(propItem, vIndex) in property.propertyValues" :key="vIndex" class="sku-value-row">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="number">{{ vIndex + 1 }}.</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="cover" v-if="property.coverType === 1">
            <!-- [zh] 开始标签 `<ImageSelect>` -->
            <ImageSelect v-model="propItem.propertyCover" :cutWidth="150" :width="30" :scale="1"></ImageSelect>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input v-model="propItem.propertyValue" placeholder="属性值" class="value-input" clearable></el-input>
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input v-model="propItem.propertyRemark" placeholder="备注（可选）" class="remark-input" clearable></el-input>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sku-op-panel">
            <!-- [zh] 开始标签 `<div>` -->
            <div
              class="iconfont icon-delete"
              @click="removePropertyValue(pIndex, vIndex)"
              v-if="property.propertyValues.length > 1"
            ></div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<el-divider>` -->
      <el-divider v-if="pIndex < productEditStore.productPropertyList.length - 1" />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="availableDimensions.length" class="add-dimension-panel">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="add-dimension-label">已移除的 SKU 维度：</span>
      <!-- [zh] 开始标签 `<el-tag>` -->
      <el-tag
        v-for="item in availableDimensions"
        :key="item.propertyId"
        class="add-dimension-tag"
        effect="plain"
        @click="restorePropertyDimension(item)"
      >
        <!-- [zh] 模板内容：`+ {{ item.propertyName }}` -->
        + {{ item.propertyName }}
      <!-- [zh] 闭合标签 `</el-tag>` -->
      </el-tag>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<ProductSkuBuild>` -->
    <ProductSkuBuild ref="productSkuBuildRef"></ProductSkuBuild>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup>
import ProductSkuBuild from './ProductSkuBuild.vue'
import ImageSelect from '@/components/ImageSelect.vue'
import { ref, computed, getCurrentInstance, watch } from 'vue'
const { proxy } = getCurrentInstance()
import { useRoute } from 'vue-router'
const route = useRoute()
import { useProductEditStore } from '@/stores/productEditStore'
const productEditStore = useProductEditStore()

const isMobileAdmin = computed(() => route.path.startsWith('/m/'))

const availableDimensions = computed(() => {
  const activeIds = new Set(productEditStore.productPropertyList.map((item) => item.propertyId))
  return productEditStore.categoryPropertyTemplates.filter((item) => !activeIds.has(item.propertyId))
})

const createDefaultPropertyValues = (index = 0) => [
  {
    propertyValueId: `${Date.now()}${index}`,
    propertyCover: '',
    propertyValue: '',
    propertyRemark: '',
  },
]

const addPropertyValue = (propertyIndex) => {
  productEditStore.productPropertyList[propertyIndex].propertyValues.push({
    propertyValueId: `${Date.now()}`,
    propertyCover: '',
    propertyValue: '',
    propertyRemark: '',
  })
}

const removePropertyValue = (propertyIndex, valueIndex) => {
  if (productEditStore.productPropertyList[propertyIndex].propertyValues.length > 1) {
    productEditStore.productPropertyList[propertyIndex].propertyValues.splice(valueIndex, 1)
  } else {
    proxy.Message.warning('至少需要保留一个属性值')
  }
}

const resetSkuGenerationState = () => {
  productEditStore.skuData = new Map()
  productEditStore.excludedSkuHashes = new Set()
}

const removePropertyDimension = (propertyIndex) => {
  if (productEditStore.productPropertyList.length <= 1) {
    proxy.Message.warning('至少保留一个 SKU 维度')
    return
  }
  const property = productEditStore.productPropertyList[propertyIndex]
  proxy.Confirm({
    message: `确定移除「${property.propertyName}」维度吗？将按剩余维度重新生成 SKU 列表`,
    okfun: () => {
      productEditStore.productPropertyList.splice(propertyIndex, 1)
      resetSkuGenerationState()
    },
  })
}

const restorePropertyDimension = (template) => {
  const restored = {
    ...template,
    propertyValues: createDefaultPropertyValues(),
  }
  const orderIds = productEditStore.categoryPropertyTemplates.map((item) => item.propertyId)
  const nextList = [...productEditStore.productPropertyList, restored]
  nextList.sort((a, b) => orderIds.indexOf(a.propertyId) - orderIds.indexOf(b.propertyId))
  productEditStore.productPropertyList = nextList
  resetSkuGenerationState()
}

const productSkuBuildRef = ref()
watch(
  () => productEditStore.productPropertyList,
  () => {
    productSkuBuildRef.value?.generateSkuList()
  },
  { deep: true }
)
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.sku-properties {` */
.sku-properties {
  /* [zh] 样式规则 `height: calc(100%);` */
  height: calc(100%);
  /* [zh] 样式规则 `overflow: auto;` */
  overflow: auto;
  /* [zh] 样式规则 `padding-right: 10px;` */
  padding-right: 10px;
  /* [zh] 样式规则 `margin-right: 10px;` */
  margin-right: 10px;
  /* [zh] 样式规则 `width: 450px;` */
  width: 450px;
  /* [zh] 样式规则 `max-width: 100%;` */
  max-width: 100%;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;

  /* [zh] 样式规则 `.sku-name-panel {` */
  .sku-name-panel {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `margin-bottom: 5px;` */
    margin-bottom: 5px;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;

    /* [zh] 样式规则 `.sku-name {` */
    .sku-name {
      /* [zh] 样式规则 `font-weight: bold;` */
      font-weight: bold;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.sku-name-actions {` */
    .sku-name-actions {
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
      /* [zh] 样式规则 `gap: 8px;` */
      gap: 8px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式声明 */
    .icon-add,
    /* [zh] 样式规则 `.icon-delete {` */
    .icon-delete {
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
      /* [zh] 样式规则 `border: 1px solid var(--header-border);` */
      border: 1px solid var(--header-border);
      /* [zh] 样式规则 `background: var(--primary-soft);` */
      background: var(--primary-soft);
      /* [zh] 样式规则 `color: var(--text2);` */
      color: var(--text2);
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `height: 25px;` */
      height: 25px;
      /* [zh] 样式规则 `width: 25px;` */
      width: 25px;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
      /* [zh] 样式规则 `justify-content: center;` */
      justify-content: center;
      /* [zh] 样式规则 `border-radius: 5px;` */
      border-radius: 5px;
      /* [zh] 样式规则 `transition: all 0.2s ease;` */
      transition: all 0.2s ease;

      /* [zh] 样式规则 `&:hover {` */
      &:hover {
        /* [zh] 样式规则 `border-color: var(--gold-border);` */
        border-color: var(--gold-border);
        /* [zh] 样式规则 `background: var(--gold-soft);` */
        background: var(--gold-soft);
        /* [zh] 样式规则 `color: var(--primary);` */
        color: var(--primary);
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sku-values {` */
  .sku-values {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;

    /* [zh] 样式规则 `.sku-value-row {` */
    .sku-value-row {
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
      /* [zh] 样式规则 `justify-content: center;` */
      justify-content: center;
      /* [zh] 样式规则 `margin-bottom: 10px;` */
      margin-bottom: 10px;

      /* [zh] 样式规则 `.number {` */
      .number {
        /* [zh] 样式规则 `font-size: 14px;` */
        font-size: 14px;
        /* [zh] 样式规则 `margin-right: 3px;` */
        margin-right: 3px;
        /* [zh] 样式规则 `color: #555555;` */
        color: #555555;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.cover {` */
      .cover {
        /* [zh] 样式规则 `margin-right: 5px;` */
        margin-right: 5px;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.value-input {` */
      .value-input {
        /* [zh] 样式规则 `flex: 1;` */
        flex: 1;
        /* [zh] 样式规则 `margin-right: 5px;` */
        margin-right: 5px;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.remark-input {` */
      .remark-input {
        /* [zh] 样式规则 `width: 120px;` */
        width: 120px;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.sku-op-panel {` */
      .sku-op-panel {
        /* [zh] 样式规则 `margin-left: 10px;` */
        margin-left: 10px;
        /* [zh] 样式规则 `width: 20px;` */
        width: 20px;
        /* [zh] 样式规则 `display: flex;` */
        display: flex;
        /* [zh] 样式规则 `justify-content: space-between;` */
        justify-content: space-between;

        /* [zh] 样式规则 `.iconfont {` */
        .iconfont {
          /* [zh] 样式规则 `cursor: pointer;` */
          cursor: pointer;
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.add-dimension-panel {` */
  .add-dimension-panel {
    /* [zh] 样式规则 `margin-top: 12px;` */
    margin-top: 12px;
    /* [zh] 样式规则 `padding-top: 12px;` */
    padding-top: 12px;
    /* [zh] 样式规则 `border-top: 1px dashed var(--header-bord` */
    border-top: 1px dashed var(--header-border);

    /* [zh] 样式规则 `.add-dimension-label {` */
    .add-dimension-label {
      /* [zh] 样式规则 `display: block;` */
      display: block;
      /* [zh] 样式规则 `margin-bottom: 8px;` */
      margin-bottom: 8px;
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
      /* [zh] 样式规则 `color: var(--text3);` */
      color: var(--text3);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.add-dimension-tag {` */
    .add-dimension-tag {
      /* [zh] 样式规则 `margin: 0 8px 8px 0;` */
      margin: 0 8px 8px 0;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-sku-property {` */
.m-sku-property {
  /* [zh] 样式声明 */
  .value-input,
  /* [zh] 样式规则 `.remark-input {` */
  .remark-input {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
