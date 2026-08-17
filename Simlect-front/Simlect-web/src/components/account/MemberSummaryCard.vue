<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<RouterLink>` -->
  <RouterLink to="/member-center" class="member-summary" :class="cardLevelClass">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="member-summary__head">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="level-badge" :style="badgeStyle">{{ profile?.levelName || '普通会员' }}</div>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="level-lv" :style="lvStyle">Lv.{{ profile?.levelCode || 1 }}</span>
      <!-- [zh] 开始标签 `<el-icon>` -->
      <el-icon class="arrow"><ArrowRight /></el-icon>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="growth-row">
      <!-- [zh] 开始标签 `<span>` -->
      <span>成长值 {{ profile?.growthValue ?? 0 }}</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="hint">{{ growthHint }}</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<el-progress>` -->
    <el-progress :percentage="growthPercent" :stroke-width="8" :show-text="false" :color="growthBarColor" />
    <!-- [zh] 开始标签 `<p>` -->
    <p v-if="(claimableCount ?? 0) > 0" class="reward-tip">有 {{ claimableCount }} 项升级礼待领取</p>
  <!-- [zh] 闭合标签 `</RouterLink>` -->
  </RouterLink>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowRight } from '@element-plus/icons-vue';
import { calcMemberGrowthHint, calcMemberGrowthPercent } from '@/constants/member';

const props = defineProps<{
  profile?: Record<string, any> | null;
  claimableCount?: number;

  nextLevelGrowth?: number | null;
  growthToNext?: number | null;
}>();

const growthHints = computed(() => ({
  nextLevelGrowth: props.nextLevelGrowth ?? null,
  growthToNext: props.growthToNext ?? null
}));
const growthPercent = computed(() =>
  calcMemberGrowthPercent(props.profile?.growthValue ?? 0, growthHints.value)
);
const growthHint = computed(() =>
  calcMemberGrowthHint(props.profile?.growthValue ?? 0, growthHints.value)
);
const growthBarColor = computed(() => {
  const code = Number(props.profile?.levelCode ?? 1);
  if (code >= 3) return '#c9a962';
  if (code >= 2) return '#a8a8ad';
  return '#c9a962';
});

const cardLevelClass = computed(() => {
  const code = Number(props.profile?.levelCode ?? 1);
  if (code >= 3) return 'level-gold';
  if (code >= 2) return 'level-silver';
  return 'level-default';
});

const badgeStyle = computed(() => {
  const code = Number(props.profile?.levelCode ?? 1);
  if (code >= 3) {
    return {
      background: 'linear-gradient(135deg, #e8c96a 0%, #c9a962 100%)',
      color: '#fff',
      border: '1px solid rgba(201, 169, 98, 0.45)'
    };
  }
  if (code >= 2) {
    return {
      background: 'linear-gradient(135deg, #d4d4d8 0%, #a8a8ad 100%)',
      color: '#fff',
      border: '1px solid rgba(168, 168, 173, 0.4)'
    };
  }
  return {
    background: 'var(--ios-fill-muted, #f5f5f7)',
    color: '#3c3c43',
    border: '1px solid var(--ios-separator, rgba(60, 60, 67, 0.1))'
  };
});

const lvStyle = computed(() => {
  const code = Number(props.profile?.levelCode ?? 1);
  if (code >= 3) {
    return { color: '#8b7355' };
  }
  if (code >= 2) {
    return { color: '#4a4a4f' };
  }
  return { color: '#333333' };
});
</script>

