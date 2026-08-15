<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-dialog>` -->
  <el-dialog v-model="visible" title="图片违规复核" width="560px" destroy-on-close @closed="reset">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="mod-detail">
      <!-- [zh] 开始标签 `<p>` -->
      <p class="detail-row"><span class="label">用户ID：</span>{{ form.userId }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="form.orderId" class="detail-row"><span class="label">订单ID：</span>{{ form.orderId }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="detail-row"><span class="label">IP：</span>{{ form.userIp || '—' }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="detail-row"><span class="label">场景：</span>{{ sceneLabel(form.scene) }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="detail-row"><span class="label">百度结论：</span>{{ form.conclusion || '—' }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="detail-row"><span class="label">上传时间：</span>{{ form.createTime || '—' }}</p>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-if="tempBanned" class="detail-row ban-row">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="label">封禁状态：</span>
        <!-- [zh] 开始标签 `<span>` -->
        <span class="ban-text">临时封禁中，解封时间 {{ unbanTimeText }}</span>
      <!-- [zh] 闭合标签 `</p>` -->
      </p>
      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="form.imagePath" class="img-wrap">
        <!-- [zh] 开始标签 `<img>` -->
        <img :src="imageUrl(form.imagePath)" alt="待审核图片" />
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form v-if="isPending" :model="form" label-position="top" style="margin-top: 16px;">
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="处理备注">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input
          v-model="form.handleRemark"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="选填，记录处理说明"
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
        <el-button @click="visible = false">关闭</el-button>
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button
          v-if="tempBanned"
          type="primary"
          plain
          :loading="unbanning"
          @click="doUnban"
        >
          <!-- [zh] 模板内容：`立即解封` -->
          立即解封
        <!-- [zh] 闭合标签 `</el-button>` -->
        </el-button>
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-if="isPending">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button :loading="submitting" @click="submit('dismiss')">误报驳回</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="success" :loading="submitting" @click="submit('approve')">确认通过</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="warning" :loading="submitting" @click="submit('ban_temp')">封禁2小时</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="danger" :loading="submitting" @click="submit('ban_perm')">永久封禁</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { reactive, ref, computed, getCurrentInstance } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `emit`
const emit = defineEmits(['reload'])

// [zh] 声明变量 `visible`
const visible = ref(false)
// [zh] 声明变量 `submitting`
const submitting = ref(false)
// [zh] 声明变量 `unbanning`
const unbanning = ref(false)
// [zh] 声明变量 `tempBanned`
const tempBanned = ref(false)
// [zh] 声明变量 `unbanAt`
const unbanAt = ref(null)
// [zh] 声明变量 `recordStatus`
const recordStatus = ref(null)

// [zh] 声明变量 `form`
const form = reactive({
  // [zh] TypeScript/JS 语句：`recordId: null,`
  recordId: null,
  // [zh] TypeScript/JS 语句：`userId: '',`
  userId: '',
  // [zh] TypeScript/JS 语句：`orderId: '',`
  orderId: '',
  // [zh] TypeScript/JS 语句：`userIp: '',`
  userIp: '',
  // [zh] TypeScript/JS 语句：`scene: '',`
  scene: '',
  // [zh] TypeScript/JS 语句：`conclusion: '',`
  conclusion: '',
  // [zh] TypeScript/JS 语句：`imagePath: '',`
  imagePath: '',
  // [zh] TypeScript/JS 语句：`createTime: '',`
  createTime: '',
  // [zh] TypeScript/JS 语句：`handleRemark: ''`
  handleRemark: ''
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `isPending`
const isPending = computed(() => recordStatus.value === 0)

// [zh] 声明变量 `formatUnbanTime`
const formatUnbanTime = (ms) => {
  // [zh] 声明变量 `d`
  const d = new Date(ms)
  // [zh] 声明变量 `pad`
  const pad = (n) => String(n).padStart(2, '0')
  // [zh] return 返回
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
// [zh] 语法符号
}

// [zh] 声明变量 `unbanTimeText`
const unbanTimeText = computed(() => (unbanAt.value ? formatUnbanTime(unbanAt.value) : '—'))

// [zh] 声明变量 `sceneLabel`
const sceneLabel = (s) => (s === 'avatar' ? '头像' : s === 'comment' ? '评论' : s || '—')
// [zh] 声明变量 `imageUrl`
const imageUrl = (path) => `${proxy.Api.sourcePath}${encodeURIComponent(path)}`

// [zh] 声明变量 `reset`
const reset = () => {
  // [zh] 代码块开始
  Object.assign(form, {
    // [zh] TypeScript/JS 语句：`recordId: null,`
    recordId: null,
    // [zh] TypeScript/JS 语句：`userId: '',`
    userId: '',
    // [zh] TypeScript/JS 语句：`orderId: '',`
    orderId: '',
    // [zh] TypeScript/JS 语句：`userIp: '',`
    userIp: '',
    // [zh] TypeScript/JS 语句：`scene: '',`
    scene: '',
    // [zh] TypeScript/JS 语句：`conclusion: '',`
    conclusion: '',
    // [zh] TypeScript/JS 语句：`imagePath: '',`
    imagePath: '',
    // [zh] TypeScript/JS 语句：`createTime: '',`
    createTime: '',
    // [zh] TypeScript/JS 语句：`handleRemark: ''`
    handleRemark: ''
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] TypeScript/JS 语句：`tempBanned.value = false`
  tempBanned.value = false
  // [zh] TypeScript/JS 语句：`unbanAt.value = null`
  unbanAt.value = null
  // [zh] TypeScript/JS 语句：`recordStatus.value = null`
  recordStatus.value = null
// [zh] 语法符号
}

// [zh] 声明变量 `loadTempBanInfo`
const loadTempBanInfo = async (userId) => {
  // [zh] 条件判断 if
  if (!userId) {
    // [zh] TypeScript/JS 语句：`tempBanned.value = false`
    tempBanned.value = false
    // [zh] TypeScript/JS 语句：`unbanAt.value = null`
    unbanAt.value = null
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.imageModerationGetTempBanInfo,`
    url: proxy.Api.imageModerationGetTempBanInfo,
    // [zh] TypeScript/JS 语句：`params: { userId },`
    params: { userId },
    // [zh] TypeScript/JS 语句：`showLoading: false,`
    showLoading: false,
    // [zh] TypeScript/JS 语句：`showError: false`
    showError: false
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) {
    // [zh] TypeScript/JS 语句：`tempBanned.value = false`
    tempBanned.value = false
    // [zh] TypeScript/JS 语句：`unbanAt.value = null`
    unbanAt.value = null
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 声明变量 `data`
  const data = result.data || {}
  // [zh] TypeScript/JS 语句：`tempBanned.value = !!data.tempBanned`
  tempBanned.value = !!data.tempBanned
  // [zh] TypeScript/JS 语句：`unbanAt.value = data.unbanAt || null`
  unbanAt.value = data.unbanAt || null
// [zh] 语法符号
}

