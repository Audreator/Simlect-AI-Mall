<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<component>` -->
  <component v-if="pageComponent" :is="pageComponent" />
</template>

<script setup lang="ts">
import { defineAsyncComponent, onMounted, shallowRef, watch, type Component } from 'vue';
import { useRoute } from 'vue-router';
import { useDevice } from '@/composables/useDevice';

type PageLoader = () => Promise<{ default: Component }>;

const route = useRoute();
const { isDesktop, sync } = useDevice();
const pageComponent = shallowRef<Component | null>(null);

const componentCache = new Map<string, Component>();

const pickPageLoader = (): PageLoader | undefined => {
  for (let i = route.matched.length - 1; i >= 0; i--) {
    const meta = route.matched[i].meta as {
      pageMobile?: PageLoader;
      pageDesktop?: PageLoader;
    };
    const loader = isDesktop.value ? meta.pageDesktop ?? meta.pageMobile : meta.pageMobile;
    if (typeof loader === 'function') return loader;
  }
  return undefined;
};

const cacheKeyForLoader = () => {
  const routeName = String(route.name ?? route.path);
  return `${routeName}:${isDesktop.value ? 'd' : 'm'}`;
};

const syncPageComponent = () => {
  const loader = pickPageLoader();
  if (!loader) {
    pageComponent.value = null;
    return;
  }
  const key = cacheKeyForLoader();
  if (!componentCache.has(key)) {
    componentCache.set(key, defineAsyncComponent(loader));
  }
  pageComponent.value = componentCache.get(key)!;
};

watch([() => route.path, isDesktop], syncPageComponent, { immediate: true });

onMounted(() => {
  sync();
  syncPageComponent();
});
</script>
