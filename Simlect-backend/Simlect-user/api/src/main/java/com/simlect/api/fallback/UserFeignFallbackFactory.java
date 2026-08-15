package com.simlect.api.fallback;

import com.simlect.api.UserFeignClient;
import com.simlect.api.dto.UserAddressQueryDTO;
import com.simlect.api.dto.UserGrowthAddDTO;
import com.simlect.api.dto.UserIdsDTO;
import com.simlect.api.dto.UserJoinCountDTO;
import com.simlect.api.dto.UserNotifyDTO;
import com.simlect.api.support.FeignFallbackResponses;
import com.simlect.api.vo.UserAddressVO;
import com.simlect.api.vo.UserBriefVO;
import com.simlect.entity.vo.ResponseVO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 这个类干什么？
 * 当 Feign 调不到「simlect-user（用户）」时（超时、宕机、熔断、网络错误），OpenFeign 会回调本工厂的 create()。
 * create() 返回一个「假的 UserFeignClient」：方法里不再发 HTTP，直接返回「用户服务暂不可用」的 ResponseVO。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单校验收货地址、查用户简要信息——用户/地址在 user 库
 * - 角色：远程调用失败时的兜底。正常调用不会进这个类；它不实现真实业务，也不假装成功。
 * <p>
 * 调用链（每一步的作用）：
 * <pre>
 * 1. order → UserFeignSupport
 *    → 作用：业务只关心远程调用成功还是失败
 * 2. UserFeignSupport → UserFeignClient.xxx()
 *    → 作用：经 Gateway + X-Internal-Token 访问 /internal/user/*
 * 3a. 成功：进入目标微服务
 *    → 作用：返回地址/用户快照
 * 3b. 失败：本类 create(cause) → 假 Client
 *    → 作用：返回统一「不可用」，并记日志（cause）
 * 4. Support 拆包 → 抛业务异常 / 上层失败
 *    → 作用：下单无法校验地址则失败
 * </pre>
 * 为什么要降级工厂？
 * 避免超时异常打爆线程或返回 null 导致 NPE；把「下游挂了」变成明确失败（下单场景必须回滚）。
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {

    /**
     * Feign 调用失败时由框架回调。
     * @param cause 下游失败根因，必须打日志
     * @return 假客户端，各方法返回「服务暂不可用」
     */
    @Override
    public UserFeignClient create(Throwable cause) {
        log.warn("User Feign fallback: {}", cause == null ? "unknown" : cause.toString());
        return new UserFeignClient() {
                @Override
                public ResponseVO<UserAddressVO> getAddress(UserAddressQueryDTO dto) {
                    return FeignFallbackResponses.unavailable("用户服务");
                }

                @Override
                public ResponseVO<Void> addGrowthOnPay(UserGrowthAddDTO dto) {
                    return FeignFallbackResponses.unavailable("用户服务");
                }

                @Override
                public ResponseVO<Void> sendNotifyAsync(UserNotifyDTO dto) {
                    return FeignFallbackResponses.unavailable("用户服务");
                }

                @Override
                public ResponseVO<List<String>> listAllUserIds() {
                    return FeignFallbackResponses.unavailable("用户服务");
                }

                @Override
                public ResponseVO<List<String>> listUserIdsByPage(Integer pageNo, Integer pageSize) {
                    return FeignFallbackResponses.unavailable("用户服务");
                }

                @Override
                public ResponseVO<List<UserBriefVO>> listBriefByUserIds(UserIdsDTO dto) {
                    return FeignFallbackResponses.unavailable("用户服务");
                }

                @Override
                public ResponseVO<Integer> countByJoinDate(UserJoinCountDTO dto) {
                    return FeignFallbackResponses.unavailable("用户服务");
                }
            };
    }
}
