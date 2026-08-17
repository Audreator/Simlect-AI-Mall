import { defineStore } from 'pinia';
import { ref } from 'vue';

export const usePcAgentPanelStore = defineStore('pcAgentPanel', () => {
  const visible = ref(false);

  const fromProduct = ref(false);

  function open(options?: { fromProduct?: boolean }) {
    fromProduct.value = !!options?.fromProduct;
    visible.value = true;
  }

  function close() {
    visible.value = false;
    fromProduct.value = false;
  }

  function toggle() {
    visible.value = !visible.value;
    if (!visible.value) fromProduct.value = false;
  }

  function consumeFromProduct() {
    const v = fromProduct.value;
    fromProduct.value = false;
    return v;
  }

  return { visible, fromProduct, open, close, toggle, consumeFromProduct };
});
