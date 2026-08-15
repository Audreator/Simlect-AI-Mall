

// [zh] TypeScript 类型 `ImportMetaEnv`（类比 Java interface/DTO）
interface ImportMetaEnv {
  // [zh] 语句
  readonly VITE_DEV_PORT: string;
  // [zh] 语句
  readonly VITE_API_PROXY_TARGET: string;
  // [zh] 语句
  readonly VITE_WS: string;
  // [zh] 语句
  readonly VITE_WS_CHECK?: string;
// [zh] 语法符号
}

// [zh] TypeScript 类型 `ImportMeta`（类比 Java interface/DTO）
interface ImportMeta {
  // [zh] 语句
  readonly env: ImportMetaEnv;
// [zh] 语法符号
}
