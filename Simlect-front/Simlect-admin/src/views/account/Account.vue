<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="login-page">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="login-panel">
      <!-- [zh] 开始标签 `<section>` -->
      <section class="panel-brand" aria-hidden="true">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="brand-glow" />
        <!-- [zh] 开始标签 `<div>` -->
        <div class="brand-content">
          <!-- [zh] 开始标签 `<BrandMark>` -->
          <BrandMark variant="light" class="brand-mark" />
          <!-- [zh] 开始标签 `<h1>` -->
          <h1 class="brand-name">简选</h1>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="brand-tagline">Simlect · 运营后台</p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="brand-desc">
            <!-- [zh] 模板内容：`统一管理商品、订单、营销与数据，为日常运营提供清晰高效的工作台。` -->
            统一管理商品、订单、营销与数据，为日常运营提供清晰高效的工作台。
          <!-- [zh] 闭合标签 `</p>` -->
          </p>
          <!-- [zh] 开始标签 `<ul>` -->
          <ul class="brand-list">
            <!-- [zh] 开始标签 `<li>` -->
            <li>商品与库存</li>
            <!-- [zh] 开始标签 `<li>` -->
            <li>订单与物流</li>
            <!-- [zh] 开始标签 `<li>` -->
            <li>营销与数据中心</li>
          <!-- [zh] 闭合标签 `</ul>` -->
          </ul>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<section>` -->
      <section class="panel-form">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="form-wrap">
          <!-- [zh] 开始标签 `<header>` -->
          <header class="form-header">
            <!-- [zh] 开始标签 `<h2>` -->
            <h2>管理员登录</h2>
            <!-- [zh] 开始标签 `<p>` -->
            <p>使用管理员账号进入系统</p>
          <!-- [zh] 闭合标签 `</header>` -->
          </header>

          <!-- [zh] 开始标签 `<el-form>` -->
          <el-form
            class="login-form"
            :model="formData"
            :rules="rules"
            ref="formDataRef"
            label-position="top"
            @submit.prevent="doSubmit"
          >
            <!-- [zh] 开始标签 `<el-form-item>` -->
            <el-form-item label="账号" prop="account">
              <!-- [zh] 开始标签 `<el-input>` -->
              <el-input
                size="large"
                clearable
                placeholder="请输入账号"
                v-model="formData.account"
                maxlength="150"
              >
                <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
                <template #prefix>
                  <!-- [zh] 开始标签 `<span>` -->
                  <span class="iconfont icon-account" />
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                show-password
                size="large"
                placeholder="请输入密码"
                v-model="formData.password"
              >
                <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
                <template #prefix>
                  <!-- [zh] 开始标签 `<span>` -->
                  <span class="iconfont icon-password" />
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="验证码" prop="checkCode">
              <div class="check-code-row">
                <el-input
                  size="large"
                  placeholder="请输入验证码"
                  v-model="formData.checkCode"
                  @keyup.enter="doSubmit"
                >
                  <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
                  <template #prefix>
                    <!-- [zh] 开始标签 `<span>` -->
                    <span class="iconfont icon-checkcode" />
                  </template>
                </el-input>
                <button
                  type="button"
                  class="captcha-btn"
                  title="点击刷新验证码"
                  @click="changeCheckCode"
                >
                  <img
                    v-if="checkCodeInfo.checkCode"
                    :src="checkCodeInfo.checkCode"
                    alt="验证码"
                    class="captcha-img"
                  />
                  <span v-else class="captcha-placeholder">加载中</span>
                </button>
              </div>
            </el-form-item>

            <el-button type="primary" size="large" class="submit-btn" @click="doSubmit">
              登录
            </el-button>
          </el-form>

          <p class="form-footer">
            <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener">闽ICP备2026020850号</a>
          </p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
 `vue`
import { ref, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BrandMark from '@/components/BrandMark.vue'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const checkCodeInfo = ref({})
const changeCheckCode = async () => {
  const result = await proxy.Request({
    url: proxy.Api.checkCode,
  })
  if (!result) {
    return
  }
  checkCodeInfo.value = result.data
}
changeCheckCode()

const formData = ref({})
const formDataRef = ref()
const rules = {
  account: [{ required: true, message: '请输入账号' }],
  password: [{ required: true, message: '请输入密码' }],
  checkCode: [{ required: true, message: '请输入图片验证码' }],
}

const doSubmit = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    const params = { ...formData.value }
    params.checkCodeKey = checkCodeInfo.value.checkCodeKey
    const result = await proxy.Request({
      url: proxy.Api.login,
      params,
      errorCallback: () => {
        changeCheckCode()
      },
    })
    if (!result) {
      return
    }
    // 登录后消费 redirect 深链（若存在）
    const redirectTo = route.query.redirect
    const safeRedirect =
      typeof redirectTo === 'string' &&
      redirectTo.startsWith('/') &&
      !redirectTo.startsWith('//') &&
      !redirectTo.startsWith('/login')
    router.push(safeRedirect ? redirectTo : '/home')
    proxy.Message.success('登录成功')
  })
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.login-page {` */
.login-page {
  /* [zh] 样式规则 `--accent: #d4845f;` */
  --accent: #d4845f;
  /* [zh] 样式规则 `--accent-hover: #c9754f;` */
  --accent-hover: #c9754f;
  /* [zh] 样式规则 `--accent-soft: #f3e6de;` */
  --accent-soft: #f3e6de;
  /* [zh] 样式规则 `--brand-ink: #4a3f3a;` */
  --brand-ink: #4a3f3a;
  /* [zh] 样式规则 `--brand-muted: #7a6a62;` */
  --brand-muted: #7a6a62;
  /* [zh] 样式规则 `--ink: #2c2c34;` */
  --ink: #2c2c34;
  /* [zh] 样式规则 `--muted: #6b7280;` */
  --muted: #6b7280;
  /* [zh] 样式规则 `--line: #e8eaed;` */
  --line: #e8eaed;
  /* [zh] 样式规则 `--panel: #ffffff;` */
  --panel: #ffffff;

  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `min-height: 100vh;` */
  min-height: 100vh;
  /* [zh] 样式规则 `min-height: 100dvh;` */
  min-height: 100dvh;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `padding: 32px 24px;` */
  padding: 32px 24px;
  /* [zh] 样式规则 `background-color: #f0ebe6;` */
  background-color: #f0ebe6;
  /* [zh] 样式规则 `background-image:` */
  background-image:
    /* [zh] 样式声明 */
    radial-gradient(ellipse 120% 80% at 0% 0%, rgba(212, 132, 95, 0.18), transparent 50%),
    /* [zh] 样式声明 */
    radial-gradient(ellipse 90% 70% at 100% 100%, rgba(255, 255, 255, 0.5), transparent 45%);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.login-panel {` */
.login-panel {
  /* [zh] 样式规则 `box-sizing: border-box;` */
  box-sizing: border-box;
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: minmax(280px, 1fr` */
  grid-template-columns: minmax(280px, 1fr) 400px;
  /* [zh] 样式规则 `width: min(920px, 100%);` */
  width: min(920px, 100%);
  /* [zh] 样式规则 `min-height: 520px;` */
  min-height: 520px;
  /* [zh] 样式规则 `border-radius: 16px;` */
  border-radius: 16px;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: var(--panel);` */
  background: var(--panel);
  /* [zh] 样式规则 `border: 1px solid rgba(255, 255, 255, 0.` */
  border: 1px solid rgba(255, 255, 255, 0.65);
  /* [zh] 样式规则 `box-shadow:` */
  box-shadow:
    /* [zh] 样式声明 */
    0 0 0 1px rgba(22, 22, 26, 0.04),
    /* [zh] 样式声明 */
    0 20px 50px rgba(22, 22, 26, 0.1);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.panel-brand {` */
.panel-brand {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `padding: 48px 40px;` */
  padding: 48px 40px;
  /* [zh] 样式规则 `background: linear-gradient(165deg, #f2e` */
  background: linear-gradient(165deg, #f2e0d6 0%, #e8cfc2 48%, #dfc4b5 100%);
  /* [zh] 样式规则 `color: var(--brand-ink);` */
  color: var(--brand-ink);
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `.brand-glow {` */
  .brand-glow {
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `inset: -20% -10% auto auto;` */
    inset: -20% -10% auto auto;
    /* [zh] 样式规则 `width: 70%;` */
    width: 70%;
    /* [zh] 样式规则 `aspect-ratio: 1;` */
    aspect-ratio: 1;
    /* [zh] 样式规则 `background: radial-gradient(circle, rgba` */
    background: radial-gradient(circle, rgba(255, 255, 255, 0.45) 0%, transparent 68%);
    /* [zh] 样式规则 `pointer-events: none;` */
    pointer-events: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-content {` */
  .brand-content {
    /* [zh] 样式规则 `position: relative;` */
    position: relative;
    /* [zh] 样式规则 `z-index: 1;` */
    z-index: 1;
    /* [zh] 样式规则 `max-width: 320px;` */
    max-width: 320px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-mark {` */
  .brand-mark {
    /* [zh] 样式规则 `width: 48px;` */
    width: 48px;
    /* [zh] 样式规则 `height: 48px;` */
    height: 48px;
    /* [zh] 样式规则 `margin-bottom: 24px;` */
    margin-bottom: 24px;
    /* [zh] 样式规则 `box-shadow: 0 4px 14px rgba(92, 74, 66, ` */
    box-shadow: 0 4px 14px rgba(92, 74, 66, 0.08);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-name {` */
  .brand-name {
    /* [zh] 样式规则 `margin: 0 0 6px;` */
    margin: 0 0 6px;
    /* [zh] 样式规则 `font-size: 28px;` */
    font-size: 28px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `letter-spacing: 0.06em;` */
    letter-spacing: 0.06em;
    /* [zh] 样式规则 `color: var(--brand-ink);` */
    color: var(--brand-ink);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-tagline {` */
  .brand-tagline {
    /* [zh] 样式规则 `margin: 0 0 20px;` */
    margin: 0 0 20px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `letter-spacing: 0.2em;` */
    letter-spacing: 0.2em;
    /* [zh] 样式规则 `text-transform: uppercase;` */
    text-transform: uppercase;
    /* [zh] 样式规则 `color: var(--brand-muted);` */
    color: var(--brand-muted);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-desc {` */
  .brand-desc {
    /* [zh] 样式规则 `margin: 0 0 28px;` */
    margin: 0 0 28px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `line-height: 1.7;` */
    line-height: 1.7;
    /* [zh] 样式规则 `color: var(--brand-muted);` */
    color: var(--brand-muted);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.brand-list {` */
  .brand-list {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `list-style: none;` */
    list-style: none;
    /* [zh] 样式规则 `border-top: 1px solid rgba(74, 63, 58, 0` */
    border-top: 1px solid rgba(74, 63, 58, 0.12);

    /* [zh] 样式规则 `li {` */
    li {
      /* [zh] 样式规则 `padding: 12px 0;` */
      padding: 12px 0;
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `color: var(--brand-muted);` */
      color: var(--brand-muted);
      /* [zh] 样式规则 `border-bottom: 1px solid rgba(74, 63, 58` */
      border-bottom: 1px solid rgba(74, 63, 58, 0.08);

      /* [zh] 样式规则 `&::before {` */
      &::before {
        /* [zh] 样式规则 `content: '—';` */
        content: '—';
        /* [zh] 样式规则 `margin-right: 10px;` */
        margin-right: 10px;
        /* [zh] 样式规则 `color: var(--accent-hover);` */
        color: var(--accent-hover);
        /* [zh] 样式规则 `opacity: 0.95;` */
        opacity: 0.95;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.panel-form {` */
.panel-form {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `padding: 48px 40px;` */
  padding: 48px 40px;
  /* [zh] 样式规则 `background: #fafbfc;` */
  background: #fafbfc;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.form-wrap {` */
.form-wrap {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: 320px;` */
  max-width: 320px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.form-header {` */
.form-header {
  /* [zh] 样式规则 `margin-bottom: 28px;` */
  margin-bottom: 28px;

  /* [zh] 样式规则 `h2 {` */
  h2 {
    /* [zh] 样式规则 `margin: 0 0 8px;` */
    margin: 0 0 8px;
    /* [zh] 样式规则 `font-size: 22px;` */
    font-size: 22px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--ink);` */
    color: var(--ink);
    /* [zh] 样式规则 `letter-spacing: 0.02em;` */
    letter-spacing: 0.02em;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `p {` */
  p {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: var(--muted);` */
    color: var(--muted);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.login-form {` */
.login-form {
  /* [zh] 样式规则 `:deep(.el-form-item) {` */
  :deep(.el-form-item) {
    /* [zh] 样式规则 `margin-bottom: 18px;` */
    margin-bottom: 18px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.el-form-item__label) {` */
  :deep(.el-form-item__label) {
    /* [zh] 样式规则 `padding-bottom: 6px;` */
    padding-bottom: 6px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: #374151;` */
    color: #374151;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.el-input__wrapper) {` */
  :deep(.el-input__wrapper) {
    /* [zh] 样式规则 `border-radius: 8px;` */
    border-radius: 8px;
    /* [zh] 样式规则 `min-height: 42px;` */
    min-height: 42px;
    /* [zh] 样式规则 `box-shadow: 0 0 0 1px var(--line) inset;` */
    box-shadow: 0 0 0 1px var(--line) inset;
    /* [zh] 样式规则 `background: #fff;` */
    background: #fff;
    /* [zh] 样式规则 `transition: box-shadow 0.2s;` */
    transition: box-shadow 0.2s;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.el-input__wrapper.is-focus) {` */
  :deep(.el-input__wrapper.is-focus) {
    /* [zh] 样式规则 `box-shadow: 0 0 0 1px var(--accent) inse` */
    box-shadow: 0 0 0 1px var(--accent) inset;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.iconfont) {` */
  :deep(.iconfont) {
    /* [zh] 样式规则 `color: #9ca3af;` */
    color: #9ca3af;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.check-code-row {` */
.check-code-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;

  /* [zh] 样式规则 `.el-input {` */
  .el-input {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.captcha-btn {` */
.captcha-btn {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `width: 108px;` */
  width: 108px;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `border: 1px solid var(--line);` */
  border: 1px solid var(--line);
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `background: #fff;` */
  background: #fff;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `transition: border-color 0.2s;` */
  transition: border-color 0.2s;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `border-color: #c4c9d0;` */
    border-color: #c4c9d0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.captcha-img {` */
.captcha-img {
  /* [zh] 样式规则 `display: block;` */
  display: block;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 40px;` */
  height: 40px;
  /* [zh] 样式规则 `object-fit: cover;` */
  object-fit: cover;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.captcha-placeholder {` */
.captcha-placeholder {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `place-items: center;` */
  place-items: center;
  /* [zh] 样式规则 `height: 40px;` */
  height: 40px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--muted);` */
  color: var(--muted);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.submit-btn {` */
.submit-btn {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `margin-top: 4px;` */
  margin-top: 4px;
  /* [zh] 样式规则 `height: 44px;` */
  height: 44px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `letter-spacing: 0.08em;` */
  letter-spacing: 0.08em;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `--el-button-bg-color: var(--accent);` */
  --el-button-bg-color: var(--accent);
  /* [zh] 样式规则 `--el-button-border-color: var(--accent);` */
  --el-button-border-color: var(--accent);
  /* [zh] 样式规则 `--el-button-hover-bg-color: var(--accent` */
  --el-button-hover-bg-color: var(--accent-hover);
  /* [zh] 样式规则 `--el-button-hover-border-color: var(--ac` */
  --el-button-hover-border-color: var(--accent-hover);
  /* [zh] 样式规则 `--el-button-active-bg-color: var(--accen` */
  --el-button-active-bg-color: var(--accent-hover);
  /* [zh] 样式规则 `--el-button-active-border-color: var(--a` */
  --el-button-active-border-color: var(--accent-hover);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.form-footer {` */
.form-footer {
  /* [zh] 样式规则 `margin: 24px 0 0;` */
  margin: 24px 0 0;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: #b0b6c0;` */
  color: #b0b6c0;

  /* [zh] 样式规则 `a {` */
  a {
    /* [zh] 样式规则 `color: #b0b6c0;` */
    color: #b0b6c0;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
    /* [zh] 样式规则 `transition: color 0.2s;` */
    transition: color 0.2s;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: var(--accent);` */
      color: var(--accent);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `@media (max-width: 860px) {` */
@media (max-width: 860px) {
  /* [zh] 样式规则 `.login-page {` */
  .login-page {
    /* [zh] 样式规则 `padding: 16px;` */
    padding: 16px;
    /* [zh] 样式规则 `align-items: flex-start;` */
    align-items: flex-start;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.login-panel {` */
  .login-panel {
    /* [zh] 样式规则 `grid-template-columns: 1fr;` */
    grid-template-columns: 1fr;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `max-width: 420px;` */
    max-width: 420px;
    /* [zh] 样式规则 `min-height: auto;` */
    min-height: auto;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.panel-brand {` */
  .panel-brand {
    /* [zh] 样式规则 `padding: 32px 28px 24px;` */
    padding: 32px 28px 24px;

    /* [zh] 样式声明 */
    .brand-desc,
    /* [zh] 样式规则 `.brand-list {` */
    .brand-list {
      /* [zh] 样式规则 `display: none;` */
      display: none;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.brand-mark {` */
    .brand-mark {
      /* [zh] 样式规则 `margin-bottom: 16px;` */
      margin-bottom: 16px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.panel-form {` */
  .panel-form {
    /* [zh] 样式规则 `padding: 32px 28px 36px;` */
    padding: 32px 28px 36px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
