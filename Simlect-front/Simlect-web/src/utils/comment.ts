
export type CommentLevel = '' | 'good' | 'medium' | 'bad';

export const matchCommentLevel = (star: unknown, level: CommentLevel): boolean => {
  if (!level) return true;
  const s = Number(star);
  if (!Number.isFinite(s)) return false;
  if (level === 'good') return s === 5;
  if (level === 'medium') return s >= 3 && s <= 4;
  if (level === 'bad') return s >= 1 && s <= 2;
  return true;
};


export const maskCommenterName = (name?: string | null): string => {
  const raw = String(name ?? '').trim();
  const stars = '*****';
  if (!raw) return `匿${stars}户`;
  const chars = Array.from(raw); 
  if (chars.length === 1) return `${chars[0]}${stars}`;
  return `${chars[0]}${stars}${chars[chars.length - 1]}`;
};
