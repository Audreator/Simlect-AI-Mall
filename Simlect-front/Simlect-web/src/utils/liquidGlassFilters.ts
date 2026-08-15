// [zh] 声明变量 `FILTER_ROOT_ID`
const FILTER_ROOT_ID = 'liquid-glass-filters-root';
// [zh] 声明变量 `STYLE_ROOT_ID`
const STYLE_ROOT_ID = 'liquid-glass-runtime-styles';

// [zh] 声明变量 `FILTER_SVG`
const FILTER_SVG = `<svg xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false" style="position:absolute;width:0;height:0;overflow:hidden;pointer-events:none">
  // [zh] TypeScript/JS 语句：`<filter id="liquid-glass-strong" x="0%" y="0%" width="1`
  <filter id="liquid-glass-strong" x="0%" y="0%" width="100%" height="100%" filterUnits="objectBoundingBox" color-interpolation-filters="sRGB">
    // [zh] TypeScript/JS 语句：`<feTurbulence type="fractalNoise" baseFrequency="0.01 0`
    <feTurbulence type="fractalNoise" baseFrequency="0.01 0.01" numOctaves="1" seed="5" result="turbulence" />
    // [zh] TypeScript/JS 语句：`<feComponentTransfer in="turbulence" result="mapped">`
    <feComponentTransfer in="turbulence" result="mapped">
      // [zh] TypeScript/JS 语句：`<feFuncR type="gamma" amplitude="1" exponent="10" offse`
      <feFuncR type="gamma" amplitude="1" exponent="10" offset="0.5" />
      // [zh] TypeScript/JS 语句：`<feFuncG type="gamma" amplitude="0" exponent="1" offset`
      <feFuncG type="gamma" amplitude="0" exponent="1" offset="0" />
      // [zh] TypeScript/JS 语句：`<feFuncB type="gamma" amplitude="0" exponent="1" offset`
      <feFuncB type="gamma" amplitude="0" exponent="1" offset="0.5" />
    // [zh] TypeScript/JS 语句：`</feComponentTransfer>`
    </feComponentTransfer>
    // [zh] TypeScript/JS 语句：`<feGaussianBlur in="turbulence" stdDeviation="3" result`
    <feGaussianBlur in="turbulence" stdDeviation="3" result="softMap" />
    // [zh] TypeScript/JS 语句：`<feDisplacementMap in="SourceGraphic" in2="softMap" sca`
    <feDisplacementMap in="SourceGraphic" in2="softMap" scale="150" xChannelSelector="R" yChannelSelector="G" />
  // [zh] TypeScript/JS 语句：`</filter>`
  </filter>
  // [zh] TypeScript/JS 语句：`<filter id="liquid-glass-medium" x="0%" y="0%" width="1`
  <filter id="liquid-glass-medium" x="0%" y="0%" width="100%" height="100%" filterUnits="objectBoundingBox" color-interpolation-filters="sRGB">
    // [zh] TypeScript/JS 语句：`<feTurbulence type="fractalNoise" baseFrequency="0.01 0`
    <feTurbulence type="fractalNoise" baseFrequency="0.01 0.01" numOctaves="1" seed="5" result="turbulence" />
    // [zh] TypeScript/JS 语句：`<feComponentTransfer in="turbulence" result="mapped">`
    <feComponentTransfer in="turbulence" result="mapped">
      // [zh] TypeScript/JS 语句：`<feFuncR type="gamma" amplitude="1" exponent="10" offse`
      <feFuncR type="gamma" amplitude="1" exponent="10" offset="0.5" />
      // [zh] TypeScript/JS 语句：`<feFuncG type="gamma" amplitude="0" exponent="1" offset`
      <feFuncG type="gamma" amplitude="0" exponent="1" offset="0" />
      // [zh] TypeScript/JS 语句：`<feFuncB type="gamma" amplitude="0" exponent="1" offset`
      <feFuncB type="gamma" amplitude="0" exponent="1" offset="0.5" />
    // [zh] TypeScript/JS 语句：`</feComponentTransfer>`
    </feComponentTransfer>
    // [zh] TypeScript/JS 语句：`<feGaussianBlur in="turbulence" stdDeviation="2.5" resu`
    <feGaussianBlur in="turbulence" stdDeviation="2.5" result="softMap" />
    // [zh] TypeScript/JS 语句：`<feDisplacementMap in="SourceGraphic" in2="softMap" sca`
    <feDisplacementMap in="SourceGraphic" in2="softMap" scale="72" xChannelSelector="R" yChannelSelector="G" />
  // [zh] TypeScript/JS 语句：`</filter>`
  </filter>
  // [zh] TypeScript/JS 语句：`<filter id="liquid-glass-subtle" x="0%" y="0%" width="1`
  <filter id="liquid-glass-subtle" x="0%" y="0%" width="100%" height="100%" filterUnits="objectBoundingBox" color-interpolation-filters="sRGB">
    // [zh] TypeScript/JS 语句：`<feTurbulence type="fractalNoise" baseFrequency="0.01 0`
    <feTurbulence type="fractalNoise" baseFrequency="0.01 0.01" numOctaves="1" seed="5" result="turbulence" />
    // [zh] TypeScript/JS 语句：`<feComponentTransfer in="turbulence" result="mapped">`
    <feComponentTransfer in="turbulence" result="mapped">
      // [zh] TypeScript/JS 语句：`<feFuncR type="gamma" amplitude="1" exponent="10" offse`
      <feFuncR type="gamma" amplitude="1" exponent="10" offset="0.5" />
      // [zh] TypeScript/JS 语句：`<feFuncG type="gamma" amplitude="0" exponent="1" offset`
      <feFuncG type="gamma" amplitude="0" exponent="1" offset="0" />
      // [zh] TypeScript/JS 语句：`<feFuncB type="gamma" amplitude="0" exponent="1" offset`
      <feFuncB type="gamma" amplitude="0" exponent="1" offset="0.5" />
    // [zh] TypeScript/JS 语句：`</feComponentTransfer>`
    </feComponentTransfer>
    // [zh] TypeScript/JS 语句：`<feGaussianBlur in="turbulence" stdDeviation="2" result`
    <feGaussianBlur in="turbulence" stdDeviation="2" result="softMap" />
    // [zh] TypeScript/JS 语句：`<feDisplacementMap in="SourceGraphic" in2="softMap" sca`
    <feDisplacementMap in="SourceGraphic" in2="softMap" scale="36" xChannelSelector="R" yChannelSelector="G" />
  // [zh] TypeScript/JS 语句：`</filter>`
  </filter>
