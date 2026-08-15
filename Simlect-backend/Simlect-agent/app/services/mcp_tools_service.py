"""
================================================================================
文件：services/mcp_tools_service.py
角色：MCP 工具的「真正业务实现」（类比 Java @Service 领域服务）
================================================================================

【这个文件干什么】
mcp_server/server.py 里每个 @mcp.tool 最终都调到本文件的函数：
- 读：java_internal_client / order_service / product 搜索 → 拼文案与卡片
- 写：pending_action_service.create_pending → 只生成确认卡（act_xxx），不改库

【在整体中的位置】
LLM 选工具 → mcp/tools schema → MCP HTTP → server.py →【本文件】→ Java 或 Redis

【如何使用】
一般不要被 Controller 直接调用；应经 MCP 或 mcp_tool_router，保证护栏与指标一致。
本地调试某个工具：可在 Python REPL await tool_query_orders(...)

【安全要点】
写操作必须走 PROPOSE_*；用户点确认后才 action_execute → JavaBridge。

【关联】java_internal_client、pending_action_service、order_service、ToolInvokeResult
导读：docs/智能客服-Java开发者导读.md
================================================================================
"""

import json  # import：JSON 序列化/反序列化，类似 Java 的 Jackson ObjectMapper / Gson

from datetime import datetime  # import：日期时间类型，类似 java.time.LocalDateTime

import structlog

logger = structlog.get_logger()

from app.constants import (  # import：从常量模块批量导入枚举/配置（类似 Java 的 static import）
    CONFIRM_RECEIPT_ORDER_STATUSES,  # 允许确认收货的订单状态集合（类似 Java Set<Integer>）
    ORDER_ITEM_STATUS_NORMAL,  # 订单项正常状态码（类似 Java 枚举 int 值）
    ORDER_STATUS_NAMES,  # 订单状态码 -> 中文名映射（类似 Map<Integer, String>）
    REFUNDABLE_ORDER_STATUSES,  # 允许退款的订单状态集合
    REVIEWABLE_ORDER_STATUSES,  # 允许评价/追评的订单状态集合
)

from app.services.java_internal_client import java_internal_client  # Java 内部 API 客户端单例（类似 @Autowired RestTemplate 封装）
from app.services.order_service import order_service  # 订单领域服务（类似 Java OrderService）
from app.services.pending_action_service import pending_action_service  # 待确认操作服务（Redis 存 act_xxx token，类似两阶段提交的「预提交」）


def _status_name(status: int | None) -> str:
    """
    将订单状态码转为中文展示名。

    Java 类比：类似 OrderStatusEnum.fromCode(code).getDesc()，
    这里用 Map 查表，未知码则回退为数字字符串。
    """
    if status is None:  # None 相当于 Java 的 null：未提供状态码
        return "未知"  # 返回默认展示文案
    return ORDER_STATUS_NAMES.get(status, str(status))  # Map.get(key, default)：查不到则用 str(status) 兜底


def _propose_reply(label: str, pending: dict) -> str:
    """
    生成「写操作提案」成功后给 LLM 的引导话术。

    Java 类比：类似 Controller 返回「请用户确认」的 DTO，附带 confirmToken；
    pending['token'] 即 act_xxx，用户确认后 agent_runtime 会解析并调 Java 执行。
    """
    return (  # 拼接多行字符串（Python 括号内隐式续行，类似 Java 的 + 拼接）
        f"已生成{label}确认卡片。请用一句话说明关键信息（勿重复工具原文、勿写【成功/失败】），"
        f"并在回复末尾附带【{pending['token']}】"  # 从 pending Map 读取确认令牌（类似 DTO.getConfirmToken()）
    )


def _truncate(text: str, max_len: int = 40) -> str:
    """
    截断过长文本，用于摘要展示。

    Java 类比：类似 StringUtils.abbreviate(text, maxLen)。
    """
    if not text or len(text) <= max_len:  # 空串或已足够短则原样返回
        return text
    return text[:max_len] + "…"  # 切片 [:max_len] 取前缀，类似 Java substring(0, maxLen)


