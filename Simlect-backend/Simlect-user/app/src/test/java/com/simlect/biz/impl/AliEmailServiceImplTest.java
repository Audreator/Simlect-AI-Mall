package com.simlect.biz.impl;

import com.simlect.biz.EmailService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AliEmailServiceImpl 单元测试。
 * <p>
 * 该实现中阿里云 {@code DefaultAcsClient} 在方法内部直接 {@code new} 构造，
 * 无法注入 mock；且成功路径依赖真实阿里云 DirectMail 网络调用。
 * 纯单测环境下既无法打桩客户端，也无法离线断言成功行为，故整体标记 @Disabled。
 */
@ExtendWith(MockitoExtension.class)
@Disabled("sendVerificationCode 内部 new DefaultAcsClient 且依赖真实阿里云网络，无法纯单测")
class AliEmailServiceImplTest {

    @InjectMocks
    private AliEmailServiceImpl aliEmailService;

    @Test
    void placeholder() {
        // 见类级 @Disabled 说明
    }

    @Test
    void serviceImplementsEmailService() {
        assertTrue(aliEmailService instanceof EmailService);
    }

    private static void assertTrue(boolean value) {
        org.junit.jupiter.api.Assertions.assertTrue(value);
    }
}
