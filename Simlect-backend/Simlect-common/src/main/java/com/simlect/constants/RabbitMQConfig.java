package com.simlect.constants;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.core.AcknowledgeMode;

@Configuration
public class RabbitMQConfig {

    @Value("${user.temp-ban.ms:7200000}")
    private int userTempBanMs;

    @Value("${rush.expire.ms:900000}")
    private int rushExpireMs;

    @Value("${order.expire.ms:900000}")
    private int orderExpireMs;

    @Value("${logistics.simulate.interval-second:3600}")
    private int logisticsSimulateIntervalSecond;

    @Value("${order.confirm.minute:10080}")
    private int orderConfirmMinute;

    // 交换机
    public static final String RUSHING_EXCHANGE = "rushing.exchange";
    // 支付、订单相关的交换机
    public static final String PAY_EXCHANGE = "pay.exchange";
    // Rag相关的交换机
    public static final String RAG_EXCHANGE = "rag.exchange";
    // 浏览足迹交换机
    public static final String BROWSE_EXCHANGE = "browse.exchange";
    // 通知交换机
    public static final String NOTIFY_EXCHANGE = "notify.exchange";

    // 队列
    public static final String RUSHING_ORDER_QUEUE = "rushing.order.queue";
    // Rag队列
    public static final String RAG_QUEUE = "rag.queue";
    // Rag死信队列
    public static final String RAG_DEAD_QUEUE = "rag.dead.queue";
    public static final String BROWSE_RECORD_QUEUE = "browse.record.queue";

    // 死信队列（超时未支付释放库存）
    public static final String RUSHING_DELAY_QUEUE = "rushing.delay.queue";
    public static final String RUSHING_DEAD_QUEUE = "rushing.dead.queue";
    // 支付超时延时队列
    public static final String PAY_TIMEOUT_DELAY_QUEUE = "pay.timeout.delay.queue";
    // 支付超时队列（死信）
    public static final String PAY_TIMEOUT_DEAD_QUEUE = "pay.timeout.dead.queue";
    // 模拟发货延时队列
    public static final String PAY_LOGISTICS_DELAY_QUEUE = "pay.logistics.delay.queue";
    // 模拟发货队列（死信）
    public static final String PAY_LOGISTICS_DEAD_QUEUE = "pay.logistics.dead.queue";
    // 自动确认收货延时队列
    public static final String PAY_CONFIRM_DELAY_QUEUE = "pay.confirm.delay.queue";
    // 自动确认收货队列（死信）
    public static final String PAY_CONFIRM_DEAD_QUEUE = "pay.confirm.dead.queue";

    // 路由键
    public static final String RUSHING_ORDER_KEY = "rushing.order";
    public static final String RUSHING_DELAY_KEY = "rushing.delay";
    public static final String RUSHING_DEAD_KEY = "rushing.dead";
    public static final String PAY_TIMEOUT_DELAY_KEY = "pay.timeout.delay";
    public static final String PAY_TIMEOUT_DEAD_KEY = "pay.timeout.dead";
    public static final String PAY_LOGISTICS_DELAY_KEY = "pay.logistics.delay";
    public static final String PAY_LOGISTICS_DEAD_KEY = "pay.logistics.dead";
    public static final String PAY_CONFIRM_DELAY_KEY = "pay.confirm.delay";
    public static final String PAY_CONFIRM_DEAD_KEY = "pay.confirm.dead";
    public static final String RAG_QUEUE_KEY = "rag.queue";
    public static final String RAG_DEAD_QUEUE_KEY = "rag.dead.queue";
    public static final String BROWSE_RECORD_KEY = "browse.record";

    // 通知队列
    public static final String NOTIFY_QUEUE = "notify.queue";
    // 通知路由键
    public static final String NOTIFY_KEY = "notify";
    // 通知死信队列（TTL 到期/消费拒绝转存，防大流量广播期间消息被静默丢弃）
    public static final String NOTIFY_DEAD_QUEUE = "notify.dead.queue";
    // 通知死信路由键（仅死信投递使用，正常路由永不匹配）
    public static final String NOTIFY_DEAD_KEY = "notify.dead";

    // MySQL 权威签到事务提交后投递的连续签到奖励事件
    public static final String SIGN_STREAK_COUPON_EXCHANGE = "sign.streak.coupon.exchange";
    public static final String SIGN_STREAK_COUPON_QUEUE = "sign.streak.coupon.queue";
    public static final String SIGN_STREAK_COUPON_KEY = "sign.streak.coupon";

    // 用户临时封禁（延时解封）
    public static final String USER_TEMP_BAN_EXCHANGE = "user.tempban.exchange";
    public static final String USER_TEMP_BAN_DELAY_QUEUE = "user.tempban.delay.queue";
    public static final String USER_TEMP_BAN_DEAD_QUEUE = "user.tempban.dead.queue";
    public static final String USER_TEMP_BAN_DELAY_KEY = "user.tempban.delay";
    public static final String USER_TEMP_BAN_DEAD_KEY = "user.tempban.dead";

