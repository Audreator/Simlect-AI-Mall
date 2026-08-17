<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="action-confirm-card" :class="statusClass">
    <!-- [zh] 开始标签 `<header>` -->
    <header class="card-head">
      <!-- [zh] 开始标签 `<p>` -->
      <p class="card-title">{{ card.label || '待确认操作' }}</p>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="isPending" class="card-badge">待确认</span>
    <!-- [zh] 闭合标签 `</header>` -->
    </header>

    <!-- [zh] 开始标签 `<p>` -->
    <p class="card-hint">{{ cardHint }}</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="card.orderId" class="order-meta">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="order-id" :title="card.orderId">订单号 {{ shortOrderId }}</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="orderAmountText" class="order-amount">{{ orderAmountText }}</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<ul>` -->
    <ul v-if="orderItems.length" class="item-list">
      <!-- [zh] 开始标签 `<li>` -->
      <li v-for="item in orderItems" :key="item.orderItemId || item.productId" class="item-row">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="item-cover" :class="{ 'is-coupon': isCouponOrder }">
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon v-if="isCouponOrder" class="coupon-icon"><Ticket /></el-icon>
          <!-- [zh] 开始标签 `<ProductImage>` -->
          <ProductImage v-else :source="item.cover" width="52" height="52" />
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="item-info">
          <!-- [zh] 开始标签 `<p>` -->
          <p class="item-name">{{ item.productName }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="item.propertyInfo && !isCouponOrder" class="item-sku">{{ item.propertyInfo }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="item.orderItemId" class="item-id" :title="item.orderItemId">
            <!-- [zh] 模板内容：`订单项 ID {{ item.orderItemId }}` -->
            订单项 ID {{ item.orderItemId }}
          <!-- [zh] 闭合标签 `</p>` -->
          </p>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="itemMeta(item)" class="item-meta">{{ itemMeta(item) }}</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</li>` -->
      </li>
    <!-- [zh] 闭合标签 `</ul>` -->
    </ul>

    <!-- [zh] 开始标签 `<dl>` -->
    <dl v-if="detailRows.length" class="detail-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="row in detailRows" :key="row.label" class="detail-row">
        <!-- [zh] 开始标签 `<dt>` -->
        <dt>{{ row.label }}</dt>
        <!-- [zh] 开始标签 `<dd>` -->
        <dd>{{ row.value }}</dd>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</dl>` -->
    </dl>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="card.summary && !orderItems.length" class="summary-fallback">{{ card.summary }}</p>

    <!-- [zh] 开始标签 `<p>` -->
    <p v-if="card.riskTip" class="risk-tip">{{ card.riskTip }}</p>

    <!-- [zh] 开始标签 `<p>` -->
    <p v-if="resultMessage" class="result-msg" :class="{ success: resultSuccess, error: !resultSuccess }">
      <!-- [zh] Mustache 插值表达式 -->
      {{ resultMessage }}
    <!-- [zh] 闭合标签 `</p>` -->
    </p>

    <!-- [zh] 开始标签 `<footer>` -->
    <footer v-if="showActions" class="actions">
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="btn-cancel" :disabled="loading" @click="onCancel">取消</button>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="btn-confirm" :disabled="loading" @click="onConfirm">
        <!-- [zh] Mustache 插值表达式 -->
        {{ loading ? '处理中…' : card.confirmText || '确认提交' }}
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</footer>` -->
    </footer>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="statusLabel" class="status-label">{{ statusLabel }}</p>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { Ticket } from '@element-plus/icons-vue';
import { agentApi } from '@/api/modules';
import ProductImage from '@/components/common/ProductImage.vue';
import { toast } from '@/utils/toast';

export interface ActionConfirmDetailRow {
  label: string;
  value: string;
}

export interface ActionConfirmOrderItem {
  orderItemId?: string;
  productId?: string;
  productName?: string;
  cover?: string;
  propertyInfo?: string;
  itemAmount?: number | string;
  buyCount?: number | string;
}

export interface ActionConfirmCardData {
  type?: string;
  token?: string;
  actionType?: string;
  label?: string;
  summary?: string;
  confirmText?: string;
  riskTip?: string;
  intro?: string;
  status?: number | string;
  orderId?: string;
  orderAmount?: number | string;
  payScene?: string | number;
  items?: ActionConfirmOrderItem[];
  details?: ActionConfirmDetailRow[];
}

