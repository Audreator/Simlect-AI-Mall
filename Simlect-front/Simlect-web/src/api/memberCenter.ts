import { userMemberApi } from '@/api/modules';
import { MEMBER_GOLD_GROWTH, MEMBER_SILVER_GROWTH } from '@/constants/member';


export function buildMemberCenterFromProfile(profile: Record<string, any>) {
  const growth = profile?.growthValue ?? 0;
  const levelCode = profile?.levelCode ?? 1;
  const rewards = [
    {
      levelCode: 1,
      levelName: '普通会员',
      growthThreshold: 0,
      rewardTitle: '入会礼遇',
      rewardDesc: '注册即享基础会员权益',
      unlocked: true,
      claimed: true,
      claimable: false
    },
    {
      levelCode: 2,
      levelName: '银卡会员',
      growthThreshold: MEMBER_SILVER_GROWTH,
      rewardTitle: '银卡升级礼',
      rewardDesc: '专属优惠券 + 20 成长值',
      unlocked: growth >= MEMBER_SILVER_GROWTH && levelCode >= 2,
      claimed: false,
      claimable: growth >= MEMBER_SILVER_GROWTH && levelCode >= 2
    },
    {
      levelCode: 3,
      levelName: '金卡会员',
      growthThreshold: MEMBER_GOLD_GROWTH,
      rewardTitle: '金卡升级礼',
      rewardDesc: '专属优惠券 + 50 成长值',
      unlocked: growth >= MEMBER_GOLD_GROWTH && levelCode >= 3,
      claimed: false,
      claimable: growth >= MEMBER_GOLD_GROWTH && levelCode >= 3
    }
  ];

  let nextLevelCode: number | null = 2;
  let nextLevelGrowth: number | null = MEMBER_SILVER_GROWTH;
  let growthToNext = MEMBER_SILVER_GROWTH - growth;
  if (growth >= MEMBER_GOLD_GROWTH) {
    nextLevelCode = null;
    nextLevelGrowth = null;
    growthToNext = 0;
  } else if (growth >= MEMBER_SILVER_GROWTH) {
    nextLevelCode = 3;
    nextLevelGrowth = MEMBER_GOLD_GROWTH;
    growthToNext = MEMBER_GOLD_GROWTH - growth;
  }

  return { profile, rewards, nextLevelCode, nextLevelGrowth, growthToNext };
}

function isMemberCenterPayload(data: any) {
  return data && data.profile && Array.isArray(data.rewards);
}


export async function fetchMemberCenter() {
  const loaders = [
    () => userMemberApi.loadMemberCenter(),
    () => userMemberApi.getProfileWithCenter(),
    () => userMemberApi.getMemberCenter()
  ];

  for (const load of loaders) {
    try {
      const data = await load();
      if (isMemberCenterPayload(data)) return data;
    } catch {
      
    }
  }

  const profile = await userMemberApi.getProfile();
  return buildMemberCenterFromProfile(profile || {});
}
