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
          <h2>找回密码</h2>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="brand-name">简选 · Simlect</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="brand-tip">输入邮箱，重置您的登录密码</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<el-form>` -->
      <el-form class="auth-form" label-position="top" @submit.prevent="submit">
        <!-- [zh] 开始标签 `<el-form-item>` -->
        <el-form-item label="邮箱">
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input v-model="form.email" placeholder="请输入注册邮箱" size="large" maxlength="150" />
        <!-- [zh] 闭合标签 `</el-form-item>` -->
        </el-form-item>
        <!-- [zh] 开始标签 `<el-form-item>` -->
        <el-form-item label="新密码">
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="8-18位，含字母与数字"
            size="large"
            show-password
          />
          <!-- [zh] 开始标签 `<p>` -->
          <p class="pwd-hint">{{ PASSWORD_FORMAT_HINT }}</p>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="form.newPassword" class="pwd-strength">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="pwd-strength-bar">
              <!-- [zh] 开始标签 `<span>` -->
              <span :class="['seg', { on: passwordStrength >= 1, weak: passwordStrength === 1 }]" />
              <!-- [zh] 开始标签 `<span>` -->
              <span :class="['seg', { on: passwordStrength >= 2, mid: passwordStrength === 2 }]" />
              <!-- [zh] 开始标签 `<span>` -->
              <span :class="['seg', { on: passwordStrength >= 3, strong: passwordStrength === 3 }]" />
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="pwd-strength-text" :class="strengthClass">{{ strengthLabel }}</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</el-form-item>` -->
        </el-form-item>
        <!-- [zh] 开始标签 `<el-form-item>` -->
        <el-form-item label="确认密码">
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            size="large"
            show-password
          />
        <!-- [zh] 闭合标签 `</el-form-item>` -->
        </el-form-item>
        <!-- [zh] 开始标签 `<el-form-item>` -->
        <el-form-item label="邮箱验证码">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="email-code-row">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input
              v-model="form.emailCode"
              placeholder="请输入验证码"
              size="large"
              maxlength="6"
              class="code-input"
            />
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button
              class="send-code-btn"
              size="large"
              :disabled="emailCodeCountdown > 0 || !form.email"
              @click="sendEmailCode"
            >
              <!-- [zh] Mustache 插值表达式 -->
              {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s后重发` : '发送验证码' }}
            <!-- [zh] 闭合标签 `</el-button>` -->
            </el-button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</el-form-item>` -->
        </el-form-item>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="error" class="error-tip">{{ error }}</p>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" class="submit-btn" size="large" :loading="submitting" @click="submit">重 置 密 码</el-button>
      <!-- [zh] 闭合标签 `</el-form>` -->
      </el-form>

      <!-- [zh] 开始标签 `<p>` -->
      <p class="auth-footer">
        <!-- [zh] 模板内容：`想起密码了？` -->
        想起密码了？
        <!-- [zh] 开始标签 `<RouterLink>` -->
        <RouterLink to="/login">去登录</RouterLink>
      <!-- [zh] 闭合标签 `</p>` -->
      </p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
  <!-- [zh] 开始标签 `<SlideCaptchaDialog>` -->
  <SlideCaptchaDialog ref="slideCaptchaRef" />
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { toast } from '@/utils/toast';
import BrandMark from '@/components/common/BrandMark.vue';
import SlideCaptchaDialog from '@/components/business/SlideCaptchaDialog.vue';
import { accountApi } from '@/api/modules';
import { isValidEmail, isValidPassword, PASSWORD_FORMAT_HINT } from '@/constants/validation';
import { useEmailCode, type SlideCaptchaDialogExpose } from '@/composables/useEmailCode';

const router = useRouter();
const slideCaptchaRef = ref<SlideCaptchaDialogExpose | null>(null);
const error = ref('');
const submitting = ref(false);
const { emailCodeCountdown, startCountdown, requestSlideVerification } = useEmailCode();

const form = reactive({
  email: '',
  newPassword: '',
  confirmPassword: '',
  emailCode: ''
});

const passwordStrength = computed(() => {
  const p = form.newPassword;
  if (!p) return 0;
  let score = 0;
  if (p.length >= 6) score++;
  if (p.length >= 10) score++;
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) score++;
  if (/\d/.test(p)) score++;
  if (/[^A-Za-z0-9]/.test(p)) score++;
  if (score <= 1) return 1;
  if (score <= 3) return 2;
  return 3;
});

const strengthLabel = computed(() => {
  const map = ['', '弱', '中', '强'] as const;
  return map[passwordStrength.value] || '';
});

