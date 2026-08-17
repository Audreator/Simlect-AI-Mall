const PAY_SUCCESS_PREFIX = 'pay_success:';

export function paySuccessKey(payOrderId: string) {
  return `${PAY_SUCCESS_PREFIX}${payOrderId}`;
}

export function markPaySuccess(payOrderId: string) {
  if (!payOrderId) return;
  sessionStorage.setItem(paySuccessKey(payOrderId), '1');
}

export function isPaySuccessMarked(payOrderId: string) {
  if (!payOrderId) return false;
  return sessionStorage.getItem(paySuccessKey(payOrderId)) === '1';
}

export function clearPaySuccess(payOrderId: string) {
  if (!payOrderId) return;
  sessionStorage.removeItem(paySuccessKey(payOrderId));
}
