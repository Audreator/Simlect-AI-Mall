package com.simlect.api;

import com.simlect.api.dto.UserAddressQueryDTO;
import com.simlect.api.dto.UserGrowthAddDTO;
import com.simlect.api.dto.UserIdsDTO;
import com.simlect.api.dto.UserJoinCountDTO;
import com.simlect.api.dto.UserNotifyDTO;
import com.simlect.api.vo.UserAddressVO;
import com.simlect.api.vo.UserBriefVO;
import com.simlect.api.fallback.UserFeignFallbackFactory;
import com.simlect.entity.vo.ResponseVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 这个接口干什么？
 * 声明「如何用 HTTP 调用 simlect-user（用户） 的内部 API」。本身没有实现代码；
 * 运行时由 OpenFeign 生成代理，按方法上的 @PostMapping 发请求。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：下单校验收货地址、查用户简要信息等——用户/地址在 user 库
 * - 角色：跨服务契约（API 模块）。一域一库后禁止跨库 Mapper，同步读写别的域必须走 Feign。
 *   内部鉴权：请求头 X-Internal-Token 由 FeignInternalAuthInterceptor 自动加上。
 * <p>
 * 调用链：
 * <pre>
 * 1. order 等 → UserFeignSupport
 * 2. UserFeignSupport 调用本接口方法
 * 3. Feign 代理 → Gateway → 目标服务 /internal/user/* → Controller → Service
 *    （失败时走 UserFeignFallbackFactory，见该类注释）
 * </pre>
 * 和 MQ/Outbox 的区别：本接口是同步 RPC（当下就要结果）；关单通知等异步最终一致走消息队列。
 */
@FeignClient(name = "simlect-user", contextId = "userFeignClient", path = "/internal/user",
        fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignClient {

    @PostMapping("/address/get")
    ResponseVO<UserAddressVO> getAddress(@RequestBody UserAddressQueryDTO dto);

    @PostMapping("/member/addGrowthOnPay")
    ResponseVO<Void> addGrowthOnPay(@RequestBody UserGrowthAddDTO dto);

    @PostMapping("/notify/sendAsync")
    ResponseVO<Void> sendNotifyAsync(@RequestBody UserNotifyDTO dto);

    @PostMapping("/listAllUserIds")
    ResponseVO<List<String>> listAllUserIds();

    /** 分页拉取用户 ID（通知广播大批量场景） */
    @PostMapping("/listUserIdsByPage")
    ResponseVO<List<String>> listUserIdsByPage(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize);

    @PostMapping("/listBriefByUserIds")
    ResponseVO<List<UserBriefVO>> listBriefByUserIds(@RequestBody UserIdsDTO dto);

    @PostMapping("/countByJoinDate")
    ResponseVO<Integer> countByJoinDate(@RequestBody UserJoinCountDTO dto);
}
