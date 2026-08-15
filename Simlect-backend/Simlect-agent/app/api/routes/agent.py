"""
================================================================================
文件：api/routes/agent.py
角色：智能客服对外 HTTP 接口（类比 @RestController("/api/agent")）
================================================================================

【这个文件干什么】
把前端/Gateway 打来的表单请求转给 Service；本文件尽量薄，不含 LLM 逻辑。

【主要接口】
| 路径 | 作用 | 下游 |
|------|------|------|
| POST /sendMessage | 发用户消息，立即返回 messageId | agent_orchestrator |
| POST /loadHistoryMessage | 拉历史 | message_service |
| POST /cancelMessage | 停止生成 | orchestrator → Redis 取消标记 |
| POST /confirmAction | 用户确认写操作 | pending → action_execute → Java |
| POST /cancelAction | 取消待确认 | pending_action_service |
| POST /clearProductConsult 等 | 商品咨询上下文 | redis_service |
| POST /admin/* | 管理端（需 X-Internal-Token） | message_service |

【如何联调】
经 Gateway：/api/agent/sendMessage，Header/Cookie 带登录 token。
流式内容看 WebSocket，不看这个 HTTP 的 body。

【写操作两阶段】PROPOSE_* 只出确认卡；点确认才走 confirmAction。
详见 app/api/MODULE.md、docs/智能客服-Java开发者导读.md
================================================================================
"""

import hmac
from fastapi import APIRouter, Depends, Form, Header, Request  # 路由注册、依赖注入、表单绑定、请求头、原始 Request
from app.api.rate_limit import limiter  # 共享全局限流器单例（防双实例限流失效）  # 接口级限流装饰器（类比 @RateLimiter 注解）
  # 默认限流 key：客户端 IP（无 token 时的兜底）

from app.api.deps import TokenUserInfo, get_request_token, require_login  # 登录校验与用户上下文 DTO
from app.config.settings import get_settings  # 读取 internal_token 等配置
from app.models.response import ResponseVO, error, success  # 统一响应包装（类比 Result<T> / ResponseEntity）
from app.services.action_execute_service import action_execute_service  # 确认后真正调 Java 订单接口
from app.services.agent_service import agent_orchestrator  # 发消息/取消/咨询上下文编排门面
from app.services.message_service import agent_message_service  # 消息持久化与历史查询
from app.services.pending_action_service import pending_action_service  # 待确认写操作（两阶段提交）
from app.services.rate_limit_service import rate_limit_service  # Redis 滑动窗口限流（与装饰器双保险）
from app.services.redis_service import redis_service  # 商品咨询态 Redis 读写

router = APIRouter(prefix="/agent", tags=["agent"])  # 路由前缀 /agent，Swagger 分组名 agent

limiter = limiter  # 共享全局限流器单例（见 app.api.rate_limit）


def _user_key(request: Request) -> str:
    """限流 key：优先用登录 token，无 token 则退化为 IP（避免同一用户多 tab 共享配额）。"""

    token = get_request_token(request)  # 从 Header/Cookie 解析 token 字符串
    return token or get_remote_address(request)  # token 为空时用 IP 作为限流 key


def _form_bool(value: str | bool | None) -> bool:
    """把表单里的布尔值统一转成 Python bool（前端可能传 "true"/"1"/true）。"""

    if value is None:  # 表单未传该字段
        return False  # 默认 false，符合「未勾选」语义
    if isinstance(value, bool):  # FastAPI 有时已解析成 bool
        return value  # 直接返回，无需再转
    return str(value).lower() in ("true", "1", "yes")  # 字符串真值集合，大小写不敏感


def _require_internal_token(x_internal_token: str | None = Header(None, alias="X-Internal-Token")) -> str:
    """管理端接口鉴权：Header 必须带正确的 X-Internal-Token（类比内部 API Key 校验）。"""

    expected = get_settings().internal_token  # 配置中的内部密钥，与 Gateway/管理端约定一致
    if not x_internal_token or not hmac.compare_digest((x_internal_token or ""), expected):  # 恒定时间比较（缺失或不匹配）
        from fastapi import HTTPException  # 局部导入，避免顶层循环依赖

        raise HTTPException(status_code=401, detail="invalid internal token")  # 401 未授权，不暴露细节
    return x_internal_token  # 校验通过，返回值供 Depends 注入（此处变量名 _token 表示未再用）


