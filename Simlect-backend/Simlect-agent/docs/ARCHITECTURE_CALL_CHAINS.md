# Simlect + Ragent 全栈调用链总览

## Simlect 商城 + Agent

```mermaid
flowchart TB
  subgraph frontend [Simlect-web Vue]
    UI[AgentChatItem / WebSocket]
  end
  subgraph agent_py [Simlect-agent Python :7050]
    API[api/routes/agent.py]
    SVC[services/agent_service]
    ENG[services/agent_engine]
    GRP[graph/runner + nodes]
    RT[agent_runtime]
    MCPc[mcp_streamable_client]
  end
  subgraph mcp_proc [MCP Server :7060]
    MS[mcp_server/server.py]
    MT[mcp_tools_service]
  end
  subgraph java [Simlect Java 微服务]
    GW[Gateway]
    PR[product/internal]
    OR[order/internal]
    US[user/internal]
  end
  UI -->|POST sendMessage| API
  UI -->|WS| API
  API --> SVC --> ENG --> GRP
  GRP --> RT
  GRP --> MCPc --> MS --> MT
  MT -->|/internal/**| PR
  MT -->|/internal/**| OR
  MT -->|/internal/**| US
  UI -->|confirmAction| API -->|/api/order/*| GW
```

## Ragent RAG

```mermaid
flowchart TB
  FE[ragent/frontend React]
  CHAT[api/routes/chat.py SSE]
  RCS[rag_chat_service]
  RUN[graph/runner]
  RET[retrieve/engine]
  LLM[infra/llm_service]
  FE --> CHAT --> RCS --> RUN
  RUN --> RET --> LLM
```

## 各模块详细文档

- Simlect-agent: 各 `app/*/MODULE.md`
- ragent-python: 各 `app/*/MODULE.md`
- Simlect Java: 各 `Simlect-*/MODULE.md`
- Ragent Java: `bootstrap/`, `infra-ai/`, `mcp-server/` 下 `MODULE.md`

生成命令: `python Simlect-agent/tools/generate_module_docs.py`
