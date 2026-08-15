<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="comments-page product-page-compact card">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="card-section-title">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3>全部评价</h3>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="count">共 {{ displayTotal }} 条</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="level-tabs toolbar-row toolbar-row--chips">
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-for="tab in levelTabs"
        :key="tab.value"
        type="button"
        class="toolbar-chip"
        :class="{ active: commentLevel === tab.value }"
        @click="commentLevel = tab.value"
      >
        <!-- [zh] Mustache 插值表达式 -->
        {{ tab.label }}
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="scrollRoot" class="comment-scroll">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="displayList.length" class="comment-list">
        <!-- [zh] 开始标签 `<article>` -->
        <article v-for="c in displayList" :key="c.orderId" class="comment-item">
          <!-- [zh] 开始标签 `<UserAvatar>` -->
          <UserAvatar :avatar="c.avatar" :size="40" />
          <!-- [zh] 开始标签 `<div>` -->
          <div class="comment-body">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="comment-head">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="user">{{ maskCommenterName(c.nickName) }}</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span v-if="getLevelBadge(c.userId)" class="cmt-level-tag" :class="levelClass(getLevelBadge(c.userId)!.levelCode)">
                <!-- [zh] Mustache 插值表达式 -->
                {{ getLevelBadge(c.userId)!.levelName }}
              <!-- [zh] 闭合标签 `</span>` -->
              </span>
              <!-- [zh] 开始标签 `<el-rate>` -->
              <el-rate v-if="c.star" :model-value="c.star" disabled size="small" />
              <!-- [zh] 开始标签 `<button>` -->
              <button v-if="currentUserId !== c.userId" type="button" class="report-btn" title="举报" @click="reportComment(c)">举报</button>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<p>` -->
            <p v-if="c.propertyInfo" class="sku">已购：{{ c.propertyInfo }}</p>
            <!-- [zh] 开始标签 `<p>` -->
            <p class="content">{{ c.commentContent }}</p>
            <!-- [zh] 开始标签 `<div>` -->
            <div v-if="commentImages(c.commentImages).length" class="img-row">
              <!-- [zh] 开始标签 `<ProductImage>` -->
              <ProductImage
                v-for="(img, idx) in commentImages(c.commentImages)"
                :key="`${c.orderId}-c-${idx}`"
                :source="img"
                width="72"
                height="72"
                :use-thumbnail="true"
                class="comment-img"
                @click="openCommentImagePreview(commentImages(c.commentImages), idx)"
              />
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<p>` -->
            <p v-if="c.commentTime" class="time">{{ c.commentTime }}</p>
            <!-- [zh] 开始标签 `<div>` -->
            <div v-if="c.commentBizReply" class="biz-reply">商家回复：{{ c.commentBizReply }}</div>
            <!-- [zh] 开始标签 `<div>` -->
            <div v-if="c.recommentContent" class="re-comment">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="re-tag">{{ recommentLabel(c.commentTime, c.recommentTime) }}追评</span>
              <!-- [zh] Mustache 插值表达式 -->
              {{ c.recommentContent }}
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div v-if="commentImages(c.recommentImages).length" class="img-row">
              <!-- [zh] 开始标签 `<ProductImage>` -->
              <ProductImage
                v-for="(img, idx) in commentImages(c.recommentImages)"
                :key="`${c.orderId}-r-${idx}`"
                :source="img"
                width="72"
                height="72"
                :use-thumbnail="true"
                class="comment-img"
                @click="openCommentImagePreview(commentImages(c.recommentImages), idx)"
              />
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</article>` -->
        </article>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<el-empty>` -->
      <el-empty v-else-if="!loading && finished" description="暂无评价" />

      <!-- [zh] 开始标签 `<div>` -->
      <div ref="sentinelRef" class="load-sentinel" />
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="loading" class="load-tip">加载中…</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else-if="finished && rawList.length" class="load-tip muted">没有更多了</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<CommentReportDialog>` -->
    <CommentReportDialog ref="reportRef" />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute } from 'vue-router';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/api/modules`
