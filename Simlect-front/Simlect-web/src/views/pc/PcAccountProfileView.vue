<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-page-embed pc-profile-page">
    <!-- [zh] 开始标签 `<section>` -->
    <section class="profile-hero">
      <!-- [zh] 开始标签 `<UserAvatar>` -->
      <UserAvatar :avatar="user.avatar" :size="72" />
      <!-- [zh] 开始标签 `<div>` -->
      <div class="profile-hero__text">
        <!-- [zh] 开始标签 `<h2>` -->
        <h2 class="nick">{{ user.nickName || '简选用户' }}</h2>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="sub">{{ user.email || '未绑定账号' }}</p>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="profile-detail card">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="info-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="label">昵称</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="value">{{ user.nickName || '—' }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="row-divider" />
      <!-- [zh] 开始标签 `<div>` -->
      <div class="info-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="label">账号</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="value">{{ user.email || '—' }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="row-divider" />
      <!-- [zh] 开始标签 `<div>` -->
      <div class="info-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="label">性别</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="value">{{ sexLabel }}</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<p>` -->
    <p class="manage-hint">
      <!-- [zh] 模板内容：`如需修改资料或退出登录，请前往` -->
      如需修改资料或退出登录，请前往
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/account/manage">设置</RouterLink>
    <!-- [zh] 闭合标签 `</p>` -->
    </p>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import { accountApi } from '@/api/modules';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const user = ref<Record<string, any>>({});

const sexLabel = computed(() => {
  const map: Record<number, string> = { 0: '女', 1: '男', 2: '保密' };
  const s = user.value?.sex;
  return map[s as number] ?? '保密';
});

onMounted(async () => {
  user.value = (await accountApi.getUserInfo()) || authStore.userInfo || {};
});
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-profile-page {` */
.pc-profile-page {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.profile-hero {` */
.profile-hero {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
  /* [zh] 样式规则 `padding-bottom: 16px;` */
  padding-bottom: 16px;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border-g` */
  border-bottom: 1px solid $color-border-gray;

  /* [zh] 样式规则 `.nick {` */
  .nick {
    /* [zh] 样式规则 `margin: 0 0 4px;` */
    margin: 0 0 4px;
    /* [zh] 样式规则 `font-size: 18px;` */
    font-size: 18px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-primary;` */
    color: $color-text-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sub {` */
  .sub {
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

/* [zh] 样式规则 `.profile-detail {` */
.profile-detail {
  /* [zh] 样式规则 `padding: 4px 16px;` */
  padding: 4px 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.info-row {` */
.info-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `padding: 12px 0;` */
  padding: 12px 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;

  /* [zh] 样式规则 `.label {` */
  .label {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.value {` */
  .value {
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `text-align: right;` */
    text-align: right;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.row-divider {` */
.row-divider {
  /* [zh] 样式规则 `height: 1px;` */
  height: 1px;
  /* [zh] 样式规则 `background: $color-border;` */
  background: $color-border;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.manage-hint {` */
.manage-hint {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;

  /* [zh] 样式规则 `a {` */
  a {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
