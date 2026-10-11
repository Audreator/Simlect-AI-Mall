package com.simlect.mappers;

import com.simlect.entity.po.SignBitmap;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SignBitmapMapper {

    Integer insertBitIfAbsent(@Param("userId") String userId,
                              @Param("yearMonth") String yearMonth,
                              @Param("mask") long mask);

    Integer setBitIfMissing(@Param("userId") String userId,
                            @Param("yearMonth") String yearMonth,
                            @Param("mask") long mask);

    SignBitmap selectByUserAndMonth(@Param("userId") String userId,
                                    @Param("yearMonth") String yearMonth);

    List<SignBitmap> selectAllByUser(@Param("userId") String userId);

    Integer countTotalSignDays(@Param("userId") String userId);
}
