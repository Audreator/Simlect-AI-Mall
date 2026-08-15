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
              <!-- [zh] 模板内容：`type="button"` -->
              type="button"
              <!-- [zh] 模板内容：`class="dim-icon-btn danger"` -->
              class="dim-icon-btn danger"
              <!-- [zh] 模板内容：`title="移除此维度"` -->
              title="移除此维度"
              <!-- [zh] 模板内容：`@click="removePropertyDimension(pIndex)"` -->
              @click="removePropertyDimension(pIndex)"
            <!-- [zh] 模板内容：`>` -->
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
            <!-- [zh] 模板内容：`type="button"` -->
            type="button"
            <!-- [zh] 模板内容：`class="op-btn sm danger block"` -->
            class="op-btn sm danger block"
            <!-- [zh] 模板内容：`style="margin-top: 8px"` -->
            style="margin-top: 8px"
            <!-- [zh] 模板内容：`@click="removePropertyValue(pIndex, vIndex)"` -->
            @click="removePropertyValue(pIndex, vIndex)"
          <!-- [zh] 模板内容：`>` -->
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
            <!-- [zh] 模板内容：`class="iconfont icon-delete"` -->
            class="iconfont icon-delete"
            <!-- [zh] 模板内容：`title="移除此 SKU 维度"` -->
            title="移除此 SKU 维度"
            <!-- [zh] 模板内容：`@click="removePropertyDimension(pIndex)"` -->
            @click="removePropertyDimension(pIndex)"
          <!-- [zh] 模板内容：`></div>` -->
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
            <!-- [zh] 模板内容：`></div>` -->
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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./ProductSkuBuild.vue`
import ProductSkuBuild from './ProductSkuBuild.vue'
// [zh] 导入 `@/components/ImageSelect.vue`
import ImageSelect from '@/components/ImageSelect.vue'
// [zh] 导入 `vue`
import { ref, computed, getCurrentInstance, watch } from 'vue'
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

// [zh] 声明变量 `isMobileAdmin`
const isMobileAdmin = computed(() => route.path.startsWith('/m/'))

// [zh] 声明变量 `availableDimensions`
const availableDimensions = computed(() => {
  // [zh] 声明变量 `activeIds`
  const activeIds = new Set(productEditStore.productPropertyList.map((item) => item.propertyId))
  // [zh] return 返回
  return productEditStore.categoryPropertyTemplates.filter((item) => !activeIds.has(item.propertyId))
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `createDefaultPropertyValues`
const createDefaultPropertyValues = (index = 0) => [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`propertyValueId: `${Date.now()}${index}`,`
    propertyValueId: `${Date.now()}${index}`,
    // [zh] TypeScript/JS 语句：`propertyCover: '',`
    propertyCover: '',
    // [zh] TypeScript/JS 语句：`propertyValue: '',`
    propertyValue: '',
    // [zh] TypeScript/JS 语句：`propertyRemark: '',`
    propertyRemark: '',
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `addPropertyValue`
const addPropertyValue = (propertyIndex) => {
  // [zh] 代码块开始
  productEditStore.productPropertyList[propertyIndex].propertyValues.push({
    // [zh] TypeScript/JS 语句：`propertyValueId: `${Date.now()}`,`
    propertyValueId: `${Date.now()}`,
    // [zh] TypeScript/JS 语句：`propertyCover: '',`
    propertyCover: '',
    // [zh] TypeScript/JS 语句：`propertyValue: '',`
    propertyValue: '',
    // [zh] TypeScript/JS 语句：`propertyRemark: '',`
    propertyRemark: '',
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `removePropertyValue`
const removePropertyValue = (propertyIndex, valueIndex) => {
  // [zh] 条件判断 if
  if (productEditStore.productPropertyList[propertyIndex].propertyValues.length > 1) {
    // [zh] TypeScript/JS 语句：`productEditStore.productPropertyList[propertyIndex].pro`
    productEditStore.productPropertyList[propertyIndex].propertyValues.splice(valueIndex, 1)
  // [zh] 代码块开始
  } else {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('至少需要保留一个属性值')`
    proxy.Message.warning('至少需要保留一个属性值')
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `resetSkuGenerationState`
const resetSkuGenerationState = () => {
  // [zh] TypeScript/JS 语句：`productEditStore.skuData = new Map()`
  productEditStore.skuData = new Map()
  // [zh] TypeScript/JS 语句：`productEditStore.excludedSkuHashes = new Set()`
  productEditStore.excludedSkuHashes = new Set()
// [zh] 语法符号
}

// [zh] 声明变量 `removePropertyDimension`
const removePropertyDimension = (propertyIndex) => {
  // [zh] 条件判断 if
  if (productEditStore.productPropertyList.length <= 1) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('至少保留一个 SKU 维度')`
    proxy.Message.warning('至少保留一个 SKU 维度')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 声明变量 `property`
  const property = productEditStore.productPropertyList[propertyIndex]
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定移除「${property.propertyName}」维度吗？将按剩余维度重新生成 `
    message: `确定移除「${property.propertyName}」维度吗？将按剩余维度重新生成 SKU 列表`,
    // [zh] 代码块开始
    okfun: () => {
      // [zh] TypeScript/JS 语句：`productEditStore.productPropertyList.splice(propertyInd`
      productEditStore.productPropertyList.splice(propertyIndex, 1)
      // [zh] TypeScript/JS 语句：`resetSkuGenerationState()`
      resetSkuGenerationState()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `restorePropertyDimension`
const restorePropertyDimension = (template) => {
  // [zh] 声明变量 `restored`
  const restored = {
    // [zh] TypeScript/JS 语句：`...template,`
    ...template,
    // [zh] TypeScript/JS 语句：`propertyValues: createDefaultPropertyValues(),`
    propertyValues: createDefaultPropertyValues(),
  // [zh] 语法符号
  }
  // [zh] 声明变量 `orderIds`
  const orderIds = productEditStore.categoryPropertyTemplates.map((item) => item.propertyId)
  // [zh] 声明变量 `nextList`
  const nextList = [...productEditStore.productPropertyList, restored]
  // [zh] TypeScript/JS 语句：`nextList.sort((a, b) => orderIds.indexOf(a.propertyId) `
  nextList.sort((a, b) => orderIds.indexOf(a.propertyId) - orderIds.indexOf(b.propertyId))
  // [zh] TypeScript/JS 语句：`productEditStore.productPropertyList = nextList`
  productEditStore.productPropertyList = nextList
  // [zh] TypeScript/JS 语句：`resetSkuGenerationState()`
  resetSkuGenerationState()
// [zh] 语法符号
}

// [zh] 声明变量 `productSkuBuildRef`
const productSkuBuildRef = ref()
// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => productEditStore.productPropertyList,`
  () => productEditStore.productPropertyList,
  // [zh] 代码块开始
  () => {
    // [zh] TypeScript/JS 语句：`productSkuBuildRef.value?.generateSkuList()`
    productSkuBuildRef.value?.generateSkuList()
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`{ deep: true }`
  { deep: true }
// [zh] 语法符号
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
