// 声明当前类所在的包名，用于组织订单模块内部 Agent 接口控制器
package com.simlect.controller.internal;

// 导入订单评论业务服务接口，用于查询订单评价信息
import com.simlect.biz.OrderCommentService;
// 导入订单信息业务服务接口，用于查询订单主表数据
import com.simlect.biz.OrderInfoService;
// 导入订单明细业务服务接口，用于查询订单商品行
import com.simlect.biz.OrderItemService;
// 导入订单物流信息业务服务接口，用于查询物流轨迹
import com.simlect.biz.OrderLogisticsInfoService;
// 导入控制器基类，提供统一的成功响应封装方法
import com.simlect.controller.ABaseController;
// 导入订单状态枚举，用于筛选各生命周期状态的订单
import com.simlect.api.enums.OrderStatusEnum;
// 导入订单评论持久化实体类
import com.simlect.entity.po.OrderComment;
// 导入订单信息持久化实体类
import com.simlect.entity.po.OrderInfo;
// 导入订单明细持久化实体类
import com.simlect.entity.po.OrderItem;
// 导入订单物流信息持久化实体类
import com.simlect.entity.po.OrderLogisticsInfo;
// 导入订单评论查询条件封装类
import com.simlect.entity.query.OrderCommentQuery;
// 导入订单信息查询条件封装类
import com.simlect.entity.query.OrderInfoQuery;
// 导入订单明细查询条件封装类
import com.simlect.entity.query.OrderItemQuery;
// 导入简单分页参数类，用于限制查询返回条数
import com.simlect.entity.query.SimplePage;
// 导入统一 API 响应包装类
import com.simlect.entity.vo.ResponseVO;
// 导入字符串工具类，用于判空等常见操作
import com.simlect.utils.StringTools;
// 导入 Jakarta 依赖注入注解，按名称/类型注入 Spring 管理的 Bean
import jakarta.annotation.Resource;
// 导入 Spring MVC 注解：将方法映射为 HTTP POST 请求处理
import org.springframework.web.bind.annotation.PostMapping;
// 导入 Spring MVC 注解：将请求体 JSON 反序列化为方法参数
import org.springframework.web.bind.annotation.RequestBody;
// 导入 Spring MVC 注解：为控制器类或方法指定 URL 路径前缀
import org.springframework.web.bind.annotation.RequestMapping;
// 导入 Spring MVC 注解：标记该类为 REST 控制器，返回值直接序列化为 JSON
import org.springframework.web.bind.annotation.RestController;

// 导入 Java 8 日期时间 API 中的本地日期时间类型
import java.time.LocalDateTime;
// 导入时区标识类，用于 Date 与 LocalDateTime 互转
import java.time.ZoneId;
// 导入日期时间格式化器，用于统一时间字符串格式
import java.time.format.DateTimeFormatter;
// 导入可变长度列表实现类
import java.util.ArrayList;
// 导入不可变空列表工具方法
import java.util.Collections;
// 导入旧版 Date 类型，与数据库/实体字段保持一致
import java.util.Date;
// 导入有序 Map 实现，保证 JSON 字段输出顺序稳定
import java.util.LinkedHashMap;
// 导入 List 接口
import java.util.List;
// 导入 Map 接口，用于构建灵活的 Agent 响应结构
import java.util.Map;

/**
 * 订单 Agent 内部接口控制器。
 * <p>
 * 供 Simlect-agent（Python 智能体服务）通过 HTTP 内部调用，
 * 提供订单列表、详情、明细、物流、评论等只读查询能力。
 * </p>
 *
 * <h3>完整调用链（查订单卡片）</h3>
 * <pre>
 * 用户「我的订单」→ graph/nodes.build_context → resolve_intent(QUERY_ORDER)
 *   → agent_loop → LLM 调用 QUERY_ORDERS 工具
 *   → mcp_tool_router → MCP query_orders
 *   → java_internal_client → 本类 POST /internal/order/agent/listOrders
 *   → OrderInfoMapper → MySQL → toOrderMap → Agent 订单卡片 JSON
 *   → finalize_node → agent_runtime.finalize_agent_response → WebSocket 推送
 * </pre>
 * <p>写操作（退款/确认收货）不走本类，见 confirmAction → action_execute_service → Gateway /api/order/*</p>
 * <p>详见 Simlect-order/MODULE.md</p>
 */
