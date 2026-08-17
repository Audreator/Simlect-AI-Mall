import type { Router } from 'vue-router';
import { notificationApi } from '@/api/modules';

export interface NotificationData {
  notificationId: string;
  title: string;
  content: string;
  bizType?: string;
  bizId?: string;
  createTime?: string;
  readStatus?: number;
}

export function showNotification(data: NotificationData): void {
  const event = new CustomEvent<NotificationData>('newNotification', {
    detail: data,
    bubbles: true,
    composed: true
  });
  window.dispatchEvent(event);
}

export function resolveNotificationRoute(item: Pick<NotificationData, 'title' | 'bizType' | 'bizId'>): string | null {
  const bizType = item.bizType || '';
  const bizId = item.bizId || '';
  const title = item.title || '';

  if (bizType === 'member_level') return '/member-center';
  if (bizType === 'sign' || bizType === 'sign_reward') return '/sign';
  if (bizType === 'logistics' && bizId) return `/order/${bizId}/logistics`;
  if (bizType === 'comment_re' && bizId) return `/order/${bizId}`;
  if (bizType === 'order' && bizId) {
    if (title.includes('追评')) return `/order/${bizId}`;
    if (title.includes('支付') || title.includes('订单列表')) return '/orders';
    if (bizId.includes(',')) return '/orders';
    return `/order/${bizId}`;
  }
  if (bizType === 'rush_coupon' || bizType === 'coupon_rush') return '/coupons';
  if (bizType === 'coupon' || bizType === 'coupon_expire') return '/my-coupons';
  return null;
}

export async function navigateNotification(
  router: Router,
  item: NotificationData,
  options?: { refreshUnread?: () => Promise<unknown> }
): Promise<void> {
  if (item.notificationId && item.readStatus === 0) {
    await notificationApi.markRead(item.notificationId);
    item.readStatus = 1;
    await options?.refreshUnread?.();
  }

  const target = resolveNotificationRoute(item);
  if (target) {
    await router.push(target);
    return;
  }
  await router.push('/notifications');
}
