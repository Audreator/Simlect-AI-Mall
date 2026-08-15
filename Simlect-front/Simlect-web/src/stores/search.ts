// [zh] 导入 `vue`
import { reactive } from 'vue';
// [zh] 导入 `pinia`
import { defineStore } from 'pinia';

// [zh] 导出模块成员
export interface SearchPayload {
  // [zh] 语句
  keyWords: string;
  // [zh] 语句
  categoryId?: string;
  // [zh] 语句
  priceFrom?: string;
  // [zh] 语句
  priceTo?: string;
  // [zh] 语句
  sortType?: string;
  // [zh] 语句
  sortField?: string;
// [zh] 语法符号
}

// [zh] 导出模块成员
export const useSearchStore = defineStore('search', () => {
  // [zh] 声明变量 `payload`
  const payload = reactive<SearchPayload>({
    // [zh] TypeScript/JS 语句：`keyWords: '',`
    keyWords: '',
    // [zh] TypeScript/JS 语句：`categoryId: '',`
    categoryId: '',
    // [zh] TypeScript/JS 语句：`priceFrom: '',`
    priceFrom: '',
    // [zh] TypeScript/JS 语句：`priceTo: '',`
    priceTo: '',
    // [zh] TypeScript/JS 语句：`sortType: '',`
    sortType: '',
    // [zh] TypeScript/JS 语句：`sortField: ''`
    sortField: ''
  // [zh] 语句
  });

  // [zh] 声明变量 `setSearch`
  const setSearch = (data: Partial<SearchPayload>) => {
    // [zh] 代码块开始
    Object.assign(payload, {
      // [zh] TypeScript/JS 语句：`keyWords: data.keyWords?.trim() ?? '',`
      keyWords: data.keyWords?.trim() ?? '',
      // [zh] TypeScript/JS 语句：`categoryId: data.categoryId ?? '',`
      categoryId: data.categoryId ?? '',
      // [zh] TypeScript/JS 语句：`priceFrom: data.priceFrom ?? '',`
      priceFrom: data.priceFrom ?? '',
      // [zh] TypeScript/JS 语句：`priceTo: data.priceTo ?? '',`
      priceTo: data.priceTo ?? '',
      // [zh] TypeScript/JS 语句：`sortType: data.sortType ?? '',`
      sortType: data.sortType ?? '',
      // [zh] TypeScript/JS 语句：`sortField: data.sortField ?? ''`
      sortField: data.sortField ?? ''
    // [zh] 语句
    });
  // [zh] 语句
  };

  // [zh] 声明变量 `clear`
  const clear = () => {
    // [zh] 语句
    setSearch({ keyWords: '', categoryId: '', priceFrom: '', priceTo: '', sortType: '', sortField: '' });
  // [zh] 语句
  };

  // [zh] return 返回
  return { payload, setSearch, clear };
// [zh] 语句
});
