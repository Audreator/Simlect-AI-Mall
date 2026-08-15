// 支付宝支付表单安全校验（alipayPay / alipayPagePay 共用，避免循环依赖）

/**
 * 精确 hostname 白名单校验（防伪冒域名）：
 * 仅允许支付宝官方域名或其以点分隔的子域。
 */
const ALIPAY_ALLOWED_HOSTS = new Set([
  'alipay.com',
  'alipaydev.com',
  'openapi.alipay.com',
  'openapi.alipaydev.com',
  'render.alipay.com',
  'render.alipaydev.com',
]);

export function isAlipayHost(hostname: string | null | undefined): boolean {
  if (!hostname) return false;
  const host = hostname.toLowerCase();
  if (ALIPAY_ALLOWED_HOSTS.has(host)) return true;
  // 子域必须以点分隔且以官方域名结尾（防 evil-alipay.com / notalipay.com）
  return ALIPAY_ALLOWED_HOSTS.has(host) === false && host.endsWith('.alipay.com')
    || ALIPAY_ALLOWED_HOSTS.has(host) === false && host.endsWith('.alipaydev.com');
}

/**
 * 解析并校验支付表单提交地址：必须是 https 且 hostname 命中支付宝官方白名单。
 * 返回规范化 action；非法返回 null。
 */
export function sanitizeAlipayAction(action: string | null | undefined): string | null {
  if (!action) return null;
  const trimmed = action.trim();
  let url: URL;
  try {
    url = new URL(trimmed);
  } catch {
    return null;
  }
  if (url.protocol !== 'https:') return null;
  // 拒绝 userinfo（https://x@alipay.com/：会向支付宝发送可控 Basic 头）
  if (url.username || url.password) return null;
  // 拒绝非标准端口（仅允许 443 默认端口）
  if (url.port && url.port !== '443') return null;
  if (!isAlipayHost(url.hostname)) return null;
  return url.toString();
}
