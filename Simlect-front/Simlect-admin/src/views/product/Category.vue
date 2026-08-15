<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-button>` -->
  <el-button @click="showEdit({ pCategoryId: '0' })" type="primary">新增分类</el-button>
  <!-- [zh] 开始标签 `<el-tree>` -->
  <el-tree class="category-tree" :expand-on-click-node="false" :data="categoryList" draggable default-expand-all
    node-key="categoryId" :allow-drop="allowDrop" @node-drop="handleDrop">
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #default="{ node, data }">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="tree-node">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="node-label">{{ data.categoryName }}</div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="node-actions">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button v-if="data.pCategoryId === '0'" link type="primary" size="small"
            @click.stop="showEdit({ pCategoryId: data.categoryId })">
            <!-- [zh] 模板内容：`添加子分类` -->
            添加子分类
          <!-- [zh] 闭合标签 `</el-button>` -->
          </el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button link type="primary" size="small" @click.stop="showEdit(data)">
            <!-- [zh] 模板内容：`编辑` -->
            编辑
          <!-- [zh] 闭合标签 `</el-button>` -->
          </el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button link type="danger" size="small" @click.stop="delCategory(data)">
            <!-- [zh] 模板内容：`删除` -->
            删除
          <!-- [zh] 闭合标签 `</el-button>` -->
          </el-button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>
  </el-tree>
  <CategoryEdit ref="categoryEditRef" @reload="loadCategory"></CategoryEdit>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./CategoryEdit.vue`
import CategoryEdit from './CategoryEdit.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick, onMounted } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()

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
      // [zh] TypeScript/JS 语句：`querySku: false,`
      querySku: false,
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
  // [zh] TypeScript/JS 语句：`categoryList.value = normalizeCategoryTree(result.data `
  categoryList.value = normalizeCategoryTree(result.data || [])
// [zh] 语法符号
}

