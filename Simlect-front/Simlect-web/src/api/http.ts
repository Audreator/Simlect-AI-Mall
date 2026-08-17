import axios from 'axios';
import router from '@/router';
import { useAuthStore } from '@/stores/auth';
import { toast } from '@/utils/toast';
import { RESPONSE_CODE } from '@/constants/backendEnums';

export interface ApiResponse<T = unknown> {
  code: number;
  info?: string;
  data: T;
}

const http: any = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
  // CSRF 防护：统一自定义头（非浏览器自动携带），配合网关 Origin 校验
  headers: { 'X-Requested-With': 'XMLHttpRequest' }
});

function buildFormData(params?: Record<string, unknown>): FormData {
  const formData = new FormData();
  if (!params) return formData;
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return;
    if (Array.isArray(value)) {
      value.forEach((item) => formData.append(key, String(item)));
    } else if (value instanceof Blob) {
      formData.append(key, value);
    } else {
      formData.append(key, String(value));
    }
  });
  return formData;
}

http.interceptors.response.use(
  async (res: any) => {
    const { data, config } = res;
    if (data.code !== 200) {
      if (data.code === RESPONSE_CODE.LOGIN_TIMEOUT) {
        if (window.location.pathname.startsWith('/payment/')) {
          return Promise.reject(data);
        }
        const authStore = useAuthStore();
        const ok = await authStore.ensureSession().catch(() => false);
        if (ok) {
          if (config._retried) {
            return Promise.reject(data);
          }
          config._retried = true;
          return http(config);
        }
        authStore.logout(false);
        router.replace('/login');
        toast.error(data.info || '登录超时，请重新登录');
        return Promise.reject(data);
      }
      toast.error(data.info || '请求失败');
      return Promise.reject(data);
    }
    return data.data as unknown;
  },
  (error: any) => {
    const status = error?.response?.status;
    if (status === 401) {
      const authStore = useAuthStore();
      authStore.logout(true);
      router.replace('/login');
      return Promise.reject(error);
    }
    toast.error('网络异常，请重试');
    return Promise.reject(error);
  }
);

export const request = {

  get: <T = any>(url: string, config?: { params?: Record<string, unknown> }) =>
    http.get(url, config) as Promise<T>,

  post: <T = any>(url: string, data?: any, config?: Record<string, any>) =>
    http.post(url, data, config) as Promise<T>,

  postForm: <T = any>(url: string, params?: Record<string, unknown>) =>
    http.post(url, buildFormData(params)) as Promise<T>
};

export default request;
