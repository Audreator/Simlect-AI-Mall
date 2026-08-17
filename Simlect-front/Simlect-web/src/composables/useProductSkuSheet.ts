import { ref } from 'vue';

const visible = ref(false);
const productId = ref('');


export function useProductSkuSheet() {
  const open = (id: string) => {
    if (!id) return;
    productId.value = id;
    visible.value = true;
  };

  const close = () => {
    visible.value = false;
  };

  return { visible, productId, open, close };
}
