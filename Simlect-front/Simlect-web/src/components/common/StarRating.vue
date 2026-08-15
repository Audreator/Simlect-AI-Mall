<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-rate>` -->
  <el-rate v-model="innerValue" allow-half :disabled="readonly" @change="$emit('update:modelValue', innerValue)" />
</template>

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { ref, watch } from 'vue';
// [zh] 声明变量 `props`
const props = defineProps<{ modelValue: number; readonly?: boolean }>();
// [zh] 语句
defineEmits<{ 'update:modelValue': [number] }>();
// [zh] 声明变量 `innerValue`
const innerValue = ref(props.modelValue || 0);
// [zh] TypeScript/JS 语句：`watch(`
watch(
  // [zh] TypeScript/JS 语句：`() => props.modelValue,`
  () => props.modelValue,
  // [zh] TypeScript/JS 语句：`(v) => (innerValue.value = v)`
  (v) => (innerValue.value = v)
// [zh] 语句
);
</script>
