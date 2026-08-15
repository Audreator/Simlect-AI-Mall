<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="member-reward-page">
    <!-- [zh] 开始标签 `<el-card>` -->
    <el-card class="config-card">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #header>
        <!-- [zh] 开始标签 `<span>` -->
        <span>会员升级礼券</span>
      </template>
      <el-form ref="formRef" :model="form" label-width="140px" class="config-form">
        <el-form-item label="银卡升级礼券">
          <el-select
            v-model="form.level2CouponId"
            filterable
            clearable
            placeholder="选填：银卡会员领取的优惠券"
            style="width: 100%; max-width: 480px"
          >
            <el-option
              v-for="c in couponOptions"
              :key="c.couponId"
              :label="couponLabel(c)"
              :value="c.couponId"
            />
          </el-select>
          <p v-if="silverCoupon" class="coupon-meta">
            剩余 {{ silverCoupon.remainCount ?? 0 }} /
            {{ silverCoupon.totalCount == 0 ? '不限' : silverCoupon.totalCount }}
            · 有效期 {{ silverCoupon.validStartTime || '—' }} ~ {{ silverCoupon.validEndTime || '—' }}
          </p>
          <span class="form-tip">用户成长值达 1000（银卡）且首次领取升级礼时发放</span>
        </el-form-item>
        <el-form-item label="金卡升级礼券">
          <el-select
            v-model="form.level3CouponId"
            filterable
            clearable
            placeholder="选填：金卡会员领取的优惠券"
            style="width: 100%; max-width: 480px"
          >
            <el-option
              v-for="c in couponOptions"
              :key="'g-' + c.couponId"
              :label="couponLabel(c)"
              :value="c.couponId"
            />
          </el-select>
          <p v-if="goldCoupon" class="coupon-meta">
            剩余 {{ goldCoupon.remainCount ?? 0 }} /
            {{ goldCoupon.totalCount == 0 ? '不限' : goldCoupon.totalCount }}
            · 有效期 {{ goldCoupon.validStartTime || '—' }} ~ {{ goldCoupon.validEndTime || '—' }}
          </p>
          <span class="form-tip">用户成长值达 5000（金卡）且首次领取升级礼时发放</span>
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
        description="配置保存在 Redis（mall:member:level:reward:config），保存后立即对用户「会员中心」领取升级礼生效。留空则仅发放成长值与站内通知，不发券。银卡另赠 20 成长值，金卡另赠 50 成长值（代码固定）。"
      />
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
const silverCoupon = computed(() =>
  // [zh] TypeScript/JS 语句：`couponOptions.value.find((c) => c.couponId === form.lev`
  couponOptions.value.find((c) => c.couponId === form.level2CouponId)
// [zh] 语法符号
)
// [zh] 声明变量 `goldCoupon`
const goldCoupon = computed(() =>
  // [zh] TypeScript/JS 语句：`couponOptions.value.find((c) => c.couponId === form.lev`
  couponOptions.value.find((c) => c.couponId === form.level3CouponId)
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

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `.member-reward-page {` */
.member-reward-page {
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
</style>
