<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pay-page">
    <!-- [zh] 开始标签 `<section>` -->
    <section class="pay-card card-flat">
      <!-- [zh] 开始标签 `<h2>` -->
      <h2 class="pay-title">订单支付</h2>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="pay-order-no">支付单号：{{ payOrderId() }}</p>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="orderInfo" class="pay-summary">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="summary-row">
          <!-- [zh] 开始标签 `<span>` -->
          <span>订单状态</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span>{{ orderStatusLabel(orderInfo.orderStatus) }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="orderInfo.subject" class="summary-row">
          <!-- [zh] 开始标签 `<span>` -->
          <span>订单说明</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="subject">{{ orderInfo.subject }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="summary-row highlight">
          <!-- [zh] 开始标签 `<span>` -->
          <span>应付金额</span>
          <!-- [zh] 开始标签 `<strong>` -->
          <strong class="price-text">¥{{ formatMoney(payAmount) }}</strong>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="paySuccess" class="pay-result success">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="result-title">支付成功</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="result-desc">订单已支付，可在「我的订单」查看</p>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="pay-actions">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" round @click="goOrders">查看订单</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button round @click="goHome">继续购物</el-button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-else-if="payLaunched" class="pay-result pending">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="result-title">请在支付宝完成支付</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="result-desc">若未自动跳转，请点击下方按钮重新唤起支付宝。</p>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" plain round :loading="reopening" @click="reopenPayPage">
          <!-- [zh] 模板内容：`重新跳转支付宝` -->
          重新跳转支付宝
        <!-- [zh] 闭合标签 `</el-button>` -->
        </el-button>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="pay-check">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" round :loading="checking" @click="checkPay">我已支付</el-button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="pay-actions secondary">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button round @click="goOrders">稍后支付</el-button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-else-if="loadError" class="pay-result error">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="result-desc">{{ loadError }}</p>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" round :loading="launching" @click="startPay">重试</el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-else-if="isProcessing" class="pay-result pending">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="result-title">支付处理中</p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="result-desc">正在确认支付结果，请稍候；也可点击下方按钮手动查询。</p>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="pay-check">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" round :loading="checking" @click="checkPay">查询支付结果</el-button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-else class="pay-loading">
        <!-- [zh] 开始标签 `<p>` -->
        <p>正在跳转支付宝…</p>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
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
} = usePaymentPage('mobile');
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pay-page {` */
.pay-page {
  /* [zh] 样式规则 `min-height: calc(100vh - 120px);` */
  min-height: calc(100vh - 120px);
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `padding: 8px 0 24px;` */
  padding: 8px 0 24px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-card {` */
.pay-card {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `max-width: 420px;` */
  max-width: 420px;
  /* [zh] 样式规则 `padding: 20px 16px;` */
  padding: 20px 16px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-title {` */
.pay-title {
  /* [zh] 样式规则 `margin: 0 0 8px;` */
  margin: 0 0 8px;
  /* [zh] 样式规则 `font-size: 20px;` */
  font-size: 20px;
  /* [zh] 样式规则 `font-weight: 700;` */
  font-weight: 700;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-order-no {` */
.pay-order-no {
  /* [zh] 样式规则 `margin: 0 0 16px;` */
  margin: 0 0 16px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
  /* [zh] 样式规则 `word-break: break-all;` */
  word-break: break-all;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-summary {` */
.pay-summary {
  /* [zh] 样式规则 `margin-bottom: 20px;` */
  margin-bottom: 20px;
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `background: #fafafa;` */
  background: #fafafa;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;
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
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `padding: 6px 0;` */
  padding: 6px 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;

  /* [zh] 样式规则 `.subject {` */
  .subject {
    /* [zh] 样式规则 `text-align: right;` */
    text-align: right;
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `word-break: break-word;` */
    word-break: break-word;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.highlight .price-text {` */
  &.highlight .price-text {
    /* [zh] 样式规则 `font-size: 22px;` */
    font-size: 22px;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-result {` */
.pay-result {
  /* [zh] 样式规则 `padding: 8px 0;` */
  padding: 8px 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.result-title {` */
.result-title {
  /* [zh] 样式规则 `margin: 0 0 8px;` */
  margin: 0 0 8px;
  /* [zh] 样式规则 `font-size: 16px;` */
  font-size: 16px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.result-desc {` */
.result-desc {
  /* [zh] 样式规则 `margin: 0 0 16px;` */
  margin: 0 0 16px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-check {` */
.pay-check {
  /* [zh] 样式规则 `margin: 16px 0 12px;` */
  margin: 16px 0 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-actions {` */
.pay-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;

  /* [zh] 样式规则 `&.secondary {` */
  &.secondary {
    /* [zh] 样式规则 `margin-top: 8px;` */
    margin-top: 8px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-loading {` */
.pay-loading {
  /* [zh] 样式规则 `padding: 24px 0;` */
  padding: 24px 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}
</style>
