<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="auth-page ignore">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="auth-card">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="auth-brand">
        <!-- [zh] 开始标签 `<BrandMark>` -->
        <BrandMark variant="light" class="brand-mark" />
        <!-- [zh] 开始标签 `<div>` -->
        <div class="auth-brand-text">
          <!-- [zh] 开始标签 `<h2>` -->
          <h2>欢迎回来</h2>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="brand-name">简选 · Simlect</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="brand-tip">登录简选，畅享品质购物</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<el-form>` -->
      <el-form class="auth-form" label-position="top" @submit.prevent="submit">
        <!-- [zh] 开始标签 `<el-form-item>` -->
        <el-form-item label="邮箱">
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input v-model="form.email" placeholder="请输入邮箱" size="large" />
        <!-- [zh] 闭合标签 `</el-form-item>` -->
        </el-form-item>
        <!-- [zh] 开始标签 `<el-form-item>` -->
        <el-form-item label="密码">
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
        <!-- [zh] 闭合标签 `</el-form-item>` -->
        </el-form-item>
        <!-- [zh] 开始标签 `<el-form-item>` -->
        <el-form-item label="图形验证码">
          <!-- [zh] 开始标签 `<CaptchaInput>` -->
          <CaptchaInput v-model="form.checkCode" :captcha-image="captchaSrc" @refresh="loadCode" />
        <!-- [zh] 闭合标签 `</el-form-item>` -->
        </el-form-item>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="error" class="error-tip">{{ error }}</p>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" class="submit-btn" size="large" :loading="submitting" @click="submit">登 录</el-button>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="forgot-link-text">
          <!-- [zh] 开始标签 `<RouterLink>` -->
          <RouterLink to="/forgot-password">忘记密码？</RouterLink>
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
      <!-- [zh] 闭合标签 `</el-form>` -->
      </el-form>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="auth-footer">
        <!-- [zh] 模板内容：`还没有账号？` -->
        还没有账号？
        <!-- [zh] 开始标签 `<RouterLink>` -->
        <RouterLink to="/register">去注册</RouterLink>
      <!-- [zh] 闭合标签 `</p>` -->
      </p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<AppFooter>` -->
    <AppFooter />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted, reactive, ref } from 'vue';
