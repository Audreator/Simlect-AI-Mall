<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div v-if="productInfo" class="detail-simlect">

    <!-- [zh] 开始标签 `<section>` -->
    <section class="gallery-wrap">
      <!-- [zh] 开始标签 `<div>` -->
      <div
        v-if="galleryImages.length"
        class="gallery-swipe"
        @touchstart="onTouchStart"
        @touchmove="onTouchMove"
        @touchend="onTouchEnd"
        @mousedown="onMouseDown"
        @mousemove="onMouseMove"
        @mouseup="onMouseUp"
        @mouseleave="onMouseUp"
      >
        <!-- [zh] 开始标签 `<div>` -->
        <div class="gallery-track" :style="{ transform: `translateX(-${activeImageIndex * 100}%)` }" @dragstart.prevent>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-for="(img, i) in galleryImages" :key="i" class="gallery-slide">
            <!-- [zh] 开始标签 `<ProductImage>` -->
            <ProductImage :source="img" :use-thumbnail="false" width="100%" height="100%" class="gallery-img" />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-else class="gallery-empty">
        <!-- [zh] 开始标签 `<ProductImage>` -->
        <ProductImage width="100%" height="100%" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="galleryImages.length > 1" class="gallery-indicator">
        <!-- [zh] Mustache 插值表达式 -->
        {{ activeImageIndex + 1 }} / {{ galleryImages.length }}
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="block info-block">
      <!-- [zh] 开始标签 `<h1>` -->
      <h1 class="product-name">{{ productInfo.productName }}</h1>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="price-line">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="currency">¥</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="amount">{{ displayPrice }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="sales-line">销量 {{ productInfo.totalSale ?? 0 }}</p>
      <!-- [zh] 开始标签 `<AgentServiceEntry>` -->
      <AgentServiceEntry
        class="info-agent-btn"
        :compact="false"
        show-label
        :icon-size="18"
        :consult-product="agentConsultProduct"
      />
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="block quality-badges-row">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="quality-badge gold">
        <!-- [zh] 开始标签 `<BrandMark>` -->
        <BrandMark class="badge-icon" />简选自营
      <!-- [zh] 闭合标签 `</span>` -->
      </span>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="quality-badge">
        <!-- [zh] 开始标签 `<i>` -->
        <i class="badge-dot"></i>正品保证
      <!-- [zh] 闭合标签 `</span>` -->
      </span>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="quality-badge">
        <!-- [zh] 开始标签 `<i>` -->
        <i class="badge-dot"></i>品质溯源
      <!-- [zh] 闭合标签 `</span>` -->
      </span>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="quality-badge">
        <!-- [zh] 开始标签 `<i>` -->
        <i class="badge-dot"></i>售后无忧
      <!-- [zh] 闭合标签 `</span>` -->
      </span>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="block sku-block-wrap">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="prop in productPropertyList" :key="prop.propertyId" class="sku-row">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sku-label">{{ prop.propertyName }}</div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="sku-values">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-for="val in prop.propertyValues"
            :key="val.propertyValueId"
            type="button"
            class="sku-tag"
            :class="{ active: selectedProperty[prop.propertyId] === val.propertyValueId }"
            @click="selectProperty(prop, val)"
          >
            <!-- [zh] 开始标签 `<ProductImage>` -->
            <ProductImage
              v-if="val.propertyCover"
              :source="val.propertyCover"
              :width="18"
              :height="18"
              fit="contain"
              :lazy="false"
              dense
              class="sku-thumb ignore"
            />
            <!-- [zh] 开始标签 `<span>` -->
            <span>{{ val.propertyValue }}</span>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="qty-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="sku-label">数量</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="stock-tip">
          <!-- [zh] 模板内容：`库存 {{ selectedSku?.stock ?? '--' }}` -->
          库存 {{ selectedSku?.stock ?? '--' }}
          <!-- [zh] 开始标签 `<em>` -->
          <em v-if="selectedSku?.stock != null && selectedSku.stock <= 5">紧张</em>
        <!-- [zh] 闭合标签 `</span>` -->
        </span>
        <!-- [zh] 开始标签 `<el-input-number>` -->
        <el-input-number v-model="quantity" :min="1" :max="maxBuy" size="small" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="block delivery-block">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="info-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="info-label">配送</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="info-value">预计3-5个工作日送达 · 包邮</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="block service-block">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="info-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="info-label">服务</span>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="service-badges">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="badge"><i class="badge-icon">✓</i>7天无理由退货</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="badge"><i class="badge-icon">✓</i>正品保障</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="badge"><i class="badge-icon">✓</i>极速退款</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="block store-block">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="store-row">
        <!-- [zh] 开始标签 `<BrandMark>` -->
        <BrandMark class="store-logo" />
        <!-- [zh] 开始标签 `<div>` -->
        <div class="store-info">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="store-name">
            <!-- [zh] Mustache 插值表达式 -->
            {{ productInfo.storeName || '简选自营' }}
            <!-- [zh] 开始标签 `<span>` -->
            <span v-if="!productInfo.storeName" class="store-badge">自营</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="block comment-block">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="block-head">
        <!-- [zh] 开始标签 `<div>` -->
        <div>
          <!-- [zh] 开始标签 `<h3>` -->
          <h3>商品评价</h3>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="commentTotal > 0" class="comment-stats">
            <!-- [zh] 模板内容：`好评率 {{ commentGoodRate }}%` -->
            好评率 {{ commentGoodRate }}%
            <!-- [zh] 开始标签 `<span>` -->
            <span v-if="commentImageCount > 0"> · {{ commentImageCount }} 条带图</span>
          <!-- [zh] 闭合标签 `</p>` -->
          </p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="link-all" @click="goAllComments">
          <!-- [zh] 模板内容：`查看全部{{ commentTotal > 0 ? `(${commentTotal})`` -->
          查看全部{{ commentTotal > 0 ? `(${commentTotal})` : '' }}
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon><ArrowRight /></el-icon>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="previewComments.length" class="comment-preview-list">
        <!-- [zh] 开始标签 `<article>` -->
        <article v-for="c in previewComments" :key="c.orderId" class="comment-preview-item">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="preview-head">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="user">{{ maskCommenterName(c.nickName) }}</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span v-if="getCommentLevel(c.userId)" class="comment-level-tag" :class="commentLevelTagClass(getCommentLevel(c.userId)!.levelCode)">
              <!-- [zh] Mustache 插值表达式 -->
              {{ getCommentLevel(c.userId)!.levelName }}
            <!-- [zh] 闭合标签 `</span>` -->
            </span>
            <!-- [zh] 开始标签 `<el-rate>` -->
            <el-rate v-if="c.star" :model-value="c.star" disabled size="small" />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="text">{{ c.commentContent }}</p>
          <!-- [zh] 开始标签 `<button>` -->
          <button
            type="button"
            class="report-btn"
            @click="openReport({ orderId: c.orderId, commentContent: c.commentContent })"
          >
            <!-- [zh] 模板内容：`举报` -->
            举报
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</article>` -->
        </article>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else class="empty-tip">暂无评价，快来抢沙发吧</p>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="block desc-block">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="block-title">图文详情</h3>
      <!-- [zh] 开始标签 `<MarkdownContent>` -->
      <MarkdownContent :content="productInfo.productDesc" class="desc-body" />
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="block similar-block">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="block-title">看了又看</h3>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="similarProducts.length" class="similar-grid">
        <!-- [zh] 开始标签 `<button>` -->
        <button
          v-for="item in similarProducts"
          :key="item.productId"
          type="button"
          class="similar-item"
          @click="goDetail(item)"
        >
          <!-- [zh] 开始标签 `<ProductImage>` -->
          <ProductImage :product="item" fit="cover" width="100%" height="100%" class="similar-img" />
          <!-- [zh] 开始标签 `<p>` -->
          <p class="similar-name">{{ item.productName }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="similar-price">¥{{ formatPrice(item.price ?? item.salePrice ?? item.minPrice) }}</p>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="loadingMore" class="load-tip">加载中…</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else-if="finished && similarProducts.length" class="load-tip">已展示全部推荐商品</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else-if="!similarProducts.length && !loadingMore" class="similar-empty">暂无推荐</p>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<LiquidGlassSurface>` -->
    <LiquidGlassSurface tag="footer" intensity="medium" class="detail-footer ignore">
      <!-- [zh] 开始标签 `<AgentServiceEntry>` -->
      <AgentServiceEntry
        class="footer-agent"
        :compact="false"
        show-label
        :consult-product="agentConsultProduct"
      />
      <!-- [zh] 开始标签 `<button>` -->
      <button
        type="button"
        class="footer-fav"
        :class="{ active: favorited }"
        :disabled="favoriteLoading"
        aria-label="收藏"
        @click="toggleFavorite"
      >
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon :size="22">
          <!-- [zh] 开始标签 `<StarFilled>` -->
          <StarFilled v-if="favorited" />
          <!-- [zh] 开始标签 `<Star>` -->
          <Star v-else />
        <!-- [zh] 闭合标签 `</el-icon>` -->
        </el-icon>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="label">{{ favorited ? '已收藏' : '收藏' }}</span>
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button class="btn-cart" type="primary" plain round @click="openAddCartSheet">加入购物车</el-button>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button class="btn-buy" type="primary" round @click="buyNow">立即购买</el-button>
    <!-- [zh] 闭合标签 `</LiquidGlassSurface>` -->
    </LiquidGlassSurface>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
  <!-- [zh] 开始标签 `<div>` -->
  <div v-else-if="loading" class="detail-loading card">
    <!-- [zh] 开始标签 `<el-skeleton>` -->
    <el-skeleton animated :rows="10" />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
  <!-- [zh] 开始标签 `<div>` -->
  <div v-else class="detail-error card">
    <!-- [zh] 开始标签 `<el-empty>` -->
    <el-empty :description="loadError ? '商品加载失败' : '商品不存在或已下架'">
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" round @click="load">重试</el-button>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button round @click="router.push('/')">返回首页</el-button>
    <!-- [zh] 闭合标签 `</el-empty>` -->
    </el-empty>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
  <!-- [zh] 开始标签 `<CommentReportDialog>` -->
  <CommentReportDialog ref="reportDialogRef" />
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { ref, watch, onMounted, onUnmounted } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { ArrowRight, Star, StarFilled } from '@element-plus/icons-vue';
// [zh] 导入 `@/components/agent/AgentServiceEntry.vue`
import AgentServiceEntry from '@/components/agent/AgentServiceEntry.vue';
// [zh] 导入 `@/components/common/LiquidGlassSurface.vue`
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/components/common/MarkdownContent.vue`
import MarkdownContent from '@/components/common/MarkdownContent.vue';
// [zh] 导入 `@/components/common/BrandMark.vue`
import BrandMark from '@/components/common/BrandMark.vue';
// [zh] 导入 `@/components/business/CommentReportDialog.vue`
import CommentReportDialog from '@/components/business/CommentReportDialog.vue';
// [zh] 导入 `@/composables/useProductDetailPage`
import { useProductDetailPage } from '@/composables/useProductDetailPage';
// [zh] 导入 `@/utils/comment`
import { maskCommenterName } from '@/utils/comment';
// [zh] 导入 `@/api/modules`
import { userMemberApi, productApi } from '@/api/modules';

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `router`
const router = useRouter();

// [zh] 代码块开始
const {
  // [zh] TypeScript/JS 语句：`loading,`
  loading,
  // [zh] TypeScript/JS 语句：`loadError,`
  loadError,
  // [zh] TypeScript/JS 语句：`load,`
  load,
  // [zh] TypeScript/JS 语句：`productInfo,`
  productInfo,
  // [zh] TypeScript/JS 语句：`productPropertyList,`
  productPropertyList,
  // [zh] TypeScript/JS 语句：`quantity,`
  quantity,
  // [zh] TypeScript/JS 语句：`selectedSku,`
  selectedSku,
  // [zh] TypeScript/JS 语句：`selectedProperty,`
  selectedProperty,
  // [zh] TypeScript/JS 语句：`activeImageIndex,`
  activeImageIndex,
  // [zh] TypeScript/JS 语句：`favorited,`
  favorited,
  // [zh] TypeScript/JS 语句：`favoriteLoading,`
  favoriteLoading,
  // [zh] TypeScript/JS 语句：`galleryImages,`
  galleryImages,
  // [zh] TypeScript/JS 语句：`displayPrice,`
  displayPrice,
  // [zh] TypeScript/JS 语句：`agentConsultProduct,`
  agentConsultProduct,
  // [zh] TypeScript/JS 语句：`previewComments,`
  previewComments,
  // [zh] TypeScript/JS 语句：`commentTotal,`
  commentTotal,
  // [zh] TypeScript/JS 语句：`commentGoodRate,`
  commentGoodRate,
  // [zh] TypeScript/JS 语句：`commentImageCount,`
  commentImageCount,
  // [zh] TypeScript/JS 语句：`maxBuy,`
  maxBuy,
  // [zh] TypeScript/JS 语句：`onTouchStart,`
  onTouchStart,
  // [zh] TypeScript/JS 语句：`onTouchMove,`
  onTouchMove,
  // [zh] TypeScript/JS 语句：`onTouchEnd,`
  onTouchEnd,
  // [zh] TypeScript/JS 语句：`onMouseDown,`
  onMouseDown,
  // [zh] TypeScript/JS 语句：`onMouseMove,`
  onMouseMove,
  // [zh] TypeScript/JS 语句：`onMouseUp,`
  onMouseUp,
  // [zh] TypeScript/JS 语句：`selectProperty,`
  selectProperty,
  // [zh] TypeScript/JS 语句：`toggleFavorite,`
  toggleFavorite,
  // [zh] TypeScript/JS 语句：`goAllComments,`
  goAllComments,
  // [zh] TypeScript/JS 语句：`openAddCartSheet,`
  openAddCartSheet,
  // [zh] TypeScript/JS 语句：`buyNow`
  buyNow
// [zh] 语句
} = useProductDetailPage();

// [zh] 声明变量 `commentLevelCache`
const commentLevelCache = ref<Record<string, { levelCode: number; levelName: string }>>({});

// [zh] 声明变量 `fetchCommentLevel`
const fetchCommentLevel = (userId: string) => {
  // [zh] 条件判断 if
  if (!userId || commentLevelCache.value[userId]) return;
  // [zh] 代码块开始
  userMemberApi.getLevelBadge(userId).then((res: any) => {
    // [zh] 条件判断 if
    if (res?.levelCode != null) {
      // [zh] 语句
      commentLevelCache.value = { ...commentLevelCache.value, [userId]: res };
    // [zh] 语法符号
    }
  // [zh] 语句
  }).catch(() => {});
// [zh] 语句
};

// [zh] 声明变量 `getCommentLevel`
const getCommentLevel = (userId?: string) => (userId && commentLevelCache.value[userId]) || null;

// [zh] 声明变量 `commentLevelTagClass`
const commentLevelTagClass = (code: number) => {
  // [zh] 条件判断 if
  if (code >= 3) return 'level-gold';
  // [zh] 条件判断 if
  if (code >= 2) return 'level-silver';
  // [zh] return 返回
  return 'level-default';
// [zh] 语句
};

// [zh] 代码块开始
watch(previewComments, () => {
  // [zh] 代码块开始
  previewComments.value.forEach((c: any) => {
    // [zh] 条件判断 if
    if (c.userId) fetchCommentLevel(c.userId);
  // [zh] 语句
  });
// [zh] 语句
}, { immediate: true });

// [zh] 声明变量 `reportDialogRef`
const reportDialogRef = ref<InstanceType<typeof CommentReportDialog>>();

// [zh] 声明变量 `similarProducts`
const similarProducts = ref<any[]>([]);
// [zh] 声明变量 `loadingMore`
const loadingMore = ref(false);
// [zh] 声明变量 `finished`
const finished = ref(false);
// [zh] 声明变量 `allSimilarProducts`
const allSimilarProducts = ref<any[]>([]);
// [zh] 声明变量 `displayCount`
const displayCount = ref(6);
// [zh] 声明变量 `PAGE_SIZE`
const PAGE_SIZE = 6;
// [zh] 声明变量 `maxSimilarProducts`
const maxSimilarProducts = 12;
// [zh] 声明变量 `scrollTicking`
let scrollTicking = false;

// [zh] 声明变量 `onScroll`
const onScroll = () => {
  // [zh] 条件判断 if
  if (scrollTicking || finished.value || loadingMore.value) return;
  // [zh] 语句
  scrollTicking = true;
  // [zh] 代码块开始
  requestAnimationFrame(() => {
    // [zh] 语句
    scrollTicking = false;
    // [zh] 声明变量 `st`
    const st = window.scrollY || document.documentElement.scrollTop;
    // [zh] 声明变量 `sh`
    const sh = document.documentElement.scrollHeight;
    // [zh] 声明变量 `ch`
    const ch = window.innerHeight;
    // [zh] 条件判断 if
    if (sh - st - ch < 200) loadMore();
  // [zh] 语句
  });
// [zh] 语句
};

// [zh] 声明变量 `loadMore`
const loadMore = () => {
  // [zh] 条件判断 if
  if (finished.value || loadingMore.value) return;
  // [zh] 条件判断 if
  if (displayCount.value >= allSimilarProducts.value.length) {
    // [zh] 语句
    finished.value = true;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  loadingMore.value = true;
  // [zh] 代码块开始
  setTimeout(() => {
    // [zh] 语句
    displayCount.value = Math.min(displayCount.value + PAGE_SIZE, maxSimilarProducts, allSimilarProducts.value.length);
    // [zh] 语句
    similarProducts.value = allSimilarProducts.value.slice(0, displayCount.value);
    // [zh] 条件判断 if
    if (displayCount.value >= allSimilarProducts.value.length) finished.value = true;
    // [zh] 语句
    loadingMore.value = false;
  // [zh] 语句
  }, 300);
// [zh] 语句
};

// [zh] 声明变量 `loadSimilarProducts`
const loadSimilarProducts = async () => {
  // [zh] 语句
  loadingMore.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `data`
    const data = await productApi.loadCommendProduct();
    // [zh] 声明变量 `list`
    const list = Array.isArray(data) ? data : data?.list || [];
    // [zh] 条件判断 if
    if (!list.length) { finished.value = true; return; }

    // [zh] 声明变量 `filled`
    let filled = [...list];
    // [zh] 代码块开始
    while (filled.length < maxSimilarProducts) {
      // [zh] 语句
      filled = filled.concat(list);
    // [zh] 语法符号
    }
    // [zh] 语句
    allSimilarProducts.value = filled.slice(0, maxSimilarProducts);
    // [zh] 语句
    similarProducts.value = allSimilarProducts.value.slice(0, displayCount.value);
    // [zh] 条件判断 if
    if (allSimilarProducts.value.length <= displayCount.value) finished.value = true;
  // [zh] 代码块开始
  } catch (error) {
    // [zh] 语句
    console.error('ProductDetailView: loadSimilarProducts error', error);
    // [zh] 语句
    finished.value = true;
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    loadingMore.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `formatPrice`
const formatPrice = (price: any): string => {
  // [zh] 声明变量 `n`
  const n = Number(price);
  // [zh] 条件判断 if
  if (isNaN(n)) return '--';
  // [zh] return 返回
  return n.toFixed(2);
// [zh] 语句
};

// [zh] 声明变量 `goDetail`
const goDetail = (p: any) => {
  // [zh] 条件判断 if
  if (p?.productId) router.push(`/product/${p.productId}`);
// [zh] 语句
};

// [zh] 代码块开始
onMounted(() => {
  // [zh] 语句
  loadSimilarProducts();
  // [zh] 语句
  window.addEventListener('scroll', onScroll, { passive: true });
// [zh] 语句
});

// [zh] 语句
onUnmounted(() => window.removeEventListener('scroll', onScroll));

// [zh] 声明变量 `openReport`
const openReport = (payload: { orderId: string; commentContent?: string }) => {
  // [zh] 代码块开始
  reportDialogRef.value?.show({
    // [zh] TypeScript/JS 语句：`orderId: payload.orderId,`
    orderId: payload.orderId,
    // [zh] TypeScript/JS 语句：`productId: String(route.params.productId || ''),`
    productId: String(route.params.productId || ''),
    // [zh] TypeScript/JS 语句：`commentContent: payload.commentContent`
    commentContent: payload.commentContent
  // [zh] 语句
  });
// [zh] 语句
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.detail-simlect {` */
.detail-simlect {
  /* [zh] 样式规则 `margin: 0 -16px;` */
  margin: 0 -16px;
  /* [zh] 样式规则 `padding-bottom: calc(56px + env(safe-are` */
  padding-bottom: calc(56px + env(safe-area-inset-bottom, 0));
  /* [zh] 样式规则 `background: transparent;` */
  background: transparent;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.detail-loading {` */
.detail-loading {
  /* [zh] 样式规则 `margin: 12px;` */
  margin: 12px;
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.gallery-wrap {` */
.gallery-wrap {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-height: 420px;` */
  max-height: 420px;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.gallery-swipe {` */
.gallery-swipe {
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `touch-action: pan-x pan-y;` */
  touch-action: pan-x pan-y;
  /* [zh] 样式规则 `user-select: none;` */
  user-select: none;
  /* [zh] 样式规则 `cursor: grab;` */
  cursor: grab;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `cursor: grabbing;` */
    cursor: grabbing;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.gallery-track {` */
.gallery-track {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `transition: transform 0.3s ease;` */
  transition: transform 0.3s ease;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.gallery-slide {` */
.gallery-slide {
  /* [zh] 样式规则 `flex: 0 0 100%;` */
  flex: 0 0 100%;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `aspect-ratio: 1;` */
  aspect-ratio: 1;
  /* [zh] 样式规则 `max-height: 420px;` */
  max-height: 420px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.gallery-img {` */
.gallery-img {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
  /* [zh] 样式规则 `object-fit: contain;` */
  object-fit: contain;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.gallery-empty {` */
.gallery-empty {
  /* [zh] 样式规则 `height: 280px;` */
  height: 280px;
  /* [zh] 样式规则 `background: $color-bg;` */
  background: $color-bg;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.gallery-indicator {` */
.gallery-indicator {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `right: 12px;` */
  right: 12px;
  /* [zh] 样式规则 `bottom: 12px;` */
  bottom: 12px;
  /* [zh] 样式规则 `padding: 2px 10px;` */
  padding: 2px 10px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.45);` */
  background: rgba(0, 0, 0, 0.45);
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `z-index: 2;` */
  z-index: 2;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.block {` */
.block {
  /* [zh] 样式规则 `margin-top: 8px;` */
  margin-top: 8px;
  /* [zh] 样式规则 `padding: 14px 16px;` */
  padding: 14px 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.info-block {` */
.info-block {
  /* [zh] 样式规则 `.product-name {` */
  .product-name {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `line-height: 1.45;` */
    line-height: 1.45;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.price-line {` */
  .price-line {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: baseline;` */
    align-items: baseline;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;

    /* [zh] 样式规则 `.currency {` */
    .currency {
      /* [zh] 样式规则 `font-size: 16px;` */
      font-size: 16px;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.amount {` */
    .amount {
      /* [zh] 样式规则 `font-size: 28px;` */
      font-size: 28px;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sales-line {` */
  .sales-line {
    /* [zh] 样式规则 `margin: 8px 0 0;` */
    margin: 8px 0 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.info-agent-btn {` */
.info-agent-btn {
  /* [zh] 样式规则 `margin-top: 12px;` */
  margin-top: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-block-wrap {` */
.sku-block-wrap {
  /* [zh] 样式规则 `.sku-row {` */
  .sku-row {
    /* [zh] 样式规则 `margin-bottom: 12px;` */
    margin-bottom: 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sku-label {` */
  .sku-label {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `margin-bottom: 8px;` */
    margin-bottom: 8px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sku-values {` */
  .sku-values {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-wrap: nowrap;` */
    flex-wrap: nowrap;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `overflow-x: auto;` */
    overflow-x: auto;
    /* [zh] 样式规则 `padding-bottom: 2px;` */
    padding-bottom: 2px;
    /* [zh] 样式规则 `-webkit-overflow-scrolling: touch;` */
    -webkit-overflow-scrolling: touch;
    /* [zh] 样式规则 `scrollbar-width: none;` */
    scrollbar-width: none;

    /* [zh] 样式规则 `&::-webkit-scrollbar {` */
    &::-webkit-scrollbar {
      /* [zh] 样式规则 `display: none;` */
      display: none;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sku-tag {` */
  .sku-tag {
    /* [zh] 样式规则 `display: inline-flex;` */
    display: inline-flex;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;
    /* [zh] 样式规则 `padding: 6px 12px;` */
    padding: 6px 12px;
    /* [zh] 样式规则 `border: 1px solid $color-border;` */
    border: 1px solid $color-border;
    /* [zh] 样式规则 `border-radius: $radius-pill;` */
    border-radius: $radius-pill;
    /* [zh] 样式规则 `background: #fafafa;` */
    background: #fafafa;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `transition: border-color $transition-fas` */
    transition: border-color $transition-fast, color $transition-fast, background $transition-fast;

    /* [zh] 样式规则 `&.active {` */
    &.active {
      /* [zh] 样式规则 `border-color: $color-primary;` */
      border-color: $color-primary;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `background: $color-primary-muted;` */
      background: $color-primary-muted;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.sku-thumb {` */
    .sku-thumb {
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
      /* [zh] 样式规则 `width: 18px;` */
      width: 18px;
      /* [zh] 样式规则 `height: 18px;` */
      height: 18px;
      /* [zh] 样式规则 `border-radius: $radius-xs;` */
      border-radius: $radius-xs;
      /* [zh] 样式规则 `overflow: hidden;` */
      overflow: hidden;
      /* [zh] 样式规则 `background: #fff;` */
      background: #fff;
      /* [zh] 样式规则 `border: 1px solid rgba($color-border, 0.` */
      border: 1px solid rgba($color-border, 0.55);

      /* [zh] 样式规则 `:deep(.product-image) {` */
      :deep(.product-image) {
        /* [zh] 样式规则 `width: 18px !important;` */
        width: 18px !important;
        /* [zh] 样式规则 `height: 18px !important;` */
        height: 18px !important;
        /* [zh] 样式规则 `border-radius: $radius-xs;` */
        border-radius: $radius-xs;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.qty-row {` */
  .qty-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-wrap: nowrap;` */
    flex-wrap: nowrap;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 10px;` */
    gap: 10px;
    /* [zh] 样式规则 `padding-top: 4px;` */
    padding-top: 4px;
    /* [zh] 样式规则 `overflow-x: auto;` */
    overflow-x: auto;
    /* [zh] 样式规则 `scrollbar-width: none;` */
    scrollbar-width: none;

    /* [zh] 样式规则 `&::-webkit-scrollbar {` */
    &::-webkit-scrollbar {
      /* [zh] 样式规则 `display: none;` */
      display: none;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.stock-tip {` */
    .stock-tip {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
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
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.delivery-block {` */
.delivery-block {
  /* [zh] 样式规则 `.info-row {` */
  .info-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.info-label {` */
  .info-label {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.info-value {` */
  .info-value {
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.quality-badges-row {` */
.quality-badges-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 12px 0;` */
  padding: 12px 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.quality-badge {` */
.quality-badge {
    /* [zh] 样式规则 `display: inline-flex;` */
    display: inline-flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;
    /* [zh] 样式规则 `padding: 4px 12px;` */
    padding: 4px 12px;
    /* [zh] 样式规则 `border-radius: $radius-tag;` */
    border-radius: $radius-tag;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;

    /* [zh] 样式规则 `.badge-dot {` */
    .badge-dot {
      /* [zh] 样式规则 `width: 6px;` */
      width: 6px;
      /* [zh] 样式规则 `height: 6px;` */
      height: 6px;
      /* [zh] 样式规则 `border-radius: 50%;` */
      border-radius: 50%;
      /* [zh] 样式规则 `background: $color-text-muted;` */
      background: $color-text-muted;
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.badge-icon {` */
    .badge-icon {
      /* [zh] 样式规则 `width: 14px;` */
      width: 14px;
      /* [zh] 样式规则 `height: 14px;` */
      height: 14px;
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.gold {` */
    &.gold {
      /* [zh] 样式规则 `background: $color-gold-soft;` */
      background: $color-gold-soft;
      /* [zh] 样式规则 `color: #8b7355;` */
      color: #8b7355;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;

      /* [zh] 样式规则 `.badge-dot {` */
      .badge-dot {
        /* [zh] 样式规则 `background: $color-gold;` */
        background: $color-gold;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.badge-icon {` */
      .badge-icon {
        /* [zh] 样式规则 `:deep(.brand-mark) {` */
        :deep(.brand-mark) {
          /* [zh] 样式规则 `color: $color-gold;` */
          color: $color-gold;
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

/* [zh] 样式规则 `.service-block {` */
.service-block {
  /* [zh] 样式规则 `.info-row {` */
  .info-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: flex-start;` */
    align-items: flex-start;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.info-label {` */
  .info-label {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `line-height: 22px;` */
    line-height: 22px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.service-badges {` */
  .service-badges {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;
    /* [zh] 样式规则 `gap: 8px 16px;` */
    gap: 8px 16px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.badge {` */
  .badge {
    /* [zh] 样式规则 `display: inline-flex;` */
    display: inline-flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 3px;` */
    gap: 3px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.badge-icon {` */
  .badge-icon {
    /* [zh] 样式规则 `font-style: normal;` */
    font-style: normal;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.store-block {` */
.store-block {
  /* [zh] 样式规则 `.store-row {` */
  .store-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 10px;` */
    gap: 10px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.store-logo {` */
  .store-logo {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `width: 36px;` */
    width: 36px;
    /* [zh] 样式规则 `height: 36px;` */
    height: 36px;
    /* [zh] 样式规则 `border-radius: 50%;` */
    border-radius: 50%;
    /* [zh] 样式规则 `background: $color-primary-muted;` */
    background: $color-primary-muted;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;

    /* [zh] 样式规则 `:deep(.brand-mark) {` */
    :deep(.brand-mark) {
      /* [zh] 样式规则 `width: 22px;` */
      width: 22px;
      /* [zh] 样式规则 `height: 22px;` */
      height: 22px;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.store-info {` */
  .store-info {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.store-name {` */
  .store-name {
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.store-badge {` */
  .store-badge {
    /* [zh] 样式规则 `display: inline-block;` */
    display: inline-block;
    /* [zh] 样式规则 `padding: 1px 5px;` */
    padding: 1px 5px;
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `background: $color-primary;` */
    background: $color-primary;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.store-enter-btn {` */
  .store-enter-btn {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `padding: 4px 12px;` */
    padding: 4px 12px;
    /* [zh] 样式规则 `border: 1px solid $color-border;` */
    border: 1px solid $color-border;
    /* [zh] 样式规则 `border-radius: $radius-pill;` */
    border-radius: $radius-pill;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `transition: border-color $transition-fas` */
    transition: border-color $transition-fast, color $transition-fast;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `border-color: $color-primary;` */
      border-color: $color-primary;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.similar-block {` */
.similar-block {
  /* [zh] 样式规则 `.block-title {` */
  .block-title {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
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
    /* [zh] 样式规则 `grid-template-columns: repeat(2, minmax(` */
    grid-template-columns: repeat(2, minmax(0, 1fr));
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `max-width: 400px;` */
    max-width: 400px;
    /* [zh] 样式规则 `margin: 0 auto;` */
    margin: 0 auto;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.similar-item {` */
  .similar-item {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
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
    /* [zh] 样式规则 `max-width: 200px;` */
    max-width: 200px;

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `opacity: 0.8;` */
      opacity: 0.8;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.similar-img {` */
  .similar-img {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `max-width: 180px;` */
    max-width: 180px;
    /* [zh] 样式规则 `aspect-ratio: 1;` */
    aspect-ratio: 1;
    /* [zh] 样式规则 `border-radius: $radius-sm;` */
    border-radius: $radius-sm;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
    /* [zh] 样式规则 `margin: 0 auto;` */
    margin: 0 auto;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.similar-name {` */
  .similar-name {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `line-height: 1.35;` */
    line-height: 1.35;
    /* [zh] 样式规则 `display: -webkit-box;` */
    display: -webkit-box;
    /* [zh] 样式规则 `-webkit-line-clamp: 1;` */
    -webkit-line-clamp: 1;
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

  /* [zh] 样式规则 `.similar-empty {` */
  .similar-empty {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `padding: 20px 0;` */
    padding: 20px 0;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
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
    /* [zh] 样式规则 `padding: 8px 0;` */
    padding: 8px 0;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.comment-block {` */
.comment-block {
  /* [zh] 样式规则 `.block-head {` */
  .block-head {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: space-between;` */
    justify-content: space-between;
    /* [zh] 样式规则 `margin-bottom: 12px;` */
    margin-bottom: 12px;

    /* [zh] 样式规则 `h3 {` */
    h3 {
      /* [zh] 样式规则 `margin: 0;` */
      margin: 0;
      /* [zh] 样式规则 `font-size: 15px;` */
      font-size: 15px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.link-all {` */
    .link-all {
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
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;

      /* [zh] 样式规则 `&:hover {` */
      &:hover {
        /* [zh] 样式规则 `color: $color-primary;` */
        color: $color-primary;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.comment-preview-item {` */
.comment-preview-item {
  /* [zh] 样式规则 `padding: 10px 0;` */
  padding: 10px 0;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.preview-head {` */
  .preview-head {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;

    /* [zh] 样式规则 `.el-rate {` */
    .el-rate {
      /* [zh] 样式规则 `margin-left: auto;` */
      margin-left: auto;
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.user {` */
  .user {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.comment-level-tag {` */
  .comment-level-tag {
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

  /* [zh] 样式规则 `.text {` */
  .text {
    /* [zh] 样式规则 `margin: 6px 0 0;` */
    margin: 6px 0 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `line-height: 1.5;` */
    line-height: 1.5;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
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
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
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

/* [zh] 样式规则 `.empty-tip {` */
.empty-tip {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.desc-block {` */
.desc-block {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `z-index: 1;` */
  z-index: 1;

  /* [zh] 样式规则 `.block-title {` */
  .block-title {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title !important;` */
    color: $color-text-title !important;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.desc-body {` */
  .desc-body {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `position: relative;` */
    position: relative;
    /* [zh] 样式规则 `z-index: 1;` */
    z-index: 1;

    /* [zh] 样式声明 */
    :deep(p),
    /* [zh] 样式声明 */
    :deep(span),
    /* [zh] 样式声明 */
    :deep(h1),
    /* [zh] 样式声明 */
    :deep(h2),
    /* [zh] 样式声明 */
    :deep(h3),
    /* [zh] 样式声明 */
    :deep(h4),
    /* [zh] 样式声明 */
    :deep(h5),
    /* [zh] 样式规则 `:deep(h6) {` */
    :deep(h6) {
      /* [zh] 样式规则 `color: $color-text-body !important;` */
      color: $color-text-body !important;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.detail-footer {` */
.detail-footer {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `left: 0;` */
  left: 0;
  /* [zh] 样式规则 `right: 0;` */
  right: 0;
  /* [zh] 样式规则 `bottom: 0;` */
  bottom: 0;
  /* [zh] 样式规则 `z-index: 1002;` */
  z-index: 1002;
  /* [zh] 样式规则 `border-top: 1px solid var(--glass-border` */
  border-top: 1px solid var(--glass-border-soft);
  /* [zh] 样式规则 `box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.` */
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);

  /* [zh] 样式规则 `:deep(.liquid-glass-surface__content) {` */
  :deep(.liquid-glass-surface__content) {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
    /* [zh] 样式规则 `padding: 8px 12px;` */
    padding: 8px 12px;
    /* [zh] 样式规则 `padding-bottom: calc(8px + env(safe-area` */
    padding-bottom: calc(8px + env(safe-area-inset-bottom, 0));
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.footer-agent {` */
  .footer-agent {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.footer-fav {` */
  .footer-fav {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 2px;` */
    gap: 2px;
    /* [zh] 样式规则 `padding: 0 4px;` */
    padding: 0 4px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: none;` */
    background: none;
    /* [zh] 样式规则 `font-size: 10px;` */
    font-size: 10px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;

    /* [zh] 样式规则 `&.active {` */
    &.active {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&:disabled {` */
    &:disabled {
      /* [zh] 样式规则 `opacity: 0.6;` */
      opacity: 0.6;
      /* [zh] 样式规则 `cursor: not-allowed;` */
      cursor: not-allowed;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  .btn-cart,
  /* [zh] 样式规则 `.btn-buy {` */
  .btn-buy {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `height: 40px;` */
    height: 40px;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
