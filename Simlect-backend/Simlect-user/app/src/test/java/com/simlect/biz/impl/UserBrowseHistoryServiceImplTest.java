package com.simlect.biz.impl;

import com.simlect.api.dto.BrowseHistoryMessageDTO;
import com.simlect.api.support.ProductFeignSupport;
import com.simlect.api.vo.ProductInfoSnapshotVO;
import com.simlect.api.vo.UserBrowseProductVO;
import com.simlect.component.RedisComponent;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.entity.po.UserBrowseHistory;
import com.simlect.entity.query.UserBrowseHistoryQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserBrowseHistoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBrowseHistoryServiceImplTest {

    @Mock
    private UserBrowseHistoryMapper<UserBrowseHistory, UserBrowseHistoryQuery> userBrowseHistoryMapper;
    @Mock
    private ProductFeignSupport productFeignSupport;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private ReliableMessageSender reliableMessageSender;

    @InjectMocks
    private UserBrowseHistoryServiceImpl userBrowseHistoryService;

    private ProductInfoSnapshotVO buildProduct(String productId) {
        ProductInfoSnapshotVO product = new ProductInfoSnapshotVO();
        product.setProductId(productId);
        product.setProductName("商品" + productId);
        product.setCover("a.jpg,b.jpg");
        return product;
    }

    @Test
    void enqueueRecordBrowse_emptyArgs_ignored() {
        userBrowseHistoryService.enqueueRecordBrowse("", "P1");
        userBrowseHistoryService.enqueueRecordBrowse("U1", null);

        verifyNoInteractions(redisComponent, reliableMessageSender);
    }

    @Test
    void enqueueRecordBrowse_recordsRecentAndSendsMq() {
        userBrowseHistoryService.enqueueRecordBrowse("U1", "P1");

        verify(redisComponent).recordBrowseRecent("U1", "P1");
        verify(reliableMessageSender).sendMessage(eq(RabbitMQConfig.BROWSE_EXCHANGE),
                eq(RabbitMQConfig.BROWSE_RECORD_KEY), any(BrowseHistoryMessageDTO.class),
                anyString(), any());
    }

    @Test
    void recordBrowse_existingHistory_updatesTime() {
        UserBrowseHistory history = new UserBrowseHistory();
        history.setHistoryId(1L);
        history.setUserId("U1");
        history.setProductId("P1");
        when(userBrowseHistoryMapper.selectList(any())).thenReturn(List.of(history));

        userBrowseHistoryService.recordBrowse("U1", "P1");

        verify(userBrowseHistoryMapper).updateByHistoryId(argThat(u -> u.getBrowseTime() != null), eq(1L));
        verify(userBrowseHistoryMapper, never()).insert(any());
    }

    @Test
    void recordBrowse_newHistory_inserts() {
        when(userBrowseHistoryMapper.selectList(any())).thenReturn(List.of());

        userBrowseHistoryService.recordBrowse("U1", "P1");

        verify(userBrowseHistoryMapper).insert(argThat(h ->
                "U1".equals(h.getUserId()) && "P1".equals(h.getProductId()) && h.getBrowseTime() != null));
    }

    @Test
    void loadBrowsePage_joinsProducts() {
        UserBrowseHistory history = new UserBrowseHistory();
        history.setHistoryId(1L);
        history.setUserId("U1");
        history.setProductId("P1");
        history.setBrowseTime(new Date());

        when(userBrowseHistoryMapper.selectCount(any())).thenReturn(1);
        when(userBrowseHistoryMapper.selectList(any())).thenReturn(List.of(history));
        when(productFeignSupport.toProductInfoMap(any())).thenReturn(Map.of("P1", buildProduct("P1")));

        PaginationResultVO<UserBrowseProductVO> result =
                userBrowseHistoryService.loadBrowsePage("U1", 1);

        assertEquals(1, result.getTotalCount());
        assertEquals("商品P1", result.getList().get(0).getProductName());
        assertEquals("a.jpg", result.getList().get(0).getCover());
    }

    @Test
    void clearBrowse_deletesByParam() {
        userBrowseHistoryService.clearBrowse("U1");

        verify(userBrowseHistoryMapper).deleteByParam(any());
    }

    @Test
    void removeBrowse_notOwned_throws() {
        UserBrowseHistory history = new UserBrowseHistory();
        history.setUserId("U2");
        when(userBrowseHistoryMapper.selectByHistoryId(1L)).thenReturn(history);

        assertThrows(BusinessException.class, () -> userBrowseHistoryService.removeBrowse("U1", 1L));
        verify(userBrowseHistoryMapper, never()).deleteByHistoryId(anyLong());
    }

    @Test
    void removeBrowse_owned_deletes() {
        UserBrowseHistory history = new UserBrowseHistory();
        history.setHistoryId(1L);
        history.setUserId("U1");
        when(userBrowseHistoryMapper.selectByHistoryId(1L)).thenReturn(history);

        userBrowseHistoryService.removeBrowse("U1", 1L);

        verify(userBrowseHistoryMapper).deleteByHistoryId(1L);
    }
}
