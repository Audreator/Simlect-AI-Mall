// [zh] 导入 `@/constants/backendEnums`
import { isProductOnSale as isOnSale } from '@/constants/backendEnums';

// [zh] 导出模块成员
export { isProductOnSale } from '@/constants/backendEnums';

// [zh] 导出模块成员
export function filterOnSaleProducts<T extends { status?: number | null; productId?: string }>(list: T[] | null | undefined): T[] {
  // [zh] return 返回
  return (list || []).filter((item) => isOnSale(item));
// [zh] 语法符号
}

// [zh] 导出模块成员
export function pickDefaultSku<T extends { stock?: number | null }>(skuList: T[]): T | null {
  // [zh] 条件判断 if
  if (!skuList.length) return null;
  // [zh] return 返回
  return skuList.find((sku) => Number(sku.stock) > 0) ?? skuList[0];
// [zh] 语法符号
}
