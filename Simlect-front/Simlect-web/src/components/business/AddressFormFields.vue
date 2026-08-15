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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, nextTick, onMounted, ref, watch } from 'vue';
// [zh] 导入 `模块`
import type { CascaderInstance } from 'element-plus';
// [zh] 导入 `@element-plus/icons-vue`
import { Location } from '@element-plus/icons-vue';
// [zh] 导入 `element-china-area-data`
import { regionData } from 'element-china-area-data';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/composables/useUserLocationWeather`
import { useUserLocationWeather } from '@/composables/useUserLocationWeather';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/utils/regionGeocode`
import { resolveRegionCodes } from '@/utils/regionGeocode';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router';

// [zh] 声明变量 `props`
const props = defineProps<{
  // [zh] 代码块开始
  form: {
    // [zh] 语句
    addressee: string;
    // [zh] 语句
    phone: string;
    // [zh] 语句
    regionCodes: string[];
    // [zh] 语句
    detailAddress: string;
    // [zh] 语句
    defaultType: number;
  // [zh] 语句
  };
  // [zh] 语句
  saving?: boolean;

  // [zh] 语句
  embedded?: boolean;
// [zh] 语句
}>();

// [zh] 声明变量 `emit`
const emit = defineEmits<{ submit: [] }>();

// [zh] 语句
const { getBrowserPosition, fetchByCoords } = useUserLocationWeather();
// [zh] 声明变量 `authStore`
const authStore = useAuthStore();
// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 声明变量 `locating`
const locating = ref(false);
// [zh] 声明变量 `regionOptions`
const regionOptions = regionData;
// [zh] 声明变量 `cascaderProps`
const cascaderProps = {
  // [zh] TypeScript/JS 语句：`label: 'label',`
  label: 'label',
  // [zh] TypeScript/JS 语句：`value: 'value',`
  value: 'value',
  // [zh] TypeScript/JS 语句：`children: 'children',`
  children: 'children',
  // [zh] TypeScript/JS 语句：`emitPath: true,`
  emitPath: true,

  // [zh] TypeScript/JS 语句：`checkStrictly: true`
  checkStrictly: true
// [zh] 语句
};

