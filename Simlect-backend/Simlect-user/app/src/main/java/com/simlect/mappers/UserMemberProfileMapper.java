package com.simlect.mappers;

import org.apache.ibatis.annotations.Param;

public interface UserMemberProfileMapper<T, P> extends BaseMapper<T, P> {

    T selectByUserId(@Param("userId") String userId);

    Integer updateByUserId(@Param("bean") T bean, @Param("userId") String userId);

    /** 成长值原子自增（防并发丢失更新） */
    Integer addGrowthValue(@Param("userId") String userId, @Param("points") Integer points);

    Integer insertDefaultIgnore(@Param("userId") String userId);

    String selectUserIdForUpdate(@Param("userId") String userId);
}
