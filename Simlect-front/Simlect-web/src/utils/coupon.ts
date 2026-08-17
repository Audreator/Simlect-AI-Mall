import {
  COUPON_TYPE,
  USER_COUPON_STATUS,
  couponTypeLabel as couponTypeLabelFromEnum
} from '@/constants/backendEnums';

export const COUPON_TYPE_MAP = COUPON_TYPE;
export const USER_COUPON_STATUS_MAP = USER_COUPON_STATUS;
export const couponTypeLabel = couponTypeLabelFromEnum;


export const couponMainValue = (coupon: Record<string, any>) => {
  const type = Number(coupon.couponType);
  if (type === 2) {
    const rate = Number(coupon.discountRate);
    if (!Number.isFinite(rate)) return '折';
    const display = rate <= 1 ? rate * 10 : rate;
    return `${display % 1 === 0 ? display.toFixed(0) : display.toFixed(1)}折`;
  }
  const amount = Number(coupon.discountAmount ?? 0);
  return `¥${amount.toFixed(0)}`;
};


export const couponConditionText = (coupon: Record<string, any>) => {
  const type = Number(coupon.couponType);
  const threshold = Number(coupon.thresholdAmount ?? 0);
  if (type === 3) return '无门槛可用';
  if (type === 2) {
    return threshold > 0 ? `满${threshold}元可用` : '折扣券';
  }
  return threshold > 0 ? `满${threshold}元减${Number(coupon.discountAmount ?? 0)}` : `减${Number(coupon.discountAmount ?? 0)}元`;
};

export const formatCouponTime = (time?: string | null) => {
  if (!time) return '--';
  return String(time).slice(0, 16);
};
