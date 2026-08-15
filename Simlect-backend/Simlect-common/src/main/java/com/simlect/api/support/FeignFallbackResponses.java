package com.simlect.api.support;

import com.simlect.entity.enums.ResponseCodeEnum;
import com.simlect.entity.vo.ResponseVO;
import org.slf4j.Logger;

/**
 * 这个类干什么？
 * 给所有 *FeignFallbackFactory 用的小工具方法：拼一份标准的「某某服务暂不可用」ResponseVO。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：微服务 Feign 调用失败后的统一降级返回
 * - 角色：不参与业务；只统一错误码和提示文案，避免每个假 Client 各写一套
 * <p>
 * 调用链：
 * FallbackFactory.create → 假 Client 某方法 → unavailable(...) → 回到 FeignSupport → 转业务异常。
 * 作用：业务侧永远按失败处理；日志/文案里带上服务名，方便排障。
 */
public final class FeignFallbackResponses {

    private FeignFallbackResponses() {
    }

    public static <T> ResponseVO<T> unavailable(String serviceLabel) {
        return unavailable(null, serviceLabel, null);
    }

    /**
     * @param log 可空；非空时打 warn，带上 cause
     * @param serviceLabel 如「库存服务」「搜索服务」
     * @param cause Feign 失败根因，可空
     */
    public static <T> ResponseVO<T> unavailable(Logger log, String serviceLabel, Throwable cause) {
        if (log != null) {
            log.warn("{} 触发降级: {}", serviceLabel, cause == null ? "unknown" : cause.toString());
        }
        ResponseVO<T> vo = new ResponseVO<>();
        vo.setStatus("error");
        vo.setCode(ResponseCodeEnum.CODE_500.getCode());
        vo.setInfo(serviceLabel + "暂不可用，请稍后重试");
        vo.setData(null);
        return vo;
    }
}
