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

<script setup>
import CategoryEdit from './CategoryEdit.vue'
import { ref, reactive, getCurrentInstance, nextTick, onMounted } from 'vue'
const { proxy } = getCurrentInstance()

const categoryList = ref([])
const loadCategory = async () => {
  let result = await proxy.Request({
    url: proxy.Api.loadCategory,
    params: {
      querySku: false,
    },
  })
  if (!result) {
    return
  }
  categoryList.value = normalizeCategoryTree(result.data || [])
}

const normalizeCategoryTree = (nodes) => {
  if (!Array.isArray(nodes)) {
    return []
  }
  return nodes.map((node) => {
    const pCategoryId =
      node.pCategoryId === null || node.pCategoryId === undefined || node.pCategoryId === ''
        ? '0'
        : String(node.pCategoryId)
    return {
      ...node,
      pCategoryId,
      children: normalizeCategoryTree(node.children || []),
    }
  })
}

const categoryEditRef = ref(null)
const showEdit = (data = {}) => {
  categoryEditRef.value.show(data)
}

const delCategory = (data) => {
  proxy.Confirm({
    message: `确定要删除【${data.categoryName}】吗?`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.delCategory,
        params: {
          categoryId: data.categoryId,
        },
      })
      if (!result) {
        return
      }
      loadCategory()
    },
  })
}

const allowDrop = (draggingNode, dropNode, type) => {

  if (type === 'inner') {
    return false
  }

  const draggingParentKey = draggingNode.parent?.key
  const dropParentKey = dropNode.parent?.key

  if (!draggingParentKey && !dropParentKey) {
    return true
  }
  if (
    draggingParentKey &&
    dropParentKey &&
    draggingParentKey === dropParentKey
  ) {
    return true
  }
  return false
}
const handleDrop = async (draggingNode, dropNode, dropType, event) => {
  let targetCategoryList = []
  if (dropNode.data.pCategoryId == '0') {
    targetCategoryList = categoryList.value
  } else {
    targetCategoryList = dropNode.parent.data.children
  }
  targetCategoryList = targetCategoryList.map((item) => {
    return item.categoryId
  })
  let result = await proxy.Request({
    url: proxy.Api.changeCategorySort,
    params: {
      categoryIds: targetCategoryList.join(','),
    },
  })
  if (!result) {
    return
  }
  proxy.Message.success('排序成功')
}
onMounted(() => {
  loadCategory()
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
