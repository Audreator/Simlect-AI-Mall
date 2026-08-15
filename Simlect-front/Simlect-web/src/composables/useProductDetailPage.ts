// [zh] 导入 `vue`
import { computed, onMounted, reactive, ref, watch } from 'vue';
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router';
// [zh] 导入 `element-plus`
import { ElMessage } from 'element-plus';
// [zh] 导入 `@/api/modules`
import { commentApi, favoriteApi, productApi } from '@/api/modules';
// [zh] 导入 `@/composables/pullRefresh`
import { usePageRefresh } from '@/composables/pullRefresh';
// [zh] 导入 `@/composables/imagePreview`
import { openImagePreview } from '@/composables/imagePreview';
// [zh] 导入 `@/composables/useProductSkuSheet`
import { useProductSkuSheet } from '@/composables/useProductSkuSheet';
// [zh] 导入 `@/stores/auth`
import { useAuthStore } from '@/stores/auth';
// [zh] 导入 `@/utils/product`
import { isProductOnSale, pickDefaultSku } from '@/utils/product';
// [zh] 导入 `@/utils/image`
import { resolveImageUrl } from '@/utils/image';
// [zh] 导入 `@/utils/checkout`
import { saveCheckoutSession } from '@/utils/checkout';
// [zh] 导入 `@/utils/toast`
import { toast } from '@/utils/toast';
// [zh] 导入 `@/constants/validation`
import { MAX_CART_QTY } from '@/constants/validation';

