import { onUnmounted, ref, type Ref } from 'vue';

export function useInfiniteScroll(loadMore: () => Promise<void> | void, options?: { rootMargin?: string }) {
  const sentinelRef = ref<HTMLElement | null>(null);
  const loading = ref(false);
  const finished = ref(false);
  let observer: IntersectionObserver | null = null;

  const bind = (scrollRoot?: Ref<HTMLElement | null>) => {
    observer?.disconnect();
    if (!sentinelRef.value) return;
    observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting) && !loading.value && !finished.value) {
          void loadMore();
        }
      },
      { root: scrollRoot?.value ?? null, rootMargin: options?.rootMargin ?? '120px', threshold: 0 }
    );
    observer.observe(sentinelRef.value);
  };

  onUnmounted(() => observer?.disconnect());

  return { sentinelRef, loading, finished, bind };
}
