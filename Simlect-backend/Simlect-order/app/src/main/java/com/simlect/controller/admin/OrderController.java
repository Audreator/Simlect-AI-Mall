package com.simlect.controller.admin;

import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.api.dto.OrderStatusDTO;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.enums.PageSize;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.po.OrderComment;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.po.OrderInfo;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.po.OrderLogisticsInfo;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.query.OrderCommentQuery;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.biz.OrderCommentService;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.biz.OrderInfoService;
import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.biz.OrderLogisticsInfoService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping("/admin/order")
@RestController("adminOrderController")
public class OrderController extends com.simlect.controller.admin.ABaseController{

    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private OrderCommentService orderCommentService;
    @Resource
    private OrderLogisticsInfoService orderLogisticsInfoService;

    @PostMapping("/loadOrder")
    public ResponseVO loadOrder(Integer pageNo, Integer pageSize, String productNameFuzzy, Integer orderStatus){
        OrderInfoQuery orderInfoQuery = new OrderInfoQuery();
        // 查询所有订单
        orderInfoQuery.setQueryItems(true);
        orderInfoQuery.setQueryUser(true);
        orderInfoQuery.setOrderStatus(orderStatus);
        orderInfoQuery.setOrderBy("order_time desc");
        orderInfoQuery.setPageNo(pageNo);
        orderInfoQuery.setPageSize(pageSize);
        PaginationResultVO<OrderInfo> resultVO = orderInfoService.findListByPage(orderInfoQuery);
        if (productNameFuzzy != null){
            // 对resultVO中的items进行过滤
            resultVO = orderInfoService.findByProductNameFuzzy(resultVO, productNameFuzzy);
        }
        return getSuccessResponseVO(resultVO);
    }

    @PostMapping("/loadOrderStatus")
    public ResponseVO loadOrderStatus(){
        // 返回OrderStatusDTO
        List<OrderStatusDTO> orderStatusDTOList = new ArrayList<>();
        return getSuccessResponseVO(Arrays.stream(OrderStatusEnum.values())
                .map(OrderStatusDTO::getByStatus)
                .collect(Collectors.toList()));
    }

    // 加载所有评论
    @PostMapping("/loadComment")
    public ResponseVO loadComment(Integer pageNo, Integer pageSize, String nickNameFuzzy, String productNameFuzzy){
        OrderCommentQuery  query = new OrderCommentQuery();
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        query.setQueryProduct(true);
        query.setQueryUserInfo(true);
        query.setOrderBy("COALESCE(recomment_time, comment_time) desc");
        PaginationResultVO<OrderComment> resultVO = orderCommentService.findListByPage(query);
        if (nickNameFuzzy != null){
            resultVO = orderCommentService.findByNickNameFuzzy(resultVO, nickNameFuzzy);
        }
        if (productNameFuzzy != null){
            resultVO = orderCommentService.findByProductNameFuzzy(resultVO, productNameFuzzy);
        }
        return getSuccessResponseVO(resultVO);
    }
    // 获取相应orderId的评论
    @PostMapping("/getComment")
    public ResponseVO getComment(@NotEmpty String orderId){
        return getSuccessResponseVO(orderCommentService.getComment(null,orderId));
    }

    // 商家评论
    @AdminSensitiveConfirm
    @PostMapping("/bizComment")
    public ResponseVO bizComment(@NotEmpty String orderId, @NotEmpty String commentBizReply, String reCommentImages){
        orderCommentService.bizComment(orderId,commentBizReply,reCommentImages);
        return getSuccessResponseVO(null);
    }

    // 删除评论，逻辑删除
@AdminSensitiveConfirm
        @PostMapping("/delComment")
    public ResponseVO delComment(@NotEmpty String orderId){
        orderCommentService.delMyComment(null, orderId);
        return getSuccessResponseVO(null);
    }

    // 获取收货地址
    @PostMapping("/getLogistics")
    public ResponseVO getLogistics(@NotEmpty String orderId){
        return getSuccessResponseVO(orderLogisticsInfoService.getOrderLogisticsRecords(null, orderId));
    }

    // 发货
    @AdminSensitiveConfirm
    @PostMapping("/delivery")
    public ResponseVO delivery(OrderLogisticsInfo orderLogisticsInfo){
        orderLogisticsInfoService.delivery(orderLogisticsInfo);
        return getSuccessResponseVO(null);
    }
}