// @RestController：Spring 组件，等价于 @Controller + @ResponseBody，响应体自动转 JSON
@RestController
// @RequestMapping：类级别 URL 前缀，所有接口路径均以 /internal/order/agent 开头
@RequestMapping("/internal/order/agent")
// 继承 ABaseController，复用 getSuccessResponseVO 等通用响应方法
public class OrderAgentInternalController extends ABaseController {

    // 日期时间格式化模板，统一输出 yyyy-MM-dd HH:mm:ss 格式
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // @Resource：按名称注入 OrderInfoService Bean，处理订单主表业务逻辑
    @Resource
    // 订单信息服务，用于列表查询与按 ID 查详情
    private OrderInfoService orderInfoService;
    // @Resource：注入订单明细服务 Bean
    @Resource
    // 订单明细服务，用于查询订单下的商品行
    private OrderItemService orderItemService;
    // @Resource：注入订单物流服务 Bean
    @Resource
    // 订单物流服务，用于查询物流单号与轨迹
    private OrderLogisticsInfoService orderLogisticsInfoService;
    // @Resource：注入订单评论服务 Bean
    @Resource
    // 订单评论服务，用于查询用户对订单/商品的评价
    private OrderCommentService orderCommentService;

    /**
     * 按用户 ID 查询订单列表（Agent 专用）。
     *
     * @param body 请求体，支持 userId、orderId、limit、timeStart、timeEnd 等字段
     * @return 订单 Map 列表，每项含订单基本信息及可选明细
     */
    // @PostMapping：映射 POST /internal/order/agent/listOrders
    @PostMapping("/listOrders")
    // @RequestBody：将 JSON 请求体反序列化为 Map<String, Object>
    public ResponseVO<List<Map<String, Object>>> listOrders(@RequestBody Map<String, Object> body) {
        // 从请求体提取 userId 字符串
        String userId = str(body, "userId");
        // userId 为空时直接返回空列表，避免全表扫描
        if (StringTools.isEmpty(userId)) {
            // 封装成功响应，data 为空列表
            return getSuccessResponseVO(Collections.emptyList());
        }
        // 构造订单查询条件对象
        OrderInfoQuery query = new OrderInfoQuery();
        // 限定查询指定用户的订单
        query.setUserId(userId);
        // 开启关联查询订单明细（由 Service/Mapper 层处理）
        query.setQueryItems(true);
        // 按下单时间倒序，最新订单在前
        query.setOrderBy("o.order_time desc");
        // 可选：按订单号精确过滤
        String orderId = str(body, "orderId");
        // 若传入 orderId 则追加精确匹配条件
        if (!StringTools.isEmpty(orderId)) {
            query.setOrderId(orderId);
        }
        // 设置允许查询的订单状态集合（覆盖常见业务状态）
        query.setOrderStatusList(new Integer[]{
                // 待支付
                OrderStatusEnum.WAIT_PAYMENT.getStatus(),
                // 已支付
                OrderStatusEnum.PAID.getStatus(),
                // 已发货
                OrderStatusEnum.SHIPPED.getStatus(),
                // 已完成
                OrderStatusEnum.COMPLETED.getStatus(),
                // 已取消
                OrderStatusEnum.CANCELLED.getStatus(),
                // 已关闭
                OrderStatusEnum.CLOSED.getStatus(),
                // 已退款
                OrderStatusEnum.REFUNDED.getStatus(),
                // 部分退款
                OrderStatusEnum.PARTIALLY_REFUNDED.getStatus()
        });
        // 解析 limit 参数，默认最多返回 30 条
        int limit = intVal(body.get("limit"), 30);
        // 设置分页：第 0 页，每页 limit 条
        query.setSimplePage(new SimplePage(0, limit));
        // 时间范围：Mapper 可能不支持 timeStart，查询后在内存中二次过滤
        // 执行数据库查询，获取订单实体列表
        List<OrderInfo> list = orderInfoService.findListByParam(query);
        // 读取可选的起始时间字符串
        String timeStart = str(body, "timeStart");
        // 读取可选的结束时间字符串
        String timeEnd = str(body, "timeEnd");
        // 准备 Agent 友好的 Map 结果列表
        List<Map<String, Object>> result = new ArrayList<>();
        // 遍历每条订单记录
        for (OrderInfo o : list) {
            // 若不在指定时间范围内则跳过
            if (!inTimeRange(o.getOrderTime(), timeStart, timeEnd)) {
                continue;
            }
            // 转换为 Map 并附带明细，加入结果集
            result.add(toOrderMap(o, true));
        }
        // 返回成功响应及订单列表
        return getSuccessResponseVO(result);
    }

