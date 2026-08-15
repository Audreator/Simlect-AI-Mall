package com.simlect.controller;

import com.simlect.annotation.GlobalInterceptor;
import com.simlect.annotation.RateLimit;
import com.simlect.component.RedisComponent;
import com.simlect.component.UserLoginLockService;
import com.simlect.component.UserTempBanService;
import com.simlect.constants.Constants;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.enums.DateTimePatternEnum;
import com.simlect.api.enums.UserSexEnum;
import com.simlect.api.enums.UserStatusEnum;
import com.simlect.entity.po.UserInfo;
import com.simlect.entity.query.UserInfoQuery;
import com.simlect.entity.vo.CheckCodeVO;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.api.vo.UserVO;
import com.simlect.exception.BusinessException;
import com.simlect.biz.EmailService;
import com.simlect.biz.impl.AliEmailServiceImpl;
import com.simlect.biz.impl.UserInfoServiceImpl;
import com.simlect.service.SlideCaptchaVerifier;
import com.simlect.utils.AuthCookieHelper;
import com.simlect.utils.CheckCodeGenerator;
import com.simlect.utils.DateUtil;
import com.simlect.utils.StringTools;
import com.simlect.service.PasswordService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RequestMapping("/account")
@RestController
@Validated
/**
 * 【功能】C 端账号：注册、登录、自动登录、登出。
 * <p>
 * 【角色】用户会话的「颁发方」。登录成功写入 Redis opaque token + Cookie；Gateway {@code AuthGlobalFilter} 是「校验方」。
 * <p>
 * 【登录调用链】login → 校验验证码 → 查用户/校验密码 → RedisComponent.saveTokenUserInfo
 * → AuthCookieHelper 写 Cookie → 后续请求经 Gateway 查 mall:token:web:{token}。
 */
public class AccountController extends ABaseController{

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoServiceImpl userInfoService;

    @Resource
    private AliEmailServiceImpl aliEmailServiceImpl;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

@Resource
private UserTempBanService userTempBanService;

@Resource
private UserLoginLockService userLoginLockService;

@Resource
private SlideCaptchaVerifier slideCaptchaVerifier;

    @Resource
    private AuthCookieHelper authCookieHelper;

    @Resource
    private PasswordService passwordService;

    private void throwIfAccountDisabled(String userId) {
        Long unbanAt = userTempBanService.getUnbanAtMs(userId);
        if (unbanAt != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("errorType", "ACCOUNT_TEMP_BANNED");
            data.put("unbanAt", unbanAt);
            throw new BusinessException(600, userTempBanService.buildTempBanMessage(unbanAt), data);
        }
        throw new BusinessException("账号被禁用！");
    }

    // 自动登录，检验当前token是否有效
    @GetMapping("/autoLogin")
    public ResponseVO autoLogin(){
        // 从请求头中获取token
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo();
        // 如果token不存在，则返回null
        if(tokenUserInfoDTO == null || StringTools.isEmpty(tokenUserInfoDTO.getToken()) ){
            return getSuccessResponseVO(null);
        }
        // 从redis验证token是否有效
        TokenUserInfoDTO validUserInfo = redisComponent.getTokenUserInfo(tokenUserInfoDTO.getToken());
        if (validUserInfo == null){
            // token无效，返回null
            return getSuccessResponseVO(null);
        }
        // 判断当前用户是否被封禁：status=0为封禁
        // 根据userId查询用户（Redis 残留 token / 库重导后用户不存在时勿 NPE）
        UserInfo userInfo = userInfoService.getUserInfoByUserId(validUserInfo.getUserId());
        if (userInfo == null || Objects.equals(userInfo.getStatus(), UserStatusEnum.DISABLE.getStatus())){
            return getSuccessResponseVO(null);
        }
        validUserInfo.setEmail(userInfo.getEmail());
        validUserInfo.setNickName(userInfo.getNickName());
        validUserInfo.setAvatar(userInfo.getAvatar());
        // 否则存入新token，刷新redis
        String newToken = redisComponent.saveTokenUserInfo(validUserInfo);
        validUserInfo.setToken(newToken);
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        authCookieHelper.writeWebTokenCookie(request, response, newToken);
        validUserInfo.setToken(null);
        return getSuccessResponseVO(validUserInfo);
    }

    // 获取验证码
    @GetMapping("/checkCode")
    public ResponseVO checkCode(){
        CheckCodeVO checkCodeVO = CheckCodeGenerator.generate(redisComponent);
        return getSuccessResponseVO(checkCodeVO);
    }

    // 注册
    // 邮箱email，昵称nickName，密码password，验证码checkCode，验证码key checkCodeKey
    @PostMapping("/register")
    public ResponseVO register(@NotEmpty @Email @Size(max = 150) String email,
                               @NotEmpty @Size(max = 20) String nickName,
                               @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String registerPassword,
                               @NotEmpty String checkCode){
            // 注册
            userInfoService.register(email, nickName, registerPassword, checkCode);
            return getSuccessResponseVO(null);
    }

