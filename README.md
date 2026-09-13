# Simlect

**体验地址：[www.simlect.com](https://www.simlect.com)**

B2C 单商家电商在线平台。面向“只能手动搜商品”的传统体验，把**对话导购、混合搜索与可执行智能客服**融入电商全流程：浏览、咨询、下单、售后均可在对话里完成读操作，写操作经“提案 -> 用户确认 -> Java 执行”闭环，模型不直改业务库。

Java 侧按领域拆成商品、订单、支付、营销（优惠券）、库存等微服务，基于 Spring Cloud 协作；智能客服为 **Python Agent 独立进程**，经 Gateway 统一对外。

---

## 目录

- [项目亮点](#项目亮点)
- [技术栈与版本](#技术栈与版本)
- [架构](#架构)
- [可扩展方向](#可扩展方向)
- [仓库结构](#仓库结构)
- [设计片段](#设计片段)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [服务端口](#服务端口)
- [文档索引](#文档索引)
- [License](#license)

---

## 项目亮点


| 能力            | 说明                                                                                              |
| ------------- | ----------------------------------------------------------------------------------------------- |
| **AI + 传统电商** | LangGraph 编排 LLM + 10 个业务工具（6 读 / 4 写提案）；**自主规划**优先，少强制调工具；流式 WebSocket（复用站内通知）；商品/订单卡片由前端结构化渲染 |
| **交易与库存**     | SKU：MySQL 行锁预检 + 条件更新防超卖；批量扣减/回补以操作号幂等，超时未支付经 RabbitMQ TTL/DLX 关单并回补。秒杀券：Redis Lua 原子预扣 + DB 兜底 |
| **签到与异步**     | MySQL 月位图为签到权威数据源，Redis 只做可重建缓存；签到/补签、跨月连续天数、成长值与连续签到发券均具备事务与幂等保障；通知死信队列**有限退避重投 + 超限丢弃审计**，消息不静默丢失 |
| **搜索与 RAG**   | ES 关键词 + 向量 RRF；**标题相关性过滤**；未命中时热销/足迹兜底并明确告知「未搜到 + 另荐」；FAQ 向量检索；商品变更 MQ 异步向量化                   |
| **支付与订单**     | 支付宝异步回调；延时关单 / 自动收货；订单状态机集中约束合法流转，并以条件更新处理支付、取消、关单等并发；支持显式开启的本地支付联调模式 |
| **微服务边界**     | 一域一库；跨库禁止直连 Mapper，统一 OpenFeign `/internal/`** + MQ；Gateway 鉴权与限流                               |
| **可靠性与安全**   | 退款单号 Redis 锚点与支付幂等键对齐（防二次退款）；pending 提案 confirm 经 **Lua CAS 原子落定状态**消除写操作重复执行；MCP Server 强制 `X-Internal-Token` 令牌（`/health` 探活白名单）；WS Origin 精确白名单；管理端敏感操作一次性票据绑定路径 |


---

## 技术栈与版本

### 后端（Java）


| 组件                   | 版本                           |
| -------------------- | ---------------------------- |
| JDK                  | 17                           |
| Spring Boot          | 3.5.4                        |
| Spring Cloud         | 2025.0.0                     |
| Spring Cloud Alibaba | 2025.0.0.0（Nacos / Sentinel） |
| MyBatis Spring Boot  | 3.0.5                        |
| MySQL Connector/J    | 8.3.0                        |
| Redis / Redisson     | Redis 7 · Redisson 4.0.0     |
| RabbitMQ             | 3.13（客户端随 Spring AMQP）       |
| Elasticsearch        | 9.2.1（本地镜像含 IK）              |
| 支付宝 SDK              | 4.40.576.ALL                 |


### 智能客服（Python）


| 组件                            | 版本约束（见 `requirements-runtime.txt`） |
| ----------------------------- | ---------------------------------- |
| FastAPI                       | ≥0.115                             |
| Uvicorn                       | ≥0.32                              |
| LangChain / LangGraph         | ≥0.3 / ≥0.2                        |
| MCP                           | ≥1.9（Streamable HTTP）              |
| Redis / Elasticsearch / httpx | 异步客户端                              |


### 前端


| 组件                                | 说明                                    |
| --------------------------------- | ------------------------------------- |
| Vue 3 + Vite + TypeScript         | C 端 `Simlect-web`、管理端 `Simlect-admin` |
| Element Plus / Pinia / Vue Router | UI 与状态                                |


### 中间件（本地 Docker 默认）

MySQL 8.3 · Redis 7 · RabbitMQ 3.13 · Nacos 2.4.3 · Elasticsearch 9.2.1-IK · Sentinel · Seata AT（默认开启，本地可用 `SEATA_ENABLED=false` 关闭）

---

## 架构

```text
                 ┌───────────────────┐
                 │浏览器 C 端 / 管理端│ www.simlect.com
                 └────────┬──────────┘
                          │ Nginx
                          ▼
                 ┌─────────────────┐
                 │  Gateway :8080  │  /api/**  /admin-api/**  /internal/**  /ws/**
                 └────────┬────────┘
          ┌───────────────┼────────────────┐
          ▼               ▼                ▼
   Java 微服务集群    Python Agent     （静态前端）
   Nacos 注册发现       :7050
   Feign + Sentinel   LangGraph
          │               │
          │               │ MCP Client（Streamable HTTP）
          │               ▼
          │         MCP Server :7060/mcp
          │         （10 个业务工具）
          │               │
          └───────┬───────┘
                  ▼
     MySQL(分库) · Redis · RabbitMQ · Elasticsearch
```

Agent 不直连业务库改写；读/写工具由 **MCP Server** 实现，经 Gateway `/internal/**` 调 Java。MCP HTTP 端点强制 `X-Internal-Token` 鉴权（与 Agent `.env` 的 `SIMLECT_INTERNAL_TOKEN` 一致，`GET /health` 探活免令牌）。写操作仍走「提案 → 用户确认 → Java 执行」，确认经 Lua CAS 原子落定状态，杜绝重复执行。

**微服务模块：** `gateway` · `user` · `product` · `stock` · `cart` · `order` · `pay` · `coupon` · `search` · `admin` · `agent(Python)` · `mcp-server(Python :7060)`

`Simlect-common` 仅保留跨域基建（鉴权、Feign 基建、Outbox/补偿、通用 `ResponseVO` 等）；领域 DTO/VO/枚举已迁入各服务 `*-api`。

### 命名约定


| 项               | 值                                                              |
| --------------- | -------------------------------------------------------------- |
| Maven `groupId` | `com.simlect`                                                  |
| Java 根包         | `com.simlect.`*（业务 `biz`、Feign `api`、基建在 common）               |
| 商品 ES 索引        | `simlect-index`                                                |
| 向量 / RAG 索引     | `simlect_vectorstore`（可用 `VECTOR_INDEX` / Agent `ES_INDEX` 覆盖） |


> 升级提示：若本地仍残留历史索引名 `myshop-index` / `myshop_vectorstore`，请重建为上述新名，或临时用环境变量指向旧名后再迁移数据。

分库表归属见 [sql/TABLE_OWNERSHIP.md](sql/TABLE_OWNERSHIP.md)。

---

## 可扩展方向

项目按领域拆分，以下能力可按需加长，而不必推倒重来：


| 方向                  | 现状                                                  | 扩展方式                                                                                                                                 |
| ------------------- | --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| **支付渠道**            | 已实现支付宝 PC / WAP（`PayChannel` + `PayChannelEnum`）    | 新增实现类（如微信支付），在枚举中注册 `beanName`，复用统一下单 / 回调 / 关单与订单状态机                                                                                |
| **Agent 工具（MCP）**   | 10 个工具经 **Streamable HTTP MCP Server**（`:7060/mcp`） | 实现：`python -m app.mcp_server` / `start-mcp.bat`；Agent 为 MCP Client；**改工具逻辑后须重启 MCP**（无热重载）；写操作仍走提案 -> confirm -> Gateway `/internal` |
| **LLM / Embedding** | DeepSeek 对话 + 通义兼容 Embedding（可换）                    | 改 `.env` 中 `LLM_`* / `EMBEDDING_`* 即可换厂商                                                                                             |
| **搜索与 RAG**         | ES 关键词 + 向量 + RRF + 相关性过滤；FAQ / 商品异步入库              | 增索引字段、改 RRF 权重、扩展品类同义词（`product_search_query.py`）；商品变更已走 MQ 向量化                                                                      |
| **营销**              | 优惠券 / 秒杀券（`simlect-coupon`）                         | 可加满减活动、会员价、分销等，保持「券库存 Lua + DB」或独立活动服务                                                                                               |
| **通知与触达**           | MQ 削峰落库 + 站内信                                       | 可接短信 / 邮件 / 企微，复用现有通知 Outbox 与消费者模式                                                                                                  |
| **前端能力**            | C 端 + 管理端                                           | 新域经 Gateway `/api`、`/admin-api` 扩展即可                                                                                                 |


写工具务必保持：**模型只提案，真正改库由 Java 执行**，避免 LLM 幻觉写库。

---

## 界面预览

**PC 端**

| C 端首页 | 管理后台 | AI 智能客服 |
|---|---|---|
| ![C 端首页](docs/screenshots/web-home.png) | ![管理后台](docs/screenshots/admin-home.png) | ![AI 智能客服](docs/screenshots/ai-assistant.png) |

**手机端**

| C 端首页 | AI 智能客服 | 管理后台（手机版） |
|---|---|---|
| ![手机端首页](docs/screenshots/mobile-home.png) | ![手机端 AI 客服](docs/screenshots/mobile-ai.png) | ![手机版管理后台](docs/screenshots/mobile-admin.png) |

---

## 测试与质量保障

**测试全覆盖**：全量回归在每次变更后执行，当前状态全部通过。

| 范围 | 覆盖内容 | 规模 |
|---|---|---|
| Java 单元/集成测试 | 26 个 Maven 模块：`mvn test` 全绿（含 Mockito 单测、控制器/服务/组件/任务/工具类全覆盖，安全敏感项均有专项用例，如退款幂等锚点、MCP 令牌、网关 IP/Origin、Redis Lua、MQ 幂等/补偿/死信） | 162 个测试类 · **1341 个用例** |
| Python 测试 | Agent 79 个源文件语法校验 0 错误；集成用例覆盖：pending 提案 confirm 五场景（CAS 落定/delete 失败/业务回滚/系统异常保持/CAS 拒绝）、ws_token 废弃分支、checkpoint 签名与脏 key 清理、MCP 中间件令牌矩阵 | 多组场景用例 |
| 前端构建校验 | 311 个 Vue/TS/JS 文件全部通过 esbuild 与 `@vue/compiler-sfc` 编译（含 style 块 SCSS） | 311 文件 0 失败 |
| 真实环境冒烟 | 全中间件 + 11 个微服务 + Agent + 双端前端联调：管理端登录跳转、C 端登录、AI 客服对话（商品搜索/物流查询/订单查询/退款与评价提案卡）、支付与订单状态机 | 全链路 |

> 关键链路均有配套回归：退款"远端成功+本地回滚"重试、pending 写操作防重复执行（Lua CAS）、MCP 带/不带令牌、通知死信有限重投、签到并发、秒杀券广播大列表。

---

## 仓库结构

```text
Simlect/
├── Simlect-backend/          # Java 微服务 + Python Agent
│   ├── Simlect-common/       # 跨域基建（鉴权 / Feign / Outbox / 补偿）
│   ├── Simlect-gateway/
│   ├── Simlect-{user,product,stock,cart,order,pay,coupon,search,admin}/
│   └── Simlect-agent/        # FastAPI + LangGraph + MCP Server（:7050 / :7060）
├── Simlect-front/
│   ├── Simlect-web/          # C 端
│   └── Simlect-admin/        # 管理端
├── sql/                      # 分库 DDL、Nacos/Seata
└── deploy/                   # Docker 中间件、环境变量、Nginx、上线清单
```

---

## 设计片段

### 智能客服：自主规划 + 读直连 / 写提案

Agent 通过 **MCP Streamable HTTP**（默认 `:7060/mcp`）调用 10 个工具；工具实现经 Gateway `/internal/`** 调 Java。

- **读类**（搜商品、查订单/物流/评价/优惠券、商品详情）直接返回结构化数据；前端渲染商品卡 / 订单卡。
- **写类**（确认收货、退款、评价、追评）只写入 Redis **待确认提案**（`【act_<32位hex>】`），用户点确认后由 Java 真正改库；确认执行前经 **Lua CAS 把提案状态原子落为 CONFIRMED**（业务失败才回滚）——即便删除/崩溃，重试也会被状态检查拒绝，写操作不会二次执行；拦截伪造 token、确认/取消用 Redis NX 防双击。
- **自主性**：默认不靠关键词强行塞工具（`FORCE_MCP_ON_LLM_SKIP=false`）；<如何取消订单 / 优惠券怎么用>等 how to 走说明，不强行查单/查券。UI 明确要<我的订单>时仍会拉订单卡。

### 订单状态机与超时关单

订单状态流转统一由状态机校验，控制器、支付回调、物流、超时任务不再各自硬编码目标状态；数据库更新同时携带期望源状态，避免并发请求越级或重复流转。

下单事务提交后经 Outbox/MQ 投递 **支付超时延迟队列**（TTL → 死信）。消费者仅在状态机允许且订单仍为待支付时关单，并按订单行以操作号幂等回补库存（秒杀券走券库存释放 + Lua 对齐）。支付成功与关单并发时，由状态机、条件写与关单标记共同收敛；晚到支付走退款路径。

### 搜索：RRF + 相关性过滤 + 兜底文案

1. 口语归一化（如<我要吃零食>→<零食>）后做 **ES 关键词 + 向量 RRF**。
2. 对召回标题做 **品类/同义词相关性过滤**；全部不相关则视为未命中。
3. 未命中再回落足迹 / 热销，工具文案为"暂未找到…"+【另荐热销/浏览推荐】。

---

## 快速开始

> 建议本机内存 **≥16GB**（全中间件 + 全服务更舒适）。以下以 Windows / PowerShell 为例，Linux/macOS 命令等价。

### 0. 环境要求

- JDK 17、Maven 3.9+
- Docker Desktop（中间件）
- Node.js 20+（前端）
- Python 3.11+（Agent，建议 venv）
- LLM / Embedding API Key（Agent 与 RAG；可用 DeepSeek + 通义兼容接口）

### 1. 启动中间件

```powershell
cd deploy
powershell -ExecutionPolicy Bypass -File .\start-middleware.ps1
# 或：docker compose -f docker-compose.middleware.yml up -d
```

说明见 [deploy/MIDDLEWARE_DOCKER.md](deploy/MIDDLEWARE_DOCKER.md)。

### 2. 导入数据库

MySQL 就绪后，按序执行（可用 `docker exec -i simlect-mysql mysql -uroot -proot`）：

```text
sql/00_create_databases.sql          # 业务分库
sql/00b_nacos_seata_databases.sql    # nacos / seata 库
sql/14_nacos.sql
sql/15_seata.sql
sql/16_seata_undo_log.sql
sql/01_user.sql … sql/10_admin.sql   # 业务表（含 order outbox）
sql/13_mq_infra_per_service.sql      # 其他服务 Outbox / 补偿表
```

也可先跑：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\init-mysql-meta.ps1
```

再按 [deploy/GO_LIVE.md](deploy/GO_LIVE.md) 补全 `01`～`10`、`13`。

#### 存量库升级（签到权威切换）

已有环境切换到 MySQL 权威签到前，需要保留旧 Redis Hash 中的补签已消费总数。请安排签到停写/切流窗口，并确保脚本仍能访问承载旧数据的 Redis；以下顺序不可省略：

```powershell
# 1. 创建 MySQL 位图权威表、成长值及奖励幂等表
Get-Content -Raw .\sql\17_sign_bitmap_authority.sql | docker exec -i simlect-mysql mysql -uroot -proot simlect_user

# 2. 将旧 Redis usedCount 快照幂等导入 MySQL（密码按实际环境填写）
powershell -ExecutionPolicy Bypass -File .\sql\18_import_legacy_sign_used_count.ps1 -MySqlPassword 'root'

# 3. Redis 快照导入后的切流前核验；最后一个差异查询必须返回空集
Get-Content -Raw .\sql\18_verify_legacy_sign_used_count.sql | docker exec -i simlect-mysql mysql -uroot -proot

# 4. 创建库存批量变更操作幂等表
Get-Content -Raw .\sql\19_stock_change_operation.sql | docker exec -i simlect-mysql mysql -uroot -proot simlect_stock
```

确认核验差异为空后再启动新版 user 服务并恢复签到写入。全新环境的 `sql/01_user.sql`、`sql/03_stock.sql` 已包含对应表结构，无需重复执行 `17`～`19`。

### 3. 构建并启动 Java 服务

```powershell
cd Simlect-backend
mvn -q package -DskipTests
```

建议启动顺序：

1. **Gateway** `:8080`
2. `user` / `product` / `stock` / `cart` / `coupon` / `order` / `pay` / `search` / `admin`
3. 确认 [Nacos](http://127.0.0.1:8848/nacos) 实例全部 UP

本地默认连接：`127.0.0.1` 的 MySQL（`root`/`root`）、Redis、RabbitMQ（`simlect`/`simlect`）、Nacos。内部调用令牌默认 `your-token`（仅开发，与各服务 `simlect.internal.token` / Agent `.env` 保持一致）。

Agent 跨域 Java 只打 Gateway（`JAVA_WEB_URL`，含 `/internal/order|product|coupon|user/`**），勿再直连微服务端口。

#### Seata AT（默认开启）

- **参与服务**：`order` / `stock` / `cart` / `coupon` / `pay`（均引入 Seata starter）。
- **典型路径**：下单 `OrderInfoServiceImpl` 上 `@GlobalTransactional`；AT 模式下各库本地事务由 Seata 代理，全局失败则各分支回滚。
- **失败语义**：`SEATA_ENABLED=true`（默认）且 Seata Server 不可达时，带 `@GlobalTransactional` 的下单会失败，**不会**静默退化为仅本地事务。
- **本地逃生**：未起 Seata（默认 `deploy` 中间件可选）时设环境变量 `SEATA_ENABLED=false`，再启动上述服务。

### 4. 启动 Python Agent

先启动 MCP 工具进程（Streamable HTTP，默认 `:7060`）：

```powershell
cd Simlect-backend\Simlect-agent
.\start-mcp.bat
```

再启动 Agent HTTP/WebSocket（`:7050`）。Windows 可一键启动（自动创建 venv、按需装依赖、缺 `.env` 时从 `.env.example` 复制）：

```powershell
cd Simlect-backend\Simlect-agent
.\start.bat
```

首次请编辑 `.env`，填写 `LLM_API_KEY`、`EMBEDDING_API_KEY`、`MCP_SERVER_URL`（默认 `http://127.0.0.1:7060`）等。

也可手动：

```powershell
cd Simlect-backend\Simlect-agent
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements-runtime.txt
copy .env.example .env   # 填写 LLM_API_KEY、EMBEDDING_API_KEY 等
python -m app.mcp_server
# 另一终端：
uvicorn app.main:app --host 0.0.0.0 --port 7050
```

健康检查：`GET http://127.0.0.1:7050/health`；MCP 端点：`http://127.0.0.1:7060/mcp`（需 `X-Internal-Token` 请求头，见 `.env` 的 `SIMLECT_INTERNAL_TOKEN`），MCP 探活：`GET http://127.0.0.1:7060/health`（免令牌）。

> **重要：** MCP 进程**无热重载**。修改 `mcp_tools_service` / `product_service` 等工具实现后，必须重启 `python -m app.mcp_server`（或 `start-mcp.bat`），否则 Agent 仍可能拿到旧逻辑。Agent 侧对 `SEARCH_PRODUCTS` 会再跑一遍本地实现作兜底，但 MCP 进程仍应保持与代码同步。

密钥只放在本地 `.env`（已 gitignore），不要写进 IDEA Run Configuration / `workspace.xml`。

### 5. 启动前端

```powershell
# C 端
cd Simlect-front\Simlect-web
npm install
npm run dev

# 管理端
cd Simlect-front\Simlect-admin
npm install
npm run dev
```

开发环境 API 指向 Gateway `http://localhost:8080`（见各自 `.env.development`）。

### 6. 生产部署摘要

1. 复制 [deploy/env.production.example](deploy/env.production.example)，设置 `SIMLECT_PRODUCTION_READY=true`、强 `SIMLECT_INTERNAL_TOKEN` / 数据库与管理员密码。
2. Nginx 反代示例：[deploy/nginx.simlect.conf.example](deploy/nginx.simlect.conf.example)
3. 完整清单：[deploy/GO_LIVE.md](deploy/GO_LIVE.md)

---

## 配置说明


| 配置项                                | 用途                                                                             |
| ---------------------------------- | ------------------------------------------------------------------------------ |
| `NACOS_ADDR`                       | 服务注册发现，默认 `127.0.0.1:8848`                                                     |
| `MYSQL_*` / 各库 URL                 | 一服务一库（`simlect_user` 等）                                                        |
| `REDIS_*` / `RABBIT_*`             | 可重建缓存（含签到日历）、会话、MQ；签到权威位图保存在 MySQL                                      |
| `ES_URIS`                          | 商品检索与向量索引                                                                      |
| `VECTOR_INDEX` / `ES_INDEX`        | 向量索引名，默认 `simlect_vectorstore`；商品关键词索引固定为 `simlect-index`                      |
| `SIMLECT_INTERNAL_TOKEN`           | 服务间与 Agent 经 Gateway 调用 `/internal/**` 的共享密钥（请求头 `X-Internal-Token`），**全服务一致** |
| `ADMIN_ACCOUNT` / `ADMIN_PASSWORD` | 管理端账号                                                                          |
| `ALIPAY_*`                         | 支付宝证书与网关（开放支付时必填）                                                              |
| `SIMLECT_PAY_LOCAL_MOCK_ENABLED`   | 仅本地支付联调；默认 `false`，开启后不访问支付宝且不应在生产环境使用                                  |
| `LLM_*` / `EMBEDDING_*`            | Agent 对话与 RAG 向量化                                                              |
| `JAVA_WEB_URL` / `AGENT_HOST`      | Agent 只连 Gateway（含 `/internal/`**）；Gateway 反代 Agent                            |
| `MCP_SERVER_URL`                   | Agent 连接 MCP Streamable HTTP，默认 `http://127.0.0.1:7060`                        |
| `SEATA_ENABLED`                    | Seata AT 开关，默认 `true`；本地无 Seata Server 时设 `false`                              |
| `SIMLECT_DEV_LOGIN_BYPASS`         | 仅本地；**禁止生产开启**                                                                 |


Agent 环境变量模板：`Simlect-backend/Simlect-agent/.env.example`。

### Agent 常用参数（`.env`）


| 参数                      | 默认示例  | 含义                                                         |
| ----------------------- | ----- | ---------------------------------------------------------- |
| `AI_CHAT_LIMIT`         | `200` | 单用户累计可发送的对话轮次上限（护栏）；`<=0` 表示不限制。超限后拒绝继续聊天，防止刷 LLM。         |
| `RAG_TOP_K`             | `15`  | 向量 / FAQ 检索时最多取回的文档条数（Top-K）。越大召回越宽，延迟与噪声也可能增加。            |
| `RAG_SCORE_THRESHOLD`   | `0.5` | 向量相似度分数下限；低于该阈值的命中会被丢掉，减少”答不实“的弱相关片段。                      |
| `HISTORY_MESSAGE_LIMIT` | `15`  | 组装 LLM 上下文时参考的历史轮次数量相关上限（实现里会按此倍数从库中拉取再筛选）。越大上下文越长、费用越高。   |
| `TASK_QUEUE_MAX`        | `300` | Agent 进程内同时进行的对话任务上限；达到后新请求排队失败/拒绝，保护本机 LLM 与下游 Java 不被打满。 |


其余如 `CIRCUIT_LLM`_*（熔断）、`GRAPH_MAX_REACT_ROUNDS`（工具循环轮数）等见 `Simlect-backend/Simlect-agent/app/config/settings.py`。

---

## 服务端口


| 服务             | 端口   |
| -------------- | ---- |
| Gateway        | 8080 |
| Agent (Python) | 7050 |
| MCP Server     | 7060 |
| cart           | 8084 |
| coupon         | 8087 |
| order          | 8093 |
| pay            | 8096 |
| product        | 8099 |
| stock          | 8102 |
| user           | 8105 |
| search         | 8108 |
| admin          | 8111 |


中间件控制台（本地）：Nacos `8848` · RabbitMQ Management `15672` · ES `9200` · Sentinel `8858`

---

## 文档索引


| 文档                                                                                 | 内容               |
| ---------------------------------------------------------------------------------- | ---------------- |
| [deploy/GO_LIVE.md](deploy/GO_LIVE.md)                                             | 上线检查清单           |
| [deploy/MIDDLEWARE_DOCKER.md](deploy/MIDDLEWARE_DOCKER.md)                         | 中间件 Docker       |
| [deploy/env.production.example](deploy/env.production.example)                     | 生产环境变量模板         |
| [sql/TABLE_OWNERSHIP.md](sql/TABLE_OWNERSHIP.md)                                   | 分库表归属与包命名约定      |
| [Simlect-backend/Simlect-agent/.env.example](Simlect-backend/Simlect-agent/.env.example) | Agent 环境变量模板 |
| [deploy/FULL_STACK.md](deploy/FULL_STACK.md)                                       | 本机全栈启动与内存建议 |


---

## License

本项目仅供学习与演示。商用请自行评估第三方依赖协议（支付宝、地图、LLM、Embedding 等）及合规要求。

---

**在线体验：** [https://www.simlect.com](https://www.simlect.com)
