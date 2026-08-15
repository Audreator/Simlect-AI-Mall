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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { reactive, ref } from 'vue';
// [zh] 导入 `模块`
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus';
// [zh] 导入 `@/api/modules`
import { commentApi, fileApi } from '@/api/modules';
// [zh] 导入 `@/components/business/ImageEditorDialog.vue`
import ImageEditorDialog from '@/components/business/ImageEditorDialog.vue';
// [zh] 导入 `@/utils/commentImagePaths`
import { normalizeCommentImagePath, serializeCommentImagePaths } from '@/utils/commentImagePaths';
// [zh] 导入 `@/utils/imageUpload`
import { resolveCommentUploadBlob } from '@/utils/imageUpload';
// [zh] 导入 `@/utils/image`
import { resolveImageUrl } from '@/utils/image';
// [zh] 导入 `@/utils/apiError`
import { formatUploadErrorMessage } from '@/utils/apiError';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';

// [zh] 声明变量 `emit`
const emit = defineEmits<{ success: [] }>();

// [zh] 声明变量 `visible`
const visible = ref(false);
// [zh] 声明变量 `submitting`
const submitting = ref(false);
// [zh] 声明变量 `formRef`
const formRef = ref<FormInstance>();
// [zh] 声明变量 `imageEditorRef`
const imageEditorRef = ref<InstanceType<typeof ImageEditorDialog> | null>(null);
// [zh] 声明变量 `imageList`
const imageList = ref<string[]>([]);

// [zh] 声明变量 `form`
const form = reactive({
  // [zh] TypeScript/JS 语句：`orderId: '',`
  orderId: '',
  // [zh] TypeScript/JS 语句：`commentContent: '',`
  commentContent: '',
  // [zh] TypeScript/JS 语句：`star: 5`
  star: 5
// [zh] 语句
});

// [zh] 声明变量 `rules`
const rules: FormRules = {
  // [zh] TypeScript/JS 语句：`commentContent: [{ required: true, message: '请输入评价内容', `
  commentContent: [{ required: true, message: '请输入评价内容', trigger: 'blur' }],
  // [zh] TypeScript/JS 语句：`star: [{ required: true, message: '请选择星级', trigger: 'ch`
  star: [{ required: true, message: '请选择星级', trigger: 'change' }]
// [zh] 语句
};

// [zh] 声明变量 `toImageSrc`
const toImageSrc = (path: unknown) => {
  // [zh] 声明变量 `normalized`
  const normalized = normalizeCommentImagePath(path);
  // [zh] return 返回
  return normalized ? resolveImageUrl(normalized, { useThumbnail: false }) : '';
// [zh] 语句
};

// [zh] 声明变量 `reset`
const reset = () => {
  // [zh] 语句
  form.orderId = '';
  // [zh] 语句
  form.commentContent = '';
  // [zh] 语句
  form.star = 5;
  // [zh] 语句
  imageList.value = [];
  // [zh] 语句
  formRef.value?.clearValidate();
// [zh] 语句
};

// [zh] 声明变量 `show`
const show = (orderId: string) => {
  // [zh] 语句
  reset();
  // [zh] 语句
  form.orderId = orderId;
  // [zh] 语句
  visible.value = true;
// [zh] 语句
};

// [zh] 声明变量 `onUpload`
const onUpload = async (options: UploadRequestOptions) => {
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `file`
    const file = options.file as File;
    // [zh] 声明变量 `blob`
    const blob = await resolveCommentUploadBlob(file, (f) => imageEditorRef.value!.open(f));
    // [zh] 声明变量 `uploaded`
    const uploaded = await fileApi.uploadImage(blob, true, 'comment', form.orderId, { skipPrepare: true });
    // [zh] 声明变量 `path`
    const path = normalizeCommentImagePath(uploaded);
    // [zh] 条件判断 if
    if (path) {
      // [zh] 语句
      imageList.value.push(path);
      // [zh] 语句
      options.onSuccess?.(path);
      // [zh] 条件判断 if
      if (uploaded.pendingReview) {
        // [zh] 语句
        toast.warning('该图片存在违规风险，已提交人工审核，提交评价后将锁定订单直至审核完成');
      // [zh] 语法符号
      }
    // [zh] 代码块开始
    } else {
      // [zh] 语句
      options.onError?.(new Error('empty') as any);
      // [zh] 语句
      toast.error('图片上传失败');
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } catch (e: any) {
    // [zh] 语句
    options.onError?.(e as any);
    // [zh] 语句
    toast.error(formatUploadErrorMessage(e));
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `removeImage`
const removeImage = (idx: number) => {
  // [zh] 语句
  imageList.value.splice(idx, 1);
// [zh] 语句
};

// [zh] 声明变量 `submit`
const submit = async () => {
  // [zh] 声明变量 `valid`
  const valid = await formRef.value?.validate().catch(() => false);
  // [zh] 条件判断 if
  if (!valid) return;

  // [zh] 语句
  submitting.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `result`
    const result = await commentApi.postComment({
      // [zh] TypeScript/JS 语句：`orderId: form.orderId,`
      orderId: form.orderId,
      // [zh] TypeScript/JS 语句：`commentContent: form.commentContent.trim(),`
      commentContent: form.commentContent.trim(),
      // [zh] TypeScript/JS 语句：`star: form.star,`
      star: form.star,
      // [zh] TypeScript/JS 语句：`commentImages: serializeCommentImagePaths(imageList.val`
      commentImages: serializeCommentImagePaths(imageList.value) || undefined
    // [zh] 语句
    }) as { pendingReview?: boolean };
    // [zh] 条件判断 if
    if (result?.pendingReview) {
      // [zh] 语句
      toast.success('评价已提交，图片审核通过后将自动展示');
    // [zh] 代码块开始
    } else {
      // [zh] 语句
      toast.success('评价成功');
    // [zh] 语法符号
    }
    // [zh] 语句
    visible.value = false;
    // [zh] 语句
    emit('success');
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    submitting.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 语句
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
