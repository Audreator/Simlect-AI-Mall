<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<component>` -->
  <component v-if="pageComponent" :is="pageComponent" />
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { defineAsyncComponent, onMounted, shallowRef, watch, type Component } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute } from 'vue-router';
// [zh] 导入 `@/composables/useDevice`
import { useDevice } from '@/composables/useDevice';

// [zh] TypeScript 类型 `PageLoader`（类比 Java interface/DTO）
type PageLoader = () => Promise<{ default: Component }>;

// [zh] 声明变量 `route`
const route = useRoute();
// [zh] 语句
const { isDesktop, sync } = useDevice();
// [zh] 声明变量 `pageComponent`
const pageComponent = shallowRef<Component | null>(null);

// [zh] 声明变量 `componentCache`
const componentCache = new Map<string, Component>();

// [zh] 声明变量 `pickPageLoader`
const pickPageLoader = (): PageLoader | undefined => {
  // [zh] for 循环
  for (let i = route.matched.length - 1; i >= 0; i--) {
    // [zh] 声明变量 `meta`
    const meta = route.matched[i].meta as {
      // [zh] 语句
      pageMobile?: PageLoader;
      // [zh] 语句
      pageDesktop?: PageLoader;
    // [zh] 语句
    };
    // [zh] 声明变量 `loader`
    const loader = isDesktop.value ? meta.pageDesktop ?? meta.pageMobile : meta.pageMobile;
    // [zh] 条件判断 if
    if (typeof loader === 'function') return loader;
  // [zh] 语法符号
  }
  // [zh] return 返回
  return undefined;
// [zh] 语句
};

// [zh] 声明变量 `cacheKeyForLoader`
const cacheKeyForLoader = () => {
  // [zh] 声明变量 `routeName`
  const routeName = String(route.name ?? route.path);
  // [zh] return 返回
  return `${routeName}:${isDesktop.value ? 'd' : 'm'}`;
// [zh] 语句
};

// [zh] 声明变量 `syncPageComponent`
const syncPageComponent = () => {
  // [zh] 声明变量 `loader`
  const loader = pickPageLoader();
  // [zh] 条件判断 if
  if (!loader) {
    // [zh] 语句
    pageComponent.value = null;
    // [zh] 语句
    return;
  // [zh] 语法符号
  }
  // [zh] 声明变量 `key`
  const key = cacheKeyForLoader();
  // [zh] 条件判断 if
  if (!componentCache.has(key)) {
    // [zh] 语句
    componentCache.set(key, defineAsyncComponent(loader));
  // [zh] 语法符号
  }
  // [zh] 语句
  pageComponent.value = componentCache.get(key)!;
// [zh] 语句
};

// [zh] 语句
watch([() => route.path, isDesktop], syncPageComponent, { immediate: true });

// [zh] 代码块开始
onMounted(() => {
  // [zh] 语句
  sync();
  // [zh] 语句
  syncPageComponent();
// [zh] 语句
});
</script>
