import { ref } from 'vue';
import { defineStore } from 'pinia';
import { cartApi } from '@/api/modules';

export const useCartStore = defineStore('cart', () => {

  const cartCount = ref(0);

  const cartTotalQty = ref(0);

  const fetchCartCount = async () => {
    const res = await cartApi.loadProductCart({ pageNo: 1 });
    const list = res?.list || [];
    cartCount.value = res?.totalCount ?? list.length;
    cartTotalQty.value = list.reduce(
      (sum: number, row: { buyCount?: number | string }) => sum + (Number(row.buyCount) || 0),
      0
    );
  };

  const resetCart = () => {
    cartCount.value = 0;
    cartTotalQty.value = 0;
  };

  return { cartCount, cartTotalQty, fetchCartCount, resetCart };
});
