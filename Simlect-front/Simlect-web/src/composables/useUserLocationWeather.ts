import { ref, shallowRef } from 'vue';
import { locationApi, type LocationPayload } from '@/api/location';
import { useAuthStore } from '@/stores/auth';
import { toast } from '@/utils/toast';

const CACHE_KEY = 'simlect_user_location';

let shared: ReturnType<typeof createState> | null = null;

const readCache = (): LocationPayload | null => {
  try {
    const raw = sessionStorage.getItem(CACHE_KEY);
    return raw ? (JSON.parse(raw) as LocationPayload) : null;
  } catch {
    return null;
  }
};

const writeCache = (data: LocationPayload) => {
  try {
    sessionStorage.setItem(CACHE_KEY, JSON.stringify(data));
  } catch {

  }
};

const getBrowserPosition = (): Promise<GeolocationPosition> =>
  new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('当前浏览器不支持定位'));
      return;
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 12000,
      maximumAge: 5 * 60 * 1000
    });
  });

function createState() {
  const authStore = useAuthStore();
  const data = shallowRef<LocationPayload | null>(readCache());
  const loading = ref(false);
  const error = ref('');

  const fetchByCoords = async (lat: number, lng: number) => {
    if (!authStore.isLoggedIn) {
      error.value = '请先登录后再获取当前位置';
      toast.warning(error.value);
      return null;
    }
    loading.value = true;
    error.value = '';
    try {
      const payload = await locationApi.sync(lat, lng);
      if (payload && typeof payload === 'object') {
        data.value = payload as LocationPayload;
        writeCache(data.value);
      }
      return data.value;
    } catch (e: any) {
      error.value = e?.message || '定位解析失败';
      return null;
    } finally {
      loading.value = false;
    }
  };

  const refresh = async (opts?: { silent?: boolean }) => {
    if (!authStore.isLoggedIn) {
      error.value = '请先登录后再获取当前位置';
      if (!opts?.silent) {
        toast.warning(error.value);
      }
      return null;
    }
    try {
      const pos = await getBrowserPosition();
      return await fetchByCoords(pos.coords.latitude, pos.coords.longitude);
    } catch (e: any) {
      const msg =
        e?.code === 1
          ? '定位被拒绝，请在浏览器设置中允许位置权限'
          : e?.message || '无法获取当前位置';
      error.value = msg;
      if (!opts?.silent) {
        toast.warning(msg);
      }
      return null;
    }
  };

  return {
    data,
    loading,
    error,
    refresh,
    fetchByCoords,
    getBrowserPosition
  };
}

export function useUserLocationWeather() {
  if (!shared) {
    shared = createState();
  }
  return shared;
}
