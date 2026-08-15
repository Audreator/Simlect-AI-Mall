package com.simlect.api.fallback;

import com.simlect.api.SearchToolFeignClient;
import com.simlect.api.support.FeignFallbackResponses;
import com.simlect.entity.vo.ResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 这个类干什么？
 * 当 Feign 调不到「simlect-search（搜索）」时（超时、宕机、熔断、网络错误），OpenFeign 会回调本工厂的 create()。
 * create() 返回一个「假的 SearchToolFeignClient」：方法里不再发 HTTP，直接返回「搜索服务暂不可用」的 ResponseVO。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：管理端「重建商品索引 / 同步 RAG 向量」：admin 不直连 search 库，远程调搜索内部接口
 * - 角色：远程调用失败时的兜底。正常调用不会进这个类；它不实现真实业务，也不假装成功。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. admin → SearchToolFeignSupport（后台点同步）
 *    → 作用：业务只关心远程调用成功还是失败
 * 2. SearchToolFeignSupport → SearchToolFeignClient.xxx()
 *    → 作用：经 Gateway + X-Internal-Token 访问 /internal/search/tool/*
 * 3a. 成功：进入目标微服务
 *    → 作用：search 真正重建 ES/向量
 * 3b. 失败：本类 create(cause) → 假 Client
 *    → 作用：返回统一「不可用」，并记日志（cause）
 * 4. Support 拆包 → 抛业务异常 / 上层失败
 *    → 作用：管理端提示不可用，不会误以为同步成功
 * </pre>
 * 为什么要降级工厂？
 * 避免超时异常打爆线程或返回 null 导致 NPE；把「下游挂了」变成明确失败（下单场景必须回滚）。
 */
@Component
public class SearchToolFeignFallbackFactory implements FallbackFactory<SearchToolFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(SearchToolFeignFallbackFactory.class);

    /**
     * Feign 调用失败时由框架回调。
     * @param cause 下游失败根因，必须打日志
     * @return 假客户端，各方法返回「服务暂不可用」
     */
    @Override
    public SearchToolFeignClient create(Throwable cause) {
        return new SearchToolFeignClient() {
                @Override
                public ResponseVO<Void> productData() {
                    log.error("SearchToolFeign productData fallback", cause);
                    return FeignFallbackResponses.unavailable(log, "搜索服务", cause);
                }

                @Override
                public ResponseVO<Void> ragData() {
                    log.error("SearchToolFeign ragData fallback", cause);
                    return FeignFallbackResponses.unavailable(log, "搜索服务", cause);
                }
            };
    }
}
