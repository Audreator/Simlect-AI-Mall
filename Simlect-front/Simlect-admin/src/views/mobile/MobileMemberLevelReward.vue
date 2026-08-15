<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="glass-card m-form">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="m-form-title">会员升级礼券</h3>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-field">
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-label">银卡升级礼券</label>
        <!-- [zh] 开始标签 `<select>` -->
        <select v-model="form.level2CouponId" class="m-select">
          <!-- [zh] 开始标签 `<option>` -->
          <option value="">选填：银卡会员领取的优惠券</option>
          <!-- [zh] 开始标签 `<option>` -->
          <option v-for="c in couponOptions" :key="c.couponId" :value="c.couponId">
            <!-- [zh] Mustache 插值表达式 -->
            {{ couponLabel(c) }}
          <!-- [zh] 闭合标签 `</option>` -->
          </option>
        <!-- [zh] 闭合标签 `</select>` -->
        </select>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="silverCoupon" class="m-tip">
          <!-- [zh] 模板内容：`剩余 {{ silverCoupon.remainCount ?? 0 }} /` -->
          剩余 {{ silverCoupon.remainCount ?? 0 }} /
          <!-- [zh] Mustache 插值表达式 -->
          {{ silverCoupon.totalCount == 0 ? '不限' : silverCoupon.totalCount }}
          <!-- [zh] 模板内容：`· {{ silverCoupon.validStartTime || '—' }} ~ ` -->
          · {{ silverCoupon.validStartTime || '—' }} ~ {{ silverCoupon.validEndTime || '—' }}
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="m-tip">用户成长值达 1000（银卡）且首次领取升级礼时发放</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-field">
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-label">金卡升级礼券</label>
        <!-- [zh] 开始标签 `<select>` -->
        <select v-model="form.level3CouponId" class="m-select">
          <!-- [zh] 开始标签 `<option>` -->
          <option value="">选填：金卡会员领取的优惠券</option>
          <!-- [zh] 开始标签 `<option>` -->
          <option v-for="c in couponOptions" :key="'g-' + c.couponId" :value="c.couponId">
            <!-- [zh] Mustache 插值表达式 -->
            {{ couponLabel(c) }}
          <!-- [zh] 闭合标签 `</option>` -->
          </option>
        <!-- [zh] 闭合标签 `</select>` -->
        </select>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="goldCoupon" class="m-tip">
          <!-- [zh] 模板内容：`剩余 {{ goldCoupon.remainCount ?? 0 }} /` -->
          剩余 {{ goldCoupon.remainCount ?? 0 }} /
          <!-- [zh] Mustache 插值表达式 -->
          {{ goldCoupon.totalCount == 0 ? '不限' : goldCoupon.totalCount }}
          <!-- [zh] 模板内容：`· {{ goldCoupon.validStartTime || '—' }} ~ {{` -->
          · {{ goldCoupon.validStartTime || '—' }} ~ {{ goldCoupon.validEndTime || '—' }}
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="m-tip">用户成长值达 5000（金卡）且首次领取升级礼时发放</span>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-form-ops">
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn primary" :disabled="saving" @click="save">保存到 Redis</button>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn" @click="load">重新加载</button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<p>` -->
      <p class="m-note" style="margin-top: 12px">
        <!-- [zh] 模板内容：`配置保存在 Redis，保存后立即对用户「会员中心」领取升级礼生效。留空则仅发放成长值与站` -->
        配置保存在 Redis，保存后立即对用户「会员中心」领取升级礼生效。留空则仅发放成长值与站内通知，不发券。
      <!-- [zh] 闭合标签 `</p>` -->
      </p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `saving`
const saving = ref(false)
// [zh] 声明变量 `couponOptions`
const couponOptions = ref([])

// [zh] 声明变量 `form`
const form = reactive({
  // [zh] TypeScript/JS 语句：`level2CouponId: '',`
  level2CouponId: '',
  // [zh] TypeScript/JS 语句：`level3CouponId: '',`
  level3CouponId: '',
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `silverCoupon`
const silverCoupon = computed(() => couponOptions.value.find((c) => c.couponId === form.level2CouponId))
// [zh] 声明变量 `goldCoupon`
const goldCoupon = computed(() => couponOptions.value.find((c) => c.couponId === form.level3CouponId))

// [zh] 声明变量 `couponLabel`
const couponLabel = (c) => {
  // [zh] 声明变量 `type`
  const type = c.couponType == 1 ? '满减' : c.couponType == 2 ? '折扣' : c.couponType == 3 ? '无门槛' : '券'
  // [zh] return 返回
  return `${c.couponName}（${c.couponId}，${type}）`
// [zh] 语法符号
}

// [zh] 声明变量 `loadCoupons`
const loadCoupons = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadDiscountCoupon,`
    url: proxy.Api.loadDiscountCoupon,
    // [zh] TypeScript/JS 语句：`params: { pageNo: 1, pageSize: 200, status: 1 },`
    params: { pageNo: 1, pageSize: 200, status: 1 },
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`couponOptions.value = result.data?.list || []`
  couponOptions.value = result.data?.list || []
// [zh] 语法符号
}

// [zh] 声明变量 `load`
const load = async () => {
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.memberLevelRewardGetConfig,`
    url: proxy.Api.memberLevelRewardGetConfig,
    // [zh] TypeScript/JS 语句：`showLoading: true,`
    showLoading: true,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] 声明变量 `data`
  const data = result.data || {}
  // [zh] TypeScript/JS 语句：`form.level2CouponId = data.level2CouponId || ''`
  form.level2CouponId = data.level2CouponId || ''
  // [zh] TypeScript/JS 语句：`form.level3CouponId = data.level3CouponId || ''`
  form.level3CouponId = data.level3CouponId || ''
// [zh] 语法符号
}

// [zh] 声明变量 `save`
const save = async () => {
  // [zh] TypeScript/JS 语句：`saving.value = true`
  saving.value = true
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `result`
    const result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.memberLevelRewardSaveConfig,`
      url: proxy.Api.memberLevelRewardSaveConfig,
      // [zh] 代码块开始
      params: {
        // [zh] TypeScript/JS 语句：`level2CouponId: form.level2CouponId || '',`
        level2CouponId: form.level2CouponId || '',
        // [zh] TypeScript/JS 语句：`level3CouponId: form.level3CouponId || '',`
        level3CouponId: form.level3CouponId || '',
      // [zh] TypeScript/JS 语句：`},`
      },
      // [zh] TypeScript/JS 语句：`showLoading: true,`
      showLoading: true,
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 条件判断 if
    if (!result) return
    // [zh] TypeScript/JS 语句：`proxy.Message.success('已保存到 Redis')`
    proxy.Message.success('已保存到 Redis')
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await load()
  // [zh] 代码块开始
  } finally {
    // [zh] TypeScript/JS 语句：`saving.value = false`
    saving.value = false
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(async () => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await loadCoupons()
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await load()
// [zh] TypeScript/JS 语句：`})`
})
</script>
