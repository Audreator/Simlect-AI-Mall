<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-dialog>` -->
  <el-dialog
    v-if="isDesktop"
    v-model="visible"
    :title="form.addressId ? '编辑地址' : '新增地址'"
    width="480px"
    align-center
    destroy-on-close
    class="address-form-dialog ignore"
    @closed="onClosed"
  >
    <!-- [zh] 开始标签 `<AddressFormFields>` -->
    <AddressFormFields :form="form" :saving="saving" @submit="save" />
  <!-- [zh] 闭合标签 `</el-dialog>` -->
  </el-dialog>

  <!-- [zh] 开始标签 `<el-drawer>` -->
  <el-drawer
    v-else
    v-model="visible"
    :title="form.addressId ? '编辑地址' : '新增地址'"
    direction="btt"
    size="92%"
    class="address-form-drawer"
    destroy-on-close
    @closed="onClosed"
  >
    <!-- [zh] 开始标签 `<div>` -->
    <div class="address-drawer-inner">
      <!-- [zh] 开始标签 `<AddressFormFields>` -->
      <AddressFormFields :form="form" :saving="saving" embedded @submit="save" />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</el-drawer>` -->
  </el-drawer>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, reactive, ref, watch } from 'vue';
// [zh] 导入 `element-china-area-data`
import { codeToText } from 'element-china-area-data';
// [zh] 导入 `@/utils/regionGeocode`
import { matchRegionFromFullAddress } from '@/utils/regionGeocode';
// [zh] 导入 `@/api/modules`
import { addressApi } from '@/api/modules';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/components/business/AddressFormFields.vue`
import AddressFormFields from '@/components/business/AddressFormFields.vue';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';

// [zh] 导出模块成员
export interface AddressFormItem {
  // [zh] 语句
  addressId: string;
  // [zh] 语句
  addressee: string;
  // [zh] 语句
  phone: string;
  // [zh] 语句
  address: string;
  // [zh] 语句
  defaultType?: number;
// [zh] 语法符号
}

// [zh] 声明变量 `props`
const props = defineProps<{
  // [zh] 语句
  modelValue: boolean;
  // [zh] 语句
  editItem?: AddressFormItem | null;
// [zh] 语句
}>();

// [zh] 声明变量 `emit`
const emit = defineEmits<{
  // [zh] 语句
  'update:modelValue': [boolean];
  // [zh] 语句
  saved: [];
// [zh] 语句
}>();

// [zh] 语句
const { isDesktop } = useDevice();
// [zh] 声明变量 `saving`
const saving = ref(false);

// [zh] 声明变量 `visible`
const visible = computed({
  // [zh] TypeScript/JS 语句：`get: () => props.modelValue,`
  get: () => props.modelValue,
  // [zh] TypeScript/JS 语句：`set: (v) => emit('update:modelValue', v)`
  set: (v) => emit('update:modelValue', v)
// [zh] 语句
});

// [zh] 声明变量 `form`
const form = reactive({
  // [zh] TypeScript/JS 语句：`addressId: '',`
  addressId: '',
  // [zh] TypeScript/JS 语句：`addressee: '',`
  addressee: '',
  // [zh] TypeScript/JS 语句：`phone: '',`
  phone: '',
  // [zh] TypeScript/JS 语句：`regionCodes: [] as string[],`
  regionCodes: [] as string[],
  // [zh] TypeScript/JS 语句：`detailAddress: '',`
  detailAddress: '',
  // [zh] TypeScript/JS 语句：`defaultType: 0 as number`
  defaultType: 0 as number
// [zh] 语句
});

// [zh] 声明变量 `resetForm`
const resetForm = () => {
  // [zh] 语句
  form.addressId = '';
  // [zh] 语句
  form.addressee = '';
  // [zh] 语句
  form.phone = '';
  // [zh] 语句
  form.regionCodes = [];
  // [zh] 语句
  form.detailAddress = '';
  // [zh] 语句
  form.defaultType = 0;
// [zh] 语句
};

// [zh] 声明变量 `matchRegion`
const matchRegion = matchRegionFromFullAddress;

// [zh] 声明变量 `fillForm`
const fillForm = (item?: AddressFormItem | null) => {
  // [zh] 条件判断 if
  if (!item) {
    // [zh] 语句
    resetForm();
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  form.addressId = item.addressId;
  // [zh] 语句
  form.addressee = item.addressee || '';
  // [zh] 语句
  form.phone = item.phone || '';
  // [zh] 声明变量 `fullAddress`
  const fullAddress = item.address || '';
  // [zh] 声明变量 `regionCodes`
  const regionCodes = matchRegion(fullAddress);
  // [zh] 条件判断 if
  if (regionCodes) {
    // [zh] 语句
    form.regionCodes = regionCodes;
    // [zh] 声明变量 `regionText`
    const regionText = regionCodes.map((c) => codeToText[c] || '').join('');
    // [zh] 语句
    form.detailAddress = fullAddress.slice(regionText.length).trim();
  // [zh] 代码块开始
  } else {
    // [zh] 语句
    form.regionCodes = [];
    // [zh] 语句
    form.detailAddress = fullAddress;
  // [zh] 语法符号
  }
  // [zh] 语句
  form.defaultType = item.defaultType === 1 ? 1 : 0;
// [zh] 语句
};

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => props.modelValue,`
  () => props.modelValue,
  // [zh] 代码块开始
  (open) => {
    // [zh] 条件判断 if
    if (open) fillForm(props.editItem);
  // [zh] 语法符号
  }
