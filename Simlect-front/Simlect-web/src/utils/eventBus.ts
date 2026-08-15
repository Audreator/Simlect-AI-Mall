// [zh] TypeScript 类型 `Handler`（类比 Java interface/DTO）
type Handler = (payload?: unknown) => void;

// [zh] 声明变量 `handlers`
const handlers = new Map<string, Set<Handler>>();

// [zh] 导出模块成员
export const mitter = {
  // [zh] 代码块开始
  on(event: string, fn: Handler) {
    // [zh] 条件判断 if
    if (!handlers.has(event)) handlers.set(event, new Set());
    // [zh] 语句
    handlers.get(event)!.add(fn);
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  off(event: string, fn?: Handler) {
    // [zh] 条件判断 if
    if (!fn) {
      // [zh] 语句
      handlers.delete(event);
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 语句
    handlers.get(event)?.delete(fn);
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  emit(event: string, payload?: unknown) {
    // [zh] 语句
    handlers.get(event)?.forEach((fn) => fn(payload));
  // [zh] 语法符号
  }
// [zh] 语句
};
