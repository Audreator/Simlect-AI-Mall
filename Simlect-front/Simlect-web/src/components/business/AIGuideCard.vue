<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="ai-guide-card" @click="goAgent">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="ai-guide-left">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="ai-avatar-pulse">
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon :size="18"><ChatDotRound /></el-icon>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="ai-text">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="ai-title">AI 帮你选</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="ai-sub">不知道买什么？告诉我你的需求</p>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="ai-prompts">
      <!-- [zh] 开始标签 `<button>` -->
      <button
        v-for="(prompt, i) in samplePrompts"
        :key="i"
        type="button"
        class="ai-prompt-chip"
        @click.stop="quickAsk(prompt)"
      >
        <!-- [zh] Mustache 插值表达式 -->
        {{ prompt }}
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="ai-arrow">
      <!-- [zh] 开始标签 `<el-icon>` -->
      <el-icon :size="14"><ArrowRight /></el-icon>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { onMounted, ref } from 'vue';
// [zh] 导入 `@element-plus/icons-vue`
import { ArrowRight, ChatDotRound } from '@element-plus/icons-vue';
// [zh] 导入 `@/composables/useOpenAgent`
import { useOpenAgent } from '@/composables/useOpenAgent';

// [zh] 声明变量 `PROMPT_POOL`
const PROMPT_POOL = [
  // [zh] TypeScript/JS 语句：`'通勤路上适合用什么',`
  '通勤路上适合用什么',
  // [zh] TypeScript/JS 语句：`'厨房好物推荐',`
  '厨房好物推荐',
  // [zh] TypeScript/JS 语句：`'宿舍生活必备品',`
  '宿舍生活必备品',
  // [zh] TypeScript/JS 语句：`'运动健身装备推荐',`
  '运动健身装备推荐',
  // [zh] TypeScript/JS 语句：`'百元以内的实用好物',`
  '百元以内的实用好物',
  // [zh] TypeScript/JS 语句：`'适合送给妈妈的礼物',`
  '适合送给妈妈的礼物',
  // [zh] TypeScript/JS 语句：`'居家收纳好物',`
  '居家收纳好物',
  // [zh] TypeScript/JS 语句：`'学生党平价好物',`
  '学生党平价好物',
  // [zh] TypeScript/JS 语句：`'数码周边推荐',`
  '数码周边推荐',
  // [zh] TypeScript/JS 语句：`'秋冬保暖好物',`
  '秋冬保暖好物',
  // [zh] TypeScript/JS 语句：`'宠物用品推荐',`
  '宠物用品推荐',
  // [zh] TypeScript/JS 语句：`'旅行出行必备',`
  '旅行出行必备',
  // [zh] TypeScript/JS 语句：`'适合送给男朋友的礼物',`
  '适合送给男朋友的礼物',
  // [zh] TypeScript/JS 语句：`'办公桌改造好物',`
  '办公桌改造好物',
  // [zh] TypeScript/JS 语句：`'卧室好物推荐',`
  '卧室好物推荐',
  // [zh] TypeScript/JS 语句：`'夏天清凉好物',`
  '夏天清凉好物',
  // [zh] TypeScript/JS 语句：`'100元内的创意礼物',`
  '100元内的创意礼物',
  // [zh] TypeScript/JS 语句：`'健康养生好物',`
  '健康养生好物',
  // [zh] TypeScript/JS 语句：`'宝宝用品推荐',`
  '宝宝用品推荐',
  // [zh] TypeScript/JS 语句：`'车载好物推荐'`
  '车载好物推荐'
// [zh] 语句
];

// [zh] 函数 `pickRandom`
function pickRandom<T>(arr: T[], count: number): T[] {
  // [zh] 声明变量 `shuffled`
  const shuffled = [...arr];
  // [zh] for 循环
  for (let i = shuffled.length - 1; i > 0; i--) {
    // [zh] 声明变量 `j`
    const j = Math.floor(Math.random() * (i + 1));
    // [zh] 语句
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  // [zh] 语法符号
  }
  // [zh] return 返回
  return shuffled.slice(0, count);
// [zh] 语法符号
}

// [zh] 声明变量 `samplePrompts`
const samplePrompts = ref<string[]>([]);

// [zh] 代码块开始
onMounted(() => {
  // [zh] 语句
  samplePrompts.value = pickRandom(PROMPT_POOL, 3);
// [zh] 语句
});

// [zh] 语句
const { openAgent } = useOpenAgent();

// [zh] 声明变量 `goAgent`
const goAgent = () => {
  // [zh] 语句
  openAgent();
// [zh] 语句
};