    // ========== 正常订单队列 ==========

    @Bean
    public DirectExchange rushingExchange() {
        // durable=true：Broker 重启后交换机仍在
        return new DirectExchange(RUSHING_EXCHANGE, true, false);
    }

    @Bean
    public Queue rushingOrderQueue() {
        // durable 队列 + 配合消息 PERSISTENT，Broker 重启不丢消息
        return QueueBuilder.durable(RUSHING_ORDER_QUEUE).build();
    }

    @Bean
    public Binding rushingOrderBinding() {
        return BindingBuilder.bind(rushingOrderQueue())
                .to(rushingExchange())
                .with(RUSHING_ORDER_KEY);
    }

    // ========== 延迟队列（1分钟超时）==========

    @Bean
    public Queue rushingDelayQueue() {
        return QueueBuilder.durable(RUSHING_DELAY_QUEUE)
                .withArgument("x-message-ttl", rushExpireMs)  // 秒杀支付超时
                .withArgument("x-dead-letter-exchange", RUSHING_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RUSHING_DEAD_KEY)
                .build();
    }

    @Bean
    public Binding rushingDelayBinding() {
        return BindingBuilder.bind(rushingDelayQueue())
                .to(rushingExchange())
                .with(RUSHING_DELAY_KEY);
    }

    // ========== 死信队列（超时处理）==========

    @Bean
    public Queue rushingDeadQueue() {
        return QueueBuilder.durable(RUSHING_DEAD_QUEUE).build();
    }

    @Bean
    public Binding rushingDeadBinding() {
        return BindingBuilder.bind(rushingDeadQueue())
                .to(rushingExchange())
                .with(RUSHING_DEAD_KEY);
    }

    // ========== 支付、订单相关 ==========
    // ========== 支付交换机 ==========
    @Bean
    public DirectExchange payExchange() {
        return new DirectExchange(PAY_EXCHANGE, true, false);
    }
    // ========== 支付超时队列 ==========
    @Bean
    public Queue payTimeoutDelayQueue() {
        return QueueBuilder.durable(PAY_TIMEOUT_DELAY_QUEUE)
                .withArgument("x-message-ttl", orderExpireMs)  // 普通订单支付超时
                .withArgument("x-dead-letter-exchange", PAY_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAY_TIMEOUT_DEAD_KEY)
                .build();
    }
    @Bean
    public Binding payTimeoutDelayBinding() {
        return BindingBuilder.bind(payTimeoutDelayQueue())
                .to(payExchange())
                .with(PAY_TIMEOUT_DELAY_KEY);
    }

    @Bean
    public Queue payTimeoutDeadQueue() {
        return QueueBuilder.durable(PAY_TIMEOUT_DEAD_QUEUE).build();
    }

    @Bean
    public Binding payTimeoutDeadBinding() {
        return BindingBuilder.bind(payTimeoutDeadQueue())
                .to(payExchange())
                .with(PAY_TIMEOUT_DEAD_KEY);
    }
    // ========== 模拟发货队列 ==========
    @Bean
    public Queue payLogisticsDelayQueue() {
        return QueueBuilder.durable(PAY_LOGISTICS_DELAY_QUEUE)
                .withArgument("x-message-ttl", logisticsSimulateIntervalSecond * 1000L)
                .withArgument("x-dead-letter-exchange", PAY_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAY_LOGISTICS_DEAD_KEY)
                .build();
    }
    @Bean
    public Binding payLogisticsDelayBinding() {
        return BindingBuilder.bind(payLogisticsDelayQueue())
                .to(payExchange())
                .with(PAY_LOGISTICS_DELAY_KEY);
    }

    @Bean
    public Queue payLogisticsDeadQueue() {
        return QueueBuilder.durable(PAY_LOGISTICS_DEAD_QUEUE).build();
    }

    @Bean
    public Binding payLogisticsDeadBinding() {
        return BindingBuilder.bind(payLogisticsDeadQueue())
                .to(payExchange())
                .with(PAY_LOGISTICS_DEAD_KEY);
    }
    // ========== 自动确认收货队列 ==========
    @Bean
    public Queue payConfirmDelayQueue() {
        return QueueBuilder.durable(PAY_CONFIRM_DELAY_QUEUE)
                .withArgument("x-message-ttl", orderConfirmMinute * 60 * 1000L)
                .withArgument("x-dead-letter-exchange", PAY_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAY_CONFIRM_DEAD_KEY)
                .build();
    }
    @Bean
    public Binding payConfirmDelayBinding() {
        return BindingBuilder.bind(payConfirmDelayQueue())
                .to(payExchange())
                .with(PAY_CONFIRM_DELAY_KEY);
    }

    @Bean
    public Queue payConfirmDeadQueue() {
        return QueueBuilder.durable(PAY_CONFIRM_DEAD_QUEUE).build();
    }

