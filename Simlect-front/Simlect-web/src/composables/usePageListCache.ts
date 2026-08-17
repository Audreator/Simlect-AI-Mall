import { nextTick, type Ref } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { clearPageListCache, getPageListCache, setPageListCache } from '@/utils/pageListCache';

type UsePageListCacheOptions = {
  
  cacheKey: string | (() => string);
  
  scrollRef?: Ref<HTMLElement | null | undefined>;
  getState: () => Record<string, unknown>;
  setState: (state: Record<string, unknown>) => void;
  
  afterRestore?: () => void | Promise<void>;
};

export function usePageListCache(options: UsePageListCacheOptions) {
  const resolveKey = () =>
    typeof options.cacheKey === 'function' ? options.cacheKey() : options.cacheKey;

  const readScrollTop = () => {
    const el = options.scrollRef?.value;
    if (el) return el.scrollTop;
    return window.scrollY || document.documentElement.scrollTop || 0;
  };

  const writeScrollTop = (top: number) => {
    const el = options.scrollRef?.value;
    if (el) {
      el.scrollTop = top;
      return;
    }
    window.scrollTo(0, top);
  };

  const save = () => {
    setPageListCache(resolveKey(), readScrollTop(), options.getState());
  };

  onBeforeRouteLeave(() => {
    save();
  });

  const tryRestore = async (): Promise<boolean> => {
    const cached = getPageListCache(resolveKey());
    if (!cached) return false;

    options.setState(cached.state);
    await options.afterRestore?.();
    await nextTick();

    const top = cached.scrollTop;
    writeScrollTop(top);
    requestAnimationFrame(() => {
      writeScrollTop(top);
      setTimeout(() => writeScrollTop(top), 50);
    });
    return true;
  };

  const clear = () => clearPageListCache(resolveKey());

  return { save, tryRestore, clear };
}
