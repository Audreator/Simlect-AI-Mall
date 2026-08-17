<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-dialog>` -->
  <el-dialog v-model="visible" title="评价订单" width="92%" :style="{ maxWidth: '480px' }" destroy-on-close @closed="reset">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="星级" prop="star">
        <!-- [zh] 开始标签 `<el-rate>` -->
        <el-rate v-model="form.star" size="large" />
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="评价内容" prop="commentContent">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input
          v-model="form.commentContent"
          type="textarea"
          :rows="4"
          maxlength="300"
          show-word-limit
          placeholder="分享你的购物体验吧"
        />
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="评价图片（选填，最多5张）">
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="imageList.length" class="img-list">
          <!-- [zh] 开始标签 `<div>` -->
          <div v-for="(img, idx) in imageList" :key="idx" class="img-item">
            <!-- [zh] 开始标签 `<img>` -->
            <img :src="toImageSrc(img)" alt="" />
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="img-del" aria-label="删除" @click="removeImage(idx)">×</button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<el-upload>` -->
        <el-upload
          v-if="imageList.length < 5"
          class="upload-trigger"
          :show-file-list="false"
          accept="image/*"
          :http-request="onUpload"
        >
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button plain type="button">{{ imageList.length ? '继续添加' : '添加图片' }}</el-button>
        <!-- [zh] 闭合标签 `</el-upload>` -->
        </el-upload>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
    <!-- [zh] 闭合标签 `</el-form>` -->
    </el-form>
    <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
    <template #footer>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="footer-row">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="footer-actions">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button @click="visible = false">取消</el-button>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" :loading="submitting" @click="submit">提交评价</el-button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    </template>
  </el-dialog>
  <ImageEditorDialog ref="imageEditorRef" />
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus';
import { commentApi, fileApi } from '@/api/modules';
import ImageEditorDialog from '@/components/business/ImageEditorDialog.vue';
import { normalizeCommentImagePath, serializeCommentImagePaths } from '@/utils/commentImagePaths';
import { resolveCommentUploadBlob } from '@/utils/imageUpload';
import { resolveImageUrl } from '@/utils/image';
import { formatUploadErrorMessage } from '@/utils/apiError';
import { toast } from '@/utils/toast';

const emit = defineEmits<{ success: [] }>();

const visible = ref(false);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const imageEditorRef = ref<InstanceType<typeof ImageEditorDialog> | null>(null);
const imageList = ref<string[]>([]);

const form = reactive({
  orderId: '',
  commentContent: '',
  star: 5
});

const rules: FormRules = {
  commentContent: [{ required: true, message: '请输入评价内容', trigger: 'blur' }],
  star: [{ required: true, message: '请选择星级', trigger: 'change' }]
};

const toImageSrc = (path: unknown) => {
  const normalized = normalizeCommentImagePath(path);
  return normalized ? resolveImageUrl(normalized, { useThumbnail: false }) : '';
};

const reset = () => {
  form.orderId = '';
  form.commentContent = '';
  form.star = 5;
  imageList.value = [];
  formRef.value?.clearValidate();
};

const show = (orderId: string) => {
  reset();
  form.orderId = orderId;
  visible.value = true;
};

const onUpload = async (options: UploadRequestOptions) => {
  try {
    const file = options.file as File;
    const blob = await resolveCommentUploadBlob(file, (f) => imageEditorRef.value!.open(f));
    const uploaded = await fileApi.uploadImage(blob, true, 'comment', form.orderId, { skipPrepare: true });
    const path = normalizeCommentImagePath(uploaded);
    if (path) {
      imageList.value.push(path);
      options.onSuccess?.(path);
      if (uploaded.pendingReview) {
        toast.warning('该图片存在违规风险，已提交人工审核，提交评价后将锁定订单直至审核完成');
      }
    } else {
      options.onError?.(new Error('empty') as any);
      toast.error('图片上传失败');
    }
  } catch (e: any) {
    options.onError?.(e as any);
    toast.error(formatUploadErrorMessage(e));
  }
};

const removeImage = (idx: number) => {
  imageList.value.splice(idx, 1);
};

const submit = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const result = await commentApi.postComment({
      orderId: form.orderId,
      commentContent: form.commentContent.trim(),
      star: form.star,
      commentImages: serializeCommentImagePaths(imageList.value) || undefined
    }) as { pendingReview?: boolean };
    if (result?.pendingReview) {
      toast.success('评价已提交，图片审核通过后将自动展示');
    } else {
      toast.success('评价成功');
    }
    visible.value = false;
    emit('success');
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

/* [zh] 样式规则 `.img-list {` */
.img-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.img-item {` */
.img-item {
  /* [zh] 样式规则 `position: relative;` */
  position: relative;
  /* [zh] 样式规则 `width: 72px;` */
  width: 72px;
  /* [zh] 样式规则 `height: 72px;` */
  height: 72px;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `overflow: hidden;` */
  overflow: hidden;

  /* [zh] 样式规则 `img {` */
  img {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 100%;` */
    height: 100%;
    /* [zh] 样式规则 `object-fit: cover;` */
    object-fit: cover;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.img-del {` */
  .img-del {
    /* [zh] 样式规则 `position: absolute;` */
    position: absolute;
    /* [zh] 样式规则 `top: 0;` */
    top: 0;
    /* [zh] 样式规则 `right: 0;` */
    right: 0;
    /* [zh] 样式规则 `width: 22px;` */
    width: 22px;
    /* [zh] 样式规则 `height: 22px;` */
    height: 22px;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `border-radius: 0 0 0 6px;` */
    border-radius: 0 0 0 6px;
    /* [zh] 样式规则 `background: rgba(0, 0, 0, 0.55);` */
    background: rgba(0, 0, 0, 0.55);
    /* [zh] 样式规则 `color: #fff;` */
    color: #fff;
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `font-size: 16px;` */
    font-size: 16px;
    /* [zh] 样式规则 `line-height: 1;` */
    line-height: 1;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.footer-row {` */
.footer-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.footer-actions {` */
.footer-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `margin-left: auto;` */
  margin-left: auto;
/* [zh] 样式规则 `}` */
}
</style>
