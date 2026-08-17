
export function resolveSafeRedirect(raw: unknown, fallback = '/'): string {
  const path = String(raw ?? '').trim();
  if (!path.startsWith('/') || path.startsWith('//')) return fallback;
  if (path.startsWith('/login') || path.startsWith('/register')) return fallback;
  return path;
}