def _fmt_dt(value) -> str | None:
    """
    将 datetime 或任意值格式化为可读时间字符串。

    Java 类比：类似 DateTimeFormatter.format(localDateTime)；
    返回 str | None 相当于 @Nullable String。
    """
    if value is None:  # null 输入直接返回 null
        return None
    if isinstance(value, datetime):  # isinstance ~ instanceof：运行时类型判断
        return value.strftime("%Y-%m-%d %H:%M:%S")  # strftime 类似 SimpleDateFormat / DateTimeFormatter
    return str(value)  # 非 datetime 则强转为字符串（类似 String.valueOf）


def _parse_dt(value) -> datetime | None:
    """
    解析多种格式的时间字符串为 datetime 对象。

    Java 类比：类似尝试多种 DateTimeFormatter 直到 parse 成功；
    全部失败则返回 null。
    """
    if value is None:  # null 输入
        return None
    if isinstance(value, datetime):  # 已是 datetime 对象则直接返回（避免重复解析）
        return value
    text = str(value).strip()  # 转为字符串并去首尾空白（类似 String.trim()）
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%dT%H:%M:%S.%f"):  # 依次尝试多种日期格式
        try:  # try-catch：当前格式不匹配则捕获 ValueError 继续下一种
            return datetime.strptime(text[:26], fmt)  # strptime ~ SimpleDateFormat.parse；[:26] 截断过长毫秒部分
        except ValueError:  # 格式不匹配，尝试下一个 fmt
            continue
    return None  # 所有格式均失败则返回 null


async def query_logistics(user_id: str, order_id: str) -> str:
    """
    查询订单物流轨迹（只读，直接调 Java Internal API）。

    Java 类比：类似 LogisticsController.query(userId, orderId) 返回 HTML 片段；
    async/await 相当于 CompletableFuture<String> 的异步 HTTP 调用。
    """
    if not order_id:  # 订单号为空则早退（类似 Java 参数校验 @NotBlank）
        return "【查询物流失败】请输入要查询物流的订单号"
    try:  # try-except：捕获下游 Java/网络异常，转为用户可读错误（类似 @ControllerAdvice）
        logistics = await java_internal_client.get_logistics(user_id, order_id)  # await ~ future.get()：等待 Java Internal API 响应
        if not logistics:  # 响应为空表示 Java 端无物流记录
            return "【查询物流失败】订单物流信息不存在"
        records = logistics.get("record_list") or logistics.get("recordList") or []  # 兼容 snake_case / camelCase 键名（Java JSON 序列化风格差异）
        company = logistics.get("logistics_company") or logistics.get("logisticsCompany") or "快递"  # 承运商，缺省「快递」
        logistics_no = logistics.get("logistics_no") or logistics.get("logisticsNo") or "-"  # 运单号
        status = logistics.get("logistics_status")  # 物流状态码（snake_case 字段）
        if status is None:  # 若 snake 键不存在
            status = logistics.get("logisticsStatus")  # 回退 camelCase 键（Java 端可能返回驼峰）
        status_name = {  # 内联 Map：状态码 -> 中文描述（类似 Java enum 描述）
            0: "待揽收",
            1: "已揽收",
            2: "运输中",
            3: "派送中",
            4: "已签收",
            5: "异常",
        }.get(int(status) if status is not None else -1, "运输中")  # 未知状态默认「运输中」

        rows: list[str] = []  # list[str] ~ List<String>：收集 HTML 表格行片段
        for r in records:  # 遍历每条物流轨迹（类似 for (Record r : records)）
            if not isinstance(r, dict):  # 非 Map 结构则跳过（防御性编程）
                continue
            t = r.get("record_time") or r.get("recordTime") or ""  # 轨迹时间，兼容两种命名
            if hasattr(t, "strftime"):  # hasattr ~ 反射：检查是否具备 strftime（datetime 对象）
                t = t.strftime("%Y-%m-%d %H:%M:%S")  # datetime 格式化为字符串
            addr = r.get("record_address") or r.get("recordAddress") or ""  # 轨迹地点
            rows.append("<tr><td>%s</td><td>%s</td></tr>" % (t, addr))  # 追加一行 HTML 表格（供前端/LLM 渲染）

        header = "【订单%s查询物流成功】" % order_id  # 成功标题
        header += chr(10) + "承运商：%s，运单号：%s，状态：%s" % (company, logistics_no, status_name)  # chr(10) 即换行符 \n
        if not rows:  # 无轨迹明细
            return header + chr(10) + "暂无物流轨迹明细。"

        table = "<table>" + chr(10)  # 开始 HTML 表格
        table += "<tr><th>时间</th><th>地点</th></tr>" + chr(10)  # 表头行
        table += chr(10).join(rows) + chr(10) + "</table>"  # join ~ String.join：拼接所有行并闭合 table
        latest = records[0] if records and isinstance(records[0], dict) else {}  # 取最新一条轨迹（List 首元素，Java 端通常按时间倒序）
        latest_addr = latest.get("record_address") or latest.get("recordAddress") or ""  # 最新位置
        footer = (chr(10) + "当前包裹最新位置：" + str(latest_addr)) if latest_addr else ""  # 有地址则追加页脚
        return header + chr(10) + table + footer  # 返回完整 HTML 文本供 LLM 展示
    except Exception as e:  # 捕获任意异常，避免 MCP 工具调用崩溃（类似全局异常处理）
        # 异常细节只进日志，不拼入用户可见文案（防内部信息泄露）
        logger.warning("mcp_tool_query_logistics_failed", error=str(e))
        return "【查询物流失败】系统处理异常，请稍后重试或联系客服"


