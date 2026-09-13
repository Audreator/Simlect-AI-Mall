package com.simlect.mappers;

import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface SignSupplementUsedMapper {

    Integer countByUserId(@Param("userId") String userId);

    Integer insert(@Param("userId") String userId,
                   @Param("signDate") String signDate,
                   @Param("createTime") Date createTime);
}