import { commentApi, userMemberApi } from '@/api/modules';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';
// [zh] 导入 `@/components/common/UserAvatar.vue`
import UserAvatar from '@/components/common/UserAvatar.vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/components/business/CommentReportDialog.vue`
import CommentReportDialog from '@/components/business/CommentReportDialog.vue';
// [zh] 导入 `@/composables/usePageListCache`
import { usePageListCache } from '@/composables/usePageListCache';
// [zh] 导入 `@/utils/comment`
import { matchCommentLevel, maskCommenterName, type CommentLevel } from '@/utils/comment';
// [zh] 导入 `@/composables/imagePreview`
import { openImagePreview } from '@/composables/imagePreview';
// [zh] 导入 `@/utils/image`
import { resolveImageUrl, splitImagePaths } from '@/utils/image';

// [zh] 声明变量 `levelCache`
const levelCache = ref<Map<string, { levelCode: number; levelName: string }>>(new Map());

// [zh] 声明变量 `fetchLevelBadge`
const fetchLevelBadge = (userId: string) => {
  // [zh] 条件判断 if
  if (!userId || levelCache.value.has(userId)) return;
  // [zh] 代码块开始
  userMemberApi.getLevelBadge(userId).then((res: any) => {
    // [zh] 条件判断 if
    if (res?.levelCode != null) {
      // [zh] 语句
      levelCache.value.set(userId, { levelCode: res.levelCode, levelName: res.levelName });

      // [zh] 语句
      levelCache.value = new Map(levelCache.value);
    // [zh] 语法符号
    }
  // [zh] 语句
  }).catch(() => {});
// [zh] 语句
};

// [zh] 声明变量 `getLevelBadge`
const getLevelBadge = (userId: string) => levelCache.value.get(userId) ?? null;

// [zh] 声明变量 `levelClass`
const levelClass = (code: number) => {
  // [zh] 条件判断 if
  if (code >= 3) return 'level-gold';
  // [zh] 条件判断 if
  if (code >= 2) return 'level-silver';
  // [zh] return 返回
  return 'level-default';
// [zh] 语句
};

// [zh] 声明变量 `levelTabs`
const levelTabs: { label: string; value: CommentLevel }[] = [
  // [zh] TypeScript/JS 语句：`{ label: '全部', value: '' },`
  { label: '全部', value: '' },
  // [zh] TypeScript/JS 语句：`{ label: '好评', value: 'good' },`
  { label: '好评', value: 'good' },
  // [zh] TypeScript/JS 语句：`{ label: '中评', value: 'medium' },`
  { label: '中评', value: 'medium' },
  // [zh] TypeScript/JS 语句：`{ label: '差评', value: 'bad' }`
  { label: '差评', value: 'bad' }
// [zh] 语句
];

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `auth`
const auth = useAuthStore();
// [zh] 声明变量 `currentUserId`
const currentUserId = computed(() => auth.userInfo?.userId ?? '');
// [zh] 声明变量 `commentLevel`
const commentLevel = ref<CommentLevel>('');
// [zh] 声明变量 `pageNo`
const pageNo = ref(0);
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(1);
// [zh] 声明变量 `apiTotal`
const apiTotal = ref(0);
// [zh] 声明变量 `rawList`
const rawList = ref<any[]>([]);
// [zh] 声明变量 `loading`
const loading = ref(false);
// [zh] 声明变量 `finished`
const finished = ref(false);
// [zh] 声明变量 `scrollRoot`
const scrollRoot = ref<HTMLElement>();
// [zh] 声明变量 `sentinelRef`
const sentinelRef = ref<HTMLElement>();
// [zh] 声明变量 `observer`
let observer: IntersectionObserver | null = null;

// [zh] 声明变量 `productId`
const productId = () => String(route.params.productId);

