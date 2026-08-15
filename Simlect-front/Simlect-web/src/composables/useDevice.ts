// [zh] 导入 `pinia`
import { storeToRefs } from 'pinia';
// [zh] 导入 `@/stores/device`
import { useDeviceStore } from '@/stores/device';

// [zh] 导出模块成员
export const useDevice = () => {
  // [zh] 声明变量 `store`
  const store = useDeviceStore();
  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`...storeToRefs(store),`
    ...storeToRefs(store),
    // [zh] TypeScript/JS 语句：`sync: store.sync`
    sync: store.sync
  // [zh] 语句
  };
// [zh] 语句
};
