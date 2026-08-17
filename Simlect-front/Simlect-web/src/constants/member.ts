
export const MEMBER_SILVER_GROWTH = 1000;
export const MEMBER_GOLD_GROWTH = 5000;

export interface MemberGrowthHints {

  nextLevelGrowth?: number | null;

  growthToNext?: number | null;
}

function resolveTierBounds(growth: number, hints?: MemberGrowthHints): { floor: number; ceil: number } {

  const backendCeil = hints?.nextLevelGrowth;
  if (typeof backendCeil === 'number' && backendCeil > 0 && growth < backendCeil) {
    const floor = backendCeil <= MEMBER_SILVER_GROWTH ? 0 : MEMBER_SILVER_GROWTH;
    return { floor, ceil: backendCeil };
  }
  if (growth >= MEMBER_SILVER_GROWTH) {
    return { floor: MEMBER_SILVER_GROWTH, ceil: MEMBER_GOLD_GROWTH };
  }
  return { floor: 0, ceil: MEMBER_SILVER_GROWTH };
}

export function calcMemberGrowthPercent(growth: number, hints?: MemberGrowthHints): number {
  const g = Math.max(0, Number(growth) || 0);
  if (g >= MEMBER_GOLD_GROWTH) return 100;

  const { floor, ceil } = resolveTierBounds(g, hints);
  const span = ceil - floor;
  if (span <= 0) return 100;

  const ratio = (g - floor) / span;
  const percent = Math.round(ratio * 100);
  if (percent <= 0) return 0;

  return Math.min(99, Math.max(4, percent));
}

export function calcMemberGrowthHint(growth: number, hints?: MemberGrowthHints): string {
  const g = Math.max(0, Number(growth) || 0);

  if (typeof hints?.growthToNext === 'number' && hints.growthToNext > 0) {
    return `距离下一等级还需 ${hints.growthToNext} 成长值`;
  }
  if (g >= MEMBER_GOLD_GROWTH) return '已达最高等级金卡会员';
  if (g >= MEMBER_SILVER_GROWTH) return `距离金卡还需 ${MEMBER_GOLD_GROWTH - g} 成长值`;
  return `距离银卡还需 ${MEMBER_SILVER_GROWTH - g} 成长值`;
}
