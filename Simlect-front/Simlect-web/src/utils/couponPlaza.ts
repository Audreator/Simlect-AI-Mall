import { couponTypeLabel } from '@/utils/coupon';
import { formatDisplayDateTime } from '@/utils/formatDateTime';

export type CouponPlazaPhase = 'disabled' | 'soldOut' | 'ended' | 'upcoming' | 'ongoing';

export function isCouponUnlimitedStock(item: Record<string, any>) {
  return Number(item.totalCount) === 0;
}

export function isCouponSoldOut(item: Record<string, any>) {
  if (item.status === 3) return true;
  if (isCouponUnlimitedStock(item)) return false;
  return Number(item.remainCount) <= 0;
}

export function resolveCouponHasBought(item: Record<string, any>) {
  return !!(item.hasBought ?? item.has_bought);
}

export function getCouponPlazaPhase(item: Record<string, any>): CouponPlazaPhase {
  const now = Date.now();
  const rushStart = item.rushingStartTime ? new Date(item.rushingStartTime).getTime() : 0;
  const rushEnd = item.rushingEndTime ? new Date(item.rushingEndTime).getTime() : 0;

  if (item.status === 0) return 'disabled';
  if (isCouponSoldOut(item)) return 'soldOut';
  if (rushEnd > 0 && now > rushEnd) return 'ended';
  if (rushStart > 0 && now < rushStart) return 'upcoming';
  return 'ongoing';
}

export function canReceiveCoupon(item: Record<string, any>) {
  if (resolveCouponHasBought(item)) return false;
  if (getCouponPlazaPhase(item) !== 'ongoing') return false;
  if (!isCouponUnlimitedStock(item) && Number(item.remainCount) <= 0) return false;
  return true;
}

export function couponReceiveBtnText(item: Record<string, any>) {
  if (resolveCouponHasBought(item)) return '已购买';
  const phase = getCouponPlazaPhase(item);
  if (phase === 'ended') return '已结束';
  if (phase === 'soldOut') return '已抢光';
  if (phase === 'upcoming') return '即将开始';
  if (phase === 'disabled') return '已停用';
  return '立即抢购';
}

export function couponSoldProgress(item: Record<string, any>) {
  if (isCouponUnlimitedStock(item)) return 0;
  const total = Number(item.totalCount) || 0;
  if (!total) return 0;
  const sold = total - (Number(item.remainCount) || 0);
  return Math.min(100, Math.max(0, (sold / total) * 100));
}

export function couponSoldCount(item: Record<string, any>) {
  if (isCouponUnlimitedStock(item)) return 0;
  return (Number(item.totalCount) || 0) - (Number(item.remainCount) || 0);
}

export function couponStockTotalLabel(item: Record<string, any>) {
  return isCouponUnlimitedStock(item) ? '不限' : String(Number(item.totalCount) || 0);
}

export function formatCouponDateTime(time?: string | null) {
  return formatDisplayDateTime(time);
}

export function couponLeftMainDisplay(item: Record<string, any>) {
  const type = Number(item.couponType);
  if (type === 2) {
    const rate = Number(item.discountRate);
    if (!Number.isFinite(rate)) return { prefix: '', value: '折', suffix: '' };
    const display = rate <= 1 ? rate * 10 : rate;
    const text = display % 1 === 0 ? display.toFixed(0) : display.toFixed(1);
    return { prefix: '', value: text, suffix: '折' };
  }
  const amount = Number(item.discountAmount ?? 0);
  return { prefix: '¥', value: amount >= 1 ? amount.toFixed(0) : amount.toFixed(1), suffix: '' };
}

export function couponThresholdText(item: Record<string, any>) {
  const type = Number(item.couponType);
  const threshold = Number(item.thresholdAmount ?? 0);
  if (type === 3 || threshold <= 0) return '无门槛';
  return `满${threshold}可用`;
}

export { couponTypeLabel };
