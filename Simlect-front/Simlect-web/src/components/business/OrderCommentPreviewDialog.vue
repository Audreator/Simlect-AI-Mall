<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-dialog>` -->
  <el-dialog
    v-model="visible"
    title="评价详情"
    width="92%"
    :style="{ maxWidth: '520px' }"
    destroy-on-close
    class="comment-preview-dialog"
  >
    <!-- [zh] 开始标签 `<el-skeleton>` -->
    <el-skeleton v-if="loading" animated :rows="6" />
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template v-else>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="data" class="preview">
        <!-- [zh] 开始标签 `<header>` -->
        <header
          v-if="data.productName || productCover"
          class="product-head"
          role="button"
          tabindex="0"
          @click="goProductDetail"
          @keydown.enter="goProductDetail"
        >
          <!-- [zh] 开始标签 `<img>` -->
          <img v-if="productCover" :src="productCover" class="product-cover" alt="" />
          <!-- [zh] 开始标签 `<div>` -->
          <div class="product-meta">
            <!-- [zh] 开始标签 `<p>` -->
            <p class="product-name">{{ data.productName || '商品' }}</p>
            <!-- [zh] 开始标签 `<p>` -->
            <p v-if="data.propertyInfo" class="product-spec">{{ data.propertyInfo }}</p>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon class="product-arrow"><ArrowRight /></el-icon>
        <!-- [zh] 闭合标签 `</header>` -->
        </header>

        <!-- [zh] 开始标签 `<div>` -->
        <div class="row">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">评价星级</span>
          <!-- [zh] 开始标签 `<el-rate>` -->
          <el-rate :model-value="Number(data.star || 0)" disabled />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="data.commentTime" class="row">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">评价时间</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="value">{{ formatTime(data.commentTime) }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="row col">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">评价内容</span>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="text">{{ data.commentContent || '（无）' }}</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="commentImages.length" class="row col">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">评价图片</span>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="img-list">
            <!-- [zh] 开始标签 `<img>` -->
            <img
              v-for="(img, idx) in commentImages"
              :key="idx"
              :src="toImageSrc(img)"
              class="img"
              alt=""
              @click="previewImage(commentImages, idx)"
            />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="data.commentBizReply" class="biz-reply">
          <!-- [zh] 开始标签 `<p>` -->
          <p class="biz-label">商家回复</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="text">{{ data.commentBizReply }}</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="data.recommentContent || recommentImages.length" class="divider">追评</div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="data.recommentTime" class="row">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">追评时间</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="value">{{ formatTime(data.recommentTime) }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="data.recommentContent" class="row col">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">追评内容</span>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="text">{{ data.recommentContent }}</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="recommentImages.length" class="row col">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">追评图片</span>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="img-list">
            <!-- [zh] 开始标签 `<img>` -->
            <img
              v-for="(img, idx) in recommentImages"
              :key="idx"
              :src="toImageSrc(img)"
              class="img"
              alt=""
              @click="previewImage(recommentImages, idx)"
            />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<el-empty>` -->
      <el-empty v-else description="暂无评价内容" />
    </template>
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #footer>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowRight } from '@element-plus/icons-vue';
import { commentApi, productApi } from '@/api/modules';
import { isProductOnSale } from '@/utils/product';
import { resolveImageUrl, splitImagePaths } from '@/utils/image';
import { openImagePreview, closeImagePreview } from '@/composables/imagePreview';
import { toast } from '@/utils/toast';

const router = useRouter();
const visible = ref(false);
const loading = ref(false);
const navigating = ref(false);
const data = ref<Record<string, any> | null>(null);

const toImageSrc = (path: string) => resolveImageUrl(path, { useThumbnail: false }) || path;

const commentImages = computed(() => splitImagePaths(data.value?.commentImages as string | null));
const recommentImages = computed(() => splitImagePaths(data.value?.recommentImages as string | null));

const productCover = computed(() => {
  const cover = data.value?.cover;
  return cover ? resolveImageUrl(cover) : '';
});

