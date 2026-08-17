import { storeToRefs } from 'pinia';
import { useDeviceStore } from '@/stores/device';

export const useDevice = () => {
  const store = useDeviceStore();
  return {
    ...storeToRefs(store),
    sync: store.sync
  };
};