// [zh] 声明变量 `normalizeCategoryTree`
const normalizeCategoryTree = (nodes) => {
  // [zh] 条件判断 if
  if (!Array.isArray(nodes)) {
    // [zh] return 返回
    return []
  // [zh] 语法符号
  }
  // [zh] return 返回
  return nodes.map((node) => {
    // [zh] 声明变量 `pCategoryId`
    const pCategoryId =
      // [zh] TypeScript/JS 语句：`node.pCategoryId === null || node.pCategoryId === undef`
      node.pCategoryId === null || node.pCategoryId === undefined || node.pCategoryId === ''
        // [zh] TypeScript/JS 语句：`? '0'`
        ? '0'
        // [zh] TypeScript/JS 语句：`: String(node.pCategoryId)`
        : String(node.pCategoryId)
    // [zh] return 返回
    return {
      // [zh] TypeScript/JS 语句：`...node,`
      ...node,
      // [zh] TypeScript/JS 语句：`pCategoryId,`
      pCategoryId,
      // [zh] TypeScript/JS 语句：`children: normalizeCategoryTree(node.children || []),`
      children: normalizeCategoryTree(node.children || []),
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `categoryEditRef`
const categoryEditRef = ref(null)
// [zh] 声明变量 `showEdit`
const showEdit = (data = {}) => {
  // [zh] TypeScript/JS 语句：`categoryEditRef.value.show(data)`
  categoryEditRef.value.show(data)
// [zh] 语法符号
}

// [zh] 声明变量 `delCategory`
const delCategory = (data) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定要删除【${data.categoryName}】吗?`,`
    message: `确定要删除【${data.categoryName}】吗?`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.delCategory,`
        url: proxy.Api.delCategory,
        // [zh] 代码块开始
        params: {
          // [zh] TypeScript/JS 语句：`categoryId: data.categoryId,`
          categoryId: data.categoryId,
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
      // [zh] TypeScript/JS 语句：`loadCategory()`
      loadCategory()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `allowDrop`
const allowDrop = (draggingNode, dropNode, type) => {

  // [zh] 条件判断 if
  if (type === 'inner') {
    // [zh] return 返回
    return false
  // [zh] 语法符号
  }

  // [zh] 声明变量 `draggingParentKey`
  const draggingParentKey = draggingNode.parent?.key
  // [zh] 声明变量 `dropParentKey`
  const dropParentKey = dropNode.parent?.key

  // [zh] 条件判断 if
  if (!draggingParentKey && !dropParentKey) {
    // [zh] return 返回
    return true
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (
    // [zh] TypeScript/JS 语句：`draggingParentKey &&`
    draggingParentKey &&
    // [zh] TypeScript/JS 语句：`dropParentKey &&`
    dropParentKey &&
    // [zh] TypeScript/JS 语句：`draggingParentKey === dropParentKey`
    draggingParentKey === dropParentKey
  // [zh] 代码块开始
  ) {
    // [zh] return 返回
    return true
  // [zh] 语法符号
  }
  // [zh] return 返回
  return false
// [zh] 语法符号
}
// [zh] 声明变量 `handleDrop`
const handleDrop = async (draggingNode, dropNode, dropType, event) => {
  // [zh] 声明变量 `targetCategoryList`
  let targetCategoryList = []
  // [zh] 条件判断 if
  if (dropNode.data.pCategoryId == '0') {
    // [zh] TypeScript/JS 语句：`targetCategoryList = categoryList.value`
    targetCategoryList = categoryList.value
  // [zh] 代码块开始
  } else {
    // [zh] TypeScript/JS 语句：`targetCategoryList = dropNode.parent.data.children`
    targetCategoryList = dropNode.parent.data.children
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  targetCategoryList = targetCategoryList.map((item) => {
    // [zh] return 返回
    return item.categoryId
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.changeCategorySort,`
    url: proxy.Api.changeCategorySort,
    // [zh] 代码块开始
    params: {
      // [zh] TypeScript/JS 语句：`categoryIds: targetCategoryList.join(','),`
      categoryIds: targetCategoryList.join(','),
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
  // [zh] TypeScript/JS 语句：`proxy.Message.success('排序成功')`
  proxy.Message.success('排序成功')
// [zh] 语法符号
}
// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`loadCategory()`
  loadCategory()
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.category-tree {` */
.category-tree {
  /* [zh] 样式规则 `margin-top: 10px;` */
  margin-top: 10px;
  /* [zh] 样式规则 `padding-right: 20px;` */
  padding-right: 20px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: 960px;` */
  max-width: 960px;
  /* [zh] 样式规则 `height: calc(100% - 42px);` */
  height: calc(100% - 42px);
  /* [zh] 样式规则 `overflow: auto;` */
  overflow: auto;

  /* [zh] 样式规则 `:deep(.el-tree-node__content) {` */
  :deep(.el-tree-node__content) {
    /* [zh] 样式规则 `padding: 10px 8px;` */
    padding: 10px 8px;
    /* [zh] 样式规则 `height: auto;` */
    height: auto;
    /* [zh] 样式规则 `min-height: 44px;` */
    min-height: 44px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.tree-node {` */
  .tree-node {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;

    /* [zh] 样式规则 `.node-label {` */
    .node-label {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `min-width: 0;` */
      min-width: 0;
      /* [zh] 样式规则 `overflow: hidden;` */
      overflow: hidden;
      /* [zh] 样式规则 `text-overflow: ellipsis;` */
      text-overflow: ellipsis;
      /* [zh] 样式规则 `white-space: nowrap;` */
      white-space: nowrap;
      /* [zh] 样式规则 `font-size: 14px;` */
      font-size: 14px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.node-actions {` */
    .node-actions {
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
      /* [zh] 样式规则 `display: inline-flex;` */
      display: inline-flex;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
      /* [zh] 样式规则 `gap: 4px;` */
      gap: 4px;
      /* [zh] 样式规则 `flex-wrap: nowrap;` */
      flex-wrap: nowrap;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
