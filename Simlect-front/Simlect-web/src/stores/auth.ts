import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { accountApi } from '@/api/modules';
import { fetchMemberCenter } from '@/api/memberCenter';
import { useCartStore } from '@/stores/cart';

const MEMBER_CACHE_TTL = 5 * 60 * 1000;
const MEMBER_CACHE_KEY = 'eshop_member_level';
const MEMBER_CENTER_CACHE_KEY = 'eshop_member_center';

interface MemberLevelCache {
  levelCode: number;
  levelName: string;
  growthValue: number;
  timestamp: number;
}

function readCache<T>(key: string): T | null {
  try {
    const raw = sessionStorage.getItem(key);
    if (!raw) return null;
    const cache = JSON.parse(raw) as T & { timestamp: number };
    if (Date.now() - cache.timestamp > MEMBER_CACHE_TTL) {
      sessionStorage.removeItem(key);
      return null;
    }
    return cache as T;
  } catch {
    sessionStorage.removeItem(key);
    return null;
  }
}

function writeCache(key: string, data: Record<string, unknown>) {
  try {
    sessionStorage.setItem(key, JSON.stringify({ ...data, timestamp: Date.now() }));
  } catch {

  }
}

function clearCache(key: string) {
  try { sessionStorage.removeItem(key); } catch {  }
}

function hasUserSession(data: Record<string, any> | null | undefined): data is Record<string, any> {
  return !!data?.userId;
}

