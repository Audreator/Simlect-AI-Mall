package com.simlect.biz.impl;

import com.simlect.biz.SignStreakService;
import com.simlect.biz.SignSummaryService;
import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.entity.dto.SignSummarySnapshot;
import com.simlect.entity.po.UserSignSummary;
import com.simlect.mappers.SignBitmapMapper;
import com.simlect.mappers.SignSupplementUsedMapper;
import com.simlect.mappers.SignSummaryMapper;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Date;
import java.util.Map;

/**
 * 签到汇总读写：Redis 缓存 -> MySQL 汇总表 -> 位图重建（首次访问回填）。
 * 汇总为派生数据（可由 sign_bitmap 重建），写路径由签到/补签事务内 upsert，
 * 缓存统一在事务提交后刷新，读路径未命中时按“缓存 -> 表 -> 重建”逐级回源。
 */
@Service
public class SignSummaryServiceImpl implements SignSummaryService {

    private static final Logger log = LoggerFactory.getLogger(SignSummaryServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    @Resource
    private RedisComponent redisComponent;
    @Resource
    private SignSummaryMapper signSummaryMapper;
    @Resource
    private SignSummaryWriter signSummaryWriter;
    @Resource
    private SignStreakService signStreakService;
    @Resource
    private SignBitmapMapper signBitmapMapper;
    @Resource
    private SignSupplementUsedMapper signSupplementUsedMapper;

    @Override
    public SignSummarySnapshot load(String userId, LocalDate today) {
        SignSummarySnapshot cached = readCache(userId);
        if (cached != null) {
            SignSummarySnapshot resolved = resolve(cached, today);
            if (resolved != null) {
                return resolved;
            }
        }
        UserSignSummary row = signSummaryMapper.selectByUserId(userId);
        if (row != null) {
            SignSummarySnapshot stored = toSnapshot(row);
            SignSummarySnapshot resolved = resolve(stored, today);
            if (resolved != null) {
                writeCache(userId, stored);
                return resolved;
            }
        }
        return rebuild(userId, today);
    }

    @Override
    public void persist(String userId, SignSummarySnapshot snapshot, LocalDate anchorDate, Date now) {
        signSummaryMapper.upsert(userId, snapshot.continuousDays(), snapshot.totalSignDays(),
                snapshot.usedCount(), anchorDate.format(DATE_FORMATTER), now);
    }

    private SignSummarySnapshot rebuild(String userId, LocalDate today) {
        int continuousDays = signStreakService.computeContinuousDays(userId, today);
        int totalSignDays = nullToZero(signBitmapMapper.countTotalSignDays(userId));
        int usedCount = nullToZero(signSupplementUsedMapper.countByUserId(userId));
        String lastSignDate = signSummaryMapper.selectLatestSignDate(userId);
        UserSignSummary current = null;
        try {
            current = signSummaryWriter.writeBackAndRead(userId, continuousDays, totalSignDays, usedCount,
                    lastSignDate, new Date());
        } catch (Exception e) {
            log.warn("签到汇总首次回填失败 userId={}", userId, e);
        }
        SignSummarySnapshot snapshot = current != null
                ? toSnapshot(current)
                : new SignSummarySnapshot(continuousDays, totalSignDays, usedCount, lastSignDate);
        writeCache(userId, snapshot);
        SignSummarySnapshot resolved = resolve(snapshot, today);
        if (resolved != null) {
            return resolved;
        }
        return new SignSummarySnapshot(0, snapshot.totalSignDays(), snapshot.usedCount(), snapshot.lastSignDate());
    }

    /**
     * 连续天数衰减修正：锚点为今天/昨天时缓存值有效；
     * 早于昨天（今昨均未签）时连续值必然为 0；空锚点或未来锚点不可判定，触发重建。
     */
    private SignSummarySnapshot resolve(SignSummarySnapshot snapshot, LocalDate today) {
        String anchor = snapshot.lastSignDate();
        if (StringTools.isEmpty(anchor)) {
            if (snapshot.totalSignDays() == 0 && snapshot.usedCount() == 0) {
                return new SignSummarySnapshot(0, 0, 0, null);
            }
            return null;
        }
        String todayText = today.format(DATE_FORMATTER);
        if (anchor.compareTo(todayText) > 0) {
            return null;
        }
        if (anchor.equals(todayText) || anchor.equals(today.minusDays(1).format(DATE_FORMATTER))) {
            return snapshot;
        }
        return new SignSummarySnapshot(0, snapshot.totalSignDays(), snapshot.usedCount(), anchor);
    }

    private SignSummarySnapshot readCache(String userId) {
        try {
            Map<String, String> hash = redisComponent.getSignHashSnapshot(userId);
            if (hash == null || hash.get(Constants.TOTAL_SIGN_DAYS) == null) {
                return null;
            }
            String anchor = hash.get(Constants.LAST_SIGN_DATE);
            return new SignSummarySnapshot(
                    parseInt(hash.get(Constants.CONTINUOUS_DAYS)),
                    parseInt(hash.get(Constants.TOTAL_SIGN_DAYS)),
                    parseInt(hash.get(Constants.USED_COUNT)),
                    StringTools.isEmpty(anchor) ? null : anchor);
        } catch (Exception e) {
            log.warn("读取签到汇总缓存失败 userId={}", userId, e);
            return null;
        }
    }

    private void writeCache(String userId, SignSummarySnapshot snapshot) {
        try {
            redisComponent.writeSignHash(userId, snapshot.continuousDays(), snapshot.totalSignDays(),
                    snapshot.usedCount(), snapshot.lastSignDate());
        } catch (Exception e) {
            log.warn("写入签到汇总缓存失败 userId={}", userId, e);
        }
    }

    private SignSummarySnapshot toSnapshot(UserSignSummary row) {
        return new SignSummarySnapshot(nullToZero(row.getContinuousDays()), nullToZero(row.getTotalSignDays()),
                nullToZero(row.getUsedCount()), row.getLastSignDate());
    }

    private int parseInt(String value) {
        if (StringTools.isEmpty(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}