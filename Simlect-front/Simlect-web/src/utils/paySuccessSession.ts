// [zh] 声明变量 `PAY_SUCCESS_PREFIX`
const PAY_SUCCESS_PREFIX = 'pay_success:';

// [zh] 导出模块成员
export function paySuccessKey(payOrderId: string) {
  // [zh] return 返回
  return `${PAY_SUCCESS_PREFIX}${payOrderId}`;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function markPaySuccess(payOrderId: string) {
  // [zh] 条件判断 if
  if (!payOrderId) return;
  // [zh] 语句
  sessionStorage.setItem(paySuccessKey(payOrderId), '1');
// [zh] 语法符号
}

// [zh] 导出模块成员
export function isPaySuccessMarked(payOrderId: string) {
  // [zh] 条件判断 if
  if (!payOrderId) return false;
  // [zh] return 返回
  return sessionStorage.getItem(paySuccessKey(payOrderId)) === '1';
// [zh] 语法符号
}

// [zh] 导出模块成员
export function clearPaySuccess(payOrderId: string) {
  // [zh] 条件判断 if
  if (!payOrderId) return;
  // [zh] 语句
  sessionStorage.removeItem(paySuccessKey(payOrderId));
// [zh] 语法符号
}
