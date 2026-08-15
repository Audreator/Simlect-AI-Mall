// [zh] 导入 `./http`
import request from './http';
// [zh] 导入 `@/utils/apiCache`
import { withCache } from '@/utils/apiCache';

// [zh] 导出模块成员
export { locationApi } from './location';
// [zh] 导出模块成员
export type { LocationPayload, LocationWeatherPayload } from './location';

// [zh] 声明变量 `CACHE_KEYS`
const CACHE_KEYS = {
  // [zh] TypeScript/JS 语句：`CATEGORY: 'product:category',`
  CATEGORY: 'product:category',
  // [zh] TypeScript/JS 语句：`COMMEND: 'product:commend'`
  COMMEND: 'product:commend'
// [zh] 语句
} as const;

// [zh] 声明变量 `productDetailKey`
const productDetailKey = (id: string) => `product:detail:${id}`;

// [zh] 导出模块成员
export const accountApi = {
  // [zh] TypeScript/JS 语句：`checkCode: () => request.get('/account/checkCode'),`
  checkCode: () => request.get('/account/checkCode'),
  // [zh] TypeScript/JS 语句：`login: (params: Record<string, unknown>) => request.pos`
  login: (params: Record<string, unknown>) => request.postForm('/account/login', params),
  // [zh] TypeScript/JS 语句：`register: (params: Record<string, unknown>) => request.`
  register: (params: Record<string, unknown>) => request.postForm('/account/register', params),
  // [zh] TypeScript/JS 语句：`autoLogin: () => request.get('/account/autoLogin'),`
  autoLogin: () => request.get('/account/autoLogin'),
  // [zh] TypeScript/JS 语句：`logout: () => request.postForm('/account/logout'),`
  logout: () => request.postForm('/account/logout'),
  // [zh] TypeScript/JS 语句：`getUserInfo: () => request.get('/account/getUserInfo'),`
  getUserInfo: () => request.get('/account/getUserInfo'),
  // [zh] TypeScript/JS 语句：`updateUserInfo: (params: Record<string, unknown>) => re`
  updateUserInfo: (params: Record<string, unknown>) => request.postForm('/account/updateUserInfo', params),
  // [zh] TypeScript/JS 语句：`updatePassword: (params: Record<string, unknown>) => re`
  updatePassword: (params: Record<string, unknown>) => request.postForm('/account/updatePassword', params),
  // [zh] TypeScript/JS 语句：`getEmailCode: (params: Record<string, unknown>) => requ`
  getEmailCode: (params: Record<string, unknown>) => request.postForm('/account/getEmailCode', params),
  // [zh] TypeScript/JS 语句：`forgetPassword: (params: Record<string, unknown>) => re`
  forgetPassword: (params: Record<string, unknown>) => request.postForm('/account/forgetPassword', params)
// [zh] 语句
};

// [zh] 导出模块成员
export const searchApi = {
  // [zh] TypeScript/JS 语句：`loadHotKeywords: () => request.get('/search/loadHotKeyw`
  loadHotKeywords: () => request.get('/search/loadHotKeywords'),
  // [zh] TypeScript/JS 语句：`loadRecentKeywords: () => request.get('/search/loadRece`
  loadRecentKeywords: () => request.get('/search/loadRecentKeywords'),
  // [zh] TypeScript/JS 语句：`saveKeyword: (keyword: string) => request.postForm('/se`
  saveKeyword: (keyword: string) => request.postForm('/search/saveKeyword', { keyword }),
  // [zh] TypeScript/JS 语句：`clearRecentKeywords: () => request.postForm('/search/cl`
  clearRecentKeywords: () => request.postForm('/search/clearRecentKeywords'),
  // [zh] TypeScript/JS 语句：`removeRecentKeyword: (keyword: string) => request.postF`
  removeRecentKeyword: (keyword: string) => request.postForm('/search/removeRecentKeyword', { keyword }),
  // [zh] TypeScript/JS 语句：`loadGuessKeywords: () => request.get('/search/loadGuess`
  loadGuessKeywords: () => request.get('/search/loadGuessKeywords'),
  // [zh] TypeScript/JS 语句：`loadRecommendProducts: (limit = 8) => request.get('/sea`
  loadRecommendProducts: (limit = 8) => request.get('/search/loadRecommendProducts', { params: { limit } })
// [zh] 语句
};

