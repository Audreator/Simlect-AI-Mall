package com.simlect.api.fallback;

import com.simlect.api.AdminAuditFeignClient;
import com.simlect.api.dto.AdminAuditLogDTO;
import com.simlect.api.support.FeignFallbackResponses;
import com.simlect.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 这个类干什么？
 * 当 Feign 调不到「管理审计内部接口」时（超时、宕机、熔断、网络错误），OpenFeign 会回调本工厂的 create()。
 * create() 返回一个「假的 AdminAuditFeignClient」：不再发 HTTP，直接返回「审计服务暂不可用」。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：管理端操作写审计日志（谁在何时做了什么）
 * - 角色：远程调用失败时的兜底。正常不会进这个类；不假装审计已写入。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. admin → AdminAuditFeignSupport.log(...)
 *    → 作用：记录管理操作
 * 2. Support → AdminAuditFeignClient.log
 *    → 作用：经 Gateway + 内部令牌访问 /internal/admin/audit/*
 * 3a. 成功：审计落库
 * 3b. 失败：本类 create(cause) → 假 Client
 *    → 作用：返回不可用，日志记下 cause
 * 4. Support/上层按失败处理
 *    → 作用：避免管理员以为审计已成功写入
 * </pre>
 * 为什么要降级工厂？
 * 把下游故障变成明确失败语义，而不是异常乱冒或静默成功。
 */
@Slf4j
@Component
public class AdminAuditFeignFallbackFactory implements FallbackFactory<AdminAuditFeignClient> {

    /**
     * Feign 调用失败时由框架回调。
     * @param cause 下游失败根因，必须打日志
     * @return 假客户端，log 方法返回「服务暂不可用」
     */
    @Override
    public AdminAuditFeignClient create(Throwable cause) {
        log.warn("Admin audit Feign fallback: {}", cause == null ? "unknown" : cause.toString());
        return new AdminAuditFeignClient() {
            @Override
            public ResponseVO<Void> log(AdminAuditLogDTO dto) {
                return FeignFallbackResponses.unavailable("管理服务");
            }
        };
    }
}
