

interface CacheEntry<T> {
  data: T;
  timestamp: number;

  ttl: number;
}

const SESSION_PREFIX = 'api_cache:';

const memoryCache = new Map<string, CacheEntry<any>>();

const pendingRequests = new Map<string, Promise<any>>();

function readFromCache<T>(key: string): { data: T; from: 'memory' | 'session' } | null {

  const mem = memoryCache.get(key);
  if (mem && Date.now() - mem.timestamp < mem.ttl) {
    return { data: mem.data, from: 'memory' };
  }

  try {
    const raw = sessionStorage.getItem(SESSION_PREFIX + key);
    if (!raw) return null;
    const entry: CacheEntry<T> = JSON.parse(raw);
    if (Date.now() - entry.timestamp < entry.ttl) {

      memoryCache.set(key, entry);
      return { data: entry.data, from: 'session' };
    }
    sessionStorage.removeItem(SESSION_PREFIX + key);
    return null;
  } catch {
    return null;
  }
}

function writeToCache<T>(key: string, data: T, ttl: number) {
  const entry: CacheEntry<T> = { data, timestamp: Date.now(), ttl };
  memoryCache.set(key, entry);

  try {
    const raw = JSON.stringify(entry);

    if (raw.length > 50 * 1024) return;
    sessionStorage.setItem(SESSION_PREFIX + key, raw);
  } catch {

  }
}

export function clearApiCache(key?: string) {
  if (key) {
    memoryCache.delete(key);
    try { sessionStorage.removeItem(SESSION_PREFIX + key); } catch {  }
  } else {
    memoryCache.clear();

    try {
      const keysToRemove: string[] = [];
      for (let i = 0; i < sessionStorage.length; i++) {
        const k = sessionStorage.key(i);
        if (k?.startsWith(SESSION_PREFIX)) keysToRemove.push(k);
      }
      keysToRemove.forEach((k) => sessionStorage.removeItem(k));
    } catch {  }
  }
}

export interface CacheOptions {

  ttl?: number;

  key?: string;
}

export function withCache<T>(
  fetcher: () => Promise<T>,
  options: CacheOptions = {}
): Promise<T> {
  const ttl = options.ttl ?? 5 * 60 * 1000;
  const cacheKey = options.key;

  if (!cacheKey) {
    return fetcher();
  }

  const cached = readFromCache<T>(cacheKey);
  if (cached) {
    return Promise.resolve(cached.data);
  }

  const pending = pendingRequests.get(cacheKey);
  if (pending) return pending;

  const promise = fetcher()
    .then((data) => {
      writeToCache(cacheKey, data, ttl);
      return data;
    })
    .finally(() => {
      pendingRequests.delete(cacheKey);
    });

  pendingRequests.set(cacheKey, promise);
  return promise;
}

export function invalidateCache(key: string) {
  memoryCache.delete(key);
  try { sessionStorage.removeItem(SESSION_PREFIX + key); } catch {  }
}
