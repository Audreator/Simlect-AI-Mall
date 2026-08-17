<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="member-center-page">
    <!-- [zh] 开始标签 `<section>` -->
    <section v-if="center?.profile" class="member-card" :class="cardLevelClass" data-member-card>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="level-tag">{{ center.profile.levelName || '普通会员' }}</p>
      <!-- [zh] 开始标签 `<h2>` -->
      <h2 class="level-code">Lv.{{ center.profile.levelCode || 1 }}</h2>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="growth-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span>成长值</span>
        <!-- [zh] 开始标签 `<strong>` -->
        <strong>{{ center.profile.growthValue ?? 0 }}</strong>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<el-progress>` -->
      <el-progress
        :percentage="growthPercent"
        :stroke-width="10"
        :show-text="false"
        :color="growthBarColor"
      />
      <!-- [zh] 开始标签 `<p>` -->
      <p class="growth-hint">{{ growthHint }}</p>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section v-if="center?.rewards?.length" class="rewards card-flat">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3>等级奖励</h3>
      <!-- [zh] 开始标签 `<ul>` -->
      <ul class="reward-list">
        <!-- [zh] 开始标签 `<li>` -->
        <li v-for="item in center.rewards" :key="item.levelCode" class="reward-item">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="reward-main">
            <!-- [zh] 开始标签 `<p>` -->
            <p class="reward-level">{{ item.levelName }}</p>
            <!-- [zh] 开始标签 `<p>` -->
            <p class="reward-title">{{ item.rewardTitle }}</p>
            <!-- [zh] 开始标签 `<p>` -->
            <p class="reward-desc">{{ item.rewardDesc }}</p>
            <!-- [zh] 开始标签 `<p>` -->
            <p class="reward-threshold">需成长值 ≥ {{ item.growthThreshold }}</p>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="reward-action">
            <!-- [zh] 开始标签 `<span>` -->
            <span v-if="item.claimed" class="status done">已领取</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span v-else-if="!item.unlocked" class="status lock">未达标</span>
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button
              v-else-if="item.claimable"
              type="primary"
              size="small"
              :loading="claiming === item.levelCode"
              @click="onClaim(item.levelCode)"
            >
              <!-- [zh] 模板内容：`领取` -->
              领取
            <!-- [zh] 闭合标签 `</el-button>` -->
            </el-button>
            <!-- [zh] 开始标签 `<span>` -->
            <span v-else class="status auto">已享有</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</li>` -->
        </li>
      <!-- [zh] 闭合标签 `</ul>` -->
      </ul>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="rules card-flat">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3>成长规则</h3>
      <!-- [zh] 开始标签 `<ul>` -->
      <ul>
        <!-- [zh] 开始标签 `<li>` -->
        <li>每消费 100 元获得 1 成长值（至少 1 点）</li>
        <!-- [zh] 开始标签 `<li>` -->
        <li>每日签到 +5 成长值</li>
        <!-- [zh] 开始标签 `<li>` -->
        <li>确认收货 +10 成长值</li>
        <!-- [zh] 开始标签 `<li>` -->
        <li>1000 成长值升级银卡，5000 成长值升级金卡</li>
      <!-- [zh] 闭合标签 `</ul>` -->
      </ul>
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/sign" class="sign-link">去签到</RouterLink>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { userMemberApi } from '@/api/modules';
import { useAuthStore } from '@/stores/auth';
import { calcMemberGrowthHint, calcMemberGrowthPercent } from '@/constants/member';
import { confirmAction } from '@/utils/confirm';
import { toast } from '@/utils/toast';

const authStore = useAuthStore();
const center = ref<any>(null);
const claiming = ref<number | null>(null);

