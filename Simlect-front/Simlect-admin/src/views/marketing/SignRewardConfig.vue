<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="sign-reward-page">
    <!-- [zh] 开始标签 `<el-card>` -->
    <el-card class="config-card">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #header>
        <!-- [zh] 开始标签 `<span>` -->
        <span>连续签到发券</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" class="config-form">
        <el-form-item label="开启连续签到发券">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item v-if="form.enabled" label="连续签到天数" prop="streakDays">
          <el-input-number v-model="form.streakDays" :min="1" :max="30" />
          <span class="form-tip">满 N 天且为 N 的整数倍时发放（如 7 表示第 7、14、21 天…）</span>
        </el-form-item>
        <el-form-item v-if="form.enabled" label="奖励优惠券" prop="couponId">
          <el-select
            v-model="form.couponId"
            filterable
            clearable
            placeholder="请选择优惠券"
            style="width: 100%; max-width: 480px"
          >
            <el-option
              v-for="c in couponOptions"
              :key="c.couponId"
              :label="couponLabel(c)"
              :value="c.couponId"
            />
          </el-select>
          <p v-if="selectedCoupon" class="coupon-meta">
            剩余 {{ selectedCoupon.remainCount ?? 0 }} /
            {{ selectedCoupon.totalCount == 0 ? '不限' : selectedCoupon.totalCount }}
            · 有效期 {{ selectedCoupon.validStartTime || '—' }} ~ {{ selectedCoupon.validEndTime || '—' }}
          </p>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存到 Redis</el-button>
          <el-button @click="load">重新加载</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="说明"
        description="配置保存在 Redis（mall:sign:reward:config），保存后立即对用户签到生效。签到明细异步落库 user_sign_record_detail，Redis 丢失时可从 DB 重建最近 365 天日历。"
        style="margin-bottom: 16px"
      />
      <el-card class="sync-card" @click="onSyncCardHeaderClick">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #header>
          <!-- [zh] 开始标签 `<span>` -->
          <span>签到数据恢复（DB → Redis）</span>
        </template>
        <p class="sync-tip">
          汇总表 user_sign_record：连续天数、总签到天数、已用补签次数 → Redis Hash。
        </p>
        <el-button type="warning" :loading="syncingHash" @click="syncSignHashFromDb">
          同步汇总到 Redis
        </el-button>

        <el-divider />

        <p class="sync-tip">
          明细表 user_sign_record_detail：最近 365 天签到日期 → Redis Bitmap 日历。
          手动同步<strong>不含今天</strong>，默认截止<strong>昨天</strong>，仅同步所选日期及更早的历史记录。
        </p>
        <el-form inline @submit.prevent>
          <el-form-item label="同步截止日期">
            <el-date-picker
              v-model="syncEndDate"
              type="date"
              value-format="YYYYMMDD"
              placeholder="选择日期"
              :disabled-date="disableSyncDate"
            />
          </el-form-item>
          <el-form-item label="用户ID（可选）">
            <el-input v-model="syncUserId" clearable placeholder="留空=全部用户" style="width: 180px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="syncingDates" @click="syncSignDatesFromDb">
              同步历史签到日期
            </el-button>
          </el-form-item>
        </el-form>
        <p v-if="showForceRebuild" class="force-tip">
          <el-button link type="danger" :loading="forcingToday" @click="forceRebuildToday">
            强制重建含今日数据
          </el-button>
        </p>
      </el-card>
    </el-card>
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `formRef`
const formRef = ref()
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

// [zh] 声明变量 `syncEndDate`
const syncEndDate = ref(yesterdayYmd())

// [zh] 声明变量 `disableSyncDate`
const disableSyncDate = (date) => {
  // [zh] 声明变量 `today`
  const today = new Date()
  // [zh] TypeScript/JS 语句：`today.setHours(0, 0, 0, 0)`
  today.setHours(0, 0, 0, 0)
  // [zh] return 返回
  return date.getTime() >= today.getTime()
// [zh] 语法符号
}

