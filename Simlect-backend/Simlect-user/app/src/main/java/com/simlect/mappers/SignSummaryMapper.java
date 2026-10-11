package com.simlect.mappers;

import com.simlect.entity.po.UserSignSummary;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface SignSummaryMapper {

    UserSignSummary selectByUserId(@Param("userId") String userId);

    /** 最近一次签到/补签日期（yyyyMMdd），用于重建连续天数锚点。 */
    String selectLatestSignDate(@Param("userId") String userId);

    /**
     * 首次访问回填：行不存在则插入；行存在且锚点缺失（存量回填行）时补写连续天数与锚点，
     * 锚点已存在则保持不动，累计/已用取较大值，不覆盖并发中的最新汇总。
     */
    Integer upsertRebuild(@Param("userId") String userId,
                          @Param("continuousDays") int continuousDays,
                          @Param("totalSignDays") int totalSignDays,
                          @Param("usedCount") int usedCount,
                          @Param("lastSignDate") String lastSignDate,
                          @Param("updateTime") Date updateTime);

    /** 签到 / 补签写入：覆盖三值，锚点取较大日期（单调不回退）。 */
    Integer upsert(@Param("userId") String userId,
                   @Param("continuousDays") int continuousDays,
                   @Param("totalSignDays") int totalSignDays,
                   @Param("usedCount") int usedCount,
                   @Param("lastSignDate") String lastSignDate,
                   @Param("updateTime") Date updateTime);
}