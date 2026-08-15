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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `@/components/markdown/EditorMarkdown.vue`
import EditorMarkdown from '@/components/markdown/EditorMarkdown.vue'
// [zh] 导入 `@/components/ImageSelect.vue`
import ImageSelect from '@/components/ImageSelect.vue'
// [zh] 导入 `vue`
import { ref, getCurrentInstance, onMounted, watch, computed } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 导入 `vue-router`
import { useRoute } from 'vue-router'
// [zh] 声明变量 `route`
const route = useRoute()
// [zh] 声明变量 `isMobileAdmin`
const isMobileAdmin = computed(() => route.path.startsWith('/m/'))

// [zh] 导入 `@/stores/productEditStore`
import { useProductEditStore } from '@/stores/productEditStore'
// [zh] 声明变量 `productEditStore`
const productEditStore = useProductEditStore()

// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  productInfo: {
    // [zh] TypeScript/JS 语句：`type: Object,`
    type: Object,
    // [zh] TypeScript/JS 语句：`default: {},`
    default: {},
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `categoryList`
const categoryList = ref([])
// [zh] 声明变量 `loadCategory`
const loadCategory = async () => {
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadCategory,`
    url: proxy.Api.loadCategory,
    // [zh] 代码块开始
    params: {
      // [zh] TypeScript/JS 语句：`queryProperty: true,`
      queryProperty: true,
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
  // [zh] TypeScript/JS 语句：`categoryList.value = result.data`
  categoryList.value = result.data
// [zh] 语法符号
}

// [zh] 声明变量 `createPropertyWithDefaultValue`
const createPropertyWithDefaultValue = (property, index = 0) => ({
  // [zh] TypeScript/JS 语句：`...property,`
  ...property,
  // [zh] TypeScript/JS 语句：`propertyValues: [`
  propertyValues: [
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
  // [zh] 语法符号
  ],
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `getProductPropertyList`
const getProductPropertyList = (data) => {
  // [zh] TypeScript/JS 语句：`applyCategoryPropertyTemplates(data[data.length - 1], t`
  applyCategoryPropertyTemplates(data[data.length - 1], true)
// [zh] 语法符号
}

// [zh] 声明变量 `syncCategoryPropertyTemplates`
const syncCategoryPropertyTemplates = (categoryId) => {
  // [zh] 条件判断 if
  if (!categoryId || !categoryList.value.length) return
  // [zh] TypeScript/JS 语句：`applyCategoryPropertyTemplates(categoryId, false)`
  applyCategoryPropertyTemplates(categoryId, false)
// [zh] 语法符号
}

// [zh] 声明变量 `findPathToNode`
const findPathToNode = (id, nodes, path = []) => {
  // [zh] for 循环
  for (const node of nodes) {
    // [zh] 声明变量 `nextPath`
    const nextPath = [...path, node]
    // [zh] 条件判断 if
    if (node.categoryId === id) return nextPath
    // [zh] 条件判断 if
    if (node.children?.length) {
      // [zh] 声明变量 `found`
      const found = findPathToNode(id, node.children, nextPath)
      // [zh] 条件判断 if
      if (found) return found
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return null
// [zh] 语法符号
}

// [zh] 声明变量 `collectCategoryPropertyTemplates`
const collectCategoryPropertyTemplates = (categoryId) => {
  // [zh] 声明变量 `id`
  const id = Array.isArray(categoryId) ? categoryId[categoryId.length - 1] : categoryId
  // [zh] 条件判断 if
  if (!id || !categoryList.value.length) return []
  // [zh] 声明变量 `path`
  const path = findPathToNode(id, categoryList.value)
  // [zh] 条件判断 if
  if (!path?.length) return []
  // [zh] 声明变量 `seen`
  const seen = new Set()
  // [zh] 声明变量 `templates`
  const templates = []
  // [zh] for 循环
  for (const node of path) {
    // [zh] for 循环
    for (const property of node.productPropertyList || []) {
      // [zh] 条件判断 if
      if (!property?.propertyId || seen.has(property.propertyId)) continue
      // [zh] TypeScript/JS 语句：`seen.add(property.propertyId)`
      seen.add(property.propertyId)
      // [zh] TypeScript/JS 语句：`templates.push(createPropertyWithDefaultValue(property,`
      templates.push(createPropertyWithDefaultValue(property, templates.length))
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return templates
// [zh] 语法符号
}

// [zh] 声明变量 `applyCategoryPropertyTemplates`
const applyCategoryPropertyTemplates = (categoryId, resetActiveList = true) => {
  // [zh] 声明变量 `templates`
  const templates = collectCategoryPropertyTemplates(categoryId)
  // [zh] 代码块开始
  productEditStore.categoryPropertyTemplates = templates.map((item) => ({
    // [zh] TypeScript/JS 语句：`...item,`
    ...item,
    // [zh] TypeScript/JS 语句：`propertyValues: item.propertyValues.map((v) => ({ ...v `
    propertyValues: item.propertyValues.map((v) => ({ ...v })),
  // [zh] TypeScript/JS 语句：`}))`
  }))
  // [zh] 条件判断 if
  if (resetActiveList) {
    // [zh] 代码块开始
    productEditStore.productPropertyList = templates.map((item) => ({
      // [zh] TypeScript/JS 语句：`...item,`
      ...item,
      // [zh] TypeScript/JS 语句：`propertyValues: item.propertyValues.map((v) => ({ ...v `
      propertyValues: item.propertyValues.map((v) => ({ ...v })),
    // [zh] TypeScript/JS 语句：`}))`
    }))
    // [zh] TypeScript/JS 语句：`productEditStore.skuData = new Map()`
    productEditStore.skuData = new Map()
    // [zh] TypeScript/JS 语句：`productEditStore.excludedSkuHashes = new Set()`
    productEditStore.excludedSkuHashes = new Set()
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`loadCategory()`
  loadCategory()
// [zh] TypeScript/JS 语句：`})`
})

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => [props.productInfo?.categoryId, categoryList.valu`
  () => [props.productInfo?.categoryId, categoryList.value.length],
  // [zh] 代码块开始
  () => {
    // [zh] TypeScript/JS 语句：`syncCategoryPropertyTemplates(props.productInfo?.catego`
    syncCategoryPropertyTemplates(props.productInfo?.categoryId)
  // [zh] 语法符号
  }
// [zh] 语法符号
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
