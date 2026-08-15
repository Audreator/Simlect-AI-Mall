"""
================================================================================
文件：services/tool_invoke_result.py
角色：MCP 工具返回值的统一 DTO + Wire 序列化协议
================================================================================

【这个文件干什么】
定义 ToolInvokeResult 数据类，封装：
- content：给 LLM 看的文本
- assistant_cards / biz_type / biz_data：给前端的结构化卡片
- product_ids / order_ids：业务元数据

【Wire 协议】
MCP 进程与 Agent 进程之间只传字符串，结构化字段通过：
  SIMLECT_TOOL_RESULT:{...json...}
前缀标识，parse_tool_wire() 反序列化还原。

【流程】
mcp_tools_service 返回 ToolInvokeResult
  → server.py _text() 调 to_wire() 序列化
  → mcp_streamable_client 收到字符串
  → mcp_tool_router 调 parse_tool_wire() 还原 DTO

【关联】
mcp_tools_service、mcp_server/server.py、mcp_tool_router、graph/nodes.tools_node
================================================================================
"""

from __future__ import annotations  # import：延迟类型注解，类似 Java 泛型前向引用

import json  # import：JSON 序列化/反序列化，类似 Jackson ObjectMapper
from dataclasses import dataclass, field  # import：@dataclass 自动生成 __init__ 等，类似 Lombok @Data

WIRE_PREFIX = "SIMLECT_TOOL_RESULT:"  # Wire 协议固定前缀 — 标识后续 JSON 为结构化工具结果


@dataclass  # 装饰器：自动生成构造器、__repr__ 等 — 类似 Java record 或 Lombok @Value
class ToolInvokeResult:  # 工具调用结果 DTO — 封装文本 + 卡片 + 业务元数据
    content: str  # 给 LLM / 用户看的纯文本摘要（必填）
    biz_type: str | None = None  # 业务类型标识，如 product_search — None 类似 @Nullable
    biz_data: str | None = None  # 业务附加数据 JSON 字符串 — 可选
    assistant_cards: str | None = None  # 前端助手卡片 JSON 字符串 — 可选
    product_ids: list[str] = field(default_factory=list)  # 商品 ID 列表 — default_factory 避免可变默认参数陷阱
    product_names: list[str] = field(default_factory=list)  # 商品名称列表
    order_ids: list[str] = field(default_factory=list)  # 订单 ID 列表

    def to_tool_message(self) -> str:  # 转为 LangChain ToolMessage.content 用的字符串
        return self.content  # 图节点只把文本塞回 LLM 上下文

    def to_biz_dict(self) -> dict | None:  # 转为 Agent 运行时合并用的业务 Map
        if not (self.product_ids or self.product_names or self.order_ids):  # 无任何 ID 字段
            return None  # 返回 null，表示无需附加业务数据
        return {  # 构造 camelCase 键的 Map — 与前端约定一致
            "productIds": self.product_ids,  # 商品 ID 数组
            "productNames": self.product_names,  # 商品名数组
            "orderIds": self.order_ids,  # 订单 ID 数组
        }

    def to_wire(self) -> str:  # 序列化为 MCP 跨进程传输用的 Wire 字符串
        """Serialize for MCP transport so Agent can restore cards/biz fields."""
        if not (self.assistant_cards or self.biz_type or self.biz_data or self.product_ids or self.order_ids):  # 无结构化字段
            return self.content or ""  # 退化为纯文本，不加前缀
        payload = {  # 构造 JSON 载荷 Map
            "content": self.content or "",  # 文本内容
            "bizType": self.biz_type,  # 业务类型
            "bizData": self.biz_data,  # 业务数据
            "assistantCards": self.assistant_cards,  # 助手卡片
            "productIds": self.product_ids,  # 商品 ID 列表
            "productNames": self.product_names,  # 商品名列表
            "orderIds": self.order_ids,  # 订单 ID 列表
        }
        return WIRE_PREFIX + json.dumps(payload, ensure_ascii=False)  # 前缀 + JSON，ensure_ascii=False 保留中文


def parse_tool_wire(text: str | None) -> ToolInvokeResult:  # 把 MCP 返回的字符串解析回 ToolInvokeResult
    """Parse MCP tool text back into ToolInvokeResult (plain text or wire envelope)."""
    raw = text or ""  # null 安全 — text 为 None 时用空串
    if not raw.startswith(WIRE_PREFIX):  # 不以 Wire 前缀开头
        return ToolInvokeResult(content=raw)  # 当作纯文本结果包装
    try:  # 尝试 JSON 反序列化
        obj = json.loads(raw[len(WIRE_PREFIX) :])  # 去掉前缀后 parse JSON → Map
    except json.JSONDecodeError:  # JSON 格式非法
        return ToolInvokeResult(content=raw)  # 退化为纯文本，避免崩溃
    if not isinstance(obj, dict):  # 解析结果不是 Map
        return ToolInvokeResult(content=raw)  # 同样退化为纯文本
    return ToolInvokeResult(  # 从 Map 还原各字段，兼容 camelCase 与 snake_case 两种键名
        content=str(obj.get("content") or ""),  # 文本内容
        biz_type=obj.get("bizType") or obj.get("biz_type"),  # 业务类型
        biz_data=obj.get("bizData") or obj.get("biz_data"),  # 业务数据
        assistant_cards=obj.get("assistantCards") or obj.get("assistant_cards"),  # 助手卡片
        product_ids=list(obj.get("productIds") or obj.get("product_ids") or []),  # 商品 ID 列表
        product_names=list(obj.get("productNames") or obj.get("product_names") or []),  # 商品名列表
        order_ids=list(obj.get("orderIds") or obj.get("order_ids") or []),  # 订单 ID 列表
    )
