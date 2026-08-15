<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="address-page">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="page-toolbar">
      <!-- [zh] 开始标签 `<p>` -->
      <p class="toolbar-tip">
        <!-- [zh] Mustache 插值表达式 -->
        {{ isSelectMode ? '点击地址即可选中并返回确认订单' : '管理你的收货地址，下单时可直接选用' }}
      <!-- [zh] 闭合标签 `</p>` -->
      </p>
      <!-- [zh] 开始标签 `<el-button>` -->
      <el-button type="primary" round @click="openForm()">
        <!-- [zh] 开始标签 `<el-icon>` -->
        <el-icon><Plus /></el-icon>
        <!-- [zh] 模板内容：`新增地址` -->
        新增地址
      <!-- [zh] 闭合标签 `</el-button>` -->
      </el-button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="address-list">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-for="item in list" :key="item.addressId">
        <!-- [zh] 开始标签 `<SwipeActionsRow>` -->
        <SwipeActionsRow
          v-if="useSwipeActions"
          :open="openSwipeId === item.addressId"
          :action-width="152"
          @open="openSwipeId = item.addressId"
          @close="onSwipeClose(item.addressId)"
        >
          <!-- [zh] 开始标签 `<article>` -->
          <article
            class="address-card card-flat"
            :class="cardClass(item)"
            @click="onCardClick(item)"
          >
            <!-- [zh] 开始标签 `<AddressCardBody>` -->
            <AddressCardBody :item="item" />
          <!-- [zh] 闭合标签 `</article>` -->
          </article>
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template #actions>
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="swipe-act edit" @click.stop="openForm(item)">编辑</button>
            <!-- [zh] 开始标签 `<button>` -->
            <button type="button" class="swipe-act delete" @click.stop="remove(item.addressId)">
              <!-- [zh] 模板内容：`删除` -->
              删除
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
          </template>
        </SwipeActionsRow>

        <article
          v-else
          class="address-card card-flat"
          :class="cardClass(item)"
          @click="onCardClick(item)"
        >
          <AddressCardBody :item="item" />
          <div v-if="!isSelectMode" class="card-actions">
            <el-button link type="primary" @click.stop="openForm(item)">编辑</el-button>
            <el-button
              v-if="item.defaultType !== 1"
              link
              @click.stop="setDefault(item.addressId)"
            >
              设为默认
            </el-button>
            <el-button link type="danger" @click.stop="remove(item.addressId)">删除</el-button>
          </div>
          <p v-else class="select-hint">点击使用此地址</p>
        </article>
      </template>
    </div>

    <el-empty v-else description="还没有收货地址" class="address-empty">
      <el-button type="primary" round @click="openForm()">添加收货地址</el-button>
    </el-empty>

    <AddressFormPanel v-model="formVisible" :edit-item="editingItem" @saved="onFormSaved" />
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, onMounted, ref } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router';
// [zh] 导入 `@element-plus/icons-vue`
import { Plus } from '@element-plus/icons-vue';
// [zh] 导入 `@/components/business/AddressCardBody.vue`
import AddressCardBody from '@/components/business/AddressCardBody.vue';
// [zh] 导入 `模块`
import AddressFormPanel, { type AddressFormItem } from '@/components/business/AddressFormPanel.vue';
// [zh] 导入 `@/components/business/SwipeActionsRow.vue`
import SwipeActionsRow from '@/components/business/SwipeActionsRow.vue';
// [zh] 导入 `@/api/modules`
import { addressApi } from '@/api/modules';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';
// [zh] 导入 `@/utils/confirm`
import { confirmAction } from '@/utils/confirm';
// [zh] 导入 `@/utils/checkout`
import { saveCheckoutSelectedAddress, loadCheckoutSelectedAddress } from '@/utils/checkout';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 声明变量 `router`
const router = useRouter();
// [zh] 语句
const { isDesktop } = useDevice();
// [zh] 声明变量 `isSelectMode`
const isSelectMode = computed(() => route.query.from === 'checkout');
// [zh] 声明变量 `useSwipeActions`
const useSwipeActions = computed(() => !isDesktop.value && !isSelectMode.value);
// [zh] 声明变量 `pickedAddressId`
const pickedAddressId = ref(loadCheckoutSelectedAddress() || '');

