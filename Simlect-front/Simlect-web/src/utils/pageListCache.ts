

export type PageListCacheEntry = {
  scrollTop: number;
  state: Record<string, unknown>;
  savedAt: number;
};

const cache = new Map<string, PageListCacheEntry>();
const MAX_ENTRIES = 24;
const MAX_AGE_MS = 30 * 60 * 1000;

function prune() {
  if (cache.size <= MAX_ENTRIES) return;
  const oldest = [...cache.entries()].sort((a, b) => a[1].savedAt - b[1].savedAt);
  const removeCount = cache.size - MAX_ENTRIES;
  for (let i = 0; i < removeCount; i++) {
    cache.delete(oldest[i][0]);
  }
}

export function getPageListCache(key: string): PageListCacheEntry | null {
  const entry = cache.get(key);
  if (!entry) return null;
  if (Date.now() - entry.savedAt > MAX_AGE_MS) {
    cache.delete(key);
    return null;
  }
  return entry;
}

export function setPageListCache(key: string, scrollTop: number, state: Record<string, unknown>) {
  cache.set(key, { scrollTop, state, savedAt: Date.now() });
  prune();
}

export function clearPageListCache(key?: string) {
  if (key) cache.delete(key);
  else cache.clear();
}
