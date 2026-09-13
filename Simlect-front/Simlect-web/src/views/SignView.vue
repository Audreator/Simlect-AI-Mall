<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="sign-page">
    <!-- [zh] 开始标签 `<section>` -->
    <section class="sign-stats">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="stats-row">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="stat-card">
          <!-- [zh] 开始标签 `<p>` -->
          <p class="stat-value">{{ signData.continuousDays }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="stat-label">连续签到</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="stat-card">
          <!-- [zh] 开始标签 `<p>` -->
          <p class="stat-value">{{ signData.totalSignDays }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="stat-label">累计签到</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="stat-card">
          <!-- [zh] 开始标签 `<p>` -->
          <p class="stat-value">{{ signData.supplementCount }}</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="stat-label">补签次数</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="stats-tip">累计签到 30 天获得 1 次补签次数</p>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="sign-reward-hint">
      <!-- [zh] 开始标签 `<svg>` -->
      <svg class="hint-icon" viewBox="0 0 20 20" width="16" height="16" fill="none">
        <!-- [zh] 开始标签 `<rect>` -->
        <rect x="2" y="4" width="16" height="4" rx="1" fill="#c9a962"/>
        <!-- [zh] 开始标签 `<path>` -->
        <path d="M4 8v7a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V8" stroke="#c9a962" stroke-width="1.2"/>
        <!-- [zh] 开始标签 `<path>` -->
        <path d="M10 4V17M7 2l3 2 3-2" stroke="#c9a962" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
      <!-- [zh] 闭合标签 `</svg>` -->
      </svg>
      <!-- [zh] 开始标签 `<span>` -->
      <span>达到运营配置的连续签到周期后，将自动发放优惠券奖励。</span>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="calendar-card card">
      <!-- [zh] 开始标签 `<header>` -->
      <header class="calendar-head">
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="nav-btn" @click="prevMonth">‹</button>
        <!-- [zh] 开始标签 `<h3>` -->
        <h3>{{ currentYear }}年{{ currentMonth + 1 }}月</h3>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="nav-btn" @click="nextMonth">›</button>
      <!-- [zh] 闭合标签 `</header>` -->
      </header>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="week-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span v-for="w in weekdays" :key="w">{{ w }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="days-grid">
        <!-- [zh] 开始标签 `<button>` -->
        <button
          v-for="(day, index) in calendarDays"
          :key="index"
          type="button"
          class="day-cell"
          :class="day.className"
          :disabled="!day.date"
          @click="handleDayClick(day)"
        >
          <!-- [zh] 开始标签 `<span>` -->
          <span class="day-num">{{ day.day }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span v-if="day.signed" class="signed-mark">✓</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span v-if="day.supplement" class="sup-tag">补</span>
        <!-- [zh] 闭合标签 `</button>` -->
        </button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<footer>` -->
    <footer class="sign-action">
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" size="large" round :disabled="isViewingCurrentMonth && todaySigned" @click="doSign">
        <!-- [zh] Mustache 插值表达式 -->
        {{ !isViewingCurrentMonth ? '返回本月签到' : todaySigned ? '今日已签到' : '立即签到' }}
      <!-- [zh] 闭合标签 `</el-button>` -->
      </el-button>
    <!-- [zh] 闭合标签 `</footer>` -->
    </footer>

    <!-- [zh] 开始标签 `<el-dialog>` -->
    <el-dialog v-model="supplementVisible" title="补签确认" width="90%" style="max-width: 360px">
      <!-- [zh] 开始标签 `<p>` -->
      <p>确定消耗 1 次补签机会，补签 {{ supplementLabel }} 吗？</p>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #footer>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button @click="supplementVisible = false">取消</el-button>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" :loading="supplementLoading" @click="doSupplement">确认补签</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { signApi } from '@/api/modules';
import { toast } from '@/utils/toast';
import { usePageRefresh } from '@/composables/pullRefresh';

const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
const today = new Date();
const currentYear = ref(today.getFullYear());
const currentMonth = ref(today.getMonth());
const supplementVisible = ref(false);
const supplementLoading = ref(false);
const supplementLabel = ref('');
const supplementDateParam = ref('');

const signData = reactive({
  continuousDays: 0,
  supplementCount: 0,
  totalSignDays: 0,
  signDays: [] as string[]
});

const yearMonth = () =>
  `${currentYear.value}${String(currentMonth.value + 1).padStart(2, '0')}`;

const isViewingCurrentMonth = computed(
  () => currentYear.value === today.getFullYear() && currentMonth.value === today.getMonth()
);
const todayDate = `${today.getFullYear()}${String(today.getMonth() + 1).padStart(2, '0')}${String(
  today.getDate()
).padStart(2, '0')}`;
const todaySigned = computed(
  () => isViewingCurrentMonth.value && signData.signDays.includes(todayDate)
);

const calendarDays = computed(() => {
  const days: Array<{
    day: number;
    date: string | null;
    signed: boolean;
    supplement: boolean;
    className: string;
  }> = [];

  const firstDay = new Date(currentYear.value, currentMonth.value, 1);
  const lastDay = new Date(currentYear.value, currentMonth.value + 1, 0);
  const startDayOfWeek = firstDay.getDay();
  const totalDays = lastDay.getDate();
  const prevLastDay = new Date(currentYear.value, currentMonth.value, 0).getDate();

  for (let i = startDayOfWeek - 1; i >= 0; i--) {
    days.push({ day: prevLastDay - i, date: null, signed: false, supplement: false, className: 'is-other' });
  }

  for (let d = 1; d <= totalDays; d++) {
    const dateStr = `${yearMonth()}${String(d).padStart(2, '0')}`;
    const isToday =
      d === today.getDate() &&
      currentMonth.value === today.getMonth() &&
      currentYear.value === today.getFullYear();
    const isPast =
      currentYear.value < today.getFullYear() ||
      (currentYear.value === today.getFullYear() && currentMonth.value < today.getMonth()) ||
      (isToday === false &&
        currentYear.value === today.getFullYear() &&
        currentMonth.value === today.getMonth() &&
        d < today.getDate());

    const signed = signData.signDays.includes(dateStr);
    const canSupplement =
      isViewingCurrentMonth.value && !signed && !isToday && isPast && signData.supplementCount > 0;

    let className = '';
    if (isToday) className += ' is-today';
    if (signed) className += ' is-signed';
    if (canSupplement) className += ' can-supplement';

    days.push({
      day: d,
      date: dateStr,
      signed,
      supplement: canSupplement,
      className
    });

  }

  const remain = 42 - days.length;
  for (let i = 1; i <= remain; i++) {
    days.push({ day: i, date: null, signed: false, supplement: false, className: 'is-other' });
  }

  return days;
});

const loadSignCalendar = async () => {
  const data = await signApi.getSignCalendar(yearMonth());
  signData.continuousDays = data?.continuousDays ?? 0;
  signData.supplementCount = data?.supplementCount ?? 0;
  signData.totalSignDays = data?.totalSignDays ?? 0;
  signData.signDays = data?.signDays ?? [];
};

const prevMonth = () => {
  if (currentMonth.value === 0) {
    currentMonth.value = 11;
    currentYear.value--;
  } else {
    currentMonth.value--;
  }
  loadSignCalendar();
};

const nextMonth = () => {
  if (currentMonth.value === 11) {
    currentMonth.value = 0;
    currentYear.value++;
  } else {
    currentMonth.value++;
  }
  loadSignCalendar();
};

const doSign = async () => {
  if (!isViewingCurrentMonth.value) {
    currentYear.value = today.getFullYear();
    currentMonth.value = today.getMonth();
    await loadSignCalendar();
    return;
  }
  if (todaySigned.value) return;
  await signApi.sign();
  toast.success('签到成功');
  await loadSignCalendar();
};

const handleDayClick = (day: { supplement?: boolean; date?: string | null; day: number }) => {
  if (!day.supplement || !day.date) return;
  supplementLabel.value = `${currentYear.value}年${currentMonth.value + 1}月${day.day}日`;
  supplementDateParam.value = day.date;
  supplementVisible.value = true;
};

const doSupplement = async () => {
  supplementLoading.value = true;
  try {
    await signApi.msign(supplementDateParam.value);
    supplementVisible.value = false;
    toast.success('补签成功');
    loadSignCalendar();
  } finally {
    supplementLoading.value = false;
  }
};

onMounted(loadSignCalendar);
usePageRefresh(loadSignCalendar);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.sign-page {` */
.sign-page {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sign-stats {` */
.sign-stats {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stats-row {` */
.stats-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `align-items: stretch;` */
  align-items: stretch;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: 360px;` */
  max-width: 360px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stat-card {` */
.stat-card {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
  /* [zh] 样式规则 `max-width: 112px;` */
  max-width: 112px;
  /* [zh] 样式规则 `background: linear-gradient(135deg, $col` */
  background: linear-gradient(135deg, $color-gold-soft, #fff);
  /* [zh] 样式规则 `border: 1px solid $color-gold-muted;` */
  border: 1px solid $color-gold-muted;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `padding: 10px 6px;` */
  padding: 10px 6px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;

  /* [zh] 样式规则 `.stat-value {` */
  .stat-value {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 20px;` */
    font-size: 20px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `line-height: 1.1;` */
    line-height: 1.1;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.stat-label {` */
  .stat-label {
    /* [zh] 样式规则 `margin: 4px 0 0;` */
    margin: 4px 0 0;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stats-tip {` */
.stats-tip {
  /* [zh] 样式规则 `margin: 6px 0 0;` */
  margin: 6px 0 0;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sign-reward-hint {` */
.sign-reward-hint {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `padding: 8px 14px;` */
  padding: 8px 14px;
  /* [zh] 样式规则 `max-width: 360px;` */
  max-width: 360px;
  /* [zh] 样式规则 `background: linear-gradient(135deg, #fef` */
  background: linear-gradient(135deg, #fef7e6, #fef0d5);
  /* [zh] 样式规则 `border: 1px solid #f5e2b5;` */
  border: 1px solid #f5e2b5;
  /* [zh] 样式规则 `border-radius: 10px;` */
  border-radius: 10px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: #8b6f3a;` */
  color: #8b6f3a;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;

  /* [zh] 样式规则 `.hint-icon {` */
  .hint-icon {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `display: block;` */
    display: block;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `strong {` */
  strong {
    /* [zh] 样式规则 `color: #c9a962;` */
    color: #c9a962;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.calendar-card {` */
.calendar-card {
  /* [zh] 样式规则 `flex: 1 1 auto;` */
  flex: 1 1 auto;
  /* [zh] 样式规则 `min-height: 0;` */
  min-height: 0;
  /* [zh] 样式规则 `padding: 10px 10px 8px;` */
  padding: 10px 10px 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.calendar-head {` */
.calendar-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `margin-bottom: 8px;` */
  margin-bottom: 8px;

  /* [zh] 样式规则 `h3 {` */
  h3 {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.nav-btn {` */
  .nav-btn {
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: $color-bg;` */
    background: $color-bg;
    /* [zh] 样式规则 `width: 28px;` */
    width: 28px;
    /* [zh] 样式规则 `height: 28px;` */
    height: 28px;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.week-row {` */
.week-row {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(7, 1fr);` */
  grid-template-columns: repeat(7, 1fr);
  /* [zh] 样式规则 `margin-bottom: 4px;` */
  margin-bottom: 4px;

  /* [zh] 样式规则 `span {` */
  span {
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `padding: 2px 0;` */
    padding: 2px 0;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.days-grid {` */
.days-grid {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: repeat(7, 1fr);` */
  grid-template-columns: repeat(7, 1fr);
  /* [zh] 样式规则 `gap: 3px;` */
  gap: 3px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.day-cell {` */
.day-cell {
  /* [zh] 样式规则 `height: 32px;` */
  height: 32px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `background: #fafafa;` */
  background: #fafafa;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 0;` */
  gap: 0;
  /* [zh] 样式规则 `cursor: default;` */
  cursor: default;
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;

  /* [zh] 样式规则 `.day-num {` */
  .day-num {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `line-height: 1.1;` */
    line-height: 1.1;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-other {` */
  &.is-other {
    /* [zh] 样式规则 `opacity: 0.35;` */
    opacity: 0.35;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-today {` */
  &.is-today {
    /* [zh] 样式规则 `box-shadow: inset 0 0 0 1.5px $color-pri` */
    box-shadow: inset 0 0 0 1.5px $color-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-signed {` */
  &.is-signed {
    /* [zh] 样式规则 `background: $color-gold-soft;` */
    background: $color-gold-soft;

    /* [zh] 样式规则 `.day-num {` */
    .day-num {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.signed-mark {` */
    .signed-mark {
      /* [zh] 样式规则 `font-size: 9px;` */
      font-size: 9px;
      /* [zh] 样式规则 `line-height: 1;` */
      line-height: 1;
      /* [zh] 样式规则 `color: $color-gold;` */
      color: $color-gold;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.can-supplement {` */
  &.can-supplement {
    /* [zh] 样式规则 `background: $color-primary-soft;` */
    background: $color-primary-soft;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;

    /* [zh] 样式规则 `.sup-tag {` */
    .sup-tag {
      /* [zh] 样式规则 `font-size: 9px;` */
      font-size: 9px;
      /* [zh] 样式规则 `background: $color-primary;` */
      background: $color-primary;
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `border-radius: $radius-xs;` */
      border-radius: $radius-xs;
      /* [zh] 样式规则 `padding: 0 3px;` */
      padding: 0 3px;
      /* [zh] 样式规则 `line-height: 1.3;` */
      line-height: 1.3;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sign-action {` */
.sign-action {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `padding: 4px 0 8px;` */
  padding: 4px 0 8px;

  /* [zh] 样式规则 `:deep(.el-button) {` */
  :deep(.el-button) {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `max-width: 280px;` */
    max-width: 280px;
    /* [zh] 样式规则 `height: 40px;` */
    height: 40px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>

/* [zh] 样式声明 */
<style lang="scss">
/* [zh] 样式规则 `.sign-reward-alert {` */
.sign-reward-alert {
  /* [zh] 样式规则 `border-radius: 20px !important;` */
  border-radius: 20px !important;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.88) !i` */
  background: rgba(255, 255, 255, 0.88) !important;
  /* [zh] 样式规则 `backdrop-filter: blur(16px) !important;` */
  backdrop-filter: blur(16px) !important;
  /* [zh] 样式规则 `-webkit-backdrop-filter: blur(16px) !imp` */
  -webkit-backdrop-filter: blur(16px) !important;
  /* [zh] 样式规则 `max-width: 320px !important;` */
  max-width: 320px !important;
  /* [zh] 样式规则 `padding: 4px !important;` */
  padding: 4px !important;
  /* [zh] 样式规则 `box-shadow:` */
  box-shadow:
    /* [zh] 样式声明 */
    0 8px 32px rgba(0, 0, 0, 0.12),
    /* [zh] 样式声明 */
    0 2px 8px rgba(0, 0, 0, 0.06) !important;

  /* [zh] 样式规则 `.el-message-box__header {` */
  .el-message-box__header {
    /* [zh] 样式规则 `padding: 20px 24px 4px !important;` */
    padding: 20px 24px 4px !important;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.el-message-box__title {` */
  .el-message-box__title {
    /* [zh] 样式规则 `font-size: 17px !important;` */
    font-size: 17px !important;
    /* [zh] 样式规则 `font-weight: 600 !important;` */
    font-weight: 600 !important;
    /* [zh] 样式规则 `color: #303133 !important;` */
    color: #303133 !important;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.el-message-box__status {` */
  .el-message-box__status {
    /* [zh] 样式规则 `font-size: 24px !important;` */
    font-size: 24px !important;
    /* [zh] 样式规则 `position: static !important;` */
    position: static !important;
    /* [zh] 样式规则 `display: inline-block !important;` */
    display: inline-block !important;
    /* [zh] 样式规则 `margin-right: 8px !important;` */
    margin-right: 8px !important;
    /* [zh] 样式规则 `vertical-align: middle !important;` */
    vertical-align: middle !important;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.el-message-box__content {` */
  .el-message-box__content {
    /* [zh] 样式规则 `padding: 8px 24px 4px !important;` */
    padding: 8px 24px 4px !important;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.el-message-box__message {` */
  .el-message-box__message {
    /* [zh] 样式规则 `font-size: 14px !important;` */
    font-size: 14px !important;
    /* [zh] 样式规则 `line-height: 1.6 !important;` */
    line-height: 1.6 !important;
    /* [zh] 样式规则 `color: #606266 !important;` */
    color: #606266 !important;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.el-message-box__btns {` */
  .el-message-box__btns {
    /* [zh] 样式规则 `padding: 12px 24px 20px !important;` */
    padding: 12px 24px 20px !important;
    /* [zh] 样式规则 `display: flex !important;` */
    display: flex !important;
    /* [zh] 样式规则 `justify-content: center !important;` */
    justify-content: center !important;

    /* [zh] 样式规则 `.el-button {` */
    .el-button {
      /* [zh] 样式规则 `border-radius: 20px !important;` */
      border-radius: 20px !important;
      /* [zh] 样式规则 `padding: 8px 32px !important;` */
      padding: 8px 32px !important;
      /* [zh] 样式规则 `min-width: 120px !important;` */
      min-width: 120px !important;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
