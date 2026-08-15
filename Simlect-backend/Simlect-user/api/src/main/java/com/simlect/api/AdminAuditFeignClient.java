package com.simlect.api;

import com.simlect.api.dto.AdminAuditLogDTO;
import com.simlect.api.fallback.AdminAuditFeignFallbackFactory;
import com.simlect.entity.vo.ResponseVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 这个接口干什么？
 * 声明「如何用 HTTP 调用 simlect-user（管理审计落在用户域相关能力） 的内部 API」。本身没有实现代码；
 * 运行时由 OpenFeign 生成代理，按方法上的 @PostMapping 发请求。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：管理操作写审计日志等内部调用
 * - 角色：跨服务契约（API 模块）。一域一库后禁止跨库 Mapper，同步读写别的域必须走 Feign。
 *   内部鉴权：请求头 X-Internal-Token 由 FeignInternalAuthInterceptor 自动加上。
 * <p>
 * 调用链：
 * <pre>
 * 1. admin → AdminAuditFeignSupport
 * 2. AdminAuditFeignSupport 调用本接口方法
 * 3. Feign 代理 → Gateway → 目标服务 /internal/...（见 Client 注解） → Controller → Service
 *    （失败时走 AdminAuditFeignFallbackFactory，见该类注释）
 * </pre>
 * 和 MQ/Outbox 的区别：本接口是同步 RPC（当下就要结果）；关单通知等异步最终一致走消息队列。
 */
@FeignClient(name = "simlect-admin", contextId = "adminAuditFeignClient", path = "/internal/admin/audit",
        fallbackFactory = AdminAuditFeignFallbackFactory.class)
public interface AdminAuditFeignClient {

    @PostMapping("/log")
    ResponseVO<Void> log(@RequestBody AdminAuditLogDTO dto);
}
