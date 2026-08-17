import { isProductOnSale as isOnSale } from '@/constants/backendEnums';

export { isProductOnSale } from '@/constants/backendEnums';

export function filterOnSaleProducts<T extends { status?: number | null; productId?: string }>(list: T[] | null | undefined): T[] {
  return (list || []).filter((item) => isOnSale(item));
}

export function pickDefaultSku<T extends { stock?: number | null }>(skuList: T[]): T | null {
  if (!skuList.length) return null;
  return skuList.find((sku) => Number(sku.stock) > 0) ?? skuList[0];
}
