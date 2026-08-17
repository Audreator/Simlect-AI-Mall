import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { commentApi, favoriteApi, productApi } from '@/api/modules';
import { usePageRefresh } from '@/composables/pullRefresh';
import { openImagePreview } from '@/composables/imagePreview';
import { useProductSkuSheet } from '@/composables/useProductSkuSheet';
import { useAuthStore } from '@/stores/auth';
import { isProductOnSale, pickDefaultSku } from '@/utils/product';
import { resolveImageUrl } from '@/utils/image';
import { saveCheckoutSession } from '@/utils/checkout';
import { toast } from '@/utils/toast';
import { MAX_CART_QTY } from '@/constants/validation';

export function useProductDetailPage() {
  const route = useRoute();
  const router = useRouter();
  const authStore = useAuthStore();
  const { open: openSkuSheet } = useProductSkuSheet();

  const loading = ref(true);
  const loadError = ref(false);
  const productInfo = ref<any>(null);
  const productPropertyList = ref<any[]>([]);
  const skuList = ref<any[]>([]);
  const comments = ref<any[]>([]);
  const commentTotal = ref(0);
  const commentGoodRate = ref(100);
  const commentImageCount = ref(0);
  const quantity = ref(1);
  const selectedSku = ref<any>({});
  const selectedProperty = reactive<Record<string, string>>({});
  const propertyImageMap = reactive<Record<string, string>>({});
  const activeImageIndex = ref(0);
  const favorited = ref(false);
  const favoriteLoading = ref(false);
  const detailTab = ref<'comments' | 'desc'>('desc');

  const PREVIEW_COMMENT_COUNT = 2;
  const productId = computed(() => String(route.params.productId || ''));

  const thumbList = computed(() => {
    const cover = productInfo.value?.cover;
    if (!cover) return [];
    return String(cover)
      .split(',')
      .map((s: string) => s.trim())
      .filter(Boolean);
  });

  const galleryImages = computed(() => thumbList.value);

  const displayPrice = computed(() =>
    Number(selectedSku.value?.price ?? productInfo.value?.minPrice ?? 0).toFixed(2)
  );

  const agentConsultProduct = computed(() => {
    const p = productInfo.value;
    if (!p?.productId) return null;
    return {
      productId: String(p.productId),
      productName: String(p.productName || '商品'),
      cover: thumbList.value[0] || '',
      minPrice: displayPrice.value
    };
  });

  const previewComments = computed(() => comments.value.slice(0, PREVIEW_COMMENT_COUNT));

  const maxBuy = computed(() => {
    const stock = Number(selectedSku.value?.stock) || MAX_CART_QTY;
    return Math.max(1, Math.min(stock, MAX_CART_QTY));
  });

  const buildSkuKey = (map: Record<string, string>) =>
    productPropertyList.value.map((p) => map[p.propertyId]).filter(Boolean).join('-');

  const syncCarouselByImage = (imgPath: string) => {
    const idx = galleryImages.value.findIndex((img) => img === imgPath);
    if (idx >= 0) activeImageIndex.value = idx;
  };

  const touchStartX = ref(0);
  const touchDeltaX = ref(0);
  const isDragging = ref(false);

  const onTouchStart = (e: TouchEvent) => {
    touchStartX.value = e.touches[0].clientX;
  };

  const onTouchMove = (e: TouchEvent) => {
    touchDeltaX.value = e.touches[0].clientX - touchStartX.value;
  };

  const onTouchEnd = () => {
    applySwipe();
    touchDeltaX.value = 0;
  };

  const onMouseDown = (e: MouseEvent) => {
    isDragging.value = true;
    touchStartX.value = e.clientX;
    e.preventDefault();
  };

  const onMouseMove = (e: MouseEvent) => {
    if (!isDragging.value) return;
    touchDeltaX.value = e.clientX - touchStartX.value;
  };

  const onMouseUp = () => {
    if (!isDragging.value) return;
    applySwipe();
    touchDeltaX.value = 0;
    isDragging.value = false;
  };

  const applySwipe = () => {
    if (Math.abs(touchDeltaX.value) > 50) {
      if (touchDeltaX.value > 0 && activeImageIndex.value > 0) {
        activeImageIndex.value--;
      } else if (touchDeltaX.value < 0 && activeImageIndex.value < galleryImages.value.length - 1) {
        activeImageIndex.value++;
      }
      return;
    }
    if (Math.abs(touchDeltaX.value) <= 10 && galleryImages.value.length) {
      openGalleryPreview(activeImageIndex.value);
    }
  };

  const openGalleryPreview = (index: number) => {
    const urls = galleryImages.value
      .map((img) => resolveImageUrl(img, { useThumbnail: false }) || img)
      .filter(Boolean);
    if (!urls.length) return;
    openImagePreview(urls, index);
  };

  const selectGalleryIndex = (index: number) => {
    if (index < 0 || index >= galleryImages.value.length) return;
    activeImageIndex.value = index;
  };

  const initDefaultSku = () => {
    const sku = pickDefaultSku(skuList.value);
    if (!sku) return;
    selectedSku.value = sku;
    const ids = String(selectedSku.value.propertyValueIds || '').split('-');
    productPropertyList.value.forEach((prop, index) => {
      const valId = ids[index];
      if (valId) selectedProperty[prop.propertyId] = valId;
      prop.propertyValues?.forEach((val: any) => {
        if (val.propertyCover) propertyImageMap[val.propertyValueId] = val.propertyCover;
      });
    });
    const coverFromSku = propertyImageMap[ids[0]];
    if (coverFromSku) syncCarouselByImage(coverFromSku);
    else activeImageIndex.value = 0;
  };

  const selectProperty = (property: any, propertyValue: any) => {
    const temp = { ...selectedProperty, [property.propertyId]: propertyValue.propertyValueId };
    const key = buildSkuKey(temp);
    const matched = skuList.value.find((sku) => sku.propertyValueIds === key);
    if (!matched) {
      ElMessage.warning('该规格组合暂不可售');
      return;
    }
    if (matched.stock === 0) {
      ElMessage.warning('该规格已售罄');
      return;
    }
    selectedProperty[property.propertyId] = propertyValue.propertyValueId;
    selectedSku.value = matched;
    if (propertyValue.propertyCover) syncCarouselByImage(propertyValue.propertyCover);
    if (quantity.value > matched.stock) quantity.value = matched.stock;
  };

  const loadFavoriteStatus = async () => {
    if (!authStore.isLoggedIn || !productId.value) {
      favorited.value = false;
      return;
    }
    try {
      favorited.value = Boolean(await favoriteApi.isFavorite(productId.value));
    } catch {
      favorited.value = false;
    }
  };

  const toggleFavorite = async () => {
    if (!productId.value) return;
    if (!authStore.isLoggedIn) {
      router.push({ path: '/login', query: { redirect: route.fullPath } });
      return;
    }
    if (favoriteLoading.value) return;
    favoriteLoading.value = true;
    try {
      favorited.value = Boolean(await favoriteApi.toggleFavorite(productId.value));
      toast.success(favorited.value ? '已加入收藏' : '已取消收藏');
    } finally {
      favoriteLoading.value = false;
    }
  };

  const load = async () => {
    loading.value = true;
    loadError.value = false;
    try {
      const data = await productApi.getProduct(productId.value);
      productInfo.value = data?.productInfo || null;
      if (productInfo.value && !isProductOnSale(productInfo.value)) {
        ElMessage.warning('该商品已下架');
        productInfo.value = null;
        return;
      }
      productPropertyList.value = data?.productPropertyList || [];
      skuList.value = data?.skuList || [];
      initDefaultSku();
      const commentRes = await commentApi.loadComment({
        pageNo: 1,
        productId: productId.value
      });
      comments.value = commentRes?.list || [];
      commentTotal.value = commentRes?.totalCount ?? comments.value.length;
      try {
        const stats = await commentApi.getProductCommentStats(productId.value);
        if (stats) {
          commentGoodRate.value = stats.goodRatePercent ?? 100;
          commentImageCount.value = stats.imageCount ?? 0;
          if (stats.totalCount != null) {
            commentTotal.value = stats.totalCount;
          }
        }
      } catch {

      }
      await loadFavoriteStatus();
    } catch {
      loadError.value = true;
      productInfo.value = null;
    } finally {
      loading.value = false;
    }
  };

  const goAllComments = () => {
    router.push(`/product/${route.params.productId}/comments`);
  };

  const openAddCartSheet = () => {
    const id = productInfo.value?.productId ?? String(route.params.productId);
    if (!id) return;
    openSkuSheet(id);
  };

  const buildPropertyData = () =>
    productPropertyList.value
      .map((prop) => {
        const valId = selectedProperty[prop.propertyId];
        const val = prop.propertyValues?.find((v: any) => v.propertyValueId === valId);
        return val ? { propertyName: prop.propertyName, propertyValue: val.propertyValue } : null;
      })
      .filter(Boolean) as { propertyName: string; propertyValue: string }[];

  const buyNow = () => {
    if (!selectedSku.value?.propertyValueIds) {
      ElMessage.warning('请选择商品规格');
      return;
    }
    const cover = thumbList.value[0] || productInfo.value?.cover?.split(',')[0];
    const checkoutItems = [
      {
        productId: productInfo.value.productId,
        productName: productInfo.value.productName,
        productCover: cover,
        propertyValueIds: selectedSku.value.propertyValueIds,
        propertyValueIdHash: selectedSku.value.propertyValueIdHash,
        propertyData: buildPropertyData(),
        price: Number(selectedSku.value.price ?? productInfo.value?.minPrice ?? 0),
        buyCount: quantity.value
      }
    ];
    saveCheckoutSession(checkoutItems, 0);
    if (!authStore.isLoggedIn) {
      router.push({ path: '/login', query: { redirect: '/checkout' } });
      return;
    }
    router.push('/checkout');
  };

  watch(
    () => route.params.productId,
    (id) => {
      if (id) load();
    }
  );

  watch(
    () => authStore.isLoggedIn,
    () => {
      loadFavoriteStatus();
    }
  );

  onMounted(load);
  usePageRefresh(load);

  return {
    loading,
    loadError,
    load,
    productInfo,
    productPropertyList,
    comments,
    commentTotal,
    commentGoodRate,
    commentImageCount,
    quantity,
    selectedSku,
    selectedProperty,
    activeImageIndex,
    favorited,
    favoriteLoading,
    detailTab,
    galleryImages,
    displayPrice,
    agentConsultProduct,
    previewComments,
    maxBuy,
    onTouchStart,
    onTouchMove,
    onTouchEnd,
    onMouseDown,
    onMouseMove,
    onMouseUp,
    openGalleryPreview,
    selectGalleryIndex,
    selectProperty,
    toggleFavorite,
    goAllComments,
    openAddCartSheet,
    buyNow
  };
}
