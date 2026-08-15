// [zh] 导入 `vue`
import { ref } from 'vue';
// [zh] 导入 `pinia`
import { defineStore } from 'pinia';
// [zh] 导入 `@/api/modules`
import { couponApi } from '@/api/modules';

// [zh] 导出模块成员
export const useCouponStore = defineStore('coupon', () => {
  // [zh] 声明变量 `myCoupons`
  const myCoupons = ref<any[]>([]);

  // [zh] 声明变量 `fetchMyCoupons`
  const fetchMyCoupons = async () => {
    // [zh] 声明变量 `res`
    const res = await couponApi.loadUserCoupon({ pageNo: 1 });
    // [zh] 语句
    myCoupons.value = res?.list || [];
  // [zh] 语句
  };

  // [zh] return 返回
  return { myCoupons, fetchMyCoupons };
// [zh] 语句
});