// [zh] 语句
</svg>`;


// [zh] 声明变量 `RUNTIME_CSS`
const RUNTIME_CSS = `
// [zh] 代码块开始
.liquid-glass-surface--strong .liquid-glass-surface__effect {
  // [zh] 语句
  backdrop-filter: blur(3px);
  // [zh] 语句
  -webkit-backdrop-filter: blur(3px);
  // [zh] 语句
  filter: url(#liquid-glass-strong);
  // [zh] 语句
  -webkit-filter: url(#liquid-glass-strong);
// [zh] 语法符号
}
// [zh] 代码块开始
.liquid-glass-surface--strong.liquid-glass-surface--active .liquid-glass-surface__effect {
  // [zh] 语句
  backdrop-filter: blur(4px);
  // [zh] 语句
  -webkit-backdrop-filter: blur(4px);
  // [zh] 语句
  filter: url(#liquid-glass-strong);
  // [zh] 语句
  -webkit-filter: url(#liquid-glass-strong);
// [zh] 语法符号
}
// [zh] 代码块开始
.liquid-glass-surface--medium .liquid-glass-surface__effect {
  // [zh] 语句
  backdrop-filter: blur(6px) saturate(1.08);
  // [zh] 语句
  -webkit-backdrop-filter: blur(6px) saturate(1.08);
  // [zh] 语句
  filter: url(#liquid-glass-medium);
  // [zh] 语句
  -webkit-filter: url(#liquid-glass-medium);
// [zh] 语法符号
}
// [zh] 代码块开始
.liquid-glass-surface--subtle .liquid-glass-surface__effect {
  // [zh] 语句
  backdrop-filter: blur(8px) saturate(1.05);
  // [zh] 语句
  -webkit-backdrop-filter: blur(8px) saturate(1.05);
  // [zh] 语句
  filter: url(#liquid-glass-subtle);
  // [zh] 语句
  -webkit-filter: url(#liquid-glass-subtle);
// [zh] 语法符号
}
// [zh] 语句
`;

// [zh] 导出模块成员
export type LiquidGlassIntensity = 'strong' | 'medium' | 'subtle';

// [zh] 声明变量 `EFFECT_STYLES`
const EFFECT_STYLES: Record<LiquidGlassIntensity, { backdrop: string; filter: string }> = {
  // [zh] TypeScript/JS 语句：`strong: { backdrop: 'blur(3px)', filter: 'url(#liquid-g`
  strong: { backdrop: 'blur(3px)', filter: 'url(#liquid-glass-strong)' },
  // [zh] TypeScript/JS 语句：`medium: { backdrop: 'blur(6px) saturate(1.08)', filter:`
  medium: { backdrop: 'blur(6px) saturate(1.08)', filter: 'url(#liquid-glass-medium)' },
  // [zh] TypeScript/JS 语句：`subtle: { backdrop: 'blur(8px) saturate(1.05)', filter:`
  subtle: { backdrop: 'blur(8px) saturate(1.05)', filter: 'url(#liquid-glass-subtle)' }
// [zh] 语句
};

// [zh] 声明变量 `ACTIVE_STRONG_BACKDROP`
const ACTIVE_STRONG_BACKDROP = 'blur(4px)';

// [zh] 导出模块成员
export function applyLiquidGlassEffect(el: HTMLElement, intensity: LiquidGlassIntensity, active = false) {
  // [zh] 声明变量 `styles`
  const styles = EFFECT_STYLES[intensity];
  // [zh] 声明变量 `backdrop`
  const backdrop = intensity === 'strong' && active ? ACTIVE_STRONG_BACKDROP : styles.backdrop;
  // [zh] 语句
  el.style.setProperty('backdrop-filter', backdrop);
  // [zh] 语句
  el.style.setProperty('-webkit-backdrop-filter', backdrop);
  // [zh] 语句
  el.style.setProperty('filter', styles.filter);
  // [zh] 语句
  el.style.setProperty('-webkit-filter', styles.filter);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function ensureLiquidGlassFilters() {
  // [zh] 条件判断 if
  if (typeof document === 'undefined') return;

  // [zh] 条件判断 if
  if (!document.getElementById(STYLE_ROOT_ID)) {
    // [zh] 声明变量 `style`
    const style = document.createElement('style');
    // [zh] 语句
    style.id = STYLE_ROOT_ID;
    // [zh] 语句
    style.textContent = RUNTIME_CSS;
    // [zh] 语句
    document.head.appendChild(style);
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (!document.getElementById('liquid-glass-strong') && !document.getElementById(FILTER_ROOT_ID)) {
    // [zh] 声明变量 `host`
    const host = document.createElement('div');
    // [zh] 语句
    host.id = FILTER_ROOT_ID;
    // [zh] 语句
    host.innerHTML = FILTER_SVG;
    // [zh] 语句
    document.body.insertBefore(host, document.body.firstChild);
  // [zh] 语法符号
  }
// [zh] 语法符号
}
