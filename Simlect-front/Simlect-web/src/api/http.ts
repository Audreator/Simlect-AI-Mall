// [zh] 导入 `axios`
import axios from 'axios';
// [zh] 导入 `@/router`
import router from '@/router';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/constants/backendEnums`
import { RESPONSE_CODE } from '@/constants/backendEnums';

// [zh] 导出模块成员
export interface ApiResponse<T = unknown> {
  // [zh] 语句
  code: number;
  // [zh] 语句
  info?: string;
  // [zh] 语句
  data: T;
// [zh] 语法符号
}

// [zh] 声明变量 `http`
const http: any = axios.create({
  // [zh] TypeScript/JS 语句：`baseURL: '/api',`
  baseURL: '/api',
  // [zh] TypeScript/JS 语句：`timeout: 15000,`
  timeout: 15000,
  // [zh] TypeScript/JS 语句：`withCredentials: true`
  withCredentials: true,
  // CSRF 防护：统一自定义头（非浏览器自动携带），配合网关 Origin 校验
  headers: { 'X-Requested-With': 'XMLHttpRequest' }
// [zh] 语句
});

// [zh] 函数 `buildFormData`
function buildFormData(params?: Record<string, unknown>): FormData {
  // [zh] 声明变量 `formData`
  const formData = new FormData();
  // [zh] 条件判断 if
  if (!params) return formData;
  // [zh] 代码块开始
  Object.entries(params).forEach(([key, value]) => {
    // [zh] 条件判断 if
    if (value === undefined || value === null) return;
    // [zh] 条件判断 if
    if (Array.isArray(value)) {
      // [zh] 语句
      value.forEach((item) => formData.append(key, String(item)));
    // [zh] 代码块开始
    } else if (value instanceof Blob) {
      // [zh] 语句
      formData.append(key, value);
    // [zh] 代码块开始
    } else {
      // [zh] 语句
      formData.append(key, String(value));
    // [zh] 语法符号
    }
  // [zh] 语句
  });
  // [zh] return 返回
  return formData;
// [zh] 语法符号
}

// [zh] TypeScript/JS 语句：`http.interceptors.response.use(`
http.interceptors.response.use(
  // [zh] 代码块开始
  async (res: any) => {
    // [zh] 语句
    const { data, config } = res;
    // [zh] 条件判断 if
    if (data.code !== 200) {
      // [zh] 条件判断 if
      if (data.code === RESPONSE_CODE.LOGIN_TIMEOUT) {
        // [zh] 条件判断 if
        if (window.location.pathname.startsWith('/payment/')) {
          // [zh] return 返回
          return Promise.reject(data);
        // [zh] 语法符号
        }
        // [zh] 声明变量 `authStore`
        const authStore = useAuthStore();
        // [zh] 声明变量 `ok`
        const ok = await authStore.ensureSession().catch(() => false);
        // [zh] 条件判断 if
        if (ok) {
          // [zh] return 返回
          if (config._retried) {
            return Promise.reject(data);
          }
          config._retried = true;
          // [zh] 重试仅限一次（防 901 与业务错误叠加形成请求风暴）
          // [zh] return 返回
          return http(config);
        // [zh] 语句
        }
        // [zh] 语句
        authStore.logout(false);
        // [zh] 语句
        router.replace('/login');
        // [zh] 语句
        toast.error(data.info || '登录超时，请重新登录');
        // [zh] return 返回
        return Promise.reject(data);
      // [zh] 语法符号
      }
      // [zh] 语句
      toast.error(data.info || '请求失败');
      // [zh] return 返回
      return Promise.reject(data);
    // [zh] 语法符号
    }
    // [zh] return 返回
    return data.data as unknown;
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  (error: any) => {
    // [zh] 声明变量 `status`
    const status = error?.response?.status;
    // [zh] 条件判断 if
    if (status === 401) {
      // [zh] 声明变量 `authStore`
      const authStore = useAuthStore();
      // [zh] 语句
      authStore.logout(true);
      // [zh] 语句
      router.replace('/login');
      // [zh] return 返回
      return Promise.reject(error);
    // [zh] 语法符号
    }
    // [zh] 语句
    toast.error('网络异常，请重试');
    // [zh] return 返回
    return Promise.reject(error);
  // [zh] 语法符号
  }
// [zh] 语句
);

// [zh] 导出模块成员
export const request = {

  // [zh] TypeScript/JS 语句：`get: <T = any>(url: string, config?: { params?: Record<`
  get: <T = any>(url: string, config?: { params?: Record<string, unknown> }) =>
    // [zh] TypeScript/JS 语句：`http.get(url, config) as Promise<T>,`
    http.get(url, config) as Promise<T>,

  // [zh] TypeScript/JS 语句：`post: <T = any>(url: string, data?: any, config?: Recor`
  post: <T = any>(url: string, data?: any, config?: Record<string, any>) =>
    // [zh] TypeScript/JS 语句：`http.post(url, data, config) as Promise<T>,`
    http.post(url, data, config) as Promise<T>,

  // [zh] TypeScript/JS 语句：`postForm: <T = any>(url: string, params?: Record<string`
  postForm: <T = any>(url: string, params?: Record<string, unknown>) =>
    // [zh] TypeScript/JS 语句：`http.post(url, buildFormData(params)) as Promise<T>`
    http.post(url, buildFormData(params)) as Promise<T>
// [zh] 语句
};

// [zh] 导出模块成员
export default request;
