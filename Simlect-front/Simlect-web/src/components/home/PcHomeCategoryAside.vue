<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<aside>` -->
  <aside
    class="pc-cat-aside ignore"
    aria-label="商品分类"
    @mouseenter="cancelClose"
    @mouseleave="scheduleClose"
  >
    <!-- [zh] 开始标签 `<div>` -->
    <div class="cat-head">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="cat-head-title">商品分类</span>
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/search" class="cat-head-more">全部分类</RouterLink>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<ul>` -->
    <ul v-if="categories.length" class="cat-list">
      <!-- [zh] 开始标签 `<li>` -->
      <li
        v-for="cat in categories"
        :key="cat.categoryId"
        class="cat-item"
        :class="{ active: activeId === cat.categoryId }"
        @mouseenter="setActive(cat.categoryId)"
      >
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="cat-link" @click="goCategory(cat.categoryId)">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="cat-name">{{ cat.categoryName }}</span>
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon v-if="cat.children?.length" class="cat-arrow"><ArrowRight /></el-icon>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
        <!-- [zh] 开始标签 `<div>` -->
        <div
          v-if="activeId === cat.categoryId && cat.children?.length"
          class="cat-flyout"
          @mouseenter="cancelClose"
        >
          <!-- [zh] 开始标签 `<div>` -->
          <div class="flyout-inner">
            <!-- [zh] 开始标签 `<h4>` -->
            <h4 class="flyout-title">{{ cat.categoryName }}</h4>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="flyout-grid">
              <!-- [zh] 开始标签 `<button>` -->
              <button
                v-for="sub in cat.children"
                :key="sub.categoryId"
                type="button"
                class="flyout-link"
                @click="goCategory(sub.categoryId)"
              >
                <!-- [zh] Mustache 插值表达式 -->
                {{ sub.categoryName }}
              <!-- [zh] 闭合标签 `</button>` -->
              </button>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="flyout-all" @click="goCategory(cat.categoryId)">
              <!-- [zh] 模板内容：`查看全部` -->
              查看全部
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</li>` -->
      </li>
    <!-- [zh] 闭合标签 `</ul>` -->
    </ul>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else class="cat-empty">暂无分类</p>
  <!-- [zh] 闭合标签 `</aside>` -->
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ArrowRight } from '@element-plus/icons-vue';

const props = defineProps<{
  categories: Array<{
    categoryId: string;
    categoryName: string;
    children?: Array<{ categoryId: string; categoryName: string }>;
  }>;
}>();

const router = useRouter();
const activeId = ref<string | null>(null);
let closeTimer: ReturnType<typeof setTimeout> | null = null;

const cancelClose = () => {
  if (closeTimer) {
    clearTimeout(closeTimer);
    closeTimer = null;
  }
};

const scheduleClose = () => {
  cancelClose();
  closeTimer = setTimeout(() => {
    activeId.value = null;
    closeTimer = null;
  }, 280);
};

const setActive = (id: string) => {
  cancelClose();
  const cat = props.categories.find((c) => c.categoryId === id);
  activeId.value = cat?.children?.length ? id : null;
};

const goCategory = (id: string) => {
  cancelClose();
  activeId.value = null;
  router.push(`/category/${id}`);
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-cat-aside.ignore {` */
.pc-cat-aside.ignore {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `width: 200px;` */
  width: 200px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
  border: 1px solid $color-border-gray;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `min-height: 360px;` */
  min-height: 360px;
  /* [zh] 样式规则 `overflow: visible;` */
  overflow: visible;
  /* [zh] 样式规则 `z-index: 5;` */
  z-index: 5;

  /* [zh] 样式规则 `.cat-head {` */
  .cat-head {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `padding: 10px 12px;` */
    padding: 10px 12px;
    /* [zh] 样式规则 `border-bottom: 1px solid $color-border-g` */
    border-bottom: 1px solid $color-border-gray;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-head-title {` */
  .cat-head-title {
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-primary;` */
    color: $color-text-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-head-more {` */
  .cat-head-more {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-list {` */
  .cat-list {
    /* [zh] 样式规则 `list-style: none;` */
    list-style: none;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `padding: 4px 0;` */
    padding: 4px 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-item {` */
  .cat-item {
    /* [zh] 样式规则 `position: relative;` */
    position: relative;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-item.active .cat-link {` */
  .cat-item.active .cat-link {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `background: $color-cat-hover-bg;` */
    background: $color-cat-hover-bg;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-link {` */
  .cat-link {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 32px;` */
    height: 32px;
    /* [zh] 样式规则 `padding: 0 12px;` */
    padding: 0 12px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: $color-text-primary;` */
    color: $color-text-primary;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `text-align: left;` */
    text-align: left;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `background: $color-cat-hover-bg;` */
      background: $color-cat-hover-bg;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-name {` */
  .cat-name {
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
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `line-height: 32px;` */
    line-height: 32px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-arrow {` */
  .cat-arrow {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-empty {` */
  .cat-empty {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `padding: 16px 12px;` */
    padding: 16px 12px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cat-flyout {` */
  .cat-flyout {
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `left: 100%;` */
    left: 100%;
    /* [zh] 样式规则 `top: 0;` */
    top: 0;
    /* [zh] 样式规则 `z-index: 30;` */
    z-index: 30;
    /* [zh] 样式规则 `padding-left: 8px;` */
    padding-left: 8px;
    /* [zh] 样式规则 `margin-left: -4px;` */
    margin-left: -4px;

    /* [zh] 样式规则 `&::before {` */
    &::before {
      /* [zh] 样式规则 `content: '';` */
      content: '';
      /* [zh] 样式规则 `position: absolute;` */
      position: absolute;
      /* [zh] 样式规则 `left: 0;` */
      left: 0;
      /* [zh] 样式规则 `top: 0;` */
      top: 0;
      /* [zh] 样式规则 `width: 8px;` */
      width: 8px;
      /* [zh] 样式规则 `height: 100%;` */
      height: 100%;
      /* [zh] 样式规则 `min-height: 32px;` */
      min-height: 32px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.flyout-inner {` */
  .flyout-inner {
    /* [zh] 样式规则 `width: 480px;` */
    width: 480px;
    /* [zh] 样式规则 `max-height: 400px;` */
    max-height: 400px;
    /* [zh] 样式规则 `overflow-y: auto;` */
    overflow-y: auto;
    /* [zh] 样式规则 `padding: 14px 16px;` */
    padding: 14px 16px;
    /* [zh] 样式规则 `background: $color-card;` */
    background: $color-card;
    /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
    border: 1px solid $color-border-gray;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
    /* [zh] 样式规则 `box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1` */
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.flyout-title {` */
  .flyout-title {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-primary;` */
    color: $color-text-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.flyout-grid {` */
  .flyout-grid {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;
    /* [zh] 样式规则 `gap: 8px 20px;` */
    gap: 8px 20px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.flyout-link {` */
  .flyout-link {
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `padding: 2px 0;` */
    padding: 2px 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.flyout-all {` */
  .flyout-all {
    /* [zh] 样式规则 `margin-top: 12px;` */
    margin-top: 12px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
