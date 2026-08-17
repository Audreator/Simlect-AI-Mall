<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="password-page">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="form-panel card">
      <!-- [zh] 开始标签 `<section>` -->
      <section class="form-section">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="section-title">旧密码</p>
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="请输入旧密码" size="large" />
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="section-divider" />

      <!-- [zh] 开始标签 `<section>` -->
      <section class="form-section">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="section-title">新密码</p>
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input v-model="pwd.password" type="password" show-password placeholder="8-18位，含字母与数字" size="large" />
        <!-- [zh] 开始标签 `<p>` -->
        <p class="hint">{{ PASSWORD_FORMAT_HINT }}</p>
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<section>` -->
      <section class="form-section">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="section-title">确认新密码</p>
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input v-model="pwd.confirmPassword" type="password" show-password placeholder="请再次输入新密码" size="large" />
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="error" class="error-tip">{{ error }}</p>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="save-row">
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" round class="save-btn" :loading="submitting" @click="savePwd">确认修改</el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { accountApi } from '@/api/modules';
import { useAuthStore } from '@/stores/auth';
import { isValidPassword, PASSWORD_FORMAT_HINT } from '@/constants/validation';

const router = useRouter();
const authStore = useAuthStore();
const pwd = reactive({ oldPassword: '', password: '', confirmPassword: '' });
const error = ref('');
const submitting = ref(false);

const savePwd = async () => {
  error.value = '';
  if (!pwd.oldPassword?.trim()) {
    ElMessage.warning('请输入旧密码');
    return;
  }
  if (!isValidPassword(pwd.password)) {
    error.value = PASSWORD_FORMAT_HINT;
    return;
  }
  if (pwd.password !== pwd.confirmPassword) {
    error.value = '两次新密码不一致';
    return;
  }
  submitting.value = true;
  try {
    await accountApi.updatePassword({ oldPassword: pwd.oldPassword, password: pwd.password });
    ElMessage.success('密码已修改，请重新登录');
    authStore.prepareLogoutNavigation();
    await authStore.logout(true);
    await router.replace({ path: '/login', query: {} });
  } catch (e: any) {
    error.value = e?.info || e?.message || '密码修改失败，请稍后重试';
  } finally {
    submitting.value = false;
  }
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.password-page {` */
.password-page {
  /* [zh] 样式规则 `padding: 0 0 24px;` */
  padding: 0 0 24px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.form-panel {` */
.form-panel {
  /* [zh] 样式规则 `padding: 8px 16px 20px;` */
  padding: 8px 16px 20px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.form-section {` */
.form-section {
  /* [zh] 样式规则 `padding: 12px 0;` */
  padding: 12px 0;

  /* [zh] 样式规则 `.section-title {` */
  .section-title {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.hint {` */
  .hint {
    /* [zh] 样式规则 `margin: 8px 0 0;` */
    margin: 8px 0 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.section-divider {` */
.section-divider {
  /* [zh] 样式规则 `height: 1px;` */
  height: 1px;
  /* [zh] 样式规则 `background: $color-border;` */
  background: $color-border;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.error-tip {` */
.error-tip {
  /* [zh] 样式规则 `margin: 8px 0 0;` */
  margin: 8px 0 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
  /* [zh] 样式规则 `color: $color-error;` */
  color: $color-error;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.save-row {` */
.save-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `padding: 20px 0 4px;` */
  padding: 20px 0 4px;

  /* [zh] 样式规则 `.save-btn {` */
  .save-btn {
    /* [zh] 样式规则 `min-width: 200px;` */
    min-width: 200px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