// [zh] 声明变量 `reportRef`
const reportRef = ref<InstanceType<typeof CommentReportDialog>>();
// [zh] 声明变量 `reportComment`
const reportComment = (c: any) => {
  // [zh] 代码块开始
  reportRef.value?.show({
    // [zh] TypeScript/JS 语句：`orderId: String(c.orderId),`
    orderId: String(c.orderId),
    // [zh] TypeScript/JS 语句：`productId: productId(),`
    productId: productId(),
    // [zh] TypeScript/JS 语句：`commentContent: c.commentContent`
    commentContent: c.commentContent
  // [zh] 语句
  });
// [zh] 语句
};

// [zh] 声明变量 `displayList`
const displayList = computed(() =>
  // [zh] TypeScript/JS 语句：`rawList.value.filter((c) => matchCommentLevel(c.star, c`
  rawList.value.filter((c) => matchCommentLevel(c.star, commentLevel.value))
// [zh] 语句
);

// [zh] 声明变量 `displayTotal`
const displayTotal = computed(() => {
  // [zh] 条件判断 if
  if (!commentLevel.value) return apiTotal.value;
  // [zh] return 返回
  return displayList.value.length;
// [zh] 语句
});

// [zh] 声明变量 `commentImages`
const commentImages = (raw?: string | null) => splitImagePaths(raw);

// [zh] 声明变量 `openCommentImagePreview`
const openCommentImagePreview = (images: string[], index: number) => {

  // [zh] 声明变量 `fullUrls`
  const fullUrls = images.map((img) => resolveImageUrl(img, { useThumbnail: false }) || img);
  // [zh] 语句
  openImagePreview(fullUrls, index);
// [zh] 语句
};

// [zh] 声明变量 `recommentLabel`
const recommentLabel = (commentTime?: string, recommentTime?: string) => {
  // [zh] 条件判断 if
  if (!commentTime || !recommentTime) return '';
  // [zh] 声明变量 `start`
  const start = new Date(commentTime.replace(/-/g, '/')).getTime();
  // [zh] 声明变量 `end`
  const end = new Date(recommentTime.replace(/-/g, '/')).getTime();
  // [zh] 条件判断 if
  if (Number.isNaN(start) || Number.isNaN(end)) return '';
  // [zh] 声明变量 `days`
  const days = Math.floor((end - start) / 86400000);
  // [zh] 条件判断 if
  if (days <= 0) return '当天';
  // [zh] return 返回
  return `${days}天后`;
// [zh] 语句
};

