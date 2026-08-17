import { couponTypeLabel } from '@/utils/coupon';

export const formatOrderMoney = (val: unknown) => Number(val ?? 0).toFixed(2);

export const hasOrderCouponDiscount = (order?: Record<string, any> | null) =>
  Number(order?.couponDiscountAmount ?? 0) > 0;

export const orderCouponTypeText = (order?: Record<string, any> | null) =>
  order?.couponType != null ? couponTypeLabel(order.couponType) : '';

export const orderCouponSummaryText = (order?: Record<string, any> | null) => {
  if (!hasOrderCouponDiscount(order)) return '';
  const name = order?.couponName ? String(order.couponName) : '优惠券';
  const type = orderCouponTypeText(order);
  return type ? `${name}（${type}）` : name;
};
