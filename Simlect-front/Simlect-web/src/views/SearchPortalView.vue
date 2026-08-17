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

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { Close, Search } from '@element-plus/icons-vue';
import { confirmAction } from '@/utils/confirm';
import { useSearchStore } from '@/stores/search';
import { searchApi } from '@/api/modules';
import { useAuthStore } from '@/stores/auth';
import { pickProductCover, resolveImageUrl } from '@/utils/image';
import { DEFAULT_HOT_SEARCH_WORDS } from '@/constants/searchHotWords';

const router = useRouter();
const searchStore = useSearchStore();
const authStore = useAuthStore();
const keyword = ref('');
const hotWords = ref<string[]>([]);
const guessWords = ref<string[]>([]);
const recentWords = ref<string[]>([]);
const recommendProducts = ref<any[]>([]);
const inputRef = ref();

const refreshRecent = async () => {
  if (!authStore.isLoggedIn) {
    recentWords.value = [];
    return;
  }
  try {
    const list = await searchApi.loadRecentKeywords();
    recentWords.value = Array.isArray(list) ? list.slice(0, 10) : [];
  } catch {
    recentWords.value = [];
  }
};

const submitSearch = async (word?: string) => {
  const keyWords = (word ?? keyword.value).trim();
  if (!keyWords) return;
  if (authStore.isLoggedIn) {
    try {
      await searchApi.saveKeyword(keyWords);
    } catch {

    }
  }
  searchStore.setSearch({ keyWords, categoryId: '' });
  router.push({ path: '/search-result', query: { q: keyWords } });
};

const searchByWord = (word: string) => {
  keyword.value = word;
  submitSearch(word);
};

const clearRecent = async () => {
  const ok = await confirmAction('确定要清空全部最近搜索记录吗？', {
    title: '清空记录',
    confirmButtonText: '清空'
  });
  if (!ok) return;
  if (authStore.isLoggedIn) {
    await searchApi.clearRecentKeywords();
  }
  await refreshRecent();
};

const removeRecent = async (word: string) => {
  if (authStore.isLoggedIn) {
    await searchApi.removeRecentKeyword(word);
  }
  await refreshRecent();
};

const coverOf = (p: Record<string, unknown>) =>
  resolveImageUrl(pickProductCover(p), { useThumbnail: true });

onMounted(async () => {
  try {
    const hot = await searchApi.loadHotKeywords();
    hotWords.value = Array.isArray(hot) && hot.length ? hot : [...DEFAULT_HOT_SEARCH_WORDS];
  } catch {
    hotWords.value = [...DEFAULT_HOT_SEARCH_WORDS];
  }
  await refreshRecent();
  if (authStore.isLoggedIn) {
    try {
      guessWords.value = (await searchApi.loadGuessKeywords()) || [];
      recommendProducts.value = (await searchApi.loadRecommendProducts(6)) || [];
    } catch {
      guessWords.value = [];
      recommendProducts.value = [];
    }
  }
  inputRef.value?.focus?.();
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
