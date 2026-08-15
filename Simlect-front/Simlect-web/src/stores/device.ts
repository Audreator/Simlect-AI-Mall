// [zh] 导入 `pinia`
import { defineStore } from 'pinia';
// [zh] 导入 `vue`
import { computed, ref } from 'vue';
// [zh] 导入 `@/constants/payChannel`
import { defaultAlipayPayMethod as resolveDefaultAlipayPayMethod } from '@/constants/payChannel';
// [zh] 导入 `@/utils/device`
import { detectDevicePlatform, type DevicePlatform } from '@/utils/device';

// [zh] 导出模块成员
export const useDeviceStore = defineStore('device', () => {
  // [zh] 声明变量 `platform`
  const platform = ref<DevicePlatform>(detectDevicePlatform());

  // [zh] 声明变量 `isMobile`
  const isMobile = computed(() => platform.value === 'mobile');
  // [zh] 声明变量 `isDesktop`
  const isDesktop = computed(() => platform.value === 'desktop');
  // [zh] 声明变量 `defaultAlipayPayMethod`
  const defaultAlipayPayMethod = computed(() => resolveDefaultAlipayPayMethod(isMobile.value));

  // [zh] 声明变量 `applyPlatform`
  const applyPlatform = (next: DevicePlatform) => {
    // [zh] 语句
    platform.value = next;
    // [zh] 条件判断 if
    if (typeof document !== 'undefined') {
      // [zh] 语句
      document.documentElement.dataset.platform = next;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `sync`
  const sync = () => {
    // [zh] 语句
    applyPlatform(detectDevicePlatform());
  // [zh] 语句
  };

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`platform,`
    platform,
    // [zh] TypeScript/JS 语句：`isMobile,`
    isMobile,
    // [zh] TypeScript/JS 语句：`isDesktop,`
    isDesktop,
    // [zh] TypeScript/JS 语句：`defaultAlipayPayMethod,`
    defaultAlipayPayMethod,
    // [zh] TypeScript/JS 语句：`sync`
    sync
  // [zh] 语句
  };
// [zh] 语句
});