// [zh] 声明变量 `parseStreetJson`
const parseStreetJson = (street: string): string => {
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `data`
    const data = JSON.parse(street);
    // [zh] 声明变量 `parts`
    const parts: string[] = [];
    // [zh] 条件判断 if
    if (data.street) parts.push(data.street);
    // [zh] 条件判断 if
    if (data.number) parts.push(data.number);
    // [zh] return 返回
    return parts.join('');
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return street;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 语句
const { isDesktop } = useDevice();
// [zh] 声明变量 `regionCascaderRef`
const regionCascaderRef = ref<CascaderInstance | null>(null);

// [zh] 声明变量 `closeRegionCascader`
const closeRegionCascader = () => {
  // [zh] 代码块开始
  nextTick(() => {
    // [zh] 语句
    regionCascaderRef.value?.togglePopperVisible(false);
    // [zh] 声明变量 `input`
    const input = regionCascaderRef.value?.$el?.querySelector('input');
    // [zh] 条件判断 if
    if (input instanceof HTMLInputElement) input.blur();
  // [zh] 语句
  });
// [zh] 语句
};

// [zh] 声明变量 `findRegionLeaf`
const findRegionLeaf = (codes: string[]) => {
  // [zh] 声明变量 `level`
  let level = regionOptions as { value: string; children?: unknown[] }[];
  // [zh] 声明变量 `node`
  let node: { value: string; children?: unknown[] } | undefined;
  // [zh] for 循环
  for (const code of codes) {
    // [zh] 语句
    node = level.find((n) => String(n.value) === String(code));
    // [zh] 条件判断 if
    if (!node) return null;
    // [zh] 语句
    level = (node.children || []) as { value: string; children?: unknown[] }[];
  // [zh] 语法符号
  }
  // [zh] return 返回
  return node ?? null;
// [zh] 语句
};

// [zh] 声明变量 `onRegionChange`
const onRegionChange = (codes: string[]) => {
  // [zh] 条件判断 if
  if (!codes?.length) return;
  // [zh] 声明变量 `leaf`
  const leaf = findRegionLeaf(codes);
  // [zh] 条件判断 if
  if (leaf && !leaf.children?.length) closeRegionCascader();
// [zh] 语句
};

// [zh] 声明变量 `regionPopperClass`
const regionPopperClass = computed(() =>
  // [zh] TypeScript/JS 语句：`isDesktop.value ? 'address-region-popper' : 'address-re`
  isDesktop.value ? 'address-region-popper' : 'address-region-popper address-region-popper--mobile'
// [zh] 语句
);

// [zh] 声明变量 `applyCurrentLocation`
const applyCurrentLocation = async () => {
  // [zh] 条件判断 if
  if (locating.value) return;
  // [zh] 条件判断 if
  if (!authStore.isLoggedIn) {
    // [zh] 语句
    toast.warning('请先登录后再获取当前位置');
    // [zh] 语句
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 语句
  locating.value = true;
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `pos`
    const pos = await getBrowserPosition();
    // [zh] 语句
    const { latitude, longitude } = pos.coords;

    // [zh] 声明变量 `info`
    const info = await fetchByCoords(latitude, longitude);

    // [zh] 声明变量 `province`
    const province = info?.province;
    // [zh] 声明变量 `city`
    const city = info?.city;
    // [zh] 声明变量 `district`
    const district = info?.district;
    // [zh] 声明变量 `street`
    const street = info?.street;

    // [zh] 条件判断 if
    if (!province && !city && !district) {
      // [zh] 语句
      toast.warning('未能解析当前位置');
      // [zh] 语句
      return;
    // [zh] 语法符号
    }

    // [zh] 声明变量 `codes`
    const codes = resolveRegionCodes(province, city, district);
    // [zh] 条件判断 if
    if (codes?.length) {
      // [zh] 语句
      props.form.regionCodes = [...codes];
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await nextTick();
      // [zh] 声明变量 `leaf`
      const leaf = findRegionLeaf(codes);
      // [zh] 条件判断 if
      if (leaf && !leaf.children?.length) closeRegionCascader();
    // [zh] 代码块开始
    } else {
      // [zh] 语句
      toast.warning('未能匹配到省市区，请手动选择');
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (street) {
      // [zh] 语句
      props.form.detailAddress = parseStreetJson(street);
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (codes?.length || street) {
      // [zh] 声明变量 `needDistrict`
      const needDistrict = codes?.length === 2;
      // [zh] TypeScript/JS 语句：`toast.success(`
      toast.success(
        // [zh] TypeScript/JS 语句：`needDistrict`
        needDistrict
          // [zh] TypeScript/JS 语句：`? '已填入省市，请再选择区县'`
          ? '已填入省市，请再选择区县'
          // [zh] TypeScript/JS 语句：`: codes?.length`
          : codes?.length
            // [zh] TypeScript/JS 语句：`? '已填入当前位置'`
            ? '已填入当前位置'
            // [zh] TypeScript/JS 语句：`: '已填入详细地址，请选择省市区'`
            : '已填入详细地址，请选择省市区'
      // [zh] 语句
      );
    // [zh] 语法符号
    }
  // [zh] 代码块开始
  } catch (e: any) {
    // [zh] 声明变量 `msg`
    const msg =
      // [zh] 语句
      e?.code === 1 ? '请允许浏览器获取位置权限' : e?.message || '定位失败';
    // [zh] 语句
    toast.warning(msg);
  // [zh] 代码块开始
  } finally {
    // [zh] 语句
    locating.value = false;
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `patchMobileRegionTrigger`
const patchMobileRegionTrigger = () => {
  // [zh] 条件判断 if
  if (isDesktop.value) return;
  // [zh] 代码块开始
  nextTick(() => {
    // [zh] 声明变量 `root`
    const root = regionCascaderRef.value?.$el;
    // [zh] 条件判断 if
    if (!root) return;
    // [zh] 代码块开始
    root.querySelectorAll('input').forEach((input: HTMLInputElement) => {
      // [zh] 语句
      input.setAttribute('readonly', 'true');
      // [zh] 语句
      input.setAttribute('inputmode', 'none');
      // [zh] 语句
      input.setAttribute('autocomplete', 'off');
    // [zh] 语句
    });
  // [zh] 语句
  });
// [zh] 语句
};

// [zh] 声明变量 `onRegionPanelVisible`
const onRegionPanelVisible = (visible: boolean) => {
  // [zh] 条件判断 if
  if (!visible || isDesktop.value) return;
  // [zh] 语句
  patchMobileRegionTrigger();
  // [zh] 代码块开始
  nextTick(() => {
    // [zh] 声明变量 `root`
    const root = regionCascaderRef.value?.$el;
    // [zh] 语句
    root?.querySelectorAll('input').forEach((input: HTMLInputElement) => input.blur());
  // [zh] 语句
  });
// [zh] 语句
};

// [zh] 语句
onMounted(patchMobileRegionTrigger);
// [zh] 语句
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
