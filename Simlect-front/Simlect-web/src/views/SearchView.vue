<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="category-page" :class="{ 'is-simlect-cate': !isDesktop }">

    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template v-if="!isDesktop">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="productSort simlect-goods-cate">
        <!-- [zh] 开始标签 `<header>` -->
        <header class="header header-fixed">
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="input" @click="router.push('/search-portal')">
            <!-- [zh] 开始标签 `<el-icon>` -->
            <el-icon class="search-icon"><Search /></el-icon>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="placeholder">搜索商品名称</span>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</header>` -->
        </header>

        <!-- [zh] 开始标签 `<el-skeleton>` -->
        <el-skeleton :loading="loading" animated :rows="8" class="scroll-box-skeleton">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template #default>
            <!-- [zh] 开始标签 `<div>` -->
            <div v-if="rootCategories.length" class="scroll-box">

              <!-- [zh] 开始标签 `<div>` -->
              <div class="aside-text-overlay">
                <!-- [zh] 开始标签 `<button>` -->
                <button
                  v-for="(root, index) in rootCategories"
                  :key="root.categoryId"
                  type="button"
                  class="item"
                  :class="{ on: navActive === index }"
                  @click="tapNav(index)"
                >
                  <!-- [zh] Mustache 插值表达式 -->
                  {{ root.categoryName }}
                <!-- [zh] 闭合标签 `</button>` -->
                </button>
              <!-- [zh] 闭合标签 `</div>` -->
              </div>

              <!-- [zh] 开始标签 `<aside>` -->
              <aside ref="asideRef" class="aside" :class="{ 'is-overflow': isAsideOverflow }" aria-label="一级分类" aria-hidden="true">
                <!-- [zh] 开始标签 `<button>` -->
                <button
                  v-for="(root, index) in rootCategories"
                  :key="'spacer-' + root.categoryId"
                  type="button"
                  class="item item-spacer"
                  :class="{ on: navActive === index }"
                  tabindex="-1"
                >
                  <!-- [zh] Mustache 插值表达式 -->
                  {{ root.categoryName }}
                <!-- [zh] 闭合标签 `</button>` -->
                </button>
              <!-- [zh] 闭合标签 `</aside>` -->
              </aside>

              <!-- [zh] 开始标签 `<div>` -->
              <div ref="conterRef" class="conter" @scroll.passive="onConterScroll">
                <!-- [zh] 开始标签 `<div>` -->
                <div class="conter-header-placeholder" />
                <!-- [zh] 开始标签 `<section>` -->
                <section
                  v-for="(root, index) in rootCategories"
                  :id="sectionId(index)"
                  :key="root.categoryId"
                  class="listw"
                >
                  <!-- [zh] 开始标签 `<div>` -->
                  <div class="title">
                    <!-- [zh] 开始标签 `<span>` -->
                    <span class="line" />
                    <!-- [zh] 开始标签 `<span>` -->
                    <span class="name">{{ root.categoryName }}</span>
                    <!-- [zh] 开始标签 `<span>` -->
                    <span class="line" />
                  <!-- [zh] 闭合标签 `</div>` -->
                  </div>
                  <!-- [zh] 开始标签 `<div>` -->
                  <div class="list">
                    <!-- [zh] 开始标签 `<button>` -->
                    <button type="button" class="item" @click="goCategory(root.categoryId)">
                      <!-- [zh] 开始标签 `<span>` -->
                      <span class="picture">
                        <!-- [zh] 开始标签 `<span>` -->
                        <span class="picture-fallback">{{ root.categoryName.slice(0, 1) }}</span>
                      <!-- [zh] 闭合标签 `</span>` -->
                      </span>
                      <!-- [zh] 开始标签 `<span>` -->
                      <span class="name line1">全部商品</span>
                    <!-- [zh] 闭合标签 `</button>` -->
                    </button>
                    <!-- [zh] 开始标签 `<button>` -->
                    <button
                      v-for="sub in root.children || []"
                      :key="sub.categoryId"
                      type="button"
                      class="item"
                      @click="goCategory(sub.categoryId)"
                    >
                      <!-- [zh] 开始标签 `<span>` -->
                      <span class="picture">
                        <!-- [zh] 开始标签 `<span>` -->
                        <span class="picture-fallback">{{ sub.categoryName.slice(0, 1) }}</span>
                      <!-- [zh] 闭合标签 `</span>` -->
                      </span>
                      <!-- [zh] 开始标签 `<span>` -->
                      <span class="name line1">{{ sub.categoryName }}</span>
                    <!-- [zh] 闭合标签 `</button>` -->
                    </button>
                  <!-- [zh] 闭合标签 `</div>` -->
                  </div>
                <!-- [zh] 闭合标签 `</section>` -->
                </section>
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<el-empty>` -->
            <el-empty v-else description="暂无分类数据" />
          </template>
        </el-skeleton>
      </div>
    </template>

    <div v-else class="card category-card">
      <div class="card-section-title">
        <h3>全部分类</h3>
        <span class="hint">共 {{ categoryCount }} 个分类</span>
      </div>

      <el-skeleton :loading="loading" animated :rows="6">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #default>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="rootCategories.length" class="category-list">
            <!-- [zh] 开始标签 `<section>` -->
            <section v-for="root in rootCategories" :key="root.categoryId" class="category-group">
              <!-- [zh] 开始标签 `<button>` -->
              <button type="button" class="group-title" @click="goCategory(root.categoryId)">
                <!-- [zh] Mustache 插值表达式 -->
                {{ root.categoryName }}
                <!-- [zh] 开始标签 `<el-icon>` -->
                <el-icon><ArrowRight /></el-icon>
              <!-- [zh] 闭合标签 `</button>` -->
              </button>
              <!-- [zh] 开始标签 `<div>` -->
              <div v-if="root.children?.length" class="sub-grid">
                <!-- [zh] 开始标签 `<button>` -->
                <button
                  v-for="sub in root.children"
                  :key="sub.categoryId"
                  type="button"
                  class="sub-item"
                  @click="goCategory(sub.categoryId)"
                >
                  <!-- [zh] Mustache 插值表达式 -->
                  {{ sub.categoryName }}
                <!-- [zh] 闭合标签 `</button>` -->
                </button>
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
              <!-- [zh] 开始标签 `<p>` -->
              <p v-else class="no-sub">点击查看该分类商品</p>
            <!-- [zh] 闭合标签 `</section>` -->
            </section>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<el-empty>` -->
          <el-empty v-else description="暂无分类数据" />
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowRight, Search } from '@element-plus/icons-vue';
import { productApi } from '@/api/modules';
import { countCategoryNodes, normalizeCategoryTree } from '@/utils/category';
import { useDevice } from '@/composables/useDevice';
import { usePageRefresh } from '@/composables/pullRefresh';

const router = useRouter();
const { isDesktop } = useDevice();
const loading = ref(true);
const rootCategories = ref<any[]>([]);
const navActive = ref(0);
const conterRef = ref<HTMLElement | null>(null);
const asideRef = ref<HTMLElement | null>(null);
const isAsideOverflow = ref(false);

const checkAsideOverflow = () => {
  const el = asideRef.value;
  if (!el) return;
  isAsideOverflow.value = el.scrollHeight > el.clientHeight;
};

const handleResize = () => {
  checkAsideOverflow();
};

const categoryCount = computed(() => countCategoryNodes(rootCategories.value));

const sectionId = (index: number) => `b${index}`;

const goCategory = (categoryId: string) => {
  router.push(`/category/${categoryId}`);
};

const tapNav = (index: number) => {
  navActive.value = index;
  const el = document.getElementById(sectionId(index));
  el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const onConterScroll = () => {
  const root = conterRef.value;
  if (!root || !rootCategories.value.length) return;
  const top = root.scrollTop;
  let active = 0;
  for (let i = 0; i < rootCategories.value.length; i++) {
    const el = document.getElementById(sectionId(i));
    if (el && el.offsetTop - root.offsetTop <= top + 80) active = i;
  }
  navActive.value = active;
};

const load = async () => {
  loading.value = true;
  try {
    const data = await productApi.loadCategory();
    rootCategories.value = normalizeCategoryTree(data);
    nextTick(checkAsideOverflow);
  } finally {
    loading.value = false;
  }
};

onMounted(load);
onMounted(() => window.addEventListener('resize', handleResize));
usePageRefresh(load);
onUnmounted(() => window.removeEventListener('resize', handleResize));
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.category-page {` */
.category-page {
  /* [zh] 样式规则 `padding-bottom: 8px;` */
  padding-bottom: 8px;

  /* [zh] 样式规则 `&.is-simlect-cate {` */
  &.is-simlect-cate {
    /* [zh] 样式规则 `padding-bottom: 0;` */
    padding-bottom: 0;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `min-height: 0;` */
    min-height: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort.simlect-goods-cate {` */
.productSort.simlect-goods-cate {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .header {` */
.productSort .header {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `padding: 8px $app-page-gutter;` */
  padding: 8px $app-page-gutter;
  /* [zh] 样式规则 `padding-top: calc(8px + env(safe-area-in` */
  padding-top: calc(8px + env(safe-area-inset-top, 0));
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `border-bottom: 1px solid #f5f5f5;` */
  border-bottom: 1px solid #f5f5f5;

  /* [zh] 样式规则 `&.header-fixed {` */
  &.header-fixed {
    /* [zh] 样式规则 `position: fixed;` */
    position: fixed;
    /* [zh] 样式规则 `top: 0;` */
    top: 0;
    /* [zh] 样式规则 `left: 0;` */
    left: 0;
    /* [zh] 样式规则 `right: 0;` */
    right: 0;
    /* [zh] 样式规则 `z-index: 100;` */
    z-index: 100;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .header .input {` */
.productSort .header .input {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 34px;` */
  height: 34px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 0 14px;` */
  padding: 0 14px;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 17px;` */
  border-radius: 17px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;

  /* [zh] 样式规则 `.search-icon {` */
  .search-icon {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.placeholder {` */
  .placeholder {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.scroll-box-skeleton {` */
.scroll-box-skeleton {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .scroll-box {` */
.productSort .scroll-box {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .aside-text-overlay {` */
.productSort .aside-text-overlay {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `left: 0;` */
  left: 0;
  /* [zh] 样式规则 `top: calc(50px + env(safe-area-inset-top` */
  top: calc(50px + env(safe-area-inset-top, 0));
  /* [zh] 样式规则 `bottom: 0;` */
  bottom: 0;
  /* [zh] 样式规则 `width: 90px;` */
  width: 90px;
  /* [zh] 样式规则 `z-index: 110;` */
  z-index: 110;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
  -webkit-overflow-scrolling: touch;
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
  /* [zh] 样式规则 `overscroll-behavior: contain;` */
  overscroll-behavior: contain;

  /* [zh] 样式规则 `&::-webkit-scrollbar {` */
  &::-webkit-scrollbar {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.item {` */
  .item {
    /* [zh] 样式规则 `display: block;` */
    display: block;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `min-height: 50px;` */
    min-height: 50px;
    /* [zh] 样式规则 `padding: 14px 6px;` */
    padding: 14px 6px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: #666666;` */
    color: #666666;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `line-height: 1.35;` */
    line-height: 1.35;
    /* [zh] 样式规则 `pointer-events: auto;` */
    pointer-events: auto;

    /* [zh] 样式规则 `&.on {` */
    &.on {
      /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.85);` */
      background: rgba(255, 255, 255, 0.85);
      /* [zh] 样式规则 `backdrop-filter: blur(8px);` */
      backdrop-filter: blur(8px);
      /* [zh] 样式规则 `-webkit-backdrop-filter: blur(8px);` */
      -webkit-backdrop-filter: blur(8px);
      /* [zh] 样式规则 `border-left: 3px solid $color-primary;` */
      border-left: 3px solid $color-primary;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `position: relative;` */
      position: relative;
      /* [zh] 样式规则 `z-index: 1;` */
      z-index: 1;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .aside {` */
.productSort .aside {
  /* [zh] 样式规则 `flex: 0 0 90px;` */
  flex: 0 0 90px;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
  /* [zh] 样式规则 `background: #f7f7f7;` */
  background: #f7f7f7;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
  -webkit-overflow-scrolling: touch;

  /* [zh] 样式规则 `&::-webkit-scrollbar {` */
  &::-webkit-scrollbar {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-overflow::after {` */
  &.is-overflow::after {
    /* [zh] 样式规则 `content: '';` */
    content: '';
    /* [zh] 样式规则 `position: sticky;` */
    position: sticky;
    /* [zh] 样式规则 `left: 0;` */
    left: 0;
    /* [zh] 样式规则 `bottom: 0;` */
    bottom: 0;
    /* [zh] 样式规则 `display: block;` */
    display: block;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 40px;` */
    height: 40px;
    /* [zh] 样式规则 `background: linear-gradient(to top, #f7f` */
    background: linear-gradient(to top, #f7f7f7, transparent);
    /* [zh] 样式规则 `pointer-events: none;` */
    pointer-events: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .aside .item-spacer {` */
.productSort .aside .item-spacer {
  /* [zh] 样式规则 `display: block;` */
  display: block;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `min-height: 50px;` */
  min-height: 50px;
  /* [zh] 样式规则 `padding: 14px 6px;` */
  padding: 14px 6px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: transparent;` */
  color: transparent;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `line-height: 1.35;` */
  line-height: 1.35;
  /* [zh] 样式规则 `pointer-events: none;` */
  pointer-events: none;
  /* [zh] 样式规则 `user-select: none;` */
  user-select: none;

  /* [zh] 样式规则 `&.on {` */
  &.on {
    /* [zh] 样式规则 `border-left: 3px solid $color-primary;` */
    border-left: 3px solid $color-primary;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `color: transparent;` */
    color: transparent;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .conter {` */
.productSort .conter {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
  /* [zh] 样式规则 `padding: 0 10px 24px;` */
  padding: 0 10px 24px;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
  -webkit-overflow-scrolling: touch;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.conter-header-placeholder {` */
.conter-header-placeholder {
  /* [zh] 样式规则 `height: calc(50px + env(safe-area-inset-` */
  height: calc(50px + env(safe-area-inset-top, 0));
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .listw {` */
.productSort .listw {
  /* [zh] 样式规则 `padding-top: 12px;` */
  padding-top: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .listw .title {` */
.productSort .listw .title {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `height: 44px;` */
  height: 44px;

  /* [zh] 样式规则 `.line {` */
  .line {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `max-width: 50px;` */
    max-width: 50px;
    /* [zh] 样式规则 `height: 1px;` */
    height: 1px;
    /* [zh] 样式规则 `background: #f0f0f0;` */
    background: #f0f0f0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.name {` */
  .name {
    /* [zh] 样式规则 `margin: 0 12px;` */
    margin: 0 12px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .list {` */
.productSort .list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px 4px;` */
  gap: 8px 4px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .list .item {` */
.productSort .list .item {
  /* [zh] 样式规则 `width: calc(33.33% - 4px);` */
  width: calc(33.33% - 4px);
  /* [zh] 样式规则 `margin-top: 8px;` */
  margin-top: 8px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .list .picture {` */
.productSort .list .picture {
  /* [zh] 样式规则 `width: 60px;` */
  width: 60px;
  /* [zh] 样式规则 `height: 60px;` */
  height: 60px;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .picture-fallback {` */
.productSort .picture-fallback {
  /* [zh] 样式规则 `font-size: 18px;` */
  font-size: 18px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.productSort .list .name {` */
.productSort .list .name {
  /* [zh] 样式规则 `margin-top: 6px;` */
  margin-top: 6px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `max-width: 72px;` */
  max-width: 72px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `line-height: 1.3;` */
  line-height: 1.3;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.line1 {` */
.line1 {
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `text-overflow: ellipsis;` */
  text-overflow: ellipsis;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.category-card {` */
.category-card {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;
  /* [zh] 样式规则 `border: 1px solid rgba(255, 255, 255, 0.` */
  border: 1px solid rgba(255, 255, 255, 0.85);
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.hint {` */
.hint {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `font-weight: 400;` */
  font-weight: 400;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.category-list {` */
.category-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.category-group {` */
.category-group {
  /* [zh] 样式规则 `padding-bottom: 14px;` */
  padding-bottom: 14px;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
    /* [zh] 样式规则 `padding-bottom: 0;` */
    padding-bottom: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.group-title {` */
.group-title {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `margin: 0 0 10px;` */
  margin: 0 0 10px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: none;` */
  background: none;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-grid {` */
.sub-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(4, 1fr);` */
  grid-template-columns: repeat(4, 1fr);
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sub-item {` */
.sub-item {
  /* [zh] 样式规则 `padding: 10px 8px;` */
  padding: 10px 8px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `background: $color-surface-inset;` */
  background: $color-surface-inset;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.no-sub {` */
.no-sub {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}
</style>
