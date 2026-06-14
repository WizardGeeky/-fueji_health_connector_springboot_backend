package com.healthconnector.app.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Utility for extracting the real client IP address from HTTP requests.
 * Handles reverse proxy headers (X-Forwarded-For, X-Real-IP).
 */
@Component
public class IPUtil {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "X-Real-IP"
    };

    /**
     * Extracts the real client IP from the request, checking proxy headers first.
     */
    public String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For may contain a comma-separated list
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Extracts a simplified device descriptor from the User-Agent header.
     */
    public String getDeviceInfo(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (!StringUtils.hasText(ua)) return "Unknown";
        if (ua.contains("Mobile") || ua.contains("Android")) return "Mobile";
        if (ua.contains("Tablet") || ua.contains("iPad")) return "Tablet";
        return "Desktop";
    }
}
