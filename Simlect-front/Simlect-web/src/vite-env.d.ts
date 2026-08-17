

interface ImportMetaEnv {
  readonly VITE_DEV_PORT: string;
  readonly VITE_API_PROXY_TARGET: string;
  readonly VITE_WS: string;
  readonly VITE_WS_CHECK?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
