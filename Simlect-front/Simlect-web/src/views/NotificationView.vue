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

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import SwipeDeleteRow from '@/components/business/SwipeDeleteRow.vue';
import { notificationApi } from '@/api/modules';
import { useDevice } from '@/composables/useDevice';
import { useUnreadCount } from '@/composables/useUnreadCount';
import { navigateNotification, type NotificationData } from '@/utils/notification';
import { confirmAction } from '@/utils/confirm';
import { toast } from '@/utils/toast';
import { formatDisplayDateTime } from '@/utils/formatDateTime';

const router = useRouter();
const { isDesktop } = useDevice();
const useSwipeDelete = computed(() => !isDesktop.value);
const { refreshUnreadCount } = useUnreadCount();
const list = ref<any[]>([]);
const pageNo = ref(1);
const total = ref(0);
const loading = ref(false);
const loadError = ref('');
const openSwipeId = ref<string | null>(null);

const hasMore = computed(() => list.value.length < total.value);

const load = async (append = false) => {
  if (!append) loadError.value = '';
  loading.value = true;
  try {
    const res: any = await notificationApi.loadNotification({ pageNo: pageNo.value });
    const rows = res?.list || [];
    total.value = res?.totalCount ?? rows.length;
    list.value = append ? [...list.value, ...rows] : rows;
  } catch (e: any) {
    if (!append) {
      loadError.value = e?.info || e?.message || '消息加载失败，请稍后重试';
      list.value = [];
    }
  } finally {
    loading.value = false;
  }
};

const retryLoad = () => {
  pageNo.value = 1;
  void load();
};

const loadMore = async () => {
  pageNo.value += 1;
  await load(true);
};

const markAll = async () => {
  await notificationApi.markAllRead();
  list.value = list.value.map((n) => ({ ...n, readStatus: 1 }));
  await refreshUnreadCount();
  toast.success('已全部标为已读');
};

const onSwipeClose = (id: string) => {
  if (openSwipeId.value === id) openSwipeId.value = null;
};

const remove = async (notificationId: string) => {
  const item = list.value.find((n) => n.notificationId === notificationId);
  await notificationApi.deleteNotification(notificationId);
  list.value = list.value.filter((n) => n.notificationId !== notificationId);
  total.value = Math.max(0, total.value - 1);
  if (openSwipeId.value === notificationId) openSwipeId.value = null;
  if (item?.readStatus === 0) await refreshUnreadCount();
  toast.success('已删除');
};

const clearAll = async () => {
  const ok = await confirmAction('确定清空全部消息吗？', {
    title: '清空消息',
    confirmButtonText: '清空'
  });
  if (!ok) return;
  await notificationApi.clearAll();
  list.value = [];
  total.value = 0;
  openSwipeId.value = null;
  await refreshUnreadCount();
  toast.success('已清空');
};

const openItem = async (item: NotificationData) => {
  await navigateNotification(router, item, { refreshUnread: refreshUnreadCount });
};

const formatTime = (t: string | number) => formatDisplayDateTime(t);

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
