import {
  inject,
  onActivated,
  onDeactivated,
  onMounted,
  onUnmounted,
  type InjectionKey
} from 'vue';

export type PageRefreshHandler = () => void | Promise<void>;
export type ScrollElGetter = () => HTMLElement | null | undefined;

export interface PageRefreshRegistration {
  refresh: PageRefreshHandler;
  getScrollEl?: ScrollElGetter;
}

export const PULL_REFRESH_REGISTER_KEY: InjectionKey<(reg: PageRefreshRegistration | null) => void> =
  Symbol('pullRefreshRegister');


export function usePageRefresh(
  refresh: PageRefreshHandler,
  options?: { getScrollEl?: ScrollElGetter }
) {
  const register = inject(PULL_REFRESH_REGISTER_KEY, null);
  if (!register) return;

  const apply = () => register({ refresh, getScrollEl: options?.getScrollEl });
  const clear = () => register(null);

  onMounted(apply);
  onActivated(apply);
  onUnmounted(clear);
  onDeactivated(clear);
}

export function getScrollTop(el: HTMLElement | null | undefined): number {
  if (!el) return window.scrollY || document.documentElement.scrollTop || 0;
  return el.scrollTop;
}
