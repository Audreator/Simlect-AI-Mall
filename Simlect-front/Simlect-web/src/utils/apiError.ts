export interface ApiErrorPayload {
  code?: number;
  errorType?: string;
  unbanAt?: number;
}

export class ApiBusinessError extends Error {
  code?: number;
  errorType?: string;
  unbanAt?: number;

  constructor(message: string, payload?: ApiErrorPayload) {
    super(message);
    this.name = 'ApiBusinessError';
    this.code = payload?.code;
    this.errorType = payload?.errorType;
    this.unbanAt = payload?.unbanAt;
  }
}


export function formatUnbanTime(unbanAtMs: number): string {
  const d = new Date(unbanAtMs);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

export function formatUploadErrorMessage(e: unknown): string {
  if (e instanceof ApiBusinessError) {
    if (e.unbanAt && e.message && !e.message.includes('解封时间')) {
      return `${e.message}（解封时间：${formatUnbanTime(e.unbanAt)}）`;
    }
    return e.message;
  }
  if (e && typeof e === 'object' && 'message' in e && typeof (e as { message: unknown }).message === 'string') {
    return (e as { message: string }).message;
  }
  return '图片上传失败';
}

export function formatLoginErrorMessage(e: unknown): string {
  if (e && typeof e === 'object') {
    const err = e as { info?: string; data?: { unbanAt?: number; errorType?: string } };
    if (err.info?.includes('解封时间')) {
      return err.info;
    }
    if (err.data?.unbanAt) {
      const base = err.info || '账号被临时封禁';
      return `${base}（解封时间：${formatUnbanTime(err.data.unbanAt)}）`;
    }
    if (err.info) return err.info;
  }
  return '登录失败';
}
