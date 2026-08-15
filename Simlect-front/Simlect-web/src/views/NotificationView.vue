<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="notification-page">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="page-toolbar">
      <!-- [zh] 开始标签 `<h2>` -->
      <h2 class="page-title">消息中心</h2>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="list.length" class="toolbar-actions">
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="link-btn" @click="markAll">全部已读</button>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="link-btn danger" @click="clearAll">清空</button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
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
    <ul v-else-if="list.length" class="notify-list">
      <!-- [zh] 开始标签 `<li>` -->
      <li v-for="item in list" :key="item.notificationId" class="notify-row">
        <!-- [zh] 开始标签 `<SwipeDeleteRow>` -->
        <SwipeDeleteRow
          v-if="useSwipeDelete"
          :open="openSwipeId === item.notificationId"
          @open="openSwipeId = item.notificationId"
          @close="onSwipeClose(item.notificationId)"
          @delete="remove(item.notificationId)"
        >
          <!-- [zh] 开始标签 `<article>` -->
          <article
            class="notify-item"
            :class="{ unread: item.readStatus === 0 }"
            @click="openItem(item)"
          >
            <!-- [zh] 开始标签 `<div>` -->
            <div class="notify-head">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="notify-title">{{ item.title }}</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span class="notify-time">{{ formatTime(item.createTime) }}</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<p>` -->
            <p class="notify-content">{{ item.content }}</p>
          <!-- [zh] 闭合标签 `</article>` -->
          </article>
        <!-- [zh] 闭合标签 `</SwipeDeleteRow>` -->
        </SwipeDeleteRow>

        <!-- [zh] 开始标签 `<article>` -->
        <article
          v-else
          class="notify-item notify-item--desktop"
          :class="{ unread: item.readStatus === 0 }"
        >
          <!-- [zh] 开始标签 `<div>` -->
          <div class="notify-main" @click="openItem(item)">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="notify-head">
              <!-- [zh] 开始标签 `<span>` -->
              <span class="notify-title">{{ item.title }}</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span class="notify-time">{{ formatTime(item.createTime) }}</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<p>` -->
            <p class="notify-content">{{ item.content }}</p>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button
            link
            type="danger"
            class="btn-delete"
            @click.stop="remove(item.notificationId)"
          >
            <!-- [zh] 模板内容：`删除` -->
            删除
          <!-- [zh] 闭合标签 `</el-button>` -->
          </el-button>
        <!-- [zh] 闭合标签 `</article>` -->
        </article>
      <!-- [zh] 闭合标签 `</li>` -->
      </li>
    <!-- [zh] 闭合标签 `</ul>` -->
    </ul>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else class="empty-tip">暂无消息</p>
    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="hasMore" class="load-more">
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button text @click="loadMore">加载更多</el-button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@/components/business/SwipeDeleteRow.vue`
import SwipeDeleteRow from '@/components/business/SwipeDeleteRow.vue';
// [zh] 导入 `@/api/modules`
import { notificationApi } from '@/api/modules';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/composables/useUnreadCount`
import { useUnreadCount } from '@/composables/useUnreadCount';
// [zh] 导入 `@/utils/notification`
import { navigateNotification, type NotificationData } from '@/utils/notification';
// [zh] 导入 `@/utils/confirm`
import { confirmAction } from '@/utils/confirm';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/utils/formatDateTime`
import { formatDisplayDateTime } from '@/utils/formatDateTime';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 语句
const { isDesktop } = useDevice();
// [zh] 声明变量 `useSwipeDelete`
const useSwipeDelete = computed(() => !isDesktop.value);
// [zh] 语句
const { refreshUnreadCount } = useUnreadCount();
// [zh] 声明变量 `list`
const list = ref<any[]>([]);
// [zh] 声明变量 `pageNo`
const pageNo = ref(1);
// [zh] 声明变量 `total`
const total = ref(0);
// [zh] 声明变量 `loading`
const loading = ref(false);
// [zh] 声明变量 `loadError`
const loadError = ref('');
// [zh] 声明变量 `openSwipeId`
const openSwipeId = ref<string | null>(null);

// [zh] 声明变量 `hasMore`
const hasMore = computed(() => list.value.length < total.value);

// [zh] 声明变量 `load`
const load = async (append = false) => {
  // [zh] 条件判断 if
  if (!append) loadError.value = '';
  // [zh] 语句
  loading.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `res`
    const res: any = await notificationApi.loadNotification({ pageNo: pageNo.value });
    // [zh] 声明变量 `rows`
    const rows = res?.list || [];
    // [zh] 语句
    total.value = res?.totalCount ?? rows.length;
    // [zh] 语句
    list.value = append ? [...list.value, ...rows] : rows;
  // [zh] 代码块开始
  } catch (e: any) {
    // [zh] 条件判断 if
    if (!append) {
      // [zh] 语句
      loadError.value = e?.info || e?.message || '消息加载失败，请稍后重试';
      // [zh] 语句
      list.value = [];
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    loading.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `retryLoad`
const retryLoad = () => {
  // [zh] 语句
  pageNo.value = 1;
  // [zh] 语句
  void load();
// [zh] 语句
};

// [zh] 声明变量 `loadMore`
const loadMore = async () => {
  // [zh] 语句
  pageNo.value += 1;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await load(true);
// [zh] 语句
};

// [zh] 声明变量 `markAll`
const markAll = async () => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await notificationApi.markAllRead();
  // [zh] 语句
  list.value = list.value.map((n) => ({ ...n, readStatus: 1 }));
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await refreshUnreadCount();
  // [zh] 语句
  toast.success('已全部标为已读');
// [zh] 语句
};

// [zh] 声明变量 `onSwipeClose`
const onSwipeClose = (id: string) => {
  // [zh] 条件判断 if
  if (openSwipeId.value === id) openSwipeId.value = null;
// [zh] 语句
};

// [zh] 声明变量 `remove`
const remove = async (notificationId: string) => {
  // [zh] 声明变量 `item`
  const item = list.value.find((n) => n.notificationId === notificationId);
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await notificationApi.deleteNotification(notificationId);
  // [zh] 语句
  list.value = list.value.filter((n) => n.notificationId !== notificationId);
  // [zh] 语句
  total.value = Math.max(0, total.value - 1);
  // [zh] 条件判断 if
  if (openSwipeId.value === notificationId) openSwipeId.value = null;
  // [zh] 条件判断 if
  if (item?.readStatus === 0) await refreshUnreadCount();
  // [zh] 语句
  toast.success('已删除');
// [zh] 语句
};

// [zh] 声明变量 `clearAll`
const clearAll = async () => {
  // [zh] 声明变量 `ok`
  const ok = await confirmAction('确定清空全部消息吗？', {
    // [zh] TypeScript/JS 语句：`title: '清空消息',`
    title: '清空消息',
    // [zh] TypeScript/JS 语句：`confirmButtonText: '清空'`
    confirmButtonText: '清空'
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (!ok) return;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await notificationApi.clearAll();
  // [zh] 语句
  list.value = [];
  // [zh] 语句
  total.value = 0;
  // [zh] 语句
  openSwipeId.value = null;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await refreshUnreadCount();
  // [zh] 语句
  toast.success('已清空');
// [zh] 语句
};

// [zh] 声明变量 `openItem`
const openItem = async (item: NotificationData) => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await navigateNotification(router, item, { refreshUnread: refreshUnreadCount });
// [zh] 语句
};

// [zh] 声明变量 `formatTime`
const formatTime = (t: string | number) => formatDisplayDateTime(t);

// [zh] 语句
onMounted(() => load());
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.notification-page {` */
.notification-page {
  /* [zh] 样式规则 `padding: 12px 16px 24px;` */
  padding: 12px 16px 24px;
  /* [zh] 样式规则 `max-width: 720px;` */
  max-width: 720px;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.page-toolbar {` */
.page-toolbar {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.page-title {` */
.page-title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 18px;` */
  font-size: 18px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.toolbar-actions {` */
.toolbar-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.link-btn {` */
.link-btn {
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: none;` */
  background: none;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;

  /* [zh] 样式规则 `&.danger {` */
  &.danger {
    /* [zh] 样式规则 `color: $color-error;` */
    color: $color-error;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notify-list {` */
.notify-list {
  /* [zh] 样式规则 `list-style: none;` */
  list-style: none;
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notify-row {` */
.notify-row {
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notify-item {` */
.notify-item {
  /* [zh] 样式规则 `padding: 14px 16px;` */
  padding: 14px 16px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;

  /* [zh] 样式规则 `&.unread {` */
  &.unread {
    /* [zh] 样式规则 `background: #ffffff;` */
    background: #ffffff;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--desktop {` */
  &--desktop {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: flex-start;` */
    align-items: flex-start;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
    /* [zh] 样式规则 `cursor: default;` */
    cursor: default;

    /* [zh] 样式规则 `.notify-main {` */
    .notify-main {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `min-width: 0;` */
      min-width: 0;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.btn-delete {` */
    .btn-delete {
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
      /* [zh] 样式规则 `margin-top: 2px;` */
      margin-top: 2px;
      /* [zh] 样式规则 `padding: 0 4px;` */
      padding: 0 4px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notify-head {` */
.notify-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notify-title {` */
.notify-title {
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notify-time {` */
.notify-time {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-secondary;` */
  color: $color-text-secondary;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notify-content {` */
.notify-content {
  /* [zh] 样式规则 `margin: 6px 0 0;` */
  margin: 6px 0 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
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

/* [zh] 样式规则 `.load-more {` */
.load-more {
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `margin-top: 12px;` */
  margin-top: 12px;
/* [zh] 样式规则 `}` */
}
</style>
