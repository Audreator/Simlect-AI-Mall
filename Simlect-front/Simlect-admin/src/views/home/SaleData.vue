<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<section>` -->
  <section class="ops-section">
    <!-- [zh] 开始标签 `<header>` -->
    <header class="section-head">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="section-title">运营待办</h3>
    <!-- [zh] 闭合标签 `</header>` -->
    </header>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="ops-layout">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="ops-panel card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="panel-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="panel-title">待发货</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="panel-body">
          <!-- [zh] 开始标签 `<ShippedOrder>` -->
          <ShippedOrder />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="ops-panel card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="panel-head">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="panel-title">库存预警</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="panel-body">
          <!-- [zh] 开始标签 `<LessStockData>` -->
          <LessStockData />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</section>` -->
  </section>
</template>

<script setup>
import LessStockData from './LessStockData.vue'
import ShippedOrder from './ShippedOrder.vue'
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.card {` */
.card {
  /* [zh] 样式规则 `background: var(--surface);` */
  background: var(--surface);
  /* [zh] 样式规则 `border: 1px solid var(--header-border);` */
  border: 1px solid var(--header-border);
  /* [zh] 样式规则 `box-shadow: var(--shadow-card);` */
  box-shadow: var(--shadow-card);
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ops-section {` */
.ops-section {
  /* [zh] 样式规则 `margin-top: 10px;` */
  margin-top: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.section-head {` */
.section-head {
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;

  /* [zh] 样式规则 `.section-title {` */
  .section-title {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--text);` */
    color: var(--text);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ops-layout {` */
.ops-layout {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: minmax(0, 1.2fr) ` */
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ops-panel {` */
.ops-panel {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `height: 196px;` */
  height: 196px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `.panel-head {` */
  .panel-head {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `padding: 8px 12px;` */
    padding: 8px 12px;
    /* [zh] 样式规则 `border-bottom: 1px solid var(--header-bo` */
    border-bottom: 1px solid var(--header-border);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.panel-title {` */
  .panel-title {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--text2);` */
    color: var(--text2);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.panel-body {` */
  .panel-body {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `padding: 4px 8px 6px;` */
    padding: 4px 8px 6px;

    /* [zh] 样式规则 `:deep(.table-height) {` */
    :deep(.table-height) {
      /* [zh] 样式规则 `height: 100%;` */
      height: 100%;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(.el-table) {` */
    :deep(.el-table) {
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(.el-table .el-table__cell) {` */
    :deep(.el-table .el-table__cell) {
      /* [zh] 样式规则 `padding: 6px 0;` */
      padding: 6px 0;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(.pagination) {` */
    :deep(.pagination) {
      /* [zh] 样式规则 `display: none;` */
      display: none;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (max-width: 960px) {` */
@media (max-width: 960px) {
  /* [zh] 样式规则 `.ops-layout {` */
  .ops-layout {
    /* [zh] 样式规则 `grid-template-columns: 1fr;` */
    grid-template-columns: 1fr;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
