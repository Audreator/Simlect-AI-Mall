
export const PRIMARY_TAB_PATHS = ['/', '/search', '/cart', '/account'] as const;

export type PrimaryTabPath = (typeof PRIMARY_TAB_PATHS)[number];

export function isPrimaryTabPath(path: string) {
  return (PRIMARY_TAB_PATHS as readonly string[]).includes(path);
}
