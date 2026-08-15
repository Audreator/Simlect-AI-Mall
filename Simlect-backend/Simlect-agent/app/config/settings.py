from functools import lru_cache

from pydantic import AliasChoices, Field

from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):

    model_config = SettingsConfigDict(

        env_file=".env",

        env_file_encoding="utf-8",

        extra="ignore",
        populate_by_name=True,
    )

    app_host: str = "0.0.0.0"
    app_port: int = 7050
    java_web_url: str = "http://localhost:8080"
    mcp_server_url: str = Field(
        default="http://127.0.0.1:7060",
        validation_alias=AliasChoices("MCP_SERVER_URL", "mcp_server_url"),
    )

    internal_token: str = Field(
        default="",
        validation_alias=AliasChoices(
            "SIMLECT_INTERNAL_TOKEN",
            "INTERNAL_TOKEN",
            "internal_token",
        ),
    )

    llm_api_key: str = ""
    llm_base_url: str = "https://api.deepseek.com"
    llm_model: str = "deepseek-chat"
    llm_fallback_model: str = "deepseek-chat"
    llm_timeout: int = 60
    llm_max_retries: int = 3

    memory_llm_api_key: str = ""
    memory_llm_base_url: str = ""
    memory_llm_model: str = ""

    memory_llm_timeout: int | None = None

    embedding_api_key: str = ""
    embedding_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    embedding_model: str = "text-embedding-v4"
    embedding_dimensions: int = 1024

    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = "root"
    mysql_password: str = "123456"
    mysql_database: str = "simlect_agent"

    redis_host: str = "127.0.0.1"
    redis_port: int = 6379
    redis_db: int = 0

    es_hosts: str = "http://localhost:9200"
    es_index: str = "simlect_vectorstore"
    es_vector_dimensions: int = 1024

    rabbitmq_url: str = "amqp://guest:guest@localhost:5672/"

    ai_chat_limit: int = 200
    rag_top_k: int = 15
    rag_score_threshold: float = 0.5
    history_message_limit: int = 15
    task_queue_max: int = 300

    session_redis_ttl: int = 86400
    session_compress_lock_ttl: int = 60

    working_token_budget: int = 100_000
    compress_token_threshold: int = 100_000
    assistant_history_max_len: int = 500

    circuit_llm_failure_threshold: int = 5
    circuit_llm_recovery_timeout: int = 60

    graph_max_react_rounds: int = 5
    graph_checkpoint_ttl: int = 3600
    graph_checkpoint_prefix: str = "mall:agent:graph:ckpt"
    # checkpoint HMAC 签名密钥（防 Redis 被注入恶意 pickle 载荷 → RCE）：
    # 留空则进程启动时随机生成（重启后旧断点失效，仅影响断点续跑，不影响会话）
    graph_checkpoint_hmac_secret: str = ""

    intent_use_llm: bool = True
    intent_rule_fallback: bool = True
    order_query_lookback_days: int = 90
    force_mcp_on_llm_skip: bool = False

    @property
    def mysql_dsn(self) -> str:

        return (
            f"mysql+aiomysql://{self.mysql_user}:{self.mysql_password}"
            f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_database}"
        )

    @property
    def redis_url(self) -> str:

        return f"redis://{self.redis_host}:{self.redis_port}/{self.redis_db}"

@lru_cache
def get_settings() -> Settings:

    settings = Settings()
    # 启动 FailFast：内部令牌未配置或仍为默认弱值时拒绝启动（防 /internal 裸奔与弱令牌直调）
    if not settings.internal_token or settings.internal_token in ("your-token", "changeme"):
        raise RuntimeError(
            "SIMLECT_INTERNAL_TOKEN 未配置或为弱值：请设置强内部令牌后再启动 Agent"
        )
    return settings
