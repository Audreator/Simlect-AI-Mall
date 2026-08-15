<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="search-panel">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="searchForm" @submit.prevent>
      <!-- [zh] 开始标签 `<el-row>` -->
      <el-row :gutter="10">
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="优惠券名称">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input clearable placeholder="输入优惠券名称" v-model="searchForm.couponNameFuzzy"></el-input>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="类型" prop="">
            <!-- [zh] 开始标签 `<el-select>` -->
            <el-select clearable placeholder="请选择类型" v-model="searchForm.couponType">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="1" label="满减券"></el-option>
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="2" label="折扣券"></el-option>
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="3" label="无门槛券"></el-option>
            <!-- [zh] 闭合标签 `</el-select>` -->
            </el-select>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="状态" prop="">
            <!-- [zh] 开始标签 `<el-select>` -->
            <el-select clearable placeholder="请选择状态" v-model="searchForm.status">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="0" label="已停用"></el-option>
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="1" label="进行中"></el-option>
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="2" label="已过期"></el-option>
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option :value="3" label="已发完"></el-option>
            <!-- [zh] 闭合标签 `</el-select>` -->
            </el-select>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="13" class="toolbar-actions">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" @click="loadDataList">搜索</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="success" @click="showEdit()">新增优惠券</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="warning" plain @click="warmupAllRush">秒杀库存预热(全部)</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="warning" plain @click="reconcileAllRush">秒杀库存对账(全部)</el-button>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
      <!-- [zh] 闭合标签 `</el-row>` -->
      </el-row>
    <!-- [zh] 闭合标签 `</el-form>` -->
    </el-form>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
  <!-- [zh] 开始标签 `<el-card>` -->
  <el-card class="table-data-card">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="table-panel">
      <!-- [zh] 开始标签 `<Table>` -->
      <Table ref="tableInfoRef" :columns="columns" :fetch="loadDataList" :dataSource="tableData">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotType="{ index, row }">
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.couponType == 1" effect="dark" type="primary">满减券</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.couponType == 2" effect="dark" type="warning">折扣券</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.couponType == 3" effect="dark" type="success">无门槛券</el-tag>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotAmount="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div v-if="row.couponType == 2">
            <!-- [zh] Mustache 插值表达式 -->
            {{ (row.discountRate * 10).toFixed(1) }}折
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div v-else>
            <!-- [zh] 模板内容：`¥{{ row.discountAmount }}` -->
            ¥{{ row.discountAmount }}
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotStock="{ index, row }">
          <!-- [zh] Mustache 插值表达式 -->
          {{ row.remainCount || 0 }} / {{ row.totalCount == 0 ? '不限' : row.totalCount }}
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotValidTime="{ index, row }">
          <!-- [zh] Mustache 插值表达式 -->
          {{ row.validStartTime || '--' }} ~ {{ row.validEndTime || '--' }}
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotRush="{ row }">
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.rushingstatus == 1 || row.rushingStatus == 1" type="danger" effect="plain">秒杀</el-tag>
          <!-- [zh] 开始标签 `<span>` -->
          <span v-else class="text-muted">—</span>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotStatus="{ index, row }">
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status == 0" effect="dark" type="info">已停用</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status == 1" effect="dark" type="success">进行中</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status == 2" effect="dark" type="danger">已过期</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status == 3" effect="dark" type="warning">已发完</el-tag>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotOp="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="list-op-panel">
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn icon="icon-edit" tips="修改" @click="showEdit(row.couponId)"></OpBtn>
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn :icon="row.status == 0 ? 'icon-shangjia' : 'icon-xiajia'"
              :tips="row.status == 0 ? '启用' : '停用'" @click="toggleStatus(row)">
            <!-- [zh] 闭合标签 `</OpBtn>` -->
            </OpBtn>
            <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
            <template v-if="row.rushingstatus == 1 || row.rushingStatus == 1">
              <!-- [zh] 开始标签 `<OpBtn>` -->
              <OpBtn icon="icon-stock" tips="预热Redis库存" @click="warmupOne(row)"></OpBtn>
              <!-- [zh] 开始标签 `<OpBtn>` -->
              <OpBtn icon="icon-view" tips="Redis对账" @click="reconcileOne(row)"></OpBtn>
            </template>
          </div>
        </template>
      </Table>
    </div>
  </el-card>

  <el-dialog
    v-model="dialogVisible"
    :title="currentCouponId ? '修改优惠券' : '新增优惠券'"
    width="600px"
    :close-on-click-modal="false"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="优惠券名称" prop="couponName">
        <el-input v-model="form.couponName" placeholder="请输入优惠券名称"></el-input>
      </el-form-item>
      <el-form-item label="优惠券类型" prop="couponType">
        <el-radio-group v-model="form.couponType" @change="onCouponTypeChange">
          <el-radio :label="1">满减券</el-radio>
          <el-radio :label="2">折扣券</el-radio>
          <el-radio :label="3">无门槛券</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="使用门槛金额" prop="thresholdAmount" v-if="form.couponType !== 3">
        <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" :step="1" style="width: 200px">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template #suffix>元</template>
        <!-- [zh] 闭合标签 `</el-input-number>` -->
        </el-input-number>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="优惠金额" prop="discountAmount" v-if="form.couponType !== 2">
        <!-- [zh] 开始标签 `<el-input-number>` -->
        <el-input-number v-model="form.discountAmount" :min="0" :precision="2" :step="1" style="width: 200px">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template #suffix>元</template>
        <!-- [zh] 闭合标签 `</el-input-number>` -->
        </el-input-number>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="折扣率" prop="discountRate" v-if="form.couponType === 2">
        <!-- [zh] 开始标签 `<el-input-number>` -->
        <el-input-number v-model="form.discountRate" :min="0" :max="1" :precision="2" :step="0.05" style="width: 200px">
        <!-- [zh] 闭合标签 `</el-input-number>` -->
        </el-input-number>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="form-tip">如0.85表示85折</div>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="发放总量" prop="totalCount">
        <!-- [zh] 开始标签 `<el-input-number>` -->
        <el-input-number v-model="form.totalCount" :min="0" :step="1" style="width: 200px"></el-input-number>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="form-tip">0表示不限量</div>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="有效期开始时间" prop="validStartTime">
        <!-- [zh] 开始标签 `<el-date-picker>` -->
        <el-date-picker
          v-model="form.validStartTime"
          type="datetime"
          placeholder="选择开始时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 200px"
        />
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="有效期结束时间" prop="validEndTime">
        <!-- [zh] 开始标签 `<el-date-picker>` -->
        <el-date-picker
          v-model="form.validEndTime"
          type="datetime"
          placeholder="选择结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 200px"
        />
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="上架秒杀" prop="rushingstatus">
        <!-- [zh] 开始标签 `<el-switch>` -->
        <el-switch v-model="form.rushingstatus" :active-value="1" :inactive-value="0"></el-switch>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-if="form.rushingstatus === 1">
        <!-- [zh] 开始标签 `<el-form-item>` -->
        <el-form-item label="秒杀开始时间" prop="rushingStartTime">
          <!-- [zh] 开始标签 `<el-date-picker>` -->
          <el-date-picker
            v-model="form.rushingStartTime"
            type="datetime"
            placeholder="选择秒杀开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 200px"
          />
        <!-- [zh] 闭合标签 `</el-form-item>` -->
        </el-form-item>
        <!-- [zh] 开始标签 `<el-form-item>` -->
        <el-form-item label="秒杀结束时间" prop="rushingEndTime">
          <!-- [zh] 开始标签 `<el-date-picker>` -->
          <el-date-picker
            v-model="form.rushingEndTime"
            type="datetime"
            placeholder="选择秒杀结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 200px"
          />
        <!-- [zh] 闭合标签 `</el-form-item>` -->
        </el-form-item>
      </template>
    </el-form>
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #footer>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button @click="dialogVisible = false">取消</el-button>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, computed } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()

