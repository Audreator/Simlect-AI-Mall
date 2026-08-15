<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="search-portal simlect-search-portal">
    <!-- [zh] 开始标签 `<header>` -->
    <header class="simlect-search-sticky">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="simlect-search-box">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input
          ref="inputRef"
          v-model="keyword"
          placeholder="搜索商品名称"
          clearable
          class="keyword-input"
          @keyup.enter="submitSearch"
        >
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template #prefix>
            <!-- [zh] 开始标签 `<el-icon>` -->
            <el-icon class="input-icon"><Search /></el-icon>
          </template>
        </el-input>
        <button type="button" class="simlect-search-btn" @click="submitSearch()">搜索</button>
      </div>
    </header>

    <div class="simlect-search-body">
      <section v-if="guessWords.length" class="section">
        <div class="section-head">
          <h3 class="section-title">猜你想搜</h3>
        </div>
        <div class="tag-cloud">
          <button
            v-for="(word, index) in guessWords"
            :key="`guess-${index}`"
            type="button"
            class="tag-chip"
            @click="searchByWord(word)"
          >
            {{ word }}
          </button>
        </div>
      </section>

      <section v-if="hotWords.length" class="section">
        <div class="section-head">
          <h3 class="section-title">大家都在搜</h3>
        </div>
        <div class="tag-cloud">
          <button
            v-for="(word, index) in hotWords"
            :key="`hot-${index}`"
            type="button"
            class="tag-chip hot"
            @click="searchByWord(word)"
          >
            <span v-if="index < 3" class="rank">{{ index + 1 }}</span>
            {{ word }}
          </button>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <h3 class="section-title">最近搜索</h3>
          <button
            v-if="recentWords.length"
            type="button"
            class="clear-btn"
            @click="clearRecent"
          >
            清空最近搜索记录
          </button>
        </div>
        <div v-if="recentWords.length" class="tag-cloud">
          <button
            v-for="word in recentWords"
            :key="`recent-${word}`"
            type="button"
            class="tag-chip recent"
            @click="searchByWord(word)"
          >
            {{ word }}
            <el-icon class="remove-icon" @click.stop="removeRecent(word)"><Close /></el-icon>
          </button>
        </div>
        <p v-else class="empty-tip">暂无搜索记录</p>
      </section>

      <section v-if="recommendProducts.length" class="section">
        <div class="section-head">
          <h3 class="section-title">推荐精选</h3>
        </div>
        <div class="recommend-grid">
          <RouterLink
            v-for="p in recommendProducts"
            :key="p.productId"
            class="recommend-item"
            :to="`/product/${p.productId}`"
          >
            <img :src="coverOf(p)" alt="" class="recommend-cover" />
            <p class="recommend-name">{{ p.productName }}</p>
            <p class="recommend-price">¥{{ p.minPrice }}</p>
          </RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { onMounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { RouterLink, useRouter } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { Close, Search } from '@element-plus/icons-vue';
// [zh] 导入 `@/utils/confirm`
import { confirmAction } from '@/utils/confirm';
// [zh] 导入 `@/stores/search`
import { useSearchStore } from '@/stores/search';
// [zh] 导入 `@/api/modules`
import { searchApi } from '@/api/modules';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/utils/image`
import { pickProductCover, resolveImageUrl } from '@/utils/image';
// [zh] 导入 `@/constants/searchHotWords`
import { DEFAULT_HOT_SEARCH_WORDS } from '@/constants/searchHotWords';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `searchStore`
const searchStore = useSearchStore();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `keyword`
const keyword = ref('');
// [zh] 声明变量 `hotWords`
const hotWords = ref<string[]>([]);
// [zh] 声明变量 `guessWords`
const guessWords = ref<string[]>([]);
// [zh] 声明变量 `recentWords`
const recentWords = ref<string[]>([]);
// [zh] 声明变量 `recommendProducts`
const recommendProducts = ref<any[]>([]);
// [zh] 声明变量 `inputRef`
const inputRef = ref();

// [zh] 声明变量 `refreshRecent`
const refreshRecent = async () => {
  // [zh] 条件判断 if
  if (!authStore.isLoggedIn) {
    // [zh] 语句
    recentWords.value = [];
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `list`
    const list = await searchApi.loadRecentKeywords();
    // [zh] 语句
    recentWords.value = Array.isArray(list) ? list.slice(0, 10) : [];
  // [zh] 代码块开始
  } catch {
    // [zh] 语句
    recentWords.value = [];
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `submitSearch`
const submitSearch = async (word?: string) => {
  // [zh] 声明变量 `keyWords`
  const keyWords = (word ?? keyword.value).trim();
  // [zh] 条件判断 if
  if (!keyWords) return;
  // [zh] 条件判断 if
  if (authStore.isLoggedIn) {
    // [zh] 代码块开始
    try {
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await searchApi.saveKeyword(keyWords);
    // [zh] 代码块开始
    } catch {

    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] 语句
  searchStore.setSearch({ keyWords, categoryId: '' });
  // [zh] 语句
  router.push({ path: '/search-result', query: { q: keyWords } });
// [zh] 语句
};

// [zh] 声明变量 `searchByWord`
const searchByWord = (word: string) => {
  // [zh] 语句
  keyword.value = word;
  // [zh] 语句
  submitSearch(word);
// [zh] 语句
};

// [zh] 声明变量 `clearRecent`
const clearRecent = async () => {
  // [zh] 声明变量 `ok`
  const ok = await confirmAction('确定要清空全部最近搜索记录吗？', {
    // [zh] TypeScript/JS 语句：`title: '清空记录',`
    title: '清空记录',
    // [zh] TypeScript/JS 语句：`confirmButtonText: '清空'`
    confirmButtonText: '清空'
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (!ok) return;
  // [zh] 条件判断 if
  if (authStore.isLoggedIn) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await searchApi.clearRecentKeywords();
  // [zh] 语法符号
  }
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await refreshRecent();
// [zh] 语句
};

// [zh] 声明变量 `removeRecent`
const removeRecent = async (word: string) => {
  // [zh] 条件判断 if
  if (authStore.isLoggedIn) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await searchApi.removeRecentKeyword(word);
  // [zh] 语法符号
  }
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await refreshRecent();
// [zh] 语句
};

// [zh] 声明变量 `coverOf`
const coverOf = (p: Record<string, unknown>) =>
  // [zh] 语句
  resolveImageUrl(pickProductCover(p), { useThumbnail: true });

// [zh] 代码块开始
onMounted(async () => {
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `hot`
    const hot = await searchApi.loadHotKeywords();
    // [zh] 语句
    hotWords.value = Array.isArray(hot) && hot.length ? hot : [...DEFAULT_HOT_SEARCH_WORDS];
  // [zh] 代码块开始
  } catch {
    // [zh] 语句
    hotWords.value = [...DEFAULT_HOT_SEARCH_WORDS];
  // [zh] 语法符号
  }
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await refreshRecent();
  // [zh] 条件判断 if
  if (authStore.isLoggedIn) {
    // [zh] 代码块开始
    try {
      // [zh] 语句
      guessWords.value = (await searchApi.loadGuessKeywords()) || [];
      // [zh] 语句
      recommendProducts.value = (await searchApi.loadRecommendProducts(6)) || [];
    // [zh] 代码块开始
    } catch {
      // [zh] 语句
      guessWords.value = [];
      // [zh] 语句
      recommendProducts.value = [];
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] 语句
  inputRef.value?.focus?.();
// [zh] 语句
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.section {` */
.section {
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.section-head {` */
.section-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.section-title {` */
.section-title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.clear-btn {` */
.clear-btn {
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: none;` */
  background: none;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: color $transition-fast;` */
  transition: color $transition-fast;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tag-cloud {` */
.tag-cloud {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tag-chip {` */
.tag-chip {
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `border-radius: 999px;` */
  border-radius: 999px;
  /* [zh] 样式规则 `padding: 6px 12px;` */
  padding: 6px 12px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `transition:` */
  transition:
    /* [zh] 样式声明 */
    border-color $transition-fast,
    /* [zh] 样式声明 */
    color $transition-fast,
    /* [zh] 样式声明 */
    background $transition-fast;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `border-color: $color-primary;` */
    border-color: $color-primary;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `background: $color-primary-soft;` */
    background: $color-primary-soft;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.hot .rank {` */
  &.hot .rank {
    /* [zh] 样式规则 `color: $color-gold;` */
    color: $color-gold;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `margin-right: 2px;` */
    margin-right: 2px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.remove-icon {` */
.remove-icon {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `opacity: 0.6;` */
  opacity: 0.6;
  /* [zh] 样式规则 `transition: opacity $transition-fast;` */
  transition: opacity $transition-fast;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `opacity: 1;` */
    opacity: 1;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.empty-tip {` */
.empty-tip {
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.recommend-grid {` */
.recommend-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(2, 1fr);` */
  grid-template-columns: repeat(2, 1fr);
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.recommend-item {` */
.recommend-item {
  /* [zh] 样式规则 `text-decoration: none;` */
  text-decoration: none;
  /* [zh] 样式规则 `color: inherit;` */
  color: inherit;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `transition: transform 0.4s cubic-bezier(` */
  transition: transform 0.4s cubic-bezier(0.25, 0.1, 0.25, 1), box-shadow 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `transform: translateY(-3px);` */
    transform: translateY(-3px);
    /* [zh] 样式规则 `box-shadow: $shadow-card-hover;` */
    box-shadow: $shadow-card-hover;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.recommend-cover {` */
.recommend-cover {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `aspect-ratio: 1;` */
  aspect-ratio: 1;
  /* [zh] 样式规则 `object-fit: cover;` */
  object-fit: cover;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.recommend-name {` */
.recommend-name {
  /* [zh] 样式规则 `margin: 6px 8px 0;` */
  margin: 6px 8px 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `text-overflow: ellipsis;` */
  text-overflow: ellipsis;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.recommend-price {` */
.recommend-price {
  /* [zh] 样式规则 `margin: 4px 8px 8px;` */
  margin: 4px 8px 8px;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
/* [zh] 样式规则 `}` */
}
</style>
