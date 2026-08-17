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

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { codeToText } from 'element-china-area-data';
import { matchRegionFromFullAddress } from '@/utils/regionGeocode';
import { addressApi } from '@/api/modules';
import { useDevice } from '@/composables/useDevice';
import AddressFormFields from '@/components/business/AddressFormFields.vue';
import { toast } from '@/utils/toast';

export interface AddressFormItem {
  addressId: string;
  addressee: string;
  phone: string;
  address: string;
  defaultType?: number;
}

const props = defineProps<{
  modelValue: boolean;
  editItem?: AddressFormItem | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [boolean];
  saved: [];
}>();

const { isDesktop } = useDevice();
const saving = ref(false);

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
});

const form = reactive({
  addressId: '',
  addressee: '',
  phone: '',
  regionCodes: [] as string[],
  detailAddress: '',
  defaultType: 0 as number
});

const resetForm = () => {
  form.addressId = '';
  form.addressee = '';
  form.phone = '';
  form.regionCodes = [];
  form.detailAddress = '';
  form.defaultType = 0;
};

const matchRegion = matchRegionFromFullAddress;

const fillForm = (item?: AddressFormItem | null) => {
  if (!item) {
    resetForm();
    return;
  }
  form.addressId = item.addressId;
  form.addressee = item.addressee || '';
  form.phone = item.phone || '';
  const fullAddress = item.address || '';
  const regionCodes = matchRegion(fullAddress);
  if (regionCodes) {
    form.regionCodes = regionCodes;
    const regionText = regionCodes.map((c) => codeToText[c] || '').join('');
    form.detailAddress = fullAddress.slice(regionText.length).trim();
  } else {
    form.regionCodes = [];
    form.detailAddress = fullAddress;
  }
  form.defaultType = item.defaultType === 1 ? 1 : 0;
};

watch(
  () => props.modelValue,
  (open) => {
    if (open) fillForm(props.editItem);
  }
);

watch(
  () => props.editItem,
  (item) => {
    if (props.modelValue) fillForm(item);
  }
);

const validateForm = () => {
  const name = form.addressee.trim();
  const phone = form.phone.trim();
  const detail = form.detailAddress.trim();
  if (!name) {
    toast.warning('请输入收货人姓名');
    return false;
  }
  if (!/^1\d{10}$/.test(phone)) {
    toast.warning('请输入正确的手机号码');
    return false;
  }
  if (!form.regionCodes.length) {
    toast.warning('请选择所在地区');
    return false;
  }
  if (!detail) {
    toast.warning('请输入详细地址');
    return false;
  }
  form.addressee = name;
  form.phone = phone;
  form.detailAddress = detail;
  return true;
};

const save = async () => {
  if (!validateForm() || saving.value) return;
  saving.value = true;
  try {
    const regionText = form.regionCodes.map((c) => codeToText[c] || '').join('');
    const fullAddress = regionText + form.detailAddress.trim();
    const payload = {
      addressee: form.addressee,
      phone: form.phone,
      address: fullAddress,
      defaultType: form.defaultType
    };
    if (form.addressId) {
      await addressApi.updateAddress({ addressId: form.addressId, ...payload });
    } else {
      await addressApi.addAddress(payload);
    }
    toast.success('保存成功');
    visible.value = false;
    emit('saved');
  } finally {
    saving.value = false;
  }
};

const onClosed = () => resetForm();
</script>
