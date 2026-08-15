<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="pc-checkout ignore">
    <!-- [zh] 开始标签 `<nav>` -->
    <nav class="pc-checkout-steps" aria-label="结账流程">
      <!-- [zh] 开始标签 `<RouterLink>` -->
      <RouterLink to="/cart" class="step-link">购物车</RouterLink>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="step-sep" aria-hidden="true" />
      <!-- [zh] 开始标签 `<span>` -->
      <span class="step-current">确认订单</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="step-sep" aria-hidden="true" />
      <!-- [zh] 开始标签 `<span>` -->
      <span class="step-muted">支付</span>
    <!-- [zh] 闭合标签 `</nav>` -->
    </nav>

    <!-- [zh] 开始标签 `<el-skeleton>` -->
    <el-skeleton v-if="pageLoading" animated :rows="10" class="pc-checkout-skeleton" />

    <!-- [zh] 开始标签 `<section>` -->
    <section v-else-if="initError" class="pc-checkout-state">
      <!-- [zh] 开始标签 `<p>` -->
      <p>{{ initError }}</p>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" @click="init">重试</el-button>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template v-else-if="items.length">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="isCouponRush" class="pc-rush-banner">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-if="payCountdownMs > 0">
          <!-- [zh] 开始标签 `<p>` -->
          <p class="rush-title">
            <!-- [zh] 模板内容：`支付剩余 <strong>{{ payCountdownText }}</strong>` -->
            支付剩余 <strong>{{ payCountdownText }}</strong>
          <!-- [zh] 闭合标签 `</p>` -->
          </p>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="rush-tip">订单已生成，超时将自动关闭。也可在「我的订单 → 待付款」中继续支付。</p>
        </template>
        <p v-else class="rush-expired">支付已超时，订单已关闭，请返回重新抢购。</p>
      </div>

      <div class="pc-checkout-layout">
        <div class="pc-checkout-main">
          <section v-if="!isCouponRush" class="pc-panel">
            <header class="panel-head">
              <h2>收货地址</h2>
              <button type="button" class="text-link" @click="openAddressForm()">新增地址</button>
            </header>
            <p v-if="addressLoadError" class="panel-error">
              {{ addressLoadError }}
              <button type="button" class="inline-retry" @click="loadAddresses">重试</button>
            </p>
            <div
              v-if="selectedAddress"
              class="pc-address-selected"
            >
              <AddressCardBody :item="selectedAddress" />
              <button type="button" class="text-link" @click="addressListOpen = !addressListOpen">
                {{ addressListOpen ? '收起' : '更换' }}
              </button>
            </div>
            <div v-else-if="addresses.length" class="pc-address-empty">
              <p>请选择收货地址</p>
              <div class="pc-address-actions">
                <el-button type="primary" @click="addressListOpen = true">选择地址</el-button>
                <el-button @click="openAddressForm()">新增地址</el-button>
              </div>
            </div>
            <div v-else class="pc-address-empty">
              <p>暂无收货地址，请先新增</p>
              <el-button type="primary" @click="openAddressForm()">新增地址</el-button>
            </div>
            <div v-if="addressListOpen && addresses.length" class="pc-address-list">
              <div
                v-for="addr in addresses"
                :key="addr.addressId"
                class="pc-address-card"
                :class="{ 'is-selected': addressId === addr.addressId }"
                @click="pickAddress(addr)"
              >
                <span class="addr-radio" :class="{ on: addressId === addr.addressId }" aria-hidden="true" />
                <AddressCardBody :item="addr" />
                <div class="addr-actions" @click.stop>
                  <el-button link type="primary" @click="openAddressForm(addr)">编辑</el-button>
                  <el-button link type="danger" @click="removeAddress(addr.addressId)">删除</el-button>
                </div>
              </div>
            </div>
          </section>

          <section class="pc-panel">
            <header class="panel-head">
              <h2>{{ isCouponRush ? '优惠券信息' : '商品清单' }}</h2>
              <span class="panel-meta">共 {{ totalCount }} 件</span>
            </header>
            <table class="pc-goods-table">
              <thead>
                <tr>
                  <th class="col-goods">商品</th>
                  <th class="col-price">单价</th>
                  <th class="col-qty">数量</th>
                  <th class="col-sub">小计</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(item, index) in items"
                  :key="`${item.productId}-${item.propertyValueIds}-${index}`"
                >
                  <td class="col-goods">
                    <div class="goods-cell">
                      <RouterLink v-if="!isCouponRush" :to="`/product/${item.productId}`" class="goods-cover">
                        <ProductImage :source="item.productCover" width="72" height="72" fit="cover" />
                      </RouterLink>
                      <div v-else class="goods-cover is-coupon">
                        <el-icon :size="28"><Ticket /></el-icon>
                      </div>
                      <div class="goods-meta">
                        <RouterLink v-if="!isCouponRush" :to="`/product/${item.productId}`" class="goods-name">
                          {{ item.productName }}
                        </RouterLink>
                        <p v-else class="goods-name">{{ item.productName }}</p>
                        <p v-if="formatSkuText(item)" class="goods-sku">{{ formatSkuText(item) }}</p>
                      </div>
                    </div>
                  </td>
                  <td class="col-price">¥{{ Number(item.price).toFixed(2) }}</td>
                  <td class="col-qty">×{{ item.buyCount }}</td>
                  <td class="col-sub">¥{{ lineSubtotal(item).toFixed(2) }}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section v-if="!isCouponRush" class="pc-panel">
            <h2 class="panel-title">订单备注</h2>
            <el-input
              v-model="remark"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="选填：配送、包装等要求"
            />
          </section>

          <section v-if="!isCouponRush" class="pc-panel">
            <header class="panel-head">
              <h2>优惠券</h2>
              <button type="button" class="text-link" @click="openCouponPicker">
                {{ selectedCouponLabel }}
              </button>
            </header>
            <p v-if="couponDiscount > 0" class="panel-tip discount">已抵扣 ¥{{ couponDiscount.toFixed(2) }}</p>
            <p v-if="showMinPayTip" class="panel-tip">使用优惠券后最低需支付 ¥{{ minPayAmountText }}</p>
            <p
              v-else-if="usableCoupons.some((c) => c.usable) && maxAvailableDiscount > 0"
              class="panel-tip highlight"
            >
              您有可用优惠券，最高可抵扣 ¥{{ maxAvailableDiscount.toFixed(2) }}
            </p>
            <p v-if="couponLoadError" class="panel-error">
              {{ couponLoadError }}
              <button type="button" class="inline-retry" @click="loadCoupons">重试</button>
            </p>
          </section>

          <section class="pc-panel">
            <h2 class="panel-title">支付方式</h2>
            <el-radio-group v-model="payMethod" class="pc-pay-methods">
              <label class="pc-pay-option" :class="{ active: payMethod === PAY_METHOD_ALIPAY_PC }">
                <el-radio :label="PAY_METHOD_ALIPAY_PC">支付宝</el-radio>
                <span class="pay-desc">提交后在新窗口打开支付宝扫码支付</span>
              </label>
            </el-radio-group>
          </section>
        </div>

        <aside class="pc-checkout-aside">
          <div class="aside-card">
            <h2>付款详情</h2>
            <div class="amount-row">
              <span>商品件数</span>
              <span>{{ totalCount }} 件</span>
            </div>
            <div class="amount-row">
              <span>商品总价</span>
              <span>¥{{ goodsAmount }}</span>
            </div>
            <div v-if="couponDiscount > 0" class="amount-row">
              <span>优惠券</span>
              <span class="discount">-¥{{ couponDiscount.toFixed(2) }}</span>
            </div>
            <p v-if="showMinPayTip" class="min-pay-tip">已按规则保留最低实付 ¥{{ minPayAmountText }}</p>
            <div class="amount-row total">
              <span>应付总额</span>
              <strong class="price-text">¥{{ payableAmount }}</strong>
            </div>
            <el-button
              type="primary"
              class="btn-submit"
              size="large"
              :loading="submitting"
              :disabled="isCouponRush && payCountdownMs <= 0"
              @click="submit"
            >
              {{ submitButtonText }}
            </el-button>
            <RouterLink to="/cart" class="back-cart">返回购物车</RouterLink>
          </div>
        </aside>
      </div>
    </template>

    <section v-else class="pc-checkout-state">
      <el-empty description="没有待结算的商品">
        <el-button type="primary" @click="router.push('/cart')">返回购物车</el-button>
      </el-empty>
    </section>

    <el-dialog v-model="couponVisible" title="选择优惠券" width="560px">
      <div class="pc-coupon-list">
        <button
          type="button"
          class="pc-coupon-row"
          :class="{ active: !selectedUserCouponId }"
          @click="selectCoupon(null)"
        >
          <div>
            <p class="name">不使用优惠券</p>
            <p class="desc">本单不抵扣</p>
          </div>
        </button>
        <el-skeleton v-if="couponLoading" animated :rows="4" />
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-else>
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-for="c in usableCoupons"
            :key="c.userCouponId"
            type="button"
            class="pc-coupon-row"
            :class="{ active: selectedUserCouponId === c.userCouponId, disabled: !c.usable }"
            :disabled="!c.usable"
            @click="selectCoupon(c)"
          >
            <!-- [zh] 开始标签 `<div>` -->
            <div>
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
            <!-- [zh] 开始标签 `<p>` -->
            <p class="off">-¥{{ calcCouponDiscount(c).toFixed(2) }}</p>
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

    <AddressFormPanel
      v-model="addressFormVisible"
      :edit-item="editingAddress"
      @saved="onAddressFormSaved"
    />
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `@element-plus/icons-vue`
import { Ticket } from '@element-plus/icons-vue';
// [zh] 导入 `vue-router`
import { RouterLink, useRouter } from 'vue-router';
// [zh] 导入 `@/components/business/AddressCardBody.vue`
import AddressCardBody from '@/components/business/AddressCardBody.vue';
// [zh] 导入 `@/components/business/AddressFormPanel.vue`
import AddressFormPanel from '@/components/business/AddressFormPanel.vue';
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
  // [zh] TypeScript/JS 语句：`addresses,`
  addresses,
  // [zh] TypeScript/JS 语句：`addressId,`
  addressId,
  // [zh] TypeScript/JS 语句：`addressFormVisible,`
  addressFormVisible,
  // [zh] TypeScript/JS 语句：`editingAddress,`
  editingAddress,
  // [zh] TypeScript/JS 语句：`addressListOpen,`
  addressListOpen,
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
  // [zh] TypeScript/JS 语句：`goodsAmount,`
  goodsAmount,
  // [zh] TypeScript/JS 语句：`selectedUserCouponId,`
  selectedUserCouponId,
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
  // [zh] TypeScript/JS 语句：`pickAddress,`
  pickAddress,
  // [zh] TypeScript/JS 语句：`openAddressForm,`
  openAddressForm,
  // [zh] TypeScript/JS 语句：`onAddressFormSaved,`
  onAddressFormSaved,
  // [zh] TypeScript/JS 语句：`removeAddress,`
  removeAddress,
  // [zh] TypeScript/JS 语句：`loadCoupons,`
  loadCoupons,
  // [zh] TypeScript/JS 语句：`openCouponPicker,`
  openCouponPicker,
  // [zh] TypeScript/JS 语句：`selectCoupon,`
  selectCoupon,
  // [zh] TypeScript/JS 语句：`submit,`
  submit,
  // [zh] TypeScript/JS 语句：`PAY_METHOD_ALIPAY_PC`
  PAY_METHOD_ALIPAY_PC