async def _read_admin_body(request: Request) -> dict:
    """管理端 body 兼容 JSON 与 form-urlencoded，统一返回 dict（≈Map<String,Object>）。"""

    ct = (request.headers.get("content-type") or "").lower()  # Content-Type 转小写便于子串匹配
    if "application/json" in ct:  # JSON 请求体（管理端常用）
        data = await request.json()  # 异步读取并解析 JSON
        return data if isinstance(data, dict) else {}  # 根节点非 dict（如数组）则当空对象
    form = await request.form()  # 表单字段（multipart 或 x-www-form-urlencoded）
    return {k: form.get(k) for k in form.keys()}  # 字典推导式转成普通 dict


def _as_int(value, default: int | None = None) -> int | None:
    """安全地把任意值转 int；失败或空则返回 default（类比 NumberUtils.toInt）。"""
    if value is None or value == "":  # 空值或空字符串
        return default  # 调用方传入的默认值
    try:
        return int(value)  # 正常转换，如 "123" → 123
    except (TypeError, ValueError):  # 非数字字符串、对象等
        return default  # 静默失败，不抛异常


@router.post("/loadHistoryMessage")
async def load_history_message(
    pageNo: int = Form(1),  # 页码，默认第 1 页（前端分页加载）
    maxMessageId: int | None = Form(None),  # 可选：只拉 id 小于该值的消息（上拉加载更早记录）
    user: TokenUserInfo = Depends(require_login),  # 必须登录，FastAPI 自动注入当前用户 DTO
) -> ResponseVO:
    """分页加载当前用户的历史聊天记录。"""
    data = await agent_message_service.load_history(user.user_id, pageNo, maxMessageId)
    return success(data)  # 包装成统一成功响应 { code:200, data:... }


@router.post("/sendMessage")
@limiter.limit("1/second", key_func=_user_key)  # 装饰器限流：每用户每秒最多 1 条（与 Service 内 Redis 限流双保险）
async def send_message(
    request: Request,  # slowapi 限流装饰器需要 Request 对象取 key
    message: str = Form(...),  # 用户消息正文，必填（... 表示无默认值）
    fromProduct: str | None = Form(None),  # 是否从商品页进入，可选
    consultProductId: str | None = Form(None),  # 商品页携带的商品 ID，可选
    user: TokenUserInfo = Depends(require_login),  # 登录用户上下文
) -> ResponseVO:
    """发送用户消息；HTTP 立即返回 messageId，LLM 推理在后台异步进行。"""
    try:
        data = await agent_orchestrator.send_message(
            user.user_id,  # 从 token 解析出的用户 ID
            message,  # 原始消息
            _form_bool(fromProduct),  # 表单 bool 规范化后再传给 Service
            consultProductId,  # 商品 ID 原样传递
        )
        return success(data)  # 含 messageId 等字段，前端据此关联 WS 流
    except ValueError as e:
        # 业务校验失败（限流、敏感词、体验次数等）→ 600 业务码，HTTP 仍 200（类比 BizException）
        return error(600, str(e))


@router.post("/cancelMessage")
@limiter.limit("1/second", key_func=_user_key)  # 取消也限 1 次/秒
async def cancel_message(
    request: Request,  # 限流装饰器需要
    messageId: int = Form(...),  # 要取消的那条用户消息 id（数据库主键）
    assistantMessage: str | None = Form(None),  # 前端已收到的部分助手回复（可选，用于 interrupt）
    user: TokenUserInfo = Depends(require_login),
) -> ResponseVO:
    """用户点击「停止生成」：设 Redis 取消标记并更新消息状态。"""
    await agent_orchestrator.cancel_message(user.user_id, messageId, assistantMessage)
    return success(None)  # 无业务数据，仅表示操作成功


@router.post("/clearProductConsult")
async def clear_product_consult(user: TokenUserInfo = Depends(require_login)) -> ResponseVO:
    """清除当前用户的商品咨询上下文（离开咨询场景时调用）。"""

    await redis_service.clear_consult(user.user_id)  # 删除 Redis 快照与 active 标记
    return success(None)


@router.post("/pauseProductConsult")
async def pause_product_consult(user: TokenUserInfo = Depends(require_login)) -> ResponseVO:
    """暂停商品咨询（保留快照，但标记为非 active，咨询条变灰）。"""

    await redis_service.pause_consult(user.user_id)  # 只改 active 标志，不删商品数据
    return success(None)


@router.post("/getProductConsultContext")
async def get_product_consult_context(
    user: TokenUserInfo = Depends(require_login),
) -> ResponseVO:
    """查询当前正在咨询的商品信息（供前端展示顶部咨询条）。"""
    ctx = await agent_orchestrator.get_consult_context(user.user_id)  # 可能为 None
    return success(ctx)  # 无咨询时 data=null


