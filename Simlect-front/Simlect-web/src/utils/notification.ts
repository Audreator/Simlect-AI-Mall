// [zh] 导入 `模块`
import type { Router } from 'vue-router';
// [zh] 导入 `@/api/modules`
import { notificationApi } from '@/api/modules';

// [zh] 导出模块成员
export interface NotificationData {
  // [zh] 语句
  notificationId: string;
  // [zh] 语句
  title: string;
  // [zh] 语句
  content: string;
  // [zh] 语句
  bizType?: string;
  // [zh] 语句
  bizId?: string;
  // [zh] 语句
  createTime?: string;
  // [zh] 语句
  readStatus?: number;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function showNotification(data: NotificationData): void {
  // [zh] 声明变量 `event`
  const event = new CustomEvent<NotificationData>('newNotification', {
    // [zh] TypeScript/JS 语句：`detail: data,`
    detail: data,
    // [zh] TypeScript/JS 语句：`bubbles: true,`
    bubbles: true,
    // [zh] TypeScript/JS 语句：`composed: true`
    composed: true
  // [zh] 语句
  });
  // [zh] 语句
  window.dispatchEvent(event);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function resolveNotificationRoute(item: Pick<NotificationData, 'title' | 'bizType' | 'bizId'>): string | null {
  // [zh] 声明变量 `bizType`
  const bizType = item.bizType || '';
  // [zh] 声明变量 `bizId`
  const bizId = item.bizId || '';
  // [zh] 声明变量 `title`
  const title = item.title || '';

  // [zh] 条件判断 if
  if (bizType === 'member_level') return '/member-center';
  // [zh] 条件判断 if
  if (bizType === 'sign' || bizType === 'sign_reward') return '/sign';
  // [zh] 条件判断 if
  if (bizType === 'logistics' && bizId) return `/order/${bizId}/logistics`;
  // [zh] 条件判断 if
  if (bizType === 'comment_re' && bizId) return `/order/${bizId}`;
  // [zh] 条件判断 if
  if (bizType === 'order' && bizId) {
    // [zh] 条件判断 if
    if (title.includes('追评')) return `/order/${bizId}`;
    // [zh] 条件判断 if
    if (title.includes('支付') || title.includes('订单列表')) return '/orders';
    // [zh] 条件判断 if
    if (bizId.includes(',')) return '/orders';
    // [zh] return 返回
    return `/order/${bizId}`;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (bizType === 'rush_coupon' || bizType === 'coupon_rush') return '/coupons';
  // [zh] 条件判断 if
  if (bizType === 'coupon' || bizType === 'coupon_expire') return '/my-coupons';
  // [zh] return 返回
  return null;
// [zh] 语法符号
}

// [zh] 导出模块成员
export async function navigateNotification(
  // [zh] TypeScript/JS 语句：`router: Router,`
  router: Router,
  // [zh] TypeScript/JS 语句：`item: NotificationData,`
  item: NotificationData,
  // [zh] TypeScript/JS 语句：`options?: { refreshUnread?: () => Promise<unknown> }`
  options?: { refreshUnread?: () => Promise<unknown> }
// [zh] 代码块开始
): Promise<void> {
  // [zh] 条件判断 if
  if (item.notificationId && item.readStatus === 0) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await notificationApi.markRead(item.notificationId);
    // [zh] 语句
    item.readStatus = 1;
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await options?.refreshUnread?.();
  // [zh] 语法符号
  }

  // [zh] 声明变量 `target`
  const target = resolveNotificationRoute(item);
  // [zh] 条件判断 if
  if (target) {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await router.push(target);
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await router.push('/notifications');
// [zh] 语法符号
}
