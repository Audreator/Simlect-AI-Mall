<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="captcha-field ignore">
    <!-- [zh] 开始标签 `<el-input>` -->
    <el-input
      v-model="innerValue"
      class="captcha-input-el"
      :placeholder="placeholder"
      maxlength="6"
      @input="$emit('update:modelValue', innerValue)"
    />
    <!-- [zh] 开始标签 `<button>` -->
    <button
      type="button"
      class="captcha-preview"
      :class="{ loading: !captchaImage }"
      :title="captchaImage ? '点击刷新验证码' : '点击加载验证码'"
      @click="$emit('refresh')"
    >
      <!-- [zh] 开始标签 `<img>` -->
      <img v-if="captchaImage" :src="captchaImage" class="captcha-img" alt="图形验证码" />
      <!-- [zh] 开始标签 `<div>` -->
      <div v-else class="captcha-skeleton">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="dot" /><span class="dot" /><span class="dot" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</button>` -->
    </button>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{ modelValue: string; captchaImage: string; placeholder?: string }>();
defineEmits<{ 'update:modelValue': [string]; refresh: [] }>();
const innerValue = ref(props.modelValue);
watch(
  () => props.modelValue,
  (v) => (innerValue.value = v)
);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.captcha-field {` */
.captcha-field {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.captcha-input-el {` */
.captcha-input-el {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.captcha-preview {` */
.captcha-preview {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `width: 118px;` */
  width: 118px;
  /* [zh] 样式规则 `height: 44px;` */
  height: 44px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `background: linear-gradient(145deg, #fff` */
  background: linear-gradient(145deg, #fff, $color-bg-subtle);
  /* [zh] 样式规则 `box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04` */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  /* [zh] 样式规则 `transition: border-color 0.2s, box-shado` */
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `border-color: rgba($color-gold, 0.45);` */
    border-color: rgba($color-gold, 0.45);
    /* [zh] 样式规则 `box-shadow: 0 4px 14px rgba($color-gold,` */
    box-shadow: 0 4px 14px rgba($color-gold, 0.12);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.98);` */
    transform: scale(0.98);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.loading {` */
  &.loading {
    /* [zh] 样式规则 `cursor: wait;` */
    cursor: wait;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.captcha-img {` */
.captcha-img {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
  /* [zh] 样式规则 `object-fit: cover;` */
  object-fit: cover;
  /* [zh] 样式规则 `display: block;` */
  display: block;
  /* [zh] 样式规则 `user-select: none;` */
  user-select: none;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.captcha-skeleton {` */
.captcha-skeleton {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 6px;` */
  gap: 6px;
  /* [zh] 样式规则 `background: linear-gradient(110deg, #f0f` */
  background: linear-gradient(110deg, #f0f0f2 8%, #fafafa 18%, #f0f0f2 33%);
  /* [zh] 样式规则 `background-size: 200% 100%;` */
  background-size: 200% 100%;
  /* [zh] 样式规则 `animation: shimmer 1.2s ease infinite;` */
  animation: shimmer 1.2s ease infinite;

  /* [zh] 样式规则 `.dot {` */
  .dot {
    /* [zh] 样式规则 `width: 6px;` */
    width: 6px;
    /* [zh] 样式规则 `height: 6px;` */
    height: 6px;
    /* [zh] 样式规则 `border-radius: 50%;` */
    border-radius: 50%;
    /* [zh] 样式规则 `background: $color-silver;` */
    background: $color-silver;
    /* [zh] 样式规则 `opacity: 0.7;` */
    opacity: 0.7;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@keyframes shimmer {` */
@keyframes shimmer {
  /* [zh] 样式规则 `to {` */
  to {
    /* [zh] 样式规则 `background-position-x: -200%;` */
    background-position-x: -200%;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
