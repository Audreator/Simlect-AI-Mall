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
        </p>
        <span class="m-tip">用户成长值达 5000（金卡）且首次领取升级礼时发放</span>
      </div>

      <div class="m-form-ops">
        <button type="button" class="op-btn primary" :disabled="saving" @click="save">保存到 Redis</button>
        <button type="button" class="op-btn" @click="load">重新加载</button>
      </div>

      <p class="m-note" style="margin-top: 12px">
        配置保存在 Redis，保存后立即对用户「会员中心」领取升级礼生效。留空则仅发放成长值与站内通知，不发券。
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'

const { proxy } = getCurrentInstance()
const saving = ref(false)
const couponOptions = ref([])

const form = reactive({
  level2CouponId: '',
  level3CouponId: '',
})

const silverCoupon = computed(() => couponOptions.value.find((c) => c.couponId === form.level2CouponId))
const goldCoupon = computed(() => couponOptions.value.find((c) => c.couponId === form.level3CouponId))

const couponLabel = (c) => {
  const type = c.couponType == 1 ? '满减' : c.couponType == 2 ? '折扣' : c.couponType == 3 ? '无门槛' : '券'
  return `${c.couponName}（${c.couponId}，${type}）`
}

const loadCoupons = async () => {
  const result = await proxy.Request({
    url: proxy.Api.loadDiscountCoupon,
    params: { pageNo: 1, pageSize: 200, status: 1 },
  })
  if (!result) return
  couponOptions.value = result.data?.list || []
}

const load = async () => {
  const result = await proxy.Request({
    url: proxy.Api.memberLevelRewardGetConfig,
    showLoading: true,
  })
  if (!result) return
  const data = result.data || {}
  form.level2CouponId = data.level2CouponId || ''
  form.level3CouponId = data.level3CouponId || ''
}

const save = async () => {
  saving.value = true
  try {
    const result = await proxy.Request({
      url: proxy.Api.memberLevelRewardSaveConfig,
      params: {
        level2CouponId: form.level2CouponId || '',
        level3CouponId: form.level3CouponId || '',
      },
      showLoading: true,
    })
    if (!result) return
    proxy.Message.success('已保存到 Redis')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadCoupons()
  await load()
})
</script>