const formatTime = (val: unknown) => {
  if (!val) return '--';
  if (typeof val === 'string') return val.replace('T', ' ').slice(0, 19);
  const d = new Date(val as string | number);
  if (Number.isNaN(d.getTime())) return String(val);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const previewImage = (list: string[], index: number) => {
  const urls = list.map((p) => toImageSrc(p)).filter(Boolean);
  if (!urls.length) return;
  openImagePreview(urls, Math.min(Math.max(index, 0), urls.length - 1));
};

const goProductDetail = async () => {
  const productId = data.value?.productId;
  if (!productId) {
    toast.warning('无法获取商品信息');
    return;
  }
  if (navigating.value) return;
  navigating.value = true;
  try {
    const res = await productApi.getProduct(String(productId));
    const info = res?.productInfo;
    if (!info) {
      toast.warning('商品不存在或已下架');
      return;
    }
    if (!isProductOnSale(info)) {
      toast.warning('该商品已下架');
      return;
    }
    visible.value = false;
    await router.push(`/product/${productId}`);
  } catch {
    toast.warning('商品不存在或已下架');
  } finally {
    navigating.value = false;
  }
};

const show = async (payload: string | Record<string, unknown>) => {
  visible.value = true;
  loading.value = true;
  data.value = null;
  try {
    if (typeof payload === 'object' && payload !== null) {
      data.value = { ...payload };
      return;
    }
    data.value = (await commentApi.getComment(payload)) || null;
  } finally {
    loading.value = false;
  }
};

watch(visible, (open) => {
  if (!open) closeImagePreview();
});

defineExpose({ show });
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.preview {` */
.preview {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `max-height: min(70vh, 560px);` */
  max-height: min(70vh, 560px);
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-head {` */
.product-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding-bottom: 12px;` */
  padding-bottom: 12px;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border-l` */
  border-bottom: 1px solid $color-border-light;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `transition: background 0.15s ease;` */
  transition: background 0.15s ease;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `opacity: 0.92;` */
    opacity: 0.92;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-arrow {` */
.product-arrow {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `font-size: 16px;` */
  font-size: 16px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-cover {` */
.product-cover {
  /* [zh] 样式规则 `width: 56px;` */
  width: 56px;
  /* [zh] 样式规则 `height: 56px;` */
  height: 56px;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `object-fit: cover;` */
  object-fit: cover;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-meta {` */
.product-meta {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-name {` */
.product-name {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `line-height: 1.4;` */
  line-height: 1.4;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.product-spec {` */
.product-spec {
  /* [zh] 样式规则 `margin: 4px 0 0;` */
  margin: 4px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.row {` */
.row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.row.col {` */
.row.col {
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.label {` */
.label {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.value {` */
.value {
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.text {` */
.text {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `line-height: 1.55;` */
  line-height: 1.55;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `white-space: pre-wrap;` */
  white-space: pre-wrap;
  /* [zh] 样式规则 `word-break: break-word;` */
  word-break: break-word;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.img-list {` */
.img-list {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(4, 1fr);` */
  grid-template-columns: repeat(4, 1fr);
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.img {` */
.img {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `aspect-ratio: 1;` */
  aspect-ratio: 1;
  /* [zh] 样式规则 `object-fit: cover;` */
  object-fit: cover;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.biz-reply {` */
.biz-reply {
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;
  /* [zh] 样式规则 `border-radius: $radius-btn;` */
  border-radius: $radius-btn;
  /* [zh] 样式规则 `background: rgba($color-primary, 0.06);` */
  background: rgba($color-primary, 0.06);
  /* [zh] 样式规则 `border: 1px solid rgba($color-primary, 0` */
  border: 1px solid rgba($color-primary, 0.12);

  /* [zh] 样式规则 `.biz-label {` */
  .biz-label {
    /* [zh] 样式规则 `margin: 0 0 6px;` */
    margin: 0 0 6px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.divider {` */
.divider {
  /* [zh] 样式规则 `margin-top: 2px;` */
  margin-top: 2px;
  /* [zh] 样式规则 `padding-top: 10px;` */
  padding-top: 10px;
  /* [zh] 样式规则 `border-top: 1px solid $color-border;` */
  border-top: 1px solid $color-border;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}
</style>
