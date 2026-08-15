package com.simlect.api.support;

import com.simlect.api.SearchToolFeignClient;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
/**
 * 这个类干什么？
 * 管理端同步搜索数据时用的门面：内部转调 SearchToolFeignClient，
 * 把 ResponseVO 拆包；失败（含降级「不可用」）统一转成业务异常，方便 Controller 返回错误提示。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：管理后台点「同步商品索引 / 同步 RAG」——不直连 search 库，经 Feign 调 search 服务
 * - 角色：admin 侧防腐层。后台 Service/Controller 应调本类，而不是自己处理 Feign 的 ResponseVO。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. 管理端接口 → 本类 productData() / ragData()
 *    → 作用：发起「请 search 重建索引/向量」
 * 2. 本类 → SearchToolFeignClient（失败时进 SearchToolFeignFallbackFactory）
 *    → 作用：HTTP 打到 Gateway /internal/search/tool/*
 * 3a. 成功：search 服务执行重建，本类正常返回
 * 3b. 失败：抛业务异常
 *    → 作用：页面提示失败，不会误以为已经同步成功
 * </pre>
 */

@Component
public class SearchToolFeignSupport {

    @Resource
    private SearchToolFeignClient searchToolFeignClient;
    @Resource
    private FeignResponseSupport feignResponseSupport;

    public void productData() {
        feignResponseSupport.run(searchToolFeignClient::productData, "同步商品搜索/RAG数据失败");
    }

    public void ragData() {
        feignResponseSupport.run(searchToolFeignClient::ragData, "同步RAG FAQ数据失败");
    }
}
