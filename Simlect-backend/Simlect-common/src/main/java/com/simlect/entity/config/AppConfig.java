package com.simlect.entity.config;

import com.simlect.service.PasswordService;
import com.simlect.utils.StringTools;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("appConfig")
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    @Value("${amap.key:}")
    private String amapKey;

    @Value("${project.folder:./data/simlect/upload/}")
    private String projectFolder;

    @Value("${admin.account:admin}")
    private String adminAccount;

    @Value("${admin.password:}")
    private String adminPassword;

    /** 管理端弱口令黑名单：生产/显式开启安全校验时拒绝启动 */
    private static final java.util.Set<String> WEAK_ADMIN_PASSWORDS = java.util.Set.of(
            "admin123", "admin123456", "123456", "admin", "password");

    private String adminPasswordHash;

    @Resource
    private PasswordService passwordService;

    /** 生产就绪标记：true 时强制校验管理端密码（各业务服务共享 common，避免整栈因未配密码无法启动） */
    @Value("${simlect.production-ready:false}")
    private boolean productionReady;

    @PostConstruct
    public void initAdminPasswordHash() {
        boolean weakOrMissing = adminPassword == null || adminPassword.isEmpty()
                || WEAK_ADMIN_PASSWORDS.contains(adminPassword);
        if (weakOrMissing && productionReady) {
            throw new IllegalStateException(
                    "管理端密码未配置或为弱口令，请通过环境变量 ADMIN_PASSWORD 设置强密码后重启");
        }
        if (weakOrMissing) {
            // 非生产：仅告警（本地开发可缺省），生产就绪时必须配置
            logger.warn("管理端密码未配置或为弱口令（仅生产就绪模式强制）");
        }
        if (adminPassword == null || adminPassword.isEmpty()) {
            // 无密码时使用不可用哈希，避免空密码可登录
            adminPasswordHash = "$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinvalidinva";
        } else if (adminPassword.startsWith("$2")) {
            adminPasswordHash = adminPassword;
        } else {
            adminPasswordHash = passwordService.encode(adminPassword);
        }
    }

    @Value("${project.domain:}")
    private String projectDomain;

    @Value("${admin.emails:}")
    private String adminEmails;

    //支付宝应用私钥
    @Value("${alipay.appPrivateKey:}")
    private String alipayAppPrivateKey;

    @Value("${alipay.appid:}")
    private String alipayAppid;

    @Value("${alipay.appCertPath:}")
    private String alipayAppCertPath;

    @Value("${alipay.alipayPublicCertPath:}")
    private String alipayPublicCertPath;

    @Value("${alipay.alipayRootCertPath:}")
    private String alipayRootCertPath;

    @Value("${alipay.serverUrl:}")
    private String alipayServerUrl;

    //订单超时（默认 15 分钟，对齐原 EShop）
    @Value("${order.expire.minute:15}")
    private Integer orderExpireMinute;

    // 自动确认收货
    @Value("${order.confirm.minute:10080}")
    private Integer orderConfirmMinute;

    // 模拟物流每步间隔（秒，默认 1 小时/站）
    @Value("${logistics.simulate.interval-second:3600}")
    private Integer logisticsSimulateIntervalSecond;

    // 模拟物流最大站点数
    @Value("${logistics.simulate.max-stations:5}")
    private Integer logisticsSimulateMaxStations;

    // 秒杀券预占超时（秒）
    @Value("${rush.prepare.expire-second:900}")
    private Integer rushPrepareExpireSecond;

    // 用户位置缓存过期（天）
    @Value("${user.location.expire-day:7}")
    private Integer userLocationExpireDay;

    // 自动校验订单
    @Value("${project.auto-checkpay:false}")
    private Boolean autoCheckpay;

    //限制ai聊天轮数
    @Value("${project.ai-chat-limit:0}")
    private Integer aiChatLimit;

    @Value("${sign.streak.coupon-id:}")
    private String signStreakCouponId;

    @Value("${member.level2.coupon-id:}")
    private String memberLevel2CouponId;

    @Value("${member.level3.coupon-id:}")
    private String memberLevel3CouponId;

    @Value("${baidu.aip.api-key:}")
    private String baiduAipApiKey;

    @Value("${baidu.aip.secret-key:}")
    private String baiduAipSecretKey;

    @Value("${baidu.aip.enabled:false}")
    private Boolean baiduAipEnabled;

    @Value("${baidu.aip.strategy-id:#{null}}")
    private Integer baiduAipStrategyId;

    public String getProjectFolder() {
        if (!StringTools.isEmpty(projectFolder) && !projectFolder.endsWith("/")) {
            projectFolder = projectFolder + "/";
        }
        return projectFolder;
    }

    public String getAdminEmails() {
        return adminEmails;
    }

    public Integer getOrderExpireMinute() {
        return orderExpireMinute;
    }

    public String getProjectDomain() {
        return projectDomain;
    }

    public String getAlipayAppCertPath() {
        return alipayAppCertPath;
    }

    public String getAlipayPublicCertPath() {
        return alipayPublicCertPath;
    }

    public String getAlipayRootCertPath() {
        return alipayRootCertPath;
    }

    public Boolean getAutoCheckpay() {
        return autoCheckpay;
    }

    public String getAlipayAppPrivateKey() {
        return alipayAppPrivateKey;
    }

    public String getAlipayAppid() {
        return alipayAppid;
    }

    public String getAlipayServerUrl() {
        return alipayServerUrl;
    }

    public Integer getOrderConfirmMinute() {
        return orderConfirmMinute;
    }

    public Integer getLogisticsSimulateIntervalSecond() {
        return logisticsSimulateIntervalSecond;
    }

    public Integer getLogisticsSimulateMaxStations() {
        return logisticsSimulateMaxStations;
    }

    public Integer getRushPrepareExpireSecond() {
        return rushPrepareExpireSecond;
    }

    public Integer getUserLocationExpireDay() {
        return userLocationExpireDay;
    }

    public String getAdminAccount() {
        return adminAccount;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public String getAdminPasswordHash() {
        return adminPasswordHash;
    }

    public Integer getAiChatLimit() {
        return aiChatLimit;
    }

    public String getSignStreakCouponId() {
        return signStreakCouponId;
    }

    public String getMemberLevel2CouponId() {
        return memberLevel2CouponId;
    }

    public String getMemberLevel3CouponId() {
        return memberLevel3CouponId;
    }

    public String getAmapKey() {
        return amapKey;
    }

    public String getBaiduAipApiKey() {
        return baiduAipApiKey;
    }

    public String getBaiduAipSecretKey() {
        return baiduAipSecretKey;
    }

    public Boolean getBaiduAipEnabled() {
        return baiduAipEnabled;
    }

    public Integer getBaiduAipStrategyId() {
        return baiduAipStrategyId;
    }
}