// [zh] 导出模块成员
export const productApi = {

  // [zh] TypeScript/JS 语句：`loadCategory: () =>`
  loadCategory: () =>
    // [zh] 代码块开始
    withCache(() => request.get('/product/loadCategory'), {
      // [zh] TypeScript/JS 语句：`key: CACHE_KEYS.CATEGORY,`
      key: CACHE_KEYS.CATEGORY,
      // [zh] TypeScript/JS 语句：`ttl: 30 * 60 * 1000`
      ttl: 30 * 60 * 1000
    // [zh] TypeScript/JS 语句：`}),`
    }),

  // [zh] TypeScript/JS 语句：`loadCommendProduct: () =>`
  loadCommendProduct: () =>
    // [zh] 代码块开始
    withCache(() => request.get('/product/loadCommendProduct'), {
      // [zh] TypeScript/JS 语句：`key: CACHE_KEYS.COMMEND,`
      key: CACHE_KEYS.COMMEND,
      // [zh] TypeScript/JS 语句：`ttl: 5 * 60 * 1000`
      ttl: 5 * 60 * 1000
    // [zh] TypeScript/JS 语句：`}),`
    }),
  // [zh] TypeScript/JS 语句：`loadProduct: (params: Record<string, unknown>) => reque`
  loadProduct: (params: Record<string, unknown>) => request.postForm('/product/loadProduct', params),

  // [zh] TypeScript/JS 语句：`getProduct: (productId: string) =>`
  getProduct: (productId: string) =>
    // [zh] 代码块开始
    withCache(() => request.postForm('/product/getProduct', { productId }), {
      // [zh] TypeScript/JS 语句：`key: productDetailKey(productId),`
      key: productDetailKey(productId),
      // [zh] TypeScript/JS 语句：`ttl: 2 * 60 * 1000`
      ttl: 2 * 60 * 1000
    // [zh] TypeScript/JS 语句：`}),`
    }),
  // [zh] TypeScript/JS 语句：`search: (params: Record<string, unknown>) => request.po`
  search: (params: Record<string, unknown>) => request.postForm('/product/search', params)
// [zh] 语句
};

// [zh] 导出模块成员
export const cartApi = {
  // [zh] TypeScript/JS 语句：`add2Cart: (params: Record<string, unknown>) => request.`
  add2Cart: (params: Record<string, unknown>) => request.postForm('/productCart/add2Cart', params),
  // [zh] TypeScript/JS 语句：`loadProductCart: (params: Record<string, unknown>) => r`
  loadProductCart: (params: Record<string, unknown>) => request.postForm('/productCart/loadProductCart', params),
  // [zh] TypeScript/JS 语句：`deleteCart: (cartId: string) => request.postForm('/prod`
  deleteCart: (cartId: string) => request.postForm('/productCart/deleteCart', { cartId })
// [zh] 语句
};

// [zh] 导出模块成员
export const orderApi = {
  // [zh] TypeScript/JS 语句：`postOrder: (payload: Record<string, unknown>) => reques`
  postOrder: (payload: Record<string, unknown>) => request.post('/order/postOrder', payload),
  // [zh] TypeScript/JS 语句：`getPayInfo: (orderId: string) => request.postForm('/ord`
  getPayInfo: (orderId: string) => request.postForm('/order/getPayInfo', { orderId }),
  // [zh] TypeScript/JS 语句：`getOrderInfo: (payOrderId: string) => request.postForm(`
  getOrderInfo: (payOrderId: string) => request.postForm('/order/getOrderInfo', { payOrderId }),
  // [zh] TypeScript/JS 语句：`loadMyOrder: (params: Record<string, unknown>) => reque`
  loadMyOrder: (params: Record<string, unknown>) => request.postForm('/order/loadMyOrder', params),
  // [zh] TypeScript/JS 语句：`cancelOrder: (orderId: string) => request.postForm('/or`
  cancelOrder: (orderId: string) => request.postForm('/order/cancelOrder', { orderId }),
  // [zh] TypeScript/JS 语句：`deleteOrder: (orderId: string) => request.postForm('/or`
  deleteOrder: (orderId: string) => request.postForm('/order/deleteOrder', { orderId }),
  // [zh] TypeScript/JS 语句：`confirmOrder: (orderId: string) => request.postForm('/o`
  confirmOrder: (orderId: string) => request.postForm('/order/confirmOrder', { orderId }),
  // [zh] TypeScript/JS 语句：`getMyOrderDetail: (orderId: string) => request.postForm`
  getMyOrderDetail: (orderId: string) => request.postForm('/order/getMyOrderDetail', { orderId }),
  // [zh] TypeScript/JS 语句：`getLogistics: (orderId: string) => request.postForm('/o`
  getLogistics: (orderId: string) => request.postForm('/order/getLogistics', { orderId }),
  // [zh] TypeScript/JS 语句：`getOrderCountInfo: () => request.get('/order/getOrderCo`
  getOrderCountInfo: () => request.get('/order/getOrderCountInfo'),
  // [zh] TypeScript/JS 语句：`refundOrder: (orderItemId: string) => request.postForm(`
  refundOrder: (orderItemId: string) => request.postForm('/order/refundOrder', { orderItemId })
// [zh] 语句
};

