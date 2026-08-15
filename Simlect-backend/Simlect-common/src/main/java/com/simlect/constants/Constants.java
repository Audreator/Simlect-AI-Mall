package com.simlect.constants;

public class Constants {

    //正则
    public static final String REGEX_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,18}$";
    public static final String ZERO_STR = "0";
    public static final Integer LENGTH_5 = 5;
    public static final Integer LENGTH_10 = 10;
    public static final Integer LENGTH_15 = 15;
    public static final Integer LENGTH_30 = 30;

    public static final String WS_MESSAGE_TOPIC = "message.topic";

    public static final String WS_MESSAGE_TOPIC_AGENT = "message.topic.agent";

    public static final String WS_MESSAGE_TYPE_NOTIFY = "notify";

    public static final String FILE_FOLDER_FILE = "file/";

    public static final String MODERATION_PENDING_PREFIX = "moderation/pending/";

    public static final long MAX_IMAGE_UPLOAD_BYTES = 5L * 1024 * 1024;

    public static final String TOKEN_WEB = "token";

    public static final String CONTINUOUS_DAYS = "continuousDays";

    public static final String USED_COUNT = "usedCount";

    public static final String TOTAL_SIGN_DAYS = "totalSignDays";

    public static final Long REDIS_KEY_EXPIRES_ONE_MIN = 60L;

    public static final Long REDIS_KEY_EXPIRES_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;

    public static final Integer NOT_ON_SALE = 0;
    public static final Integer ON_SALE = 1;
    public static final Integer DELETED = -1;

    private static final String REDIS_KEY_PREFIX = "mall:";

    public static final String TOKEN_ADMIN = "adminToken";

    public static final String REDIS_KEY_RUSHING_STOCK = REDIS_KEY_PREFIX + "rushing:stock:";

    public static final int RUSHING_STOCK_UNLIMITED = -1;

    public static final String REDIS_KEY_RUSHING_STOCK_SYNCING = REDIS_KEY_PREFIX + "rushing:stock:syncing:";

    public static final String REDIS_KEY_RUSHING_STOCK_DEPLETED = REDIS_KEY_PREFIX + "rushing:stock:depleted:";

    public static final long RUSHING_STOCK_SYNC_LOCK_SECONDS = 30L;

    public static final String REDIS_KEY_RUSHING_COUPON = REDIS_KEY_PREFIX + "rushing:coupon:";

    public static final String REDIS_KEY_RUSHING_USERID = REDIS_KEY_PREFIX + "rushing:userId:";

    // 邮件验证码
    public static final String REDIS_KEY_EMAIL_CODE = REDIS_KEY_PREFIX + "email:code:";

    // 接口限流
    public static final String REDIS_RATE_LIMIT = REDIS_KEY_PREFIX + "rate:limit:";

    public static final String REDIS_BAIDU_ACCESS_TOKEN = REDIS_KEY_PREFIX + "baidu:access_token";

    public static final String REDIS_USER_TEMP_BAN = REDIS_KEY_PREFIX + "user:temp_ban:";

    public static final String REDIS_KEY_RUSH_RATE_USER = REDIS_KEY_PREFIX + "rush:rate:user:";

    public static final String REDIS_KEY_RUSH_RATE_COUPON = REDIS_KEY_PREFIX + "rush:rate:coupon:";

    public static final int RUSH_RATE_USER_MAX_PER_MINUTE = 30;

    public static final long RUSH_RATE_USER_WINDOW_SECONDS = 60L;

    public static final int RUSH_RATE_COUPON_MAX_PER_SECOND = 200;

    public static final long RUSH_RATE_COUPON_WINDOW_SECONDS = 1L;

    public static final String REDIS_KEY_BROWSE_RECENT = REDIS_KEY_PREFIX + "browse:recent:";

    public static final int BROWSE_REDIS_MAX_SIZE = 200;

    public static final String REDIS_KEY_PRODUCT_BLOOM = REDIS_KEY_PREFIX + "product:bloom";

    public static final long PRODUCT_BLOOM_EXPECTED_INSERTIONS = 500_000L;

    public static final double PRODUCT_BLOOM_FALSE_PROBABILITY = 0.001;

    public static final String RUSHING_COUPON_PAY_AMOUNT = "0.01";

    public static final String MIN_ORDER_PAY_AMOUNT = "0.01";

    public static final String COUPON_RUSH_ORDER_SUBJECT_PREFIX = "优惠券：";

    public static final String REDIS_KEY_PAY_TRADE_INITIATED = REDIS_KEY_PREFIX + "pay:trade:initiated:";

    public static final String REDIS_KEY_TOKEN_ADMIN = REDIS_KEY_PREFIX + "token:admin:";

    public static final String REDIS_KEY_TOKEN_ADMIN_ACCOUNT = REDIS_KEY_PREFIX + "token:admin:account:";

    /** 管理端敏感操作一次性确认票据：mall:admin:confirm:{token} */
    public static final String REDIS_KEY_ADMIN_CONFIRM = REDIS_KEY_PREFIX + "admin:confirm:";

    /** 管理端敏感操作确认票据有效期（秒） */
    public static final int ADMIN_CONFIRM_TOKEN_TTL_SECONDS = 300;

    public static final String REDIS_KEY_ADMIN_LOGIN_FAIL = REDIS_KEY_PREFIX + "admin:login:fail:";

    public static final String REDIS_KEY_ADMIN_LOGIN_LOCK = REDIS_KEY_PREFIX + "admin:login:lock:";

    public static final String REDIS_KEY_CHECK_CODE = REDIS_KEY_PREFIX + "checkcode:";

    public static final String REDIS_KEY_TOKEN_WEB = REDIS_KEY_PREFIX + "token:web:";

    public static final String REDIS_KEY_TOKEN_USERID_WEB = REDIS_KEY_PREFIX + "token:web:userId:";

    public static final String REDIS_KEY_SIGN = REDIS_KEY_PREFIX + "sign:userId:";

    //SETBIT sign:202605:uid:1001 0 1   # 5月1日签到
    public static final String REDIS_KEY_SIGN_MONTH = REDIS_KEY_PREFIX + "sign:month:";
    public static final String REDIS_KEY_SIGN_USERID = "userId:";

    public static final String REDIS_KEY_SIGN_NULL = REDIS_KEY_PREFIX + "null:sign:";

    public static final String REDIS_KEY_SIGN_REBUILD_LOCK = REDIS_KEY_PREFIX + "lock:rebuild:sign:";

    public static final String REDIS_SIGN_NULL_PLACEHOLDER = "@SIGN_NULL@";

    public static final long SIGN_NULL_CACHE_TTL_SECONDS = 5 * 60L;

    public static final long SIGN_REBUILD_LOCK_SECONDS = 5L;

    public static final int SIGN_CALENDAR_REBUILD_DAYS = 365;

    public static final String REDIS_KEY_SIGN_DAILY_INIT_LOCK = REDIS_KEY_PREFIX + "lock:sign:daily:init";

    public static final String REDIS_KEY_SIGN_RECONCILE_LOCK = REDIS_KEY_PREFIX + "lock:sign:reconcile:hourly";

    public static final String REDIS_KEY_CATEGORY_LIST = REDIS_KEY_PREFIX + "category:list:";

    public static final String REDIS_KEY_COUPON_PLAZA_CACHE_VERSION = REDIS_KEY_PREFIX + "coupon:plaza:version";

    public static final String REDIS_KEY_COUPON_PLAZA_LIST = REDIS_KEY_PREFIX + "coupon:plaza:list:";

    public static final String REDIS_KEY_COUPON_DETAIL = REDIS_KEY_PREFIX + "coupon:detail:";

    public static final String REDIS_KEY_COUPON_REBUILD_LOCK = REDIS_KEY_PREFIX + "coupon:rebuild:lock:";

    public static final String REDIS_COUPON_NULL_PLACEHOLDER = "@COUPON_NULL@";

    public static final long COUPON_CACHE_LOGICAL_TTL_SECONDS = 30 * 60L;

    public static final long COUPON_CACHE_PHYSICAL_TTL_SECONDS = 24 * 60 * 60L;

    public static final long COUPON_CACHE_NULL_TTL_SECONDS = 2 * 60L;

    public static final long COUPON_CACHE_REBUILD_LOCK_SECONDS = 10L;

    public static final String CART_PAY_NAME = "购物车支付-%d件商品";

    public static final String REDIS_KEY_SETTING_LOGISTICS = REDIS_KEY_PREFIX + "setting:logistics:";

    public static final String REDIS_KEY_SIGN_REWARD_CONFIG = REDIS_KEY_PREFIX + "sign:reward:config";

    public static final String REDIS_KEY_MEMBER_LEVEL_REWARD_CONFIG = REDIS_KEY_PREFIX + "member:level:reward:config";

    public static final String REDIS_KEY_USER_LOCATION = REDIS_KEY_PREFIX + "user:location:";

    public static final String REDIS_KEY_NOTIFY_DEDUP = REDIS_KEY_PREFIX + "notify:dedup:";

    public static final String REDIS_KEY_USER_UNREAD_COUNT = REDIS_KEY_PREFIX + "user:unread:count:";

    public static final String REDIS_KEY_USER_POPUP_NOTIFY = REDIS_KEY_PREFIX + "user:popup:notify:";

    public static final String REDIS_KEY_NOTIFY_PENDING_LIST = REDIS_KEY_PREFIX + "notify:pending:list";

    public static final String REDIS_KEY_PAY_NOTIFY_LOCK = REDIS_KEY_PREFIX + "pay:notify:lock:";

    public static final String REDIS_KEY_PAY_ORDER_CLOSE_DONE = REDIS_KEY_PREFIX + "pay:close:done:";

    public static final String REDIS_KEY_PAY_ORDER_LIFECYCLE_LOCK = REDIS_KEY_PREFIX + "pay:lifecycle:lock:";

    public static final String REDIS_KEY_PAY_LATE_REFUND_DONE = REDIS_KEY_PREFIX + "pay:late:refund:done:";

    /** 用户退款幂等：mall:pay:refund:done:{orderItemId} */
    public static final String REDIS_KEY_ORDER_REFUND_DONE = REDIS_KEY_PREFIX + "pay:refund:done:";

    /** 支付/关单互斥锁看门狗超时（毫秒）：业务执行多久续期多久，进程崩溃后此时间自动释放 */
    public static final long PAY_ORDER_LIFECYCLE_LOCK_WATCHDOG_MS = 30_000L;

    public static final long PAY_ORDER_LIFECYCLE_LOCK_WAIT_MS = 15_000L;

    //向量数据库死信队列（异常消息）
    public static final String REDIS_RAG_FAIL_RECORD = REDIS_KEY_PREFIX + "rag:fail:record:";

    public static final String REDIS_KEY_CANCEL_AGENT_MESSAGE = REDIS_KEY_PREFIX + "cancel:agent:message:userId:";

    //提示词
    public static final String REDIS_KEY_PROMPT = REDIS_KEY_PREFIX + "prompt:";

    public static final String REDIS_KEY_AGENT_CONSULT_PRODUCT = REDIS_KEY_PREFIX + "agent:consult:product:userId:";

    public static final String REDIS_KEY_AGENT_CONSULT_ACTIVE = REDIS_KEY_PREFIX + "agent:consult:active:";

    public static final String REDIS_KEY_AGENT_PENDING_ACTION = REDIS_KEY_PREFIX + "agent:pending:action:";

    public static final String REDIS_KEY_MQ_SEND_IDEMPOTENT = REDIS_KEY_PREFIX + "mq:send:idempotent:";

    public static final String REDIS_KEY_MQ_CONSUME_IDEMPOTENT = REDIS_KEY_PREFIX + "mq:consume:done:";

    public static final String REDIS_KEY_MQ_COMPENSATE = REDIS_KEY_PREFIX + "mq:compensate:";

    public static final String REDIS_KEY_MQ_COMPENSATE_PENDING = REDIS_KEY_PREFIX + "mq:compensate:pending";

    public static final String REDIS_KEY_MQ_CONSUME_RETRY = REDIS_KEY_PREFIX + "mq:consume:retry:";

    public static final String REDIS_KEY_MQ_COMPENSATE_AUTO_REPLAY_LOCK = REDIS_KEY_PREFIX + "mq:compensate:auto:replay:lock";

    public static final String MQ_CONSUME_FAILURE_EXCHANGE = "mq.consume";

    public static final String REDIS_KEY_AGENT_PENDING_MSG_CTX = REDIS_KEY_PREFIX + "agent:pending:msg:userId:";

    public static final String REDIS_KEY_SENSITIVE_WORD_PAYLOAD = REDIS_KEY_PREFIX + "sensitive:word:payload";

    public static final String REDIS_KEY_SENSITIVE_WORD_VERSION = REDIS_KEY_PREFIX + "sensitive:word:version";

    public static final String REDIS_KEY_SENSITIVE_WORD_DB_SYNC_LOCK = REDIS_KEY_PREFIX + "sensitive:word:db:sync:lock";

    public static final String IMAGE_THUMBNAIL_SUFFIX = "_thumbnail";

    //最近订单天数
    public static final Integer LATEST_ORDER_DAYS = 15;

    public static final int ORDER_MAX_BUY_COUNT_PER_SKU = 99;

}
