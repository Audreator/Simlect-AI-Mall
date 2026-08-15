// [zh] 导入 `pinia`
import { defineStore } from 'pinia';
// [zh] 导入 `vue`
import { ref } from 'vue';

// [zh] 导出模块成员
export const usePcAgentPanelStore = defineStore('pcAgentPanel', () => {
  // [zh] 声明变量 `visible`
  const visible = ref(false);

  // [zh] 声明变量 `fromProduct`
  const fromProduct = ref(false);

  // [zh] 函数 `open`
  function open(options?: { fromProduct?: boolean }) {
    // [zh] 语句
    fromProduct.value = !!options?.fromProduct;
    // [zh] 语句
    visible.value = true;
  // [zh] 语法符号
  }

  // [zh] 函数 `close`
  function close() {
    // [zh] 语句
    visible.value = false;
    // [zh] 语句
    fromProduct.value = false;
  // [zh] 语法符号
  }

  // [zh] 函数 `toggle`
  function toggle() {
    // [zh] 语句
    visible.value = !visible.value;
    // [zh] 条件判断 if
    if (!visible.value) fromProduct.value = false;
  // [zh] 语法符号
  }

  // [zh] 函数 `consumeFromProduct`
  function consumeFromProduct() {
    // [zh] 声明变量 `v`
    const v = fromProduct.value;
    // [zh] 语句
    fromProduct.value = false;
    // [zh] return 返回
    return v;
  // [zh] 语法符号
  }

  // [zh] return 返回
  return { visible, fromProduct, open, close, toggle, consumeFromProduct };
// [zh] 语句
});