const growthHints = computed(() => ({
  nextLevelGrowth: center.value?.nextLevelGrowth ?? null,
  growthToNext: center.value?.growthToNext ?? null
}));
const growthPercent = computed(() =>
  calcMemberGrowthPercent(center.value?.profile?.growthValue ?? 0, growthHints.value)
);
const growthHint = computed(() =>
  calcMemberGrowthHint(center.value?.profile?.growthValue ?? 0, growthHints.value)
);
const growthBarColor = computed(() => {
  const code = Number(center.value?.profile?.levelCode ?? 1);
  if (code >= 3) return '#c9a962';
  if (code >= 2) return '#a8a8ad';
  return '#c9a962';
});

const cardLevelClass = computed(() => {
  const code = Number(center.value?.profile?.levelCode ?? 1);
  if (code >= 3) return 'level-gold';
  if (code >= 2) return 'level-silver';
  return 'level-default';
});

const load = async () => {
  center.value = await authStore.loadMemberCenter();
};

const onClaim = async (levelCode: number) => {
  const reward = center.value?.rewards?.find((r: any) => r.levelCode === levelCode);
  if (!reward) return;
  const ok = await confirmAction(
    `领取「${reward.rewardTitle}」\n${reward.rewardDesc}\n确定要领取吗？`,
    {
      title: '领取升级礼',
      confirmButtonText: '确定领取'
    }
  );
  if (!ok) return;
  claiming.value = levelCode;
  try {
    await userMemberApi.claimLevelReward(levelCode);
    center.value = await authStore.loadMemberCenter(true);
    toast.success('领取成功');
  } catch (e: any) {
    toast.error(e?.info || e?.message || '领取失败');
  } finally {
    claiming.value = null;
  }
};

onMounted(load);
</script>

