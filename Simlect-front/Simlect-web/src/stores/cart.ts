// [zh] 导入 `vue`
import { ref } from 'vue';
// [zh] 导入 `pinia`
import { defineStore } from 'pinia';
// [zh] 导入 `@/api/modules`
import { cartApi } from '@/api/modules';

// [zh] 导出模块成员
export const useCartStore = defineStore('cart', () => {

  // [zh] 声明变量 `cartCount`
  const cartCount = ref(0);

  // [zh] 声明变量 `cartTotalQty`
  const cartTotalQty = ref(0);

  // [zh] 声明变量 `fetchCartCount`
  const fetchCartCount = async () => {
    // [zh] 声明变量 `res`
    const res = await cartApi.loadProductCart({ pageNo: 1 });
    // [zh] 声明变量 `list`
    const list = res?.list || [];
    // [zh] 语句
    cartCount.value = res?.totalCount ?? list.length;
    // [zh] TypeScript/JS 语句：`cartTotalQty.value = list.reduce(`
    cartTotalQty.value = list.reduce(
      // [zh] TypeScript/JS 语句：`(sum: number, row: { buyCount?: number | string }) => s`
      (sum: number, row: { buyCount?: number | string }) => sum + (Number(row.buyCount) || 0),
      // [zh] TypeScript/JS 语句：`0`
      0
    // [zh] 语句
    );
  // [zh] 语句
  };

  // [zh] 声明变量 `resetCart`
  const resetCart = () => {
    // [zh] 语句
    cartCount.value = 0;
    // [zh] 语句
    cartTotalQty.value = 0;
  // [zh] 语句
  };

  // [zh] return 返回
  return { cartCount, cartTotalQty, fetchCartCount, resetCart };
// [zh] 语句
});