@router.post("/confirmAction")
@limiter.limit("3/second", key_func=_user_key)  # 确认操作稍宽松：3 次/秒
async def confirm_action(
    request: Request,
    actionToken: str = Form(...),  # 待确认操作的 token（由 LLM 确认卡下发，一次性）
    user: TokenUserInfo = Depends(require_login),
) -> ResponseVO:
    """用户点击确认卡：执行下单/改地址等写操作（调 Java 后端）。"""

    if not await rate_limit_service.allow(user.user_id, "confirmAction", 1, 3):
        # Redis 滑动窗口：3 秒内最多 1 次 confirm（与装饰器限流双保险）
        return success({
            "actionType": None,  # 未识别到操作类型
            "success": False,  # 业务失败
            "resultMessage": "操作过于频繁，请稍后再试",
        })
    token = get_request_token(request) or user.token or ""  # 调 Java 时需要透传用户 token 做鉴权

    async def executor(pending: dict) -> str:
        """pending_action_service 回调：真正执行已确认的写操作（类比 Strategy 函数式接口）。"""
        return await action_execute_service.execute(pending, token)  # pending≈Map，含 actionType、参数等

    try:
        action_type, ok, msg = await pending_action_service.confirm(
            user.user_id, actionToken, executor  # 校验 token 后调用 executor
        )
        return success({
            "actionType": action_type,  # 如 PLACE_ORDER、UPDATE_ADDRESS
            "success": ok,  # Java 侧是否执行成功
            "resultMessage": msg,  # 给用户看的结果文案
        })
    except ValueError as e:
        # token 过期、已执行、参数非法等业务错误仍返回 HTTP 200 + success:false
        return success({
            "actionType": None,
            "success": False,
            "resultMessage": str(e),  # 异常消息直接返回前端展示
        })


@router.post("/cancelAction")
@limiter.limit("3/second", key_func=_user_key)
async def cancel_action(
    request: Request,
    actionToken: str = Form(...),  # 要作废的 pending action token
    user: TokenUserInfo = Depends(require_login),
) -> ResponseVO:
    """用户拒绝确认卡：作废 pending 记录，不调用 Java。"""
    if not await rate_limit_service.allow(user.user_id, "cancelAction", 1, 3):
        return success({
            "actionType": None,
            "success": False,
            "resultMessage": "操作过于频繁，请稍后再试",
        })
    try:
        await pending_action_service.cancel(user.user_id, actionToken)  # 仅删 Redis pending，不调 Java
        return success(None)  # 取消成功，无额外 data
    except ValueError as e:
        return success({
            "actionType": None,
            "success": False,
            "resultMessage": str(e),
        })


@router.post("/admin/loadMessages")
async def admin_load_messages(
    request: Request,  # 需要读 body，不能只用 Form
    _token: str = Depends(_require_internal_token),  # 内部 token 校验；_ 前缀表示返回值未使用
) -> ResponseVO:
    """管理后台：分页查询消息列表，可按 userId 过滤。"""
    body = await _read_admin_body(request)  # 兼容 JSON/form
    page_no = _as_int(body.get("pageNo"), 1) or 1  # 默认第 1 页；or 1 防止 _as_int 返回 0
    page_size = _as_int(body.get("pageSize"), 15) or 15  # 默认每页 15 条
    user_id = body.get("userId") or None  # 可选过滤条件
    if user_id is not None:
        user_id = str(user_id).strip() or None  # 空字符串当 None，表示查全部用户
    data = await agent_message_service.admin_load_messages(page_no, page_size, user_id)
    return success(data)  # 分页结果 List + total


@router.post("/admin/getMessage")
async def admin_get_message(
    request: Request,
    _token: str = Depends(_require_internal_token),  # 管理端鉴权
) -> ResponseVO:
    """管理后台：按 messageId 查单条消息详情（含助手回复、卡片等）。"""
    body = await _read_admin_body(request)
    message_id = _as_int(body.get("messageId"))  # 必填，但用安全转换
    if not message_id:
        return error(600, "messageId 不能为空")  # 参数校验失败
    data = await agent_message_service.admin_get_message(message_id)
    return success(data)


@router.post("/admin/deleteMessage")
async def admin_delete_message(
    request: Request,
    _token: str = Depends(_require_internal_token),
) -> ResponseVO:
    """管理后台：删除指定消息（软删或硬删由 Service 实现决定）。"""
    body = await _read_admin_body(request)
    message_id = _as_int(body.get("messageId"))
    if not message_id:
        return error(600, "messageId 不能为空")
    ok = await agent_message_service.admin_delete_message(message_id)  # 返回是否删除成功
    return success({"deleted": ok})  # 布尔结果包在 data 里
