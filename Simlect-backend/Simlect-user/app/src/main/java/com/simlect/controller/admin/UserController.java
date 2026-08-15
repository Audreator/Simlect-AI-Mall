package com.simlect.controller.admin;

import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.component.RedisComponent;
import com.simlect.component.UserTempBanService;
import com.simlect.api.enums.UserStatusEnum;
import com.simlect.entity.po.UserInfo;
import com.simlect.entity.query.UserInfoQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.biz.UserInfoService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/admin/user")
@RestController
public class UserController extends com.simlect.controller.admin.ABaseController{

    @Resource
    private UserInfoService userInfoService;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserTempBanService userTempBanService;

    // 加载所有用户
    @PostMapping("/loadUser")
    public ResponseVO loadUser(Integer pageNo, Integer pageSize, Integer status, String nickNameFuzzy){
        UserInfoQuery userInfoQuery = new UserInfoQuery();
        userInfoQuery.setPageNo(pageNo);
        userInfoQuery.setPageSize(pageSize);
        userInfoQuery.setStatus(status);
        userInfoQuery.setNickNameFuzzy(nickNameFuzzy);
        userInfoQuery.setOrderBy("join_time desc");
        PaginationResultVO<UserInfo> resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

    // 改变用户状态
    @AdminSensitiveConfirm
    @PostMapping("/changeStatus")
    public ResponseVO changeStatus(@NotNull String userId, @NotNull Integer status){
        // 根据userId查询用户
        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId);
        userInfo.setStatus(status);
        userTempBanService.clearTempBanMark(userId);
        if (status.equals(UserStatusEnum.DISABLE.getStatus())) {
            redisComponent.cleanAllToken(userId);
        }
        userInfoService.updateUserInfoByUserId(userInfo, userId);
        return getSuccessResponseVO(null);
    }
}
