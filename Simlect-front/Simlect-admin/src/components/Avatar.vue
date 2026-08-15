<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-avatar>` -->
  <el-avatar :size="width" :src="avatarSrc" class="admin-avatar">
    <!-- [zh] 开始标签 `<el-icon>` -->
    <el-icon :size="iconSize"><UserFilled /></el-icon>
  <!-- [zh] 闭合标签 `</el-avatar>` -->
  </el-avatar>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { computed } from 'vue'
// [zh] 导入 `@element-plus/icons-vue`
import { UserFilled } from '@element-plus/icons-vue'
// [zh] 导入 `vue`
import { getCurrentInstance } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()

// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  avatar: {
    // [zh] TypeScript/JS 语句：`type: String,`
    type: String,
    // [zh] TypeScript/JS 语句：`default: null,`
    default: null,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  width: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 50,`
    default: 50,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  lazy: {
    // [zh] TypeScript/JS 语句：`type: Boolean,`
    type: Boolean,
    // [zh] TypeScript/JS 语句：`default: true,`
    default: true,
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `iconSize`
const iconSize = computed(() => Math.floor(props.width * 0.45))

// [zh] 声明变量 `avatarSrc`
const avatarSrc = computed(() => {
  // [zh] 条件判断 if
  if (!props.avatar || props.avatar === 'avatar.png') {
    // [zh] return 返回
    return ''
  // [zh] 语法符号
  }
  // [zh] return 返回
  return proxy.Api.sourcePath + props.avatar
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.admin-avatar {` */
.admin-avatar {
  /* [zh] 样式规则 `background: linear-gradient(135deg, #f5f` */
  background: linear-gradient(135deg, #f5f0e8, #e8e0d0);
  /* [zh] 样式规则 `color: #8b7355;` */
  color: #8b7355;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
/* [zh] 样式规则 `}` */
}
</style>
