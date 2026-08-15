// 支付宝支付 HTML 安全解析：不执行后端返回的任意脚本，
// 仅提取 form 的 action/method/input 白名单，action 域名必须命中支付宝官方白名单。
import { openAlipayPagePay } from './alipayPagePay';
import { sanitizeAlipayAction } from './alipaySecurity';

interface ParsedPayForm {
  action: string;
  method: string;
  fields: { name: string; value: string }[];
}

function parsePayForm(payInfoHtml: string): ParsedPayForm | null {
  if (!payInfoHtml?.trim()) return null;
  const doc = new DOMParser().parseFromString(payInfoHtml, 'text/html');
  const srcForm =
    (doc.querySelector('form[name="punchout_form"]') as HTMLFormElement | null) ||
    (doc.querySelector('form') as HTMLFormElement | null);
  if (!srcForm) return null;
  const action = sanitizeAlipayAction(srcForm.getAttribute('action'));
  // 仅允许提交到支付宝官方域名（精确 hostname 白名单，防伪冒域名）
  if (!action) return null;
  const method = (srcForm.getAttribute('method') || 'POST').toUpperCase();
  const fields: { name: string; value: string }[] = [];
  srcForm.querySelectorAll('input').forEach((input) => {
    const name = input.getAttribute('name');
    if (!name) return;
    fields.push({ name, value: input.getAttribute('value') || '' });
  });
  return { action, method, fields };
}

// 在指定 document 下构造提交表单（WAP 用当前文档，PC 用新窗口文档）
function buildForm(doc: Document, parsed: ParsedPayForm): HTMLFormElement {
  const form = doc.createElement('form');
  form.method = parsed.method;
  form.action = parsed.action;
  form.style.display = 'none';
  parsed.fields.forEach(({ name, value }) => {
    const input = doc.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
  });
  doc.body.appendChild(form);
  return form;
}

export function openAlipayWapPay(payInfoHtml: string): boolean {
  const parsed = parsePayForm(payInfoHtml);
  if (!parsed) return false;
  const form = buildForm(document, parsed);
  form.submit();
  form.remove();
  return true;
}

export function launchAlipayPay(payInfoHtml: string, isMobile: boolean): boolean {
  return isMobile ? openAlipayWapPay(payInfoHtml) : openAlipayPagePay(payInfoHtml);
}
