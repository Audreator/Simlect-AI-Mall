<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="glass-card m-form">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="m-form-title">连续签到发券</h3>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="m-field m-form-row">
        <!-- [zh] 开始标签 `<label>` -->
        <label class="m-label" style="margin: 0">开启</label>
        <!-- [zh] 开始标签 `<el-switch>` -->
        <el-switch v-model="form.enabled" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-if="form.enabled">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="m-field">
          <!-- [zh] 开始标签 `<label>` -->
          <label class="m-label">连续签到天数</label>
          <!-- [zh] 开始标签 `<input>` -->
          <input v-model.number="form.streakDays" class="m-input" type="number" min="1" max="30" />
          <!-- [zh] 开始标签 `<span>` -->
          <span class="m-tip">满 N 天且为 N 的整数倍时发放（如 7 表示第 7、14、21 天…）</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="m-field">
          <!-- [zh] 开始标签 `<label>` -->
          <label class="m-label">奖励优惠券</label>
          <!-- [zh] 开始标签 `<select>` -->
          <select v-model="form.couponId" class="m-select">
            <!-- [zh] 开始标签 `<option>` -->
            <option value="">请选择优惠券</option>
            <!-- [zh] 开始标签 `<option>` -->
            <option v-for="c in couponOptions" :key="c.couponId" :value="c.couponId">
              <!-- [zh] Mustache 插值表达式 -->
              {{ couponLabel(c) }}
            <!-- [zh] 闭合标签 `</option>` -->
            </option>
          <!-- [zh] 闭合标签 `</select>` -->
          </select>
          <!-- [zh] 开始标签 `<p>` -->
          <p v-if="selectedCoupon" class="m-tip">
            <!-- [zh] 模板内容：`剩余 {{ selectedCoupon.remainCount ?? 0 }} /` -->
            剩余 {{ selectedCoupon.remainCount ?? 0 }} /
            <!-- [zh] Mustache 插值表达式 -->
            {{ selectedCoupon.totalCount == 0 ? '不限' : selectedCoupon.totalCount }}
            <!-- [zh] 模板内容：`· {{ selectedCoupon.validStartTime || '—' }} ` -->
            · {{ selectedCoupon.validStartTime || '—' }} ~ {{ selectedCoupon.validEndTime || '—' }}
          <!-- [zh] 闭合标签 `</p>` -->
          </p>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      </template>

      <div class="m-form-ops">
        <button type="button" class="op-btn primary" :disabled="saving" @click="save">保存到 Redis</button>
        <button type="button" class="op-btn" @click="load">重新加载</button>
      </div>

      <p class="m-note" style="margin-top: 12px">
        奖励配置保存在 Redis；签到数据以 MySQL 位图为唯一权威，Redis 日历缓存会在读取时自动回源。
        请确保所选优惠券库存充足且处于进行中状态。
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
  enabled: false,
  couponId: '',
  streakDays: 7,
})

const selectedCoupon = computed(() => couponOptions.value.find((c) => c.couponId === form.couponId))

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
    url: proxy.Api.signRewardGetConfig,
    showLoading: true,
  })
  if (!result) return
  const data = result.data || {}
  form.enabled = !!data.enabled
  form.couponId = data.couponId || ''
  form.streakDays = data.streakDays ?? 7
}

const save = async () => {
  if (form.enabled && !form.couponId) {
    proxy.Message.warning('请选择优惠券')
    return
  }
  if (form.enabled && (!form.streakDays || form.streakDays < 1)) {
    proxy.Message.warning('请设置连续天数')
    return
  }
  saving.value = true
  try {
    const result = await proxy.Request({
      url: proxy.Api.signRewardSaveConfig,
      params: {
        enabled: form.enabled,
        couponId: form.enabled ? form.couponId : '',
        streakDays: form.streakDays,
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
