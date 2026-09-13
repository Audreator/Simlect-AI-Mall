package com.simlect.mappers;

import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface SignGrowthRecordMapper {

    Integer insertIgnore(@Param("userId") String userId,
                         @Param("signDate") String signDate,
                         @Param("amount") int amount,
                         @Param("createTime") Date createTime);
}
