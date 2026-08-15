// [zh] 导入 `vue`
import { computed, reactive, ref } from 'vue';
// [zh] 导入 `element-plus`
import { ElMessage } from 'element-plus';
// [zh] 导入 `@/api/modules`
import { productApi } from '@/api/modules';
// [zh] 导入 `@/utils/product`
import { isProductOnSale, pickDefaultSku } from '@/utils/product';
// [zh] 导入 `@/constants/validation`
import { MAX_CART_QTY } from '@/constants/validation';

// [zh] 导出模块成员
export function useProductSku(getProductId: () => string) {
  // [zh] 声明变量 `loading`
  const loading = ref(true);
  // [zh] 声明变量 `productInfo`
  const productInfo = ref<Record<string, any> | null>(null);
  // [zh] 声明变量 `productPropertyList`
  const productPropertyList = ref<any[]>([]);
  // [zh] 声明变量 `skuList`
  const skuList = ref<any[]>([]);
  // [zh] 声明变量 `quantity`
  const quantity = ref(1);
  // [zh] 声明变量 `selectedSku`
  const selectedSku = ref<Record<string, any>>({});
  // [zh] 声明变量 `selectedProperty`
  const selectedProperty = reactive<Record<string, string>>({});

  // [zh] 声明变量 `displayPrice`
  const displayPrice = computed(() =>
    // [zh] TypeScript/JS 语句：`Number(selectedSku.value?.price ?? productInfo.value?.m`
    Number(selectedSku.value?.price ?? productInfo.value?.minPrice ?? 0).toFixed(2)
  // [zh] 语句
  );

  // [zh] 声明变量 `maxBuy`
  const maxBuy = computed(() => {
    // [zh] 声明变量 `stock`
    const stock = Number(selectedSku.value?.stock) || MAX_CART_QTY;
    // [zh] return 返回
    return Math.max(1, Math.min(stock, MAX_CART_QTY));
  // [zh] 语句
  });

  // [zh] 声明变量 `coverImage`
  const coverImage = computed(() => {
    // [zh] 声明变量 `cover`
    const cover = productInfo.value?.cover;
    // [zh] 条件判断 if
    if (!cover) return '';
    // [zh] return 返回
    return String(cover).split(',')[0]?.trim() || '';
  // [zh] 语句
  });

  // [zh] 声明变量 `buildSkuKey`
  const buildSkuKey = (map: Record<string, string>) =>
    // [zh] 语句
    productPropertyList.value.map((p) => map[p.propertyId]).filter(Boolean).join('-');

  // [zh] 声明变量 `initDefaultSku`
  const initDefaultSku = () => {
    // [zh] 声明变量 `sku`
    const sku = pickDefaultSku(skuList.value);
    // [zh] 条件判断 if
    if (!sku) return;
    // [zh] 语句
    selectedSku.value = sku;
    // [zh] 声明变量 `ids`
    const ids = String(selectedSku.value.propertyValueIds || '').split('-');
    // [zh] 代码块开始
    productPropertyList.value.forEach((prop, index) => {
      // [zh] 声明变量 `valId`
      const valId = ids[index];
      // [zh] 条件判断 if
      if (valId) selectedProperty[prop.propertyId] = valId;
    // [zh] 语句
    });
    // [zh] 语句
    quantity.value = 1;
  // [zh] 语句
  };

  // [zh] 声明变量 `selectProperty`
  const selectProperty = (property: Record<string, any>, propertyValue: Record<string, any>) => {
    // [zh] 声明变量 `temp`
    const temp = { ...selectedProperty, [property.propertyId]: propertyValue.propertyValueId };
    // [zh] 声明变量 `key`
    const key = buildSkuKey(temp);
    // [zh] 声明变量 `matched`
    const matched = skuList.value.find((sku) => sku.propertyValueIds === key);
    // [zh] 条件判断 if
    if (!matched) {
      // [zh] 语句
      ElMessage.warning('该规格组合暂不可售');
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (matched.stock === 0) {
      // [zh] 语句
      ElMessage.warning('该规格已售罄');
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 语句
    selectedProperty[property.propertyId] = propertyValue.propertyValueId;
    // [zh] 语句
    selectedSku.value = matched;
    // [zh] 条件判断 if
    if (quantity.value > matched.stock) quantity.value = matched.stock;
  // [zh] 语句
  };

  // [zh] 声明变量 `load`
  const load = async () => {
    // [zh] 声明变量 `productId`
    const productId = getProductId();
    // [zh] 条件判断 if
    if (!productId) return;

    // [zh] 语句
    loading.value = true;
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `data`
      const data = await productApi.getProduct(productId);
      // [zh] 语句
      productInfo.value = data?.productInfo || null;
      // [zh] 条件判断 if
      if (productInfo.value && !isProductOnSale(productInfo.value)) {
        // [zh] 语句
        ElMessage.warning('该商品已下架');
        // [zh] 语句
        productInfo.value = null;
        // [zh] 语句
        productPropertyList.value = [];
        // [zh] 语句
        skuList.value = [];
        // [zh] 语句
        return;
      // [zh] 语法符号
      }
      // [zh] 语句
      productPropertyList.value = data?.productPropertyList || [];
      // [zh] 语句
      skuList.value = data?.skuList || [];
      // [zh] 语句
      initDefaultSku();
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      loading.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `validateSku`
  const validateSku = () => {
    // [zh] 条件判断 if
    if (!selectedSku.value?.propertyValueIds) {
      // [zh] 语句
      ElMessage.warning('请选择商品规格');
      // [zh] return 返回
      return false;
    // [zh] 语法符号
    }
    // [zh] return 返回
    return true;
  // [zh] 语句
  };

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`loading,`
    loading,
    // [zh] TypeScript/JS 语句：`productInfo,`
    productInfo,
    // [zh] TypeScript/JS 语句：`productPropertyList,`
    productPropertyList,
    // [zh] TypeScript/JS 语句：`skuList,`
    skuList,
    // [zh] TypeScript/JS 语句：`quantity,`
    quantity,
    // [zh] TypeScript/JS 语句：`selectedSku,`
    selectedSku,
    // [zh] TypeScript/JS 语句：`selectedProperty,`
    selectedProperty,
    // [zh] TypeScript/JS 语句：`displayPrice,`
    displayPrice,
    // [zh] TypeScript/JS 语句：`maxBuy,`
    maxBuy,
    // [zh] TypeScript/JS 语句：`coverImage,`
    coverImage,
    // [zh] TypeScript/JS 语句：`selectProperty,`
    selectProperty,
    // [zh] TypeScript/JS 语句：`load,`
    load,
    // [zh] TypeScript/JS 语句：`validateSku`
    validateSku
  // [zh] 语句
  };
// [zh] 语法符号
}
