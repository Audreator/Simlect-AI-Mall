import { computed, onActivated, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { addressApi, couponApi, orderApi } from '@/api/modules';
import {
  PAY_METHOD_ALIPAY_PC,
  PAY_METHOD_ALIPAY_WAP
} from '@/constants/payChannel';
import { confirmAction } from '@/utils/confirm';
import { toast } from '@/utils/toast';
import {
  capCouponDiscountForMinPay,
  calcPayableAfterCoupon,
  clearCheckoutSession,
  formatPayCountdown,
  formatSkuText,
  lineSubtotal,
  loadCheckoutSession,
  loadCheckoutSelectedAddress,
  saveCheckoutSelectedAddress,
  MIN_ORDER_PAY_AMOUNT,
  type CheckoutLineItem,
  type CheckoutOrderFrom,
  type CouponRushCheckoutMeta
} from '@/utils/checkout';
import type { AddressFormItem } from '@/components/business/AddressFormPanel.vue';

export type CheckoutPageMode = 'mobile' | 'desktop';

export function useCheckoutPage(mode: CheckoutPageMode = 'mobile') {
  const router = useRouter();
  const isMobile = computed(() => mode === 'mobile');
  const defaultPayMethod = mode === 'desktop' ? PAY_METHOD_ALIPAY_PC : PAY_METHOD_ALIPAY_WAP;

  const pageLoading = ref(true);
  const initError = ref('');
  const addressLoadError = ref('');
  const couponLoadError = ref('');
  const submitting = ref(false);
  const items = ref<CheckoutLineItem[]>([]);
  const orderFrom = ref<CheckoutOrderFrom>(1);
  const isCouponRush = computed(() => orderFrom.value === 2);
  const couponRushMeta = ref<CouponRushCheckoutMeta | null>(null);
  const payCountdownMs = ref(0);
  let countdownTimer: ReturnType<typeof setInterval> | null = null;

  const payCountdownText = computed(() => formatPayCountdown(payCountdownMs.value));
  const submitButtonText = computed(() => (isCouponRush.value ? '去支付' : '提交订单'));

  const syncPayCountdown = () => {
    if (!couponRushMeta.value?.payExpireAt) {
      payCountdownMs.value = 0;
      return;
    }
    payCountdownMs.value = Math.max(0, couponRushMeta.value.payExpireAt - Date.now());
  };

  const startPayCountdown = () => {
    syncPayCountdown();
    if (countdownTimer) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
    if (!isCouponRush.value || !couponRushMeta.value?.payExpireAt) return;
    countdownTimer = setInterval(() => {
      syncPayCountdown();
      if (payCountdownMs.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }, 1000);
  };

  const addresses = ref<AddressFormItem[]>([]);
  const addressId = ref('');
  const addressFormVisible = ref(false);
  const editingAddress = ref<AddressFormItem | null>(null);
  const addressListOpen = ref(false);
  const remark = ref('');

  const selectedAddress = computed(
    () => addresses.value.find((a) => a.addressId === addressId.value) ?? null
  );
  const payMethod = ref(defaultPayMethod);

  const couponVisible = ref(false);
  const couponLoading = ref(false);
  const couponList = ref<any[]>([]);
  const selectedUserCouponId = ref<string>('');

  const totalCount = computed(() =>
    items.value.reduce((sum, row) => sum + (Number(row.buyCount) || 0), 0)
  );

  const goodsAmount = computed(() =>
    items.value.reduce((sum, row) => sum + lineSubtotal(row), 0).toFixed(2)
  );

  const goodsAmountNum = computed(() => Number(goodsAmount.value));

  const nowTs = () => Date.now();
  const isCouponValid = (c: any) => {
    const start = c?.validStartTime ? new Date(c.validStartTime).getTime() : 0;
    const end = c?.validEndTime ? new Date(c.validEndTime).getTime() : 0;
    const t = nowTs();
    if (start && t < start) return false;
    if (end && t > end) return false;
    return true;
  };

  const calcCouponDiscountRaw = (c: any) => {
    const amount = goodsAmountNum.value;
    const threshold = Number(c?.thresholdAmount ?? 0);
    if (threshold > 0 && amount < threshold) return 0;
    const type = Number(c?.couponType ?? 1);
    if (type === 2) {
      const rawRate = Number(c?.discountRate);
      const rate = Number.isFinite(rawRate) && rawRate > 0 && rawRate < 1 ? rawRate : 1;
      const off = amount - amount * rate;
      return Math.max(0, Math.min(off, amount));
    }
    const off = Number(c?.discountAmount ?? 0);
    return Math.max(0, Math.min(off, amount));
  };

  const calcCouponDiscount = (c: any) =>
    capCouponDiscountForMinPay(goodsAmountNum.value, calcCouponDiscountRaw(c));

  const usableCoupons = computed(() =>
    (couponList.value || [])
      .filter((c) => Number(c?.status ?? 0) === 0)
      .map((c) => {
        const usable = isCouponValid(c) && calcCouponDiscount(c) > 0;
        return { ...c, usable };
      })
  );

  const selectedCoupon = computed(() => {
    if (!selectedUserCouponId.value) return null;
    return (couponList.value || []).find((c) => c.userCouponId === selectedUserCouponId.value) ?? null;
  });

  const couponDiscount = computed(() =>
    selectedCoupon.value ? calcCouponDiscount(selectedCoupon.value) : 0
  );

  const payableAmount = computed(() =>
    calcPayableAfterCoupon(goodsAmountNum.value, couponDiscount.value).toFixed(2)
  );

  const minPayAmountText = MIN_ORDER_PAY_AMOUNT.toFixed(2);

  const showMinPayTip = computed(() => {
    if (!selectedCoupon.value || goodsAmountNum.value <= MIN_ORDER_PAY_AMOUNT) return false;
    const raw = calcCouponDiscountRaw(selectedCoupon.value);
    return raw > couponDiscount.value + 0.001;
  });

  const selectedCouponLabel = computed(() => {
    if (!selectedCoupon.value) return '不使用';
    const off = couponDiscount.value;
    return off > 0 ? `已选：抵扣¥${off.toFixed(2)}` : '不使用';
  });

  const maxAvailableDiscount = computed(() => {
    if (!usableCoupons.value.length) return 0;
    return Math.max(...usableCoupons.value.map((c) => calcCouponDiscount(c)));
  });

  const formatCouponEnd = (val: any) => {
    if (!val) return '--';
    const d = new Date(val);
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${mm}-${dd}`;
  };

  const sortAddresses = (rows: AddressFormItem[]) =>
    [...rows].sort((a, b) => (b.defaultType === 1 ? 1 : 0) - (a.defaultType === 1 ? 1 : 0));

  const applyAddressSelection = () => {
    const savedId = loadCheckoutSelectedAddress();
    if (savedId && addresses.value.some((a) => a.addressId === savedId)) {
      addressId.value = savedId;
      if (!isMobile.value) addressListOpen.value = false;
      return;
    }
    const def = addresses.value.find((a) => a.defaultType === 1) ?? addresses.value[0];
    if (def?.addressId) {
      addressId.value = def.addressId;
      saveCheckoutSelectedAddress(def.addressId);
      if (!isMobile.value) addressListOpen.value = false;
      return;
    }
    addressId.value = '';
    if (!isMobile.value) addressListOpen.value = false;
  };

  const loadAddresses = async () => {
    addressLoadError.value = '';
    try {
      const data = await addressApi.loadDataList();
      addresses.value = sortAddresses(Array.isArray(data) ? data : []);
      applyAddressSelection();
    } catch (e: any) {
      addressLoadError.value = e?.info || '地址加载失败，请重试';
    }
  };

  const goSelectAddress = () => {
    router.push({ path: '/address', query: { from: 'checkout' } });
  };

  const goAddAddress = () => {
    router.push({ path: '/address', query: { from: 'checkout', action: 'add' } });
  };

  const pickAddress = (addr: AddressFormItem) => {
    addressId.value = addr.addressId;
    saveCheckoutSelectedAddress(addr.addressId);
    if (!isMobile.value) addressListOpen.value = false;
  };

  const openAddressForm = (item?: AddressFormItem) => {
    editingAddress.value = item ?? null;
    addressFormVisible.value = true;
  };

  const onAddressFormSaved = async () => {
    const wasAdd = !editingAddress.value;
    const prevIds = new Set(addresses.value.map((a) => a.addressId));
    editingAddress.value = null;
    await loadAddresses();
    if (!isMobile.value && wasAdd) {
      const added = addresses.value.find((a) => !prevIds.has(a.addressId));
      if (added) pickAddress(added);
    }
  };

  const removeAddress = async (id: string) => {
    const ok = await confirmAction('删除后无法恢复，确定要删除该收货地址吗？', {
      title: '删除地址',
      confirmButtonText: '删除'
    });
    if (!ok) return;
    await addressApi.delAddress(id);
    toast.success('已删除');
    if (addressId.value === id) addressId.value = '';
    await loadAddresses();
  };

  const init = async () => {
    pageLoading.value = true;
    initError.value = '';
    try {
      const session = loadCheckoutSession();
      if (!session?.items.length) {
        items.value = [];
        return;
      }
      items.value = session.items;
      orderFrom.value = session.orderFrom;
      if (orderFrom.value === 2) {
        if (!session.couponRush?.orderId) {
          items.value = [];
          toast.warning('订单信息已失效，请重新抢购');
          return;
        }
        couponRushMeta.value = session.couponRush;
        startPayCountdown();
      } else {
        couponRushMeta.value = null;
        await loadAddresses();
        loadCoupons();
      }
    } catch (e: any) {
      initError.value = e?.info || '加载结账信息失败，请重试';
    } finally {
      pageLoading.value = false;
    }
  };

  const loadCoupons = async () => {
    couponLoading.value = true;
    couponLoadError.value = '';
    try {
      const r = await couponApi.loadUserCoupon({ pageNo: 1, status: 0 });
      couponList.value = r?.list || [];
      if (
        selectedUserCouponId.value &&
        !couponList.value.some((c: any) => c.userCouponId === selectedUserCouponId.value)
      ) {
        selectedUserCouponId.value = '';
      }
    } catch (e: any) {
      couponLoadError.value = e?.info || '优惠券加载失败，请重试';
    } finally {
      couponLoading.value = false;
    }
  };

  const openCouponPicker = async () => {
    couponVisible.value = true;
    if (!couponList.value.length) await loadCoupons();
  };

  const selectCoupon = (c: any | null) => {
    selectedUserCouponId.value = c?.userCouponId ? String(c.userCouponId) : '';
  };

  const submit = async () => {
    if (!items.value.length) {
      toast.warning('没有可结算的商品');
      return;
    }
    if (isCouponRush.value && payCountdownMs.value <= 0) {
      toast.warning('支付已超时，请前往我的订单查看或重新抢购');
      return;
    }
    if (!isCouponRush.value && !addressId.value) {
      toast.warning('请选择收货地址，或新增地址');
      if (isMobile.value) {
        goSelectAddress();
      } else if (addresses.value.length) {
        addressListOpen.value = true;
      } else {
        openAddressForm();
      }
      return;
    }
    if (!payMethod.value) {
      toast.warning('请选择支付方式');
      return;
    }

    const minPayHint = showMinPayTip.value ? `（已使用优惠券，最低实付 ¥${minPayAmountText}）` : '';
    const confirmText = isCouponRush.value
      ? `订单已创建，支付优惠券「${items.value[0]?.productName || ''}」合计 ¥${payableAmount.value}，确定去支付吗？`
      : `共 ${totalCount.value} 件商品，合计 ¥${payableAmount.value}${minPayHint}，确定提交订单吗？`;
    const ok = await confirmAction(confirmText, {
      title: isCouponRush.value ? '去支付' : '提交订单',
      confirmButtonText: isCouponRush.value ? '去支付' : '提交订单'
    });
    if (!ok) return;

    submitting.value = true;
    try {
      let payInfo: { payOrderId?: string; orderId?: string };
      if (isCouponRush.value) {
        const couponId = items.value[0]?.productId;
        if (!couponId) {
          toast.warning('优惠券信息无效');
          return;
        }
        payInfo = await couponApi.buyDiscountCoupon(couponId, payMethod.value);
      } else {
        if (selectedUserCouponId.value) {
          if (!selectedCoupon.value) {
            toast.warning('所选优惠券已失效，请重新选择');
            selectedUserCouponId.value = '';
            return;
          }
          if (Number(selectedCoupon.value.status) !== 0) {
            toast.warning('所选优惠券已不可用，请重新选择');
            selectedUserCouponId.value = '';
            return;
          }
          if (!isCouponValid(selectedCoupon.value)) {
            toast.warning('所选优惠券已过期，请重新选择');
            selectedUserCouponId.value = '';
            return;
          }
          if (calcCouponDiscount(selectedCoupon.value) <= 0) {
            toast.warning('订单金额不满足优惠券使用条件，请重新选择');
            selectedUserCouponId.value = '';
            return;
          }
        }
        const orderList = items.value.map((item) => ({
          productId: item.productId,
          propertyValueIds: item.propertyValueIds,
          buyCount: Math.min(99, Math.max(1, Math.trunc(Number(item.buyCount) || 1))),
          remark: item.remark?.trim() || remark.value.trim() || ''
        }));
        payInfo = await orderApi.postOrder({
          payMethod: payMethod.value,
          addressId: addressId.value,
          orderFrom: orderFrom.value,
          orderList,
          userCouponId: selectedUserCouponId.value || undefined
        });
      }
      clearCheckoutSession();
      const payOrderId = payInfo?.payOrderId || payInfo?.orderId;
      if (!payOrderId) {
        toast.error('下单成功但未返回支付单号');
        router.push('/orders');
        return;
      }
      toast.success(isCouponRush.value ? '正在跳转支付' : '订单已创建');
      router.push(`/payment/${payOrderId}`);
    } catch {
      
    } finally {
      submitting.value = false;
    }
  };

  onMounted(() => {
    init();
  });

  onActivated(() => {
    if (!pageLoading.value && !isCouponRush.value && items.value.length) {
      void loadAddresses();
    }
  });

  onUnmounted(() => {
    if (countdownTimer) clearInterval(countdownTimer);
  });

  return {
    isMobile,
    pageLoading,
    initError,
    addressLoadError,
    couponLoadError,
    submitting,
    items,
    isCouponRush,
    payCountdownMs,
    payCountdownText,
    submitButtonText,
    addresses,
    addressId,
    addressFormVisible,
    editingAddress,
    addressListOpen,
    remark,
    selectedAddress,
    payMethod,
    couponVisible,
    couponLoading,
    usableCoupons,
    selectedUserCouponId,
    couponDiscount,
    payableAmount,
    minPayAmountText,
    showMinPayTip,
    selectedCouponLabel,
    maxAvailableDiscount,
    totalCount,
    goodsAmount,
    formatCouponEnd,
    calcCouponDiscount,
    formatSkuText,
    lineSubtotal,
    init,
    loadAddresses,
    goSelectAddress,
    goAddAddress,
    pickAddress,
    openAddressForm,
    onAddressFormSaved,
    removeAddress,
    loadCoupons,
    openCouponPicker,
    selectCoupon,
    submit,
    PAY_METHOD_ALIPAY_PC,
    PAY_METHOD_ALIPAY_WAP
  };
}
