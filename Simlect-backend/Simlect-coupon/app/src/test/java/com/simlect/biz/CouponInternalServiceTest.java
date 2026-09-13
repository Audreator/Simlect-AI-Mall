package com.simlect.biz;

import com.simlect.api.dto.CouponValidateAndLockDTO;
import com.simlect.api.dto.NotificationMessageDTO;
import com.simlect.api.dto.SignStreakCouponMessageDTO;
import com.simlect.api.dto.UserCouponCreateDTO;
import com.simlect.api.dto.UserCouponStatusChangeDTO;
import com.simlect.api.enums.CouponStatusEnum;
import com.simlect.api.enums.CouponTypeEnum;
import com.simlect.api.enums.UserCouponStatusEnum;
import com.simlect.api.vo.CouponBriefVO;
import com.simlect.api.vo.CouponLockResultVO;
import com.simlect.api.vo.DiscountCouponVO;
import com.simlect.api.vo.UserCouponVO;
import com.simlect.component.CouponRushStockService;
import com.simlect.component.DiscountCouponCacheComponent;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.TransactionalMqSender;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.entity.po.DiscountCoupon;
import com.simlect.entity.po.SignStreakCouponGrant;
import com.simlect.entity.po.UserCoupon;
import com.simlect.entity.query.DiscountCouponQuery;
import com.simlect.entity.query.UserCouponQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.DiscountCouponMapper;
import com.simlect.mappers.SignStreakCouponGrantMapper;
import com.simlect.mappers.UserCouponMapper;
import com.simlect.support.MqIdempotencyKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CouponInternalService 内部优惠券服务（校验锁定/库存扣减/状态流转）单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CouponInternalServiceTest {

    @Mock
    private UserCouponMapper<UserCoupon, UserCouponQuery> userCouponMapper;
    @Mock
    private DiscountCouponMapper<DiscountCoupon, DiscountCouponQuery> discountCouponMapper;
    @Mock
    private CouponRushStockService couponRushStockService;
    @Mock
    private DiscountCouponService discountCouponService;
    @Mock
    private DiscountCouponCacheComponent discountCouponCacheComponent;
    @Mock
    private SignStreakCouponGrantMapper signStreakCouponGrantMapper;
    @Mock
    private TransactionalMqSender transactionalMqSender;

    @InjectMocks
    private CouponInternalService couponInternalService;

    private static final String USER_ID = "U1";
    private static final String USER_COUPON_ID = "UC1";
    private static final String COUPON_ID = "CP1";

    private UserCoupon userCoupon(int status) {
        UserCoupon uc = new UserCoupon();
        uc.setUserCouponId(USER_COUPON_ID);
        uc.setUserId(USER_ID);
        uc.setCouponId(COUPON_ID);
        uc.setStatus(status);
        return uc;
    }

    private DiscountCoupon discountCoupon(int type) {
        DiscountCoupon c = new DiscountCoupon();
        c.setCouponId(COUPON_ID);
        c.setCouponName("测试券");
        c.setCouponType(type);
        c.setThresholdAmount(new BigDecimal("100"));
        c.setDiscountAmount(new BigDecimal("20"));
        c.setDiscountRate(new BigDecimal("0.85"));
        return c;
    }

    private CouponValidateAndLockDTO lockDto(BigDecimal orderAmount) {
        CouponValidateAndLockDTO dto = new CouponValidateAndLockDTO();
        dto.setUserId(USER_ID);
        dto.setUserCouponId(USER_COUPON_ID);
        dto.setOrderAmount(orderAmount);
        return dto;
    }

    private SignStreakCouponMessageDTO streakCouponDto() {
        return new SignStreakCouponMessageDTO(USER_ID, COUPON_ID, 7);
    }

    private DiscountCoupon streakCoupon(boolean unlimited) {
        DiscountCoupon coupon = discountCoupon(CouponTypeEnum.FULL.getStatus());
        coupon.setStatus(CouponStatusEnum.NORMAL.getStatus());
        coupon.setTotalCount(unlimited ? 0 : 10);
        coupon.setRemainCount(unlimited ? 0 : 1);
        return coupon;
    }

    // ==================== validateAndLock ====================

    @Test
    void validateAndLock_userCouponNotExists_throws() {
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponInternalService.validateAndLock(lockDto(new BigDecimal("200"))));

        assertEquals("优惠券不存在", e.getMessage());
    }

    @Test
    void validateAndLock_userMismatch_throws() {
        UserCoupon uc = userCoupon(UserCouponStatusEnum.NOUSE.getStatus());
        uc.setUserId("OTHER");
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID)).thenReturn(uc);

        assertThrows(BusinessException.class,
                () -> couponInternalService.validateAndLock(lockDto(new BigDecimal("200"))));
    }

    @Test
    void validateAndLock_statusNotUsable_throws() {
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID))
                .thenReturn(userCoupon(UserCouponStatusEnum.USED.getStatus()));

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponInternalService.validateAndLock(lockDto(new BigDecimal("200"))));

        assertEquals("优惠券不可用", e.getMessage());
    }

    @Test
    void validateAndLock_couponMissing_throws() {
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID))
                .thenReturn(userCoupon(UserCouponStatusEnum.NOUSE.getStatus()));
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> couponInternalService.validateAndLock(lockDto(new BigDecimal("200"))));
    }

    @Test
    void validateAndLock_notStarted_throws() {
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID))
                .thenReturn(userCoupon(UserCouponStatusEnum.NOUSE.getStatus()));
        DiscountCoupon c = discountCoupon(CouponTypeEnum.FULL.getStatus());
        c.setValidStartTime(new Date(System.currentTimeMillis() + 3_600_000L));
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(c);

        assertThrows(BusinessException.class,
                () -> couponInternalService.validateAndLock(lockDto(new BigDecimal("200"))));
    }

    @Test
    void validateAndLock_expired_throws() {
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID))
                .thenReturn(userCoupon(UserCouponStatusEnum.NOUSE.getStatus()));
        DiscountCoupon c = discountCoupon(CouponTypeEnum.FULL.getStatus());
        c.setValidEndTime(new Date(System.currentTimeMillis() - 3_600_000L));
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(c);

        assertThrows(BusinessException.class,
                () -> couponInternalService.validateAndLock(lockDto(new BigDecimal("200"))));
    }

    @Test
    void validateAndLock_belowThreshold_throws() {
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID))
                .thenReturn(userCoupon(UserCouponStatusEnum.NOUSE.getStatus()));
        when(discountCouponMapper.selectByCouponId(COUPON_ID))
                .thenReturn(discountCoupon(CouponTypeEnum.FULL.getStatus()));

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponInternalService.validateAndLock(lockDto(new BigDecimal("50"))));

        assertEquals("未满足优惠券使用门槛", e.getMessage());
    }

    @Test
    void validateAndLock_optimisticLockFailed_throws() {
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID))
                .thenReturn(userCoupon(UserCouponStatusEnum.NOUSE.getStatus()));
        when(discountCouponMapper.selectByCouponId(COUPON_ID))
                .thenReturn(discountCoupon(CouponTypeEnum.FULL.getStatus()));
        when(userCouponMapper.updateByParam(any(), any())).thenReturn(0);

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponInternalService.validateAndLock(lockDto(new BigDecimal("200"))));

        assertEquals("优惠券已被使用", e.getMessage());
    }

    @Test
    void validateAndLock_success_locksUserCoupon() {
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID))
                .thenReturn(userCoupon(UserCouponStatusEnum.NOUSE.getStatus()));
        when(discountCouponMapper.selectByCouponId(COUPON_ID))
                .thenReturn(discountCoupon(CouponTypeEnum.FULL.getStatus()));
        when(userCouponMapper.updateByParam(any(), any())).thenReturn(1);

        CouponLockResultVO result = couponInternalService.validateAndLock(lockDto(new BigDecimal("200")));

        assertEquals(COUPON_ID, result.getCouponId());
        assertEquals(USER_COUPON_ID, result.getUserCouponId());
        assertEquals(0, result.getDiscountAmount().compareTo(new BigDecimal("20")));
        assertEquals(Boolean.TRUE, result.getLocked());
        verify(userCouponMapper).updateByParam(any(), any());
    }

    @Test
    void validateAndLock_zeroDiscount_returnsUnlocked() {
        DiscountCoupon c = discountCoupon(CouponTypeEnum.FULL.getStatus());
        c.setDiscountAmount(BigDecimal.ZERO);
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID))
                .thenReturn(userCoupon(UserCouponStatusEnum.NOUSE.getStatus()));
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(c);

        CouponLockResultVO result = couponInternalService.validateAndLock(lockDto(new BigDecimal("200")));

        assertEquals(0, result.getDiscountAmount().compareTo(BigDecimal.ZERO));
        assertEquals(Boolean.FALSE, result.getLocked());
        verify(userCouponMapper, never()).updateByParam(any(), any());
    }

    @Test
    void validateAndLock_discountType_computesDiscount() {
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID))
                .thenReturn(userCoupon(UserCouponStatusEnum.NOUSE.getStatus()));
        when(discountCouponMapper.selectByCouponId(COUPON_ID))
                .thenReturn(discountCoupon(CouponTypeEnum.DISCOUNT.getStatus()));
        when(userCouponMapper.updateByParam(any(), any())).thenReturn(1);

        CouponLockResultVO result = couponInternalService.validateAndLock(lockDto(new BigDecimal("200")));

        // 200 * (1-0.85) = 30，封顶不超过订单额
        assertEquals(new BigDecimal("30.00"), result.getDiscountAmount());
        assertEquals(Boolean.TRUE, result.getLocked());
    }

    // ==================== 查询 ====================

    @Test
    void getCoupon_notExists_returnsNull() {
        when(discountCouponMapper.selectByCouponId(COUPON_ID)).thenReturn(null);
        assertNull(couponInternalService.getCoupon(COUPON_ID));
    }

    @Test
    void getCoupon_exists_copiesProperties() {
        when(discountCouponMapper.selectByCouponId(COUPON_ID))
                .thenReturn(discountCoupon(CouponTypeEnum.FULL.getStatus()));

        DiscountCouponVO vo = couponInternalService.getCoupon(COUPON_ID);

        assertEquals(COUPON_ID, vo.getCouponId());
        assertEquals("测试券", vo.getCouponName());
    }

    @Test
    void getCouponBrief_returnsBrief() {
        when(discountCouponMapper.selectByCouponId(COUPON_ID))
                .thenReturn(discountCoupon(CouponTypeEnum.FULL.getStatus()));

        CouponBriefVO vo = couponInternalService.getCouponBrief(COUPON_ID);

        assertEquals(COUPON_ID, vo.getCouponId());
        assertEquals("测试券", vo.getCouponName());
        assertEquals(CouponTypeEnum.FULL.getStatus(), vo.getCouponType());
    }

    @Test
    void getUserCoupon_exists_copiesProperties() {
        when(userCouponMapper.selectByUserCouponId(USER_COUPON_ID))
                .thenReturn(userCoupon(UserCouponStatusEnum.NOUSE.getStatus()));

        UserCouponVO vo = couponInternalService.getUserCoupon(USER_COUPON_ID);

        assertEquals(USER_ID, vo.getUserId());
        assertEquals(COUPON_ID, vo.getCouponId());
    }

    // ==================== 状态流转 ====================

    @Test
    void changeUserCouponStatus_updated_returnsSilently() {
        when(userCouponMapper.updateByParam(any(), any())).thenReturn(1);
        UserCouponStatusChangeDTO dto = new UserCouponStatusChangeDTO();
        dto.setUserCouponId(USER_COUPON_ID);
        dto.setUserId(USER_ID);
        dto.setFromStatus(UserCouponStatusEnum.NOUSE.getStatus());
        dto.setToStatus(UserCouponStatusEnum.USED.getStatus());

        couponInternalService.changeUserCouponStatus(dto);

        verify(userCouponMapper).updateByParam(any(), any());
    }

    @Test
    void changeUserCouponStatus_noRows_throws() {
        when(userCouponMapper.updateByParam(any(), any())).thenReturn(0);
        UserCouponStatusChangeDTO dto = new UserCouponStatusChangeDTO();
        dto.setUserCouponId(USER_COUPON_ID);
        dto.setUserId(USER_ID);
        dto.setFromStatus(0);
        dto.setToStatus(1);

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponInternalService.changeUserCouponStatus(dto));

        assertEquals("用户券状态更新失败", e.getMessage());
    }

    @Test
    void createUserCoupon_inserts() {
        UserCouponCreateDTO dto = new UserCouponCreateDTO();
        dto.setUserCouponId(USER_COUPON_ID);
        dto.setUserId(USER_ID);
        dto.setCouponId(COUPON_ID);
        dto.setStatus(UserCouponStatusEnum.NOUSE.getStatus());

        couponInternalService.createUserCoupon(dto);

        verify(userCouponMapper).insert(any(UserCoupon.class));
    }

    // ==================== 库存扣减 ====================

    @Test
    void deductStock_emptyCouponId_throws() {
        assertThrows(BusinessException.class, () -> couponInternalService.deductStock(""));
    }

    @Test
    void deductStock_couponNotExists_throws() {
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponInternalService.deductStock(COUPON_ID));

        assertEquals("优惠券不存在", e.getMessage());
    }

    @Test
    void deductStock_success_returnsAffectedRows() {
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID))
                .thenReturn(discountCoupon(CouponTypeEnum.FULL.getStatus()));
        when(discountCouponMapper.deductStock(COUPON_ID)).thenReturn(1);

        assertEquals(1, couponInternalService.deductStock(COUPON_ID));
    }

    @Test
    void deductStock_unlimitedCoupon_returnsSuccessWithoutChangingCounter() {
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID))
                .thenReturn(streakCoupon(true));

        assertEquals(1, couponInternalService.deductStock(COUPON_ID));

        verify(discountCouponMapper, never()).deductStock(anyString());
    }

    @Test
    void deductStock_nullAffected_returnsZero() {
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID))
                .thenReturn(discountCoupon(CouponTypeEnum.FULL.getStatus()));
        when(discountCouponMapper.deductStock(COUPON_ID)).thenReturn(null);

        assertEquals(0, couponInternalService.deductStock(COUPON_ID));
    }

    // ==================== 秒杀相关委托 ====================

    @Test
    void rushOps_delegateToComponents() {
        couponInternalService.assertRushNotBlocked(COUPON_ID);
        verify(couponRushStockService).assertRushNotBlocked(COUPON_ID);

        when(couponRushStockService.hasAvailableStock(COUPON_ID)).thenReturn(true);
        assertTrue(couponInternalService.hasAvailableRushStock(COUPON_ID));

        couponInternalService.syncRushStockFromDbIfRedisZero(COUPON_ID);
        verify(couponRushStockService).syncFromDbIfRedisZero(COUPON_ID);

        couponInternalService.releaseRushRedisReserve(COUPON_ID, USER_ID);
        verify(discountCouponService).releaseRushRedisReserve(COUPON_ID, USER_ID);

        couponInternalService.releaseRushCouponReserve(COUPON_ID, USER_ID);
        verify(discountCouponService).releaseRushCouponReserve(COUPON_ID, USER_ID);

        couponInternalService.invalidateCouponCache(COUPON_ID);
        verify(discountCouponCacheComponent).invalidateAfterWrite(COUPON_ID);
    }

    // ==================== 连续签到奖励 ====================

    @Test
    void grantSignStreakCoupon_invalidPayload_throwsBeforeWriting() {
        SignStreakCouponMessageDTO dto = new SignStreakCouponMessageDTO(USER_ID, COUPON_ID, 0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> couponInternalService.grantSignStreakCoupon(dto));

        assertEquals("连续签到奖励消息参数错误", exception.getMessage());
        verify(signStreakCouponGrantMapper, never()).insertPending(any());
    }

    @Test
    void grantSignStreakCoupon_duplicateGranted_returnsExistingResult() {
        SignStreakCouponGrant existing = new SignStreakCouponGrant();
        existing.setStatus(1);
        existing.setUserCouponId(USER_COUPON_ID);
        when(signStreakCouponGrantMapper.insertPending(any())).thenReturn(0);
        when(signStreakCouponGrantMapper.selectByIdempotencyKey(
                MqIdempotencyKeys.signStreakCoupon(USER_ID, COUPON_ID, 7))).thenReturn(existing);

        CouponInternalService.StreakCouponGrantResult result =
                couponInternalService.grantSignStreakCoupon(streakCouponDto());

        assertEquals("ALREADY_GRANTED", result.outcome());
        assertEquals(USER_COUPON_ID, result.userCouponId());
        verify(discountCouponMapper, never()).selectByCouponIdForUpdate(anyString());
    }

    @Test
    void grantSignStreakCoupon_duplicateRejected_returnsExistingReason() {
        SignStreakCouponGrant existing = new SignStreakCouponGrant();
        existing.setStatus(2);
        existing.setRejectReason("奖励优惠券库存不足");
        when(signStreakCouponGrantMapper.insertPending(any())).thenReturn(0);
        when(signStreakCouponGrantMapper.selectByIdempotencyKey(anyString())).thenReturn(existing);

        CouponInternalService.StreakCouponGrantResult result =
                couponInternalService.grantSignStreakCoupon(streakCouponDto());

        assertEquals("ALREADY_REJECTED", result.outcome());
        assertEquals("奖励优惠券库存不足", result.reason());
        verify(discountCouponMapper, never()).selectByCouponIdForUpdate(anyString());
    }

    @Test
    void grantSignStreakCoupon_existingPending_throwsForRetry() {
        SignStreakCouponGrant existing = new SignStreakCouponGrant();
        existing.setStatus(0);
        when(signStreakCouponGrantMapper.insertPending(any())).thenReturn(0);
        when(signStreakCouponGrantMapper.selectByIdempotencyKey(anyString())).thenReturn(existing);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> couponInternalService.grantSignStreakCoupon(streakCouponDto()));

        assertTrue(exception.getMessage().contains("长期处于处理中"));
    }

    @Test
    void grantSignStreakCoupon_missingCoupon_recordsBusinessRejection() {
        when(signStreakCouponGrantMapper.insertPending(any())).thenReturn(1);
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID)).thenReturn(null);
        when(signStreakCouponGrantMapper.markRejected(anyString(), anyString(), any(Date.class))).thenReturn(1);

        CouponInternalService.StreakCouponGrantResult result =
                couponInternalService.grantSignStreakCoupon(streakCouponDto());

        assertEquals("REJECTED", result.outcome());
        assertEquals("奖励优惠券不存在", result.reason());
        verify(signStreakCouponGrantMapper).markRejected(
                eq(MqIdempotencyKeys.signStreakCoupon(USER_ID, COUPON_ID, 7)),
                eq("奖励优惠券不存在"), any(Date.class));
        verify(userCouponMapper, never()).insert(any());
    }

    @Test
    void grantSignStreakCoupon_limitedStock_successWritesCouponAndNotification() {
        when(signStreakCouponGrantMapper.insertPending(any())).thenReturn(1);
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID)).thenReturn(streakCoupon(false));
        when(discountCouponMapper.deductStock(COUPON_ID)).thenReturn(1);
        when(userCouponMapper.insert(any())).thenReturn(1);
        when(signStreakCouponGrantMapper.markGranted(anyString(), anyString(), any(Date.class))).thenReturn(1);

        CouponInternalService.StreakCouponGrantResult result =
                couponInternalService.grantSignStreakCoupon(streakCouponDto());

        assertEquals("GRANTED", result.outcome());
        assertNotNull(result.userCouponId());
        verify(discountCouponMapper).deductStock(COUPON_ID);
        ArgumentCaptor<UserCoupon> userCouponCaptor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).insert(userCouponCaptor.capture());
        UserCoupon created = userCouponCaptor.getValue();
        assertEquals(USER_ID, created.getUserId());
        assertEquals(COUPON_ID, created.getCouponId());
        assertEquals(UserCouponStatusEnum.NOUSE.getStatus(), created.getStatus());
        assertNotNull(created.getReceiveTime());
        assertEquals(result.userCouponId(), created.getUserCouponId());
        verify(transactionalMqSender).sendAfterCommit(
                eq(RabbitMQConfig.NOTIFY_EXCHANGE),
                eq(RabbitMQConfig.NOTIFY_KEY),
                any(NotificationMessageDTO.class),
                eq(MqIdempotencyKeys.notification(USER_ID, "sign_reward", result.userCouponId())),
                eq(MessageReliabilityLevelEnum.HIGH));
    }

    @Test
    void grantSignStreakCoupon_unlimitedStock_doesNotDeductCounter() {
        when(signStreakCouponGrantMapper.insertPending(any())).thenReturn(1);
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID)).thenReturn(streakCoupon(true));
        when(userCouponMapper.insert(any())).thenReturn(1);
        when(signStreakCouponGrantMapper.markGranted(anyString(), anyString(), any(Date.class))).thenReturn(1);

        CouponInternalService.StreakCouponGrantResult result =
                couponInternalService.grantSignStreakCoupon(streakCouponDto());

        assertEquals("GRANTED", result.outcome());
        verify(discountCouponMapper, never()).deductStock(anyString());
        verify(userCouponMapper).insert(any(UserCoupon.class));
    }

    @Test
    void grantSignStreakCoupon_stockRace_recordsRejectionWithoutCreatingCoupon() {
        when(signStreakCouponGrantMapper.insertPending(any())).thenReturn(1);
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID)).thenReturn(streakCoupon(false));
        when(discountCouponMapper.deductStock(COUPON_ID)).thenReturn(0);
        when(signStreakCouponGrantMapper.markRejected(anyString(), anyString(), any(Date.class))).thenReturn(1);

        CouponInternalService.StreakCouponGrantResult result =
                couponInternalService.grantSignStreakCoupon(streakCouponDto());

        assertEquals("REJECTED", result.outcome());
        assertEquals("奖励优惠券库存不足", result.reason());
        verify(userCouponMapper, never()).insert(any());
        verify(transactionalMqSender, never()).sendAfterCommit(
                anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void grantSignStreakCoupon_userCouponWriteFailure_throwsForTransactionRollback() {
        when(signStreakCouponGrantMapper.insertPending(any())).thenReturn(1);
        when(discountCouponMapper.selectByCouponIdForUpdate(COUPON_ID)).thenReturn(streakCoupon(false));
        when(discountCouponMapper.deductStock(COUPON_ID)).thenReturn(1);
        when(userCouponMapper.insert(any())).thenReturn(0);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> couponInternalService.grantSignStreakCoupon(streakCouponDto()));

        assertEquals("连续签到奖励用户券写入失败", exception.getMessage());
        verify(signStreakCouponGrantMapper, never()).markGranted(anyString(), anyString(), any(Date.class));
        verify(transactionalMqSender, never()).sendAfterCommit(
                anyString(), anyString(), any(), anyString(), any());
    }
}
