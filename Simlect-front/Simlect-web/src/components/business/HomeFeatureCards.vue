<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="home-feature-cards">
    <!-- [zh] 开始标签 `<section>` -->
    <section class="feature-grid">
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-for="item in featureItems"
        :key="item.path"
        type="button"
        class="feature-card"
        @click="handleClick(item)"
      >
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon class="feature-icon" :style="{ color: iconColor }" :size="22">
          <!-- [zh] 开始标签 `<component>` -->
          <component :is="item.icon" />
        <!-- [zh] 闭合标签 `</el-icon>` -->
        </el-icon>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="feature-label">{{ item.label }}</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span v-if="item.badge" class="feature-badge">{{ item.badge }}</span>
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@element-plus/icons-vue`
import { Star, Medal, Check, Discount } from '@element-plus/icons-vue';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();

// [zh] 声明变量 `featureItems`
const featureItems = ref([
  // [zh] TypeScript/JS 语句：`{ icon: Star, label: '猜你喜欢', path: '#recommend-section'`
  { icon: Star, label: '猜你喜欢', path: '#recommend-section', badge: '', isAnchor: true },
  // [zh] TypeScript/JS 语句：`{ icon: Medal, label: '会员中心', path: '/member-center', b`
  { icon: Medal, label: '会员中心', path: '/member-center', badge: '', isAnchor: false },
  // [zh] TypeScript/JS 语句：`{ icon: Check, label: '签到有礼', path: '/sign', badge: '',`
  { icon: Check, label: '签到有礼', path: '/sign', badge: '', isAnchor: false },
  // [zh] TypeScript/JS 语句：`{ icon: Discount, label: '优惠券', path: '/coupons', badge`
  { icon: Discount, label: '优惠券', path: '/coupons', badge: '', isAnchor: false },
// [zh] 语句
]);

// [zh] 声明变量 `iconColor`
const iconColor = computed(() => {
  // [zh] 声明变量 `level`
  const level = authStore.memberLevelCode;
  // [zh] 条件判断 if
  if (level >= 3) {
    // [zh] return 返回
    return '#B8860B';
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (level >= 2) {
    // [zh] return 返回
    return '#757575';
  // [zh] 语法符号
  }
  // [zh] return 返回
  return '#1D1D1F';
// [zh] 语句
});

// [zh] 声明变量 `handleClick`
const handleClick = (item: { path: string; isAnchor: boolean }) => {
  // [zh] 条件判断 if
  if (item.isAnchor) {
    // [zh] 声明变量 `element`
    const element = document.querySelector(item.path);
    // [zh] 条件判断 if
    if (element) {
      // [zh] 声明变量 `top`
      const top = element.getBoundingClientRect().top + window.scrollY - 60;
      // [zh] 语句
      window.scrollTo({ top, behavior: 'smooth' });
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    router.push(item.path);
  // [zh] 语法符号
  }
// [zh] 语句
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.home-feature-cards {` */
.home-feature-cards {
  /* [zh] 样式规则 `margin: 12px $app-page-gutter 0;` */
  margin: 12px $app-page-gutter 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feature-grid {` */
.feature-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(2, 1fr);` */
  grid-template-columns: repeat(2, 1fr);
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feature-card {` */
.feature-card {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: row;` */
  flex-direction: row;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: flex-start;` */
  justify-content: flex-start;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding: 16px 14px;` */
  padding: 16px 14px;
  /* [zh] 样式规则 `border: 1px solid var(--ios-surface-bord` */
  border: 1px solid var(--ios-surface-border, rgba(0, 0, 0, 0.045));
  /* [zh] 样式规则 `border-radius: var(--ios-surface-radius,` */
  border-radius: var(--ios-surface-radius, 16px);
  /* [zh] 样式规则 `background: var(--ios-surface-bg, #fff);` */
  background: var(--ios-surface-bg, #fff);
  /* [zh] 样式规则 `box-shadow: var(--ios-surface-shadow);` */
  box-shadow: var(--ios-surface-shadow);
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: transform $transition-fast;` */
  transition: transform $transition-fast;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.98);` */
    transform: scale(0.98);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feature-icon {` */
.feature-icon {
  /* [zh] 样式规则 `transition: color 0.2s ease;` */
  transition: color 0.2s ease;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feature-label {` */
.feature-label {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.feature-badge {` */
.feature-badge {
  /* [zh] 样式规则 `position: absolute;` */
  position: absolute;
  /* [zh] 样式规则 `top: 6px;` */
  top: 6px;
  /* [zh] 样式规则 `right: 6px;` */
  right: 6px;
  /* [zh] 样式规则 `padding: 1px 5px;` */
  padding: 1px 5px;
  /* [zh] 样式规则 `border-radius: $radius-pill;` */
  border-radius: $radius-pill;
  /* [zh] 样式规则 `background: $color-gold-soft;` */
  background: $color-gold-soft;
  /* [zh] 样式规则 `font-size: 10px;` */
  font-size: 10px;
  /* [zh] 样式规则 `font-weight: 700;` */
  font-weight: 700;
  /* [zh] 样式规则 `color: $color-price;` */
  color: $color-price;
/* [zh] 样式规则 `}` */
}
</style>