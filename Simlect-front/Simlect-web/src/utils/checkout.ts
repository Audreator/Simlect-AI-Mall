
// [zh] 导出模块成员
export interface CheckoutLineItem {
  // [zh] 语句
  cartId?: string;
  // [zh] 语句
  productId: string;
  // [zh] 语句
  productName: string;
  // [zh] 语句
  productCover?: string;
  // [zh] 语句
  propertyValueIds: string;
  // [zh] 语句
  propertyValueIdHash?: string;
  // [zh] 语句
  propertyData?: { propertyName: string; propertyValue: string }[];
  // [zh] 语句
  price: number;
  // [zh] 语句
  buyCount: number;
  // [zh] 语句
  remark?: string;
// [zh] 语法符号
}

// [zh] 导出模块成员
export interface CouponRushCheckoutMeta {
  // [zh] 语句
  orderId: string;
  // [zh] 语句
  payOrderId?: string;
  // [zh] 语句
  payExpireAt: number;
// [zh] 语法符号
}

// [zh] 声明变量 `ITEMS_KEY`
const ITEMS_KEY = 'eshop_checkout_items';
// [zh] 声明变量 `FROM_KEY`
const FROM_KEY = 'eshop_checkout_order_from';
// [zh] 声明变量 `ADDRESS_KEY`
const ADDRESS_KEY = 'eshop_checkout_address_id';
// [zh] 声明变量 `COUPON_ORDER_KEY`
const COUPON_ORDER_KEY = 'eshop_checkout_coupon_order_id';
// [zh] 声明变量 `COUPON_PAY_EXPIRE_KEY`
const COUPON_PAY_EXPIRE_KEY = 'eshop_checkout_coupon_pay_expire';
// [zh] 声明变量 `COUPON_PAY_ORDER_KEY`
const COUPON_PAY_ORDER_KEY = 'eshop_checkout_coupon_pay_order_id';

// [zh] 导出模块成员
export type CheckoutOrderFrom = 0 | 1 | 2;

// [zh] 导出模块成员
export const RUSHING_COUPON_PAY_AMOUNT = 0.01;

// [zh] 导出模块成员
export const MIN_ORDER_PAY_AMOUNT = 0.01;

// [zh] 导出模块成员
export function capCouponDiscountForMinPay(goodsAmount: number, rawDiscount: number) {
  // [zh] 条件判断 if
  if (!Number.isFinite(goodsAmount) || goodsAmount <= MIN_ORDER_PAY_AMOUNT) return 0;
  // [zh] 声明变量 `maxOff`
  const maxOff = goodsAmount - MIN_ORDER_PAY_AMOUNT;
  // [zh] 声明变量 `off`
  const off = Number.isFinite(rawDiscount) ? rawDiscount : 0;
  // [zh] return 返回
  return Math.max(0, Math.min(off, maxOff));
// [zh] 语法符号
}

