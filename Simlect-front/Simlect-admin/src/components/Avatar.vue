<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-avatar>` -->
  <el-avatar :size="width" :src="avatarSrc" class="admin-avatar">
    <!-- [zh] 开始标签 `<el-icon>` -->
    <el-icon :size="iconSize"><UserFilled /></el-icon>
  <!-- [zh] 闭合标签 `</el-avatar>` -->
  </el-avatar>
</template>

<script setup>
import { computed } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'
import { getCurrentInstance } from 'vue'
const { proxy } = getCurrentInstance()

const props = defineProps({
  avatar: {
    type: String,
    default: null,
  },
  width: {
    type: Number,
    default: 50,
  },
  lazy: {
    type: Boolean,
    default: true,
  },
})

const iconSize = computed(() => Math.floor(props.width * 0.45))

const avatarSrc = computed(() => {
  if (!props.avatar || props.avatar === 'avatar.png') {
    return ''
  }
  return proxy.Api.sourcePath + props.avatar
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
