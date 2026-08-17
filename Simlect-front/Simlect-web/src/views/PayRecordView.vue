<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pay-record-page">
    <!-- [zh] 开始标签 `<h2>` -->
    <h2 class="page-title">支付记录</h2>
    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="loading && !list.length" class="loading-tip">加载中…</div>
    <!-- [zh] 开始标签 `<div>` -->
    <div v-else-if="loadError && !list.length" class="load-error">
      <!-- [zh] 开始标签 `<p>` -->
      <p>{{ loadError }}</p>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" size="small" @click="retryLoad">重试</el-button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<ul>` -->
    <ul v-else-if="list.length" class="record-list">
      <!-- [zh] 开始标签 `<li>` -->
      <li v-for="item in list" :key="item.tradeId" class="record-item card-flat">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="row">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">支付单号</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="value">{{ item.payOrderId }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="row">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">金额</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="amount">¥{{ item.payAmount }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="row">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">状态</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span :class="['status', statusClass(item.tradeStatus)]">{{ statusText(item.tradeStatus) }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="row">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">时间</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="value">{{ formatTime(item.payTime || item.createTime) }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</li>` -->
      </li>
    <!-- [zh] 闭合标签 `</ul>` -->
    </ul>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else class="empty-tip">暂无支付记录</p>
    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="hasMore" class="load-more">
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button text :loading="loadingMore" @click="loadMore">加载更多</el-button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { payTradeApi } from '@/api/modules';

const list = ref<any[]>([]);
const loading = ref(false);
const loadingMore = ref(false);
const loadError = ref('');
const pageNo = ref(1);
const totalCount = ref(0);

const hasMore = computed(() => list.value.length < totalCount.value);

const statusText = (s: number) => {
  const map: Record<number, string> = { 0: '待支付', 1: '支付成功', 2: '已关闭', 3: '已退款' };
  return map[s] ?? '未知';
};

const statusClass = (s: number) => {
  if (s === 1) return 'ok';
  if (s === 0) return 'pending';
  return 'closed';
};

const formatTime = (t: string | number) => {
  if (!t) return '-';
  const d = new Date(t);
  return d.toLocaleString('zh-CN');
};

const load = async (append = false) => {
  if (!append) loadError.value = '';
  if (append) {
    loadingMore.value = true;
  } else {
    loading.value = true;
  }
  try {
    const res: any = await payTradeApi.loadMyTrades(pageNo.value);
    const rows = res?.list || [];
    totalCount.value = res?.totalCount ?? rows.length;
    list.value = append ? [...list.value, ...rows] : rows;
  } catch (e: any) {
    if (!append) {
      loadError.value = e?.info || e?.message || '支付记录加载失败，请稍后重试';
      list.value = [];
    }
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
};

const retryLoad = () => {
  pageNo.value = 1;
  void load();
};

const loadMore = async () => {
  if (!hasMore.value || loadingMore.value) return;
  pageNo.value += 1;
  await load(true);
};

onMounted(() => load());
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pay-record-page {` */
.pay-record-page {
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;
  /* [zh] 样式规则 `max-width: 720px;` */
  max-width: 720px;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.page-title {` */
.page-title {
  /* [zh] 样式规则 `margin: 0 0 16px;` */
  margin: 0 0 16px;
  /* [zh] 样式规则 `font-size: 18px;` */
  font-size: 18px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.record-list {` */
.record-list {
  /* [zh] 样式规则 `list-style: none;` */
  list-style: none;
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.record-item {` */
.record-item {
  /* [zh] 样式规则 `padding: 14px;` */
  padding: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.row {` */
.row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `margin-bottom: 6px;` */
  margin-bottom: 6px;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `margin-bottom: 0;` */
    margin-bottom: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.label {` */
.label {
  /* [zh] 样式规则 `color: $color-text-secondary;` */
  color: $color-text-secondary;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.amount {` */
.amount {
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.status.ok {` */
.status.ok {
  /* [zh] 样式规则 `color: #16a34a;` */
  color: #16a34a;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.status.pending {` */
.status.pending {
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.status.closed {` */
.status.closed {
  /* [zh] 样式规则 `color: $color-text-secondary;` */
  color: $color-text-secondary;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.empty-tip,
/* [zh] 样式声明 */
.loading-tip,
/* [zh] 样式规则 `.load-error {` */
.load-error {
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `color: $color-text-secondary;` */
  color: $color-text-secondary;
  /* [zh] 样式规则 `padding: 40px 0;` */
  padding: 40px 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.load-error p {` */
.load-error p {
  /* [zh] 样式规则 `margin: 0 0 12px;` */
  margin: 0 0 12px;
/* [zh] 样式规则 `}` */
}
</style>
