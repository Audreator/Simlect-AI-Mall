package com.simlect.controller.admin;

import com.simlect.component.AdminLoginLockService;
import com.simlect.component.RedisComponent;
import com.simlect.entity.config.AppConfig;
import com.simlect.entity.vo.CheckCodeVO;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.exception.BusinessException;
import com.simlect.service.PasswordService;
import com.simlect.utils.AuthCookieHelper;
import com.simlect.utils.CheckCodeGenerator;
import com.simlect.utils.IpUtils;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController("adminAccountController")
@RequestMapping("/admin/account")
@Validated
public class AccountController extends com.simlect.controller.admin.ABaseController {
    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AuthCookieHelper authCookieHelper;

    @Resource
    private AdminLoginLockService adminLoginLockService;

    @Resource
    private PasswordService passwordService;

    @PostMapping("/checkCode")
    public ResponseVO checkCode(){
        CheckCodeVO checkCodeVO = CheckCodeGenerator.generate(redisComponent);
        return getSuccessResponseVO(checkCodeVO);
    }

    @PostMapping("/login")
    public ResponseVO login(@NotEmpty String account,
                            @NotEmpty String password,
                            @NotEmpty String checkCode,
                            @NotEmpty String checkCodeKey){
        HttpServletRequest request = currentRequest();
        String ip = IpUtils.resolveClientIp(request);
        adminLoginLockService.ensureNotLocked(ip);
        try {
            if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))){
                throw new BusinessException("验证码错误！");
            }
            if(!account.equals(appConfig.getAdminAccount()) || !passwordService.matches(password, appConfig.getAdminPasswordHash())){
                throw new BusinessException("账号或密码错误！");
            }
            adminLoginLockService.clearFailures(ip);
            String token = redisComponent.saveToken4Admin(account);
            HttpServletResponse response = currentResponse();
            authCookieHelper.writeAdminTokenCookie(request, response, token);
            return getSuccessResponseVO(null);
        } catch (BusinessException e) {
            adminLoginLockService.recordFailure(ip);
            throw e;
        } finally {
            redisComponent.cleanCheckCode(checkCodeKey);
        }
    }

    @PostMapping("/logout")
    public ResponseVO logout(){
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        String token = authCookieHelper.resolveAdminToken(request);
        if (!StringTools.isEmpty(token)) {
            redisComponent.cleanToken4Admin(token);
        }
        authCookieHelper.clearAdminTokenCookie(request, response);
        return getSuccessResponseVO(null);
    }

    /** 管理端登录态校验（前端路由守卫用）：已登录返回 true，未登录返回 false */
    @GetMapping("/checkLogin")
    public ResponseVO<Boolean> checkLogin(HttpServletRequest request){
        String token = authCookieHelper.resolveAdminToken(request);
        boolean logged = !StringTools.isEmpty(token)
                && redisComponent.getLoginInfo4Admin(token) != null;
        return getSuccessResponseVO(logged);
    }

    /**
     * 敏感操作二次确认：校验管理员密码，签发一次性确认票据（防明文密码入请求头）。
     * 前端拿到票据后在敏感请求头 X-Admin-Confirm-Token 中携带，切面原子消费。
     */
    @PostMapping("/confirmPassword")
    public ResponseVO<String> confirmPassword(@NotEmpty String password,
                                              @NotEmpty String targetPath,
                                              HttpServletRequest request){
        if (!passwordService.matches(password, appConfig.getAdminPasswordHash())) {
            throw new BusinessException("管理员密码确认失败");
        }
        String token = authCookieHelper.resolveAdminToken(request);
        Object loginInfo = redisComponent.getLoginInfo4Admin(token);
        if (StringTools.isEmpty(token) || loginInfo == null) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }
        // 票据绑定管理员与目标操作路径（防 5 分钟内票据泄露后被用于任意敏感操作）
        String path = targetPath.trim();
        // 路径归一化：与切面消费端一致（去掉 /api 或 /admin-api 前缀）
        if (path.startsWith("/api/")) {
            path = path.substring(4);
        } else if (path.startsWith("/admin-api/")) {
            path = path.substring(10);
        }
        if (!path.startsWith("/admin/")) {
            throw new BusinessException("非法的敏感操作路径");
        }
        return getSuccessResponseVO(redisComponent.issueAdminConfirmToken(
                String.valueOf(loginInfo), path));
    }

    private HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private HttpServletResponse currentResponse() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
    }
}
