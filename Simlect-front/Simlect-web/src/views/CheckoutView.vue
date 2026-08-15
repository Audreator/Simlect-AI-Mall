<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="checkout-page">
    <!-- [zh] 开始标签 `<el-skeleton>` -->
    <el-skeleton v-if="pageLoading" animated :rows="8" class="checkout-skeleton" />

    <!-- [zh] 开始标签 `<div>` -->
    <div v-else-if="initError" class="checkout-error card-flat">
      <!-- [zh] 开始标签 `<p>` -->
      <p>{{ initError }}</p>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" round @click="init">重试</el-button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template v-else-if="items.length">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="isCouponRush" class="rush-pay-banner card-flat">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-if="payCountdownMs > 0">
          <!-- [zh] 开始标签 `<p>` -->
          <p class="rush-pay-title">
            <!-- [zh] 模板内容：`支付剩余 <strong>{{ payCountdownText }}</strong>` -->
            支付剩余 <strong>{{ payCountdownText }}</strong>
          <!-- [zh] 闭合标签 `</p>` -->
          </p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="rush-pay-tip">订单已生成，超时将自动关闭。也可在「我的 → 我的订单 → 待付款」中继续支付。</p>
        </template>
        <p v-else class="rush-pay-expired">支付已超时，订单已关闭，请返回重新抢购。</p>
      </div>

      <section v-if="!isCouponRush" class="block card-flat checkout-address-block">
        <div class="block-head">
          <h3 class="block-title">收货地址</h3>
        </div>
        <p v-if="addressLoadError" class="block-error">
          {{ addressLoadError }}
          <button type="button" class="inline-retry" @click="loadAddresses">重试</button>
        </p>
        <div
          v-if="selectedAddress"
          class="checkout-address-picker is-filled"
          role="button"
          tabindex="0"
          @click="goSelectAddress"
          @keydown.enter.prevent="goSelectAddress"
        >
          <AddressCardBody :item="selectedAddress" />
          <span class="picker-change">更换</span>
        </div>
        <div v-else class="checkout-address-picker is-empty">
          <p class="empty-tip">请选择收货地址</p>
          <div class="empty-actions">
            <el-button type="primary" round size="small" @click="goSelectAddress">选择地址</el-button>
            <el-button round size="small" @click="goAddAddress">新增地址</el-button>
          </div>
        </div>
      </section>

      <section class="block card-flat">
        <div class="block-head">
          <h3 class="block-title">{{ isCouponRush ? '优惠券信息' : '商品清单' }}</h3>
          <span class="hint">共 {{ totalCount }} 件</span>
        </div>
        <ul class="goods-list">
          <li v-for="(item, index) in items" :key="`${item.productId}-${item.propertyValueIds}-${index}`" class="goods-item">
            <RouterLink v-if="!isCouponRush" :to="`/product/${item.productId}`" class="cover-wrap">
              <ProductImage :source="item.productCover" class="cover" />
            </RouterLink>
            <div v-else class="cover-wrap is-coupon">
              <el-icon class="coupon-icon"><Ticket /></el-icon>
            </div>
            <div class="goods-info">
              <RouterLink v-if="!isCouponRush" :to="`/product/${item.productId}`" class="goods-name">
                {{ item.productName }}
              </RouterLink>
              <p v-else class="goods-name">{{ item.productName }}</p>
              <p v-if="formatSkuText(item)" class="goods-sku">{{ formatSkuText(item) }}</p>
              <div class="goods-foot">
                <span class="unit-price">¥{{ Number(item.price).toFixed(2) }}</span>
                <span class="qty">×{{ item.buyCount }}</span>
                <span class="subtotal">小计 ¥{{ lineSubtotal(item).toFixed(2) }}</span>
              </div>
            </div>
          </li>
        </ul>
      </section>

      <section v-if="!isCouponRush" class="block card-flat">
        <h3 class="block-title">订单备注</h3>
        <el-input
          v-model="remark"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
          placeholder="选填：配送、包装等要求"
        />
      </section>

      <section v-if="!isCouponRush" class="block card-flat">
        <div class="block-head">
          <h3 class="block-title">优惠券</h3>
          <button type="button" class="link-btn" @click="openCouponPicker">
            {{ selectedCouponLabel }}
          </button>
        </div>
        <p v-if="couponDiscount > 0" class="coupon-tip">已抵扣 ¥{{ couponDiscount.toFixed(2) }}</p>
        <p v-if="showMinPayTip" class="coupon-tip min-pay">使用优惠券后最低需支付 ¥{{ minPayAmountText }}</p>
        <p v-else-if="usableCoupons.some((c) => c.usable) && maxAvailableDiscount > 0" class="coupon-tip hint">
          您有可用优惠券，最高可抵扣 ¥{{ maxAvailableDiscount.toFixed(2) }}
        </p>
        <p v-if="couponLoadError" class="block-error">
          {{ couponLoadError }}
          <button type="button" class="inline-retry" @click="loadCoupons">重试</button>
        </p>
      </section>

      <section class="block card-flat">
        <h3 class="block-title">支付方式</h3>
        <el-radio-group v-model="payMethod" class="pay-methods">
          <div class="pay-option" :class="{ active: payMethod === PAY_METHOD_ALIPAY_WAP }">
            <el-radio :label="PAY_METHOD_ALIPAY_WAP">支付宝</el-radio>
            <p class="pay-desc">提交后将跳转支付宝完成手机支付</p>
          </div>
        </el-radio-group>
      </section>
    </template>

    <el-empty v-else description="没有待结算的商品">
      <el-button type="primary" round @click="router.push('/cart')">返回购物车</el-button>
    </el-empty>

    <LiquidGlassSurface
      v-if="items.length && !pageLoading"
      tag="footer"
      intensity="medium"
      class="checkout-bar ignore"
    >
      <div class="bar-summary">
        <span class="label">合计</span>
        <strong class="price-text">¥{{ payableAmount }}</strong>
        <span class="count">共 {{ totalCount }} 件</span>
      </div>
      <el-button
        type="primary"
        class="btn-submit"
        round
        :loading="submitting"
        :disabled="isCouponRush && payCountdownMs <= 0"
        @click="submit"
      >
        {{ submitButtonText }}
      </el-button>
    </LiquidGlassSurface>

    <el-dialog v-model="couponVisible" title="选择优惠券" width="92%" style="max-width: 520px">
      <div class="coupon-list">
        <button type="button" class="coupon-row" :class="{ active: !selectedUserCouponId }" @click="selectCoupon(null)">
          <div class="left">
            <p class="name">不使用优惠券</p>
            <p class="desc">本单不抵扣</p>
          </div>
          <span class="tag">默认</span>
        </button>
        <el-skeleton v-if="couponLoading" animated :rows="4" />
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-else>
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-for="c in usableCoupons"
            :key="c.userCouponId"
            type="button"
            class="coupon-row"
            :class="{ active: selectedUserCouponId === c.userCouponId, disabled: !c.usable }"
            :disabled="!c.usable"
            @click="selectCoupon(c)"
          >
            <!-- [zh] 开始标签 `<div>` -->
            <div class="left">
              <!-- [zh] 开始标签 `<p>` -->
              <p class="name">{{ c.couponName }}</p>
              <!-- [zh] 开始标签 `<p>` -->
              <p class="desc">
                <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
                <template v-if="c.couponType === 2">折扣券 {{ Number(c.discountRate || 1) * 100 }}%</template>
                <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
                <template v-else>满减券</template>
                <!-- [zh] 模板内容：`·` -->
                ·
                <!-- [zh] 开始标签 `<span>` -->
                <span v-if="Number(c.thresholdAmount || 0) > 0">满 ¥{{ Number(c.thresholdAmount).toFixed(2) }} 可用</span>
                <!-- [zh] 开始标签 `<span>` -->
                <span v-else>无门槛</span>
                <!-- [zh] 模板内容：`· 有效期至 {{ formatCouponEnd(c.validEndTime) }}` -->
                · 有效期至 {{ formatCouponEnd(c.validEndTime) }}
              <!-- [zh] 闭合标签 `</p>` -->
              </p>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="right">
              <!-- [zh] 开始标签 `<p>` -->
              <p class="off">-¥{{ calcCouponDiscount(c).toFixed(2) }}</p>
              <!-- [zh] 开始标签 `<span>` -->
              <span v-if="c.usable" class="tag">可用</span>
              <!-- [zh] 开始标签 `<span>` -->
              <span v-else class="tag muted">不可用</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
          <!-- [zh] 开始标签 `<el-empty>` -->
          <el-empty v-if="!usableCoupons.length" description="暂无可用优惠券" />
        </template>
      </div>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #footer>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button @click="couponVisible = false">取消</el-button>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" :disabled="couponLoading" @click="couponVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue-router`
