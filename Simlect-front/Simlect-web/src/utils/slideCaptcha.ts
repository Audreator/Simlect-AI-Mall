// [zh] 导入 `axios`
import axios from 'axios';
// [zh] 导入 `crypto-js`
import CryptoJS from 'crypto-js';


// [zh] 导出模块成员
export interface CaptchaPlusResponse<T = Record<string, unknown>> {
  // [zh] 语句
  repCode: string;
  // [zh] 语句
  repMsg?: string;
  // [zh] 语句
  repData?: T;
// [zh] 语法符号
}

// [zh] 导出模块成员
export interface CaptchaGetData {
  // [zh] 语句
  originalImageBase64: string;
  // [zh] 语句
  jigsawImageBase64: string;
  // [zh] 语句
  token: string;
  // [zh] 语句
  secretKey?: string;
// [zh] 语法符号
}

// [zh] 声明变量 `captchaHttp`
const captchaHttp = axios.create({
  // [zh] TypeScript/JS 语句：`baseURL: '/api',`
  baseURL: '/api',
  // [zh] TypeScript/JS 语句：`timeout: 15000`
  timeout: 15000
// [zh] 语句
});

// [zh] 导出模块成员
export function aesEncrypt(word: string, keyWord: string): string {
  // [zh] 声明变量 `key`
  const key = CryptoJS.enc.Utf8.parse(keyWord);
  // [zh] 声明变量 `src`
  const src = CryptoJS.enc.Utf8.parse(word);
  // [zh] return 返回
  return CryptoJS.AES.encrypt(src, key, {
    // [zh] TypeScript/JS 语句：`mode: CryptoJS.mode.ECB,`
    mode: CryptoJS.mode.ECB,
    // [zh] TypeScript/JS 语句：`padding: CryptoJS.pad.Pkcs7`
    padding: CryptoJS.pad.Pkcs7
  // [zh] 语句
  }).toString();
// [zh] 语法符号
}

// [zh] 导出模块成员
export async function fetchSlideCaptcha(): Promise<CaptchaGetData> {
  // [zh] 代码块开始
  const { data } = await captchaHttp.post<CaptchaPlusResponse<CaptchaGetData>>('/captcha/get', {
    // [zh] TypeScript/JS 语句：`captchaType: 'blockPuzzle',`
    captchaType: 'blockPuzzle',
    // [zh] TypeScript/JS 语句：`clientUid: localStorage.getItem('slider') || '',`
    clientUid: localStorage.getItem('slider') || '',
    // [zh] TypeScript/JS 语句：`ts: Date.now()`
    ts: Date.now()
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (data.repCode !== '0000' || !data.repData) {
    // [zh] 语句
    throw new Error(data.repMsg || '验证码加载失败');
  // [zh] 语法符号
  }
  // [zh] return 返回
  return data.repData;
// [zh] 语法符号
}

// [zh] 导出模块成员
export async function checkSlideCaptcha(payload: {
  // [zh] 语句
  token: string;
  // [zh] 语句
  secretKey?: string;
  // [zh] 语句
  moveX: number;
// [zh] 代码块开始
}): Promise<string> {
  // [zh] 声明变量 `point`
  const point = { x: payload.moveX, y: 5.0 };
  // [zh] 声明变量 `pointJson`
  const pointJson = payload.secretKey
    // [zh] TypeScript/JS 语句：`? aesEncrypt(JSON.stringify(point), payload.secretKey)`
    ? aesEncrypt(JSON.stringify(point), payload.secretKey)
    // [zh] 语句
    : JSON.stringify(point);

  // [zh] 代码块开始
  const { data } = await captchaHttp.post<CaptchaPlusResponse>('/captcha/check', {
    // [zh] TypeScript/JS 语句：`captchaType: 'blockPuzzle',`
    captchaType: 'blockPuzzle',
    // [zh] TypeScript/JS 语句：`pointJson,`
    pointJson,
    // [zh] TypeScript/JS 语句：`token: payload.token`
    token: payload.token
  // [zh] 语句
  });

  // [zh] 条件判断 if
  if (data.repCode !== '0000') {
    // [zh] 语句
    throw new Error(data.repMsg || '验证失败，请重试');
  // [zh] 语法符号
  }

  // [zh] 声明变量 `raw`
  const raw = `${payload.token}---${JSON.stringify(point)}`;
  // [zh] return 返回
  return payload.secretKey ? aesEncrypt(raw, payload.secretKey) : raw;
// [zh] 语法符号
}


// [zh] 导出模块成员
export function toCaptchaX(movePx: number, displayWidth: number): number {
  // [zh] 条件判断 if
  if (!displayWidth) return movePx;
  // [zh] return 返回
  return Math.round(movePx * (310 / displayWidth));
// [zh] 语法符号
}


// [zh] 导出模块成员
export function cancelSlideCaptchaToken(token: string): void {
  // [zh] 条件判断 if
  if (!token) return;
  // [zh] 语句
  captchaHttp.post('/captcha/cancel', { token }).catch(() => {});
// [zh] 语法符号
}
