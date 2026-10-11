package com.simlect.entity.dto;

/**
 * 签到汇总快照：连续签到天数 / 累计签到天数 / 已用补签次数 / 最近签到日期锚点（yyyyMMdd）。
 * 连续天数语义为“截至 lastSignDate 的连续值”，读取时需按锚点判断是否衰减。
 */
public record SignSummarySnapshot(int continuousDays, int totalSignDays, int usedCount, String lastSignDate) {
}