// [zh] 声明变量 `show`
const show = async (row) => {
  // [zh] TypeScript/JS 语句：`reset()`
  reset()
  // [zh] TypeScript/JS 语句：`form.recordId = row.recordId`
  form.recordId = row.recordId
  // [zh] TypeScript/JS 语句：`form.userId = row.userId || ''`
  form.userId = row.userId || ''
  // [zh] TypeScript/JS 语句：`form.orderId = row.orderId || ''`
  form.orderId = row.orderId || ''
  // [zh] TypeScript/JS 语句：`form.userIp = row.userIp || ''`
  form.userIp = row.userIp || ''
  // [zh] TypeScript/JS 语句：`form.scene = row.scene || ''`
  form.scene = row.scene || ''
  // [zh] TypeScript/JS 语句：`form.conclusion = row.conclusion || ''`
  form.conclusion = row.conclusion || ''
  // [zh] TypeScript/JS 语句：`form.imagePath = row.imagePath || ''`
  form.imagePath = row.imagePath || ''
  // [zh] TypeScript/JS 语句：`form.createTime = row.createTime || ''`
  form.createTime = row.createTime || ''
  // [zh] TypeScript/JS 语句：`recordStatus.value = row.status`
  recordStatus.value = row.status
  // [zh] TypeScript/JS 语句：`visible.value = true`
  visible.value = true
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await loadTempBanInfo(form.userId)
// [zh] 语法符号
}

