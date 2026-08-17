import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { defaultAlipayPayMethod as resolveDefaultAlipayPayMethod } from '@/constants/payChannel';
import { detectDevicePlatform, type DevicePlatform } from '@/utils/device';

export const useDeviceStore = defineStore('device', () => {
  const platform = ref<DevicePlatform>(detectDevicePlatform());

  const isMobile = computed(() => platform.value === 'mobile');
  const isDesktop = computed(() => platform.value === 'desktop');
  const defaultAlipayPayMethod = computed(() => resolveDefaultAlipayPayMethod(isMobile.value));

  const applyPlatform = (next: DevicePlatform) => {
    platform.value = next;
    if (typeof document !== 'undefined') {
      document.documentElement.dataset.platform = next;
    }
  };

  const sync = () => {
    applyPlatform(detectDevicePlatform());
  };

  return {
    platform,
    isMobile,
    isDesktop,
    defaultAlipayPayMethod,
    sync
  };
});
