
// [zh] 导出模块成员
export const PAY_METHOD_ALIPAY_PC = 'alipay_pc';
// [zh] 导出模块成员
export const PAY_METHOD_ALIPAY_WAP = 'alipay_wap';

// [zh] 导出模块成员
export type AlipayPayMethod = typeof PAY_METHOD_ALIPAY_PC | typeof PAY_METHOD_ALIPAY_WAP;

// [zh] 导出模块成员
export const defaultAlipayPayMethod = (isMobile: boolean): AlipayPayMethod =>
  // [zh] 语句
  isMobile ? PAY_METHOD_ALIPAY_WAP : PAY_METHOD_ALIPAY_PC;