// [zh] 声明变量 `setupObserver`
const setupObserver = () => {
  // [zh] 语句
  observer?.disconnect();
  // [zh] 条件判断 if
  if (!sentinelRef.value) return;
  // [zh] TypeScript/JS 语句：`observer = new IntersectionObserver(`
  observer = new IntersectionObserver(
    // [zh] 代码块开始
    (entries) => {
      // [zh] 条件判断 if
      if (entries.some((e) => e.isIntersecting)) loadMore();
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ root: scrollRoot.value, rootMargin: '80px', threshold`
    { root: scrollRoot.value, rootMargin: '80px', threshold: 0 }
  // [zh] 语句
  );
  // [zh] 语句
  observer.observe(sentinelRef.value);
// [zh] 语句
};

// [zh] 声明变量 `pageCache`
const pageCache = usePageListCache({
  // [zh] TypeScript/JS 语句：`cacheKey: () => `/product/${productId()}/comments|${com`
  cacheKey: () => `/product/${productId()}/comments|${commentLevel.value}`,
  // [zh] TypeScript/JS 语句：`scrollRef: scrollRoot,`
  scrollRef: scrollRoot,
  // [zh] 代码块开始
  getState: () => ({
    // [zh] TypeScript/JS 语句：`commentLevel: commentLevel.value,`
    commentLevel: commentLevel.value,
    // [zh] TypeScript/JS 语句：`rawList: rawList.value,`
    rawList: rawList.value,
    // [zh] TypeScript/JS 语句：`pageNo: pageNo.value,`
    pageNo: pageNo.value,
    // [zh] TypeScript/JS 语句：`pageTotal: pageTotal.value,`
    pageTotal: pageTotal.value,
    // [zh] TypeScript/JS 语句：`apiTotal: apiTotal.value,`
    apiTotal: apiTotal.value,
    // [zh] TypeScript/JS 语句：`finished: finished.value`
    finished: finished.value
  // [zh] TypeScript/JS 语句：`}),`
  }),
  // [zh] 代码块开始
  setState: (state) => {
    // [zh] 语句
    commentLevel.value = (state.commentLevel as CommentLevel) ?? '';
    // [zh] 语句
    rawList.value = (state.rawList as any[]) || [];
    // [zh] 语句
    pageNo.value = Number(state.pageNo) || 0;
    // [zh] 语句
    pageTotal.value = Number(state.pageTotal) || 1;
    // [zh] 语句
    apiTotal.value = Number(state.apiTotal) || 0;
    // [zh] 语句
    finished.value = !!state.finished;
    // [zh] 语句
    loading.value = false;
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`afterRestore: setupObserver`
  afterRestore: setupObserver
// [zh] 语句
});

// [zh] 声明变量 `loadMore`
const loadMore = async () => {
  // [zh] 条件判断 if
  if (loading.value || finished.value) return;
  // [zh] 条件判断 if
  if (pageNo.value >= pageTotal.value && pageNo.value > 0) {
    // [zh] 语句
    finished.value = true;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }

  // [zh] 语句
  loading.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `nextPage`
    const nextPage = pageNo.value + 1;
    // [zh] 声明变量 `r`
    const r = await commentApi.loadComment({ pageNo: nextPage, productId: productId() });
    // [zh] 声明变量 `chunk`
    const chunk = r?.list || [];
    // [zh] 条件判断 if
    if (nextPage === 1) rawList.value = chunk;
    // [zh] else 分支
    else rawList.value = rawList.value.concat(chunk);

    // [zh] 语句
    pageNo.value = r?.pageNo ?? nextPage;
    // [zh] 语句
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    // [zh] 语句
    apiTotal.value = r?.totalCount ?? rawList.value.length;
    // [zh] 语句
    finished.value = pageNo.value >= pageTotal.value;
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    loading.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `resetAndLoad`
const resetAndLoad = async () => {
  // [zh] 语句
  pageNo.value = 0;
  // [zh] 语句
  pageTotal.value = 1;
  // [zh] 语句
  finished.value = false;
  // [zh] 语句
  rawList.value = [];
  // [zh] 语句
  apiTotal.value = 0;
  // [zh] 条件判断 if
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await loadMore();
// [zh] 语句
};

// [zh] 代码块开始
watch(commentLevel, async () => {
  // [zh] 语句
  pageCache.clear();
  // [zh] 条件判断 if
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await resetAndLoad();
  // [zh] 语句
  setupObserver();
// [zh] 语句
});

// [zh] 代码块开始
watch(rawList, () => {
  // [zh] 代码块开始
  rawList.value.forEach((c: any) => {
    // [zh] 条件判断 if
    if (c.userId) fetchLevelBadge(c.userId);
  // [zh] 语句
  });
// [zh] 语句
}, { deep: false });

// [zh] 代码块开始
onMounted(async () => {
  // [zh] 声明变量 `restored`
  const restored = await pageCache.tryRestore();
  // [zh] 条件判断 if
  if (!restored) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await resetAndLoad();
  // [zh] 语法符号
  }
  // [zh] 语句
  setupObserver();
// [zh] 语句
});

// [zh] 代码块开始
usePageRefresh(async () => {
  // [zh] 语句
  pageCache.clear();
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await resetAndLoad();
  // [zh] 语句
  setupObserver();
// [zh] 语句
}, { getScrollEl: () => scrollRoot.value });

// [zh] 语句
onUnmounted(() => observer?.disconnect());
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.count {` */
.count {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `font-weight: 400;` */
  font-weight: 400;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.level-tabs {` */
.level-tabs {
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.comment-scroll {` */
.comment-scroll {
  /* [zh] 样式规则 `max-height: calc(100vh - 200px);` */
  max-height: calc(100vh - 200px);
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
  /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
  -webkit-overflow-scrolling: touch;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.comment-list {` */
.comment-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.comment-item {` */
.comment-item {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding: 14px 0;` */
  padding: 14px 0;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.comment-body {` */
.comment-body {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.comment-head {` */
.comment-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `margin-bottom: 4px;` */
  margin-bottom: 4px;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;

  /* [zh] 样式规则 `.user {` */
  .user {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cmt-level-tag {` */
  .cmt-level-tag {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `padding: 1px 6px;` */
    padding: 1px 6px;
    /* [zh] 样式规则 `border-radius: $radius-pill;` */
    border-radius: $radius-pill;
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `line-height: 1.6;` */
    line-height: 1.6;

    /* [zh] 样式规则 `&.level-default {` */
    &.level-default {
      /* [zh] 样式规则 `background: $color-bg-subtle;` */
      background: $color-bg-subtle;
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
      /* [zh] 样式规则 `border: 1px solid $color-border;` */
      border: 1px solid $color-border;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.level-silver {` */
    &.level-silver {
      /* [zh] 样式规则 `background: linear-gradient(135deg, #d0d` */
      background: linear-gradient(135deg, #d0d0d0, #b0b0b5);
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `border: 1px solid #a8a8ad;` */
      border: 1px solid #a8a8ad;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.level-gold {` */
    &.level-gold {
      /* [zh] 样式规则 `background: $color-accent-gradient-gold;` */
      background: $color-accent-gradient-gold;
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `border: 1px solid $color-gold;` */
      border: 1px solid $color-gold;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.report-btn {` */
  .report-btn {
    /* [zh] 样式规则 `margin-left: auto;` */
    margin-left: auto;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `transition: color $transition-fast;` */
    transition: color $transition-fast;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku {` */
.sku {
  /* [zh] 样式规则 `margin: 0 0 6px;` */
  margin: 0 0 6px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `line-height: 1.4;` */
  line-height: 1.4;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.content {` */
.content {
  /* [zh] 样式规则 `margin: 0 0 8px;` */
  margin: 0 0 8px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.img-row {` */
.img-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;

  /* [zh] 样式规则 `.comment-img {` */
  .comment-img {
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `transition: transform $transition-fast, ` */
    transition: transform $transition-fast, box-shadow $transition-fast;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `transform: scale(1.05);` */
      transform: scale(1.05);
      /* [zh] 样式规则 `box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1` */
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.time {` */
.time {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.biz-reply {` */
.biz-reply {
  /* [zh] 样式规则 `margin-top: 10px;` */
  margin-top: 10px;
  /* [zh] 样式规则 `padding-top: 10px;` */
  padding-top: 10px;
  /* [zh] 样式规则 `border-top: 1px solid $color-border;` */
  border-top: 1px solid $color-border;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.re-comment {` */
.re-comment {
  /* [zh] 样式规则 `margin-top: 10px;` */
  margin-top: 10px;
  /* [zh] 样式规则 `padding-top: 10px;` */
  padding-top: 10px;
  /* [zh] 样式规则 `border-top: 1px solid $color-border;` */
  border-top: 1px solid $color-border;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;

  /* [zh] 样式规则 `.re-tag {` */
  .re-tag {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `margin-right: 4px;` */
    margin-right: 4px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.load-sentinel {` */
.load-sentinel {
  /* [zh] 样式规则 `height: 1px;` */
  height: 1px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.load-tip {` */
.load-tip {
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `padding: 12px 0;` */
  padding: 12px 0;

  /* [zh] 样式规则 `&.muted {` */
  &.muted {
    /* [zh] 样式规则 `opacity: 0.8;` */
    opacity: 0.8;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
