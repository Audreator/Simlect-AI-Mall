// [zh] 导入 `@/api/modules`
import { userMemberApi } from '@/api/modules';
// [zh] 导入 `@/constants/member`
import { MEMBER_GOLD_GROWTH, MEMBER_SILVER_GROWTH } from '@/constants/member';


// [zh] 导出模块成员
export function buildMemberCenterFromProfile(profile: Record<string, any>) {
  // [zh] 声明变量 `growth`
  const growth = profile?.growthValue ?? 0;
  // [zh] 声明变量 `levelCode`
  const levelCode = profile?.levelCode ?? 1;
  // [zh] 声明变量 `rewards`
  const rewards = [
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`levelCode: 1,`
      levelCode: 1,
      // [zh] TypeScript/JS 语句：`levelName: '普通会员',`
      levelName: '普通会员',
      // [zh] TypeScript/JS 语句：`growthThreshold: 0,`
      growthThreshold: 0,
      // [zh] TypeScript/JS 语句：`rewardTitle: '入会礼遇',`
      rewardTitle: '入会礼遇',
      // [zh] TypeScript/JS 语句：`rewardDesc: '注册即享基础会员权益',`
      rewardDesc: '注册即享基础会员权益',
      // [zh] TypeScript/JS 语句：`unlocked: true,`
      unlocked: true,
      // [zh] TypeScript/JS 语句：`claimed: true,`
      claimed: true,
      // [zh] TypeScript/JS 语句：`claimable: false`
      claimable: false
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`levelCode: 2,`
      levelCode: 2,
      // [zh] TypeScript/JS 语句：`levelName: '银卡会员',`
      levelName: '银卡会员',
      // [zh] TypeScript/JS 语句：`growthThreshold: MEMBER_SILVER_GROWTH,`
      growthThreshold: MEMBER_SILVER_GROWTH,
      // [zh] TypeScript/JS 语句：`rewardTitle: '银卡升级礼',`
      rewardTitle: '银卡升级礼',
      // [zh] TypeScript/JS 语句：`rewardDesc: '专属优惠券 + 20 成长值',`
      rewardDesc: '专属优惠券 + 20 成长值',
      // [zh] TypeScript/JS 语句：`unlocked: growth >= MEMBER_SILVER_GROWTH && levelCode >`
      unlocked: growth >= MEMBER_SILVER_GROWTH && levelCode >= 2,
      // [zh] TypeScript/JS 语句：`claimed: false,`
      claimed: false,
      // [zh] TypeScript/JS 语句：`claimable: growth >= MEMBER_SILVER_GROWTH && levelCode `
      claimable: growth >= MEMBER_SILVER_GROWTH && levelCode >= 2
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`levelCode: 3,`
      levelCode: 3,
      // [zh] TypeScript/JS 语句：`levelName: '金卡会员',`
      levelName: '金卡会员',
      // [zh] TypeScript/JS 语句：`growthThreshold: MEMBER_GOLD_GROWTH,`
      growthThreshold: MEMBER_GOLD_GROWTH,
      // [zh] TypeScript/JS 语句：`rewardTitle: '金卡升级礼',`
      rewardTitle: '金卡升级礼',
      // [zh] TypeScript/JS 语句：`rewardDesc: '专属优惠券 + 50 成长值',`
      rewardDesc: '专属优惠券 + 50 成长值',
      // [zh] TypeScript/JS 语句：`unlocked: growth >= MEMBER_GOLD_GROWTH && levelCode >= `
      unlocked: growth >= MEMBER_GOLD_GROWTH && levelCode >= 3,
      // [zh] TypeScript/JS 语句：`claimed: false,`
      claimed: false,
      // [zh] TypeScript/JS 语句：`claimable: growth >= MEMBER_GOLD_GROWTH && levelCode >=`
      claimable: growth >= MEMBER_GOLD_GROWTH && levelCode >= 3
    // [zh] 语法符号
    }
  // [zh] 语句
  ];

  // [zh] 声明变量 `nextLevelCode`
  let nextLevelCode: number | null = 2;
  // [zh] 声明变量 `nextLevelGrowth`
  let nextLevelGrowth: number | null = MEMBER_SILVER_GROWTH;
  // [zh] 声明变量 `growthToNext`
  let growthToNext = MEMBER_SILVER_GROWTH - growth;
  // [zh] 条件判断 if
  if (growth >= MEMBER_GOLD_GROWTH) {
    // [zh] 语句
    nextLevelCode = null;
    // [zh] 语句
    nextLevelGrowth = null;
    // [zh] 语句
    growthToNext = 0;
  // [zh] 代码块开始
  } else if (growth >= MEMBER_SILVER_GROWTH) {
    // [zh] 语句
    nextLevelCode = 3;
    // [zh] 语句
    nextLevelGrowth = MEMBER_GOLD_GROWTH;
    // [zh] 语句
    growthToNext = MEMBER_GOLD_GROWTH - growth;
  // [zh] 语法符号
  }

  // [zh] return 返回
  return { profile, rewards, nextLevelCode, nextLevelGrowth, growthToNext };
// [zh] 语法符号
}

// [zh] 函数 `isMemberCenterPayload`
function isMemberCenterPayload(data: any) {
  // [zh] return 返回
  return data && data.profile && Array.isArray(data.rewards);
// [zh] 语法符号
}


// [zh] 导出模块成员
export async function fetchMemberCenter() {
  // [zh] 声明变量 `loaders`
  const loaders = [
    // [zh] TypeScript/JS 语句：`() => userMemberApi.loadMemberCenter(),`
    () => userMemberApi.loadMemberCenter(),
    // [zh] TypeScript/JS 语句：`() => userMemberApi.getProfileWithCenter(),`
    () => userMemberApi.getProfileWithCenter(),
    // [zh] TypeScript/JS 语句：`() => userMemberApi.getMemberCenter()`
    () => userMemberApi.getMemberCenter()
  // [zh] 语句
  ];

  // [zh] for 循环
  for (const load of loaders) {
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `data`
      const data = await load();
      // [zh] 条件判断 if
      if (isMemberCenterPayload(data)) return data;
    // [zh] 代码块开始
    } catch {
      
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }

  // [zh] 声明变量 `profile`
  const profile = await userMemberApi.getProfile();
  // [zh] return 返回
  return buildMemberCenterFromProfile(profile || {});
// [zh] 语法符号
}