// [zh] 导入 `element-plus`
import { ElMessage } from 'element-plus';
// [zh] 导入 `@/utils/captcha`
import { formatCaptchaSrc } from '@/utils/captcha';
// [zh] 导入 `vue-router`
import { RouterLink, useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@/components/common/BrandMark.vue`
import BrandMark from '@/components/common/BrandMark.vue';
// [zh] 导入 `@/components/common/CaptchaInput.vue`
import CaptchaInput from '@/components/common/CaptchaInput.vue';
// [zh] 导入 `@/components/layout/AppFooter.vue`
import AppFooter from '@/components/layout/AppFooter.vue';
// [zh] 导入 `@/api/modules`
import { accountApi } from '@/api/modules';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/utils/apiError`
import { formatLoginErrorMessage } from '@/utils/apiError';
// [zh] 导入 `@/constants/validation`
import { isValidEmail } from '@/constants/validation';
// [zh] 导入 `@/utils/navigation`
import { resolveSafeRedirect } from '@/utils/navigation';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `error`
const error = ref('');
// [zh] 声明变量 `submitting`
const submitting = ref(false);
// [zh] 声明变量 `captcha`
const captcha = reactive({ checkCode: '', checkCodeKey: '' });
// [zh] 声明变量 `form`
const form = reactive<any>({ email: '', password: '', checkCode: '' });
// [zh] 声明变量 `captchaSrc`
const captchaSrc = computed(() => formatCaptchaSrc(captcha.checkCode));

// [zh] 声明变量 `loadCode`
const loadCode = async () => {
  // [zh] 声明变量 `data`
  const data = await accountApi.checkCode();
  // [zh] 语句
  captcha.checkCode = data?.checkCode || '';
  // [zh] 语句
  captcha.checkCodeKey = data?.checkCodeKey || '';
// [zh] 语句
};

// [zh] 声明变量 `submit`
const submit = async () => {
  // [zh] 语句
  error.value = '';
  // [zh] 条件判断 if
  if (!form.email?.trim()) {
    // [zh] 语句
    ElMessage.warning('请输入邮箱');
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!isValidEmail(form.email)) {
    // [zh] 语句
    error.value = '请输入有效的邮箱地址';
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!form.password) {
    // [zh] 语句
    ElMessage.warning('请输入密码');
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!form.checkCode) {
    // [zh] 语句
    ElMessage.warning('请输入验证码');
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  submitting.value = true;
  // [zh] 代码块开始
  try {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await authStore.login({ ...form, checkCodeKey: captcha.checkCodeKey });
    // [zh] 语句
    toast.success('登录成功');
    // [zh] 语句
    router.replace(resolveSafeRedirect(route.query.redirect));
  // [zh] 代码块开始
  } catch (e: any) {
    // [zh] 语句
    error.value = formatLoginErrorMessage(e);
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await loadCode();
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    submitting.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 语句
onMounted(loadCode);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.auth-page.ignore {` */
.auth-page.ignore {
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `min-height: 100vh;` */
  min-height: 100vh;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `padding: 24px 16px 0;` */
  padding: 24px 16px 0;
  /* [zh] 样式规则 `background:` */
  background:
    /* [zh] 样式声明 */
    radial-gradient(ellipse 80% 50% at 20% 0%, rgba($color-primary, 0.1), transparent 55%),
    /* [zh] 样式声明 */
    radial-gradient(ellipse 50% 40% at 90% 100%, rgba($color-primary, 0.06), transparent 50%),
    /* [zh] 样式声明 */
    $color-bg;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.auth-card {` */
.auth-card {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: 420px;` */
  max-width: 420px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `margin-bottom: 24px;` */
  margin-bottom: 24px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
  /* [zh] 样式规则 `box-shadow: $shadow-card-hover;` */
  box-shadow: $shadow-card-hover;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `animation: card-in 0.4s cubic-bezier(0.3` */
  animation: card-in 0.4s cubic-bezier(0.34, 1.1, 0.64, 1);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@keyframes card-in {` */
@keyframes card-in {
  /* [zh] 样式规则 `from {` */
  from {
    /* [zh] 样式规则 `opacity: 0;` */
    opacity: 0;
    /* [zh] 样式规则 `transform: translateY(16px);` */
    transform: translateY(16px);
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `to {` */
  to {
    /* [zh] 样式规则 `opacity: 1;` */
    opacity: 1;
    /* [zh] 样式规则 `transform: translateY(0);` */
    transform: translateY(0);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.auth-brand {` */
.auth-brand {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 14px;` */
  gap: 14px;
  /* [zh] 样式规则 `padding: 30px 28px 26px;` */
  padding: 30px 28px 26px;
  /* [zh] 样式规则 `background: linear-gradient(135deg, $col` */
  background: linear-gradient(135deg, $color-primary 0%, $color-primary-hover 100%);
  /* [zh] 样式规则 `position: relative;` */
  position: relative;

  /* [zh] 样式规则 `&::after {` */
  &::after {
    /* [zh] 样式规则 `content: '';` */
    content: '';
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `left: 28px;` */
    left: 28px;
    /* [zh] 样式规则 `right: 28px;` */
    right: 28px;
    /* [zh] 样式规则 `bottom: 0;` */
    bottom: 0;
    /* [zh] 样式规则 `height: 2px;` */
    height: 2px;
    /* [zh] 样式规则 `background: linear-gradient(90deg, $colo` */
    background: linear-gradient(90deg, $color-gold, transparent 70%);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-mark {` */
  .brand-mark {
    /* [zh] 样式规则 `width: 48px;` */
    width: 48px;
    /* [zh] 样式规则 `height: 48px;` */
    height: 48px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.auth-brand-text {` */
  .auth-brand-text {
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `h2 {` */
  h2 {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 20px;` */
    font-size: 20px;
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-name {` */
  .brand-name {
    /* [zh] 样式规则 `margin: 6px 0 2px;` */
    margin: 6px 0 2px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `letter-spacing: 0.08em;` */
    letter-spacing: 0.08em;
    /* [zh] 样式规则 `color: $color-gold;` */
    color: $color-gold;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-tip {` */
  .brand-tip {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: rgba(255, 255, 255, 0.78);` */
    color: rgba(255, 255, 255, 0.78);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.auth-form {` */
.auth-form {
  /* [zh] 样式规则 `padding: 20px 28px 8px;` */
  padding: 20px 28px 8px;

  /* [zh] 样式规则 `:deep(.el-form-item) {` */
  :deep(.el-form-item) {
    /* [zh] 样式规则 `margin-bottom: 18px;` */
    margin-bottom: 18px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.el-form-item__label) {` */
  :deep(.el-form-item__label) {
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `padding-bottom: 6px;` */
    padding-bottom: 6px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.el-input) {` */
  :deep(.el-input) {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.error-tip {` */
.error-tip {
  /* [zh] 样式规则 `margin: 0 0 12px;` */
  margin: 0 0 12px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-price;` */
  color: $color-price;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.submit-btn {` */
.submit-btn {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `margin-top: 4px;` */
  margin-top: 4px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `letter-spacing: 2px;` */
  letter-spacing: 2px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.forgot-link-text {` */
.forgot-link-text {
  /* [zh] 样式规则 `margin: 10px 0 0;` */
  margin: 10px 0 0;
  /* [zh] 样式规则 `text-align: right;` */
  text-align: right;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;

  /* [zh] 样式规则 `a {` */
  a {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.auth-footer {` */
.auth-footer {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 16px 28px 24px;` */
  padding: 16px 28px 24px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;

  /* [zh] 样式规则 `a {` */
  a {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary-hover;` */
      color: $color-primary-hover;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
