
// [zh] 导出模块成员
export function resolveAgentWsUrl(): string {
  // [zh] 声明变量 `raw`
  const raw = (import.meta.env.VITE_WS as string | undefined)?.trim() || '/ws/';
  // [zh] 声明变量 `pageHost`
  const pageHost = window.location.hostname;
  // [zh] 声明变量 `pageProtocol`
  const pageProtocol = window.location.protocol;

  // [zh] 条件判断 if
  if (/^wss?:\/\//i.test(raw)) {
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `parsed`
      const parsed = new URL(raw.replace(/\?token=.*$/i, ''));
      // [zh] 声明变量 `envHost`
      const envHost = parsed.hostname;
      // [zh] 条件判断 if
      if (
        // [zh] TypeScript/JS 语句：`(envHost === 'localhost' || envHost === '127.0.0.1') &&`
        (envHost === 'localhost' || envHost === '127.0.0.1') &&
        // [zh] TypeScript/JS 语句：`pageHost !== 'localhost' &&`
        pageHost !== 'localhost' &&
        // [zh] TypeScript/JS 语句：`pageHost !== '127.0.0.1'`
        pageHost !== '127.0.0.1'
      // [zh] 代码块开始
      ) {
        // [zh] 声明变量 `protocol`
        const protocol = pageProtocol === 'https:' ? 'wss:' : 'ws:';
        // [zh] return 返回
        return `${protocol}//${window.location.host}/ws/`;
      // [zh] 语法符号
      }
    // [zh] 代码块开始
    } catch {

    // [zh] 语法符号
    }
    // [zh] return 返回
    return raw.replace(/\?token=.*$/i, '').replace(/\?$/, '') || raw;
  // [zh] 语法符号
  }

  // [zh] 声明变量 `protocol`
  const protocol = pageProtocol === 'https:' ? 'wss:' : 'ws:';
  // [zh] 声明变量 `path`
  const path = raw.startsWith('/') ? raw.replace(/\?token=.*$/i, '') : '/ws/';
  // [zh] return 返回
  return `${protocol}//${window.location.host}${path}`;
// [zh] 语法符号
}