async def query_comment(user_id: str, order_id: str) -> str:
    """
    查询订单评价详情（只读，调 Java Internal API）。

    Java 类比：类似 CommentInternalController.getByOrderId(userId, orderId)，
    返回 JSON 字符串给 LLM 解析。
    """
    if not order_id:  # 缺少订单号
        return "【查询评价失败】请输入要查询评价的订单号"
    try:  # try-except 包裹 Java 调用
        row = await java_internal_client.get_comment(user_id, order_id)  # await 调用 Java 内部评价接口
        if not row:  # Java 返回 null / 空 Map
            return "【查询评价失败】订单评价不存在"
        return json.dumps(  # 将 Map 序列化为 JSON 字符串（类似 Jackson writeValueAsString）
            {k: (_fmt_dt(v) if isinstance(v, datetime) else v) for k, v in row.items()},  # 字典推导式 ~ Stream.map：datetime 字段先格式化
            ensure_ascii=False,  # 允许中文等非 ASCII 直接输出（Jackson 默认也可能保留中文）
            default=str,  # 无法序列化的类型回退 str()（类似 JsonSerializer fallback）
        )
    except Exception as e:  # 下游异常转用户文案
        return f"【查询评价失败】系统处理异常，请稍后重试或联系客服。错误信息：请稍后重试或联系客服"


async def query_user_coupons(user_id: str, status: int | None = None) -> str:
    """
    查询用户优惠券列表（只读，调 Java Internal API 后在 Python 侧过滤）。

    Java 类比：类似 CouponService.listByUser(userId) + 内存 filter；
    status 为 @Nullable Integer，null 表示查「可用」类默认状态。
    """
    if not user_id:  # 用户 ID 必填（类似 @NotNull userId）
        return "【查询优惠券失败】用户ID不能为空"
    try:  # try-except 包裹整个查询与过滤逻辑
        query_status = 0 if status is None else status  # null 时默认查状态 0（可用/全部语义）
        rows = await java_internal_client.list_user_coupons(user_id)  # 拉取用户全部优惠券原始 List<Map>（Java Internal API）

        result = []  # List ~ 过滤后的结果集（类似 Java ArrayList<CouponVO>）
        now = datetime.now()  # 当前时间，用于判断券是否已过期
        for r in rows:  # 遍历每张券（for-each）
            dynamic = r.get("uc_status")  # 用户券动态状态（优先 snake_case 字段名）
            if dynamic is None:  # snake 字段不存在
                dynamic = r.get("status")  # 回退通用 status 字段
            end = _parse_dt(r.get("valid_end_time"))  # 解析有效期结束时间
            if dynamic == 0 and end and end < now:  # 标记为可用(0)但已过期 -> 修正为已过期(2)
                dynamic = 2
            if query_status == 0 and dynamic != 0:  # 查「可用」时跳过非可用券
                continue
            if query_status == 2 and dynamic != 2:  # 查「已过期」时跳过非过期券
                continue
            if query_status not in (0, 2) and dynamic != query_status:  # 其他明确状态时精确匹配
                continue
            result.append({  # 组装前端/LLM 友好的 camelCase 字段（类似 Java VO 转换）
                "userCouponId": r.get("user_coupon_id"),
                "couponId": r.get("coupon_id"),
                "couponName": r.get("coupon_name"),
                "couponType": r.get("coupon_type"),
                "thresholdAmount": float(r["threshold_amount"]) if r.get("threshold_amount") is not None else None,  # 门槛金额，可能为 null
                "discountAmount": float(r["discount_amount"]) if r.get("discount_amount") is not None else None,  # 减免金额
                "discountRate": float(r["discount_rate"]) if r.get("discount_rate") is not None else None,  # 折扣率
                "validStartTime": _fmt_dt(r.get("valid_start_time")),  # 有效期开始
                "validEndTime": _fmt_dt(r.get("valid_end_time")),  # 有效期结束
                "status": dynamic,  # 计算后的最终状态
            })
            if len(result) >= 20:  # 最多返回 20 条，防止 LLM 上下文过长
                break

        if not result:  # 过滤后无数据
            return "【查询优惠券成功】当前没有符合条件的优惠券"
        return f"【查询优惠券成功】共 {len(result)} 张：{json.dumps(result, ensure_ascii=False)}"  # 成功摘要 + JSON 明细
    except Exception:  # 吞掉具体异常，返回通用失败文案（生产环境可打日志）
        return "【查询优惠券失败】系统处理异常，请稍后重试"