// [zh] 声明变量 `onSyncCardHeaderClick`
const onSyncCardHeaderClick = () => {
  // [zh] TypeScript/JS 语句：`forceClickCount.value += 1`
  forceClickCount.value += 1
  // [zh] 条件判断 if
  if (forceClickCount.value >= 5) {
    // [zh] TypeScript/JS 语句：`showForceRebuild.value = true`
    showForceRebuild.value = true
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `syncSignHashFromDb`
const syncSignHashFromDb = () => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message:`
    message:
      // [zh] TypeScript/JS 语句：`'将 user_sign_record 汇总数据（连续天数、总签到天数、已用补签次数）覆盖写入 Redis H`
      '将 user_sign_record 汇总数据（连续天数、总签到天数、已用补签次数）覆盖写入 Redis Hash，确定继续？',
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
        // [zh] TypeScript/JS 语句：`proxy.Message.success(`汇总同步完成：写入 ${d.synced ?? 0} 条，跳过 `
        proxy.Message.success(`汇总同步完成：写入 ${d.synced ?? 0} 条，跳过 ${d.skipped ?? 0} 条`)
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
  // [zh] 条件判断 if
  if (!syncEndDate.value) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('请选择同步截止日期')`
    proxy.Message.warning('请选择同步截止日期')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `将 ${syncUserId.value || '全部用户'} 截至 ${syncEndD`
    message: `将 ${syncUserId.value || '全部用户'} 截至 ${syncEndDate.value} 的签到明细同步到 Redis 日历（不含今天），确定？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] TypeScript/JS 语句：`syncingDates.value = true`
      syncingDates.value = true
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `params`
        const params = { syncEndDate: syncEndDate.value }
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
        // [zh] TypeScript/JS 语句：`proxy.Message.success(`
        proxy.Message.success(
          // [zh] TypeScript/JS 语句：``日期同步完成：${d.syncedUsers ?? 0} 用户，${d.syncedDates ?? 0} `
          `日期同步完成：${d.syncedUsers ?? 0} 用户，${d.syncedDates ?? 0} 条日期`
        // [zh] 语法符号
        )
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
    // [zh] TypeScript/JS 语句：`message: '隐藏运维：强制从 DB 重建含今日在内的签到日历，仅极端恢复时使用，确定？',`
    message: '隐藏运维：强制从 DB 重建含今日在内的签到日历，仅极端恢复时使用，确定？',
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
        // [zh] TypeScript/JS 语句：`proxy.Message.success(`
        proxy.Message.success(
          // [zh] TypeScript/JS 语句：``强制重建完成：${d.syncedUsers ?? 0} 用户，${d.syncedDates ?? 0} `
          `强制重建完成：${d.syncedUsers ?? 0} 用户，${d.syncedDates ?? 0} 条日期`
        // [zh] 语法符号
        )
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

// [zh] 声明变量 `rules`
const rules = {
  // [zh] TypeScript/JS 语句：`streakDays: [{ required: true, message: '请设置连续天数', trig`
  streakDays: [{ required: true, message: '请设置连续天数', trigger: 'blur' }],
  // [zh] TypeScript/JS 语句：`couponId: [`
  couponId: [
    // [zh] 代码块开始
    {
      // [zh] 代码块开始
      validator: (_rule, value, callback) => {
        // [zh] 条件判断 if
        if (form.enabled && !value) {
          // [zh] TypeScript/JS 语句：`callback(new Error('请选择优惠券'))`
          callback(new Error('请选择优惠券'))
        // [zh] 代码块开始
        } else {
          // [zh] TypeScript/JS 语句：`callback()`
          callback()
        // [zh] 语法符号
        }
      // [zh] TypeScript/JS 语句：`},`
      },
      // [zh] TypeScript/JS 语句：`trigger: 'change',`
      trigger: 'change',
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] 语法符号
  ],
// [zh] 语法符号
}

// [zh] 声明变量 `selectedCoupon`
const selectedCoupon = computed(() =>
  // [zh] TypeScript/JS 语句：`couponOptions.value.find((c) => c.couponId === form.cou`
  couponOptions.value.find((c) => c.couponId === form.couponId)
// [zh] 语法符号
)

// [zh] 声明变量 `couponLabel`
const couponLabel = (c) => {
  // [zh] 声明变量 `type`
  const type =
    // [zh] TypeScript/JS 语句：`c.couponType == 1 ? '满减' : c.couponType == 2 ? '折扣' : c`
    c.couponType == 1 ? '满减' : c.couponType == 2 ? '折扣' : c.couponType == 3 ? '无门槛' : '券'
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
  if (!formRef.value) return
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await formRef.value.validate()
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

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `.sign-reward-page {` */
.sign-reward-page {
  /* [zh] 样式规则 `max-width: 720px;` */
  max-width: 720px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.config-card {` */
.config-card {
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.config-form {` */
.config-form {
  /* [zh] 样式规则 `margin-bottom: 16px;` */
  margin-bottom: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.form-tip {` */
.form-tip {
  /* [zh] 样式规则 `display: block;` */
  display: block;
  /* [zh] 样式规则 `margin-top: 6px;` */
  margin-top: 6px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: #888;` */
  color: #888;
  /* [zh] 样式规则 `line-height: 1.4;` */
  line-height: 1.4;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.coupon-meta {` */
.coupon-meta {
  /* [zh] 样式规则 `margin: 8px 0 0;` */
  margin: 8px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: #666;` */
  color: #666;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sync-card {` */
.sync-card {
  /* [zh] 样式规则 `margin-top: 8px;` */
  margin-top: 8px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sync-tip {` */
.sync-tip {
  /* [zh] 样式规则 `margin: 0 0 12px;` */
  margin: 0 0 12px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: #666;` */
  color: #666;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.force-tip {` */
.force-tip {
  /* [zh] 样式规则 `margin: 8px 0 0;` */
  margin: 8px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
/* [zh] 样式规则 `}` */
}
</style>
