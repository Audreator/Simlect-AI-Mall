<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<el-rate>` -->
  <el-rate v-model="innerValue" allow-half :disabled="readonly" @change="$emit('update:modelValue', innerValue)" />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
const props = defineProps<{ modelValue: number; readonly?: boolean }>();
defineEmits<{ 'update:modelValue': [number] }>();
const innerValue = ref(props.modelValue || 0);
watch(
  () => props.modelValue,
  (v) => (innerValue.value = v)
);
</script>