// [zh] 声明变量 `list`
const list = ref<AddressFormItem[]>([]);
// [zh] 声明变量 `formVisible`
const formVisible = ref(false);
// [zh] 声明变量 `editingItem`
const editingItem = ref<AddressFormItem | null>(null);
// [zh] 声明变量 `openSwipeId`
const openSwipeId = ref('');

// [zh] 声明变量 `sortList`
const sortList = (rows: AddressFormItem[]) =>
  // [zh] 语句
  [...rows].sort((a, b) => (b.defaultType === 1 ? 1 : 0) - (a.defaultType === 1 ? 1 : 0));

// [zh] 声明变量 `cardClass`
const cardClass = (item: AddressFormItem) => ({
  // [zh] TypeScript/JS 语句：`'is-default': item.defaultType === 1,`
  'is-default': item.defaultType === 1,
  // [zh] TypeScript/JS 语句：`'is-selectable': isSelectMode.value,`
  'is-selectable': isSelectMode.value,
  // [zh] TypeScript/JS 语句：`'is-picked': isSelectMode.value && pickedAddressId.valu`
  'is-picked': isSelectMode.value && pickedAddressId.value === item.addressId
// [zh] 语句
});

// [zh] 声明变量 `load`
const load = async () => {
  // [zh] 声明变量 `data`
  const data = await addressApi.loadDataList();
  // [zh] 语句
  list.value = sortList(Array.isArray(data) ? data : []);
// [zh] 语句
};

// [zh] 声明变量 `openForm`
const openForm = (item?: AddressFormItem) => {
  // [zh] 语句
  editingItem.value = item ?? null;
  // [zh] 语句
  formVisible.value = true;
// [zh] 语句
};

