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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { reactive, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `element-plus`
import { ElMessage } from 'element-plus';
// [zh] 导入 `@/api/modules`
import { accountApi } from '@/api/modules';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/constants/validation`
import { isValidPassword, PASSWORD_FORMAT_HINT } from '@/constants/validation';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `pwd`
const pwd = reactive({ oldPassword: '', password: '', confirmPassword: '' });
// [zh] 声明变量 `error`
const error = ref('');
// [zh] 声明变量 `submitting`
const submitting = ref(false);

// [zh] 声明变量 `savePwd`
const savePwd = async () => {
  // [zh] 语句
  error.value = '';
  // [zh] 条件判断 if
  if (!pwd.oldPassword?.trim()) {
    // [zh] 语句
    ElMessage.warning('请输入旧密码');
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!isValidPassword(pwd.password)) {
    // [zh] 语句
    error.value = PASSWORD_FORMAT_HINT;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (pwd.password !== pwd.confirmPassword) {
    // [zh] 语句
    error.value = '两次新密码不一致';
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  submitting.value = true;
  // [zh] 代码块开始
  try {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await accountApi.updatePassword({ oldPassword: pwd.oldPassword, password: pwd.password });
    // [zh] 语句
    ElMessage.success('密码已修改，请重新登录');
    // [zh] 语句
    authStore.prepareLogoutNavigation();
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await authStore.logout(true);
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await router.replace({ path: '/login', query: {} });
  // [zh] 代码块开始
  } catch (e: any) {
    // [zh] 语句
    error.value = e?.info || e?.message || '密码修改失败，请稍后重试';
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    submitting.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
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
