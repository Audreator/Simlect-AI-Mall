// [zh] 导入 `@/utils/coupon`
import { couponTypeLabel } from '@/utils/coupon';
// [zh] 导入 `@/utils/formatDateTime`
import { formatDisplayDateTime } from '@/utils/formatDateTime';

// [zh] 导出模块成员
export type CouponPlazaPhase = 'disabled' | 'soldOut' | 'ended' | 'upcoming' | 'ongoing';

// [zh] 导出模块成员
export function isCouponUnlimitedStock(item: Record<string, any>) {
  // [zh] return 返回
  return Number(item.totalCount) === 0;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function isCouponSoldOut(item: Record<string, any>) {
  // [zh] 条件判断 if
  if (item.status === 3) return true;
  // [zh] 条件判断 if
  if (isCouponUnlimitedStock(item)) return false;
  // [zh] return 返回
  return Number(item.remainCount) <= 0;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function resolveCouponHasBought(item: Record<string, any>) {
  // [zh] return 返回
  return !!(item.hasBought ?? item.has_bought);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function getCouponPlazaPhase(item: Record<string, any>): CouponPlazaPhase {
  // [zh] 声明变量 `now`
  const now = Date.now();
  // [zh] 声明变量 `rushStart`
  const rushStart = item.rushingStartTime ? new Date(item.rushingStartTime).getTime() : 0;
  // [zh] 声明变量 `rushEnd`
  const rushEnd = item.rushingEndTime ? new Date(item.rushingEndTime).getTime() : 0;

  // [zh] 条件判断 if
  if (item.status === 0) return 'disabled';
  // [zh] 条件判断 if
  if (isCouponSoldOut(item)) return 'soldOut';
  // [zh] 条件判断 if
  if (rushEnd > 0 && now > rushEnd) return 'ended';
  // [zh] 条件判断 if
  if (rushStart > 0 && now < rushStart) return 'upcoming';
  // [zh] return 返回
  return 'ongoing';
// [zh] 语法符号
}

// [zh] 导出模块成员
export function canReceiveCoupon(item: Record<string, any>) {
  // [zh] 条件判断 if
  if (resolveCouponHasBought(item)) return false;
  // [zh] 条件判断 if
  if (getCouponPlazaPhase(item) !== 'ongoing') return false;
  // [zh] 条件判断 if
  if (!isCouponUnlimitedStock(item) && Number(item.remainCount) <= 0) return false;
  // [zh] return 返回
  return true;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function couponReceiveBtnText(item: Record<string, any>) {
  // [zh] 条件判断 if
  if (resolveCouponHasBought(item)) return '已购买';
  // [zh] 声明变量 `phase`
  const phase = getCouponPlazaPhase(item);
  // [zh] 条件判断 if
  if (phase === 'ended') return '已结束';
  // [zh] 条件判断 if
  if (phase === 'soldOut') return '已抢光';
  // [zh] 条件判断 if
  if (phase === 'upcoming') return '即将开始';
  // [zh] 条件判断 if
  if (phase === 'disabled') return '已停用';
  // [zh] return 返回
  return '立即抢购';
// [zh] 语法符号
}

// [zh] 导出模块成员
export function couponSoldProgress(item: Record<string, any>) {
  // [zh] 条件判断 if
  if (isCouponUnlimitedStock(item)) return 0;
  // [zh] 声明变量 `total`
  const total = Number(item.totalCount) || 0;
  // [zh] 条件判断 if
  if (!total) return 0;
  // [zh] 声明变量 `sold`
  const sold = total - (Number(item.remainCount) || 0);
  // [zh] return 返回
  return Math.min(100, Math.max(0, (sold / total) * 100));
// [zh] 语法符号
}

// [zh] 导出模块成员
export function couponSoldCount(item: Record<string, any>) {
  // [zh] 条件判断 if
  if (isCouponUnlimitedStock(item)) return 0;
  // [zh] return 返回
  return (Number(item.totalCount) || 0) - (Number(item.remainCount) || 0);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function couponStockTotalLabel(item: Record<string, any>) {
  // [zh] return 返回
  return isCouponUnlimitedStock(item) ? '不限' : String(Number(item.totalCount) || 0);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function formatCouponDateTime(time?: string | null) {
  // [zh] return 返回
  return formatDisplayDateTime(time);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function couponLeftMainDisplay(item: Record<string, any>) {
  // [zh] 声明变量 `type`
  const type = Number(item.couponType);
  // [zh] 条件判断 if
  if (type === 2) {
    // [zh] 声明变量 `rate`
    const rate = Number(item.discountRate);
    // [zh] 条件判断 if
    if (!Number.isFinite(rate)) return { prefix: '', value: '折', suffix: '' };
    // [zh] 声明变量 `display`
    const display = rate <= 1 ? rate * 10 : rate;
    // [zh] 声明变量 `text`
    const text = display % 1 === 0 ? display.toFixed(0) : display.toFixed(1);
    // [zh] return 返回
    return { prefix: '', value: text, suffix: '折' };
  // [zh] 语法符号
  }
  // [zh] 声明变量 `amount`
  const amount = Number(item.discountAmount ?? 0);
  // [zh] return 返回
  return { prefix: '¥', value: amount >= 1 ? amount.toFixed(0) : amount.toFixed(1), suffix: '' };
// [zh] 语法符号
}

// [zh] 导出模块成员
export function couponThresholdText(item: Record<string, any>) {
  // [zh] 声明变量 `type`
  const type = Number(item.couponType);
  // [zh] 声明变量 `threshold`
  const threshold = Number(item.thresholdAmount ?? 0);
  // [zh] 条件判断 if
  if (type === 3 || threshold <= 0) return '无门槛';
  // [zh] return 返回
  return `满${threshold}可用`;
// [zh] 语法符号
}

// [zh] 导出模块成员
export { couponTypeLabel };
