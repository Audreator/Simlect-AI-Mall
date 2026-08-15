
// PC 端支付宝收银台：在新窗口构造安全表单提交（仅白名单字段，action 域名精确白名单校验）
import { sanitizeAlipayAction } from './alipaySecurity';

export function openAlipayPagePay(payInfoHtml: string): boolean {
  if (!payInfoHtml?.trim()) return false;
  const win = window.open('', '_blank');
  if (!win) return false;
  // 复用安全解析：把 HTML 交给当前文档解析（DOMParser 不执行脚本），
  // 再在新窗口文档中构造表单提交。
  const parsed = (() => {
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
  })();
  if (!parsed) {
    win.close();
    return false;
  }
  const form = win.document.createElement('form');
  form.method = parsed.method;
  form.action = parsed.action;
  parsed.fields.forEach(({ name, value }) => {
    const input = win.document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
  });
  win.document.body.appendChild(form);
  form.submit();
  return true;
}