const props = defineProps<{
  card: ActionConfirmCardData;
}>();

const emit = defineEmits<{
  updated: [card: ActionConfirmCardData];
}>();

const PENDING = 0;
const CONFIRMED = 1;
const CANCELLED = 2;
const EXPIRED = 3;

const loading = ref(false);
const localStatus = ref<number | null>(null);
const resultMessage = ref('');
const resultSuccess = ref(false);

const effectiveStatus = computed(() => {
  const raw = localStatus.value ?? props.card.status ?? PENDING;
  const num = Number(raw);
  return Number.isFinite(num) ? num : PENDING;
});

const isPending = computed(() => effectiveStatus.value === PENDING);

const showActions = computed(() => isPending.value && !resultMessage.value);

const statusClass = computed(() => {
  if (effectiveStatus.value === CONFIRMED) return 'is-confirmed';
  if (effectiveStatus.value === CANCELLED) return 'is-cancelled';
  if (effectiveStatus.value === EXPIRED) return 'is-expired';
  return 'is-pending';
});

const statusLabel = computed(() => {
  if (effectiveStatus.value === CONFIRMED) return '已确认执行';
  if (effectiveStatus.value === CANCELLED) return '已取消';
  if (effectiveStatus.value === EXPIRED) return '已过期，请重新发起';
  return '';
});

const cardHint = computed(() => {
  const intro = (props.card.intro || '').trim();
  if (!intro || intro.length > 100) {
    return '请核对以下信息，确认后将立即执行。';
  }
  return intro;
});

const detailRows = computed(() => {
  const rows = props.card.details;
  if (!Array.isArray(rows)) return [];
  return rows
    .map((row) => ({
      label: row?.label != null ? String(row.label).trim() : '',
      value: row?.value != null ? String(row.value).trim() : ''
    }))
    .filter((row) => row.label && row.value);
});

const orderItems = computed(() => {
  const items = props.card.items;
  if (!Array.isArray(items)) return [];
  return items
    .map((item) => ({
      orderItemId: item?.orderItemId != null ? String(item.orderItemId) : undefined,
      productId: item?.productId != null ? String(item.productId) : undefined,
      productName: item?.productName != null ? String(item.productName).trim() : '',
      cover: item?.cover != null ? String(item.cover) : undefined,
      propertyInfo: item?.propertyInfo != null ? String(item.propertyInfo).trim() : undefined,
      itemAmount: item?.itemAmount,
      buyCount: item?.buyCount
    }))
    .filter((item) => item.productName);
});

const COUPON_ORDER_PAY_SCENE = '2';
const COUPON_ORDER_PROPERTY = '优惠券秒杀';

const isCouponOrder = computed(() => {
  if (String(props.card.payScene) === COUPON_ORDER_PAY_SCENE) {
    return true;
  }
  return orderItems.value.some((item) => item.propertyInfo === COUPON_ORDER_PROPERTY);
});

const shortOrderId = computed(() => {
  const id = props.card.orderId || '';
  if (id.length <= 18) return id;
  return `${id.slice(0, 10)}…${id.slice(-6)}`;
});

const orderAmountText = computed(() => {
  const amount = props.card.orderAmount;
  if (amount == null || amount === '') return '';
  const hasDetailAmount = detailRows.value.some((row) => row.label.includes('金额'));
  if (hasDetailAmount) return '';
  return `¥${amount}`;
});

const itemMeta = (item: ActionConfirmOrderItem) => {
  const parts: string[] = [];
  if (item.buyCount != null && item.buyCount !== '') {
    parts.push(`×${item.buyCount}`);
  }
  if (item.itemAmount != null && item.itemAmount !== '') {
    parts.push(`¥${item.itemAmount}`);
  }
  return parts.join('  ');
};

const patchCard = (status: number) => {
  localStatus.value = status;
  emit('updated', { ...props.card, status });
};