    /**
     * 按订单号查询单笔订单详情（含明细）。
     *
     * @param body 请求体，需包含 orderId
     * @return 订单 Map；不存在时 data 为 null
     */
    // @PostMapping：映射 POST /internal/order/agent/getOrder
    @PostMapping("/getOrder")
    public ResponseVO<Map<String, Object>> getOrder(@RequestBody Map<String, Object> body) {
        // 提取订单号
        String orderId = str(body, "orderId");
        // 订单号为空则返回 null 数据
        if (StringTools.isEmpty(orderId)) {
            return getSuccessResponseVO(null);
        }
        // 按订单号查询订单主信息
        OrderInfo order = orderInfoService.getOrderInfoByOrderId(orderId);
        // 订单不存在
        if (order == null) {
            return getSuccessResponseVO(null);
        }
        // 构造明细查询条件
        OrderItemQuery iq = new OrderItemQuery();
        // 关联当前订单号
        iq.setOrderId(orderId);
        // 查询并填充订单明细列表
        order.setOrderItemList(orderItemService.findListByParam(iq));
        // 转换为 Map 并返回（含明细）
        return getSuccessResponseVO(toOrderMap(order, true));
    }

    /**
     * 按订单明细 ID 查询单条明细。
     *
     * @param body 请求体，需包含 orderItemId
     * @return 明细 Map；不存在时 data 为 null
     */
    // @PostMapping：映射 POST /internal/order/agent/getOrderItem
    @PostMapping("/getOrderItem")
    public ResponseVO<Map<String, Object>> getOrderItem(@RequestBody Map<String, Object> body) {
        // 提取订单明细主键
        String orderItemId = str(body, "orderItemId");
        // 明细 ID 为空则返回 null
        if (StringTools.isEmpty(orderItemId)) {
            return getSuccessResponseVO(null);
        }
        // 按 ID 查询明细实体
        OrderItem item = orderItemService.getOrderItemByOrderItemId(orderItemId);
        // 存在则转 Map，否则返回 null
        return getSuccessResponseVO(item == null ? null : toItemMap(item));
    }

    /**
     * 按订单号查询该订单下全部明细行。
     *
     * @param body 请求体，需包含 orderId
     * @return 明细 Map 列表
     */
    // @PostMapping：映射 POST /internal/order/agent/listOrderItems
    @PostMapping("/listOrderItems")
    public ResponseVO<List<Map<String, Object>>> listOrderItems(@RequestBody Map<String, Object> body) {
        // 提取订单号
        String orderId = str(body, "orderId");
        // 订单号为空返回空列表
        if (StringTools.isEmpty(orderId)) {
            return getSuccessResponseVO(Collections.emptyList());
        }
        // 构造明细查询条件
        OrderItemQuery iq = new OrderItemQuery();
        // 限定订单号
        iq.setOrderId(orderId);
        // 按明细 ID 升序排列
        iq.setOrderBy("order_item_id asc");
        // 执行查询
        List<OrderItem> items = orderItemService.findListByParam(iq);
        // 准备结果列表
        List<Map<String, Object>> result = new ArrayList<>();
        // 非空则逐条转换
        if (items != null) {
            for (OrderItem item : items) {
                result.add(toItemMap(item));
            }
        }
        // 返回明细列表
        return getSuccessResponseVO(result);
    }

