

const INVALID = /^\[object Object\]$/i;

export function normalizeCommentImagePath(value: unknown): string {
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed || INVALID.test(trimmed)) return '';
    return trimmed;
  }
  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>;
    if (typeof record.path === 'string') {
      return normalizeCommentImagePath(record.path);
    }
  }
  return '';
}

export function serializeCommentImagePaths(values: readonly unknown[]): string {
  return values.map(normalizeCommentImagePath).filter(Boolean).join(',');
}
