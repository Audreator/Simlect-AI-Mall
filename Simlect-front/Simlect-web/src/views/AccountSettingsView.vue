<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="settings-page">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="form-panel card">
      <!-- [zh] 开始标签 `<section>` -->
      <section class="form-section">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="section-title">头像</p>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="avatar-row">
          <!-- [zh] 开始标签 `<UserAvatar>` -->
          <UserAvatar :avatar="form.avatar" :size="72" />
          <!-- [zh] 开始标签 `<div>` -->
          <div class="avatar-actions">
            <!-- [zh] 开始标签 `<input>` -->
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              class="file-input"
              @change="onAvatarChange"
            />
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button type="primary" plain round :loading="uploading" @click="pickAvatar">
              <!-- [zh] 模板内容：`修改头像` -->
              修改头像
            <!-- [zh] 闭合标签 `</el-button>` -->
            </el-button>
            <!-- [zh] 开始标签 `<p>` -->
            <p class="hint">支持 JPG、PNG、GIF 等格式，选择后可裁剪区域</p>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<AvatarCropperDialog>` -->
      <AvatarCropperDialog ref="cropperDialogRef" />

      <!-- [zh] 开始标签 `<div>` -->
      <div class="section-divider" />

      <!-- [zh] 开始标签 `<section>` -->
      <section class="form-section">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="section-title">昵称</p>
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input v-model="form.nickName" placeholder="请输入昵称" maxlength="20" show-word-limit />
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="section-divider" />

      <!-- [zh] 开始标签 `<section>` -->
      <section class="form-section">
        <!-- [zh] 开始标签 `<p>` -->
        <p class="section-title">性别</p>
        <!-- [zh] 开始标签 `<el-radio-group>` -->
        <el-radio-group v-model="form.sex" class="sex-group">
          <!-- [zh] 开始标签 `<el-radio>` -->
          <el-radio :value="0">女</el-radio>
          <!-- [zh] 开始标签 `<el-radio>` -->
          <el-radio :value="1">男</el-radio>
          <!-- [zh] 开始标签 `<el-radio>` -->
          <el-radio :value="2">保密</el-radio>
        <!-- [zh] 闭合标签 `</el-radio-group>` -->
        </el-radio-group>
      <!-- [zh] 闭合标签 `</section>` -->
      </section>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="save-row">
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button type="primary" round class="save-btn" @click="saveInfo">保存资料</el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { onMounted, reactive, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';
// [zh] 导入 `element-plus`
import { ElMessage } from 'element-plus';
// [zh] 导入 `@/utils/apiError`
import { formatUploadErrorMessage } from '@/utils/apiError';
// [zh] 导入 `@/components/common/UserAvatar.vue`
import UserAvatar from '@/components/common/UserAvatar.vue';
// [zh] 导入 `@/components/business/AvatarCropperDialog.vue`
import AvatarCropperDialog from '@/components/business/AvatarCropperDialog.vue';
// [zh] 导入 `@/api/modules`
import { accountApi, fileApi } from '@/api/modules';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';

// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `uploading`
const uploading = ref(false);
// [zh] 声明变量 `fileInputRef`
const fileInputRef = ref<HTMLInputElement>();
// [zh] 声明变量 `cropperDialogRef`
const cropperDialogRef = ref<InstanceType<typeof AvatarCropperDialog>>();
// [zh] 声明变量 `form`
const form = reactive<any>({ nickName: '', sex: 2, avatar: '' });

// [zh] 声明变量 `load`
const load = async () => {
  // [zh] 语句
  Object.assign(form, await accountApi.getUserInfo());
// [zh] 语句
};

// [zh] 声明变量 `pickAvatar`
const pickAvatar = () => {
  // [zh] 语句
  fileInputRef.value?.click();
// [zh] 语句
};

// [zh] 声明变量 `onAvatarChange`
const onAvatarChange = async (e: Event) => {
    // [zh] 声明变量 `file`
    const file = (e.target as HTMLInputElement).files?.[0];
    // [zh] 条件判断 if
    if (!file) return;
    // [zh] 条件判断 if
    if (!file.type.startsWith('image/')) {
      // [zh] 语句
      ElMessage.warning('请选择图片文件');
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (file.size === 0) {
      // [zh] 语句
      ElMessage.warning('文件为空，请选择其他图片');
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (fileInputRef.value) fileInputRef.value.value = '';
    // [zh] 语句
    uploading.value = true;
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `blob`
      const blob = await cropperDialogRef.value!.open(file);
      // [zh] 条件判断 if
      if (!blob || blob.size === 0) {
        // [zh] 语句
        ElMessage.error('图片处理失败，尝试直接上传原始图片...');
        // [zh] 声明变量 `uploaded`
        const uploaded = await fileApi.uploadImage(file, true, 'avatar');
        // [zh] 语句
        form.avatar = uploaded.path;
        // [zh] await 等待 Promise（类比 CompletableFuture.get）
        await accountApi.updateUserInfo({ nickName: form.nickName, sex: form.sex, avatar: form.avatar });
        // [zh] await 等待 Promise（类比 CompletableFuture.get）
        await authStore.fetchUserInfo();
        // [zh] 语句
        ElMessage.success('头像已更新');
        // [zh] 语句
        uploading.value = false;
        // [zh] 语句
        return;
      // [zh] 语法符号
      }
      // [zh] 声明变量 `uploaded`
      const uploaded = await fileApi.uploadImage(blob, true, 'avatar');
      // [zh] 语句
      form.avatar = uploaded.path;
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await accountApi.updateUserInfo({ nickName: form.nickName, sex: form.sex, avatar: form.avatar });
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await authStore.fetchUserInfo();
      // [zh] 语句
      ElMessage.success('头像已更新');
    // [zh] 代码块开始
    } catch (err) {
      // [zh] 语句
      console.error('头像上传失败:', err);
      // [zh] 语句
      ElMessage.error(formatUploadErrorMessage(err));
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      uploading.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

// [zh] 声明变量 `saveInfo`
const saveInfo = async () => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await accountApi.updateUserInfo(form);
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await authStore.fetchUserInfo();
  // [zh] 语句
  ElMessage.success('资料已保存');
  // [zh] 语句
  router.push('/account/manage');
// [zh] 语句
};

// [zh] 语句
onMounted(load);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.settings-page {` */
.settings-page {
  /* [zh] 样式规则 `padding: 0 0 24px;` */
  padding: 0 0 24px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.form-panel {` */
.form-panel {
  /* [zh] 样式规则 `padding: 8px 16px 20px;` */
  padding: 8px 16px 20px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.form-section {` */
.form-section {
  /* [zh] 样式规则 `padding: 12px 0;` */
  padding: 12px 0;

  /* [zh] 样式规则 `.section-title {` */
  .section-title {
    /* [zh] 样式规则 `margin: 0 0 10px;` */
    margin: 0 0 10px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.section-divider {` */
.section-divider {
  /* [zh] 样式规则 `height: 1px;` */
  height: 1px;
  /* [zh] 样式规则 `background: $color-border;` */
  background: $color-border;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.avatar-row {` */
.avatar-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.avatar-actions {` */
.avatar-actions {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `align-items: flex-start;` */
  align-items: flex-start;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;

  /* [zh] 样式规则 `.hint {` */
  .hint {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.file-input {` */
.file-input {
  /* [zh] 样式规则 `display: none;` */
  display: none;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sex-group {` */
.sex-group {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 8px 16px;` */
  gap: 8px 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.save-row {` */
.save-row {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `padding: 20px 0 4px;` */
  padding: 20px 0 4px;

  /* [zh] 样式规则 `.save-btn {` */
  .save-btn {
    /* [zh] 样式规则 `min-width: 200px;` */
    min-width: 200px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
