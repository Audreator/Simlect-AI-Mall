<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<button>` -->
    <button type="button" class="op-btn primary block" @click="showEdit({ pCategoryId: '0' })">新增一级分类</button>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="categoryList.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="cat in categoryList" :key="cat.categoryId" class="glass-card cat-card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="cat-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="cat-name">{{ cat.categoryName }}</span>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="cat-ops">
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="op-btn sm" @click="showEdit({ pCategoryId: cat.categoryId })">子分类</button>
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="op-btn sm" @click="showEdit(cat)">编辑</button>
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="op-btn sm danger" @click="delCategory(cat)">删除</button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="cat.children?.length" class="cat-children">
          <!-- [zh] 开始标签 `<div>` -->
          <div v-for="sub in cat.children" :key="sub.categoryId" class="sub-row">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="sub-name">{{ sub.categoryName }}</span>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="sub-ops">
              <!-- [zh] 开始标签 `<button>` -->
              <button type="button" class="op-btn sm" @click="showEdit(sub)">编辑</button>
              <!-- [zh] 开始标签 `<button>` -->
              <button type="button" class="op-btn sm danger" @click="delCategory(sub)">删除</button>
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
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else class="m-empty-tip">暂无分类</p>

    <!-- [zh] 开始标签 `<CategoryEdit>` -->
    <CategoryEdit ref="categoryEditRef" @reload="loadCategory" />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup>
import CategoryEdit from '@/views/product/CategoryEdit.vue'
import { ref, getCurrentInstance, onMounted } from 'vue'

const { proxy } = getCurrentInstance()
const categoryList = ref([])
const categoryEditRef = ref(null)

const normalizeCategoryTree = (nodes) => {
  if (!Array.isArray(nodes)) return []
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

const loadCategory = async () => {
  const result = await proxy.Request({
    url: proxy.Api.loadCategory,
    params: { querySku: false },
  })
  if (!result) return
  categoryList.value = normalizeCategoryTree(result.data || [])
}

const showEdit = (data = {}) => {
  categoryEditRef.value.show(data)
}

const delCategory = (data) => {
  proxy.Confirm({
    message: `确定要删除【${data.categoryName}】吗?`,
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api.delCategory,
        params: { categoryId: data.categoryId },
      })
      if (!result) return
      proxy.Message.success('已删除')
      loadCategory()
    },
  })
}

onMounted(loadCategory)
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.cat-card {` */
.cat-card {
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cat-head {` */
.cat-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;

  /* [zh] 样式规则 `.cat-name {` */
  .cat-name {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-ops {` */
  .cat-ops {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cat-children {` */
.cat-children {
  /* [zh] 样式规则 `margin-top: 10px;` */
  margin-top: 10px;
  /* [zh] 样式规则 `padding-top: 10px;` */
  padding-top: 10px;
  /* [zh] 样式规则 `border-top: 1px dashed rgba(120, 120, 12` */
  border-top: 1px dashed rgba(120, 120, 128, 0.2);
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-row {` */
.sub-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 8px 10px;` */
  padding: 8px 10px;
  /* [zh] 样式规则 `border-radius: 10px;` */
  border-radius: 10px;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.45);` */
  background: rgba(255, 255, 255, 0.45);

  /* [zh] 样式规则 `.sub-name {` */
  .sub-name {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sub-ops {` */
  .sub-ops {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
