package com.simlect.task;

import com.simlect.api.support.PayFeignSupport;
import com.simlect.component.RedisComponent;
import com.simlect.entity.config.AppConfig;
import com.simlect.api.dto.PayOrderNotifyDTO;
import com.simlect.api.enums.OrderStatusEnum;
import com.simlect.api.enums.PayChannelEnum;
import com.simlect.entity.po.OrderInfo;
import com.simlect.entity.query.OrderInfoQuery;
import com.simlect.biz.OrderInfoService;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PayOrderTask {

    /** 单页最多处理的待支付订单（防每 5s 全表扫描拖垮 DB） */
    private static final int POLL_BATCH_LIMIT = 100;

    /** 每轮最多拉取页数：1s 内最多处理 300 单待支付，防尾部队列饥饿 */
    private static final int POLL_MAX_PAGES = 3;

    /** 只轮询最近 N 分钟创建的订单（与异步通知主链路配合，缩短扫描窗口） */
    private static final int POLL_WINDOW_MINUTES = 10;

    /** 分布式锁 key：多实例部署时同一时刻仅一个实例轮询（防重复外呼支付宝） */
    private static final String POLL_LOCK_KEY = "mall:pay-order-task:lock";

    /** 锁 TTL：覆盖 5s 调度周期，保证锁不提前释放 */
    private static final long POLL_LOCK_TTL_SECONDS = 8L;

    @Resource
    private AppConfig appConfig;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PayFeignSupport payFeignSupport;

    @Resource
    private RedisComponent redisComponent;

    @Scheduled(fixedDelay = 5000)
    public void pollPayOrders() {
        if (!appConfig.getAutoCheckpay()) {
            return;
        }
        // 分布式锁：SET NX EX —— 抢不到说明其他实例正在轮询，直接跳过本轮（防多实例重复外呼）
        // value 用本机持有者标识：释放时 Lua 比对后删除（防任务超 TTL 后误删他人锁）
        String lockOwner = UUID.randomUUID().toString();
        if (!redisComponent.setIfAbsent(POLL_LOCK_KEY, lockOwner, POLL_LOCK_TTL_SECONDS, TimeUnit.SECONDS)) {
            return;
        }
        try {
            pollPendingOrders();
        } finally {
            // 仅当锁仍由本机持有才删除（TTL 过期后可能已被其他实例接管）
            redisComponent.deleteIfOwned(POLL_LOCK_KEY, lockOwner);
        }
    }

    private void pollPendingOrders() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        // 时间窗查询（order_time 倒序查询） + 真实分页 + 最多 3 页，防全表扫描与尾部队列饥饿
        OrderInfoQuery query = new OrderInfoQuery();
        query.setOrderStatus(OrderStatusEnum.WAIT_PAYMENT.getStatus());
        query.setOrderTimeStart(now.minusMinutes(POLL_WINDOW_MINUTES).format(fmt));
        query.setOrderTimeEnd(now.format(fmt));
        query.setOrderBy("order_time asc");
        for (int pageNo = 1; pageNo <= POLL_MAX_PAGES; pageNo++) {
            query.setPageNo(pageNo);
            query.setPageSize(POLL_BATCH_LIMIT);
            List<OrderInfo> orderInfoList = orderInfoService.findListByPage(query).getList();
            if (orderInfoList == null || orderInfoList.isEmpty()) {
                break;
            }
            for (OrderInfo orderInfo : orderInfoList) {
                // 单条异常不中断整轮（继续处理其余订单）
                try {
                    handleOrder(orderInfo);
                } catch (Exception e) {
                    log.error("轮询支付状态异常 orderId={}", orderInfo.getOrderId(), e);
                }
            }
        }
    }

    private void handleOrder(OrderInfo orderInfo) {
        String payOrderId = orderInfo.getPayOrderId();
        if (StringTools.isEmpty(payOrderId)) {
            return;
        }
        if (!redisComponent.isPayTradeInitiated(payOrderId)) {
            return;
        }
        PayChannelEnum payChannelEnum = PayChannelEnum.resolve(orderInfo.getPayChannel());
        if (payChannelEnum == null) {
            return;
        }
        PayOrderNotifyDTO payOrderNotifyDTO = payFeignSupport.queryOrder(payOrderId, payChannelEnum.getPayScene());
        if (payOrderNotifyDTO == null) {
            return;
        }
        orderInfoService.paySuccess(payOrderNotifyDTO);
    }
}