async def _order_items_params(order_id: str, order: dict | None = None) -> list[dict]:
    """
    构建订单项摘要 List<Map>，供确认收货/评价等 pending 卡片展示。

    Java 类比：类似 OrderItemVO 列表，最多 5 条，字段对齐前端卡片组件。
    """
    items_map = await order_service._fetch_order_items([order_id])  # 批量拉取订单项，返回 Map<orderId, List<item>>
    items = items_map.get(order_id, [])  # 取当前订单的项列表，缺省空 List
    return [  # 列表推导式 ~ Stream.map().limit(5).collect()
        {
            "orderItemId": i.get("order_item_id"),  # 订单项 ID
            "productName": i.get("product_name"),  # 商品名
            "cover": i.get("cover", "").split(",")[0] if i.get("cover") else None,  # 封面图取逗号分隔第一张
            "propertyInfo": i.get("property_info"),  # SKU/规格信息
        }
        for i in items[:5]  # 切片 [:5] 限制最多 5 条
    ]


async def propose_confirm_receipt(user_id: str, order_id: str) -> str:
    """
    提案：确认收货（写操作第一阶段，不直接改 Java 数据库）。

    Java 类比：类似「预提交」——只创建 PendingAction（Redis 存 act_xxx），
    用户在前端点确认后，agent_runtime.resolve_action_confirm → action_execute → Java Internal API 真正收货。
    """
    if not order_id:  # 参数校验
        return "【确认收货失败】请输入要确认收货的订单号"
    try:  # try-except 包裹校验与 create_pending
        order = await order_service.get_order(order_id)  # 加载订单主表（可能来自 Java 或本地缓存）
        if not order:  # 订单不存在
            return "【确认收货失败】订单不存在"
        if order["user_id"] != user_id:  # 水平权限校验：只能操作自己的订单（类似 Java @PreAuthorize）
            return "【确认收货失败】您没有权限操作此订单"
        st = order["order_status"]  # 当前订单状态码
        if st not in CONFIRM_RECEIPT_ORDER_STATUSES:  # 状态不在允许集合内（类似状态机校验）
            return (
                f"【确认收货失败】当前订单状态无法确认收货，当前状态：{_status_name(st)}"
            )
        params = {  # 写入 pending action 的业务参数 Map（确认后原样传给 Java）
            "orderId": order_id,
            "orderAmount": float(order["amount"]),  # 实付金额转 float 便于 JSON 序列化
            "payScene": order.get("pay_scene"),  # 支付场景
            "orderItems": await _order_items_params(order_id, order),  # 嵌套异步拉取订单项摘要
        }
        pending = await pending_action_service.create_pending(  # 创建待确认操作记录（Redis，生成 act_xxx token）
            "CONFIRM_RECEIPT",  # 操作类型枚举值（类似 Java ActionType.CONFIRM_RECEIPT）
            user_id,
            params,
            f"确认收货：订单 {order_id}，实付金额 {order['amount']} 元",  # 人类可读摘要，展示在确认卡上
        )
        return _propose_reply("确认收货", pending)  # 返回 LLM 引导话术 + 【act_xxx】token
    except Exception as e:  # 系统异常
        return f"【确认收货失败】系统处理异常，请稍后重试或联系客服。错误信息：请稍后重试或联系客服"


