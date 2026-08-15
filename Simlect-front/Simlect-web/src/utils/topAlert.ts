// [zh] 导出模块成员
export function showTopAlert(message: string) {
  // [zh] 声明变量 `overlay`
  const overlay = document.createElement('div');
  // [zh] TypeScript/JS 语句：`overlay.style.cssText =`
  overlay.style.cssText =
    // [zh] 语句
    'position:fixed;inset:0;z-index:999999;background:rgba(0,0,0,.35);display:flex;align-items:center;justify-content:center;';
  // [zh] 声明变量 `box`
  const box = document.createElement('div');
  // [zh] TypeScript/JS 语句：`box.style.cssText =`
  box.style.cssText =
    // [zh] 语句
    'background:#fff;border-radius:12px;padding:32px 28px 24px;min-width:260px;max-width:360px;text-align:center;box-shadow:0 8px 30px rgba(0,0,0,.15);';
  // [zh] 声明变量 `p`
  const p = document.createElement('p');
  // [zh] 语句
  p.style.cssText = 'margin:0 0 20px;font-size:15px;line-height:1.5;color:#333333;';
  // [zh] 语句
  p.textContent = message;
  // [zh] 声明变量 `btn`
  const btn = document.createElement('button');
  // [zh] 语句
  btn.textContent = '知道了';
  // [zh] TypeScript/JS 语句：`btn.style.cssText =`
  btn.style.cssText =
    // [zh] 语句
    'padding:8px 28px;border:none;border-radius:8px;background:#165dff;color:#fff;font-size:14px;cursor:pointer;';
  // [zh] 语句
  btn.onclick = () => document.body.removeChild(overlay);
  // [zh] 语句
  box.appendChild(p);
  // [zh] 语句
  box.appendChild(btn);
  // [zh] 语句
  overlay.appendChild(box);
  // [zh] 语句
  document.body.appendChild(overlay);
// [zh] 语法符号
}
