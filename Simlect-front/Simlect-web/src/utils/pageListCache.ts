

// [zh] 导出模块成员
export type PageListCacheEntry = {
  // [zh] 语句
  scrollTop: number;
  // [zh] 语句
  state: Record<string, unknown>;
  // [zh] 语句
  savedAt: number;
// [zh] 语句
};

// [zh] 声明变量 `cache`
const cache = new Map<string, PageListCacheEntry>();
// [zh] 声明变量 `MAX_ENTRIES`
const MAX_ENTRIES = 24;
// [zh] 声明变量 `MAX_AGE_MS`
const MAX_AGE_MS = 30 * 60 * 1000;

// [zh] 函数 `prune`
function prune() {
  // [zh] 条件判断 if
  if (cache.size <= MAX_ENTRIES) return;
  // [zh] 声明变量 `oldest`
  const oldest = [...cache.entries()].sort((a, b) => a[1].savedAt - b[1].savedAt);
  // [zh] 声明变量 `removeCount`
  const removeCount = cache.size - MAX_ENTRIES;
  // [zh] for 循环
  for (let i = 0; i < removeCount; i++) {
    // [zh] 语句
    cache.delete(oldest[i][0]);
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 导出模块成员
export function getPageListCache(key: string): PageListCacheEntry | null {
  // [zh] 声明变量 `entry`
  const entry = cache.get(key);
  // [zh] 条件判断 if
  if (!entry) return null;
  // [zh] 条件判断 if
  if (Date.now() - entry.savedAt > MAX_AGE_MS) {
    // [zh] 语句
    cache.delete(key);
    // [zh] return 返回
    return null;
  // [zh] 语法符号
  }
  // [zh] return 返回
  return entry;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function setPageListCache(key: string, scrollTop: number, state: Record<string, unknown>) {
  // [zh] 语句
  cache.set(key, { scrollTop, state, savedAt: Date.now() });
  // [zh] 语句
  prune();
// [zh] 语法符号
}

// [zh] 导出模块成员
export function clearPageListCache(key?: string) {
  // [zh] 条件判断 if
  if (key) cache.delete(key);
  // [zh] else 分支
  else cache.clear();
// [zh] 语法符号
}