async def propose_refund(user_id: str, order_item_id: str) -> str:
    """
    提案：申请退款（写操作第一阶段，支持订单项 ID 或订单号兜底）。

    Java 类比：类似 RefundService.propose(userId, orderItemId) 只落 Redis pending，
    不调用 Java 退款接口；用户 confirm 后才真正 refund。
    LLM 常误传 orderId 而非 orderItemId，本函数有多项可退时的歧义处理。
    """
    if not order_item_id:  # 参数不能为空
        return "【退款失败】请输入要退款的订单项ID"
    try:  # try-except 包裹整段退款提案逻辑
        from app.utils.order_ids import extract_order_id, extract_order_item_id  # 延迟 import：避免模块级循环依赖（类似 Java 懒加载 Bean）

        # 规范化：优先解析为 orderItemId；纯订单号会在下方 fallback 分支处理
        normalized = extract_order_item_id(order_item_id) or (order_item_id or "").strip()  # 规范化 ID，或保留 trim 后的原串
        item = await order_service.get_order_item(normalized)  # 按 ID 查订单项
        if not item:  # 查不到项：LLM / 强制路径常传 orderId 而非 orderItemId
            # LLM 或 force 路径经常只给 orderId，此处尝试按订单号兜底
            order_id = extract_order_id(normalized) or normalized  # 解析不出项 ID 则当订单号处理
            refundable = await order_service.list_refundable_items(user_id, order_id)  # 列出该订单下可退款项
            if len(refundable) == 1:  # 仅一项可退：自动选中（减少用户歧义）
                item = refundable[0]
                normalized = str(item.get("order_item_id") or "")
            elif len(refundable) > 1:  # 多项可退：要求用户明确指定 orderItemId
                lines = []  # 收集提示行
                for row in refundable[:8]:  # 最多展示 8 个候选
                    oid = row.get("order_item_id")
                    name = row.get("product_name") or "商品"
                    lines.append(f"- {name}（订单项ID：{oid}）")
                return (
                    "【退款失败】该订单有多个可退款商品，请指定其中一个订单项ID后再试：\n"
                    + "\n".join(lines)  # join 多行提示（类似 String.join("\n", lines)）
                )
            else:  # 无可退项：区分「订单不存在」vs「状态不允许」
                order = await order_service.get_order(order_id)
                if order and order.get("user_id") == user_id:  # 订单存在且归属正确
                    st = order.get("order_status")
                    return (
                        f"【退款失败】订单存在，但当前状态为{_status_name(st)}，"
                        "仅待发货/已发货/部分退款订单可申请退款。"
                        "若订单项ID形如「订单号_1」，请使用完整订单项ID重试。"
                    )
                return (
                    "【退款失败】订单项不存在，请确认订单项ID是否正确"
                    "（格式一般为：订单号_1）。"
                )
        order_item_id = normalized  # 统一使用规范化后的 ID
        if not item or not order_item_id:  # 二次防御：仍无有效项
            return "【退款失败】订单项不存在，请确认订单项ID是否正确（格式一般为：订单号_1）。"
        order = await order_service.get_order(item["order_id"])  # 加载所属订单
        if not order or order["user_id"] != user_id:  # 权限校验
            return "【退款失败】您没有权限操作此订单项"
        st = order.get("order_status")
        if st not in REFUNDABLE_ORDER_STATUSES:  # 订单状态不允许退款
            return (
                f"【退款失败】当前订单状态为{_status_name(st)}，"
                "仅待发货/已发货/部分退款订单可申请退款"
            )
        if item.get("order_item_status") != ORDER_ITEM_STATUS_NORMAL:  # 订单项已退或异常
            return "【退款失败】当前订单项已退款，无法重复申请"
        params = {  # pending 参数：退款金额、商品快照等（confirm 后传给 Java）
            "orderItemId": order_item_id,
            "orderId": item["order_id"],
            "refundAmount": float(item["item_amount"]),
            "payScene": order.get("pay_scene"),
            "orderItems": [{  # 单元素 List，结构与确认收货卡片一致
                "orderItemId": order_item_id,
                "productName": item.get("product_name"),
                "cover": (item.get("cover") or "").split(",")[0] or None,
                "propertyInfo": item.get("property_info"),
                "itemAmount": float(item["item_amount"]),
                "buyCount": item.get("buy_count"),
            }],
        }
        name = item.get("product_name") or "商品"
        pending = await pending_action_service.create_pending(  # 写入 Redis，生成 act_xxx
            "REFUND",
            user_id,
            params,
            f"退款：订单项 {order_item_id}（{name}），金额 {item['item_amount']} 元",
        )
        return _propose_reply("退款", pending)  # 引导 LLM 在回复末尾附带 token
    except Exception as e:
        return f"【退款失败】系统处理异常，请稍后重试或联系客服。错误信息：请稍后重试或联系客服"


