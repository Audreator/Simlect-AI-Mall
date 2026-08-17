import axios from 'axios';
import CryptoJS from 'crypto-js';


export interface CaptchaPlusResponse<T = Record<string, unknown>> {
  repCode: string;
  repMsg?: string;
  repData?: T;
}

export interface CaptchaGetData {
  originalImageBase64: string;
  jigsawImageBase64: string;
  token: string;
  secretKey?: string;
}

const captchaHttp = axios.create({
  baseURL: '/api',
  timeout: 15000
});

export function aesEncrypt(word: string, keyWord: string): string {
  const key = CryptoJS.enc.Utf8.parse(keyWord);
  const src = CryptoJS.enc.Utf8.parse(word);
  return CryptoJS.AES.encrypt(src, key, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7
  }).toString();
}

export async function fetchSlideCaptcha(): Promise<CaptchaGetData> {
  const { data } = await captchaHttp.post<CaptchaPlusResponse<CaptchaGetData>>('/captcha/get', {
    captchaType: 'blockPuzzle',
    clientUid: localStorage.getItem('slider') || '',
    ts: Date.now()
  });
  if (data.repCode !== '0000' || !data.repData) {
    throw new Error(data.repMsg || '验证码加载失败');
  }
  return data.repData;
}

export async function checkSlideCaptcha(payload: {
  token: string;
  secretKey?: string;
  moveX: number;
}): Promise<string> {
  const point = { x: payload.moveX, y: 5.0 };
  const pointJson = payload.secretKey
    ? aesEncrypt(JSON.stringify(point), payload.secretKey)
    : JSON.stringify(point);

  const { data } = await captchaHttp.post<CaptchaPlusResponse>('/captcha/check', {
    captchaType: 'blockPuzzle',
    pointJson,
    token: payload.token
  });

  if (data.repCode !== '0000') {
    throw new Error(data.repMsg || '验证失败，请重试');
  }

  const raw = `${payload.token}---${JSON.stringify(point)}`;
  return payload.secretKey ? aesEncrypt(raw, payload.secretKey) : raw;
}


export function toCaptchaX(movePx: number, displayWidth: number): number {
  if (!displayWidth) return movePx;
  return Math.round(movePx * (310 / displayWidth));
}


export function cancelSlideCaptchaToken(token: string): void {
  if (!token) return;
  captchaHttp.post('/captcha/cancel', { token }).catch(() => {});
}
