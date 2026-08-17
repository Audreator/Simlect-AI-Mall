
export interface CheckoutLineItem {
  cartId?: string;
  productId: string;
  productName: string;
  productCover?: string;
  propertyValueIds: string;
  propertyValueIdHash?: string;
  propertyData?: { propertyName: string; propertyValue: string }[];
  price: number;
  buyCount: number;
  remark?: string;
}

export interface CouponRushCheckoutMeta {
  orderId: string;
  payOrderId?: string;
  payExpireAt: number;
}

const ITEMS_KEY = 'eshop_checkout_items';
const FROM_KEY = 'eshop_checkout_order_from';
const ADDRESS_KEY = 'eshop_checkout_address_id';
const COUPON_ORDER_KEY = 'eshop_checkout_coupon_order_id';
const COUPON_PAY_EXPIRE_KEY = 'eshop_checkout_coupon_pay_expire';
const COUPON_PAY_ORDER_KEY = 'eshop_checkout_coupon_pay_order_id';

export type CheckoutOrderFrom = 0 | 1 | 2;

export const RUSHING_COUPON_PAY_AMOUNT = 0.01;

export const MIN_ORDER_PAY_AMOUNT = 0.01;

export function capCouponDiscountForMinPay(goodsAmount: number, rawDiscount: number) {
  if (!Number.isFinite(goodsAmount) || goodsAmount <= MIN_ORDER_PAY_AMOUNT) return 0;
  const maxOff = goodsAmount - MIN_ORDER_PAY_AMOUNT;
  const off = Number.isFinite(rawDiscount) ? rawDiscount : 0;
  return Math.max(0, Math.min(off, maxOff));
}

export function calcPayableAfterCoupon(goodsAmount: number, couponDiscount: number) {
  if (!Number.isFinite(goodsAmount) || goodsAmount <= 0) return 0;
  const off = Number.isFinite(couponDiscount) ? couponDiscount : 0;
  const pay = Math.round((goodsAmount - off) * 100) / 100;
  if (pay <= 0) return MIN_ORDER_PAY_AMOUNT;
  if (pay < MIN_ORDER_PAY_AMOUNT) return MIN_ORDER_PAY_AMOUNT;
  return pay;
}

export function normalizeDisplayPayAmount(amount: unknown): number {
  const n = Number(amount);
  if (!Number.isFinite(n) || n <= 0) return MIN_ORDER_PAY_AMOUNT;
  if (n < MIN_ORDER_PAY_AMOUNT) return MIN_ORDER_PAY_AMOUNT;
  return Math.round(n * 100) / 100;
}

export const PAY_ORDER_TIMEOUT_MS = 60_000;

export function saveCheckoutSession(
  items: CheckoutLineItem[],
  orderFrom: CheckoutOrderFrom,
  couponRush?: CouponRushCheckoutMeta
) {
  sessionStorage.setItem(ITEMS_KEY, JSON.stringify(items));
  sessionStorage.setItem(FROM_KEY, String(orderFrom));
  if (couponRush?.orderId) {
    sessionStorage.setItem(COUPON_ORDER_KEY, couponRush.orderId);
    sessionStorage.setItem(COUPON_PAY_EXPIRE_KEY, String(couponRush.payExpireAt));
    if (couponRush.payOrderId) {
      sessionStorage.setItem(COUPON_PAY_ORDER_KEY, couponRush.payOrderId);
    }
  } else {
    sessionStorage.removeItem(COUPON_ORDER_KEY);
    sessionStorage.removeItem(COUPON_PAY_EXPIRE_KEY);
    sessionStorage.removeItem(COUPON_PAY_ORDER_KEY);
  }
}

export function loadCheckoutSession(): {
  items: CheckoutLineItem[];
  orderFrom: CheckoutOrderFrom;
  couponRush?: CouponRushCheckoutMeta;
} | null {
  const raw = sessionStorage.getItem(ITEMS_KEY);
  if (!raw) return null;
  try {
    const items = JSON.parse(raw) as CheckoutLineItem[];
    if (!Array.isArray(items) || !items.length) return null;
    const fromRaw = sessionStorage.getItem(FROM_KEY);
    const orderFrom: CheckoutOrderFrom =
      fromRaw === '0' ? 0 : fromRaw === '2' ? 2 : 1;
    const orderId = sessionStorage.getItem(COUPON_ORDER_KEY);
    const payExpireRaw = sessionStorage.getItem(COUPON_PAY_EXPIRE_KEY);
    const payExpireAt = payExpireRaw ? Number(payExpireRaw) : 0;
    const couponRush =
      orderFrom === 2 && orderId && payExpireAt > 0
        ? {
            orderId,
            payExpireAt,
            payOrderId: sessionStorage.getItem(COUPON_PAY_ORDER_KEY) || undefined
          }
        : undefined;
    return { items, orderFrom, couponRush };
  } catch {
    return null;
  }
}

export function saveCheckoutSelectedAddress(addressId: string) {
  sessionStorage.setItem(ADDRESS_KEY, addressId);
}

export function loadCheckoutSelectedAddress(): string | null {
  return sessionStorage.getItem(ADDRESS_KEY);
}

export function clearCheckoutSession() {
  sessionStorage.removeItem(ITEMS_KEY);
  sessionStorage.removeItem(FROM_KEY);
  sessionStorage.removeItem(ADDRESS_KEY);
  sessionStorage.removeItem(COUPON_ORDER_KEY);
  sessionStorage.removeItem(COUPON_PAY_EXPIRE_KEY);
  sessionStorage.removeItem(COUPON_PAY_ORDER_KEY);
  sessionStorage.removeItem('eshop_checkout_cart_ids');
}

export function formatSkuText(item: CheckoutLineItem) {
  if (!item.propertyData?.length) return '';
  return item.propertyData.map((p) => `${p.propertyName}：${p.propertyValue}`).join(' · ');
}

export function lineSubtotal(item: CheckoutLineItem) {
  return Number(item.price) * (Number(item.buyCount) || 0);
}

export function formatPayCountdown(remainMs: number) {
  const sec = Math.max(0, Math.ceil(remainMs / 1000));
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}
