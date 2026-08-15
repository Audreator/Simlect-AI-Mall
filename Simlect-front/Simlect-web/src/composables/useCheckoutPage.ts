// [zh] 导入 `vue`
import { computed, onActivated, onMounted, onUnmounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@/api/modules`
import { addressApi, couponApi, orderApi } from '@/api/modules';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`PAY_METHOD_ALIPAY_PC,`
  PAY_METHOD_ALIPAY_PC,
  // [zh] TypeScript/JS 语句：`PAY_METHOD_ALIPAY_WAP`
  PAY_METHOD_ALIPAY_WAP
// [zh] 语句
} from '@/constants/payChannel';
// [zh] 导入 `@/utils/confirm`
import { confirmAction } from '@/utils/confirm';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`capCouponDiscountForMinPay,`
  capCouponDiscountForMinPay,
  // [zh] TypeScript/JS 语句：`calcPayableAfterCoupon,`
  calcPayableAfterCoupon,
  // [zh] TypeScript/JS 语句：`clearCheckoutSession,`
  clearCheckoutSession,
  // [zh] TypeScript/JS 语句：`formatPayCountdown,`
  formatPayCountdown,
  // [zh] TypeScript/JS 语句：`formatSkuText,`
  formatSkuText,
  // [zh] TypeScript/JS 语句：`lineSubtotal,`
  lineSubtotal,
  // [zh] TypeScript/JS 语句：`loadCheckoutSession,`
  loadCheckoutSession,
  // [zh] TypeScript/JS 语句：`loadCheckoutSelectedAddress,`
  loadCheckoutSelectedAddress,
  // [zh] TypeScript/JS 语句：`saveCheckoutSelectedAddress,`
  saveCheckoutSelectedAddress,
  // [zh] TypeScript/JS 语句：`MIN_ORDER_PAY_AMOUNT,`
  MIN_ORDER_PAY_AMOUNT,
  // [zh] TypeScript 类型 `CheckoutLineItem`（类比 Java interface/DTO）
  type CheckoutLineItem,
  // [zh] TypeScript 类型 `CheckoutOrderFrom`（类比 Java interface/DTO）
  type CheckoutOrderFrom,
  // [zh] TypeScript 类型 `CouponRushCheckoutMeta`（类比 Java interface/DTO）
  type CouponRushCheckoutMeta
// [zh] 语句
} from '@/utils/checkout';
// [zh] 导入 `模块`
import type { AddressFormItem } from '@/components/business/AddressFormPanel.vue';

// [zh] 导出模块成员
export type CheckoutPageMode = 'mobile' | 'desktop';

// [zh] 导出模块成员
export function useCheckoutPage(mode: CheckoutPageMode = 'mobile') {
  // [zh] 声明变量 `router`
  const router = useRouter();
  // [zh] 声明变量 `isMobile`
  const isMobile = computed(() => mode === 'mobile');
  // [zh] 声明变量 `defaultPayMethod`
  const defaultPayMethod = mode === 'desktop' ? PAY_METHOD_ALIPAY_PC : PAY_METHOD_ALIPAY_WAP;

  // [zh] 声明变量 `pageLoading`
  const pageLoading = ref(true);
  // [zh] 声明变量 `initError`
  const initError = ref('');
  // [zh] 声明变量 `addressLoadError`
  const addressLoadError = ref('');
  // [zh] 声明变量 `couponLoadError`
  const couponLoadError = ref('');
  // [zh] 声明变量 `submitting`
  const submitting = ref(false);
  // [zh] 声明变量 `items`
  const items = ref<CheckoutLineItem[]>([]);
  // [zh] 声明变量 `orderFrom`
  const orderFrom = ref<CheckoutOrderFrom>(1);
  // [zh] 声明变量 `isCouponRush`
  const isCouponRush = computed(() => orderFrom.value === 2);
  // [zh] 声明变量 `couponRushMeta`
  const couponRushMeta = ref<CouponRushCheckoutMeta | null>(null);
  // [zh] 声明变量 `payCountdownMs`
  const payCountdownMs = ref(0);
  // [zh] 声明变量 `countdownTimer`
  let countdownTimer: ReturnType<typeof setInterval> | null = null;

  // [zh] 声明变量 `payCountdownText`
  const payCountdownText = computed(() => formatPayCountdown(payCountdownMs.value));
  // [zh] 声明变量 `submitButtonText`
  const submitButtonText = computed(() => (isCouponRush.value ? '去支付' : '提交订单'));

  // [zh] 声明变量 `syncPayCountdown`
  const syncPayCountdown = () => {
    // [zh] 条件判断 if
    if (!couponRushMeta.value?.payExpireAt) {
      // [zh] 语句
      payCountdownMs.value = 0;
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 语句
    payCountdownMs.value = Math.max(0, couponRushMeta.value.payExpireAt - Date.now());
  // [zh] 语句
  };

  // [zh] 声明变量 `startPayCountdown`
  const startPayCountdown = () => {
    // [zh] 语句
    syncPayCountdown();
    // [zh] 条件判断 if
    if (countdownTimer) {
      // [zh] 语句
      clearInterval(countdownTimer);
      // [zh] 语句
      countdownTimer = null;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (!isCouponRush.value || !couponRushMeta.value?.payExpireAt) return;
    // [zh] 代码块开始
    countdownTimer = setInterval(() => {
      // [zh] 语句
      syncPayCountdown();
      // [zh] 条件判断 if
      if (payCountdownMs.value <= 0 && countdownTimer) {
        // [zh] 语句
        clearInterval(countdownTimer);
        // [zh] 语句
        countdownTimer = null;
      // [zh] 语法符号
      }
    // [zh] 语句
    }, 1000);
  // [zh] 语句
  };

  // [zh] 声明变量 `addresses`
  const addresses = ref<AddressFormItem[]>([]);
  // [zh] 声明变量 `addressId`
  const addressId = ref('');
  // [zh] 声明变量 `addressFormVisible`
  const addressFormVisible = ref(false);
  // [zh] 声明变量 `editingAddress`
  const editingAddress = ref<AddressFormItem | null>(null);
  // [zh] 声明变量 `addressListOpen`
  const addressListOpen = ref(false);
  // [zh] 声明变量 `remark`
  const remark = ref('');

  // [zh] 声明变量 `selectedAddress`
  const selectedAddress = computed(
    // [zh] TypeScript/JS 语句：`() => addresses.value.find((a) => a.addressId === addre`
    () => addresses.value.find((a) => a.addressId === addressId.value) ?? null
  // [zh] 语句
  );
  // [zh] 声明变量 `payMethod`
  const payMethod = ref(defaultPayMethod);

  // [zh] 声明变量 `couponVisible`
  const couponVisible = ref(false);
  // [zh] 声明变量 `couponLoading`
  const couponLoading = ref(false);
  // [zh] 声明变量 `couponList`
  const couponList = ref<any[]>([]);
  // [zh] 声明变量 `selectedUserCouponId`
  const selectedUserCouponId = ref<string>('');

  // [zh] 声明变量 `totalCount`
  const totalCount = computed(() =>
    // [zh] TypeScript/JS 语句：`items.value.reduce((sum, row) => sum + (Number(row.buyC`
    items.value.reduce((sum, row) => sum + (Number(row.buyCount) || 0), 0)
  // [zh] 语句
  );

  // [zh] 声明变量 `goodsAmount`
  const goodsAmount = computed(() =>
    // [zh] TypeScript/JS 语句：`items.value.reduce((sum, row) => sum + lineSubtotal(row`
    items.value.reduce((sum, row) => sum + lineSubtotal(row), 0).toFixed(2)
  // [zh] 语句
  );

  // [zh] 声明变量 `goodsAmountNum`
  const goodsAmountNum = computed(() => Number(goodsAmount.value));

  // [zh] 声明变量 `nowTs`
  const nowTs = () => Date.now();
  // [zh] 声明变量 `isCouponValid`
  const isCouponValid = (c: any) => {
    // [zh] 声明变量 `start`
    const start = c?.validStartTime ? new Date(c.validStartTime).getTime() : 0;
    // [zh] 声明变量 `end`
    const end = c?.validEndTime ? new Date(c.validEndTime).getTime() : 0;
    // [zh] 声明变量 `t`
    const t = nowTs();
    // [zh] 条件判断 if
    if (start && t < start) return false;
    // [zh] 条件判断 if
    if (end && t > end) return false;
    // [zh] return 返回
    return true;
  // [zh] 语句
  };

  // [zh] 声明变量 `calcCouponDiscountRaw`
  const calcCouponDiscountRaw = (c: any) => {
    // [zh] 声明变量 `amount`
    const amount = goodsAmountNum.value;
    // [zh] 声明变量 `threshold`
    const threshold = Number(c?.thresholdAmount ?? 0);
    // [zh] 条件判断 if
    if (threshold > 0 && amount < threshold) return 0;
    // [zh] 声明变量 `type`
    const type = Number(c?.couponType ?? 1);
    // [zh] 条件判断 if
    if (type === 2) {
      // [zh] 声明变量 `rate`（校验折扣率合法性：0<rate<1，非法回退 1 无折扣）
      const rawRate = Number(c?.discountRate);
      const rate = Number.isFinite(rawRate) && rawRate > 0 && rawRate < 1 ? rawRate : 1;
      // [zh] 声明变量 `off`
      const off = amount - amount * rate;
      // [zh] return 返回
      return Math.max(0, Math.min(off, amount));
    // [zh] 语法符号
    }
    // [zh] 声明变量 `off`
    const off = Number(c?.discountAmount ?? 0);
    // [zh] return 返回
    return Math.max(0, Math.min(off, amount));
  // [zh] 语句
  };

  // [zh] 声明变量 `calcCouponDiscount`
  const calcCouponDiscount = (c: any) =>
    // [zh] 语句
    capCouponDiscountForMinPay(goodsAmountNum.value, calcCouponDiscountRaw(c));

  // [zh] 声明变量 `usableCoupons`
  const usableCoupons = computed(() =>
    // [zh] TypeScript/JS 语句：`(couponList.value || [])`
    (couponList.value || [])
      // [zh] TypeScript/JS 语句：`.filter((c) => Number(c?.status ?? 0) === 0)`
      .filter((c) => Number(c?.status ?? 0) === 0)
      // [zh] 代码块开始
      .map((c) => {
        // [zh] 声明变量 `usable`
        const usable = isCouponValid(c) && calcCouponDiscount(c) > 0;
        // [zh] return 返回
        return { ...c, usable };
      // [zh] TypeScript/JS 语句：`})`
      })
  // [zh] 语句
  );

  // [zh] 声明变量 `selectedCoupon`
  const selectedCoupon = computed(() => {
    // [zh] 条件判断 if
    if (!selectedUserCouponId.value) return null;
    // [zh] return 返回
    return (couponList.value || []).find((c) => c.userCouponId === selectedUserCouponId.value) ?? null;
  // [zh] 语句
  });

  // [zh] 声明变量 `couponDiscount`
  const couponDiscount = computed(() =>
    // [zh] TypeScript/JS 语句：`selectedCoupon.value ? calcCouponDiscount(selectedCoupo`
    selectedCoupon.value ? calcCouponDiscount(selectedCoupon.value) : 0
  // [zh] 语句
  );

  // [zh] 声明变量 `payableAmount`
  const payableAmount = computed(() =>
    // [zh] TypeScript/JS 语句：`calcPayableAfterCoupon(goodsAmountNum.value, couponDisc`
    calcPayableAfterCoupon(goodsAmountNum.value, couponDiscount.value).toFixed(2)
  // [zh] 语句
  );

  // [zh] 声明变量 `minPayAmountText`
  const minPayAmountText = MIN_ORDER_PAY_AMOUNT.toFixed(2);

  // [zh] 声明变量 `showMinPayTip`
  const showMinPayTip = computed(() => {
    // [zh] 条件判断 if
    if (!selectedCoupon.value || goodsAmountNum.value <= MIN_ORDER_PAY_AMOUNT) return false;
    // [zh] 声明变量 `raw`
    const raw = calcCouponDiscountRaw(selectedCoupon.value);
    // [zh] return 返回
    return raw > couponDiscount.value + 0.001;
  // [zh] 语句
  });

  // [zh] 声明变量 `selectedCouponLabel`
  const selectedCouponLabel = computed(() => {
    // [zh] 条件判断 if
    if (!selectedCoupon.value) return '不使用';
    // [zh] 声明变量 `off`
    const off = couponDiscount.value;
    // [zh] return 返回
    return off > 0 ? `已选：抵扣¥${off.toFixed(2)}` : '不使用';
  // [zh] 语句
  });

  // [zh] 声明变量 `maxAvailableDiscount`
  const maxAvailableDiscount = computed(() => {
    // [zh] 条件判断 if
    if (!usableCoupons.value.length) return 0;
    // [zh] return 返回
    return Math.max(...usableCoupons.value.map((c) => calcCouponDiscount(c)));
  // [zh] 语句
  });

  // [zh] 声明变量 `formatCouponEnd`
  const formatCouponEnd = (val: any) => {
    // [zh] 条件判断 if
    if (!val) return '--';
    // [zh] 声明变量 `d`
    const d = new Date(val);
    // [zh] 声明变量 `mm`
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    // [zh] 声明变量 `dd`
    const dd = String(d.getDate()).padStart(2, '0');
    // [zh] return 返回
    return `${mm}-${dd}`;
  // [zh] 语句
  };

  // [zh] 声明变量 `sortAddresses`
  const sortAddresses = (rows: AddressFormItem[]) =>
    // [zh] 语句
    [...rows].sort((a, b) => (b.defaultType === 1 ? 1 : 0) - (a.defaultType === 1 ? 1 : 0));

  // [zh] 声明变量 `applyAddressSelection`
  const applyAddressSelection = () => {
    // [zh] 声明变量 `savedId`
    const savedId = loadCheckoutSelectedAddress();
    // [zh] 条件判断 if
    if (savedId && addresses.value.some((a) => a.addressId === savedId)) {
      // [zh] 语句
      addressId.value = savedId;
      // [zh] 条件判断 if
      if (!isMobile.value) addressListOpen.value = false;
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 声明变量 `def`
    const def = addresses.value.find((a) => a.defaultType === 1) ?? addresses.value[0];
    // [zh] 条件判断 if
    if (def?.addressId) {
      // [zh] 语句
      addressId.value = def.addressId;
      // [zh] 语句
      saveCheckoutSelectedAddress(def.addressId);
      // [zh] 条件判断 if
      if (!isMobile.value) addressListOpen.value = false;
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 语句
    addressId.value = '';
    // [zh] 条件判断 if
    if (!isMobile.value) addressListOpen.value = false;
  // [zh] 语句
  };

  // [zh] 声明变量 `loadAddresses`
  const loadAddresses = async () => {
    // [zh] 语句
    addressLoadError.value = '';
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `data`
      const data = await addressApi.loadDataList();
      // [zh] 语句
      addresses.value = sortAddresses(Array.isArray(data) ? data : []);
      // [zh] 语句
      applyAddressSelection();
    // [zh] 代码块开始
    } catch (e: any) {
      // [zh] 语句
      addressLoadError.value = e?.info || '地址加载失败，请重试';
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `goSelectAddress`
  const goSelectAddress = () => {
    // [zh] 语句
    router.push({ path: '/address', query: { from: 'checkout' } });
  // [zh] 语句
  };

  // [zh] 声明变量 `goAddAddress`
  const goAddAddress = () => {
    // [zh] 语句
    router.push({ path: '/address', query: { from: 'checkout', action: 'add' } });
  // [zh] 语句
  };

  // [zh] 声明变量 `pickAddress`
  const pickAddress = (addr: AddressFormItem) => {
    // [zh] 语句
    addressId.value = addr.addressId;
    // [zh] 语句
    saveCheckoutSelectedAddress(addr.addressId);
    // [zh] 条件判断 if
    if (!isMobile.value) addressListOpen.value = false;
  // [zh] 语句
  };

  // [zh] 声明变量 `openAddressForm`
  const openAddressForm = (item?: AddressFormItem) => {
    // [zh] 语句
    editingAddress.value = item ?? null;
    // [zh] 语句
    addressFormVisible.value = true;
  // [zh] 语句
  };

  // [zh] 声明变量 `onAddressFormSaved`
  const onAddressFormSaved = async () => {
    // [zh] 声明变量 `wasAdd`
    const wasAdd = !editingAddress.value;
    // [zh] 声明变量 `prevIds`
    const prevIds = new Set(addresses.value.map((a) => a.addressId));
    // [zh] 语句
    editingAddress.value = null;
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await loadAddresses();
    // [zh] 条件判断 if
    if (!isMobile.value && wasAdd) {
      // [zh] 声明变量 `added`
      const added = addresses.value.find((a) => !prevIds.has(a.addressId));
      // [zh] 条件判断 if
      if (added) pickAddress(added);
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `removeAddress`
  const removeAddress = async (id: string) => {
    // [zh] 声明变量 `ok`
    const ok = await confirmAction('删除后无法恢复，确定要删除该收货地址吗？', {
      // [zh] TypeScript/JS 语句：`title: '删除地址',`
      title: '删除地址',
      // [zh] TypeScript/JS 语句：`confirmButtonText: '删除'`
      confirmButtonText: '删除'
    // [zh] 语句
    });
    // [zh] 条件判断 if
    if (!ok) return;
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await addressApi.delAddress(id);
    // [zh] 语句
    toast.success('已删除');
    // [zh] 条件判断 if
    if (addressId.value === id) addressId.value = '';
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await loadAddresses();
  // [zh] 语句
  };

  // [zh] 声明变量 `init`
  const init = async () => {
    // [zh] 语句
    pageLoading.value = true;
    // [zh] 语句
    initError.value = '';
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `session`
      const session = loadCheckoutSession();
      // [zh] 条件判断 if
      if (!session?.items.length) {
        // [zh] 语句
        items.value = [];
        // [zh] 语句
        return;
      // [zh] 语法符号
      }
      // [zh] 语句
      items.value = session.items;
      // [zh] 语句
      orderFrom.value = session.orderFrom;
      // [zh] 条件判断 if
      if (orderFrom.value === 2) {
        // [zh] 条件判断 if
        if (!session.couponRush?.orderId) {
          // [zh] 语句
          items.value = [];
          // [zh] 语句
          toast.warning('订单信息已失效，请重新抢购');
          // [zh] 语句
          return;
        // [zh] 语法符号
        }
        // [zh] 语句
        couponRushMeta.value = session.couponRush;
        // [zh] 语句
        startPayCountdown();
      // [zh] 代码块开始
      } else {
        // [zh] 语句
        couponRushMeta.value = null;
        // [zh] await 等待 Promise（类比 CompletableFuture.get）
        await loadAddresses();
        // [zh] 语句
        loadCoupons();
      // [zh] 语法符号
      }
    // [zh] 代码块开始
    } catch (e: any) {
      // [zh] 语句
      initError.value = e?.info || '加载结账信息失败，请重试';
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      pageLoading.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `loadCoupons`
  const loadCoupons = async () => {
    // [zh] 语句
    couponLoading.value = true;
    // [zh] 语句
    couponLoadError.value = '';
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `r`
      const r = await couponApi.loadUserCoupon({ pageNo: 1, status: 0 });
      // [zh] 语句
      couponList.value = r?.list || [];
      // [zh] 条件判断 if
      if (
        // [zh] TypeScript/JS 语句：`selectedUserCouponId.value &&`
        selectedUserCouponId.value &&
        // [zh] TypeScript/JS 语句：`!couponList.value.some((c: any) => c.userCouponId === s`
        !couponList.value.some((c: any) => c.userCouponId === selectedUserCouponId.value)
      // [zh] 代码块开始
      ) {
        // [zh] 语句
        selectedUserCouponId.value = '';
      // [zh] 语法符号
      }
    // [zh] 代码块开始
    } catch (e: any) {
      // [zh] 语句
      couponLoadError.value = e?.info || '优惠券加载失败，请重试';
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      couponLoading.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `openCouponPicker`
  const openCouponPicker = async () => {
    // [zh] 语句
    couponVisible.value = true;
    // [zh] 条件判断 if
    if (!couponList.value.length) await loadCoupons();
  // [zh] 语句
  };

  // [zh] 声明变量 `selectCoupon`
  const selectCoupon = (c: any | null) => {
    // [zh] 语句
    selectedUserCouponId.value = c?.userCouponId ? String(c.userCouponId) : '';
  // [zh] 语句
  };

  // [zh] 声明变量 `submit`
  const submit = async () => {
    // [zh] 条件判断 if
    if (!items.value.length) {
      // [zh] 语句
      toast.warning('没有可结算的商品');
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (isCouponRush.value && payCountdownMs.value <= 0) {
      // [zh] 语句
      toast.warning('支付已超时，请前往我的订单查看或重新抢购');
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (!isCouponRush.value && !addressId.value) {
      // [zh] 语句
      toast.warning('请选择收货地址，或新增地址');
      // [zh] 条件判断 if
      if (isMobile.value) {
        // [zh] 语句
        goSelectAddress();
      // [zh] 代码块开始
      } else if (addresses.value.length) {
        // [zh] 语句
        addressListOpen.value = true;
      // [zh] 代码块开始
      } else {
        // [zh] 语句
        openAddressForm();
      // [zh] 语法符号
      }
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (!payMethod.value) {
      // [zh] 语句
      toast.warning('请选择支付方式');
      // [zh] 语句
      return;
    // [zh] 语法符号
    }

    // [zh] 声明变量 `minPayHint`
    const minPayHint = showMinPayTip.value ? `（已使用优惠券，最低实付 ¥${minPayAmountText}）` : '';
    // [zh] 声明变量 `confirmText`
    const confirmText = isCouponRush.value
      // [zh] TypeScript/JS 语句：`? `订单已创建，支付优惠券「${items.value[0]?.productName || ''}」合计 `
      ? `订单已创建，支付优惠券「${items.value[0]?.productName || ''}」合计 ¥${payableAmount.value}，确定去支付吗？`
      // [zh] 语句
      : `共 ${totalCount.value} 件商品，合计 ¥${payableAmount.value}${minPayHint}，确定提交订单吗？`;
    // [zh] 声明变量 `ok`
    const ok = await confirmAction(confirmText, {
      // [zh] TypeScript/JS 语句：`title: isCouponRush.value ? '去支付' : '提交订单',`
      title: isCouponRush.value ? '去支付' : '提交订单',
      // [zh] TypeScript/JS 语句：`confirmButtonText: isCouponRush.value ? '去支付' : '提交订单'`
      confirmButtonText: isCouponRush.value ? '去支付' : '提交订单'
    // [zh] 语句
    });
    // [zh] 条件判断 if
    if (!ok) return;

    // [zh] 语句
    submitting.value = true;
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `payInfo`
      let payInfo: { payOrderId?: string; orderId?: string };
      // [zh] 条件判断 if
      if (isCouponRush.value) {
        // [zh] 声明变量 `couponId`
        const couponId = items.value[0]?.productId;
        // [zh] 条件判断 if
        if (!couponId) {
          // [zh] 语句
          toast.warning('优惠券信息无效');
          // [zh] 语句
          return;
        // [zh] 语法符号
        }
        // [zh] 语句
        payInfo = await couponApi.buyDiscountCoupon(couponId, payMethod.value);
      // [zh] 代码块开始
      } else {
        // [zh] 条件判断 if
        if (selectedUserCouponId.value) {
          // [zh] 条件判断 if
          if (!selectedCoupon.value) {
            // [zh] 语句
            toast.warning('所选优惠券已失效，请重新选择');
            // [zh] 语句
            selectedUserCouponId.value = '';
            // [zh] 语句
            return;
          // [zh] 语法符号
          }
          // [zh] 条件判断 if
          if (Number(selectedCoupon.value.status) !== 0) {
            // [zh] 语句
            toast.warning('所选优惠券已不可用，请重新选择');
            // [zh] 语句
            selectedUserCouponId.value = '';
            // [zh] 语句
            return;
          // [zh] 语法符号
          }
          // [zh] 条件判断 if
          if (!isCouponValid(selectedCoupon.value)) {
            // [zh] 语句
            toast.warning('所选优惠券已过期，请重新选择');
            // [zh] 语句
            selectedUserCouponId.value = '';
            // [zh] 语句
            return;
          // [zh] 语法符号
          }
          // [zh] 条件判断 if
          if (calcCouponDiscount(selectedCoupon.value) <= 0) {
            // [zh] 语句
            toast.warning('订单金额不满足优惠券使用条件，请重新选择');
            // [zh] 语句
            selectedUserCouponId.value = '';
            // [zh] 语句
            return;
          // [zh] 语法符号
          }
        // [zh] 语法符号
        }
        // [zh] 声明变量 `orderList`
        const orderList = items.value.map((item) => ({
          // [zh] TypeScript/JS 语句：`productId: item.productId,`
          productId: item.productId,
          // [zh] TypeScript/JS 语句：`propertyValueIds: item.propertyValueIds,`
          propertyValueIds: item.propertyValueIds,
          // [zh] TypeScript/JS 语句：`buyCount: 钳制 1~99（与后端一致，防 sessionStorage 篡改为超大数量）,`
          buyCount: Math.min(99, Math.max(1, Math.trunc(Number(item.buyCount) || 1))),
          // [zh] TypeScript/JS 语句：`remark: item.remark?.trim() || remark.value.trim() || '`
          remark: item.remark?.trim() || remark.value.trim() || ''
        // [zh] 语句
        }));
        // [zh] 代码块开始
        payInfo = await orderApi.postOrder({
          // [zh] TypeScript/JS 语句：`payMethod: payMethod.value,`
          payMethod: payMethod.value,
          // [zh] TypeScript/JS 语句：`addressId: addressId.value,`
          addressId: addressId.value,
          // [zh] TypeScript/JS 语句：`orderFrom: orderFrom.value,`
          orderFrom: orderFrom.value,
          // [zh] TypeScript/JS 语句：`orderList,`
          orderList,
          // [zh] TypeScript/JS 语句：`userCouponId: selectedUserCouponId.value || undefined`
          userCouponId: selectedUserCouponId.value || undefined
        // [zh] 语句
        });
      // [zh] 语法符号
      }
      // [zh] 语句
      clearCheckoutSession();
      // [zh] 声明变量 `payOrderId`
      const payOrderId = payInfo?.payOrderId || payInfo?.orderId;
      // [zh] 条件判断 if
      if (!payOrderId) {
        // [zh] 语句
        toast.error('下单成功但未返回支付单号');
        // [zh] 语句
        router.push('/orders');
        // [zh] 语句
        return;
      // [zh] 语法符号
      }
      // [zh] 语句
      toast.success(isCouponRush.value ? '正在跳转支付' : '订单已创建');
      // [zh] 语句
      router.push(`/payment/${payOrderId}`);
    // [zh] 代码块开始
    } catch {
      
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      submitting.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 代码块开始
  onMounted(() => {
    // [zh] 语句
    init();
  // [zh] 语句
  });

  // [zh] 代码块开始
  onActivated(() => {
    // [zh] 条件判断 if
    if (!pageLoading.value && !isCouponRush.value && items.value.length) {
      // [zh] 语句
      void loadAddresses();
    // [zh] 语法符号
    }
  // [zh] 语句
  });

  // [zh] 代码块开始
  onUnmounted(() => {
    // [zh] 条件判断 if
    if (countdownTimer) clearInterval(countdownTimer);
  // [zh] 语句
  });

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`isMobile,`
    isMobile,
    // [zh] TypeScript/JS 语句：`pageLoading,`
    pageLoading,
    // [zh] TypeScript/JS 语句：`initError,`
    initError,
    // [zh] TypeScript/JS 语句：`addressLoadError,`
    addressLoadError,
    // [zh] TypeScript/JS 语句：`couponLoadError,`
    couponLoadError,
    // [zh] TypeScript/JS 语句：`submitting,`
    submitting,
    // [zh] TypeScript/JS 语句：`items,`
    items,
    // [zh] TypeScript/JS 语句：`isCouponRush,`
    isCouponRush,
    // [zh] TypeScript/JS 语句：`payCountdownMs,`
    payCountdownMs,
    // [zh] TypeScript/JS 语句：`payCountdownText,`
    payCountdownText,
    // [zh] TypeScript/JS 语句：`submitButtonText,`
    submitButtonText,
    // [zh] TypeScript/JS 语句：`addresses,`
    addresses,
    // [zh] TypeScript/JS 语句：`addressId,`
    addressId,
    // [zh] TypeScript/JS 语句：`addressFormVisible,`
    addressFormVisible,
    // [zh] TypeScript/JS 语句：`editingAddress,`
    editingAddress,
    // [zh] TypeScript/JS 语句：`addressListOpen,`
    addressListOpen,
    // [zh] TypeScript/JS 语句：`remark,`
    remark,
    // [zh] TypeScript/JS 语句：`selectedAddress,`
    selectedAddress,
    // [zh] TypeScript/JS 语句：`payMethod,`
    payMethod,
    // [zh] TypeScript/JS 语句：`couponVisible,`
    couponVisible,
    // [zh] TypeScript/JS 语句：`couponLoading,`
    couponLoading,
    // [zh] TypeScript/JS 语句：`usableCoupons,`
    usableCoupons,
    // [zh] TypeScript/JS 语句：`selectedUserCouponId,`
    selectedUserCouponId,
    // [zh] TypeScript/JS 语句：`couponDiscount,`
    couponDiscount,
    // [zh] TypeScript/JS 语句：`payableAmount,`
    payableAmount,
    // [zh] TypeScript/JS 语句：`minPayAmountText,`
    minPayAmountText,
    // [zh] TypeScript/JS 语句：`showMinPayTip,`
    showMinPayTip,
    // [zh] TypeScript/JS 语句：`selectedCouponLabel,`
    selectedCouponLabel,
    // [zh] TypeScript/JS 语句：`maxAvailableDiscount,`
    maxAvailableDiscount,
    // [zh] TypeScript/JS 语句：`totalCount,`
    totalCount,
    // [zh] TypeScript/JS 语句：`goodsAmount,`
    goodsAmount,
    // [zh] TypeScript/JS 语句：`formatCouponEnd,`
    formatCouponEnd,
    // [zh] TypeScript/JS 语句：`calcCouponDiscount,`
    calcCouponDiscount,
    // [zh] TypeScript/JS 语句：`formatSkuText,`
    formatSkuText,
    // [zh] TypeScript/JS 语句：`lineSubtotal,`
    lineSubtotal,
    // [zh] TypeScript/JS 语句：`init,`
    init,
    // [zh] TypeScript/JS 语句：`loadAddresses,`
    loadAddresses,
    // [zh] TypeScript/JS 语句：`goSelectAddress,`
    goSelectAddress,
    // [zh] TypeScript/JS 语句：`goAddAddress,`
    goAddAddress,
    // [zh] TypeScript/JS 语句：`pickAddress,`
    pickAddress,
    // [zh] TypeScript/JS 语句：`openAddressForm,`
    openAddressForm,
    // [zh] TypeScript/JS 语句：`onAddressFormSaved,`
    onAddressFormSaved,
    // [zh] TypeScript/JS 语句：`removeAddress,`
    removeAddress,
    // [zh] TypeScript/JS 语句：`loadCoupons,`
    loadCoupons,
    // [zh] TypeScript/JS 语句：`openCouponPicker,`
    openCouponPicker,
    // [zh] TypeScript/JS 语句：`selectCoupon,`
    selectCoupon,
    // [zh] TypeScript/JS 语句：`submit,`
    submit,
    // [zh] TypeScript/JS 语句：`PAY_METHOD_ALIPAY_PC,`
    PAY_METHOD_ALIPAY_PC,
    // [zh] TypeScript/JS 语句：`PAY_METHOD_ALIPAY_WAP`
    PAY_METHOD_ALIPAY_WAP
  // [zh] 语句
  };
// [zh] 语法符号
}
