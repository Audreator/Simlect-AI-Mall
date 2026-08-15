package com.simlect.mappers;

import org.apache.ibatis.annotations.Param;

public interface UserCouponMapper<T,P> extends BaseMapper<T,P> {

	 Integer updateByUserCouponId(@Param("bean") T t,@Param("userCouponId") String userCouponId);

	 Integer deleteByUserCouponId(@Param("userCouponId") String userCouponId);

	 T selectByUserCouponId(@Param("userCouponId") String userCouponId);

    java.util.List<java.util.Map<String, Object>> selectExpiringUnused(@Param("limit") int limit);

    /** 即将过期未使用券（keyset 游标分页：按 valid_end_time + user_coupon_id 翻页，防 limit 截断漏提醒） */
    java.util.List<java.util.Map<String, Object>> selectExpiringUnusedByCursor(
            @Param("limit") int limit,
            @Param("cursorEndTime") java.util.Date cursorEndTime,
            @Param("cursorUserCouponId") String cursorUserCouponId);

}
