// [zh] 导入 `vue`
import { onMounted, onUnmounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@/api/modules`
import { orderApi } from '@/api/modules';
// [zh] 导入 `@/constants/backendEnums`
import { orderStatusLabel } from '@/constants/backendEnums';
// [zh] 导入 `@/utils/alipayPay`
import { launchAlipayPay } from '@/utils/alipayPay';
// [zh] 导入 `@/utils/checkout`
import { normalizeDisplayPayAmount } from '@/utils/checkout';
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`clearPaySuccess,`
  clearPaySuccess,
  // [zh] TypeScript/JS 语句：`isPaySuccessMarked,`
  isPaySuccessMarked,
  // [zh] TypeScript/JS 语句：`markPaySuccess`
  markPaySuccess
// [zh] 语句
} from '@/utils/paySuccessSession';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';

// [zh] 导出模块成员
export type PaymentPageMode = 'mobile' | 'desktop';

// [zh] 导出模块成员
export function usePaymentPage(mode: PaymentPageMode = 'mobile') {
  // [zh] 声明变量 `route`
  const route = useRoute();
  // [zh] 声明变量 `router`
  const router = useRouter();
  // [zh] 声明变量 `isMobile`
  const isMobile = mode === 'mobile';

  // [zh] 声明变量 `orderInfo`
  const orderInfo = ref<Record<string, any> | null>(null);
  // [zh] 声明变量 `payAmount`
  const payAmount = ref(0);
  // [zh] 声明变量 `payHtml`
  const payHtml = ref('');
  // [zh] 声明变量 `payLaunched`
  const payLaunched = ref(false);
  // [zh] 声明变量 `paySuccess`
  const paySuccess = ref(false);
  // [zh] 声明变量 `launching`
  const launching = ref(false);
  // [zh] 声明变量 `reopening`
  const reopening = ref(false);
  // [zh] 声明变量 `checking`
  const checking = ref(false);
  // [zh] 声明变量 `loadError`
  const loadError = ref('');
  // [zh] 声明变量 `isProcessing`
  const isProcessing = ref(false);
  // [zh] 声明变量 `pollTimer`
  let pollTimer: number | undefined;

  // [zh] 声明变量 `payOrderId`
  const payOrderId = () => String(route.params.payOrderId || '');
  // [zh] 声明变量 `payLookupId`
  const payLookupId = () => String(route.query.orderId || route.params.payOrderId || '');
  // [zh] 声明变量 `isAlipayReturn`
  const isAlipayReturn = () => !!route.query.out_trade_no;

  // [zh] 声明变量 `formatMoney`
  const formatMoney = (v: unknown) => normalizeDisplayPayAmount(v).toFixed(2);

  // [zh] 声明变量 `resolvePayAmount`
  const resolvePayAmount = (payInfoAmount: unknown, order: Record<string, any> | null) =>
    // [zh] 语句
    normalizeDisplayPayAmount(payInfoAmount ?? order?.payTotalAmount ?? order?.amount);

  // [zh] 声明变量 `isPaidStatus`
  const isPaidStatus = (status: unknown) => {
    // [zh] 声明变量 `s`
    const s = Number(status);
    // [zh] return 返回
    return s === 1 || s === 2 || s === 3;
  // [zh] 语句
  };

  // [zh] 声明变量 `showPaidSuccess`
  const showPaidSuccess = (info?: Record<string, any> | null) => {
    // [zh] 条件判断 if
    if (info) orderInfo.value = info;
    // [zh] 语句
    paySuccess.value = true;
    // [zh] 语句
    payLaunched.value = false;
    // [zh] 语句
    markPaySuccess(payOrderId());
    // [zh] 条件判断 if
    if (pollTimer) window.clearInterval(pollTimer);
  // [zh] 语句
  };

  // [zh] 声明变量 `loadOrder`
  const loadOrder = async () => {
    // [zh] 语句
    orderInfo.value = (await orderApi.getOrderInfo(payOrderId())) || null;
    // [zh] 条件判断 if
    if (orderInfo.value) {
      // [zh] 语句
      payAmount.value = resolvePayAmount(undefined, orderInfo.value);
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (orderInfo.value && isPaidStatus(orderInfo.value.orderStatus)) {
      // [zh] 语句
      showPaidSuccess(orderInfo.value);
      // [zh] return 返回
      return true;
    // [zh] 语法符号
    }
    // [zh] return 返回
    return false;
  // [zh] 语句
  };

  // [zh] 声明变量 `launchAlipay`
  const launchAlipay = (html: string) => {
    // [zh] 声明变量 `ok`
    const ok = launchAlipayPay(html, isMobile);
    // [zh] 条件判断 if
    if (!ok) {
      // [zh] TypeScript/JS 语句：`toast.warning(`
      toast.warning(
        // [zh] TypeScript/JS 语句：`isMobile ? '无法跳转支付宝，请稍后重试' : '无法打开支付窗口，请允许浏览器弹窗后重试'`
        isMobile ? '无法跳转支付宝，请稍后重试' : '无法打开支付窗口，请允许浏览器弹窗后重试'
      // [zh] 语句
      );
      // [zh] return 返回
      return false;
    // [zh] 语法符号
    }
    // [zh] 语句
    payLaunched.value = true;
    // [zh] return 返回
    return true;
  // [zh] 语句
  };

  // [zh] 声明变量 `redirectIfNotPayable`
  const redirectIfNotPayable = async () => {
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `info`
      const info = await orderApi.getOrderInfo(payOrderId());
      // [zh] 条件判断 if
      if (!info) {
        // [zh] 语句
        clearPaySuccess(payOrderId());
        // [zh] 语句
        router.replace('/orders');
        // [zh] return 返回
        return true;
      // [zh] 语法符号
      }
      // [zh] 声明变量 `status`
      const status = Number(info.orderStatus);
      // [zh] 条件判断 if
      if (isPaidStatus(status)) {
        // [zh] 语句
        showPaidSuccess(info);
        // [zh] return 返回
        return true;
      // [zh] 语法符号
      }
      // [zh] 条件判断 if
      if (status !== 0) {
        // [zh] 语句
        clearPaySuccess(payOrderId());
        // [zh] 语句
        router.replace('/orders');
        // [zh] return 返回
        return true;
      // [zh] 语法符号
      }
      // [zh] return 返回
      return false;
    // [zh] 代码块开始
    } catch {
      // [zh] 语句
      clearPaySuccess(payOrderId());
      // [zh] 语句
      router.replace('/orders');
      // [zh] return 返回
      return true;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `startPay`
  const startPay = async () => {
    // [zh] 条件判断 if
    if (!payLookupId()) return;
    // [zh] 条件判断 if
    if (await redirectIfNotPayable()) return;

    // [zh] 语句
    launching.value = true;
    // [zh] 语句
    loadError.value = '';
    // [zh] 语句
    isProcessing.value = false;
    // [zh] 代码块开始
    try {
      // [zh] 条件判断 if
      if (await loadOrder()) return;

      // [zh] 条件判断 if
      if (isAlipayReturn()) {
        // [zh] 语句
        isProcessing.value = true;
        // [zh] 语句
        return;
      // [zh] 语法符号
      }

      // [zh] 声明变量 `data`
      const data = await orderApi.getPayInfo(payLookupId());
      // [zh] 声明变量 `html`
      const html = data?.payInfo;
      // [zh] 条件判断 if
      if (!html) {
        // [zh] 语句
        loadError.value = '获取支付信息失败，请稍后重试';
        // [zh] 语句
        return;
      // [zh] 语法符号
      }
      // [zh] 语句
      payHtml.value = html;
      // [zh] 语句
      payAmount.value = resolvePayAmount(data?.amount, orderInfo.value);
      // [zh] 条件判断 if
      if (!launchAlipay(html)) {
        // [zh] 语句
        loadError.value = isMobile ? '跳转支付宝失败，请点击重试' : '请允许弹窗后点击重试';
      // [zh] 语法符号
      }
    // [zh] 代码块开始
    } catch (e: any) {
      // [zh] 条件判断 if
      if (e?.code === 901) {
        // [zh] 语句
        clearPaySuccess(payOrderId());
        // [zh] 语句
        router.replace('/login');
        // [zh] 语句
        return;
      // [zh] 语法符号
      }
      // [zh] 语句
      loadError.value = e?.info || '获取支付信息失败';
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      launching.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `reopenPayPage`
  const reopenPayPage = () => {
    // [zh] 条件判断 if
    if (!payHtml.value) {
      // [zh] 语句
      startPay();
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 语句
    reopening.value = true;
    // [zh] 代码块开始
    try {
      // [zh] 语句
      launchAlipay(payHtml.value);
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      reopening.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `goOrders`
  const goOrders = () => {
    // [zh] 语句
    clearPaySuccess(payOrderId());
    // [zh] 语句
    router.replace('/orders');
  // [zh] 语句
  };

  // [zh] 声明变量 `goHome`
  const goHome = () => {
    // [zh] 语句
    clearPaySuccess(payOrderId());
    // [zh] 语句
    router.replace('/');
  // [zh] 语句
  };

  // [zh] 声明变量 `checkPay`
  const checkPay = async () => {
    // [zh] 语句
    checking.value = true;
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `info`
      const info = await orderApi.getOrderInfo(payOrderId());
      // [zh] 语句
      orderInfo.value = info;
      // [zh] 条件判断 if
      if (isPaidStatus(info?.orderStatus)) {
        // [zh] 语句
        showPaidSuccess(info);
        // [zh] 语句
        toast.success('支付成功');
      // [zh] 代码块开始
      } else {
        // [zh] 语句
        toast.info('订单尚未支付，请完成支付宝付款后再试');
      // [zh] 语法符号
      }
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      checking.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `pollOrder`
  const pollOrder = async () => {
    // [zh] 条件判断 if
    if (paySuccess.value) return;
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `info`
      const info = await orderApi.getOrderInfo(payOrderId());
      // [zh] 语句
      orderInfo.value = info;
      // [zh] 条件判断 if
      if (isPaidStatus(info?.orderStatus)) {
        // [zh] 语句
        showPaidSuccess(info);
        // [zh] 语句
        toast.success('支付成功');
      // [zh] 语法符号
      }
    // [zh] 代码块开始
    } catch {
      
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `onPageShow`
  const onPageShow = async () => {
    // [zh] 声明变量 `currentPayOrderId`
    const currentPayOrderId = payOrderId();
    // [zh] 条件判断 if
    if (isAlipayReturn() || isPaySuccessMarked(currentPayOrderId)) {
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `info`
        const info = await orderApi.getOrderInfo(currentPayOrderId);
        // [zh] 条件判断 if
        if (info) {
          // [zh] 声明变量 `s`
          const s = Number(info.orderStatus);
          // [zh] 条件判断 if
          if (isPaidStatus(s)) {
            // [zh] 语句
            showPaidSuccess(info);
            // [zh] 语句
            return;
          // [zh] 语法符号
          }
          // [zh] 条件判断 if
          if (isAlipayReturn() && s !== 0) {
            // [zh] 语句
            clearPaySuccess(currentPayOrderId);
            // [zh] 语句
            router.replace('/orders');
            // [zh] 语句
            return;
          // [zh] 语法符号
          }
        // [zh] 语法符号
        }
      // [zh] 代码块开始
      } catch {
        
      // [zh] 语法符号
      }
      // [zh] 条件判断 if
      if (isAlipayReturn()) {
        // [zh] 语句
        clearPaySuccess(currentPayOrderId);
        // [zh] 语句
        return;
      // [zh] 语法符号
      }
      // [zh] 语句
      clearPaySuccess(currentPayOrderId);
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (!paySuccess.value && !payLaunched.value) {
      // [zh] 条件判断 if
      if (await redirectIfNotPayable()) return;
      // [zh] 语句
      startPay();
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 代码块开始
  onMounted(async () => {
    // [zh] 声明变量 `currentPayOrderId`
    const currentPayOrderId = payOrderId();

    // [zh] 条件判断 if
    if (isAlipayReturn()) {
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await loadOrder();
      // [zh] 条件判断 if
      if (orderInfo.value) {
        // [zh] 声明变量 `s`
        const s = Number(orderInfo.value.orderStatus);
        // [zh] 条件判断 if
        if (isPaidStatus(s)) {
          // [zh] 语句
          showPaidSuccess(orderInfo.value);
          // [zh] 语句
          return;
        // [zh] 语法符号
        }
        // [zh] 条件判断 if
        if (s !== 0) {
          // [zh] 语句
          clearPaySuccess(currentPayOrderId);
          // [zh] 语句
          router.replace('/orders');
          // [zh] 语句
          return;
        // [zh] 语法符号
        }
      // [zh] 语法符号
      }
      // [zh] 语句
      pollTimer = window.setInterval(pollOrder, 4000);
      // [zh] 语句
      window.addEventListener('pageshow', onPageShow);
      // [zh] 语句
      return;
    // [zh] 语法符号
    }

    // [zh] 条件判断 if
    if (isPaySuccessMarked(currentPayOrderId)) {
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `info`
        const info = await orderApi.getOrderInfo(currentPayOrderId);
        // [zh] 条件判断 if
        if (info && isPaidStatus(info.orderStatus)) {
          // [zh] 语句
          showPaidSuccess(info);
          // [zh] 语句
          return;
        // [zh] 语法符号
        }
      // [zh] 代码块开始
      } catch {
        
      // [zh] 语法符号
      }
      // [zh] 语句
      clearPaySuccess(currentPayOrderId);
    // [zh] 语法符号
    }

    // [zh] 条件判断 if
    if (await redirectIfNotPayable()) return;

    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await startPay();
    // [zh] 语句
    pollTimer = window.setInterval(pollOrder, 4000);
    // [zh] 语句
    window.addEventListener('pageshow', onPageShow);
  // [zh] 语句
  });

  // [zh] 代码块开始
  onUnmounted(() => {
    // [zh] 条件判断 if
    if (pollTimer) window.clearInterval(pollTimer);
    // [zh] 语句
    window.removeEventListener('pageshow', onPageShow);
  // [zh] 语句
  });

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`route,`
    route,
    // [zh] TypeScript/JS 语句：`orderInfo,`
    orderInfo,
    // [zh] TypeScript/JS 语句：`payAmount,`
    payAmount,
    // [zh] TypeScript/JS 语句：`payLaunched,`
    payLaunched,
    // [zh] TypeScript/JS 语句：`paySuccess,`
    paySuccess,
    // [zh] TypeScript/JS 语句：`launching,`
    launching,
    // [zh] TypeScript/JS 语句：`reopening,`
    reopening,
    // [zh] TypeScript/JS 语句：`checking,`
    checking,
    // [zh] TypeScript/JS 语句：`loadError,`
    loadError,
    // [zh] TypeScript/JS 语句：`isProcessing,`
    isProcessing,
    // [zh] TypeScript/JS 语句：`payOrderId,`
    payOrderId,
    // [zh] TypeScript/JS 语句：`formatMoney,`
    formatMoney,
    // [zh] TypeScript/JS 语句：`orderStatusLabel,`
    orderStatusLabel,
    // [zh] TypeScript/JS 语句：`startPay,`
    startPay,
    // [zh] TypeScript/JS 语句：`reopenPayPage,`
    reopenPayPage,
    // [zh] TypeScript/JS 语句：`goOrders,`
    goOrders,
    // [zh] TypeScript/JS 语句：`goHome,`
    goHome,
    // [zh] TypeScript/JS 语句：`checkPay`
    checkPay
  // [zh] 语句
  };
// [zh] 语法符号
}
