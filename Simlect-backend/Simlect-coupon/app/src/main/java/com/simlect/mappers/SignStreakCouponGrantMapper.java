package com.simlect.mappers;

import com.simlect.entity.po.SignStreakCouponGrant;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface SignStreakCouponGrantMapper {

    Integer insertPending(@Param("bean") SignStreakCouponGrant bean);

    SignStreakCouponGrant selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    Integer markGranted(@Param("idempotencyKey") String idempotencyKey,
                        @Param("userCouponId") String userCouponId,
                        @Param("updateTime") Date updateTime);

    Integer markRejected(@Param("idempotencyKey") String idempotencyKey,
                         @Param("reason") String reason,
                         @Param("updateTime") Date updateTime);
}
