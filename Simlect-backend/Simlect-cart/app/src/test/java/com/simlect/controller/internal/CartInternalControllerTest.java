package com.simlect.controller.internal;

import com.simlect.biz.CartInternalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CartInternalController 批量删除接口单元测试（MockMvc standalone）。
 */
@ExtendWith(MockitoExtension.class)
class CartInternalControllerTest {

    @Mock
    private CartInternalService cartInternalService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CartInternalController controller = new CartInternalController();
        ReflectionTestUtils.setField(controller, "cartInternalService", cartInternalService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void deleteBatch_delegates() throws Exception {
        mockMvc.perform(post("/internal/cart/deleteBatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"userId\":\"U1\",\"productId\":\"P1\",\"propertyValueIdHash\":\"h1\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartInternalService).deleteBatch(any());
    }
}
