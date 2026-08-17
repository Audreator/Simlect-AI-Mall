import { reactive } from 'vue';
import { defineStore } from 'pinia';

export interface SearchPayload {
  keyWords: string;
  categoryId?: string;
  priceFrom?: string;
  priceTo?: string;
  sortType?: string;
  sortField?: string;
}

export const useSearchStore = defineStore('search', () => {
  const payload = reactive<SearchPayload>({
    keyWords: '',
    categoryId: '',
    priceFrom: '',
    priceTo: '',
    sortType: '',
    sortField: ''
  });

  const setSearch = (data: Partial<SearchPayload>) => {
    Object.assign(payload, {
      keyWords: data.keyWords?.trim() ?? '',
      categoryId: data.categoryId ?? '',
      priceFrom: data.priceFrom ?? '',
      priceTo: data.priceTo ?? '',
      sortType: data.sortType ?? '',
      sortField: data.sortField ?? ''
    });
  };

  const clear = () => {
    setSearch({ keyWords: '', categoryId: '', priceFrom: '', priceTo: '', sortType: '', sortField: '' });
  };

  return { payload, setSearch, clear };
});
