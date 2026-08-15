package com.simlect.task;

import com.simlect.biz.CouponReminderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * CouponReminderTask 定时任务单元测试：正常执行与异常吞掉。
 */
@ExtendWith(MockitoExtension.class)
class CouponReminderTaskTest {

    @Mock
    private CouponReminderService couponReminderService;

    @InjectMocks
    private CouponReminderTask couponReminderTask;

    @Test
    void remindExpiringCoupons_delegates() {
        couponReminderTask.remindExpiringCoupons();
        verify(couponReminderService).remindExpiringCoupons();
    }

    @Test
    void remindExpiringCoupons_exception_swallowed() {
        doThrow(new IllegalStateException("boom")).when(couponReminderService).remindExpiringCoupons();

        couponReminderTask.remindExpiringCoupons();
    }
}