async def propose_product_review(user_id: str, order_id: str, content: str, star: int) -> str:
    """
    提案：首次商品评价（写操作第一阶段）。

    Java 类比：类似 ReviewService.propose(userId, orderId, content, star)，
    只 create_pending，用户 confirm 后调 Java 写评价表。
    """
    if not order_id:  # 订单号必填
        return "【评价失败】请输入要评价的订单号"
    if not content:  # 评价内容必填
        return "【评价失败】请输入评价内容"
    if star is None or star < 1 or star > 5:  # 星级 1-5 校验（类似 @Min(1) @Max(5)）
        return "【评价失败】评价星级必须是1-5的整数"
    try:
        order = await order_service.get_order(order_id)
        if not order:
            return "【评价失败】订单不存在"
        if order["user_id"] != user_id:  # 权限：只能评价自己的订单
            return "【评价失败】您没有权限评价此订单"
        st = order["order_status"]
        if st not in REVIEWABLE_ORDER_STATUSES:  # 须已完成等可评价状态
            return f"【评价失败】当前订单状态为{_status_name(st)}，订单完成后才能评价"
        params = {  # pending 业务参数
            "orderId": order_id,
            "commentContent": content,  # 评价正文
            "star": star,  # 1-5 星
            "payScene": order.get("pay_scene"),
            "orderItems": await _order_items_params(order_id, order),
        }
        pending = await pending_action_service.create_pending(
            "PRODUCT_REVIEW",
            user_id,
            params,
            f"提交评价：订单 {order_id}，{star} 星，内容「{_truncate(content)}」",  # 摘要中截断过长内容
        )
        return _propose_reply("评价", pending)
    except Exception as e:
        return f"【评价失败】系统处理异常，请稍后重试或联系客服。错误信息：请稍后重试或联系客服"


async def propose_recomment(user_id: str, order_id: str, content: str) -> str:
    """
    提案：追评（二次评价，写操作第一阶段）。

    Java 类比：类似 ReCommentService.propose，与首次评价共用 REVIEWABLE 状态校验，
    pending 类型为 RECOMMENT，confirm 后调 Java 追评接口。
    """
    if not order_id:
        return "【追评失败】请输入要追评的订单号"
    if not content:
        return "【追评失败】请输入追评内容"
    try:
        order = await order_service.get_order(order_id)
        if not order:
            return "【追评失败】订单不存在"
        if order["user_id"] != user_id:
            return "【追评失败】您没有权限评价此订单"
        st = order["order_status"]
        if st not in REVIEWABLE_ORDER_STATUSES:  # 与首次评价相同的状态要求
            return f"【追评失败】当前订单状态为{_status_name(st)}，订单完成后才能追评"
        params = {
            "orderId": order_id,
            "reCommentContent": content,  # 追评正文（与首次评价字段 commentContent 区分）
            "payScene": order.get("pay_scene"),
            "orderItems": await _order_items_params(order_id, order),
        }
        pending = await pending_action_service.create_pending(
            "RECOMMENT",
            user_id,
            params,
            f"提交追评：订单 {order_id}，内容「{_truncate(content)}」",
        )
        return _propose_reply("追评", pending)
    except Exception as e:
        return f"【追评失败】系统处理异常，请稍后重试或联系客服。错误信息：请稍后重试或联系客服"