const strengthClass = computed(() => {
  const map = ['', 'weak', 'mid', 'strong'] as const;
  return map[passwordStrength.value] || '';
});

const sendEmailCode = async () => {
  error.value = '';
  if (!form.email?.trim()) {
    ElMessage.warning('请先输入邮箱');
    return;
  }
  if (!isValidEmail(form.email)) {
    error.value = '请输入有效的邮箱地址';
    return;
  }
  try {
    const captchaVerification = await requestSlideVerification(slideCaptchaRef.value);
    await accountApi.getEmailCode({ email: form.email, captchaVerification });
    ElMessage.success('验证码已发送');
    startCountdown();
  } catch (e: any) {
    error.value = e?.info || e?.message || '验证码发送失败';
  }
};

const submit = async () => {
  error.value = '';
  if (!form.email?.trim()) {
    ElMessage.warning('请输入邮箱');
    return;
  }
  if (!isValidEmail(form.email)) {
    error.value = '请输入有效的邮箱地址';
    return;
  }
  if (!isValidPassword(form.newPassword)) {
    error.value = PASSWORD_FORMAT_HINT;
    return;
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = '两次密码不一致';
    return;
  }
  if (!form.emailCode) {
    error.value = '请输入邮箱验证码';
    return;
  }
  submitting.value = true;
  try {
    await accountApi.forgetPassword({
      email: form.email,
      newPassword: form.newPassword,
      checkCode: form.emailCode
    });
    toast.success('密码重置成功，请登录');
    router.push('/login');
  } catch (e: any) {
    error.value = e?.info || e?.message || '密码重置失败，请稍后重试';
  } finally {
    submitting.value = false;
  }
};
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
  /* [zh] 样式规则 `min-height: calc(100vh - 56px - 80px);` */
  min-height: calc(100vh - 56px - 80px);
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `padding: 24px 16px 48px;` */
  padding: 24px 16px 48px;
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
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
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

/* [zh] 样式规则 `.pwd-hint {` */
.pwd-hint {
  /* [zh] 样式规则 `margin: 8px 0 0;` */
  margin: 8px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pwd-strength {` */
.pwd-strength {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `margin-top: 8px;` */
  margin-top: 8px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pwd-strength-bar {` */
.pwd-strength-bar {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `height: 6px;` */
  height: 6px;

  /* [zh] 样式规则 `.seg {` */
  .seg {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `background: #eee;` */
    background: #eee;
    /* [zh] 样式规则 `transition: background 0.2s;` */
    transition: background 0.2s;

    /* [zh] 样式规则 `&.on.weak {` */
    &.on.weak {
      /* [zh] 样式规则 `background: rgba($color-primary, 0.35);` */
      background: rgba($color-primary, 0.35);
    /* [zh] 样式规则 `}` */
    }
    /* [zh] 样式规则 `&.on.mid {` */
    &.on.mid {
      /* [zh] 样式规则 `background: rgba($color-primary, 0.6);` */
      background: rgba($color-primary, 0.6);
    /* [zh] 样式规则 `}` */
    }
    /* [zh] 样式规则 `&.on.strong {` */
    &.on.strong {
      /* [zh] 样式规则 `background: $color-primary;` */
      background: $color-primary;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pwd-strength-text {` */
.pwd-strength-text {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;

  /* [zh] 样式规则 `&.weak {` */
  &.weak {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `&.mid {` */
  &.mid {
    /* [zh] 样式规则 `color: $color-primary-hover;` */
    color: $color-primary-hover;
  /* [zh] 样式规则 `}` */
  }
  /* [zh] 样式规则 `&.strong {` */
  &.strong {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
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

/* [zh] 样式规则 `.email-code-row {` */
.email-code-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;

  /* [zh] 样式规则 `.code-input {` */
  .code-input {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.send-code-btn {` */
  .send-code-btn {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.auth-footer {` */
.auth-footer {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 16px 28px 28px;` */
  padding: 16px 28px 28px;
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

/* [zh] 样式规则 `@media (max-width: 480px) {` */
@media (max-width: 480px) {
  /* [zh] 样式规则 `.auth-page.ignore {` */
  .auth-page.ignore {
    /* [zh] 样式规则 `padding: 16px 12px 32px;` */
    padding: 16px 12px 32px;
    /* [zh] 样式规则 `min-height: auto;` */
    min-height: auto;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  .auth-brand,
  /* [zh] 样式声明 */
  .auth-form,
  /* [zh] 样式规则 `.auth-footer {` */
  .auth-footer {
    /* [zh] 样式规则 `padding-left: 20px;` */
    padding-left: 20px;
    /* [zh] 样式规则 `padding-right: 20px;` */
    padding-right: 20px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>