package com.simlect.api.support;

import com.simlect.api.UserFeignClient;
import com.simlect.api.dto.UserAddressQueryDTO;
import com.simlect.api.dto.UserGrowthAddDTO;
import com.simlect.api.dto.UserIdsDTO;
import com.simlect.api.dto.UserJoinCountDTO;
import com.simlect.api.dto.UserNotifyDTO;
import com.simlect.api.vo.UserAddressVO;
import com.simlect.api.vo.UserBriefVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * 这个类干什么？
 * 业务 Service 调用 simlect-user（用户） 时的门面：内部转调 UserFeignClient，
 * 把 ResponseVO 拆成领域数据；失败（含降级「不可用」）统一转成 BusinessException。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单校验收货地址、查用户简要信息等——用户/地址在 user 库
 * - 角色：防腐层。order 等业务应注入本类，而不是自己处理 Feign 的 ResponseVO/异常细节。
 * <p>
 * 调用链：
 * <pre>
 * 1. 领域 Service（如下单）→ 本类对外方法
 * 2. 本类 → UserFeignClient（Feign；失败可能进 UserFeignFallbackFactory）
 * 3. 成功：取出 data 返回给 Service
 * 4. 失败：抛业务异常 → 下单无法校验地址则失败，避免写脏订单
 * </pre>
 * 为什么多这一层？避免每个 Service 重复「拆包 + 判 status + 转异常」，并固定失败语义。
 */

@Slf4j
@Component
public class UserFeignSupport {

    @Resource
    private UserFeignClient userFeignClient;
    @Resource
    private FeignResponseSupport feignResponseSupport;

    public UserAddressVO getAddress(String addressId, String userId) {
        return feignResponseSupport.call(
                () -> userFeignClient.getAddress(new UserAddressQueryDTO(addressId, userId)),
                "查询收货地址失败");
    }

    public void addGrowthOnPay(String userId, BigDecimal payAmount) {
        feignResponseSupport.run(
                () -> userFeignClient.addGrowthOnPay(new UserGrowthAddDTO(userId, payAmount)),
                "增加成长值失败");
    }

    public void sendNotifyAsync(String userId, String title, String content, String bizType, String bizId) {
        try {
            feignResponseSupport.run(
                    () -> userFeignClient.sendNotifyAsync(new UserNotifyDTO(userId, title, content, bizType, bizId)),
                    "发送站内通知失败");
        } catch (Exception e) {
            log.warn("发送站内通知降级跳过 userId={}, title={}, err={}", userId, title, e.getMessage());
        }
    }

    public List<String> listAllUserIds() {
        try {
            List<String> ids = feignResponseSupport.call(
                    () -> userFeignClient.listAllUserIds(),
                    "查询用户列表失败");
            return ids == null ? Collections.emptyList() : ids;
        } catch (Exception e) {
            log.warn("查询全部用户ID降级为空: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 分页拉取用户 ID（通知广播大批量场景，避免全量加载） */
    public List<String> listUserIdsByPage(Integer pageNo, Integer pageSize) {
        try {
            List<String> ids = feignResponseSupport.call(
                    () -> userFeignClient.listUserIdsByPage(pageNo, pageSize),
                    "分页查询用户列表失败");
            return ids == null ? Collections.emptyList() : ids;
        } catch (Exception e) {
            log.warn("分页查询用户ID降级为空: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, UserBriefVO> mapBriefByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<UserBriefVO> list = feignResponseSupport.call(
                    () -> userFeignClient.listBriefByUserIds(new UserIdsDTO(userIds)),
                    "批量查询用户信息失败");
            if (list == null || list.isEmpty()) {
                return Collections.emptyMap();
            }
            return list.stream()
                    .filter(u -> u != null && u.getUserId() != null)
                    .collect(Collectors.toMap(UserBriefVO::getUserId, Function.identity(), (a, b) -> a));
        } catch (Exception e) {
            log.warn("批量查询用户信息降级为空: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Integer countByJoinDate(String joinDateStart, String joinDateEnd) {
        Integer count = feignResponseSupport.call(
                () -> userFeignClient.countByJoinDate(new UserJoinCountDTO(joinDateStart, joinDateEnd)),
                "统计新用户失败");
        return count == null ? 0 : count;
    }
}
