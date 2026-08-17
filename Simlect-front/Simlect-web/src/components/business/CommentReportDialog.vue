<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-dialog>` -->
  <el-dialog
    v-model="visible"
    title="举报评论"
    width="92%"
    :style="{ maxWidth: '440px' }"
    destroy-on-close
    @closed="reset"
  >
    <!-- [zh] 开始标签 `<p>` -->
    <p v-if="snapshot" class="report-target">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="target-label">举报内容</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span class="target-text">{{ snapshot }}</span>
    <!-- [zh] 闭合标签 `</p>` -->
    </p>

    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="举报理由" prop="reason">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="reason-grid">
          <!-- [zh] 开始标签 `<button>` -->
          <button
            v-for="r in reasons"
            :key="r"
            type="button"
            class="reason-chip"
            :class="{ active: form.reason === r }"
            @click="form.reason = r"
          >
            <!-- [zh] Mustache 插值表达式 -->
            {{ r }}
          <!-- [zh] 闭合标签 `</button>` -->
          </button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="补充说明（选填）">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input
          v-model="form.detail"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="可补充具体问题，便于平台核实"
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
        <el-button type="primary" :loading="submitting" @click="submit">提交举报</el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { commentReportApi } from '@/api/modules';
import { toast } from '@/utils/toast';

const reasons = ['广告/垃圾信息', '辱骂攻击', '违法违规', '不实信息', '泄露隐私', '其他'];

const visible = ref(false);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const snapshot = ref('');

const form = reactive({
  orderId: '',
  productId: '',
  reason: '',
  detail: ''
});

const rules: FormRules = {
  reason: [{ required: true, message: '请选择举报理由', trigger: 'change' }]
};

const reset = () => {
  form.orderId = '';
  form.productId = '';
  form.reason = '';
  form.detail = '';
  snapshot.value = '';
  formRef.value?.clearValidate();
};

const show = (payload: { orderId: string; productId?: string; commentContent?: string }) => {
  reset();
  form.orderId = String(payload.orderId || '');
  form.productId = String(payload.productId || '');
  snapshot.value = String(payload.commentContent || '').slice(0, 1000);
  visible.value = true;
};

const submit = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    await commentReportApi.submitReport({
      orderId: form.orderId,
      productId: form.productId || undefined,
      reason: form.reason,
      detail: form.detail.trim() || undefined,
      commentSnapshot: snapshot.value || undefined
    });
    toast.success('举报已提交，平台会尽快核实');
    visible.value = false;
  } finally {
    submitting.value = false;
  }
};

defineExpose({ show });
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.report-target {` */
.report-target {
  /* [zh] 样式规则 `margin: 0 0 14px;` */
  margin: 0 0 14px;
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;
  /* [zh] 样式规则 `border-radius: $radius-btn;` */
  border-radius: $radius-btn;
  /* [zh] 样式规则 `background: $color-bg-subtle;` */
  background: $color-bg-subtle;
  /* [zh] 样式规则 `border: 1px solid $color-border-light;` */
  border: 1px solid $color-border-light;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;

  /* [zh] 样式规则 `.target-label {` */
  .target-label {
    /* [zh] 样式规则 `display: block;` */
    display: block;
    /* [zh] 样式规则 `margin-bottom: 4px;` */
    margin-bottom: 4px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.target-text {` */
  .target-text {
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `display: -webkit-box;` */
    display: -webkit-box;
    /* [zh] 样式规则 `-webkit-line-clamp: 3;` */
    -webkit-line-clamp: 3;
    /* [zh] 样式规则 `-webkit-box-orient: vertical;` */
    -webkit-box-orient: vertical;
    /* [zh] 样式规则 `overflow: hidden;` */
    overflow: hidden;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.reason-grid {` */
.reason-grid {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.reason-chip {` */
.reason-chip {
  /* [zh] 样式规则 `padding: 7px 14px;` */
  padding: 7px 14px;
  /* [zh] 样式规则 `border: 1px solid $color-border-gray;` */
  border: 1px solid $color-border-gray;
  /* [zh] 样式规则 `border-radius: $radius-pill;` */
  border-radius: $radius-pill;
  /* [zh] 样式规则 `background: $color-card;` */
  background: $color-card;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: border-color $transition-fas` */
  transition: border-color $transition-fast, color $transition-fast, background $transition-fast;

  /* [zh] 样式规则 `&:hover {` */
  &:hover {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.45)` */
    border-color: rgba($color-primary, 0.45);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.active {` */
  &.active {
    /* [zh] 样式规则 `border-color: $color-primary;` */
    border-color: $color-primary;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `background: $color-primary-soft;` */
    background: $color-primary-soft;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
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
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
/* [zh] 样式规则 `}` */
}
</style>
