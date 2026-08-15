package com.simlect.biz.impl;

import com.simlect.entity.po.UserAddress;
import com.simlect.entity.query.UserAddressQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserAddressMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceImplTest {

    @Mock
    private UserAddressMapper<UserAddress, UserAddressQuery> userAddressMapper;

    @InjectMocks
    private UserAddressServiceImpl userAddressService;

    private UserAddress buildAddress() {
        UserAddress address = new UserAddress();
        address.setAddressId("addr1");
        address.setUserId("U1");
        address.setDefaultType(0);
        return address;
    }

    @Test
    void findListByPage_defaultPageSize15() {
        when(userAddressMapper.selectCount(any())).thenReturn(20);
        when(userAddressMapper.selectList(any())).thenReturn(new ArrayList<>());
        UserAddressQuery query = new UserAddressQuery();
        query.setPageNo(1);

        PaginationResultVO<UserAddress> result = userAddressService.findListByPage(query);

        assertEquals(20, result.getTotalCount());
        assertEquals(15, result.getPageSize());
        assertNotNull(query.getSimplePage());
    }

    @Test
    void saveAddress_add_generatesIdAndDefaultsType() {
        UserAddress address = buildAddress();
        address.setAddressId(null);
        address.setDefaultType(null);

        userAddressService.saveAddress(address);

        assertNotNull(address.getAddressId());
        assertEquals(15, address.getAddressId().length());
        assertEquals(0, address.getDefaultType());
        verify(userAddressMapper).insert(address);
        verify(userAddressMapper, never()).cleanAllDefaultType(anyString());
    }

    @Test
    void saveAddress_addAsDefault_cleansOthers() {
        UserAddress address = buildAddress();
        address.setAddressId(null);
        address.setDefaultType(1);

        userAddressService.saveAddress(address);

        verify(userAddressMapper).cleanAllDefaultType("U1");
        verify(userAddressMapper).insert(address);
    }

    @Test
    void saveAddress_updateAsDefault_cleansOthersAndUpdates() {
        when(userAddressMapper.selectByAddressId("addr1")).thenReturn(buildAddress());
        UserAddress address = buildAddress();
        address.setDefaultType(1);

        userAddressService.saveAddress(address);

        verify(userAddressMapper).cleanAllDefaultType("U1");
        verify(userAddressMapper).updateByAddressId(address, "addr1");
    }

    @Test
    void saveAddress_updateNotOwned_throws() {
        // 他人地址：水平越权拦截
        UserAddress other = buildAddress();
        other.setUserId("U2");
        when(userAddressMapper.selectByAddressId("addr1")).thenReturn(other);
        UserAddress bean = buildAddress();

        BusinessException e = assertThrows(BusinessException.class,
                () -> userAddressService.saveAddress(bean));

        assertTrue(e.getMessage().contains("不属于该用户"));
        verify(userAddressMapper, never()).updateByAddressId(any(), anyString());
    }

    @Test
    void saveAddress_updateNonexistent_throws() {
        when(userAddressMapper.selectByAddressId("addr1")).thenReturn(null);
        UserAddress bean = buildAddress();

        assertThrows(BusinessException.class, () -> userAddressService.saveAddress(bean));
        verify(userAddressMapper, never()).updateByAddressId(any(), anyString());
    }

    @Test
    void saveAddress_updateWithForeignUserId_throws() {
        // 试图把地址过户到他人名下：归属校验直接拦截
        when(userAddressMapper.selectByAddressId("addr1")).thenReturn(buildAddress());
        UserAddress bean = buildAddress();
        bean.setUserId("U-HACKED");

        assertThrows(BusinessException.class, () -> userAddressService.saveAddress(bean));
        verify(userAddressMapper, never()).updateByAddressId(any(), anyString());
    }

    @Test
    void updateDefault_notOwned_throws() {
        when(userAddressMapper.selectByAddressId("addr1")).thenReturn(null);

        assertThrows(BusinessException.class, () -> userAddressService.updateDefault("U1", "addr1"));

        UserAddress other = buildAddress();
        other.setUserId("U2");
        when(userAddressMapper.selectByAddressId("addr1")).thenReturn(other);
        BusinessException e = assertThrows(BusinessException.class,
                () -> userAddressService.updateDefault("U1", "addr1"));
        assertTrue(e.getMessage().contains("不属于该用户"));
    }

    @Test
    void updateDefault_owned_cleansAndSetsDefault() {
        when(userAddressMapper.selectByAddressId("addr1")).thenReturn(buildAddress());

        userAddressService.updateDefault("U1", "addr1");

        verify(userAddressMapper).cleanAllDefaultType("U1");
        verify(userAddressMapper).updateDefaultType("addr1");
    }

    @Test
    void deleteUserAddress_notOwned_throws() {
        UserAddress other = buildAddress();
        other.setUserId("U2");
        when(userAddressMapper.selectByAddressId("addr1")).thenReturn(other);

        assertThrows(BusinessException.class, () -> userAddressService.deleteUserAddress("U1", "addr1"));
        verify(userAddressMapper, never()).deleteByAddressId(anyString());
    }

    @Test
    void deleteUserAddress_owned_deletes() {
        when(userAddressMapper.selectByAddressId("addr1")).thenReturn(buildAddress());

        userAddressService.deleteUserAddress("U1", "addr1");

        verify(userAddressMapper).deleteByAddressId("addr1");
    }
}
