// [zh] 导入 `./http`
import request from './http';

// [zh] 导出模块成员
export interface LocationPayload {
  // [zh] 语句
  latitude?: number;
  // [zh] 语句
  longitude?: number;
  // [zh] 语句
  city?: string;
  // [zh] 语句
  province?: string;
  // [zh] 语句
  district?: string;
  // [zh] 语句
  street?: string;
  // [zh] 语句
  summary?: string;
// [zh] 语法符号
}

// [zh] 导出模块成员
export type LocationWeatherPayload = LocationPayload;

// [zh] 导出模块成员
export const locationApi = {
  // [zh] TypeScript/JS 语句：`resolve: (latitude: number, longitude: number) =>`
  resolve: (latitude: number, longitude: number) =>
    // [zh] 代码块开始
    request.get('/location/resolve', {
      // [zh] TypeScript/JS 语句：`params: { latitude, longitude }`
      params: { latitude, longitude }
    // [zh] TypeScript/JS 语句：`}),`
    }),

  // [zh] TypeScript/JS 语句：`sync: (latitude: number, longitude: number) =>`
  sync: (latitude: number, longitude: number) =>
    // [zh] 代码块开始
    request.get('/location/sync', {
      // [zh] TypeScript/JS 语句：`params: { latitude, longitude }`
      params: { latitude, longitude }
    // [zh] TypeScript/JS 语句：`})`
    })
// [zh] 语句
};