    /**
     * 查询指定用户订单的物流信息及轨迹。
     *
     * @param body 请求体，需包含 userId 与 orderId
     * @return 物流信息 Map；查询失败或无数据时 data 为 null
     */
    // @PostMapping：映射 POST /internal/order/agent/getLogistics
    @PostMapping("/getLogistics")
    public ResponseVO<Map<String, Object>> getLogistics(@RequestBody Map<String, Object> body) {
        // 提取用户 ID
        String userId = str(body, "userId");
        // 提取订单号
        String orderId = str(body, "orderId");
        // 任一必填参数缺失则返回 null
        if (StringTools.isEmpty(userId) || StringTools.isEmpty(orderId)) {
            return getSuccessResponseVO(null);
        }
        // 物流信息实体，可能由远程或本地服务填充
        OrderLogisticsInfo info;
        try {
            // 调用物流服务，校验用户与订单归属并拉取轨迹
            info = orderLogisticsInfoService.getOrderLogisticsRecords(userId, orderId);
        } catch (Exception e) {
            // 异常时静默返回 null，避免 Agent 侧解析失败
            return getSuccessResponseVO(null);
        }
        // 无物流记录
        if (info == null) {
            return getSuccessResponseVO(null);
        }
        // 使用 LinkedHashMap 保持字段顺序
        Map<String, Object> map = new LinkedHashMap<>();
        // 订单号
        map.put("orderId", info.getOrderId());
        // 用户 ID
        map.put("userId", info.getUserId());
        // 物流单号
        map.put("logisticsNo", info.getLogisticsNo());
        // 物流公司名称
        map.put("logisticsCompany", info.getLogisticsCompany());
        // 物流状态码/描述
        map.put("logisticsStatus", info.getLogisticsStatus());
        // 收件人姓名
        map.put("receiverName", info.getReceiverName());
        // 收件人电话
        map.put("receiverPhone", info.getReceiverPhone());
        // 收件地址
        map.put("receiverAddress", info.getReceiverAddress());
        // 物流轨迹节点列表
        map.put("recordList", info.getRecordList());
        // 返回物流详情
        return getSuccessResponseVO(map);
    }

    /**
     * 查询指定用户对某订单的评价（取首条）。
     *
     * @param body 请求体，需包含 userId 与 orderId
     * @return 评论 Map；无评论时 data 为 null
     */
    // @PostMapping：映射 POST /internal/order/agent/getComment
    @PostMapping("/getComment")
    public ResponseVO<Map<String, Object>> getComment(@RequestBody Map<String, Object> body) {
        // 提取用户 ID
        String userId = str(body, "userId");
        // 提取订单号
        String orderId = str(body, "orderId");
        // 参数校验
        if (StringTools.isEmpty(userId) || StringTools.isEmpty(orderId)) {
            return getSuccessResponseVO(null);
        }
        // 构造评论查询条件
        OrderCommentQuery q = new OrderCommentQuery();
        // 限定用户
        q.setUserId(userId);
        // 限定订单
        q.setOrderId(orderId);
        // 查询评论列表
        List<OrderComment> list = orderCommentService.findListByParam(q);
        // 无评论记录
        if (list == null || list.isEmpty()) {
            return getSuccessResponseVO(null);
        }
        // 取第一条评论（通常一订单一评）
        OrderComment c = list.get(0);
        // 组装 Agent 响应 Map
        Map<String, Object> map = new LinkedHashMap<>();
        // 订单号
        map.put("orderId", c.getOrderId());
        // 商品 ID
        map.put("productId", c.getProductId());
        // 用户 ID
        map.put("userId", c.getUserId());
        // 评论正文
        map.put("commentContent", c.getCommentContent());
        // 星级评分
        map.put("star", c.getStar());
        // 评论时间（格式化字符串）
        map.put("commentTime", formatDate(c.getCommentTime()));
        // 评论图片 URL 列表或 JSON
        map.put("commentImages", c.getCommentImages());
        // 商家回复
        map.put("commentBizReply", c.getCommentBizReply());
        // 追评内容
        map.put("recommentContent", c.getRecommentContent());
        // 返回评论详情
        return getSuccessResponseVO(map);
    }

