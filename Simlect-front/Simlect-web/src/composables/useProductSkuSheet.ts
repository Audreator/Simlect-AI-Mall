// [zh] 导入 `vue`
import { ref } from 'vue';

// [zh] 声明变量 `visible`
const visible = ref(false);
// [zh] 声明变量 `productId`
const productId = ref('');


// [zh] 导出模块成员
export function useProductSkuSheet() {
  // [zh] 声明变量 `open`
  const open = (id: string) => {
    // [zh] 条件判断 if
    if (!id) return;
    // [zh] 语句
    productId.value = id;
    // [zh] 语句
    visible.value = true;
  // [zh] 语句
  };

  // [zh] 声明变量 `close`
  const close = () => {
    // [zh] 语句
    visible.value = false;
  // [zh] 语句
  };

  // [zh] return 返回
  return { visible, productId, open, close };
// [zh] 语法符号
}
