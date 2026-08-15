package com.simlect.cloud.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
/**
 * 【功能】Gateway 侧 Sentinel 限流规则与阻塞响应。
 * <p>
 * 【角色】南北向过载保护：登录等 auth-sensitive 更严；web/admin 与按 routeId 限 QPS，超限 429。
 * <p>
 * 【调用链】启动 init → 注册 API 分组与 GatewayRule → 被限流时 BlockHandler 返回 JSON。
 * 与业务侧 CouponRushRateLimit（Redis Lua）互补。
 */
public class SentinelGatewayConfig {

    private final GatewayRateLimitProperties rateLimitProperties;

    public SentinelGatewayConfig(GatewayRateLimitProperties rateLimitProperties) {
        this.rateLimitProperties = rateLimitProperties;
    }

    @PostConstruct
    public void init() {
        initBlockHandler();
        if (!rateLimitProperties.isEnabled()) {
            return;
        }
        initCustomApis();
        initGatewayRules();
    }

    private void initCustomApis() {
        Set<ApiDefinition> definitions = new HashSet<>();

        Set<ApiPredicateItem> authItems = new HashSet<>();
        authItems.add(exact("/api/account/login"));
        authItems.add(exact("/api/account/register"));
        authItems.add(exact("/api/account/getEmailCode"));
        authItems.add(exact("/api/account/forgetPassword"));
        authItems.add(prefix("/admin-api/account/"));
        definitions.add(new ApiDefinition("auth-sensitive").setPredicateItems(authItems));

        Set<ApiPredicateItem> webItems = new HashSet<>();
        webItems.add(prefix("/api/"));
        definitions.add(new ApiDefinition("web-api").setPredicateItems(webItems));

        Set<ApiPredicateItem> adminItems = new HashSet<>();
        adminItems.add(prefix("/admin-api/"));
        definitions.add(new ApiDefinition("admin-api").setPredicateItems(adminItems));

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        double defaultQps = rateLimitProperties.getDefaultQps();
        double authQps = rateLimitProperties.getAuthQps();

        rules.add(apiRule("auth-sensitive", authQps));
        rules.add(apiRule("web-api", defaultQps));
        rules.add(apiRule("admin-api", defaultQps));

        for (String routeId : new String[]{
                "user", "product", "cart", "order", "pay", "coupon",
                "search", "stock", "admin", "agent-http"
        }) {
            rules.add(new GatewayFlowRule(routeId).setCount(defaultQps).setIntervalSec(1));
        }
        GatewayRuleManager.loadRules(rules);
    }

    private void initBlockHandler() {
        BlockRequestHandler blockHandler = (ServerWebExchange exchange, Throwable ex) -> {
            Map<String, Object> body = new HashMap<>();
            body.put("status", "error");
            body.put("code", 429);
            body.put("info", "请求过于频繁，请稍后再试");
            body.put("data", null);
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body);
        };
        GatewayCallbackManager.setBlockHandler(blockHandler);
    }

    private static GatewayFlowRule apiRule(String apiName, double qps) {
        return new GatewayFlowRule(apiName)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(qps)
                .setIntervalSec(1);
    }

    private static ApiPathPredicateItem exact(String path) {
        return new ApiPathPredicateItem()
                .setPattern(path)
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT);
    }

    private static ApiPathPredicateItem prefix(String path) {
        return new ApiPathPredicateItem()
                .setPattern(path)
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX);
    }
}