// [zh] 语句
);

// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => props.editItem,`
  () => props.editItem,
  // [zh] 代码块开始
  (item) => {
    // [zh] 条件判断 if
    if (props.modelValue) fillForm(item);
  // [zh] 语法符号
  }
// [zh] 语句
);

// [zh] 声明变量 `validateForm`
const validateForm = () => {
  // [zh] 声明变量 `name`
  const name = form.addressee.trim();
  // [zh] 声明变量 `phone`
  const phone = form.phone.trim();
  // [zh] 声明变量 `detail`
  const detail = form.detailAddress.trim();
  // [zh] 条件判断 if
  if (!name) {
    // [zh] 语句
    toast.warning('请输入收货人姓名');
    // [zh] return 返回
    return false;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!/^1\d{10}$/.test(phone)) {
    // [zh] 语句
    toast.warning('请输入正确的手机号码');
    // [zh] return 返回
    return false;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!form.regionCodes.length) {
    // [zh] 语句
    toast.warning('请选择所在地区');
    // [zh] return 返回
    return false;
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!detail) {
    // [zh] 语句
    toast.warning('请输入详细地址');
    // [zh] return 返回
    return false;
  // [zh] 语法符号
  }
  // [zh] 语句
  form.addressee = name;
  // [zh] 语句
  form.phone = phone;
  // [zh] 语句
  form.detailAddress = detail;
  // [zh] return 返回
  return true;
// [zh] 语句
};

// [zh] 声明变量 `save`
const save = async () => {
  // [zh] 条件判断 if
  if (!validateForm() || saving.value) return;
  // [zh] 语句
  saving.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `regionText`
    const regionText = form.regionCodes.map((c) => codeToText[c] || '').join('');
    // [zh] 声明变量 `fullAddress`
    const fullAddress = regionText + form.detailAddress.trim();
    // [zh] 声明变量 `payload`
    const payload = {
      // [zh] TypeScript/JS 语句：`addressee: form.addressee,`
      addressee: form.addressee,
      // [zh] TypeScript/JS 语句：`phone: form.phone,`
      phone: form.phone,
      // [zh] TypeScript/JS 语句：`address: fullAddress,`
      address: fullAddress,
      // [zh] TypeScript/JS 语句：`defaultType: form.defaultType`
      defaultType: form.defaultType
    // [zh] 语句
    };
    // [zh] 条件判断 if
    if (form.addressId) {
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await addressApi.updateAddress({ addressId: form.addressId, ...payload });
    // [zh] 代码块开始
    } else {
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await addressApi.addAddress(payload);
    // [zh] 语法符号
    }
    // [zh] 语句
    toast.success('保存成功');
    // [zh] 语句
    visible.value = false;
    // [zh] 语句
    emit('saved');
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    saving.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `onClosed`
const onClosed = () => resetForm();
</script>
