<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div v-if="productInfo" class="pc-product-detail ignore">
    <!-- [zh] 开始标签 `<section>` -->
    <section class="pc-detail-main">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="pc-detail-gallery">
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="galleryImages.length > 1" class="thumb-strip" role="tablist" aria-label="商品图">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-for="(img, i) in galleryImages"
            :key="i"
            type="button"
            class="thumb-btn"
            :class="{ active: activeImageIndex === i }"
            :aria-selected="activeImageIndex === i"
            @mouseenter="selectGalleryIndex(i)"
            @click="selectGalleryIndex(i)"
          >
            <!-- [zh] 开始标签 `<ProductImage>` -->
            <ProductImage :source="img" width="56" height="56" fit="cover" dense />
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="main-image-wrap" @click="openGalleryPreview(activeImageIndex)">
          <!-- [zh] 开始标签 `<ProductImage>` -->
          <ProductImage
            v-if="galleryImages.length"
            :source="galleryImages[activeImageIndex]"
            :use-thumbnail="false"
            width="100%"
            height="100%"
            fit="contain"
            class="main-image"
          />
          <!-- [zh] 开始标签 `<div>` -->
          <div v-else class="main-image-empty">
            <!-- [zh] 开始标签 `<ProductImage>` -->
            <ProductImage width="100%" height="100%" />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<span>` -->
          <span v-if="galleryImages.length > 1" class="image-index">
            <!-- [zh] Mustache 插值表达式 -->
            {{ activeImageIndex + 1 }} / {{ galleryImages.length }}
          <!-- [zh] 闭合标签 `</span>` -->
          </span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="pc-detail-info">
        <!-- [zh] 开始标签 `<h1>` -->
        <h1 class="product-title">{{ productInfo.productName }}</h1>

        <!-- [zh] 开始标签 `<div>` -->
        <div class="price-panel">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="price-row">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="price-label">促销价</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="price-value">
              <!-- [zh] 开始标签 `<em>` -->
              <em>¥</em>{{ displayPrice }}
            </span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="sales-meta">销量 {{ productInfo.totalSale ?? 0 }} 件</p>
          <!-- [zh] 开始标签 `<AgentServiceEntry>` -->
          <AgentServiceEntry
            class="price-agent-btn"
            :compact="false"
            show-label
            :icon-size="16"
            :consult-product="agentConsultProduct"
          />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div class="sku-panel">
          <!-- [zh] 开始标签 `<div>` -->
          <div v-for="prop in productPropertyList" :key="prop.propertyId" class="sku-line">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="sku-name">{{ prop.propertyName }}</span>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="sku-options">
              <!-- [zh] 开始标签 `<button>` -->
              <button
                v-for="val in prop.propertyValues"
                :key="val.propertyValueId"
                type="button"
                class="sku-option"
                :class="{ active: selectedProperty[prop.propertyId] === val.propertyValueId }"
                @click="selectProperty(prop, val)"
              >
                <!-- [zh] 开始标签 `<ProductImage>` -->
                <ProductImage
                  v-if="val.propertyCover"
                  :source="val.propertyCover"
                  :width="32"
                  :height="32"
                  fit="contain"
                  :lazy="false"
                  dense
                  class="sku-option-thumb"
                />
                <!-- [zh] 开始标签 `<span>` -->
                <span class="sku-option-text">{{ val.propertyValue }}</span>
              <!-- [zh] 闭合标签 `</button>` -->
              </button>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>

          <!-- [zh] 开始标签 `<div>` -->
          <div class="qty-line">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="sku-name">数量</span>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="qty-control">
              <!-- [zh] 开始标签 `<el-input-number>` -->
              <el-input-number v-model="quantity" :min="1" :max="maxBuy" size="default" />
              <!-- [zh] 开始标签 `<span>` -->
              <span class="stock-hint">
                <!-- [zh] 模板内容：`库存 {{ selectedSku?.stock ?? '--' }}` -->
                库存 {{ selectedSku?.stock ?? '--' }}
                <!-- [zh] 开始标签 `<em>` -->
                <em v-if="selectedSku?.stock != null && selectedSku.stock <= 5">紧张</em>
              <!-- [zh] 闭合标签 `</span>` -->
              </span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div class="action-panel">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="action-side">
            <!-- [zh] 开始标签 `<AgentServiceEntry>` -->
            <AgentServiceEntry
              :compact="false"
              show-label
              :consult-product="agentConsultProduct"
            />
            <!-- [zh] 开始标签 `<button>` -->
            <button
              type="button"
              class="btn-fav"
              :class="{ active: favorited }"
              :disabled="favoriteLoading"
              @click="toggleFavorite"
            >
              <!-- [zh] 开始标签 `<el-icon>` -->
              <el-icon :size="18">
                <!-- [zh] 开始标签 `<StarFilled>` -->
                <StarFilled v-if="favorited" />
                <!-- [zh] 开始标签 `<Star>` -->
                <Star v-else />
              <!-- [zh] 闭合标签 `</el-icon>` -->
              </el-icon>
              <!-- [zh] 开始标签 `<span>` -->
              <span>{{ favorited ? '已收藏' : '收藏' }}</span>
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="action-main">
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button class="btn-cart" type="primary" size="large" @click="openAddCartSheet">
              <!-- [zh] 模板内容：`加入购物车` -->
              加入购物车
            <!-- [zh] 闭合标签 `</el-button>` -->
            </el-button>
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button class="btn-buy" type="danger" size="large" @click="buyNow">立即购买</el-button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="pc-detail-tabs card">
      <!-- [zh] 开始标签 `<el-tabs>` -->
      <el-tabs v-model="detailTab" class="detail-tabs">
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane label="图文详情" name="desc">
          <!-- [zh] 开始标签 `<MarkdownContent>` -->
          <MarkdownContent :content="productInfo.productDesc" class="desc-content" center-images />
        <!-- [zh] 闭合标签 `</el-tab-pane>` -->
        </el-tab-pane>
        <!-- [zh] 开始标签 `<el-tab-pane>` -->
        <el-tab-pane :label="`商品评价${commentTotal > 0 ? ` (${commentTotal})` : ''}`" name="comments">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="tab-comments">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="tab-comments-head">
              <!-- [zh] 开始标签 `<p>` -->
              <p class="comments-summary">
                <!-- [zh] 模板内容：`累计评价 {{ commentTotal }} 条` -->
                累计评价 {{ commentTotal }} 条
                <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
                <template v-if="commentTotal > 0">
                  <!-- [zh] 模板内容：`· 好评率 {{ commentGoodRate }}%` -->
                  · 好评率 {{ commentGoodRate }}%
                  <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
                  <template v-if="commentImageCount > 0"> · {{ commentImageCount }} 条带图</template>
                </template>
              </p>
              <button v-if="commentTotal > 0" type="button" class="link-more" @click="goAllComments">
                查看全部评价
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>
            <div v-if="comments.length" class="comment-list">
              <article v-for="c in comments" :key="c.orderId" class="comment-item">
                <div class="comment-head">
                  <div class="user-info">
                    <el-avatar :size="36" :src="resolveAvatarUrl(c.avatar)" class="user-avatar">
                      {{ (c.nickName || '用')[0] }}
                    </el-avatar>
                    <div class="user-detail">
                      <span class="user">{{ maskCommenterName(c.nickName) }}</span>
                      <span v-if="getCommentLevelProxy(c.userId)" class="comment-level-tag" :class="commentLevelTagClass(getCommentLevelProxy(c.userId)!.levelCode)">
                        {{ getCommentLevelProxy(c.userId)!.levelName }}
                      </span>
                    </div>
                  </div>
                  <el-rate v-if="c.star" :model-value="c.star" disabled size="small" />
                </div>
                <p v-if="c.propertyInfo" class="sku-info">{{ c.propertyInfo }}</p>
                <p class="comment-text">{{ c.commentContent }}</p>
                <button
                  type="button"
                  class="report-btn"
                  @click="openReport({ orderId: c.orderId, commentContent: c.commentContent })"
                >
                  举报
                </button>
              </article>
            </div>
            <el-empty v-else description="暂无评价，快来抢沙发吧" :image-size="80" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <section class="pc-recommend-block card">
      <h3 class="block-title">看了又看</h3>
      <div v-if="similarProducts.length" class="similar-grid">
        <button
          v-for="item in similarProducts"
          :key="item.productId"
          type="button"
          class="similar-item"
          @click="goDetail(item)"
        >
          <ProductImage :product="item" fit="cover" width="100%" height="100%" class="similar-img" />
          <div class="similar-info">
            <p class="similar-name">{{ item.productName }}</p>
            <p class="similar-price">¥{{ formatPrice(item.price ?? item.salePrice ?? item.minPrice) }}</p>
          </div>
        </button>
      </div>
      <p v-if="loadingMore" class="load-tip">加载中…</p>
      <p v-else-if="finished && similarProducts.length" class="load-tip">已展示全部推荐商品</p>
    </section>
  </div>

  <div v-else-if="loading" class="pc-detail-loading card">
    <el-skeleton animated :rows="12" />
  </div>
  <div v-else class="pc-detail-error card">
    <el-empty :description="loadError ? '商品加载失败' : '商品不存在或已下架'">
      <el-button type="primary" round @click="load">重试</el-button>
      <el-button round @click="router.push('/')">返回首页</el-button>
    </el-empty>
  </div>
  <CommentReportDialog ref="reportDialogRef" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowRight, Star, StarFilled } from '@element-plus/icons-vue';
import AgentServiceEntry from '@/components/agent/AgentServiceEntry.vue';
import ProductImage from '@/components/common/ProductImage.vue';
import MarkdownContent from '@/components/common/MarkdownContent.vue';
import CommentReportDialog from '@/components/business/CommentReportDialog.vue';
import { useProductDetailPage } from '@/composables/useProductDetailPage';
import { maskCommenterName } from '@/utils/comment';
import { productApi, userMemberApi } from '@/api/modules';
import { resolveAvatarUrl } from '@/utils/image';

const route = useRoute();
const router = useRouter();

const {
  loading,
  loadError,
  load,
  productInfo,
  productPropertyList,
  comments,
  commentTotal,
  commentGoodRate,
  commentImageCount,
  quantity,
  selectedSku,
  selectedProperty,
  activeImageIndex,
  favorited,
  favoriteLoading,
  detailTab,
  galleryImages,
  displayPrice,
  agentConsultProduct,
  maxBuy,
  openGalleryPreview,
  selectGalleryIndex,
  selectProperty,
  toggleFavorite,
  goAllComments,
  openAddCartSheet,
  buyNow
} = useProductDetailPage();

const reportDialogRef = ref<InstanceType<typeof CommentReportDialog>>();

const similarProducts = ref<any[]>([]);
const loadingMore = ref(false);
const finished = ref(false);
const allSimilarProducts = ref<any[]>([]);
const displayCount = ref(8);
const PAGE_SIZE = 8;
const MAX_SIMILAR = 16;
let scrollTicking = false;

const onScroll = () => {
  if (scrollTicking || finished.value || loadingMore.value) return;
  scrollTicking = true;
  requestAnimationFrame(() => {
    scrollTicking = false;
    const st = window.scrollY || document.documentElement.scrollTop;
    const sh = document.documentElement.scrollHeight;
    const ch = window.innerHeight;
    if (sh - st - ch < 200) loadMore();
  });
};

const loadMore = () => {
  if (finished.value || loadingMore.value) return;
  if (displayCount.value >= allSimilarProducts.value.length) {
    finished.value = true;
    return;
  }
  loadingMore.value = true;
  setTimeout(() => {
    displayCount.value = Math.min(displayCount.value + PAGE_SIZE, MAX_SIMILAR, allSimilarProducts.value.length);
    similarProducts.value = allSimilarProducts.value.slice(0, displayCount.value);
    if (displayCount.value >= allSimilarProducts.value.length) finished.value = true;
    loadingMore.value = false;
  }, 300);
};

const loadSimilarProducts = async () => {
  loadingMore.value = true;
  try {
    const r = await productApi.loadCommendProduct();
    const list = Array.isArray(r) ? r : r?.list || [];
    if (!list.length) { finished.value = true; return; }

    let filled = [...list];
    while (filled.length < MAX_SIMILAR) {
      filled = filled.concat(list);
    }
    allSimilarProducts.value = filled.slice(0, MAX_SIMILAR);
    similarProducts.value = allSimilarProducts.value.slice(0, displayCount.value);
    if (allSimilarProducts.value.length <= displayCount.value) finished.value = true;
  } catch (error) {
    console.error('PcProductDetailView: loadSimilarProducts error', error);
    finished.value = true;
  } finally {
    loadingMore.value = false;
  }
};

const formatPrice = (price: any): string => {
  const n = Number(price);
  if (isNaN(n)) return '--';
  return n.toFixed(2);
};

const goDetail = (p: any) => router.push(`/product/${p.productId}`);

const levelCache = ref<Record<string, { levelCode: number; levelName: string }>>({});

const fetchCommentLevel = (userId: string) => {
  if (!userId || levelCache.value[userId]) return;
  userMemberApi.getLevelBadge(userId).then((res: any) => {
    if (res?.levelCode != null) {
      levelCache.value = { ...levelCache.value, [userId]: res };
    }
  }).catch(() => {});
};

const getCommentLevelProxy = (userId: string | number | undefined): { levelCode: number; levelName: string } | null => {
  if (!userId) return null;
  const key = String(userId);

  fetchCommentLevel(key);
  return levelCache.value[key] ?? null;
};

const commentLevelTagClass = (code: number): string => {
  if (code >= 3) return 'level-gold';
  if (code >= 2) return 'level-silver';
  return 'level-normal';
};

watch(comments, (val) => {
  if (!val?.length) return;
  val.forEach((c: any) => {
    if (c.userId) fetchCommentLevel(String(c.userId));
  });
}, { immediate: true });

onMounted(() => {
  loadSimilarProducts();
  window.addEventListener('scroll', onScroll, { passive: true });
});

onUnmounted(() => window.removeEventListener('scroll', onScroll));

const openReport = (payload: { orderId: string; commentContent?: string }) => {
  reportDialogRef.value?.show({
    orderId: payload.orderId,
    productId: String(route.params.productId || ''),
    commentContent: payload.commentContent
  });
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-product-detail {` */
.pc-product-detail {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-detail-main {` */
.pc-detail-main {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: nowrap;` */
  flex-wrap: nowrap;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 24px;` */
  gap: 24px;
  /* [zh] 样式规则 `padding: 20px;` */
  padding: 20px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
  border: 1px solid $color-border-gray;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-detail-gallery {` */
.pc-detail-gallery {
  /* [zh] 样式规则 `flex: 0 0 $pc-detail-gallery-width;` */
  flex: 0 0 $pc-detail-gallery-width;
  /* [zh] 样式规则 `width: $pc-detail-gallery-width;` */
  width: $pc-detail-gallery-width;
  /* [zh] 样式规则 `max-width: $pc-detail-gallery-width;` */
  max-width: $pc-detail-gallery-width;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.thumb-strip {` */
.thumb-strip {
  /* [zh] 样式规则 `flex: 0 0 64px;` */
  flex: 0 0 64px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `max-height: 460px;` */
  max-height: 460px;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;

  /* [zh] 样式规则 `.thumb-btn {` */
  .thumb-btn {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `width: 56px;` */
    width: 56px;
    /* [zh] 样式规则 `height: 56px;` */
    height: 56px;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `border: 2px solid transparent;` */
    border: 2px solid transparent;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `background: #fff;` */
    background: #fff;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;

    /* [zh] 样式声明 */
    &.active,
    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `border-color: $color-primary;` */
      border-color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.main-image-wrap {` */
.main-image-wrap {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `aspect-ratio: 1;` */
  aspect-ratio: 1;
  /* [zh] 样式规则 `max-height: 460px;` */
  max-height: 460px;
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
  /* [zh] 样式规则 `border-radius: 4px;` */
  border-radius: 4px;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `cursor: zoom-in;` */
  cursor: zoom-in;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式声明 */
  .main-image,
  /* [zh] 样式规则 `.main-image-empty {` */
  .main-image-empty {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.image-index {` */
  .image-index {
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `right: 10px;` */
    right: 10px;
    /* [zh] 样式规则 `bottom: 10px;` */
    bottom: 10px;
    /* [zh] 样式规则 `padding: 2px 8px;` */
    padding: 2px 8px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.45);` */
    background: rgba(0, 0, 0, 0.45);
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-detail-info {` */
.pc-detail-info {
  /* [zh] 样式规则 `flex: 1 1 $pc-detail-info-min-width;` */
  flex: 1 1 $pc-detail-info-min-width;
  /* [zh] 样式规则 `min-width: $pc-detail-info-min-width;` */
  min-width: $pc-detail-info-min-width;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-title {` */
.product-title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 20px;` */
  font-size: 20px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `line-height: 1.45;` */
  line-height: 1.45;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `word-break: normal;` */
  word-break: normal;
  /* [zh] 样式规则 `overflow-wrap: break-word;` */
  overflow-wrap: break-word;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.price-panel {` */
.price-panel {
  /* [zh] 样式规则 `padding: 14px 16px;` */
  padding: 14px 16px;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `border-radius: 4px;` */
  border-radius: 4px;

  /* [zh] 样式规则 `.price-row {` */
  .price-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: baseline;` */
    align-items: baseline;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price-label {` */
  .price-label {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price-value {` */
  .price-value {
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
    /* [zh] 样式规则 `font-size: 32px;` */
    font-size: 32px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;

    /* [zh] 样式规则 `em {` */
    em {
      /* [zh] 样式规则 `font-size: 18px;` */
      font-size: 18px;
      /* [zh] 样式规则 `font-style: normal;` */
      font-style: normal;
      /* [zh] 样式规则 `margin-right: 2px;` */
      margin-right: 2px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sales-meta {` */
  .sales-meta {
    /* [zh] 样式规则 `margin: 8px 0 0;` */
    margin: 8px 0 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.price-agent-btn {` */
.price-agent-btn {
  /* [zh] 样式规则 `margin-top: 10px;` */
  margin-top: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-panel {` */
.sku-panel {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 14px;` */
  gap: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.sku-line,
/* [zh] 样式规则 `.qty-line {` */
.qty-line {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-name {` */
.sku-name {
  /* [zh] 样式规则 `flex: 0 0 56px;` */
  flex: 0 0 56px;
  /* [zh] 样式规则 `padding-top: 8px;` */
  padding-top: 8px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-options {` */
.sku-options {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-option {` */
.sku-option {
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `max-width: 260px;` */
  max-width: 260px;
  /* [zh] 样式规则 `padding: 6px 12px;` */
  padding: 6px 12px;
  /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
  border: 1px solid $color-border-gray;
  /* [zh] 样式规则 `border-radius: 4px;` */
  border-radius: 4px;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: border-color $transition-fas` */
  transition: border-color $transition-fast, color $transition-fast;

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `border-color: $color-primary;` */
    border-color: $color-primary;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `background: $color-gold-soft;` */
    background: $color-gold-soft;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:hover:not(.active) {` */
  &:hover:not(.active) {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.45)` */
    border-color: rgba($color-primary, 0.45);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sku-option-text {` */
  .sku-option-text {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `line-height: 1.3;` */
    line-height: 1.3;
    /* [zh] 样式规则 `text-align: left;` */
    text-align: left;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.qty-control {` */
.qty-control {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;

  /* [zh] 样式规则 `.stock-hint {` */
  .stock-hint {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;

    /* [zh] 样式规则 `em {` */
    em {
      /* [zh] 样式规则 `margin-left: 6px;` */
      margin-left: 6px;
      /* [zh] 样式规则 `font-style: normal;` */
      font-style: normal;
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.action-panel {` */
.action-panel {
  /* [zh] 样式规则 `margin-top: auto;` */
  margin-top: auto;
  /* [zh] 样式规则 `padding-top: 8px;` */
  padding-top: 8px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 14px;` */
  gap: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.action-side {` */
.action-side {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;

  /* [zh] 样式规则 `.btn-fav {` */
  .btn-fav {
    /* [zh] 样式规则 `display: inline-flex;` */
    display: inline-flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;
    /* [zh] 样式规则 `padding: 6px 10px;` */
    padding: 6px 10px;
    /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
    border: 1px solid $color-border-gray;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `background: #fff;` */
    background: #fff;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `transition: border-color $transition-fas` */
    transition: border-color $transition-fast, color $transition-fast, background $transition-fast;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `border-color: rgba($color-primary, 0.45)` */
      border-color: rgba($color-primary, 0.45);
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.active {` */
    &.active {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `border-color: rgba($color-primary, 0.35)` */
      border-color: rgba($color-primary, 0.35);
      /* [zh] 样式规则 `background: $color-primary-soft;` */
      background: $color-primary-soft;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.action-main {` */
.action-main {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: nowrap;` */
  flex-wrap: nowrap;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;

  /* [zh] 样式规则 `.btn-cart {` */
  .btn-cart {
    /* [zh] 样式规则 `flex: 0 0 200px;` */
    flex: 0 0 200px;
    /* [zh] 样式规则 `width: 200px;` */
    width: 200px;
    /* [zh] 样式规则 `max-width: 200px;` */
    max-width: 200px;
    /* [zh] 样式规则 `height: 48px;` */
    height: 48px;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `--el-button-bg-color: #fff;` */
    --el-button-bg-color: #fff;
    /* [zh] 样式规则 `--el-button-text-color: #333;` */
    --el-button-text-color: #333;
    /* [zh] 样式规则 `--el-button-border-color: #d9d9d9;` */
    --el-button-border-color: #d9d9d9;
    /* [zh] 样式规则 `--el-button-hover-bg-color: #fafafa;` */
    --el-button-hover-bg-color: #fafafa;
    /* [zh] 样式规则 `--el-button-hover-text-color: #333;` */
    --el-button-hover-text-color: #333;
    /* [zh] 样式规则 `--el-button-hover-border-color: #c0c0c0;` */
    --el-button-hover-border-color: #c0c0c0;
    /* [zh] 样式规则 `--el-button-active-bg-color: #f0f0f0;` */
    --el-button-active-bg-color: #f0f0f0;
    /* [zh] 样式规则 `--el-button-active-border-color: #b3b3b3` */
    --el-button-active-border-color: #b3b3b3;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.btn-buy {` */
  .btn-buy {
    /* [zh] 样式规则 `flex: 0 0 200px;` */
    flex: 0 0 200px;
    /* [zh] 样式规则 `width: 200px;` */
    width: 200px;
    /* [zh] 样式规则 `max-width: 200px;` */
    max-width: 200px;
    /* [zh] 样式规则 `height: 48px;` */
    height: 48px;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `--el-button-bg-color: #1D1D1F;` */
    --el-button-bg-color: #1D1D1F;
    /* [zh] 样式规则 `--el-button-text-color: #fff;` */
    --el-button-text-color: #fff;
    /* [zh] 样式规则 `--el-button-border-color: #1D1D1F;` */
    --el-button-border-color: #1D1D1F;
    /* [zh] 样式规则 `--el-button-hover-bg-color: #333;` */
    --el-button-hover-bg-color: #333;
    /* [zh] 样式规则 `--el-button-hover-text-color: #fff;` */
    --el-button-hover-text-color: #fff;
    /* [zh] 样式规则 `--el-button-hover-border-color: #333;` */
    --el-button-hover-border-color: #333;
    /* [zh] 样式规则 `--el-button-active-bg-color: #000;` */
    --el-button-active-bg-color: #000;
    /* [zh] 样式规则 `--el-button-active-border-color: #000;` */
    --el-button-active-border-color: #000;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-detail-tabs.card {` */
.pc-detail-tabs.card {
  /* [zh] 样式规则 `padding: 0 16px 16px;` */
  padding: 0 16px 16px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
  border: 1px solid $color-border-gray;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;

  /* [zh] 样式规则 `:deep(.el-tabs__header) {` */
  :deep(.el-tabs__header) {
    /* [zh] 样式规则 `margin-bottom: 0;` */
    margin-bottom: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.el-tabs__item) {` */
  :deep(.el-tabs__item) {
    /* [zh] 样式规则 `height: 44px;` */
    height: 44px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tab-comments {` */
.tab-comments {
  /* [zh] 样式规则 `padding: 16px 4px 8px;` */
  padding: 16px 4px 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tab-comments-head {` */
.tab-comments-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;

  /* [zh] 样式规则 `.comments-summary {` */
  .comments-summary {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.link-more {` */
  .link-more {
    /* [zh] 样式规则 `display: inline-flex;` */
    display: inline-flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 2px;` */
    gap: 2px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: none;` */
    background: none;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.comment-item {` */
.comment-item {
  /* [zh] 样式规则 `padding: 14px 0;` */
  padding: 14px 0;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border-l` */
  border-bottom: 1px solid $color-border-light;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.comment-head {` */
  .comment-head {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-info {` */
  .user-info {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 10px;` */
    gap: 10px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-avatar {` */
  .user-avatar {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user-detail {` */
  .user-detail {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `gap: 2px;` */
    gap: 2px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user {` */
  .user {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.comment-level-tag {` */
  .comment-level-tag {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `padding: 1px 6px;` */
    padding: 1px 6px;
    /* [zh] 样式规则 `border-radius: 2px;` */
    border-radius: 2px;

    /* [zh] 样式规则 `&.level-gold {` */
    &.level-gold {
      /* [zh] 样式规则 `color: #B8860B;` */
      color: #B8860B;
      /* [zh] 样式规则 `background: rgba(184, 134, 11, 0.1);` */
      background: rgba(184, 134, 11, 0.1);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.level-silver {` */
    &.level-silver {
      /* [zh] 样式规则 `color: #757575;` */
      color: #757575;
      /* [zh] 样式规则 `background: rgba(117, 117, 117, 0.1);` */
      background: rgba(117, 117, 117, 0.1);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.level-normal {` */
    &.level-normal {
      /* [zh] 样式规则 `color: #9E9E9E;` */
      color: #9E9E9E;
      /* [zh] 样式规则 `background: rgba(158, 158, 158, 0.1);` */
      background: rgba(158, 158, 158, 0.1);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sku-info {` */
  .sku-info {
    /* [zh] 样式规则 `margin: 4px 0 0;` */
    margin: 4px 0 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.comment-text {` */
  .comment-text {
    /* [zh] 样式规则 `margin: 8px 0 0;` */
    margin: 8px 0 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `line-height: 1.6;` */
    line-height: 1.6;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.report-btn {` */
.report-btn {
  /* [zh] 样式规则 `margin-top: 6px;` */
  margin-top: 6px;
  /* [zh] 样式规则 `padding: 2px 8px;` */
  padding: 2px 8px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: color $transition-fast;` */
  transition: color $transition-fast;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `color: $color-error;` */
    color: $color-error;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.desc-content {` */
.desc-content {
  /* [zh] 样式规则 `padding: 16px 4px;` */
  padding: 16px 4px;
  /* [zh] 样式规则 `min-height: 120px;` */
  min-height: 120px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.pc-detail-loading,
/* [zh] 样式规则 `.pc-detail-error {` */
.pc-detail-error {
  /* [zh] 样式规则 `padding: 24px;` */
  padding: 24px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-recommend-block {` */
.pc-recommend-block {
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
  border: 1px solid $color-border-gray;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;

  /* [zh] 样式规则 `.block-title {` */
  .block-title {
    /* [zh] 样式规则 `margin: 0 0 12px;` */
    margin: 0 0 12px;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.similar-grid {` */
  .similar-grid {
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `grid-template-columns: repeat(4, minmax(` */
    grid-template-columns: repeat(4, minmax(0, 1fr));
    /* [zh] 样式规则 `gap: 16px;` */
    gap: 16px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.similar-item {` */
  .similar-item {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `text-align: left;` */
    text-align: left;
    /* [zh] 样式规则 `max-width: 220px;` */
    max-width: 220px;

    /* [zh] 样式规则 `&:hover .similar-img {` */
    &:hover .similar-img {
      /* [zh] 样式规则 `opacity: 0.9;` */
      opacity: 0.9;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.similar-img {` */
  .similar-img {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `max-width: 200px;` */
    max-width: 200px;
    /* [zh] 样式规则 `aspect-ratio: 1;` */
    aspect-ratio: 1;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
    /* [zh] 样式规则 `transition: opacity $transition-fast;` */
    transition: opacity $transition-fast;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.similar-info {` */
  .similar-info {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.similar-name {` */
  .similar-name {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `line-height: 1.35;` */
    line-height: 1.35;
    /* [zh] 样式规则 `display: -webkit-box;` */
    display: -webkit-box;
    /* [zh] 样式规则 `-webkit-line-clamp: 2;` */
    -webkit-line-clamp: 2;
    /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
    -webkit-box-orient: vertical;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.similar-price {` */
  .similar-price {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.load-tip {` */
  .load-tip {
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `padding: 12px 0;` */
    padding: 12px 0;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
