
export const IMAGE_BASE = '/api/file/getResource?sourceName=';

const THUMB_SUFFIX = '_thumbnail';

const INVALID_IMAGE_PATH = /^\[object Object\]$/i;

export function extractUploadPath(value: unknown): string {
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed || INVALID_IMAGE_PATH.test(trimmed)) return '';
    return trimmed;
  }
  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>;
    if (typeof record.path === 'string') {
      return extractUploadPath(record.path);
    }
  }
  return '';
}

export function joinImagePaths(paths: unknown[]): string {
  return paths.map(extractUploadPath).filter(Boolean).join(',');
}

export function splitImagePaths(raw?: string | null): string[] {
  if (!raw) return [];
  return String(raw)
    .split(',')
    .map((item) => extractUploadPath(item))
    .filter(Boolean);
}

export function resolveImageUrl(
  source?: string | null,
  options?: { useThumbnail?: boolean; index?: number }
): string {
  if (!source) return '';
  const raw = String(source).trim();
  if (!raw) return '';
  if (raw.startsWith('http://') || raw.startsWith('https://')) {
    return raw.split(',')[0].trim();
  }

  const parts = raw.split(',').map((s) => s.trim()).filter(Boolean);
  let path = parts[options?.index ?? 0] ?? '';
  if (!path) return '';

  const useThumbnail = options?.useThumbnail !== false;
  if (!useThumbnail && path.includes(THUMB_SUFFIX)) {
    path = path.replace(THUMB_SUFFIX, '');
  }

  return `${IMAGE_BASE}${path}`;
}

export function resolveAvatarUrl(avatar?: string | null): string {
  if (!avatar || !String(avatar).trim()) return '';
  const av = String(avatar).trim();
  if (av === 'avatar.png') return '';
  if (av.startsWith('http://') || av.startsWith('https://')) return av;
  return `${IMAGE_BASE}${av}`;
}

export function pickProductCover(product: Record<string, any>): string {
  return product?.cover || product?.productCover || product?.image || '';
}
