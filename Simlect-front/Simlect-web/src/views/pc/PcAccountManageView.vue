<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-manage-page">
    <!-- [zh] 开始标签 `<section>` -->
    <section class="user-brief">
      <!-- [zh] 开始标签 `<UserAvatar>` -->
      <UserAvatar :avatar="user.avatar" :size="56" />
      <!-- [zh] 开始标签 `<div>` -->
      <div class="brief-text">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="nick">{{ user.nickName || '简选用户' }}</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="account">{{ user.email || '未绑定账号' }}</p>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="menu-card">
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/account/profile" class="menu-item">
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon class="menu-icon"><User /></el-icon>
        <!-- [zh] 开始标签 `<span>` -->
        <span>个人资料</span>
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon class="arrow"><ArrowRight /></el-icon>
      <!-- [zh] 闭合标签 `</RouterLink>` -->
      </RouterLink>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="item-divider" />
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/account/settings" class="menu-item">
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon class="menu-icon"><Edit /></el-icon>
        <!-- [zh] 开始标签 `<span>` -->
        <span>修改个人信息</span>
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon class="arrow"><ArrowRight /></el-icon>
      <!-- [zh] 闭合标签 `</RouterLink>` -->
      </RouterLink>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="item-divider" />
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/account/password" class="menu-item">
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon class="menu-icon"><Lock /></el-icon>
        <!-- [zh] 开始标签 `<span>` -->
        <span>修改密码</span>
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon class="arrow"><ArrowRight /></el-icon>
      <!-- [zh] 闭合标签 `</RouterLink>` -->
      </RouterLink>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<el-button>` -->
    <el-button class="btn-logout" plain type="danger" @click="logout">退出登录</el-button>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowRight, Edit, Lock, User } from '@element-plus/icons-vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import { accountApi } from '@/api/modules';
import { useAuthStore } from '@/stores/auth';
import { confirmAction } from '@/utils/confirm';
import { toast } from '@/utils/toast';

const router = useRouter();
const authStore = useAuthStore();
const user = ref<Record<string, any>>({});

onMounted(async () => {
  user.value = (await accountApi.getUserInfo()) || authStore.userInfo || {};
});

const logout = async () => {
  const ok = await confirmAction('确定要退出当前账号吗？', {
    title: '退出登录',
    confirmButtonText: '退出'
  });
  if (!ok) return;
  authStore.prepareLogoutNavigation();
  await authStore.logout();
  toast.success('已退出登录');
  await router.replace({ path: '/login', query: {} });
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-manage-page {` */
.pc-manage-page {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
  /* [zh] 样式规则 `max-width: 480px;` */
  max-width: 480px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.user-brief {` */
.user-brief {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 14px;` */
  gap: 14px;
  /* [zh] 样式规则 `padding-bottom: 16px;` */
  padding-bottom: 16px;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border-g` */
  border-bottom: 1px solid $color-border-gray;

  /* [zh] 样式规则 `.nick {` */
  .nick {
    /* [zh] 样式规则 `margin: 0 0 4px;` */
    margin: 0 0 4px;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.account {` */
  .account {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.menu-card {` */
.menu-card {
  /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
  border: 1px solid $color-border-gray;
  /* [zh] 样式规则 `border-radius: $radius-sm;` */
  border-radius: $radius-sm;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.menu-item {` */
.menu-item {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `text-decoration: none;` */
  text-decoration: none;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `background: #fafafa;` */
    background: #fafafa;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.menu-icon {` */
  .menu-icon {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `span {` */
  span {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.arrow {` */
  .arrow {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.item-divider {` */
.item-divider {
  /* [zh] 样式规则 `height: 1px;` */
  height: 1px;
  /* [zh] 样式规则 `margin: 0 14px;` */
  margin: 0 14px;
  /* [zh] 样式规则 `background: $color-border;` */
  background: $color-border;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.btn-logout {` */
.btn-logout {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: 280px;` */
  max-width: 280px;
  /* [zh] 样式规则 `margin-top: 8px;` */
  margin-top: 8px;
/* [zh] 样式规则 `}` */
}
</style>