    /**
     * 将订单实体转换为 Agent 使用的 Map 结构。
     *
     * @param o         订单实体
     * @param withItems 是否附带订单明细列表
     * @return 字段有序的 Map
     */
    // 私有方法：订单实体 → Map，供多个接口复用
    private Map<String, Object> toOrderMap(OrderInfo o, boolean withItems) {
        // 有序 Map，便于 Agent/LLM 阅读
        Map<String, Object> m = new LinkedHashMap<>();
        // 订单号
        m.put("orderId", o.getOrderId());
        // 用户 ID
        m.put("userId", o.getUserId());
        // 订单状态
        m.put("orderStatus", o.getOrderStatus());
        // 订单金额
        m.put("amount", o.getAmount());
        // 支付场景
        m.put("payScene", o.getPayScene());
        // 支付渠道
        m.put("payChannel", o.getPayChannel());
        // 关联支付单号
        m.put("payOrderId", o.getPayOrderId());
        // 下单时间（格式化）
        m.put("orderTime", formatDate(o.getOrderTime()));
        // 订单标题/主题
        m.put("subject", o.getSubject());
        // 评价状态（是否已评）
        m.put("commentStatus", o.getCommentStatus());
        // 按需填充明细
        if (withItems) {
            // 明细 Map 列表
            List<Map<String, Object>> items = new ArrayList<>();
            // 实体上已有关联明细时遍历转换
            if (o.getOrderItemList() != null) {
                for (OrderItem item : o.getOrderItemList()) {
                    items.add(toItemMap(item));
                }
            }
            // 写入 items 字段
            m.put("items", items);
        }
        // 返回完整订单 Map
        return m;
    }

    /**
     * 将订单明细实体转换为 Map。
     *
     * @param item 订单明细实体
     * @return 明细字段 Map
     */
    // 私有方法：订单明细 → Map
    private Map<String, Object> toItemMap(OrderItem item) {
        // 有序 Map
        Map<String, Object> m = new LinkedHashMap<>();
        // 所属订单号
        m.put("orderId", item.getOrderId());
        // 明细主键
        m.put("orderItemId", item.getOrderItemId());
        // 商品 ID
        m.put("productId", item.getProductId());
        // 商品名称（下单快照）
        m.put("productName", item.getProductName());
        // 封面图
        m.put("cover", item.getCover());
        // 规格属性描述
        m.put("propertyInfo", item.getPropertyInfo());
        // 规格值哈希，用于 SKU 匹配
        m.put("propertyValueIdHash", item.getPropertyValueIdHash());
        // 行金额
        m.put("itemAmount", item.getItemAmount());
        // 购买数量
        m.put("buyCount", item.getBuyCount());
        // 明细状态（退款/完成等）
        m.put("orderItemStatus", item.getOrderItemStatus());
        // 返回明细 Map
        return m;
    }

