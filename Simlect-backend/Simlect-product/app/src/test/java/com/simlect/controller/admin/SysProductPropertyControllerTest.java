package com.simlect.controller.admin;

import com.simlect.biz.SysProductPropertyService;
import com.simlect.controller.AGlobalExceptionHandlerController;
import com.simlect.entity.po.SysProductProperty;
import com.simlect.entity.vo.PaginationResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SysProductPropertyControllerTest {

    @Mock
    private SysProductPropertyService sysProductPropertyService;

    @InjectMocks
    private SysProductPropertyController sysProductPropertyController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sysProductPropertyController)
                .setControllerAdvice(new AGlobalExceptionHandlerController())
                .build();
    }

    @Test
    void loadDataList_ok() throws Exception {
        PaginationResultVO<SysProductProperty> page = new PaginationResultVO<>();
        page.setList(new ArrayList<>());
        when(sysProductPropertyService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/admin/sysProductProperty/loadDataList").param("pageNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void add_ok() throws Exception {
        mockMvc.perform(post("/admin/sysProductProperty/add")
                        .param("propertyId", "prop1")
                        .param("categoryId", "1")
                        .param("propertyName", "口味"))
                .andExpect(status().isOk());

        verify(sysProductPropertyService).add(any(SysProductProperty.class));
    }

    @Test
    void addBatch_ok() throws Exception {
        String body = "[{\"propertyId\":\"prop1\",\"categoryId\":\"1\",\"propertyName\":\"口味\"}]";

        mockMvc.perform(post("/admin/sysProductProperty/addBatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(sysProductPropertyService).addBatch(any());
    }

    @Test
    void updateByPropertyId_ok() throws Exception {
        mockMvc.perform(post("/admin/sysProductProperty/updateSysProductPropertyByPropertyId")
                        .param("propertyId", "prop1")
                        .param("propertyName", "规格"))
                .andExpect(status().isOk());

        verify(sysProductPropertyService).updateSysProductPropertyByPropertyId(
                any(SysProductProperty.class), org.mockito.ArgumentMatchers.eq("prop1"));
    }

    @Test
    void deleteByPropertyId_ok() throws Exception {
        mockMvc.perform(post("/admin/sysProductProperty/deleteSysProductPropertyByPropertyId")
                        .param("propertyId", "prop1"))
                .andExpect(status().isOk());

        verify(sysProductPropertyService).deleteSysProductPropertyByPropertyId("prop1");
    }
}
