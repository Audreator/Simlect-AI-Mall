package com.simlect.aspect;

import com.simlect.annotation.GlobalInterceptor;
import com.simlect.component.RedisComponent;
import com.simlect.entity.dto.TokenUserInfoDTO;
import com.simlect.entity.enums.ResponseCodeEnum;
import com.simlect.exception.BusinessException;
import com.simlect.utils.AuthCookieHelper;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Component
@Aspect
@Order(1)
public class GlobalOperationAspect {

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AuthCookieHelper authCookieHelper;

    // 校验登录
    @Before("@annotation(com.simlect.annotation.GlobalInterceptor)")
    public void interceptorDo(JoinPoint point){
        try {
            // 获取方法签名
            MethodSignature methodSignature = (MethodSignature) point.getSignature();
            // 获取目标方法
            Method method = methodSignature.getMethod();
            // 判断当前操作的方法是否带了GlobalInterceptor注解
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (interceptor == null){
                return;
            }
            // 若携带了GlobalInterceptor注解，判断当前的checkLogin状态是否为true
            // 若checkLogin状态为true，则调用检验登录方法
            if (interceptor.checkLogin()){
                checkLogin();
            }
        }catch (BusinessException e){
            throw e;
        }catch (Exception e){
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }

    // 校验登录的具体方法
    private void checkLogin(){
        // 获取操作前状态请求头中的token
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = authCookieHelper.resolveWebToken(request);
        // 判断当前token是否有效
        // 若当前token为空，返回登陆超时异常
        if (StringTools.isEmpty(token)){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        // 根据token查询用户
        TokenUserInfoDTO tokenUserInfoDto = redisComponent.getTokenUserInfo(token);
        // 若查询不到用户，返回登陆超时异常
        if(tokenUserInfoDto == null ){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
    }
}
