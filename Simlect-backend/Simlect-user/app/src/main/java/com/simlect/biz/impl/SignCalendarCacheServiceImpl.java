package com.simlect.biz.impl;

import com.simlect.api.support.AdminAuditFeignSupport;
import com.simlect.component.RedisComponent;
import com.simlect.constants.Constants;
import com.simlect.entity.po.UserSignRecord;
import com.simlect.entity.po.UserSignRecordDetail;
import com.simlect.entity.query.UserSignRecordQuery;
import com.simlect.api.vo.SignDateSyncResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserSignRecordDetailMapper;
import com.simlect.mappers.UserSignRecordMapper;
import com.simlect.biz.SignCalendarCacheService;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class SignCalendarCacheServiceImpl implements SignCalendarCacheService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String AUDIT_ACTION_FORCE_REBUILD = "SIGN_FORCE_REBUILD_TODAY";

    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserSignRecordDetailMapper<UserSignRecordDetail, com.simlect.entity.query.UserSignRecordDetailQuery> userSignRecordDetailMapper;
    @Resource
    private UserSignRecordMapper<UserSignRecord, UserSignRecordQuery> userSignRecordMapper;
    @Resource
    private AdminAuditFeignSupport adminAuditFeignSupport;

    @Override
    public void ensureCalendarHydrated(String userId) {
        if (StringTools.isEmpty(userId)) {
            return;
        }
        if (redisComponent.hasSignNullCache(userId)) {
            return;
        }
        if (redisComponent.hasSignHashSnapshot(userId)) {
            return;
        }
        rebuildWithLock(userId, todayYmd(), true);
    }

    @Override
    public SignDateSyncResultVO syncSignDatesFromDb(String userId, String syncEndDate, boolean forceIncludeToday) {
        validateSyncEndDate(syncEndDate, forceIncludeToday);
        SignDateSyncResultVO result = new SignDateSyncResultVO();
        if (StringTools.isEmpty(userId)) {
            throw new BusinessException("userId 不能为空");
        }
        String startDate = rebuildStartDate(syncEndDate);
        List<UserSignRecordDetail> details = userSignRecordDetailMapper.selectByUserIdAndDateRange(
                userId.trim(), startDate, syncEndDate);
        result.setTotalInDb(details == null ? 0 : details.size());
        if (details == null || details.isEmpty()) {
            result.setSkipped(1);
            return result;
        }
        supplementDetailsToRedis(userId.trim(), details, result);
        hydrateHashWithoutDowngrade(userId.trim(), details);
        redisComponent.clearSignNullCache(userId.trim());
        result.setSyncedUsers(1);
        result.setSyncedDates(result.getSupplementedDates());
        return result;
    }

    @Override
    public SignDateSyncResultVO syncAllSignDatesFromDb(String syncEndDate, boolean forceIncludeToday) {
        validateSyncEndDate(syncEndDate, forceIncludeToday);
        SignDateSyncResultVO result = new SignDateSyncResultVO();
        String startDate = rebuildStartDate(syncEndDate);
        List<String> userIds = userSignRecordDetailMapper.selectDistinctUserIdsInDateRange(startDate, syncEndDate);
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        result.setTotalInDb(userIds.size());
        for (String userId : userIds) {
            if (StringTools.isEmpty(userId)) {
                continue;
            }
            List<UserSignRecordDetail> details = userSignRecordDetailMapper.selectByUserIdAndDateRange(
                    userId, startDate, syncEndDate);
            if (details == null || details.isEmpty()) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            supplementDetailsToRedis(userId, details, result);
            hydrateHashWithoutDowngrade(userId, details);
            redisComponent.clearSignNullCache(userId);
            result.setSyncedUsers(result.getSyncedUsers() + 1);
        }
        result.setSyncedDates(result.getSupplementedDates());
        log.info("签到日期批量同步完成 endDate={}, users={}, supplemented={}",
                syncEndDate, result.getSyncedUsers(), result.getSupplementedDates());
        return result;
    }

    @Override
    public SignDateSyncResultVO forceRebuildToday(String userId, String operatorAccount) {
        String endDate = todayYmd();
        String startDate = rebuildStartDate(endDate);
        SignDateSyncResultVO result = new SignDateSyncResultVO();

        Set<String> targetUserIds = resolveForceRebuildUserIds(userId, startDate, endDate);
        result.setTotalInDb(targetUserIds.size());

        List<String> diffNotes = new ArrayList<>();
        for (String uid : targetUserIds) {
            forceRebuildUserSupplementOnly(uid, startDate, endDate, result, diffNotes);
        }

        String detail = String.format(
                "users=%d, supplemented=%d, diff=%d, notes=%s",
                result.getSyncedUsers(),
                result.getSupplementedDates(),
                result.getDiffCount(),
                diffNotes.isEmpty() ? "none" : String.join(";", diffNotes));
        log.warn("签到强制重建(只增不减) operator={}, action={}, {}", operatorAccount, AUDIT_ACTION_FORCE_REBUILD, detail);
        adminAuditFeignSupport.log(
                operatorAccount,
                AUDIT_ACTION_FORCE_REBUILD,
                StringTools.isEmpty(userId) ? null : userId.trim(),
                detail);
        result.setSyncedDates(result.getSupplementedDates());
        return result;
    }

    @Override
    public int reconcileRecentHour() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -1);
        Date since = cal.getTime();
        List<UserSignRecordDetail> rows = userSignRecordDetailMapper.selectCreatedAfter(since);
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        int repaired = 0;
        for (UserSignRecordDetail row : rows) {
            if (row == null || StringTools.isEmpty(row.getUserId()) || StringTools.isEmpty(row.getSignDate())) {
                continue;
            }
            if (redisComponent.ensureSignBitmapBit(row.getUserId(), row.getSignDate())) {
                repaired++;
            }
        }
        if (repaired > 0) {
            log.info("签到 Redis 对账补回 {} 条", repaired);
        }
        return repaired;
    }

    @Override
    public int initTodayBitmapForActiveUsers() {
        List<UserSignRecord> users = userSignRecordMapper.selectList(new UserSignRecordQuery());
        if (users == null || users.isEmpty()) {
            return 0;
        }
        int initialized = 0;
        LocalDate today = LocalDate.now();
        String yyyyMM = today.format(YM);
        int dayOfMonth = today.getDayOfMonth();
        for (UserSignRecord user : users) {
            if (user == null || StringTools.isEmpty(user.getUserId())) {
                continue;
            }
            if (redisComponent.initTodaySignBitmapIfAbsent(user.getUserId(), yyyyMM, dayOfMonth)) {
                initialized++;
            }
        }
        log.info("签到每日初始化完成，处理 {} 个活跃用户", initialized);
        return initialized;
    }

    @Override
    public void applyDetailsToRedis(String userId, List<UserSignRecordDetail> details) {
        supplementDetailsToRedis(userId, details, null);
    }

    private void supplementDetailsToRedis(String userId, List<UserSignRecordDetail> details,
                                          SignDateSyncResultVO result) {
        if (StringTools.isEmpty(userId) || details == null || details.isEmpty()) {
            return;
        }
        for (UserSignRecordDetail detail : details) {
            if (detail == null || StringTools.isEmpty(detail.getSignDate()) || detail.getSignDate().length() != 8) {
                continue;
            }
            if (redisComponent.ensureSignBitmapBit(userId, detail.getSignDate()) && result != null) {
                result.setSupplementedDates(result.getSupplementedDates() + 1);
            }
        }
    }

    private void forceRebuildUserSupplementOnly(String userId, String startDate, String endDate,
                                                SignDateSyncResultVO result, List<String> diffNotes) {
        List<UserSignRecordDetail> details = userSignRecordDetailMapper.selectByUserIdAndDateRange(
                userId, startDate, endDate);
        if (details == null) {
            details = List.of();
        }

        validateTodayDiff(userId, details, result, diffNotes);
        supplementDetailsToRedis(userId, details, result);
        result.setSyncedUsers(result.getSyncedUsers() + 1);
    }

    private void validateTodayDiff(String userId, List<UserSignRecordDetail> details,
                                   SignDateSyncResultVO result, List<String> diffNotes) {
        String today = todayYmd();
        boolean redisToday = isSignedOnDate(userId, today);
        boolean mysqlToday = details.stream().anyMatch(d -> d != null && today.equals(d.getSignDate()));

        if (redisToday && !mysqlToday) {
            result.setDiffCount(result.getDiffCount() + 1);
            String note = userId + ":today redis=1 mysql=0 kept-redis";
            diffNotes.add(note);
            log.warn("签到强制重建差异(保留Redis) {}", note);
        } else if (!redisToday && mysqlToday) {
            result.setDiffCount(result.getDiffCount() + 1);
            String note = userId + ":today redis=0 mysql=1 will-supplement";
            diffNotes.add(note);
            log.info("签到强制重建差异(将补缺) {}", note);
        }
    }

    private Set<String> resolveForceRebuildUserIds(String userId, String startDate, String endDate) {
        Set<String> userIds = new LinkedHashSet<>();
        if (!StringTools.isEmpty(userId)) {
            userIds.add(userId.trim());
            return userIds;
        }
        List<String> fromDetail = userSignRecordDetailMapper.selectDistinctUserIdsInDateRange(startDate, endDate);
        if (fromDetail != null) {
            userIds.addAll(fromDetail);
        }
        List<UserSignRecord> summaries = userSignRecordMapper.selectList(new UserSignRecordQuery());
        if (summaries != null) {
            for (UserSignRecord row : summaries) {
                if (row != null && !StringTools.isEmpty(row.getUserId())) {
                    userIds.add(row.getUserId());
                }
            }
        }
        return userIds;
    }

    private void hydrateHashWithoutDowngrade(String userId, List<UserSignRecordDetail> details) {
        int redisTotal = safeInt(redisComponent.totalSignDays(userId));
        int redisContinuous = safeInt(redisComponent.getContinuousDays(userId));
        int redisUsed = safeInt(redisComponent.getUsedCount(userId));

        UserSignRecord record = userSignRecordMapper.selectByUserId(userId);
        if (record != null) {
            redisComponent.writeSignHash(
                    userId,
                    Math.max(redisContinuous, safeInt(record.getContinuousDays())),
                    Math.max(redisTotal, safeInt(record.getTotalSignDays())),
                    Math.max(redisUsed, safeInt(record.getUsedCount())));
            return;
        }

        int usedFromDetails = 0;
        if (details != null) {
            for (UserSignRecordDetail detail : details) {
                if (detail != null && detail.getSignType() != null && detail.getSignType() == 1) {
                    usedFromDetails++;
                }
            }
        }
        int totalFromDetails = details == null ? 0 : details.size();
        redisComponent.writeSignHash(
                userId,
                Math.max(redisContinuous, 0),
                Math.max(redisTotal, totalFromDetails),
                Math.max(redisUsed, usedFromDetails));
    }

    private void rebuildWithLock(String userId, String endDate, boolean forceIncludeToday) {
        String lockKey = redisComponent.signRebuildLockKey(userId);
        String token = UUID.randomUUID() + ":" + Thread.currentThread().getId();
        boolean locked = redisComponent.setIfAbsent(lockKey, token,
                Constants.SIGN_REBUILD_LOCK_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        if (locked) {
            try {
                if (redisComponent.hasSignHashSnapshot(userId)) {
                    return;
                }
                doRebuildFromDb(userId, endDate, forceIncludeToday);
            } finally {
                // 仅当自己仍是持有者时删除（防 TTL 过期后被他人获取时误删他人锁）
                redisComponent.deleteIfOwned(lockKey, token);
            }
            return;
        }
        for (int i = 0; i < 50; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (redisComponent.hasSignHashSnapshot(userId)) {
                return;
            }
        }
        redisComponent.setSignNullCache(userId);
    }

    private void doRebuildFromDb(String userId, String endDate, boolean forceIncludeToday) {
        if (!forceIncludeToday) {
            validateSyncEndDate(endDate, false);
        }
        String startDate = rebuildStartDate(endDate);
        List<UserSignRecordDetail> details = userSignRecordDetailMapper.selectByUserIdAndDateRange(
                userId, startDate, endDate);
        if (details == null || details.isEmpty()) {
            redisComponent.setSignNullCache(userId);
            return;
        }
        SignDateSyncResultVO scratch = new SignDateSyncResultVO();
        supplementDetailsToRedis(userId, details, scratch);
        hydrateHashWithoutDowngrade(userId, details);
        redisComponent.clearSignNullCache(userId);
        log.info("签到日历已从 DB 重建 userId={}, dates={}, supplemented={}",
                userId, details.size(), scratch.getSupplementedDates());
    }

    private boolean isSignedOnDate(String userId, String yyyyMMdd) {
        String yyyyMM = yyyyMMdd.substring(0, 6);
        int day = Integer.parseInt(yyyyMMdd.substring(6, 8));
        return Boolean.TRUE.equals(redisComponent.isSign(userId, yyyyMM, day));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void validateSyncEndDate(String syncEndDate, boolean forceIncludeToday) {
        if (StringTools.isEmpty(syncEndDate) || syncEndDate.length() != 8) {
            throw new BusinessException("syncEndDate 格式应为 yyyyMMdd");
        }
        LocalDate end = LocalDate.parse(syncEndDate, YMD);
        LocalDate today = LocalDate.now();
        if (!forceIncludeToday && !end.isBefore(today)) {
            throw new BusinessException("手动同步仅允许同步昨天及更早的签到日期，禁止同步今天");
        }
        if (forceIncludeToday && end.isAfter(today)) {
            throw new BusinessException("同步截止日期不能晚于今天");
        }
    }

    private String rebuildStartDate(String endDate) {
        LocalDate end = LocalDate.parse(endDate, YMD);
        return end.minusDays(Constants.SIGN_CALENDAR_REBUILD_DAYS - 1L).format(YMD);
    }

    private String todayYmd() {
        return LocalDate.now().format(YMD);
    }
}
