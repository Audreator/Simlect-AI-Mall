"""
================================================================================
文件：graph/__init__.py
角色：graph 包对外出口（类比 Java 的门面：外界只关心「跑图」这一件事）
================================================================================

【这个文件干什么】
把 runner.run_agent_graph 提升为包级 API，方便写：

    from app.graph import run_agent_graph

【关联】
- 真正逻辑在 runner.py
- 拓扑在 builder.py，节点在 nodes.py，断点在 checkpoint/redis_saver.py
================================================================================
"""

# 从 runner 模块导入「执行一轮智能客服图」的入口函数
from app.graph.runner import run_agent_graph

# 限制 from app.graph import * 时只导出这一符号
__all__ = ["run_agent_graph"]
