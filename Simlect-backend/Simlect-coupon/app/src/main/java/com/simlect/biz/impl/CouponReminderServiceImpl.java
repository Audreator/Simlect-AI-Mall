package com.simlect.biz.impl;

import com.simlect.api.support.UserFeignSupport;
import com.simlect.mappers.UserCouponMapper;
import com.simlect.biz.CouponReminderService;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("couponReminderService")
@Slf4j
public class CouponReminderServiceImpl implements CouponReminderService {

    private static final int BATCH_LIMIT = 500;

    @Resource
    private UserCouponMapper<com.simlect.entity.po.UserCoupon, com.simlect.entity.query.UserCouponQuery> userCouponMapper;
    @Resource
    private UserFeignSupport userFeignSupport;
    @Resource
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Override
    public void remindExpiringCoupons() {
        // keyset 游标分页处理全部即将过期券（防 limit 截断漏提醒）+ Redis 7 天去重（防每日重复打扰）
        int sent = 0;
        java.util.Date cursorEndTime = null;
        String cursorUserCouponId = null;
        while (true) {
            List<Map<String, Object>> rows = userCouponMapper.selectExpiringUnusedByCursor(
                    BATCH_LIMIT, cursorEndTime, cursorUserCouponId);
            if (rows == null || rows.isEmpty()) {
                break;
            }
            for (Map<String, Object> row : rows) {
                String userId = stringVal(row.get("userId"));
                String userCouponId = stringVal(row.get("userCouponId"));
                String couponName = stringVal(row.get("couponName"));
                if (StringTools.isEmpty(userId) || StringTools.isEmpty(userCouponId)) {
                    continue;
                }
                // 7 天内同一张券只提醒一次
                String remindKey = "mall:coupon:remind:" + userCouponId;
                Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(
                        remindKey, "1", 7, java.util.concurrent.TimeUnit.DAYS);
                if (!Boolean.TRUE.equals(first)) {
                    continue;
                }
                String title = "优惠券即将过期";
                String content = "您的「" + (StringTools.isEmpty(couponName) ? "优惠券" : couponName)
                        + "」将在 3 天内过期，请尽快使用";
                userFeignSupport.sendNotifyAsync(userId, title, content, "coupon_expire", userCouponId);
                sent++;
            }
            // 游标推进：每批无条件用最后一行推进（含全部命中去重键的批，防游标不推进导致死循环）
            Map<String, Object> last = rows.get(rows.size() - 1);
            cursorEndTime = (java.util.Date) last.get("validEndTime");
            cursorUserCouponId = stringVal(last.get("userCouponId"));
            if (rows.size() < BATCH_LIMIT) {
                break;
            }
        }
        log.info("优惠券即将过期提醒完成，处理 {} 条", sent);
    }

    private static String stringVal(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