async def tool_search_products(
    user_id: str,
    keyword: str,
    exclude_product_id: str | None = None,  # @Nullable：排除已咨询/已选商品 ID
) -> "ToolInvokeResult":
    """
    MCP 工具：搜索商品（只读，返回 ToolInvokeResult 含卡片 JSON）。

    Java 类比：类似 ProductSearchFacade.search(userId, keyword)，
    返回 DTO 含 content（给 LLM）、assistant_cards（给前端）、bizData。
    与 PROPOSE_* 不同：本工具不创建 pending，直接读商品库/Java API。
    """
    from app.services.product_service import product_service  # 延迟 import 避免循环依赖
    from app.services.redis_service import redis_service  # Redis：咨询上下文、会话状态
    from app.services.tool_invoke_result import ToolInvokeResult  # MCP 工具统一返回 DTO（类似 Java ResponseEntity<T>）

    consult = await redis_service.get_consult_product(user_id)  # 读取用户当前「咨询中」的商品（来自商品详情页）

    if not await redis_service.is_consult_active(user_id):  # 咨询会话未激活则忽略 consult
        consult = None
    assistant, biz_data, biz_type, products, source = await product_service.search_products(  # 解构五元组返回值
        user_id,
        keyword,
        user_text=keyword or "",  # 原始用户检索词
        consult_product=consult,  # 结合咨询上下文做排序/过滤
        exclude_product_id=exclude_product_id,
    )
    if not products:  # 空结果
        return ToolInvokeResult(content="【搜索结果】未找到相关商品。")
    from app.services.product_service import format_search_tool_message  # 延迟 import 格式化函数

    names = [str(p.get("product_name") or p.get("productName") or "") for p in products]  # 列表推导：商品名 List
    ids = [str(p.get("product_id") or p.get("productId") or "") for p in products if p.get("product_id") or p.get("productId")]  # 有 ID 的才收集
    content = format_search_tool_message(keyword, consult, products, source)  # 生成给 LLM 的文本摘要
    return ToolInvokeResult(  # 结构化结果：文本 + 卡片 + 业务元数据
        content=content,
        biz_type=biz_type,
        biz_data=biz_data,
        assistant_cards=assistant,  # 前端助手卡片 JSON 串
        product_ids=ids,
        product_names=[n for n in names if n],  # 过滤空名
    )


async def tool_query_orders(user_id: str, order_id: str | None = None) -> "ToolInvokeResult":
    """
    MCP 工具：查询用户订单列表或单笔订单（只读）。

    Java 类比：类似 OrderQueryService.query(userId, orderId)，
    assistant_cards 为订单卡片 JSON，供 agent_runtime 渲染 query_order UI。
    """
    from app.services.tool_invoke_result import ToolInvokeResult
    import json  # 局部 import json（与模块顶部重复，保持原逻辑不变）

    assistant, biz_data, biz_type = await order_service.query_orders(user_id, order_id or None)  # order_id or None：空串转 null
    if assistant == "[]":  # 无订单
        return ToolInvokeResult(content="【订单查询】未找到相关订单。")
    try:  # 尝试解析 assistant 为 JSON 数组
        cards = json.loads(assistant)  # 解析助手卡片 JSON 为 List/Map 结构
        order_ids = [str(c.get("orderId") or c.get("order_id") or "") for c in cards if isinstance(c, dict)]  # 提取订单号
        order_ids = [oid for oid in order_ids if oid]  # 去掉空串
        summary = "、".join(order_ids[:5])  # 摘要最多展示 5 个订单号
        return ToolInvokeResult(
            content=f"【订单查询】找到 {len(cards)} 笔订单：{summary}",
            biz_type=biz_type,
            biz_data=biz_data,
            assistant_cards=assistant,
            order_ids=order_ids,
        )
    except json.JSONDecodeError:  # assistant 非合法 JSON 时降级为纯文本
        return ToolInvokeResult(content=f"【订单查询】{assistant[:300]}", biz_type=biz_type, biz_data=biz_data)


async def tool_get_product_detail(user_id: str, product_id: str) -> str:
    """
    MCP 工具：获取商品详情纯文本（只读）。

    Java 类比：类似 ProductDetailService.getDetailText(productId)；
    user_id 保留在签名中以满足 MCP 工具统一接口，当前实现未使用。
    """
    from app.services.product_service import product_service

    _ = user_id  # 占位：签名保留 user_id 供 MCP 工具统一接口，当前实现未使用（类似 Java 接口方法未用到的参数）
    return await product_service.get_product_detail_text(product_id)  # 委托商品服务生成详情文案
