// [zh] 导出模块成员
export interface ApiErrorPayload {
  // [zh] 语句
  code?: number;
  // [zh] 语句
  errorType?: string;
  // [zh] 语句
  unbanAt?: number;
// [zh] 语法符号
}

// [zh] 导出模块成员
export class ApiBusinessError extends Error {
  // [zh] 语句
  code?: number;
  // [zh] 语句
  errorType?: string;
  // [zh] 语句
  unbanAt?: number;

  // [zh] 代码块开始
  constructor(message: string, payload?: ApiErrorPayload) {
    // [zh] 语句
    super(message);
    // [zh] 语句
    this.name = 'ApiBusinessError';
    // [zh] 语句
    this.code = payload?.code;
    // [zh] 语句
    this.errorType = payload?.errorType;
    // [zh] 语句
    this.unbanAt = payload?.unbanAt;
  // [zh] 语法符号
  }
// [zh] 语法符号
}


// [zh] 导出模块成员
export function formatUnbanTime(unbanAtMs: number): string {
  // [zh] 声明变量 `d`
  const d = new Date(unbanAtMs);
  // [zh] 声明变量 `pad`
  const pad = (n: number) => String(n).padStart(2, '0');
  // [zh] return 返回
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function formatUploadErrorMessage(e: unknown): string {
  // [zh] 条件判断 if
  if (e instanceof ApiBusinessError) {
    // [zh] 条件判断 if
    if (e.unbanAt && e.message && !e.message.includes('解封时间')) {
      // [zh] return 返回
      return `${e.message}（解封时间：${formatUnbanTime(e.unbanAt)}）`;
    // [zh] 语法符号
    }
    // [zh] return 返回
    return e.message;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (e && typeof e === 'object' && 'message' in e && typeof (e as { message: unknown }).message === 'string') {
    // [zh] return 返回
    return (e as { message: string }).message;
  // [zh] 语法符号
  }
  // [zh] return 返回
  return '图片上传失败';
// [zh] 语法符号
}

// [zh] 导出模块成员
export function formatLoginErrorMessage(e: unknown): string {
  // [zh] 条件判断 if
  if (e && typeof e === 'object') {
    // [zh] 声明变量 `err`
    const err = e as { info?: string; data?: { unbanAt?: number; errorType?: string } };
    // [zh] 条件判断 if
    if (err.info?.includes('解封时间')) {
      // [zh] return 返回
      return err.info;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (err.data?.unbanAt) {
      // [zh] 声明变量 `base`
      const base = err.info || '账号被临时封禁';
      // [zh] return 返回
      return `${base}（解封时间：${formatUnbanTime(err.data.unbanAt)}）`;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (err.info) return err.info;
  // [zh] 语法符号
  }
  // [zh] return 返回
  return '登录失败';
// [zh] 语法符号
}
