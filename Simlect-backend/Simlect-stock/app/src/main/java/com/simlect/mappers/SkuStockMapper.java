package com.simlect.mappers;

import com.simlect.domain.SkuStock;
import org.apache.ibatis.annotations.Param;

public interface SkuStockMapper {

    SkuStock selectByKey(@Param("productId") String productId,
                         @Param("propertyValueIdHash") String propertyValueIdHash);

    SkuStock selectByKeyForUpdate(@Param("productId") String productId,
                                  @Param("propertyValueIdHash") String propertyValueIdHash);

    int changeStock(@Param("productId") String productId,
                    @Param("propertyValueIdHash") String propertyValueIdHash,
                    @Param("changeAmount") Integer changeAmount);

    Integer selectTotalStockByProductId(@Param("productId") String productId);

    int upsert(@Param("productId") String productId,
               @Param("propertyValueIdHash") String propertyValueIdHash,
               @Param("stock") Integer stock);

    Integer countLessThan(@Param("threshold") int threshold);

    java.util.List<SkuStock> selectLessThan(@Param("threshold") int threshold,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    int insertChangeOperation(@Param("operationId") String operationId);

    int insertCancelledChangeOperation(@Param("operationId") String operationId);

    Integer selectChangeOperationResult(@Param("operationId") String operationId);

    String selectChangeOperationIdForUpdate(@Param("operationId") String operationId);

    int completeChangeOperation(@Param("operationId") String operationId,
                                @Param("affectedRows") int affectedRows);
}