export const useAuthStore = defineStore('auth', () => {
  const userInfo = ref<Record<string, any> | null>(null);
  const isLoggedIn = computed(() => !!userInfo.value?.userId);

  let sessionPromise: Promise<boolean> | null = null;
  let sessionReady = false;

  const loggingOut = ref(false);

  const memberLevelCode = ref(1);
  const memberLevelName = ref('');
  const memberGrowthValue = ref(0);
  const memberCenterData = ref<Record<string, any> | null>(null);

  let memberLevelPromise: Promise<void> | null = null;

  const cachedLevel = readCache<MemberLevelCache>(MEMBER_CACHE_KEY);
  if (cachedLevel) {
    memberLevelCode.value = cachedLevel.levelCode;
    memberLevelName.value = cachedLevel.levelName;
    memberGrowthValue.value = cachedLevel.growthValue;
  }
  const cachedCenter = readCache<Record<string, any>>(MEMBER_CENTER_CACHE_KEY);
  if (cachedCenter) {
    memberCenterData.value = cachedCenter;
  }

  const applyMemberCenterData = (center: Record<string, any> | null) => {
    if (!center?.profile) {
      memberCenterData.value = null;
      return;
    }
    const profile = center.profile;
    const code = Number(profile.levelCode ?? 1);
    const name = profile.levelName || '';
    const growth = Number(profile.growthValue ?? 0);
    memberLevelCode.value = code;
    memberLevelName.value = name;
    memberGrowthValue.value = growth;
    memberCenterData.value = center;
    writeCache(MEMBER_CACHE_KEY, { levelCode: code, levelName: name, growthValue: growth });
    writeCache(MEMBER_CENTER_CACHE_KEY, center);
  };

  const loadMemberLevel = async (forceRefresh = false): Promise<void> => {
    if (!isLoggedIn.value) return;
    if (!forceRefresh) {
      const cachedCenter2 = readCache<Record<string, any>>(MEMBER_CENTER_CACHE_KEY);
      if (cachedCenter2) {
        applyMemberCenterData(cachedCenter2);
        return;
      }
      const cached2 = readCache<MemberLevelCache>(MEMBER_CACHE_KEY);
      if (cached2) {
        memberLevelCode.value = cached2.levelCode;
        memberLevelName.value = cached2.levelName;
        memberGrowthValue.value = cached2.growthValue;
        if (memberCenterData.value) return;
      }
    }
    if (memberLevelPromise) return memberLevelPromise;
    memberLevelPromise = (async () => {
      try {
        applyMemberCenterData(await fetchMemberCenter());
      } catch {

      }
    })().finally(() => {
      memberLevelPromise = null;
    });
    return memberLevelPromise;
  };

  const loadMemberCenter = async (forceRefresh = false): Promise<Record<string, any> | null> => {
    if (!isLoggedIn.value) return null;
    if (forceRefresh) {
      memberCenterData.value = null;
      clearCache(MEMBER_CENTER_CACHE_KEY);
      clearCache(MEMBER_CACHE_KEY);
    }
    if (memberCenterData.value) return memberCenterData.value;
    const cached2 = readCache<Record<string, any>>(MEMBER_CENTER_CACHE_KEY);
    if (cached2) {
      applyMemberCenterData(cached2);
      return memberCenterData.value;
    }
    await loadMemberLevel(forceRefresh);
    return memberCenterData.value;
  };

  const clearAuth = () => {
    userInfo.value = null;
    sessionReady = false;
    clearCache(MEMBER_CACHE_KEY);
    clearCache(MEMBER_CENTER_CACHE_KEY);
    memberLevelCode.value = 1;
    memberLevelName.value = '';
    memberGrowthValue.value = 0;
    memberCenterData.value = null;
    try {
      useCartStore().resetCart();
    } catch {

    }
  };

  const fetchUserInfo = async () => {
    if (!isLoggedIn.value) return;
    userInfo.value = { ...(userInfo.value || {}), ...(await accountApi.getUserInfo()) };
  };

  const login = async (payload: Record<string, unknown>) => {
    const data = await accountApi.login({
      ...payload,
      password: String(payload.password ?? '')
    });
    userInfo.value = data;
    sessionReady = true;
    try {
      await fetchUserInfo();
    } catch {

    }
    loadMemberLevel();
    try {
      await useCartStore().fetchCartCount();
    } catch {

    }
  };

  const prepareLogoutNavigation = () => {
    loggingOut.value = true;
  };

  const logout = async (silent = false) => {
    if (!silent && isLoggedIn.value) await accountApi.logout();
    clearAuth();
  };

  const finishLogoutNavigation = () => {
    loggingOut.value = false;
  };

  const ensureSession = (): Promise<boolean> => {
    if (sessionReady && isLoggedIn.value) return Promise.resolve(true);
    if (sessionPromise) return sessionPromise;

    sessionPromise = (async () => {
      try {
        const data = await accountApi.autoLogin();
        if (!hasUserSession(data)) {
          clearAuth();
          return false;
        }
        userInfo.value = data;
        sessionReady = true;
        try {
          await fetchUserInfo();
        } catch {

        }
        loadMemberLevel();
        return true;
      } catch {
        clearAuth();
        return false;
      }
    })().finally(() => {
      sessionPromise = null;
    });

    return sessionPromise;
  };

  const autoLogin = ensureSession;

  let restorePromise: Promise<void> | null = null;
  const tryRestoreSession = (): Promise<void> => {
    if (sessionReady && isLoggedIn.value) return Promise.resolve();
    if (restorePromise) return restorePromise;
    restorePromise = (async () => {
      try {
        const data = await accountApi.autoLogin();
        if (hasUserSession(data)) {
          userInfo.value = data;
          sessionReady = true;
          try { await fetchUserInfo(); } catch {  }
          loadMemberLevel();
        }
      } catch {

      } finally {
        restorePromise = null;
      }
    })();
    return restorePromise;
  };

  return {
    userInfo,
    isLoggedIn,
    loggingOut,
    memberLevelCode,
    memberLevelName,
    memberGrowthValue,
    memberCenterData,
    loadMemberLevel,
    loadMemberCenter,
    login,
    logout,
    prepareLogoutNavigation,
    finishLogoutNavigation,
    fetchUserInfo,
    autoLogin,
    ensureSession,
    tryRestoreSession
  };
});
