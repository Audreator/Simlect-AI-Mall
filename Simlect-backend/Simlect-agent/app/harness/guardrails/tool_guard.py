"""
工具调用护栏模块（Tool Guardrail）。

职责：限制 Agent 只能调用白名单内的工具，并校验工具参数（如 userId 归属）。
类似 Java 里 @PreAuthorize 或自定义 ToolInvocationInterceptor。

工具分两类：
- READ_TOOLS：只读查询（搜商品、查订单等）
- WRITE_TOOLS：写操作提案（PROPOSE_*，需用户确认后才真正执行）
"""

# 只读工具白名单，frozenset 不可变，类似 Collections.unmodifiableSet
READ_TOOLS = frozenset({
    "SEARCH_PRODUCTS",  # 搜索商品
    "QUERY_ORDERS",  # 查询订单
    "GET_PRODUCT_DETAIL",  # 商品详情
    "QUERY_LOGISTICS",  # 物流
    "QUERY_COMMENT",  # 评价查询
    "QUERY_USER_COUPONS",  # 用户优惠券
})

# 写操作「提案」工具：生成确认卡片，不直接改库
WRITE_TOOLS = frozenset({
    "PROPOSE_REFUND",  # 提案退款
    "PROPOSE_CONFIRM_RECEIPT",  # 提案确认收货
    "PROPOSE_PRODUCT_REVIEW",  # 提案商品评价
    "PROPOSE_RECOMMENT",  # 提案追评
})

# 并集：所有允许调用的工具名
ALL_ALLOWED_TOOLS = READ_TOOLS | WRITE_TOOLS  # 集合 union，类似 Set.addAll


class ToolGuardrail:
    """
    工具护栏类。

    类似 Java @Component ToolGuardrail，在 tool 执行前调用。
    """

    def is_allowed(self, tool_name: str) -> bool:
        """
        判断工具名是否在白名单内。

        参数:
            tool_name: LLM 请求调用的工具名

        返回:
            True 表示允许执行
        """
        return tool_name in ALL_ALLOWED_TOOLS  # 集合 contains

    def is_write_tool(self, tool_name: str) -> bool:
        """
        判断是否为写操作提案类工具。

        用于输出护栏：只有调用过 PROPOSE_* 才允许展示确认卡片话术。

        参数:
            tool_name: 工具名

        返回:
            True 表示 WRITE_TOOLS 成员
        """
        return tool_name in WRITE_TOOLS

    def validate_tool_args(self, tool_name: str, args: dict, user_id: str) -> bool:
        """
        校验工具参数字典是否合法（防越权）。

        当前规则：若 args 含 userId，必须与当前会话 user_id 一致。

        参数:
            tool_name: 工具名（预留按工具扩展校验）
            args: LLM 传入的参数 dict，类似 Map<String, Object>
            user_id: 当前登录/会话用户 ID

        返回:
            True 表示参数合法
        """
        if "userId" in args and args["userId"] != user_id:  # 禁止查他人数据
            return False
        return True  # 通过
