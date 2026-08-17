import { ref } from 'vue';
import { defineStore } from 'pinia';
import { couponApi } from '@/api/modules';

export const useCouponStore = defineStore('coupon', () => {
  const myCoupons = ref<any[]>([]);

  const fetchMyCoupons = async () => {
    const res = await couponApi.loadUserCoupon({ pageNo: 1 });
    myCoupons.value = res?.list || [];
  };

  return { myCoupons, fetchMyCoupons };
});