import { RouterLink, useRouter } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { Ticket } from '@element-plus/icons-vue';
// [zh] 导入 `@/components/common/LiquidGlassSurface.vue`
import LiquidGlassSurface from '@/components/common/LiquidGlassSurface.vue';
// [zh] 导入 `@/components/business/AddressCardBody.vue`
import AddressCardBody from '@/components/business/AddressCardBody.vue';
// [zh] 导入 `@/components/common/ProductImage.vue`
import ProductImage from '@/components/common/ProductImage.vue';
// [zh] 导入 `@/composables/useCheckoutPage`
import { useCheckoutPage } from '@/composables/useCheckoutPage';

// [zh] 声明变量 `router`
const router = useRouter();

// [zh] 代码块开始
const {
  // [zh] TypeScript/JS 语句：`pageLoading,`
  pageLoading,
  // [zh] TypeScript/JS 语句：`initError,`
  initError,
  // [zh] TypeScript/JS 语句：`addressLoadError,`
  addressLoadError,
  // [zh] TypeScript/JS 语句：`couponLoadError,`
  couponLoadError,
  // [zh] TypeScript/JS 语句：`submitting,`
  submitting,
  // [zh] TypeScript/JS 语句：`items,`
  items,
  // [zh] TypeScript/JS 语句：`isCouponRush,`
  isCouponRush,
  // [zh] TypeScript/JS 语句：`payCountdownMs,`
  payCountdownMs,
  // [zh] TypeScript/JS 语句：`payCountdownText,`
  payCountdownText,
  // [zh] TypeScript/JS 语句：`submitButtonText,`
  submitButtonText,
  // [zh] TypeScript/JS 语句：`remark,`
  remark,
  // [zh] TypeScript/JS 语句：`selectedAddress,`
  selectedAddress,
  // [zh] TypeScript/JS 语句：`payMethod,`
  payMethod,
  // [zh] TypeScript/JS 语句：`couponVisible,`
  couponVisible,
  // [zh] TypeScript/JS 语句：`couponLoading,`
  couponLoading,
  // [zh] TypeScript/JS 语句：`usableCoupons,`
  usableCoupons,
  // [zh] TypeScript/JS 语句：`selectedUserCouponId,`
  selectedUserCouponId,
  // [zh] TypeScript/JS 语句：`couponDiscount,`
  couponDiscount,
  // [zh] TypeScript/JS 语句：`payableAmount,`
  payableAmount,
  // [zh] TypeScript/JS 语句：`minPayAmountText,`
  minPayAmountText,
  // [zh] TypeScript/JS 语句：`showMinPayTip,`
  showMinPayTip,
  // [zh] TypeScript/JS 语句：`selectedCouponLabel,`
  selectedCouponLabel,
  // [zh] TypeScript/JS 语句：`maxAvailableDiscount,`
  maxAvailableDiscount,
  // [zh] TypeScript/JS 语句：`totalCount,`
  totalCount,
  // [zh] TypeScript/JS 语句：`formatCouponEnd,`
  formatCouponEnd,
  // [zh] TypeScript/JS 语句：`calcCouponDiscount,`
  calcCouponDiscount,
  // [zh] TypeScript/JS 语句：`formatSkuText,`
  formatSkuText,
  // [zh] TypeScript/JS 语句：`lineSubtotal,`
  lineSubtotal,
  // [zh] TypeScript/JS 语句：`init,`
  init,
  // [zh] TypeScript/JS 语句：`loadAddresses,`
  loadAddresses,
  // [zh] TypeScript/JS 语句：`goSelectAddress,`
  goSelectAddress,
  // [zh] TypeScript/JS 语句：`goAddAddress,`
  goAddAddress,
  // [zh] TypeScript/JS 语句：`openCouponPicker,`
  openCouponPicker,
  // [zh] TypeScript/JS 语句：`selectCoupon,`
  selectCoupon,
  // [zh] TypeScript/JS 语句：`loadCoupons,`
  loadCoupons,
  // [zh] TypeScript/JS 语句：`submit,`
  submit,
  // [zh] TypeScript/JS 语句：`PAY_METHOD_ALIPAY_WAP`
  PAY_METHOD_ALIPAY_WAP
// [zh] 语句
} = useCheckoutPage('mobile');
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.checkout-page {` */
.checkout-page {
  /* [zh] 样式规则 `padding-bottom: calc(72px + env(safe-are` */
  padding-bottom: calc(72px + env(safe-area-inset-bottom, 0));
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.rush-pay-banner {` */
.rush-pay-banner {
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
  /* [zh] 样式规则 `background: linear-gradient(135deg, rgba` */
  background: linear-gradient(135deg, rgba($color-primary, 0.12), rgba($color-primary, 0.04));
  /* [zh] 样式规则 `border: 1px solid rgba($color-primary, 0` */
  border: 1px solid rgba($color-primary, 0.2);

  /* [zh] 样式规则 `.rush-pay-title {` */
  .rush-pay-title {
    /* [zh] 样式规则 `margin: 0 0 6px;` */
    margin: 0 0 6px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;

    /* [zh] 样式规则 `strong {` */
    strong {
      /* [zh] 样式规则 `font-size: 18px;` */
      font-size: 18px;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `font-variant-numeric: tabular-nums;` */
      font-variant-numeric: tabular-nums;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rush-pay-tip {` */
  .rush-pay-tip {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `line-height: 1.5;` */
    line-height: 1.5;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rush-pay-expired {` */
  .rush-pay-expired {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-error;` */
    color: $color-error;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.checkout-skeleton {` */
.checkout-skeleton {
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.checkout-error {` */
.checkout-error {
  /* [zh] 样式规则 `margin: 16px;` */
  margin: 16px;
  /* [zh] 样式规则 `padding: 24px 16px;` */
  padding: 24px 16px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;

  /* [zh] 样式规则 `p {` */
  p {
    /* [zh] 样式规则 `margin: 0 0 12px;` */
    margin: 0 0 12px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: $color-text-secondary;` */
    color: $color-text-secondary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.block-error {` */
.block-error {
  /* [zh] 样式规则 `margin: 0 0 10px;` */
  margin: 0 0 10px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
  /* [zh] 样式规则 `color: $color-error;` */
  color: $color-error;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.inline-retry {` */
.inline-retry {
  /* [zh] 样式规则 `margin-left: 6px;` */
  margin-left: 6px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: none;` */
  background: none;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `text-decoration: underline;` */
  text-decoration: underline;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.block {` */
.block {
  /* [zh] 样式规则 `padding: 14px 12px;` */
  padding: 14px 12px;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.block-head {` */
.block-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.block-title {` */
.block-title {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.link-btn {` */
.link-btn {
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `background: none;` */
  background: none;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-primary;` */
  color: $color-primary;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.coupon-tip {` */
.coupon-tip {
  /* [zh] 样式规则 `margin: 8px 0 0;` */
  margin: 8px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-price;` */
  color: $color-price;

  /* [zh] 样式规则 `&.hint {` */
  &.hint {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.min-pay {` */
  &.min-pay {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `line-height: 1.5;` */
    line-height: 1.5;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.coupon-list {` */
.coupon-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.coupon-row {` */
.coupon-row {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.6);` */
    border-color: rgba($color-primary, 0.6);
    /* [zh] 样式规则 `background: $color-primary-soft;` */
    background: $color-primary-soft;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.disabled {` */
  &.disabled {
    /* [zh] 样式规则 `opacity: 0.55;` */
    opacity: 0.55;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.left {` */
  .left {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.name {` */
  .name {
    /* [zh] 样式规则 `margin: 0 0 4px;` */
    margin: 0 0 4px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.desc {` */
  .desc {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `line-height: 1.35;` */
    line-height: 1.35;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.right {` */
  .right {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `text-align: right;` */
    text-align: right;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.off {` */
  .off {
    /* [zh] 样式规则 `margin: 0 0 4px;` */
    margin: 0 0 4px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.tag {` */
  .tag {
    /* [zh] 样式规则 `font-size: 11px;` */
    font-size: 11px;
    /* [zh] 样式规则 `padding: 2px 8px;` */
    padding: 2px 8px;
    /* [zh] 样式规则 `border-radius: $radius-pill;` */
    border-radius: $radius-pill;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `background: rgba($color-primary, 0.1);` */
    background: rgba($color-primary, 0.1);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.tag.muted {` */
  .tag.muted {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.hint {` */
.hint {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.checkout-address-picker {` */
.checkout-address-picker {
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `background: #fafafa;` */
  background: #fafafa;

  /* [zh] 样式规则 `&.is-filled {` */
  &.is-filled {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: flex-start;` */
    align-items: flex-start;
    /* [zh] 样式规则 `gap: 10px;` */
    gap: 10px;
    /* [zh] 样式规则 `padding: 12px;` */
    padding: 12px;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
    -webkit-tap-highlight-color: transparent;

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `background: #fffaf7;` */
      background: #fffaf7;
      /* [zh] 样式规则 `border-color: rgba($color-primary, 0.35)` */
      border-color: rgba($color-primary, 0.35);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `:deep(.card-main) {` */
    :deep(.card-main) {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `min-width: 0;` */
      min-width: 0;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-empty {` */
  &.is-empty {
    /* [zh] 样式规则 `padding: 16px 12px;` */
    padding: 16px 12px;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;

    /* [zh] 样式规则 `.empty-tip {` */
    .empty-tip {
      /* [zh] 样式规则 `margin: 0 0 12px;` */
      margin: 0 0 12px;
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.empty-actions {` */
    .empty-actions {
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `justify-content: center;` */
      justify-content: center;
      /* [zh] 样式规则 `gap: 10px;` */
      gap: 10px;
      /* [zh] 样式规则 `flex-wrap: wrap;` */
      flex-wrap: wrap;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.picker-change {` */
  .picker-change {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `padding-top: 2px;` */
    padding-top: 2px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-list {` */
.goods-list {
  /* [zh] 样式规则 `list-style: none;` */
  list-style: none;
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `padding: 0;` */
  padding: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-item {` */
.goods-item {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `padding: 10px 0;` */
  padding: 10px 0;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border-l` */
  border-bottom: 1px solid $color-border-light;

  /* [zh] 样式规则 `&:last-child {` */
  &:last-child {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cover-wrap {` */
.cover-wrap {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
  /* [zh] 样式规则 `width: 72px;` */
  width: 72px;
  /* [zh] 样式规则 `height: 72px;` */
  height: 72px;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;

  /* [zh] 样式规则 `&.is-coupon {` */
  &.is-coupon {
    /* [zh] 样式规则 `display: grid;` */
    display: grid;
    /* [zh] 样式规则 `place-items: center;` */
    place-items: center;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-info {` */
.goods-info {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-name {` */
.goods-name {
  /* [zh] 样式规则 `margin: 0 0 4px;` */
  margin: 0 0 4px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 500;` */
  font-weight: 500;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
  /* [zh] 样式规则 `text-decoration: none;` */
  text-decoration: none;
  /* [zh] 样式规则 `display: -webkit-box;` */
  display: -webkit-box;
  /* [zh] 样式规则 `-webkit-line-clamp: 2;` */
  -webkit-line-clamp: 2;
  /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
  -webkit-box-orient: vertical;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-sku {` */
.goods-sku {
  /* [zh] 样式规则 `margin: 0 0 6px;` */
  margin: 0 0 6px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-foot {` */
.goods-foot {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;

  /* [zh] 样式规则 `.unit-price {` */
  .unit-price {
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.qty {` */
  .qty {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.subtotal {` */
  .subtotal {
    /* [zh] 样式规则 `margin-left: auto;` */
    margin-left: auto;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-methods {` */
.pay-methods {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pay-option {` */
.pay-option {
  /* [zh] 样式规则 `padding: 12px;` */
  padding: 12px;
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.45)` */
    border-color: rgba($color-primary, 0.45);
    /* [zh] 样式规则 `background: rgba($color-primary, 0.04);` */
    background: rgba($color-primary, 0.04);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.pay-desc {` */
  .pay-desc {
    /* [zh] 样式规则 `margin: 4px 0 0 24px;` */
    margin: 4px 0 0 24px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.checkout-bar {` */
.checkout-bar {
  /* [zh] 样式规则 `position: fixed;` */
  position: fixed;
  /* [zh] 样式规则 `left: 0;` */
  left: 0;
  /* [zh] 样式规则 `right: 0;` */
  right: 0;
  /* [zh] 样式规则 `bottom: 0;` */
  bottom: 0;
  /* [zh] 样式规则 `z-index: 100;` */
  z-index: 100;
  /* [zh] 样式规则 `border-top: 1px solid var(--glass-border` */
  border-top: 1px solid var(--glass-border-soft);
  /* [zh] 样式规则 `box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.` */
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);

  /* [zh] 样式规则 `:deep(.liquid-glass-surface__content) {` */
  :deep(.liquid-glass-surface__content) {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
    /* [zh] 样式规则 `padding: 10px 12px;` */
    padding: 10px 12px;
    /* [zh] 样式规则 `padding-bottom: calc(10px + env(safe-are` */
    padding-bottom: calc(10px + env(safe-area-inset-bottom, 0));
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.bar-summary {` */
  .bar-summary {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: baseline;` */
    align-items: baseline;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;

    /* [zh] 样式规则 `.label {` */
    .label {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.price-text {` */
    .price-text {
      /* [zh] 样式规则 `font-size: 20px;` */
      font-size: 20px;
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.count {` */
    .count {
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.btn-submit {` */
  .btn-submit {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `min-width: 120px;` */
    min-width: 120px;
    /* [zh] 样式规则 `height: 44px;` */
    height: 44px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
