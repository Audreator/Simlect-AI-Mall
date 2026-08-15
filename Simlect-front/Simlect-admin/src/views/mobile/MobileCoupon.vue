<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-toolbar">
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="op-btn primary" @click="showEdit()">新增优惠券</button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-search glass-card glass-strong">
      <!-- [zh] 开始标签 `<input>` -->
      <input v-model="searchForm.couponNameFuzzy" class="search-input" placeholder="优惠券名称" @keyup.enter="reload" />
      <!-- [zh] 开始标签 `<select>` -->
      <select v-model="searchForm.status" class="search-select" @change="reload">
        <!-- [zh] 开始标签 `<option>` -->
        <option value="">全部</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="1">进行中</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="0">已停用</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="2">已过期</option>
        <!-- [zh] 开始标签 `<option>` -->
        <option :value="3">已发完</option>
      <!-- [zh] 闭合标签 `</select>` -->
      </select>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<p>` -->
    <p class="m-note">支持新增/编辑、启用停用与秒杀库存维护。</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="row in list" :key="row.couponId" class="glass-card coupon-card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="coupon-left">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="coupon-amount">
            <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
            <template v-if="row.couponType == 2">{{ (row.discountRate * 10).toFixed(1) }}折</template>
            <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
            <template v-else>¥{{ row.discountAmount }}</template>
          <!-- [zh] 闭合标签 `</span>` -->
          </span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="coupon-type">{{ typeText(row.couponType) }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="coupon-mid">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="coupon-name">{{ row.couponName }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="coupon-stock">库存 {{ row.remainCount || 0 }} / {{ row.totalCount == 0 ? '不限' : row.totalCount }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="coupon-valid">{{ row.validStartTime || '--' }} ~ {{ row.validEndTime || '--' }}</span>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="coupon-tags">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="m-tag" :class="statusClass(row.status)">{{ statusText(row.status) }}</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span v-if="isRush(row)" class="m-tag danger">秒杀</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="!loading" class="m-empty-tip">暂无优惠券</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list ops-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="row in list" :key="'op-' + row.couponId" class="glass-card coupon-ops">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="ops-name">{{ row.couponName }}</span>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn" @click="showEdit(row.couponId)">编辑</button>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn" @click="toggleStatus(row)">{{ row.status == 0 ? '启用' : '停用' }}</button>
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-if="isRush(row)">
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn" @click="warmupOne(row)">预热</button>
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn" @click="reconcileOne(row)">对账</button>
        </template>
      </div>
    </div>

    <div ref="sentinel" class="m-sentinel">
      <span v-if="loading">加载中…</span>
      <span v-else-if="finished && list.length">没有更多了</span>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="currentCouponId ? '修改优惠券' : '新增优惠券'"
      width="92%"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="优惠券名称" prop="couponName">
          <el-input v-model="form.couponName" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="优惠券类型" prop="couponType">
          <el-radio-group v-model="form.couponType" @change="onCouponTypeChange">
            <el-radio :label="1">满减券</el-radio>
            <el-radio :label="2">折扣券</el-radio>
            <el-radio :label="3">无门槛券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.couponType !== 3" label="使用门槛金额" prop="thresholdAmount">
          <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.couponType !== 2" label="优惠金额" prop="discountAmount">
          <el-input-number v-model="form.discountAmount" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.couponType === 2" label="折扣率" prop="discountRate">
          <el-input-number v-model="form.discountRate" :min="0" :max="1" :precision="2" :step="0.05" style="width: 100%" />
          <div class="m-tip">如 0.85 表示 85 折</div>
        </el-form-item>
        <el-form-item label="发放总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="0" :step="1" style="width: 100%" />
          <div class="m-tip">0 表示不限量</div>
        </el-form-item>
        <el-form-item label="有效期开始" prop="validStartTime">
          <el-date-picker
            v-model="form.validStartTime"
            type="datetime"
            placeholder="开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="有效期结束" prop="validEndTime">
          <el-date-picker
            v-model="form.validEndTime"
            type="datetime"
            placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="上架秒杀" prop="rushingstatus">
          <el-switch v-model="form.rushingstatus" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-if="form.rushingstatus === 1">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="秒杀开始时间" prop="rushingStartTime">
            <!-- [zh] 开始标签 `<el-date-picker>` -->
            <el-date-picker
              v-model="form.rushingStartTime"
              type="datetime"
              placeholder="秒杀开始"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="秒杀结束时间" prop="rushingEndTime">
            <!-- [zh] 开始标签 `<el-date-picker>` -->
            <el-date-picker
              v-model="form.rushingEndTime"
              type="datetime"
              placeholder="秒杀结束"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        </template>
      </el-form>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #footer>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn" @click="dialogVisible = false">取消</button>
        <!-- [zh] 开始标签 `<button>` -->
        <button type="button" class="op-btn primary" @click="handleSave">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, computed, getCurrentInstance, onMounted, onUnmounted } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `searchForm`
const searchForm = reactive({ couponNameFuzzy: '', status: '' })
// [zh] 声明变量 `list`
const list = ref([])
// [zh] 声明变量 `pageNo`
const pageNo = ref(0)
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(1)
// [zh] 声明变量 `loading`
const loading = ref(false)
// [zh] 声明变量 `finished`
const finished = ref(false)
// [zh] 声明变量 `sentinel`
const sentinel = ref(null)
// [zh] 声明变量 `dialogVisible`
const dialogVisible = ref(false)
// [zh] 声明变量 `formRef`
const formRef = ref()
// [zh] 声明变量 `currentCouponId`
const currentCouponId = ref(null)
// [zh] 声明变量 `observer`
let observer = null

// [zh] 声明变量 `form`
const form = reactive({
  // [zh] TypeScript/JS 语句：`couponName: '',`
  couponName: '',
  // [zh] TypeScript/JS 语句：`couponType: 1,`
  couponType: 1,
  // [zh] TypeScript/JS 语句：`thresholdAmount: 0,`
  thresholdAmount: 0,
  // [zh] TypeScript/JS 语句：`discountAmount: 0,`
  discountAmount: 0,
  // [zh] TypeScript/JS 语句：`discountRate: 0.9,`
  discountRate: 0.9,
  // [zh] TypeScript/JS 语句：`totalCount: 0,`
  totalCount: 0,
  // [zh] TypeScript/JS 语句：`validStartTime: '',`
  validStartTime: '',
  // [zh] TypeScript/JS 语句：`validEndTime: '',`
  validEndTime: '',
  // [zh] TypeScript/JS 语句：`rushingstatus: 0,`
  rushingstatus: 0,
  // [zh] TypeScript/JS 语句：`rushingStartTime: '',`
  rushingStartTime: '',
  // [zh] TypeScript/JS 语句：`rushingEndTime: '',`
  rushingEndTime: '',
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `rules`
const rules = computed(() => {
  // [zh] 声明变量 `rule`
  const rule = {
    // [zh] TypeScript/JS 语句：`couponName: [{ required: true, message: '请输入优惠券名称', tri`
    couponName: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
    // [zh] TypeScript/JS 语句：`couponType: [{ required: true, message: '请选择优惠券类型', tri`
    couponType: [{ required: true, message: '请选择优惠券类型', trigger: 'change' }],
    // [zh] TypeScript/JS 语句：`discountAmount: [{ required: true, message: '请输入优惠金额', `
    discountAmount: [{ required: true, message: '请输入优惠金额', trigger: 'blur' }],
    // [zh] TypeScript/JS 语句：`discountRate: [{ required: true, message: '请输入折扣率', tri`
    discountRate: [{ required: true, message: '请输入折扣率', trigger: 'blur' }],
    // [zh] TypeScript/JS 语句：`totalCount: [{ required: true, message: '请输入发放总量', trig`
    totalCount: [{ required: true, message: '请输入发放总量', trigger: 'blur' }],
    // [zh] TypeScript/JS 语句：`validStartTime: [{ required: true, message: '请选择有效期开始时间`
    validStartTime: [{ required: true, message: '请选择有效期开始时间', trigger: 'change' }],
    // [zh] TypeScript/JS 语句：`validEndTime: [{ required: true, message: '请选择有效期结束时间',`
    validEndTime: [{ required: true, message: '请选择有效期结束时间', trigger: 'change' }],
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (form.couponType !== 3) {
    // [zh] TypeScript/JS 语句：`rule.thresholdAmount = [{ required: true, message: '请输入`
    rule.thresholdAmount = [{ required: true, message: '请输入使用门槛金额', trigger: 'blur' }]
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (form.rushingstatus === 1) {
    // [zh] TypeScript/JS 语句：`rule.rushingStartTime = [{ required: true, message: '请选`
    rule.rushingStartTime = [{ required: true, message: '请选择秒杀开始时间', trigger: 'change' }]
    // [zh] TypeScript/JS 语句：`rule.rushingEndTime = [{ required: true, message: '请选择秒`
    rule.rushingEndTime = [{ required: true, message: '请选择秒杀结束时间', trigger: 'change' }]
  // [zh] 语法符号
  }
  // [zh] return 返回
  return rule
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `typeText`
const typeText = (t) => (t == 1 ? '满减券' : t == 2 ? '折扣券' : '无门槛')
// [zh] 声明变量 `statusText`
const statusText = (s) => (s == 0 ? '已停用' : s == 1 ? '进行中' : s == 2 ? '已过期' : '已发完')
// [zh] 声明变量 `statusClass`
const statusClass = (s) => (s == 1 ? 'green' : s == 0 ? 'muted' : 'gold')
// [zh] 声明变量 `isRush`
const isRush = (row) => row.rushingstatus == 1 || row.rushingStatus == 1

// [zh] 声明变量 `loadList`
const loadList = async (reset = false) => {
  // [zh] 条件判断 if
  if (loading.value) return
  // [zh] 条件判断 if
  if (reset) {
    // [zh] TypeScript/JS 语句：`pageNo.value = 0`
    pageNo.value = 0
    // [zh] TypeScript/JS 语句：`pageTotal.value = 1`
    pageTotal.value = 1
    // [zh] TypeScript/JS 语句：`finished.value = false`
    finished.value = false
    // [zh] TypeScript/JS 语句：`list.value = []`
    list.value = []
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (finished.value) return
  // [zh] TypeScript/JS 语句：`loading.value = true`
  loading.value = true
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `next`
    const next = pageNo.value + 1
    // [zh] 声明变量 `params`
    const params = { pageNo: next, pageSize: 10 }
    // [zh] 条件判断 if
    if (searchForm.couponNameFuzzy) params.couponNameFuzzy = searchForm.couponNameFuzzy
    // [zh] 条件判断 if
    if (searchForm.status !== '') params.status = searchForm.status
    // [zh] 声明变量 `result`
    const result = await proxy.Request({ url: proxy.Api.loadDiscountCoupon, params, showLoading: false })
    // [zh] 条件判断 if
    if (!result) return
    // [zh] 声明变量 `data`
    const data = result.data || {}
    // [zh] 声明变量 `chunk`
    const chunk = data.list || []
    // [zh] TypeScript/JS 语句：`list.value = next === 1 ? chunk : list.value.concat(chu`
    list.value = next === 1 ? chunk : list.value.concat(chunk)
    // [zh] TypeScript/JS 语句：`pageNo.value = Number(data.pageNo) || next`
    pageNo.value = Number(data.pageNo) || next
    // [zh] TypeScript/JS 语句：`pageTotal.value = Number(data.pageTotal) || pageNo.valu`
    pageTotal.value = Number(data.pageTotal) || pageNo.value
    // [zh] TypeScript/JS 语句：`finished.value = pageNo.value >= pageTotal.value`
    finished.value = pageNo.value >= pageTotal.value
  // [zh] 代码块开始
  } finally {
    // [zh] TypeScript/JS 语句：`loading.value = false`
    loading.value = false
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `reload`
const reload = () => loadList(true)

// [zh] 声明变量 `toggleStatus`
const toggleStatus = (row) => {
  // [zh] 代码块开始
  proxy.ConfirmSensitive({
    // [zh] TypeScript/JS 语句：`message: `确定要${row.status == 0 ? '启用' : '停用'}「${row.cou`
    message: `确定要${row.status == 0 ? '启用' : '停用'}「${row.couponName}」吗？`,
    // [zh] 代码块开始
    okfun: async (sensitiveConfirmToken) => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.updateDiscountCouponStatus,`
        url: proxy.Api.updateDiscountCouponStatus,
        // [zh] TypeScript/JS 语句：`sensitiveConfirmToken,`
        sensitiveConfirmToken,
        // [zh] TypeScript/JS 语句：`params: { couponId: row.couponId, status: row.status ==`
        params: { couponId: row.couponId, status: row.status == 0 ? 1 : 0 }
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('操作成功')`
      proxy.Message.success('操作成功')
      // [zh] TypeScript/JS 语句：`reload()`
      reload()
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `warmupOne`
const warmupOne = (row) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `预热「${row.couponName}」的秒杀 Redis 库存？`,`
    message: `预热「${row.couponName}」的秒杀 Redis 库存？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({ url: proxy.Api.warmupRushStock, params: { couponId: row.couponId }, showLoading: true })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('预热成功')`
      proxy.Message.success('预热成功')
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `reconcileOne`
const reconcileOne = (row) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `对「${row.couponName}」执行 Redis/DB 对账？`,`
    message: `对「${row.couponName}」执行 Redis/DB 对账？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({ url: proxy.Api.reconcileRushStock, params: { couponId: row.couponId }, showLoading: true })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('对账完成')`
      proxy.Message.success('对账完成')
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `showEdit`
const showEdit = async (couponId) => {
  // [zh] TypeScript/JS 语句：`currentCouponId.value = couponId || null`
  currentCouponId.value = couponId || null
  // [zh] TypeScript/JS 语句：`dialogVisible.value = true`
  dialogVisible.value = true
  // [zh] 代码块开始
  Object.assign(form, {
    // [zh] TypeScript/JS 语句：`couponName: '',`
    couponName: '',
    // [zh] TypeScript/JS 语句：`couponType: 1,`
    couponType: 1,
    // [zh] TypeScript/JS 语句：`thresholdAmount: 0,`
    thresholdAmount: 0,
    // [zh] TypeScript/JS 语句：`discountAmount: 0,`
    discountAmount: 0,
    // [zh] TypeScript/JS 语句：`discountRate: 0.9,`
    discountRate: 0.9,
    // [zh] TypeScript/JS 语句：`totalCount: 0,`
    totalCount: 0,
    // [zh] TypeScript/JS 语句：`validStartTime: '',`
    validStartTime: '',
    // [zh] TypeScript/JS 语句：`validEndTime: '',`
    validEndTime: '',
    // [zh] TypeScript/JS 语句：`rushingstatus: 0,`
    rushingstatus: 0,
    // [zh] TypeScript/JS 语句：`rushingStartTime: '',`
    rushingStartTime: '',
    // [zh] TypeScript/JS 语句：`rushingEndTime: '',`
    rushingEndTime: '',
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (couponId) {
    // [zh] 声明变量 `result`
    const result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.getDiscountCouponInfo,`
      url: proxy.Api.getDiscountCouponInfo,
      // [zh] TypeScript/JS 语句：`params: { couponId },`
      params: { couponId },
      // [zh] TypeScript/JS 语句：`showLoading: true,`
      showLoading: true,
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 条件判断 if
    if (!result) {
      // [zh] TypeScript/JS 语句：`dialogVisible.value = false`
      dialogVisible.value = false
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
    // [zh] TypeScript/JS 语句：`Object.assign(form, result.data)`
    Object.assign(form, result.data)
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `onCouponTypeChange`
const onCouponTypeChange = () => {
  // [zh] 条件判断 if
  if (form.couponType === 2) form.discountAmount = 0
  // [zh] else 分支
  else form.discountRate = 0.9
  // [zh] 条件判断 if
  if (form.couponType === 3) form.thresholdAmount = 0
// [zh] 语法符号
}

// [zh] 声明变量 `handleSave`
const handleSave = async () => {
  // [zh] 条件判断 if
  if (!formRef.value) return
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await formRef.value.validate(async (valid) => {
    // [zh] 条件判断 if
    if (!valid) return
    // [zh] 声明变量 `params`
    const params = { ...form }
    // [zh] 条件判断 if
    if (currentCouponId.value) params.couponId = currentCouponId.value
    // [zh] 声明变量 `result`
    const result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.saveDiscountCoupon,`
      url: proxy.Api.saveDiscountCoupon,
      // [zh] TypeScript/JS 语句：`params,`
      params,
      // [zh] TypeScript/JS 语句：`showLoading: true,`
      showLoading: true,
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 条件判断 if
    if (!result) return
    // [zh] TypeScript/JS 语句：`proxy.Message.success('保存成功')`
    proxy.Message.success('保存成功')
    // [zh] TypeScript/JS 语句：`dialogVisible.value = false`
    dialogVisible.value = false
    // [zh] TypeScript/JS 语句：`reload()`
    reload()
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`loadList(true)`
  loadList(true)
  // [zh] TypeScript/JS 语句：`observer = new IntersectionObserver(`
  observer = new IntersectionObserver(
    // [zh] 代码块开始
    (entries) => {
      // [zh] 条件判断 if
      if (entries.some((e) => e.isIntersecting)) loadList()
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ rootMargin: '0px 0px 300px 0px' }`
    { rootMargin: '0px 0px 300px 0px' }
  // [zh] 语法符号
  )
  // [zh] 条件判断 if
  if (sentinel.value) observer.observe(sentinel.value)
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] TypeScript/JS 语句：`observer && observer.disconnect()`
  observer && observer.disconnect()
  // [zh] TypeScript/JS 语句：`observer = null`
  observer = null
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.m-simple {` */
.m-simple {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-search {` */
.m-search {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `height: 44px;` */
  height: 44px;
  /* [zh] 样式规则 `padding: 0 12px;` */
  padding: 0 12px;
  /* [zh] 样式规则 `border-radius: 16px;` */
  border-radius: 16px;

  /* [zh] 样式规则 `.search-input {` */
  .search-input {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
    /* [zh] 样式规则 `outline: none;` */
    outline: none;

    /* [zh] 样式规则 `&::placeholder {` */
    &::placeholder {
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.search-select {` */
  .search-select {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.5);` */
    background: rgba(255, 255, 255, 0.5);
    /* [zh] 样式规则 `border-radius: 10px;` */
    border-radius: 10px;
    /* [zh] 样式规则 `padding: 4px 6px;` */
    padding: 4px 6px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
    /* [zh] 样式规则 `outline: none;` */
    outline: none;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-note {` */
.m-note {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-list {` */
.m-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.coupon-card {` */
.coupon-card {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;

  /* [zh] 样式规则 `.coupon-left {` */
  .coupon-left {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `width: 78px;` */
    width: 78px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `border-right: 1px dashed rgba(120, 120, ` */
    border-right: 1px dashed rgba(120, 120, 128, 0.3);

    /* [zh] 样式规则 `.coupon-amount {` */
    .coupon-amount {
      /* [zh] 样式规则 `font-size: 19px;` */
      font-size: 19px;
      /* [zh] 样式规则 `font-weight: 700;` */
      font-weight: 700;
      /* [zh] 样式规则 `color: var(--m-gold);` */
      color: var(--m-gold);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.coupon-type {` */
    .coupon-type {
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.coupon-mid {` */
  .coupon-mid {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `gap: 3px;` */
    gap: 3px;

    /* [zh] 样式规则 `.coupon-name {` */
    .coupon-name {
      /* [zh] 样式规则 `font-size: 14px;` */
      font-size: 14px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `color: var(--m-ink);` */
      color: var(--m-ink);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式声明 */
    .coupon-stock,
    /* [zh] 样式规则 `.coupon-valid {` */
    .coupon-valid {
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.coupon-tags {` */
  .coupon-tags {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
    /* [zh] 样式规则 `margin-top: 2px;` */
    margin-top: 2px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.ops-list .coupon-ops {` */
.ops-list .coupon-ops {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `padding: 10px 14px;` */
  padding: 10px 14px;

  /* [zh] 样式规则 `.ops-name {` */
  .ops-name {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `text-overflow: ellipsis;` */
    text-overflow: ellipsis;
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.op-btn {` */
  .op-btn {
    /* [zh] 样式规则 `flex-shrink: 0;` */
    flex-shrink: 0;
    /* [zh] 样式规则 `height: 30px;` */
    height: 30px;
    /* [zh] 样式规则 `padding: 0 12px;` */
    padding: 0 12px;
    /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
    border: 1px solid rgba(120, 120, 128, 0.24);
    /* [zh] 样式规则 `border-radius: 10px;` */
    border-radius: 10px;
    /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.5);` */
    background: rgba(255, 255, 255, 0.5);
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-tag {` */
.m-tag {
  /* [zh] 样式规则 `padding: 2px 8px;` */
  padding: 2px 8px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `font-size: 11px;` */
  font-size: 11px;

  /* [zh] 样式规则 `&.green {` */
  &.green {
    /* [zh] 样式规则 `background: rgba(52, 199, 89, 0.16);` */
    background: rgba(52, 199, 89, 0.16);
    /* [zh] 样式规则 `color: #1c8c3c;` */
    color: #1c8c3c;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.muted {` */
  &.muted {
    /* [zh] 样式规则 `background: rgba(120, 120, 128, 0.16);` */
    background: rgba(120, 120, 128, 0.16);
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.gold {` */
  &.gold {
    /* [zh] 样式规则 `background: var(--m-gold-soft);` */
    background: var(--m-gold-soft);
    /* [zh] 样式规则 `color: #927535;` */
    color: #927535;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.danger {` */
  &.danger {
    /* [zh] 样式规则 `background: rgba(255, 59, 48, 0.14);` */
    background: rgba(255, 59, 48, 0.14);
    /* [zh] 样式规则 `color: var(--m-danger);` */
    color: var(--m-danger);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-sentinel {` */
.m-sentinel {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `min-height: 28px;` */
  min-height: 28px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-empty-tip {` */
.m-empty-tip {
  /* [zh] 样式规则 `margin: 24px 0;` */
  margin: 24px 0;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
/* [zh] 样式规则 `}` */
}
</style>
