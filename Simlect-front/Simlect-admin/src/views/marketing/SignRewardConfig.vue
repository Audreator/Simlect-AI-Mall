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

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'

const { proxy } = getCurrentInstance()
const formRef = ref()
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

const syncEndDate = ref(yesterdayYmd())

const disableSyncDate = (date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() >= today.getTime()
}

const onSyncCardHeaderClick = () => {
  forceClickCount.value += 1
  if (forceClickCount.value >= 5) {
    showForceRebuild.value = true
  }
}

const syncSignHashFromDb = () => {
  proxy.Confirm({
    message:
      '将 user_sign_record 汇总数据（连续天数、总签到天数、已用补签次数）覆盖写入 Redis Hash，确定继续？',
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
        proxy.Message.success(`汇总同步完成：写入 ${d.synced ?? 0} 条，跳过 ${d.skipped ?? 0} 条`)
      } finally {
        syncingHash.value = false
      }
    },
  })
}

const syncSignDatesFromDb = () => {
  if (!syncEndDate.value) {
    proxy.Message.warning('请选择同步截止日期')
    return
  }
  proxy.Confirm({
    message: `将 ${syncUserId.value || '全部用户'} 截至 ${syncEndDate.value} 的签到明细同步到 Redis 日历（不含今天），确定？`,
    okfun: async () => {
      syncingDates.value = true
      try {
        const params = { syncEndDate: syncEndDate.value }
        if (syncUserId.value) params.userId = syncUserId.value.trim()
        const result = await proxy.Request({
          url: proxy.Api.signRecordSyncSignDatesFromDb,
          params,
          showLoading: true,
        })
        if (!result) return
        const d = result.data || {}
        proxy.Message.success(
          `日期同步完成：${d.syncedUsers ?? 0} 用户，${d.syncedDates ?? 0} 条日期`
        )
      } finally {
        syncingDates.value = false
      }
    },
  })
}

const forceRebuildToday = () => {
  proxy.Confirm({
    message: '隐藏运维：强制从 DB 重建含今日在内的签到日历，仅极端恢复时使用，确定？',
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
        proxy.Message.success(
          `强制重建完成：${d.syncedUsers ?? 0} 用户，${d.syncedDates ?? 0} 条日期`
        )
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

const rules = {
  streakDays: [{ required: true, message: '请设置连续天数', trigger: 'blur' }],
  couponId: [
    {
      validator: (_rule, value, callback) => {
        if (form.enabled && !value) {
          callback(new Error('请选择优惠券'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

const selectedCoupon = computed(() =>
  couponOptions.value.find((c) => c.couponId === form.couponId)
)

const couponLabel = (c) => {
  const type =
    c.couponType == 1 ? '满减' : c.couponType == 2 ? '折扣' : c.couponType == 3 ? '无门槛' : '券'
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
  if (!formRef.value) return
  await formRef.value.validate()
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