// [zh] 导出模块成员
export function useProductDetailPage() {
  // [zh] 声明变量 `route`
  const route = useRoute();
  // [zh] 声明变量 `router`
  const router = useRouter();
  // [zh] 声明变量 `authStore`
  const authStore = useAuthStore();
  // [zh] 语句
  const { open: openSkuSheet } = useProductSkuSheet();

  // [zh] 声明变量 `loading`
  const loading = ref(true);
  // [zh] 声明变量 `loadError`
  const loadError = ref(false);
  // [zh] 声明变量 `productInfo`
  const productInfo = ref<any>(null);
  // [zh] 声明变量 `productPropertyList`
  const productPropertyList = ref<any[]>([]);
  // [zh] 声明变量 `skuList`
  const skuList = ref<any[]>([]);
  // [zh] 声明变量 `comments`
  const comments = ref<any[]>([]);
  // [zh] 声明变量 `commentTotal`
  const commentTotal = ref(0);
  // [zh] 声明变量 `commentGoodRate`
  const commentGoodRate = ref(100);
  // [zh] 声明变量 `commentImageCount`
  const commentImageCount = ref(0);
  // [zh] 声明变量 `quantity`
  const quantity = ref(1);
  // [zh] 声明变量 `selectedSku`
  const selectedSku = ref<any>({});
  // [zh] 声明变量 `selectedProperty`
  const selectedProperty = reactive<Record<string, string>>({});
  // [zh] 声明变量 `propertyImageMap`
  const propertyImageMap = reactive<Record<string, string>>({});
  // [zh] 声明变量 `activeImageIndex`
  const activeImageIndex = ref(0);
  // [zh] 声明变量 `favorited`
  const favorited = ref(false);
  // [zh] 声明变量 `favoriteLoading`
  const favoriteLoading = ref(false);
  // [zh] 声明变量 `detailTab`
  const detailTab = ref<'comments' | 'desc'>('desc');

  // [zh] 声明变量 `PREVIEW_COMMENT_COUNT`
  const PREVIEW_COMMENT_COUNT = 2;
  // [zh] 声明变量 `productId`
  const productId = computed(() => String(route.params.productId || ''));

  // [zh] 声明变量 `thumbList`
  const thumbList = computed(() => {
    // [zh] 声明变量 `cover`
    const cover = productInfo.value?.cover;
    // [zh] 条件判断 if
    if (!cover) return [];
    // [zh] return 返回
    return String(cover)
      // [zh] TypeScript/JS 语句：`.split(',')`
      .split(',')
      // [zh] TypeScript/JS 语句：`.map((s: string) => s.trim())`
      .map((s: string) => s.trim())
      // [zh] 语句
      .filter(Boolean);
  // [zh] 语句
  });

  // [zh] 声明变量 `galleryImages`
  const galleryImages = computed(() => thumbList.value);

  // [zh] 声明变量 `displayPrice`
  const displayPrice = computed(() =>
    // [zh] TypeScript/JS 语句：`Number(selectedSku.value?.price ?? productInfo.value?.m`
    Number(selectedSku.value?.price ?? productInfo.value?.minPrice ?? 0).toFixed(2)
  // [zh] 语句
  );

  // [zh] 声明变量 `agentConsultProduct`
  const agentConsultProduct = computed(() => {
    // [zh] 声明变量 `p`
    const p = productInfo.value;
    // [zh] 条件判断 if
    if (!p?.productId) return null;
    // [zh] return 返回
    return {
      // [zh] TypeScript/JS 语句：`productId: String(p.productId),`
      productId: String(p.productId),
      // [zh] TypeScript/JS 语句：`productName: String(p.productName || '商品'),`
      productName: String(p.productName || '商品'),
      // [zh] TypeScript/JS 语句：`cover: thumbList.value[0] || '',`
      cover: thumbList.value[0] || '',
      // [zh] TypeScript/JS 语句：`minPrice: displayPrice.value`
      minPrice: displayPrice.value
    // [zh] 语句
    };
  // [zh] 语句
  });

  // [zh] 声明变量 `previewComments`
  const previewComments = computed(() => comments.value.slice(0, PREVIEW_COMMENT_COUNT));

  // [zh] 声明变量 `maxBuy`
  const maxBuy = computed(() => {
    // [zh] 声明变量 `stock`
    const stock = Number(selectedSku.value?.stock) || MAX_CART_QTY;
    // [zh] return 返回
    return Math.max(1, Math.min(stock, MAX_CART_QTY));
  // [zh] 语句
  });

  // [zh] 声明变量 `buildSkuKey`
  const buildSkuKey = (map: Record<string, string>) =>
    // [zh] 语句
    productPropertyList.value.map((p) => map[p.propertyId]).filter(Boolean).join('-');

  // [zh] 声明变量 `syncCarouselByImage`
  const syncCarouselByImage = (imgPath: string) => {
    // [zh] 声明变量 `idx`
    const idx = galleryImages.value.findIndex((img) => img === imgPath);
    // [zh] 条件判断 if
    if (idx >= 0) activeImageIndex.value = idx;
  // [zh] 语句
  };

  // [zh] 声明变量 `touchStartX`
  const touchStartX = ref(0);
  // [zh] 声明变量 `touchDeltaX`
  const touchDeltaX = ref(0);
  // [zh] 声明变量 `isDragging`
  const isDragging = ref(false);

  // [zh] 声明变量 `onTouchStart`
  const onTouchStart = (e: TouchEvent) => {
    // [zh] 语句
    touchStartX.value = e.touches[0].clientX;
  // [zh] 语句
  };

  // [zh] 声明变量 `onTouchMove`
  const onTouchMove = (e: TouchEvent) => {
    // [zh] 语句
    touchDeltaX.value = e.touches[0].clientX - touchStartX.value;
  // [zh] 语句
  };

  // [zh] 声明变量 `onTouchEnd`
  const onTouchEnd = () => {
    // [zh] 语句
    applySwipe();
    // [zh] 语句
    touchDeltaX.value = 0;
  // [zh] 语句
  };

  // [zh] 声明变量 `onMouseDown`
  const onMouseDown = (e: MouseEvent) => {
    // [zh] 语句
    isDragging.value = true;
    // [zh] 语句
    touchStartX.value = e.clientX;
    // [zh] 语句
    e.preventDefault();
  // [zh] 语句
  };

  // [zh] 声明变量 `onMouseMove`
  const onMouseMove = (e: MouseEvent) => {
    // [zh] 条件判断 if
    if (!isDragging.value) return;
    // [zh] 语句
    touchDeltaX.value = e.clientX - touchStartX.value;
  // [zh] 语句
  };

  // [zh] 声明变量 `onMouseUp`
  const onMouseUp = () => {
    // [zh] 条件判断 if
    if (!isDragging.value) return;
    // [zh] 语句
    applySwipe();
    // [zh] 语句
    touchDeltaX.value = 0;
    // [zh] 语句
    isDragging.value = false;
  // [zh] 语句
  };

  // [zh] 声明变量 `applySwipe`
  const applySwipe = () => {
    // [zh] 条件判断 if
    if (Math.abs(touchDeltaX.value) > 50) {
      // [zh] 条件判断 if
      if (touchDeltaX.value > 0 && activeImageIndex.value > 0) {
        // [zh] 语句
        activeImageIndex.value--;
      // [zh] 代码块开始
      } else if (touchDeltaX.value < 0 && activeImageIndex.value < galleryImages.value.length - 1) {
        // [zh] 语句
        activeImageIndex.value++;
      // [zh] 语法符号
      }
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (Math.abs(touchDeltaX.value) <= 10 && galleryImages.value.length) {
      // [zh] 语句
      openGalleryPreview(activeImageIndex.value);
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `openGalleryPreview`
  const openGalleryPreview = (index: number) => {
    // [zh] 声明变量 `urls`
    const urls = galleryImages.value
      // [zh] TypeScript/JS 语句：`.map((img) => resolveImageUrl(img, { useThumbnail: fals`
      .map((img) => resolveImageUrl(img, { useThumbnail: false }) || img)
      // [zh] 语句
      .filter(Boolean);
    // [zh] 条件判断 if
    if (!urls.length) return;
    // [zh] 语句
    openImagePreview(urls, index);
  // [zh] 语句
  };

  // [zh] 声明变量 `selectGalleryIndex`
  const selectGalleryIndex = (index: number) => {
    // [zh] 条件判断 if
    if (index < 0 || index >= galleryImages.value.length) return;
    // [zh] 语句
    activeImageIndex.value = index;
  // [zh] 语句
  };

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
      // [zh] 代码块开始
      prop.propertyValues?.forEach((val: any) => {
        // [zh] 条件判断 if
        if (val.propertyCover) propertyImageMap[val.propertyValueId] = val.propertyCover;
      // [zh] 语句
      });
    // [zh] 语句
    });
    // [zh] 声明变量 `coverFromSku`
    const coverFromSku = propertyImageMap[ids[0]];
    // [zh] 条件判断 if
    if (coverFromSku) syncCarouselByImage(coverFromSku);
    // [zh] else 分支
    else activeImageIndex.value = 0;
  // [zh] 语句
  };

  // [zh] 声明变量 `selectProperty`
  const selectProperty = (property: any, propertyValue: any) => {
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
    if (propertyValue.propertyCover) syncCarouselByImage(propertyValue.propertyCover);
    // [zh] 条件判断 if
    if (quantity.value > matched.stock) quantity.value = matched.stock;
  // [zh] 语句
  };

  // [zh] 声明变量 `loadFavoriteStatus`
  const loadFavoriteStatus = async () => {
    // [zh] 条件判断 if
    if (!authStore.isLoggedIn || !productId.value) {
      // [zh] 语句
      favorited.value = false;
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 代码块开始
    try {
      // [zh] 语句
      favorited.value = Boolean(await favoriteApi.isFavorite(productId.value));
    // [zh] 代码块开始
    } catch {
      // [zh] 语句
      favorited.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `toggleFavorite`
  const toggleFavorite = async () => {
    // [zh] 条件判断 if
    if (!productId.value) return;
    // [zh] 条件判断 if
    if (!authStore.isLoggedIn) {
      // [zh] 语句
      router.push({ path: '/login', query: { redirect: route.fullPath } });
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 条件判断 if
    if (favoriteLoading.value) return;
    // [zh] 语句
    favoriteLoading.value = true;
    // [zh] 代码块开始
    try {
      // [zh] 语句
      favorited.value = Boolean(await favoriteApi.toggleFavorite(productId.value));
      // [zh] 语句
      toast.success(favorited.value ? '已加入收藏' : '已取消收藏');
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      favoriteLoading.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `load`
  const load = async () => {
    // [zh] 语句
    loading.value = true;
    // [zh] 语句
    loadError.value = false;
    // [zh] 代码块开始
    try {
      // [zh] 声明变量 `data`
      const data = await productApi.getProduct(productId.value);
      // [zh] 语句
      productInfo.value = data?.productInfo || null;
      // [zh] 条件判断 if
      if (productInfo.value && !isProductOnSale(productInfo.value)) {
        // [zh] 语句
        ElMessage.warning('该商品已下架');
        // [zh] 语句
        productInfo.value = null;
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
      // [zh] 声明变量 `commentRes`
      const commentRes = await commentApi.loadComment({
        // [zh] TypeScript/JS 语句：`pageNo: 1,`
        pageNo: 1,
        // [zh] TypeScript/JS 语句：`productId: productId.value`
        productId: productId.value
      // [zh] 语句
      });
      // [zh] 语句
      comments.value = commentRes?.list || [];
      // [zh] 语句
      commentTotal.value = commentRes?.totalCount ?? comments.value.length;
      // [zh] 代码块开始
      try {
        // [zh] 声明变量 `stats`
        const stats = await commentApi.getProductCommentStats(productId.value);
        // [zh] 条件判断 if
        if (stats) {
          // [zh] 语句
          commentGoodRate.value = stats.goodRatePercent ?? 100;
          // [zh] 语句
          commentImageCount.value = stats.imageCount ?? 0;
          // [zh] 条件判断 if
          if (stats.totalCount != null) {
            // [zh] 语句
            commentTotal.value = stats.totalCount;
          // [zh] 语法符号
          }
        // [zh] 语法符号
        }
      // [zh] 代码块开始
      } catch {

      // [zh] 语法符号
      }
      // [zh] await 等待 Promise（类比 CompletableFuture.get）
      await loadFavoriteStatus();
    // [zh] 代码块开始
    } catch {
      // [zh] 语句
      loadError.value = true;
      // [zh] 语句
      productInfo.value = null;
    // [zh] 代码块开始
    } finally {
      // [zh] 语句
      loading.value = false;
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] 声明变量 `goAllComments`
  const goAllComments = () => {
    // [zh] 语句
    router.push(`/product/${route.params.productId}/comments`);
  // [zh] 语句
  };

  // [zh] 声明变量 `openAddCartSheet`
  const openAddCartSheet = () => {
    // [zh] 声明变量 `id`
    const id = productInfo.value?.productId ?? String(route.params.productId);
    // [zh] 条件判断 if
    if (!id) return;
    // [zh] 语句
    openSkuSheet(id);
  // [zh] 语句
  };

  // [zh] 声明变量 `buildPropertyData`
  const buildPropertyData = () =>
    // [zh] TypeScript/JS 语句：`productPropertyList.value`
    productPropertyList.value
      // [zh] 代码块开始
      .map((prop) => {
        // [zh] 声明变量 `valId`
        const valId = selectedProperty[prop.propertyId];
        // [zh] 声明变量 `val`
        const val = prop.propertyValues?.find((v: any) => v.propertyValueId === valId);
        // [zh] return 返回
        return val ? { propertyName: prop.propertyName, propertyValue: val.propertyValue } : null;
      // [zh] TypeScript/JS 语句：`})`
      })
      // [zh] 语句
      .filter(Boolean) as { propertyName: string; propertyValue: string }[];

  // [zh] 声明变量 `buyNow`
  const buyNow = () => {
    // [zh] 条件判断 if
    if (!selectedSku.value?.propertyValueIds) {
      // [zh] 语句
      ElMessage.warning('请选择商品规格');
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 声明变量 `cover`
    const cover = thumbList.value[0] || productInfo.value?.cover?.split(',')[0];
    // [zh] 声明变量 `checkoutItems`
    const checkoutItems = [
      // [zh] 代码块开始
      {
        // [zh] TypeScript/JS 语句：`productId: productInfo.value.productId,`
        productId: productInfo.value.productId,
        // [zh] TypeScript/JS 语句：`productName: productInfo.value.productName,`
        productName: productInfo.value.productName,
        // [zh] TypeScript/JS 语句：`productCover: cover,`
        productCover: cover,
        // [zh] TypeScript/JS 语句：`propertyValueIds: selectedSku.value.propertyValueIds,`
        propertyValueIds: selectedSku.value.propertyValueIds,
        // [zh] TypeScript/JS 语句：`propertyValueIdHash: selectedSku.value.propertyValueIdH`
        propertyValueIdHash: selectedSku.value.propertyValueIdHash,
        // [zh] TypeScript/JS 语句：`propertyData: buildPropertyData(),`
        propertyData: buildPropertyData(),
        // [zh] TypeScript/JS 语句：`price: Number(selectedSku.value.price ?? productInfo.va`
        price: Number(selectedSku.value.price ?? productInfo.value?.minPrice ?? 0),
        // [zh] TypeScript/JS 语句：`buyCount: quantity.value`
        buyCount: quantity.value
      // [zh] 语法符号
      }
    // [zh] 语句
    ];
    // [zh] 语句
    saveCheckoutSession(checkoutItems, 0);
    // [zh] 条件判断 if
    if (!authStore.isLoggedIn) {
      // [zh] 语句
      router.push({ path: '/login', query: { redirect: '/checkout' } });
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 语句
    router.push('/checkout');
  // [zh] 语句
  };

  // [zh] TypeScript/JS 语句：`watch(`
  watch(
    // [zh] TypeScript/JS 语句：`() => route.params.productId,`
    () => route.params.productId,
    // [zh] 代码块开始
    (id) => {
      // [zh] 条件判断 if
      if (id) load();
    // [zh] 语法符号
    }
  // [zh] 语句
  );

  // [zh] TypeScript/JS 语句：`watch(`
  watch(
    // [zh] TypeScript/JS 语句：`() => authStore.isLoggedIn,`
    () => authStore.isLoggedIn,
    // [zh] 代码块开始
    () => {
      // [zh] 语句
      loadFavoriteStatus();
    // [zh] 语法符号
    }
  // [zh] 语句
  );

  // [zh] 语句
  onMounted(load);
  // [zh] 语句
  usePageRefresh(load);

  // [zh] return 返回
  return {
    // [zh] TypeScript/JS 语句：`loading,`
    loading,
    // [zh] TypeScript/JS 语句：`loadError,`
    loadError,
    // [zh] TypeScript/JS 语句：`load,`
    load,
    // [zh] TypeScript/JS 语句：`productInfo,`
    productInfo,
    // [zh] TypeScript/JS 语句：`productPropertyList,`
    productPropertyList,
    // [zh] TypeScript/JS 语句：`comments,`
    comments,
    // [zh] TypeScript/JS 语句：`commentTotal,`
    commentTotal,
    // [zh] TypeScript/JS 语句：`commentGoodRate,`
    commentGoodRate,
    // [zh] TypeScript/JS 语句：`commentImageCount,`
    commentImageCount,
    // [zh] TypeScript/JS 语句：`quantity,`
    quantity,
    // [zh] TypeScript/JS 语句：`selectedSku,`
    selectedSku,
    // [zh] TypeScript/JS 语句：`selectedProperty,`
    selectedProperty,
    // [zh] TypeScript/JS 语句：`activeImageIndex,`
    activeImageIndex,
    // [zh] TypeScript/JS 语句：`favorited,`
    favorited,
    // [zh] TypeScript/JS 语句：`favoriteLoading,`
    favoriteLoading,
    // [zh] TypeScript/JS 语句：`detailTab,`
    detailTab,
    // [zh] TypeScript/JS 语句：`galleryImages,`
    galleryImages,
    // [zh] TypeScript/JS 语句：`displayPrice,`
    displayPrice,
    // [zh] TypeScript/JS 语句：`agentConsultProduct,`
    agentConsultProduct,
    // [zh] TypeScript/JS 语句：`previewComments,`
    previewComments,
    // [zh] TypeScript/JS 语句：`maxBuy,`
    maxBuy,
    // [zh] TypeScript/JS 语句：`onTouchStart,`
    onTouchStart,
    // [zh] TypeScript/JS 语句：`onTouchMove,`
    onTouchMove,
    // [zh] TypeScript/JS 语句：`onTouchEnd,`
    onTouchEnd,
    // [zh] TypeScript/JS 语句：`onMouseDown,`
    onMouseDown,
    // [zh] TypeScript/JS 语句：`onMouseMove,`
    onMouseMove,
    // [zh] TypeScript/JS 语句：`onMouseUp,`
    onMouseUp,
    // [zh] TypeScript/JS 语句：`openGalleryPreview,`
    openGalleryPreview,
    // [zh] TypeScript/JS 语句：`selectGalleryIndex,`
    selectGalleryIndex,
    // [zh] TypeScript/JS 语句：`selectProperty,`
    selectProperty,
    // [zh] TypeScript/JS 语句：`toggleFavorite,`
    toggleFavorite,
    // [zh] TypeScript/JS 语句：`goAllComments,`
    goAllComments,
    // [zh] TypeScript/JS 语句：`openAddCartSheet,`
    openAddCartSheet,
    // [zh] TypeScript/JS 语句：`buyNow`
    buyNow
  // [zh] 语句
  };
// [zh] 语法符号
}