// [zh] 语句
} = useCheckoutPage('desktop');
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.pc-checkout {` */
.pc-checkout {
  /* [zh] 样式规则 `max-width: 1180px;` */
  max-width: 1180px;
  /* [zh] 样式规则 `margin: 0 auto;` */
  margin: 0 auto;
  /* [zh] 样式规则 `padding: 8px 0 40px;` */
  padding: 8px 0 40px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-checkout-steps {` */
.pc-checkout-steps {
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

  /* [zh] 样式规则 `.step-muted {` */
  .step-muted {
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
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

/* [zh] 样式规则 `.pc-checkout-skeleton {` */
.pc-checkout-skeleton {
  /* [zh] 样式规则 `padding: 24px;` */
  padding: 24px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-checkout-state {` */
.pc-checkout-state {
  /* [zh] 样式规则 `padding: 48px 24px;` */
  padding: 48px 24px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;

  /* [zh] 样式规则 `p {` */
  p {
    /* [zh] 样式规则 `margin: 0 0 16px;` */
    margin: 0 0 16px;
    /* [zh] 样式规则 `color: $color-text-secondary;` */
    color: $color-text-secondary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-rush-banner {` */
.pc-rush-banner {
  /* [zh] 样式规则 `margin-bottom: 16px;` */
  margin-bottom: 16px;
  /* [zh] 样式规则 `padding: 16px 20px;` */
  padding: 16px 20px;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `background: linear-gradient(135deg, rgba` */
  background: linear-gradient(135deg, rgba($color-primary, 0.1), rgba($color-primary, 0.03));
  /* [zh] 样式规则 `border: 1px solid rgba($color-primary, 0` */
  border: 1px solid rgba($color-primary, 0.18);

  /* [zh] 样式规则 `.rush-title {` */
  .rush-title {
    /* [zh] 样式规则 `margin: 0 0 6px;` */
    margin: 0 0 6px;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;

    /* [zh] 样式规则 `strong {` */
    strong {
      /* [zh] 样式规则 `font-size: 20px;` */
      font-size: 20px;
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `font-variant-numeric: tabular-nums;` */
      font-variant-numeric: tabular-nums;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rush-tip {` */
  .rush-tip {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.rush-expired {` */
  .rush-expired {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `color: $color-error;` */
    color: $color-error;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-checkout-layout {` */
.pc-checkout-layout {
  /* [zh] 样式规则 `display: grid;` */
  display: grid;
  /* [zh] 样式规则 `grid-template-columns: minmax(0, 1fr) 32` */
  grid-template-columns: minmax(0, 1fr) 320px;
  /* [zh] 样式规则 `gap: 20px;` */
  gap: 20px;
  /* [zh] 样式规则 `align-items: start;` */
  align-items: start;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-panel {` */
.pc-panel {
  /* [zh] 样式规则 `margin-bottom: 16px;` */
  margin-bottom: 16px;
  /* [zh] 样式规则 `padding: 20px 24px;` */
  padding: 20px 24px;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `box-shadow: $shadow-card;` */
  box-shadow: $shadow-card;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.panel-head {` */
.panel-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `margin-bottom: 16px;` */
  margin-bottom: 16px;

  /* [zh] 样式规则 `h2 {` */
  h2 {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.panel-title {` */
.panel-title {
  /* [zh] 样式规则 `margin: 0 0 14px;` */
  margin: 0 0 14px;
  /* [zh] 样式规则 `font-size: 16px;` */
  font-size: 16px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: $color-text-title;` */
  color: $color-text-title;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.panel-meta {` */
.panel-meta {
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
.panel-error,
/* [zh] 样式规则 `.panel-tip {` */
.panel-tip {
  /* [zh] 样式规则 `margin: 0 0 12px;` */
  margin: 0 0 12px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.panel-error {` */
.panel-error {
  /* [zh] 样式规则 `color: $color-error;` */
  color: $color-error;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.panel-tip {` */
.panel-tip {
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;

  /* [zh] 样式声明 */
  &.discount,
  /* [zh] 样式规则 `&.highlight {` */
  &.highlight {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.text-link {` */
.text-link {
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

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `text-decoration: underline;` */
    text-decoration: underline;
  /* [zh] 样式规则 `}` */
  }
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

/* [zh] 样式规则 `.pc-address-selected {` */
.pc-address-selected {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
  /* [zh] 样式规则 `padding: 14px 16px;` */
  padding: 14px 16px;
  /* [zh] 样式规则 `border: 1px solid rgba($color-primary, 0` */
  border: 1px solid rgba($color-primary, 0.25);
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `background: rgba($color-primary, 0.04);` */
  background: rgba($color-primary, 0.04);

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

/* [zh] 样式规则 `.pc-address-empty {` */
.pc-address-empty {
  /* [zh] 样式规则 `padding: 20px;` */
  padding: 20px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `border: 1px dashed $color-border;` */
  border: 1px dashed $color-border;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;

  /* [zh] 样式规则 `p {` */
  p {
    /* [zh] 样式规则 `margin: 0 0 12px;` */
    margin: 0 0 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-address-actions {` */
.pc-address-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-address-list {` */
.pc-address-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `margin-top: 12px;` */
  margin-top: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-address-card {` */
.pc-address-card {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `padding: 14px 16px;` */
  padding: 14px 16px;
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: border-color $transition-fas` */
  transition: border-color $transition-fast, background $transition-fast;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.35)` */
    border-color: rgba($color-primary, 0.35);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-selected {` */
  &.is-selected {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.55)` */
    border-color: rgba($color-primary, 0.55);
    /* [zh] 样式规则 `background: rgba($color-primary, 0.04);` */
    background: rgba($color-primary, 0.04);
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

/* [zh] 样式规则 `.addr-radio {` */
.addr-radio {
  /* [zh] 样式规则 `width: 16px;` */
  width: 16px;
  /* [zh] 样式规则 `height: 16px;` */
  height: 16px;
  /* [zh] 样式规则 `margin-top: 4px;` */
  margin-top: 4px;
  /* [zh] 样式规则 `border: 2px solid $color-border;` */
  border: 2px solid $color-border;
  /* [zh] 样式规则 `border-radius: 50%;` */
  border-radius: 50%;
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;

  /* [zh] 样式规则 `&.on {` */
  &.on {
    /* [zh] 样式规则 `border-color: $color-primary;` */
    border-color: $color-primary;
    /* [zh] 样式规则 `box-shadow: inset 0 0 0 3px $color-card,` */
    box-shadow: inset 0 0 0 3px $color-card, inset 0 0 0 8px $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.addr-actions {` */
.addr-actions {
  /* [zh] 样式规则 `flex-shrink: 0;` */
  flex-shrink: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-goods-table {` */
.pc-goods-table {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `border-collapse: collapse;` */
  border-collapse: collapse;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;

  /* [zh] 样式规则 `th {` */
  th {
    /* [zh] 样式规则 `padding: 10px 12px;` */
    padding: 10px 12px;
    /* [zh] 样式规则 `text-align: left;` */
    text-align: left;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
    border-bottom: 1px solid $color-border;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `td {` */
  td {
    /* [zh] 样式规则 `padding: 14px 12px;` */
    padding: 14px 12px;
    /* [zh] 样式规则 `vertical-align: middle;` */
    vertical-align: middle;
    /* [zh] 样式规则 `border-bottom: 1px solid $color-border-l` */
    border-bottom: 1px solid $color-border-light;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `tr:last-child td {` */
  tr:last-child td {
    /* [zh] 样式规则 `border-bottom: none;` */
    border-bottom: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  .col-price,
  /* [zh] 样式声明 */
  .col-qty,
  /* [zh] 样式规则 `.col-sub {` */
  .col-sub {
    /* [zh] 样式规则 `width: 100px;` */
    width: 100px;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.col-sub {` */
  .col-sub {
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-cell {` */
.goods-cell {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 14px;` */
  gap: 14px;
  /* [zh] 样式规则 `min-width: 0;` */
  min-width: 0;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-cover {` */
.goods-cover {
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

/* [zh] 样式规则 `.goods-meta {` */
.goods-meta {
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

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.goods-sku {` */
.goods-sku {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-pay-methods {` */
.pc-pay-methods {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-pay-option {` */
.pc-pay-option {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 4px;` */
  gap: 4px;
  /* [zh] 样式规则 `padding: 14px 16px;` */
  padding: 14px 16px;
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.5);` */
    border-color: rgba($color-primary, 0.5);
    /* [zh] 样式规则 `background: rgba($color-primary, 0.04);` */
    background: rgba($color-primary, 0.04);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.pay-desc {` */
  .pay-desc {
    /* [zh] 样式规则 `margin-left: 24px;` */
    margin-left: 24px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-checkout-aside {` */
.pc-checkout-aside {
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
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.amount-row {` */
.amount-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `padding: 8px 0;` */
  padding: 8px 0;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;

  /* [zh] 样式规则 `.discount {` */
  .discount {
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.total {` */
  &.total {
    /* [zh] 样式规则 `margin-top: 8px;` */
    margin-top: 8px;
    /* [zh] 样式规则 `padding-top: 14px;` */
    padding-top: 14px;
    /* [zh] 样式规则 `border-top: 1px dashed $color-border;` */
    border-top: 1px dashed $color-border;
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;

    /* [zh] 样式规则 `.price-text {` */
    .price-text {
      /* [zh] 样式规则 `font-size: 24px;` */
      font-size: 24px;
      /* [zh] 样式规则 `color: $color-price;` */
      color: $color-price;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.min-pay-tip {` */
.min-pay-tip {
  /* [zh] 样式规则 `margin: 0 0 8px;` */
  margin: 0 0 8px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.btn-submit {` */
.btn-submit {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `margin-top: 16px;` */
  margin-top: 16px;
  /* [zh] 样式规则 `height: 44px;` */
  height: 44px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.back-cart {` */
.back-cart {
  /* [zh] 样式规则 `display: block;` */
  display: block;
  /* [zh] 样式规则 `margin-top: 12px;` */
  margin-top: 12px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
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

/* [zh] 样式规则 `.pc-coupon-list {` */
.pc-coupon-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `max-height: 420px;` */
  max-height: 420px;
  /* [zh] 样式规则 `overflow-y: auto;` */
  overflow-y: auto;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pc-coupon-row {` */
.pc-coupon-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `border-radius: $radius-card;` */
  border-radius: $radius-card;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `text-align: left;` */
  text-align: left;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.55)` */
    border-color: rgba($color-primary, 0.55);
    /* [zh] 样式规则 `background: rgba($color-primary, 0.05);` */
    background: rgba($color-primary, 0.05);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.disabled {` */
  &.disabled {
    /* [zh] 样式规则 `opacity: 0.55;` */
    opacity: 0.55;
    /* [zh] 样式规则 `cursor: not-allowed;` */
    cursor: not-allowed;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.name {` */
  .name {
    /* [zh] 样式规则 `margin: 0 0 4px;` */
    margin: 0 0 4px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
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
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.off {` */
  .off {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `font-weight: 700;` */
    font-weight: 700;
    /* [zh] 样式规则 `color: $color-price;` */
    color: $color-price;
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