const onConfirm = async () => {
  if (!props.card.token || loading.value) return;
  loading.value = true;
  try {
    const res = await agentApi.confirmAction(props.card.token);
    const data = res as { success?: boolean; resultMessage?: string; actionType?: string };
    resultSuccess.value = !!data?.success;
    resultMessage.value = data?.resultMessage || (data?.success ? '操作成功' : '操作失败');
    if (data?.success) {
      patchCard(CONFIRMED);
      toast.success(resultMessage.value);
    } else {
      toast.error(resultMessage.value);
    }
  } catch {
    toast.error('确认失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const onCancel = async () => {
  if (!props.card.token || loading.value) return;
  loading.value = true;
  try {
    await agentApi.cancelAction(props.card.token);
    patchCard(CANCELLED);
    resultMessage.value = '已取消操作';
    resultSuccess.value = false;
  } catch {
    toast.error('取消失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.action-confirm-card {` */
.action-confirm-card {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `min-width: 240px;` */
  min-width: 240px;
  /* [zh] 样式规则 `max-width: 100%;` */
  max-width: 100%;
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
  /* [zh] 样式规则 `border-radius: 10px;` */
  border-radius: 10px;
  /* [zh] 样式规则 `border: 1px solid rgba($color-primary, 0` */
  border: 1px solid rgba($color-primary, 0.28);
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.card-head {` */
.card-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.card-title {` */
.card-title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.card-badge {` */
.card-badge {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `padding: 2px 8px;` */
  padding: 2px 8px;
  /* [zh] 样式规则 `border-radius: 999px;` */
  border-radius: 999px;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `line-height: 1.4;` */
  line-height: 1.4;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `background: rgba($color-primary, 0.1);` */
  background: rgba($color-primary, 0.1);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.card-hint {` */
.card-hint {
  /* [zh] 样式规则 `margin: 0 0 10px;` */
  margin: 0 0 10px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `line-height: 1.45;` */
  line-height: 1.45;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-meta {` */
.order-meta {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-id {` */
.order-id {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `line-height: 1.35;` */
  line-height: 1.35;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `text-overflow: ellipsis;` */
  text-overflow: ellipsis;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-amount {` */
.order-amount {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-list {` */
.item-list {
  /* [zh] 样式规则 `list-style: none;` */
  list-style: none;
  /* [zh] 样式规则 `margin: 0 0 10px;` */
  margin: 0 0 10px;
  /* [zh] 样式规则 `padding: 8px;` */
  padding: 8px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-row {` */
.item-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;

  /* [zh] 样式规则 `& + & {` */
  & + & {
    /* [zh] 样式规则 `margin-top: 8px;` */
    margin-top: 8px;
    /* [zh] 样式规则 `padding-top: 8px;` */
    padding-top: 8px;
    /* [zh] 样式规则 `border-top: 1px dashed rgba($color-text-` */
    border-top: 1px dashed rgba($color-text-muted, 0.2);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-cover {` */
.item-cover {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `width: 52px;` */
  width: 52px;
  /* [zh] 样式规则 `height: 52px;` */
  height: 52px;
  /* [zh] 样式规则 `border-radius: 6px;` */
  border-radius: 6px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;

  /* [zh] 样式规则 `&.is-coupon {` */
  &.is-coupon {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `background: linear-gradient(135deg, rgba` */
    background: linear-gradient(135deg, rgba($color-primary, 0.12), rgba($color-price, 0.1));

    /* [zh] 样式规则 `.coupon-icon {` */
    .coupon-icon {
      /* [zh] 样式规则 `font-size: 28px;` */
      font-size: 28px;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-info {` */
.item-info {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-name {` */
.item-name {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.4;` */
  line-height: 1.4;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
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

/* [zh] 样式规则 `.item-sku {` */
.item-sku {
  /* [zh] 样式规则 `margin: 4px 0 0;` */
  margin: 4px 0 0;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `line-height: 1.35;` */
  line-height: 1.35;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-id {` */
.item-id {
  /* [zh] 样式规则 `margin: 4px 0 0;` */
  margin: 4px 0 0;
  /* [zh] 样式规则 `font-size: 10px;` */
  font-size: 10px;
  /* [zh] 样式规则 `line-height: 1.35;` */
  line-height: 1.35;
  /* [zh] 样式规则 `font-family: ui-monospace, SFMono-Regula` */
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `word-break: break-all;` */
  word-break: break-all;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-meta {` */
.item-meta {
  /* [zh] 样式规则 `margin: 4px 0 0;` */
  margin: 4px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `line-height: 1.35;` */
  line-height: 1.35;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.detail-list {` */
.detail-list {
  /* [zh] 样式规则 `margin: 0 0 10px;` */
  margin: 0 0 10px;
  /* [zh] 样式规则 `padding: 10px;` */
  padding: 10px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.detail-row {` */
.detail-row {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: 72px 1fr;` */
  grid-template-columns: 72px 1fr;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.45;` */
  line-height: 1.45;

  /* [zh] 样式规则 `& + & {` */
  & + & {
    /* [zh] 样式规则 `margin-top: 8px;` */
    margin-top: 8px;
    /* [zh] 样式规则 `padding-top: 8px;` */
    padding-top: 8px;
    /* [zh] 样式规则 `border-top: 1px dashed rgba($color-text-` */
    border-top: 1px dashed rgba($color-text-muted, 0.25);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.detail-row dt {` */
.detail-row dt {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `word-break: keep-all;` */
  word-break: keep-all;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.detail-row dd {` */
.detail-row dd {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `word-break: break-all;` */
  word-break: break-all;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.summary-fallback {` */
.summary-fallback {
  /* [zh] 样式规则 `margin: 0 0 10px;` */
  margin: 0 0 10px;
  /* [zh] 样式规则 `padding: 10px;` */
  padding: 10px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.45;` */
  line-height: 1.45;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.risk-tip {` */
.risk-tip {
  /* [zh] 样式规则 `margin: 0 0 10px;` */
  margin: 0 0 10px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `line-height: 1.4;` */
  line-height: 1.4;
  /* [zh] 样式规则 `color: #b45309;` */
  color: #b45309;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.actions {` */
.actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-top: 4px;` */
  margin-top: 4px;
  /* [zh] 样式规则 `padding-top: 10px;` */
  padding-top: 10px;
  /* [zh] 样式规则 `border-top: 1px solid rgba($color-text-m` */
  border-top: 1px solid rgba($color-text-muted, 0.15);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.btn-cancel,
/* [zh] 样式规则 `.btn-confirm {` */
.btn-confirm {
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `min-width: 72px;` */
  min-width: 72px;
  /* [zh] 样式规则 `padding: 8px 16px;` */
  padding: 8px 16px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.btn-cancel {` */
.btn-cancel {
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;

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

/* [zh] 样式规则 `.btn-confirm {` */
.btn-confirm {
  /* [zh] 样式规则 `background: $color-primary;` */
  background: $color-primary;
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;

  /* [zh] 样式规则 `&:disabled {` */
  &:disabled {
    /* [zh] 样式规则 `opacity: 0.7;` */
    opacity: 0.7;
    /* [zh] 样式规则 `cursor: not-allowed;` */
    cursor: not-allowed;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.result-msg {` */
.result-msg {
  /* [zh] 样式规则 `margin: 0 0 8px;` */
  margin: 0 0 8px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.45;` */
  line-height: 1.45;

  /* [zh] 样式规则 `&.success {` */
  &.success {
    /* [zh] 样式规则 `color: #16a34a;` */
    color: #16a34a;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.error {` */
  &.error {
    /* [zh] 样式规则 `color: #dc2626;` */
    color: #dc2626;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.status-label {` */
.status-label {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding-top: 8px;` */
  padding-top: 8px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `border-top: 1px solid rgba($color-text-m` */
  border-top: 1px solid rgba($color-text-muted, 0.15);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.is-confirmed {` */
.is-confirmed {
  /* [zh] 样式规则 `border-color: rgba(#16a34a, 0.35);` */
  border-color: rgba(#16a34a, 0.35);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.is-cancelled,
/* [zh] 样式规则 `.is-expired {` */
.is-expired {
  /* [zh] 样式规则 `border-color: rgba($color-text-muted, 0.` */
  border-color: rgba($color-text-muted, 0.3);
  /* [zh] 样式规则 `opacity: 0.92;` */
  opacity: 0.92;
/* [zh] 样式规则 `}` */
}
</style>
