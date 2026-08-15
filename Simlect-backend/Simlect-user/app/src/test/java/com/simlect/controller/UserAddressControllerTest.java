package com.simlect.controller;

import com.simlect.biz.UserAddressService;
import com.simlect.component.RedisComponent;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.UserAddress;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserAddressControllerTest {

    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;
    @Mock
    private UserAddressService userAddressService;

    @InjectMocks
    private UserAddressController userAddressController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userAddressController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
        when(authCookieHelper.resolveWebToken(any())).thenReturn("tok");
        TokenUserInfoDTO tokenInfo = new TokenUserInfoDTO();
        tokenInfo.setToken("tok");
        tokenInfo.setUserId("U1");
        when(redisComponent.getTokenUserInfo("tok")).thenReturn(tokenInfo);
    }

    @Test
    void loadDataList_returnsUserAddresses() throws Exception {
        UserAddress address = new UserAddress();
        address.setAddressId("addr1");
        when(userAddressService.findListByParam(any())).thenReturn(List.of(address));

        mockMvc.perform(get("/userAddress/loadDataList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].addressId").value("addr1"));

        verify(userAddressService).findListByParam(argThat(q -> "U1".equals(q.getUserId())));
    }

    @Test
    void addAddress_setsUserIdAndSaves() throws Exception {
        mockMvc.perform(post("/userAddress/addAddress")
                        .param("address", "北京市朝阳区")
                        .param("addressee", "张三")
                        .param("phone", "13800000000")
                        .param("defaultType", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(userAddressService).saveAddress(argThat(a ->
                "U1".equals(a.getUserId()) && "北京市朝阳区".equals(a.getAddress())));
    }

    @Test
    void updateAddress_setsUserIdAndSaves() throws Exception {
        mockMvc.perform(post("/userAddress/updateAddress")
                        .param("addressId", "addr1")
                        .param("address", "上海市浦东新区")
                        .param("addressee", "李四")
                        .param("phone", "13900000000")
                        .param("defaultType", "1"))
                .andExpect(status().isOk());

        verify(userAddressService).saveAddress(argThat(a ->
                "addr1".equals(a.getAddressId()) && "U1".equals(a.getUserId())));
    }

    @Test
    void updateDefault_ok() throws Exception {
        mockMvc.perform(post("/userAddress/updateDefault").param("addressId", "addr1"))
                .andExpect(status().isOk());

        verify(userAddressService).updateDefault("U1", "addr1");
    }

    @Test
    void delAddress_ok() throws Exception {
        mockMvc.perform(post("/userAddress/delAddress").param("addressId", "addr1"))
                .andExpect(status().isOk());

        verify(userAddressService).deleteUserAddress("U1", "addr1");
    }
}