// [zh] 声明变量 `columns`
const columns = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '优惠券ID',`
    label: '优惠券ID',
    // [zh] TypeScript/JS 语句：`prop: 'couponId',`
    prop: 'couponId',
    // [zh] TypeScript/JS 语句：`width: 150,`
    width: 150,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '优惠券名称',`
    label: '优惠券名称',
    // [zh] TypeScript/JS 语句：`prop: 'couponName',`
    prop: 'couponName',
    // [zh] TypeScript/JS 语句：`width: 180,`
    width: 180,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '类型',`
    label: '类型',
    // [zh] TypeScript/JS 语句：`prop: 'couponType',`
    prop: 'couponType',
    // [zh] TypeScript/JS 语句：`width: 100,`
    width: 100,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotType',`
    scopedSlots: 'slotType',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '门槛金额',`
    label: '门槛金额',
    // [zh] TypeScript/JS 语句：`prop: 'thresholdAmount',`
    prop: 'thresholdAmount',
    // [zh] TypeScript/JS 语句：`width: 100,`
    width: 100,
    // [zh] TypeScript/JS 语句：`formatter: (row) => row.thresholdAmount == 0 ? '无门槛' : `
    formatter: (row) => row.thresholdAmount == 0 ? '无门槛' : '¥' + row.thresholdAmount,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '优惠金额/折扣',`
    label: '优惠金额/折扣',
    // [zh] TypeScript/JS 语句：`prop: 'discountAmount',`
    prop: 'discountAmount',
    // [zh] TypeScript/JS 语句：`width: 140,`
    width: 140,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotAmount',`
    scopedSlots: 'slotAmount',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '剩余/总量',`
    label: '剩余/总量',
    // [zh] TypeScript/JS 语句：`prop: 'remainCount',`
    prop: 'remainCount',
    // [zh] TypeScript/JS 语句：`width: 120,`
    width: 120,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotStock',`
    scopedSlots: 'slotStock',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '秒杀',`
    label: '秒杀',
    // [zh] TypeScript/JS 语句：`prop: 'rushingstatus',`
    prop: 'rushingstatus',
    // [zh] TypeScript/JS 语句：`width: 80,`
    width: 80,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotRush',`
    scopedSlots: 'slotRush',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '状态',`
    label: '状态',
    // [zh] TypeScript/JS 语句：`prop: 'status',`
    prop: 'status',
    // [zh] TypeScript/JS 语句：`width: 100,`
    width: 100,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotStatus',`
    scopedSlots: 'slotStatus',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '有效期',`
    label: '有效期',
    // [zh] TypeScript/JS 语句：`prop: 'validStartTime',`
    prop: 'validStartTime',
    // [zh] TypeScript/JS 语句：`width: 280,`
    width: 280,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotValidTime',`
    scopedSlots: 'slotValidTime',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '操作',`
    label: '操作',
    // [zh] TypeScript/JS 语句：`prop: 'op',`
    prop: 'op',
    // [zh] TypeScript/JS 语句：`width: 320,`
    width: 320,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotOp',`
    scopedSlots: 'slotOp',
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `warmupAllRush`
const warmupAllRush = () => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: '将为全部秒杀券预热 Redis 库存（以 DB 剩余量为准），确定继续？',`
    message: '将为全部秒杀券预热 Redis 库存（以 DB 剩余量为准），确定继续？',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.warmupRushStock,`
        url: proxy.Api.warmupRushStock,
        // [zh] TypeScript/JS 语句：`params: {},`
        params: {},
        // [zh] TypeScript/JS 语句：`showLoading: true,`
        showLoading: true,
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success(`预热完成，共 ${result.data ?? 0} 张券`)`
      proxy.Message.success(`预热完成，共 ${result.data ?? 0} 张券`)
      // [zh] TypeScript/JS 语句：`loadDataList()`
      loadDataList()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `reconcileAllRush`
const reconcileAllRush = () => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: '将以 DB 剩余库存为准对全部秒杀券 Redis 对账，确定继续？',`
    message: '将以 DB 剩余库存为准对全部秒杀券 Redis 对账，确定继续？',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.reconcileRushStock,`
        url: proxy.Api.reconcileRushStock,
        // [zh] TypeScript/JS 语句：`params: {},`
        params: {},
        // [zh] TypeScript/JS 语句：`showLoading: true,`
        showLoading: true,
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] 声明变量 `list`
      const list = result.data || []
      // [zh] TypeScript/JS 语句：`proxy.Message.success(`对账完成，共处理 ${Array.isArray(list) ?`
      proxy.Message.success(`对账完成，共处理 ${Array.isArray(list) ? list.length : 0} 张券`)
      // [zh] TypeScript/JS 语句：`loadDataList()`
      loadDataList()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `warmupOne`
const warmupOne = (row) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `预热券「${row.couponName}」的 Redis 秒杀库存？`,`
    message: `预热券「${row.couponName}」的 Redis 秒杀库存？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.warmupRushStock,`
        url: proxy.Api.warmupRushStock,
        // [zh] TypeScript/JS 语句：`params: { couponId: row.couponId },`
        params: { couponId: row.couponId },
        // [zh] TypeScript/JS 语句：`showLoading: true,`
        showLoading: true,
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('预热成功')`
      proxy.Message.success('预热成功')
      // [zh] TypeScript/JS 语句：`loadDataList()`
      loadDataList()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `reconcileOne`
const reconcileOne = (row) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `对券「${row.couponName}」执行 Redis/DB 库存对账？`,`
    message: `对券「${row.couponName}」执行 Redis/DB 库存对账？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.reconcileRushStock,`
        url: proxy.Api.reconcileRushStock,
        // [zh] TypeScript/JS 语句：`params: { couponId: row.couponId },`
        params: { couponId: row.couponId },
        // [zh] TypeScript/JS 语句：`showLoading: true,`
        showLoading: true,
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] 声明变量 `data`
      const data = result.data
      // [zh] 声明变量 `msg`
      const msg = data?.redisStockAfter != null
        // [zh] TypeScript/JS 语句：`? `DB=${data.dbRemainCount}，Redis ${data.redisStockBefo`
        ? `DB=${data.dbRemainCount}，Redis ${data.redisStockBefore}→${data.redisStockAfter}`
        // [zh] TypeScript/JS 语句：`: '对账完成'`
        : '对账完成'
      // [zh] TypeScript/JS 语句：`proxy.Message.success(msg)`
      proxy.Message.success(msg)
      // [zh] TypeScript/JS 语句：`loadDataList()`
      loadDataList()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `tableInfoRef`
const tableInfoRef = ref()
// [zh] 声明变量 `searchForm`
const searchForm = ref({})
// [zh] 声明变量 `tableData`
const tableData = ref({})
// [zh] 声明变量 `dialogVisible`
const dialogVisible = ref(false)
// [zh] 声明变量 `formRef`
const formRef = ref()
// [zh] 声明变量 `currentCouponId`
const currentCouponId = ref(null)

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

// [zh] 声明变量 `loadDataList`
const loadDataList = async () => {
  // [zh] 声明变量 `params`
  let params = {
    // [zh] TypeScript/JS 语句：`pageNo: tableData.value.pageNo,`
    pageNo: tableData.value.pageNo,
    // [zh] TypeScript/JS 语句：`pageSize: tableData.value.pageSize,`
    pageSize: tableData.value.pageSize,
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`Object.assign(params, searchForm.value)`
  Object.assign(params, searchForm.value)
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadDiscountCoupon,`
    url: proxy.Api.loadDiscountCoupon,
    // [zh] TypeScript/JS 语句：`params: params,`
    params: params,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`Object.assign(tableData.value, result.data)`
  Object.assign(tableData.value, result.data)
// [zh] 语法符号
}

// [zh] 声明变量 `toggleStatus`
const toggleStatus = (row) => {
  // [zh] 代码块开始
  proxy.ConfirmSensitive({
    // [zh] TypeScript/JS 语句：`message: `确定要【${row.status == 0 ? '启用' : '停用'}】吗？`,`
    message: `确定要【${row.status == 0 ? '启用' : '停用'}】吗？`,
    // [zh] 代码块开始
    okfun: async (sensitiveConfirmToken) => {
      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.updateDiscountCouponStatus,`
        url: proxy.Api.updateDiscountCouponStatus,
        // [zh] TypeScript/JS 语句：`sensitiveConfirmToken,`
        sensitiveConfirmToken,
        // [zh] 代码块开始
        params: {
          // [zh] TypeScript/JS 语句：`couponId: row.couponId,`
          couponId: row.couponId,
          // [zh] TypeScript/JS 语句：`status: row.status == 0 ? 1 : 0,`
          status: row.status == 0 ? 1 : 0,
        // [zh] TypeScript/JS 语句：`},`
        },
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) {
        // [zh] TypeScript/JS 语句：`return`
        return
      // [zh] 语法符号
      }
      // [zh] TypeScript/JS 语句：`proxy.Message.success('操作成功')`
      proxy.Message.success('操作成功')
      // [zh] TypeScript/JS 语句：`loadDataList()`
      loadDataList()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `showEdit`
const showEdit = async (couponId) => {
  // [zh] TypeScript/JS 语句：`currentCouponId.value = couponId`
  currentCouponId.value = couponId
  // [zh] TypeScript/JS 语句：`dialogVisible.value = true`
  dialogVisible.value = true
  // [zh] 条件判断 if
  if (formRef.value) {
    // [zh] TypeScript/JS 语句：`formRef.value.clearValidate()`
    formRef.value.clearValidate()
  // [zh] 语法符号
  }

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
    let result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.getDiscountCouponInfo,`
      url: proxy.Api.getDiscountCouponInfo,
      // [zh] 代码块开始
      params: {
        // [zh] TypeScript/JS 语句：`couponId: couponId,`
        couponId: couponId,
      // [zh] TypeScript/JS 语句：`},`
      },
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
    // [zh] 声明变量 `data`
    const data = result.data
    // [zh] TypeScript/JS 语句：`Object.assign(form, data)`
    Object.assign(form, data)
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `onCouponTypeChange`
const onCouponTypeChange = () => {

  // [zh] 条件判断 if
  if (form.couponType === 2) {
    // [zh] TypeScript/JS 语句：`form.discountAmount = 0`
    form.discountAmount = 0
  // [zh] 代码块开始
  } else {
    // [zh] TypeScript/JS 语句：`form.discountRate = 0.9`
    form.discountRate = 0.9
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (form.couponType === 3) {
    // [zh] TypeScript/JS 语句：`form.thresholdAmount = 0`
    form.thresholdAmount = 0
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `handleSave`
const handleSave = async () => {
  // [zh] 条件判断 if
  if (!formRef.value) return
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await formRef.value.validate(async (valid) => {
    // [zh] 条件判断 if
    if (valid) {
      // [zh] 声明变量 `url`
      let url = proxy.Api.saveDiscountCoupon

      // [zh] 声明变量 `params`
      const params = {
        // [zh] TypeScript/JS 语句：`...form,`
        ...form,
      // [zh] 语法符号
      }

      // [zh] 条件判断 if
      if (currentCouponId.value) {
        // [zh] TypeScript/JS 语句：`params.couponId = currentCouponId.value`
        params.couponId = currentCouponId.value
      // [zh] 语法符号
      }

      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: url,`
        url: url,
        // [zh] TypeScript/JS 语句：`params: params,`
        params: params,
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 条件判断 if
      if (!result) {
        // [zh] TypeScript/JS 语句：`return`
        return
      // [zh] 语法符号
      }
      // [zh] TypeScript/JS 语句：`proxy.Message.success('保存成功')`
      proxy.Message.success('保存成功')
      // [zh] TypeScript/JS 语句：`dialogVisible.value = false`
      dialogVisible.value = false
      // [zh] TypeScript/JS 语句：`loadDataList()`
      loadDataList()
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.table-panel {` */
.table-panel {
  /* [zh] 样式规则 `height: calc(100vh - 135px);` */
  height: calc(100vh - 135px);
/* [zh] 样式规则 `}` */
}
/* [zh] 样式规则 `.form-tip {` */
.form-tip {
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: #999;` */
  color: #999;
  /* [zh] 样式规则 `margin-left: 10px;` */
  margin-left: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.toolbar-actions {` */
.toolbar-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.text-muted {` */
.text-muted {
  /* [zh] 样式规则 `color: #999;` */
  color: #999;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
/* [zh] 样式规则 `}` */
}
</style>
