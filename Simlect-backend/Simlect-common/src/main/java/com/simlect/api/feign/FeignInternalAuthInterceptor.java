package com.simlect.api.feign;

import com.simlect.constants.InternalApiHeaders;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.util.StringUtils;

/**
 * 【功能】Feign 请求拦截器：自动给发往 /internal/** 的调用加上 X-Internal-Token。
 * <p>
 * 【所属功能】东西向服务鉴权（与 Gateway InternalTokenGlobalFilter 配对）。
 * <p>
 * 【角色】所有 *FeignClient 出站请求的统一鉴权注入点，避免每个 Client 手写 Header。
 * <p>
 * 【调用链】
 * <pre>
 * Service → *FeignSupport → *FeignClient
 *   → FeignInternalAuthInterceptor.apply 注入 X-Internal-Token
 *   → Gateway InternalTokenGlobalFilter 校验
 *   → 目标微服务 InternalController
 * </pre>
 * 作用：内部调用可认证；没有令牌会被 Gateway 直接 401。
 */
public class FeignInternalAuthInterceptor implements RequestInterceptor {

    private final String internalToken;

    public FeignInternalAuthInterceptor(String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (StringUtils.hasText(internalToken)) {
            template.header(InternalApiHeaders.INTERNAL_TOKEN, internalToken);
        }
    }
}
