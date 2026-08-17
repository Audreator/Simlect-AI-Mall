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

<script setup>
import { reactive, ref, computed, getCurrentInstance } from 'vue'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['reload'])

const visible = ref(false)
const submitting = ref(false)
const unbanning = ref(false)
const tempBanned = ref(false)
const unbanAt = ref(null)
const recordStatus = ref(null)

const form = reactive({
  recordId: null,
  userId: '',
  orderId: '',
  userIp: '',
  scene: '',
  conclusion: '',
  imagePath: '',
  createTime: '',
  handleRemark: ''
})

const isPending = computed(() => recordStatus.value === 0)

const formatUnbanTime = (ms) => {
  const d = new Date(ms)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const unbanTimeText = computed(() => (unbanAt.value ? formatUnbanTime(unbanAt.value) : '—'))

const sceneLabel = (s) => (s === 'avatar' ? '头像' : s === 'comment' ? '评论' : s || '—')
const imageUrl = (path) => `${proxy.Api.sourcePath}${encodeURIComponent(path)}`

const reset = () => {
  Object.assign(form, {
    recordId: null,
    userId: '',
    orderId: '',
    userIp: '',
    scene: '',
    conclusion: '',
    imagePath: '',
    createTime: '',
    handleRemark: ''
  })
  tempBanned.value = false
  unbanAt.value = null
  recordStatus.value = null
}

const loadTempBanInfo = async (userId) => {
  if (!userId) {
    tempBanned.value = false
    unbanAt.value = null
    return
  }
  const result = await proxy.Request({
    url: proxy.Api.imageModerationGetTempBanInfo,
    params: { userId },
    showLoading: false,
    showError: false
  })
  if (!result) {
    tempBanned.value = false
    unbanAt.value = null
    return
  }
  const data = result.data || {}
  tempBanned.value = !!data.tempBanned
  unbanAt.value = data.unbanAt || null
}

const show = async (row) => {
  reset()
  form.recordId = row.recordId
  form.userId = row.userId || ''
  form.orderId = row.orderId || ''
  form.userIp = row.userIp || ''
  form.scene = row.scene || ''
  form.conclusion = row.conclusion || ''
  form.imagePath = row.imagePath || ''
  form.createTime = row.createTime || ''
  recordStatus.value = row.status
  visible.value = true
  await loadTempBanInfo(form.userId)
}

const doUnban = () => {
  proxy.Confirm({
    message: `确认立即解封用户 ${form.userId}？解封时间原为 ${unbanTimeText.value}`,
    okfun: async () => {
      unbanning.value = true
      try {
        const result = await proxy.Request({
          url: proxy.Api.imageModerationUnbanUser,
          params: { userId: form.userId }
        })
        if (!result) return
        proxy.Message.success('已解封')
        tempBanned.value = false
        unbanAt.value = null
        emit('reload')
      } finally {
        unbanning.value = false
      }
    }
  })
}

const submit = async (action) => {
  const tips = {
    approve: '确认标记为通过？',
    dismiss: '确认误报驳回？',
    ban_temp: '确认对该用户临时封禁2小时？',
    ban_perm: '确认永久封禁该用户？'
  }
  proxy.Confirm({
    message: tips[action] || '确认处理？',
    okfun: async () => {
      submitting.value = true
      try {
        const result = await proxy.Request({
          url: proxy.Api.imageModerationHandleReview,
          params: {
            recordId: form.recordId,
            action,
            handleRemark: form.handleRemark || undefined
          }
        })
        if (!result) return
        proxy.Message.success('处理成功')
        if (action === 'ban_temp') {
          await loadTempBanInfo(form.userId)
        } else {
          visible.value = false
        }
        emit('reload')
      } finally {
        submitting.value = false
      }
    }
  })
}

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
