import Request from "@/utils/Request"
const Api = {

    checkCode: "/account/checkCode",
    login: "/account/login",
    logout: "/account/logout",

    sourcePath: "/api/file/getResource?sourceName=",
    uploadImage: "/file/uploadImage",

    loadCategory: "/sysCategory/loadCategory",
    saveCategory: "/sysCategory/saveCategory",
    delCategory: "/sysCategory/delCategory",
    changeCategorySort: "/sysCategory/changeCategorySort",

    saveProductProperty: "/sysCategory/saveProductProperty",
    delProductProperty: "/sysCategory/delProductProperty",

    addProduct: "/productInfo/addProduct",
    updateProduct: "/productInfo/updateProduct",
    getProductInfo: "/productInfo/getProductInfo",
    loadProduct: "/productInfo/loadProduct",
    updateSkuStock: "/productInfo/updateSkuStock",
    updateProductStatus: "/productInfo/updateProductStatus",
    deleteProduct: "/productInfo/deleteProduct",
    commendProduct: "/productInfo/commendProduct",

    loadOrderStatus: "/order/loadOrderStatus",
    loadOrder: "/order/loadOrder",
    getLogistics: "/order/getLogistics",
    delivery: "/order/delivery",
    getComment: "/order/getComment",
    bizComment: "/order/bizComment",

    loadComment: "/order/loadComment",
    delComment: "/order/delComment",

    loadCommentReport: "/commentReport/loadDataList",
    getCommentReport: "/commentReport/getCommentReportByReportId",
    handleCommentReport: "/commentReport/handleReport",
    deleteCommentReport: "/commentReport/deleteCommentReportByReportId",
    imageModerationLoadList: "/imageModeration/loadDataList",
    imageModerationGetByRecordId: "/imageModeration/getByRecordId",
    imageModerationHandleReview: "/imageModeration/handleReview",
    imageModerationGetTempBanInfo: "/imageModeration/getTempBanInfo",
    imageModerationUnbanUser: "/imageModeration/unbanUser",
    imageModerationCensorImage: "/imageModeration/censorImage",
    mqCompensationLogLoadList: "/mqCompensationLog/loadDataList",
    mqCompensationLogGetByLogId: "/mqCompensationLog/getByLogId",
    mqCompensationLogUpdateStatus: "/mqCompensationLog/updateStatus",
    mqCompensationLogReplay: "/mqCompensationLog/replay",
    ragSyncFailureLoadList: "/ragSyncFailure/loadDataList",
    ragSyncFailureReplay: "/ragSyncFailure/replay",
    ragSyncFailureUpdateStatus: "/ragSyncFailure/updateStatus",
    ragSyncFailureDismissRedis: "/ragSyncFailure/dismissRedisSnapshot",

    loadUser: "/user/loadUser",
    changeStatus: "/user/changeStatus",

    saveSysSaveLogistics: "/setting/saveLogistics",
    getSysLogistics: "/setting/getLogistics",
    loadPromptList: "/setting/loadPromptList",
    getPromptDetail: "/setting/getPromptDetail",
    savePrompt: "/setting/savePrompt",
    cleanPromptCache: "/setting/cleanPromptCache",

    loadRagQuestion: "/rag/loadRagQuestion",
    saveRagQuestion: "/rag/saveRagQuestion",
    delRagQuestion: "/rag/delRagQuestion",

    getTodayData: "/home/getTodayData",
    loadWeeklyStatisticsData: "/home/loadWeeklyStatisticsData",
    loadLessStockProduct: "/home/loadLessStockProduct",

    loadDiscountCoupon: "/discountCoupon/loadDiscountCoupon",
    saveDiscountCoupon: "/discountCoupon/saveDiscountCoupon",
    getDiscountCouponInfo: "/discountCoupon/getDiscountCouponInfo",
    updateDiscountCouponStatus: "/discountCoupon/updateDiscountCouponStatus",

    searchHotKeywordLoadList: "/searchHotKeyword/loadList",
    searchHotKeywordSave: "/searchHotKeyword/save",
    searchHotKeywordDel: "/searchHotKeyword/del",

    sensitiveWordList: "/sensitiveWord/list",
    sensitiveWordSave: "/sensitiveWord/save",
    sensitiveWordDelete: "/sensitiveWord/delete",
    sensitiveWordRefresh: "/sensitiveWord/refresh",

    warmupRushStock: "/discountCoupon/warmupRushStock",
    reconcileRushStock: "/discountCoupon/reconcileRushStock",

    signRewardGetConfig: "/signRewardConfig/getConfig",
    signRewardSaveConfig: "/signRewardConfig/saveConfig",
    memberLevelRewardGetConfig: "/memberLevelRewardConfig/getConfig",
    memberLevelRewardSaveConfig: "/memberLevelRewardConfig/saveConfig",

    toolStatistics: "/tool/statistics",
    toolProductData: "/tool/productData",
    toolRagData: "/tool/ragData",
    toolAddAllOrderToDelayQueue: "/tool/addAllOrderToDelayQueue",

    statisticsInfoLoadList: "/statisticsInfo/loadDataList",

    agentMessageLoadList: "/agentMessage/loadDataList",
    agentMessageDelete: "/agentMessage/deleteAgentMessageByMessageId",

    userAddressLoadList: "/userAddress/loadDataList",
    userAddressDelete: "/userAddress/deleteUserAddressByAddressId",
}

const uploadImage = async (file, createThumbnail = false) => {
    const { prepareImageForUpload } = await import('@/utils/imageUpload.js')
    const prepared = await prepareImageForUpload(file)
    const ext = prepared.type === 'image/png' ? 'png' : 'jpg'
    const uploadFile = new File(
        [prepared],
        file?.name ? file.name.replace(/\.\w+$/, `.${ext}`) : `image.${ext}`,
        { type: prepared.type || 'image/jpeg' }
    )
    let result = await Request({
        url: Api.uploadImage,
        params: {
            file: uploadFile,
            createThumbnail
        },
    })
    if (!result) {
        return;
    }
    return result.data;
}
export {
    Api,
    uploadImage
}
