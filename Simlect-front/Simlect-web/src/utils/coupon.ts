// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`COUPON_TYPE,`
  COUPON_TYPE,
  // [zh] TypeScript/JS 语句：`USER_COUPON_STATUS,`
  USER_COUPON_STATUS,
  // [zh] TypeScript/JS 语句：`couponTypeLabel as couponTypeLabelFromEnum`
  couponTypeLabel as couponTypeLabelFromEnum
// [zh] 语句
} from '@/constants/backendEnums';

// [zh] 导出模块成员
export const COUPON_TYPE_MAP = COUPON_TYPE;
// [zh] 导出模块成员
export const USER_COUPON_STATUS_MAP = USER_COUPON_STATUS;
// [zh] 导出模块成员
export const couponTypeLabel = couponTypeLabelFromEnum;


// [zh] 导出模块成员
export const couponMainValue = (coupon: Record<string, any>) => {
  // [zh] 声明变量 `type`
  const type = Number(coupon.couponType);
  // [zh] 条件判断 if
  if (type === 2) {
    // [zh] 声明变量 `rate`
    const rate = Number(coupon.discountRate);
    // [zh] 条件判断 if
    if (!Number.isFinite(rate)) return '折';
    // [zh] 声明变量 `display`
    const display = rate <= 1 ? rate * 10 : rate;
    // [zh] return 返回
    return `${display % 1 === 0 ? display.toFixed(0) : display.toFixed(1)}折`;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `amount`
  const amount = Number(coupon.discountAmount ?? 0);
  // [zh] return 返回
  return `¥${amount.toFixed(0)}`;
// [zh] 语句
};


// [zh] 导出模块成员
export const couponConditionText = (coupon: Record<string, any>) => {
  // [zh] 声明变量 `type`
  const type = Number(coupon.couponType);
  // [zh] 声明变量 `threshold`
  const threshold = Number(coupon.thresholdAmount ?? 0);
  // [zh] 条件判断 if
  if (type === 3) return '无门槛可用';
  // [zh] 条件判断 if
  if (type === 2) {
    // [zh] return 返回
    return threshold > 0 ? `满${threshold}元可用` : '折扣券';
  // [zh] 语法符号
  }
  // [zh] return 返回
  return threshold > 0 ? `满${threshold}元减${Number(coupon.discountAmount ?? 0)}` : `减${Number(coupon.discountAmount ?? 0)}元`;
// [zh] 语句
};

// [zh] 导出模块成员
export const formatCouponTime = (time?: string | null) => {
  // [zh] 条件判断 if
  if (!time) return '--';
  // [zh] return 返回
  return String(time).slice(0, 16);
// [zh] 语句
};
