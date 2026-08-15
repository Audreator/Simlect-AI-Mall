package com.simlect.controller;

import com.simlect.api.enums.CommentStatusEnum;
import com.simlect.api.vo.ProductCommentStatsVO;
import com.simlect.biz.OrderCommentService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.po.OrderComment;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.utils.AuthCookieHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCommentControllerTest {

    private static final String USER_ID = "U1";
    private static final String ORDER_ID = "O1";

    @Mock
    private OrderCommentService orderCommentService;
    @Mock
    private RedisComponent redisComponent;
    @Mock
    private AuthCookieHelper authCookieHelper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderCommentController controller = new OrderCommentController();
        ReflectionTestUtils.setField(controller, "orderCommentService", orderCommentService);
        ReflectionTestUtils.setField(controller, "redisComponent", redisComponent);
        ReflectionTestUtils.setField(controller, "authCookieHelper", authCookieHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(authCookieHelper.resolveWebToken(any())).thenReturn("token");
        TokenUserInfoDTO tokenUser = new TokenUserInfoDTO();
        tokenUser.setUserId(USER_ID);
        when(redisComponent.getTokenUserInfo("token")).thenReturn(tokenUser);
    }

    @Test
    void postComment_normal_returnsPendingFalse() throws Exception {
        when(orderCommentService.postComment(USER_ID, ORDER_ID, "好评", null, 5)).thenReturn(false);

        mockMvc.perform(post("/order/comment/postComment")
                        .param("orderId", ORDER_ID)
                        .param("commentContent", "好评")
                        .param("star", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.pendingReview").value(false));
    }

    @Test
    void postComment_pendingReview_returnsTrue() throws Exception {
        when(orderCommentService.postComment(USER_ID, ORDER_ID, "好评", "moderation/pending/a.jpg", 5)).thenReturn(true);

        mockMvc.perform(post("/order/comment/postComment")
                        .param("orderId", ORDER_ID)
                        .param("commentContent", "好评")
                        .param("commentImages", "moderation/pending/a.jpg")
                        .param("star", "5"))
                .andExpect(jsonPath("$.data.pendingReview").value(true));
    }

    @Test
    void loadComment_success() throws Exception {
        PaginationResultVO<OrderComment> page = new PaginationResultVO<>();
        page.setList(List.of());
        when(orderCommentService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/order/comment/loadComment").param("pageNo", "1").param("productId", "P1"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void getComment_success() throws Exception {
        OrderComment comment = new OrderComment();
        comment.setOrderId(ORDER_ID);
        when(orderCommentService.getComment(USER_ID, ORDER_ID)).thenReturn(comment);

        mockMvc.perform(post("/order/comment/getComment").param("orderId", ORDER_ID))
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID));
    }

    @Test
    void postReComment_success() throws Exception {
        mockMvc.perform(post("/order/comment/postReComment")
                        .param("orderId", ORDER_ID)
                        .param("reCommentContent", "补充"))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderCommentService).postReComment(USER_ID, ORDER_ID, "补充", null);
    }

    @Test
    void loadMyComment_success() throws Exception {
        PaginationResultVO<OrderComment> page = new PaginationResultVO<>();
        page.setList(List.of());
        when(orderCommentService.findListByPage(any())).thenReturn(page);

        mockMvc.perform(post("/order/comment/loadMyComment").param("pageNo", "1"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void delMyComment_success() throws Exception {
        mockMvc.perform(post("/order/comment/delMyComment").param("orderId", ORDER_ID))
                .andExpect(jsonPath("$.status").value("success"));
        verify(orderCommentService).delMyComment(USER_ID, ORDER_ID);
    }

    @Test
    void getProductCommentStats_success() throws Exception {
        ProductCommentStatsVO vo = new ProductCommentStatsVO();
        vo.setTotalCount(10);
        when(orderCommentService.getProductCommentStats("P1")).thenReturn(vo);

        mockMvc.perform(post("/order/comment/getProductCommentStats").param("productId", "P1"))
                .andExpect(jsonPath("$.data.totalCount").value(10));
    }
}