// [zh] 导出模块成员
export function calcPayableAfterCoupon(goodsAmount: number, couponDiscount: number) {
  // [zh] 条件判断 if
  if (!Number.isFinite(goodsAmount) || goodsAmount <= 0) return 0;
  // [zh] 声明变量 `off`
  const off = Number.isFinite(couponDiscount) ? couponDiscount : 0;
  // [zh] 声明变量 `pay`
  const pay = Math.round((goodsAmount - off) * 100) / 100;
  // [zh] 条件判断 if
  if (pay <= 0) return MIN_ORDER_PAY_AMOUNT;
  // [zh] 条件判断 if
  if (pay < MIN_ORDER_PAY_AMOUNT) return MIN_ORDER_PAY_AMOUNT;
  // [zh] return 返回
  return pay;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function normalizeDisplayPayAmount(amount: unknown): number {
  // [zh] 声明变量 `n`
  const n = Number(amount);
  // [zh] 条件判断 if
  if (!Number.isFinite(n) || n <= 0) return MIN_ORDER_PAY_AMOUNT;
  // [zh] 条件判断 if
  if (n < MIN_ORDER_PAY_AMOUNT) return MIN_ORDER_PAY_AMOUNT;
  // [zh] return 返回
  return Math.round(n * 100) / 100;
// [zh] 语法符号
}

// [zh] 导出模块成员
export const PAY_ORDER_TIMEOUT_MS = 60_000;

// [zh] 导出模块成员
export function saveCheckoutSession(
  // [zh] TypeScript/JS 语句：`items: CheckoutLineItem[],`
  items: CheckoutLineItem[],
  // [zh] TypeScript/JS 语句：`orderFrom: CheckoutOrderFrom,`
  orderFrom: CheckoutOrderFrom,
  // [zh] TypeScript/JS 语句：`couponRush?: CouponRushCheckoutMeta`
  couponRush?: CouponRushCheckoutMeta
// [zh] 代码块开始
) {
  // [zh] 语句
  sessionStorage.setItem(ITEMS_KEY, JSON.stringify(items));
  // [zh] 语句
  sessionStorage.setItem(FROM_KEY, String(orderFrom));
  // [zh] 条件判断 if
  if (couponRush?.orderId) {
    // [zh] 语句
    sessionStorage.setItem(COUPON_ORDER_KEY, couponRush.orderId);
    // [zh] 语句
    sessionStorage.setItem(COUPON_PAY_EXPIRE_KEY, String(couponRush.payExpireAt));
    // [zh] 条件判断 if
    if (couponRush.payOrderId) {
      // [zh] 语句
      sessionStorage.setItem(COUPON_PAY_ORDER_KEY, couponRush.payOrderId);
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    sessionStorage.removeItem(COUPON_ORDER_KEY);
    // [zh] 语句
    sessionStorage.removeItem(COUPON_PAY_EXPIRE_KEY);
    // [zh] 语句
    sessionStorage.removeItem(COUPON_PAY_ORDER_KEY);
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 导出模块成员
export function loadCheckoutSession(): {
  // [zh] 语句
  items: CheckoutLineItem[];
  // [zh] 语句
  orderFrom: CheckoutOrderFrom;
  // [zh] 语句
  couponRush?: CouponRushCheckoutMeta;
// [zh] 代码块开始
} | null {
  // [zh] 声明变量 `raw`
  const raw = sessionStorage.getItem(ITEMS_KEY);
  // [zh] 条件判断 if
  if (!raw) return null;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `items`
    const items = JSON.parse(raw) as CheckoutLineItem[];
    // [zh] 条件判断 if
    if (!Array.isArray(items) || !items.length) return null;
    // [zh] 声明变量 `fromRaw`
    const fromRaw = sessionStorage.getItem(FROM_KEY);
    // [zh] 声明变量 `orderFrom`
    const orderFrom: CheckoutOrderFrom =
      // [zh] 语句
      fromRaw === '0' ? 0 : fromRaw === '2' ? 2 : 1;
    // [zh] 声明变量 `orderId`
    const orderId = sessionStorage.getItem(COUPON_ORDER_KEY);
    // [zh] 声明变量 `payExpireRaw`
    const payExpireRaw = sessionStorage.getItem(COUPON_PAY_EXPIRE_KEY);
    // [zh] 声明变量 `payExpireAt`
    const payExpireAt = payExpireRaw ? Number(payExpireRaw) : 0;
    // [zh] 声明变量 `couponRush`
    const couponRush =
      // [zh] TypeScript/JS 语句：`orderFrom === 2 && orderId && payExpireAt > 0`
      orderFrom === 2 && orderId && payExpireAt > 0
        // [zh] 代码块开始
        ? {
            // [zh] TypeScript/JS 语句：`orderId,`
            orderId,
            // [zh] TypeScript/JS 语句：`payExpireAt,`
            payExpireAt,
            // [zh] TypeScript/JS 语句：`payOrderId: sessionStorage.getItem(COUPON_PAY_ORDER_KEY`
            payOrderId: sessionStorage.getItem(COUPON_PAY_ORDER_KEY) || undefined
          // [zh] 语法符号
          }
        // [zh] 语句
        : undefined;
    // [zh] return 返回
    return { items, orderFrom, couponRush };
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return null;
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 导出模块成员
export function saveCheckoutSelectedAddress(addressId: string) {
  // [zh] 语句
  sessionStorage.setItem(ADDRESS_KEY, addressId);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function loadCheckoutSelectedAddress(): string | null {
  // [zh] return 返回
  return sessionStorage.getItem(ADDRESS_KEY);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function clearCheckoutSession() {
  // [zh] 语句
  sessionStorage.removeItem(ITEMS_KEY);
  // [zh] 语句
  sessionStorage.removeItem(FROM_KEY);
  // [zh] 语句
  sessionStorage.removeItem(ADDRESS_KEY);
  // [zh] 语句
  sessionStorage.removeItem(COUPON_ORDER_KEY);
  // [zh] 语句
  sessionStorage.removeItem(COUPON_PAY_EXPIRE_KEY);
  // [zh] 语句
  sessionStorage.removeItem(COUPON_PAY_ORDER_KEY);
  // [zh] 语句
  sessionStorage.removeItem('eshop_checkout_cart_ids');
// [zh] 语法符号
}

// [zh] 导出模块成员
export function formatSkuText(item: CheckoutLineItem) {
  // [zh] 条件判断 if
  if (!item.propertyData?.length) return '';
  // [zh] return 返回
  return item.propertyData.map((p) => `${p.propertyName}：${p.propertyValue}`).join(' · ');
// [zh] 语法符号
}

// [zh] 导出模块成员
export function lineSubtotal(item: CheckoutLineItem) {
  // [zh] return 返回
  return Number(item.price) * (Number(item.buyCount) || 0);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function formatPayCountdown(remainMs: number) {
  // [zh] 声明变量 `sec`
  const sec = Math.max(0, Math.ceil(remainMs / 1000));
  // [zh] 声明变量 `m`
  const m = Math.floor(sec / 60);
  // [zh] 声明变量 `s`
  const s = sec % 60;
  // [zh] return 返回
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
// [zh] 语法符号
}
