
export const MOBILE_MAX_WIDTH = 767;

export type DevicePlatform = 'mobile' | 'desktop';

const FORCE_VIEW_KEY = 'eshop_view';


const PHONE_UA_RE =
  /iPhone|iPod|Android\s+.*Mobile|webOS|BlackBerry|IEMobile|Opera Mini/i;


export const isMobileUserAgent = (): boolean => {
  if (typeof navigator === 'undefined') return false;
  const ua = navigator.userAgent || '';
  if (PHONE_UA_RE.test(ua)) return true;
  if (/iPad/i.test(ua)) return true;
  
  if (/Android/i.test(ua) && !/Mobile/i.test(ua)) return true;
  return false;
};


export const readForcedPlatform = (): DevicePlatform | null => {
  if (typeof window === 'undefined') return null;
  try {
    const q = new URLSearchParams(window.location.search).get('view');
    if (q === 'mobile' || q === 'desktop') {
      localStorage.setItem(FORCE_VIEW_KEY, q);
      return q;
    }
    const saved = localStorage.getItem(FORCE_VIEW_KEY);
    
    if (saved === 'mobile' && typeof window !== 'undefined' && window.innerWidth > MOBILE_MAX_WIDTH) {
      localStorage.removeItem(FORCE_VIEW_KEY);
      return 'desktop';
    }
    if (saved === 'mobile' || saved === 'desktop') return saved;
  } catch {
    
  }
  return null;
};


export const detectDevicePlatform = (): DevicePlatform => {
  if (typeof window === 'undefined') return 'desktop';
  const forced = readForcedPlatform();
  if (forced) return forced;
  if (isMobileUserAgent()) return 'mobile';
  
  if (typeof window !== 'undefined' && window.innerWidth > MOBILE_MAX_WIDTH) {
    return 'desktop';
  }
  return 'desktop';
};
