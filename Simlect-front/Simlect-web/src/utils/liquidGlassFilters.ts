const FILTER_ROOT_ID = 'liquid-glass-filters-root';
const STYLE_ROOT_ID = 'liquid-glass-runtime-styles';

const FILTER_SVG = `<svg xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false" style="position:absolute;width:0;height:0;overflow:hidden;pointer-events:none">
  <filter id="liquid-glass-strong" x="0%" y="0%" width="100%" height="100%" filterUnits="objectBoundingBox" color-interpolation-filters="sRGB">
    <feTurbulence type="fractalNoise" baseFrequency="0.01 0.01" numOctaves="1" seed="5" result="turbulence" />
    <feComponentTransfer in="turbulence" result="mapped">
      <feFuncR type="gamma" amplitude="1" exponent="10" offset="0.5" />
      <feFuncG type="gamma" amplitude="0" exponent="1" offset="0" />
      <feFuncB type="gamma" amplitude="0" exponent="1" offset="0.5" />
    </feComponentTransfer>
    <feGaussianBlur in="turbulence" stdDeviation="3" result="softMap" />
    <feDisplacementMap in="SourceGraphic" in2="softMap" scale="150" xChannelSelector="R" yChannelSelector="G" />
  </filter>
  <filter id="liquid-glass-medium" x="0%" y="0%" width="100%" height="100%" filterUnits="objectBoundingBox" color-interpolation-filters="sRGB">
    <feTurbulence type="fractalNoise" baseFrequency="0.01 0.01" numOctaves="1" seed="5" result="turbulence" />
    <feComponentTransfer in="turbulence" result="mapped">
      <feFuncR type="gamma" amplitude="1" exponent="10" offset="0.5" />
      <feFuncG type="gamma" amplitude="0" exponent="1" offset="0" />
      <feFuncB type="gamma" amplitude="0" exponent="1" offset="0.5" />
    </feComponentTransfer>
    <feGaussianBlur in="turbulence" stdDeviation="2.5" result="softMap" />
    <feDisplacementMap in="SourceGraphic" in2="softMap" scale="72" xChannelSelector="R" yChannelSelector="G" />
  </filter>
  <filter id="liquid-glass-subtle" x="0%" y="0%" width="100%" height="100%" filterUnits="objectBoundingBox" color-interpolation-filters="sRGB">
    <feTurbulence type="fractalNoise" baseFrequency="0.01 0.01" numOctaves="1" seed="5" result="turbulence" />
    <feComponentTransfer in="turbulence" result="mapped">
      <feFuncR type="gamma" amplitude="1" exponent="10" offset="0.5" />
      <feFuncG type="gamma" amplitude="0" exponent="1" offset="0" />
      <feFuncB type="gamma" amplitude="0" exponent="1" offset="0.5" />
    </feComponentTransfer>
    <feGaussianBlur in="turbulence" stdDeviation="2" result="softMap" />
    <feDisplacementMap in="SourceGraphic" in2="softMap" scale="36" xChannelSelector="R" yChannelSelector="G" />
  </filter>
</svg>`;


const RUNTIME_CSS = `
.liquid-glass-surface--strong .liquid-glass-surface__effect {
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  filter: url(#liquid-glass-strong);
  -webkit-filter: url(#liquid-glass-strong);
}
.liquid-glass-surface--strong.liquid-glass-surface--active .liquid-glass-surface__effect {
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  filter: url(#liquid-glass-strong);
  -webkit-filter: url(#liquid-glass-strong);
}
.liquid-glass-surface--medium .liquid-glass-surface__effect {
  backdrop-filter: blur(6px) saturate(1.08);
  -webkit-backdrop-filter: blur(6px) saturate(1.08);
  filter: url(#liquid-glass-medium);
  -webkit-filter: url(#liquid-glass-medium);
}
.liquid-glass-surface--subtle .liquid-glass-surface__effect {
  backdrop-filter: blur(8px) saturate(1.05);
  -webkit-backdrop-filter: blur(8px) saturate(1.05);
  filter: url(#liquid-glass-subtle);
  -webkit-filter: url(#liquid-glass-subtle);
}
`;

export type LiquidGlassIntensity = 'strong' | 'medium' | 'subtle';

const EFFECT_STYLES: Record<LiquidGlassIntensity, { backdrop: string; filter: string }> = {
  strong: { backdrop: 'blur(3px)', filter: 'url(#liquid-glass-strong)' },
  medium: { backdrop: 'blur(6px) saturate(1.08)', filter: 'url(#liquid-glass-medium)' },
  subtle: { backdrop: 'blur(8px) saturate(1.05)', filter: 'url(#liquid-glass-subtle)' }
};

const ACTIVE_STRONG_BACKDROP = 'blur(4px)';

export function applyLiquidGlassEffect(el: HTMLElement, intensity: LiquidGlassIntensity, active = false) {
  const styles = EFFECT_STYLES[intensity];
  const backdrop = intensity === 'strong' && active ? ACTIVE_STRONG_BACKDROP : styles.backdrop;
  el.style.setProperty('backdrop-filter', backdrop);
  el.style.setProperty('-webkit-backdrop-filter', backdrop);
  el.style.setProperty('filter', styles.filter);
  el.style.setProperty('-webkit-filter', styles.filter);
}

export function ensureLiquidGlassFilters() {
  if (typeof document === 'undefined') return;

  if (!document.getElementById(STYLE_ROOT_ID)) {
    const style = document.createElement('style');
    style.id = STYLE_ROOT_ID;
    style.textContent = RUNTIME_CSS;
    document.head.appendChild(style);
  }

  if (!document.getElementById('liquid-glass-strong') && !document.getElementById(FILTER_ROOT_ID)) {
    const host = document.createElement('div');
    host.id = FILTER_ROOT_ID;
    host.innerHTML = FILTER_SVG;
    document.body.insertBefore(host, document.body.firstChild);
  }
}
