<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-form>` -->
  <el-form
    label-position="top"
    class="address-form-fields"
    :class="{ 'is-drawer': embedded }"
    @submit.prevent="emit('submit')"
  >
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item label="收货人" required>
      <!-- [zh] 开始标签 `<el-input>` -->
      <el-input v-model="form.addressee" placeholder="请输入收货人姓名" maxlength="20" clearable />
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item label="手机号码" required>
      <!-- [zh] 开始标签 `<el-input>` -->
      <el-input v-model="form.phone" placeholder="请输入 11 位手机号" maxlength="11" clearable />
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item label="所在地区" required>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="region-row">
        <!-- [zh] 开始标签 `<el-cascader>` -->
        <el-cascader
          ref="regionCascaderRef"
          v-model="form.regionCodes"
          :options="regionOptions"
          :props="cascaderProps"
          placeholder="请选择省/市/区"
          clearable
          :filterable="isDesktop"
          teleported
          placement="bottom-start"
          :popper-class="regionPopperClass"
          class="region-cascader"
          @change="onRegionChange"
          @visible-change="onRegionPanelVisible"
        />
        <!-- [zh] 开始标签 `<el-button>` -->
        <el-button
          type="primary"
          plain
          round
          size="small"
          class="btn-locate"
          :loading="locating"
          @click="applyCurrentLocation"
        >
          <!-- [zh] 开始标签 `<el-icon>` -->
          <el-icon><Location /></el-icon>
          <!-- [zh] 模板内容：`当前位置` -->
          当前位置
        <!-- [zh] 闭合标签 `</el-button>` -->
        </el-button>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item label="详细地址" required>
      <!-- [zh] 开始标签 `<el-input>` -->
      <el-input
        v-model="form.detailAddress"
        type="textarea"
        :rows="2"
        maxlength="120"
        show-word-limit
        placeholder="街道、小区、门牌号等"
      />
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
    <!-- [zh] 开始标签 `<el-form-item>` -->
    <el-form-item class="form-item-default">
      <!-- [zh] 开始标签 `<el-checkbox>` -->
      <el-checkbox v-model="form.defaultType" :true-value="1" :false-value="0">
        <!-- [zh] 模板内容：`设为默认地址` -->
        设为默认地址
      <!-- [zh] 闭合标签 `</el-checkbox>` -->
      </el-checkbox>
    <!-- [zh] 闭合标签 `</el-form-item>` -->
    </el-form-item>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="form-actions">
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" class="btn-save" round native-type="submit" :loading="saving">
        <!-- [zh] 模板内容：`保存` -->
        保存
      <!-- [zh] 闭合标签 `</el-button>` -->
      </el-button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</el-form>` -->
  </el-form>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import type { CascaderInstance } from 'element-plus';
import { Location } from '@element-plus/icons-vue';
import { regionData } from 'element-china-area-data';
import { useDevice } from '@/composables/useDevice';
import { useUserLocationWeather } from '@/composables/useUserLocationWeather';
import { useAuthStore } from '@/stores/auth';
import { resolveRegionCodes } from '@/utils/regionGeocode';
import { toast } from '@/utils/toast';
import { useRouter } from 'vue-router';

const props = defineProps<{
  form: {
    addressee: string;
    phone: string;
    regionCodes: string[];
    detailAddress: string;
    defaultType: number;
  };
  saving?: boolean;

  embedded?: boolean;
}>();

const emit = defineEmits<{ submit: [] }>();

const { getBrowserPosition, fetchByCoords } = useUserLocationWeather();
const authStore = useAuthStore();
const router = useRouter();
const locating = ref(false);
const regionOptions = regionData;
const cascaderProps = {
  label: 'label',
  value: 'value',
  children: 'children',
  emitPath: true,

  checkStrictly: true
};

const parseStreetJson = (street: string): string => {
  try {
    const data = JSON.parse(street);
    const parts: string[] = [];
    if (data.street) parts.push(data.street);
    if (data.number) parts.push(data.number);
    return parts.join('');
  } catch {
    return street;
  }
};

const { isDesktop } = useDevice();
const regionCascaderRef = ref<CascaderInstance | null>(null);

const closeRegionCascader = () => {
  nextTick(() => {
    regionCascaderRef.value?.togglePopperVisible(false);
    const input = regionCascaderRef.value?.$el?.querySelector('input');
    if (input instanceof HTMLInputElement) input.blur();
  });
};

const findRegionLeaf = (codes: string[]) => {
  let level = regionOptions as { value: string; children?: unknown[] }[];
  let node: { value: string; children?: unknown[] } | undefined;
  for (const code of codes) {
    node = level.find((n) => String(n.value) === String(code));
    if (!node) return null;
    level = (node.children || []) as { value: string; children?: unknown[] }[];
  }
  return node ?? null;
};

const onRegionChange = (codes: string[]) => {
  if (!codes?.length) return;
  const leaf = findRegionLeaf(codes);
  if (leaf && !leaf.children?.length) closeRegionCascader();
};

const regionPopperClass = computed(() =>
  isDesktop.value ? 'address-region-popper' : 'address-region-popper address-region-popper--mobile'
);

