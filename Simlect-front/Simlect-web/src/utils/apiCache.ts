

// [zh] TypeScript 类型 `CacheEntry`（类比 Java interface/DTO）
interface CacheEntry<T> {
  // [zh] 语句
  data: T;
  // [zh] 语句
  timestamp: number;

  // [zh] 语句
  ttl: number;
// [zh] 语法符号
}

// [zh] 声明变量 `SESSION_PREFIX`
const SESSION_PREFIX = 'api_cache:';

// [zh] 声明变量 `memoryCache`
const memoryCache = new Map<string, CacheEntry<any>>();

// [zh] 声明变量 `pendingRequests`
const pendingRequests = new Map<string, Promise<any>>();

// [zh] 函数 `readFromCache`
function readFromCache<T>(key: string): { data: T; from: 'memory' | 'session' } | null {

  // [zh] 声明变量 `mem`
  const mem = memoryCache.get(key);
  // [zh] 条件判断 if
  if (mem && Date.now() - mem.timestamp < mem.ttl) {
    // [zh] return 返回
    return { data: mem.data, from: 'memory' };
  // [zh] 语法符号
  }

  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `raw`
    const raw = sessionStorage.getItem(SESSION_PREFIX + key);
    // [zh] 条件判断 if
    if (!raw) return null;
    // [zh] 声明变量 `entry`
    const entry: CacheEntry<T> = JSON.parse(raw);
    // [zh] 条件判断 if
    if (Date.now() - entry.timestamp < entry.ttl) {

      // [zh] 语句
      memoryCache.set(key, entry);
      // [zh] return 返回
      return { data: entry.data, from: 'session' };
    // [zh] 语法符号
    }
    // [zh] 语句
    sessionStorage.removeItem(SESSION_PREFIX + key);
    // [zh] return 返回
    return null;
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return null;
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 函数 `writeToCache`
function writeToCache<T>(key: string, data: T, ttl: number) {
  // [zh] 声明变量 `entry`
  const entry: CacheEntry<T> = { data, timestamp: Date.now(), ttl };
  // [zh] 语句
  memoryCache.set(key, entry);

  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `raw`
    const raw = JSON.stringify(entry);

    // [zh] 条件判断 if
    if (raw.length > 50 * 1024) return;
    // [zh] 语句
    sessionStorage.setItem(SESSION_PREFIX + key, raw);
  // [zh] 代码块开始
  } catch {

  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 导出模块成员
export function clearApiCache(key?: string) {
  // [zh] 条件判断 if
  if (key) {
    // [zh] 语句
    memoryCache.delete(key);
    // [zh] TypeScript/JS 语句：`try { sessionStorage.removeItem(SESSION_PREFIX + key); `
    try { sessionStorage.removeItem(SESSION_PREFIX + key); } catch {  }
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    memoryCache.clear();

    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `keysToRemove`
      const keysToRemove: string[] = [];
      // [zh] for 循环
      for (let i = 0; i < sessionStorage.length; i++) {
        // [zh] 声明变量 `k`
        const k = sessionStorage.key(i);
        // [zh] 条件判断 if
        if (k?.startsWith(SESSION_PREFIX)) keysToRemove.push(k);
      // [zh] 语法符号
      }
      // [zh] 语句
      keysToRemove.forEach((k) => sessionStorage.removeItem(k));
    // [zh] TypeScript/JS 语句：`} catch {  }`
    } catch {  }
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 导出模块成员
export interface CacheOptions {

  // [zh] 语句
  ttl?: number;

  // [zh] 语句
  key?: string;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function withCache<T>(
  // [zh] TypeScript/JS 语句：`fetcher: () => Promise<T>,`
  fetcher: () => Promise<T>,
  // [zh] TypeScript/JS 语句：`options: CacheOptions = {}`
  options: CacheOptions = {}
// [zh] 代码块开始
): Promise<T> {
  // [zh] 声明变量 `ttl`
  const ttl = options.ttl ?? 5 * 60 * 1000;
  // [zh] 声明变量 `cacheKey`
  const cacheKey = options.key;

  // [zh] 条件判断 if
  if (!cacheKey) {
    // [zh] return 返回
    return fetcher();
  // [zh] 语法符号
  }

  // [zh] 声明变量 `cached`
  const cached = readFromCache<T>(cacheKey);
  // [zh] 条件判断 if
  if (cached) {
    // [zh] return 返回
    return Promise.resolve(cached.data);
  // [zh] 语法符号
  }

  // [zh] 声明变量 `pending`
  const pending = pendingRequests.get(cacheKey);
  // [zh] 条件判断 if
  if (pending) return pending;

  // [zh] 声明变量 `promise`
  const promise = fetcher()
    // [zh] 代码块开始
    .then((data) => {
      // [zh] 语句
      writeToCache(cacheKey, data, ttl);
      // [zh] return 返回
      return data;
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 代码块开始
    .finally(() => {
      // [zh] 语句
      pendingRequests.delete(cacheKey);
    // [zh] 语句
    });

  // [zh] 语句
  pendingRequests.set(cacheKey, promise);
  // [zh] return 返回
  return promise;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function invalidateCache(key: string) {
  // [zh] 语句
  memoryCache.delete(key);
  // [zh] TypeScript/JS 语句：`try { sessionStorage.removeItem(SESSION_PREFIX + key); `
  try { sessionStorage.removeItem(SESSION_PREFIX + key); } catch {  }
// [zh] 语法符号
}
