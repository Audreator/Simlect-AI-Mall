<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-avatar>` -->
  <el-avatar :size="size" :src="avatarSrc" class="user-avatar">
    <!-- [zh] 开始标签 `<el-icon>` -->
    <el-icon :size="iconSize"><UserFilled /></el-icon>
  <!-- [zh] 闭合标签 `</el-avatar>` -->
  </el-avatar>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed } from 'vue';
// [zh] 导入 `@element-plus/icons-vue`
import { UserFilled } from '@element-plus/icons-vue';
// [zh] 导入 `@/utils/image`
import { resolveAvatarUrl } from '@/utils/image';

// [zh] 声明变量 `props`
const props = withDefaults(
  // [zh] 代码块开始
  defineProps<{
    // [zh] 语句
    avatar?: string | null;
    // [zh] 语句
    size?: number;
  // [zh] TypeScript/JS 语句：`}>(),`
  }>(),
  // [zh] TypeScript/JS 语句：`{ size: 64 }`
  { size: 64 }
// [zh] 语句
);

// [zh] 声明变量 `avatarSrc`
const avatarSrc = computed(() => resolveAvatarUrl(props.avatar));
// [zh] 声明变量 `iconSize`
const iconSize = computed(() => Math.floor(props.size * 0.45));
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.user-avatar {` */
.user-avatar {
  /* [zh] 样式规则 `background: linear-gradient(135deg, $col` */
  background: linear-gradient(135deg, $color-primary-soft, $color-bg-subtle);
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `box-shadow: 0 4px 12px rgba($color-prima` */
  box-shadow: 0 4px 12px rgba($color-primary, 0.15);
/* [zh] 样式规则 `}` */
}
</style>
