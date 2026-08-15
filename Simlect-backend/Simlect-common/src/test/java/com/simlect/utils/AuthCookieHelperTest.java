package com.simlect.utils;

import com.simlect.constants.Constants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCookieHelperTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthCookieHelper helper;

    @Test
    void resolveWebToken_fromHeader() {
        when(request.getHeader(Constants.TOKEN_WEB)).thenReturn("hdr-token");

        assertEquals("hdr-token", helper.resolveWebToken(request));
    }

    @Test
    void resolveWebToken_fallsBackToCookie() {
        when(request.getHeader(Constants.TOKEN_WEB)).thenReturn(null);
        Cookie[] cookies = new Cookie[]{new Cookie(Constants.TOKEN_WEB, "cookie-token")};
        when(request.getCookies()).thenReturn(cookies);

        assertEquals("cookie-token", helper.resolveWebToken(request));
    }

    @Test
    void resolveWebToken_noHeaderNoCookie_returnsNull() {
        when(request.getHeader(Constants.TOKEN_WEB)).thenReturn("");
        when(request.getCookies()).thenReturn(new Cookie[0]);

        assertNull(helper.resolveWebToken(request));
    }

    @Test
    void resolveAdminToken_fromHeader() {
        when(request.getHeader(Constants.TOKEN_ADMIN)).thenReturn("admin-hdr");

        assertEquals("admin-hdr", helper.resolveAdminToken(request));
    }

    @Test
    void resolveAdminToken_fallsBackToCookie() {
        when(request.getHeader(Constants.TOKEN_ADMIN)).thenReturn(null);
        Cookie[] cookies = new Cookie[]{new Cookie(Constants.TOKEN_ADMIN, "admin-cookie")};
        when(request.getCookies()).thenReturn(cookies);

        assertEquals("admin-cookie", helper.resolveAdminToken(request));
    }

    @Test
    void writeWebTokenCookie_addsSetCookieHeader() {
        when(request.isSecure()).thenReturn(true);

        helper.writeWebTokenCookie(request, response, "abc-token");

        ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), header.capture());
        String cookie = header.getValue();
        assertTrue(cookie.contains("token=abc-token"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Path=/"));
    }

    @Test
    void writeAdminTokenCookie_nonSecureRequest() {
        when(request.isSecure()).thenReturn(false);

        helper.writeAdminTokenCookie(request, response, "admin-token");

        ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), header.capture());
        String cookie = header.getValue();
        assertTrue(cookie.contains("adminToken=admin-token"));
        assertTrue(!cookie.contains("Secure"));
    }

    @Test
    void clearWebTokenCookie_setsMaxAgeZero() {
        when(request.isSecure()).thenReturn(false);

        helper.clearWebTokenCookie(request, response);

        ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), header.capture());
        assertTrue(header.getValue().contains("token="));
        assertTrue(header.getValue().contains("Max-Age=0"));
    }

    @Test
    void clearAdminTokenCookie_setsMaxAgeZero() {
        when(request.isSecure()).thenReturn(false);

        helper.clearAdminTokenCookie(request, response);

        ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), header.capture());
        assertTrue(header.getValue().contains("adminToken="));
        assertTrue(header.getValue().contains("Max-Age=0"));
    }
}