const applyCurrentLocation = async () => {
  if (locating.value) return;
  if (!authStore.isLoggedIn) {
    toast.warning('请先登录后再获取当前位置');
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }
  locating.value = true;
  try {
    const pos = await getBrowserPosition();
    const { latitude, longitude } = pos.coords;

    const info = await fetchByCoords(latitude, longitude);

    const province = info?.province;
    const city = info?.city;
    const district = info?.district;
    const street = info?.street;

    if (!province && !city && !district) {
      toast.warning('未能解析当前位置');
      return;
    }

    const codes = resolveRegionCodes(province, city, district);
    if (codes?.length) {
      props.form.regionCodes = [...codes];
      await nextTick();
      const leaf = findRegionLeaf(codes);
      if (leaf && !leaf.children?.length) closeRegionCascader();
    } else {
      toast.warning('未能匹配到省市区，请手动选择');
    }
    if (street) {
      props.form.detailAddress = parseStreetJson(street);
    }
    if (codes?.length || street) {
      const needDistrict = codes?.length === 2;
      toast.success(
        needDistrict
          ? '已填入省市，请再选择区县'
          : codes?.length
            ? '已填入当前位置'
            : '已填入详细地址，请选择省市区'
      );
    }
  } catch (e: any) {
    const msg =
      e?.code === 1 ? '请允许浏览器获取位置权限' : e?.message || '定位失败';
    toast.warning(msg);
  } finally {
    locating.value = false;
  }
};

const patchMobileRegionTrigger = () => {
  if (isDesktop.value) return;
  nextTick(() => {
    const root = regionCascaderRef.value?.$el;
    if (!root) return;
    root.querySelectorAll('input').forEach((input: HTMLInputElement) => {
      input.setAttribute('readonly', 'true');
      input.setAttribute('inputmode', 'none');
      input.setAttribute('autocomplete', 'off');
    });
  });
};

const onRegionPanelVisible = (visible: boolean) => {
  if (!visible || isDesktop.value) return;
  patchMobileRegionTrigger();
  nextTick(() => {
    const root = regionCascaderRef.value?.$el;
    root?.querySelectorAll('input').forEach((input: HTMLInputElement) => input.blur());
  });
};

onMounted(patchMobileRegionTrigger);
watch(isDesktop, () => patchMobileRegionTrigger());
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.address-form-fields {` */
.address-form-fields {
  /* [zh] 样式规则 `padding: 4px 4px 12px;` */
  padding: 4px 4px 12px;

  /* [zh] 样式规则 `:deep(.el-form-item) {` */
  :deep(.el-form-item) {
    /* [zh] 样式规则 `margin-bottom: 16px;` */
    margin-bottom: 16px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.el-form-item__label) {` */
  :deep(.el-form-item__label) {
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 500;` */
    font-weight: 500;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
    /* [zh] 样式规则 `padding-bottom: 6px;` */
    padding-bottom: 6px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  :deep(.el-input__wrapper),
  /* [zh] 样式规则 `:deep(.el-textarea__inner) {` */
  :deep(.el-textarea__inner) {
    /* [zh] 样式规则 `font-size: 15px;` */
    font-size: 15px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.form-item-default {` */
  .form-item-default {
    /* [zh] 样式规则 `margin-bottom: 8px !important;` */
    margin-bottom: 8px !important;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.region-row {` */
  .region-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;

    /* [zh] 样式规则 `.region-cascader {` */
    .region-cascader {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `min-width: 0;` */
      min-width: 0;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.btn-locate {` */
    .btn-locate {
      /* [zh] 样式规则 `align-self: flex-start;` */
      align-self: flex-start;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.region-cascader {` */
  .region-cascader {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;

    /* [zh] 样式规则 `:deep(.el-input__inner) {` */
    :deep(.el-input__inner) {
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.form-actions {` */
  .form-actions {
    /* [zh] 样式规则 `margin-top: 4px;` */
    margin-top: 4px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.btn-save {` */
  .btn-save {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `height: 44px;` */
    height: 44px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-drawer {` */
  &.is-drawer {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-direction: column;` */
    flex-direction: column;
    /* [zh] 样式规则 `min-height: 100%;` */
    min-height: 100%;
    /* [zh] 样式规则 `padding: 0;` */
    padding: 0;
    /* [zh] 样式规则 `padding-bottom: 72px;` */
    padding-bottom: 72px;

    /* [zh] 样式规则 `:deep(.el-form-item) {` */
    :deep(.el-form-item) {
      /* [zh] 样式规则 `margin-bottom: 14px;` */
      margin-bottom: 14px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.form-actions {` */
    .form-actions {
      /* [zh] 样式规则 `position: fixed;` */
      position: fixed;
      /* [zh] 样式规则 `left: 0;` */
      left: 0;
      /* [zh] 样式规则 `right: 0;` */
      right: 0;
      /* [zh] 样式规则 `bottom: $mobile-tab-height;` */
      bottom: $mobile-tab-height;
      /* [zh] 样式规则 `z-index: 1000;` */
      z-index: 1000;
      /* [zh] 样式规则 `margin-top: auto;` */
      margin-top: auto;
      /* [zh] 样式规则 `margin: 0 12px;` */
      margin: 0 12px;
      /* [zh] 样式规则 `padding: 8px 16px;` */
      padding: 8px 16px;
      /* [zh] 样式规则 `padding-bottom: calc(8px + env(safe-area` */
      padding-bottom: calc(8px + env(safe-area-inset-bottom, 0));
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.btn-save {` */
    .btn-save {
      /* [zh] 样式规则 `margin-top: 0;` */
      margin-top: 0;
      /* [zh] 样式规则 `box-shadow: 0 4px 12px rgba($color-prima` */
      box-shadow: 0 4px 12px rgba($color-primary, 0.25);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
