package com.simlect.biz.impl;

import com.simlect.entity.po.UserCoupon;
import com.simlect.entity.query.UserCouponQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.mappers.UserCouponMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserCouponServiceImpl 用户券 CRUD/分页单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserCouponServiceImplTest {

    @Mock
    private UserCouponMapper<UserCoupon, UserCouponQuery> userCouponMapper;

    @InjectMocks
    private UserCouponServiceImpl userCouponService;

    @Test
    void findListByPage_buildsPagination() {
        UserCouponQuery query = new UserCouponQuery();
        query.setPageNo(1);
        when(userCouponMapper.selectCount(query)).thenReturn(16);
        when(userCouponMapper.selectList(query)).thenReturn(List.of(new UserCoupon()));

        PaginationResultVO<UserCoupon> page = userCouponService.findListByPage(query);

        assertEquals(16, page.getTotalCount());
        assertEquals(15, page.getPageSize());
        assertEquals(2, page.getPageTotal());
        assertEquals(1, page.getList().size());
    }

    @Test
    void addBatch_nullOrEmpty_returnsZero() {
        assertEquals(0, userCouponService.addBatch(null));
        assertEquals(0, userCouponService.addBatch(Collections.emptyList()));
        verify(userCouponMapper, never()).insertBatch(any());
    }

    @Test
    void addOrUpdateBatch_nullOrEmpty_returnsZero() {
        assertEquals(0, userCouponService.addOrUpdateBatch(null));
        assertEquals(0, userCouponService.addOrUpdateBatch(Collections.emptyList()));
        verify(userCouponMapper, never()).insertOrUpdateBatch(any());
    }

    @Test
    void basicCrud_delegates() {
        UserCoupon coupon = new UserCoupon();
        coupon.setUserCouponId("UC1");
        UserCouponQuery query = new UserCouponQuery();
        query.setUserCouponId("UC1");
        when(userCouponMapper.insert(coupon)).thenReturn(1);
        when(userCouponMapper.updateByParam(coupon, query)).thenReturn(2);
        when(userCouponMapper.deleteByParam(query)).thenReturn(3);
        when(userCouponMapper.selectByUserCouponId("UC1")).thenReturn(coupon);
        when(userCouponMapper.updateByUserCouponId(coupon, "UC1")).thenReturn(4);
        when(userCouponMapper.deleteByUserCouponId("UC1")).thenReturn(5);
        when(userCouponMapper.selectList(query)).thenReturn(List.of(coupon));
        when(userCouponMapper.selectCount(query)).thenReturn(6);

        assertEquals(1, userCouponService.add(coupon));
        assertEquals(2, userCouponService.updateByParam(coupon, query));
        assertEquals(3, userCouponService.deleteByParam(query));
        assertSame(coupon, userCouponService.getUserCouponByUserCouponId("UC1"));
        assertEquals(4, userCouponService.updateUserCouponByUserCouponId(coupon, "UC1"));
        assertEquals(5, userCouponService.deleteUserCouponByUserCouponId("UC1"));
        assertNotNull(userCouponService.findListByPage(query));
        assertNotNull(userCouponService.findListByParam(query));
        assertEquals(6, userCouponService.findCountByParam(query));
    }
}
