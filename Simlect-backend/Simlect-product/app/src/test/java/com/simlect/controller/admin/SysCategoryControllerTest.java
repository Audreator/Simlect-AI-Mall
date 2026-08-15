package com.simlect.controller.admin;

import com.simlect.biz.SysCategoryService;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.po.SysCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SysCategoryControllerTest {

    @Mock
    private SysCategoryService sysCategoryService;

    @InjectMocks
    private SysCategoryController sysCategoryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sysCategoryController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadCategory_ok() throws Exception {
        when(sysCategoryService.findListByParam(any())).thenReturn(List.of());

        mockMvc.perform(post("/admin/sysCategory/loadCategory").param("queryProperty", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(sysCategoryService).findListByParam(argThat(q ->
                Boolean.TRUE.equals(q.getPropertyQuery()) && Boolean.TRUE.equals(q.getParent())));
    }

    @Test
    void saveCategory_ok() throws Exception {
        mockMvc.perform(post("/admin/sysCategory/saveCategory")
                        .param("categoryId", "1")
                        .param("categoryName", "零食")
                        .param("pCategoryId", "0")
                        .param("sort", "1"))
                .andExpect(status().isOk());

        verify(sysCategoryService).saveCategory(any(SysCategory.class));
    }

    @Test
    void delCategory_ok() throws Exception {
        mockMvc.perform(post("/admin/sysCategory/delCategory")
                        .param("categoryId", "1")
                        .param("pCategoryId", "0"))
                .andExpect(status().isOk());

        verify(sysCategoryService).deleteSysCategory(any(SysCategory.class));
    }

    @Test
    void changeCategorySort_ok() throws Exception {
        mockMvc.perform(post("/admin/sysCategory/changeCategorySort").param("categoryIds", "2,1"))
                .andExpect(status().isOk());

        verify(sysCategoryService).changeCategorySort("2,1");
    }

    @Test
    void saveProductProperty_ok() throws Exception {
        mockMvc.perform(post("/admin/sysCategory/saveProductProperty")
                        .param("propertyId", "prop1")
                        .param("categoryId", "1")
                        .param("propertyName", "口味"))
                .andExpect(status().isOk());

        verify(sysCategoryService).saveProductProperty(any());
    }

    @Test
    void addBatch_ok() throws Exception {
        String body = "[{\"categoryId\":\"1\",\"categoryName\":\"零食\",\"pCategoryId\":\"0\"}]";

        mockMvc.perform(post("/admin/sysCategory/addBatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(sysCategoryService).addBatch(any());
    }
}
