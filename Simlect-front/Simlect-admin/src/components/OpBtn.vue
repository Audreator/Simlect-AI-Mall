<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-tooltip>` -->
  <el-tooltip v-if="icon" effect="dark" :content="tooltipText" placement="top" :disabled="!tooltipText">
    <!-- [zh] 开始标签 `<div>` -->
    <div
      class="btn-panel btn-panel--icon"
      :class="[`btn-panel--${type}`, { 'is-disabled': disabled }]"
      :aria-label="tips"
      :aria-disabled="disabled"
      role="button"
      :tabindex="disabled ? -1 : 0"
      @click="handleClick"
      @keydown.enter.prevent="handleClick"
    >
      <!-- [zh] 开始标签 `<div>` -->
      <div :class="['iconfont', icon]" :style="{ color: TYPE_MAP[type] }"></div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</el-tooltip>` -->
  </el-tooltip>
  <!-- [zh] 开始标签 `<div>` -->
  <div
    v-else
    class="btn-panel btn-panel--text"
    :class="[`btn-panel--${type}`, { 'is-disabled': disabled }]"
    role="button"
    :aria-disabled="disabled"
    :tabindex="disabled ? -1 : 0"
    @click="handleClick"
    @keydown.enter.prevent="handleClick"
  >
    <!-- [zh] 开始标签 `<span>` -->
    <span class="btn-text">{{ tips }}</span>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  icon: {
    type: String,
  },
  type: {
    type: String,
    default: 'primary',
  },
  tips: {
    type: String,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  disabledTips: {
    type: String,
    default: '',
  },
  fun: {
    type: [String, Function],
  },
})

const tooltipText = computed(() => (props.disabled && props.disabledTips ? props.disabledTips : props.tips))

const TYPE_MAP = {
  primary: 'var(--primary)',
  success: 'var(--green)',
  warning: '#d4a24e',
  danger: '#e56b5b',
  info: '#8b95a8',
}

const emit = defineEmits(['click'])
const handleClick = (event) => {
  if (props.disabled) return
  emit('click', event)
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.btn-panel {` */
.btn-panel {
  /* [zh] 样式规则 `display: inline-flex;` */
  display: inline-flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `border-radius: 4px;` */
  border-radius: 4px;
  /* [zh] 样式规则 `transition: all 0.2s;` */
  transition: all 0.2s;
  /* [zh] 样式规则 `user-select: none;` */
  user-select: none;

  /* [zh] 样式规则 `&--icon {` */
  &--icon {
    /* [zh] 样式规则 `width: 30px;` */
    width: 30px;
    /* [zh] 样式规则 `height: 30px;` */
    height: 30px;
    /* [zh] 样式规则 `background: var(--primary-muted);` */
    background: var(--primary-muted);
    /* [zh] 样式规则 `border: 1px solid rgba(201, 117, 79, 0.2` */
    border: 1px solid rgba(201, 117, 79, 0.22);

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: var(--primary-soft);` */
      background: var(--primary-soft);
      /* [zh] 样式规则 `border-color: rgba(201, 117, 79, 0.35);` */
      border-color: rgba(201, 117, 79, 0.35);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.iconfont {` */
    .iconfont {
      /* [zh] 样式规则 `font-size: 14px;` */
      font-size: 14px;
      /* [zh] 样式规则 `line-height: 1;` */
      line-height: 1;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&--text {` */
  &--text {
    /* [zh] 样式规则 `min-width: 52px;` */
    min-width: 52px;
    /* [zh] 样式规则 `height: 30px;` */
    height: 30px;
    /* [zh] 样式规则 `padding: 0 10px;` */
    padding: 0 10px;
    /* [zh] 样式规则 `background: var(--primary-muted);` */
    background: var(--primary-muted);
    /* [zh] 样式规则 `border: 1px solid rgba(201, 117, 79, 0.2` */
    border: 1px solid rgba(201, 117, 79, 0.22);

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: var(--primary-soft);` */
      background: var(--primary-soft);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  &--danger.btn-panel--icon,
  /* [zh] 样式规则 `&--danger.btn-panel--text {` */
  &--danger.btn-panel--text {
    /* [zh] 样式规则 `background: rgba(229, 107, 91, 0.1);` */
    background: rgba(229, 107, 91, 0.1);
    /* [zh] 样式规则 `border-color: rgba(229, 107, 91, 0.28);` */
    border-color: rgba(229, 107, 91, 0.28);

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: rgba(229, 107, 91, 0.16);` */
      background: rgba(229, 107, 91, 0.16);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  &--success.btn-panel--icon,
  /* [zh] 样式规则 `&--success.btn-panel--text {` */
  &--success.btn-panel--text {
    /* [zh] 样式规则 `background: rgba(20, 184, 166, 0.1);` */
    background: rgba(20, 184, 166, 0.1);
    /* [zh] 样式规则 `border-color: rgba(20, 184, 166, 0.28);` */
    border-color: rgba(20, 184, 166, 0.28);

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: rgba(20, 184, 166, 0.16);` */
      background: rgba(20, 184, 166, 0.16);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  &--warning.btn-panel--icon,
  /* [zh] 样式规则 `&--warning.btn-panel--text {` */
  &--warning.btn-panel--text {
    /* [zh] 样式规则 `background: rgba(212, 162, 78, 0.12);` */
    background: rgba(212, 162, 78, 0.12);
    /* [zh] 样式规则 `border-color: rgba(212, 162, 78, 0.3);` */
    border-color: rgba(212, 162, 78, 0.3);

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `background: rgba(212, 162, 78, 0.18);` */
      background: rgba(212, 162, 78, 0.18);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-disabled {` */
  &.is-disabled {
    /* [zh] 样式规则 `opacity: 0.45;` */
    opacity: 0.45;
    /* [zh] 样式规则 `cursor: not-allowed;` */
    cursor: not-allowed;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.btn-text {` */
.btn-text {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--primary);` */
  color: var(--primary);
  /* [zh] 样式规则 `white-space: nowrap;` */
  white-space: nowrap;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.btn-panel--danger .btn-text {` */
.btn-panel--danger .btn-text {
  /* [zh] 样式规则 `color: #e56b5b;` */
  color: #e56b5b;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.btn-panel--success .btn-text {` */
.btn-panel--success .btn-text {
  /* [zh] 样式规则 `color: var(--green);` */
  color: var(--green);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.btn-panel--warning .btn-text {` */
.btn-panel--warning .btn-text {
  /* [zh] 样式规则 `color: #d4a24e;` */
  color: #d4a24e;
/* [zh] 样式规则 `}` */
}
</style>