// [zh] 声明变量 `doUnban`
const doUnban = () => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确认立即解封用户 ${form.userId}？解封时间原为 ${unbanTimeTex`
    message: `确认立即解封用户 ${form.userId}？解封时间原为 ${unbanTimeText.value}`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] TypeScript/JS 语句：`unbanning.value = true`
      unbanning.value = true
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `result`
        const result = await proxy.Request({
          // [zh] TypeScript/JS 语句：`url: proxy.Api.imageModerationUnbanUser,`
          url: proxy.Api.imageModerationUnbanUser,
          // [zh] TypeScript/JS 语句：`params: { userId: form.userId }`
          params: { userId: form.userId }
        // [zh] TypeScript/JS 语句：`})`
        })
        // [zh] 条件判断 if
        if (!result) return
        // [zh] TypeScript/JS 语句：`proxy.Message.success('已解封')`
        proxy.Message.success('已解封')
        // [zh] TypeScript/JS 语句：`tempBanned.value = false`
        tempBanned.value = false
        // [zh] TypeScript/JS 语句：`unbanAt.value = null`
        unbanAt.value = null
        // [zh] TypeScript/JS 语句：`emit('reload')`
        emit('reload')
      // [zh] 代码块开始
      } finally {
        // [zh] TypeScript/JS 语句：`unbanning.value = false`
        unbanning.value = false
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `submit`
const submit = async (action) => {
  // [zh] 声明变量 `tips`
  const tips = {
    // [zh] TypeScript/JS 语句：`approve: '确认标记为通过？',`
    approve: '确认标记为通过？',
    // [zh] TypeScript/JS 语句：`dismiss: '确认误报驳回？',`
    dismiss: '确认误报驳回？',
    // [zh] TypeScript/JS 语句：`ban_temp: '确认对该用户临时封禁2小时？',`
    ban_temp: '确认对该用户临时封禁2小时？',
    // [zh] TypeScript/JS 语句：`ban_perm: '确认永久封禁该用户？'`
    ban_perm: '确认永久封禁该用户？'
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: tips[action] || '确认处理？',`
    message: tips[action] || '确认处理？',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] TypeScript/JS 语句：`submitting.value = true`
      submitting.value = true
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `result`
        const result = await proxy.Request({
          // [zh] TypeScript/JS 语句：`url: proxy.Api.imageModerationHandleReview,`
          url: proxy.Api.imageModerationHandleReview,
          // [zh] 代码块开始
          params: {
            // [zh] TypeScript/JS 语句：`recordId: form.recordId,`
            recordId: form.recordId,
            // [zh] TypeScript/JS 语句：`action,`
            action,
            // [zh] TypeScript/JS 语句：`handleRemark: form.handleRemark || undefined`
            handleRemark: form.handleRemark || undefined
          // [zh] 语法符号
          }
        // [zh] TypeScript/JS 语句：`})`
        })
        // [zh] 条件判断 if
        if (!result) return
        // [zh] TypeScript/JS 语句：`proxy.Message.success('处理成功')`
        proxy.Message.success('处理成功')
        // [zh] 条件判断 if
        if (action === 'ban_temp') {
          // [zh] await 等待 Promise（类比 CompletableFuture.get）
          await loadTempBanInfo(form.userId)
        // [zh] 代码块开始
        } else {
          // [zh] TypeScript/JS 语句：`visible.value = false`
          visible.value = false
        // [zh] 语法符号
        }
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
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] TypeScript/JS 语句：`defineExpose({ show })`
defineExpose({ show })
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.mod-detail {` */
.mod-detail {
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
    /* [zh] 样式规则 `word-break: break-all;` */
    word-break: break-all;

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

  /* [zh] 样式规则 `.ban-row .ban-text {` */
  .ban-row .ban-text {
    /* [zh] 样式规则 `color: #ff3b30;` */
    color: #ff3b30;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.img-wrap {` */
  .img-wrap {
    /* [zh] 样式规则 `margin-top: 10px;` */
    margin-top: 10px;
    /* [zh] 样式规则 `border-radius: 8px;` */
    border-radius: 8px;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
    /* [zh] 样式规则 `background: #fff;` */
    background: #fff;

    /* [zh] 样式规则 `img {` */
    img {
      /* [zh] 样式规则 `display: block;` */
      display: block;
      /* [zh] 样式规则 `max-width: 100%;` */
      max-width: 100%;
      /* [zh] 样式规则 `max-height: 280px;` */
      max-height: 280px;
      /* [zh] 样式规则 `margin: 0 auto;` */
      margin: 0 auto;
      /* [zh] 样式规则 `object-fit: contain;` */
      object-fit: contain;
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
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `justify-content: flex-end;` */
  justify-content: flex-end;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}
</style>