// [zh] 声明变量 `quickAsk`
const quickAsk = (prompt: string) => {
  // [zh] 语句
  openAgent({ presetMessage: prompt });
// [zh] 语句
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.ai-guide-card {` */
.ai-guide-card {
  /* [zh] 样式规则 `margin: 12px $app-page-gutter 0;` */
  margin: 12px $app-page-gutter 0;
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `background: linear-gradient(135deg, #fff` */
  background: linear-gradient(135deg, #ffffff 0%, #fefaf3 100%);
  /* [zh] 样式规则 `border: 1px solid rgba(212, 162, 78, 0.1` */
  border: 1px solid rgba(212, 162, 78, 0.18);
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `transition: transform 0.2s ease, box-sha` */
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `border-color: rgba(212, 162, 78, 0.35);` */
    border-color: rgba(212, 162, 78, 0.35);
    /* [zh] 样式规则 `box-shadow: $shadow-card;` */
    box-shadow: $shadow-card;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.985);` */
    transform: scale(0.985);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ai-guide-left {` */
.ai-guide-left {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `z-index: 1;` */
  z-index: 1;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ai-avatar-pulse {` */
.ai-avatar-pulse {
  /* [zh] 样式规则 `width: 40px;` */
  width: 40px;
  /* [zh] 样式规则 `height: 40px;` */
  height: 40px;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `background: $color-gold-soft;` */
  background: $color-gold-soft;
  /* [zh] 样式规则 `color: $color-gold;` */
  color: $color-gold;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `box-shadow: 0 0 0 3px rgba(212, 162, 78,` */
  box-shadow: 0 0 0 3px rgba(212, 162, 78, 0.1);
  /* [zh] 样式规则 `animation: aiPulse 3s ease-in-out infini` */
  animation: aiPulse 3s ease-in-out infinite;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@keyframes aiPulse {` */
@keyframes aiPulse {
  /* [zh] 样式规则 `0%, 100% { box-shadow: 0 0 0 3px rgba(21` */
  0%, 100% { box-shadow: 0 0 0 3px rgba(212, 162, 78, 0.1); }
  /* [zh] 样式规则 `50% { box-shadow: 0 0 0 6px rgba(212, 16` */
  50% { box-shadow: 0 0 0 6px rgba(212, 162, 78, 0.06); }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ai-text {` */
.ai-text {
  /* [zh] 样式规则 `.ai-title {` */
  .ai-title {
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `line-height: 1.3;` */
    line-height: 1.3;
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `.ai-sub {` */
  .ai-sub {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `margin: 2px 0 0;` */
    margin: 2px 0 0;
    /* [zh] 样式规则 `line-height: 1.2;` */
    line-height: 1.2;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ai-prompts {` */
.ai-prompts {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `z-index: 1;` */
  z-index: 1;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ai-prompt-chip {` */
.ai-prompt-chip {
  /* [zh] 样式规则 `padding: 4px 10px;` */
  padding: 4px 10px;
  /* [zh] 样式规则 `border: 1px solid rgba(212, 162, 78, 0.2` */
  border: 1px solid rgba(212, 162, 78, 0.25);
  /* [zh] 样式规则 `border-radius: $radius-pill;` */
  border-radius: $radius-pill;
  /* [zh] 样式规则 `background: rgba(212, 162, 78, 0.06);` */
  background: rgba(212, 162, 78, 0.06);
  /* [zh] 样式规则 `color: #8b7355;` */
  color: #8b7355;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
  /* [zh] 样式规则 `transition: background 0.2s, border-colo` */
  transition: background 0.2s, border-color 0.2s, color 0.2s;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `background: rgba(212, 162, 78, 0.14);` */
    background: rgba(212, 162, 78, 0.14);
    /* [zh] 样式规则 `border-color: rgba(212, 162, 78, 0.4);` */
    border-color: rgba(212, 162, 78, 0.4);
    /* [zh] 样式规则 `color: $color-gold;` */
    color: $color-gold;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `background: $color-gold-soft;` */
    background: $color-gold-soft;
    /* [zh] 样式规则 `border-color: $color-gold;` */
    border-color: $color-gold;
    /* [zh] 样式规则 `color: $color-gold;` */
    color: $color-gold;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ai-arrow {` */
.ai-arrow {
  /* [zh] 样式规则 `z-index: 1;` */
  z-index: 1;
  /* [zh] 样式规则 `color: $color-gold;` */
  color: $color-gold;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `opacity: 0.5;` */
  opacity: 0.5;
  /* [zh] 样式规则 `transition: opacity 0.2s;` */
  transition: opacity 0.2s;

  /* [zh] 样式规则 `.ai-guide-card:hover & {` */
  .ai-guide-card:hover & {
    /* [zh] 样式规则 `opacity: 1;` */
    opacity: 1;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (max-width: 400px) {` */
@media (max-width: 400px) {
  /* [zh] 样式规则 `.ai-guide-card {` */
  .ai-guide-card {
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `align-items: stretch;` */
    align-items: stretch;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.ai-guide-left {` */
  .ai-guide-left {
    /* [zh] 样式规则 `flex: none;` */
    flex: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.ai-prompts {` */
  .ai-prompts {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.ai-arrow {` */
  .ai-arrow {
    /* [zh] 样式规则 `display: none;` */
    display: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>