/* [zh] 样式声明 */
<style lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.member-center-page {` */
.member-center-page {
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;
  /* [zh] 样式规则 `max-width: 720px;` */
  max-width: 720px;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.member-center-page [data-member-card] {` */
.member-center-page [data-member-card] {
  /* [zh] 样式规则 `padding: 20px !important;` */
  padding: 20px !important;
  /* [zh] 样式规则 `text-align: center !important;` */
  text-align: center !important;
  /* [zh] 样式规则 `transition: all 0.3s ease !important;` */
  transition: all 0.3s ease !important;
  /* [zh] 样式规则 `border-radius: $radius-card !important;` */
  border-radius: $radius-card !important;
  /* [zh] 样式规则 `margin-bottom: 16px !important;` */
  margin-bottom: 16px !important;
  /* [zh] 样式规则 `position: relative !important;` */
  position: relative !important;
  /* [zh] 样式规则 `z-index: 1 !important;` */
  z-index: 1 !important;
  /* [zh] 样式规则 `background: transparent !important;` */
  background: transparent !important;
  /* [zh] 样式规则 `-webkit-backdrop-filter: none !important` */
  -webkit-backdrop-filter: none !important;
  /* [zh] 样式规则 `backdrop-filter: none !important;` */
  backdrop-filter: none !important;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.member-center-page [data-member-card].l` */
.member-center-page [data-member-card].level-default {
  /* [zh] 样式规则 `background: linear-gradient(135deg, #f5f` */
  background: linear-gradient(135deg, #f5f5f7 0%, #e0e0e0 100%) !important;
  /* [zh] 样式规则 `border: 1px solid #d1d1d6 !important;` */
  border: 1px solid #d1d1d6 !important;
  /* [zh] 样式规则 `box-shadow: none !important;` */
  box-shadow: none !important;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.member-center-page [data-member-card].l` */
.member-center-page [data-member-card].level-silver {
  /* [zh] 样式规则 `background: linear-gradient(135deg, #e8e` */
  background: linear-gradient(135deg, #e8e8ed 0%, #c8c8cc 50%, #a8a8ad 100%) !important;
  /* [zh] 样式规则 `border: 1px solid #a8a8ad !important;` */
  border: 1px solid #a8a8ad !important;
  /* [zh] 样式规则 `box-shadow: 0 4px 20px rgba(168, 168, 17` */
  box-shadow: 0 4px 20px rgba(168, 168, 173, 0.3) !important;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.member-center-page [data-member-card].level-silver .level-tag,
/* [zh] 样式规则 `.member-center-page [data-member-card].l` */
.member-center-page [data-member-card].level-silver .level-code {
  /* [zh] 样式规则 `color: #4a4a4f !important;` */
  color: #4a4a4f !important;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.member-center-page [data-member-card].l` */
.member-center-page [data-member-card].level-gold {
  /* [zh] 样式规则 `background: linear-gradient(135deg, #fff` */
  background: linear-gradient(135deg, #fff8e7 0%, #f5e6c8 50%, #e8d5a3 100%) !important;
  /* [zh] 样式规则 `border: 1px solid #c9a962 !important;` */
  border: 1px solid #c9a962 !important;
  /* [zh] 样式规则 `box-shadow: 0 4px 20px rgba(201, 169, 98` */
  box-shadow: 0 4px 20px rgba(201, 169, 98, 0.4) !important;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.member-center-page [data-member-card].level-gold .level-tag,
/* [zh] 样式规则 `.member-center-page [data-member-card].l` */
.member-center-page [data-member-card].level-gold .level-code {
  /* [zh] 样式规则 `color: #8b7355 !important;` */
  color: #8b7355 !important;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.level-tag {` */
.level-tag {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.level-code {` */
.level-code {
  /* [zh] 样式规则 `margin: 8px 0 16px;` */
  margin: 8px 0 16px;
  /* [zh] 样式规则 `font-size: 28px;` */
  font-size: 28px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.growth-row {` */
.growth-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.growth-hint {` */
.growth-hint {
  /* [zh] 样式规则 `margin: 10px 0 0;` */
  margin: 10px 0 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-secondary;` */
  color: $color-text-secondary;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.rewards {` */
.rewards {
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;

  /* [zh] 样式规则 `h3 {` */
  h3 {
    /* [zh] 样式规则 `margin: 0 0 12px;` */
    margin: 0 0 12px;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.reward-list {` */
.reward-list {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `list-style: none;` */
  list-style: none;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.reward-item {` */
.reward-item {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `padding: 14px 0;` */
  padding: 14px 0;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
    /* [zh] 样式规则 `padding-bottom: 0;` */
    padding-bottom: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.reward-main {` */
.reward-main {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.reward-level {` */
.reward-level {
  /* [zh] 样式规则 `margin: 0 0 4px;` */
  margin: 0 0 4px;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.reward-title {` */
.reward-title {
  /* [zh] 样式规则 `margin: 0 0 4px;` */
  margin: 0 0 4px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.reward-desc,
/* [zh] 样式规则 `.reward-threshold {` */
.reward-threshold {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-secondary;` */
  color: $color-text-secondary;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.reward-action {` */
.reward-action {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `padding-top: 4px;` */
  padding-top: 4px;

  /* [zh] 样式规则 `.status {` */
  .status {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;

    /* [zh] 样式规则 `&.done {` */
    &.done {
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.lock {` */
    &.lock {
      /* [zh] 样式规则 `color: $color-text-disabled;` */
      color: $color-text-disabled;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.auto {` */
    &.auto {
      /* [zh] 样式规则 `color: $color-success;` */
      color: $color-success;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.rules {` */
.rules {
  /* [zh] 样式规则 `padding: 16px;` */
  padding: 16px;

  /* [zh] 样式规则 `h3 {` */
  h3 {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `ul {` */
  ul {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `padding-left: 18px;` */
    padding-left: 18px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: $color-text-secondary;` */
    color: $color-text-secondary;
    /* [zh] 样式规则 `line-height: 1.8;` */
    line-height: 1.8;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sign-link {` */
.sign-link {
  /* [zh] 样式规则 `display: inline-block;` */
  display: inline-block;
  /* [zh] 样式规则 `margin-top: 12px;` */
  margin-top: 12px;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
/* [zh] 样式规则 `}` */
}
</style>
