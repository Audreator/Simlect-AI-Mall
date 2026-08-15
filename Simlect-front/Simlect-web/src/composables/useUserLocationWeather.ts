// [zh] 导入 `vue`
import { ref, shallowRef } from 'vue';
// [zh] 导入 `@/api/location`
import { locationApi, type LocationPayload } from '@/api/location';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';

// [zh] 声明变量 `CACHE_KEY`
const CACHE_KEY = 'simlect_user_location';

// [zh] 声明变量 `shared`
let shared: ReturnType<typeof createState> | null = null;

// [zh] 声明变量 `readCache`
const readCache = (): LocationPayload | null => {
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `raw`
    const raw = sessionStorage.getItem(CACHE_KEY);
    // [zh] return 返回
    return raw ? (JSON.parse(raw) as LocationPayload) : null;
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return null;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `writeCache`
const writeCache = (data: LocationPayload) => {
  // [zh] 代码块开始
  try {
    // [zh] 语句
    sessionStorage.setItem(CACHE_KEY, JSON.stringify(data));
  // [zh] 代码块开始
  } catch {

  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `getBrowserPosition`
const getBrowserPosition = (): Promise<GeolocationPosition> =>
  // [zh] 代码块开始
  new Promise((resolve, reject) => {
    // [zh] 条件判断 if
    if (!navigator.geolocation) {
      // [zh] 语句
      reject(new Error('当前浏览器不支持定位'));
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 代码块开始
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      // [zh] TypeScript/JS 语句：`enableHighAccuracy: true,`
      enableHighAccuracy: true,
      // [zh] TypeScript/JS 语句：`timeout: 12000,`
      timeout: 12000,
      // [zh] TypeScript/JS 语句：`maximumAge: 5 * 60 * 1000`
      maximumAge: 5 * 60 * 1000
    // [zh] 语句
    });
  // [zh] 语句
  });

// [zh] 函数 `createState`
function createState() {
  // [zh] 声明变量 `authStore`
  const authStore = useAuthStore();
  // [zh] 声明变量 `data`
  const data = shallowRef<LocationPayload | null>(readCache());
  // [zh] 声明变量 `loading`
  const loading = ref(false);
  // [zh] 声明变量 `error`
  const error = ref('');

  // [zh] 声明变量 `fetchByCoords`
  const fetchByCoords = async (lat: number, lng: number) => {
    // [zh] 条件判断 if
    if (!authStore.isLoggedIn) {
      // [zh] 语句
      error.value = '请先登录后再获取当前位置';
      // [zh] 语句
      toast.warning(error.value);
      // [zh] return 返回
      return null;
    // [zh] 语法符号
    }
    // [zh] 语句
    loading.value = true;
    // [zh] 语句
    error.value = '';
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `payload`
      const payload = await locationApi.sync(lat, lng);
      // [zh] 条件判断 if
      if (payload && typeof payload === 'object') {
        // [zh] 语句
        data.value = payload as LocationPayload;
        // [zh] 语句
        writeCache(data.value);
      // [zh] 语法符号
      }
      // [zh] return 返回
      return data.value;
    // [zh] 代码块开始
    } catch (e: any) {
      // [zh] 语句
      error.value = e?.message || '定位解析失败';
      // [zh] return 返回
      return null;
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      loading.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `refresh`
  const refresh = async (opts?: { silent?: boolean }) => {
    // [zh] 条件判断 if
    if (!authStore.isLoggedIn) {
      // [zh] 语句
      error.value = '请先登录后再获取当前位置';
      // [zh] 条件判断 if
      if (!opts?.silent) {
        // [zh] 语句
        toast.warning(error.value);
      // [zh] 语法符号
      }
      // [zh] return 返回
      return null;
    // [zh] 语法符号
    }
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `pos`
      const pos = await getBrowserPosition();
      // [zh] return 返回
      return await fetchByCoords(pos.coords.latitude, pos.coords.longitude);
    // [zh] 代码块开始
    } catch (e: any) {
      // [zh] 声明变量 `msg`
      const msg =
        // [zh] TypeScript/JS 语句：`e?.code === 1`
        e?.code === 1
          // [zh] TypeScript/JS 语句：`? '定位被拒绝，请在浏览器设置中允许位置权限'`
          ? '定位被拒绝，请在浏览器设置中允许位置权限'
          // [zh] 语句
          : e?.message || '无法获取当前位置';
      // [zh] 语句
      error.value = msg;
      // [zh] 条件判断 if
      if (!opts?.silent) {
        // [zh] 语句
        toast.warning(msg);
      // [zh] 语法符号
      }
      // [zh] return 返回
      return null;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`data,`
    data,
    // [zh] TypeScript/JS 语句：`loading,`
    loading,
    // [zh] TypeScript/JS 语句：`error,`
    error,
    // [zh] TypeScript/JS 语句：`refresh,`
    refresh,
    // [zh] TypeScript/JS 语句：`fetchByCoords,`
    fetchByCoords,
    // [zh] TypeScript/JS 语句：`getBrowserPosition`
    getBrowserPosition
  // [zh] 语句
  };
// [zh] 语法符号
}

// [zh] 导出模块成员
export function useUserLocationWeather() {
  // [zh] 条件判断 if
  if (!shared) {
    // [zh] 语句
    shared = createState();
  // [zh] 语法符号
  }
  // [zh] return 返回
  return shared;
// [zh] 语法符号
}
