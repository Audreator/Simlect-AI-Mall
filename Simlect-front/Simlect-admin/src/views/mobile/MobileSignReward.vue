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

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `saving`
const saving = ref(false)
// [zh] 声明变量 `syncingHash`
const syncingHash = ref(false)
// [zh] 声明变量 `syncingDates`
const syncingDates = ref(false)
// [zh] 声明变量 `forcingToday`
const forcingToday = ref(false)
// [zh] 声明变量 `syncUserId`
const syncUserId = ref('')
// [zh] 声明变量 `showForceRebuild`
const showForceRebuild = ref(false)
// [zh] 声明变量 `forceClickCount`
const forceClickCount = ref(0)
// [zh] 声明变量 `couponOptions`
const couponOptions = ref([])

// [zh] 声明变量 `yesterdayYmd`
const yesterdayYmd = () => {
  // [zh] 声明变量 `d`
  const d = new Date()
  // [zh] TypeScript/JS 语句：`d.setDate(d.getDate() - 1)`
  d.setDate(d.getDate() - 1)
  // [zh] 声明变量 `y`
  const y = d.getFullYear()
  // [zh] 声明变量 `m`
  const m = String(d.getMonth() + 1).padStart(2, '0')
  // [zh] 声明变量 `day`
  const day = String(d.getDate()).padStart(2, '0')
  // [zh] return 返回
  return `${y}${m}${day}`
// [zh] 语法符号
}

