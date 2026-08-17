<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>

  <!-- [zh] 开始标签 `<div>` -->
  <div v-if="isMobileAdmin" class="m-product-base">
    <!-- [zh] 开始标签 `<section>` -->
    <section class="glass-card m-edit-section">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="section-head">
        <!-- [zh] 开始标签 `<div>` -->
        <div>
          <!-- [zh] 开始标签 `<h3>` -->
          <h3 class="section-title">商品主图</h3>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="section-hint">最多 {{ proxy.productMainImageCount }} 张，建议首张作为封面</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="section-badge">必填</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-cover-grid">
        <!-- [zh] 开始标签 `<ImageSelect>` -->
        <ImageSelect
          v-for="(_, index) in proxy.productMainImageCount"
          :key="index"
          v-model="productInfo.cover[index]"
          :cutWidth="250"
          :width="100"
        />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="glass-card m-edit-section">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="section-title">基本信息</h3>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-field-block">
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-field-label">商品名称</label>
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input v-model="productInfo.productName" placeholder="请输入商品名称" clearable />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-field-block">
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-field-label">商品分类</label>
        <!-- [zh] 开始标签 `<el-cascader>` -->
        <el-cascader
          v-model="productInfo.categoryId"
          :options="categoryList"
          :props="{ label: 'categoryName', value: 'categoryId' }"
          style="width: 100%"
          placeholder="请选择分类"
          @change="getProductPropertyList"
          :disabled="route.params.productId != null"
        />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="glass-card m-edit-section">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="section-head">
        <!-- [zh] 开始标签 `<div>` -->
        <div>
          <!-- [zh] 开始标签 `<h3>` -->
          <h3 class="section-title">商品描述</h3>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="section-hint">支持 Markdown，用于详情页展示</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="product-desc is-mobile">
        <!-- [zh] 开始标签 `<EditorMarkdown>` -->
        <EditorMarkdown v-model="productInfo.productDesc"></EditorMarkdown>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>

  <!-- [zh] 开始标签 `<el-form>` -->
  <el-form v-else class="form-style" label-width="auto" @submit.prevent>
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item label="主图">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="cover-list">
        <!-- [zh] 开始标签 `<ImageSelect>` -->
        <ImageSelect
          v-for="(_, index) in proxy.productMainImageCount"
          :key="index"
          v-model="productInfo.cover[index]"
          :cutWidth="250"
          :width="120"
        />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item label="商品名称" prop="productName">
      <!-- [zh] 开始标签 `<el-input>` -->
      <el-input v-model="productInfo.productName" placeholder="请输入商品名称" clearable></el-input>
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item label="分类" prop="categoryIdArray">
      <!-- [zh] 开始标签 `<el-cascader>` -->
      <el-cascader
        v-model="productInfo.categoryId"
        :options="categoryList"
        :props="{ label: 'categoryName', value: 'categoryId' }"
        :style="{ width: '300px' }"
        @change="getProductPropertyList"
        :disabled="route.params.productId != null"
      />
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item label="商品描述" prop="productDesc">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="product-desc">
        <!-- [zh] 开始标签 `<EditorMarkdown>` -->
        <EditorMarkdown v-model="productInfo.productDesc"></EditorMarkdown>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
  <!-- [zh] 闭合标签 `</el-form>` -->
  </el-form>
</template>

<script setup>
import EditorMarkdown from '@/components/markdown/EditorMarkdown.vue'
import ImageSelect from '@/components/ImageSelect.vue'
import { ref, getCurrentInstance, onMounted, watch, computed } from 'vue'
const { proxy } = getCurrentInstance()
import { useRoute } from 'vue-router'
const route = useRoute()
const isMobileAdmin = computed(() => route.path.startsWith('/m/'))

import { useProductEditStore } from '@/stores/productEditStore'
const productEditStore = useProductEditStore()

const props = defineProps({
  productInfo: {
    type: Object,
    default: {},
  },
})

const categoryList = ref([])
const loadCategory = async () => {
  let result = await proxy.Request({
    url: proxy.Api.loadCategory,
    params: {
      queryProperty: true,
    },
  })
  if (!result) {
    return
  }
  categoryList.value = result.data
}

const createPropertyWithDefaultValue = (property, index = 0) => ({
  ...property,
  propertyValues: [
    {
      propertyValueId: `${Date.now()}${index}`,
      propertyCover: '',
      propertyValue: '',
      propertyRemark: '',
    },
  ],
})

const getProductPropertyList = (data) => {
  applyCategoryPropertyTemplates(data[data.length - 1], true)
}

const syncCategoryPropertyTemplates = (categoryId) => {
  if (!categoryId || !categoryList.value.length) return
  applyCategoryPropertyTemplates(categoryId, false)
}

const findPathToNode = (id, nodes, path = []) => {
  for (const node of nodes) {
    const nextPath = [...path, node]
    if (node.categoryId === id) return nextPath
    if (node.children?.length) {
      const found = findPathToNode(id, node.children, nextPath)
      if (found) return found
    }
  }
  return null
}

const collectCategoryPropertyTemplates = (categoryId) => {
  const id = Array.isArray(categoryId) ? categoryId[categoryId.length - 1] : categoryId
  if (!id || !categoryList.value.length) return []
  const path = findPathToNode(id, categoryList.value)
  if (!path?.length) return []
  const seen = new Set()
  const templates = []
  for (const node of path) {
    for (const property of node.productPropertyList || []) {
      if (!property?.propertyId || seen.has(property.propertyId)) continue
      seen.add(property.propertyId)
      templates.push(createPropertyWithDefaultValue(property, templates.length))
    }
  }
  return templates
}

const applyCategoryPropertyTemplates = (categoryId, resetActiveList = true) => {
  const templates = collectCategoryPropertyTemplates(categoryId)
  productEditStore.categoryPropertyTemplates = templates.map((item) => ({
    ...item,
    propertyValues: item.propertyValues.map((v) => ({ ...v })),
  }))
  if (resetActiveList) {
    productEditStore.productPropertyList = templates.map((item) => ({
      ...item,
      propertyValues: item.propertyValues.map((v) => ({ ...v })),
    }))
    productEditStore.skuData = new Map()
    productEditStore.excludedSkuHashes = new Set()
  }
}

onMounted(() => {
  loadCategory()
})

watch(
  () => [props.productInfo?.categoryId, categoryList.value.length],
  () => {
    syncCategoryPropertyTemplates(props.productInfo?.categoryId)
  }
)
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.form-style {` */
.form-style {
  /* [zh] 样式规则 `.cover-list {` */
  .cover-list {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;

    /* [zh] 样式规则 `:deep(.cover) {` */
    :deep(.cover) {
      /* [zh] 样式规则 `margin-right: 10px;` */
      margin-right: 10px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(.image-upload) {` */
    :deep(.image-upload) {
      /* [zh] 样式规则 `margin-right: 10px;` */
      margin-right: 10px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-desc {` */
.product-desc {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: calc(100vh - 400px);` */
  height: calc(100vh - 400px);

  /* [zh] 样式规则 `&.is-mobile {` */
  &.is-mobile {
    /* [zh] 样式规则 `height: 300px;` */
    height: 300px;
    /* [zh] 样式规则 `border-radius: 12px;` */
    border-radius: 12px;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
