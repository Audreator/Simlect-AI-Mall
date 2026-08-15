package com.simlect.controller.internal;

import com.simlect.api.dto.UserAddressQueryDTO;
import com.simlect.api.dto.UserGrowthAddDTO;
import com.simlect.api.dto.UserIdsDTO;
import com.simlect.api.dto.UserJoinCountDTO;
import com.simlect.api.dto.UserNotifyDTO;
import com.simlect.api.vo.UserAddressVO;
import com.simlect.api.vo.UserBriefVO;
import com.simlect.biz.UserInternalService;
import com.simlect.controller.ABaseController;
import com.simlect.entity.vo.ResponseVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/internal/user")
public class UserInternalController extends ABaseController {

    @Resource
    private UserInternalService userInternalService;

    @PostMapping("/address/get")
    public ResponseVO<UserAddressVO> getAddress(@Valid @RequestBody UserAddressQueryDTO dto) {
        return getSuccessResponseVO(userInternalService.getAddress(dto.getAddressId(), dto.getUserId()));
    }

    @PostMapping("/member/addGrowthOnPay")
    public ResponseVO<Void> addGrowthOnPay(@Valid @RequestBody UserGrowthAddDTO dto) {
        userInternalService.addGrowthOnPay(dto.getUserId(), dto.getPayAmount());
        return getSuccessResponseVO(null);
    }

    @PostMapping("/notify/sendAsync")
    public ResponseVO<Void> sendNotifyAsync(@RequestBody UserNotifyDTO dto) {
        userInternalService.sendNotifyAsync(dto);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/listAllUserIds")
    public ResponseVO<List<String>> listAllUserIds() {
        return getSuccessResponseVO(userInternalService.listAllUserIds());
    }

    /** 分页拉取用户 ID（通知广播大批量场景） */
    @PostMapping("/listUserIdsByPage")
    public ResponseVO<List<String>> listUserIdsByPage(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        return getSuccessResponseVO(userInternalService.listUserIdsByPage(pageNo, pageSize));
    }

    @PostMapping("/listBriefByUserIds")
    public ResponseVO<List<UserBriefVO>> listBriefByUserIds(@RequestBody UserIdsDTO dto) {
        List<String> ids = dto == null ? Collections.emptyList() : dto.getUserIds();
        return getSuccessResponseVO(userInternalService.listBriefByUserIds(ids));
    }

    @PostMapping("/countByJoinDate")
    public ResponseVO<Integer> countByJoinDate(@RequestBody UserJoinCountDTO dto) {
        String start = dto == null ? null : dto.getJoinDateStart();
        String end = dto == null ? null : dto.getJoinDateEnd();
        return getSuccessResponseVO(userInternalService.countByJoinDate(start, end));
    }
}
