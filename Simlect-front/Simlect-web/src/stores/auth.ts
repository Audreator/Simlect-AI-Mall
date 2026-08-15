// [zh] 导入 `vue`
import { computed, ref } from 'vue';
// [zh] 导入 `pinia`
import { defineStore } from 'pinia';
// [zh] 导入 `@/api/modules`
import { accountApi } from '@/api/modules';
// [zh] 导入 `@/api/memberCenter`
import { fetchMemberCenter } from '@/api/memberCenter';
// [zh] 导入 `@/stores/cart`
import { useCartStore } from '@/stores/cart';

// [zh] 声明变量 `MEMBER_CACHE_TTL`
const MEMBER_CACHE_TTL = 5 * 60 * 1000;
// [zh] 声明变量 `MEMBER_CACHE_KEY`
const MEMBER_CACHE_KEY = 'eshop_member_level';
// [zh] 声明变量 `MEMBER_CENTER_CACHE_KEY`
const MEMBER_CENTER_CACHE_KEY = 'eshop_member_center';

// [zh] TypeScript 类型 `MemberLevelCache`（类比 Java interface/DTO）
interface MemberLevelCache {
  // [zh] 语句
  levelCode: number;
  // [zh] 语句
  levelName: string;
  // [zh] 语句
  growthValue: number;
  // [zh] 语句
  timestamp: number;
// [zh] 语法符号
}

