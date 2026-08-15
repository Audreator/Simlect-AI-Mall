<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<p>` -->
    <p class="m-note">在二级及以下分类上配置 SKU 属性（如颜色、规格）。点击属性可编辑，点「+ 属性」新增。</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="propertyCategories.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="node in propertyCategories" :key="node.categoryId" class="glass-card prop-card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="prop-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="prop-path">{{ node.pathLabel }}</span>
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn sm primary" @click="showEdit(node)">+ 属性</button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="node.productPropertyList?.length" class="prop-tags">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-for="tag in node.productPropertyList"
            :key="tag.propertyId"
            type="button"
            class="prop-tag"
            @click="showEdit(node, tag)"
          >
            <!-- [zh] Mustache 插值表达式 -->
            {{ tag.propertyName }}<template v-if="tag.coverType == 1">(含图)</template>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="tag-del" @click.stop="delProperty(tag)">×</span>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-else class="prop-empty">暂无属性</p>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else class="m-empty-tip">暂无可用分类</p>

    <!-- [zh] 开始标签 `<ProductPropertyEdit>` -->
    <ProductPropertyEdit ref="productPropertyEditRef" @reload="loadCategory" />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `@/views/product/ProductPropertyEdit.vue`
import ProductPropertyEdit from '@/views/product/ProductPropertyEdit.vue'
// [zh] 导入 `vue`
import { ref, getCurrentInstance, onMounted } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `categoryList`
const categoryList = ref([])
// [zh] 声明变量 `propertyCategories`
const propertyCategories = ref([])
// [zh] 声明变量 `productPropertyEditRef`
const productPropertyEditRef = ref(null)

// [zh] 声明变量 `collectPropertyCategories`
const collectPropertyCategories = (nodes, parentPath = '') => {
  // [zh] 声明变量 `result`
  const result = []
  // [zh] for 循环
  for (const n of nodes) {
    // [zh] 声明变量 `path`
    const path = parentPath ? `${parentPath} / ${n.categoryName}` : n.categoryName
    // [zh] 条件判断 if
    if (n.pCategoryId != '0' && n.pCategoryId !== 0) {
      // [zh] TypeScript/JS 语句：`result.push({ ...n, pathLabel: path })`
      result.push({ ...n, pathLabel: path })
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (n.children?.length) {
      // [zh] TypeScript/JS 语句：`result.push(...collectPropertyCategories(n.children, pa`
      result.push(...collectPropertyCategories(n.children, path))
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return result
// [zh] 语法符号
}

// [zh] 声明变量 `loadCategory`
const loadCategory = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadCategory,`
    url: proxy.Api.loadCategory,
    // [zh] TypeScript/JS 语句：`params: { queryProperty: true },`
    params: { queryProperty: true },
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`categoryList.value = result.data || []`
  categoryList.value = result.data || []
  // [zh] TypeScript/JS 语句：`propertyCategories.value = collectPropertyCategories(ca`
  propertyCategories.value = collectPropertyCategories(categoryList.value)
// [zh] 语法符号
}

// [zh] 声明变量 `showEdit`
const showEdit = ({ categoryId, pCategoryId }, tag) => {
  // [zh] TypeScript/JS 语句：`productPropertyEditRef.value.show({ categoryId, pCatego`
  productPropertyEditRef.value.show({ categoryId, pCategoryId, ...tag })
// [zh] 语法符号
}

// [zh] 声明变量 `delProperty`
const delProperty = (data) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定要删除【${data.propertyName}】吗?`,`
    message: `确定要删除【${data.propertyName}】吗?`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.delProductProperty,`
        url: proxy.Api.delProductProperty,
        // [zh] TypeScript/JS 语句：`params: { propertyId: data.propertyId },`
        params: { propertyId: data.propertyId },
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('已删除')`
      proxy.Message.success('已删除')
      // [zh] TypeScript/JS 语句：`loadCategory()`
      loadCategory()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] TypeScript/JS 语句：`onMounted(loadCategory)`
onMounted(loadCategory)
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.prop-card {` */
.prop-card {
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.prop-head {` */
.prop-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;

  /* [zh] 样式规则 `.prop-path {` */
  .prop-path {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.prop-tags {` */
.prop-tags {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.prop-tag {` */
.prop-tag {
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `padding: 6px 10px;` */
  padding: 6px 10px;
  /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
  border: 1px solid rgba(120, 120, 128, 0.24);
  /* [zh] 样式规则 `border-radius: 10px;` */
  border-radius: 10px;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.55);` */
  background: rgba(255, 255, 255, 0.55);
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: var(--m-ink-2);` */
  color: var(--m-ink-2);
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `.tag-del {` */
  .tag-del {
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
    /* [zh] 样式规则 `padding-left: 2px;` */
    padding-left: 2px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.prop-empty {` */
.prop-empty {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
/* [zh] 样式规则 `}` */
}
</style>
