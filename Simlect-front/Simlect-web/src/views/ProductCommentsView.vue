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

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { commentApi, userMemberApi } from '@/api/modules';
import { usePageRefresh } from '@/composables/pullRefresh';
import UserAvatar from '@/components/common/UserAvatar.vue';
import ProductImage from '@/components/common/ProductImage.vue';
import CommentReportDialog from '@/components/business/CommentReportDialog.vue';
import { usePageListCache } from '@/composables/usePageListCache';
import { matchCommentLevel, maskCommenterName, type CommentLevel } from '@/utils/comment';
import { openImagePreview } from '@/composables/imagePreview';
import { resolveImageUrl, splitImagePaths } from '@/utils/image';

const levelCache = ref<Map<string, { levelCode: number; levelName: string }>>(new Map());

const fetchLevelBadge = (userId: string) => {
  if (!userId || levelCache.value.has(userId)) return;
  userMemberApi.getLevelBadge(userId).then((res: any) => {
    if (res?.levelCode != null) {
      levelCache.value.set(userId, { levelCode: res.levelCode, levelName: res.levelName });

      levelCache.value = new Map(levelCache.value);
    }
  }).catch(() => {});
};

const getLevelBadge = (userId: string) => levelCache.value.get(userId) ?? null;

const levelClass = (code: number) => {
  if (code >= 3) return 'level-gold';
  if (code >= 2) return 'level-silver';
  return 'level-default';
};

const levelTabs: { label: string; value: CommentLevel }[] = [
  { label: '全部', value: '' },
  { label: '好评', value: 'good' },
  { label: '中评', value: 'medium' },
  { label: '差评', value: 'bad' }
];

const route = useRoute();
const auth = useAuthStore();
const currentUserId = computed(() => auth.userInfo?.userId ?? '');
const commentLevel = ref<CommentLevel>('');
const pageNo = ref(0);
const pageTotal = ref(1);
const apiTotal = ref(0);
const rawList = ref<any[]>([]);
const loading = ref(false);
const finished = ref(false);
const scrollRoot = ref<HTMLElement>();
const sentinelRef = ref<HTMLElement>();
let observer: IntersectionObserver | null = null;

const productId = () => String(route.params.productId);

const reportRef = ref<InstanceType<typeof CommentReportDialog>>();
const reportComment = (c: any) => {
  reportRef.value?.show({
    orderId: String(c.orderId),
    productId: productId(),
    commentContent: c.commentContent
  });
};

const displayList = computed(() =>
  rawList.value.filter((c) => matchCommentLevel(c.star, commentLevel.value))
);

const displayTotal = computed(() => {
  if (!commentLevel.value) return apiTotal.value;
  return displayList.value.length;
});

const commentImages = (raw?: string | null) => splitImagePaths(raw);

const openCommentImagePreview = (images: string[], index: number) => {

  const fullUrls = images.map((img) => resolveImageUrl(img, { useThumbnail: false }) || img);
  openImagePreview(fullUrls, index);
};

const recommentLabel = (commentTime?: string, recommentTime?: string) => {
  if (!commentTime || !recommentTime) return '';
  const start = new Date(commentTime.replace(/-/g, '/')).getTime();
  const end = new Date(recommentTime.replace(/-/g, '/')).getTime();
  if (Number.isNaN(start) || Number.isNaN(end)) return '';
  const days = Math.floor((end - start) / 86400000);
  if (days <= 0) return '当天';
  return `${days}天后`;
};

const setupObserver = () => {
  observer?.disconnect();
  if (!sentinelRef.value) return;
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) loadMore();
    },
    { root: scrollRoot.value, rootMargin: '80px', threshold: 0 }
  );
  observer.observe(sentinelRef.value);
};

const pageCache = usePageListCache({
  cacheKey: () => `/product/${productId()}/comments|${commentLevel.value}`,
  scrollRef: scrollRoot,
  getState: () => ({
    commentLevel: commentLevel.value,
    rawList: rawList.value,
    pageNo: pageNo.value,
    pageTotal: pageTotal.value,
    apiTotal: apiTotal.value,
    finished: finished.value
  }),
  setState: (state) => {
    commentLevel.value = (state.commentLevel as CommentLevel) ?? '';
    rawList.value = (state.rawList as any[]) || [];
    pageNo.value = Number(state.pageNo) || 0;
    pageTotal.value = Number(state.pageTotal) || 1;
    apiTotal.value = Number(state.apiTotal) || 0;
    finished.value = !!state.finished;
    loading.value = false;
  },
  afterRestore: setupObserver
});

const loadMore = async () => {
  if (loading.value || finished.value) return;
  if (pageNo.value >= pageTotal.value && pageNo.value > 0) {
    finished.value = true;
    return;
  }

  loading.value = true;
  try {
    const nextPage = pageNo.value + 1;
    const r = await commentApi.loadComment({ pageNo: nextPage, productId: productId() });
    const chunk = r?.list || [];
    if (nextPage === 1) rawList.value = chunk;
    else rawList.value = rawList.value.concat(chunk);

    pageNo.value = r?.pageNo ?? nextPage;
    pageTotal.value = r?.pageTotal ?? pageNo.value;
    apiTotal.value = r?.totalCount ?? rawList.value.length;
    finished.value = pageNo.value >= pageTotal.value;
  } finally {
    loading.value = false;
  }
};

const resetAndLoad = async () => {
  pageNo.value = 0;
  pageTotal.value = 1;
  finished.value = false;
  rawList.value = [];
  apiTotal.value = 0;
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
  await loadMore();
};

watch(commentLevel, async () => {
  pageCache.clear();
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
  await resetAndLoad();
  setupObserver();
});

watch(rawList, () => {
  rawList.value.forEach((c: any) => {
    if (c.userId) fetchLevelBadge(c.userId);
  });
}, { deep: false });

onMounted(async () => {
  const restored = await pageCache.tryRestore();
  if (!restored) {
    await resetAndLoad();
  }
  setupObserver();
});

usePageRefresh(async () => {
  pageCache.clear();
  await resetAndLoad();
  setupObserver();
}, { getScrollEl: () => scrollRoot.value });

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