// [zh] 声明变量 `yesterdayInputValue`
const yesterdayInputValue = computed(() => {
  // [zh] 声明变量 `ymd`
  const ymd = yesterdayYmd()
  // [zh] return 返回
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `syncEndDateInput`
const syncEndDateInput = ref(yesterdayInputValue.value)

// [zh] 声明变量 `syncEndDateYmd`
const syncEndDateYmd = () => syncEndDateInput.value.replace(/-/g, '')

// [zh] 声明变量 `onSyncCardClick`
const onSyncCardClick = () => {
  // [zh] TypeScript/JS 语句：`forceClickCount.value += 1`
  forceClickCount.value += 1
  // [zh] 条件判断 if
  if (forceClickCount.value >= 5) showForceRebuild.value = true
// [zh] 语法符号
}

// [zh] 声明变量 `syncSignHashFromDb`
const syncSignHashFromDb = () => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: '将 DB 签到汇总覆盖同步到 Redis Hash，确定？',`
    message: '将 DB 签到汇总覆盖同步到 Redis Hash，确定？',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] TypeScript/JS 语句：`syncingHash.value = true`
      syncingHash.value = true
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `result`
        const result = await proxy.Request({
          // [zh] TypeScript/JS 语句：`url: proxy.Api.signRecordSyncAllFromDb,`
          url: proxy.Api.signRecordSyncAllFromDb,
          // [zh] TypeScript/JS 语句：`params: { force: true },`
          params: { force: true },
          // [zh] TypeScript/JS 语句：`showLoading: true,`
          showLoading: true,
        // [zh] TypeScript/JS 语句：`})`
        })
        // [zh] 条件判断 if
        if (!result) return
        // [zh] 声明变量 `d`
        const d = result.data || {}
        // [zh] TypeScript/JS 语句：`proxy.Message.success(`汇总同步 ${d.synced ?? 0} 条，跳过 ${d.s`
        proxy.Message.success(`汇总同步 ${d.synced ?? 0} 条，跳过 ${d.skipped ?? 0} 条`)
      // [zh] 代码块开始
      } finally {
        // [zh] TypeScript/JS 语句：`syncingHash.value = false`
        syncingHash.value = false
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `syncSignDatesFromDb`
const syncSignDatesFromDb = () => {
  // [zh] 声明变量 `endDate`
  const endDate = syncEndDateYmd()
  // [zh] 条件判断 if
  if (!endDate || endDate.length !== 8) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请选择同步截止日期')`
    proxy.Message.warning('请选择同步截止日期')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `同步截至 ${endDate} 的签到日期到 Redis（不含今天），确定？`,`
    message: `同步截至 ${endDate} 的签到日期到 Redis（不含今天），确定？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] TypeScript/JS 语句：`syncingDates.value = true`
      syncingDates.value = true
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `params`
        const params = { syncEndDate: endDate }
        // [zh] 条件判断 if
        if (syncUserId.value) params.userId = syncUserId.value.trim()
        // [zh] 声明变量 `result`
        const result = await proxy.Request({
          // [zh] TypeScript/JS 语句：`url: proxy.Api.signRecordSyncSignDatesFromDb,`
          url: proxy.Api.signRecordSyncSignDatesFromDb,
          // [zh] TypeScript/JS 语句：`params,`
          params,
          // [zh] TypeScript/JS 语句：`showLoading: true,`
          showLoading: true,
        // [zh] TypeScript/JS 语句：`})`
        })
        // [zh] 条件判断 if
        if (!result) return
        // [zh] 声明变量 `d`
        const d = result.data || {}
        // [zh] TypeScript/JS 语句：`proxy.Message.success(`日期同步 ${d.syncedUsers ?? 0} 用户，${`
        proxy.Message.success(`日期同步 ${d.syncedUsers ?? 0} 用户，${d.syncedDates ?? 0} 条`)
      // [zh] 代码块开始
      } finally {
        // [zh] TypeScript/JS 语句：`syncingDates.value = false`
        syncingDates.value = false
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `forceRebuildToday`
const forceRebuildToday = () => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: '强制重建含今日的签到日历，确定？',`
    message: '强制重建含今日的签到日历，确定？',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] TypeScript/JS 语句：`forcingToday.value = true`
      forcingToday.value = true
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `params`
        const params = {}
        // [zh] 条件判断 if
        if (syncUserId.value) params.userId = syncUserId.value.trim()
        // [zh] 声明变量 `result`
        const result = await proxy.Request({
          // [zh] TypeScript/JS 语句：`url: proxy.Api.signRecordForceRebuildToday,`
          url: proxy.Api.signRecordForceRebuildToday,
          // [zh] TypeScript/JS 语句：`params,`
          params,
          // [zh] TypeScript/JS 语句：`showLoading: true,`
          showLoading: true,
        // [zh] TypeScript/JS 语句：`})`
        })
        // [zh] 条件判断 if
        if (!result) return
        // [zh] 声明变量 `d`
        const d = result.data || {}
        // [zh] TypeScript/JS 语句：`proxy.Message.success(`强制重建 ${d.syncedUsers ?? 0} 用户，${`
        proxy.Message.success(`强制重建 ${d.syncedUsers ?? 0} 用户，${d.syncedDates ?? 0} 条`)
      // [zh] 代码块开始
      } finally {
        // [zh] TypeScript/JS 语句：`forcingToday.value = false`
        forcingToday.value = false
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `form`
const form = reactive({
  // [zh] TypeScript/JS 语句：`enabled: false,`
  enabled: false,
  // [zh] TypeScript/JS 语句：`couponId: '',`
  couponId: '',
  // [zh] TypeScript/JS 语句：`streakDays: 7,`
  streakDays: 7,
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `selectedCoupon`
const selectedCoupon = computed(() => couponOptions.value.find((c) => c.couponId === form.couponId))

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
    // [zh] TypeScript/JS 语句：`url: proxy.Api.signRewardGetConfig,`
    url: proxy.Api.signRewardGetConfig,
    // [zh] TypeScript/JS 语句：`showLoading: true,`
    showLoading: true,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] 声明变量 `data`
  const data = result.data || {}
  // [zh] TypeScript/JS 语句：`form.enabled = !!data.enabled`
  form.enabled = !!data.enabled
  // [zh] TypeScript/JS 语句：`form.couponId = data.couponId || ''`
  form.couponId = data.couponId || ''
  // [zh] TypeScript/JS 语句：`form.streakDays = data.streakDays ?? 7`
  form.streakDays = data.streakDays ?? 7
// [zh] 语法符号
}

// [zh] 声明变量 `save`
const save = async () => {
  // [zh] 条件判断 if
  if (form.enabled && !form.couponId) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请选择优惠券')`
    proxy.Message.warning('请选择优惠券')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (form.enabled && (!form.streakDays || form.streakDays < 1)) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请设置连续天数')`
    proxy.Message.warning('请设置连续天数')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`saving.value = true`
  saving.value = true
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `result`
    const result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.signRewardSaveConfig,`
      url: proxy.Api.signRewardSaveConfig,
      // [zh] 代码块开始
      params: {
        // [zh] TypeScript/JS 语句：`enabled: form.enabled,`
        enabled: form.enabled,
        // [zh] TypeScript/JS 语句：`couponId: form.enabled ? form.couponId : '',`
        couponId: form.enabled ? form.couponId : '',
        // [zh] TypeScript/JS 语句：`streakDays: form.streakDays,`
        streakDays: form.streakDays,
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
