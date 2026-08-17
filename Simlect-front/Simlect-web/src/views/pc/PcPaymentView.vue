<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-payment ignore">
    <!-- [zh] 开始标签 `<nav>` -->
    <nav class="pc-payment-steps" aria-label="支付流程">
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/cart" class="step-link">购物车</RouterLink>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="step-sep" aria-hidden="true" />
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/checkout" class="step-link">确认订单</RouterLink>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="step-sep" aria-hidden="true" />
      <!-- [zh] 开始标签 `<span>` -->
      <span class="step-current">支付</span>
    <!-- [zh] 闭合标签 `</nav>` -->
    </nav>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="pc-payment-layout">
      <!-- [zh] 开始标签 `<section>` -->
      <section class="pc-payment-main">
        <!-- [zh] 开始标签 `<header>` -->
        <header class="main-head">
          <!-- [zh] 开始标签 `<h1>` -->
          <h1>订单支付</h1>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="order-no">支付单号：{{ payOrderId() }}</p>
        <!-- [zh] 闭合标签 `</header>` -->
        </header>

        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="orderInfo" class="order-summary">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="summary-row">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="label">订单状态</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span>{{ orderStatusLabel(orderInfo.orderStatus) }}</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="orderInfo.subject" class="summary-row">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="label">订单说明</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="value">{{ orderInfo.subject }}</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="summary-row highlight">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="label">应付金额</span>
            <!-- [zh] 开始标签 `<strong>` -->
            <strong class="price-text">¥{{ formatMoney(payAmount) }}</strong>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="paySuccess" class="result-card success">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="result-icon" aria-hidden="true">✓</div>
          <!-- [zh] 开始标签 `<h2>` -->
          <h2>支付成功</h2>
          <!-- [zh] 开始标签 `<p>` -->
          <p>订单已支付，可在「我的订单」查看详情与物流信息。</p>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="result-actions">
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button type="primary" size="large" @click="goOrders">查看订单</el-button>
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button size="large" @click="goHome">继续购物</el-button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div v-else-if="payLaunched" class="result-card pending">
          <!-- [zh] 开始标签 `<h2>` -->
          <h2>请在支付宝页面完成支付</h2>
          <!-- [zh] 开始标签 `<p>` -->
          <p>
            <!-- [zh] 模板内容：`支付页已在新窗口打开。若未弹出，请允许浏览器弹窗后点击下方按钮重新打开。` -->
            支付页已在新窗口打开。若未弹出，请允许浏览器弹窗后点击下方按钮重新打开。
            <!-- [zh] 模板内容：`完成支付后请点击「我已支付」同步订单状态。` -->
            完成支付后请点击「我已支付」同步订单状态。
          <!-- [zh] 闭合标签 `</p>` -->
          </p>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="pending-actions">
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button type="primary" plain :loading="reopening" @click="reopenPayPage">
              <!-- [zh] 模板内容：`重新打开支付页` -->
              重新打开支付页
            <!-- [zh] 闭合标签 `</el-button>` -->
            </el-button>
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button type="primary" :loading="checking" @click="checkPay">我已支付</el-button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="secondary-actions">
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button link @click="goOrders">稍后支付，查看订单</el-button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div v-else-if="loadError" class="result-card error">
          <!-- [zh] 开始标签 `<h2>` -->
          <h2>支付发起失败</h2>
          <!-- [zh] 开始标签 `<p>` -->
          <p>{{ loadError }}</p>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" :loading="launching" @click="startPay">重试</el-button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div v-else-if="isProcessing" class="result-card pending">
          <!-- [zh] 开始标签 `<h2>` -->
          <h2>支付处理中</h2>
          <!-- [zh] 开始标签 `<p>` -->
          <p>正在确认支付结果，请稍候；也可点击下方按钮手动查询。</p>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" :loading="checking" @click="checkPay">查询支付结果</el-button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>

        <!-- [zh] 开始标签 `<div>` -->
        <div v-else class="result-card loading">
          <!-- [zh] 开始标签 `<el-skeleton>` -->
          <el-skeleton animated :rows="3" />
          <!-- [zh] 开始标签 `<p>` -->
          <p class="loading-tip">正在跳转支付宝…</p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<aside>` -->
      <aside class="pc-payment-aside">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="aside-card">
          <!-- [zh] 开始标签 `<h2>` -->
          <h2>支付说明</h2>
          <!-- [zh] 开始标签 `<ul>` -->
          <ul class="tips-list">
            <!-- [zh] 开始标签 `<li>` -->
            <li>推荐使用支付宝扫码或登录账户完成付款。</li>
            <!-- [zh] 开始标签 `<li>` -->
            <li>支付完成后系统会自动同步状态，通常几秒内生效。</li>
            <!-- [zh] 开始标签 `<li>` -->
            <li>若长时间未更新，请点击「我已支付」手动查询。</li>
            <!-- [zh] 开始标签 `<li>` -->
            <li>遇到问题可联系智能客服或查看订单详情。</li>
          <!-- [zh] 闭合标签 `</ul>` -->
          </ul>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="secure-note">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="secure-badge">安全支付</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span>由支付宝提供担保交易</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</aside>` -->
      </aside>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router';