// [zh] 函数 `readCache`
function readCache<T>(key: string): T | null {
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `raw`
    const raw = sessionStorage.getItem(key);
    // [zh] 条件判断 if
    if (!raw) return null;
    // [zh] 声明变量 `cache`
    const cache = JSON.parse(raw) as T & { timestamp: number };
    // [zh] 条件判断 if
    if (Date.now() - cache.timestamp > MEMBER_CACHE_TTL) {
      // [zh] 语句
      sessionStorage.removeItem(key);
      // [zh] return 返回
      return null;
    // [zh] 语法符号
    }
    // [zh] return 返回
    return cache as T;
  // [zh] 代码块开始
  } catch {
    // [zh] 语句
    sessionStorage.removeItem(key);
    // [zh] return 返回
    return null;
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 函数 `writeCache`
function writeCache(key: string, data: Record<string, unknown>) {
  // [zh] 代码块开始
  try {
    // [zh] 语句
    sessionStorage.setItem(key, JSON.stringify({ ...data, timestamp: Date.now() }));
  // [zh] 代码块开始
  } catch {

  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 函数 `clearCache`
function clearCache(key: string) {
  // [zh] TypeScript/JS 语句：`try { sessionStorage.removeItem(key); } catch {  }`
  try { sessionStorage.removeItem(key); } catch {  }
// [zh] 语法符号
}

// [zh] 函数 `hasUserSession`
function hasUserSession(data: Record<string, any> | null | undefined): data is Record<string, any> {
  // [zh] return 返回
  return !!data?.userId;
// [zh] 语法符号
}

// [zh] 导出模块成员
export const useAuthStore = defineStore('auth', () => {
  // [zh] 声明变量 `userInfo`
  const userInfo = ref<Record<string, any> | null>(null);
  // [zh] 声明变量 `isLoggedIn`
  const isLoggedIn = computed(() => !!userInfo.value?.userId);

  // [zh] 声明变量 `sessionPromise`
  let sessionPromise: Promise<boolean> | null = null;
  // [zh] 声明变量 `sessionReady`
  let sessionReady = false;

  // [zh] 声明变量 `loggingOut`
  const loggingOut = ref(false);

  // [zh] 声明变量 `memberLevelCode`
  const memberLevelCode = ref(1);
  // [zh] 声明变量 `memberLevelName`
  const memberLevelName = ref('');
  // [zh] 声明变量 `memberGrowthValue`
  const memberGrowthValue = ref(0);
  // [zh] 声明变量 `memberCenterData`
  const memberCenterData = ref<Record<string, any> | null>(null);

  // [zh] 声明变量 `memberLevelPromise`
  let memberLevelPromise: Promise<void> | null = null;

  // [zh] 声明变量 `cachedLevel`
  const cachedLevel = readCache<MemberLevelCache>(MEMBER_CACHE_KEY);
  // [zh] 条件判断 if
  if (cachedLevel) {
    // [zh] 语句
    memberLevelCode.value = cachedLevel.levelCode;
    // [zh] 语句
    memberLevelName.value = cachedLevel.levelName;
    // [zh] 语句
    memberGrowthValue.value = cachedLevel.growthValue;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `cachedCenter`
  const cachedCenter = readCache<Record<string, any>>(MEMBER_CENTER_CACHE_KEY);
  // [zh] 条件判断 if
  if (cachedCenter) {
    // [zh] 语句
    memberCenterData.value = cachedCenter;
  // [zh] 语法符号
  }

  // [zh] 声明变量 `applyMemberCenterData`
  const applyMemberCenterData = (center: Record<string, any> | null) => {
    // [zh] 条件判断 if
    if (!center?.profile) {
      // [zh] 语句
      memberCenterData.value = null;
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 声明变量 `profile`
    const profile = center.profile;
    // [zh] 声明变量 `code`
    const code = Number(profile.levelCode ?? 1);
    // [zh] 声明变量 `name`
    const name = profile.levelName || '';
    // [zh] 声明变量 `growth`
    const growth = Number(profile.growthValue ?? 0);
    // [zh] 语句
    memberLevelCode.value = code;
    // [zh] 语句
    memberLevelName.value = name;
    // [zh] 语句
    memberGrowthValue.value = growth;
    // [zh] 语句
    memberCenterData.value = center;
    // [zh] 语句
    writeCache(MEMBER_CACHE_KEY, { levelCode: code, levelName: name, growthValue: growth });
    // [zh] 语句
    writeCache(MEMBER_CENTER_CACHE_KEY, center);
  // [zh] 语句
  };

  // [zh] 声明变量 `loadMemberLevel`
  const loadMemberLevel = async (forceRefresh = false): Promise<void> => {
    // [zh] 条件判断 if
    if (!isLoggedIn.value) return;
    // [zh] 条件判断 if
    if (!forceRefresh) {
      // [zh] 声明变量 `cachedCenter2`
      const cachedCenter2 = readCache<Record<string, any>>(MEMBER_CENTER_CACHE_KEY);
      // [zh] 条件判断 if
      if (cachedCenter2) {
        // [zh] 语句
        applyMemberCenterData(cachedCenter2);
        // [zh] 语句
        return;
      // [zh] 语法符号
      }
      // [zh] 声明变量 `cached2`
      const cached2 = readCache<MemberLevelCache>(MEMBER_CACHE_KEY);
      // [zh] 条件判断 if
      if (cached2) {
        // [zh] 语句
        memberLevelCode.value = cached2.levelCode;
        // [zh] 语句
        memberLevelName.value = cached2.levelName;
        // [zh] 语句
        memberGrowthValue.value = cached2.growthValue;
        // [zh] 条件判断 if
        if (memberCenterData.value) return;
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (memberLevelPromise) return memberLevelPromise;
    // [zh] 代码块开始
    memberLevelPromise = (async () => {
      // [zh] 代码块开始
      try {
        // [zh] 语句
        applyMemberCenterData(await fetchMemberCenter());
      // [zh] 代码块开始
      } catch {

      // [zh] 语法符号
      }
    // [zh] 代码块开始
    })().finally(() => {
      // [zh] 语句
      memberLevelPromise = null;
    // [zh] 语句
    });
    // [zh] return 返回
    return memberLevelPromise;
  // [zh] 语句
  };

  // [zh] 声明变量 `loadMemberCenter`
  const loadMemberCenter = async (forceRefresh = false): Promise<Record<string, any> | null> => {
    // [zh] 条件判断 if
    if (!isLoggedIn.value) return null;
    // [zh] 条件判断 if
    if (forceRefresh) {
      // [zh] 语句
      memberCenterData.value = null;
      // [zh] 语句
      clearCache(MEMBER_CENTER_CACHE_KEY);
      // [zh] 语句
      clearCache(MEMBER_CACHE_KEY);
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (memberCenterData.value) return memberCenterData.value;
    // [zh] 声明变量 `cached2`
    const cached2 = readCache<Record<string, any>>(MEMBER_CENTER_CACHE_KEY);
    // [zh] 条件判断 if
    if (cached2) {
      // [zh] 语句
      applyMemberCenterData(cached2);
      // [zh] return 返回
      return memberCenterData.value;
    // [zh] 语法符号
    }
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await loadMemberLevel(forceRefresh);
    // [zh] return 返回
    return memberCenterData.value;
  // [zh] 语句
  };

  // [zh] 声明变量 `clearAuth`
  const clearAuth = () => {
    // [zh] 语句
    userInfo.value = null;
    // [zh] 语句
    sessionReady = false;
    // [zh] 语句
    clearCache(MEMBER_CACHE_KEY);
    // [zh] 语句
    clearCache(MEMBER_CENTER_CACHE_KEY);
    // [zh] 语句
    memberLevelCode.value = 1;
    // [zh] 语句
    memberLevelName.value = '';
    // [zh] 语句
    memberGrowthValue.value = 0;
    // [zh] 语句
    memberCenterData.value = null;
    // [zh] 代码块开始
    try {
      // [zh] 语句
      useCartStore().resetCart();
    // [zh] 代码块开始
    } catch {

    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `fetchUserInfo`
  const fetchUserInfo = async () => {
    // [zh] 条件判断 if
    if (!isLoggedIn.value) return;
    // [zh] 语句
    userInfo.value = { ...(userInfo.value || {}), ...(await accountApi.getUserInfo()) };
  // [zh] 语句
  };

  // [zh] 声明变量 `login`
  const login = async (payload: Record<string, unknown>) => {
    // [zh] 声明变量 `data`
    const data = await accountApi.login({
      // [zh] TypeScript/JS 语句：`...payload,`
      ...payload,
      // [zh] TypeScript/JS 语句：`password: String(payload.password ?? '')`
      password: String(payload.password ?? '')
    // [zh] 语句
    });
    // [zh] 语句
    userInfo.value = data;
    // [zh] 语句
    sessionReady = true;
    // [zh] 代码块开始
    try {
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await fetchUserInfo();
    // [zh] 代码块开始
    } catch {

    // [zh] 语法符号
    }
    // [zh] 语句
    loadMemberLevel();
    // [zh] 代码块开始
    try {
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await useCartStore().fetchCartCount();
    // [zh] 代码块开始
    } catch {

    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `prepareLogoutNavigation`
  const prepareLogoutNavigation = () => {
    // [zh] 语句
    loggingOut.value = true;
  // [zh] 语句
  };

  // [zh] 声明变量 `logout`
  const logout = async (silent = false) => {
    // [zh] 条件判断 if
    if (!silent && isLoggedIn.value) await accountApi.logout();
    // [zh] 语句
    clearAuth();
  // [zh] 语句
  };

  // [zh] 声明变量 `finishLogoutNavigation`
  const finishLogoutNavigation = () => {
    // [zh] 语句
    loggingOut.value = false;
  // [zh] 语句
  };

  // [zh] 声明变量 `ensureSession`
  const ensureSession = (): Promise<boolean> => {
    // [zh] 条件判断 if
    if (sessionReady && isLoggedIn.value) return Promise.resolve(true);
    // [zh] 条件判断 if
    if (sessionPromise) return sessionPromise;

    // [zh] 代码块开始
    sessionPromise = (async () => {
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `data`
        const data = await accountApi.autoLogin();
        // [zh] 条件判断 if
        if (!hasUserSession(data)) {
          // [zh] 语句
          clearAuth();
          // [zh] return 返回
          return false;
        // [zh] 语法符号
        }
        // [zh] 语句
        userInfo.value = data;
        // [zh] 语句
        sessionReady = true;
        // [zh] 代码块开始
        try {
          // [zh] await 等待 Promise（类比 CompletableFuture.get）
          await fetchUserInfo();
        // [zh] 代码块开始
        } catch {

        // [zh] 语法符号
        }
        // [zh] 语句
        loadMemberLevel();
        // [zh] return 返回
        return true;
      // [zh] 代码块开始
      } catch {
        // [zh] 语句
        clearAuth();
        // [zh] return 返回
        return false;
      // [zh] 语法符号
      }
    // [zh] 代码块开始
    })().finally(() => {
      // [zh] 语句
      sessionPromise = null;
    // [zh] 语句
    });

    // [zh] return 返回
    return sessionPromise;
  // [zh] 语句
  };

  // [zh] 声明变量 `autoLogin`
  const autoLogin = ensureSession;

  // [zh] 声明变量 `restorePromise`
  let restorePromise: Promise<void> | null = null;
  // [zh] 声明变量 `tryRestoreSession`
  const tryRestoreSession = (): Promise<void> => {
    // [zh] 条件判断 if
    if (sessionReady && isLoggedIn.value) return Promise.resolve();
    // [zh] 条件判断 if
    if (restorePromise) return restorePromise;
    // [zh] 代码块开始
    restorePromise = (async () => {
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `data`
        const data = await accountApi.autoLogin();
        // [zh] 条件判断 if
        if (hasUserSession(data)) {
          // [zh] 语句
          userInfo.value = data;
          // [zh] 语句
          sessionReady = true;
          // [zh] TypeScript/JS 语句：`try { await fetchUserInfo(); } catch {  }`
          try { await fetchUserInfo(); } catch {  }
          // [zh] 语句
          loadMemberLevel();
        // [zh] 语法符号
        }
      // [zh] 代码块开始
      } catch {

      // [zh] 代码块开始
      } finally {
        // [zh] 语句
        restorePromise = null;
      // [zh] 语法符号
      }
    // [zh] 语句
    })();
    // [zh] return 返回
    return restorePromise;
  // [zh] 语句
  };

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`userInfo,`
    userInfo,
    // [zh] TypeScript/JS 语句：`isLoggedIn,`
    isLoggedIn,
    // [zh] TypeScript/JS 语句：`loggingOut,`
    loggingOut,
    // [zh] TypeScript/JS 语句：`memberLevelCode,`
    memberLevelCode,
    // [zh] TypeScript/JS 语句：`memberLevelName,`
    memberLevelName,
    // [zh] TypeScript/JS 语句：`memberGrowthValue,`
    memberGrowthValue,
    // [zh] TypeScript/JS 语句：`memberCenterData,`
    memberCenterData,
    // [zh] TypeScript/JS 语句：`loadMemberLevel,`
    loadMemberLevel,
    // [zh] TypeScript/JS 语句：`loadMemberCenter,`
    loadMemberCenter,
    // [zh] TypeScript/JS 语句：`login,`
    login,
    // [zh] TypeScript/JS 语句：`logout,`
    logout,
    // [zh] TypeScript/JS 语句：`prepareLogoutNavigation,`
    prepareLogoutNavigation,
    // [zh] TypeScript/JS 语句：`finishLogoutNavigation,`
    finishLogoutNavigation,
    // [zh] TypeScript/JS 语句：`fetchUserInfo,`
    fetchUserInfo,
    // [zh] TypeScript/JS 语句：`autoLogin,`
    autoLogin,
    // [zh] TypeScript/JS 语句：`ensureSession,`
    ensureSession,
    // [zh] TypeScript/JS 语句：`tryRestoreSession`
    tryRestoreSession
  // [zh] 语句
  };
// [zh] 语句
});
