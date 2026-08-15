package com.simlect.cloud.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GatewayProductionSafetyValidator {

    private static final Logger log = LoggerFactory.getLogger(GatewayProductionSafetyValidator.class);
    private static final String WEAK_INTERNAL = "your-token";

    @Value("${simlect.production-ready:false}")
    private boolean productionReady;

    @Value("${simlect.internal.token:}")
    private String internalToken;

    @PostConstruct
    public void validate() {
        // 内部令牌未配置或仍为默认弱值：生产就绪时拒绝启动；非生产就绪仅告警
        if (!StringUtils.hasText(internalToken) || WEAK_INTERNAL.equals(internalToken)) {
            if (productionReady) {
                throw new IllegalStateException("生产就绪校验失败: 请设置强 SIMLECT_INTERNAL_TOKEN");
            }
            log.warn("simlect.internal.token 未配置（内部接口将全部 401），上线前请设置强 SIMLECT_INTERNAL_TOKEN");
            return;
        }
        if (!productionReady) {
            log.warn("simlect.production-ready=false（Gateway）：上线前请设为 true");
        }
        log.info("Gateway 生产就绪校验通过");
    }
}
