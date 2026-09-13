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
        description="奖励配置保存在 Redis，签到数据以 MySQL sign_bitmap 为唯一权威；成长值、明细和奖励事件与位图在同一事务提交，Redis 日历仅作可自动回源的读缓存。"
        style="margin-bottom: 16px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'

const { proxy } = getCurrentInstance()
const formRef = ref()
const saving = ref(false)
const couponOptions = ref([])

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
