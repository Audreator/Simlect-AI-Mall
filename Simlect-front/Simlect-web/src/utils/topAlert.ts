export function showTopAlert(message: string) {
  const overlay = document.createElement('div');
  overlay.style.cssText =
    'position:fixed;inset:0;z-index:999999;background:rgba(0,0,0,.35);display:flex;align-items:center;justify-content:center;';
  const box = document.createElement('div');
  box.style.cssText =
    'background:#fff;border-radius:12px;padding:32px 28px 24px;min-width:260px;max-width:360px;text-align:center;box-shadow:0 8px 30px rgba(0,0,0,.15);';
  const p = document.createElement('p');
  p.style.cssText = 'margin:0 0 20px;font-size:15px;line-height:1.5;color:#333333;';
  p.textContent = message;
  const btn = document.createElement('button');
  btn.textContent = '知道了';
  btn.style.cssText =
    'padding:8px 28px;border:none;border-radius:8px;background:#165dff;color:#fff;font-size:14px;cursor:pointer;';
  btn.onclick = () => document.body.removeChild(overlay);
  box.appendChild(p);
  box.appendChild(btn);
  overlay.appendChild(box);
  document.body.appendChild(overlay);
}