// [zh] 导出模块成员
export const addressApi = {
  // [zh] TypeScript/JS 语句：`loadDataList: () => request.get('/userAddress/loadDataL`
  loadDataList: () => request.get('/userAddress/loadDataList'),
  // [zh] TypeScript/JS 语句：`addAddress: (params: Record<string, unknown>) => reques`
  addAddress: (params: Record<string, unknown>) => request.postForm('/userAddress/addAddress', params),
  // [zh] TypeScript/JS 语句：`updateAddress: (params: Record<string, unknown>) => req`
  updateAddress: (params: Record<string, unknown>) => request.postForm('/userAddress/updateAddress', params),
  // [zh] TypeScript/JS 语句：`delAddress: (addressId: string) => request.postForm('/u`
  delAddress: (addressId: string) => request.postForm('/userAddress/delAddress', { addressId }),
  // [zh] TypeScript/JS 语句：`updateDefault: (addressId: string) => request.postForm(`
  updateDefault: (addressId: string) => request.postForm('/userAddress/updateDefault', { addressId })
// [zh] 语句
};

// [zh] 导出模块成员
export const commentApi = {
  // [zh] TypeScript/JS 语句：`loadComment: (params: Record<string, unknown>) => reque`
  loadComment: (params: Record<string, unknown>) => request.postForm('/order/comment/loadComment', params),
  // [zh] TypeScript/JS 语句：`getProductCommentStats: (productId: string) =>`
  getProductCommentStats: (productId: string) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/order/comment/getProductCommentStats`
    request.postForm('/order/comment/getProductCommentStats', { productId }),
  // [zh] TypeScript/JS 语句：`getComment: (orderId: string) => request.postForm('/ord`
  getComment: (orderId: string) => request.postForm('/order/comment/getComment', { orderId }),
  // [zh] TypeScript/JS 语句：`postComment: (params: Record<string, unknown>) => reque`
  postComment: (params: Record<string, unknown>) => request.postForm('/order/comment/postComment', params),
  // [zh] TypeScript/JS 语句：`postReComment: (params: Record<string, unknown>) => req`
  postReComment: (params: Record<string, unknown>) => request.postForm('/order/comment/postReComment', params),
  // [zh] TypeScript/JS 语句：`loadMyComment: (params: Record<string, unknown>) => req`
  loadMyComment: (params: Record<string, unknown>) => request.postForm('/order/comment/loadMyComment', params),
  // [zh] TypeScript/JS 语句：`delMyComment: (orderId: string) => request.postForm('/o`
  delMyComment: (orderId: string) => request.postForm('/order/comment/delMyComment', { orderId })
// [zh] 语句
};

// [zh] 导出模块成员
export const commentReportApi = {
  // [zh] TypeScript/JS 语句：`submitReport: (params: Record<string, unknown>) => requ`
  submitReport: (params: Record<string, unknown>) => request.postForm('/commentReport/submitReport', params)
// [zh] 语句
};

// [zh] 导出模块成员
export const couponApi = {
  // [zh] TypeScript/JS 语句：`loadDiscountCoupon: (params: Record<string, unknown>) =`
  loadDiscountCoupon: (params: Record<string, unknown>) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/discountCoupon/loadDiscountCoupon', `
    request.postForm('/discountCoupon/loadDiscountCoupon', params),
  // [zh] TypeScript/JS 语句：`rushCoupon: (couponId: string) => request.postForm('/di`
  rushCoupon: (couponId: string) => request.postForm('/discountCoupon/rushCoupon', { couponId }),
  // [zh] TypeScript/JS 语句：`buyDiscountCoupon: (couponId: string, payMethod: string`
  buyDiscountCoupon: (couponId: string, payMethod: string) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/discountCoupon/buyDiscountCoupon', {`
    request.postForm('/discountCoupon/buyDiscountCoupon', { couponId, payMethod }),
  // [zh] TypeScript/JS 语句：`loadUserCoupon: (params: Record<string, unknown>) =>`
  loadUserCoupon: (params: Record<string, unknown>) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/discountCoupon/loadUserCoupon', para`
    request.postForm('/discountCoupon/loadUserCoupon', params),
  // [zh] TypeScript/JS 语句：`getDiscountCouponDetail: (couponId: string) =>`
  getDiscountCouponDetail: (couponId: string) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/discountCoupon/getDiscountCouponDeta`
    request.postForm('/discountCoupon/getDiscountCouponDetail', { couponId })
// [zh] 语句
};

// [zh] 导出模块成员
export const agentApi = {
  // [zh] TypeScript/JS 语句：`loadHistoryMessage: (params: Record<string, unknown>) =`
  loadHistoryMessage: (params: Record<string, unknown>) => request.postForm('/agent/loadHistoryMessage', params),
  // [zh] TypeScript/JS 语句：`sendMessage: (message: string, fromProduct?: boolean, c`
  sendMessage: (message: string, fromProduct?: boolean, consultProductId?: string) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/agent/sendMessage', { message, fromP`
    request.postForm('/agent/sendMessage', { message, fromProduct, consultProductId }),
  // [zh] TypeScript/JS 语句：`cancelMessage: (messageId: number, assistantMessage?: s`
  cancelMessage: (messageId: number, assistantMessage?: string) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/agent/cancelMessage', { messageId, a`
    request.postForm('/agent/cancelMessage', { messageId, assistantMessage }),
  // [zh] TypeScript/JS 语句：`clearProductConsult: () => request.postForm('/agent/cle`
  clearProductConsult: () => request.postForm('/agent/clearProductConsult', {}),
  // [zh] TypeScript/JS 语句：`pauseProductConsult: () => request.postForm('/agent/pau`
  pauseProductConsult: () => request.postForm('/agent/pauseProductConsult', {}),
  // [zh] TypeScript/JS 语句：`getProductConsultContext: () =>`
  getProductConsultContext: () =>
    // [zh] TypeScript/JS 语句：`request.postForm<{ productId?: string; productName?: st`
    request.postForm<{ productId?: string; productName?: string; active?: boolean } | null>(
      // [zh] TypeScript/JS 语句：`'/agent/getProductConsultContext',`
      '/agent/getProductConsultContext',
      // [zh] TypeScript/JS 语句：`{}`
      {}
    // [zh] TypeScript/JS 语句：`),`
    ),
  // [zh] TypeScript/JS 语句：`confirmAction: (actionToken: string) =>`
  confirmAction: (actionToken: string) =>
    // [zh] TypeScript/JS 语句：`request.postForm<{ actionType?: string; success?: boole`
    request.postForm<{ actionType?: string; success?: boolean; resultMessage?: string }>(
      // [zh] TypeScript/JS 语句：`'/agent/confirmAction',`
      '/agent/confirmAction',
      // [zh] TypeScript/JS 语句：`{ actionToken }`
      { actionToken }
    // [zh] TypeScript/JS 语句：`),`
    ),
  // [zh] TypeScript/JS 语句：`cancelAction: (actionToken: string) => request.postForm`
  cancelAction: (actionToken: string) => request.postForm('/agent/cancelAction', { actionToken })
// [zh] 语句
};

// [zh] 导出模块成员
export const signApi = {
  // [zh] TypeScript/JS 语句：`getSignCalendar: (yearMonth: string) => request.postFor`
  getSignCalendar: (yearMonth: string) => request.postForm('/sign/getSignCalendar', { yearMonth }),
  // [zh] TypeScript/JS 语句：`sign: () => request.postForm('/sign/sign'),`
  sign: () => request.postForm('/sign/sign'),
  // [zh] TypeScript/JS 语句：`msign: (date: string) => request.postForm('/sign/msign'`
  msign: (date: string) => request.postForm('/sign/msign', { date })
// [zh] 语句
};

// [zh] 导出模块成员
export const favoriteApi = {
  // [zh] TypeScript/JS 语句：`loadFavorite: (params: Record<string, unknown>) => requ`
  loadFavorite: (params: Record<string, unknown>) => request.postForm('/userFavorite/loadFavorite', params),
  // [zh] TypeScript/JS 语句：`toggleFavorite: (productId: string) => request.postForm`
  toggleFavorite: (productId: string) => request.postForm('/userFavorite/toggleFavorite', { productId }),
  // [zh] TypeScript/JS 语句：`isFavorite: (productId: string) => request.postForm('/u`
  isFavorite: (productId: string) => request.postForm('/userFavorite/isFavorite', { productId }),
  // [zh] TypeScript/JS 语句：`removeFavorite: (favoriteId: string) => request.postFor`
  removeFavorite: (favoriteId: string) => request.postForm('/userFavorite/removeFavorite', { favoriteId })
// [zh] 语句
};

// [zh] 导出模块成员
export const browseApi = {
  // [zh] TypeScript/JS 语句：`loadBrowse: (params: Record<string, unknown>) => reques`
  loadBrowse: (params: Record<string, unknown>) => request.postForm('/browseHistory/loadBrowse', params),
  // [zh] TypeScript/JS 语句：`clearBrowse: () => request.postForm('/browseHistory/cle`
  clearBrowse: () => request.postForm('/browseHistory/clearBrowse'),
  // [zh] TypeScript/JS 语句：`removeBrowse: (historyId: number) => request.postForm('`
  removeBrowse: (historyId: number) => request.postForm('/browseHistory/removeBrowse', { historyId })
// [zh] 语句
};

// [zh] 导出模块成员
export const userMemberApi = {
  // [zh] TypeScript/JS 语句：`getProfile: () => request.get('/userMember/getProfile')`
  getProfile: () => request.get('/userMember/getProfile'),
  // [zh] TypeScript/JS 语句：`getProfileWithCenter: () => request.get('/userMember/ge`
  getProfileWithCenter: () => request.get('/userMember/getProfile', { params: { center: true } }),
  // [zh] TypeScript/JS 语句：`getMemberCenter: () => request.get('/userMember/getMemb`
  getMemberCenter: () => request.get('/userMember/getMemberCenter'),
  // [zh] TypeScript/JS 语句：`loadMemberCenter: () => request.get('/userMember/loadMe`
  loadMemberCenter: () => request.get('/userMember/loadMemberCenter'),
  // [zh] TypeScript/JS 语句：`claimLevelReward: (levelCode: number) =>`
  claimLevelReward: (levelCode: number) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/userMember/claimLevelReward', { leve`
    request.postForm('/userMember/claimLevelReward', { levelCode }),
  // [zh] TypeScript/JS 语句：`getLevelBadge: (userId: string) => request.get('/userMe`
  getLevelBadge: (userId: string) => request.get('/userMember/getLevelBadge', { params: { userId } })
// [zh] 语句
};

// [zh] 导出模块成员
export const notificationApi = {
  // [zh] TypeScript/JS 语句：`loadNotification: (params: Record<string, unknown>) =>`
  loadNotification: (params: Record<string, unknown>) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/userNotification/loadNotification', `
    request.postForm('/userNotification/loadNotification', params),
  // [zh] TypeScript/JS 语句：`countUnread: () => request.get('/userNotification/count`
  countUnread: () => request.get('/userNotification/countUnread'),
  // [zh] TypeScript/JS 语句：`markRead: (notificationId: string) => request.postForm(`
  markRead: (notificationId: string) => request.postForm('/userNotification/markRead', { notificationId }),
  // [zh] TypeScript/JS 语句：`markAllRead: () => request.postForm('/userNotification/`
  markAllRead: () => request.postForm('/userNotification/markAllRead'),
  // [zh] TypeScript/JS 语句：`deleteNotification: (notificationId: string) =>`
  deleteNotification: (notificationId: string) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/userNotification/deleteNotification'`
    request.postForm('/userNotification/deleteNotification', { notificationId }),
  // [zh] TypeScript/JS 语句：`clearAll: () => request.postForm('/userNotification/cle`
  clearAll: () => request.postForm('/userNotification/clearAll'),
  // [zh] TypeScript/JS 语句：`getPopupNotification: () => request.get('/userNotificat`
  getPopupNotification: () => request.get('/userNotification/getPopupNotification'),
  // [zh] TypeScript/JS 语句：`clearPopupNotification: (notificationId: string) =>`
  clearPopupNotification: (notificationId: string) =>
    // [zh] TypeScript/JS 语句：`request.postForm('/userNotification/clearPopupNotificat`
    request.postForm('/userNotification/clearPopupNotification', { notificationId })
// [zh] 语句
};

// [zh] 导出模块成员
export const payTradeApi = {
  // [zh] TypeScript/JS 语句：`loadMyTrades: (pageNo: number) => request.postForm('/pa`
  loadMyTrades: (pageNo: number) => request.postForm('/payTrade/loadMyTrades', { pageNo })
// [zh] 语句
};

// [zh] 导出模块成员
export interface ImageUploadResult {
  // [zh] 语句
  path: string;
  // [zh] 语句
  pendingReview?: boolean;
// [zh] 语法符号
}

// [zh] 导出模块成员
export interface ImageUploadOptions {

  // [zh] 语句
  skipPrepare?: boolean;
// [zh] 语法符号
}

// [zh] 导出模块成员
export const fileApi = {
  // [zh] TypeScript/JS 语句：`uploadImage: async (`
  uploadImage: async (
    // [zh] TypeScript/JS 语句：`file: Blob,`
    file: Blob,
    // [zh] TypeScript/JS 语句：`createThumbnail = true,`
    createThumbnail = true,
    // [zh] TypeScript/JS 语句：`scene?: 'avatar' | 'comment',`
    scene?: 'avatar' | 'comment',
    // [zh] TypeScript/JS 语句：`orderId?: string,`
    orderId?: string,
    // [zh] TypeScript/JS 语句：`options?: ImageUploadOptions`
    options?: ImageUploadOptions
  // [zh] 代码块开始
  ): Promise<ImageUploadResult> => {
    // [zh] 语句
    const { prepareForUpload } = await import('@/utils/imageUpload');
    // [zh] 声明变量 `prepared`
    const prepared = options?.skipPrepare ? file : await prepareForUpload(file);
    // [zh] 条件判断 if
    if (!prepared || prepared.size < 1024) {
      // [zh] 语句
      throw new Error('图片导出失败，请重试');
    // [zh] 语法符号
    }
    // [zh] 声明变量 `ext`
    const ext = prepared.type === 'image/png' ? 'png' : 'jpg';
    // [zh] 声明变量 `formData`
    const formData = new FormData();
    // [zh] 语句
    formData.append('file', prepared, file instanceof File ? file.name.replace(/\.\w+$/, `.${ext}`) : `image.${ext}`);
    // [zh] 语句
    formData.append('createThumbnail', String(createThumbnail));
    // [zh] 条件判断 if
    if (scene) formData.append('scene', scene);
    // [zh] 条件判断 if
    if (orderId) formData.append('orderId', orderId);
    // [zh] 声明变量 `response`
    const response = await fetch('/api/file/uploadImage', {
      // [zh] TypeScript/JS 语句：`method: 'POST',`
      method: 'POST',
      // [zh] TypeScript/JS 语句：`body: formData,`
      body: formData,
      // [zh] TypeScript/JS 语句：`credentials: 'include'`
      credentials: 'include'
    // [zh] 语句
    });
    // [zh] 声明变量 `result`
    const result = await response.json();
    // [zh] 条件判断 if
    if (result.code !== 200) {
      // [zh] 语句
      const { ApiBusinessError } = await import('@/utils/apiError');
      // [zh] 声明变量 `payload`
      const payload = result.data || {};
      // [zh] 代码块开始
      throw new ApiBusinessError(result.info || '上传失败', {
        // [zh] TypeScript/JS 语句：`code: result.code,`
        code: result.code,
        // [zh] TypeScript/JS 语句：`errorType: payload.errorType,`
        errorType: payload.errorType,
        // [zh] TypeScript/JS 语句：`unbanAt: payload.unbanAt`
        unbanAt: payload.unbanAt
      // [zh] 语句
      });
    // [zh] 语法符号
    }
    // [zh] 声明变量 `data`
    const data = result.data;
    // [zh] 语句
    const { normalizeCommentImagePath } = await import('@/utils/commentImagePaths');
    // [zh] 声明变量 `path`
    const path = normalizeCommentImagePath(data);
    // [zh] 条件判断 if
    if (!path) {
      // [zh] 语句
      throw new Error('上传响应缺少图片路径');
    // [zh] 语法符号
    }
    // [zh] 声明变量 `pendingReview`
    const pendingReview =
      // [zh] TypeScript/JS 语句：`typeof data === 'object' && data !== null && 'pendingRe`
      typeof data === 'object' && data !== null && 'pendingReview' in data
        // [zh] TypeScript/JS 语句：`? !!(data as { pendingReview?: boolean }).pendingReview`
        ? !!(data as { pendingReview?: boolean }).pendingReview
        // [zh] 语句
        : false;
    // [zh] return 返回
    return { path, pendingReview };
  // [zh] 语法符号
  }
// [zh] 语句
};
