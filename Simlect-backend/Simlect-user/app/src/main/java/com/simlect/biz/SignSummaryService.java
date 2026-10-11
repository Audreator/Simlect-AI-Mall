package com.simlect.biz;

import com.simlect.entity.dto.SignSummarySnapshot;

import java.time.LocalDate;
import java.util.Date;

public interface SignSummaryService {

    /**
     * 读取签到汇总：Redis 缓存 -> MySQL 汇总表 -> 位图重建（首次访问回填），
     * 并按最近签到日期锚点修正连续天数的衰减。
     */
    SignSummarySnapshot load(String userId, LocalDate today);

    /** 签到 / 补签事务内落库：三值覆盖，锚点取较大日期（单调不回退）。 */
    void persist(String userId, SignSummarySnapshot snapshot, LocalDate anchorDate, Date now);
}