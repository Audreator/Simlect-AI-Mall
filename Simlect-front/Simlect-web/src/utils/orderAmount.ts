// [zh] 导入 `@/utils/coupon`
import { couponTypeLabel } from '@/utils/coupon';

// [zh] 导出模块成员
export const formatOrderMoney = (val: unknown) => Number(val ?? 0).toFixed(2);

// [zh] 导出模块成员
export const hasOrderCouponDiscount = (order?: Record<string, any> | null) =>
  // [zh] 语句
  Number(order?.couponDiscountAmount ?? 0) > 0;

// [zh] 导出模块成员
export const orderCouponTypeText = (order?: Record<string, any> | null) =>
  // [zh] 语句
  order?.couponType != null ? couponTypeLabel(order.couponType) : '';

// [zh] 导出模块成员
export const orderCouponSummaryText = (order?: Record<string, any> | null) => {
  // [zh] 条件判断 if
  if (!hasOrderCouponDiscount(order)) return '';
  // [zh] 声明变量 `name`
  const name = order?.couponName ? String(order.couponName) : '优惠券';
  // [zh] 声明变量 `type`
  const type = orderCouponTypeText(order);
  // [zh] return 返回
  return type ? `${name}（${type}）` : name;
// [zh] 语句
};
