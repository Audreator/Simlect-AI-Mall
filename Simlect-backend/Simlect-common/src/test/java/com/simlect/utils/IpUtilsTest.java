package com.simlect.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void nullRequest_returnsUnknown() {
        assertEquals("unknown", IpUtils.resolveClientIp(null));
    }

    @Test
    void xForwardedFor_takesRightmostTrustedAddress() {
        // 只信任最右一跳（网关写入的真实 IP），客户端伪造的左侧地址被忽略
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");

        assertEquals("5.6.7.8", IpUtils.resolveClientIp(request));
    }

    @Test
    void xForwardedFor_spoofedByClient_ignored() {
        // 攻击者伪造 XFF 指向受害者 IP：应取最右真实 IP，而非伪造值
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.66, 8.8.8.8");

        assertEquals("8.8.8.8", IpUtils.resolveClientIp(request));
    }

    @Test
    void xForwardedFor_invalidRightmost_skipsToValid() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, not-an-ip, 5.6.7.8");

        assertEquals("5.6.7.8", IpUtils.resolveClientIp(request));
    }

    @Test
    void fallsBackThroughProxyHeaders() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("unknown");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("10.0.0.1");

        assertEquals("10.0.0.1", IpUtils.resolveClientIp(request));
    }

    @Test
    void fallsBackToXRealIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn("unknown");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.5");

        assertEquals("192.168.1.5", IpUtils.resolveClientIp(request));
    }

    @Test
    void fallsBackToRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("127.0.0.1", IpUtils.resolveClientIp(request));
    }

    @Test
    void allUnknown_returnsUnknown() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("UNKNOWN");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        assertEquals("unknown", IpUtils.resolveClientIp(request));
    }
}