    // 登录
    // 邮箱email，密码password，验证码checkCode，验证码key checkCodeKey
    @PostMapping("/login")
    public ResponseVO login(@NotEmpty @Email @Size(max = 150) String email,
                            @NotEmpty String password,
                            @NotEmpty String checkCodeKey,
                            @NotEmpty String checkCode
                            ){
        String clientIp = getClientIp();
        userLoginLockService.ensureNotLocked(clientIp);
        try {
            // 验证验证码
            if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))){
                throw new BusinessException("验证码错误！");
            }
            // 检查账号或密码
            // 查询UserInfo
            UserInfo userInfo = userInfoService.getUserInfoByEmail(email);
            if (userInfo == null || !passwordService.matches(password, userInfo.getPassword())){
                userLoginLockService.recordFailure(clientIp);
                throw new BusinessException("账号或密码错误！");
            }
            userLoginLockService.clearFailures(clientIp);
            if (!passwordService.isBcrypt(userInfo.getPassword())) {
                UserInfo upgrade = new UserInfo();
                upgrade.setPassword(passwordService.encode(password));
                UserInfoQuery upgradeQuery = new UserInfoQuery();
                upgradeQuery.setUserId(userInfo.getUserId());
                userInfoService.updateByParam(upgrade, upgradeQuery);
            }
            if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())){
                throwIfAccountDisabled(userInfo.getUserId());
            }
            // 登录成功，返回userId,nickName,avatar,token:TokenUserInfoDTO
            TokenUserInfoDTO tokenUserInfoDTO = new TokenUserInfoDTO();
            tokenUserInfoDTO.setUserId(userInfo.getUserId());
            tokenUserInfoDTO.setEmail(userInfo.getEmail());
            tokenUserInfoDTO.setNickName(userInfo.getNickName());
            tokenUserInfoDTO.setAvatar(userInfo.getAvatar());
            tokenUserInfoDTO.setToken(redisComponent.saveTokenUserInfo(tokenUserInfoDTO));
            HttpServletRequest request = currentRequest();
            HttpServletResponse response = currentResponse();
            authCookieHelper.writeWebTokenCookie(request, response, tokenUserInfoDTO.getToken());
            tokenUserInfoDTO.setToken(null);
            // 更新最近登录时间和ip
            Date now = DateUtil.parse(DateUtil.getTimeOnParttern(0, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()), DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern());
            String ip = getClientIp();
            userInfo.setLastLoginTime(now);
            userInfo.setLastLoginIp(ip);
            UserInfoQuery userInfoQuery = new UserInfoQuery();
            userInfoQuery.setUserId(userInfo.getUserId());
            userInfoService.updateByParam(userInfo, userInfoQuery);
            return getSuccessResponseVO(tokenUserInfoDTO);
        }finally {
            redisComponent.cleanCheckCode(checkCodeKey);
        }
    }

    // 退出登录（不要求 Redis 中 token 仍有效，始终清除 Cookie）
    @PostMapping("/logout")
    public ResponseVO logout(){
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        String token = authCookieHelper.resolveWebToken(request);
        if (!StringTools.isEmpty(token)){
            redisComponent.cleanTokenUserInfo(token);
        }
        authCookieHelper.clearWebTokenCookie(request, response);
        return getSuccessResponseVO(null);
    }

    // 获取个人信息
    @GetMapping("/getUserInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getUserInfo(){
        String userId = getTokenUserInfo().getUserId();
        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userInfo, userVO);
        return getSuccessResponseVO(userVO);
    }

    // 修改个人信息
    @PostMapping("/updateUserInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO updateUserInfo(String avatar,@NotEmpty String nickName,@NotNull Integer sex){
        String userId = getTokenUserInfo().getUserId();
        userInfoService.updateUserInfo(userId, avatar, nickName, sex);
        return getSuccessResponseVO(null);
    }

    // 修改密码
    @PostMapping("/updatePassword")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO updatePassword(@NotEmpty String oldPassword,@NotEmpty String password){
        String userId = getTokenUserInfo().getUserId();
        userInfoService.updatePassword(userId, oldPassword, password);
        return getSuccessResponseVO(null);
    }

    private String getClientIp() {
        return com.simlect.utils.IpUtils.resolveClientIp(currentRequest());
    }

    private HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private HttpServletResponse currentResponse() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
    }

    // 获取邮件验证码
    @PostMapping("/getEmailCode")
    @RateLimit(limitType = RateLimit.LimitType.IP, windowSeconds = 20, maxCount = 1, message = "获取验证码过于频繁，请稍后再试")
    public ResponseVO getEmailCode(@NotEmpty @Email @Size(max = 150) String email,
                                   @NotEmpty String captchaVerification){
        slideCaptchaVerifier.verify(captchaVerification);
        // email 维度限流：60 秒内同一邮箱最多发 1 次（换 IP 不可绕过）
        String emailLimitKey = "mall:email:code:limit:" + email;
        Long emailCount = stringRedisTemplate.opsForValue().increment(emailLimitKey);
        if (emailCount != null && emailCount == 1) {
            stringRedisTemplate.expire(emailLimitKey, 60, TimeUnit.SECONDS);
        }
        if (emailCount != null && emailCount > 1) {
            throw new BusinessException("验证码发送过于频繁，请 1 分钟后再试");
        }
        // 60秒内只能获取一次
        String existingCode = redisComponent.getEmailCode(email);
        if (existingCode != null){
            // 获取剩余时间，单位为毫秒
            Long expire = stringRedisTemplate.getExpire(Constants.REDIS_KEY_EMAIL_CODE + email, TimeUnit.MILLISECONDS);
            // 判断是否大于四分钟
            if (expire > 4 * 60 * 1000){
                throw new BusinessException("验证码已发送，请" + ((expire / 1000) - 240) + "秒后再试");
            }
        }
        String code = aliEmailServiceImpl.sendVerificationCode(email);
        // 存入redis，有效期5分钟
        redisComponent.saveEmailCode(email, code);
        return getSuccessResponseVO(null);
    }

    // 找回密码
    @PostMapping("/forgetPassword")
    public ResponseVO forgetPassword(@NotEmpty @Email @Size(max = 150) String email, @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String newPassword, @NotEmpty String checkCode){
        // 更新密码
        userInfoService.forgetPassword(email, newPassword, checkCode);
        return getSuccessResponseVO(null);
    }
}