    /**
     * 判断订单时间是否落在 [timeStart, timeEnd] 闭区间内。
     * <p>起止时间为空时表示不限制该边界。</p>
     *
     * @param orderTime 订单下单时间
     * @param timeStart 起始时间字符串，可为 null
     * @param timeEnd   结束时间字符串，可为 null
     * @return true 表示在范围内或未设边界
     */
    // 静态工具：内存中按时间范围过滤
    private static boolean inTimeRange(Date orderTime, String timeStart, String timeEnd) {
        // 无下单时间则不过滤（视为通过）
        if (orderTime == null) {
            return true;
        }
        // 起止均未指定则全部通过
        if (StringTools.isEmpty(timeStart) && StringTools.isEmpty(timeEnd)) {
            return true;
        }
        // Date 转系统默认时区的 LocalDateTime
        LocalDateTime t = LocalDateTime.ofInstant(orderTime.toInstant(), ZoneId.systemDefault());
        // 检查起始边界
        if (!StringTools.isEmpty(timeStart)) {
            // 解析起始时间，缺省补 00:00:00
            LocalDateTime start = parseDt(timeStart, true);
            // 早于起始则排除
            if (start != null && t.isBefore(start)) {
                return false;
            }
        }
        // 检查结束边界
        if (!StringTools.isEmpty(timeEnd)) {
            // 解析结束时间，缺省补 23:59:59
            LocalDateTime end = parseDt(timeEnd, false);
            // 晚于结束则排除
            if (end != null && t.isAfter(end)) {
                return false;
            }
        }
        // 通过时间范围校验
        return true;
    }

    /**
     * 解析日期时间字符串为 LocalDateTime。
     *
     * @param s           原始字符串，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss
     * @param startOfDay  仅日期时是否补当天开始时刻（true）或结束时刻（false）
     * @return 解析结果；失败返回 null
     */
    // 静态工具：解析 Agent 传入的时间参数
    private static LocalDateTime parseDt(String s, boolean startOfDay) {
        try {
            // 规范化 ISO 格式中的 T 分隔符
            String n = normalizeDt(s);
            // 仅日期长度 10 时补全时分秒
            if (n.length() == 10) {
                n = n + (startOfDay ? " 00:00:00" : " 23:59:59");
            }
            // 按固定格式解析
            return LocalDateTime.parse(n, DT);
        } catch (Exception e) {
            // 解析失败返回 null，由调用方决定是否过滤
            return null;
        }
    }

    /**
     * 规范化日期字符串：将 ISO-8601 的 T 替换为空格。
     *
     * @param s 原始字符串
     * @return 规范化后的字符串；null 入参返回 null
     */
    // 静态工具：统一 datetime 字符串格式
    private static String normalizeDt(String s) {
        // null 安全
        if (s == null) {
            return null;
        }
        // 含 T 则替换为空格便于 DT 解析
        return s.contains("T") ? s.replace('T', ' ') : s;
    }

    /**
     * 将 Date 格式化为 yyyy-MM-dd HH:mm:ss 字符串。
     *
     * @param d 日期对象，可为 null
     * @return 格式化字符串；d 为 null 时返回 null
     */
    // 静态工具：实体 Date 字段 → Agent 可读字符串
    private static String formatDate(Date d) {
        // null 直接返回 null
        if (d == null) {
            return null;
        }
        // 转 LocalDateTime 再格式化
        return DT.format(LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()));
    }

    /**
     * 从请求体 Map 中安全取出字符串字段。
     *
     * @param body 请求体
     * @param key  字段名
     * @return 字符串值；缺失时为 null
     */
    // 静态工具：读取 body 中的字符串参数
    private static String str(Map<String, Object> body, String key) {
        // body 或 key 对应值为 null 时返回 null
        if (body == null || body.get(key) == null) {
            return null;
        }
        // Object 转 String
        return String.valueOf(body.get(key));
    }

    /**
     * 将 Object 解析为 int，失败时使用默认值。
     *
     * @param v   待解析值
     * @param def 默认值
     * @return 解析后的整数
     */
    // 静态工具：读取 limit 等数值参数
    private static int intVal(Object v, int def) {
        // null 使用默认值
        if (v == null) {
            return def;
        }
        try {
            // 先转字符串再 parseInt
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            // 非法数字回退默认值
            return def;
        }
    }
}