import { usePaymentPage } from '@/composables/usePaymentPage';

const {
  orderInfo,
  payAmount,
  payLaunched,
  paySuccess,
  launching,
  reopening,
  checking,
  loadError,
  isProcessing,
  payOrderId,
  formatMoney,
  orderStatusLabel,
  startPay,
  reopenPayPage,
  goOrders,
  goHome,
  checkPay
} = usePaymentPage('desktop');
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-payment {` */
.pc-payment {
  /* [zh] 样式规则 `max-width: 1080px;` */
  max-width: 1080px;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `padding: 8px 0 48px;` */
  padding: 8px 0 48px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-payment-steps {` */
.pc-payment-steps {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `margin-bottom: 20px;` */
  margin-bottom: 20px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;

  /* [zh] 样式规则 `.step-link {` */
  .step-link {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `text-decoration: underline;` */
      text-decoration: underline;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.step-current {` */
  .step-current {
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.step-sep {` */
  .step-sep {
    /* [zh] 样式规则 `width: 24px;` */
    width: 24px;
    /* [zh] 样式规则 `height: 1px;` */
    height: 1px;
    /* [zh] 样式规则 `background: $color-border;` */
    background: $color-border;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-payment-layout {` */
.pc-payment-layout {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: minmax(0, 1fr) 30` */
  grid-template-columns: minmax(0, 1fr) 300px;
  /* [zh] 样式规则 `gap: 20px;` */
  gap: 20px;
  /* [zh] 样式规则 `align-items: start;` */
  align-items: start;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-payment-main {` */
.pc-payment-main {
  /* [zh] 样式规则 `padding: 28px 32px;` */
  padding: 28px 32px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.main-head {` */
.main-head {
  /* [zh] 样式规则 `margin-bottom: 20px;` */
  margin-bottom: 20px;
  /* [zh] 样式规则 `padding-bottom: 16px;` */
  padding-bottom: 16px;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border-l` */
  border-bottom: 1px solid $color-border-light;

  /* [zh] 样式规则 `h1 {` */
  h1 {
    /* [zh] 样式规则 `margin: 0 0 8px;` */
    margin: 0 0 8px;
    /* [zh] 样式规则 `font-size: 22px;` */
    font-size: 22px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.order-no {` */
  .order-no {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `word-break: break-all;` */
    word-break: break-all;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.order-summary {` */
.order-summary {
  /* [zh] 样式规则 `margin-bottom: 24px;` */
  margin-bottom: 24px;
  /* [zh] 样式规则 `padding: 16px 18px;` */
  padding: 16px 18px;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.summary-row {` */
.summary-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
  /* [zh] 样式规则 `padding: 8px 0;` */
  padding: 8px 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;

  /* [zh] 样式规则 `.label {` */
  .label {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.value {` */
  .value {
    /* [zh] 样式规则 `text-align: right;` */
    text-align: right;
    /* [zh] 样式规则 `word-break: break-word;` */
    word-break: break-word;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.highlight .price-text {` */
  &.highlight .price-text {
    /* [zh] 样式规则 `font-size: 28px;` */
    font-size: 28px;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.result-card {` */
.result-card {
  /* [zh] 样式规则 `padding: 8px 0;` */
  padding: 8px 0;

  /* [zh] 样式规则 `h2 {` */
  h2 {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 18px;` */
    font-size: 18px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `p {` */
  p {
    /* [zh] 样式规则 `margin: 0 0 20px;` */
    margin: 0 0 20px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `line-height: 1.6;` */
    line-height: 1.6;
    /* [zh] 样式规则 `color: $color-text-secondary;` */
    color: $color-text-secondary;
    /* [zh] 样式规则 `max-width: 520px;` */
    max-width: 520px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.success {` */
  &.success {
    /* [zh] 样式规则 `text-align: left;` */
    text-align: left;

    /* [zh] 样式规则 `.result-icon {` */
    .result-icon {
      /* [zh] 样式规则 `width: 48px;` */
      width: 48px;
      /* [zh] 样式规则 `height: 48px;` */
      height: 48px;
      /* [zh] 样式规则 `margin-bottom: 12px;` */
      margin-bottom: 12px;
      /* [zh] 样式规则 `border-radius: 50%;` */
      border-radius: 50%;
      /* [zh] 样式规则 `display: grid;` */
      display: grid;
      /* [zh] 样式规则 `place-items: center;` */
      place-items: center;
      /* [zh] 样式规则 `font-size: 24px;` */
      font-size: 24px;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `background: $color-success;` */
      background: $color-success;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.result-actions,
/* [zh] 样式规则 `.pending-actions {` */
.pending-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.secondary-actions {` */
.secondary-actions {
  /* [zh] 样式规则 `margin-top: 12px;` */
  margin-top: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.loading-tip {` */
.loading-tip {
  /* [zh] 样式规则 `margin-top: 12px;` */
  margin-top: 12px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-payment-aside {` */
.pc-payment-aside {
  /* [zh] 样式规则 `position: sticky;` */
  position: sticky;
  /* [zh] 样式规则 `top: 88px;` */
  top: 88px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.aside-card {` */
.aside-card {
  /* [zh] 样式规则 `padding: 20px;` */
  padding: 20px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;

  /* [zh] 样式规则 `h2 {` */
  h2 {
    /* [zh] 样式规则 `margin: 0 0 14px;` */
    margin: 0 0 14px;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tips-list {` */
.tips-list {
  /* [zh] 样式规则 `margin: 0 0 16px;` */
  margin: 0 0 16px;
  /* [zh] 样式规则 `padding-left: 18px;` */
  padding-left: 18px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.7;` */
  line-height: 1.7;
  /* [zh] 样式规则 `color: $color-text-secondary;` */
  color: $color-text-secondary;

  /* [zh] 样式规则 `li + li {` */
  li + li {
    /* [zh] 样式规则 `margin-top: 6px;` */
    margin-top: 6px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.secure-note {` */
.secure-note {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding-top: 14px;` */
  padding-top: 14px;
  /* [zh] 样式规则 `border-top: 1px solid $color-border-ligh` */
  border-top: 1px solid $color-border-light;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.secure-badge {` */
.secure-badge {
  /* [zh] 样式规则 `padding: 2px 8px;` */
  padding: 2px 8px;
  /* [zh] 样式规则 `border-radius: $radius-pill;` */
  border-radius: $radius-pill;
  /* [zh] 样式规则 `background: rgba($color-success, 0.12);` */
  background: rgba($color-success, 0.12);
  /* [zh] 样式规则 `color: $color-success;` */
  color: $color-success;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
/* [zh] 样式规则 `}` */
}
</style>
