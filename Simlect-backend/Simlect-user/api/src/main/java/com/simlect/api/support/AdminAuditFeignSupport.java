package com.simlect.api.support;

import com.simlect.api.AdminAuditFeignClient;
import com.simlect.api.dto.AdminAuditLogDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
/**
 * 这个类干什么？
 * 业务 Service 调用 simlect-user（管理审计落在用户域相关能力） 时的门面：内部转调 AdminAuditFeignClient，
 * 把 ResponseVO 拆成领域数据；失败（含降级「不可用」）统一转成 BusinessException。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：管理操作写审计日志等内部调用
 * - 角色：防腐层。admin 应注入本类，而不是自己处理 Feign 的 ResponseVO/异常细节。
 * <p>
 * 调用链：
 * <pre>
 * 1. 领域 Service（如下单）→ 本类对外方法
 * 2. 本类 → AdminAuditFeignClient（Feign；失败可能进 AdminAuditFeignFallbackFactory）
 * 3. 成功：取出 data 返回给 Service
 * 4. 失败：抛业务异常 → 管理操作可提示失败，避免以为审计已写入
 * </pre>
 * 为什么多这一层？避免每个 Service 重复「拆包 + 判 status + 转异常」，并固定失败语义。
 */

@Slf4j
@Component
public class AdminAuditFeignSupport {

    @Resource
    private AdminAuditFeignClient adminAuditFeignClient;

    public void log(String operator, String action, String targetUserId, String detail) {
        try {
            adminAuditFeignClient.log(new AdminAuditLogDTO(operator, action, targetUserId, detail));
        } catch (Exception e) {
            log.warn("写入管理端审计日志失败 action={}, operator={}", action, operator, e);
        }
    }
}
