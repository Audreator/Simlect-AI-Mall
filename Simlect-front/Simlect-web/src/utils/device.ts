
// [zh] 导出模块成员
export const MOBILE_MAX_WIDTH = 767;

// [zh] 导出模块成员
export type DevicePlatform = 'mobile' | 'desktop';

// [zh] 声明变量 `FORCE_VIEW_KEY`
const FORCE_VIEW_KEY = 'eshop_view';


// [zh] 声明变量 `PHONE_UA_RE`
const PHONE_UA_RE =
  // [zh] 语句
  /iPhone|iPod|Android\s+.*Mobile|webOS|BlackBerry|IEMobile|Opera Mini/i;


// [zh] 导出模块成员
export const isMobileUserAgent = (): boolean => {
  // [zh] 条件判断 if
  if (typeof navigator === 'undefined') return false;
  // [zh] 声明变量 `ua`
  const ua = navigator.userAgent || '';
  // [zh] 条件判断 if
  if (PHONE_UA_RE.test(ua)) return true;
  // [zh] 条件判断 if
  if (/iPad/i.test(ua)) return true;
  
  // [zh] 条件判断 if
  if (/Android/i.test(ua) && !/Mobile/i.test(ua)) return true;
  // [zh] return 返回
  return false;
// [zh] 语句
};


// [zh] 导出模块成员
export const readForcedPlatform = (): DevicePlatform | null => {
  // [zh] 条件判断 if
  if (typeof window === 'undefined') return null;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `q`
    const q = new URLSearchParams(window.location.search).get('view');
    // [zh] 条件判断 if
    if (q === 'mobile' || q === 'desktop') {
      // [zh] 语句
      localStorage.setItem(FORCE_VIEW_KEY, q);
      // [zh] return 返回
      return q;
    // [zh] 语法符号
    }
    // [zh] 声明变量 `saved`
    const saved = localStorage.getItem(FORCE_VIEW_KEY);
    
    // [zh] 条件判断 if
    if (saved === 'mobile' && typeof window !== 'undefined' && window.innerWidth > MOBILE_MAX_WIDTH) {
      // [zh] 语句
      localStorage.removeItem(FORCE_VIEW_KEY);
      // [zh] return 返回
      return 'desktop';
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (saved === 'mobile' || saved === 'desktop') return saved;
  // [zh] 代码块开始
  } catch {
    
  // [zh] 语法符号
  }
  // [zh] return 返回
  return null;
// [zh] 语句
};


// [zh] 导出模块成员
export const detectDevicePlatform = (): DevicePlatform => {
  // [zh] 条件判断 if
  if (typeof window === 'undefined') return 'desktop';
  // [zh] 声明变量 `forced`
  const forced = readForcedPlatform();
  // [zh] 条件判断 if
  if (forced) return forced;
  // [zh] 条件判断 if
  if (isMobileUserAgent()) return 'mobile';
  
  // [zh] 条件判断 if
  if (typeof window !== 'undefined' && window.innerWidth > MOBILE_MAX_WIDTH) {
    // [zh] return 返回
    return 'desktop';
  // [zh] 语法符号
  }
  // [zh] return 返回
  return 'desktop';
// [zh] 语句
};
