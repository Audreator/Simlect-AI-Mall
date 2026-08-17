<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-tree>` -->
  <el-tree class="category-tree" :expand-on-click-node="false" default-expand-all :data="categoryList"
    node-key="categoryId">
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #default="{ node, data }">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="tree-node">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="node-label">{{ data.categoryName }}</div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sku-name">
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-for="tag in data.productPropertyList" :key="tag.propertId" closable @close.stop="delProperty(tag)"
            @click="showEdit(data, tag)">
            <!-- [zh] Mustache 插值表达式 -->
            {{ tag.propertyName }}<template v-if="tag.coverType == 1">(含图)</template>
          <!-- [zh] 闭合标签 `</el-tag>` -->
          </el-tag>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<el-tooltip>` -->
        <el-tooltip effect="dark" content="新增SKU属性" placement="top">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="iconfont icon-add" v-if="data.pCategoryId != '0'" @click="showEdit(data)"></div>
        <!-- [zh] 闭合标签 `</el-tooltip>` -->
        </el-tooltip>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>
  </el-tree>
  <ProductPropertyEdit ref="productPropertyEditRef" @reload="loadCategory"></ProductPropertyEdit>
</template>

<script setup>
import ProductPropertyEdit from './ProductPropertyEdit.vue'
import { ref, reactive, getCurrentInstance, nextTick, onMounted } from 'vue'
const { proxy } = getCurrentInstance()

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

const productPropertyEditRef = ref(null)
const showEdit = ({ categoryId, pCategoryId }, tag) => {
  productPropertyEditRef.value.show({ categoryId, pCategoryId, ...tag })
}

const delProperty = (data) => {
  proxy.Confirm({
    message: `确定要删除【${data.propertyName}】吗?`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.delProductProperty,
        params: {
          propertyId: data.propertyId,
        },
      })
      if (!result) {
        return
      }
      loadCategory()
    },
  })
}
onMounted(() => {
  loadCategory()
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.category-tree {` */
.category-tree {
  /* [zh] 样式规则 `width: 700px;` */
  width: 700px;
  /* [zh] 样式规则 `overflow: auto;` */
  overflow: auto;

  /* [zh] 样式规则 `:deep(.el-tree-node__content) {` */
  :deep(.el-tree-node__content) {
    /* [zh] 样式规则 `padding: 20px 0px;` */
    padding: 20px 0px;
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
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;

    /* [zh] 样式规则 `.node-label {` */
    .node-label {
      /* [zh] 样式规则 `width: 200px;` */
      width: 200px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.sku-name {` */
    .sku-name {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;

      /* [zh] 样式规则 `:deep(.el-tag) {` */
      :deep(.el-tag) {
        /* [zh] 样式规则 `margin-right: 10px;` */
        margin-right: 10px;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.icon-add {` */
    .icon-add {
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
      /* [zh] 样式规则 `height: 22px;` */
      height: 22px;
      /* [zh] 样式规则 `width: 22px;` */
      width: 22px;
      /* [zh] 样式规则 `font-size: 14px;` */
      font-size: 14px;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
      /* [zh] 样式规则 `justify-content: center;` */
      justify-content: center;
      /* [zh] 样式规则 `border-radius: 5px;` */
      border-radius: 5px;
      /* [zh] 样式规则 `margin: 0px 10px;` */
      margin: 0px 10px;
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
/* [zh] 样式规则 `}` */
}
</style>
