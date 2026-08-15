// [zh] 导入 `vue`
import { onUnmounted, ref, type Ref } from 'vue';

// [zh] 导出模块成员
export function useInfiniteScroll(loadMore: () => Promise<void> | void, options?: { rootMargin?: string }) {
  // [zh] 声明变量 `sentinelRef`
  const sentinelRef = ref<HTMLElement | null>(null);
  // [zh] 声明变量 `loading`
  const loading = ref(false);
  // [zh] 声明变量 `finished`
  const finished = ref(false);
  // [zh] 声明变量 `observer`
  let observer: IntersectionObserver | null = null;

  // [zh] 声明变量 `bind`
  const bind = (scrollRoot?: Ref<HTMLElement | null>) => {
    // [zh] 语句
    observer?.disconnect();
    // [zh] 条件判断 if
    if (!sentinelRef.value) return;
    // [zh] TypeScript/JS 语句：`observer = new IntersectionObserver(`
    observer = new IntersectionObserver(
      // [zh] 代码块开始
      (entries) => {
        // [zh] 条件判断 if
        if (entries.some((e) => e.isIntersecting) && !loading.value && !finished.value) {
          // [zh] 语句
          void loadMore();
        // [zh] 语法符号
        }
      // [zh] TypeScript/JS 语句：`},`
      },
      // [zh] TypeScript/JS 语句：`{ root: scrollRoot?.value ?? null, rootMargin: options?`
      { root: scrollRoot?.value ?? null, rootMargin: options?.rootMargin ?? '120px', threshold: 0 }
    // [zh] 语句
    );
    // [zh] 语句
    observer.observe(sentinelRef.value);
  // [zh] 语句
  };

  // [zh] 语句
  onUnmounted(() => observer?.disconnect());

  // [zh] return 返回
  return { sentinelRef, loading, finished, bind };
// [zh] 语法符号
}