// [zh] 声明变量 `onFormSaved`
const onFormSaved = async () => {
  // [zh] 声明变量 `wasAdd`
  const wasAdd = !editingItem.value;
  // [zh] 声明变量 `prevIds`
  const prevIds = new Set(list.value.map((a) => a.addressId));
  // [zh] 语句
  editingItem.value = null;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await load();
  // [zh] 条件判断 if
  if (isSelectMode.value && wasAdd) {
    // [zh] 声明变量 `added`
    const added = list.value.find((a) => !prevIds.has(a.addressId));
    // [zh] 条件判断 if
    if (added) {
      // [zh] 语句
      selectForCheckout(added);
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 声明变量 `remove`
const remove = async (addressId: string) => {
  // [zh] 声明变量 `ok`
  const ok = await confirmAction('删除后无法恢复，确定要删除该收货地址吗？', {
    // [zh] TypeScript/JS 语句：`title: '删除地址',`
    title: '删除地址',
    // [zh] TypeScript/JS 语句：`confirmButtonText: '删除'`
    confirmButtonText: '删除'
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (!ok) return;
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await addressApi.delAddress(addressId);
  // [zh] 语句
  toast.success('已删除');
  // [zh] 条件判断 if
  if (openSwipeId.value === addressId) openSwipeId.value = '';
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await load();
// [zh] 语句
};

// [zh] 声明变量 `setDefault`
const setDefault = async (addressId: string) => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await addressApi.updateDefault(addressId);
  // [zh] 语句
  toast.success('已设为默认地址');
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await load();
// [zh] 语句
};

// [zh] 声明变量 `selectForCheckout`
const selectForCheckout = (item: AddressFormItem) => {
  // [zh] 语句
  saveCheckoutSelectedAddress(item.addressId);
  // [zh] 语句
  router.push('/checkout');
// [zh] 语句
};

// [zh] 声明变量 `onCardClick`
const onCardClick = (item: AddressFormItem) => {
  // [zh] 条件判断 if
  if (isSelectMode.value) selectForCheckout(item);
// [zh] 语句
};

// [zh] 声明变量 `onSwipeClose`
const onSwipeClose = (id: string) => {
  // [zh] 条件判断 if
  if (openSwipeId.value === id) openSwipeId.value = '';
// [zh] 语句
};

// [zh] 声明变量 `bootstrap`
const bootstrap = async () => {
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await load();
  // [zh] 条件判断 if
  if (isSelectMode.value && route.query.action === 'add') {
    // [zh] 语句
    openForm();
  // [zh] 语法符号
  }
// [zh] 语句
};

// [zh] 语句
onMounted(bootstrap);
// [zh] 语句
usePageRefresh(load);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.address-page {` */
.address-page {
  /* [zh] 样式规则 `padding-bottom: calc(16px + env(safe-are` */
  padding-bottom: calc(16px + env(safe-area-inset-bottom, 0));
  /* [zh] 样式规则 `min-height: min(100%, calc(100dvh - 120p` */
  min-height: min(100%, calc(100dvh - 120px));
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.page-toolbar {` */
.page-toolbar {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
  /* [zh] 样式规则 `margin-bottom: 14px;` */
  margin-bottom: 14px;

  /* [zh] 样式规则 `.toolbar-tip {` */
  .toolbar-tip {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.address-list {` */
.address-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.address-card {` */
.address-card {
  /* [zh] 样式规则 `padding: 14px 14px 10px;` */
  padding: 14px 14px 10px;
  /* [zh] 样式规则 `border: 1px solid $color-border;` */
  border: 1px solid $color-border;
  /* [zh] 样式规则 `transition: border-color $transition-fas` */
  transition: border-color $transition-fast, box-shadow $transition-fast;

  /* [zh] 样式规则 `&.is-default {` */
  &.is-default {
    /* [zh] 样式规则 `border-color: rgba($color-primary, 0.45)` */
    border-color: rgba($color-primary, 0.45);
    /* [zh] 样式规则 `background: linear-gradient(180deg, #fff` */
    background: linear-gradient(180deg, #fffaf7 0%, #fff 40%);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-selectable {` */
  &.is-selectable {
    /* [zh] 样式规则 `cursor: pointer;` */
    cursor: pointer;
    /* [zh] 样式规则 `-webkit-tap-highlight-color: transparent` */
    -webkit-tap-highlight-color: transparent;

    /* [zh] 样式规则 `&:active {` */
    &:active {
      /* [zh] 样式规则 `transform: scale(0.995);` */
      transform: scale(0.995);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&.is-picked {` */
    &.is-picked {
      /* [zh] 样式规则 `border-color: rgba($color-primary, 0.55)` */
      border-color: rgba($color-primary, 0.55);
      /* [zh] 样式规则 `box-shadow: 0 0 0 1px rgba($color-primar` */
      box-shadow: 0 0 0 1px rgba($color-primary, 0.15);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.select-hint {` */
  .select-hint {
    /* [zh] 样式规则 `margin: 10px 0 0;` */
    margin: 10px 0 0;
    /* [zh] 样式规则 `padding-top: 10px;` */
    padding-top: 10px;
    /* [zh] 样式规则 `border-top: 1px dashed $color-border;` */
    border-top: 1px dashed $color-border;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.card-actions {` */
  .card-actions {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `justify-content: flex-end;` */
    justify-content: flex-end;
    /* [zh] 样式规则 `gap: 4px;` */
    gap: 4px;
    /* [zh] 样式规则 `margin-top: 10px;` */
    margin-top: 10px;
    /* [zh] 样式规则 `padding-top: 10px;` */
    padding-top: 10px;
    /* [zh] 样式规则 `border-top: 1px dashed $color-border;` */
    border-top: 1px dashed $color-border;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.swipe-act {` */
.swipe-act {
  /* [zh] 样式规则 `flex: 1;` */
  flex: 1;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;

  /* [zh] 样式规则 `&.edit {` */
  &.edit {
    /* [zh] 样式规则 `background: $color-primary;` */
    background: $color-primary;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.delete {` */
  &.delete {
    /* [zh] 样式规则 `background: #e74c3c;` */
    background: #e74c3c;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.address-empty {` */
.address-empty {
  /* [zh] 样式规则 `padding: 40px 0;` */
  padding: 40px 0;
/* [zh] 样式规则 `}` */
}
</style>
