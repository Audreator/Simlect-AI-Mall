// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`inject,`
  inject,
  // [zh] TypeScript/JS 语句：`onActivated,`
  onActivated,
  // [zh] TypeScript/JS 语句：`onDeactivated,`
  onDeactivated,
  // [zh] TypeScript/JS 语句：`onMounted,`
  onMounted,
  // [zh] TypeScript/JS 语句：`onUnmounted,`
  onUnmounted,
  // [zh] TypeScript 类型 `InjectionKey`（类比 Java interface/DTO）
  type InjectionKey
// [zh] 语句
} from 'vue';

// [zh] 导出模块成员
export type PageRefreshHandler = () => void | Promise<void>;
// [zh] 导出模块成员
export type ScrollElGetter = () => HTMLElement | null | undefined;

// [zh] 导出模块成员
export interface PageRefreshRegistration {
  // [zh] 语句
  refresh: PageRefreshHandler;
  // [zh] 语句
  getScrollEl?: ScrollElGetter;
// [zh] 语法符号
}

// [zh] 导出模块成员
export const PULL_REFRESH_REGISTER_KEY: InjectionKey<(reg: PageRefreshRegistration | null) => void> =
  // [zh] 语句
  Symbol('pullRefreshRegister');


// [zh] 导出模块成员
export function usePageRefresh(
  // [zh] TypeScript/JS 语句：`refresh: PageRefreshHandler,`
  refresh: PageRefreshHandler,
  // [zh] TypeScript/JS 语句：`options?: { getScrollEl?: ScrollElGetter }`
  options?: { getScrollEl?: ScrollElGetter }
// [zh] 代码块开始
) {
  // [zh] 声明变量 `register`
  const register = inject(PULL_REFRESH_REGISTER_KEY, null);
  // [zh] 条件判断 if
  if (!register) return;

  // [zh] 声明变量 `apply`
  const apply = () => register({ refresh, getScrollEl: options?.getScrollEl });
  // [zh] 声明变量 `clear`
  const clear = () => register(null);

  // [zh] 语句
  onMounted(apply);
  // [zh] 语句
  onActivated(apply);
  // [zh] 语句
  onUnmounted(clear);
  // [zh] 语句
  onDeactivated(clear);
// [zh] 语法符号
}

// [zh] 导出模块成员
export function getScrollTop(el: HTMLElement | null | undefined): number {
  // [zh] 条件判断 if
  if (!el) return window.scrollY || document.documentElement.scrollTop || 0;
  // [zh] return 返回
  return el.scrollTop;
// [zh] 语法符号
}
