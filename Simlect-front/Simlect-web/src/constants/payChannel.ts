
export const PAY_METHOD_ALIPAY_PC = 'alipay_pc';
export const PAY_METHOD_ALIPAY_WAP = 'alipay_wap';

export type AlipayPayMethod = typeof PAY_METHOD_ALIPAY_PC | typeof PAY_METHOD_ALIPAY_WAP;

export const defaultAlipayPayMethod = (isMobile: boolean): AlipayPayMethod =>
  isMobile ? PAY_METHOD_ALIPAY_WAP : PAY_METHOD_ALIPAY_PC;