    @Bean
    public Binding payConfirmDeadBinding() {
        return BindingBuilder.bind(payConfirmDeadQueue())
                .to(payExchange())
                .with(PAY_CONFIRM_DEAD_KEY);
    }

    // ========== 浏览足迹异步落库 ==========
    @Bean
    public DirectExchange browseExchange() {
        return new DirectExchange(BROWSE_EXCHANGE, true, false);
    }

    @Bean
    public Queue browseRecordQueue() {
        return QueueBuilder.durable(BROWSE_RECORD_QUEUE).build();
    }

    @Bean
    public Binding browseRecordBinding() {
        return BindingBuilder.bind(browseRecordQueue())
                .to(browseExchange())
                .with(BROWSE_RECORD_KEY);
    }

    // Rag交换机
    @Bean
    public DirectExchange ragExchange() {
        return new DirectExchange(RAG_EXCHANGE, true, false);
    }

    // Rag队列（消费失败经 DLX 进入 rag.dead.queue）
    @Bean
    public Queue ragQuestionQueue() {
        return QueueBuilder.durable(RAG_QUEUE)
                .withArgument("x-dead-letter-exchange", RAG_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RAG_DEAD_QUEUE_KEY)
                .build();
    }

    @Bean
    public Binding ragQuestionBinding() {
        return BindingBuilder.bind(ragQuestionQueue())
                .to(ragExchange())
                .with(RAG_QUEUE_KEY);
    }

    // Rag死信队列
    @Bean
    public Queue ragDeadQueue() {
        return QueueBuilder.durable(RAG_DEAD_QUEUE).build();
    }

    @Bean
    public Binding ragDeadBinding() {
        return BindingBuilder.bind(ragDeadQueue())
                .to(ragExchange())
                .with(RAG_DEAD_QUEUE_KEY);
    }

    // ========== 通知队列 ==========
    @Bean
    public DirectExchange notifyExchange() {
        return new DirectExchange(NOTIFY_EXCHANGE, true, false);
    }

@Bean
public Queue notifyQueue() {
    return QueueBuilder.durable(NOTIFY_QUEUE)
            .withArgument("x-message-ttl", 300000)  // 5分钟过期上限
            .withArgument("x-dead-letter-exchange", NOTIFY_EXCHANGE)  // TTL 到期/拒绝转死信，防消息静默丢弃
            .withArgument("x-dead-letter-routing-key", NOTIFY_DEAD_KEY)
            .build();
}

@Bean
public Queue notifyDeadQueue() {  // 通知死信队列：转存积压超期/消费失败的消息
    // 自身 7 天 TTL（无 DLX，防与正常路由循环）：消费者在线时秒级处理，仅作消费者宕机时的防无限堆积兜底
    return QueueBuilder.durable(NOTIFY_DEAD_QUEUE)
            .withArgument("x-message-ttl", 604800000)
            .build();
}

@Bean
public Binding notifyDeadBinding() {
    return BindingBuilder.bind(notifyDeadQueue())
            .to(notifyExchange())
            .with(NOTIFY_DEAD_KEY);
}

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue())
                .to(notifyExchange())
                .with(NOTIFY_KEY);
    }

    @Bean
    public DirectExchange signStreakCouponExchange() {
        return new DirectExchange(SIGN_STREAK_COUPON_EXCHANGE, true, false);
    }

    @Bean
    public Queue signStreakCouponQueue() {
        return QueueBuilder.durable(SIGN_STREAK_COUPON_QUEUE).build();
    }

    @Bean
    public Binding signStreakCouponBinding() {
        return BindingBuilder.bind(signStreakCouponQueue())
                .to(signStreakCouponExchange())
                .with(SIGN_STREAK_COUPON_KEY);
    }

    // ========== 用户临时封禁（延时解封）==========
    @Bean
    public DirectExchange userTempBanExchange() {
        return new DirectExchange(USER_TEMP_BAN_EXCHANGE, true, false);
    }

    @Bean
    public Queue userTempBanDelayQueue() {
        return QueueBuilder.durable(USER_TEMP_BAN_DELAY_QUEUE)
                .withArgument("x-message-ttl", userTempBanMs)
                .withArgument("x-dead-letter-exchange", USER_TEMP_BAN_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_TEMP_BAN_DEAD_KEY)
                .build();
    }

    @Bean
    public Queue userTempBanDeadQueue() {
        return QueueBuilder.durable(USER_TEMP_BAN_DEAD_QUEUE).build();
    }

    @Bean
    public Binding userTempBanDelayBinding() {
        return BindingBuilder.bind(userTempBanDelayQueue())
                .to(userTempBanExchange())
                .with(USER_TEMP_BAN_DELAY_KEY);
    }

    @Bean
    public Binding userTempBanDeadBinding() {
        return BindingBuilder.bind(userTempBanDeadQueue())
                .to(userTempBanExchange())
                .with(USER_TEMP_BAN_DEAD_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
