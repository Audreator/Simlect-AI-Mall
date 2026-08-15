<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-dialog>` -->
  <el-dialog v-model="visible" title="处理举报" width="500px" destroy-on-close @closed="reset">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="report-detail">
      <!-- [zh] 开始标签 `<p>` -->
      <p class="detail-row"><span class="label">订单号：</span>{{ form.orderId }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="detail-row"><span class="label">举报人：</span>{{ form.reporterUserId }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="detail-row"><span class="label">举报理由：</span>{{ form.reason }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="detail-row"><span class="label">补充说明：</span>{{ form.detail || '无' }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="detail-row"><span class="label">评论内容：</span>{{ form.commentSnapshot || '无' }}</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="form" label-position="top" style="margin-top: 16px;">
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="处理备注">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input
          v-model="form.handleRemark"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入处理说明"
        />
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
    <!-- [zh] 闭合标签 `</el-form>` -->
    </el-form>

    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #footer>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="footer-actions">
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button @click="visible = false">取消</el-button>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="danger" :loading="submitting" @click="submit(2)">驳回举报</el-button>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" :loading="submitting" @click="submit(1)">确认处理</el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>
  </el-dialog>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { reactive, ref, getCurrentInstance } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `emit`
const emit = defineEmits(['reload'])

// [zh] 声明变量 `visible`
const visible = ref(false)
// [zh] 声明变量 `submitting`
const submitting = ref(false)

// [zh] 声明变量 `form`
const form = reactive({
  // [zh] TypeScript/JS 语句：`reportId: '',`
  reportId: '',
  // [zh] TypeScript/JS 语句：`orderId: '',`
  orderId: '',
  // [zh] TypeScript/JS 语句：`reporterUserId: '',`
  reporterUserId: '',
  // [zh] TypeScript/JS 语句：`reason: '',`
  reason: '',
  // [zh] TypeScript/JS 语句：`detail: '',`
  detail: '',
  // [zh] TypeScript/JS 语句：`commentSnapshot: '',`
  commentSnapshot: '',
  // [zh] TypeScript/JS 语句：`handleRemark: ''`
  handleRemark: ''
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `reset`
const reset = () => {
  // [zh] 代码块开始
  Object.assign(form, {
    // [zh] TypeScript/JS 语句：`reportId: '',`
    reportId: '',
    // [zh] TypeScript/JS 语句：`orderId: '',`
    orderId: '',
    // [zh] TypeScript/JS 语句：`reporterUserId: '',`
    reporterUserId: '',
    // [zh] TypeScript/JS 语句：`reason: '',`
    reason: '',
    // [zh] TypeScript/JS 语句：`detail: '',`
    detail: '',
    // [zh] TypeScript/JS 语句：`commentSnapshot: '',`
    commentSnapshot: '',
    // [zh] TypeScript/JS 语句：`handleRemark: ''`
    handleRemark: ''
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `show`
const show = (row) => {
  // [zh] TypeScript/JS 语句：`reset()`
  reset()
  // [zh] TypeScript/JS 语句：`form.reportId = row.reportId`
  form.reportId = row.reportId
  // [zh] TypeScript/JS 语句：`form.orderId = row.orderId || ''`
  form.orderId = row.orderId || ''
  // [zh] TypeScript/JS 语句：`form.reporterUserId = row.reporterUserId || ''`
  form.reporterUserId = row.reporterUserId || ''
  // [zh] TypeScript/JS 语句：`form.reason = row.reason || ''`
  form.reason = row.reason || ''
  // [zh] TypeScript/JS 语句：`form.detail = row.detail || ''`
  form.detail = row.detail || ''
  // [zh] TypeScript/JS 语句：`form.commentSnapshot = row.commentSnapshot || ''`
  form.commentSnapshot = row.commentSnapshot || ''
  // [zh] TypeScript/JS 语句：`visible.value = true`
  visible.value = true
// [zh] 语法符号
}

// [zh] 声明变量 `submit`
const submit = async (status) => {
  // [zh] TypeScript/JS 语句：`submitting.value = true`
  submitting.value = true
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `result`
    const result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.handleCommentReport,`
      url: proxy.Api.handleCommentReport,
      // [zh] 代码块开始
      params: {
        // [zh] TypeScript/JS 语句：`reportId: form.reportId,`
        reportId: form.reportId,
        // [zh] TypeScript/JS 语句：`status: status,`
        status: status,
        // [zh] TypeScript/JS 语句：`handleRemark: form.handleRemark || undefined`
        handleRemark: form.handleRemark || undefined
      // [zh] 语法符号
      }
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 条件判断 if
    if (!result) return
    // [zh] TypeScript/JS 语句：`proxy.Message.success(status === 1 ? '已确认处理' : '已驳回举报')`
    proxy.Message.success(status === 1 ? '已确认处理' : '已驳回举报')
    // [zh] TypeScript/JS 语句：`visible.value = false`
    visible.value = false
    // [zh] TypeScript/JS 语句：`emit('reload')`
    emit('reload')
  // [zh] 代码块开始
  } finally {
    // [zh] TypeScript/JS 语句：`submitting.value = false`
    submitting.value = false
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] TypeScript/JS 语句：`defineExpose({ show })`
defineExpose({ show })
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.report-detail {` */
.report-detail {
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;
  /* [zh] 样式规则 `border-radius: 8px;` */
  border-radius: 8px;
  /* [zh] 样式规则 `background: #f5f5f7;` */
  background: #f5f5f7;
  /* [zh] 样式规则 `border: 1px solid #e8e8ed;` */
  border: 1px solid #e8e8ed;

  /* [zh] 样式规则 `.detail-row {` */
  .detail-row {
    /* [zh] 样式规则 `margin: 0 0 8px;` */
    margin: 0 0 8px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `line-height: 1.5;` */
    line-height: 1.5;
    /* [zh] 样式规则 `color: var(--text);` */
    color: var(--text);
    /* [zh] 样式规则 `word-break: break-all;` */
    word-break: break-all;

    /* [zh] 样式规则 `&:last-child {` */
    &:last-child {
      /* [zh] 样式规则 `margin-bottom: 0;` */
      margin-bottom: 0;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.label {` */
    .label {
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `color: var(--text2);` */
      color: var(--text2);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.footer-actions {` */
.footer-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}
</style>
