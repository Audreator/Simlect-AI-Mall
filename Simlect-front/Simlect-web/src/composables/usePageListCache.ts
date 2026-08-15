// [zh] 导入 `vue`
import { nextTick, type Ref } from 'vue';
// [zh] 导入 `vue-router`
import { onBeforeRouteLeave } from 'vue-router';
// [zh] 导入 `@/utils/pageListCache`
import { clearPageListCache, getPageListCache, setPageListCache } from '@/utils/pageListCache';

// [zh] TypeScript 类型 `UsePageListCacheOptions`（类比 Java interface/DTO）
type UsePageListCacheOptions = {
  
  // [zh] 语句
  cacheKey: string | (() => string);
  
  // [zh] 语句
  scrollRef?: Ref<HTMLElement | null | undefined>;
  // [zh] 语句
  getState: () => Record<string, unknown>;
  // [zh] 语句
  setState: (state: Record<string, unknown>) => void;
  
  // [zh] 语句
  afterRestore?: () => void | Promise<void>;
// [zh] 语句
};

// [zh] 导出模块成员
export function usePageListCache(options: UsePageListCacheOptions) {
  // [zh] 声明变量 `resolveKey`
  const resolveKey = () =>
    // [zh] 语句
    typeof options.cacheKey === 'function' ? options.cacheKey() : options.cacheKey;

  // [zh] 声明变量 `readScrollTop`
  const readScrollTop = () => {
    // [zh] 声明变量 `el`
    const el = options.scrollRef?.value;
    // [zh] 条件判断 if
    if (el) return el.scrollTop;
    // [zh] return 返回
    return window.scrollY || document.documentElement.scrollTop || 0;
  // [zh] 语句
  };

  // [zh] 声明变量 `writeScrollTop`
  const writeScrollTop = (top: number) => {
    // [zh] 声明变量 `el`
    const el = options.scrollRef?.value;
    // [zh] 条件判断 if
    if (el) {
      // [zh] 语句
      el.scrollTop = top;
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 语句
    window.scrollTo(0, top);
  // [zh] 语句
  };

  // [zh] 声明变量 `save`
  const save = () => {
    // [zh] 语句
    setPageListCache(resolveKey(), readScrollTop(), options.getState());
  // [zh] 语句
  };

  // [zh] 代码块开始
  onBeforeRouteLeave(() => {
    // [zh] 语句
    save();
  // [zh] 语句
  });

  // [zh] 声明变量 `tryRestore`
  const tryRestore = async (): Promise<boolean> => {
    // [zh] 声明变量 `cached`
    const cached = getPageListCache(resolveKey());
    // [zh] 条件判断 if
    if (!cached) return false;

    // [zh] 语句
    options.setState(cached.state);
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await options.afterRestore?.();
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await nextTick();

    // [zh] 声明变量 `top`
    const top = cached.scrollTop;
    // [zh] 语句
    writeScrollTop(top);
    // [zh] 代码块开始
    requestAnimationFrame(() => {
      // [zh] 语句
      writeScrollTop(top);
      // [zh] 语句
      setTimeout(() => writeScrollTop(top), 50);
    // [zh] 语句
    });
    // [zh] return 返回
    return true;
  // [zh] 语句
  };

  // [zh] 声明变量 `clear`
  const clear = () => clearPageListCache(resolveKey());

  // [zh] return 返回
  return { save, tryRestore, clear };
// [zh] 语法符号
}