/* [zh] 样式声明 */
<style lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.member-summary {` */
.member-summary {
  /* [zh] 样式规则 `display: block;` */
  display: block;
  /* [zh] 样式规则 `margin: 0 $app-page-gutter 8px;` */
  margin: 0 $app-page-gutter 8px;
  /* [zh] 样式规则 `padding: 14px 16px;` */
  padding: 14px 16px;
  /* [zh] 样式规则 `text-decoration: none;` */
  text-decoration: none;
  /* [zh] 样式规则 `color: inherit;` */
  color: inherit;
  /* [zh] 样式规则 `border-radius: var(--ios-surface-radius,` */
  border-radius: var(--ios-surface-radius, 16px);
  /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
  -webkit-tap-highlight-color: transparent;
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `z-index: 1;` */
  z-index: 1;
  /* [zh] 样式规则 `backdrop-filter: none;` */
  backdrop-filter: none;
  /* [zh] 样式规则 `-webkit-backdrop-filter: none;` */
  -webkit-backdrop-filter: none;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `opacity: 0.96;` */
    opacity: 0.96;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.member-summary.level-default {` */
.member-summary.level-default {
  /* [zh] 样式规则 `background: var(--ios-surface-bg, #fff);` */
  background: var(--ios-surface-bg, #fff);
  /* [zh] 样式规则 `border: 1px solid var(--ios-surface-bord` */
  border: 1px solid var(--ios-surface-border, rgba(0, 0, 0, 0.045));
  /* [zh] 样式规则 `box-shadow: var(--ios-surface-shadow);` */
  box-shadow: var(--ios-surface-shadow);

  /* [zh] 样式规则 `.level-lv {` */
  .level-lv {
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.member-summary.level-silver {` */
.member-summary.level-silver {
  /* [zh] 样式规则 `background: linear-gradient(165deg, #faf` */
  background: linear-gradient(165deg, #fafafa 0%, #f0f0f3 100%);
  /* [zh] 样式规则 `border: 1px solid rgba(168, 168, 173, 0.` */
  border: 1px solid rgba(168, 168, 173, 0.32);
  /* [zh] 样式规则 `box-shadow: var(--ios-surface-shadow);` */
  box-shadow: var(--ios-surface-shadow);

  /* [zh] 样式规则 `.level-lv {` */
  .level-lv {
    /* [zh] 样式规则 `color: #636366;` */
    color: #636366;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.member-summary.level-gold {` */
.member-summary.level-gold {
  /* [zh] 样式规则 `background: linear-gradient(165deg, #fff` */
  background: linear-gradient(165deg, #fffcf5 0%, #faf6eb 100%);
  /* [zh] 样式规则 `border: 1px solid rgba(201, 169, 98, 0.2` */
  border: 1px solid rgba(201, 169, 98, 0.28);
  /* [zh] 样式规则 `box-shadow: var(--ios-surface-shadow);` */
  box-shadow: var(--ios-surface-shadow);

  /* [zh] 样式规则 `.level-lv {` */
  .level-lv {
    /* [zh] 样式规则 `color: #8b7355;` */
    color: #8b7355;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.member-summary__head {` */
.member-summary__head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.level-badge {` */
.level-badge {
  /* [zh] 样式规则 `padding: 2px 10px;` */
  padding: 2px 10px;
  /* [zh] 样式规则 `border-radius: $radius-pill;` */
  border-radius: $radius-pill;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.level-lv {` */
.level-lv {
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.arrow {` */
.arrow {
  /* [zh] 样式规则 `margin-left: auto;` */
  margin-left: auto;
  /* [zh] 样式规则 `color: $color-text-disabled;` */
  color: $color-text-disabled;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.growth-row {` */
.growth-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `align-items: baseline;` */
  align-items: baseline;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;

  /* [zh] 样式规则 `.hint {` */
  .hint {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.reward-tip {` */
.reward-tip {
  /* [zh] 样式规则 `margin: 8px 0 0;` */
  margin: 8px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.member-summary.card {` */
.member-summary.card {
  /* [zh] 样式规则 `backdrop-filter: none !important;` */
  backdrop-filter: none !important;
  /* [zh] 样式规则 `-webkit-backdrop-filter: none !important` */
  -webkit-backdrop-filter: none !important;
/* [zh] 样式规则 `}` */
}
</style>
