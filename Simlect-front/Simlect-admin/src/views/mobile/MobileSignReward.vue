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
        配置保存在 Redis，保存后立即对用户签到生效。请确保所选优惠券库存充足且处于进行中状态。
      </p>
    </div>

    <div class="glass-card m-form" style="margin-top: 12px" @click="onSyncCardClick">
      <h3 class="m-form-title">签到数据恢复</h3>
      <p class="m-tip">汇总表 → Redis Hash（连续/总天数/补签次数）</p>
      <div class="m-form-ops">
        <button type="button" class="op-btn warn" :disabled="syncingHash" @click.stop="syncSignHashFromDb">
          同步汇总
        </button>
      </div>

      <p class="m-tip" style="margin-top: 12px">
        明细表 → Redis 日历（最近365天）。手动同步禁止包含今天，默认截止昨天。
      </p>
      <div class="m-field">
        <label class="m-label">同步截止日期</label>
        <input v-model="syncEndDateInput" class="m-input" type="date" :max="yesterdayInputValue" />
      </div>
      <div class="m-field">
        <label class="m-label">用户ID（可选）</label>
        <input v-model="syncUserId" class="m-input" type="text" placeholder="留空=全部用户" />
      </div>
      <div class="m-form-ops">
        <button type="button" class="op-btn primary" :disabled="syncingDates" @click.stop="syncSignDatesFromDb">
          同步历史签到日期
        </button>
      </div>
      <p v-if="showForceRebuild" class="m-tip">
        <button type="button" class="op-btn danger" :disabled="forcingToday" @click.stop="forceRebuildToday">
          强制重建含今日
        </button>
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'

const { proxy } = getCurrentInstance()
const saving = ref(false)
const syncingHash = ref(false)
const syncingDates = ref(false)
const forcingToday = ref(false)
const syncUserId = ref('')
const showForceRebuild = ref(false)
const forceClickCount = ref(0)
const couponOptions = ref([])

const yesterdayYmd = () => {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}${m}${day}`
}

const yesterdayInputValue = computed(() => {
  const ymd = yesterdayYmd()
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
})

const syncEndDateInput = ref(yesterdayInputValue.value)

const syncEndDateYmd = () => syncEndDateInput.value.replace(/-/g, '')

const onSyncCardClick = () => {
  forceClickCount.value += 1
  if (forceClickCount.value >= 5) showForceRebuild.value = true
}

const syncSignHashFromDb = () => {
  proxy.Confirm({
    message: '将 DB 签到汇总覆盖同步到 Redis Hash，确定？',
    okfun: async () => {
      syncingHash.value = true
      try {
        const result = await proxy.Request({
          url: proxy.Api.signRecordSyncAllFromDb,
          params: { force: true },
          showLoading: true,
        })
        if (!result) return
        const d = result.data || {}
        proxy.Message.success(`汇总同步 ${d.synced ?? 0} 条，跳过 ${d.skipped ?? 0} 条`)
      } finally {
        syncingHash.value = false
      }
    },
  })
}

const syncSignDatesFromDb = () => {
  const endDate = syncEndDateYmd()
  if (!endDate || endDate.length !== 8) {
    proxy.Message.warning('请选择同步截止日期')
    return
  }
  proxy.Confirm({
    message: `同步截至 ${endDate} 的签到日期到 Redis（不含今天），确定？`,
    okfun: async () => {
      syncingDates.value = true
      try {
        const params = { syncEndDate: endDate }
        if (syncUserId.value) params.userId = syncUserId.value.trim()
        const result = await proxy.Request({
          url: proxy.Api.signRecordSyncSignDatesFromDb,
          params,
          showLoading: true,
        })
        if (!result) return
        const d = result.data || {}
        proxy.Message.success(`日期同步 ${d.syncedUsers ?? 0} 用户，${d.syncedDates ?? 0} 条`)
      } finally {
        syncingDates.value = false
      }
    },
  })
}

const forceRebuildToday = () => {
  proxy.Confirm({
    message: '强制重建含今日的签到日历，确定？',
    okfun: async () => {
      forcingToday.value = true
      try {
        const params = {}
        if (syncUserId.value) params.userId = syncUserId.value.trim()
        const result = await proxy.Request({
          url: proxy.Api.signRecordForceRebuildToday,
          params,
          showLoading: true,
        })
        if (!result) return
        const d = result.data || {}
        proxy.Message.success(`强制重建 ${d.syncedUsers ?? 0} 用户，${d.syncedDates ?? 0} 条`)
      } finally {
        forcingToday.value = false
      }
    },
  })
}

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
