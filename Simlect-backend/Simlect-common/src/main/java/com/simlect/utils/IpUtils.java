package com.simlect.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtils {

    private IpUtils() {
    }

    /**
     * 解析客户端真实 IP：
     * - 只信任 X-Forwarded-For 最右侧（离服务最近一跳，即网关写入的）IP，
     *   防止客户端伪造左侧地址绕过限流/定向锁死
     * - 无有效 XFF 时回退 X-Real-IP / remoteAddr
     */
    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = extractTrusted(request.getHeader("X-Forwarded-For"));
        if (ip == null) {
            String proxy = request.getHeader("Proxy-Client-IP");
            if (isValidIp(proxy)) {
                ip = proxy;
            }
        }
        if (ip == null) {
            String wlProxy = request.getHeader("WL-Proxy-Client-IP");
            if (isValidIp(wlProxy)) {
                ip = wlProxy;
            }
        }
        if (ip == null) {
            String real = request.getHeader("X-Real-IP");
            if (isValidIp(real)) {
                ip = real;
            }
        }
        if (ip == null) {
            String remote = request.getRemoteAddr();
            if (isValidIp(remote)) {
                ip = remote;
            }
        }
        return ip == null ? "unknown" : ip;
    }

    /** 从 X-Forwarded-For 链中取最右侧合法 IP（最近一跳=网关写入的真实客户端 IP） */
    private static String extractTrusted(String forwarded) {
        if (StringTools.isEmpty(forwarded)) {
            return null;
        }
        String[] parts = forwarded.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = parts[i].trim();
            if (isValidIp(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isValidIp(String s) {
        if (StringTools.isEmpty(s) || "unknown".equalsIgnoreCase(s)) {
            return false;
        }
        String v = s.trim();
        if (v.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            return true;
        }
        // IPv6（含冒号且无端口/路径等非法字符），避免 IPv6 客户端绕过登录锁定/限流
        return v.contains(":") && !v.contains("/") && !v.contains(" ") && !v.contains(",");
    }
}
