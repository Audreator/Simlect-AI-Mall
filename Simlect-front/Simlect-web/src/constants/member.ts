
// [zh] 导出模块成员
export const MEMBER_SILVER_GROWTH = 1000;
// [zh] 导出模块成员
export const MEMBER_GOLD_GROWTH = 5000;

// [zh] 导出模块成员
export interface MemberGrowthHints {

  // [zh] 语句
  nextLevelGrowth?: number | null;

  // [zh] 语句
  growthToNext?: number | null;
// [zh] 语法符号
}

// [zh] 函数 `resolveTierBounds`
function resolveTierBounds(growth: number, hints?: MemberGrowthHints): { floor: number; ceil: number } {

  // [zh] 声明变量 `backendCeil`
  const backendCeil = hints?.nextLevelGrowth;
  // [zh] 条件判断 if
  if (typeof backendCeil === 'number' && backendCeil > 0 && growth < backendCeil) {
    // [zh] 声明变量 `floor`
    const floor = backendCeil <= MEMBER_SILVER_GROWTH ? 0 : MEMBER_SILVER_GROWTH;
    // [zh] return 返回
    return { floor, ceil: backendCeil };
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (growth >= MEMBER_SILVER_GROWTH) {
    // [zh] return 返回
    return { floor: MEMBER_SILVER_GROWTH, ceil: MEMBER_GOLD_GROWTH };
  // [zh] 语法符号
  }
  // [zh] return 返回
  return { floor: 0, ceil: MEMBER_SILVER_GROWTH };
// [zh] 语法符号
}

// [zh] 导出模块成员
export function calcMemberGrowthPercent(growth: number, hints?: MemberGrowthHints): number {
  // [zh] 声明变量 `g`
  const g = Math.max(0, Number(growth) || 0);
  // [zh] 条件判断 if
  if (g >= MEMBER_GOLD_GROWTH) return 100;

  // [zh] 语句
  const { floor, ceil } = resolveTierBounds(g, hints);
  // [zh] 声明变量 `span`
  const span = ceil - floor;
  // [zh] 条件判断 if
  if (span <= 0) return 100;

  // [zh] 声明变量 `ratio`
  const ratio = (g - floor) / span;
  // [zh] 声明变量 `percent`
  const percent = Math.round(ratio * 100);
  // [zh] 条件判断 if
  if (percent <= 0) return 0;

  // [zh] return 返回
  return Math.min(99, Math.max(4, percent));
// [zh] 语法符号
}

// [zh] 导出模块成员
export function calcMemberGrowthHint(growth: number, hints?: MemberGrowthHints): string {
  // [zh] 声明变量 `g`
  const g = Math.max(0, Number(growth) || 0);

  // [zh] 条件判断 if
  if (typeof hints?.growthToNext === 'number' && hints.growthToNext > 0) {
    // [zh] return 返回
    return `距离下一等级还需 ${hints.growthToNext} 成长值`;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (g >= MEMBER_GOLD_GROWTH) return '已达最高等级金卡会员';
  // [zh] 条件判断 if
  if (g >= MEMBER_SILVER_GROWTH) return `距离金卡还需 ${MEMBER_GOLD_GROWTH - g} 成长值`;
  // [zh] return 返回
  return `距离银卡还需 ${MEMBER_SILVER_GROWTH - g} 成长值`;
// [zh] 语法符号
}
